/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
public interface WriteOnlyUserRepository {

  /**
   * <code>setData</code> method <!-- beauty loves beast --> sets data value for
   * given user ID in repository under given node path and associates it with
   * given key.
   * If there already exists value for given key in given node, old value is
   * replaced with new value. No warning or exception is thrown in case if
   * methods overwrites old value.
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
   * @exception UserNotFoundException if user id hasn't been found in reository.
   */
  void setData(String user, String subnode, String key, String value)
    throws UserNotFoundException;

  /**
   * This <code>setData</code> method sets data value for given user ID
   * associated with given key in default repository node.
   * Default node is dependent on implementation and usually it is root user
   * node. If there already exists value for given key in given node, old value
   * is replaced with new value. No warning or exception is thrown in case if
   * methods overwrites old value.
   *
   * @param user a <code>String</code> value of user ID for which data must be
   * stored. User ID consists of user name and domain name.
   * @param key a <code>String</code> with which the specified value is to be
   * associated.
   * @param value a <code>String</code> value to be associated with the
   * specified key.
   * @exception UserNotFoundException if user id hasn't been found in reository.
   */
  void setData(String user, String key, String value)
    throws UserNotFoundException;

  /**
   * <code>setDataList</code> method sets list of values for given user
   * associated given key in repository under given node path.
   * If there already exist values for given key in given node, all old values are
   * replaced with new values. No warning or exception is thrown in case if
   * methods overwrites old value.
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
  void setDataList(String user, String subnode, String key, String[] list)
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
	void addDataList(String user, String subnode, String key, String[] list)
    throws UserNotFoundException;

} // WriteOnlyUserRepository
