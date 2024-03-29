/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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

Get any file

AS:Description: Get any file
AS:CommandId: get-any-file
AS:Component: message-router
*/

package tigase.admin

import tigase.server.Command
import tigase.server.Packet

def p = (Packet) packet
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def FILE_PATH = "file";

def filepath = Command.getFieldValue(p, FILE_PATH);

def result = p.commandResult(filepath ? Command.DataType.result : Command.DataType.form);

if (!isServiceAdmin) {
	Command.addTextField(result, "Error", "You are not service administrator");
} else if (filepath == null) {
	Command.addFieldValue(result, FILE_PATH, "", "text-single", "File");
} else {
	if (filepath == null) {
		Command.addTextField(result, "Error", "File not specified");
	} else {
		def file = new File(filepath);
		if (file.exists()) {
			def lines = [ ];
			file.eachLine { line -> lines += line; };
			Command.addFieldMultiValue(result, "Content", lines);
		} else {
			Command.addTextField(result, "Error", "File not found");
		}
	}
}

return result;
