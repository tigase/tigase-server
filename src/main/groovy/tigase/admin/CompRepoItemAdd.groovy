/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev: $
 * Last modified by $Author: $
 * $Date: $
 */

/*

Add an item in a component repository:
tigase.db.ComponentRepository
Works only for some components which actually use the repository that way.

AS:Description: Add component repo item
AS:CommandId: comp-repo-item-add
AS:Component: vhost-man,ext
*/

package tigase.admin

import tigase.db.*
import tigase.server.*

def MARKER = "command-marker"

def repo = (ComponentRepository)comp_repo
def p = (Packet)packet
def item = repo.getItemInstance()
def marker = Command.getFieldValue(packet, MARKER)

if (marker == null) {
  def result = p.commandResult(Command.DataType.form)
  item.addCommandFields(result)
	Command.addHiddenField(result, MARKER, MARKER)
	return result
}

item.initFromCommand(packet)
repo.addItem(item)

def result = p.commandResult(Command.DataType.result)
Command.addTextField(result, "Note", "Operation successful");

return result
