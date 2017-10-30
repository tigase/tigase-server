/*
 * ConnectionRecordIfc.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
package tigase.cluster.strategy;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;
import tigase.xmpp.jid.JID;

/**
 * @author kobit
 */
public interface ConnectionRecordIfc {

	Element toElement();

	//~--- get methods ----------------------------------------------------------

	JID getConnectionId();

	JID getNode();

	String getSessionId();

	JID getUserJid();

	//~--- set methods ----------------------------------------------------------

	void setElement(Element elem);

	void setRecordFields(JID node, JID userJid, String sessionId, JID connectionId);
}

//~ Formatted in Tigase Code Convention on 13/11/01
