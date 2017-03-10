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
 */

/*
 User delete script as described in XEP-0133:
 http://jabber.org/protocol/admin#end-user-session
 AS:Description: End User Session
 AS:CommandId: http://jabber.org/protocol/admin#end-user-session
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
//def auth_repo =/**/ (AuthRepository)authRepository
//def user_repo = (UserRepository)userRepository
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

	Command.addTitle(result, "Ending session for the users")
	Command.addInstructions(result, "Fill out this form to end user(s) session(s). BareJID - will end all user sessions, FullJID - only particular session.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin",
			"hidden")
	Command.addFieldValue(result, JIDS, userJids ?: "", "jid-multi",
			"The Jabber ID(s) for which end session")
	if 	( clusterMode  ) {
		Command.addHiddenField(result, NOTIFY_CLUSTER, true.toString())
	}

	return result
}

def results = new LinkedList<Packet>();


if (clusterMode && notifyCluster) {
	def nodes = (List)clusterStrategy.getNodesConnected();
	if (nodes && nodes.size() > 0 ) {
		nodes.each { node ->
			def forward = p.copyElementOnly();
			Command.removeFieldValue(forward, NOTIFY_CLUSTER)
			Command.addHiddenField(forward, NOTIFY_CLUSTER, false.toString())
			forward.setPacketTo( node );
			forward.setPermissions( Permissions.ADMIN );

			println "forwarding: " + forward

			results.offer(forward)
		}
	}
}

def result = p.commandResult(Command.DataType.result)
results.offer(result);
def msgs = [];
def errors = [];
for (userJid in userJids) {
	try {

		JID userFullJID = JID.jidInstance(userJid)
		def sess = user_sessions.get(userFullJID.getBareJID());
		VHostItem vhost = vhost_man.getVHostItem(userFullJID.getDomain())
		if (isServiceAdmin ||
				(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {

			if (sess != null) {
				def conns = sess.getConnectionIds();
				for (conn in conns) {
					XMPPResourceConnection res = sess.getResourceForConnectionId(conn);
					if (res != null && userFullJID.getResource() == null || (userFullJID.getResource() == res.getResource())) {
						def commandClose = Command.CLOSE.getPacket(p.getStanzaTo(), conn,
								StanzaType.set, res.nextStanzaId());
						results.offer(commandClose);
						msgs.add("Operation successful for user "+res.getjid());
					}
				}
			}
		} else {
			errors.add("You do not have enough permissions to end sessions for accounts for domain "+bareJID.getDomain()+".");
		}

	} catch (Exception ex) {
		ex.printStackTrace();
	}
}

if (!msgs.isEmpty())
	Command.addFieldMultiValue(result, "Notes", msgs);

if (!errors.isEmpty())
	Command.addFieldMultiValue(result, "Errors", errors);
return results


