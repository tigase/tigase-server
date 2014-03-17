/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 24, 2009 3:57:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class CompRepoDefaults {
	private static final BareJID comp_user = BareJID.bareJIDInstanceNS("ext-comp-manager");
	private static final String comp_list_pkey = "ext-comp-lists";

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static String getConfigKey() {
		return "comp-items";
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static String[] getDefaultPropetyItems() {
		return new String[] { "muc.domain.tld:passwd:listen:5277:accept" };
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static CompRepoItem getItemInstance() {
		return new CompRepoItem();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static String getItemsListPKey() {
		return comp_list_pkey;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static String getPropertyKey() {
		return "--external";
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static BareJID getRepoUser() {
		return comp_user;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
