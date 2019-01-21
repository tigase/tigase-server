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
package tigase.server.ext;

import tigase.xmpp.jid.BareJID;

/**
 * Created: Oct 24, 2009 3:57:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class CompRepoDefaults {

	private static final BareJID comp_user = BareJID.bareJIDInstanceNS("ext-comp-manager");
	private static final String comp_list_pkey = "ext-comp-lists";

	public static String getConfigKey() {
		return "comp-items";
	}

	public static String[] getDefaultPropetyItems() {
		return null;
	}

	public static CompRepoItem getItemInstance() {
		return new CompRepoItem();
	}

	public static String getItemsListPKey() {
		return comp_list_pkey;
	}

	public static String getPropertyKey() {
		return "--external";
	}

	public static BareJID getRepoUser() {
		return comp_user;
	}
}

