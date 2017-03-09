#!/usr/bin/env groovy
/*
 * Copyright (c) 2009 "Tomasz Sterna" <tomek@xiaoka.com>
 * License: (either of)
 * a. Creative Commons Attribution-Share Alike 3.0 Unported
 *    http://creativecommons.org/licenses/by-sa/3.0/
 * b. GNU Lesser General Public License v3
 *    http://www.gnu.org/licenses/lgpl-3.0.html
 *
 * Version: 2.3
 */

/* Configure here vvv */
def tigase_config = "/etc/tigase/tigase.xml"

def source = "jdbc:postgresql://jabberd.db.host/jabberd?user=tigase"
def feeder = new jabberd2(source)

//def source = "jdbc:mysql://localhost/ejabberd?useOldAliasMetadataBehavior=true&user=tigase&password=secret"
//def feeder = new ejabberd(source)

//def source = "jdbc:hsqldb:/var/lib/openfire/embedded-db/openfire"
//def feeder = new OpenFire(source)
/* Configure here ^^^ */



/* you definitely not need to change anything below */
//import tigase.server.*
//import tigase.util.*
//import tigase.xmpp.*
//import tigase.db.*
//import tigase.conf.*
//import tigase.xmpp.impl.roster.*
//import tigase.xmpp.impl.VCardTemp
//import tigase.xmpp.impl.Privacy
//import tigase.xmpp.impl.JabberIqPrivate
//import tigase.vhosts.*
//import groovy.xml.StreamingMarkupBuilder
//
//
//def conf_repo = new ConfigRepository(false, tigase_config)
//def auth_repo = RepositoryFactory.getAuthRepository("util",
//			conf_repo.get("basic-conf", null, Configurable.AUTH_REPO_CLASS_PROP_KEY , null),
//			conf_repo.get("basic-conf", null, Configurable.AUTH_REPO_URL_PROP_KEY , null),
//			null)
//def user_repo = RepositoryFactory.getUserRepository("util",
//			conf_repo.get("basic-conf", null, Configurable.USER_REPO_CLASS_PROP_KEY, null),
//			conf_repo.get("basic-conf", null, Configurable.USER_REPO_URL_PROP_KEY, null),
//			null)
//def vhost_repo = (VHostRepository) Class.forName(conf_repo.get(Configurable.DEF_VHOST_MAN_NAME, null, VHostManager.VHOSTS_REPO_CLASS_PROP_KEY, null)).newInstance()
//vhost_repo.setProperties([
//		(Configurable.SHARED_USER_REPO_PROP_KEY): user_repo,
//		(Configurable.HOSTNAMES_PROP_KEY): conf_repo.get(Configurable.DEF_VHOST_MAN_NAME, null, Configurable.HOSTNAMES_PROP_KEY, null)
//	])
//
//def domains = vhost_repo.localDomains().collect{ it.getVhost() }
//
//domains.each{ DOMAIN ->
//	println "Converting domain `$DOMAIN'"
//	feeder.eachUser(DOMAIN, { UID, JID, PASSWORD, EMAIL ->
//		// user and authentication data
//		try {
//			auth_repo.addUser(JID, PASSWORD )
//			user_repo.setData(JID, null, "email", EMAIL)
//		}
//		catch (UserExistsException e) { /*e.printStackTrace()*/ }
//		catch (TigaseDBException e) { /*e.printStackTrace()*/ }
//
//		def roster = ""
//		feeder.eachRoster(UID, { RJID, NAME, GROUPS, S10N ->
//			def rel = new RosterElement(RJID, NAME, (String[]) GROUPS )
//			rel.setSubscription(RosterAbstract.SubscriptionType.valueOf(S10N))
//			roster += rel.getRosterElement().toString()
//		})
//		if (roster != "") user_repo.setData(JID, null, RosterAbstract.ROSTER, roster);
//
//		feeder.eachVcard(UID, { VCARD ->
//			user_repo.setData(JID, NonAuthUserRepository.PUBLIC_DATA_NODE + "/" + VCardTemp.ID,
//					VCardTemp.VCARD_KEY, VCARD);
//		})
//
//		feeder.eachPrivacy(UID, { NAME, LIST ->
//			user_repo.setData(JID, Privacy.listNode(NAME), Privacy.PRIVACY_LIST, LIST)
//		})
//		feeder.setPrivacyDefault(UID, { DEFAULT ->
//			user_repo.setData(JID, Privacy.PRIVACY, Privacy.DEFAULT, DEFAULT)
//		})
//
//		feeder.eachPrivate(UID, { NS, XML ->
//			def xmlstring = ((String) XML)
//			xmlstring = xmlstring.substring(xmlstring.indexOf('<'))
//			def xml = new XmlSlurper().parseText(xmlstring)
//			def nodes = []
//			xml.'**'.grep{ it.name() == 'query' && it.namespaceURI() == "jabber:iq:private" }.each{ it.children().each{ nodes.add(it) } }
//			if (nodes.size() == 0 && NS != null) {
//				nodes = xml
//			}
//
//			nodes.each { node ->
//				if (NS == null || node.namespaceURI() == NS) {
//					def outputBuilder = new StreamingMarkupBuilder()
//					String result = outputBuilder.bind{ mkp.yield node }
//					def pairKey = node.name() + node.namespaceURI()
//					user_repo.setData(JID, JabberIqPrivate.PRIVATE_KEY, node.name() + node.namespaceURI(), result)
//				}
//			}
//		})
//	})
//}
//
//feeder.Finish()
