/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- interfaces -------------------------------------------------------------

/**
 * Describe interface WriteOnlyUserRepository here.
 *
 *
 * Created: Sat Oct 14 20:42:30 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface NonAuthUserRepository {

	/** Field description */
	public static final String PUBLIC_DATA_NODE = "public";

	/** Field description */
	public static final String OFFLINE_DATA_NODE = "offline";

	//~--- methods --------------------------------------------------------------

	/**
	 * <code>addDataList</code> method adds mode entries to existing data list
	 * associated with given key in repository under given node path.
	 * This method is very similar to <code>setDataList(...)</code> except it
	 * doesn't remove existing data.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified values list is to
	 * be associated.
	 * @param value a <code>String</code> is an array of values to be assosiated
	 * with the specified key.
	 * @exception UserNotFoundException if user id hasn't been found in reository.
	 * @throws DataOverwriteException
	 */
	void addOfflineData(BareJID user, String subnode, String key, String value)
			throws UserNotFoundException, DataOverwriteException;

	/**
	 * <code>addDataList</code> method adds mode entries to existing data list
	 * associated with given key in repository under given node path.
	 * This method is very similar to <code>setDataList(...)</code> except it
	 * doesn't remove existing data.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified values list is to
	 * be associated.
	 * @param list a <code>String[]</code> is an array of values to be assosiated
	 * with the specified key.
	 * @exception UserNotFoundException if user id hasn't been found in reository.
	 */
	void addOfflineDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Retrieves and returns a value associated with given subnode and key from a publicly
	 * available space. The space is specific to given virtual domain and is shared among all
	 * running cluster nodes. The data are stored in some temporary space outside of the
	 * registered user data so no information for registered users can be retrieved.<br>
	 *
	 * @param domain is a DNS domain name with which the data is associated.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the needed value is
	 * associated.
	 * @param def a {@link String} value which is returned in case if data
	 * for specified key does not exixist in repository.
	 * @return a {@link String} value for a given subnode and key or {@code def]}
	 * if no entry has been found.
	 * @throws TigaseDBException if there was an error during reading data from the repository.
	 */
	String getDomainTempData(BareJID domain, String subnode, String key, String def)
			throws TigaseDBException;

	/**
	 * <code>getPublicData</code> method returns a value associated with given key for
	 * user repository in given subnode.
	 * If key is not found in repository given default value is returned.
	 *
	 * @param user a {@link String} value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the needed value is
	 * associated.
	 * @param def a {@link String} value which is returned in case if data
	 * for specified key does not exixist in repository.
	 * @return a {@link String} value for a given subnode and key or {@code def}
	 * if no entry has been found.
	 * @exception UserNotFoundException if user id hasn't been found in reository.
	 */
	String getPublicData(BareJID user, String subnode, String key, String def)
			throws UserNotFoundException;

	/**
	 * <code>getPublicDataList</code> method returns array of values associated with
	 * given key or <code>null</code> if given key does not exist for given user
	 * ID in given node path.
	 *
	 * @param user a {@link String} value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the needed values list is
	 * associated.
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if user id hasn't been found in reository.
	 */
	String[] getPublicDataList(BareJID user, String subnode, String key)
			throws UserNotFoundException;

	/**
	 * Retrieves and returns a value associated with given subnode and key from a publicly
	 * available space. The space is specific for the Tigase instance and is not shared among
	 * different cluster nodes. The data is stored in some temporary space outside of the
	 * registered user data. So no information for registered users can be retrieved.<br>
	 *
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the needed value is
	 * associated.
	 * @param def a {@link String} value which is returned in case if data
	 * for specified key does not exixist in repository.
	 * @return a {@link String} value for a given subnode and key or <code>def</code>
	 * if no entry has been found.
	 * @throws TigaseDBException if there was an error during reading data from the repository.
	 */
	String getTempData(String subnode, String key, String def) throws TigaseDBException;

	//~--- methods --------------------------------------------------------------

	/**
	 * The method allows to store some temporary data by the plugin in publicly available
	 * space. The space is specific to given virtual domain and is shared among all
	 * running cluster nodes. The data is stored in some place outside of the normal user space
	 * so no information for registered user can be overwriten.<br>
	 * If there is already a value for a given subnode and key it will be overwritten otherwise
	 * a new entry will be created.
	 * @param domain is a DNS domain name with which the data is associated.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the specified values list is to
	 * be associated.
	 * @param value a {@link String} is an array of values to be assosiated
	 * with the specified key.
	 * @throws TigaseDBException if there was an error during writing data to the repository.
	 */
	void putDomainTempData(BareJID domain, String subnode, String key, String value)
			throws TigaseDBException;

	/**
	 * The method allows to store some temporary data by the plugin in publicly available
	 * space. The space is specific for the Tigase instance and is not shared among different
	 * cluster nodes. The data is stored in some place outside of the normal user space so
	 * no information for registered user can be overwriten.<br>
	 * If there is already a value for a given subnode and key it will be overwritten otherwise
	 * a new entry will be created.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the specified values list is to
	 * be associated.
	 * @param value a {@link String} is an array of values to be assosiated
	 * with the specified key.
	 * @throws TigaseDBException if there was an error during writing data to the repository.
	 */
	void putTempData(String subnode, String key, String value) throws TigaseDBException;

	/**
	 * The method allows to remove existing data stored in a temporary storage space associated
	 * with a given DNS domain.
	 * @param domain is a DNS domain name with which the data is associated.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the specified values list is to
	 * be associated.
	 * @throws TigaseDBException if there was an error during writing data to the repository.
	 */
	void removeDomainTempData(BareJID domain, String subnode, String key)
			throws TigaseDBException;

	/**
	 * The method allows to remove existing data stored in the Tigase instance specific
	 * temporary storage.
	 * @param subnode a {@link String} value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a {@link String} with which the specified values list is to
	 * be associated.
	 * @throws TigaseDBException if there was an error during writing data to the repository.
	 */
	void removeTempData(String subnode, String key) throws TigaseDBException;
}    // WriteOnlyUserRepository


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
