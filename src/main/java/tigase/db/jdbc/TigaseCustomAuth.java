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
package tigase.db.jdbc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;
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

import tigase.util.Base64;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.TigaseDBException;
import tigase.db.UserAuthRepository;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.util.Algorithms;
import tigase.util.JIDUtils;

import static tigase.db.UserAuthRepository.*;

/**
 * The user authentication connector allows for customized SQL queries to be used.
 * Queries are defined in the configuration file and they can be either plain SQL
 * queries or stored procedures.
 *
 * If the query starts with characters: <code>{ call</code> then the server
 * assumes this is a stored procedure call, otherwise it is executed as a plain
 * SQL query. Each configuration value is stripped from white characters on both
 * ends before processing.
 *
 * Please don't use semicolon <code>';'</code> at the end of the query as many
 * JDBC drivers get confused and the query may not work for unknown obious
 * reason.
 *
 * Some queries take arguments. Arguments are marked by question marks
 * <code>'?'</code> in the query. Refer to the configuration parameters
 * description for more details about what parameters are expected in each
 * query.
 *
 * Example configuration.
 *
 * The first example shows how to put a stored procedure as a query with
 * 2 required parameters.
 * <pre>
 * add-user-query={ call TigAddUserPlainPw(?, ?) }
 * </pre>
 * The same query with plain SQL parameters instead:
 * <pre>
 * add-user-query=insert into users (user_id, password) values (?, ?)
 * </pre>
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseCustomAuth implements UserAuthRepository {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
    Logger.getLogger("tigase.db.jdbc.TigaseCustomAuth");

	/**
	 * Query executing periodically to ensure active connection with the database.
	 *
	 * Takes no arguments.
	 *
	 * Example query:
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
	 * <pre>
	 * delete from tig_users where user_id = ?
	 * </pre>
	 */
	public static final String DEF_DELUSER_KEY = "del-user-query";
	/**
	 * Rertieves user password from the database for given user_id (JID).
	 *
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 *
	 * Example query:
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
	 * <pre>
	 * update tig_users set user_pw = ? where user_id = ?
	 * </pre>
	 */
	public static final String DEF_UPDATEPASSWORD_KEY = "update-password-query";
	/**
	 * Performs user login. Normally used when there is a special SP used for this
	 * purpose. This is an alternative way to a method requiring retrieving
	 * user password. Therefore at least one of those queries must be defined:
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
	 * <pre>
	 * update tig_users, set online_status = online_status - 1 where user_id = ?
	 * </pre>
	 */
	public static final String DEF_USERLOGOUT_KEY = "user-logout-query";
	/**
	 * Comma separated list of NON-SASL authentication mechanisms. Possible mechanisms
	 * are: <code>password</code> and <code>digest</code>. <code>digest</code>
	 * mechanism can work only with <code>get-password-query</code> active and only
	 * when password are stored in plain text format in the database.
	 */
	public static final String DEF_NONSASL_MECHS_KEY = "non-sasl-mechs";
	/**
	 * Comma separated list of SASL authentication mechanisms. Possible mechanisms
	 * are all mechanisms supported by Java implementation. The most common are:
	 * <code>PLAIN</code>, <code>DIGEST-MD5</code>, <code>CRAM-MD5</code>.
	 *
	 * "Non-PLAIN" mechanisms will work only with the <code>get-password-query</code>
	 * active and only when passwords are stored in plain text formay in the database.
	 */
	public static final String DEF_SASL_MECHS_KEY = "sasl-mechs";

	public static final String DEF_CONNVALID_QUERY = "select 1";
	public static final String DEF_INITDB_QUERY = "{ call TigInitdb() }";
	public static final String DEF_ADDUSER_QUERY = "{ call TigAddUserPlainPw(?, ?) }";
	public static final String DEF_DELUSER_QUERY = "{ call TigRemoveUser(?) }";
	public static final String DEF_GETPASSWORD_QUERY = "{ call TigGetPassword(?) }";
	public static final String DEF_UPDATEPASSWORD_QUERY
    = "{ call TigUpdatePasswordPlainPwRev(?, ?) }";
	public static final String DEF_USERLOGIN_QUERY
    = "{ call TigUserLoginPlainPw(?, ?) }";
	public static final String DEF_USERLOGOUT_QUERY = "{ call TigUserLogout(?) }";

	public static final String DEF_NONSASL_MECHS = "password";
	public static final String DEF_SASL_MECHS = "PLAIN";

	public static final String SP_STARTS_WITH = "{ call";

	private String convalid_query = DEF_CONNVALID_QUERY;
	private String initdb_query = DEF_INITDB_QUERY;
	private String adduser_query = DEF_ADDUSER_QUERY;
	private String deluser_query = DEF_DELUSER_QUERY;
	private String getpassword_query = DEF_GETPASSWORD_QUERY;
	private String updatepassword_query = DEF_UPDATEPASSWORD_QUERY;
	private String userlogin_query = DEF_USERLOGIN_QUERY;
	// It is better just to not call the query if it is not defined by the user
	// By default it is null then and not called.
	private String userlogout_query = null;
//	private String userlogout_query = DEF_USERLOGOUT_QUERY;

	private String[] nonsasl_mechs = DEF_NONSASL_MECHS.split(",");
	private String[] sasl_mechs = DEF_SASL_MECHS.split(",");

	/**
	 * Database connection string.
	 */
	private String db_conn = null;
	/**
	 * Database active connection.
	 */
	private Connection conn = null;
	private PreparedStatement init_db = null;
	private PreparedStatement add_user = null;
	private PreparedStatement remove_user = null;
	private PreparedStatement get_pass = null;
	private PreparedStatement update_pass = null;
	private PreparedStatement user_login = null;
	private PreparedStatement user_logout = null;
	/**
	 * Prepared statement for testing whether database connection is still
	 * working. If not connection to database is recreated.
	 */
	private PreparedStatement conn_valid_st = null;

	/**
	 * Connection validation helper.
	 */
	private long lastConnectionValidated = 0;
	/**
	 * Connection validation helper.
	 */
	private long connectionValidateInterval = 1000*60;
	private boolean online_status = false;
	private boolean userlogin_active = false;

	private PreparedStatement prepareQuery(String query) throws SQLException {
		if (query.startsWith(SP_STARTS_WITH)) {
			return conn.prepareCall(query);
		} else {
			return conn.prepareStatement(query);
		}
	}

	/**
	 * <code>initPreparedStatements</code> method initializes internal
	 * database connection variables such as prepared statements.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	private void initPreparedStatements() throws SQLException {
		conn_valid_st = prepareQuery(convalid_query);
		init_db = prepareQuery(initdb_query);
		add_user = prepareQuery(adduser_query);
		remove_user = prepareQuery(deluser_query);
		get_pass = prepareQuery(getpassword_query);
		update_pass = prepareQuery(updatepassword_query);
		user_login = prepareQuery(userlogin_query);
		if (userlogout_query != null) {
			user_logout = prepareQuery(userlogout_query);
		}
	}

	/**
	 * <code>checkConnection</code> method checks database connection before any
	 * query. For some database servers (or JDBC drivers) it happens the connection
	 * is dropped if not in use for a long time or after certain timeout passes.
	 * This method allows us to detect the problem and reinitialize database
	 * connection.
	 *
	 * @return a <code>boolean</code> value if the database connection is working.
	 * @exception SQLException if an error occurs on database query.
	 */
	private boolean checkConnection() throws SQLException {
		ResultSet rs = null;
		try {
			synchronized (conn_valid_st) {
				long tmp = System.currentTimeMillis();
				if ((tmp - lastConnectionValidated) >= connectionValidateInterval) {
					rs = conn_valid_st.executeQuery();
					lastConnectionValidated = tmp;
				} // end of if ()
			}
		} catch (Exception e) {
			initRepo();
		} finally {
			release(null, rs);
		} // end of try-catch
		return true;
	}

	private void release(Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) { }
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException sqlEx) { }
		}
	}

	private String getPassword(final String user)
		throws TigaseDBException, UserNotFoundException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (get_pass) {
				get_pass.setString(1, JIDUtils.getNodeID(user));
				rs = get_pass.executeQuery();
				if (rs.next()) {
					return rs.getString(1);
				} else {
					throw new UserNotFoundException("User does not exist: " + user);
				} // end of if (isnext) else
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem with retrieving user password.", e);
		} finally {
			release(null, rs);
		}
	}

	// Implementation of tigase.db.UserAuthRepository

	/**
	 * Describe <code>queryAuth</code> method here.
	 *
	 * @param authProps a <code>Map</code> value
	 */
	public void queryAuth(final Map<String, Object> authProps) {
		String protocol = (String)authProps.get(PROTOCOL_KEY);
		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, nonsasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
	}

	/**
	 * <code>initRepo</code> method initializes database connection
	 * and data repository.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	private void initRepo() throws SQLException {
		synchronized (db_conn) {
			conn = DriverManager.getConnection(db_conn);
			initPreparedStatements();
		}
	}

	private String getParamWithDef(Map<String, String> params, String key, String def) {
		if (params == null) {
			return def;
		}
		String result = params.get(key);
		if (result != null) {
			log.config("Custom query loaded for '" + key + "': '" + result + "'");
		} else {
			log.config("Default query loaded for '" + key + "': '" + def + "'");
		}
		return result != null ? result.trim() : def;
	}

	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param connection_str a <code>String</code> value
	 * @exception DBInitException if an error occurs
	 */
	public void initRepository(final String connection_str,
		Map<String, String> params) throws DBInitException {
		db_conn = connection_str;
		convalid_query = getParamWithDef(params, DEF_CONNVALID_KEY, DEF_CONNVALID_QUERY);
		initdb_query = getParamWithDef(params, DEF_INITDB_KEY, DEF_INITDB_QUERY);
		adduser_query = getParamWithDef(params, DEF_ADDUSER_KEY, DEF_ADDUSER_QUERY);
		deluser_query = getParamWithDef(params, DEF_DELUSER_KEY, DEF_DELUSER_QUERY);
		getpassword_query = getParamWithDef(params, DEF_GETPASSWORD_KEY,
			DEF_GETPASSWORD_QUERY);
		updatepassword_query = getParamWithDef(params, DEF_UPDATEPASSWORD_KEY,
			DEF_UPDATEPASSWORD_QUERY);
		if (params != null && params.get(DEF_USERLOGIN_KEY) != null) {
			userlogin_query = getParamWithDef(params, DEF_USERLOGIN_KEY,
				DEF_USERLOGIN_QUERY);
			userlogin_active = true;
		}
		userlogout_query = getParamWithDef(params, DEF_USERLOGOUT_KEY, null);

		nonsasl_mechs = getParamWithDef(params, DEF_NONSASL_MECHS_KEY,
			DEF_NONSASL_MECHS).split(",");
		sasl_mechs = getParamWithDef(params, DEF_SASL_MECHS_KEY,
			DEF_SASL_MECHS).split(",");
		try {
			initRepo();
			if (params != null && params.get("init-db") != null) {
				init_db.executeQuery();
			}
		} catch (SQLException e) {
			conn = null;
			throw	new DBInitException("Problem initializing jdbc connection: "
				+ db_conn, e);
		}
	}

	public String getResourceUri() { return db_conn; }

	private boolean userLoginAuth(final String user, final String password)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (user_login) {
				String user_id = JIDUtils.getNodeID(user);
				user_login.setString(1, user_id);
				user_login.setString(2, password);
				rs = user_login.executeQuery();
				if (rs.next()) {
					boolean auth_result_ok = user_id.equals(rs.getString(1));
					if (auth_result_ok) {
						return true;
					} else {
						log.fine("Login failed, for user: '" + user_id + "'"
							+ ", password: '" + password + "'"
							+ ", from DB got: " + rs.getString(1));
					}
				}
				throw new UserNotFoundException("User does not exist: " + user);
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		} finally {
			release(null, rs);
		} // end of catch
	}

	/**
	 * Describe <code>plainAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public boolean plainAuth(final String user, final String password)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		if (userlogin_active) {
			return userLoginAuth(user, password);
		} else {
			String db_password = getPassword(user);
			return password != null && db_password != null &&
			  db_password.equals(password);
		}
	}

	/**
	 * Describe <code>digestAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param digest a <code>String</code> value
	 * @param id a <code>String</code> value
	 * @param alg a <code>String</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	public boolean digestAuth(final String user, final String digest,
		final String id, final String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		if (userlogin_active) {
			throw new AuthorizationException("Not supported.");
		} else {
			final String db_password = getPassword(user);
			try {
				final String digest_db_pass =	Algorithms.hexDigest(id, db_password, alg);
				log.finest("Comparing passwords, given: " + digest
					+ ", db: " + digest_db_pass);
				return digest.equals(digest_db_pass);
			} catch (NoSuchAlgorithmException e) {
				throw new AuthorizationException("No such algorithm.", e);
			} // end of try-catch
		}
	}

	/**
	 * Describe <code>otherAuth</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	public boolean otherAuth(final Map<String, Object> props)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String)props.get(PROTOCOL_KEY);
		if (proto.equals(PROTOCOL_VAL_SASL)) {
			String mech = (String)props.get(MACHANISM_KEY);
			if (mech.equals("PLAIN")) {
				return saslPlainAuth(props);
			} else {
				return saslAuth(props);
			}
		} // end of if (proto.equals(PROTOCOL_VAL_SASL))
		throw new AuthorizationException("Protocol is not supported.");
	}

	public void logout(final String user)
		throws UserNotFoundException, TigaseDBException {
		if (user_logout != null) {
			try {
				checkConnection();
				synchronized (user_logout) {
					user_logout.setString(1, JIDUtils.getNodeID(user));
					user_logout.execute();
				}
			} catch (SQLException e) {
				throw new TigaseDBException("Problem accessing repository.", e);
			}
		}
	}

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void addUser(final String user, final String password)
		throws UserExistsException, TigaseDBException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (add_user) {
				add_user.setString(1, JIDUtils.getNodeID(user));
				add_user.setString(2, password);
				boolean is_result = add_user.execute();
				if (is_result) {
					rs = add_user.getResultSet();
				}
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new UserExistsException("Error while adding user to repository, user exists?", e);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		} finally {
			release(null, rs);
		}
	}

	/**
	 * Describe <code>updatePassword</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void updatePassword(final String user, final String password)
		throws UserNotFoundException, TigaseDBException {
		try {
			checkConnection();
			synchronized (update_pass) {
				update_pass.setString(1, password);
				update_pass.setString(2, JIDUtils.getNodeID(user));
				update_pass.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void removeUser(final String user)
		throws UserNotFoundException, TigaseDBException {
		try {
			checkConnection();
			synchronized (remove_user) {
				remove_user.setString(1, JIDUtils.getNodeID(user));
				remove_user.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	private String decodeString(byte[] source, int start_from) {
		int idx = start_from;
		while (source[idx] != 0 && idx < source.length)	{ ++idx;	}
		return new String(source, start_from, idx - start_from);
	}

	private boolean saslPlainAuth(final Map<String, Object> props)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String data_str = (String)props.get(DATA_KEY);
		String domain = (String)props.get(REALM_KEY);
		props.put(RESULT_KEY, null);
		byte[] in_data = (data_str != null ? Base64.decode(data_str) : new byte[0]);

		int auth_idx = 0;
		while (in_data[auth_idx] != 0 && auth_idx < in_data.length)
		{ ++auth_idx;	}
		String authoriz = new String(in_data, 0, auth_idx);
		int user_idx = ++auth_idx;
		while (in_data[user_idx] != 0 && user_idx < in_data.length)
		{ ++user_idx;	}
		String user_name = new String(in_data, auth_idx, user_idx - auth_idx);
		++user_idx;
		String jid = user_name;
		if (JIDUtils.getNodeNick(user_name) == null) {
			jid = JIDUtils.getNodeID(user_name, domain);
		}
		props.put(USER_ID_KEY, jid);
		String passwd =	new String(in_data, user_idx, in_data.length - user_idx);
		return plainAuth(jid, passwd);
	}

	private boolean saslAuth(final Map<String, Object> props)
		throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer)props.get("SaslServer");
			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();
				sasl_props.put(Sasl.QOP, "auth");
				ss = Sasl.createSaslServer((String)props.get(MACHANISM_KEY),
					"xmpp",	(String)props.get(SERVER_NAME_KEY),
					sasl_props, new SaslCallbackHandler(props));
				props.put("SaslServer", ss);
			} // end of if (ss == null)
			String data_str = (String)props.get(DATA_KEY);
			byte[] in_data =
				(data_str != null ? Base64.decode(data_str) : new byte[0]);
			log.finest("response: " + new String(in_data));
			byte[] challenge = ss.evaluateResponse(in_data);
			log.finest("challenge: " +
				(challenge != null ? new String(challenge) : "null"));
			String challenge_str = (challenge != null && challenge.length > 0
				? Base64.encode(challenge) : null);
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

	private class SaslCallbackHandler implements CallbackHandler {

		private Map<String, Object> options = null;

		private SaslCallbackHandler(final Map<String, Object> options) {
			this.options = options;
		}

		// Implementation of javax.security.auth.callback.CallbackHandler
		/**
		 * Describe <code>handle</code> method here.
		 *
		 * @param callbacks a <code>Callback[]</code> value
		 * @exception IOException if an error occurs
		 * @exception UnsupportedCallbackException if an error occurs
		 */
		public void handle(final Callback[] callbacks)
			throws IOException, UnsupportedCallbackException {

			String jid = null;

			for (int i = 0; i < callbacks.length; i++) {
				log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
				if (callbacks[i] instanceof RealmCallback) {
					RealmCallback rc = (RealmCallback)callbacks[i];
					String realm = (String)options.get(REALM_KEY);
					if (realm != null) {
						rc.setText(realm);
					} // end of if (realm == null)
					log.finest("RealmCallback: " + realm);
				} else if (callbacks[i] instanceof NameCallback) {
					NameCallback nc = (NameCallback)callbacks[i];
					String user_name = nc.getName();
					if (user_name == null) {
						user_name = nc.getDefaultName();
					} // end of if (name == null)
					jid = JIDUtils.getNodeID(user_name, (String)options.get(REALM_KEY));
					options.put(USER_ID_KEY, jid);
					log.finest("NameCallback: " + user_name);
				} else if (callbacks[i] instanceof PasswordCallback) {
					PasswordCallback pc = (PasswordCallback)callbacks[i];
					try {
						String passwd = getPassword(jid);
						pc.setPassword(passwd.toCharArray());
						log.finest("PasswordCallback: " +	passwd);
					} catch (Exception e) {
						throw new IOException("Password retrieving problem.", e);
					} // end of try-catch
				} else if (callbacks[i] instanceof AuthorizeCallback) {
					AuthorizeCallback authCallback = ((AuthorizeCallback)callbacks[i]);
					String authenId = authCallback.getAuthenticationID();
					log.finest("AuthorizeCallback: authenId: " + authenId);
					String authorId = authCallback.getAuthorizationID();
					log.finest("AuthorizeCallback: authorId: " + authorId);
					if (authenId.equals(authorId)) {
						authCallback.setAuthorized(true);
					} // end of if (authenId.equals(authorId))
				} else {
					throw new UnsupportedCallbackException
						(callbacks[i], "Unrecognized Callback");
				}
			}
		}
	}

} // TigaseCustomAuth
