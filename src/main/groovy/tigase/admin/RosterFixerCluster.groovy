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
The roster fixer scripts is used in a case if for whatever reason user's roster got
broken, lost or otherwise messed up. If we know the user's contact list, this script
can be used to restore the contact list. It adds missing entries to the user's roster.
If the user is online, he gets a roster push with updated entries to make sure
he is up to date with all the changes.
The script accepts a user JID, action (update or remove) and a list of buddies in
a following format:
buddy_jid,buddy_name,subscription
buddy_jid is a JID (bare JID)
buddy_name is just a string, it is optional, if omit, localpart of the JID is used
subscription is one of following (none, from, to, both), it is optional, if omit 'both' is used
AS:Description: Fixes user's roster on Tigase cluster
AS:CommandId: roster-fixer-cluster
AS:Component: sess-man
 */

package tigase.admin

import java.util.Set;

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xmpp.impl.roster.*
import tigase.xml.*
import tigase.vhosts.*
import tigase.cluster.strategy.*

def ROSTER_OWNER_JID = "roster-owner-jid"

def ROSTER_BUDDY_LIST = "roster-buddy-list"

def ROSTER_ACTION = "roster-action"

def DISCONNECTED_PHASE = "disconnected-phase"

def UPDATE = "update"
def REMOVE = "remove"
def subscriptions = ["both", "from", "to", "none"]
def actions = [UPDATE, REMOVE]
def actions_descr = ["Add/Update item", "Remove item"]
//def notify_cluster = ["no", "yes"]

def p = (Packet)packet
def repository = (UserRepository)userRepository
def sessions = (Map<BareJID, XMPPSession>)userSessions
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet

def stanzaFromBare = p.getStanzaFrom().getBareJID();
def isServiceAdmin = admins.contains(stanzaFromBare);

def rosterOwnerJid = Command.getFieldValue(packet, ROSTER_OWNER_JID)
def rosterAction = Command.getFieldValue(packet, ROSTER_ACTION)
def rosterBuddyList = Command.getFieldValues(packet, ROSTER_BUDDY_LIST) as List;

//def rosterNotifyCluster = Command.getFieldValue(packet, ROSTER_NOTIFY_CLUSTER)

boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );

if (rosterOwnerJid == null || rosterBuddyList == null ||
	rosterAction == null) {
	def res = p.commandResult(Command.DataType.form);
	Command.addFieldValue(res, ROSTER_OWNER_JID, rosterOwnerJid ?: "",
			"jid-single", "Roster owner JID")
	Command.addFieldValue(res, ROSTER_ACTION, actions[0],
			"Action", (String[])actions_descr, (String[])actions)

	if (rosterBuddyList == null) {
		rosterBuddyList = [""]
	}
	Command.addFieldMultiValue(res, ROSTER_BUDDY_LIST, rosterBuddyList)

	//	Command.addFieldValue(res, ROSTER_NOTIFY_CLUSTER, notify_cluster[0],
	//    "Notify cluster", (String[])notify_cluster, (String[])notify_cluster)

	return res
}

def remove_item = rosterAction == REMOVE
def res_report = []
def jidRosterOwnerJid = JID.jidInstanceNS(rosterOwnerJid)
def Queue<Packet> results = new LinkedList<Packet>()

Packet result = p.commandResult(Command.DataType.result)
def vhost = vhost_man.getVHostItem(jidRosterOwnerJid.getDomain());
if (vhost == null ||
	(!isServiceAdmin &&
		!vhost.isOwner(stanzaFromBare.toString()) &&
		!vhost.isAdmin(stanzaFromBare.toString()))) {
	Command.addTextField(result, "Error", "You do not have enough permissions to modify roster of "+rosterOwnerJid);
	results.offer(result);
	return results;
}

def disconnected = Command.getFieldValue(p, DISCONNECTED_PHASE)

// Disconnecting all user's connections on the whole cluster

if (!disconnected) {

	def sess = sessions == null ? null : sessions.get(jidRosterOwnerJid.getBareJID())
	def online = false

	if (sess && sess.getActiveResourcesSize() > 0) {
		sess.getActiveResources().each{ conn ->
			def commandClose = Command.CLOSE.getPacket(p.getStanzaTo(), conn.getConnectionId(),
				StanzaType.set, conn.nextStanzaId())
			results.offer(commandClose)
			res_report += "User: " + conn.getjid() + " is online, disconnected."
		}
		online = true
	}

	if 	( clusterMode ) {
		if ( null != clusterStrategy ) {
			def cluster = (ClusteringStrategyIfc)clusterStrategy
			Set<ConnectionRecord> cl_conns = cluster.getConnectionRecords(jidRosterOwnerJid.getBareJID())
			if (cl_conns && cl_conns.size() > 0) {
				cl_conns.each {
					def commandClose = Command.CLOSE.getPacket(p.getStanzaTo(), it.getConnectionId(),
						StanzaType.set, "77")
					results.offer(commandClose)
					res_report += "User: " + it.getUserJid() + " is online on node: " + it.getNode() + ", disconnected."

				}
				online = true
			}
		}
	}

	if (online) {
		Command.addFieldMultiValue(p, "Report: ", res_report)
		Command.addHiddenField(p, DISCONNECTED_PHASE, DISCONNECTED_PHASE)
		results.offer(p)

		return results
	}
} else {

	Command.getFieldValues(packet, "Report: ").each {
		res_report += it
	}
}

// Waiting 2 secs

sleep(2000)

// Updating user's roster in DB

def updateRoster = { sess, online, jid, i_jid, i_name, i_subscr ->

	// Update offline
	String rosterStr = repository.getData(jid.getBareJID(), null,
		RosterAbstract.ROSTER, null) ?: ""
	Map<BareJID, RosterElement> roster = new LinkedHashMap<BareJID, RosterElement>()
	RosterFlat.parseRosterUtil(rosterStr, roster, null)
	if (remove_item) {
		roster.remove(i_jid.getBareJID())
		res_report += "Buddy: "+ i_jid + " removed"
	} else {
		if (roster.get(i_jid.getBareJID()) == null) {
			RosterElement rel = new RosterElement(i_jid, i_name, null, null)
			rel.setSubscription(RosterAbstract.SubscriptionType.valueOf(i_subscr))
			roster.put(i_jid, rel)
			res_report += "Buddy: "+ i_jid + " added to the roster"
		} else {
			res_report += "Buddy: "+ i_jid + " already in the roster, skipping"
		}
	}
	StringBuilder sb = new StringBuilder(200)
	for (RosterElement relem: roster.values())
	sb.append(relem.getRosterElement().toString())
	repository.setData(jid.getBareJID(), null, RosterAbstract.ROSTER, sb.toString());

}


def rosterItemJid = null
def sess = sessions == null ? null : sessions.get(jidRosterOwnerJid.getBareJID());
def conn = sess != null ? sess.getActiveResources().get(0) : null;
def online = true
if (conn) {
	res_report += "User: " + jidRosterOwnerJid + " is online, updating database and online connections"
} else {
	res_report += "User: " + jidRosterOwnerJid + " is offline, updating database only"
	online = false
}

rosterBuddyList.each {
	def	buddy = it.split(",")
	if (it.contains(';')) {
		buddy = it.split(";")
	}
	rosterItemJid = buddy[0]
	def jidRosterItemJid = JID.jidInstanceNS(rosterItemJid)
	def rosterItemName = (buddy as List)[1] ?: jidRosterItemJid.getLocalpart()
	def rosterItemSubscr = (buddy as List)[2] ?: "both"

	updateRoster(sess, online, jidRosterOwnerJid, jidRosterItemJid, rosterItemName, rosterItemSubscr)

	if (!remove_item) {

		Element pres = new Element("presence",
			(String[])["from", "to", "type"], (String[])[rosterOwnerJid, rosterItemJid,
					"probe"])
		results.offer(Packet.packetInstance(pres))
		pres = new Element("presence",
			(String[])["from", "to", "type"], (String[])[rosterItemJid, rosterOwnerJid,
					"probe"])
		results.offer(Packet.packetInstance(pres))
	}

}

// Finishing the work and completing script

Command.addTextField(result, "Note", "Operation successful");
Command.addFieldMultiValue(result, "Report: ", res_report)
results.add(result)

return results

