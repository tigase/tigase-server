/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.db;

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

	public static final String PUBLIC_DATA_NODE = "public";
	public static final String OFFLINE_DATA_NODE = "offline";

	/**
   * <code>getPublicData</code> method returns a value associated with given key for
   * user repository in given subnode.
   * If key is not found in repository given default value is returned.
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
   * @return a <code>String</code> value
   * @exception UserNotFoundException if user id hasn't been found in reository.
   */
  String getPublicData(String user, String subnode, String key, String def)
    throws UserNotFoundException;

  /**
   * <code>getPublicDataList</code> method returns array of values associated with
   * given key or <code>null</code> if given key does not exist for given user
   * ID in given node path.
   *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
   * @param subnode a <code>String</code> value is a node path where data is
   * stored. Node path has the same form as directory path on file system:
   * <pre>/root/subnode1/subnode2</pre>.
   * @param key a <code>String</code> with which the needed values list is
   * associated.
   * @return a <code>String[]</code> value
   * @exception UserNotFoundException if user id hasn't been found in reository.
   */
  String[] getPublicDataList(String user, String subnode, String key)
    throws UserNotFoundException;

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
	void addOfflineDataList(String user, String subnode, String key, String[] list)
    throws UserNotFoundException;

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
	 */
	void addOfflineData(String user, String subnode, String key, String value)
    throws UserNotFoundException, DataOverwriteException;

} // WriteOnlyUserRepository
