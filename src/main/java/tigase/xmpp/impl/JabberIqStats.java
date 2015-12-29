/*
 * JabberIqStats.java
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

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;

import tigase.util.ElementUtils;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
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
 * XEP-0039: Statistics Gathering.
 * http://www.xmpp.org/extensions/xep-0039.html
 *
 * Created: Sat Mar 25 06:45:00 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqStats
				extends XMPPProcessor
				implements XMPPProcessorIfc {
	private static final String[][] ELEMENTS = {
		Iq.IQ_QUERY_PATH, Iq.IQ_COMMAND_PATH
	};
	private static final Logger     log = Logger.getLogger(JabberIqStats.class.getName());
	private static final String     XMLNS    = "http://jabber.org/protocol/stats";
	private static final String     ID       = XMLNS;
	private static final String[]   XMLNSS   = { XMLNS, Command.XMLNS };
	private static final Element[]  DISCO_FEATURES = { new Element("feature",
			new String[] { "var" }, new String[] { XMLNS }) };

	//~--- methods --------------------------------------------------------------

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		if (conn == null)
			return null;
		return super.canHandle(packet, conn);
	}
	
	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {
		if (session == null) {
			return;
		}
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Received packet: {0}", packet);
			}
			if (packet.isCommand()) {
				if ((packet.getCommand() == Command.GETSTATS) && (packet.getType() == StanzaType
						.result)) {
					JID conId = session.getConnectionId(packet.getStanzaTo());

					if (conId == null) {

						// Drop it, user is no longer online.
						return;
					}

					// Send it back to user.
					Element iq = ElementUtils.createIqQuery(session.getDomainAsJID(), session
							.getJID(), StanzaType.result, packet.getStanzaId(), XMLNS);
					Element query = iq.getChild("query");
					Element stats = Command.getData(packet, "statistics", null);

					query.addChildren(stats.getChildren());

					Packet result = Packet.packetInstance(iq, session.getSMComponentId(), session
							.getJID());

					result.setPacketTo(conId);
					results.offer(result);
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Sending result: {0}", result);
					}

					return;
				} else {
					return;
				}
			}    // end of if (packet.isCommand()

			// Maybe it is message to admininstrator:
			BareJID id = (packet.getStanzaTo() != null)
					? packet.getStanzaTo().getBareJID()
					: null;

			// If ID part of user account contains only host name
			// and this is local domain it is message to admin
			if ((id == null) || session.isLocalDomain(id.toString(), false)) {
				String oldto = packet.getAttributeStaticStr("oldto");
				Packet result = Command.GETSTATS.getPacket(packet.getStanzaFrom(), session
						.getDomainAsJID(), StanzaType.get, packet.getStanzaId());

				if (oldto != null) {
					result.getElement().setAttribute("oldto", oldto);
				}
				results.offer(result);
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending result: " + result);
				}

				return;
			}
			if (session.isUserId(id)) {

				// Yes this is message to 'this' client
				Packet result = packet.copyElementOnly();

				result.setPacketTo(session.getConnectionId(packet.getStanzaTo()));
				result.setPacketFrom(packet.getTo());
				results.offer(result);
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending result: " + result);
				}
			} else {

				// This is message to some other client so I need to
				// set proper 'from' attribute whatever it is set to now.
				// Actually processor should not modify request but in this
				// case it is absolutely safe and recommended to set 'from'
				// attribute
				// Not needed anymore. Packet filter does it for all stanzas.
//      // According to spec we must set proper FROM attribute
//      el_res.setAttribute("from", session.getJID());
				Packet result = packet.copyElementOnly();

				results.offer(result);
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending result: " + result);
				}
			}    // end of else
		} catch (NotAuthorizedException e) {
			log.warning("Received stats request but user session is not authorized yet: " +
					packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		}    // end of try-catch
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
}    // JabberIqStats

