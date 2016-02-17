/*
 * EventName.java
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

public class EventName {

	private final String eventName;

	private final String eventPackage;

	public EventName(String eventName) {
		int i = eventName.lastIndexOf(".");
		String tmp = i >= 0 ? eventName.substring(0, i) : "";
		this.eventPackage = tmp.equals("*") ? null : tmp;

		tmp = eventName.substring(i + 1);
		this.eventName = tmp.equals("*") ? null : tmp;
	}

	public EventName(String eventPackage, String eventName) {
		super();
		this.eventName = eventName;
		this.eventPackage = eventPackage;
	}

	public final static String toString(final String eventPackage, final String eventName) {
		String result = "";
		if (eventPackage == null)
			result += "*";
		else
			result += eventPackage;

		if (!result.isEmpty())
			result += ".";

		if (eventName == null)
			result += "*";
		else
			result += eventName;

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventName other = (EventName) obj;
		if (eventName == null) {
			if (other.eventName != null)
				return false;
		} else if (!eventName.equals(other.eventName))
			return false;
		if (eventPackage == null) {
			if (other.eventPackage != null)
				return false;
		} else if (!eventPackage.equals(other.eventPackage))
			return false;
		return true;
	}

	public String getName() {
		return eventName;
	}

	public String getPackage() {
		return eventPackage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eventName == null) ? 0 : eventName.hashCode());
		result = prime * result + ((eventPackage == null) ? 0 : eventPackage.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return toString(eventPackage, eventName);
	}

}