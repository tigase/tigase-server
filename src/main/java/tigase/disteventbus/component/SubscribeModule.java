package tigase.disteventbus.component;

import java.util.*;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
import tigase.disteventbus.EventHandler;
import tigase.disteventbus.component.stores.Affiliation;
import tigase.disteventbus.component.stores.Subscription;
import tigase.disteventbus.impl.EventName;
import tigase.disteventbus.impl.LocalEventBus;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

public class SubscribeModule extends AbstractEventBusModule {

	public final static String ID = "subscribe";
	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "subscribe" },
			new String[] { null, "http://jabber.org/protocol/pubsub", null });

	private final EventHandler eventBusHandlerAddedHandler = new EventHandler() {

		private final String[] NAME_PATH = new String[] { LocalEventBus.HANDLER_ADDED_EVENT_NAME, "name" };
		private final String[] XMLNS_PATH = new String[] { LocalEventBus.HANDLER_ADDED_EVENT_NAME, "xmlns" };

		@Override
		public void onEvent(String name, String xmlns, Element event) {
			String n = event.getCData(NAME_PATH);
			String x = event.getCData(XMLNS_PATH);
			if (x == null || !x.equals(LocalEventBus.EVENTBUS_INTERNAL_EVENTS_XMLNS))
				SubscribeModule.this.onAddHandler(n, x);
		}
	};

	@Override
	public void afterRegistration() {
		super.afterRegistration();
		context.getEventBusInstance().addHandler(LocalEventBus.HANDLER_ADDED_EVENT_NAME,
				LocalEventBus.EVENTBUS_INTERNAL_EVENTS_XMLNS, eventBusHandlerAddedHandler);
	}

	public void clusterNodeConnected(JID node) {
		if (context.getComponentID().equals(node))
			return;
		// context.getSubscriptionStore().addSubscription(null,
		// "tigase:eventbus", JID.jidInstanceNS("eventbus", node, null));

		if (log.isLoggable(Level.FINER))
			log.finer("Node " + node + " is connected. Preparing subscribe request.");

		Set<Element> pubsubNodes = new HashSet<>();
		for (EventName eventName : context.getEventBusInstance().getAllListenedEvents()) {
			pubsubNodes.add(prepareSubscribeElement(eventName, context.getComponentID(), null));
		}

		for (EventName eventName : context.getSubscriptionStore().getSubscribedEvents()) {
			Collection<Subscription> subscriptions = context.getSubscriptionStore().getSubscribersJIDs(eventName.getName(),
					eventName.getXmlns());
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
		if (context.getComponentID().equals(node))
			return;

		if (log.isLoggable(Level.FINER))
			log.finer("Node " + node + " is disconnected.");
		context.getSubscriptionStore().remove(new Subscription(JID.jidInstanceNS("eventbus", node.getDomain(), null)));
	}

	@Override
	public String[] getFeatures() {
		return new String[] { "http://jabber.org/protocol/pubsub#subscribe" };
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	protected void onAddHandler(String eventName, String eventXmlns) {
		for (JID node : context.getConnectedNodes()) {
			if (context.getComponentID().equals(node))
				continue;

			Element se = prepareSubscribeElement(new EventName(eventName, eventXmlns), context.getComponentID(), null);
			sendSubscribeRequest("eventbus@" + node.getDomain(), Collections.singleton(se));
		}
	}

	private Element prepareSubscribeElement(EventName event, JID jid, String service) {
		Element subscribeElem = new Element("subscribe");
		subscribeElem.addAttribute("node", event.toEventBusNode());
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

			final EventName parsedName = NodeNameUtil.parseNodeName(subscribe.getAttributeStaticStr("node"));
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

			context.getSubscriptionStore().addSubscription(parsedName.getName(), parsedName.getXmlns(), subscription);

		}
		return null;
	}

	protected Element processNonClusterSubscription(final Packet packet) throws TigaseStringprepException, ComponentException {
		// subscription from something out of cluster
		if (log.isLoggable(Level.FINEST))
			log.finest("Processing noncluster subscription request from " + packet.getStanzaFrom());

		final Affiliation affiliation = context.getAffiliationStore().getAffiliation(packet.getStanzaFrom());

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
			EventName parsedName = NodeNameUtil.parseNodeName(subscribe.getAttributeStaticStr("node"));
			JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

			if (log.isLoggable(Level.FINE))
				log.fine("Entity " + jid + " subscribed for events " + parsedName);

			Subscription subscription = new Subscription(jid, packet.getStanzaTo());
			subscription.setInClusterSubscription(false);

			context.getSubscriptionStore().addSubscription(parsedName.getName(), parsedName.getXmlns(), subscription);

			subscribedNodes.add(prepareSubscribeElement(parsedName, jid, packet.getStanzaTo().toString()));

			response.addChild(new Element("subscription", new String[] { "node", "jid", "subscription" },
					new String[] { parsedName.toEventBusNode(), jid.toString(), "subscribed" }));
		}

		if (log.isLoggable(Level.FINER))
			log.finer("Forwarding subscription request to: " + context.getConnectedNodes());

		for (JID node : context.getConnectedNodes()) {
			if (context.getComponentID().equals(node))
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
					new String[] { context.getComponentID().toString(), to, "set", id });

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

	@Override
	public void unregisterModule() {
		context.getEventBusInstance().removeHandler(LocalEventBus.HANDLER_ADDED_EVENT_NAME,
				LocalEventBus.EVENTBUS_INTERNAL_EVENTS_XMLNS, eventBusHandlerAddedHandler);
		super.unregisterModule();
	}

}
