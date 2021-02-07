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

Update user roster entry.

AS:Description: Update user roster entry
AS:CommandId: user-roster-management
AS:Component: sess-man
*/

package tigase.admin

import tigase.db.UserRepository
import tigase.server.Command
import tigase.server.Packet
import tigase.vhosts.VHostManagerIfc
import tigase.xmpp.XMPPSession
import tigase.xmpp.impl.roster.RosterAbstract
import tigase.xmpp.impl.roster.RosterElement
import tigase.xmpp.impl.roster.RosterFactory
import tigase.xmpp.jid.BareJID
import tigase.xmpp.jid.JID

import java.util.function.Function

class Field {

	String name;
	String label;
	String type;
	String defVal = ""
}

class RosterChangesControler {

	UserRepository repository
	VHostManagerIfc vhost_man
	Set<BareJID> admins

	Map<String, XMPPSession> sessions

	Function<String,Boolean> isAllowedForDomain

	Field addOperation = new Field(name: "addJid", label: "Add")
	Field removeOperation = new Field(name: "removeJid", label: "Remove")
	List<Field> operationTypes = [ addOperation, removeOperation ]

	Field subscriptionNone = new Field(name: "none", label: "None")
	Field subscriptionFrom = new Field(name: "from", label: "From")
	Field subscriptionTo = new Field(name: "to", label: "To")
	Field subscriptionBoth = new Field(name: "both", label: "Both")
	List<Field> subscriptionTypes = [ subscriptionNone, subscriptionFrom, subscriptionTo, subscriptionBoth ]

	Field ownerJid = new Field(name: "rosterOwnerJID", label: "Roster owner JID", type: "jid-single")
	Field jidToChange = new Field(name: "jidToManipulate", label: "JID to manipulate", type: "jid-single")
	Field groups = new Field(name: "groups", label: "Comma separated groups", type: "text-single")
	Field operationType = new Field(name: "operationType", label: "Operation type",
									defVal: addOperation.name)
	Field subscriptionType = new Field(name: "subscriptionType",
									   label: "Subscription type", defVal: subscriptionBoth.name)
	List<Field> formFields = [ ownerJid, jidToChange, groups, operationType, subscriptionType ]

	def addField(Packet form, Field field, List<Field> listFields = [ ]) {
		if (listFields != null && listFields.size() == 0) {
			Command.addFieldValue(form, field.name, field.defVal,
								  field.type, field.label)
		} else {
			def listValues = (listFields.collect { it.name }).toArray(new String[0])
			def listLabels = (listFields.collect { it.label }).toArray(new String[0])
			Command.addFieldValue(form, field.name, field.defVal, field.label, listLabels, listValues)
		}
	}

	def getFieldValue(Packet form, Field field) {
		return Command.getFieldValue(form, field.name)
	}

	def processPacket(Packet p) {
		if ((formFields.find { it.name != groups.name && Command.getFieldValue(p, it.name) == null }) == null) {
			String ownerJidStr = getFieldValue(p, ownerJid)
			String jidToManipulate = getFieldValue(p, jidToChange)
			String[] groups = (getFieldValue(p, groups) ?: "").split(",")
			String operationTypeStr = getFieldValue(p, operationType)
			String subscriptionTypeStr = getFieldValue(p, subscriptionType)

			Packet result = p.commandResult(Command.DataType.result)

			BareJID stanzaFromBare = p.getStanzaFrom().getBareJID();

			JID jidRosterOwnerJid = JID.jidInstanceNS(ownerJidStr);
			JID jidRosterItemJid = JID.jidInstanceNS(jidToManipulate);

			if (!isAllowedForDomain.apply(jidRosterOwnerJid.getDomain())) {

				//if ( !(isServiceAdmin || (vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) ) {
				Command.addTextField(result, "Error",
									 "You do not have enough permissions to modify roster of " + ownerJidStr);
				return result;
			}

			if (!isAllowedForDomain.apply(jidRosterItemJid.getDomain())) {

				//if ( !(isServiceAdmin || (vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) ) {

				Command.addTextField(result, "Error",
									 "You do not have enough permissions to modify roster of " + jidToManipulate);
				return result;
			}

			Queue<Packet> results;
			if (operationTypeStr == addOperation.name) {
				RosterElement item = new RosterElement(jidRosterItemJid, jidRosterItemJid.toString(), groups);
				item.setSubscription(subscription(subscriptionTypeStr));
				results = RosterFactory.getRosterImplementation(true).addJidToRoster(repository, sessions.get(jidRosterOwnerJid.getBareJID()), jidRosterOwnerJid.getBareJID(), item);
			} else {
				results = RosterFactory.getRosterImplementation(true).removeJidFromRoster(repository, sessions.get(jidRosterOwnerJid.getBareJID()), jidRosterOwnerJid.getBareJID(), jidRosterItemJid);
			}

			Command.addTextField(result, "Note", "Operation successful");
			results.add(result)
			return results
		} else {
			Packet result = p.commandResult(Command.DataType.form)
			addField(result, ownerJid)
			addField(result, jidToChange)
			addField(result, groups)
			addField(result, operationType, operationTypes)
			addField(result, subscriptionType, subscriptionTypes)
			return result
		}
	}

	def subscription(String str) {
		return RosterAbstract.SubscriptionType.valueOf(str)
	}

}

def changesControler = new RosterChangesControler(repository: userRepository,
												  admins: adminsSet,
												  vhost_man: vhostMan,
												  sessions: userSessions,
												  isAllowedForDomain: (Function<String, Boolean>) isAllowedForDomain)
changesControler.processPacket((Packet) packet)