/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.db.jdbc;

import tigase.annotations.TigaseDeprecated;
import tigase.auth.XmppSaslException;
import tigase.auth.credentials.Credentials;
import tigase.db.*;
import tigase.db.util.RepositoryVersionAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Algorithms;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.*;
import javax.security.sasl.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.db.AuthRepository.Meta;

/**
 * The user authentication connector allows for customized SQL queries to be used. Queries are defined in the
 * configuration file and they can be either plain SQL queries or stored procedures.
 * <br>
 * If the query starts with characters: <code>{ call</code> then the server assumes this is a stored procedure call,
 * otherwise it is executed as a plain SQL query. Each configuration value is stripped from white characters on both
 * ends before processing.
 * <br>
 * Please don't use semicolon <code>';'</code> at the end of the query as many JDBC drivers get confused and the query
 * may not work for unknown obvious reason.
 * <br>
 * Some queries take arguments. Arguments are marked by question marks <code>'?'</code> in the query. Refer to the
 * configuration parameters description for more details about what parameters are expected in each query.
 * <br>
 * Example configuration.
 * <br>
 * The first example shows how to put a stored procedure as a query with 2 required parameters.
 * <br>
 * <pre>
 * add-user-query={ call TigAddUserPlainPw(?, ?) }
 * </pre>
 * <br>
 * The same query with plain SQL parameters instead:
 * <br>
 * <pre>
 * add-user-query=insert into users (user_id, password) values (?, ?)
 * </pre>
 * <br>
 * Created: Sat Nov 11 22:22:04 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Meta(isDefault = true, supportedUris = {"jdbc:[^:]+:.*"})
@Repository.SchemaId(id = Schema.SERVER_SCHEMA_ID, name = Schema.SERVER_SCHEMA_NAME)
public class TigaseCustomAuth
		extends AbstractAuthRepositoryWithCredentials
		implements DataSourceAware<DataRepository>, RepositoryVersionAware {

	/**
	 * Query executing periodically to ensure active connection with the database.
	 * <br>
	 * Takes no arguments.
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * select 1
	 * </pre>
	 */
	public static final String DEF_CONNVALID_KEY = "conn-valid-query";
	/**
	 * Database initialization query which is run after the server is started.
	 * <br>
	 * Takes no arguments.
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * update tig_users set online_status = 0
	 * </pre>
	 */
	public static final String DEF_INITDB_KEY = "init-db-query";
	/**
	 * Query adding a new user to the database.
	 * <br>
	 * Takes 2 arguments: <code>(user_id (JID), password)</code>
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * insert into tig_users (user_id, user_pw) values (?, ?)
	 * </pre>
	 */
	public static final String DEF_ADDUSER_KEY = "add-user-query";
	/**
	 * Removes a user from the database.
	 * <br>
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * delete from tig_users where user_id = ?
	 * </pre>
	 */
	public static final String DEF_DELUSER_KEY = "del-user-query";
	/**
	 * Retrieves user password from the database for given user_id (JID).
	 * <br>
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * select user_pw from tig_users where user_id = ?
	 * </pre>
	 */
	public static final String DEF_GETPASSWORD_KEY = "get-password-query";
	/**
	 * Updates (changes) password for a given user_id (JID).
	 * <br>
	 * Takes 2 arguments: <code>(password, user_id (JID))</code>
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * update tig_users set user_pw = ? where user_id = ?
	 * </pre>
	 */
	public static final String DEF_UPDATEPASSWORD_KEY = "update-password-query";
	/**
	 * Performs user login. Normally used when there is a special SP used for this purpose. This is an alternative way
	 * to a method requiring retrieving user password. Therefore at least one of those queries must be defined:
	 * <code>user-login-query</code> or <code>get-password-query</code>.
	 * <br>
	 * If both queries are defined then <code>user-login-query</code> is used. Normally this method should be only used
	 * with plain text password authentication or sasl-plain.
	 * <br>
	 * The Tigase server expects a result set with user_id to be returned from the query if login is successful and
	 * empty results set if the login is unsuccessful.
	 * <br>
	 * Takes 2 arguments: <code>(user_id (JID), password)</code>
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * select user_id from tig_users where (user_id = ?) AND (user_pw = ?)
	 * </pre>
	 */
	public static final String DEF_USERLOGIN_KEY = "user-login-query";
	/**
	 * This query is called when user logs out or disconnects. It can record that event in the database.
	 * <br>
	 * Takes 1 argument: <code>(user_id (JID))</code>
	 * <br>
	 * Example query:
	 * <br>
	 * <pre>
	 * update tig_users, set online_status = online_status - 1 where user_id = ?
	 * </pre>
	 */
	public static final String DEF_USERLOGOUT_KEY = "user-logout-query";
	public static final String DEF_UPDATELOGINTIME_KEY = "update-login-time-query";
	public static final String DEF_USERS_COUNT_KEY = "users-count-query";
	public static final String DEF_USERS_DOMAIN_COUNT_KEY = "" + "users-domain-count-query";
	public static final String DEF_LISTDISABLEDACCOUNTS_KEY = "users-list-disabled-accounts-query";
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public static final String DEF_DISABLEACCOUNT_KEY = "user-disable-account-query";
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public static final String DEF_ENABLEACCOUNT_KEY = "user-enable-account-query";
	public static final String DEF_UPDATEACCOUNTSTATUS_KEY = "user-update-account-status-query";
	public static final String DEF_ACCOUNTSTATUS_KEY = "user-account-status-query";
	/**
	 * Comma separated list of NON-SASL authentication mechanisms. Possible mechanisms are: <code>password</code> and
	 * <code>digest</code>. <code>digest</code> mechanism can work only with <code>get-password-query</code> active and
	 * only when password are stored in plain text format in the database.
	 */
	public static final String DEF_NONSASL_MECHS_KEY = "non-sasl-mechs";
	/**
	 * Comma separated list of SASL authentication mechanisms. Possible mechanisms are all mechanisms supported by Java
	 * implementation. The most common are: <code>PLAIN</code>, <code>DIGEST-MD5</code>, <code>CRAM-MD5</code>.
	 * <br>
	 * "Non-PLAIN" mechanisms will work only with the <code>get-password-query</code> active and only when passwords are
	 * stored in plain text format in the database.
	 */
	public static final String DEF_SASL_MECHS_KEY = "sasl-mechs";
	public static final String NO_QUERY = "none";
	public static final String DEF_INITDB_QUERY = "{ call TigInitdb() }";
	public static final String DEF_ADDUSER_QUERY = "{ call TigAddUserPlainPw(?, ?) }";
	public static final String DEF_DELUSER_QUERY = "{ call TigRemoveUser(?) }";
	public static final String DEF_GETPASSWORD_QUERY = "{ call TigGetPassword(?) }";
	public static final String DEF_USERS_COUNT_QUERY = "{ call TigAllUsersCount() }";
	public static final String DEF_USERS_DOMAIN_COUNT_QUERY =
			"" + "select count(*) from tig_users where user_id like ?";
	public static final String DEF_LISTDISABLEDACCOUNTS_QUERY = "{ call TigDisabledAccounts() }";
	public static final String DEF_UPDATEACCOUNTSTATUS_QUERY = "{ call TigUpdateAccountStatus(?, ?) }";
	public static final String DEF_ACCOUNTSTATUS_QUERY = "{ call TigAccountStatus(?) }";
	public static final String DEF_NONSASL_MECHS = "password";
	public static final String DEF_SASL_MECHS = "PLAIN";
	public static final String SP_STARTS_WITH = "{ call";
	private static final Logger log = Logger.getLogger(TigaseCustomAuth.class.getName());
	private static final String DEF_UPDATELOGINTIME_QUERY = "{ call TigUpdateLoginTime(?) }";

	// ~--- fields ---------------------------------------------------------------
	@ConfigField(desc = "Checks account status", alias = DEF_ACCOUNTSTATUS_KEY)
	private String accountstatus_query = DEF_ACCOUNTSTATUS_QUERY;
	@ConfigField(desc = "Query adding a new user to the database", alias = DEF_ADDUSER_KEY)
	private String adduser_query = DEF_ADDUSER_QUERY;
	private DataRepository data_repo = null;
	@ConfigField(desc = "Removes a user from the database", alias = DEF_DELUSER_KEY)
	private String deluser_query = DEF_DELUSER_QUERY;
	// credentials queries
	@ConfigField(desc = "Select list of credentials for account and credential-id", alias = "get-account-credentials-query")
	private String getaccountcredentials_query = "{ call TigUserCredentials_Get(?,?) }";
	@ConfigField(desc = "Select list of credential IDs for account", alias = "get-account-credentialids-query")
	private String getaccountcredentialids_query = "{ call TigUserUsernames_Get(?) }";
	@ConfigField(desc = "Database initialization query which is run after the server is started", alias = DEF_INITDB_KEY)
	private String initdb_query = null;
	@ConfigField(desc = "Lists disabled accounts", alias = DEF_LISTDISABLEDACCOUNTS_KEY)
	private String listdisabledaccounts_query = DEF_LISTDISABLEDACCOUNTS_QUERY;
	@ConfigField(desc = "Comma separated list of NON-SASL authentication mechanisms", alias = DEF_NONSASL_MECHS_KEY)
	private String[] nonsasl_mechs = DEF_NONSASL_MECHS.split(",");
	@ConfigField(desc = "Remove credential for account and credential ID", alias = "remove-account-credential-query")
	private String removeaccountcredential_query = "{ call TigUserCredential_Remove(?,?) }";
	// private String userlogout_query = DEF_USERLOGOUT_QUERY;
	@ConfigField(desc = "Comma separated list of SASL authentication mechanisms", alias = DEF_SASL_MECHS_KEY)
	private String[] sasl_mechs = DEF_SASL_MECHS.split(",");
	@ConfigField(desc = "Update credential for account and credential ID", alias = "update-account-credential-query")
	private String updateaccountcredential_query = "{ call TigUserCredential_Update(?,?,?,?) }";
	@ConfigField(desc = "Updates (changes) account status", alias = DEF_UPDATEACCOUNTSTATUS_KEY)
	private String updateaccountstatus_query = DEF_UPDATEACCOUNTSTATUS_QUERY;
	@ConfigField(desc = "Updates last login/logout timestamps", alias = DEF_UPDATELOGINTIME_KEY)
	private String updatelastlogin_query = DEF_UPDATELOGINTIME_QUERY;
	@ConfigField(desc = "Count users for domain", alias = DEF_USERS_DOMAIN_COUNT_KEY)
	private String userdomaincount_query = DEF_USERS_DOMAIN_COUNT_QUERY;
	private boolean userlogin_active = false;
	@ConfigField(desc = "Performs user login", alias = DEF_USERLOGIN_KEY)
	private String userlogin_query = null;
	// It is better just to not call the query if it is not defined by the user
	// By default it is null then and not called.
	@ConfigField(desc = "Performs user logout", alias = DEF_USERLOGOUT_KEY)
	private String userlogout_query = null;
	@ConfigField(desc = "Counts users", alias = DEF_USERS_COUNT_KEY)
	private String userscount_query = DEF_USERS_COUNT_QUERY;

	@Override
	public void addUser(BareJID user, final String password) throws TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Adding account: {0}", new Object[]{user});
		}

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

			updateCredential(user, Credentials.DEFAULT_CREDENTIAL_ID, password);
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new UserExistsException("Error while adding user to repository, user possibly exists: " + user, e);
		} catch (SQLException e) {
			if (e.getMessage() != null && (e.getMessage().contains("Violation of UNIQUE KEY") ||
					e.getMessage().contains("violates unique constraint \"user_id\"")) ||
					e.getMessage().contains("DerbySQLIntegrityConstraintViolationException")) {
				// This is a workaround SQL Server which just throws SLQ Exception
				throw new UserExistsException("Error while adding user to repository, user possibly exists: " + user,
											  e);
			} else {
				throw new TigaseDBException("Problem accessing repository for user: " + user, e);
			}
		}
	}

	private boolean digestAuth(BareJID user, final String digest, final String id, final String alg)
			throws TigaseDBException, AuthorizationException {
		if (userlogin_active) {
			throw new AuthorizationException("Not supported.");
		} else {
			final String db_password = getPassword(user);

			try {
				final String digest_db_pass = Algorithms.hexDigest(id, db_password, alg);

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Comparing passwords, given: {0}, db: {1}",
							new Object[]{digest, digest_db_pass});
				}

				return digest.equals(digest_db_pass);
			} catch (NoSuchAlgorithmException e) {
				throw new AuthorizationException("No such algorithm.", e);
			} // end of try-catch
		}
	}

	@Override
	public AccountStatus getAccountStatus(final BareJID user) throws TigaseDBException {
		if (accountstatus_query == null) {
			return null;
		}
		try {
			ResultSet rs = null;
			PreparedStatement get_status = data_repo.getPreparedStatement(user, accountstatus_query);

			synchronized (get_status) {
				try {
					get_status.setString(1, user.toString());
					rs = get_status.executeQuery();

					if (rs.next()) {
						int v = rs.getInt(1);
						final AccountStatus accountStatus = AccountStatus.byValue(v);
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Got account: {0} status: {1}", new Object[]{user, accountStatus});
						}
						return accountStatus;
					} else {
						throw new UserNotFoundException("User does not exist: " + user);
					}
				} finally {
					data_repo.release(null, rs);
				}
			}

		} catch (SQLException e) {
			throw new TigaseDBException("Problem with retrieving account status.", e);
		}
	}

	@Override
	public Credentials getCredentials(BareJID user, String credentialId) throws TigaseDBException {
		if (userlogin_active) {
			return new SingleCredential(user, getAccountStatus(user), new Credentials.Entry() {
				@Override
				public String getMechanism() {
					return "STORED-PROCEDURE";
				}

				@Override
				public boolean verifyPlainPassword(String password) {
					try {
						return userLoginAuth(user, password);
					} catch (TigaseDBException | AuthorizationException e) {
						log.log(Level.FINEST, "authorization failed with an error", e);
					}
					return false;
				}
			});
		}
		try {
			PreparedStatement get_credentials = data_repo.getPreparedStatement(user, getaccountcredentials_query);

			List<DefaultCredentials.RawEntry> entries = new ArrayList<>();
			AccountStatus accountStatus = null;
			synchronized (get_credentials) {
				ResultSet rs = null;
				try {
					get_credentials.setString(1, user.toString());
					get_credentials.setString(2, credentialId);
					rs = get_credentials.executeQuery();

					while (rs.next()) {
						String mechanism = rs.getString(1);
						String value = rs.getString(2);
						accountStatus = AccountStatus.byValue(rs.getInt(3));

						// TODO: if we were to add status of particular credentials we would
						// have include it here; currently we only use global status from tig_users;
						entries.add(new DefaultCredentials.RawEntry(mechanism, value));
					}

				} finally {
					data_repo.release(null, rs);
				}
			}
			if (accountStatus == null && entries.isEmpty()) {
				throw new UserNotFoundException(
						"No credentials found for the user: " + user + " (credential ID: " + credentialId + ")");
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Got account: {0} credentials: {1}", new Object[]{user, accountStatus});
			}
			return new DefaultCredentials(user, accountStatus, entries, getCredentialsDecoder());
		} catch (SQLException e) {
			throw new TigaseDBException(
					"Problem with retrieving credentials for account " + user + " and credential ID " + credentialId, e);
		}
	}

	protected String getParamWithDef(Map<String, String> params, String key, String def) {
		if (params == null) {
			return def;
		}

		String result = params.get(key);

		if (result != null) {
			log.log(Level.CONFIG, "Custom query loaded for ''{0}'': ''{1}''", new Object[]{key, result});
		} else {
			result = def;
			log.log(Level.CONFIG, "Default query loaded for ''{0}'': ''{1}''", new Object[]{key, def});
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
	public String getResourceUri() {
		return data_repo.getResourceUri();
	}

	@Override
	public Collection<String> getCredentialIds(BareJID user) throws TigaseDBException {
		try {
			PreparedStatement credentialIdsStatement = data_repo.getPreparedStatement(user, getaccountcredentialids_query);
			List<String> result = new ArrayList<>();
			synchronized (credentialIdsStatement) {
				ResultSet rs = null;
				try {
					credentialIdsStatement.setString(1, user.toString());
					rs = credentialIdsStatement.executeQuery();

					while (rs.next()) {
						String credentialId = rs.getString(1);
						result.add(credentialId);
					}

				} finally {
					data_repo.release(null, rs);
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Account: {0} has credentialId: {1}", new Object[]{user, result});
			}

			return result;
		} catch (SQLException e) {
			throw new TigaseDBException("Problem with retrieving credential IDs for account " + user, e);
		}
	}

	/**
	 * <code>getUsersCount</code> method is thread safe. It uses local variable for storing <code>Statement</code>.
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
			PreparedStatement users_count = data_repo.getPreparedStatement(null, userscount_query);

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
			PreparedStatement users_domain_count = data_repo.getPreparedStatement(null, userdomaincount_query);

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

	private void initDb() throws SQLException {
		if (initdb_query == null) {
			return;
		}

		PreparedStatement init_db = data_repo.getPreparedStatement(null, initdb_query);

		synchronized (init_db) {
			init_db.executeUpdate();
		}
	}

	@Override
	@Deprecated
	public void initRepository(final String connection_str, Map<String, String> params) throws DBInitException {
		try {
			initdb_query = getParamWithDef(params, DEF_INITDB_KEY, null);

			adduser_query = getParamWithDef(params, DEF_ADDUSER_KEY, DEF_ADDUSER_QUERY);

			deluser_query = getParamWithDef(params, DEF_DELUSER_KEY, DEF_DELUSER_QUERY);

			userlogin_query = getParamWithDef(params, DEF_USERLOGIN_KEY, null);

			userlogout_query = getParamWithDef(params, DEF_USERLOGOUT_KEY, null);

			updatelastlogin_query = getParamWithDef(params, DEF_UPDATELOGINTIME_KEY, DEF_UPDATELOGINTIME_QUERY);

			userscount_query = getParamWithDef(params, DEF_USERS_COUNT_KEY, DEF_USERS_COUNT_QUERY);

			userdomaincount_query = getParamWithDef(params, DEF_USERS_DOMAIN_COUNT_KEY, DEF_USERS_DOMAIN_COUNT_QUERY);

			listdisabledaccounts_query = getParamWithDef(params, DEF_LISTDISABLEDACCOUNTS_KEY,
														 DEF_LISTDISABLEDACCOUNTS_QUERY);

			updateaccountstatus_query = getParamWithDef(params, DEF_UPDATEACCOUNTSTATUS_KEY,
														DEF_UPDATEACCOUNTSTATUS_QUERY);

			accountstatus_query = getParamWithDef(params, DEF_ACCOUNTSTATUS_KEY, DEF_ACCOUNTSTATUS_QUERY);

			nonsasl_mechs = getParamWithDef(params, DEF_NONSASL_MECHS_KEY, DEF_NONSASL_MECHS).split(",");
			sasl_mechs = getParamWithDef(params, DEF_SASL_MECHS_KEY, DEF_SASL_MECHS).split(",");

			if (data_repo == null) {
				DataRepository dataRepo = RepositoryFactory.getDataRepository(null, connection_str, params);
				dataRepo.checkSchemaVersion(this, true);
				setDataSource(dataRepo);
			}

			if ((params != null) && (params.get("init-db") != null)) {
				initDb();
			}
		} catch (Exception e) {
			data_repo = null;

			throw new DBInitException("Problem initializing jdbc connection: " + connection_str, e);
		}
	}

	@Override
	public boolean isMechanismSupported(String domain, String mechanism) {
		if ("PLAIN".equals(mechanism)) {
			return true;
		}
		if (userlogin_active) {
			return false;
		}
		return super.isMechanismSupported(domain, mechanism);
	}

	@Override
	public void loggedIn(BareJID user) throws TigaseDBException {
		if (updatelastlogin_query == null) {
			return;
		}

		try {
			PreparedStatement ps = data_repo.getPreparedStatement(user, updatelastlogin_query);

			if (ps != null) {
				synchronized (ps) {
					ps.setString(1, user.toString());
					ps.execute();
				}
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	@Override
	public void logout(BareJID user) throws TigaseDBException {
		if (userlogout_query == null) {
			return;
		}

		try {
			PreparedStatement user_logout = data_repo.getPreparedStatement(user, userlogout_query);

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

	// Implementation of tigase.db.AuthRepository

	@Override
	public boolean otherAuth(final Map<String, Object> props)
			throws TigaseDBException, AuthorizationException {
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

	private boolean plainAuth(BareJID user, final String password)
			throws TigaseDBException, AuthorizationException {
		if (userlogin_active) {
			return userLoginAuth(user, password);
		} else {
			String db_password = getPassword(user);

			return (password != null) && (db_password != null) && db_password.equals(password);
		}
	}

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
	public void removeCredential(BareJID user, String credentialId) throws TigaseDBException {
		try {
			PreparedStatement removeCredential_stmt = data_repo.getPreparedStatement(user,
																					 removeaccountcredential_query);
			synchronized (removeCredential_stmt) {
				removeCredential_stmt.setString(1, user.toString());
				removeCredential_stmt.setString(2, credentialId);
				removeCredential_stmt.execute();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		}
	}

	@Override
	public void removeUser(BareJID user) throws TigaseDBException {
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

	private boolean saslAuth(final Map<String, Object> props) throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer) props.get("SaslServer");

			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();

				sasl_props.put(Sasl.QOP, "auth");
				ss = Sasl.createSaslServer((String) props.get(MACHANISM_KEY), "xmpp",
										   (String) props.get(SERVER_NAME_KEY), sasl_props,
										   new SaslCallbackHandler(props));
				props.put("SaslServer", ss);
			} // end of if (ss == null)

			String data_str = (String) props.get(DATA_KEY);
			byte[] in_data = ((data_str != null) ? Base64.decode(data_str) : new byte[0]);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "response: {0}", new String(in_data));
			}

			byte[] challenge = ss.evaluateResponse(in_data);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "challenge: {0}", ((challenge != null) ? new String(challenge) : "null"));
			}

			String challenge_str = (((challenge != null) && (challenge.length > 0)) ? Base64.encode(challenge) : null);

			props.put(RESULT_KEY, challenge_str);

			// end of if (ss.isComplete()) else
			return ss.isComplete();
		} catch (SaslException e) {
			throw new AuthorizationException("Sasl exception.", e);
		} // end of try-catch
	}

	private boolean saslPlainAuth(final Map<String, Object> props)
			throws TigaseDBException, AuthorizationException, TigaseStringprepException {
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

	@Override
	public void setAccountStatus(BareJID user, AccountStatus value) throws TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Setting account: {0} status to: {1}", new Object[]{user, value});
		}
		try {
			PreparedStatement changeState = data_repo.getPreparedStatement(user, updateaccountstatus_query);
			synchronized (changeState) {
				changeState.setString(1, user.toString());
				changeState.setInt(2, value.getValue());
				changeState.execute();
			}
		} catch (SQLException e) {
			log.log(Level.FINEST, "problem with changing user account state", e);
			throw new TigaseDBException("Problem with changing user account state", e);
		}
	}

	@Override
	public void setDataSource(DataRepository data_repo) throws DBInitException {
		try {
			if (initdb_query != null) {
				data_repo.initPreparedStatement(initdb_query, initdb_query);
			}
			if ((adduser_query != null)) {
				data_repo.initPreparedStatement(adduser_query, adduser_query);
			}
			if ((deluser_query != null)) {
				data_repo.initPreparedStatement(deluser_query, deluser_query);
			}
			if (userlogin_query != null) {
				data_repo.initPreparedStatement(userlogin_query, userlogin_query);
				userlogin_active = true;
			}
			if ((userlogout_query != null)) {
				data_repo.initPreparedStatement(userlogout_query, userlogout_query);
			}
			if (updatelastlogin_query != null) {
				data_repo.initPreparedStatement(updatelastlogin_query, updatelastlogin_query);
			}
			if ((userscount_query != null)) {
				data_repo.initPreparedStatement(userscount_query, userscount_query);
			}
			if ((userdomaincount_query != null)) {
				data_repo.initPreparedStatement(userdomaincount_query, userdomaincount_query);
			}
			if (listdisabledaccounts_query != null) {
				data_repo.initPreparedStatement(listdisabledaccounts_query, listdisabledaccounts_query);
			}
			if (updateaccountstatus_query != null) {
				data_repo.initPreparedStatement(updateaccountstatus_query, updateaccountstatus_query);
			}
			if (accountstatus_query != null) {
				data_repo.initPreparedStatement(accountstatus_query, accountstatus_query);
			}
			if (getaccountcredentials_query != null) {
				data_repo.initPreparedStatement(getaccountcredentials_query, getaccountcredentials_query);
			}
			if (getaccountcredentialids_query != null) {
				data_repo.initPreparedStatement(getaccountcredentialids_query, getaccountcredentialids_query);
			}
			if (removeaccountcredential_query != null) {
				data_repo.initPreparedStatement(removeaccountcredential_query, removeaccountcredential_query);
			}
			if (updateaccountcredential_query != null) {
				data_repo.initPreparedStatement(updateaccountcredential_query, updateaccountcredential_query);
			}

			this.data_repo = data_repo;

			if (initdb_query != null) {
				initDb();
			}
		} catch (SQLException ex) {
			data_repo = null;
			throw new DBInitException("Could not initialize TigaseCustomAuth instance", ex);
		}
	}

	@Override
	public void updateCredential(BareJID user, String credentialId, String password)
			throws TigaseDBException {
		List<String[]> entries = getCredentialsEncoder().encodeForAllMechanisms(user, password);
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Updating credentials for user: {0}, credentialId: {1}",
						new Object[]{user, credentialId});
			}
			removeCredential(user, credentialId);

			PreparedStatement updateCredential_stmt = data_repo.getPreparedStatement(user,
																					 updateaccountcredential_query);
			synchronized (updateCredential_stmt) {
				for (String[] entry : entries) {
					updateCredential_stmt.setString(1, user.toString());
					updateCredential_stmt.setString(2, credentialId);
					updateCredential_stmt.setString(3, entry[0]);
					updateCredential_stmt.setString(4, entry[1]);

					updateCredential_stmt.addBatch();
				}
				updateCredential_stmt.executeBatch();
			}
		} catch (SQLException ex) {
			throw new TigaseDBException("Problem accessing repository.", ex);
		}
	}

	@Override
	public void updatePassword(BareJID user, final String password) throws TigaseDBException {
		updateCredential(user, "default", password);
	}

	private boolean userLoginAuth(BareJID user, final String password)
			throws TigaseDBException, AuthorizationException {
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
								log.log(Level.FINE,
										"Login failed, for user: ''{0}" + "''" + ", password: ''" + "{1}" + "''" +
												", from DB got: " + "{2}", new Object[]{user, password, res_string});
							}
						}
					}
				} finally {
					data_repo.release(null, rs);
				}
				throw new UserNotFoundException("User does not exist: " + user + ", in database: " + getResourceUri());
			}
		} catch (TigaseStringprepException ex) {
			throw new AuthorizationException("Stringprep failed for: " + res_string, ex);
		} catch (SQLException e) {
			throw new TigaseDBException("Problem accessing repository.", e);
		} // end of catch
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	private class SaslCallbackHandler
			implements CallbackHandler {

		private Map<String, Object> options = null;

		private SaslCallbackHandler(final Map<String, Object> options) {
			this.options = options;
		}

		// Implementation of javax.security.auth.callback.CallbackHandler

		@Override
		public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
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
						try {
							final AccountStatus accountStatus = getAccountStatus(jid);
							if (accountStatus.isInactive()) {
								throw XmppSaslException.getExceptionFor(accountStatus);
							}
						} catch (TigaseDBException e) {
							throw new IOException("Account Status retrieving problem.", e);
						}

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
								throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
							}
						}
					}
				}
			}
		}
	}
} // TigaseCustomAuth

