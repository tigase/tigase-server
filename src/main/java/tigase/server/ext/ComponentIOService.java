/*
 * ComponentIOService.java
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



package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import tigase.net.ConnectionType;

import tigase.util.SizedCache;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

/**
 * Created: Jun 14, 2010 12:05:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentIOService
				extends XMPPIOService<List<ComponentConnection>> {
	private static final long MAX_CACHE_TIME  = 100000;
	private static final int  MAX_RECENT_JIDS = 10000;

	//~--- fields ---------------------------------------------------------------

	private boolean              authenticated = false;
	private String               routings      = null;
	private SizedCache<JID, JID> recentJIDs    = new SizedCache<JID, JID>(MAX_RECENT_JIDS);

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid is a <code>JID</code>
	 */
	public void addRecentJID(JID jid) {

		// We only save recent JIDs on the external component side
		if (connectionType() == ConnectionType.connect) {
			recentJIDs.put(jid, jid);
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getRoutings() {
		return routings;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid is a <code>JID</code>
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isRecentJID(JID jid) {
		return (jid != null) && (recentJIDs.get(jid) != null);
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param authenticated
	 */
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	/**
	 * Method description
	 *
	 *
	 * @param r is a <code>String</code>
	 */
	public void setRoutings(String r) {
		routings = r;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
