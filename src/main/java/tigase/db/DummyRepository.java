/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.db;

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
public class DummyRepository implements UserRepository {

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
   * @return a <code>String</code> value of null always.
	 */
	public String getData(String user, String subnode, String key, String def) {
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
   * @return a <code>String</code> value of null always.
	 */
	public String getData(String user, String subnode, String key) { return null; }

	/**
	 * Describe <code>getData</code> method here.
	 *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
   * @param key a <code>String</code> with which the needed value is
   * associated.
   * @return a <code>String</code> value of null always.
	 */
	public String getData(String user, String key) { return null;	}

	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param string a <code>String</code> value
	 * @exception DBInitException if an error occurs
	 */
	public void initRepository(final String string, Map<String, String> params) {	}

	/**
	 * Describe <code>getResourceUri</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getResourceUri() { return null;	}

	/**
	 * Describe <code>getUsers</code> method here.
	 *
	 * @return a <code>List</code> value
	 * @exception TigaseDBException if an error occurs
	 */
	public List<String> getUsers() { return null; }

	/**
	 * Describe <code>getUsersCount</code> method here.
	 *
	 * @return a <code>long</code> value
	 */
	public long getUsersCount() {	return 0; }

	/**
	 * Describe <code>addUser</code> method here.
	 *
   * @param user a <code>String</code> value of user id consisting of user name
   * and domain address.
	 */
	public void addUser(final String user) { }

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void removeUser(final String user) {	}

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
	 * @return a <code>String[]</code> value
	 */
	public String[] getDataList(String user, String subnode, String key) {
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
	 * @return a <code>String[]</code> value
	 */
	public String[] getSubnodes(String user, String subnode) {
		return null;
	}

	/**
	 * Describe <code>getSubnodes</code> method here.
	 *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
	 * @return a <code>String[]</code> value
	 */
	public String[] getSubnodes(final String user) {
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
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public String[] getKeys(String user, String subnode) {
		return null;
	}

	/**
	 * Describe <code>getKeys</code> method here.
	 *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
	 * @return a <code>String[]</code> value
	 */
	public String[] getKeys(final String user) {
		return null;
	}

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
	public void removeData(String user, String subnode, String key) {	}

	/**
	 * Describe <code>removeData</code> method here.
	 *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
   * @param key a <code>String</code> for which the value is to be removed.
	 */
	public void removeData(String user, String key) {	}

	/**
	 * Describe <code>removeSubnode</code> method here.
	 *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
   * @param subnode a <code>String</code> value is a node path to subnode which
   * has to be removed. Node path has the same form as directory path on file
   * system: <pre>/root/subnode1/subnode2</pre>.
	 */
	public void removeSubnode(String user, String subnode) {}

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
	public void setData(String user, String subnode, String key, String value) {}

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
	public void setData(String user, String key, String value) {}

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
	public void setDataList(String user, String subnode, String key, String[] list) {	}

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
	public void addDataList(String user, String subnode, String key, String[] list) {
	}

}
