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

package tigase.monitor.tasks;

import tigase.eventbus.EventBusEvent;
import tigase.util.datetime.TimestampHelper;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.dns.DNSResolverIfc;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class TasksEvent
		implements Serializable, EventBusEvent {

	private final static TimestampHelper dtf = new TimestampHelper();
	private final DNSResolverIfc dnsResolver = DNSResolverFactory.getInstance();
	String description;
	String external_hostname;
	String hostname;
	String name;
	String timestamp;



	public TasksEvent(String name, String description) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(description);
		this.name = name;
		this.description = description;
		this.timestamp = "" + dtf.format(new Date());
		this.hostname = dnsResolver.getDefaultHost();
		this.external_hostname = dnsResolver.getSecondaryHost();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TasksEvent that = (TasksEvent) o;

		if (!name.equals(that.name)) {
			return false;
		}
		if (!description.equals(that.description)) {
			return false;
		}
		if (!timestamp.equals(that.timestamp)) {
			return false;
		}
		if (!hostname.equals(that.hostname)) {
			return false;
		}
		return external_hostname.equals(that.external_hostname);
	}

	public String asString() {
		return getAdditionalData().entrySet()
				.stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.collect(Collectors.joining("\n"));
	}

	abstract public Map<String, String> getAdditionalData();

	public String getDescription() {
		return description;
	}

	public String getExternal_hostname() {
		return external_hostname;
	}

	public String getHostname() {
		return hostname;
	}

	public String getName() {
		return name;
	}

	public String getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + description.hashCode();
		result = 31 * result + timestamp.hashCode();
		result = 31 * result + hostname.hashCode();
		result = 31 * result + external_hostname.hashCode();
		return result;
	}
}
