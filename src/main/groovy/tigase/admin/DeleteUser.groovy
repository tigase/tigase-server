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
 User delete script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#delete-user
 AS:Description: Delete user
 AS:CommandId: http://jabber.org/protocol/admin#delete-user
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

def JIDS = "accountjids"

def p = (Packet)packet
def auth_repo = (AuthRepository)authRepository
def user_repo = (UserRepository)userRepository
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)
def NOTIFY_CLUSTER = "notify-cluster"
boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );
def notifyClusterStr = Command.getFieldValue(packet, NOTIFY_CLUSTER);
boolean notifyCluster = (notifyClusterStr != null) ? Boolean.valueOf( notifyClusterStr ) : true;

def user_sessions = (Map)userSessions;

def userJids = Command.getFieldValues(packet, JIDS)

if (userJids == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Deleting a User")
	Command.addInstructions(result, "Fill out this form to delete a user.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")
	Command.addFieldValue(result, JIDS, userJids ?: "", "jid-multi",
			"The Jabber ID(s) to delete")
	if 	( clusterMode  ) {
		Command.addHiddenField(result, NOTIFY_CLUSTER, true.toString())
	}

	return result
}

def results = new LinkedList<Packet>();

def closeUserSessions = { userJid ->
	try {
		def bareJID = BareJID.bareJIDInstance(userJid)
		def sess = user_sessions.get(bareJID);
		if (sess != null) {
			def conns = sess.getConnectionIds();
			for (conn in conns) {
				def res = sess.getResourceForConnectionId(conn);
				if (res != null) {
					def commandClose = Command.CLOSE.getPacket(p.getStanzaTo(), conn,
							StanzaType.set, res.nextStanzaId());
					results.offer(commandClose);
				}
			}
		}
	} catch (Exception ex) {
		ex.printStackTrace();
	}
};

if (clusterMode) {
	if (!notifyCluster) {
		for (userJid in userJids) {
			closeUserSessions(userJid);
		}
		return results;
	}
}


def result = p.commandResult(Command.DataType.result)
results.offer(result);
def msgs = [];
def errors = [];
for (userJid in userJids) {
	try {
		def bareJID = BareJID.bareJIDInstance(userJid)
		VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
		if (isServiceAdmin ||
		(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {
			if (user_repo.userExists(bareJID)) {
				auth_repo.removeUser(bareJID)
				try {
					user_repo.removeUser(bareJID)
				} catch (UserNotFoundException ex) {
					// We ignore this error here. If auth_repo and user_repo are in fact the same
					// database, then user has been already removed with the auth_repo.removeUser(...)
					// then the second call to user_repo may throw the exception which is fine.
				}
				if (clusterMode && notifyCluster) {
					def nodes = (List)clusterStrategy.getNodesConnected();
					if (nodes && nodes.size() > 0 ) {
						nodes.each { node ->
							def forward = p.copyElementOnly();
							Command.removeFieldValue(forward, NOTIFY_CLUSTER)
							Command.addHiddenField(forward, NOTIFY_CLUSTER, false.toString())
							forward.setPacketTo( node );
							forward.setPermissions( Permissions.ADMIN );
							
							results.offer(forward)
						}
					}	
				} 
				closeUserSessions(userJid);

				msgs.add("Operation successful for user "+userJid);
			}
			else {
				errors.add("User "+userJid+" not found, can't be deleted.");
			}
		} else {
			errors.add("You do not have enough permissions to delete accounts for domain "+bareJID.getDomain()+".");
		}
	} catch (UserNotFoundException ex) {
		errors.add("User "+userJid+" not exists, can't be deleted.");
	} catch (TigaseDBException ex) {
		errors.add("Problem accessing database, user "+userJid+" not deleted.");
	}
}

if (!msgs.isEmpty())
	Command.addFieldMultiValue(result, "Notes", msgs);

if (!errors.isEmpty())
	Command.addFieldMultiValue(result, "Errors", errors);
return results


