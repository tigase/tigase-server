package tigase.eventbus.component;

import java.util.*;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
import tigase.eventbus.EventBusImplementation;
import tigase.eventbus.EventName;
import tigase.eventbus.component.stores.Subscription;
import tigase.eventbus.component.stores.SubscriptionStore;
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

@Bean(name = UnsubscribeModule.ID)
public class UnsubscribeModule extends AbstractEventBusModule implements Initializable, UnregisterAware {

	public final static String ID = "unsubscribe";
	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "unsubscribe" },
			new String[] { null, "http://jabber.org/protocol/pubsub", null });
	@Inject
	private EventBusComponent component;
	@Inject(nullAllowed = false, bean = "localEventBus")
	private EventBusImplementation localEventBus;

	private final tigase.eventbus.EventListener<EventBusImplementation.ListenerRemovedEvent> eventBusHandlerRemovedHandler = new tigase.eventbus.EventListener<EventBusImplementation.ListenerRemovedEvent>() {
		@Override
		public void onEvent(final EventBusImplementation.ListenerRemovedEvent event) {
			if (!event.getPackageName().startsWith("tigase.eventbus"))
				UnsubscribeModule.this.onRemoveHandler(event.getPackageName(), event.getEventName());
		}
	};

	@Inject
	private SubscriptionStore subscriptionStore;

	@Override
	public void beforeUnregister() {
		localEventBus.removeListener(eventBusHandlerRemovedHandler);
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
		localEventBus.addListener(EventBusImplementation.ListenerRemovedEvent.class, eventBusHandlerRemovedHandler);
	}

	protected void onRemoveHandler(String eventPackage, String eventName) {
		boolean listenedByHandlers = localEventBus.isListened(eventPackage, eventName);

		if (!listenedByHandlers) {
			for (JID node : component.getNodesConnected()) {
				Element se = prepareUnsubscribeElement(new EventName(eventPackage, eventName), component.getComponentId(),
						null);
				sendUnsubscribeRequest("eventbus@" + node.getDomain(), Collections.singleton(se));
			}
		}
	}

	private Element prepareUnsubscribeElement(EventName event, JID jid, String service) {
		Element subscribeElem = new Element("unsubscribe");
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
			throw new ComponentException(Authorization.NOT_ALLOWED);
	}

	private void processSet(final Packet packet) throws TigaseStringprepException {
		List<Element> unsubscribeElements = packet.getElemChildrenStaticStr(new String[] { "iq", "pubsub" });

		if (isClusteredEventBus(packet.getStanzaFrom())) {
			// request from cluster node
			for (Element unsubscribe : unsubscribeElements) {
				EventName parsedName = new EventName(unsubscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(unsubscribe.getAttributeStaticStr("jid"));

				subscriptionStore.removeSubscription(parsedName.getPackage(), parsedName.getName(), new Subscription(jid));
			}
		} else {
			// request from something out of cluster
			final Set<Element> subscribedNodes = new HashSet<>();
			for (Element subscribe : unsubscribeElements) {
				EventName parsedName = new EventName(subscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

				if (log.isLoggable(Level.FINER))
					log.finer("Entity " + jid + " subscribed for events " + parsedName);

				subscriptionStore.removeSubscription(parsedName.getPackage(), parsedName.getName(),
						new Subscription(jid, packet.getStanzaTo()));

				subscribedNodes.add(prepareUnsubscribeElement(parsedName, jid, packet.getStanzaTo().toString()));
			}

			if (log.isLoggable(Level.FINER))
				log.finer("Forwarding unsubcribe to: " + component.getNodesConnected());

			for (JID node : component.getNodesConnected()) {
				sendUnsubscribeRequest("eventbus@" + node.getDomain(), subscribedNodes);
			}
		}
		Packet response = packet.okResult((Element) null, 0);
		response.setPermissions(Permissions.ADMIN);
		write(response);
	}

	protected void sendUnsubscribeRequest(String to, Collection<Element> subscriptionElement) {
		try {
			Element iq = new Element("iq", new String[] { "from", "to", "type", "id" },
					new String[] { component.getComponentId().toString(), to, "set", nextStanzaID() });

			Element pubsubElem = new Element("pubsub", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub" });
			iq.addChild(pubsubElem);

			for (Element node : subscriptionElement) {
				pubsubElem.addChild(node);
			}

			final Packet packet = Packet.packetInstance(iq);
			packet.setPermissions(Permissions.ADMIN);
			packet.setXMLNS(Packet.CLIENT_XMLNS);

			write(packet, new AsyncCallback() {

				@Override
				public void onError(Packet responseStanza, String errorCondition) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onSuccess(Packet responseStanza) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onTimeout() {
					// TODO Auto-generated method stub

				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
