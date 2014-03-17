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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.ext;

import tigase.net.ConnectionType;
import tigase.util.SizedCache;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.JID;

import java.util.List;

/**
 * Created: Jun 14, 2010 12:05:41 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentIOService extends XMPPIOService<List<ComponentConnection>> {
	private static final int MAX_RECENT_JIDS = 10000;
	private static final long MAX_CACHE_TIME = 100000;

	private boolean authenticated = false;
	private String routings = null;
	private SizedCache<JID, JID> recentJIDs = new SizedCache<JID, JID>(MAX_RECENT_JIDS);

	/**
	 * Method description
	 * 
	 * 
	 * 
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	public String getRoutings() {
		return routings;
	}

	public void addRecentJID(JID jid) {
		// We only save recent JIDs on the external component side
		if (connectionType() == ConnectionType.connect) {
			recentJIDs.put(jid, jid);
		}
	}

	public boolean isRecentJID(JID jid) {
		return jid != null && recentJIDs.get(jid) != null;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param authenticated
	 */
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public void setRoutings(String r) {
		routings = r;
	}

}
