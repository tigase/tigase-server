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
 */

/*
Persists all items to the repository
tigase.db.ComponentRepository
Works only for some components which actually use the repository that way.

@Deprecated
AS:Description: Persist item configuration
AS:CommandId: comp-repo-item-persist
AS:Component: vhost-man,ext,basic-conf
AS:ComponentClass: tigase.server.ext.ComponentProtocol
AS:Group: Configuration
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

Queue results = new LinkedList()

def result = p.commandResult(Command.DataType.result)

if (isServiceAdmin) {
	repo.allItems().each { it -> repo.addItem(it) }
	Command.addTextField(result, "Note", "Operation successful");
} else {
	Command.addTextField(result, "Error", "You have not enough permission to change the item.")
}

results.add(result);
return results;
