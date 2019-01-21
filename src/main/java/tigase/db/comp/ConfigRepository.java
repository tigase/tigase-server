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
package tigase.db.comp;

import tigase.annotations.TigaseDeprecated;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Oct 3, 2009 2:58:41 PM
 *
 * @param <Item>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConfigRepository<Item extends RepositoryItem>
		implements ComponentRepository<Item>, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(ConfigRepository.class.getName());

	@ConfigField(desc = "Automatic items load interval", alias = "repo-autoreload-interval")
	protected long autoReloadInterval = 0;
	protected Map<String, Item> items = new ConcurrentSkipListMap<String, Item>(String.CASE_INSENSITIVE_ORDER);
	protected int itemsHash = 0;

	private Timer autoLoadTimer = null;
	private boolean initialized = false;
	private RepositoryChangeListenerIfc<Item> repoChangeList = null;

	public ConfigRepository() {
		String propKey = getPropertyKey();
		if (propKey != null) {
			if (propKey.startsWith("--")) {
				propKey = propKey.substring(2);
			}

			String value = System.getProperty(propKey);
			String[] items = null;
			if (value != null) {
				items = value.split(",");
				for (int i = 0; i < items.length; i++) {
					items[i] = items[i].trim();
				}
			}
			this.setItemsOld(items);
		}
	}

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
					try {
						reload();
					} catch (Exception ex) {
						log.log(Level.SEVERE, "exception during reload of config repository items", ex);
					}
				}
			}, 5 * 1000, interval);
		}
	}

	public void setAutoReloadInterval(long autoLoadInterval) {
		this.autoReloadInterval = autoLoadInterval;
		setAutoloadTimer(autoLoadInterval);
	}

	@Override
	public void addRepoChangeListener(RepositoryChangeListenerIfc<Item> repoChangeListener) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Adding new repository listener: {0}", repoChangeListener);
		}
		this.repoChangeList = repoChangeListener;
	}

	@Override
	public void removeRepoChangeListener(RepositoryChangeListenerIfc<Item> repoChangeListener) {
		this.repoChangeList = null;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("RepoItems");
		sb.append(", size=").append(items.size());
		sb.append(", items=").append(items.keySet().toString().substring(0, 1024));
		if (items.keySet().toString().length() > 1024) {
			sb.append("...");
		}
		return sb.toString();
	}

	public abstract String getConfigKey();

	public Item[] getDefaultItems() {
		return null;
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public String[] getDefaultPropetyItems() {
		return null;
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public abstract String getPropertyKey();

	@Override
	public void addItem(Item item) {
		addItemNoStore(item);

		store();
	}

	@Override
	public void addItemNoStore(Item item) {
		Item old = items.put(item.getKey(), item);

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
						log.log(Level.FINEST, "Not calling itemUpadted for: {0}, item unchanged.", item);
					}
				}
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No repoChangeListener for: {0}", item);
			}
		}
	}

	public boolean itemChanged(Item oldItem, Item newItem) {
		return true;
	}

	@Override
	public Collection<Item> allItems() {
		return items.values();
	}

	@Override
	public boolean contains(String key) {
		return items.keySet().contains(key);
	}

	@Deprecated
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		initItemsMap();
		String[] items_arr = getDefaultPropetyItems();

		if (params.get(getPropertyKey()) != null) {
			items_arr = ((String) params.get(getPropertyKey())).split(",");
			for (int i = 0; i < items_arr.length; i++) {
				items_arr[i] = items_arr[i].trim();
			}
		}
		defs.put(getConfigKey(), items_arr);
	}

	@Override
	public Item getItem(String key) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Getting item: {0} of items: {1}", new Object[]{key, items.keySet()});
		}

		return items.get(key);
	}
	
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public String[] getItemsOld() {
		List<String> itemsStrs = new ArrayList<>();
		for (Item item : items.values()) {
			itemsStrs.add(item.toPropertyString());
		}
		return itemsStrs.toArray(new String[itemsStrs.size()]);
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public void setItemsOld(String[] items_arr) {
		if (items_arr != null) {
			for (String it : items_arr) {
				log.log(Level.CONFIG, "Loading config item: {0}", it);

				Item item = getItemInstance();

				item.initFromPropertyString(it);
				if (!items.containsKey(item.getKey())) {
					addItem(item);
					log.log(Level.CONFIG, "Loaded config item: {0}", item);
				} else {
					log.log(Level.CONFIG, "Config item already loaded, skipping: {0}", item);
				}
			}
		}
	}

	@Override
	public Iterator<Item> iterator() {
		return items.values().iterator();
	}

	@Override
	public void reload() {
	}

	@Override
	public void removeItem(String key) {
		Item item = items.remove(key);

		if (item != null) {
			store();
			if (repoChangeList != null) {
				repoChangeList.itemRemoved(item);
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Removing item: {0}", item);
			}
		}
	}

	@Override
	public void removeItemNoStore(String key) {
		Item item = items.remove(key);

		if (item != null) {
			if (repoChangeList != null) {
				repoChangeList.itemRemoved(item);
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Removing item: {0}", item);
			}
		}
	}

	@Deprecated
	@Override
	public void setProperties(Map<String, Object> properties) {
		initItemsMap();
		String[] items_arr = (String[]) properties.get(getConfigKey());

		if ((items_arr != null) && (items_arr.length > 0)) {
			setItemsOld(items_arr);
		} else {
			log.warning("Items list is not set in the configuration file!!");
		}
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public void store() {
	}

	@Override
	public String validateItem(Item item) {
		return null;
	}

	@Override
	public void beforeUnregister() {
		setAutoloadTimer(0);
	}

	@Override
	public void initialize() {
		this.initialized = true;
		if (items.isEmpty()) {
			String[] itemsStr = getDefaultPropetyItems();
			if (itemsStr != null) {
				setItemsOld(itemsStr);
			} else {
				Optional.ofNullable(getDefaultItems()).ifPresent(items -> Arrays.stream(items).forEach(this::addItem));
			}
		} else {
			store();
		}
		setAutoReloadInterval(autoReloadInterval);
	}

	/**
	 * Method create instance of items Map. By overriding it it's possible to change implementation and it's settings.
	 */
	protected void initItemsMap() {
		if (null == items) {
			items = new ConcurrentSkipListMap<String, Item>(String.CASE_INSENSITIVE_ORDER);
		}
	}

	protected boolean isInitialized() {
		return initialized;
	}
}
