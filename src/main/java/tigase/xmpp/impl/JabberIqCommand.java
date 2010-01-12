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

import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.db.NonAuthUserRepository;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;

/**
 * Describe class JabberIqCommand here.
 *
 *
 * Created: Mon Jan 22 22:41:17 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqCommand extends XMPPProcessor implements XMPPProcessorIfc {

  private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.JabberIqCommand");

  private static final String XMLNS = Command.XMLNS;
	private static final String ID = XMLNS;
  private static final String[] ELEMENTS =
	{"command"};
  private static final String[] XMLNSS =
	{Command.XMLNS};
  private static final Element[] DISCO_FEATURES =
	{new Element("feature",	new String[] {"var"},	new String[] {XMLNS})};

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()
	{ return ELEMENTS; }

	@Override
  public String[] supNamespaces()
	{ return XMLNSS; }

	@Override
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		if (session == null) { return; }

		// Processing only commands (that should be quaranteed by name space)
		// and only unknown commands. All known commands are processed elsewhere
//		if (!packet.isCommand() || packet.getCommand() != Command.OTHER) {
//			return;
//		}

		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Received packet: " + packet.toString());
			}

			// Not needed anymore. Packet filter does it for all stanzas.
// 			// For all messages coming from the owner of this account set
// 			// proper 'from' attribute. This is actually needed for the case
// 			// when the user sends a message to himself.
// 			if (packet.getFrom().equals(session.getConnectionId())) {
// 				packet.getElement().setAttribute("from", session.getJID());
// 			} // end of if (packet.getFrom().equals(session.getConnectionId()))

			if (packet.getStanzaTo() == null) {
				packet.getElement().setAttribute("to", session.getSMComponentId().toString());
				packet.initVars(packet.getStanzaFrom(), session.getSMComponentId());
			}
			BareJID id = packet.getStanzaTo().getBareJID();

			if (id.equals(session.getUserId())) {
				// Yes this is message to 'this' client
				Packet result = packet.copyElementOnly();
				result.setPacketTo(session.getConnectionId(packet.getStanzaTo()));
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {
				// This is message to some other client
				Packet result = packet.copyElementOnly();
				results.offer(result);
			} // end of else
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: "	+ packet.toString());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}

}
