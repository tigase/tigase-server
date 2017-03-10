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
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 *
 */
/*
 Retrieves from the server specified number of top active users
 AS:Description: Get top active users
 AS:CommandId: http://jabber.org/protocol/admin#get-top-active-users
 AS:Component: sess-man
 AS:Group: Statistics
 */

package tigase.admin

import tigase.server.*
import tigase.xmpp.*

def TOP_NUM = "top-num"

def p = (Packet)packet

def topNum = Command.getFieldValue(packet, TOP_NUM)

if (topNum == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Get top active users")
	Command.addInstructions(result, "Fill out this form to get top active users.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")
	Command.addFieldValue(result, TOP_NUM, topNum ?: "10", "text-single",
			"Number of top active users to show")

	return result
}

def user_sessions = (Map)userSessions

def mc= [
			compare: {XMPPSession a, XMPPSession b->
				a.getPacketsCounter() == b.getPacketsCounter() ? 0: a.getPacketsCounter()>b.getPacketsCounter()? -1: 1
			}
		] as Comparator

def sessions = []
// TODO: this is memory inefficient way to do it. We need to find a more memory friendly way
user_sessions.entrySet().each {
	if (!it.getKey().toString().startsWith("sess-man")) {
		sessions += it.getValue()
	}
}

sessions.sort(mc)
def max = topNum.toInteger()
if (max > sessions.size() ) {
	max = sessions.size()
}

def usr_list = []

sessions[0..(max-1)].each { XMPPSession it ->
	usr_list += it.getJIDs()[0].toString() + " online " + (it.getLiveTime()/1000) + " sec, " + it.getPacketsCounter() + " packets"
}

def result = p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(result, "Top active users ", usr_list);
return result


