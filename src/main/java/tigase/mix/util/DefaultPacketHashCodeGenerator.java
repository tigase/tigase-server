/*
 * Tigase MIX - MIX component for Tigase
 * Copyright (C) 2020 Tigase, Inc. (office@tigase.com)
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
package tigase.mix.util;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.IMixComponent;
import tigase.pubsub.PubSubComponent;
import tigase.server.Packet;

/**
 * Custom PacketHashCodeGenerator for MIX to process all requests from the same user on the same thread.
 */
@Bean(name = "packetHashCodeGenerator", parent = IMixComponent.class, active = true)
public class DefaultPacketHashCodeGenerator implements PubSubComponent.PacketHashCodeGenerator {

	@Inject(nullAllowed = true)
	private IMixComponent component;

	@Override
	public int hashCodeForPacket(Packet packet) {
		if ((packet.getStanzaFrom() != null) && (packet.getPacketFrom() != null) &&
				!component.getComponentId().equals(packet.getPacketFrom())) {
			return packet.getStanzaFrom().hashCode();
		}

		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}

		return 1;
	}
}
