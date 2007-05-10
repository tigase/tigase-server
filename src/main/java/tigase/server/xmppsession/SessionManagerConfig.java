/*  Package Jabber Server
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
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
	public static final String XML_REPO_CLASS_PROP_VAL =
		"tigase.db.xml.XMLRepository";
	public static final String MYSQL_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.JDBCRepository";
	public static final String PGSQL_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.JDBCRepository";
	public static final String DRUPAL_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.DrupalAuth";
	public static final String LIBRESOURCE_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.LibreSourceAuth";
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";
	public static final String XML_REPO_URL_PROP_VAL = "user-repository.xml";
	public static final String MYSQL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/tigase?user=root&password=mypass";
	public static final String PGSQL_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/tigase?user=tigase";
	public static final String DRUPAL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/drupal?user=root&password=mypass";
	public static final String LIBRESOURCE_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/libresource?user=demo";

	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";
	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";

	public static final String COMPONENTS_PROP_KEY = "components";
	/**
	 * List of default components loaded by the server. It can be changed later
	 * in config file or at runtime.
	 */
	public static final String[] COMPONENTS_NO_REG_PROP_VAL =
	{"jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "jabber:iq:roster", "jabber:iq:privacy", "presence", "msgoffline",
	 "jabber:iq:version", "http://jabber.org/protocol/stats", "starttls",
	 "vcard-temp", "http://jabber.org/protocol/commands", "jabber:iq:private",
	 "urn:xmpp:ping"};
	/**
	 * List of components loaded when the server is loaded in test mode.
	 * Some components like off-line message storage is disabled during tests.
	 */
	public static final String[] COMPONENTS_FULL_PROP_VAL =
	{"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "jabber:iq:roster", "jabber:iq:privacy", "presence", "jabber:iq:version",
	 "http://jabber.org/protocol/stats", "starttls", "msgoffline",
	 "vcard-temp", "http://jabber.org/protocol/commands", "jabber:iq:private",
	 "urn:xmpp:ping"};

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	public static final String ADMINS_PROP_KEY = "admins";
	public static String[] ADMINS_PROP_VAL =	{"admin@localhost", "admin@hostname"};

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
			}
			if (params.get(GEN_USER_DB).equals("pgsql")) {
				user_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
				user_repo_url = PGSQL_REPO_URL_PROP_VAL;
				auth_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = PGSQL_REPO_URL_PROP_VAL;
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
			}
			if (params.get(GEN_AUTH_DB).equals("pgsql")) {
				auth_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = PGSQL_REPO_URL_PROP_VAL;
			}
			if (params.get(GEN_AUTH_DB).equals("drupal")) {
				auth_repo_class = DRUPAL_REPO_CLASS_PROP_VAL;
				auth_repo_url = DRUPAL_REPO_URL_PROP_VAL;
				// For Drupal or LibreSource authentication all account
				// management is done via Web interface so accounts containers
				// for Jabber data have to be created automatically
				user_repo_url += "&autoCreateUser=true";
				full_comps = false;
			}
			if (params.get(GEN_AUTH_DB).equals("libresource")) {
				auth_repo_class = LIBRESOURCE_REPO_CLASS_PROP_VAL;
				auth_repo_url = LIBRESOURCE_REPO_URL_PROP_VAL;
				// For Drupal or LibreSource authentication all account
				// management is done via Web interface so accounts containers
				// for Jabber data have to be created automatically
				user_repo_url += "&autoCreateUser=true";
				full_comps = false;
			}
		}
		if (params.get(GEN_AUTH_DB_URI) != null) {
			auth_repo_url = (String)params.get(GEN_AUTH_DB_URI);
		}

		props.put(USER_REPO_CLASS_PROP_KEY, user_repo_class);
	  props.put(USER_REPO_URL_PROP_KEY, user_repo_url);

	  props.put(AUTH_REPO_CLASS_PROP_KEY, auth_repo_class);
	  props.put(AUTH_REPO_URL_PROP_KEY, auth_repo_url);

		if (full_comps) {
			// Some components are not loaded during tests at least until proper
			// test cases are created for them. Sample case is off-line message
			// storage which may impact some test cases.
			props.put(COMPONENTS_PROP_KEY, COMPONENTS_FULL_PROP_VAL);
		} else {
			props.put(COMPONENTS_PROP_KEY, COMPONENTS_NO_REG_PROP_VAL);
		}

		if (params.get(GEN_VIRT_HOSTS) != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		}
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		if (params.get(GEN_ADMINS) != null) {
			ADMINS_PROP_VAL = ((String)params.get(GEN_ADMINS)).split(",");
		} else {
			ADMINS_PROP_VAL = new String[HOSTNAMES_PROP_VAL.length];
			for (int i = 0; i < ADMINS_PROP_VAL.length; i++) {
				ADMINS_PROP_VAL[i] = "admin@"+HOSTNAMES_PROP_VAL[i];
			} // end of for (int i = 0; i < ADMINS_PROP_VAL.length; i++)
		}
		props.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);
	}

} // SessionManagerConfig
