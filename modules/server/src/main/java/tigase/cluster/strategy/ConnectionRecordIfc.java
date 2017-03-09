/*
 * ConnectionRecordIfc.java
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
package tigase.cluster.strategy;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;

import tigase.xmpp.JID;

/**
 *
 * @author kobit
 */
public interface ConnectionRecordIfc {
	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>Element</code>
	 */
	Element toElement();

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	JID getConnectionId();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	JID getNode();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	String getSessionId();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	JID getUserJid();

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param elem
	 */
	void setElement(Element elem);

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param userJid
	 * @param sessionId
	 * @param connectionId
	 */
	void setRecordFields(JID node, JID userJid, String sessionId, JID connectionId);
}


//~ Formatted in Tigase Code Convention on 13/11/01
