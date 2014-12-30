/*
 * ComponentRepository.java
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

//~--- non-JDK imports --------------------------------------------------------

import java.util.Collection;
import java.util.Map;
import tigase.db.Repository;
import tigase.db.TigaseDBException;

/**
 * A convenience interface for a unified access to component specific repository
 * data. This is not intended to keep huge number of elements. Rather then it is
 * more like for storing Component dynamic configuration data. In simple cases
 * this data can be stored in configuration file, in more complex cases it can
 * be a database represented by UserRepository or even something else.
 * <br>
 * The repository is intended to store elements of a single type only. Each
 * element is identified by a unique key. All elements are cached in memory for
 * a fast retrieval so this kind of repository is recommended for small data
 * only when you need very fast and efficient access to all the information.<br>
 * Some implementations however may behave differently and not cache all the
 * repository items in memory.
 * <br>
 * Created: Oct 3, 2009 1:46:25 PM
 *
 * @param <Item>
 *          An element stored in the component repository.
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ComponentRepository<Item extends RepositoryItem>
				extends Iterable<Item>, Repository {
	/** Field description */
	public static final String COMP_REPO_BIND = "comp_repo";

	//~--- methods --------------------------------------------------------------

	/**
	 * Adds a listener for repository Item change.
	 * @param repoChangeListener
	 */
	void addRepoChangeListener(RepositoryChangeListenerIfc<Item> repoChangeListener);

	/**
	 * Removes a listener for repository Item change.
	 * @param repoChangeListener
	 */
	void removeRepoChangeListener(RepositoryChangeListenerIfc<Item> repoChangeListener);

	/**
	 * The method adds a new or updates existing Item in the repository. It needs
	 * to have all fields set correctly. After this method call is finished a new
	 * added item must be available in the component repository. The method adds
	 * the item to memory cache and permanent storage.
	 *
	 * @param item
	 *          a <code>Item</code> with all it's configuration parameters.
	 * @throws TigaseDBException
	 */
	void addItem(Item item) throws TigaseDBException;

	/**
	 * The method adds a new or updates existing Item. It needs
	 * to have all fields set correctly. After this method call is finished a new
	 * added item must be available in the component repository. The method adds
	 * the item to memory cache but not to a permanent storage.
	 *
	 * @param item
	 *          a <code>Item</code> with all it's configuration parameters.
	 */
	void addItemNoStore(Item item);

	/**
	 * Returns a collection with all items stored in the repository.
	 *
	 *
	 * @throws TigaseDBException
	 */
	Collection<Item> allItems() throws TigaseDBException;

	/**
	 * The method checks whether the item is stored in the repository.
	 *
	 * @param key
	 *          a <code>String</code> with key to search for.
	 * @return a <code>boolean</code> value <code>true</code> if the item exists
	 *         in the repository or <code>false</code> of it does not.
	 */
	boolean contains(String key);
	
	/**
	 * Method destroys this instance of ComponentRepository releasing resources
	 * allocated for this instance of ComponentRepository if possible
	 */
	void destroy();

	//~--- get methods ----------------------------------------------------------

	/**
	 * The method is called to obtain default configuration settings if there are
	 * any for this repository implementation The configuration settings are
	 * implementation dependent and there are no defaults set by the server.
	 * Default settings returned by this method are then saved in the
	 * configuration file and presented to the admin for further adjustments.
	 *
	 * @param defs
	 *          is a <code>Map</code> collection where all repository
	 *          configuration defaults must be put.
	 * @param params
	 *          is a <code>Map</code> collection with some preset properties for
	 *          the server. These settings can be used to adjust repository
	 *          defaults, for example they can contain database connection URL or
	 *          initial list of virtual domains.
	 */
	void getDefaults(Map<String, Object> defs, Map<String, Object> params);

	/**
	 * The method returns all item configuration parameters for a key or
	 * <code>null</code> if the item does not exist in the repository.
	 *
	 * @param key
	 *          a <code>String</code> with item identifier to search for.
	 * @return a <code>Item</code> for a given key or <code>null</code> if the
	 *         item is not in the repository.
	 */
	Item getItem(String key);

	/**
	 * Creates a new, uninitialized instance of the repository Item.
	 *
	 * @return a new, uninitialized instance of the repository Item.
	 */
	Item getItemInstance();

	//~--- methods --------------------------------------------------------------

	/**
	 * This method is called to reload items from the database or other permanent
	 * storage. It is possible that items list is modified externally by
	 * third-party system. When all modifications are done this method is called
	 * to refresh the class cache. Whether the implementation load whole list or
	 * just last modifications is implementation dependent.
	 *
	 * @throws TigaseDBException
	 */
	void reload() throws TigaseDBException;

	/**
	 * The method is called to remove given Item from the memory cache and
	 * permanent storage. After this method is completed the item should no longer
	 * be available in the component repository.
	 *
	 * @param key
	 *          a <code>String</code> with domain name to remove.
	 * @throws TigaseDBException
	 */
	void removeItem(String key) throws TigaseDBException;

	//~--- set methods ----------------------------------------------------------

	/**
	 * The method is called to set configuration for this repository
	 * implementation. The configuration is repository implementation dependent.
	 * There are no default settings for the repository.
	 *
	 * @param properties
	 *          a <code>Map</code> with configuration settings. Content of this
	 *          <code>Map</code> must not be modified. This read-only collection.
	 */
	void setProperties(Map<String, Object> properties);

	//~--- methods --------------------------------------------------------------

	/**
	 * The method returns number of items in the repository.
	 *
	 * @return an <code>int</code> value with number of items in the repository.
	 */
	int size();

	/**
	 * The method is called to store all data in the database. It is used when the
	 * repository has been changed in some way and the changes have to be put to a
	 * permanent storage for later retrieval.
	 *
	 * @throws TigaseDBException
	 */
	void store() throws TigaseDBException;

	/**
	 * Performs Item validation to check whether it meets the repository policy.
	 * If validation is successful the method returns <code>null</code>, otherwise
	 * it returns an error description.
	 *
	 * @param item
	 *          is an <code>Item</code> object to perform validation checking
	 *          upon.
	 * @return <code>null</code> on success and an error message otherwise.
	 */
	String validateItem(Item item);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Sets autoload task to periodically reload data from database.
	 *
	 *
	 * @param delay in seconds between each database reload.
	 */
	void setAutoloadTimer(long delay);
}


//~ Formatted in Tigase Code Convention on 13/03/09
