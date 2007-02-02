/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
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
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";
	public static final String XML_REPO_URL_PROP_VAL = "user-repository.xml";
	public static final String MYSQL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/tigase?user=root&password=mypass";
	public static final String PGSQL_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/tigase?user=tigase";
	public static final String DRUPAL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/drupal?user=root&password=mypass";

	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";
	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";

	public static final String COMPONENTS_PROP_KEY = "components";
	public static final String[] COMPONENTS_PROP_VAL =
	{"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "jabber:iq:roster", "jabber:iq:privacy", "presence", "msgoffline",
	 "jabber:iq:version", "http://jabber.org/protocol/stats", "starttls", "disco",
	 "vcard-temp", "http://jabber.org/protocol/si",
	 "http://jabber.org/protocol/commands"};

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	public static final String ADMINS_PROP_KEY = "admins";
	public static String[] ADMINS_PROP_VAL =	{"admin@localhost", "admin@hostname"};

	public static void getDefaults(Map<String, Object> props,
		Map<String, Object> params) {

		String user_repo_class = XML_REPO_CLASS_PROP_VAL;
		String user_repo_url = XML_REPO_URL_PROP_VAL;
		String auth_repo_class = XML_REPO_CLASS_PROP_VAL;
		String auth_repo_url = XML_REPO_URL_PROP_VAL;
		if (params.get("--user-db") != null) {
			if (params.get("--user-db").equals("mysql")) {
				user_repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				user_repo_url = MYSQL_REPO_URL_PROP_VAL;
				auth_repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = MYSQL_REPO_URL_PROP_VAL;
			}
			if (params.get("--user-db").equals("pgsql")) {
				user_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
				user_repo_url = PGSQL_REPO_URL_PROP_VAL;
				auth_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = PGSQL_REPO_URL_PROP_VAL;
			}
		}
		if (params.get("--user-db-uri") != null) {
			user_repo_url = (String)params.get("--user-db-uri");
			auth_repo_url = user_repo_url;
		}
		if (params.get("--auth-db") != null) {
			if (params.get("--auth-db").equals("mysql")) {
				auth_repo_class = MYSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = MYSQL_REPO_URL_PROP_VAL;
			}
			if (params.get("--auth-db").equals("pgsql")) {
				auth_repo_class = PGSQL_REPO_CLASS_PROP_VAL;
				auth_repo_url = PGSQL_REPO_URL_PROP_VAL;
			}
		}
		if (params.get("--auth-db-uri") != null) {
			auth_repo_url = (String)params.get("--auth-db-uri");
		}

		props.put(USER_REPO_CLASS_PROP_KEY, user_repo_class);
	  props.put(USER_REPO_URL_PROP_KEY, user_repo_url);

	  props.put(AUTH_REPO_CLASS_PROP_KEY, auth_repo_class);
	  props.put(AUTH_REPO_URL_PROP_KEY, auth_repo_url);

		props.put(COMPONENTS_PROP_KEY, COMPONENTS_PROP_VAL);
		if (params.get("--virt-hosts") != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get("--virt-hosts")).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		}
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		if (params.get("--admins") != null) {
			ADMINS_PROP_VAL = ((String)params.get("--admins")).split(",");
		} else {
			ADMINS_PROP_VAL = new String[HOSTNAMES_PROP_VAL.length];
			for (int i = 0; i < ADMINS_PROP_VAL.length; i++) {
				ADMINS_PROP_VAL[i] = "admin@"+HOSTNAMES_PROP_VAL[i];
			} // end of for (int i = 0; i < ADMINS_PROP_VAL.length; i++)
		}
		props.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);
	}

} // SessionManagerConfig
