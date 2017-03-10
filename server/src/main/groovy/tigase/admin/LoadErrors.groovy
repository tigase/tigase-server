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
Load errors caught by the server during execution:

AS:Description: Load errors
AS:CommandId: load-errors
AS:Component: monitor
*/

package tigase.admin

import tigase.server.*
import tigase.xmpp.*
import tigase.util.*

def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

if (!isServiceAdmin) {
	def result = p.commandResult(Command.DataType.result);
	Command.addTextField(result, "Error", "You are not service administrator");
	return result
}

def result = []

def size = LogFormatter.errors.size()
if (size > 0) {
	LogFormatter.errors.each {
		def entry = it.getValue()
		result += "";
		result += entry.getMessage() + ": " + entry.getCounter()
		entry.getRecord().split("\n").each {
			result += it
		}
	}
} else {
	result += "No errors so far!"
}

def res = p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(res, "Errors: " + size, result)
return res
