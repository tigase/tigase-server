/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.component.adhoc;

import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.JID;

public class AdhHocRequest {

	private final String action;

	private final Element command;

	private final Packet iq;

	private final String node;

	private final JID sender;

	private final String sessionId;

	AdhHocRequest(Packet iq, Element command, String node, JID sender, String action, String sessionId) {
		super();
		this.iq = iq;
		this.command = command;
		this.node = node;
		this.action = action;
		this.sessionId = sessionId;
		this.sender = sender;
	}

	public String getAction() {
		return action;
	}

	public Element getCommand() {
		return command;
	}

	public Packet getIq() {
		return iq;
	}

	public String getNode() {
		return node;
	}

	public JID getSender() {
		return sender;
	}

	public String getSessionId() {
		return sessionId;
	}

}
