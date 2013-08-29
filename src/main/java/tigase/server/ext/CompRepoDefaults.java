/*
 * CompRepoDefaults.java
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



package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

/**
 * Created: Oct 24, 2009 3:57:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class CompRepoDefaults {
	private static final String  comp_list_pkey = "ext-comp-lists";
	private static final BareJID comp_user = BareJID.bareJIDInstanceNS("ext-comp-manager");

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public static String getConfigKey() {
		return "comp-items";
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	public static String[] getDefaultPropetyItems() {
		return new String[] { "muc.domain.tld:passwd:listen:5277:accept" };
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>CompRepoItem</code>
	 */
	public static CompRepoItem getItemInstance() {
		return new CompRepoItem();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public static String getItemsListPKey() {
		return comp_list_pkey;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public static String getPropertyKey() {
		return "--external";
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>BareJID</code>
	 */
	public static BareJID getRepoUser() {
		return comp_user;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
