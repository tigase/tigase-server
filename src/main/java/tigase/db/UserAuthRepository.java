/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.util.Map;
import java.security.NoSuchAlgorithmException;

/**
 * Describe interface UserAuthRepository here.
 *
 *
 * Created: Sun Nov  5 21:15:46 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface UserAuthRepository {

	// Query params (and otherAuth)
	public static final String PROTOCOL_KEY = "protocol";
	public static final String PROTOCOL_VAL_SASL = "sasl";
	public static final String PROTOCOL_VAL_NONSASL = "nonsasl";
	public static final String RESULT_KEY = "result";

	// otherAuth params
	public static final String MACHANISM_KEY = "mechanism";
	public static final String REALM_KEY = "realm";
	public static final String SERVER_NAME_KEY = "server-name";
	public static final String DATA_KEY = "data";
	public static final String USER_ID_KEY = "user-id";

	void queryAuth(Map<String, Object> authProps);

	void initRepository(String resource_uri) throws DBInitException;

	String getResourceUri();

	boolean plainAuth(String user, String password)
		throws UserNotFoundException, TigaseDBException, AuthorizationException;

	boolean digestAuth(String user, String digest, String id, String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException;

	boolean otherAuth(Map<String, Object> authProps)
		throws UserNotFoundException, TigaseDBException, AuthorizationException;

	void addUser(String user, String password)
		throws UserExistsException, TigaseDBException;

  void updatePassword(String user, String password)
		throws TigaseDBException;

  void removeUser(String user) throws UserNotFoundException, TigaseDBException;

	void logout(String user) throws UserNotFoundException, TigaseDBException;

} // UserAuthRepository