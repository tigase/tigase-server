/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.vhosts;

import tigase.util.DNSResolver;
import static tigase.conf.Configurable.*;

/**
 * Created: Oct 3, 2009 4:26:09 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class VHostRepoDefaults {

	private static final String vhost_user = "vhost-manager";
	private static final String vhost_list_pkey = "vhosts-lists";

	public static String getRepoUser() {
		return vhost_user;
	}

	public static String getItemsListPKey() {
		return vhost_list_pkey;
	}

	public static String[] getDefaultPropetyItems() {
		return DNSResolver.getDefHostNames();
	}

	public static String getPropertyKey() {
		return GEN_VIRT_HOSTS;
	}

	public static String getConfigKey() {
		return HOSTNAMES_PROP_KEY;
	}

	public static VHostItem getItemInstance() {
		return new VHostItem();
	}

}
