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

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- interfaces -------------------------------------------------------------

/**
 * Interface <code>AuthRepository</code> defines a proxy bridge between user
 * authentication data storage and the Tigase server authentication logic. Important
 * thing about the authentication repository is that it not only stores login credentials
 * but also performs actual user authentication.
 * This is because available authentication mechanisms depend on the way data are stored
 * in the repository (database).
 *
 * Created: Sun Nov  5 21:15:46 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface AuthRepository {

	// Query params (and otherAuth)

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide
	 * desired authentication protocol to the authentication logic.
	 */
	public static final String PROTOCOL_KEY = "protocol";

	/**
	 * Property value for <code>otherAuth</code> method call. It is used to provide
	 * desired authentication SASL protocol to the authentication logic.
	 */
	public static final String PROTOCOL_VAL_SASL = "sasl";

	/**
	 * Property value for <code>otherAuth</code> method call. It is used to provide
	 * desired authentication NON-SASL protocol to the authentication logic.
	 */
	public static final String PROTOCOL_VAL_NONSASL = "nonsasl";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide
	 * authentication handshaking data during login process. Some authentication mechanisms
	 * require exchanging requests between the client and the server. This property key points
	 * back to the data which need to be sent back to the client.
	 */
	public static final String RESULT_KEY = "result";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide
	 * desired authentication mechanism to the authentication logic.
	 */
	public static final String MACHANISM_KEY = "mechanism";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide
	 * authentication realm to the authentication logic. In most cases, the realm is just
	 * a domain name.
	 */
	public static final String REALM_KEY = "realm";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide
	 * authentication domain to the authentication logic. It is highly recommended that this
	 * property is always set, even if the authentication protocol/mechanism does not need it
	 * strictly.
	 */
	public static final String SERVER_NAME_KEY = "server-name";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide an
	 * extra authentication data by the client to the authentication logic.
	 * Please note the <code>RESULT_KEY</code> property key is used to provide authentication
	 * data from the server to the client. This property is used to provide authentication data
	 * from the client to the server.
	 */
	public static final String DATA_KEY = "data";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide a user
	 * ID on successful user login. Please note, the key points to the object of
	 * <code>BareJID</code> type.
	 */
	public static final String USER_ID_KEY = "user-id";

	//~--- methods --------------------------------------------------------------

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>BareJID</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	void addUser(BareJID user, String password) throws UserExistsException, TigaseDBException;

	/**
	 * <code>digestAuth</code> method performs non-sasl, digest authentication
	 * as described in non-sasl authentication
	 * <a href="http://www.xmpp.org/extensions/xep-0078.html">XEP-0078</a>
	 * For now it is empty and always returns <code>false</code> as I don't
	 * have description for database with passwords.
	 *
	 * @param user a <code>BareJID</code> value of user name
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
	boolean digestAuth(BareJID user, String digest, String id, String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException;

	//~--- get methods ----------------------------------------------------------

	/**
	 * <code>getResourceUri</code> method returns database connection string.
	 *
	 * @return a <code>String</code> value of database connection string.
	 */
	String getResourceUri();

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
	 * <code>initRepository</code> method is doing initialization for database
	 * connection. It may also do lazy initialization with database.
	 * Connection to database might be established during the first authentication
	 * request.
	 *
	 * @param resource_uri a <code>String</code> value of database connection string.
	 * The string must also contain database user name and password if required
	 * for connection.
	 * @param params
	 * @exception DBInitException if an error occurs during access database. It won't
	 * happen however as in this method we do simple variable assigment.
	 */
	void initRepository(String resource_uri, Map<String, String> params) throws DBInitException;

	/**
	 * Describe <code>logout</code> method here.
	 *
	 * @param user a <code>BareJID</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	void logout(BareJID user) throws UserNotFoundException, TigaseDBException;

	/**
	 * Describe <code>otherAuth</code> method here.
	 *
	 * @param authProps a <code>Map</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	boolean otherAuth(Map<String, Object> authProps)
			throws UserNotFoundException, TigaseDBException, AuthorizationException;

	/**
	 * <code>plainAuth</code> method performs non-sasl, plain authentication
	 * as described in non-sasl authentication
	 * <a href="http://www.xmpp.org/extensions/xep-0078.html">XEP-0078</a>.
	 *
	 * @param user a <code>BareJID</code> value of user name
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
	boolean plainAuth(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException;

	/**
	 * <code>queryAuth</code> returns mechanisms available for authentication.
	 *
	 * @param authProps a <code>Map</code> value with parameters for authentication.
	 */
	void queryAuth(Map<String, Object> authProps);

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user a <code>BareJID</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException;

	/**
	 * Describe <code>updatePassword</code> method here.
	 *
	 * @param user a <code>BareJID</code> value
	 * @param password a <code>String</code> value
	 * @throws UserNotFoundException
	 * @exception TigaseDBException if an error occurs
	 */
	void updatePassword(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException;
}    // AuthRepository


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
