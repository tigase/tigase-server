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
package tigase.server;

import java.util.Map;

/**
 * Describe class MessageRouterConfig here.
 * <br>
 * Created: Fri Jan  6 14:54:21 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageRouterConfig {

	public static final String DISCO_NAME_PROP_KEY = "disco-name";

	public static final String DISCO_SHOW_VERSION_PROP_KEY = "disco-show-version";

	public static final String LOCAL_ADDRESSES_PROP_KEY = "hostnames";

	public static final String MSG_RECEIVERS_PROP_KEY = "components/msg-receivers/";

	public static final String REGISTRATOR_PROP_KEY = "components/registrators/";

	public static final String UPDATES_CHECKING_INTERVAL_PROP_KEY = "updates-checking-interval";

	public static final long UPDATES_CHECKING_INTERVAL_PROP_VAL = 7;

	public static final String UPDATES_CHECKING_PROP_KEY = "updates-checking";
	public static final String MSG_RECEIVERS_NAMES_PROP_KEY = MSG_RECEIVERS_PROP_KEY + "id-names";
	public static final Boolean UPDATES_CHECKING_PROP_VAL = true;

	public static final String REGISTRATOR_NAMES_PROP_KEY = REGISTRATOR_PROP_KEY + "id-names";

	public static final boolean DISCO_SHOW_VERSION_PROP_VAL = true;

	public static final String DISCO_NAME_PROP_VAL = tigase.server.XMPPServer.NAME;

	private Map<String, Object> props = null;

	private static boolean isTrue(String val) {
		if (val == null) {
			return false;
		}

		String value = val.toLowerCase();

		return (value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1"));
	}


	public MessageRouterConfig(Map<String, Object> props) {
		this.props = props;

		// System.out.println("MessageRouterConfig() properties: " + props.toString());
	}
}    // MessageRouterConfig

