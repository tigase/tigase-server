package tigase.disteventbus.component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
import tigase.disteventbus.EventHandler;
import tigase.disteventbus.LocalEventBus.EventName;
import tigase.disteventbus.LocalEventBus.LocalEventBusListener;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

public class SubscribeModule extends AbstractEventBusModule {

	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "iq", "pubsub", "subscribe" }, new String[] {
			null, "http://jabber.org/protocol/pubsub", null });

	public final static String ID = "subscribe";

	private final LocalEventBusListener eventBusListener = new LocalEventBusListener() {

		@Override
		public void onAddHandler(String name, String xmlns, EventHandler handler) {
			SubscribeModule.this.onAddHandler(name, xmlns);
		}

		@Override
		public void onFire(String name, String xmlns, Element event) {
		}

		@Override
		public void onRemoveHandler(String name, String xmlns, EventHandler handler) {
		}
	};

	@Override
	public void afterRegistration() {
		super.afterRegistration();

		context.getEventBusInstance().addListener(eventBusListener);

	}

	public void clusterNodeConnected(String node) {
		// context.getSubscriptionStore().addSubscription(null,
		// "tigase:eventbus", JID.jidInstanceNS("eventbus", node, null));

		if (log.isLoggable(Level.FINER))
			log.finer("Node " + node + " is connected.");

		Set<String> pubsubNodes = new HashSet<String>();
		for (EventName eventName : context.getEventBusInstance().getAllListenedEvents()) {
			pubsubNodes.add(NodeNameUtil.createNodeName(eventName.getName(), eventName.getXmlns()));
		}

		for (EventName eventName : context.getNonClusterSubscriptionStore().getSubscribedEvents()) {
			pubsubNodes.add(NodeNameUtil.createNodeName(eventName.getName(), eventName.getXmlns()));
		}

		sendSubscribeRequest("eventbus@" + node, context.getComponentID().toString(), pubsubNodes.toArray(new String[] {}));
	}

	public void clusterNodeDisconnected(String node) {
		if (log.isLoggable(Level.FINER))
			log.finer("Node " + node + " is disconnected.");
		context.getSubscriptionStore().remove(JID.jidInstanceNS("eventbus", node, null));
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
		for (String node : context.getConnectedNodes()) {
			sendSubscribeRequest("eventbus@" + node, context.getComponentID().toString(),
					NodeNameUtil.createNodeName(eventName, eventXmlns));
		}
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getType() == StanzaType.set) {
			processSet(packet);
		} else
			throw new ComponentException(Authorization.NOT_ALLOWED);
	}

	private void processSet(final Packet packet) throws TigaseStringprepException {
		List<Element> subscribeElements = packet.getElemChildrenStaticStr(new String[] { "iq", "pubsub" });

		if (packet.getStanzaFrom().getLocalpart().equals("eventbus")
				&& context.getConnectedNodes().contains(packet.getStanzaFrom().getDomain())) {
			// subscription from cluster node
			for (Element subscribe : subscribeElements) {
				String[] parsedName = NodeNameUtil.parseNodeName(subscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

				if (log.isLoggable(Level.FINER))
					log.finer("Node " + jid + " subscribed for events " + parsedName[0] + ", " + parsedName[1]);

				context.getSubscriptionStore().addSubscription(parsedName[0], parsedName[1], jid);
			}
		} else {
			// subscription from something out of cluster

			final Set<String> subscribedNodes = new HashSet<String>();
			for (Element subscribe : subscribeElements) {
				String[] parsedName = NodeNameUtil.parseNodeName(subscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(subscribe.getAttributeStaticStr("jid"));

				if (log.isLoggable(Level.FINER))
					log.finer("Entity " + jid + " subscribed for events " + parsedName[0] + ", " + parsedName[1]);

				context.getNonClusterSubscriptionStore().addSubscription(parsedName[0], parsedName[1],
						new NonClusterSubscription(jid, packet.getStanzaTo()));
				subscribedNodes.add(NodeNameUtil.createNodeName(parsedName[0], parsedName[1]));
			}

			for (String node : context.getConnectedNodes()) {
				sendSubscribeRequest("eventbus@" + node, context.getComponentID().toString(),
						subscribedNodes.toArray(new String[] {}));
			}
		}
		Packet response = packet.okResult((Element) null, 0);
		response.setPermissions(Permissions.ADMIN);
		write(response);
	}

	protected void sendSubscribeRequest(String to, String subscriberJID, String... eventBusNodes) {
		try {
			Element iq = new Element("iq", new String[] { "from", "to", "type", "id" }, new String[] {
					context.getComponentID().toString(), to, "set", nextStanzaID() });

			Element pubsubElem = new Element("pubsub", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub" });
			iq.addChild(pubsubElem);

			for (String node : eventBusNodes) {
				Element subscribeElem = new Element("subscribe");
				subscribeElem.addAttribute("node", node);
				subscribeElem.addAttribute("jid", context.getComponentID().toString());
				pubsubElem.addChild(subscribeElem);
			}

			final Packet packet = Packet.packetInstance(iq);
			packet.setPermissions(Permissions.ADMIN);
			packet.setXMLNS(Packet.CLIENT_XMLNS);

			if (log.isLoggable(Level.FINER))
				log.finer("Sending subscription for " + subscriberJID + ", events: " + Arrays.toString(eventBusNodes) + " to "
						+ to);

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
