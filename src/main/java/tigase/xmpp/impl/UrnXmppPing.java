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

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;

import tigase.server.BasicComponent;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * XEP-0199: XMPP Ping
 *
 *
 * @author <a href="mailto:bmalkow@tigase.org">Bartosz Małkowski</a>
 * @version $Rev$
 */
public class UrnXmppPing extends XMPPProcessor implements XMPPProcessorIfc {
	private static final String[] ELEMENTS = { "ping" };
	private static final String XMLNS = "urn:xmpp:ping";
	private static final String ID = XMLNS;
	private static final Element[] DISCO_FEATURES = { new Element("feature",
					new String[] { "var" },
					new String[] { XMLNS }) };
	private static final Logger log = Logger.getLogger("tigase.xmpp.impl.UrnXmppPing");
	private static final String[] XMLNSS = { XMLNS };

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session,
											NonAuthUserRepository repo, Queue<Packet> results,
											Map<String, Object> settings) {
		if (session == null) {
			try {
				results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
								"Service not available.", true));
			} catch (PacketErrorTypeException e) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("This is already ping error packet, ignoring... " + packet);
				}
			}

			return;
		}

		BareJID id = session.getDomainAsJID().getBareJID();

		if (packet.getStanzaTo() != null) {
			id = packet.getStanzaTo().getBareJID();
		}

		if ((id == null) || id.toString().equals(session.getDomain())
				|| session.isLocalDomain(id.toString(), false)
				|| (session.getConnectionId() == BasicComponent.NULL_ROUTING)) {
			results.offer(packet.okResult((Element) null, 0));

			return;
		}

		try {
			if (id.equals(session.getUserId())) {
				Packet result = packet.copyElementOnly();

				result.setPacketTo(session.getConnectionId(packet.getStanzaTo()));
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {
				results.offer(packet.copyElementOnly());
			}
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
