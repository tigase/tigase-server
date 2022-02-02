/*
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
package tigase.map;

import tigase.eventbus.EventBusEvent;

import java.util.Map;

public class MapDestroyedEvent implements EventBusEvent {

	private final Map map;
	private final String uid;

	public MapDestroyedEvent(Map map, String uid) {
		this.map = map;
		this.uid = uid;
	}

	public Map getMap() {
		return map;
	}

	public String getUid() {
		return uid;
	}

}
