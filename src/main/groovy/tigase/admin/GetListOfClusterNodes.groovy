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
/*
 Get list of all connected cluster nodes

 AS:Description: Get list of all connected cluster nodes
 AS:CommandId: cluster-nodes-list
 AS:Component: cl-comp
 AS:Group: Configuration
 */
package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.db.comp.ComponentRepository
import tigase.xml.*

def p = (Packet)packet
def result = p.commandResult(Command.DataType.result)
try {
	def nodes = component.getNodesConnectedWithLocal().collect { return it.getDomain() }
	Command.addFieldMultiValue(result, "Cluster nodes:", nodes);
} catch (Exception ex) {
	Command.addTextField(result, "Note", "Problem with retrieving list of all connected cluster nodes: " + ex.getMessage());
}
return result

