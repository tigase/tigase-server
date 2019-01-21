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
package tigase.admin

val num1 = Command getFieldValue (packet, "num1")
val num2 = Command getFieldValue (packet, "num2")

scriptResult = if (num1 == null || num2 == null) {
  val cmd = Packet.commandResultForm (packet)
  Command.addTextField (cmd, "Note", "This is a Scala script!")
  Command.addFieldValue (cmd, "num1", "")
  Command.addFieldValue (cmd, "num2", "")
  cmd
} else {
  num1 + num2
}
