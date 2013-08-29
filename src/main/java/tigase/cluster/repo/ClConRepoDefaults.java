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

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.DNSResolver;

import tigase.xmpp.BareJID;

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
	 *
	 * @return a value of <code>BareJID</code>
	 */
	public static BareJID getRepoUser() {
		return clcon_user;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	static String getConfigKey() {
		return "cluster-nodes";
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	static String[] getDefaultPropetyItems() {
		return new String[] { DNSResolver.getDefaultHostname() };
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>ClusterRepoItem</code>
	 */
	static ClusterRepoItem getItemInstance() {
		return new ClusterRepoItem();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	static String getPropertyKey() {
		return "--cluster-nodes";
	}
}


//~ Formatted in Tigase Code Convention on 13/08/29
