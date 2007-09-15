/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.xmppsession;

import java.util.Queue;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.StanzaType;
import tigase.util.JIDUtils;

/**
 * Describe class PacketFilter here.
 *
 *
 * Created: Fri Feb  2 15:08:58 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PacketFilter {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
		Logger.getLogger("tigase.server.xmppsession.PacketFilter");

	/**
	 * Creates a new <code>PacketFilter</code> instance.
	 *
	 */
	public PacketFilter() {	}

	public boolean forward(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {
		if (session == null) {
			return false;
		} // end of if (session == null)

		try {
			// For all messages coming from the owner of this account set
			// proper 'from' attribute. This is actually needed for the case
			// when the user sends a message to himself.
			if (packet.getFrom() != null
				&& packet.getFrom().equals(session.getConnectionId())) {
				packet.getElement().setAttribute("from", session.getJID());
				log.finest("Setting correct from attribute: " + session.getJID());
			}

			// Apparently all code below breaks all cases when packet addressed to
			// the user should be processed in server on user behalf so it is commented
			// for now. The correct solution is to handle all unprocessed packets in
			// "process" method.
			// So here we just set proper from address and that's it.
			// I think we should modify all plugins code and remove setting proper
			// from address as this has been already done here so no need for
			// duplicated code to maintaind and process.

// 			// This could be earlier at the beginnig of the method, but I want to have
// 			// from address set properly whenever possible
// 			if (packet.getElemTo() == null || session.isDummy()
// 				|| packet.getElemName().equals("presence")) {
// 				return false;
// 			}

// 			String id = JIDUtils.getNodeID(packet.getElemTo());

// 			if (id.equals(session.getUserId())
// 				&& packet.getFrom() != null
// 				&& packet.getFrom().equals(packet.getElemFrom())) {
// 				// Yes this is message to 'this' client
// 				log.finest("Yes, this is packet to 'this' client: " + id);
// 				Element elem = packet.getElement().clone();
// 				Packet result = new Packet(elem);
// 				result.setTo(session.getParentSession().
// 					getResourceConnection(packet.getElemTo()).getConnectionId());
// 				log.finest("Setting to: " + result.getTo());
// 				result.setFrom(packet.getTo());
// 				results.offer(result);
// 			} else {
// // 				// This is message to some other client
// // 				Element result = packet.getElement().clone();
// // 				results.offer(new Packet(result));
// 				return false;
// 			} // end of else
		} catch (NotAuthorizedException e) {
			return false;
		} // end of try-catch

		return false;
	}

	public boolean process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return false;
		} // end of if (session == null)

		log.finest("Processing packet: " + packet.toString());

		try {
			// Can not forward packet if there is no destination address
			if (packet.getElemTo() == null) {
				// If this is simple <iq type="result"/> then ignore it
				// and consider it OK
				if (packet.getElemName().equals("iq")
					&& packet.getType() != null
					&& packet.getType() == StanzaType.result) {
					// Nothing to do....
					return true;
				}
				log.warning("No 'to' address, can't deliver packet: "
					+ packet.getStringData());
				return false;
			}

			// Already done in forward method....
			// No need to repeat this (performance - everything counts...)
// 			// For all messages coming from the owner of this account set
// 			// proper 'from' attribute. This is actually needed for the case
// 			// when the user sends a message to himself.
// 			if (packet.getFrom() != null
// 				&& packet.getFrom().equals(session.getConnectionId())) {
// 				packet.getElement().setAttribute("from", session.getJID());
// 				log.finest("Setting correct from attribute: " + session.getJID());
// 			} // end of if (packet.getFrom().equals(session.getConnectionId()))

			String id = null;

			id = JIDUtils.getNodeID(packet.getElemTo());
			if (id.equals(session.getUserId())) {
				// Yes this is message to 'this' client
				log.finest("Yes, this is packet to 'this' client: " + id);
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getParentSession().
					getResourceConnection(packet.getElemTo()).getConnectionId());
				log.finest("Setting to: " + result.getTo());
				result.setFrom(packet.getTo());
				results.offer(result);
				return true;
			} // end of else

			id = JIDUtils.getNodeID(packet.getElemFrom());
			if (id.equals(session.getUserId())) {
				Element result = packet.getElement().clone();
				results.offer(new Packet(result));
				return true;
			}
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: "	+ packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch

		return false;
	}

}
