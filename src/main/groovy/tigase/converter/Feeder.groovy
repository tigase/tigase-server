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
//import groovy.sql.Sql
//import tigase.util.*
//
//def sql
//def sourceSQL = [:]
//
//def Feeder(jdbc) {
//	println "INFO: Source database connection: $jdbc"
//	sql = Sql.newInstance(jdbc)
//	sql.execute(sourceSQL["PRE"])
//}
//
//def eachUser(DOMAIN, closure) {
//	sql.eachRow(sourceSQL["users"], [DOMAIN], {
//		def JID = JIDUtils.getNodeID(it.jid)
//		print "$JID -- "
//		closure(it.jid, JID, it.password, it.email)
//		println "--"
//	})
//}
//
//def eachRoster(UID, closure) {
//	sql.eachRow(sourceSQL["rosteritems"], [UID], {
//		closure(it.jid, it.name, getRosterGroups(UID, it.jid), it.s10n)
//	})
//}
//
//def getRosterGroups(UID, ITEM) {
//	return sql.rows(sourceSQL["rostergroups"], [UID, ITEM]).collect{it.group}
//}
//
//def eachVcard(UID, closure) {
//	def vcard = sql.firstRow(sourceSQL["vCard"], [UID])
//	if(vcard != null) {
//		closure(vcard.vcard)
//	}
//}
//
//def eachPrivacy(UID, closure) {
//	sql.eachRow(sourceSQL["privacylists"], [UID], {
//		closure(it.name, it.list)
//	})
//}
//
//def setPrivacyDefault(UID, closure) {
//	def prdef = sql.firstRow(sourceSQL["privacydefault"], [UID])
//	if (prdef != null) closure(prdef.name)
//}
//
//def eachPrivate(UID, closure) {
//	sql.eachRow(sourceSQL["privatestorage"], [UID], {
//		closure(it.ns, it.xml)
//	})
//}
//
//def Finish() {
//	sql.execute(sourceSQL["POST"])
//}
