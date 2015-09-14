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

4.23 Send Announcement to Online Users as described in XEP-0133:
http://xmpp.org/extensions/xep-0133.html#announce

AS:Description: Send Announcement to Online Users
AS:CommandId: http://jabber.org/protocol/admin#announce
AS:Component: sess-man
AS:Group: Notifications
*/

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.cluster.*;
import tigase.cluster.api.*;
import tigase.cluster.strategy.*;


def FROM_JID = "from-jid"
def SUBJECT = "subject"
def MSG_TYPE = "msg-type"
def MSG_BODY = "announcement"

def p = (Iq)packet

def fromJid = Command.getFieldValue(p, FROM_JID)
def subject = Command.getFieldValue(p, SUBJECT)
def msg_type = Command.getFieldValue(p, MSG_TYPE)
def body = Command.getFieldValues(p, MSG_BODY)

def NOTIFY_CLUSTER = "notify-cluster"
boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );
boolean notifyCluster = Boolean.valueOf( Command.getFieldValue(packet, NOTIFY_CLUSTER) )

if (fromJid == null || subject == null || msg_type == null || body == null) {
	def res = (Iq)p.commandResult(Command.DataType.form);
  Command.addTitle(res, "Message to online users")
  Command.addInstructions(res, "Fill out this form to make an announcement to all active users of this service.")

  Command.addFieldValue(res, "FORM_TYPE", "http://jabber.org/protocol/admin", "hidden")

  Command.addFieldValue(res, FROM_JID, fromJid ?: p.getStanzaFrom().getDomain().toString(), "jid-single", "From address")

  Command.addFieldValue(res, SUBJECT, subject ?: "Message from administrators", "text-single", "Subject")

  def msg_types = ["normal", "headline", "chat" ]
  Command.addFieldValue(res, MSG_TYPE, msg_type ?: msg_types[0], "Type", (String[])msg_types, (String[])msg_types)

  if (body == null) {
     body = [""]
  }

	Command.addFieldMultiValue(res, MSG_BODY, body as List)

	if 	( clusterMode  ) {
		Command.addHiddenField(res, NOTIFY_CLUSTER, true.toString())
	}


	return res
}

Queue results = new LinkedList()
if 	( clusterMode && notifyCluster ) {
	if ( null != clusterStrategy ) {
		def cluster = (ClusteringStrategyIfc) clusterStrategy
		List<JID> cl_conns = cluster.getNodesConnected()
		if (cl_conns && cl_conns.size() > 0) {
			cl_conns.each { node ->

				def forward = p.copyElementOnly();
				Command.removeFieldValue(forward, NOTIFY_CLUSTER)
				Command.addHiddenField(forward, NOTIFY_CLUSTER, false.toString())
				forward.setPacketTo( node );
				forward.setPermissions( Permissions.ADMIN );

				results.offer(forward)
			}
		}
	}
}


def jidFrom = JID.jidInstanceNS(fromJid)
def type = StanzaType.valueOf(msg_type)
def msg_body = body.join('\n')

def msg = Message.getMessage(null, null, type, msg_body, subject, null, "admin")
def result = p.commandResult(Command.DataType.result)
Command.addTextField(result, "Note", "Operation successful");
results += result
def conns = (Map)userConnections
conns.each { key, value ->
  if (value.isAuthorized()) {
		def res = msg.copyElementOnly()
		res.initVars(jidFrom, value.getJID())
		res.setPacketTo(key)
		results += res
	}

}

return (Queue)results
