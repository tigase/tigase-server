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
package tigase.component.responses;

import tigase.component.responses.ResponseManager.Entry;
import tigase.server.Packet;

public class DefaultResponseHandler
		implements Runnable {

	private final Entry entry;

	private final Packet packet;

	public DefaultResponseHandler(Packet packet, ResponseManager.Entry entry) {
		this.packet = packet;
		this.entry = entry;
	}

	@Override
	public void run() {
		final String type = this.packet.getElement().getAttributeStaticStr("type");

		if (type != null && type.equals("result")) {
			entry.getCallback().onSuccess(packet);
		} else if (type != null && type.equals("error")) {
			String condition = packet.getErrorCondition();
			entry.getCallback().onError(packet, condition);
		}
	}

}
