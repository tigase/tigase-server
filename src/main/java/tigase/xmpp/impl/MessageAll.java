/*
 * MessageAll.java
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

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
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
 * Variant of the <code>Message</code> forwarder class. This implementation forwards
 * messages to all connected resources. <br>
 * When a message <strong>'from'</strong> the user is being processed, the plugin forwards
 * the message to the destination address and also sends the message to all other connected
 * resources.<br>
 * When a message <strong>'to'</strong> the user is being processed, the plugin forwards
 * the message to all connected resources.<br>
 * The idea behind this implementation is to keep all connected resources synchronized with
 * a complete chat content. User should be able to switch between connections and continue
 * the chat.
 *
 * Created: Feb 14, 2010 4:35:45 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageAll
				extends XMPPProcessor
				implements XMPPProcessorIfc {
	private static final String[][] ELEMENTS = {
		{ "message" }
	};
	private static final String     ID       = "message-all";

	/** Class logger */
	private static final Logger   log    = Logger.getLogger(MessageAll.class.getName());
	private static final String   XMLNS  = "jabber:client";
	private static final String[] XMLNSS = { XMLNS };

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
			log.finest("Processing packet: " + packet);
		}

		// You may want to skip processing completely if the user is offline.
		if (session == null) {
			return;
		}    // end of if (session == null)
		try {

			// Remember to cut the resource part off before comparing JIDs
			BareJID id = (packet.getStanzaTo() != null)
					? packet.getStanzaTo().getBareJID()
					: null;

			// Checking if this is a packet TO the owner of the session
			if (session.isUserId(id)) {

				// Yes this is message to 'this' client
				// Send the message to all connected resources:
				for (XMPPResourceConnection conn : session.getActiveSessions()) {
					Packet result = packet.copyElementOnly();

					// This is where and how we set the address of the component
					// which should receive the packet for the final delivery
					// to the end-user. In most cases this is a c2s or Bosh component
					// which keep the user connection.
					result.setPacketTo(conn.getConnectionId());

					// In most cases this might be skept, however if there is a
					// problem during packet delivery an error might be sent back
					result.setPacketFrom(packet.getTo());

					// Don't forget to add the packet to the results queue or it
					// will be lost.
					results.offer(result);
				}

				return;
			}    // end of else

			// Remember to cut the resource part off before comparing JIDs
			id = (packet.getStanzaFrom() != null)
					? packet.getStanzaFrom().getBareJID()
					: null;

			// Checking if this is maybe packet FROM the client
			if (session.isUserId(id)) {

				// First we have to update all the other resources with this message just send
				// from the user.
				for (XMPPResourceConnection conn : session.getActiveSessions()) {

					// Don't send the message back to the connection from which is has been
					// received, only to all other connections.
					if (conn != session) {
						Packet result = packet.copyElementOnly();

						// This is where and how we set the address of the component
						// which should receive the packet for the final delivery
						// to the end-user. In most cases this is a c2s or Bosh component
						// which keep the user connection.
						result.setPacketTo(conn.getConnectionId());

						// Don't forget to add the packet to the results queue or it
						// will be lost.
						results.offer(result);
					}
				}

				// This is a packet FROM this client, the simplest action is
				// to forward it to is't destination:
				// Simple clone the XML element and....
				// ... putting it to results queue is enough
				results.offer(packet.copyElementOnly());

				return;
			}

			// Can we really reach this place here?
			// If the point is reached then the message packet does not have either from or
			// to address. In such a case we ignore it.
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
}
