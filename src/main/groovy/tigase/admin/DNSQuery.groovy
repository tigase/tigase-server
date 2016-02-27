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
 User add script as described in XEP-0133:
 http://xmpp.org/extensions/xep-0133.html#add-user
 AS:Description: DNS Query
 AS:CommandId: query-dns
 AS:Component: vhost-man
 */

package tigase.admin

import tigase.server.*
import tigase.util.*
import tigase.xmpp.*


def DOMAIN = "domain-name"

def p = (Packet)packet

def domain = Command.getFieldValue(packet, DOMAIN)

if (domain == null) {
	def result = p.commandResult(Command.DataType.form);

	Command.addTitle(result, "Provide domain name")
	Command.addInstructions(result, "Fill out this form to query DNS for domain.")

	Command.addFieldValue(result, DOMAIN, domain ?: "", "text-single",
			"Domain name to query DNS")

	return result
}

def result = p.commandResult(Command.DataType.result)
def response_data = []
try {
	response_data += "IP: " + DNSResolverFactory.getInstance().getHostIP(domain)
} catch (Exception ex) {
	response_data += "IP: " + ex.toString()
}
try {
	DNSEntry[] entries = DNSResolverFactory.getInstance().getHostSRV_Entries(domain)
	int cnt = 1
	entries.each {
		response_data += "SRV " + (cnt++) + ": " + it.toString()
	}
	response_data += "Selected SRV: " + DNSResolverFactory.getInstance().getHostSRV_Entry(domain).toString()
	response_data += "Selected SRV IP: " + DNSResolverFactory.getInstance().getHostSRV_IP(domain)


	Command.addFieldMultiValue(result, "DNS Response ", response_data);
} catch (Exception ex) {
	Command.addTextField(result, "Note", "Problem querying DNS for domain: " + domain);
	// There is a new API to pass exception, for now we have to do it manually
	//Command.addFieldMultiValue(result, "Exception: ", ex)
	def stes = []
	stes += ex.toString()
	ex.getStackTrace().each {
		stes += it.toString()
	}
	Command.addFieldMultiValue(result, "Exception: ", stes);

}

return result
