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

Update an item in a component repository:
tigase.db.ComponentRepository
Works only for some components which actually use the repository that way.

AS:Description: Update component repo item
AS:CommandId: comp-repo-item-update
AS:Component: vhost-man,ext
*/

package tigase.admin

import tigase.db.*
import tigase.server.*

def MARKER = "command-marker"
def ITEMS = "item-list"

def repo = (ComponentRepository)comp_repo
def p = (Packet)packet
def itemKey = Command.getFieldValue(packet, ITEMS)
def marker = Command.getFieldValue(packet, MARKER)

if (itemKey == null) {
	def items = repo.allItems()
	if (items.size() > 0) {
		def result = p.commandResult(Command.DataType.form)
		def itemsStr = []
		items.each { itemsStr += it.getKey() }
		Command.addFieldValue(result, ITEMS, itemsStr[0], "List of items",
			(String[])itemsStr, (String[])itemsStr)
		return result
	} else {
		def result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Note", "There are no items on the list");
		return result
	}
}

if (marker == null) {
  def result = p.commandResult(Command.DataType.form)
	def item = repo.getItem(itemKey)
  item.addCommandFields(result)
	Command.addHiddenField(result, MARKER, MARKER)
	Command.addHiddenField(result, ITEMS, ITEMS)
	return result
}

def item = repo.getItemInstance()
item.initFromCommand(packet)
repo.addItem(item)

def result = p.commandResult(Command.DataType.result)
Command.addTextField(result, "Note", "Operation successful");

return result
