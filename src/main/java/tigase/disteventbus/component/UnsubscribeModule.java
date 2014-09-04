package tigase.disteventbus.component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.component.responses.AsyncCallback;
import tigase.criteria.Criteria;
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

	@Override
	public String[] getFeatures() {
		return new String[] { "http://jabber.org/protocol/pubsub#subscribe" };
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
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

		if (packet.getStanzaFrom().getLocalpart().equals("eventbus")
				&& context.getConnectedNodes().contains(packet.getStanzaFrom().getDomain())) {
			// request from cluster node
			for (Element unsubscribe : unsubscribeElements) {
				String[] parsedName = NodeNameUtil.parseNodeName(unsubscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(unsubscribe.getAttributeStaticStr("jid"));

				context.getSubscriptionStore().removeSubscription(parsedName[0], parsedName[1], jid);
			}
		} else {
			// request from something out of cluster
			final Set<String> unsubscribedNodes = new HashSet<String>();
			for (Element unsubscribe : unsubscribeElements) {
				String[] parsedName = NodeNameUtil.parseNodeName(unsubscribe.getAttributeStaticStr("node"));
				JID jid = JID.jidInstance(unsubscribe.getAttributeStaticStr("jid"));

				context.getNonClusterSubscriptionStore().removeSubscription(parsedName[0], parsedName[1],
						new NonClusterSubscription(jid, packet.getStanzaTo()));
				unsubscribedNodes.add(NodeNameUtil.createNodeName(parsedName[0], parsedName[1]));
			}

			for (String node : context.getConnectedNodes()) {
				String[] parsedName = NodeNameUtil.parseNodeName(node);

				boolean subscribedByNonClustered = context.getNonClusterSubscriptionStore().hasSubscriber(parsedName[0],
						parsedName[1]);

				// if (!subscribedByNonClustered) {
				// sendUnsubscribeRequest("eventbus@" + node,
				// context.getComponentID().toString(),
				// unsubscribedNodes.toArray(new String[] {}));
				// }
			}
		}
		Packet response = packet.okResult((Element) null, 0);
		response.setPermissions(Permissions.ADMIN);
		write(response);
	}

	protected void sendUnsubscribeRequest(String to, String subscriberJID, String... eventBusNodes) {
		try {
			Element iq = new Element("iq", new String[] { "from", "to", "type", "id" }, new String[] {
					context.getComponentID().toString(), to, "set", nextStanzaID() });

			Element pubsubElem = new Element("pubsub", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub" });
			iq.addChild(pubsubElem);

			for (String node : eventBusNodes) {
				Element subscribeElem = new Element("unsubscribe");
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
}
