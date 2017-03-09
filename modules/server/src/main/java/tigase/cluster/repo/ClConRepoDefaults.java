/*
 * ClConRepoDefaults.java
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



package tigase.cluster.repo;

import tigase.xmpp.BareJID;

import tigase.util.DNSResolverFactory;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/03/09
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public abstract class ClConRepoDefaults {
	private static final BareJID clcon_user = BareJID.bareJIDInstanceNS("cl-conn-manager");

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	static String[] getDefaultPropetyItems() {
		return new String[] { DNSResolverFactory.getInstance().getDefaultHost()};
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	static String getPropertyKey() {
		return "--cluster-nodes";
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	static String getConfigKey() {
		return "cluster-nodes";
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	static ClusterRepoItem getItemInstance() {
		return new ClusterRepoItem();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public static BareJID getRepoUser() {
		return clcon_user;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/09
