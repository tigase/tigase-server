/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.api;

import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.xml.Element;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kobit
 */
public interface SessionManagerClusteredIfc
		extends SessionManagerHandler {

	public static final String SESSION_FOUND_KEY = "user-session-found-key";

	boolean fastAddOutPacket(Packet packet);

	void processPacket(Packet el_packet, XMPPResourceConnection conn);

	void processPresenceUpdate(XMPPSession session, Element element);

	XMPPResourceConnection getXMPPResourceConnection(Packet el_packet);

	ConcurrentHashMap<JID, XMPPResourceConnection> getXMPPResourceConnections();

	ConcurrentHashMap<BareJID, XMPPSession> getXMPPSessions();

	/**
	 * Method to check if there is XMPPResourceConnection instance for connection JID.
	 *
	 * @param connJid
	 *
	 * @return true - if there is XMPPResourceConnection for connection JID
	 */
	boolean hasXMPPResourceConnectionForConnectionJid(JID connJid);

	JID getComponentId();

	List<JID> getNodesConnected();
}

