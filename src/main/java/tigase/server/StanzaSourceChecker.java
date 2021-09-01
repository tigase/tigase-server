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

package tigase.server;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.server.xmppclient.ClientConnectionManager;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Bean(name = "stanza-source-checker", parent = Kernel.class, active = true, exportable = true)
public class StanzaSourceChecker {

	@Inject(nullAllowed = true)
	private Set<ClientConnectionManager> clientConnectionManagers;
	private Set<String> clientConnectionManagersIds = new HashSet<>(3);

	public void setClientConnectionManagers(Set<ClientConnectionManager> clientConnectionManagers) {
		this.clientConnectionManagers = clientConnectionManagers;
		if (clientConnectionManagers != null) {
			this.clientConnectionManagersIds = clientConnectionManagers.stream()
					.map(BasicComponent::getName)
					.collect(Collectors.toSet());
		}
	}

	public boolean isPacketFromConnectionManager(Packet packet) {
		return packet.getPacketFrom() != null && packet.getPacketFrom().getLocalpart() != null &&
				clientConnectionManagersIds.contains(packet.getPacketFrom().getLocalpart());
	}
}
