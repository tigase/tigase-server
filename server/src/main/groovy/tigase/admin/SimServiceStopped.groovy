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

Simulate service stopped uncontrolled execution to help with testing weird network
problems.

AS:Description: Simulate serviceStopped method call
AS:CommandId: sim-serv-stopped
AS:Component: cl-comp
*/

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.net.*
import tigase.cluster.*

def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}

def KEY = "key"

def key = Command.getFieldValue(packet, KEY)

if (key == null) {
	def result = p.commandResult(Command.DataType.form);
  Command.addTitle(result, "Simulate serviceStopped method call")
	Command.addInstructions(result, "Provide a key for IOService you wish to test.")
	Command.addFieldValue(result, KEY, key ?: "", "text-single",  "Key")

	return result
}

Map services = (Map)servicesMap

IOService serv = services.get(key)

if (serv == null) {
	return "IOService for key: ${key} not found!"
} else {
  ClusterConnectionManager clCM = (ClusterConnectionManager)clusterCM
	clCM.serviceStopped(serv)
	return "serviceStopped called for IOService for key: ${key}."
}
