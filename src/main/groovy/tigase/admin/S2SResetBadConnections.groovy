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
 Script tries to find S2S connection in a bad state and resets their state
 AS:Description: S2S Reset Bad State Connections
 AS:CommandId: s2s-reset-bad-state-conns
 AS:Component: s2s
 */

package tigase.admin

import tigase.server.Command
import tigase.server.Packet
import tigase.server.xmppserver.CIDConnections

def cidConns = (Map) cidConnections

def p = (Packet) packet
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}


def conns = [ ]

conns += "Total count: " + cidConns.size()

cidConns.entrySet().each {
	CIDConnections con = it.getValue()
	// Bad state is when the OutgoingInProgress is set to true but there is no outgoing
	// or outgoing handshaking connections.
	if (con.getOutgoingInProgress() && (con.getOutgoingCount() == 0) && (con.getOutgoingHandshakingCount() == 0)) {
		conns += it.getKey().toString() + ", waiting: " + con.getWaitingCount()
		con.resetOutgoingInProgress()
	}
}

def result = p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(result, "Reset connections", conns);
return result
