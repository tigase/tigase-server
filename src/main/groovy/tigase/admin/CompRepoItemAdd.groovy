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

Add an item in a component repository:
tigase.db.ComponentRepository
Works only for some components which actually use the repository that way.

AS:Description: Add new item
AS:CommandId: comp-repo-item-add
AS:Component: vhost-man,ext
AS:ComponentClass: tigase.server.ext.ComponentProtocol
 */

package tigase.admin

import tigase.db.*
import tigase.db.comp.*
import tigase.server.*

def MARKER = "command-marker"

def repo = (ComponentRepository)comp_repo
def p = (Iq)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def item = repo.getItemInstance()
def marker = Command.getFieldValue(p, MARKER)

def supportedComponents = ["vhost-man"]
def NOTIFY_CLUSTER = "notify-cluster"
boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );
boolean notifyCluster = Boolean.valueOf( Command.getFieldValue(packet, NOTIFY_CLUSTER) )
Queue results = new LinkedList()


if (marker == null) {
  def result = p.commandResult(Command.DataType.form)
  item.addCommandFields(result)
	Command.addHiddenField(result, MARKER, MARKER)
	if 	( clusterMode  ) {
		Command.addHiddenField(result, NOTIFY_CLUSTER, true.toString())
	}
	return result
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

item.initFromCommand(p)
def oldItem = item.getKey() != null ? repo.getItem(item.getKey()) : null;
def result = p.commandResult(Command.DataType.result)
if (oldItem == null) {
	def validateResult = repo.validateItem(item)
	if (validateResult == null && (isServiceAdmin && !item.getKey().isEmpty())) {
		repo.addItem(item)
		Command.addTextField(result, "Note", "Operation successful.")
		if (validateResult != null) {
			Command.addTextField(result, "Note", "   ")
			Command.addTextField(result, "Warning", validateResult)
		}
	} else {
		Command.addTextField(result, "Error", "The item did not pass validation checking.")
		Command.addTextField(result, "Note", "   ")
		Command.addTextField(result, "Warning", validateResult)
	}
} else {
	Command.addTextField(result, "Error", "The item is already added, you can't add it twice.")
}

results.add(result);
return results;
