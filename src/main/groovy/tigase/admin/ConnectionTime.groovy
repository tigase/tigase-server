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
 Calculates maximum and average connection time for all connected users
 AS:Description: Connections time
 AS:CommandId: connection-time
 AS:Component: sess-man
 */

package tigase.admin

import tigase.conf.*
import tigase.server.*
import tigase.xmpp.*

def user_connections = (Map)userConnections
def p = (Iq)packet

def total_time = 0
def max_time = 0
def start_time = System.currentTimeMillis()
user_connections.entrySet().each {
	if (!it.getKey().toString().startsWith("sess-man")) {
		def session = (XMPPResourceConnection) it.getValue()
		def creation_time = start_time - session.getCreationTime()
		total_time += creation_time
		if (creation_time  > max_time) {
			max_time = creation_time
		}
	}
}
def average_time = total_time / user_connections.size()


def res = (Iq)p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(res, "Connections time: ",
		[
			"Longest connection: " + (max_time / 1000),
			"Average connection time:" + (average_time / 1000)
		])
return res
