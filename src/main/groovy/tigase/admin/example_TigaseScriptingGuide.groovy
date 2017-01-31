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

This is an example script for Tigase scripting support.

AS:Description: [example] Tigase scripting guide
AS:CommandId: groovy-example
AS:Component: sess-man
AS:Group: Example scripts
*/

package tigase.admin

import tigase.server.Command
import tigase.server.Packet

Packet p = (Packet)packet
num1 = Command.getFieldValue(p, "num1")
num2 = Command.getFieldValue(p, "num2")

if (num1 == null || num2 == null) {
	Packet res = p.commandResult(Command.DataType.form)
	Command.addTextField(res, "Note", "This is Groovy script!")
	Command.addFieldValue(res, "num1", "", "text-single")
	Command.addFieldValue(res, "num2", "", "text-single")
	return res
}

return num1 + num2
