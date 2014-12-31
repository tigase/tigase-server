/*
 * MessageForwarding.java
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

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Message forwarder class. Forwards <code>Message</code> packet to it's
 * destination address.
 *
 * Created: Tue Feb 21 15:49:08 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev: 2348 $
 */
public class MessageForwarding
				extends XMPPProcessor
				implements XMPPProcessorIfc {
	private static final String FORWARD_EL    = "forward";
	private static final String FORWARD_XMLNS = "http://tigase.org/protocol/forward#v1";
	private static final String ID            = "message-vhost-forward";

	/** Class logger */
	private static final Logger     log = Logger.getLogger(MessageForwarding.class
			.getName());
	private static final String     XMLNS    = "jabber:client";
	private static final String[][] ELEMENTS = {
		{ tigase.server.Message.ELEM_NAME }
	};
	private static final String[]   XMLNSS   = { XMLNS };
	private static final String[]   MESSAGE_FORWARD_PATH = { tigase.server.Message
			.ELEM_NAME,
			FORWARD_EL };
	private static final Element forw_el = new Element(FORWARD_EL, new String[] {
			"xmlns" }, new String[] { FORWARD_XMLNS });

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws XMPPException {

		// For performance reasons it is better to do the check
		// before calling logging method.
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}

		// If the session is null (user offline), then we cannot forward as we
		// do not have access to user's configuration. The message should be
		// processed
		// once again when the user comes back online and his messages are read
		// from offline storage.
		if (session == null) {
			return;
		}    // end of if (session == null)

		// We only want to forward messages which contain body element
		if (packet.getElemCDataStaticStr(tigase.server.Message.MESSAGE_BODY_PATH) == null) {
			return;
		}

		// If the message contains forward payload already we drop it to prevent
		// infinite loop due to some misconfiguration
		if (packet.isXMLNSStaticStr(MESSAGE_FORWARD_PATH, FORWARD_XMLNS)) {
			return;
		}

		JID forwardAddr = session.getDomain().getMessageForward();

		// No forward address means no forwarding which means no further processing
		// is required.
		if (forwardAddr == null) {
			return;
		}
		try {
			JID user = null;

			// Remember to cut the resource part off before comparing JIDs
			BareJID id = (packet.getStanzaTo() != null)
					? packet.getStanzaTo().getBareJID()
					: null;

			// Checking if this is a packet TO the owner of the session
			if (session.isUserId(id)) {

				// Yes this is message to 'this' client
				user = packet.getStanzaTo();
			} else {

				// Remember to cut the resource part off before comparing JIDs
				id = (packet.getStanzaFrom() != null)
						? packet.getStanzaFrom().getBareJID()
						: null;

				// Checking if this is maybe packet FROM the client
				if (session.isUserId(id)) {
					user = packet.getStanzaFrom();
				}
			}

			// Can we really reach this place here?
			// Yes, some packets don't even have from or to address.
			// The best example is IQ packet which is usually a request to
			// the server for some data. Such packets may not have any addresses
			// And they usually require more complex processing
			// This is how you check whether this is a packet FROM the user
			// who is owner of the session:
			if (user == null) {
				user = session.getJID();
			}

			Element forward_payload = forw_el.clone();

			forward_payload.addAttribute("from", packet.getStanzaFrom().toString());
			forward_payload.addAttribute("to", packet.getStanzaTo().toString());

			Packet result = Packet.packetInstance(packet.getElement(), user, forwardAddr);

			result.getElement().addChild(forward_payload);
			results.offer(result);
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		}    // end of try-catch
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}    // Message

