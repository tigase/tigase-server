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
//
//class jabberd2 extends Feeder {
//
//def sourceDefinitions = [
//postgresql: [
//	PRE: '''
//CREATE OR REPLACE FUNCTION public.wrap_tag(text, text)
// RETURNS text AS
// $$SELECT COALESCE('<'||$1||'>'||$2||'</'||$1||'>', '<'||$1||'/>', '')$$
// LANGUAGE 'sql' VOLATILE''',
//	POST: 'DROP FUNCTION wrap_tag(text, text)',
//	users: "SELECT username||'@'||realm AS jid, password, email FROM authreg WHERE realm = ? ORDER BY jid",
//	rosteritems: 'SELECT jid,name,"to","from" FROM "roster-items" WHERE "collection-owner" = ?',
//	rostergroups: 'SELECT "group" FROM "roster-groups" WHERE "collection-owner" = ? AND jid = ?',
//	vCard: '''SELECT
//wrap_tag('FN',"fn") ||
//wrap_tag('N',wrap_tag('FAMILY',"n-family")||wrap_tag('GIVEN',"n-given")||wrap_tag('MIDDLE',"n-middle")||wrap_tag('PREFIX',"n-prefix")||wrap_tag('SUFFIX',"n-suffix")) ||
//wrap_tag('NICKNAME',"nickname") ||
//wrap_tag('PHOTO',wrap_tag('TYPE',"photo-type")||wrap_tag('BINVAL',"photo-binval")||wrap_tag('EXTVAL',"photo-extval")) ||
//wrap_tag('BDAY',"bday") ||
//wrap_tag('ADR',wrap_tag('POBOX',"adr-pobox")||wrap_tag('EXTADD',"adr-extadd")||wrap_tag('STREET',"adr-street")||wrap_tag('LOCALITY',"adr-locality")||wrap_tag('REGION',"adr-region")||wrap_tag('PCODE',"adr-pcode")||wrap_tag('CTRY',"adr-country")) ||
//wrap_tag('TEL',wrap_tag('NUMBER',"tel")) ||
//wrap_tag('EMAIL',wrap_tag('USERID',"email")) ||
//wrap_tag('JABBERID',"jabberid") ||
//wrap_tag('MAILER',"mailer") ||
//wrap_tag('TZ',"tz") ||
//wrap_tag('GEO',wrap_tag('LAT',"geo-lat")||wrap_tag('LON',"geo-lon")) ||
//wrap_tag('TITLE',"title") ||
//wrap_tag('ROLE',"role") ||
//wrap_tag('LOGO',wrap_tag('TYPE',"logo-type")||wrap_tag('BINVAL',"logo-binval")||wrap_tag('EXTVAL',"logo-extval")) ||
//wrap_tag('AGENT',wrap_tag('EXTVAL',"agent-extval")) ||
//wrap_tag('ORG',wrap_tag('ORGNAME',"org-orgname")||wrap_tag('ORGUNIT',"org-orgunit")) ||
//wrap_tag('NOTE',"note") ||
//wrap_tag('REV',"rev") ||
//wrap_tag('SORT-STRING',"sort-string") ||
//wrap_tag('SOUND',wrap_tag('PHONETIC',"sound-phonetic")||wrap_tag('BINVAL',"sound-binval")||wrap_tag('EXTVAL',"sound-extval")) ||
//wrap_tag('UID',"uid") ||
//wrap_tag('URL',"url") ||
//wrap_tag('DESC',"desc") ||
//wrap_tag('KEY',wrap_tag('TYPE',"key-type")||wrap_tag('CRED',"key-cred")) ||
//'' AS vcard
//FROM vcard WHERE "collection-owner" = ?''',
//	privacylists: 'SELECT DISTINCT list AS name FROM "privacy-items" WHERE "collection-owner" = ?',
//	privacyitems: 'SELECT type,value,deny,"order",block FROM "privacy-items" WHERE "collection-owner" = ? AND list = ?',
//	privacydefault: 'SELECT "default" AS "name" FROM "privacy-default" WHERE "collection-owner" = ?',
//	privatestorage: 'SELECT ns,xml FROM "private" WHERE "collection-owner" = ?'
//]
//
//]
//
//def jabberd2(jdbc) {
//	sourceSQL = sourceDefinitions[jdbc.split(":")[1]]
//	println "INFO: Source database connection: $jdbc"
//	sql = Sql.newInstance(jdbc)
//	sql.execute(sourceSQL["PRE"])
//}
//
//def eachRoster(JID, closure) {
//	sql.eachRow(sourceSQL["rosteritems"], [JID], {
//		def subscr = "none"
//		if (it.to && it.from) subscr = "both"
//		else if (it.to) subscr = "to"
//		else if (it.from) subscr = "from"
//
//		closure(it.jid, it.name, getRosterGroups(JID, it.jid), subscr)
//	})
//}
//
//def eachVcard(JID, closure) {
//	def vcard = sql.firstRow(sourceSQL["vCard"], [JID])
//	if(vcard != null) {
//		vcard = '<vCard prodid="-//HandGen//NONSGML vGen v1.0//EN" xmlns="vcard-temp" version="2.0">' + vcard.vcard + '</vCard>'
//		closure(vcard)
//	}
//}
//
//def eachPrivacy(JID, closure) {
//	sql.eachRow(sourceSQL["privacylists"], [JID], {
//		def listname = it.name
//		def writer = new StringWriter()
//		def xml = new MarkupBuilder(writer)
//		xml.setOmitNullAttributes(true)
//		xml.list(name:listname) {
//			sql.eachRow(sourceSQL["privacyitems"], [JID, listname], { row ->
//				def action = row.deny ? 'deny' : 'allow'
//				xml.item(type:row.type, value:row.value, action:action, order:row.order) {
//					if (row.block & 0x01) xml.message()
//					if (row.block & 0x02) xml."presence-in"()
//					if (row.block & 0x04) xml."presence-out"()
//					if (row.block & 0x08) xml.iq()
//				}
//			})
//		}
//		closure(listname, writer.toString())
//	})
//}
//
//}
