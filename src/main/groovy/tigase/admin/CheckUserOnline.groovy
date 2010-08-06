/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev: $
 * Last modified by $Author: $
 * $Date: $
 */

/*

User add script as described in XEP-0133:
http://xmpp.org/extensions/xep-0133.html#add-user

AS:Description: Checks if the user is online, where he is connected from and how many connections.
AS:CommandId: check-online-user
AS:Component: sess-man
*/

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*

def JID = "accountjid"

def p = (Packet)packet
def sessions = (Map<BareJID, XMPPSession>)userSessions

def userJid = Command.getFieldValue(packet, JID)

if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

  Command.addTitle(result, "Checking online user")
	Command.addInstructions(result, "Fill out this form to check a user.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
    "hidden")
	Command.addFieldValue(result, JID, userJid ?: "", "jid-single",
    "The Jabber ID for the account to be added")

	return result
}

XMPPSession session = sessions.get(BareJID.bareJIDInstanceNS(userJid))

if (session == null) {
	return "There is no user's ${userJid} active session on the server"
} else {
  List<XMPPResourceConnection> conns = session.getActiveResources()
	String conns_str = "Connections: "
	for (XMPPResourceConnection con: conns)
	  conns_str += con.toString() + "###\n"
	return "There is an active user's ${userJid} session on the server with ${conns?.size()} " +
    "user connections: " + conns_str
}
