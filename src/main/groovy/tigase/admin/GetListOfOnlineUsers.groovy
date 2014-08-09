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
 Get list of online users script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#get-online-users-list

 AS:Description: Get list of online users
 AS:CommandId: http://jabber.org/protocol/admin#get-online-users-list
 AS:Component: sess-man
 AS:Group: Statistics
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*

def JID = "domainjid"
def MAX_ITEMS = "max_items"

def p = (Packet)packet
def auth_repo = (AuthRepository)authRepository
def user_repo = (UserRepository)userRepository
def users_sessions = (Map)userSessions
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def domainJid = Command.getFieldValue(packet, JID);
def maxItemsStr = Command.getFieldValue(packet, MAX_ITEMS);

if (domainJid == null || maxItemsStr == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Requesting List of Online Users")
	Command.addInstructions(result, "Fill out this form to request the online users\nof this service.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")

//	if (isServiceAdmin) {
	//Command.addFieldValue(result, JID, domainJid ?: "", "jid-single",
	//		"The domain for the list of online users")
//	}
//	else {
		def vhosts = [];
		vhost_man.repo.allItems().each {
			if (it.isOwner(stanzaFromBare.toString()) || it.isAdmin(stanzaFromBare.toString()) || isServiceAdmin) {
				vhosts += it.getVhost().toString()
			}
		}
		vhosts = vhosts.sort();
		def vhostsArr = vhosts.toArray(new String[vhosts.size()]);
		Command.addFieldValue(result, JID, "", "The domain for the list of online users", vhostsArr, vhostsArr);
//	}

	Command.addFieldValue(result, MAX_ITEMS, maxItemsStr ?: "", "Maximum number of items to show", ["25", "50", "75", "100", "150", "200", "None"].toArray(new String[7]),  ["25", "50", "75", "100", "150", "200", "None"].toArray(new String[7]));

	return result
}

def result = p.commandResult(Command.DataType.result)
try {
	def maxItems = maxItemsStr ?  (maxItemsStr == "None" ? null : Integer.parseInt(maxItemsStr)) : 25;

	bareJID = BareJID.bareJIDInstance(domainJid)
	VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
	if (isServiceAdmin ||
	(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {
		def users_list = [];
		users_sessions.entrySet().each {
			if (!it.getKey().toString().startsWith("sess-man") && it.getKey().getDomain().equals(bareJID.getDomain())) {

				if (!maxItems || users_list.size() < maxItems) {
					def user = it.getKey().toString();
					def session = it.getValue();
					user += " (" + session.getActiveResourcesSize() + ":";
					session.getJIDs().each { user += it.getResource() + ", "; }
					user = user[0..user.size() - 3] + ")";

					users_list += user;
				}
			}
		}

		Command.addFieldMultiValue(result, "Users: "+users_list.size(), users_list);
	} else {
		Command.addTextField(result, "Error", "You do not have enough permissions to list online accounts for this domain.");
	}
} catch (TigaseDBException ex) {
	Command.addTextField(result, "Note", "Problem accessing database, online users not listed.");
}

return result
