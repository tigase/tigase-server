/*
 * SubscribeModule.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.eventbus.component;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
import tigase.eventbus.EventListener;
import tigase.eventbus.component.stores.Affiliation;
import tigase.eventbus.component.stores.AffiliationStore;
import tigase.eventbus.component.stores.Subscription;
import tigase.eventbus.component.stores.SubscriptionStore;
import tigase.eventbus.impl.EventBusImplementation;
import tigase.eventbus.impl.EventName;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import java.util.*;
import java.util.logging.Level;

@Bean(name = SubscribeModule.ID, active = true)
public class SubscribeModule extends AbstractEventBusModule implements Initializable, UnregisterAware {

	public final static String ID = "subscribe";
	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "subscribe" },
			new String[] { null, "http://jabber.org/protocol/pubsub", null });
	@Inject
	private AffiliationStore affiliationStore;

	@Inject
	private EventBusComponent component;

	private final EventListener<EventBusImplementation.ListenerAddedEvent> eventBusHandlerAddedHandler = new EventListener<EventBusImplementation.ListenerAddedEvent>() {
		@Override
		public void onEvent(final EventBusImplementation.ListenerAddedEvent event) {
			if (!event.getPackageName().startsWith("tigase.eventbus"))
				SubscribeModule.this.onAddHandler(event.getEventName(), event.getPackageName());
		}
	};

	@Inject(nullAllowed = false, bean = "localEventBus")
	private EventBusImplementation localEventBus;

	@Inject
	private SubscriptionStore subscriptionStore;

	@Override
	public void beforeUnregister() {
		localEventBus.removeListener(eventBusHandlerAddedHandler);
	}

	public void clusterNodeConnected(JID node) {
		if (component.getComponentId().equals(node))
			return;
		// context.getSubscriptionStore().addSubscription(null,
		// "tigase:eventbus", JID.jidInstanceNS("eventbus", node, null));

		if (log.isLoggable(Level.FINER))
			log.finer("Node " + node + " is connected. Preparing subscribe request.");

		Set<Element> pubsubNodes = new HashSet<>();
		for (EventName eventName : localEventBus.getAllListenedEvents()) {
			pubsubNodes.add(prepareSubscribeElement(eventName, component.getComponentId(), null));
		}

		for (EventName eventName : subscriptionStore.getSubscribedEvents()) {
			Collection<Subscription> subscriptions = subscriptionStore.getSubscribersJIDs(eventName.getPackage(), eventName.getName()
			);
			for (Subscription subscription : subscriptions) {
				if (subscription.getServiceJID() != null)
					pubsubNodes.add(
							prepareSubscribeElement(eventName, subscription.getJid(), subscription.getServiceJID().toString()));
			}
		}

		if (!pubsubNodes.isEmpty())
			sendSubscribeRequest("eventbus@" + node.getDomain(), pubsubNodes);
	}

	public void clusterNodeDisconnected(JID node) {
		if (component.getComponentId().equals(node))
			return;

		if (log.isLoggable(Level.FINER))
			log.finer("Node " + node + " is disconnected.");
		subscriptionStore.remove(new Subscription(JID.jidInstanceNS("eventbus", node.getDomain(), null)));
	}

	@Override
	public String[] getFeatures() {
		return new String[] { "http://jabber.org/protocol/pubsub#subscribe" };
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void initialize() {
		localEventBus.addListener(EventBusImplementation.ListenerAddedEvent.class, eventBusHandlerAddedHandler);
	}

	protected void onAddHandler(String eventName, String eventPackage) {
		for (JID node : component.getNodesConnected()) {
			if (component.getComponentId().equals(node))
				continue;

			Element se = prepareSubscribeElement(new EventName(eventPackage, eventName), component.getComponentId(), null);
			sendSubscribeRequest("eventbus@" + node.getDomain(), Collections.singleton(se));
		}
	}

	private Element prepareSubscribeElement(EventName event, JID jid, String service) {
		Element subscribeElem = new Element("subscribe");
		subscribeElem.addAttribute("node", event.toString());
		subscribeElem.addAttribute("jid", jid.toString());

		if (service != null) {
			subscribeElem.addChild(new Element("service", service));
		}

		return subscribeElem;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getType() == StanzaType.set) {
			processSet(packet);
		} else
			throw new ComponentException(Authorization.NOT_ALLOWED, "Only type set is allowed.");
	}

	protected Element processClusterSubscription(final Packet packet) throws TigaseStringprepException {
		// subscription from cluster node
		log.finest("Processing cluster subscription request from " + packet.getStanzaFrom());
		List<Element> subscribeElements = packet.getElemChildrenStaticStr(new String[] { "iq", "pubsub" });

		for (Element subscribe : subscribeElements) {
			Element serviceItem = subscribe.getChild("service");

			final EventName parsedName = new EventName(subscribe.getAttributeStaticStr("node"));
			final JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));
			final String service;

			if (serviceItem != null && serviceItem.getCData() != null) {
				service = serviceItem.getCData();
			} else {
				service = null;
			}

			if (log.isLoggable(Level.FINE))
				log.fine("Node " + jid + " subscribed for events " + parsedName);

			Subscription subscription = new Subscription(jid);
			subscription.setInClusterSubscription(true);
			subscription.setServiceJID(JID.jidInstanceNS(service));

			subscriptionStore.addSubscription(parsedName.getPackage(), parsedName.getName(), subscription);

		}
		return null;
	}

	protected Element processNonClusterSubscription(final Packet packet) throws TigaseStringprepException, ComponentException {
		// subscription from something out of cluster
		if (log.isLoggable(Level.FINEST))
			log.finest("Processing noncluster subscription request from " + packet.getStanzaFrom());
		final Affiliation affiliation = affiliationStore.getAffiliation(packet.getStanzaFrom());

		if (!affiliation.isSubscribe()) {
			if (log.isLoggable(Level.FINE))
				log.fine(
						"Subscription rejected. Subscriber " + packet.getStanzaFrom() + " has bad affiliation: " + affiliation);
			throw new ComponentException(Authorization.FORBIDDEN, "Bad affiliation: " + affiliation);
		}

		List<Element> subscribeElements = packet.getElemChildrenStaticStr(new String[] { "iq", "pubsub" });
		Element response = new Element("pubsub", new String[] { "xmlns" },
				new String[] { "http://jabber.org/protocol/pubsub" });

		final Set<Element> subscribedNodes = new HashSet<>();
		for (Element subscribe : subscribeElements) {
			EventName parsedName = new EventName(subscribe.getAttributeStaticStr("node"));
			JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

			if (log.isLoggable(Level.FINE))
				log.fine("Entity " + jid + " subscribed for events " + parsedName);

			Subscription subscription = new Subscription(jid, packet.getStanzaTo());
			subscription.setInClusterSubscription(false);

			subscriptionStore.addSubscription(parsedName.getPackage(), parsedName.getName(), subscription);

			subscribedNodes.add(prepareSubscribeElement(parsedName, jid, packet.getStanzaTo().toString()));

			response.addChild(new Element("subscription", new String[] { "node", "jid", "subscription" },
					new String[] { parsedName.toString(), jid.toString(), "subscribed" }));
		}

		if (log.isLoggable(Level.FINER))
			log.finer("Forwarding subscription to: " + component.getNodesConnected());

		for (JID node : component.getNodesConnected()) {
			if (component.getComponentId().equals(node))
				continue;
			sendSubscribeRequest("eventbus@" + node.getDomain(), subscribedNodes);
		}

		return response;
	}

	private void processSet(final Packet packet) throws TigaseStringprepException, ComponentException {
		Element subscriptionResponse;
		if (isClusteredEventBus(packet.getStanzaFrom())) {
			subscriptionResponse = processClusterSubscription(packet);
		} else {
			subscriptionResponse = processNonClusterSubscription(packet);
		}

		Packet response = packet.okResult(subscriptionResponse, 0);
		response.setPermissions(Permissions.ADMIN);
		write(response);
	}

	protected void sendSubscribeRequest(final String to, Collection<Element> subscriptionElements) {
		try {
			final String id = nextStanzaID();
			Element iq = new Element("iq", new String[] { "from", "to", "type", "id" },
					new String[] { component.getComponentId().toString(), to, "set", id });

			Element pubsubElem = new Element("pubsub", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub" });
			iq.addChild(pubsubElem);

			subscriptionElements.forEach(pubsubElem::addChild);

			final Packet packet = Packet.packetInstance(iq);
			packet.setPermissions(Permissions.ADMIN);
			packet.setXMLNS(Packet.CLIENT_XMLNS);

			if (log.isLoggable(Level.FINER))
				log.finer("Sending subscribe request (id=" + id + ") to node " + to);

			write(packet, new AsyncCallback() {

				@Override
				public void onError(Packet responseStanza, String errorCondition) {
					// TODO Auto-generated method stub
					if (log.isLoggable(Level.FINE))
						log.fine("Subscription request was cancelled by node " + to + " with error " + errorCondition);
				}

				@Override
				public void onSuccess(Packet responseStanza) {
					// TODO Auto-generated method stub
					if (log.isLoggable(Level.FINE))
						log.fine("Subscription request was accepted by node " + to + ".");
				}

				@Override
				public void onTimeout() {
					// TODO Auto-generated method stub
					if (log.isLoggable(Level.FINE))
						log.fine("Subscription request timeout. Node " + to + " not answered.");

				}
			});
		} catch (Exception e) {
			log.log(Level.WARNING, "Why? Oh Why?", e);
		}
	}

}
