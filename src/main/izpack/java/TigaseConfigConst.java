/*  Tigase Project
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package com.izforge.izpack.panels;

import java.util.LinkedHashMap;
import java.util.Properties;

/**
 * Describe class TigaseConfigConst here.
 *
 *
 * Created: Sat Oct 25 21:23:06 2008
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TigaseConfigConst {

	public static Properties props = new Properties();

	public static LinkedHashMap<String, String> tigaseIzPackMap =
    new LinkedHashMap<String, String>();
// 	public static LinkedHashMap<String, String> izPackTigaseMap =
//     new LinkedHashMap<String, String>();

	public static LinkedHashMap<String, String> userDBMap =
    new LinkedHashMap<String, String>();

	public static LinkedHashMap<String, String> userDBUriMap =
    new LinkedHashMap<String, String>();

	public static final String SERVER_DEBUG = "serverDebug";
	public static final String PLUGIN_DEBUG = "pluginsDebug";
	public static final String DB_DEBUG = "dbDebug";
	public static final String CLUSTER_DEBUG = "clusterDebug";

	public static String[] ALL_DEBUGS =
	{SERVER_DEBUG, PLUGIN_DEBUG, DB_DEBUG, CLUSTER_DEBUG};
	public static LinkedHashMap<String, String> debugMap =
    new LinkedHashMap<String, String>();

	public static String[] ALL_PLUGINS =
	{"messageArchivePlugin", "nonSaslAuthPlugin", "saslAuthPlugin", "resBindPlugin",
	 "sessionBindPlugin", "registerPlugin", "rosterPlugin", "presencePlugin", "privacyPlugin",
	 "versionPlugin", "statsPlugin", "tlsPlugin", "msgOfflinePlugin",
	 "vcardPlugin", "commandsPlugin", "privatePlugin", "pingPlugin",
	 "basicFilterPlugin", "domainFilterPlugin", "pepPlugin", "zlibPlugin"};
	public static LinkedHashMap<String, String> pluginsMap =
    new LinkedHashMap<String, String>();

	// Special variable names:
	public static final String DB_TYPE = "dbType";
	public static final String AUTH_DB = "authDB";
	public static final String AUTH_HANDLE = "dbAuthHandle";
	public static final String MUC_COMP = "mucComponent";
	public static final String PUBSUB_COMP = "PubSubComponent";
	public static final String STUN_COMP = "stunComponent";
	public static final String SOCKS5_COMP = "socks5Component";
	public static final String ARCHIVE_COMP = "messageArchiveComponent";
	public static final String HTTP_COMP = "httpApiComponent";
	public static final String ACS_COMP = "acsComponent";
	public static final String ACS_MUC_COMP = "acsMUCComponent";
	public static final String ACS_PUBSUB_COMP = "acsPubSubComponent";
	public static final String PLUGINS = "plugins";
	public static final String DEBUG = "debug";
	public static final String USER_DB_URI = "userDBUri";
	public static final String AUTH_DB_URI = "dbAuthType";

	// classes
	public static final String MUC_COMP_CLASS = "tigase.muc.MUCComponent";
	public static final String PUBSUB_COMP_CLASS = "tigase.pubsub.PubSubComponent";
	public static final String SOCKS5_COMP_CLASS = "tigase.socks5.Socks5ProxyComponent";
	public static final String STUN_COMP_CLASS = "tigase.stun.StunComponent";
	public static final String ARCHIVE_COMP_CLASS = "tigase.archive.MessageArchiveComponent";
	public static final String HTTP_COMP_CLASS = "tigase.http.HttpMessageReceiver";
	public static final String ACS_COMP_CLASS = "tigase.cluster.strategy.OnlineUsersCachingStrategy";
	public static final String ACS_MUC_COMP_CLASS = "tigase.muc.cluster.MUCComponentClustered";
	public static final String ACS_PUBSUB_COMP_CLASS = "tigase.pubsub.cluster.PubSubComponentClustered";

	static {
		tigaseIzPackMap.put("config-type", "configType");
		tigaseIzPackMap.put("--virt-hosts", "virtualDomains");
		tigaseIzPackMap.put("--admins", "admins");
		tigaseIzPackMap.put("--user-db", DB_TYPE);
		tigaseIzPackMap.put("--auth-db", AUTH_HANDLE);
		tigaseIzPackMap.put("--muc", MUC_COMP);
		tigaseIzPackMap.put("--pubsub", PUBSUB_COMP);
		tigaseIzPackMap.put("--socks5", SOCKS5_COMP);
		tigaseIzPackMap.put("--stun", STUN_COMP);
		tigaseIzPackMap.put("--http", HTTP_COMP);
		tigaseIzPackMap.put("--archive", ARCHIVE_COMP);
		tigaseIzPackMap.put("--cluster-mode", "clusterMode");
//		tigaseIzPackMap.put("--cluster-nodes", "clusterNodes");
		tigaseIzPackMap.put("--debug", DEBUG);
		tigaseIzPackMap.put("--sm-plugins", PLUGINS);
		tigaseIzPackMap.put("--user-db-uri", USER_DB_URI);
		tigaseIzPackMap.put("--auth-db-uri", AUTH_DB_URI);
		tigaseIzPackMap.put("--sm-cluster-strategy-class", ACS_COMP);
		tigaseIzPackMap.put("--muc_acs", ACS_MUC_COMP);
		tigaseIzPackMap.put("--pubsub_acs", ACS_PUBSUB_COMP);
// 		for (Map.Entry entry: tigaseIzPackMap.entrySet()) {
// 			izPackTigaseMap.put(entry.getValue(), entry.getKey());
// 		}

		userDBMap.put("Derby", "derby");
		userDBMap.put("MySQL", "mysql");
		userDBMap.put("PostgreSQL", "pgsql");
		userDBMap.put("SQLServer", "sqlserver");

		userDBUriMap.put("derby", "Derby");
		userDBUriMap.put("mysql", "MySQL");
		userDBUriMap.put("postgresql", "PostgreSQL");
		userDBUriMap.put("pgsql", "PostgreSQL");
		userDBUriMap.put("sqlserver", "SQLServer");
		userDBUriMap.put("jtds:sqlserver", "SQLServer");

		debugMap.put("server", SERVER_DEBUG);
		debugMap.put("db", DB_DEBUG);
		debugMap.put("xmpp.impl", PLUGIN_DEBUG);
		debugMap.put("cluster", CLUSTER_DEBUG);

		pluginsMap.put("jabber:iq:auth", "nonSaslAuthPlugin");
		pluginsMap.put("urn:ietf:params:xml:ns:xmpp-sasl", "saslAuthPlugin");
		pluginsMap.put("urn:ietf:params:xml:ns:xmpp-bind", "resBindPlugin");
		pluginsMap.put("urn:ietf:params:xml:ns:xmpp-session", "sessionBindPlugin");
		pluginsMap.put("jabber:iq:register", "registerPlugin");
		pluginsMap.put("jabber:iq:roster", "rosterPlugin");
		pluginsMap.put("presence", "presencePlugin");
		pluginsMap.put("jabber:iq:privacy", "privacyPlugin");
		pluginsMap.put("jabber:iq:version", "versionPlugin");
		pluginsMap.put("http://jabber.org/protocol/stats", "statsPlugin");
		pluginsMap.put("starttls", "tlsPlugin");
		pluginsMap.put("msgoffline", "msgOfflinePlugin");
		pluginsMap.put("vcard-temp", "vcardPlugin");
		pluginsMap.put("http://jabber.org/protocol/commands", "commandsPlugin");
		pluginsMap.put("jabber:iq:private", "privatePlugin");
		pluginsMap.put("urn:xmpp:ping", "pingPlugin");
		pluginsMap.put("basic-filter", "basicFilterPlugin");
		pluginsMap.put("domain-filter", "domainFilterPlugin");
		pluginsMap.put("pep", "pepPlugin");
		pluginsMap.put("zlib", "zlibPlugin");
		pluginsMap.put("message-archive-xep-0136", "messageArchivePlugin");
	}

	public static String getPluginId(String pluginVarNameToFind) {
		for (String pluginId : pluginsMap.keySet()) {
			String pluginVarName = pluginsMap.get(pluginId);
			if (pluginVarName.equals(pluginVarNameToFind)) {
				return pluginId;
			}
		}
		return null;
	}

	public static final String PGSQL_DRIVER = "org.postgresql.Driver";
	public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String SQLSERVER_DRIVER = "net.sourceforge.jtds.jdbc.Driver";

	public static final String JDBC_CHECKUSERTABLE_QUERY
    = "select count(*) from tig_users";

	public static final String JDBC_GETSCHEMAVER_QUERY
    = "select TigGetDBProperty('schema-version')";
	public static final String DERBY_GETSCHEMAVER_QUERY
    = "values TigGetDBProperty('schema-version')";
	public static final String SQLSERVER_GETSCHEMAVER_QUERY
    = "select dbo.TigGetDBProperty('schema-version')";

}
