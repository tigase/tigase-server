/*
 * EventsRegistrar.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 */

package tigase.eventbus.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class EventsRegistrar {

	private final ConcurrentHashMap<String, EventInfo> events = new ConcurrentHashMap<>();

	public String getDescription(String eventName) {
		EventInfo info = events.get(eventName);
		return info == null ? null : info.getDescription();
	}

	public Collection<String> getRegisteredEvents() {
		ArrayList<String> result = new ArrayList<>();
		for (EventInfo info : events.values()) {
			if (info.isPrivateEvent())
				continue;
			result.add(info.event);
		}
		return result;
	}

	public boolean isRegistered(String eventName) {
		return this.events.containsKey(eventName);
	}

	public boolean isRegistered(Class<?> eventClass) {
		return this.events.containsKey(eventClass.getName());
	}

	public void register(String event, String description, boolean privateEvent) {
		EventInfo info = new EventInfo(event);
		info.setDescription(description);
		info.setPrivateEvent(privateEvent);

		this.events.put(event, info);
	}

	private static class EventInfo {

		private final String event;
		private String description;
		private boolean privateEvent;

		public EventInfo(String event) {
			this.event = event;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isPrivateEvent() {
			return privateEvent;
		}

		public void setPrivateEvent(boolean privateEvent) {
			this.privateEvent = privateEvent;
		}
	}

}
