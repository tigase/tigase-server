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

import tigase.xmpp.BareJID;
import java.util.List;
import java.util.Map;

/**
 * <code>UserRepository</code> interface defines all functionalities required
 * to store user data.
 * It contains adding, removing and searching methods. User repository is
 * organized as hierarchical data base. It means you can add items to repository
 * on different levels like files in file systems. Instead, however of working
 * with directories you work with nodes. You can create many levels of nodes and
 * store data on any level. It helps to organize data in more logical order.
 *
 * <p>
 * Created: Tue Oct 26 15:09:28 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface UserRepository extends Repository {

	/**
	 * <code>addDataList</code> method adds mode entries to existing data list
	 * associated with given key in repository under given node path.
	 * This method is very similar to <code>setDataList(...)</code> except it
	 * doesn't remove existing data.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified values list is to
	 * be associated.
	 * @param list a <code>String[]</code> is an array of values to be associated
	 * with the specified key.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void addDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * This <code>addUser</code> method allows to add new user to repository.
	 * It <b>must</b> throw en exception <code>UserExistsException</code> if such
	 * user already exists because user <b>must</b> be unique within user
	 * repository data base.<br>
	 * As one <em>XMPP</em> server can support many virtual internet domains it
	 * is required that <code>user</code> id consists of user name and domain
	 * address: <em>username@domain.address.net</em> for example.
	 *
	 * @param user a <code>BareJID</code> value of user id consisting of user name
	 * and domain address.
	 * @exception UserExistsException if user with the same id already exists.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void addUser(BareJID user) throws UserExistsException, TigaseDBException;

	/**
	 * <code>getData</code> method returns a value associated with given key for
	 * user repository in given subnode.
	 * If key is not found in repository given default value is returned.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @param def a <code>String</code> value which is returned in case if data
	 * for specified key does not exixist in repository.
	 * @return a <code>String</code> value
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String getData(BareJID user, String subnode, String key, String def)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>getData</code> method returns a value associated with given key for
	 * user repository in given subnode.
	 * If key is not found in repository <code>null</code> value is returned.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @return a <code>String</code> value
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String getData(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>getData</code> method returns a value associated with given key for
	 * user repository in default subnode.
	 * If key is not found in repository <code>null</code> value is returned.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @return a <code>String</code> value
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String getData(BareJID user, String key) throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>getDataList</code> method returns array of values associated with
	 * given key or <code>null</code> if given key does not exist for given user
	 * ID in given node path.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed values list is
	 * associated.
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String[] getDataList(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>getKeys</code> method returns list of all keys stored in given
	 * subnode in user repository.
	 * There is a value (or list of values) associated with each key. It is up to
	 * user (developer) to know what key keeps one value and what key keeps list
	 * of values.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String[] getKeys(BareJID user, String subnode) throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>getKeys</code> method returns list of all keys stored in default user
	 * repository node.
	 * There is some a value (or list of values) associated with each key. It is
	 * up to user (developer) to know what key keeps one value and what key keeps
	 * list of values.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored or retrieved. User ID consists of user name and domain name.
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException;

	/**
	 * Returns a DB connection string or DB connection URI.
	 * @return a <code>String</code> value representing database connection string.
	 */
	String getResourceUri();

	/**
	 * <code>getSubnodes</code> method returns list of all direct subnodes from
	 * given node.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @return a <code>String[]</code> value is an array of all direct subnodes.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String[] getSubnodes(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>getSubnodes</code> method returns list of all <em>root</em> nodes for
	 * given user.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @return a <code>String[]</code> value is an array of all <em>root</em>
	 * nodes for given user.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	String[] getSubnodes(BareJID user) throws UserNotFoundException, TigaseDBException;

	/**
	 * Returns a user unique ID number within the given repository. Please note it is also
	 * possible that the ID number is unique only for the user domain. The ID is a positive
	 * number if the user exists and negative if the user was not found in the repository.
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored or retrieved. User ID consists of user name and domain name.
	 * @return a user inique ID number within the repository or domain. The ID is a positive
	 * number if the user exists and negative if the user was not found in the repository.
	 * @throws TigaseDBException if there is a problem with accessing user repository.
	 */
	long getUserUID(BareJID user) throws TigaseDBException;

	/**
	 * This method is only used by the data conversion tools. They attempt
	 * to copy whole user repositories from one to another database. Databases
	 * might not be compatible but as long as the API is implemented and they
	 * support adding user the user database can be copied to a different data
	 * source.
	 * @return returns a collection of all user IDs (Jabber IDs) stored in
	 * the user repository.
	 * @throws tigase.db.TigaseDBException
	 */
	List<BareJID> getUsers() throws TigaseDBException;

	/**
	 * This method is only used by the server statistics component to report
	 * number of registered users.
	 * @return a <code>long</code> number of registered users in the repository.
	 */
	long getUsersCount();

	/**
	 * This method is only used by the server statistics component to report
	 * number of registered users for given domain.
	 * @param domain
	 * @return a <code>long</code> number of registered users in the repository.
	 */
	long getUsersCount(String domain);

	//~--- methods --------------------------------------------------------------

	/**
	 * <code>removeData</code> method removes pair (key, value) from user
	 * repository in given subnode.
	 * If the key exists in user repository there is always a value
	 * associated with this key - even empty <code>String</code>. If key does not
	 * exist the <code>null</code> value is returned from repository backend or
	 * given default value.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> for which the value is to be removed.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void removeData(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>removeData</code> method removes pair (key, value) from user
	 * repository in default repository node.
	 * If the key exists in user repository there is always a value
	 * associated with this key - even empty <code>String</code>. If key does not
	 * exist the <code>null</code> value is returned from repository backend or
	 * given default value.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> for which the value is to be removed.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void removeData(BareJID user, String key) throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>removeSubnode</code> method removes given subnode with all subnodes
	 * in this node and all data stored in this node and in all subnodes.
	 * Effectively it removes entire repository tree starting from given node.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path to subnode which
	 * has to be removed. Node path has the same form as directory path on file
	 * system: <pre>/root/subnode1/subnode2</pre>.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void removeSubnode(BareJID user, String subnode) throws UserNotFoundException, TigaseDBException;

	/**
	 * This <code>removeUser</code> method allows to remove user and all his data
	 * from user repository.
	 * If given user id does not exist <code>UserNotFoundException</code> must be
	 * thrown. As one <em>XMPP</em> server can support many virtual internet
	 * domains it is required that <code>user</code> id consists of user name and
	 * domain address: <em>username@domain.address.net</em> for example.
	 *
	 * @param user a <code>BareJID</code> value of user id consisting of user name
	 * and domain address.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>setData</code> method sets data value for given user ID in repository
	 * under given node path and associates it with given key.
	 * If there already exists value for given key in given node, old value is
	 * replaced with new value. No warning or exception is thrown in case if
	 * methods overwrites old value.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified value is to be
	 * associated.
	 * @param value a <code>String</code> value to be associated with the
	 * specified key.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void setData(BareJID user, String subnode, String key, String value)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * This <code>setData</code> method sets data value for given user ID
	 * associated with given key in default repository node.
	 * Default node is dependent on implementation and usually it is root user
	 * node. If there already exists value for given key in given node, old value
	 * is replaced with new value. No warning or exception is thrown in case if
	 * methods overwrites old value.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> with which the specified value is to be
	 * associated.
	 * @param value a <code>String</code> value to be associated with the
	 * specified key.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void setData(BareJID user, String key, String value)
			throws UserNotFoundException, TigaseDBException;

	/**
	 * <code>setDataList</code> method sets list of values for given user
	 * associated given key in repository under given node path.
	 * If there already exist values for given key in given node, all old values are
	 * replaced with new values. No warning or exception is thrown in case if
	 * methods overwrites old value.
	 *
	 * @param user a <code>BareJID</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified values list is to
	 * be associated.
	 * @param list a <code>String[]</code> is an array of values to be associated
	 * with the specified key.
	 * @exception UserNotFoundException if user id hasn't been found in repository.
	 * @throws TigaseDBException if database backend error occurs.
	 */
	void setDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method <code>userExists</code> checks whether the user (or repository top node)
	 * exists in the database. The method doesn't throw any exception nor it creates
	 * the user in case it is missing. It just checks whether the user is already
	 * in the database.
	 *
	 * Please don't overuse this method. All other methods
	 * throw <code>UserNotFoundException</code> exception in case the user is missing
	 * for which you executed the method. The exception is thrown unless
	 * <code>userAutoCreate</code> property is set to true. In such case the exception
	 * is never thrown and the methods are executed for given parameters prior to
	 * creating user entry if it is missing.
	 *
	 * Therefore this method should be used only to check whether the account exists
	 * without creating it.
	 *
	 * @param user a <code>BareJID</code> value
	 * @return a <code>boolean</code> value
	 */
	boolean userExists(BareJID user);
}    // UserRepository
