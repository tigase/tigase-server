/*
 * OnlineJidsReporter.java
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



package tigase.sys;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

/**
 * Created: Apr 19, 2009 12:15:07 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface OnlineJidsReporter {
	/**
	 * Checks whether there is an online session for the given user BareJID.
	 *
	 * @param jid
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	boolean containsJid(BareJID jid);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid is a <code>BareJID</code>
	 *
	 * @return a value of <code>JID[]</code>
	 */
	JID[] getConnectionIdsForJid(BareJID jid);

	// Set<String> getOnlineJids();

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	boolean hasCompleteJidsInfo();
}


//~ Formatted in Tigase Code Convention on 13/08/29
