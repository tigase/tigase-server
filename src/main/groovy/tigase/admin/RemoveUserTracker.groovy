/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 *
 */
/*
 Activate on the server user tracking mechanisms to aid in problem resolution.
 AS:Description: Remove log tracker for a user
 AS:CommandId: http://jabber.org/protocol/admin#remove-user-tracker
 AS:Component: sess-man
 */
package tigase.admin

import tigase.server.*
import tigase.xmpp.*
import tigase.util.*
import java.util.logging.*

def JID = "accountjid"

def p = (Packet)packet

def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}

def userJid = Command.getFieldValue(packet, JID)

if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Removing a User Log Tracker")
	//Command.addInstructions(result, "Fill out this form to add a user log tracker.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")
	Command.addFieldValue(result, JID, userJid ?: "", "jid-single",
			"The Jabber ID for the tracker to be removed")
	return result
}

def result = p.commandResult(Command.DataType.result)

def hand = null

Handler[] handlers = Logger.getLogger("").getHandlers()
handlers.each {
	Filter filt = it.getFilter()
	if (filt != null && filt.class == LogUserFilter && ((LogUserFilter)filt).getId() == userJid) {
		hand = it
	}
}

if (hand != null) {
	Logger.getLogger("").removeHandler(hand)
	hand.close()
	Command.addTextField(result, "Note", "Operation successful, tracker removed for " + userJid);
} else {
	Command.addTextField(result, "Note", "No tracker found for user " + userJid)
}

return result
