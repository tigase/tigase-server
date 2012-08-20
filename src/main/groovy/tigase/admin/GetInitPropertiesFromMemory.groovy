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

Get init.properties configuration from memory.

AS:Description: Get init.properties configuration
AS:CommandId: get-init-properties
AS:Component: basic-conf
*/

package tigase.admin

import tigase.db.*
import tigase.db.comp.*
import tigase.server.*
import tigase.cert.*
import tigase.io.*

def repo = (ComponentRepository)comp_repo
def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def result = p.commandResult(Command.DataType.result);

if (!isServiceAdmin) {
	Command.addTextField(result, "Error", "You are not service administrator");
}
else {
	def lines = [];
	initProperties.each { key, value ->
		lines += key + "=" + value;
	}

	Command.addFieldMultiValue(result, "init.properties", lines);
}

return result;
