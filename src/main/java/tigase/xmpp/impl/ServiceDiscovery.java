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
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Implementation of JEP-030.
 *
 *
 * Created: Mon Mar 27 20:45:36 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
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

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param connectionId
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException {

		// Handled elsewhere (in MessageRouter)
		results.offer(packet.copyElementOnly());
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {
		results.offer(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet,
				"The target is unavailable at this time.", true));
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
	 *
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {

		// Handled elsewhere (in MessageRouter)
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}    // ServiceDiscovery


//~ Formatted in Tigase Code Convention on 13/03/12
