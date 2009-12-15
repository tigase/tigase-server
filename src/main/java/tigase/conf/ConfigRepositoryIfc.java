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

package tigase.conf;

import java.util.Map;
import tigase.db.ComponentRepository;

/**
 * Created: Dec 10, 2009 2:04:20 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ConfigRepositoryIfc extends ComponentRepository<ConfigItem> {

	public static final String RESOURCE_URI = "--resource-uri";
	public static final String RELOAD_DELAY = "--reload-delay";

	/**
	 * Initializes the configuration repository.
	 * @param params
	 * @throws ConfigurationException
	 */
	void init(Map<String, Object> params) throws ConfigurationException;

	/**
	 * Returns all known settings for the given component name.
	 * @param compName
	 * @return
	 * @throws ConfigurationException
	 */
	Map<String, Object> getProperties(String compName) throws ConfigurationException;

	/**
	 * Sets/adds properties for the given component name.
	 * @param compName
	 * @param props
	 * @throws ConfigurationException
	 */
	void putProperties(String compName, Map<String, Object> props)
			throws ConfigurationException;

	/**
	 * Returns a configuration setting for a given component, node and key. If the
	 * configuration parameters is not found, returns given default value.
	 * @param compName
	 * @param node
	 * @param key
	 * @param def
	 * @return
	 */
	Object get(String compName, String node, String key, Object def);

	/**
	 * Puts/sets/adds/updates a configuration setting to the configuration repository.
	 * @param compName
	 * @param node
	 * @param key
	 * @param value
	 */
	void set(String compName, String node, String key, Object value);

	/**
	 * Returns all component names for which there are some configuration settings
	 * available.
	 * @return
	 */
	String[] getCompNames();

	/**
	 * Returns an array of all configuration keys for a given component and configuration
	 * node.
	 * @param compName
	 * @param node
	 * @return
	 */
	String[] getKeys(String compName, String node);

	/**
	 * Removes a configuration setting from the configuration repository.
	 * @param compName
	 * @param node
	 * @param key
	 */
	void remove(String compName, String node, String key);

	/**
	 * Method adds an Item to the configuration repository where the key is
	 * the item key constructed of component name, node name and property key name.
	 * @param key
	 * @param value
	 * @throws ConfigurationException
	 */
	void addItem(String key, Object value) throws ConfigurationException;

}
