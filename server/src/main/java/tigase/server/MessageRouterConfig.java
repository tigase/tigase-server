/*
 * MessageRouterConfig.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Map;

/**
 * Describe class MessageRouterConfig here.
 *
 *
 * Created: Fri Jan  6 14:54:21 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageRouterConfig {
	/** Field description */
	public static final String DISCO_NAME_PROP_KEY = "disco-name";

	/** Field description */
	public static final String DISCO_SHOW_VERSION_PROP_KEY = "disco-show-version";

	/** Field description */
	public static final String LOCAL_ADDRESSES_PROP_KEY = "hostnames";

	/** Field description */
	public static final String MSG_RECEIVERS_PROP_KEY = "components/msg-receivers/";

	/** Field description */
	public static final String REGISTRATOR_PROP_KEY = "components/registrators/";

	/** Field description */
	public static final String UPDATES_CHECKING_INTERVAL_PROP_KEY =
			"updates-checking-interval";

	/** Field description */
	public static final long UPDATES_CHECKING_INTERVAL_PROP_VAL = 7;

	/** Field description */
	public static final String    UPDATES_CHECKING_PROP_KEY        = "updates-checking";
	/** Field description */
	public static final String MSG_RECEIVERS_NAMES_PROP_KEY = MSG_RECEIVERS_PROP_KEY +
			"id-names";
	/** Field description */
	public static final Boolean UPDATES_CHECKING_PROP_VAL = true;

	/** Field description */
	public static final String REGISTRATOR_NAMES_PROP_KEY = REGISTRATOR_PROP_KEY +
			"id-names";

	/** Field description */
	public static final boolean DISCO_SHOW_VERSION_PROP_VAL = true;

	/** Field description */
	public static final String DISCO_NAME_PROP_VAL = tigase.server.XMPPServer.NAME;

	//~--- static initializers --------------------------------------------------

	//~--- fields ---------------------------------------------------------------

	private Map<String, Object> props = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param props
	 */
	public MessageRouterConfig(Map<String, Object> props) {
		this.props = props;

		// System.out.println("MessageRouterConfig() properties: " + props.toString());
	}

	//~--- get methods ----------------------------------------------------------

	private static boolean isTrue(String val) {
		if (val == null) {
			return false;
		}

		String value = val.toLowerCase();

		return (value.equals("true") || value.equals("yes") || value.equals("on") || value
				.equals("1"));
	}
}    // MessageRouterConfig


//~ Formatted in Tigase Code Convention on 13/10/15
