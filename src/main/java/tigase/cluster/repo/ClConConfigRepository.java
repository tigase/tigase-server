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



package tigase.cluster.repo;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.ConfigRepository;

import tigase.sys.TigaseRuntime;

import tigase.util.DNSResolver;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/03/09
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class ClConConfigRepository
				extends ConfigRepository<ClusterRepoItem> {
	/** Field description */
	public static final String AUTORELOAD_INTERVAL_PROP_KEY = "repo-autoreload-interval";

	/** Field description */
	public static final long AUTORELOAD_INTERVAL_PROP_VAL = 15;

	//~--- fields ---------------------------------------------------------------

	private long autoreload_interval = AUTORELOAD_INTERVAL_PROP_VAL;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 */
	@Override
	public String[] getDefaultPropetyItems() {
		return ClConRepoDefaults.getDefaultPropetyItems();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	@Override
	public String getPropertyKey() {
		return ClConRepoDefaults.getPropertyKey();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	@Override
	public String getConfigKey() {
		return ClConRepoDefaults.getConfigKey();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	@Override
	public ClusterRepoItem getItemInstance() {
		return ClConRepoDefaults.getItemInstance();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void reload() {
		super.reload();

		String          host = DNSResolver.getDefaultHostname();
		ClusterRepoItem item = getItem(host);

		if (item == null) {
			item = getItemInstance();
			item.setHostname(host);
		}
		item.setLastUpdate(System.currentTimeMillis());
		item.setCpuUsage(TigaseRuntime.getTigaseRuntime().getCPUUsage());
		item.setMemUsage(TigaseRuntime.getTigaseRuntime().getHeapMemUsage());
		storeItem(item);
	}

	/**
	 * Method description
	 *
	 *
	 * @param item
	 */
	public void itemLoaded(ClusterRepoItem item) {
		if (System.currentTimeMillis() - item.getLastUpdate() <= 5000 * autoreload_interval && clusterRecordValid(item)) {
			addItem(item);
		} else {
			removeItem(item.getHostname());
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param oldItem
	 * @param newItem
	 *
	 *
	 */
	@Override
	public boolean itemChanged(ClusterRepoItem oldItem, ClusterRepoItem newItem) {
		return !oldItem.getPassword().equals(newItem.getPassword()) || (oldItem
				.getPortNo() != newItem.getPortNo());
	}

	//~--- get methods ----------------------------------------------------------

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
		defs.put(AUTORELOAD_INTERVAL_PROP_KEY, AUTORELOAD_INTERVAL_PROP_VAL);

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

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		autoreload_interval = (Long) props.get(AUTORELOAD_INTERVAL_PROP_KEY);
		setAutoloadTimer(autoreload_interval);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param item
	 */
	public void storeItem(ClusterRepoItem item) {}

	private boolean clusterRecordValid(ClusterRepoItem item) {
		return !item.getHostname().equalsIgnoreCase("localhost");
	}
}


//~ Formatted in Tigase Code Convention on 13/03/11
