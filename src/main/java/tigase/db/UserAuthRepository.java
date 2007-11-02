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

	/**
	 * <code>queryAuth</code> returns mechanisms available for authentication.
	 *
	 * @param authProps a <code>Map</code> value with parameters for authentication.
	 */
	void queryAuth(Map<String, Object> authProps);

	/**
	 * <code>initRepository</code> method is doing initialization for database
	 * connection. It may also do lazy initialization with database.
	 * Connection to database might be established during the first authentication
	 * request.
	 *
	 * @param conn_str a <code>String</code> value of database connection string.
	 * The string must also contain database user name and password if required
	 * for connection.
	 * @exception DBInitException if an error occurs during access database. It won't
	 * happen however as in this method we do simple variable assigment.
	 */
	void initRepository(String resource_uri) throws DBInitException;

	/**
	 * <code>getResourceUri</code> method returns database connection string.
	 *
	 * @return a <code>String</code> value of database connection string.
	 */
	String getResourceUri();

	/**
	 * <code>plainAuth</code> method performs non-sasl, plain authentication
	 * as described in non-sasl authentication
	 * <a href="http://www.xmpp.org/extensions/xep-0078.html">XEP-0078</a>.
	 *
	 * @param user a <code>String</code> value of user name
	 * @param password a <code>String</code> value of plain user password.
	 * @return a <code>boolean</code> value <code>true</code> on successful
	 * authentication, <code>false</code> on authentication failure.
	 * @exception UserNotFoundException if an given user name is not found in
	 * the authentication repository.
	 * @exception TigaseDBException if an error occurs during during accessing
	 * database;
	 * @exception AuthorizationException if an error occurs during authentication
	 * process.
	 */
	boolean plainAuth(String user, String password)
		throws UserNotFoundException, TigaseDBException, AuthorizationException;

	/**
	 * <code>plainAuth</code> method performs non-sasl, digest authentication
	 * as described in non-sasl authentication
	 * <a href="http://www.xmpp.org/extensions/xep-0078.html">XEP-0078</a>
	 * For now it is empty and always returns <code>false</code> as I don't
	 * have description for database with passwords.
	 *
	 * @param user a <code>String</code> value of user name
	 * @param digest a <code>String</code> value password digest sum
	 * @param id a <code>String</code> value session ID used for digest sum
	 * calculation.
	 * @param alg a <code>String</code> value of algorithm ID used for digest sum
	 * calculation.
	 * @return a <code>boolean</code> value <code>true</code> on successful
	 * authentication, <code>false</code> on authentication failure.
	 * @exception UserNotFoundException if an given user name is not found in
	 * the authentication repository.
	 * @exception TigaseDBException if an error occurs during during accessing
	 * database;
	 * @exception AuthorizationException if an error occurs during authentication
	 * process.
	 */
	boolean digestAuth(String user, String digest, String id, String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException;

	/**
	 * Describe <code>otherAuth</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	boolean otherAuth(Map<String, Object> authProps)
		throws UserNotFoundException, TigaseDBException, AuthorizationException;

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	void addUser(String user, String password)
		throws UserExistsException, TigaseDBException;

	/**
	 * Describe <code>updatePassword</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception TigaseDBException if an error occurs
	 */
  void updatePassword(String user, String password)
		throws TigaseDBException;

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
  void removeUser(String user) throws UserNotFoundException, TigaseDBException;

	/**
	 * Describe <code>logout</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	void logout(String user) throws UserNotFoundException, TigaseDBException;

} // UserAuthRepository
