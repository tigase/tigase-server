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
package tigase.mix.modules.mam;

import tigase.component.exceptions.ComponentException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.IMixComponent;
import tigase.mix.modules.RoomPresenceModule;
import tigase.server.Packet;

@Bean(name = "mamQueryParser", parent = IMixComponent.class, active = true)
public class MAMQueryParser extends tigase.pubsub.modules.mam.MAMQueryParser {

	@Inject(nullAllowed = true)
	private RoomPresenceModule roomPresenceModule;

	public MAMQueryParser() {
	}

	@Override
	protected String parseQueryForNode(Packet packet) throws ComponentException {
		String node = super.parseQueryForNode(packet);
		if (node == null && roomPresenceModule != null) {
			return "urn:xmpp:mix:nodes:messages";
		}
		return node;
	}

}
