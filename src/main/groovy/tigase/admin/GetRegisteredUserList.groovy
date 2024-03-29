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
 Get registered user list script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#get-registered-users-list

 AS:Description: Get registered user list
 AS:CommandId: http://jabber.org/protocol/admin#get-registered-users-list
 AS:Component: sess-man
 AS:Group: Users
 */

package tigase.admin

import tigase.db.AuthRepository
import tigase.db.TigaseDBException
import tigase.db.UserRepository
import tigase.server.Command
import tigase.server.Packet
import tigase.vhosts.VHostManagerIfc
import tigase.xmpp.jid.BareJID

def JID = "domainjid"
def MAX_ITEMS = "max_items"

def p = (Packet) packet
def auth_repo = (AuthRepository) authRepository
def user_repo = (UserRepository) userRepository
def vhost_man = (VHostManagerIfc) vhostMan
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def domainJid = Command.getFieldValue(packet, JID);
def maxItemsStr = Command.getFieldValue(packet, MAX_ITEMS);

if (domainJid == null || maxItemsStr == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Requesting List of Registered Users")
	Command.addInstructions(result, "Fill out this form to request the registered users \n of this service.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
						  "hidden")
//	if (isServiceAdmin) {
	//Command.addFieldValue(result, JID, domainJid ?: "", "jid-single",
	//		"The domain for the list of online users")
//	}
//	else {
	def vhosts = [ ];
	vhost_man.repo.allItems().each {
		if (isAllowedForDomain.apply(it.getVhost().toString())) {
			vhosts += it.getVhost().toString()
		}
	}
	vhosts = vhosts.sort();
	def vhostsArr = vhosts.toArray(new String[vhosts.size()]);
	Command.addFieldValue(result, JID, "", "The domain for the list of registered users", vhostsArr, vhostsArr);
//	}

	Command.addFieldValue(result, MAX_ITEMS, maxItemsStr ?: "", "Maximum number of items to show",
						  [ "25", "50", "75", "100", "150", "200", "None" ].toArray(new String[7]),
						  [ "25", "50", "75", "100", "150", "200", "None" ].toArray(new String[7]));

	return result
}

def result = p.commandResult(Command.DataType.result)
try {
	def maxItems = maxItemsStr ? (maxItemsStr == "None" ? null : Integer.parseInt(maxItemsStr)) : 25;

	def bareJID = BareJID.bareJIDInstance(domainJid)
	if (isAllowedForDomain.apply(bareJID.getDomain())) {
		def users_list = [ ];
		def domain_user_repo = (user_repo instanceof tigase.db.UserRepositoryMDImpl) ? user_repo.getRepo(
				bareJID.getDomain()) : user_repo;
		def users = domain_user_repo.getUsers();
		for (user in users) {
			if (!user.getDomain().equals(bareJID.getDomain())) {
				continue
			};

			users_list.add(user.toString());
			if (maxItems && users_list.size() > maxItems) {
				break
			};
		}

		Command.addFieldMultiValue(result, "Users: " + users_list.size(), users_list);
	} else {
		Command.addTextField(result, "Error", "You do not have enough permissions to list accounts for this domain.");
	}
} catch (TigaseDBException ex) {
	Command.addTextField(result, "Note", "Problem accessing database, users not listed.");
}

return result
