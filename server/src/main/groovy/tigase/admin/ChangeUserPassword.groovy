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
 User change password script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#
 AS:Description: Change user password
 AS:CommandId: http://jabber.org/protocol/admin#change-user-password
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
def PASSWORD = "password"
//def PASSWORD_VERIFY = "password-verify"

def p = (Packet)packet
def auth_repo = (AuthRepository)authRepository
def user_repo = (UserRepository)userRepository
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def userJid = Command.getFieldValue(packet, JID)
def userPass = Command.getFieldValue(packet, PASSWORD)
//def userPassVer = Command.getFieldValue(packet, PASSWORD_VERIFY)

if (userJid == null || userPass == null /*|| userPassVer == null*/) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Changing a User Password")
	Command.addInstructions(result, "Fill out this form to change a user's password.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")
	Command.addFieldValue(result, JID, userJid ?: "", "jid-single",
			"The Jabber ID for this account")
	Command.addFieldValue(result, PASSWORD, userPass ?: "", "text-private",
			"The new password for this account")
//	Command.addFieldValue(result, PASSWORD_VERIFY, userPassVer ?: "", "text-private",
//			"Retype password")

	return result
}

def result = p.commandResult(Command.DataType.result)
try {
	bareJID = BareJID.bareJIDInstance(userJid)
	VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
	if (isServiceAdmin ||
	(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {
		if (user_repo.userExists(bareJID)) {
			auth_repo.updatePassword(bareJID, userPass)
			Command.addTextField(result, "Note", "Operation successful");
		}
		else {
			Command.addTextField(result, "Note", "User not exists, can't change password.");
		}
	} else {
		Command.addTextField(result, "Error", "You do not have enough permissions to change account password for this domain.");
	}
} catch (UserNotFoundException ex) {
	Command.addTextField(result, "Note", "User not exists, can't change password.");
} catch (TigaseDBException ex) {
	Command.addTextField(result, "Note", "Problem accessing database, password not changed.");
}

return result
