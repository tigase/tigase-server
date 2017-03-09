/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
 Get list of available adhoc commands with additional metadata.

 AS:Description: Get list available commands
 AS:CommandId: list-commands
 AS:Group: Example scripts
 AS:ComponentClass: tigase.server.BasicComponent
 */
package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xml.Element
import tigase.xmpp.*

def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def result = p.commandResult(Command.DataType.form)
Command.addTitle(result, "Retrieving list of commands");

def type = Command.getFieldValue(p, "type");
if (!type) {	
	Command.addInstructions(result, "Select a informations to retrieve")
	Command.addFieldValue(result, "type", "", "Informations to retrive", ["Groups", "Commands"].toArray(new String[2]), ["groups", "commands"].toArray(new String[2]));
	return result;
}
else if (type == "groups") {
	def groups = [];
	Command.addInstructions(result, "Select a group for which to retrieve commands");
	Command.addFieldValue(result, "type", "commands", "hidden");
	adminCommands.each { id, script -> 
		if (!component.canCallCommand(p.getStanzaFrom(), id))
			return;
		
		def group = script.getGroup();
		if (group == null) {
			group = "--";
		}
		if (!groups.contains(group))
			groups.add(group);
	}
	Command.addFieldValue(result, "group", "", "Group", groups.toArray(new String[groups.size()]), groups.toArray(new String[groups.size()]));
	return result;
}
else if (type == "commands") {
	def group = Command.getFieldValue(p, "group");
	Command.addHiddenField(result, "group", group ?: "");
	Command.addInstructions(result, "Following commands are available" + (group ? " for group $group" : ""));
	def x = Command.getData(result).find { it.getName() == "x" && it.getXMLNS() };
	def reported = new Element("reported");
	def fields = ["node", "group", "name", "jid"];
	reported.addChildren(fields.collect {
		new Element("field", ["var"].toArray(new String[1]), [it].toArray(new String[1]));	
	});
	x.addChild(reported);
	def scripts = [];
	adminCommands.each { id, script -> 
		if (!component.canCallCommand(p.getStanzaFrom(), id))
			return;
			
		if (group && !group.equals(script.getGroup()))
			return;
			
		def item = new Element("item");
		fields.each {
			def value = new Element("value");
			def field = new Element("field");
			field.setAttribute("var", it);
			field.addChild(value);
			item.addChild(field);
			
			if (it == "node") {
				value.setCData(id);
			} else if (it == "group") {
				value.setCData(script.getGroup());
			} else if (it == "name") {
				value.setCData(script.getDescription());
			} else if (it == "jid") {
				value.setCData(packet.getStanzaTo().toString());
			}
		}
		x.addChild(item);
		
		scripts.add([id:id, name:script.getDescription()]);
	}	
	
	Command.addFieldValue(result, "commands", "", "Commands", scripts.collect{ it.name }.toArray(new String[scripts.size()]), scripts.collect{ it.id }.toArray(new String[scripts.size()]));
	return result;
}