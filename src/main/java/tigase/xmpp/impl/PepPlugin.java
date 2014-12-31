/*
 * PepPlugin.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
 *
 */



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Class description
 *
 *
 * @version 5.0.0, 2010.03.01 at 03:12:30 GMT
 * @author Artur Hefczyc
 */
public class PepPlugin
				extends XMPPProcessorAbstract {
	private static final String    _XMLNS = "http://jabber.org/protocol/pubsub";
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { _XMLNS }),
			new Element("feature", new String[] { "var" }, new String[] { _XMLNS + "#owner" }),
			new Element("feature", new String[] { "var" }, new String[] { _XMLNS +
					"#publish" }),
			new Element("identity", new String[] { "category", "type" }, new String[] {
					"pubsub",
					"pep" }), };
	private static final String[][]                ELEMENTS             = {
		Iq.IQ_PUBSUB_PATH
	};
	private static final String                    ID                   = "pep-simple";
	private static final String[]                  IQ_PUBSUB_PATH       = { "iq",
			"pubsub" };
	private static final Logger                    log = Logger.getLogger(
			"tigase.xmpp.impl.PepPlugin");
	private static final String                    PUBSUB_COMPONENT_URL =
			"pubsub-component";
	private static RosterAbstract                  roster = RosterFactory
			.getRosterImplementation(true);
	private static final EnumSet<SubscriptionType> SUBSCRITION_TYPES = EnumSet.of(
			SubscriptionType.both, SubscriptionType.from);
	private static final String[] XMLNSS = { _XMLNS };

	//~--- fields ---------------------------------------------------------------

	private final HashSet<String> supportedNodes = new HashSet<String>();

	//~--- constructors ---------------------------------------------------------

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

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException {
		try {
			List<Element> x         = packet.getElemChildrenStaticStr(IQ_PUBSUB_PATH);
			boolean       processed = false;

			for (Element element : x) {
				String action = element.getName();
				String node   = element.getAttributeStaticStr("node");

				if (this.supportedNodes.contains(node)) {
					if (action == "retract") {
						Element item    = element.getChild("item", null);
						Element retract = new Element("retract");

						if (item.getAttributeStaticStr(Packet.ID_ATT) != null) {
							retract.addAttribute(Packet.ID_ATT, item.getAttributeStaticStr(Packet
									.ID_ATT));
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
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (TigaseDBException ex) {
			Logger.getLogger(PepPlugin.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {
		results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
				"Service not available.", true));
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {

		// I guess we ignore such packets here, no pep support for the server
		// itself yet
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	private void forward(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws XMPPException {
		String pubSubComponentUrl = (settings == null)
				? null
				: (String) settings.get(PUBSUB_COMPONENT_URL);

		if ((session == null) || (pubSubComponentUrl == null)) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Packet reject. No Session or PubSub Component URL.");
			}

			return;
		}    // end of if (session == null)
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
			}    // end of else
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		}    // end of try-catch
	}

	private void processPEPPublish(Packet packet, String node, Element pepItem,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws NotAuthorizedException, TigaseDBException {
		JID[]   buddies = roster.getBuddies(session, SUBSCRITION_TYPES);
		Element event = new Element("event", new String[] { "xmlns" }, new String[] {
				"http://jabber.org/protocol/pubsub#event" });
		Element items = new Element("items", new String[] { "node" }, new String[] { node });

		event.addChild(items);
		items.addChild(pepItem);

		JID from = packet.getStanzaFrom();

		if (buddies != null) {
			for (JID buddy : buddies) {
				Element message = new Element("message", new String[] { "from", "to", "type",
						"id" }, new String[] { from.toString(), buddy.toString(), "headline",
						packet.getStanzaId() });

				message.addChild(event);
				results.offer(Packet.packetInstance(message, from, buddy));
			}
		}

		Element message = new Element("message", new String[] { "from", "to", "type", "id" },
				new String[] { from.toString(),
				from.toString(), "headline", packet.getStanzaId() });

		message.addChild(event);
		results.offer(Packet.packetInstance(message, from, from));
	}
}


//~ Formatted in Tigase Code Convention on 13/03/12
