/*
 * UserPresenceChangedEvent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.server.xmppsession;

import tigase.server.Packet;
import tigase.xmpp.XMPPSession;

/**
 * UserPresenceChangeEvent is local event (called on local node), which is fired
 * when user changes presence.
 * 
 * This event is local only as SessionManagerClustered will forward information
 * to other cluster nodes that presence is changed and on that nodes this event
 * also will be called locally, if and only if on that node is at least one
 * XMPPResouceConnection for same bare jid as client which changed presence.
 * 
 * @author andrzej
 */
public class UserPresenceChangedEvent {
	
	/**
	 * Packet containing new presence with "from" attribute set to full jid of
	 * connection which changed presence.
	 */
	private final Packet presence;
	/**
	 * Instance of XMPPSesssion for client which bare jid is same as bare jid of
	 * from attribute of changed presence.
	 */
	private final XMPPSession session;
	
	public UserPresenceChangedEvent(XMPPSession session, Packet presence) {
		this.session = session;
		this.presence = presence;
	}
	
	public Packet getPresence() {
		return presence;
	}
	
	public XMPPSession getSession() {
		return session;
	}
	
}
