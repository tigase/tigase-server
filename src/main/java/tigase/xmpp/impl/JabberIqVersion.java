/*
 * JabberIqVersion.java
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

import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.XMPPServer;

import tigase.xml.Element;

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
 * XEP-0092: Software Version
 *
 *
 * Created: Tue Mar 21 06:45:51 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqVersion
				extends XMPPProcessorAbstract {
	private static final Logger     log = Logger.getLogger(JabberIqVersion.class.getName());
	private static final String     XMLNS    = "jabber:iq:version";
	private static final String     ID       = XMLNS;
	private static final String[][] ELEMENTS = {
		Iq.IQ_QUERY_PATH
	};
	private static final String[]   XMLNSS   = { XMLNS };
	private static final Element    SERVER_VERSION = new Element("query", new Element[] {
			new Element("name", XMPPServer.NAME),
			new Element("version", XMPPServer.getImplementationVersion()), new Element("os",
					System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "-" +
					System.getProperty("os.version") + ", " + System.getProperty("java.vm.name") +
					"-" + System.getProperty("java.vm.version") + "-" + System.getProperty(
					"java.vm.vendor")) }, new String[] { "xmlns" }, new String[] { XMLNS });
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { XMLNS }) };

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserPacket( JID connectionId, Packet packet, XMPPResourceConnection session,
																		 NonAuthUserRepository repo, Queue<Packet> results,
																		 Map<String, Object> settings )
			throws PacketErrorTypeException {
			// Check whether the packet is addressed to the server or some other, XMPP entity
			if ((packet.getStanzaTo() == null)
					|| session.isLocalDomain(packet.getStanzaTo().toString(), false)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
							new Object[] { "processFromUserToServerPacket",
							packet, session });
				}
				processFromUserToServerPacket(connectionId, packet, session, repo, results,
						settings);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
							new Object[] { "processFromUserOutPacket",
							packet, session });
				}
				processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
			}
	}

	@Override
	public void processFromUserOutPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) {
		results.offer(packet.copyElementOnly());
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) {
		results.offer(packet.okResult(SERVER_VERSION, 0));
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		results.offer(packet.okResult(SERVER_VERSION, 0));
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
}    // JabberIqVersion

