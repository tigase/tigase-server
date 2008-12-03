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

package tigase.vhosts;

import java.util.Map;

/**
 * Implementation of this interface is responsible for loading and storing
 * virtual hosts for the server installation. Configuration based storage
 * is normally read-only. Database storage is read-write.
 *
 * Created: Nov 27, 2008 1:52:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface VHostRepository {

	/**
	 * The method is called to remove given VHost from the memory cache and
	 * permanent storage. After this method is completed the vhosts should no
	 * longer be availble as a local domain.
	 * @param vh a <code>String</code> with domain name to remove.
	 */
	void removeVHost(String vh);

	/**
	 * The method adds a new or updates existing virtual hosts in the repository.
	 * It needs to have all fields set correctly. By the default
	 * <code>VHostItem</code> has all fields set to "enabled". After this method
	 * call is finished a new added virtual domain must be available as a local
	 * domain for the server installation.
	 * The method adds the vhosts to memory cache and permamnent storage.
	 * @param vhost a <code>VHostItem</code> with virtual domain and all it's
	 * configuration parameters.
	 */
	void addVHost(VHostItem vhost);

	/**
	 * The method returns all domain configuration parameters for a given domain
	 * or <code>null</code> if the domain does not exist in the repository. In
	 * other words it returns <code>null</code> if given domain is not local.
	 * @param domain a <code>String</code> with domain name to search for.
	 * @return a <code>VHostItem</code> for a given domain or <code>null</code>
	 * if the domain is not in the repository.
	 */
	VHostItem getVHost(String domain);

	/**
	 * The method checks whether the given domain is stored in the repository. In
	 * other words the method checks whether this is a local domain.
	 * @param domain a <code>String</code> with domain name to search for.
	 * @return a <code>boolean</code> value <code>true</code> if domain exist in
	 * the repository or <code>false</code> of domain does not exist.
	 */
	boolean contains(String domain);

	/**
	 * The method is called to set configuration for this repository
	 * implementation. The configuration is repository implementation dependent.
	 * There are no default settings for the repository.
	 * @param properties a <code>Map</code> with configuration settings. Content
	 * of this <code>Map</code> must not be modified. This read-only collection.
	 */
	void setProperties(Map<String, Object> properties);

	/**
	 * The method is called to obtain defualt configuration settings if there are
	 * any for this repository implementation The configuration settings are
	 * implementation dependent and there are no defaults set by the server.
	 * Default settings returned by this method are then saved in the configuration
	 * file and presented to the admin for further adjustements.
	 * @param defs is a <code>Map</code> collection where all repository
	 * configuration defaults must be put.
	 * @param params is a <code>Map</code> collection with some preset properties
	 * for the server. These settings can be used to adjust repository defaults,
	 * for example they can contain database connection URL or initial list of
	 * virtual domains.
	 */
	void getDefaults(Map<String, Object> defs, Map<String, Object> params);

	/**
	 * This method is called to reload virtual hosts from the database or other
	 * permanent storage. It is possible that virtual domains list is modified
	 * externally by third-party system. When all modifications are done this
	 * method is called to refresh the class cache. Whether the implementation
	 * load whole list or just last modifications is implementation dependent.
	 */
	void reload();

	/**
	 * The method returns number of virtual domains in the repository.
	 * @return an <code>int</code> value with number of virtual hosts in the
	 * repository.
	 */
	int size();

}
