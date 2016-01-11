/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.org>
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
Script executes graceful shutdown of cluster node
AS:Description: Shutdown
AS:CommandId: http://jabber.org/protocol/admin#shutdown
AS:Component: basic-conf
AS:Group: Configuration
 */

package tigase.admin

import tigase.xml.*;
import tigase.server.*
import tigase.server.xmppserver.*
import tigase.disteventbus.EventBusFactory;

def DELAY = "delay";
def NODE = "node";
def NOTIFY = "Notify users";
def MESSAGE = "Message to users";
def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "This command is available only to administrator of this service");
	return result
}

def nodes = Command.getFieldValues(p, NODE) as List;
def notify = Command.getCheckBoxFieldValue(p, NOTIFY);
def msg = (Command.getFieldValues(p, MESSAGE) as List) ?: ["Server will be restarted.", "During restart you will be disconnected from XMPP server."];
def delay = Command.getFieldValue(p, DELAY) ?: "30";

def result = null;
if (nodes == null || nodes.isEmpty()) {
	result = p.commandResult(Command.DataType.form)
	Command.addTitle(result, "Shutting Down the Service")
	Command.addInstructions(result, "Fill out this form to shut down the service.")
	nodes = component.getNodesConnectedWithLocal().collect { it.getDomain() };
	Command.addFieldValue(result, NODE, [] as String[], "Nodes to shutdown", nodes as String[], nodes as String[])
	Command.addFieldValue(result, DELAY, delay, "Delay before node shutdown", ["30sec", "1min", "3min", "5min"] as String[], ["30", 60, "180", "300"] as String[]);
	Command.addCheckBoxField(result, NOTIFY, false);
	Command.addFieldMultiValue(result, MESSAGE, msg)
} else {
	result = p.commandResult(Command.DataType.result);
	nodes.each { node ->
		def event = new Element("shutdown", ["xmlns", "node", "delay"] as String[], ["tigase:server", node, delay] as String[]);
		if (notify && msg) {
			event.addChild(new Element("msg", msg.join("\n")));
		}
		eventBus.fire(event);
	}
	Command.addTextField(result, "Info", "Shutdown of service started");
}

return result