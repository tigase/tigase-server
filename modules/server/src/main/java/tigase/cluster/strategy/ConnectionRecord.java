/*
 * ConnectionRecord.java
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



package tigase.cluster.strategy;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;

import tigase.xmpp.JID;

/**
 * @author Artur Hefczyc Created Mar 15, 2011
 */
public class ConnectionRecord
				implements ConnectionRecordIfc, Comparable<ConnectionRecord> {
	private static final String CONNECTION_ID_ELEMENT = "connection-id";
	private static final String JID_ELEMENT           = "user-jid";
	private static final String NODE_ELEMENT          = "node-jid";
	private static final String SESSION_ID_ELEMENT    = "session-id";
	private static final String TOP_ELEMENT           = "conn-rec";

	//~--- fields ---------------------------------------------------------------

	private JID    connectionId;
	private JID    node;
	private String sessionId;
	private JID    userJid;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public ConnectionRecord() {
		super();
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public int compareTo(ConnectionRecord rec) {
		return connectionId.compareTo(rec.connectionId);
	}

	@Override
	public boolean equals(Object rec) {
		boolean result = false;

		if (rec instanceof ConnectionRecord) {
			result = connectionId.equals(((ConnectionRecord) rec).connectionId);
		}

		return result;
	}

	@Override
	public int hashCode() {
		return connectionId.hashCode();
	}

	@Override
	public Element toElement() {
		Element result = new Element(TOP_ELEMENT);

		result.addChild(new Element(NODE_ELEMENT, node.toString()));
		result.addChild(new Element(JID_ELEMENT, userJid.toString()));
		result.addChild(new Element(CONNECTION_ID_ELEMENT, connectionId.toString()));
		result.addChild(new Element(SESSION_ID_ELEMENT, sessionId));

		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("ConnectionRecord=[");
		sb.append("node: ").append(node);
		sb.append(", userJid: ").append(userJid);
		sb.append(", connectionId: ").append(connectionId);
		sb.append(", sessionId: ").append(sessionId);
		sb.append("]");

		return sb.toString();
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public JID getConnectionId() {
		return connectionId;
	}

	@Override
	public JID getNode() {
		return node;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public JID getUserJid() {
		return userJid;
	}

	//~--- set methods ----------------------------------------------------------

	public void setConnectionId(JID connectionId) {
		this.connectionId = connectionId;
	}

	@Override
	public void setElement(Element elem) {
		this.node         = JID.jidInstanceNS(elem.getChild(NODE_ELEMENT).getCData());
		this.userJid      = JID.jidInstanceNS(elem.getChild(JID_ELEMENT).getCData());
		this.connectionId = JID.jidInstanceNS(elem.getChild(CONNECTION_ID_ELEMENT)
				.getCData());
		this.sessionId = elem.getChild(SESSION_ID_ELEMENT).getCData();
	}

	@Override
	public void setRecordFields(JID node, JID userJid, String sessionId, JID connectionId) {
		this.node         = node;
		this.userJid      = userJid;
		this.connectionId = connectionId;
		this.sessionId    = sessionId;
	}
}
