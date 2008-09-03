/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.xmppsession;

import tigase.util.DNSResolver;
import java.util.Map;

import static tigase.conf.Configurable.*;

/**
 * Describe class SessionManagerConfig here.
 *
 *
 * Created: Tue Oct 24 23:07:57 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManagerConfig {

	public static final String USER_REPO_CLASS_PROP_KEY = "user-repo-class";
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";
	public static final String USER_REPO_PARAMS_NODE = "user-repo-params";

	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";
	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";
	public static final String AUTH_REPO_PARAMS_NODE = "auth-repo-params";

	public static final String PLUGINS_PROP_KEY = "plugins";
	public static final String PLUGINS_CONF_PROP_KEY = "plugins-conf";

	public static final String ANONYMOUS_DOMAINS_PROP_KEY = "anonymous-domains";
	//	public static final String ANONYMOUS_PEERS_PROP_KEY = "anonymous-peers";

	/**
	 * List of default plugins loaded by the server. It can be changed later
	 * in config file or at runtime.
	 */
	private static final String[] PLUGINS_NO_REG_PROP_VAL =
	{"jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "roster-presence", "jabber:iq:privacy", "msgoffline",
	 "jabber:iq:version", "http://jabber.org/protocol/stats", "starttls",
	 "vcard-temp", "http://jabber.org/protocol/commands", "jabber:iq:private",
	 "urn:xmpp:ping", "http://jabber.org/protocol/pubsub"};
	/**
	 * List of plugins loaded when the server is loaded in test mode.
	 * Some plugins like off-line message storage is disabled during tests.
	 */
	private static final String[] PLUGINS_FULL_PROP_VAL =
	{"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "roster-presence", "jabber:iq:privacy", "jabber:iq:version",
	 "http://jabber.org/protocol/stats", "starttls", "msgoffline",
	 "vcard-temp", "http://jabber.org/protocol/commands", "jabber:iq:private",
	 "urn:xmpp:ping", "http://jabber.org/protocol/pubsub"};

	private static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};
	private static String[] ANONYMOUS_DOMAINS_PROP_VAL = {"localhost", "hostname"};

	private static String[] ADMINS_PROP_VAL =	{"admin@localhost", "admin@hostname"};
	private static String[] TRUSTED_PROP_VAL = {"admin@localhost", "admin@hostname"};
	private static String[] CLUSTER_NODES_PROP_VAL = {"sess-man@localhost"};

	public static void getDefaults(Map<String, Object> props,
		Map<String, Object> params) {

		boolean full_comps = true;
		String user_repo_class = XML_REPO_CLASS_PROP_VAL;
		String user_repo_url = XML_REPO_URL_PROP_VAL;
		String auth_repo_class = XML_REPO_CLASS_PROP_VAL;
		String auth_repo_url = XML_REPO_URL_PROP_VAL;
		if (params.get(GEN_USER_DB) != null) {
			if (params.get(GEN_USER_DB).equals("mysql")) {
				user_repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				user_repo_url = MYSQL_REPO_URL_PROP_VAL;
				auth_repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = MYSQL_REPO_URL_PROP_VAL;
			} else {
				if (params.get(GEN_USER_DB).equals("pgsql")) {
					user_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
					user_repo_url = PGSQL_REPO_URL_PROP_VAL;
					auth_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
					auth_repo_url = PGSQL_REPO_URL_PROP_VAL;
				} else {
					user_repo_class = (String)params.get(GEN_USER_DB);
					auth_repo_class = (String)params.get(GEN_USER_DB);
				}
			}
		}
		if (params.get(GEN_USER_DB_URI) != null) {
			user_repo_url = (String)params.get(GEN_USER_DB_URI);
			auth_repo_url = user_repo_url;
		}
		if (params.get(GEN_AUTH_DB) != null) {
			if (params.get(GEN_AUTH_DB).equals("mysql")) {
				auth_repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = MYSQL_REPO_URL_PROP_VAL;
			} else {
				if (params.get(GEN_AUTH_DB).equals("pgsql")) {
					auth_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
					auth_repo_url = PGSQL_REPO_URL_PROP_VAL;
				} else {
					if (params.get(GEN_AUTH_DB).equals("tigase-auth")) {
						auth_repo_class = TIGASE_AUTH_REPO_CLASS_PROP_VAL;
						auth_repo_url = TIGASE_AUTH_REPO_URL_PROP_VAL;
						// For any external authentication connector like TigaseAuth,
						// Drupal or LibreSource authentication all account
						// management is done via Web interface so accounts containers
						// for Jabber data have to be created automatically
						user_repo_url += "&autoCreateUser=true";
					} else {
						if (params.get(GEN_AUTH_DB).equals("drupal")) {
							auth_repo_class = DRUPAL_REPO_CLASS_PROP_VAL;
							auth_repo_url = DRUPAL_REPO_URL_PROP_VAL;
							// For Drupal or LibreSource authentication all account
							// management is done via Web interface so accounts containers
							// for Jabber data have to be created automatically
							user_repo_url += "&autoCreateUser=true";
							full_comps = false;
						} else {
							if (params.get(GEN_AUTH_DB).equals("libresource")) {
								auth_repo_class = LIBRESOURCE_REPO_CLASS_PROP_VAL;
								auth_repo_url = LIBRESOURCE_REPO_URL_PROP_VAL;
								// For Drupal or LibreSource authentication all account
								// management is done via Web interface so accounts containers
								// for Jabber data have to be created automatically
								user_repo_url += "&autoCreateUser=true";
								full_comps = false;
							} else {
								auth_repo_class = (String)params.get(GEN_AUTH_DB);
							}
						}
					}
				}
			}
		}
		if (params.get(GEN_AUTH_DB_URI) != null) {
			auth_repo_url = (String)params.get(GEN_AUTH_DB_URI);
		}

		props.put(USER_REPO_CLASS_PROP_KEY, user_repo_class);
	  props.put(USER_REPO_URL_PROP_KEY, user_repo_url);
		props.put(USER_REPO_PARAMS_NODE + "/param-1", "value-1");

	  props.put(AUTH_REPO_CLASS_PROP_KEY, auth_repo_class);
	  props.put(AUTH_REPO_URL_PROP_KEY, auth_repo_url);
		props.put(AUTH_REPO_PARAMS_NODE + "/param-1", "value-1");

		String str_plugins = (String)params.get(GEN_SM_PLUGINS);
		if (str_plugins != null) {
			props.put(PLUGINS_PROP_KEY, str_plugins.split(","));
		} else {
			if (full_comps) {
				// Some plugins are not loaded during tests at least until proper
				// test cases are created for them. Sample case is off-line message
				// storage which may impact some test cases.
				props.put(PLUGINS_PROP_KEY, PLUGINS_FULL_PROP_VAL);
			} else {
				props.put(PLUGINS_PROP_KEY, PLUGINS_NO_REG_PROP_VAL);
			}
		}

		if (params.get(GEN_VIRT_HOSTS) != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
			ANONYMOUS_DOMAINS_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
			ANONYMOUS_DOMAINS_PROP_VAL = DNSResolver.getDefHostNames();
		}
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(ANONYMOUS_DOMAINS_PROP_KEY, ANONYMOUS_DOMAINS_PROP_VAL);
		if (params.get(GEN_ADMINS) != null) {
			ADMINS_PROP_VAL = ((String)params.get(GEN_ADMINS)).split(",");
		} else {
			ADMINS_PROP_VAL = new String[HOSTNAMES_PROP_VAL.length];
			for (int i = 0; i < ADMINS_PROP_VAL.length; i++) {
				ADMINS_PROP_VAL[i] = "admin@"+HOSTNAMES_PROP_VAL[i];
			} // end of for (int i = 0; i < ADMINS_PROP_VAL.length; i++)
		}
		props.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);
		//		props.put(ANONYMOUS_PEERS_PROP_KEY, ADMINS_PROP_VAL);
		if (params.get(GEN_TRUSTED) != null) {
			TRUSTED_PROP_VAL = ((String)params.get(GEN_TRUSTED)).split(",");
		} else {
			TRUSTED_PROP_VAL = new String[HOSTNAMES_PROP_VAL.length];
			for (int i = 0; i < TRUSTED_PROP_VAL.length; i++) {
				TRUSTED_PROP_VAL[i] = "admin@"+HOSTNAMES_PROP_VAL[i];
			} // end of for (int i = 0; i < TRUSTED_PROP_VAL.length; i++)
		}
		props.put(TRUSTED_PROP_KEY, TRUSTED_PROP_VAL);
	}

} // SessionManagerConfig
