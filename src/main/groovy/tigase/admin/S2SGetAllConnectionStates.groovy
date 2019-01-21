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
 Script lists all CID connections.
 AS:Description: S2S State of All Connections
 AS:CommandId: s2s-all-conns-state
 AS:Component: s2s
 */

package tigase.admin

import tigase.server.Command
import tigase.server.Packet
import tigase.server.xmppserver.CIDConnections

def p = (Packet) packet
def cidConns = (Map) cidConnections

def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}

def conns = [ ]

conns += "Total count: " + cidConns.size() + ":"

cidConns.entrySet().each {
	CIDConnections con = it.getValue()
	conns += it.getKey().toString() + ", out in progress: " + con.getOutgoingInProgress() + ", waiting: " +
			con.getWaitingCount() + ", control: " + con.getWaitingControlCount() + ", incoming: " +
			con.getIncomingCount() + ", outgoing: " + con.getOutgoingCount() + ", out-handshaking: " +
			con.getOutgoingHandshakingCount()
}

def result = p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(result, "Connections with waiting packets", conns);
return result
