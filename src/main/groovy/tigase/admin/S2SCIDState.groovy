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
/*
 Returns S2S connection state for given CID
 AS:Description: S2S get CID connection state
 AS:CommandId: s2s-get-cid-connection
 AS:Component: s2s
 */

package tigase.admin

import tigase.server.Command
import tigase.server.Packet
import tigase.server.xmppserver.CID
import tigase.server.xmppserver.CIDConnections

def p = (Packet) packet
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def CID_KEY = "cid"

def cid = Command.getFieldValue(packet, CID_KEY)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}


if (cid == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Get S2S connection state")
	Command.addInstructions(result, "Fill out this form to get S2S connection state")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
						  "hidden")
	Command.addFieldValue(result, CID_KEY, cid ?: "", "text-single",
						  "S2S connection CID")

	return result
}

def conns = [ ]
def cidConns = (Map) cidConnections

CID c = new CID(cid)
CIDConnections con = cidConns.get(c)

if (con == null) {
	conns += "No such CID connection found"
} else {
	conns += cid
	conns += "out in progress: " + con.getOutgoingInProgress()
	conns += "waiting: " + con.getWaitingCount()
	conns += "control: " + con.getWaitingControlCount()
	conns += "incoming: " + con.getIncomingCount()
	conns += "outgoing: " + con.getOutgoingCount()
	conns += "out-handshaking: " + con.getOutgoingHandshakingCount()
}

def result = p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(result, "S2S connection", conns);
return result
