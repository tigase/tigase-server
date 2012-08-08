package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.BasicComponent;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.RosterFactory;

//~--- JDK imports ------------------------------------------------------------

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Class description
 * 
 * 
 * @version 5.0.0, 2010.03.01 at 03:12:30 GMT
 * @author Artur Hefczyc <artur.hefczyc@tigase.org>
 */
public class PepPlugin extends XMPPProcessorAbstract {
	private static final String _XMLNS = "http://jabber.org/protocol/pubsub";
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" }, new String[] { _XMLNS }),
			new Element("feature", new String[] { "var" }, new String[] { _XMLNS + "#owner" }),
			new Element("feature", new String[] { "var" }, new String[] { _XMLNS + "#publish" }),
			new Element("identity", new String[] { "category", "type" }, new String[] { "pubsub", "pep" }), };
	private static final String[] ELEMENTS = { "pubsub" };
	private static final String ID = "pep";
	private static final Logger log = Logger.getLogger("tigase.xmpp.impl.PepPlugin");
	private static final String PUBSUB_COMPONENT_URL = "pubsub-component";
	private static RosterAbstract roster = RosterFactory.getRosterImplementation(true);
	private static final EnumSet<SubscriptionType> SUBSCRITION_TYPES = EnumSet.of(SubscriptionType.both, SubscriptionType.from);
	private static final String[] XMLNSS = { _XMLNS };

	// ~--- fields
	// ---------------------------------------------------------------

	private final HashSet<String> supportedNodes = new HashSet<String>();

	// ~--- constructors
	// ---------------------------------------------------------

	/**
	 * Constructs ...
	 * 
	 */
	public PepPlugin() {
		this.supportedNodes.add("http://jabber.org/protocol/tune");
		this.supportedNodes.add("http://jabber.org/protocol/mood");
		this.supportedNodes.add("http://jabber.org/protocol/activity");
		this.supportedNodes.add("http://jabber.org/protocol/geoloc");
		this.supportedNodes.add("urn:xmpp:avatar:data");
		this.supportedNodes.add("urn:xmpp:avatar:metadata");

	}

	// ~--- methods
	// --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param connectionId
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 * 
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		try {
			List<Element> x = packet.getElemChildren("/iq/pubsub");
			boolean processed = false;

			for (Element element : x) {
				String action = element.getName();
				String node = element.getAttribute("node");

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
					} else {
						if (action == "publish") {
							Element item = element.getChild("item", null);

							processed = true;
							processPEPPublish(packet, node, item, session, repo, results, settings);

							break;
						}
					}
				}
			}

			if (processed) {
				results.offer(packet.okResult((Element) null, 0));
			} else {
				results.offer(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet,
						"The PEP node is not yet supported.", true));
			}
		} catch (NotAuthorizedException ex) {
			log.warning("NotAuthorizedException for packet: " + packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.", true));
		} catch (TigaseDBException ex) {
			Logger.getLogger(PepPlugin.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param packet
	 * @param repo
	 * @param results
	 * @param settings
	 * 
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws PacketErrorTypeException {
		results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, "Service not available.", true));
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 * 
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {

		// I guess we ignore such packets here, no pep support for the server
		// itself yet
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param session
	 * 
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	private void forward(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws XMPPException {
		String pubSubComponentUrl = (settings == null) ? null : (String) settings.get(PUBSUB_COMPONENT_URL);

		if ((session == null) || (pubSubComponentUrl == null)) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Packet reject. No Session or PubSub Component URL.");
			}

			return;
		} // end of if (session == null)

		try {
			packet.getElement().setAttribute("to", pubSubComponentUrl);

			BareJID id = packet.getStanzaTo().getBareJID();

			if (session.isUserId(id)) {

				// Yes this is message to 'this' client
				Packet result = packet.copyElementOnly();

				result.setPacketTo(session.getConnectionId());
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {

				// This is message to some other client
				results.offer(packet.copyElementOnly());
			} // end of else
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.", true));
		} // end of try-catch
	}

	private void processPEPPublish(Packet packet, String node, Element pepItem, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws NotAuthorizedException,
			TigaseDBException {
		JID[] buddies = roster.getBuddies(session, SUBSCRITION_TYPES);
		Element event = new Element("event", new String[] { "xmlns" },
				new String[] { "http://jabber.org/protocol/pubsub#event" });
		Element items = new Element("items", new String[] { "node" }, new String[] { node });

		event.addChild(items);
		items.addChild(pepItem);

		JID from = packet.getStanzaFrom();

		if (buddies != null)
			for (JID buddy : buddies) {
				Element message = new Element("message", new String[] { "from", "to", "type", "id" }, new String[] {
						from.toString(), buddy.toString(), "headline", packet.getStanzaId() });

				message.addChild(event);
				results.offer(Packet.packetInstance(message, from, buddy));
			}

		Element message = new Element("message", new String[] { "from", "to", "type", "id" }, new String[] { from.toString(),
				from.toString(), "headline", packet.getStanzaId() });

		message.addChild(event);
		results.offer(Packet.packetInstance(message, from, from));
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
