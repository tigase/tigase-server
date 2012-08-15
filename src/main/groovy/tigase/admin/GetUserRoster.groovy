/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

Obtaining User Statistics as described in in XEP-0133:
http://xmpp.org/extensions/xep-0133.html#get-user-roster

AS:Description: Get User Roster
AS:CommandId: http://jabber.org/protocol/admin#get-user-roster
AS:Component: sess-man
*/

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*
import tigase.xmpp.impl.roster.*

def JID = "accountjid"

def p = (Packet)packet
def repository = (UserRepository)userRepository
def sessions = (Map<BareJID, XMPPSession>)userSessions
def vhost_man = (VHostManagerIfc)vhostMan
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def userJid = Command.getFieldValue(packet, JID)

if (userJid == null) {
	def result = p.commandResult(Command.DataType.form);

        Command.addTitle(result, "Getting a User's Roster")
        Command.addInstructions(result, "Fill out this form to get a user's roster.")

	Command.addFieldValue(result, "FORM_TYPE", "http://jabber.org/protocol/admin", "hidden")
        Command.addFieldValue(result, JID, userJid ?: "", "jid-single","The Jabber ID for which to retrieve roster")

	return result
}

bareJID = BareJID.bareJIDInstance(userJid)
VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())
def result = p.commandResult(Command.DataType.result)

if (isServiceAdmin ||
	(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {

            Command.addFieldValue(result, JID, userJid ?: "", "jid-single","The Jabber ID for which to retrieve roster")

            XMPPSession session = sessions.get(BareJID.bareJIDInstanceNS(userJid))

	    Element query = new Element("query");
	    query.setXMLNS("jabber:iq:roster");
	    if (session == null) {
		String rosterStr = repository.getData(bareJID, null,
			RosterAbstract.ROSTER, null) ?: ""
		Map<BareJID, RosterElement> roster = new LinkedHashMap<BareJID, RosterElement>()
		RosterFlat.parseRosterUtil(rosterStr, roster, null)
		roster.values().each {
		    query.addChild(it.getRosterItem());
		}
    	    } else {
		RosterAbstract rosterUtil = RosterFactory.getRosterImplementation(true)
		query.addChildren(rosterUtil.getRosterItems(session.getActiveResources().get(0)));
    	    }
	    if (query != null) {
		result.getElement().getChild("command").getChild("x", "jabber:x:data").addChild(query);
	    }
	} else {
	    Command.addTextField(result, "Error", "You do not have enough permissions to retrieve roster for user in this domain.");
	}

return result
