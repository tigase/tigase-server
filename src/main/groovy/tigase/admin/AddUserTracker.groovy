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
/*
 Activate on the server user tracking mechanisms to aid in problem resolution.
 AS:Description: Activate log tracker for a user
 AS:CommandId: http://jabber.org/protocol/admin#add-user-tracker
 AS:Component: sess-man
 */
package tigase.admin

import tigase.server.Command
import tigase.server.Packet
import tigase.util.log.LogUserFilter
import tigase.xmpp.XMPPSession
import tigase.xmpp.jid.BareJID

import java.util.logging.*

def JID = "accountjid"
def FILE_NAME = "file-name"
def FILE_LIMIT = 25000000
def FILE_COUNT = 5

def p = (Packet) packet

def userJid = Command.getFieldValue(packet, JID)
def fileName = Command.getFieldValue(packet, FILE_NAME)

def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}


if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Adding a User Log Tracker")
	Command.addInstructions(result, "Fill out this form to add a user log tracker.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
						  "hidden")
	Command.addFieldValue(result, JID, userJid ?: "", "jid-single",
						  "The Jabber ID for the account to be tracked")
	Command.addFieldValue(result, FILE_NAME, fileName ?: "", "text-single",
						  "File name to write user's log entries")

	return result
}

// Remove the old tracker if there is any active for that user
def hand = null

Handler[] handlers = Logger.getLogger("").getHandlers()
handlers.each {
	Filter filt = it.getFilter()
	if (filt != null && filt.class == LogUserFilter && ((LogUserFilter) filt).getId() == userJid) {
		hand = it
	}
}
if (hand != null) {
	Logger.getLogger("").removeHandler(hand)
	hand.close()
	Command.addTextField(result, "Note", "Operation successful, tracker removed for " + userJid);
}

// Ok now we can setup a new tracker
def result = p.commandResult(Command.DataType.result)

def users_sessions = (Map) userSessions
def bareJID = BareJID.bareJIDInstance(userJid)

XMPPSession session = users_sessions.get(bareJID)

if (fileName == null || fileName == "") {
	fileName = "logs/" + userJid
}
LogUserFilter filter = new LogUserFilter(bareJID, users_sessions)

FileHandler handler = new FileHandler(fileName, FILE_LIMIT, FILE_COUNT)
handler.setLevel(Level.ALL)
handler.setFilter(filter)
Logger.getLogger("").addHandler(handler)

Command.addTextField(result, "Note", "Operation successful");
Command.addTextField(result, "Note", "Tracking user " + userJid)

return result
