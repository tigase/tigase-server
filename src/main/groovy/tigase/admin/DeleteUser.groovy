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
 User delete script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#delete-user
 AS:Description: Delete user
 AS:CommandId: http://jabber.org/protocol/admin#delete-user
 AS:Component: sess-man
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*

def JIDS = "accountjids"

def p = (Packet)packet
def auth_repo = (AuthRepository)authRepository
def user_repo = (UserRepository)userRepository
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def userJids = Command.getFieldValues(packet, JIDS)

if (userJids == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Deleting a User")
	Command.addInstructions(result, "Fill out this form to delete a user.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")
	Command.addFieldValue(result, JIDS, userJids ?: "", "jid-multi",
			"The Jabber ID(s) to delete")

	return result
}

def result = p.commandResult(Command.DataType.result)
def msgs = [];
def errors = [];
for (userJid in userJids) {
	try {
		bareJID = BareJID.bareJIDInstance(userJid)
		VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
		if (isServiceAdmin ||
		(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {
			if (user_repo.userExists(bareJID)) {
				auth_repo.removeUser(bareJID)
				msgs.add("Operation successful for user "+userJid);
			}
			else {
				errors.add("User "+userJid+" not exists, can't be deleted.");
			}
		} else {
			errors.add("You do not have enough permissions to delete accounts for domain "+userJid.getDomain()+".");
		}
	} catch (UserNotFoundException ex) {
		errors.add("User "+userJid+" not exists, can't be deleted.");
	} catch (TigaseDBException ex) {
		errors.add("Problem accessing database, user "+userJid.toString()+" not deleted.");
	}
}

if (!msgs.isEmpty())
	Command.addFieldMultiValue(result, "Notes", msgs);

if (!errors.isEmpty())
	Command.addFieldMultiValue(result, "Errors", errors);
return result
