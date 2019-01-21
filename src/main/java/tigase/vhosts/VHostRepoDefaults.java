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
package tigase.vhosts;

import tigase.util.dns.DNSResolverFactory;
import tigase.xmpp.jid.BareJID;

import static tigase.conf.Configurable.GEN_VIRT_HOSTS;
import static tigase.conf.Configurable.HOSTNAMES_PROP_KEY;

/**
 * Created: Oct 3, 2009 4:26:09 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class VHostRepoDefaults {

	private static final BareJID vhost_user = BareJID.bareJIDInstanceNS("vhost-manager");
	private static final String vhost_list_pkey = "vhosts-lists";

	public static String getConfigKey() {
		return HOSTNAMES_PROP_KEY;
	}

	public static String[] getDefaultPropetyItems() {
		return DNSResolverFactory.getInstance().getDefaultHosts();
	}

	public static VHostItem getItemInstance() {
		return new VHostItem();
	}

	public static String getItemsListPKey() {
		return vhost_list_pkey;
	}

	public static String getPropertyKey() {
		return GEN_VIRT_HOSTS;
	}

	public static BareJID getRepoUser() {
		return vhost_user;
	}
}

