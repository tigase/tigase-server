/*
 * ServiceDiscovery.java
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

import tigase.disco.XMPPServiceCollector;

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

/**
 * Implementation of JEP-030.
 *
 *
 * Created: Mon Mar 27 20:45:36 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class ServiceDiscovery
				extends XMPPProcessorAbstract {
	private static final String     ID       = "disco";
	private static final Logger     log = Logger.getLogger(ServiceDiscovery.class
			.getName());
	private static final String[][] ELEMENTS = {
		Iq.IQ_QUERY_PATH, Iq.IQ_QUERY_PATH, Iq.IQ_QUERY_PATH
	};
	private static final String[]   XMLNSS = { XMPPServiceCollector.INFO_XMLNS,
			XMPPServiceCollector.ITEMS_XMLNS, Command.XMLNS };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { XMPPServiceCollector.INFO_XMLNS }),
			new Element("feature", new String[] { "var" }, new String[] { XMPPServiceCollector
					.ITEMS_XMLNS }) };

	//~--- methods --------------------------------------------------------------

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		if (packet.isServiceDisco()) {
			try {
				if (packet.getStanzaTo() != null && packet.getStanzaTo().getLocalpart() != null 
						&& packet.getStanzaTo().getResource() == null
						&& (conn == null || conn.isUserId(packet.getStanzaTo().getBareJID())))
					return null;
			} catch (NotAuthorizedException ex) {
			}
		}
		return super.canHandle(packet, conn);
	}	
	
	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException {

		// Handled elsewhere (in MessageRouter)
		if ( packet.getStanzaTo() != null ){
			log.log( Level.FINEST, "forwarding packet to MR" + packet.toString() );
			results.offer( packet.copyElementOnly() );
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {
		results.offer(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet,
				"The target is unavailable at this time.", true));
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {

		// Handled elsewhere (in MessageRouter)
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
}    // ServiceDiscovery
