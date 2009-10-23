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
import tigase.server.Command;
import tigase.server.Packet;
import tigase.disco.XMPPServiceCollector;
import tigase.util.JIDUtils;
import tigase.util.ElementUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;
import tigase.db.NonAuthUserRepository;

/**
 * Implementation of JEP-030.
 *
 *
 * Created: Mon Mar 27 20:45:36 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServiceDiscovery extends XMPPProcessor
	implements XMPPProcessorIfc {

	private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.ServiceDiscovery");

	private static final String ID = "disco";
  private static final String[] ELEMENTS =
	{"query", "query", "query"};
  private static final String[] XMLNSS = {
    XMPPServiceCollector.INFO_XMLNS,
		XMPPServiceCollector.ITEMS_XMLNS,
		Command.XMLNS
	};
  private static final Element[] DISCO_FEATURES = {
    new Element("feature",
			new String[] {"var"},
			new String[] {XMPPServiceCollector.INFO_XMLNS}),
		new Element("feature",
			new String[] {"var"},
			new String[] {XMPPServiceCollector.ITEMS_XMLNS})
	};

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()	{ return ELEMENTS; }

	@Override
  public String[] supNamespaces()	{ return XMLNSS; }

	@Override
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {
		// Service discovery is normally processed by MessageRouter so no processing
		// is needed here. This plugin exists only to make sure the elemFrom address
		// is set properly and it is set by the packet filter before it gets here.
		// All we need to do here is passing the packet back.

		// Fast packet processing if the session is null
		if (session == null) {
			results.offer(new Packet(packet.getElement()));
			return;
		}

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
			}

			// Otherwise just pass the packet for further processing
			results.offer(new Packet(packet.getElement()));
		} catch (Exception e) {
			log.warning("NotAuthorizedException for packet: "	+ packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		}
		// If the packet is addressed to the user of this session, make sure it gets through
	}

} // ServiceDiscovery
