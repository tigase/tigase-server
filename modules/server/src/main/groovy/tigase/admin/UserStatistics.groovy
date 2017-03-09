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
 * $Rev: $
 * Last modified by $Author: $
 * $Date: $
 */

/*
 Obtaining User Statistics as described in in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#get-user-stats
 AS:Description: Get User Statistics
 AS:CommandId: http://jabber.org/protocol/admin#user-stats
 AS:Component: sess-man
 AS:Group: Statistics
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*

def JID = "accountjid"

def p = (Packet)packet
def sessions = (Map<BareJID, XMPPSession>)userSessions
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def userJid = Command.getFieldValue(packet, JID)

if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Get User Statistics")
	Command.addInstructions(result, "Fill out this form to gather user statistics.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin", "hidden")
	Command.addFieldValue(result, JID, userJid ?: "", "jid-single","The Jabber ID for statistics")

	return result
}

bareJID = BareJID.bareJIDInstance(userJid)
VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
def result = p.commandResult(Command.DataType.result)

if (isServiceAdmin ||
(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {
	XMPPSession session = sessions.get(BareJID.bareJIDInstanceNS(userJid))

	if (session == null) {
		return "There is no user's ${userJid} active session on the server"
	} else {
		List<XMPPResourceConnection> conns = session.getActiveResources()
		String conns_str = "Connections: "
		for (XMPPResourceConnection con: conns)
			conns_str += con.toString() + "###\n"
		return "There is ${conns?.size()} active user's ${userJid} sessions, packets: ${session.getPacketsCounter()}\n" +
		"user connections:\n" + conns_str
	}
} else {
	Command.addTextField(result, "Error", "You do not have enough permissions to obtain statistics for user in this domain.");
}

return result
