/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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
 User modify script

 AS:Description: Modify user
 AS:CommandId: modify-user
 AS:Component: sess-man
 AS:Group: Users
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*

def JID = "accountjid"
def EMAIL = "email"

def p = (Packet)packet
def auth_repo = (AuthRepository)authRepository
def user_repo = (UserRepository)userRepository
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def userJid = Command.getFieldValue(packet, JID)
def userEmail = Command.getFieldValue(packet, EMAIL)

if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

	//Command.addFieldValue(result, "FORM_TYPE", "", "hidden")
	Command.addTitle(result, "Modifying a User")
	Command.addInstructions(result, "Fill out this form to modify a user.")

	Command.addFieldValue(result, JID, userJid ?: "", "jid-single",
			"The Jabber ID for the account to be modified")

	return result
}

def result = null;
try {
	bareJID = BareJID.bareJIDInstance(userJid)
	VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
	if (isServiceAdmin ||
		(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {

		if (Command.getFieldValue(packet, "FORM_TYPE") == null || Command.getFieldValue(packet, "FORM_TYPE").isEmpty()) {
		//if (Command.getFieldValue(packet, EMAIL) == null)
			result = p.commandResult(Command.DataType.form);

			Command.addTitle(result, "Modifying a User")
			Command.addInstructions(result, "Fill out this form to modify a user " + (userJid ?: ""))

			Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
					"hidden")
			Command.addFieldValue(result, JID, userJid ?: "", "hidden")	
			Command.addFieldValue(result, EMAIL, user_repo.getData(bareJID, "email") ?: "", "text-single",
					"Email address")
			
			Command.addCheckBoxField(result, "Account enabled", !authRepository.isUserDisabled(bareJID));
//			-- add disabled/enabled? vcard? roster?
		} else {
			result = p.commandResult(Command.DataType.result);
			user_repo.setData(bareJID, "email", userEmail);
			Command.addTextField(result, "Note", "Operation successful");
			try {
				authRepository.setUserDisabled(bareJID, !Command.getCheckBoxFieldValue(p, "Account enabled"));
			} catch (TigaseDBException ex) {
				Command.addTextField(result, "Warning", "Account state was not changed as it is not supported by used auth repository: " + ex.getMessage());
			}
		}
	} else {
		result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Error", "You do not have enough permissions to create account for this domain.");	
	}
} catch (TigaseDBException ex) {
	result = p.commandResult(Command.DataType.result)
	Command.addTextField(result, "Note", "Problem accessing database, user not added.");
}

return result