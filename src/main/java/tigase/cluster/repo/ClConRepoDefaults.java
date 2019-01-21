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
package tigase.cluster.repo;

import tigase.util.dns.DNSResolverFactory;
import tigase.xmpp.jid.BareJID;

/**
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version 5.2.0, 13/03/09
 */
public abstract class ClConRepoDefaults {

	private static final BareJID clcon_user = BareJID.bareJIDInstanceNS("cl-conn-manager");

	static String getConfigKey() {
		return "cluster-nodes";
	}

	static String[] getDefaultPropetyItems() {
		return new String[]{DNSResolverFactory.getInstance().getDefaultHost()};
	}

	static ClusterRepoItem getItemInstance() {
		return new ClusterRepoItem();
	}

	static String getPropertyKey() {
		return "--cluster-nodes";
	}

	public static BareJID getRepoUser() {
		return clcon_user;
	}
}

