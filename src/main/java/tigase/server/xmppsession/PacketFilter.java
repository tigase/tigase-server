/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
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
import tigase.util.JID;

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

	public boolean process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return false;
		} // end of if (session == null)

		log.finest("Processing packet: " + packet.getStringData());

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

			// For all messages coming from the owner of this account set
			// proper 'from' attribute. This is actually needed for the case
			// when the user sends a message to himself.
			if (packet.getFrom() != null
				&& packet.getFrom().equals(session.getConnectionId())) {
				packet.getElement().setAttribute("from", session.getJID());
				log.finest("Setting correct from attribute: " + session.getJID());
			} // end of if (packet.getFrom().equals(session.getConnectionId()))

			String id = JID.getNodeID(packet.getElemTo());

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
			} else {
				// This is message to some other client
				Element result = packet.getElement().clone();
				results.offer(new Packet(result));
			} // end of else
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: "	+ packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch

		return true;
	}

}
