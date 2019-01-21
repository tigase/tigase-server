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
 Get list of online users script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#get-active-users-num

 AS:Description: Get number of active users
 AS:CommandId: http://jabber.org/protocol/admin#get-active-users-num
 AS:Component: sess-man
 AS:Group: Statistics
 */
package tigase.admin

import tigase.db.AuthRepository
import tigase.db.TigaseDBException
import tigase.db.UserRepository
import tigase.server.Command
import tigase.server.Packet
import tigase.vhosts.VHostManagerIfc
import tigase.xmpp.jid.BareJID

def SECOND = 1000;
def MINUTE = 60 * SECOND;
def JID = "domainjid"
def TIME_BEFORE_IDLE = 5 * MINUTE;

def p = (Packet) packet
def auth_repo = (AuthRepository) authRepository
def user_repo = (UserRepository) userRepository
def users_sessions = (Map) userSessions
def vhost_man = (VHostManagerIfc) vhostMan
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)
def domainJid = Command.getFieldValue(packet, JID);
if (domainJid == null) {
	def result = p.commandResult(Command.DataType.form);
	Command.addTitle(result, "Requesting Number of Active Users")
	Command.addInstructions(result, "Fill out this form to request the number of active users\nof this service.")
	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
						  "hidden")
//	if (isServiceAdmin) {
	//Command.addFieldValue(result, JID, domainJid ?: "", "jid-single",
	//		"The domain for the list of active users")
//	}
//	else {
	def vhosts = [ ];
	vhost_man.repo.allItems().each {
		if (isAllowedForDomain.apply(it.getVhost().toString())) {
			vhosts += it.getVhost().toString()
		}
	}
	def vhostsArr = vhosts.toArray(new String[vhosts.size()]);
	Command.addFieldValue(result, JID, "", "The domain for the number of active users", vhostsArr, vhostsArr);
//	}
	return result
}
def result = p.commandResult(Command.DataType.result)
try {
	bareJID = BareJID.bareJIDInstance(domainJid)
	if (isAllowedForDomain.apply(bareJID.getDomain())) {
		def users_list = [ ];
		users_sessions.entrySet().each {
			if (!it.getKey().toString().startsWith("sess-man") && it.getKey().getDomain().equals(bareJID.getDomain())) {
				//if (!maxItems || users_list.size() < maxItems) {
				def user = it.getKey().toString();
				def session = it.getValue();
				def active = false;
				session.getActiveResources().each {
					active = active || ((System.currentTimeMillis() - it.getLastAccessed()) < TIME_BEFORE_IDLE);
				}
				if (active == true) {
//						user += " (" + session.getActiveResourcesSize() + ":";
//						session.getJIDs().each { user += it.getResource() + ", "; }
//						user = user[0..user.size() - 3] + ")";
					users_list += user;
				}
				//}
			}
		}
		Command.addFieldValue(result, "activeusersnum", String.valueOf(users_list.size()), "fixed",
							  "The number of active users");
	} else {
		Command.addTextField(result, "Error",
							 "You do not have enough permissions to get number of active accounts for this domain.");
	}
} catch (TigaseDBException ex) {
	Command.addTextField(result, "Note", "Problem accessing database, active users not counted.");
}
return result
