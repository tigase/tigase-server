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
package tigase.server.xmppclient;

import tigase.server.Command;
import tigase.server.Packet;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StreamManagementCommand {
	ENABLE,
	ENABLED,
	MOVE_STREAM,
	STREAM_MOVED;

	public static StreamManagementCommand fromPacket(Packet packet) {
		return valueof(Command.getFieldValue(packet, "cmd"));
	}

	public static StreamManagementCommand valueof(String cmdId) {
		return COMMANDS.get(cmdId);
	}
	
	private static final Map<String, StreamManagementCommand> COMMANDS = Arrays.stream(StreamManagementCommand.values()).collect(
			Collectors.toMap(StreamManagementCommand::getId, Function.identity()));

	private final String cmdId;

	private StreamManagementCommand() {
		this.cmdId = name().toLowerCase().replace('_', '-');
	}

	public Packet create(JID from, JID to) {
		Packet packet = Command.STREAM_MOVED.getPacket(from, to, StanzaType.set, UUID.randomUUID().toString());
		Command.addFieldValue(packet, "cmd", getId());
		return packet;
	}

	public String getId() {
		return cmdId;
	}
	
}
