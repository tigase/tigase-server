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
import tigase.disteventbus.component.stores.SubscriptionStore;
import tigase.disteventbus.impl.EventName;
import tigase.disteventbus.impl.LocalEventBus;
import tigase.disteventbus.impl.LocalEventBus.LocalEventBusListener;
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

	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "unsubscribe" },
			new String[] { null, "http://jabber.org/protocol/pubsub", null });

	public final static String ID = "unsubscribe";

	@Inject
	private EventBusComponent component;

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

	@Inject(nullAllowed = false, bean = "localEventBus")
	private LocalEventBus localEventBus;

	@Inject
	private SubscriptionStore subscriptionStore;

	@Override
	public void beforeUnregister() {
		localEventBus.removeListener(eventBusListener);

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
		localEventBus.addListener(eventBusListener);
	}

	protected void onRemoveHandler(String eventName, String eventXmlns) {
		boolean listenedByHandlers = localEventBus.hasHandlers(eventName, eventXmlns);

		if (!listenedByHandlers) {
			for (JID node : component.getNodesConnected()) {
				Element se = prepareUnsubscribeElement(new EventName(eventName, eventXmlns), component.getComponentId(), null);
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

				subscriptionStore.removeSubscription(parsedName.getName(), parsedName.getXmlns(), new Subscription(jid));
			}
		} else {
			// request from something out of cluster
			final Set<Element> subscribedNodes = new HashSet<Element>();
			for (Element subscribe : unsubscribeElements) {
				EventName parsedName = NodeNameUtil.parseNodeName(subscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

				if (log.isLoggable(Level.FINER))
					log.finer("Entity " + jid + " subscribed for events " + parsedName);

				subscriptionStore.removeSubscription(parsedName.getName(), parsedName.getXmlns(),
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
