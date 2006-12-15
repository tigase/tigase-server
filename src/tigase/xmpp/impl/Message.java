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
package tigase.xmpp.impl;

import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.db.NonAuthUserRepository;

/**
 * Describe class Message here.
 *
 *
 * Created: Tue Feb 21 15:49:08 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Message extends XMPPProcessor
	implements XMPPProcessorIfc {

  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.Message");

	protected static final String ID = "message";
  protected static final String[] ELEMENTS = {"message"};
  protected static final String[] XMLNSS = {"jabber:client"};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return;
		} // end of if (session == null)

		log.finest("INPUT: " + packet.getStringData());

		final String jid;
		try {
			jid = session.getJID();
		} catch (NotAuthorizedException e) {
      log.warning("Received message but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
			return;
		} // end of try-catch

		// For all messages coming from the owner of this account set
		// proper 'from' attribute. This is actually needed for the case
		// when the user sends a message to himself.
		if (packet.getFrom().equals(session.getConnectionId())) {
			packet.getElement().setAttribute("from", jid);
		} // end of if (packet.getFrom().equals(session.getConnectionId()))

		// Is it incoming or outgoing message?
		// It might be incoming message without specified resource part
		final String nodeid = JID.getNodeID(packet.getElemTo());
		if (nodeid.equals(JID.getNodeID(jid))) {
			// Yes this is incoming message for this user
			Element elem = (Element)packet.getElement().clone();
			Packet result = new Packet(elem);
			result.setTo(session.getConnectionId());
			result.setFrom(packet.getTo());
			results.offer(result);

			log.finest("OUTPUT: " + result.getStringData());

			return;
		} // end of if (nodeid.equals(session.getUserId()))

		// Assuming this is outgoing message
		Element result = (Element)packet.getElement().clone();
		// According to spec we must set proper FROM attribute
		// It has been already set above....
		//		result.setAttribute("from", jid);
		results.offer(new Packet(result));

		log.finest("OUTPUT: " + result.toString());

	}

} // Message
