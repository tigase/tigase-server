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

Force stop IOService for a given key.

AS:Description: Force stop service
AS:CommandId: force-stop-service
AS:Component: cl-comp
*/

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.net.*

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
  Command.addTitle(result, "Force-stopping IOService for a given key")
	Command.addInstructions(result, "Provide a key for IOService you wish to stop.")
	Command.addFieldValue(result, KEY, key ?: "", "text-single",  "Key")

	return result
}

Map services = (Map)servicesMap

IOService serv = services.get(key)

if (serv == null) {
	return "IOService for key: ${key} not found!"
} else {
  serv.forceStop()
	return "Stopped IOService for key: ${key}."
}
