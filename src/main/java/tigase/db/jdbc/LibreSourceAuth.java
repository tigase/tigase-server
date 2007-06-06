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
package tigase.db.jdbc;

import java.math.BigDecimal;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
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
import tigase.auth.SaslPLAIN;
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
 * Describe class LibreSourceAuth here.
 *
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class LibreSourceAuth implements UserAuthRepository {

  private static final Logger log =
    Logger.getLogger("tigase.db.jdbc.LibreSourceAuth");

	// Since now LS stores passwords in clear text all mechanisms
	// are available.

	private static final String[] non_sasl_mechs = {
		"password"
		, "digest"
	};
	private static final String[] sasl_mechs =
	{
		"PLAIN"
		,	"DIGEST-MD5"
		, "CRAM-MD5"
	};

// 	private static final String[] non_sasl_mechs = {"password"};
// 	private static final String[] sasl_mechs = {"PLAIN"};

	public static final String DEF_USERS_TBL = "casusers_";
	public static final String DEF_PROFILES_TBL = "profileresource_";

	private String users_tbl = DEF_USERS_TBL;
	private String profiles_tbl = DEF_PROFILES_TBL;
	private String db_conn = null;
	private Connection conn = null;
	private PreparedStatement pass_st = null;
	private PreparedStatement status_st = null;
	//	private PreparedStatement max_uid_st = null;
	private PreparedStatement conn_valid_st = null;
	private PreparedStatement update_password = null;
	private PreparedStatement update_last_login_st = null;
	private PreparedStatement update_online_status = null;

	private long lastConnectionValidated = 0;
	private long connectionValidateInterval = 1000*60;

	private void initPreparedStatements() throws SQLException {
		String query = "select passworddigest_ from " + users_tbl
			+ " where username_ = ?;";
		pass_st = conn.prepareStatement(query);

		query = "select accountstatus_ from " + profiles_tbl
			+ " where id_ = ?;";
		status_st = conn.prepareStatement(query);

		query = "select localtime;";
		conn_valid_st = conn.prepareStatement(query);

		query = "update " + users_tbl + " set passworddigest_ = ? where username_ = ?;";
		update_password = conn.prepareStatement(query);
		query = "update " + profiles_tbl + " set lastlogintime_ = ? where id_ = ?;";
		update_last_login_st = conn.prepareStatement(query);
		query = "update " + profiles_tbl + " set onlinestatus_ = ? where id_ = ?;";
		update_online_status = conn.prepareStatement(query);
	}

	private boolean checkConnection() throws SQLException {
		try {
			long tmp = System.currentTimeMillis();
			if ((tmp - lastConnectionValidated) >= connectionValidateInterval) {
				conn_valid_st.executeQuery();
				lastConnectionValidated = tmp;
			} // end of if ()
		} catch (Exception e) {
			initRepo();
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

	private void updateLastLogin(String user) throws TigaseDBException {
		try {
			update_last_login_st.setDate(1, new Date(System.currentTimeMillis()));
			update_last_login_st.setString(2, JIDUtils.getNodeNick(user));
			update_last_login_st.executeUpdate();
		} catch (SQLException e) {
			throw new TigaseDBException("Error accessin repository.", e);
		} // end of try-catch
	}

	private void updateOnlineStatus(String user, int status)
		throws TigaseDBException {
		try {
			update_online_status.setInt(1, status);
			update_online_status.setString(2, JIDUtils.getNodeNick(user));
			update_online_status.executeUpdate();
		} catch (SQLException e) {
			throw new TigaseDBException("Error accessin repository.", e);
		} // end of try-catch
	}

	private boolean isActive(final String user)
		throws SQLException, UserNotFoundException {
		ResultSet rs = null;
		try {
			status_st.setString(1, JIDUtils.getNodeNick(user));
			rs = status_st.executeQuery();
			if (rs.next()) {
				int res = rs.getInt(1);
				return (rs.wasNull() || res == 0);
			} else {
				throw new UserNotFoundException("User does not exist: " + user);
			} // end of if (isnext) else
		} finally {
			release(null, rs);
		}
	}

	private String getPassword(final String user)
		throws SQLException, UserNotFoundException {
		ResultSet rs = null;
		try {
			pass_st.setString(1, JIDUtils.getNodeNick(user));
			rs = pass_st.executeQuery();
			if (rs.next()) {
				return rs.getString(1);
			} else {
				throw new UserNotFoundException("User does not exist: " + user);
			} // end of if (isnext) else
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
			authProps.put(RESULT_KEY, non_sasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
	}

	private void initRepo() throws SQLException {
		conn = DriverManager.getConnection(db_conn);
		initPreparedStatements();
	}

	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param connection_str a <code>String</code> value
	 * @exception DBInitException if an error occurs
	 */
	public void initRepository(final String connection_str)
		throws DBInitException {
		db_conn = connection_str;
		try {
			initRepo();
		} catch (SQLException e) {
			conn = null;
			throw	new DBInitException("Problem initializing jdbc connection: "
				+ db_conn, e);
		}
	}

	public String getResourceUri() { return db_conn; }

// 	/**
// 	 * This is not fully correct HEX representation of digest sum but
// 	 * this is how Libre Source does it so I have to be compatible with them.
// 	 *
// 	 * @param passwd a <code>String</code> value
// 	 * @return a <code>String</code> value
// 	 */
// 	private String ls_digest(String passwd) throws NoSuchAlgorithmException {
// 		byte[] md5 = Algorithms.digest("", passwd, "MD5");
// 		StringBuilder sb = new StringBuilder();
// 		for (byte b: md5) {
// 			sb.append(Integer.toHexString(b));
// 		}
// 		return sb.toString();
// 	}

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
		try {
			checkConnection();
			if (!isActive(user)) {
				throw new AuthorizationException("User account has been blocked.");
			} // end of if (!isActive(user))
			//			String enc_passwd = ls_digest(password);
			String db_password = getPassword(user);
			boolean login_ok = db_password.equals(password);
			if (login_ok) {
				updateLastLogin(user);
				updateOnlineStatus(user, 1);
			} // end of if (login_ok)
			return login_ok;
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		} // end of catch
	}

	public void logout(final String user)
		throws UserNotFoundException, TigaseDBException {
		try {
			checkConnection();
			updateOnlineStatus(user, 0);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
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
	public boolean digestAuth(final String user, final String digest,
		final String id, final String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		try {
			checkConnection();
			if (!isActive(user)) {
				throw new AuthorizationException("User account has been blocked.");
			} // end of if (!isActive(user))
			final String db_password = getPassword(user);
			final String digest_db_pass =	Algorithms.hexDigest(id, db_password, alg);
			log.finest("Comparing passwords, given: " + digest
				+ ", db: " + digest_db_pass);
			return digest.equals(digest_db_pass);
		} catch (NoSuchAlgorithmException e) {
			throw new AuthorizationException("No such algorithm.", e);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		} // end of catch
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
			//			String mech = (String)props.get(MACHANISM_KEY);
			//			if (mech.equals("PLAIN")) {
				boolean login_ok = saslAuth(props);
				if (login_ok) {
					String user = (String)props.get(USER_ID_KEY);
					updateLastLogin(user);
					updateOnlineStatus(user, 1);
				} // end of if (login_ok)
				return login_ok;
				//			} // end of if (mech.equals("PLAIN"))
			//throw new AuthorizationException("Mechanism is not supported: " + mech);
		} // end of if (proto.equals(PROTOCOL_VAL_SASL))
		throw new AuthorizationException("Protocol is not supported: " + proto);
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
		Statement stmt = null;
		try {
			checkConnection();
			stmt = conn.createStatement();
// 			String query = "insert into " + users_tbl
// 				+ " (username_, passworddigest_)"
// 				+ " values ('" + JIDUtils.getNodeNick(user)
// 				+ "', '" + ls_digest(password) + "');";
			String query = "insert into " + users_tbl
				+ " (username_, passworddigest_)"
				+ " values ('" + JIDUtils.getNodeNick(user)
				+ "', '" + password + "');";
			stmt.executeUpdate(query);
			query = "insert into " + profiles_tbl
				+ " (id_, accountstatus_)"
				+ " values ('" + JIDUtils.getNodeNick(user) + "', 0);";
			stmt.executeUpdate(query);
		} catch (SQLException e) {
			throw new UserExistsException("Error while adding user to repository, user exists?", e);
		} finally {
			release(stmt, null);
			stmt = null;
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
		throws UserExistsException, TigaseDBException {
		try {
			checkConnection();
			update_password.setString(1, password);
			update_password.setString(2, JIDUtils.getNodeNick(user));
			update_password.executeUpdate();
		} catch (SQLException e) {
			throw new TigaseDBException("Error accessin repository.", e);
		} // end of try-catch
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
		Statement stmt = null;
		try {
			checkConnection();
			stmt = conn.createStatement();
			String query = "delete from " + users_tbl
				+ " where (username_ = '" + JIDUtils.getNodeNick(user)	+ "');";
			stmt.executeUpdate(query);
			query = "delete from " + profiles_tbl
				+ " where (id_ = '" + JIDUtils.getNodeNick(user)	+ "');";
			stmt.executeUpdate(query);
		} catch (SQLException e) {
			throw new UserExistsException("Error while adding user to repository, user exists?", e);
		} finally {
			release(stmt, null);
			stmt = null;
		}
	}

	private boolean saslAuth(final Map<String, Object> props)
		throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer)props.get("SaslServer");
			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();
				sasl_props.put(Sasl.QOP, "auth");
// 				sasl_props.put(SaslPLAIN.ENCRYPTION_KEY, SaslPLAIN.ENCRYPTION_LS_MD5);
				ss = Sasl.createSaslServer((String)props.get(MACHANISM_KEY),
					"xmpp",	(String)props.get(SERVER_NAME_KEY),
					sasl_props, new SaslCallbackHandler(props));
				props.put("SaslServer", ss);
			} // end of if (ss == null)
			String data_str = (String)props.get(DATA_KEY);
			byte[] in_data =
				(data_str != null ? Base64.decode(data_str) : new byte[0]);
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

} // LibreSourceAuth
