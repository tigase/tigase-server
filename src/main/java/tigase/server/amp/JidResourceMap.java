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
package tigase.server.amp;

import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic utility class to allow storage of any value for particular JID (including resource).
 *
 * @param <T>
 *
 * @author andrzej
 */
public class JidResourceMap<T> {

	private final ConcurrentHashMap<BareJID, Map<String, T>> usersMap = new ConcurrentHashMap<BareJID, Map<String, T>>();

	public JidResourceMap() {

	}

	public boolean containsKey(BareJID jid) {
		return usersMap.containsKey(jid);
	}

	public boolean containsKey(JID jid) {
		Map<String, T> resources = usersMap.get(jid.getBareJID());
		return resources != null && resources.containsKey(jid.getResource());
	}

	public T get(JID jid) {
		Map<String, T> resources = usersMap.get(jid.getBareJID());
		if (resources == null) {
			return null;
		} else {
			synchronized (resources) {
				return resources.get(jid.getResource());
			}
		}
	}

	public T put(JID jid, T value) {
		if (value == null) {
			return remove(jid);
		}

		Map<String, T> resources = usersMap.get(jid.getBareJID());

		if (resources == null) {
			resources = new HashMap<String, T>();

			Map<String, T> oldResources = usersMap.putIfAbsent(jid.getBareJID(), resources);

			if (oldResources != null) {
				resources = oldResources;
			}
		}
		if (jid.getResource() != null) {
			synchronized (resources) {
				return resources.put(jid.getResource(), value);
			}
		}
		return null;
	}

	public T remove(JID jid) {
		Map<String, T> resources = usersMap.get(jid.getBareJID());

		if (resources == null) {
			return null;
		}

		synchronized (resources) {
			return resources.remove(jid.getResource());
		}
	}
}
