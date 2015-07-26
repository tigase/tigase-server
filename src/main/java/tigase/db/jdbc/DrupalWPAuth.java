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

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.Repository.Meta;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.util.Algorithms;
import tigase.util.Base64;
import tigase.xmpp.BareJID;
//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------

/**
 * Describe class DrupalWPAuth here.
 *
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Meta( supportedUris = { "jdbc:[^:]+:.*" } )
public class DrupalWPAuth implements AuthRepository {

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(DrupalWPAuth.class.getName());
	private static final String[] non_sasl_mechs = { "password" };
	private static final String[] sasl_mechs = { "PLAIN" };

	/** Field description */
	public static final String DRUPAL_USERS_TBL = "users";

	/** Field description */
	public static final String DRUPAL_NAME_FLD = "name";

	/** Field description */
	public static final String DRUPAL_PASS_FLD = "pass";

	/** Field description */
	public static final String DRUPAL_STATUS_FLD = "status";

	/** Field description */
	public static final int DRUPAL_OK_STATUS_VAL = 1;

	/** Field description */
	public static final String WP_USERS_TBL = "wp_users";

	/** Field description */
	public static final String WP_NAME_FLD = "user_login";

	/** Field description */
	public static final String WP_PASS_FLD = "user_pass";

	/** Field description */
	public static final String WP_STATUS_FLD = "user_status";

	/** Field description */
	public static final int WP_OK_STATUS_VAL = 0;
	private static final String SELECT_PASSWORD_QUERY_KEY = "select-password-drupal-wp-query-key";
	private static final String SELECT_STATUS_QUERY_KEY = "select-status-drupal-wp-query-key";
	private static final String INSERT_USER_QUERY_KEY = "insert-user-drupal-wp-query-key";
	private static final String UPDATE_LAST_LOGIN_QUERY_KEY = "update-last-login-drupal-wp-query-key";
	private static final String UPDATE_ONLINE_STATUS_QUERY_KEY =
		"update-online-status-drupal-wp-query-key";

	//~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;
	private String name_fld = DRUPAL_NAME_FLD;
	private String users_tbl = DRUPAL_USERS_TBL;
	private int status_val = DRUPAL_OK_STATUS_VAL;
	private String status_fld = DRUPAL_STATUS_FLD;
	private String pass_fld = DRUPAL_PASS_FLD;
	private boolean online_status = false;
	private boolean last_login = true;

	//~--- methods --------------------------------------------------------------

	@Override
	public void addUser(BareJID user, final String password)
			throws UserExistsException, TigaseDBException {
		try {
			PreparedStatement user_add_st = data_repo.getPreparedStatement(user, INSERT_USER_QUERY_KEY);

			synchronized (user_add_st) {
				user_add_st.setString(1, user.getLocalpart());
				user_add_st.setString(2, Algorithms.hexDigest("", password, "MD5"));
				user_add_st.executeUpdate();
			}
		} catch (NoSuchAlgorithmException e) {
			throw new TigaseDBException("Password encoding algorithm is not supported.", e);
		} catch (SQLException e) {
			throw new UserExistsException("Error while adding user to repository, user exists?", e);
		}
	}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, final String digest, final String id, final String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		throw new AuthorizationException("Not supported.");
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getResourceUri() {
		return data_repo.getResourceUri();
	}

	@Override
	public long getUsersCount() {
		return -1;
	}

	@Override
	public long getUsersCount(String domain) {
		return -1;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(final String connection_str, Map<String, String> params)
			throws DBInitException {
		try {
			data_repo = RepositoryFactory.getDataRepository(null, connection_str, params);

			if (connection_str.contains("online_status=true")) {
				online_status = true;
			}

			if (connection_str.contains("wp_mode=true")) {
				online_status = false;
				last_login = false;
				name_fld = WP_NAME_FLD;
				users_tbl = WP_USERS_TBL;
				status_val = WP_OK_STATUS_VAL;
				status_fld = WP_STATUS_FLD;
				pass_fld = WP_PASS_FLD;
				log.log(Level.INFO, "Initializing Wordpress repository: {0}", connection_str);
			} else {
				log.log(Level.INFO, "Initializing Drupal repository: {0}", connection_str);
			}

			String query = "select " + pass_fld + " from " + users_tbl + " where " + name_fld + " = ?";

			data_repo.initPreparedStatement(SELECT_PASSWORD_QUERY_KEY, query);
			query = "select " + status_fld + " from " + users_tbl + " where " + name_fld + " = ?";
			data_repo.initPreparedStatement(SELECT_STATUS_QUERY_KEY, query);
			query = "insert into " + users_tbl + " (" + name_fld + ", " + pass_fld + ", " + status_fld
					+ ")" + " values (?, ?, " + status_val + ")";
			data_repo.initPreparedStatement(INSERT_USER_QUERY_KEY, query);
			query = "update " + users_tbl + " set access=?, login=? where " + name_fld + " = ?";
			data_repo.initPreparedStatement(UPDATE_LAST_LOGIN_QUERY_KEY, query);
			query = "update " + users_tbl + " set online_status=online_status+? where " + name_fld
					+ " = ?";
			data_repo.initPreparedStatement(UPDATE_ONLINE_STATUS_QUERY_KEY, query);
		} catch (Exception e) {
			data_repo = null;

			throw new DBInitException("Problem initializing jdbc connection: " + connection_str, e);
		}

		try {
			if (online_status) {
				Statement stmt = data_repo.createStatement(null);

				stmt.executeUpdate("update users set online_status = 0;");
				stmt.close();
				stmt = null;
			}
		} catch (SQLException e) {
			if (e.getMessage().contains("'online_status'")) {
				try {
					Statement stmt = data_repo.createStatement(null);

					stmt.executeUpdate("alter table users add online_status int default 0;");
					stmt.close();
					stmt = null;
				} catch (SQLException ex) {
					data_repo = null;

					throw new DBInitException("Problem initializing jdbc connection: " + connection_str, ex);
				}
			} else {
				data_repo = null;

				throw new DBInitException("Problem initializing jdbc connection: " + connection_str, e);
			}
		}
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		updateOnlineStatus(user, -1);
	}

	@Override
	public boolean otherAuth(final Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String) props.get(PROTOCOL_KEY);

		if (proto.equals(PROTOCOL_VAL_SASL)) {
			String mech = (String) props.get(MACHANISM_KEY);

			try {
				if (mech.equals("PLAIN")) {
					boolean login_ok = saslAuth(props);

					if (login_ok) {
						BareJID user = (BareJID) props.get(USER_ID_KEY);

						// Unfortunately, unlike with plainAuth we have to check whether the user
						// is active after successful authentication as before it is completed the
						// user id is not known
						if ( !isActive(user)) {
							throw new AuthorizationException("User account has been blocked.");
						}    // end of if (!isActive(user))

						updateLastLogin(user);
						updateOnlineStatus(user, 1);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "User authenticated: {0}", user);
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("User NOT authenticated");
						}
					}

					return login_ok;
				}        // end of if (mech.equals("PLAIN"))
			} catch (Exception e) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "OTHER authentication error: ", e);
				}

				throw new AuthorizationException("Sasl exception.", e);
			}          // end of try-catch

			throw new AuthorizationException("Mechanism is not supported: " + mech);
		}            // end of if (proto.equals(PROTOCOL_VAL_SASL))

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

		throw new AuthorizationException("Protocol is not supported: " + proto);
	}

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		try {
			if ( !isActive(user)) {
				throw new AuthorizationException("User account has been blocked.");
			}    // end of if (!isActive(user))

			String enc_passwd = Algorithms.hexDigest("", password, "MD5");
			String db_password = getPassword(user);
			boolean login_ok = db_password.equals(enc_passwd);

			if (login_ok) {
				updateLastLogin(user);
				updateOnlineStatus(user, 1);

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "User authenticated: {0}", user);
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "User NOT authenticated: {0}", user);
				}
			}

			return login_ok;
		} catch (NoSuchAlgorithmException e) {
			throw new AuthorizationException("Password encoding algorithm is not supported.", e);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}      // end of catch
	}

	// Implementation of tigase.db.AuthRepository

	@Override
	public void queryAuth(final Map<String, Object> authProps) {
		String protocol = (String) authProps.get(PROTOCOL_KEY);

		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, non_sasl_mechs);
		}    // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))

		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		}    // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Removing user is not supported.");
	}

	@Override
	public void updatePassword(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Updating user password is not supported.");
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException  {
		ResultSet rs = null;

		try {
			PreparedStatement pass_st = data_repo.getPreparedStatement(user, SELECT_PASSWORD_QUERY_KEY);

			synchronized (pass_st) {
				try {
					pass_st.setString(1, user.getLocalpart());
					rs = pass_st.executeQuery();

					if (rs.next()) {
						return rs.getString(1);
					} else {
						throw new UserNotFoundException("User does not exist: " + user);
					}    // end of if (isnext) else
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
		return false;
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) 
					throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Feature not supported");		
	}
	
	private boolean isActive(BareJID user) throws SQLException, UserNotFoundException {
		ResultSet rs = null;

		PreparedStatement status_st = data_repo.getPreparedStatement(user, SELECT_STATUS_QUERY_KEY);

		synchronized (status_st) {
			try {
				status_st.setString(1, user.getLocalpart());
				rs = status_st.executeQuery();

				if (rs.next()) {
					return (rs.getInt(1) == status_val);
				} else {
					throw new UserNotFoundException("User does not exist: " + user);
				}    // end of if (isnext) else
			} finally {
				data_repo.release(null, rs);
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	private boolean saslAuth(final Map<String, Object> props) throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer) props.get("SaslServer");

			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();

				sasl_props.put(Sasl.QOP, "auth");
				sasl_props.put("password-encryption", "MD5");
				ss = Sasl.createSaslServer((String) props.get(MACHANISM_KEY), "xmpp",
						(String) props.get(SERVER_NAME_KEY), sasl_props, new SaslCallbackHandler(props));
				props.put("SaslServer", ss);
			}    // end of if (ss == null)

			String data_str = (String) props.get(DATA_KEY);
			byte[] in_data = ((data_str != null) ? Base64.decode(data_str) : new byte[0]);
			byte[] challenge = ss.evaluateResponse(in_data);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "challenge: {0}",
						((challenge != null) ? new String(challenge) : "null"));
			}

			String challenge_str = (((challenge != null) && (challenge.length > 0))
				? Base64.encode(challenge) : null);

			props.put(RESULT_KEY, challenge_str);

			if (ss.isComplete()) {
				return true;
			} else {
				return false;
			}    // end of if (ss.isComplete()) else
		} catch (SaslException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "SASL authentication error: ", e);
			}

			throw new AuthorizationException("Sasl exception.", e);
		} catch (Exception e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "SASL authentication error: ", e);
			}

			throw new AuthorizationException("Sasl exception.", e);
		}      // end of try-catch
	}

	private void updateLastLogin(BareJID user) throws TigaseDBException {
		if (last_login) {
			try {
				PreparedStatement update_last_login_st =
					data_repo.getPreparedStatement(user, UPDATE_LAST_LOGIN_QUERY_KEY);

				synchronized (update_last_login_st) {
					BigDecimal bd = new BigDecimal((System.currentTimeMillis() / 1000));

					update_last_login_st.setBigDecimal(1, bd);
					update_last_login_st.setBigDecimal(2, bd);
					update_last_login_st.setString(3, user.getLocalpart());
					update_last_login_st.executeUpdate();
				}
			} catch (SQLException e) {
				throw new TigaseDBException("Error accessing repository.", e);
			}    // end of try-catch
		}
	}

	private void updateOnlineStatus(BareJID user, int status) throws TigaseDBException {
		if (online_status) {
			try {
				PreparedStatement update_online_status =
					data_repo.getPreparedStatement(user, UPDATE_ONLINE_STATUS_QUERY_KEY);

				synchronized (update_online_status) {
					update_online_status.setInt(1, status);
					update_online_status.setString(2, user.getLocalpart());
					update_online_status.executeUpdate();
				}
			} catch (SQLException e) {
				throw new TigaseDBException("Error accessing repository.", e);
			}    // end of try-catch
		}
	}

	//~--- inner classes --------------------------------------------------------

	private class SaslCallbackHandler implements CallbackHandler {
		private Map<String, Object> options = null;

		//~--- constructors -------------------------------------------------------

		private SaslCallbackHandler(final Map<String, Object> options) {
			this.options = options;
		}

		//~--- methods ------------------------------------------------------------

		// Implementation of javax.security.auth.callback.CallbackHandler

		@Override
		public void handle(final Callback[] callbacks)
				throws IOException, UnsupportedCallbackException {
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
					}        // end of if (realm == null)

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "RealmCallback: {0}", realm);
					}
				} else {
					if (callbacks[i] instanceof NameCallback) {
						NameCallback nc = (NameCallback) callbacks[i];
						String user_name = nc.getName();

						if (user_name == null) {
							user_name = nc.getDefaultName();
						}      // end of if (name == null)

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
							}    // end of try-catch
						} else {
							if (callbacks[i] instanceof AuthorizeCallback) {
								AuthorizeCallback authCallback = ((AuthorizeCallback) callbacks[i]);
								String authenId = authCallback.getAuthenticationID();
								String authorId = authCallback.getAuthorizationID();

								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
									log.log(Level.FINEST, "AuthorizeCallback: authorId: {0}", authorId);
								}

								// if (authenId.equals(authorId)) {
								authCallback.setAuthorized(true);

								// }    // end of if (authenId.equals(authorId))
							} else {
								throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
							}
						}
					}
				}
			}
		}
	}
}    // DrupalWPAuth


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
