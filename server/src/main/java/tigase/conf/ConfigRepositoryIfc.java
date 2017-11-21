/*
 * ConfigRepositoryIfc.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.conf;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.ComponentRepository;

import java.util.Map;
import java.util.Set;

//~--- JDK imports ------------------------------------------------------------

/**
 * Created: Dec 10, 2009 2:04:20 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ConfigRepositoryIfc
		extends ComponentRepository<ConfigItem> {

	public static final String RELOAD_DELAY = "--reload-delay";

	public static final String RESOURCE_URI = "--resource-uri";

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns all known settings for the given component name.
	 */
	Map<String, Object> getProperties(String compName) throws ConfigurationException;

	Set<ConfigItem> getItemsForComponent(String compName);

	//~--- methods --------------------------------------------------------------

	/**
	 * Sets/adds properties for the given component name.
	 */
	void putProperties(String compName, Map<String, Object> props) throws ConfigurationException;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns a configuration setting for a given component, node and key. If the configuration parameters is not
	 * found, returns given default value.
	 */
	Object get(String compName, String node, String key, Object def);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Puts/sets/adds/updates a configuration setting to the configuration repository.
	 */
	void set(String compName, String node, String key, Object value);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns all component names for which there are some configuration settings available.
	 */
	String[] getCompNames();

	/**
	 * Returns an array of all configuration keys for a given component and configuration node.
	 */
	String[] getKeys(String compName, String node);

	//~--- methods --------------------------------------------------------------

	/**
	 * Removes a configuration setting from the configuration repository.
	 */
	void remove(String compName, String node, String key);

	/**
	 * Method adds an Item to the configuration repository where the key is the item key constructed of component name,
	 * node name and property key name.
	 */
	void addItem(String key, Object value) throws ConfigurationException;

	//~--- set methods ----------------------------------------------------------

	/**
	 * This is used to load a configuration for a selected cluster node. The configuration repository (file or database)
	 * may contain settings for all cluster nodes, some of the settings may be exclusive to one or another cluster node.
	 * This method informs the repository what node name (hostname) it is running on.
	 */
	void setDefHostname(String hostname);

	//~--- get methods ----------------------------------------------------------

	Map<String, Object> getInitProperties();
}
