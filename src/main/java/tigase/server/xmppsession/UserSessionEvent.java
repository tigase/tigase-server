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
package tigase.server.xmppsession;

import tigase.xmpp.XMPPSession;
import tigase.xmpp.jid.JID;

import java.io.Serializable;

/**
 * Base class for implementation of events related to user session. For this event exists additional routing mechanism
 * which will optimize delivery of this event in clustered environment.
 *
 * @author andrzej
 */
public class UserSessionEvent
		implements Serializable {

	private JID sender;
	private transient XMPPSession session;
	// this is destination to which event will be routed
	private JID userJid;

	public UserSessionEvent() {
	}

	public UserSessionEvent(JID sender, JID userJid, XMPPSession session) {
		this.sender = sender;
		this.session = session;
		this.userJid = userJid;
	}

	public XMPPSession getSession() {
		return session;
	}

	public void setSession(XMPPSession session) {
		this.session = session;
	}

	public JID getUserJid() {
		return userJid;
	}

	public JID getSender() {
		return sender;
	}

}
