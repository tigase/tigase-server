/*
 * ConfigRepository.java
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



package tigase.db.comp;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created: Oct 3, 2009 2:58:41 PM
 *
 * @param <Item>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConfigRepository<Item extends RepositoryItem>
				implements ComponentRepository<Item> {
	private static final Logger log = Logger.getLogger(ConfigRepository.class.getName());

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected ConcurrentSkipListMap<String, Item> items = new ConcurrentSkipListMap<String,
			Item>();
	private Timer                             autoLoadTimer  = null;
	private RepositoryChangeListenerIfc<Item> repoChangeList = null;

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param delay
	 */
	@Override
	public void setAutoloadTimer(long delay) {
		long interval = delay * 1000;

		if (autoLoadTimer != null) {
			autoLoadTimer.cancel();
			autoLoadTimer = null;
		}
		if (interval > 0) {
			autoLoadTimer = new Timer(getConfigKey(), true);
			autoLoadTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					reload();
				}
			}, interval, interval);
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param repoChangeListener
	 */
	@Override
	public void addRepoChangeListener(
			RepositoryChangeListenerIfc<Item> repoChangeListener) {
		this.repoChangeList = repoChangeListener;
	}

	/**
	 * Method description
	 *
	 *
	 * @param repoChangeListener
	 */
	@Override
	public void removeRepoChangeListener(
			RepositoryChangeListenerIfc<Item> repoChangeListener) {
		this.repoChangeList = null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return items.toString();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public abstract String getConfigKey();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public abstract String[] getDefaultPropetyItems();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public abstract String getPropertyKey();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param item
	 */
	@Override
	public void addItem(Item item) {
		Item old = items.put(item.getKey(), item);

		store();
		if (repoChangeList != null) {
			if (old == null) {
				log.log(Level.INFO, "Calling itemAdded for: {0}", item);
				repoChangeList.itemAdded(item);
			} else {
				if (itemChanged(old, item)) {
					log.log(Level.INFO, "Calling itemUpadted for: {0}", item);
					repoChangeList.itemUpdated(item);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Not calling itemUpadted for: {0}, item unchanged.",
								item);
					}
				}
			}
		} else {
			log.log(Level.INFO, "No repoChangeListener for: {0}", item);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param oldItem
	 * @param newItem
	 *
	 * @return
	 */
	public boolean itemChanged(Item oldItem, Item newItem) {
		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public Collection<Item> allItems() {
		return items.values();
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 *
	 * @return
	 */
	@Override
	public boolean contains(String key) {
		return items.keySet().contains(key);
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
		String[] items_arr = getDefaultPropetyItems();

		if (params.get(getPropertyKey()) != null) {
			items_arr = ((String) params.get(getPropertyKey())).split(",");
		}
		defs.put(getConfigKey(), items_arr);
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 *
	 * @return
	 */
	@Override
	public Item getItem(String key) {
		return items.get(key);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public Iterator<Item> iterator() {
		return items.values().iterator();
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void reload() {}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 */
	@Override
	public void removeItem(String key) {
		Item item = items.remove(key);

		if (item != null) {
			store();
			if (repoChangeList != null) {
				repoChangeList.itemRemoved(item);
			}
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {
		String[] items_arr = (String[]) properties.get(getConfigKey());

		if ((items_arr != null) && (items_arr.length > 0)) {
			items.clear();
			for (String it : items_arr) {
				log.log(Level.CONFIG, "Loading config item: {0}", it);

				Item item = getItemInstance();

				item.initFromPropertyString(it);
				addItem(item);
				log.log(Level.CONFIG, "Loaded config item: {0}", item);
			}
		} else {
			log.warning("Items list is not set in the configuration file!!");
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int size() {
		return items.size();
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void store() {}

	/**
	 * Method description
	 *
	 *
	 * @param item
	 *
	 * @return
	 */
	@Override
	public String validateItem(Item item) {
		return null;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/11
