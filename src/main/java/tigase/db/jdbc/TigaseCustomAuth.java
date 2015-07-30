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

package tigase.db.jdbc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;

import tigase.util.Algorithms;
import tigase.util.Base64;
import tigase.util.TigaseStringprepException;

import tigase.xmpp.BareJID;

import static tigase.db.AuthRepository.*;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.security.NoSuchAlgorithmException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

//~--- classes ----------------------------------------------------------------

/**
 * The user authentication connector allows for customized SQL queries to be
 * used. Queries are defined in the configuration file and they can be either
 * plain SQL queries or stored procedures.
 *
 * If the query starts with characters: <code>{ call</code> then the server
 * assumes this is a stored procedure call, otherwise it is executed as a plain
 * SQL query. Each configuration value is stripped from white characters on both
 * ends before processing.
 *
 * Please don't use semicolon <code>';'</code> at the end of the query as many
 * JDBC drivers get confused and the query may not work for unknown obvious
 * reason.
 *
 * Some queries take arguments. Arguments are marked by question marks
 * <code>'?'</code> in the query. Refer to the configuration parameters
 * description for more details about what parameters are expected in each
 * query.
 *
 * Example configuration.
 *
 * The first example shows how to put a stored procedure as a query with 2
 * required parameters.
 *
 * <pre>
 * add-user-query={ call TigAddUserPlainPw(?, ?) }
 * </pre>
 *
 * The same query with plain SQL parameters instead:
 *
 * <pre>
 * add-user-query=insert into users (user_id, password) values (?, ?)
 * </pre>
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Meta( isDefault=true, supportedUris = { "jdbc:[^:]+:.*" } )
public class TigaseCustomAuth implements AuthRepository {

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(TigaseCustomAuth.class.getName());

	/**
	 * Query executing periodically to ensure active connection with the database.
	 *
	 * Takes no arguments.
	 *
	 * Example query:
	 *
	 * <pre>
	 * select 1
	 * </pre>
	 */
	public static final String DEF_CONNVALID_KEY = "conn-valid-query";

	/**
	 * Database initialization query which is run after the server is started.
	 *
	 * Takes no arguments.
	 *
	 * Example query:
	 *
	 * <pre>
	 * update tig_users set online_status = 0
	 * </pre>
	 */
	public static final String DEF_INITDB_KEY = "init-db-query";

	/**
	 * Query adding a new user to the database.
	 *
	 * Takes 2 arguments: <code>(user_id (JID), password)</code>
	 *
	 * Example query:
	 *
	 * <pre>
	 * insert into tig_users (user_id, user_pw) values (?, ?)
	 * </pre>
	 */
	public static final String DEF_ADDUSER_KEY = "add-user-query";

	/**
	 * Removes a user from the database.
	 *
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 *
	 * Example query:
	 *
	 * <pre>
	 * delete from tig_users where user_id = ?
	 * </pre>
	 */
	public static final String DEF_DELUSER_KEY = "del-user-query";

	/**
	 * Retrieves user password from the database for given user_id (JID).
	 *
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 *
	 * Example query:
	 *
	 * <pre>
	 * select user_pw from tig_users where user_id = ?
	 * </pre>
	 */
	public static final String DEF_GETPASSWORD_KEY = "get-password-query";

	/**
	 * Updates (changes) password for a given user_id (JID).
	 *
	 * Takes 2 arguments: <code>(password, user_id (JID))</code>
	 *
	 * Example query:
	 *
	 * <pre>
	 * update tig_users set user_pw = ? where user_id = ?
	 * </pre>
	 */
	public static final String DEF_UPDATEPASSWORD_KEY = "update-password-query";

	/**
	 * Performs user login. Normally used when there is a special SP used for this
	 * purpose. This is an alternative way to a method requiring retrieving user
	 * password. Therefore at least one of those queries must be defined:
	 * <code>user-login-query</code> or <code>get-password-query</code>.
	 *
	 * If both queries are defined then <code>user-login-query</code> is used.
	 * Normally this method should be only used with plain text password
	 * authentication or sasl-plain.
	 *
	 * The Tigase server expects a result set with user_id to be returned from the
	 * query if login is successful and empty results set if the login is
	 * unsuccessful.
	 *
	 * Takes 2 arguments: <code>(user_id (JID), password)</code>
	 *
	 * Example query:
	 *
	 * <pre>
	 * select user_id from tig_users where (user_id = ?) AND (user_pw = ?)
	 * </pre>
	 */
	public static final String DEF_USERLOGIN_KEY = "user-login-query";

	/**
	 * This query is called when user logs out or disconnects. It can record that
	 * event in the database.
	 *
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 *
	 * Example query:
	 *
	 * <pre>
	 * update tig_users, set online_status = online_status - 1 where user_id = ?
	 * </pre>
	 */
	public static final String DEF_USERLOGOUT_KEY = "user-logout-query";

	/** Field description */
	public static final String DEF_USERS_COUNT_KEY = "users-count-query";

	/** Field description */
	public static final String DEF_USERS_DOMAIN_COUNT_KEY = "" + "users-domain-count-query";

	public static final String DEF_LISTDISABLEDACCOUNTS_KEY= "users-list-disabled-accounts-query";
	
	public static final String DEF_DISABLEACCOUNT_KEY = "user-disable-account-query";
	
	public static final String DEF_ENABLEACCOUNT_KEY = "user-enable-account-query";	
	
	/**
	 * Comma separated list of NON-SASL authentication mechanisms. Possible
	 * mechanisms are: <code>password</code> and <code>digest</code>.
	 * <code>digest</code> mechanism can work only with
	 * <code>get-password-query</code> active and only when password are stored in
	 * plain text format in the database.
	 */
	public static final String DEF_NONSASL_MECHS_KEY = "non-sasl-mechs";

	/**
	 * Comma separated list of SASL authentication mechanisms. Possible mechanisms
	 * are all mechanisms supported by Java implementation. The most common are:
	 * <code>PLAIN</code>, <code>DIGEST-MD5</code>, <code>CRAM-MD5</code>.
	 *
	 * "Non-PLAIN" mechanisms will work only with the
	 * <code>get-password-query</code> active and only when passwords are stored
	 * in plain text format in the database.
	 */
	public static final String DEF_SASL_MECHS_KEY = "sasl-mechs";

	public static final String NO_QUERY = "none";

	/** Field description */
	public static final String DEF_INITDB_QUERY = "{ call TigInitdb() }";

	/** Field description */
	public static final String DEF_ADDUSER_QUERY = "{ call TigAddUserPlainPw(?, ?) }";

	/** Field description */
	public static final String DEF_DELUSER_QUERY = "{ call TigRemoveUser(?) }";

	/** Field description */
	public static final String DEF_GETPASSWORD_QUERY = "{ call TigGetPassword(?) }";

	/** Field description */
	public static final String DEF_UPDATEPASSWORD_QUERY =
			"{ call TigUpdatePasswordPlainPwRev(?, ?) }";

	/** Field description */
	public static final String DEF_USERLOGIN_QUERY = "{ call TigUserLoginPlainPw(?, ?) }";

	/** Field description */
	public static final String DEF_USERLOGOUT_QUERY = "{ call TigUserLogout(?) }";

	/** Field description */
	public static final String DEF_USERS_COUNT_QUERY = "{ call TigAllUsersCount() }";

	/** Field description */
	public static final String DEF_USERS_DOMAIN_COUNT_QUERY = ""
			+ "select count(*) from tig_users where user_id like ?";

	public static final String DEF_LISTDISABLEDACCOUNTS_QUERY = "{ call TigDisabledAccounts() }";
	
	public static final String DEF_DISABLEACCOUNT_QUERY = "{ call TigDisableAccount(?) }";
	
	public static final String DEF_ENABLEACCOUNT_QUERY = "{ call TigEnableAccount(?) }";	
	
	/** Field description */
	public static final String DEF_NONSASL_MECHS = "password";

	/** Field description */
	public static final String DEF_SASL_MECHS = "PLAIN";

	/** Field description */
	public static final String SP_STARTS_WITH = "{ call";

	// ~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;
	private String initdb_query = DEF_INITDB_QUERY;
	private String getpassword_query = DEF_GETPASSWORD_QUERY;
	private String deluser_query = DEF_DELUSER_QUERY;
	private String adduser_query = DEF_ADDUSER_QUERY;
	private String updatepassword_query = DEF_UPDATEPASSWORD_QUERY;
	private String userlogin_query = DEF_USERLOGIN_QUERY;
	private String userdomaincount_query = DEF_USERS_DOMAIN_COUNT_QUERY;
	private String listdisabledaccounts_query = DEF_LISTDISABLEDACCOUNTS_QUERY;
	private String disableaccount_query = DEF_DISABLEACCOUNT_QUERY;
	private String enableaccount_query = DEF_ENABLEACCOUNT_QUERY;
	

	// It is better just to not call the query if it is not defined by the user
	// By default it is null then and not called.
	private String userlogout_query = null;
	private String userscount_query = DEF_USERS_COUNT_QUERY;
	private boolean userlogin_active = false;

	// private String userlogout_query = DEF_USERLOGOUT_QUERY;
	private String[] sasl_mechs = DEF_SASL_MECHS.split(",");
	private String[] nonsasl_mechs = DEF_NONSASL_MECHS.split(",");

	// ~--- methods --------------------------------------------------------------

	@Override
	public void addUser(BareJID user, final String password) throws UserExistsException,
			TigaseDBException {
		if (adduser_query == null) {
			return;
		}

		try {
			ResultSet rs = null;
			PreparedStatement add_user = data_repo.getPreparedStatement(user, adduser_query);

			synchronized (add_user) {
				try {
					add_user.setString(1, user.toString());
					add_user.setString(2, password);

					boolean is_result = add_user.execute();

					if (is_result) {
						rs = add_user.getResultSet();
					}
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new UserExistsException(
					"Error while adding user to repository, user possibly exists: " + user, e);
		} catch (SQLException e) {
			if (e.getMessage() != null 
					&& (e.getMessage().contains("Violation of UNIQUE KEY") || e.getMessage().contains("violates unique constraint \"user_id\""))) {
				// This is a workaround SQL Server which just throws SLQ Exception
				throw new UserExistsException(
						"Error while adding user to repository, user possibly exists: " + user, e);
			} else {
				throw new TigaseDBException("Problem accessing repository for user: " + user, e);
			}
		}
	}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, final String digest, final String id,
			final String alg) throws UserNotFoundException, TigaseDBException,
			AuthorizationException {
		if (userlogin_active) {
			throw new AuthorizationException("Not supported.");
		} else {
			final String db_password = getPassword(user);

			try {
				final String digest_db_pass = Algorithms.hexDigest(id, db_password, alg);

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Comparing passwords, given: {0}, db: {1}", new Object[] {
							digest, digest_db_pass });
				}

				return digest.equals(digest_db_pass);
			} catch (NoSuchAlgorithmException e) {
				throw new AuthorizationException("No such algorithm.", e);
			} // end of try-catch
		}
	}

	// ~--- get methods ----------------------------------------------------------

	@Override
	public String getResourceUri() {
		return data_repo.getResourceUri();
	}

	/**
	 * <code>getUsersCount</code> method is thread safe. It uses local variable
	 * for storing <code>Statement</code>.
	 *
	 * @return a <code>long</code> number of user accounts in database.
	 */
	@Override
	public long getUsersCount() {
		if (userscount_query == null) {
			return -1;
		}

		try {
			long users = -1;
			ResultSet rs = null;
			PreparedStatement users_count =
					data_repo.getPreparedStatement(null, userscount_query);

			synchronized (users_count) {
				try {
					// Load all user count from database
					rs = users_count.executeQuery();

					if (rs.next()) {
						users = rs.getLong(1);
					} // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
				}
			}

			return users;
		} catch (SQLException e) {
			return -1;

			// throw new
			// TigaseDBException("Problem loading user list from repository", e);
		}
	}

	@Override
	public long getUsersCount(String domain) {
		if (userdomaincount_query == null) {
			return -1;
		}

		try {
			long users = -1;
			ResultSet rs = null;
			PreparedStatement users_domain_count =
					data_repo.getPreparedStatement(null, userdomaincount_query);

			synchronized (users_domain_count) {
				try {
					// Load all user count from database
					users_domain_count.setString(1, "%@" + domain);
					rs = users_domain_count.executeQuery();

					if (rs.next()) {
						users = rs.getLong(1);
					} // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
					rs = null;
				}
			}

			return users;
		} catch (SQLException e) {
			return -1;

			// throw new
			// TigaseDBException("Problem loading user list from repository", e);
		}
	}

	// ~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(final String connection_str, Map<String, String> params)
			throws DBInitException {
		try {
			data_repo = RepositoryFactory.getDataRepository(null, connection_str, params);
			initdb_query = getParamWithDef(params, DEF_INITDB_KEY, DEF_INITDB_QUERY);

			if (initdb_query != null) {
				data_repo.initPreparedStatement(initdb_query, initdb_query);
			}

			adduser_query = getParamWithDef(params, DEF_ADDUSER_KEY, DEF_ADDUSER_QUERY);

			if ((adduser_query != null)) {
				data_repo.initPreparedStatement(adduser_query, adduser_query);
			}

			deluser_query = getParamWithDef(params, DEF_DELUSER_KEY, DEF_DELUSER_QUERY);

			if ((deluser_query != null)) {
				data_repo.initPreparedStatement(deluser_query, deluser_query);
			}

			getpassword_query = getParamWithDef(params, DEF_GETPASSWORD_KEY, DEF_GETPASSWORD_QUERY);

			if ((getpassword_query != null)) {
				data_repo.initPreparedStatement(getpassword_query, getpassword_query);
			}

			updatepassword_query =
					getParamWithDef(params, DEF_UPDATEPASSWORD_KEY, DEF_UPDATEPASSWORD_QUERY);

			if ((updatepassword_query != null)) {
				data_repo.initPreparedStatement(updatepassword_query, updatepassword_query);
			}

			userlogin_query = getParamWithDef(params, DEF_USERLOGIN_KEY, DEF_USERLOGIN_QUERY);
			if (userlogin_query  != null) {
				data_repo.initPreparedStatement(userlogin_query, userlogin_query);
				userlogin_active = true;
			}

			userlogout_query =
					getParamWithDef(params, DEF_USERLOGOUT_KEY, DEF_USERLOGOUT_QUERY);

			if ((userlogout_query != null)) {
				data_repo.initPreparedStatement(userlogout_query, userlogout_query);
			}

			userscount_query =
					getParamWithDef(params, DEF_USERS_COUNT_KEY, DEF_USERS_COUNT_QUERY);

			if ((userscount_query != null)) {
				data_repo.initPreparedStatement(userscount_query, userscount_query);
			}

			userdomaincount_query =
					getParamWithDef(params, DEF_USERS_DOMAIN_COUNT_KEY,
							DEF_USERS_DOMAIN_COUNT_QUERY);

			if ((userdomaincount_query != null)) {
				data_repo.initPreparedStatement(userdomaincount_query, userdomaincount_query);
			}
			
			listdisabledaccounts_query = getParamWithDef(params, DEF_LISTDISABLEDACCOUNTS_KEY, 
					DEF_LISTDISABLEDACCOUNTS_QUERY);
			if (listdisabledaccounts_query != null) {
				data_repo.initPreparedStatement(listdisabledaccounts_query, listdisabledaccounts_query);
			}

			disableaccount_query = getParamWithDef(params, DEF_DISABLEACCOUNT_KEY, 
					DEF_DISABLEACCOUNT_QUERY);
			if (disableaccount_query != null) {
				data_repo.initPreparedStatement(disableaccount_query, disableaccount_query);
			}
			
			enableaccount_query = getParamWithDef(params, DEF_ENABLEACCOUNT_KEY, 
					DEF_ENABLEACCOUNT_QUERY);
			if (enableaccount_query != null) {
				data_repo.initPreparedStatement(enableaccount_query, enableaccount_query);
			}
			
			nonsasl_mechs =
					getParamWithDef(params, DEF_NONSASL_MECHS_KEY, DEF_NONSASL_MECHS).split(",");
			sasl_mechs = getParamWithDef(params, DEF_SASL_MECHS_KEY, DEF_SASL_MECHS).split(",");

			if ((params != null) && (params.get("init-db") != null)) {
				initDb();
			}
		} catch (Exception e) {
			data_repo = null;

			throw new DBInitException(
					"Problem initializing jdbc connection: " + connection_str, e);
		}
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		if (userlogout_query == null) {
			return;
		}

		try {
			PreparedStatement user_logout =
					data_repo.getPreparedStatement(user, userlogout_query);

			if (user_logout != null) {
				synchronized (user_logout) {
					user_logout.setString(1, user.toString());
					user_logout.execute();
				}
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	@Override
	public boolean otherAuth(final Map<String, Object> props) throws UserNotFoundException,
			TigaseDBException, AuthorizationException {
		String proto = (String) props.get(PROTOCOL_KEY);

		if (proto.equals(PROTOCOL_VAL_SASL)) {
			String mech = (String) props.get(MACHANISM_KEY);

			if (mech.equals("PLAIN")) {
				try {
					if (saslPlainAuth(props)) {
						return true;
					} else {
						throw new AuthorizationException("Authentication failed.");
					}
				} catch (TigaseStringprepException ex) {
					throw new AuthorizationException("Stringprep failed for: " + props, ex);
				}
			} else {
				return saslAuth(props);
			}
		} // end of if (proto.equals(PROTOCOL_VAL_SASL))

		if (proto.equals(PROTOCOL_VAL_NONSASL)) {
			String password = (String) props.get(PASSWORD_KEY);
			BareJID user_id = (BareJID) props.get(USER_ID_KEY);
			if (password != null) {
				return plainAuth(user_id, password);
			}
			String digest = (String) props.get(DIGEST_KEY);
			if (digest != null) {
				String digest_id = (String) props.get(DIGEST_ID_KEY);
				return digestAuth(user_id, digest, digest_id, "SHA");
			}
		} // end of if (proto.equals(PROTOCOL_VAL_SASL))

		throw new AuthorizationException("Protocol is not supported.");
	}

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		if (userlogin_active) {
			return userLoginAuth(user, password);
		} else {
			String db_password = getPassword(user);

			return (password != null) && (db_password != null) && db_password.equals(password);
		}
	}

	// Implementation of tigase.db.AuthRepository

	@Override
	public void queryAuth(final Map<String, Object> authProps) {
		String protocol = (String) authProps.get(PROTOCOL_KEY);

		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, nonsasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))

		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		if (deluser_query == null) {
			return;
		}

		try {
			PreparedStatement remove_user = data_repo.getPreparedStatement(user, deluser_query);

			synchronized (remove_user) {
				remove_user.setString(1, user.toString());
				remove_user.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	@Override
	public void updatePassword(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException {
		if (updatepassword_query == null) {
			return;
		}

		try {
			PreparedStatement update_pass =
					data_repo.getPreparedStatement(user, updatepassword_query);

			synchronized (update_pass) {
				update_pass.setString(1, password);
				update_pass.setString(2, user.toString());
				update_pass.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	// ~--- get methods ----------------------------------------------------------

	protected String getParamWithDef(Map<String, String> params, String key, String def) {
		if (params == null) {
			return def;
		}

		String result = params.get(key);

		if (result != null) {
			log.log(Level.CONFIG, "Custom query loaded for ''{0}'': ''{1}''", new Object[] {
					key, result });
		} else {
			result = def;
			log.log(Level.CONFIG, "Default query loaded for ''{0}'': ''{1}''", new Object[] {
					key, def });
		}

		if (result != null) {
			result = result.trim();

			if (result.isEmpty() || result.equals(NO_QUERY)) {
				result = null;
			}
		}

		return result;
	}

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException  {
		if (getpassword_query == null) {
			return null;
		}

		try {
			ResultSet rs = null;
			PreparedStatement get_pass =
					data_repo.getPreparedStatement(user, getpassword_query);

			synchronized (get_pass) {
				try {
					get_pass.setString(1, user.toString());
					rs = get_pass.executeQuery();

					if (rs.next()) {
						return rs.getString(1);
					} else {
						throw new UserNotFoundException("User does not exist: " + user);
					} // end of if (isnext) else
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem with retrieving user password.", e);
		}
	}

		@Override
	public boolean isUserDisabled(BareJID user) 
					throws UserNotFoundException, TigaseDBException {
		try {
			ResultSet rs = null;
			PreparedStatement list_disabledaccounts
					= data_repo.getPreparedStatement(user, listdisabledaccounts_query);

			synchronized (list_disabledaccounts) {
				try {
					rs = list_disabledaccounts.executeQuery();
					while (rs.next()) {
						String accountJid = rs.getString(1);
						if (user.toString().equals(accountJid)) {
							return true;
						}
					}
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem with checking if user account is disabled", e);
		}
		return false;
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) 
					throws UserNotFoundException, TigaseDBException {
		try {
			String query = (value == null || !value)
					? enableaccount_query : disableaccount_query;
			PreparedStatement changeState = data_repo.getPreparedStatement(user, query);
			
			synchronized (changeState) {
				changeState.setString(1, user.toString());
				changeState.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new TigaseDBException("Problem with changing user account state", e);
		}
	}
	// ~--- methods --------------------------------------------------------------

	private void initDb() throws SQLException {
		if (initdb_query == null) {
			return;
		}

		PreparedStatement init_db = data_repo.getPreparedStatement(null, initdb_query);

		synchronized (init_db) {
			init_db.executeUpdate();
		}
	}

	private boolean saslAuth(final Map<String, Object> props) throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer) props.get("SaslServer");

			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();

				sasl_props.put(Sasl.QOP, "auth");
				ss =
						Sasl.createSaslServer((String) props.get(MACHANISM_KEY), "xmpp",
								(String) props.get(SERVER_NAME_KEY), sasl_props, new SaslCallbackHandler(
										props));
				props.put("SaslServer", ss);
			} // end of if (ss == null)

			String data_str = (String) props.get(DATA_KEY);
			byte[] in_data = ((data_str != null) ? Base64.decode(data_str) : new byte[0]);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "response: {0}", new String(in_data));
			}

			byte[] challenge = ss.evaluateResponse(in_data);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "challenge: {0}", ((challenge != null) ? new String(
						challenge) : "null"));
			}

			String challenge_str =
					(((challenge != null) && (challenge.length > 0)) ? Base64.encode(challenge)
							: null);

			props.put(RESULT_KEY, challenge_str);

			if (ss.isComplete()) {
				return true;
			} else {
				return false;
			} // end of if (ss.isComplete()) else
		} catch (SaslException e) {
			throw new AuthorizationException("Sasl exception.", e);
		} // end of try-catch
	}

	private boolean saslPlainAuth(final Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException,
			TigaseStringprepException {
		String data_str = (String) props.get(DATA_KEY);
		String domain = (String) props.get(REALM_KEY);

		props.put(RESULT_KEY, null);

		byte[] in_data = ((data_str != null) ? Base64.decode(data_str) : new byte[0]);
		int auth_idx = 0;

		while ((in_data[auth_idx] != 0) && (auth_idx < in_data.length)) {
			++auth_idx;
		}

		String authoriz = new String(in_data, 0, auth_idx);
		int user_idx = ++auth_idx;

		while ((in_data[user_idx] != 0) && (user_idx < in_data.length)) {
			++user_idx;
		}

		String user_name = new String(in_data, auth_idx, user_idx - auth_idx);

		++user_idx;

		BareJID jid = null;

		if (BareJID.parseJID(user_name)[0] == null) {
			jid = BareJID.bareJIDInstance(user_name, domain);
		} else {
			jid = BareJID.bareJIDInstance(user_name);
		}

		props.put(USER_ID_KEY, jid);

		String passwd = new String(in_data, user_idx, in_data.length - user_idx);

		return plainAuth(jid, passwd);
	}

	private boolean userLoginAuth(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		if (userlogin_query == null) {
			return false;
		}

		String res_string = null;

		try {
			ResultSet rs = null;
			PreparedStatement user_login = data_repo.getPreparedStatement(user, userlogin_query);

			synchronized (user_login) {
				try {
					user_login.setString(1, user.toString());
					user_login.setString(2, password);

					switch (data_repo.getDatabaseType()) {
						case sqlserver:
							user_login.executeUpdate();
							rs = user_login.getGeneratedKeys();
							break;
						default:
							rs = user_login.executeQuery();
							break;
					}

					boolean auth_result_ok = false;

					if (rs.next()) {
						res_string = rs.getString(1);

						if (res_string != null) {
							BareJID result = BareJID.bareJIDInstance(res_string);

							auth_result_ok = user.equals(result);
						}

						if (auth_result_ok) {
							return true;
						} else {
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, "Login failed, for user: ''{0}" + "''"
										+ ", password: ''" + "{1}" + "''" + ", from DB got: " + "{2}",
										new Object[]{user, password, res_string});
							}
						}
					}
				} finally {
					data_repo.release(null, rs);
				}
				throw new UserNotFoundException("User does not exist: " + user
						+ ", in database: " + getResourceUri());
			}
		} catch (TigaseStringprepException ex) {
			throw new AuthorizationException("Stringprep failed for: " + res_string, ex);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		} // end of catch
	}

	// ~--- inner classes --------------------------------------------------------

	private class SaslCallbackHandler implements CallbackHandler {
		private Map<String, Object> options = null;

		// ~--- constructors -------------------------------------------------------

		private SaslCallbackHandler(final Map<String, Object> options) {
			this.options = options;
		}

		// ~--- methods ------------------------------------------------------------

		// Implementation of javax.security.auth.callback.CallbackHandler

		@Override
		public void handle(final Callback[] callbacks) throws IOException,
				UnsupportedCallbackException {
			BareJID jid = null;

			for (int i = 0; i < callbacks.length; i++) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Callback: {0}", callbacks[i].getClass().getSimpleName());
				}

				if (callbacks[i] instanceof RealmCallback) {
					RealmCallback rc = (RealmCallback) callbacks[i];
					String realm = (String) options.get(REALM_KEY);

					if (realm != null) {
						rc.setText(realm);
					} // end of if (realm == null)

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "RealmCallback: {0}", realm);
					}
				} else {
					if (callbacks[i] instanceof NameCallback) {
						NameCallback nc = (NameCallback) callbacks[i];
						String user_name = nc.getName();

						if (user_name == null) {
							user_name = nc.getDefaultName();
						} // end of if (name == null)

						jid = BareJID.bareJIDInstanceNS(user_name, (String) options.get(REALM_KEY));
						options.put(USER_ID_KEY, jid);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "NameCallback: {0}", user_name);
						}
					} else {
						if (callbacks[i] instanceof PasswordCallback) {
							PasswordCallback pc = (PasswordCallback) callbacks[i];

							try {
								String passwd = getPassword(jid);

								pc.setPassword(passwd.toCharArray());

								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "PasswordCallback: {0}", passwd);
								}
							} catch (Exception e) {
								throw new IOException("Password retrieving problem.", e);
							} // end of try-catch
						} else {
							if (callbacks[i] instanceof AuthorizeCallback) {
								AuthorizeCallback authCallback = ((AuthorizeCallback) callbacks[i]);
								String authenId = authCallback.getAuthenticationID();

								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
								}

								String authorId = authCallback.getAuthorizationID();

								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "AuthorizeCallback: authorId: {0}", authorId);
								}

								if (authenId.equals(authorId)) {
									authCallback.setAuthorized(true);
								} // end of if (authenId.equals(authorId))
							} else {
								throw new UnsupportedCallbackException(callbacks[i],
										"Unrecognized Callback");
							}
						}
					}
				}
			}
		}
	}
} // TigaseCustomAuth

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
