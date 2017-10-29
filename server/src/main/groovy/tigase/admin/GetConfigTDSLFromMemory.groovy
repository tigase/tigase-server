/*
 * GetConfigTDSLFromMemory.groovy
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

Get config.tdsl configuration from memory.

AS:Description: Get config.tdsl configuration
AS:CommandId: get-config-tdsl
AS:Component: basic-conf
*/

package tigase.admin

import tigase.db.comp.ComponentRepository
import tigase.server.Command
import tigase.server.Packet

def repo = (ComponentRepository) comp_repo
def p = (Packet) packet
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def result = p.commandResult(Command.DataType.result);

if (!isServiceAdmin) {
	Command.addTextField(result, "Error", "You are not service administrator");
} else {
	def lines = [ ];
	initProperties.each { key, value -> lines += key + "=" + value;
	}

	Command.addFieldMultiValue(result, "config.tdsl", lines);
}

return result;
