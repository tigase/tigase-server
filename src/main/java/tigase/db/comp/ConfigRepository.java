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

package tigase.db.comp;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.conf.ConfigItem;

//~--- classes ----------------------------------------------------------------

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

	protected LinkedHashMap<String, Item> items = new LinkedHashMap<String, Item>(100);
	private RepositoryChangeListenerIfc<Item> repoChangeList = null;

	@Override
	public void addRepoChangeListener(
			RepositoryChangeListenerIfc<Item> repoChangeListener) {
		this.repoChangeList = repoChangeListener;
	}

	@Override
	public void removeRepoChangeListener(
			RepositoryChangeListenerIfc<Item> repoChangeListener) {
		this.repoChangeList = null;
	}
	
	@Override
	public String toString() {
		return items.toString();
	}

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
		items.put(item.getKey(), item);
		store();
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
		items.remove(key);
		store();
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
				items.put(item.getKey(), item);
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


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
