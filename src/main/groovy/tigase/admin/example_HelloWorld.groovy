/**
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
Example Hello World admin script for the script development guide.

AS:Description: [example] Hello World Script.
AS:CommandId: hello
AS:Component: sess-man
AS:Group: Example scripts
*/

package tigase.admin

import tigase.server.Command
import tigase.server.Packet

def p = (Packet) packet

def name = Command.getFieldValue(packet, "name")

if (name == null) {
	def res = p.commandResult(Command.DataType.form)
	Command.addTitle(res, "Hello World Script")
	Command.addInstructions(res, "Please provide some details")
	Command.addFieldValue(res, "name", name ?: "", "text-single",
						  "Your name")
	return res
}

def res = p.commandResult(Command.DataType.result)
Command.addTitle(res, "Hello World Script")
Command.addInstructions(res, "Hello ${name}, how are you?")

return res
