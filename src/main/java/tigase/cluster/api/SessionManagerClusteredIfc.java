/*
 * SessionManagerClusteredIfc.java
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import java.util.List;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author kobit
 */
public interface SessionManagerClusteredIfc
				extends SessionManagerHandler {
	/** Field description */
	public static final String SESSION_FOUND_KEY = "user-session-found-key";

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	boolean fastAddOutPacket(Packet packet);

	/**
	 * Method description
	 *
	 *
	 * @param el_packet
	 * @param conn
	 */
	void processPacket(Packet el_packet, XMPPResourceConnection conn);

	/**
	 * Method description
	 *
	 *
	 * @param session is a <code>XMPPSession</code>
	 * @param element is a <code>Element</code>
	 */
	void processPresenceUpdate(XMPPSession session, Element element);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param el_packet
	 *
	 *
	 *
	 * @return a value of <code>XMPPResourceConnection</code>
	 */
	XMPPResourceConnection getXMPPResourceConnection(Packet el_packet);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of {@code ConcurrentHashMap<JID,XMPPResourceConnection>}
	 */
	ConcurrentHashMap<JID, XMPPResourceConnection> getXMPPResourceConnections();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of {@code ConcurrentHashMap<BareJID,XMPPSession>}
	 */
	ConcurrentHashMap<BareJID, XMPPSession> getXMPPSessions();
	
	/**
	 * Method to check if there is XMPPResourceConnection instance for 
	 * connection JID.
	 * 
	 * @param connJid
	 * @return true - if there is XMPPResourceConnection for connection JID
	 */
	boolean hasXMPPResourceConnectionForConnectionJid(JID connJid);

	JID getComponentId();

	List<JID> getNodesConnected();
}


//~ Formatted in Tigase Code Convention on 13/11/29
