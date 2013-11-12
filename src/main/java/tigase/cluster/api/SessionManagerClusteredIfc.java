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

import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.ConcurrentHashMap;
import tigase.xml.Element;

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

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param el_packet
	 *
	 * 
	 */
	XMPPResourceConnection getXMPPResourceConnection(Packet el_packet);

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	ConcurrentHashMap<JID, XMPPResourceConnection> getXMPPResourceConnections();

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	ConcurrentHashMap<BareJID, XMPPSession> getXMPPSessions();
	
	void processPresenceUpdate(XMPPSession session, Element element);
}


//~ Formatted in Tigase Code Convention on 13/07/06
