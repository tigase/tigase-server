///*
// * Copyright (c) 2009 "Tomasz Sterna" <tomek@xiaoka.com>
// * License: (either of)
// * a. Creative Commons Attribution-Share Alike 3.0 Unported
// *    http://creativecommons.org/licenses/by-sa/3.0/
// * b. GNU Lesser General Public License v3
// *    http://www.gnu.org/licenses/lgpl-3.0.html
// */
//
//import groovy.xml.MarkupBuilder
//import groovy.sql.Sql
//import tigase.util.*
//
//class OpenFire extends Feeder {
//
//def sourceDefinitions = [
//hsqldb: [
//	users: "SELECT username, COALESCE(plainPassword, '{blowfish}{'+(SELECT propValue FROM OFProperty WHERE name='passwordKey' ORDER BY propValue LIMIT 1)+'}'+encryptedPassword) AS password, email FROM OFUser",
//	rosteritems: "SELECT rosterid, jid, nick AS name, sub FROM OFRoster WHERE userName = ?",
//	rostergroups: 'SELECT groupName AS "group" FROM OFRosterGroups WHERE rosterid = ?',
//	vCard: 'SELECT vcard FROM OFvcard WHERE username = ?',
//	privacylists: 'SELECT name, list FROM OFprivacyList WHERE username = ?',
//	privacydefault: 'SELECT name FROM OFprivacyList WHERE isDefault != 0 AND username = ? LIMIT 1',
//	privatestorage: 'SELECT namespace AS ns, privatedata AS xml FROM OFprivate WHERE username = ?'
//
//]
//
//]
//
//def OpenFire(jdbc) {
//	def driver = jdbc.split(":")[1]
//	sourceSQL = sourceDefinitions[driver]
//	println "INFO: Source database connection: $jdbc"
//	if (driver == 'hsqldb') Sql.loadDriver("org.hsqldb.jdbcDriver")
//	sql = Sql.newInstance(jdbc)
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
//		if (it.sub == '3') subscr = "both"
//		else if (it.sub == '2') subscr = "to"
//		else if (it.sub == '1') subscr = "from"
//
//		closure(it.jid, it.name, getRosterGroups(it.rosterid), subscr)
//	})
//}
//
//def getRosterGroups(ROSTERID) {
//	return sql.rows(sourceSQL["rostergroups"], [ROSTERID]).collect{it.group}
//}
//
//def Finish() {}
//
//}
