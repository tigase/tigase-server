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

Update user roster entry, extended version.
If both given JIDs are local, rosters for both users are updated accordingly.

AS:Description: Update user roster entry, extended version.
AS:CommandId: user-roster-management-ext
AS:Component: sess-man
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xmpp.impl.roster.*
import tigase.xml.*
import tigase.vhosts.*
import tigase.cluster.*;
import tigase.cluster.api.*;
import tigase.cluster.strategy.*;


try {
	println "=========================="

	def ROSTER_OWNER_JID = "roster-owner-jid"
	def ROSTER_OWNER_PRESENCE = "roster-owner-presence"
	def ROSTER_OWNER_NAME = "roster-owner-name"
	def ROSTER_OWNER_GROUPS = "roster-owner-groups"
	def ROSTER_ITEM_JID = "roster-item-jid"
	def ROSTER_ITEM_NAME = "roster-item-name"
	def ROSTER_ITEM_GROUPS = "roster-item-groups"
	def ROSTER_ITEM_SUBSCR = "roster-item-subscr"
	def ROSTER_ACTION = "roster-action"
	def NOTIFY_CLUSTER = "notify-cluster"

	def UPDATE = "update"
	def REMOVE = "remove"
	def UPDATE_EXT = "update-ext"
	def REMOVE_EXT = "remove-ext"
	def subscriptions = ["both", "from", "to", "none"]
	def actions = [UPDATE, REMOVE, UPDATE_EXT, REMOVE_EXT]
	def actions_descr = ["Add/Update item", "Remove item", "Add/Update both rosters", "Remove from both rosters"]

	Queue results = new LinkedList()
	def p = (Packet)packet
	def repository = (UserRepository)userRepository
	def sessions = (Map<BareJID, XMPPSession>)userSessions
	def vhost_man = (VHostManagerIfc)vhostMan
	def admins = (Set)adminsSet

	def stanzaFromBare = p.getStanzaFrom().getBareJID();
	def isServiceAdmin = admins.contains(stanzaFromBare);

	def rosterOwnerJid = Command.getFieldValue(packet, ROSTER_OWNER_JID)
	def rosterOwnerName = Command.getFieldValue(packet, ROSTER_OWNER_NAME)
	def rosterOwnerGroups = Command.getFieldValue(packet, ROSTER_OWNER_GROUPS)
	def rosterItemJid = Command.getFieldValue(packet, ROSTER_ITEM_JID)
	def rosterItemName = Command.getFieldValue(packet, ROSTER_ITEM_NAME)
	def rosterItemGroups = Command.getFieldValue(packet, ROSTER_ITEM_GROUPS)
	def rosterItemSubscr = Command.getFieldValue(packet, ROSTER_ITEM_SUBSCR)
	def rosterAction = Command.getFieldValue(packet, ROSTER_ACTION)
	boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );
	boolean notifyCluster = Boolean.valueOf( Command.getFieldValue(packet, NOTIFY_CLUSTER) )

	if (rosterOwnerJid == null || rosterItemJid == null ||
		rosterItemSubscr == null || rosterAction == null) {
		def res = p.commandResult(Command.DataType.form);
		Command.addFieldValue(res, ROSTER_OWNER_JID, rosterOwnerJid ?: "", "jid-single", "Roster owner JID")
		Command.addFieldValue(res, ROSTER_OWNER_NAME, rosterOwnerName ?: "",  "text-single", "Roster owner name")
		Command.addFieldValue(res, ROSTER_OWNER_GROUPS, rosterOwnerGroups ?: "", "text-single", "Comma separated list of owner groups")
		Command.addFieldValue(res, ROSTER_ITEM_JID, rosterItemJid ?: "", "jid-single", "Roster item JID")
		Command.addFieldValue(res, ROSTER_ITEM_NAME, rosterItemName ?: "",  "text-single", "Roster item name")
		Command.addFieldValue(res, ROSTER_ITEM_GROUPS, rosterItemGroups ?: "", "text-single", "Comma separated list of item groups")
		Command.addFieldValue(res, ROSTER_ITEM_SUBSCR, subscriptions[0], "Roster item Subscription", (String[])subscriptions, (String[])subscriptions)
		Command.addFieldValue(res, ROSTER_ACTION, actions[0], "Action", (String[])actions_descr, (String[])actions)
		if 	( clusterMode  ) {
			Command.addHiddenField(res, NOTIFY_CLUSTER, true.toString())
		}
		return res
	}

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


	def remove_item = rosterAction == REMOVE || rosterAction == REMOVE_EXT

	def updateRoster = { jid, i_jid, i_name, i_groups, i_subscr, i_original_node ->

		def sess = sessions == null ? null : sessions.get(jid.getBareJID());
		def conn = (sess != null && sess.getActiveResourcesSize() > 0) ? sess.getActiveResources().get(0) : null;
		if (conn) {
			// Update online
			RosterAbstract rosterUtil = RosterFactory.getRosterImplementation(true)
			Element item = new Element("item",
				(String[])["jid", "subscription"], (String[])[i_jid, "remove"])
			if (remove_item) {
				rosterUtil.removeBuddy(conn, i_jid)
			} else {
				rosterUtil.addBuddy(conn, i_jid, i_name ?: i_jid.getLocalpart(), i_groups ? i_groups.split(",") : null, null)
				rosterUtil.setBuddySubscription(conn, RosterAbstract.SubscriptionType.valueOf(i_subscr), i_jid)
				item = rosterUtil.getBuddyItem(conn, i_jid)
			}
			rosterUtil.updateBuddyChange(conn, results, item)
		} else if (i_original_node) {
			// We need to synchronize on some object (ie. on UserRepository instance) to fix issue with
			// race condition when we modify roster of user which is offline
			// Is there a better object to use for synchronization?
			synchronized (repository) {
				// Update offline and only on original node
				String rosterStr = repository.getData(jid.getBareJID(), null, RosterAbstract.ROSTER, null) ?: ""
				Map<BareJID, RosterElement> roster = new LinkedHashMap<BareJID, RosterElement>()
				RosterFlat.parseRosterUtil(rosterStr, roster, null)
				if (remove_item) {
					roster.remove(i_jid.getBareJID())
				} else {
					RosterElement rel = new RosterElement(i_jid, i_name, i_groups ? i_groups.split(",") : null, null)
					rel.setSubscription(RosterAbstract.SubscriptionType.valueOf(i_subscr))
					rel.setPersistent(true);
					roster.put(i_jid, rel)
				}
				StringBuilder sb = new StringBuilder(200)
				for (RosterElement relem: roster.values())
				sb.append(relem.getRosterElement().toString())
				repository.setData(jid.getBareJID(), null, RosterAbstract.ROSTER, sb.toString());
			}
		}
	}

	def jidRosterOwnerJid = JID.jidInstanceNS(rosterOwnerJid);
	def jidRosterItemJid = JID.jidInstanceNS(rosterItemJid);

	Packet result = p.commandResult(Command.DataType.result)
	def vhost = vhost_man.getVHostItem(jidRosterOwnerJid.getDomain());
	if (vhost == null || (!isServiceAdmin && !vhost.isOwner(stanzaFromBare.toString()) && !vhost.isAdmin(stanzaFromBare.toString()))) {
		Command.addTextField(result, "Error", "You do not have enough permissions to modify roster of "+rosterOwnerJid);
		results.add(result);
		return results;
	}

	updateRoster(jidRosterOwnerJid, jidRosterItemJid, rosterItemName, rosterItemGroups, rosterItemSubscr, notifyCluster)

	Element pres;
	if (rosterAction == UPDATE_EXT || rosterAction == REMOVE_EXT) {
		def subscr = rosterItemSubscr;
		switch (rosterItemSubscr) {
		case "to" : subscr = "from"; break;
		case "from" : subscr = "to"; break;
		}

		vhost = vhost_man.getVHostItem(jidRosterItemJid.getDomain());
		if (vhost == null || (!isServiceAdmin && !vhost.isOwner(stanzaFromBare.toString()) && !vhost.isAdmin(stanzaFromBare.toString()))) {
			Command.addTextField(result, "Error", "You do not have enough permissions to modify roster of "+rosterItemJid);
			results.add(result);
			return results;
		}

		updateRoster(jidRosterItemJid, jidRosterOwnerJid, rosterOwnerName, rosterOwnerGroups, subscr, notifyCluster)
		
		if (!remove_item) {
			pres = new Element("presence", (String[])["from", "to", "type"], (String[])[rosterOwnerJid, rosterItemJid, "probe"])
			results.offer(Packet.packetInstance(pres))
		}
	}
	if (!remove_item) {
		pres = new Element("presence", (String[])["from", "to", "type"], (String[])[rosterItemJid, rosterOwnerJid, "probe"])
		results.offer(Packet.packetInstance(pres))
	}

	Command.addTextField(result, "Note", "Operation successful");
	results.add(result)

	//return results
	return (Queue)results

}  catch (Exception ex) { ex.printStackTrace(); }