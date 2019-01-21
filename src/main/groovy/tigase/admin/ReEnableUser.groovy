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
 User modify script

 AS:Description: Re-Enable User
 AS:CommandId: http://jabber.org/protocol/admin#reenable-user
 AS:Component: sess-man
 AS:Group: Users
 */

package tigase.admin

import tigase.db.AuthRepository
import tigase.db.UserRepository
import tigase.db.TigaseDBException
import tigase.db.UserNotFoundException
import tigase.server.Command
import tigase.server.Packet
import tigase.server.Permissions
import tigase.vhosts.VHostManagerIfc
import tigase.xmpp.StanzaType
import tigase.xmpp.jid.BareJID

def JIDS = "accountjids"

def p = (Packet) packet
def auth_repo = (AuthRepository) authRepository
def user_repo = (UserRepository) userRepository
def vhost_man = (VHostManagerIfc) vhostMan
def admins = (Set) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)
def NOTIFY_CLUSTER = "notify-cluster"
boolean clusterMode = Boolean.valueOf(System.getProperty("cluster-mode", false.toString()))
def notifyClusterStr = Command.getFieldValue(packet, NOTIFY_CLUSTER)
boolean notifyCluster = (notifyClusterStr != null) ? Boolean.valueOf(notifyClusterStr) : true

def user_sessions = (Map) userSessions

def userJids = Command.getFieldValues(packet, JIDS)

if (userJids == null) {
	def result = p.commandResult(Command.DataType.form)

	Command.addTitle(result, "Re-enabling a User")
	Command.addInstructions(result, "Fill out this form to re-enable a user.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
						  "hidden")
	Command.addFieldValue(result, JIDS, userJids ?: "", "jid-multi",
						  "The Jabber ID(s) to re-enable")
	if (clusterMode) {
		Command.addHiddenField(result, NOTIFY_CLUSTER, true.toString())
	}

	return result
}

def results = new LinkedList<Packet>()

def closeUserSessions = { userJid ->
	try {
		def bareJID = BareJID.bareJIDInstance(userJid)
		def sess = user_sessions.get(bareJID)
		if (sess != null) {
			def conns = sess.getConnectionIds()
			for (conn in conns) {
				def res = sess.getResourceForConnectionId(conn)
				if (res != null) {
					def commandClose = Command.CLOSE.getPacket(p.getStanzaTo(), conn,
															   StanzaType.set, res.nextStanzaId())
					results.offer(commandClose)
				}
			}
		}
	} catch (Exception ex) {
		ex.printStackTrace()
	}
}

if (clusterMode) {
	if (!notifyCluster) {
		for (userJid in userJids) {
			closeUserSessions(userJid)
		}
		return results
	}
}

if (clusterMode && notifyCluster) {
	def nodes = (List) clusterStrategy.getNodesConnected()
	if (nodes && nodes.size() > 0) {
		nodes.each { node ->
			def forward = p.copyElementOnly()
			Command.removeFieldValue(forward, NOTIFY_CLUSTER)
			Command.addHiddenField(forward, NOTIFY_CLUSTER, false.toString())
			forward.setPacketTo(node)
			forward.setPermissions(Permissions.ADMIN)

			results.offer(forward)
		}
	}
}

def result = p.commandResult(Command.DataType.result)
def msgs = [ ]
def errors = [ ]
for (userJid in userJids) {
	try {
		def bareJID = BareJID.bareJIDInstance(userJid)
		if (isAllowedForDomain.apply(bareJID.getDomain())) {
			if (user_repo.userExists(bareJID)) {
				try {
					auth_repo.setAccountStatus(bareJID, AuthRepository.AccountStatus.active)
				} catch (TigaseDBException ex) {
					errors.add("Account " + userJid + " was not re-enabled: " + ex.getMessage())
				}
				closeUserSessions(userJid)

				msgs.add("Operation successful for user " + userJid)
			} else {
				msgs.add("User " + userJid + " doesn't exist")
			}
		} else {
			errors.add(
					"You do not have enough permissions to re-enable accounts for domain " + bareJID.getDomain() + ".")
		}
	} catch (UserNotFoundException ex) {
		errors.add("User " + userJid + " not exists, can't be re-enabled.")
	} catch (TigaseDBException ex) {
		errors.add("Problem accessing database, user " + userJid + " not re-enabled.")
	}
}

if (!msgs.isEmpty()) {
	Command.addFieldMultiValue(result, "Notes", msgs)
}

if (!errors.isEmpty()) {
	Command.addFieldMultiValue(result, "Errors", errors)
}

results.offer(result)

return results

