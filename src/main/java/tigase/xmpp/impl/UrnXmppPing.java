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

import java.util.Arrays;
import java.util.Queue;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * XEP-0199: XMPP Ping
 *
 *
 * @author <a href="mailto:bmalkow@tigase.org">Bartosz Małkowski</a>
 * @version $Rev$
 */
public class UrnXmppPing extends XMPPProcessor implements XMPPProcessorIfc {

	private static final String XMLNS = "urn:xmpp:ping";

	private static final Element[] DISCO_FEATURES =
	{ new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };

	private static final String[] ELEMENTS = { "ping" };

	private static final String ID = XMLNS;

	private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.UrnXmppPing");

	private static final String[] XMLNSS = { XMLNS };

	public String id() {
		return ID;
	}

	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return;
		}
		String id = session.getDomain();
		if (packet.getElemTo() != null) {
			id = JIDUtils.getNodeID(packet.getElemTo());
		}
		if (id == null || id.equals("") || id.equalsIgnoreCase(session.getDomain())) {
			results.offer(packet.okResult((Element) null, 0));
			return;
		}

		try {
			// Not needed anymore. Packet filter does it for all stanzas.
// 			if (packet.getFrom().equals(session.getConnectionId())) {
// 				packet.getElement().setAttribute("from", session.getJID());
// 			}

			if (id.equals(session.getUserId())) {
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId());
				result.setFrom(packet.getTo());
				results.offer(result);
			} else {
				Element result = packet.getElement().clone();
				results.offer(new Packet(result));
			}
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet.getStringData());
		}
	}

	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return Arrays.copyOf(DISCO_FEATURES, DISCO_FEATURES.length);
	}

	public String[] supElements() {
		return Arrays.copyOf(ELEMENTS, ELEMENTS.length);
	}

	public String[] supNamespaces() {
		return Arrays.copyOf(XMLNSS, XMLNSS.length);
	}

}
