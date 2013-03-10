/*
 * ClConConfigRepository.java
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.repo;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.ConfigRepository;

import tigase.util.DNSResolver;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

/**
 *
 * @author kobit
 */
public class ClConConfigRepository
				extends ConfigRepository<ClusterRepoItem> {
	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] getDefaultPropetyItems() {
		return ClConRepoDefaults.getDefaultPropetyItems();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getPropertyKey() {
		return ClConRepoDefaults.getPropertyKey();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getConfigKey() {
		return ClConRepoDefaults.getConfigKey();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public ClusterRepoItem getItemInstance() {
		return ClConRepoDefaults.getItemInstance();
	}

	/**
	 * Method description
	 *
	 *
	 * @param defs
	 * @param params
	 */
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);

		String[] items_arr = (String[]) defs.get(getConfigKey());

		for (String it : items_arr) {
			ClusterRepoItem item = getItemInstance();

			item.initFromPropertyString(it);
			addItem(item);
		}
		if (getItem(DNSResolver.getDefaultHostname()) == null) {
			ClusterRepoItem item = getItemInstance();

			item.initFromPropertyString(DNSResolver.getDefaultHostname());
			addItem(item);
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/03/09
