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

import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.AuthRepository;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;

import tigase.util.Base64;
import tigase.util.TigaseStringprepException;

import tigase.xmpp.BareJID;

import static tigase.db.AuthRepository.*;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class TigaseAuth here.
 *
 *
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Meta( supportedUris = { "jdbc:[^:]+:.*" } )
public class TigaseAuth implements AuthRepository {

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger("tigase.db.jdbc.TigaseAuth");
	private static final String[] non_sasl_mechs = { "password" };
	private static final String[] sasl_mechs = { "PLAIN" };
	private static final String INIT_DB_QUERY = "{ call TigInitdb() }";
	private static final String ADD_USER_PLAIN_PW_QUERY = "{ call TigAddUserPlainPw(?, ?) }";
	private static final String REMOVE_USER_QUERY = "{ call TigRemoveUser(?) }";
	private static final String GET_PASSWORD_QUERY = "{ call TigGetPassword(?) }";
	private static final String UPDATE_PASSWORD_PLAIN_PW_QUERY =
		"{ call TigUpdatePasswordPlainPw(?, ?) }";
	private static final String USER_LOGIN_PLAIN_PW_QUERY = "{ call TigUserLoginPlainPw(?, ?) }";
	private static final String USER_LOGOUT_QUERY = "{ call TigUserLogout(?) }";
	private static final String USERS_COUNT_QUERY = "{ call TigAllUsersCount() }";
	private static final String USERS_DOMAIN_COUNT_QUERY =
		"select count(*) from tig_users where user_id like ?";

	//~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;

	//~--- methods --------------------------------------------------------------

	@Override
	public void addUser(BareJID user, final String password)
			throws UserExistsException, TigaseDBException {
		ResultSet rs = null;

		try {
			PreparedStatement add_user_plain_pw_sp =
				data_repo.getPreparedStatement(user, ADD_USER_PLAIN_PW_QUERY);

			synchronized (add_user_plain_pw_sp) {
				try {
					add_user_plain_pw_sp.setString(1, user.toString());
					add_user_plain_pw_sp.setString(2, password);
					rs = add_user_plain_pw_sp.executeQuery();
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new UserExistsException("Error while adding user to repository, user exists?", e);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
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
			long users = -1;
			PreparedStatement users_count_sp = data_repo.getPreparedStatement(null, USERS_COUNT_QUERY);

			synchronized (users_count_sp) {
				try {
					// Load all user count from database
					rs = users_count_sp.executeQuery();

					if (rs.next()) {
						users = rs.getLong(1);
					}    // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
					rs = null;
				}
			}

			return users;
		} catch (SQLException e) {
			return -1;

			// throw new TigaseDBException("Problem loading user list from repository", e);
		}
	}

	@Override
	public long getUsersCount(String domain) {
		ResultSet rs = null;

		try {
			long users = -1;
			PreparedStatement users_domain_count_st =
				data_repo.getPreparedStatement(null, USERS_DOMAIN_COUNT_QUERY);

			synchronized (users_domain_count_st) {
				try {
					// Load all user count from database
					users_domain_count_st.setString(1, "%@" + domain);
					rs = users_domain_count_st.executeQuery();

					if (rs.next()) {
						users = rs.getLong(1);
					}    // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
					rs = null;
				}
			}

			return users;
		} catch (SQLException e) {
			return -1;

			// throw new TigaseDBException("Problem loading user list from repository", e);
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(final String connection_str, Map<String, String> params)
			throws DBInitException {
		try {
			data_repo = RepositoryFactory.getDataRepository(null, connection_str, params);
			data_repo.initPreparedStatement(INIT_DB_QUERY, INIT_DB_QUERY);
			data_repo.initPreparedStatement(ADD_USER_PLAIN_PW_QUERY, ADD_USER_PLAIN_PW_QUERY);
			data_repo.initPreparedStatement(REMOVE_USER_QUERY, REMOVE_USER_QUERY);
			data_repo.initPreparedStatement(GET_PASSWORD_QUERY, GET_PASSWORD_QUERY);
			data_repo.initPreparedStatement(UPDATE_PASSWORD_PLAIN_PW_QUERY,
					UPDATE_PASSWORD_PLAIN_PW_QUERY);
			data_repo.initPreparedStatement(USER_LOGIN_PLAIN_PW_QUERY, USER_LOGIN_PLAIN_PW_QUERY);
			data_repo.initPreparedStatement(USER_LOGOUT_QUERY, USER_LOGOUT_QUERY);
			data_repo.initPreparedStatement(USERS_COUNT_QUERY, USERS_COUNT_QUERY);
			data_repo.initPreparedStatement(USERS_DOMAIN_COUNT_QUERY, USERS_DOMAIN_COUNT_QUERY);

			if ((params != null) && (params.get("init-db") != null)) {
				data_repo.getPreparedStatement(null, INIT_DB_QUERY).executeQuery();
			}
		} catch (Exception e) {
			data_repo = null;

			throw new DBInitException("Problem initializing jdbc connection: " + connection_str, e);
		}
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		try {
			PreparedStatement user_logout_sp = data_repo.getPreparedStatement(user, USER_LOGOUT_QUERY);

			synchronized (user_logout_sp) {
				user_logout_sp.setString(1, user.toString());
				user_logout_sp.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	@Override
	public boolean otherAuth(final Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String) props.get(PROTOCOL_KEY);

		if (proto.equals(PROTOCOL_VAL_SASL)) {
			String mech = (String) props.get(MACHANISM_KEY);

			if (mech.equals("PLAIN")) {
				try {
					return saslAuth(props);
				} catch (TigaseStringprepException ex) {
					throw new AuthorizationException("Stringprep failed for: " + props, ex);
				}
			}    // end of if (mech.equals("PLAIN"))

			throw new AuthorizationException("Mechanism is not supported: " + mech);
		}      // end of if (proto.equals(PROTOCOL_VAL_SASL))

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
		ResultSet rs = null;
		String res_string = null;

		try {
			PreparedStatement user_login_plain_pw_sp
					= data_repo.getPreparedStatement(user, USER_LOGIN_PLAIN_PW_QUERY);

			synchronized (user_login_plain_pw_sp) {
				try {
					user_login_plain_pw_sp.setString(1, user.toString());
					user_login_plain_pw_sp.setString(2, password);
					switch (data_repo.getDatabaseType()) {
						case jtds:
						case sqlserver:
							user_login_plain_pw_sp.executeUpdate();
							rs = user_login_plain_pw_sp.getGeneratedKeys();
							break;
						default:
							rs = user_login_plain_pw_sp.executeQuery();
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
								log.log(Level.FINE,
										"Login failed, for user: ''{0}" + "''" + ", password: ''" + "{1}" + "''"
										+ ", from DB got: " + "{2}", new Object[]{user,
											password, res_string});
							}
						}
					}
				} finally {
					data_repo.release(null, rs);
				}
				throw new UserNotFoundException("User does not exist: " + user);
			}
		} catch (TigaseStringprepException ex) {
			throw new AuthorizationException("Stringprep failed for: " + res_string, ex);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}    // end of catch
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
		try {
			PreparedStatement remove_user_sp = data_repo.getPreparedStatement(user, REMOVE_USER_QUERY);

			synchronized (remove_user_sp) {
				remove_user_sp.setString(1, user.toString());
				remove_user_sp.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	@Override
	public void updatePassword(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException {
		try {
			PreparedStatement update_pass_plain_pw_sp =
				data_repo.getPreparedStatement(user, UPDATE_PASSWORD_PLAIN_PW_QUERY);

			synchronized (update_pass_plain_pw_sp) {
				update_pass_plain_pw_sp.setString(1, user.toString());
				update_pass_plain_pw_sp.setString(2, password);
				update_pass_plain_pw_sp.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		ResultSet rs = null;

		try {
			PreparedStatement get_pass_sp = data_repo.getPreparedStatement(user, GET_PASSWORD_QUERY);

			synchronized (get_pass_sp) {
				try {
					get_pass_sp.setString(1, user.toString());
					rs = get_pass_sp.executeQuery();

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
	
	//~--- methods --------------------------------------------------------------

	private boolean saslAuth(final Map<String, Object> props)
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
}    // TigaseAuth


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
