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
package tigase.xmpp.impl;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Message forwarder class. Normally it is not needed to be loaded and
 * used unless there is a plugin for message packet loaded which
 * doesn't forward messages. For example if message archivizer plugin
 * is loaded then you probably need to load this one as well.
 *
 * Created: Tue Feb 21 15:49:08 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Message extends XMPPProcessor implements XMPPProcessorIfc {

  /** Class loggeer */
	private static final Logger log =
		Logger.getLogger(Message.class.getName());

  private static final String XMLNS = "jabber:client";
	private static final String ID = "message";
  private static final String[] ELEMENTS = {"message"};
  private static final String[] XMLNSS = {XMLNS};

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()
	{ return ELEMENTS; }

	@Override
  public String[] supNamespaces()
	{ return XMLNSS; }

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		// For performance reasons it is better to do the check
		// before calling logging method.
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toString());
		}

		// You may want to skip processing completely if the user is offline.
		if (session == null) {
			return;
		} // end of if (session == null)

		try {

			// Remember to cut the resource part off before comparing JIDs
			String id = JIDUtils.getNodeID(packet.getElemTo());
			// Checking if this is a packet TO the owner of the session
			if (session.getUserId().equals(id)) {
				// Yes this is message to 'this' client
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				// This is where and how we set the address of the component
				// which should rceive the result packet for the final delivery
				// to the end-user. In most cases this is a c2s or Bosh component
				// which keep the user connection.
				result.setTo(session.getConnectionId(packet.getElemTo()));
				// In most cases this might be skept, however if there is a
				// problem during packet delivery an error might be sent back
				result.setFrom(packet.getTo());
				// Don't forget to add the packet to the results queue or it
				// will be lost.
				results.offer(result);
				return;
			} // end of else

			// Remember to cut the resource part off before comparing JIDs
			id = JIDUtils.getNodeID(packet.getElemFrom());
			// Checking if this is maybe packet FROM the client
			if (session.getUserId().equals(id)) {
				// This is a packet FROM this client, the simplest action is
				// to forward it to is't destination:
				// Simple clone the XML element and....
				Element result = packet.getElement().clone();
				// ... putting it to results queue is enough
				results.offer(new Packet(result));
				return;
			}

			// Can we really reach this place here?
			// Yes, some packets don't even have from or to address.
			// The best example is IQ packet which is usually a request to
			// the server for some data. Such packets may not have any addresses
			// And they usually require more complex processing
			// This is how you check whether this is a packet FROM the user
			// who is owner of the session:
			id = packet.getFrom();
			// This test is in most cases equal to checking getElemFrom()
			if (session.getConnectionId().equals(id)) {
        // Do some packet specific processing here, but we are dealing
				// with messages here which normally need just forwarding
				Element result = packet.getElement().clone();
				// If we are here it means FROM address was missing from the
				// packet, it is a place to set it here:
				result.setAttribute("from", session.getJID());
				// ... putting it to results queue is enough
				results.offer(new Packet(result));
			}

		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: "	+ packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch

	}

} // Message
