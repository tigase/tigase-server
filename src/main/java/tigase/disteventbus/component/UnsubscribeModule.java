package tigase.disteventbus.component;

import java.util.*;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
import tigase.disteventbus.EventHandler;
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

public class UnsubscribeModule extends AbstractEventBusModule {

	public final static String ID = "unsubscribe";
	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "unsubscribe" },
			new String[] { null, "http://jabber.org/protocol/pubsub", null });
	private final EventHandler eventBusHandlerRemovedHandler = new EventHandler() {

		private final String[] NAME_PATH = new String[] { LocalEventBus.HANDLER_REMOVED_EVENT_NAME, "name" };
		private final String[] XMLNS_PATH = new String[] { LocalEventBus.HANDLER_REMOVED_EVENT_NAME, "xmlns" };

		@Override
		public void onEvent(String name, String xmlns, Element event) {
			String n = event.getCData(NAME_PATH);
			String x = event.getCData(XMLNS_PATH);
			if (x == null || !x.equals(LocalEventBus.EVENTBUS_INTERNAL_EVENTS_XMLNS))
				UnsubscribeModule.this.onRemoveHandler(n, x);
		}
	};

	@Override
	public void afterRegistration() {
		super.afterRegistration();

		context.getEventBusInstance().addHandler(LocalEventBus.HANDLER_REMOVED_EVENT_NAME,
				LocalEventBus.EVENTBUS_INTERNAL_EVENTS_XMLNS, eventBusHandlerRemovedHandler);

	}

	@Override
	public String[] getFeatures() {
		return new String[] { "http://jabber.org/protocol/pubsub#subscribe" };
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	protected void onRemoveHandler(String eventName, String eventXmlns) {
		boolean listenedByHandlers = context.getEventBusInstance().hasHandlers(eventName, eventXmlns);

		if (!listenedByHandlers) {
			for (JID node : context.getConnectedNodes()) {
				Element se = prepareUnsubscribeElement(new EventName(eventName, eventXmlns), context.getComponentID(), null);
				sendUnsubscribeRequest("eventbus@" + node.getDomain(), Collections.singleton(se));
			}
		}
	}

	private Element prepareUnsubscribeElement(EventName event, JID jid, String service) {
		Element subscribeElem = new Element("unsubscribe");
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
			throw new ComponentException(Authorization.NOT_ALLOWED);
	}

	private void processSet(final Packet packet) throws TigaseStringprepException {
		List<Element> unsubscribeElements = packet.getElemChildrenStaticStr(new String[] { "iq", "pubsub" });

		if (isClusteredEventBus(packet.getStanzaFrom())) {
			// request from cluster node
			for (Element unsubscribe : unsubscribeElements) {
				EventName parsedName = NodeNameUtil.parseNodeName(unsubscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(unsubscribe.getAttributeStaticStr("jid"));

				context.getSubscriptionStore().removeSubscription(parsedName.getName(), parsedName.getXmlns(),
						new Subscription(jid));
			}
		} else {
			// request from something out of cluster
			final Set<Element> subscribedNodes = new HashSet<>();
			for (Element subscribe : unsubscribeElements) {
				EventName parsedName = NodeNameUtil.parseNodeName(subscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

				if (log.isLoggable(Level.FINER))
					log.finer("Entity " + jid + " subscribed for events " + parsedName);

				context.getSubscriptionStore().removeSubscription(parsedName.getName(), parsedName.getXmlns(),
						new Subscription(jid, packet.getStanzaTo()));

				subscribedNodes.add(prepareUnsubscribeElement(parsedName, jid, packet.getStanzaTo().toString()));
			}

			if (log.isLoggable(Level.FINER))
				log.finer("Forwarding unsubcribe to: " + context.getConnectedNodes());

			for (JID node : context.getConnectedNodes()) {
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
					new String[] { context.getComponentID().toString(), to, "set", nextStanzaID() });

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

	@Override
	public void unregisterModule() {
		context.getEventBusInstance().removeHandler(LocalEventBus.HANDLER_REMOVED_EVENT_NAME,
				LocalEventBus.EVENTBUS_INTERNAL_EVENTS_XMLNS, eventBusHandlerRemovedHandler);
		super.unregisterModule();
	}
}
