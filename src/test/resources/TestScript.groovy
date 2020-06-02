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

package tigase.admin

import tigase.db.UserExistsException
import tigase.server.Command
import tigase.server.Packet
import tigase.xmpp.jid.BareJID

import java.util.logging.Level
import java.util.logging.Logger

def log = Logger.getLogger("tigase.admin");

def JID = "accountjid"

def p = (Packet) packet

def userJid = Command.getFieldValue(packet, JID)

if (log.isLoggable(Level.FINEST)) {
	log.log(Level.FINEST, "Executing command test-cmd: ${userJid}. Request: ${p}, command: ${this}")
}

def result = p.commandResult(Command.DataType.result)
try {
	Command.addTextField(result, "Note", "${userJid}");
	if (log.isLoggable(Level.FINEST)) {
		log.log(Level.FINEST, "User ${userJid} processed correctly. Request packet: ${p}, command: ${this}")
	}
} catch (Exception ex) {
	log.log(Level.WARNING, "Error while processing user ${userJid}", ex)
	Command.addTextField(result, "Note", "Problem processing user ${userJid}.");
}

return result

