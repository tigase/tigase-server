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
 Obtaining informations about user
 AS:Description: Get User Info
 AS:CommandId: get-user-info
 AS:Component: sess-man
 AS:Group: Users
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xml.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*
import tigase.cluster.*;
import tigase.cluster.api.*;
import tigase.cluster.strategy.*;


def JID = "accountjid"

def p = (Packet)packet
def sessions = (Map<BareJID, XMPPSession>)userSessions
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def userJid = Command.getFieldValue(packet, JID)

if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Get User Info")
	Command.addInstructions(result, "Fill out this form to gather informations about user.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin", "hidden")
	Command.addFieldValue(result, JID, userJid ?: "", "jid-single","The Jabber ID for statistics")
	Command.addCheckBoxField(result, "Show connected resources in table", true)

	return result
}

def bareJID = BareJID.bareJIDInstance(userJid)
VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
def resourcesAsTable = Command.getCheckBoxFieldValue(p, "Show connected resources in table");
def result = p.commandResult(Command.DataType.result)

if (isServiceAdmin ||
(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {

		Command.addTextField(result, "JID", "JID: " + userJid)
		def userRes = [];
		if (binding.variables.containsKey("clusterStrategy")) { 
            def cluster = (ClusteringStrategyIfc) clusterStrategy
			def conns = cluster.getConnectionRecords(bareJID);
			if (cluster.containsJid(bareJID) && (conns != null)) {
				conns.each { rec ->
					userRes.add([res:rec.getUserJid().getResource(), node:rec.getNode().getDomain()]);
				}
			}
		} 

		XMPPSession session = sessions.get(BareJID.bareJIDInstanceNS(userJid))
		if (session != null) {
			List<XMPPResourceConnection> conns = session.getActiveResources()
			conns.each { con ->
				userRes.add([res:con.getResource(), node:con.getConnectionId()?.getDomain()]);
			}
			
		}
		if (userRes.isEmpty()) {
			Command.addTextField(result, "Status", "Status: offline")
		} else {
			userRes.sort { it.res };
			Command.addTextField(result, "Status", "Status: " + (userRes.size() ? "online" : "offline"))
			Command.addTextField(result, "Active connections", "Active connections: " + userRes.size())
			if (resourcesAsTable) {
				Element reported = new Element("reported");
				reported.addAttribute("label", "Connected resources");
				def cols = ["Resource", "Cluster node"];
				cols.each {
					Element el = new Element("field");
					el.setAttribute("var", it);
					reported.addChild(el);
				}
				result.getElement().getChild('command').getChild('x').addChild(reported);
				userRes.each { con ->	
					Element item = new Element("item");
					Element res = new Element("field");
					res.setAttribute("var", "Resource");
					res.addChild(new Element("value", con.res));
					item.addChild(res);
				
					Element node = new Element("field");
					node.setAttribute("var", "Cluster node");
					node.addChild(new Element("value", con.node));
					item.addChild(node);
					result.getElement().getChild('command').getChild('x').addChild(item);
				}				
			} else {
				for (def con: userRes) {
					Command.addTextField(result, con.res + " is connected to", con.res + " is connected to " + con.node);
				}
			}				
		}

		def sessionManager = component;
		def offlineMsgsRepo = sessionManager.processors.values().find { it.hasProperty("msg_repo") }?.msg_repo;
		if (offlineMsgsRepo && offlineMsgsRepo.metaClass.respondsTo(offlineMsgsRepo, "getMessagesCount", [tigase.xmpp.JID] as Object[])) {
			def offlineStats = 0;
			try {
				offlineStats = offlineMsgsRepo.getMessagesCount(tigase.xmpp.JID.jidInstance(bareJID));
			} catch (tigase.db.UserNotFoundException ex) {
				// ignoring this error for now as it is not important
			}
			def msg = "Offline messages: " + (offlineStats ? (offlineStats[offlineStats.keySet().find { it.name() == "message" }] ?: 0) : 0);
			Command.addTextField(result, msg, msg);
		}		
		
		def loginHistoryProcessor = sessionManager.outFilters["login-history"];
		if (loginHistoryProcessor) {
			def unifiedArchiveComp = tigase.server.XMPPServer.getComponent(loginHistoryProcessor.getComponentJid().getLocalpart())//sessionManager.parent.components_byId[loginHistoryProcessor.componentJid];
			if (unifiedArchiveComp) {
				def ua_repo = unifiedArchiveComp.msg_repo;
				def criteria = ua_repo.newCriteriaInstance();
				criteria.setWith(bareJID.toString());
				criteria.getRSM().hasBefore = true;
				criteria.getRSM().max = 10;
				criteria.itemType = "login";
//				def logins = ua_repo.getItems(bareJID, criteria).reverse().collect { new java.util.Date(criteria.getStart().getTime() + 
//							(Integer.parseInt(it.getAttribute("secs"))*1000)).toString() + " for resource '" + it.getChildren().first().getCData() + "'" }.join("\n");

				Element reported = new Element("reported");
				reported.addAttribute("label", "Login times");
				def cols = ["Resource", "Date"];
				cols.each {
					Element el = new Element("field");
					el.setAttribute("var", it);
					reported.addChild(el);
				}
				result.getElement().getChild('command').getChild('x').addChild(reported);
			
				ua_repo.getItems(bareJID, criteria).reverse().each {
					Element item = new Element("item");
					Element res = new Element("field");
					res.setAttribute("var", "Resource");
					res.addChild(new Element("value", it.getChildren().first().getCData()));
					item.addChild(res);
					
					String ts = new java.util.Date(criteria.getStart().getTime() + 
						(Integer.parseInt(it.getAttribute("secs"))*1000)).format("yyyy-MM-dd HH:mm:ss.S");
					Element node = new Element("field");
					node.setAttribute("var", "Date");
					node.addChild(new Element("value", ts));
					item.addChild(node);
					result.getElement().getChild('command').getChild('x').addChild(item);
				}
			}
		}
} else {
	Command.addTextField(result, "Error", "You do not have enough permissions to obtain statistics for user in this domain.");
}

return result
