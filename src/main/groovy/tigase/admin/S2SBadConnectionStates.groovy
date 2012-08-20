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
 Script tries to find S2S connection in a bad state and reports connection details.
 AS:Description: S2S Bad State Connections
 AS:CommandId: s2s-bad-state-conns
 AS:Component: s2s
 */

package tigase.admin

import tigase.server.*
import tigase.server.xmppserver.*

def p = (Packet)packet
def cidConns = (Map)cidConnections

def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}

def conns = []

conns += "Total count: " + cidConns.size()

cidConns.entrySet().each {
	CIDConnections con = it.getValue()
	if (con.getWaitingCount() > 0) {
		conns += it.getKey().toString() +
				", out in progress: " + con.getOutgoingInProgress() +
				", waiting: " + con.getWaitingCount() +
				", control: " + con.getWaitingControlCount() +
				", incoming: " + con.getIncomingCount() +
				", outgoing: " + con.getOutgoingCount() +
				", out-handshaking: " + con.getOutgoingHandshakingCount()
	}
}

def result = p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(result, "Connections with waiting packets", conns);
return result
