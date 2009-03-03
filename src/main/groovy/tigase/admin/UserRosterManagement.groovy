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
Description: Update user roster entry
CommandId: user-roster-management
*/

package tigase.admin

import tigase.server.*
import tigase.xmpp.*
import tigase.xmpp.impl.roster.*
import tigase.xml.*
import tigase.db.UserRepository

class Field { String name; String label; String type; String defVal = ""}

class RosterChangesControler {
	UserRepository repository 
	Map<String, XMPPSession> sessions

	Field addOperation = new Field(name: "addJid", label: "Add")
	Field removeOperation = new Field(name: "removeJid", label: "Remove")
	List<Field> operationTypes = [addOperation, removeOperation]

	Field subscriptionNone = new Field(name: "none", label: "None")
	Field subscriptionFrom = new Field(name: "from", label: "From")
	Field subscriptionTo = new Field(name: "to", label: "To")
	Field subscriptionBoth = new Field(name: "both", label: "Both")
	List<Field> subscriptionTypes = [subscriptionNone, subscriptionFrom, subscriptionTo, subscriptionBoth]

	Field ownerJid = new Field(name: "rosterOwnerJID", label: "Roster owner JID", type: "jid-single")
	Field jidToChange= new Field(name: "jidToManipulate", label: "JID to manipulate", type: "jid-single")
	Field groups = new Field(name: "groups", label: "Comma separated groups")
	Field operationType = new Field(name: "operationType", label: "Operation type", 
			defVal: addOperation.name)
	Field subscriptionType = new Field(name: "subscriptionType", 
			label: "Subscription type", defVal: subscriptionBoth.name)
	List<Field> formFields = [ownerJid, jidToChange, groups, operationType, subscriptionType]
	
	def addField(Packet form, Field field, List<Field> listFields = []) {
		if (listFields != null && listFields.size() == 0)
			Command.addFieldValue(form, field.name, field.defVal, field.type, field.label) 	
		else {
			def listValues = (listFields.collect { it.name }).toArray(new String[0])
			def listLabels = (listFields.collect { it.label }).toArray(new String[0])
			Command.addFieldValue(form, field.name, field.defVal, field.label, listLabels, listValues)
		}
	}

	def getFieldValue(Packet form, Field field) { return Command.getFieldValue(form, field.name) }		
		
	def processPacket(Packet p) {
		if ((formFields.find { it.name != groups.name && Command.getFieldValue(p, it.name) == null}) == null) {
			String ownerJidStr = getFieldValue(p, ownerJid)
			String jidToManipulate = getFieldValue(p, jidToChange)
			String[] groups = (getFieldValue(p, groups) ?: "").split(",")
			String operationTypeStr = getFieldValue(p, operationType)
			String subscriptionTypeStr = getFieldValue(p, subscriptionType)
			
			Queue<Packet> results;
			if (operationTypeStr == addOperation.name)
				results = addJidToRoster(ownerJidStr, jidToManipulate, groups, subscriptionTypeStr)
			else
				results = removeJidFromRoster(ownerJidStr, jidToManipulate)	
			
			Packet result = p.commandResult(Command.DataType.result)
			Command.addTextField(result, "Note", "Operation successful");
			results.add(result)
			return results
		}
		else {
			Packet result = p.commandResult(Command.DataType.form)
			addField(result, ownerJid)
			addField(result, jidToChange)
			addField(result, groups)
			addField(result, operationType, operationTypes)
			addField(result, subscriptionType, subscriptionTypes)		
			return result
		}
	}
		
	def getActiveConnections(String ownerJid) {
		XMPPSession session = sessions.get(ownerJid)
		return (session == null) ? [] : session.getActiveResources()
	}
	
	def subscription(String str) { return RosterAbstract.SubscriptionType.valueOf(str) }

	Queue<Packet> updateLiveRoster(String jid, String jidToChange, boolean remove, 
			String[] groups = null, String subStr = null) {
		RosterAbstract rosterUtil = RosterFactory.getRosterImplementation(true)
		Queue<Packet> packets = new LinkedList<Packet>()
		List<XMPPResourceConnection> activeConnections = getActiveConnections(jid)
		for (XMPPResourceConnection conn : activeConnections) {
			if (remove == false) {
				rosterUtil.addBuddy(conn, jidToChange, jidToChange, groups)
				rosterUtil.setBuddySubscription(conn, subscription(subStr), jidToChange)
				rosterUtil.updateBuddyChange(conn, packets,
						rosterUtil.getBuddyItem(conn, jidToChange))				
			} else {
				Element it = new Element("item")
				it.setAttribute("jid", jidToChange)
				it.setAttribute("subscription", "remove")
				rosterUtil.updateBuddyChange(conn, packets, it)
				rosterUtil.removeBuddy(conn, jidToChange) 
			}
		}
		return packets
	}	
	
	def modifyDbRoster(String ownerJid, modifyFunc) {
		String rosterStr = repository.getData(ownerJid, null, RosterAbstract.ROSTER, null)
		rosterStr = (rosterStr == null) ? "" : rosterStr
		Map<String, RosterElement> roster = new LinkedHashMap<String, RosterElement>()
		RosterFlat.parseRoster(rosterStr, roster)
		modifyFunc(roster)
		StringBuilder sb = new StringBuilder()
		for (RosterElement relem: roster.values())
			sb.append(relem.getRosterElement().toString())
		repository.setData(ownerJid, null, RosterAbstract.ROSTER, sb.toString());		
	}
	
	Queue<Packet> addJidToRoster(ownerJid, jidToAdd, groups, subscriptionType) {
		List<XMPPResourceConnection> activeConnections = getActiveConnections(ownerJid)
		if (activeConnections.size() == 0) {
			modifyDbRoster(ownerJid, { roster -> 
				RosterElement userToAdd = roster.get(jidToAdd)
				if (userToAdd == null) {
					userToAdd = new RosterElement(
							jidToAdd, jidToAdd, groups)
				}
				userToAdd.setSubscription(subscription(subscriptionType))
				userToAdd.setGroups(groups)
				roster.put(jidToAdd, userToAdd)								
			})
			return new LinkedList<Packet>()
		} 
		else
			return updateLiveRoster(ownerJid, jidToAdd, false, groups, subscriptionType)
	}
	
	Queue<Packet> removeJidFromRoster(ownerJid, jidToRemove) {
		List<XMPPResourceConnection> activeConnections = getActiveConnections(ownerJid)
		if (activeConnections.size() == 0) {
			modifyDbRoster(ownerJid, { roster -> 
				RosterElement userToRemove = roster.get(jidToRemove)
				if (userToRemove == null) {
					throw new Exception("User to be deleted is not on roster")
				}
				roster.remove(jidToRemove)
			})
			return new LinkedList<Packet>()
		} 
		else
			return updateLiveRoster(ownerJid, jidToRemove, true)
	}	
}

new RosterChangesControler(repository: userRepository, 
		sessions: userSessions).processPacket((Packet)packet)