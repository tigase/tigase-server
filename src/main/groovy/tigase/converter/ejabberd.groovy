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
//import groovy.xml.MarkupBuilder
//import groovy.sql.Sql
//import tigase.util.*
//
//class ejabberd extends Feeder {
//
//def sourceDefinitions = [
//mysql: [
//	PRE: 'SELECT 1',
//	POST: 'SELECT 1',
//	users: 'SELECT username, password, NULL AS email FROM users',
//	rosteritems: 'SELECT jid,nick AS name,subscription FROM rosterusers WHERE username = ?',
//	rostergroups: 'SELECT grp AS "group" FROM rostergroups WHERE username = ? AND jid = ?',
//	vCard: 'SELECT vcard FROM vcard WHERE username = ?',
//	privacylists: 'SELECT name,id FROM privacy_list WHERE username = ?',
//	privacyitems: 'SELECT t AS type,value,action,ord AS "order",match_all,match_iq,match_message,match_presence_in,match_presence_out FROM privacy_list_data WHERE id = ?',
//	privacydefault: 'SELECT name FROM privacy_default_list WHERE username = ?',
//	privatestorage: 'SELECT namespace AS ns, data AS xml FROM private_storage WHERE username = ?'
//]
//
//]
//
//def ejabberd(jdbc) {
//	sourceSQL = sourceDefinitions[jdbc.split(":")[1]]
//	println "INFO: Source database connection: $jdbc"
//	sql = Sql.newInstance(jdbc)
//	sql.execute(sourceSQL["PRE"])
//}
//
//def eachUser(DOMAIN, closure) {
//	sql.eachRow(sourceSQL["users"], {
//		def JID = JIDUtils.getNodeID(it.username + '@' + DOMAIN)
//		print "$JID -- "
//		closure(it.username, JID, it.password, it.email)
//		println "--"
//	})
//}
//
//def eachRoster(JID, closure) {
//	sql.eachRow(sourceSQL["rosteritems"], [JID], {
//		def subscr = "none"
//		if (it.subscription == 'B') subscr = "both"
//		else if (it.subscription == 'T') subscr = "to"
//		else if (it.subscription == 'F') subscr = "from"
//
//		closure(it.jid, it.name, getRosterGroups(JID, it.jid), subscr)
//	})
//}
//
//def eachPrivacy(JID, closure) {
//	sql.eachRow(sourceSQL["privacylists"], [JID], {
//		def listname = it.name
//		def listid = it.id
//		def writer = new StringWriter()
//		def xml = new MarkupBuilder(writer)
//		xml.setOmitNullAttributes(true)
//		xml.list(name:listname) {
//			sql.eachRow(sourceSQL["privacyitems"], [listid], { row ->
//				def type = null
//				switch (row.type) {
//					case "j":
//						type = "jid"
//						break
//					case "g":
//						type = "group"
//						break
//					case "s":
//						type = "subscription"
//						break
//				}
//				def action = row.action=="d" ? 'deny' : 'allow'
//				xml.item(type:type, value:row.value, action:action, order:row.order) {
//					if (! (row.match_all != "0" && row.match_all) ) {
//						if (row.match_message != "0" && row.match_message) xml.message()
//						if (row.match_presence_in != "0" && row.match_presence_in) xml."presence-in"()
//						if (row.match_presence_out != "0" && row.match_presence_out) xml."presence-out"()
//						if (row.match_iq != "0" && row.match_iq) xml.iq()
//					}
//				}
//			})
//		}
//		closure(listname, writer.toString())
//	})
//}
//
//}
