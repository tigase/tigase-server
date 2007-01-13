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
	public static final String USER_REPO_CLASS_PROP_VAL =
		"tigase.db.xml.XMLRepository";
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";
	public static final String USER_REPO_URL_PROP_VAL = "user-repository.xml";

	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";
	public static final String AUTH_REPO_CLASS_PROP_VAL =
		"tigase.db.xml.XMLRepository";
	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";
	public static final String AUTH_REPO_URL_PROP_VAL = "user-repository.xml";

	public static final String COMPONENTS_PROP_KEY = "components";
	public static final String[] COMPONENTS_PROP_VAL =
	{"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "message", "jabber:iq:roster", "jabber:iq:privacy", "presence", "msgoffline",
	 "jabber:iq:version", "jabber:iq:stats", "starttls", "disco", "vcard-temp",
	 "http://jabber.org/protocol/bytestreams", "http://jabber.org/protocol/ibb",
	 "http://jabber.org/protocol/si", "jabber:iq:oob"};

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	public static final String ADMINS_PROP_KEY = "admins";
	public static String[] ADMINS_PROP_VAL =	{"admin@localhost", "admin@hostname"};

// 	public static final String SECURITY_PROP_KEY = "security";

// 	public static final String AUTHENTICATION_IDS_PROP_KEY = "authentication-ids";
// 	public static final String[] AUTHENTICATION_IDS_PROP_VAL =
// 	{"auth-plain", "auth-digest", "auth-sasl"};

// 	public static final String AUTH_PLAIN_CLASS_PROP_KEY = "auth-plain/class";
// 	public static final String AUTH_PLAIN_CLASS_PROP_VAL =
// 		"tigase.auth.PlainAuth";
// 	public static final String AUTH_PLAIN_FLAG_PROP_KEY = "auth-plain/flag";
// 	public static final String AUTH_PLAIN_FLAG_PROP_VAL =	"sufficient";

// 	public static final String AUTH_DIGEST_CLASS_PROP_KEY = "auth-digest/class";
// 	public static final String AUTH_DIGEST_CLASS_PROP_VAL =
// 		"tigase.auth.DigestAuth";
// 	public static final String AUTH_DIGEST_FLAG_PROP_KEY = "auth-digest/flag";
// 	public static final String AUTH_DIGEST_FLAG_PROP_VAL =	"sufficient";

// 	public static final String AUTH_SASL_CLASS_PROP_KEY = "auth-sasl/class";
// 	public static final String AUTH_SASL_CLASS_PROP_VAL =	"None";
// 	public static final String AUTH_SASL_FLAG_PROP_KEY = "auth-sasl/flag";
// 	public static final String AUTH_SASL_FLAG_PROP_VAL =	"sufficient";

	public static void getDefaults(Map<String, Object> props) {
	  props.put(USER_REPO_CLASS_PROP_KEY, USER_REPO_CLASS_PROP_VAL);
	  props.put(USER_REPO_URL_PROP_KEY, USER_REPO_URL_PROP_VAL);

	  props.put(AUTH_REPO_CLASS_PROP_KEY, AUTH_REPO_CLASS_PROP_VAL);
	  props.put(AUTH_REPO_URL_PROP_KEY, AUTH_REPO_URL_PROP_VAL);

		props.put(COMPONENTS_PROP_KEY, COMPONENTS_PROP_VAL);
		HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		ADMINS_PROP_VAL = new String[HOSTNAMES_PROP_VAL.length];
		for (int i = 0; i < ADMINS_PROP_VAL.length; i++) {
			ADMINS_PROP_VAL[i] = "admin@"+HOSTNAMES_PROP_VAL[i];
		} // end of for (int i = 0; i < ADMINS_PROP_VAL.length; i++)
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTHENTICATION_IDS_PROP_KEY,
// 			AUTHENTICATION_IDS_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTH_PLAIN_CLASS_PROP_KEY,
// 			AUTH_PLAIN_CLASS_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTH_PLAIN_FLAG_PROP_KEY,
// 			AUTH_PLAIN_FLAG_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTH_DIGEST_CLASS_PROP_KEY,
// 			AUTH_DIGEST_CLASS_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTH_DIGEST_FLAG_PROP_KEY,
// 			AUTH_DIGEST_FLAG_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTH_SASL_CLASS_PROP_KEY,
// 			AUTH_SASL_CLASS_PROP_VAL);
// 		props.put(SECURITY_PROP_KEY + "/" + AUTH_SASL_FLAG_PROP_KEY,
// 			AUTH_SASL_FLAG_PROP_VAL);
	}

} // SessionManagerConfig
