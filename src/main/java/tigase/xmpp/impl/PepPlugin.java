package tigase.xmpp.impl;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.conf.Configurable;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

public class PepPlugin extends XMPPProcessor implements XMPPProcessorIfc {

	private static final String _XMLNS = "http://jabber.org/protocol/pubsub";

	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" }, new String[] { _XMLNS }),
			new Element("feature", new String[] { "var" }, new String[] { _XMLNS + "#owner" }),
			new Element("feature", new String[] { "var" }, new String[] { _XMLNS + "#publish" }),
			new Element("identity", new String[] { "category", "type" }, new String[] { "pubsub", "pep" }),

	};

	private static final String[] ELEMENTS = { "pubsub" };

	private static final String ID = "pep";

	private static final Logger log = Logger.getLogger("tigase.xmpp.impl.PepPlugin");

	private static final String PUBSUB_COMPONENT_URL = "pubsub-component";

	private static RosterAbstract roster = RosterFactory.getRosterImplementation(true);

	private static final EnumSet<SubscriptionType> SUBSCRITION_TYPES = EnumSet.of(SubscriptionType.both, SubscriptionType.from);

	private static final String[] XMLNSS = { _XMLNS };

	private final HashSet<String> supportedNodes = new HashSet<String>();

	public PepPlugin() {
		this.supportedNodes.add("http://jabber.org/protocol/tune");
		this.supportedNodes.add("http://jabber.org/protocol/mood");
		this.supportedNodes.add("http://jabber.org/protocol/activity");
		this.supportedNodes.add("http://jabber.org/protocol/geoloc");
	}

	private void forward(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
			final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {

		final String pubSubComponentUrl = settings == null ? null : (String) settings.get(PUBSUB_COMPONENT_URL);

		if (session == null || pubSubComponentUrl == null) {
			if (log.isLoggable(Level.FINE))
				log.fine("Packet reject. No Session or PubSub Component URL.");
			return;
		} // end of if (session == null)

		try {

			packet.getElement().setAttribute("to", pubSubComponentUrl);

			String id = JIDUtils.getNodeID(packet.getElemTo());

			if (id.equals(session.getUserId())) {
				// Yes this is message to 'this' client
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId());
				result.setFrom(packet.getTo());
				results.offer(result);
			} else {
				// This is message to some other client
				Element result = packet.getElement().clone();
				results.offer(new Packet(result));
			} // end of else
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.", true));
		} // end of try-catch

	}

	public String id() {
		return ID;
	}

	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
			final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {

		if (session == null) {
			try {
				results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, "Service not available.", true));
			} catch (PacketErrorTypeException e) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("This is already error packet, ignoring... " + packet.toString());
				}
			}
			return;
		}

		try {

			String id = session.getDomain();
			if (packet.getElemTo() != null) {
				id = JIDUtils.getNodeID(packet.getElemTo());
			}
			if (id == null || id.equals("") || id.equalsIgnoreCase(session.getDomain())
					|| session.getConnectionId() == Configurable.NULL_ROUTING) {

				List<Element> x = packet.getElemChildren("/iq/pubsub");
				boolean processed = false;
				for (Element element : x) {
					final String action = element.getName();
					final String node = element.getAttribute("node");
					if (this.supportedNodes.contains(node)) {
						if (action == "retract") {
							Element item = element.getChild("item", null);
							Element retract = new Element("retract");
							if (item.getAttribute("id") != null) {
								retract.addAttribute("id", item.getAttribute("id"));
							}
							processed = true;
							processPEPPublish(packet, node, retract, session, repo, results, settings);
							break;
						} else if (action == "publish") {
							Element item = element.getChild("item", null);
							processed = true;
							processPEPPublish(packet, node, item, session, repo, results, settings);
							break;
						}
					}
				}

				if (!processed) {
					forward(packet, session, repo, results, settings);
				} else {
					results.offer(packet.okResult((Element) null, 0));
				}

				return;
			}
			if (packet.getElemTo() == null) {
				forward(packet, session, repo, results, settings);
				return;
			}
			if (id.equals(session.getUserId())) {
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId(packet.getElemTo()));
				result.setFrom(packet.getTo());
				results.offer(result);
			} else {
				Element result = packet.getElement().clone();
				results.offer(new Packet(result));
			}
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet.getStringData());
		} catch (TigaseDBException e) {
			log.warning("TigaseDBException for packet: " + packet.getStringData());
		}
	}

	private void processPEPPublish(final Packet packet, final String node, final Element pepItem,
			final XMPPResourceConnection session, final NonAuthUserRepository repo, final Queue<Packet> results,
			final Map<String, Object> settings) throws NotAuthorizedException, TigaseDBException {
		String[] buddies = roster.getBuddies(session, SUBSCRITION_TYPES);

		final Element event = new Element("event", new String[] { "xmlns" },
				new String[] { "http://jabber.org/protocol/pubsub#event" });
		final Element items = new Element("items", new String[] { "node" }, new String[] { node });
		event.addChild(items);
		items.addChild(pepItem);

		final String from = JIDUtils.getNodeID(packet.getElemFrom());

		for (String buddy : buddies) {
			final Element message = new Element("message", new String[] { "from", "to", "type", "id" }, new String[] { from,
					buddy, "headline", packet.getElemId() });
			message.addChild(event);

			results.offer(new Packet(message));
		}
		final Element message = new Element("message", new String[] { "from", "to", "type", "id" }, new String[] { from,
				packet.getElemFrom(), "headline", packet.getElemId() });
		message.addChild(event);

		results.offer(new Packet(message));
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

}
