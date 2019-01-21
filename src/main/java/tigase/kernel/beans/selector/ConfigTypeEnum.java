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
package tigase.kernel.beans.selector;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andrzej on 26.04.2017.
 */
public enum ConfigTypeEnum {
	DefaultMode("default"),
	SessionManagerMode("session-manager"),
	ConnectionManagersMode("connection-managers"),
	ComponentMode("component"),
	SetupMode("setup");

	private static final ConcurrentHashMap<String, ConfigTypeEnum> values = new ConcurrentHashMap();

	static {
		Arrays.asList(values()).forEach(val -> values.put(val.id(), val));
	}

	private final String id;

	public static ConfigTypeEnum valueForId(String id) {
		return values.get(id);
	}

	ConfigTypeEnum(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

}
