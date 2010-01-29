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
AS:Description: Command to change user inter-domain communication permission.
AS:CommandId: user-domain-perm
AS:Component: sess-man
*/

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.impl.DomainFilter
import tigase.db.UserRepository
import tigase.db.UserNotFoundException

def JID = "jid"
def DOMAIN = "domain"
def DOMAIN_LIST = "domainList"

def p = (Iq)packet
def jid = Command.getFieldValue(p, JID)
def domain = Command.getFieldValue(p, DOMAIN)
def domainList = Command.getFieldValue(p, DOMAIN_LIST)

if (jid == null || domain == null || 
	(domain == DomainFilter.DOMAINS.LIST.name() && domainList == null)) {
	def res = (Iq)p.commandResult(Command.DataType.form);
	Command.addFieldValue(res, JID, jid ?: "", "jid-single", "User JID")
	def domainStr = []
  DomainFilter.DOMAINS.values().each { domainStr += it.name() }
	Command.addFieldValue(res, DOMAIN, domain ?: domainStr[0], "List of domains",
		(String[])domainStr, (String[])domainStr)
	Command.addFieldValue(res, DOMAIN_LIST, domainList ?: "", "text-single", "Domains List")
	return res
}

jid = JIDUtils.getNodeID(jid)

def repo = (UserRepository)userRepository

try {
	def old_value = repo.getData(jid, null,
		DomainFilter.ALLOWED_DOMAINS_KEY, null)

	def new_value = domain
	if (domain == DomainFilter.DOMAINS.LIST.name()) {
		new_value = domainList
	}
	repo.setData(jid, null, DomainFilter.ALLOWED_DOMAINS_KEY, new_value)

	return "Changed an old value: $old_value to a new value: $new_value for user: $jid"
} catch (e) {
  if (e in UserNotFoundException)	{
		return "The user $jid was not found in the user repository"
	} else {
		return "Unexpected error: " + e
	}
}
