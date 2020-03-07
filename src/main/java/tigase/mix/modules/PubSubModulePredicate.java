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
package tigase.mix.modules;

import tigase.kernel.beans.Bean;
import tigase.mix.IMixComponent;
import tigase.server.Packet;
import tigase.xmpp.jid.JID;

import java.util.function.Predicate;

@Bean(name = "pubsubModulePredicate", parent = IMixComponent.class, active = true)
public class PubSubModulePredicate implements Predicate<Packet> {

	@Override
	public boolean test(Packet packet) {
		JID jid = packet.getStanzaTo();
		if (jid == null || jid.getLocalpart() == null) {
			return true;
		}
		return !jid.getLocalpart().contains("#");

	}
}
