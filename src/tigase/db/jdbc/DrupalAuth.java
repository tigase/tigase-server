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
import tigase.util.JID;

import static tigase.db.UserAuthRepository.*;

/**
 * Describe class DrupalAuth here.
 *
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DrupalAuth implements UserAuthRepository {

  private static final Logger log =
    Logger.getLogger("tigase.db.jdbc.DrupalAuth");
	private static final String[] non_sasl_mechs = {"password"};
	private static final String[] sasl_mechs = {"PLAIN"};

	public static final String DEF_USERS_TBL = "users";

	private String users_tbl = DEF_USERS_TBL;
	private String db_conn = null;
	private Connection conn = null;
	private PreparedStatement pass_st = null;
	private PreparedStatement status_st = null;
	private PreparedStatement user_add_st = null;
	private PreparedStatement max_uid_st = null;
	private PreparedStatement conn_valid_st = null;
	private PreparedStatement update_last_login_st = null;

	private long lastConnectionValidated = 0;
	private long connectionValidateInterval = 1000*60;

	private void initPreparedStatements() throws SQLException {
		String query = "select pass from " + users_tbl
			+ " where name = ?;";
		pass_st = conn.prepareStatement(query);

		query = "select status from " + users_tbl
			+ " where name = ?;";
		status_st = conn.prepareStatement(query);

		query = "insert into " + users_tbl
			+ " (uid, name, pass, status)"
			+ " values (?, ?, ?, 1);";
		user_add_st = conn.prepareStatement(query);

		query = "select max(uid) from " + users_tbl;
		max_uid_st = conn.prepareStatement(query);

		query = "select count(*) from " + users_tbl;
		conn_valid_st = conn.prepareStatement(query);

		query = "update " + users_tbl + " set access=?, login=? where name=?;";
		update_last_login_st = conn.prepareStatement(query);
	}

	private boolean checkConnection() throws SQLException {
		try {
// 			if (!conn.isValid(5)) {
// 				initRepo();
// 			} // end of if (!conn.isValid())
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
			BigDecimal bd = new BigDecimal((System.currentTimeMillis()/1000));
			update_last_login_st.setBigDecimal(1, bd);
			update_last_login_st.setBigDecimal(2, bd);
			update_last_login_st.setString(3, JID.getNodeNick(user));
			update_last_login_st.executeUpdate();
		} catch (SQLException e) {
			throw new TigaseDBException("Error accessin repository.", e);
		} // end of try-catch
	}

	private boolean isActive(final String user)
		throws SQLException, UserNotFoundException {
		ResultSet rs = null;
		try {
			status_st.setString(1, JID.getNodeNick(user));
			rs = status_st.executeQuery();
			if (rs.next()) {
				return (rs.getInt(1) == 1);
			} else {
				throw new UserNotFoundException("User does not exist: " + user);
			} // end of if (isnext) else
		} finally {
			release(null, rs);
		}
	}

	private long getMaxUID() throws SQLException {
		ResultSet rs = null;
		try {
			rs = max_uid_st.executeQuery();
			if (rs.next()) {
				BigDecimal max_uid = rs.getBigDecimal(1);
				System.out.println("MAX UID = " + max_uid.longValue());
				return max_uid.longValue();
			} else {
				System.out.println("MAX UID = -1!!!!");
				return -1;
			} // end of else
		} finally {
			release(null, rs);
		}
	}

	private String getPassword(final String user)
		throws SQLException, UserNotFoundException {
		ResultSet rs = null;
		try {
			checkConnection();
			pass_st.setString(1, JID.getNodeNick(user));
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
			String enc_passwd = Algorithms.hexDigest("", password, "MD5");
			String db_password = getPassword(user);
			boolean login_ok = db_password.equals(enc_passwd);
			if (login_ok) {
				updateLastLogin(user);
			} // end of if (login_ok)
			return login_ok;
		} catch (NoSuchAlgorithmException e) {
			throw
				new AuthorizationException("Password encoding algorithm is not supported.",
					e);
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
	public boolean otherAuth(final Map<String, Object> props)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String)props.get(PROTOCOL_KEY);
		if (proto.equals(PROTOCOL_VAL_SASL)) {
			String mech = (String)props.get(MACHANISM_KEY);
			if (mech.equals("PLAIN")) {
				boolean login_ok = saslAuth(props);
				if (login_ok) {
					String user = (String)props.get(USER_ID_KEY);
					updateLastLogin(user);
				} // end of if (login_ok)
				return login_ok;
			} // end of if (mech.equals("PLAIN"))
			throw new AuthorizationException("Mechanism is not supported: " + mech);
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
		ResultSet rs = null;
		try {
			checkConnection();
			long uid = getMaxUID()+1;
			user_add_st.setLong(1, uid);
			user_add_st.setString(2, JID.getNodeNick(user));
			user_add_st.setString(3, Algorithms.hexDigest("", password, "MD5"));
			user_add_st.executeUpdate();
		} catch (NoSuchAlgorithmException e) {
			throw
				new TigaseDBException("Password encoding algorithm is not supported.",
					e);
		} catch (SQLException e) {
			throw new UserExistsException("Error while adding user to repository, user exists?", e);
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
		throws UserExistsException, TigaseDBException {
		throw new TigaseDBException("Updatin user password is not supported.");
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
		throw new TigaseDBException("Removing user is not supported.");
	}

	private boolean saslAuth(final Map<String, Object> props)
		throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer)props.get("SaslServer");
			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();
				sasl_props.put(Sasl.QOP, "auth");
				sasl_props.put(SaslPLAIN.ENCRYPTION_KEY, SaslPLAIN.ENCRYPTION_MD5);
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
					jid = JID.getNodeID(user_name, (String)options.get(REALM_KEY));
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

} // DrupalAuth
