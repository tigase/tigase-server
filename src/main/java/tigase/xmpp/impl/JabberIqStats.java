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

import java.util.Arrays;
import java.util.Queue;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.server.Command;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPException;
import tigase.util.JIDUtils;
import tigase.util.ElementUtils;
import tigase.db.NonAuthUserRepository;

/**
 * XEP-0039: Statistics Gathering.
 * http://www.xmpp.org/extensions/xep-0039.html
 *
 * Created: Sat Mar 25 06:45:00 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqStats extends XMPPProcessor
	implements XMPPProcessorIfc {

  private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.JabberIqStats");

  private static final String XMLNS = "http://jabber.org/protocol/stats";
	private static final String ID = XMLNS;
  private static final String[] ELEMENTS =
	{"query", "command"};
  private static final String[] XMLNSS =
	{XMLNS, Command.XMLNS};
  private static final Element[] DISCO_FEATURES =
	{new Element("feature",	new String[] {"var"},	new String[] {XMLNS})};

	public String id() { return ID; }

	public String[] supElements()
	{ return ELEMENTS; }

  public String[] supNamespaces()
	{ return XMLNSS; }

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		if (session == null) { return; }

		try {
			log.finest("Received packet: " + packet.getStringData());

			if (packet.isCommand()) {
				if (packet.getCommand() == Command.GETSTATS
					&& packet.getType() == StanzaType.result) {
					// Send it back to user.
					Element iq =
						ElementUtils.createIqQuery(session.getDomain(), session.getJID(),
							StanzaType.result, packet.getElemId(), XMLNS);
					Element query = iq.getChild("query");
					Element stats = Command.getData(packet, "statistics", null);
					query.addChildren(stats.getChildren());
					Packet result = new Packet(iq);
					result.setTo(session.getConnectionId());
					results.offer(result);
					log.finest("Sending result: " + result.getStringData());
					return;
				} else {
					return;
				}
			} // end of if (packet.isCommand()


			// Maybe it is message to admininstrator:
			String id = packet.getElemTo() != null ?
				JIDUtils.getNodeID(packet.getElemTo()) : null;

			// If ID part of user account contains only host name
			// and this is local domain it is message to admin
			if (id == null || id.equals("")
				|| id.equalsIgnoreCase(session.getDomain())) {
				Packet result =
					Command.GETSTATS.getPacket(packet.getElemFrom(),
					session.getDomain(), StanzaType.get, packet.getElemId());
				results.offer(result);
				log.finest("Sending result: " + result.getStringData());
				return;
			}

			if (id.equals(session.getUserId())) {
				// Yes this is message to 'this' client
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId());
				result.setFrom(packet.getTo());
				results.offer(result);
				log.finest("Sending result: " + result.getStringData());
			} else {
				// This is message to some other client so I need to
				// set proper 'from' attribute whatever it is set to now.
				// Actually processor should not modify request but in this
				// case it is absolutely safe and recommended to set 'from'
				// attribute
				Element el_res = packet.getElement().clone();
				// Not needed anymore. Packet filter does it for all stanzas.
// 				// According to spec we must set proper FROM attribute
// 				el_res.setAttribute("from", session.getJID());
				Packet result = new Packet(el_res);
				results.offer(result);
				log.finest("Sending result: " + result.getStringData());
			} // end of else
		} catch (NotAuthorizedException e) {
      log.warning(
				"Received stats request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}

} // JabberIqStats
