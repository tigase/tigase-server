/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------


import java.util.List;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

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
@Repository.Meta( supportedUris = { "dummy" } )
public class DummyRepository implements UserRepository, AuthRepository {

	@Override
	public void addDataList(BareJID user, String subnode, String key, String[] list) {}

	@Override
	public void addUser(BareJID user) {}

	@Override
	public void addUser(BareJID user, String password)
			throws UserExistsException, TigaseDBException {}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, String digest, String id, String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return false;
	}

	//~--- get methods ----------------------------------------------------------

	// Implementation of tigase.db.UserRepository

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * @return a <code>String</code> value of null always.
	 */
	@Override
	public String getData(BareJID user, String subnode, String key, String def) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * @return a <code>String</code> value of null always.
	 */
	@Override
	public String getData(BareJID user, String subnode, String key) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * @return a <code>String</code> value of null always.
	 */
	@Override
	public String getData(BareJID user, String key) {
		return null;
	}

	@Override
	public String[] getDataList(BareJID user, String subnode, String key) {
		return null;
	}

	@Override
	public String[] getKeys(BareJID user, String subnode) {
		return null;
	}

	@Override
	public String[] getKeys(BareJID user) {
		return null;
	}

	@Override
	public String getResourceUri() {
		return null;
	}

	@Override
	public String[] getSubnodes(BareJID user, String subnode) {
		return null;
	}

	@Override
	public String[] getSubnodes(BareJID user) {
		return null;
	}

	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		return -1;
	}

	@Override
	public List<BareJID> getUsers() {
		return null;
	}

	@Override
	public long getUsersCount() {
		return 0;
	}

	@Override
	public long getUsersCount(String domain) {
		return 0;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String string, Map<String, String> params) {}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {}

	@Override
	public boolean otherAuth(Map<String, Object> authProps)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return false;
	}

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return false;
	}

	@Override
	public void queryAuth(Map<String, Object> authProps) {}

	@Override
	public void removeData(BareJID user, String subnode, String key) {}

	@Override
	public void removeData(BareJID user, String key) {}

	@Override
	public void removeSubnode(BareJID user, String subnode) {}

	@Override
	public void removeUser(BareJID user) {}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setData(BareJID user, String subnode, String key, String value) {}

	@Override
	public void setData(BareJID user, String key, String value) {}

	@Override
	public void setDataList(BareJID user, String subnode, String key, String[] list) {}

	//~--- methods --------------------------------------------------------------

	@Override
	public void updatePassword(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException {}

	@Override
	public boolean userExists(BareJID user) {
		return false;
	}

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isUserDisabled(BareJID user) throws UserNotFoundException, TigaseDBException {
		return false;
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Feature not supported");
	}		
}
