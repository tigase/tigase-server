/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.cluster.strategy;

import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

/**
 * @author Artur Hefczyc Created Mar 15, 2011
 */
public class ConnectionRecord implements Comparable<ConnectionRecord> {

	private static final String TOP_ELEMENT = "conn-rec";
	private static final String NODE_ELEMENT = "node-jid";
	private static final String JID_ELEMENT = "user-jid";
	private static final String CONNECTION_ID_ELEMENT = "connection-id";
	private static final String SESSION_ID_ELEMENT = "session-id";
	private static final String PRESENCE_ELEMENT = "presence";

	private JID node;
	private JID userJid;
	private JID connectionId;
	private String sessionId;
	private Element lastPresence;

	/**
	 * @param node
	 * @param user_jid
	 * @param sessionId
	 * @param connectionId
	 */
	public ConnectionRecord(JID node, JID userJid, String sessionId, JID connectionId) {
		super();
		this.node = node;
		this.userJid = userJid;
		this.connectionId = connectionId;
		this.sessionId = sessionId;
	}

	public ConnectionRecord(Element elem) {
		super();
		this.node = JID.jidInstanceNS(elem.getChild(NODE_ELEMENT).getCData());
		this.userJid = JID.jidInstanceNS(elem.getChild(JID_ELEMENT).getCData());
		this.connectionId =
			JID.jidInstanceNS(elem.getChild(CONNECTION_ID_ELEMENT).getCData());
		this.sessionId = elem.getChild(SESSION_ID_ELEMENT).getCData();
		this.lastPresence = elem.getChild(PRESENCE_ELEMENT);
	}

	public Element toElement() {
		Element result = new Element(TOP_ELEMENT);
		result.addChild(new Element(NODE_ELEMENT, node.toString()));
		result.addChild(new Element(JID_ELEMENT, userJid.toString()));
		result.addChild(new Element(CONNECTION_ID_ELEMENT, connectionId.toString()));
		result.addChild(new Element(SESSION_ID_ELEMENT, sessionId));
		if (lastPresence != null) {
			result.addChild(lastPresence);
		}
		return result;
	}

	/**
	 * @return the node
	 */
	public JID getNode() {
		return node;
	}

	/**
	 * @return the user_jid
	 */
	public JID getUserJid() {
		return userJid;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @return the connectionId
	 */
	public JID getConnectionId() {
		return connectionId;
	}

	/**
	 * @param last_presence
	 *          the last_presence to set
	 */
	public void setLastPresence(Element lastPresence) {
		this.lastPresence = lastPresence;
	}

	/**
	 * @return the last_presence
	 */
	public Element getLastPresence() {
		return lastPresence;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ConnectionRecord=[");
		sb.append("node: ").append(node);
		sb.append(", userJid: ").append(userJid);
		sb.append(", connectionId: ").append(connectionId);
		sb.append(", sessionId: ").append(sessionId);
		sb.append(", lastPresence: ").append(lastPresence);
		sb.append("]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
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

	public int hashCode() {
		return connectionId.hashCode();
	}

}
