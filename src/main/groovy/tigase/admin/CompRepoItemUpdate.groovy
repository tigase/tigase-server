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

Update an item in a component repository:
tigase.db.ComponentRepository
Works only for some components which actually use the repository that way.

AS:Description: Update item configuration
AS:CommandId: comp-repo-item-update
AS:Component: vhost-man,ext,basic-conf
AS:ComponentClass: tigase.server.ext.ComponentProtocol
 */

package tigase.admin

import tigase.db.*
import tigase.db.comp.*
import tigase.server.*

def MARKER = "command-marker"
def ITEMS = "item-list"

def repo = (ComponentRepository)comp_repo
def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def itemKey = Command.getFieldValue(packet, ITEMS)
def marker = Command.getFieldValue(packet, MARKER)

def supportedComponents = ["vhost-man"]
def NOTIFY_CLUSTER = "notify-cluster"
boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );
boolean notifyCluster = Boolean.valueOf( Command.getFieldValue(packet, NOTIFY_CLUSTER) )
Queue results = new LinkedList()

if (itemKey == null) {
	def items = repo.allItems()
	def itemsStr = []
	if (items.size() > 0) {
		items.each {
			if (isServiceAdmin || it.isOwner(stanzaFromBare.toString()) || it.isAdmin(stanzaFromBare.toString())) {
				itemsStr += it.getKey()
			}
		}
	}
	if(itemsStr.size() > 0) {
		def result = p.commandResult(Command.DataType.form)
		Command.addFieldValue(result, ITEMS, itemsStr[0], "List of items",
			(String[])itemsStr, (String[])itemsStr)
		return result
	} else {
		def result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Error", "There are no items on the list");
		return result
	}
}

if (marker == null) {
	def item = repo.getItem(itemKey)
	if (item == null) {
		Command.addTextField(result, "Error", "No such item, update impossible.");
	} else {
		if (isServiceAdmin || item.isOwner(stanzaFromBare.toString()) || item.isAdmin(stanzaFromBare.toString())) {
			def result = p.commandResult(Command.DataType.form)
			item.addCommandFields(result)
			Command.addHiddenField(result, MARKER, MARKER)
			Command.addHiddenField(result, ITEMS, itemKey)
			if 	( clusterMode  ) {
				Command.addHiddenField(result, NOTIFY_CLUSTER, true.toString())
			}
			return result
		} else {
			def result = p.commandResult(Command.DataType.result)
			Command.addTextField(result, "Error", "You do not have enough permissions to manage this item.")
			return result
		}
	}
}

if 	( clusterMode && notifyCluster && supportedComponents.contains(componentName) ) {
	def nodes = (List)connectedNodes

	if (nodes && nodes.size() > 0 ) {
		nodes.each { node ->
			def forward = p.copyElementOnly();
			Command.removeFieldValue(forward, NOTIFY_CLUSTER)
			Command.addHiddenField(forward, NOTIFY_CLUSTER, false.toString())
			forward.setPacketTo( node );
			forward.setPermissions( Permissions.ADMIN );

			results.offer(forward)
		}
	}
}

def result = p.commandResult(Command.DataType.result)

def item = repo.getItemInstance()
item.initFromCommand(packet)
def oldItem = repo.getItem(item.getKey())

if (oldItem == null) {
	Command.addTextField(result, "Error", "The item you try to update does not exist.");
} else {
	def validateResult = repo.validateItem(item)
	if (validateResult == null) {
		if (isServiceAdmin || oldItem.isOwner(stanzaFromBare.toString())
			// we allow changes by admins, except for changing the owner of the domain.
			|| oldItem.isAdmin(stanzaFromBare.toString()) && oldItem.getOwner().equals(item.getOwner())
		) {
			repo.addItem(item)
			Command.addTextField(result, "Note", "Operation successful");
		} else {
			Command.addTextField(result, "Error", "You are not the Item owner or you have no "
				+ "enough permission to change the item.")
		}
	} else {
		Command.addTextField(result, "Error", "The item did not pass validation checking.")
		Command.addTextField(result, "Note", "   ")
		Command.addTextField(result, "Warning", validateResult)
	}
}


results.add(result);
return results;
