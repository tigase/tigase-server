package tigase.disteventbus.component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
import tigase.disteventbus.EventHandler;
import tigase.disteventbus.component.stores.Subscription;
import tigase.disteventbus.impl.EventName;
import tigase.disteventbus.impl.LocalEventBus.LocalEventBusListener;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

public class UnsubscribeModule extends AbstractEventBusModule {

	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "unsubscribe" }, new String[] {
			null, "http://jabber.org/protocol/pubsub", null });

	public final static String ID = "unsubscribe";

	private final LocalEventBusListener eventBusListener = new LocalEventBusListener() {

		@Override
		public void onAddHandler(String name, String xmlns, EventHandler handler) {
		}

		@Override
		public void onFire(String name, String xmlns, Element event) {
		}

		@Override
		public void onRemoveHandler(String name, String xmlns, EventHandler handler) {
			UnsubscribeModule.this.onRemoveHandler(name, xmlns);
		}
	};

	@Override
	public void afterRegistration() {
		super.afterRegistration();

		context.getEventBusInstance().addListener(eventBusListener);

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
			for (String node : context.getConnectedNodes()) {
				Element se = prepareUnsubscribeElement(new EventName(eventName, eventXmlns), context.getComponentID(), null);
				sendUnsubscribeRequest("eventbus@" + node, Collections.singleton(se));
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
			final Set<Element> subscribedNodes = new HashSet<Element>();
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

			for (String node : context.getConnectedNodes()) {
				sendUnsubscribeRequest("eventbus@" + node, subscribedNodes);
			}
		}
		Packet response = packet.okResult((Element) null, 0);
		response.setPermissions(Permissions.ADMIN);
		write(response);
	}

	protected void sendUnsubscribeRequest(String to, Collection<Element> subscriptionElement) {
		try {
			Element iq = new Element("iq", new String[] { "from", "to", "type", "id" }, new String[] {
					context.getComponentID().toString(), to, "set", nextStanzaID() });

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
		context.getEventBusInstance().removeListener(eventBusListener);
		super.unregisterModule();
	}
}
