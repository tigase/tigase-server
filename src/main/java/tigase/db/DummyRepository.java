/*
 * DummyRepository.java
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



package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

/**
 * DummyRepository is a class with all methods empty. They don't return
 * anything and they don't throw exception. SessionManager requires a
 * user repository to work properly but in some installations there is
 * no need for user repository as authentication is done through external
 * data source and user roster is pulled dynamically.
 *
 * Created: Sat Nov  3 16:17:03 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DummyRepository
				implements UserRepository, AuthRepository {
	/**
	 * Describe <code>addDataList</code> method here.
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
	 */
	@Override
	public void addDataList(BareJID user, String subnode, String key, String[] list) {}

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>String</code> value of user id consisting of user name
	 * and domain address.
	 */
	@Override
	public void addUser(BareJID user) {}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 * @throws TigaseDBException
	 * @throws UserExistsException
	 */
	@Override
	public void addUser(BareJID user, String password)
					throws UserExistsException, TigaseDBException {}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param digest
	 * @param id
	 * @param alg
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, String digest, String id, String alg)
					throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return false;
	}

	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param string a <code>String</code> value
	 * @param params
	 */
	@Override
	public void initRepository(String string, Map<String, String> params) {}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {}

	/**
	 * Method description
	 *
	 *
	 * @param authProps
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public boolean otherAuth(Map<String, Object> authProps)
					throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, String password)
					throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param authProps
	 */
	@Override
	public void queryAuth(Map<String, Object> authProps) {}

	/**
	 * Describe <code>removeData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> for which the value is to be removed.
	 */
	@Override
	public void removeData(BareJID user, String subnode, String key) {}

	/**
	 * Describe <code>removeData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> for which the value is to be removed.
	 */
	@Override
	public void removeData(BareJID user, String key) {}

	/**
	 * Describe <code>removeSubnode</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path to subnode which
	 * has to be removed. Node path has the same form as directory path on file
	 * system: <pre>/root/subnode1/subnode2</pre>.
	 */
	@Override
	public void removeSubnode(BareJID user, String subnode) {}

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 */
	@Override
	public void removeUser(BareJID user) {}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void updatePassword(BareJID user, String password)
					throws UserNotFoundException, TigaseDBException {}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean userExists(BareJID user) {
		return false;
	}

	//~--- get methods ----------------------------------------------------------

	// Implementation of tigase.db.UserRepository

	/**
	 * Describe <code>getData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 * @param def a <code>String</code> value which is returned in case if data
	 * for specified key does not exixist in repository.
	 *  a <code>String</code> value of null always.
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getData(BareJID user, String subnode, String key, String def) {
		return null;
	}

	/**
	 * Describe <code>getData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 *  a <code>String</code> value of null always.
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getData(BareJID user, String subnode, String key) {
		return null;
	}

	/**
	 * Describe <code>getData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> with which the needed value is
	 * associated.
	 *  a <code>String</code> value of null always.
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getData(BareJID user, String key) {
		return null;
	}

	/**
	 * Describe <code>getDataList</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the needed values list is
	 * associated.
	 *  a <code>String[]</code> value
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getDataList(BareJID user, String subnode, String key) {
		return null;
	}

	/**
	 * Describe <code>getKeys</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 *  a <code>String[]</code> value
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getKeys(BareJID user, String subnode) {
		return null;
	}

	/**
	 * Describe <code>getKeys</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 *  a <code>String[]</code> value
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getKeys(BareJID user) {
		return null;
	}

	/**
	 * Describe <code>getResourceUri</code> method here.
	 *
	 *  a <code>String</code> value
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getResourceUri() {
		return null;
	}

	/**
	 * Describe <code>getSubnodes</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 *  a <code>String[]</code> value
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getSubnodes(BareJID user, String subnode) {
		return null;
	}

	/**
	 * Describe <code>getSubnodes</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 *  a <code>String[]</code> value
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getSubnodes(BareJID user) {
		return null;
	}

	/**
	 * Describe <code>getUsers</code> method here.
	 *
	 *  a <code>List</code> value
	 *
	 * @return a value of <code>List<BareJID></code>
	 */
	@Override
	public List<BareJID> getUsers() {
		return null;
	}

	/**
	 * Describe <code>getUsersCount</code> method here.
	 *
	 *  a <code>long</code> value
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getUsersCount() {
		return 0;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getUsersCount(String domain) {
		return 0;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 * @throws TigaseDBException
	 */
	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		return -1;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Describe <code>setData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param subnode a <code>String</code> value is a node path where data is
	 * stored. Node path has the same form as directory path on file system:
	 * <pre>/root/subnode1/subnode2</pre>.
	 * @param key a <code>String</code> with which the specified value is to be
	 * associated.
	 * @param value a <code>String</code> value to be associated with the
	 * specified key.
	 */
	@Override
	public void setData(BareJID user, String subnode, String key, String value) {}

	/**
	 * Describe <code>setData</code> method here.
	 *
	 * @param user a <code>String</code> value of user ID for which data must be
	 * stored. User ID consists of user name and domain name.
	 * @param key a <code>String</code> with which the specified value is to be
	 * associated.
	 * @param value a <code>String</code> value to be associated with the
	 * specified key.
	 */
	@Override
	public void setData(BareJID user, String key, String value) {}

	/**
	 * Describe <code>setDataList</code> method here.
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
	 */
	@Override
	public void setDataList(BareJID user, String subnode, String key, String[] list) {}
}


//~ Formatted in Tigase Code Convention on 13/08/29
