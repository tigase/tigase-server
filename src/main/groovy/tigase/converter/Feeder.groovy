/*
 * Copyright (c) 2009 "Tomasz Sterna" <tomek@xiaoka.com>
 * License: (either of)
 * a. Creative Commons Attribution-Share Alike 3.0 Unported
 *    http://creativecommons.org/licenses/by-sa/3.0/
 * b. GNU Lesser General Public License v3
 *    http://www.gnu.org/licenses/lgpl-3.0.html
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
