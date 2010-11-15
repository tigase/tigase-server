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
List users IDs connected to the server

AS:Description: List of online users
AS:CommandId: users-list
AS:Component: sess-man
*/

package tigase.admin

import tigase.conf.*
import tigase.server.*
import tigase.xmpp.*

def user_sessions = (Map)userSessions
def p = (Iq)packet

def users_list = []
user_sessions.entrySet().each {
  if (!it.getKey().toString().startsWith("sess-man")) {
    def user = it.getKey().toString()
    def session = (XMPPSession) it.getValue()
    user += "  (" + session.getActiveResourcesSize() + ": "
    session.getJIDs().each { user += it.getResource() + ", " }
    user = user[0..user.size()-3] + ")"
    users_list += user
  }
}

def res = (Iq)p.commandResult(Command.DataType.result)
Command.addFieldMultiValue(res, "Users: " + users_list.size(), users_list)
return res
