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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;

import tigase.util.Base64;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.TigaseDBException;
import tigase.db.UserAuthRepository;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.xmpp.BareJID;

import static tigase.db.UserAuthRepository.*;

/**
 * Describe class TigaseAuth here.
 *
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseAuth implements UserAuthRepository {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
    Logger.getLogger("tigase.db.jdbc.TigaseAuth");
	private static final String[] non_sasl_mechs = {"password"};
	private static final String[] sasl_mechs = {"PLAIN"};

	public static final String DERBY_CONNVALID_QUERY = "values 1";
	public static final String JDBC_CONNVALID_QUERY = "select 1";

	/**
	 * Database connection string.
	 */
	private String db_conn = null;
	/**
	 * Database active connection.
	 */
	private Connection conn = null;
	private CallableStatement init_db_sp = null;
	private CallableStatement add_user_plain_pw_sp = null;
	private CallableStatement remove_user_sp = null;
	private CallableStatement get_pass_sp = null;
	private CallableStatement update_pass_plain_pw_sp = null;
	private CallableStatement user_login_plain_pw_sp = null;
	private CallableStatement user_logout_sp = null;
	private CallableStatement users_count_sp = null;

	/**
	 * Prepared statement for testing whether database connection is still
	 * working. If not connection to database is recreated.
	 */
	private PreparedStatement conn_valid_st = null;
	private PreparedStatement users_domain_count_st = null;

	/**
	 * Connection validation helper.
	 */
	private long lastConnectionValidated = 0;
	/**
	 * Connection validation helper.
	 */
	private long connectionValidateInterval = 1000*60;
	private boolean online_status = false;
	private boolean derby_mode = false;

	/**
	 * <code>initPreparedStatements</code> method initializes internal
	 * database connection variables such as prepared statements.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	private void initPreparedStatements() throws SQLException {
		String query = (derby_mode ? DERBY_CONNVALID_QUERY : JDBC_CONNVALID_QUERY);
		conn_valid_st = conn.prepareStatement(query);

		query = "{ call TigInitdb() }";
		init_db_sp = conn.prepareCall(query);

		query = "{ call TigAddUserPlainPw(?, ?) }";
		add_user_plain_pw_sp = conn.prepareCall(query);

		query = "{ call TigRemoveUser(?) }";
		remove_user_sp = conn.prepareCall(query);

		query = "{ call TigGetPassword(?) }";
		get_pass_sp = conn.prepareCall(query);

		query = "{ call TigUpdatePasswordPlainPw(?, ?) }";
		update_pass_plain_pw_sp = conn.prepareCall(query);

		query = "{ call TigUserLoginPlainPw(?, ?) }";
		user_login_plain_pw_sp = conn.prepareCall(query);

		query = "{ call TigUserLogout(?) }";
		user_logout_sp = conn.prepareCall(query);

		query = "{ call TigAllUsersCount() }";
		users_count_sp = conn.prepareCall(query);

		query = "select count(*) from tig_users where user_id like ?";
		users_domain_count_st = conn.prepareCall(query);

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
		throws SQLException, UserNotFoundException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (get_pass_sp) {
				get_pass_sp.setString(1, BareJID.jidToBareJID(user));
				rs = get_pass_sp.executeQuery();
				if (rs.next()) {
					return rs.getString(1);
				} else {
					throw new UserNotFoundException("User does not exist: " + user);
				} // end of if (isnext) else
			}
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
	@Override
	public void queryAuth(final Map<String, Object> authProps) {
		String protocol = (String)authProps.get(PROTOCOL_KEY);
		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, non_sasl_mechs);
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
			derby_mode = db_conn.startsWith("jdbc:derby");
			initPreparedStatements();
		}
	}

	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param connection_str a <code>String</code> value
	 * @exception DBInitException if an error occurs
	 */
	@Override
	public void initRepository(final String connection_str,
		Map<String, String> params) throws DBInitException {
		db_conn = connection_str;
		try {
			initRepo();
			if (params != null && params.get("init-db") != null) {
				init_db_sp.executeQuery();
			}
		} catch (SQLException e) {
			conn = null;
			throw	new DBInitException("Problem initializing jdbc connection: "
				+ db_conn, e);
		}
	}

	@Override
	public String getResourceUri() { return db_conn; }

	/**
	 * <code>getUsersCount</code> method is thread safe. It uses local variable
	 * for storing <code>Statement</code>.
	 *
	 * @return a <code>long</code> number of user accounts in database.
	 */
	@Override
	public long getUsersCount() {
		ResultSet rs = null;
		try {
			checkConnection();
			long users = -1;
			synchronized (users_count_sp) {
				// Load all user count from database
				rs = users_count_sp.executeQuery();
				if (rs.next()) {
					users = rs.getLong(1);
				} // end of while (rs.next())
			}
			return users;
		} catch (SQLException e) {
			return -1;
			//throw new TigaseDBException("Problem loading user list from repository", e);
		} finally {
			release(null, rs);
			rs = null;
		}
	}

	@Override
	public long getUsersCount(String domain) {
		ResultSet rs = null;
		try {
			checkConnection();
			long users = -1;
			synchronized (users_domain_count_st) {
				// Load all user count from database
				users_domain_count_st.setString(1, "%@" + domain);
				rs = users_domain_count_st.executeQuery();
				if (rs.next()) {
					users = rs.getLong(1);
				} // end of while (rs.next())
			}
			return users;
		} catch (SQLException e) {
			return -1;
			//throw new TigaseDBException("Problem loading user list from repository", e);
		} finally {
			release(null, rs);
			rs = null;
		}
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
	@Override
	public boolean plainAuth(final String user, final String password)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (user_login_plain_pw_sp) {
				String user_id = BareJID.jidToBareJID(user);
				user_login_plain_pw_sp.setString(1, user_id);
				user_login_plain_pw_sp.setString(2, password);
				rs = user_login_plain_pw_sp.executeQuery();
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
	@Override
	public boolean digestAuth(final String user, final String digest,
		final String id, final String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		throw new AuthorizationException("Not supported.");
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
	@Override
	public boolean otherAuth(final Map<String, Object> props)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String)props.get(PROTOCOL_KEY);
		if (proto.equals(PROTOCOL_VAL_SASL)) {
			String mech = (String)props.get(MACHANISM_KEY);
			if (mech.equals("PLAIN")) {
				return saslAuth(props);
			} // end of if (mech.equals("PLAIN"))
			throw new AuthorizationException("Mechanism is not supported: " + mech);
		} // end of if (proto.equals(PROTOCOL_VAL_SASL))
		throw new AuthorizationException("Protocol is not supported: " + proto);
	}

	@Override
	public void logout(final String user)
		throws UserNotFoundException, TigaseDBException {
		try {
			checkConnection();
			synchronized (user_logout_sp) {
				user_logout_sp.setString(1, BareJID.jidToBareJID(user));
				user_logout_sp.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
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
	@Override
	public void addUser(final String user, final String password)
		throws UserExistsException, TigaseDBException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (add_user_plain_pw_sp) {
				add_user_plain_pw_sp.setString(1, BareJID.jidToBareJID(user));
				add_user_plain_pw_sp.setString(2, password);
				rs = add_user_plain_pw_sp.executeQuery();
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
			synchronized (update_pass_plain_pw_sp) {
				update_pass_plain_pw_sp.setString(1, BareJID.jidToBareJID(user));
				update_pass_plain_pw_sp.setString(2, password);
				update_pass_plain_pw_sp.execute();
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
			synchronized (remove_user_sp) {
				remove_user_sp.setString(1, BareJID.jidToBareJID(user));
				remove_user_sp.execute();
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

	private boolean saslAuth(final Map<String, Object> props)
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
		if (BareJID.parseJID(user_name)[0] == null) {
			jid = BareJID.toString(user_name, domain);
		}
		props.put(USER_ID_KEY, jid);
		String passwd =	new String(in_data, user_idx, in_data.length - user_idx);
		return plainAuth(jid, passwd);
	}

} // TigaseAuth
