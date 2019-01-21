/**
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
package tigase.db.derby;

import tigase.auth.credentials.Credentials;
import tigase.util.Algorithms;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Artur Hefczyc
 * @version 5.1.0, 2010.09.11 at 02:08:58 BST
 */
public class StoredProcedures {

	private static final Logger log = Logger.getLogger(StoredProcedures.class.getName());

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final String DEFAULT_USERNAME_SHA1 = sha1(Credentials.DEFAULT_USERNAME);

	private static final String GET_VERSION = "select version from tig_schema_versions where (component = ?)";

	private static String encodePassword(String encMethod, String userId, String userPw) {
		if ((encMethod != null) && "MD5-PASSWORD".equals(encMethod)) {
			return md5(userPw);
		} else if ((encMethod != null) && "MD5-USERID-PASSWORD".equals(encMethod)) {
			return md5(userId + userPw);
		} else if ((encMethod != null) && "MD5-USERNAME-PASSWORD".equals(encMethod)) {
			return md5(userId.substring(0, userId.indexOf("@")) + userPw);
		} else {
			return userPw;
		}

	}

	private static String md5(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			if (data != null) {
				md.update(data.getBytes(UTF8));
			}

			byte[] digest = md.digest();

			return Algorithms.bytesToHex(digest);
		} catch (Exception e) {
			throw new RuntimeException("Error on encoding password", e);
		}
	}

	public static void migrateCredentials() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select 1 from tig_user_credentials");
			boolean migrated = rs.next();
			rs.close();

			if (migrated) {
				return;
			}

			rs = stmt.executeQuery("select uid, user_pw from tig_users where user_pw is not null");

			String encoding = Optional.ofNullable(tigGetDBProperty("password-encoding")).orElse("PLAIN");

			PreparedStatement ps = conn.prepareStatement(
					"insert into tig_user_credentials (uid, username, mechanism, value) values (?, ?, ?, ?)");
			while (rs.next()) {
				ps.setLong(1, rs.getLong(1));
				ps.setString(2, "default");
				ps.setString(3, encoding);
				ps.setString(4, rs.getString(2));
				ps.execute();
			}

			stmt.execute("update tig_users set user_pw = null where user_pw is not null");
		} catch (SQLException e) {
			log.log(Level.WARNING, "Migration of data failed", e);
			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			//throw e;
		} finally {
			conn.close();
		}
	}

	private static String sha1(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");

			if (data != null) {
				md.update(data.getBytes(UTF8));
			}

			byte[] digest = md.digest();

			return Algorithms.bytesToHex(digest);
		} catch (Exception e) {
			throw new RuntimeException("Error on encoding password", e);
		}
	}

	public static void tigAccountStatus(final String user, ResultSet[] data) throws SQLException {
		try (Connection conn = DriverManager.getConnection("jdbc:default:connection")) {
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			PreparedStatement ps = conn.prepareStatement(
					"SELECT account_status FROM tig_users WHERE lower(user_id) = ?");
			ps.setString(1, user);
			data[0] = ps.executeQuery();
		}
	}

	public static void tigActiveAccounts(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where account_status > 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigAddNode(long parentNid, long uid, String node, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"insert into tig_nodes (parent_nid, uid, node) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

			ps.setLong(1, parentNid);
			ps.setLong(2, uid);
			ps.setString(3, node);
			ps.executeUpdate();
			data[0] = ps.getGeneratedKeys();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigAddUser(String userId, String userPw, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("insert into tig_users (user_id) values (?)",
														 Statement.RETURN_GENERATED_KEYS);

			ps.setString(1, userId);
			ps.executeUpdate();

			ResultSet rs = ps.getGeneratedKeys();

			rs.next();

			long generatedKey = rs.getLong(1);

			if (userPw != null) {
				tigUpdatePasswordPlainPw(userId, userPw);
			}

			PreparedStatement ps3 = conn.prepareStatement("select uid from tig_users where uid=?");

			ps3.setLong(1, generatedKey);
			data[0] = ps3.executeQuery();

			PreparedStatement ps2 = conn.prepareStatement(
					"insert into tig_nodes (parent_nid, uid, node) values (NULL, ?, 'root')");

			ps2.setLong(1, generatedKey);
			ps2.executeUpdate();

			if (null == userPw) {
				PreparedStatement ps4 = conn.prepareStatement("update tig_users set account_status = -1 where uid = ?");

				ps4.setLong(1, generatedKey);
				ps4.executeUpdate();
			}
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigAddUserPlainPw(String userId, String userPw, ResultSet[] data) throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);

		tigAddUser(userId, encp, data);
	}

	public static void tigAllUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigAllUsersCount(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("select count(*) as res_cnt from tig_users");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigDisableAccount(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"update tig_users set account_status = 0 where lower(user_id) = ?");

			ps.setString(1, userId.toLowerCase());
			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigDisabledAccounts(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where account_status = 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigEnableAccount(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"update tig_users set account_status = 1 where lower(user_id) = ?");

			ps.setString(1, userId.toLowerCase());
			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigGetComponentVersion(final String component, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String result = null;

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Getting version of the component: " + component);
			}

			PreparedStatement ps = conn.prepareStatement(GET_VERSION);

			ps.setString(1, component.toLowerCase());
			data[0] = ps.executeQuery();

		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static String tigGetDBProperty(final String key) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String result = null;

			if (log.isLoggable(Level.FINEST)) {
				log.finest("function tigGetDBProperty('" + key + "') called");
			}

			PreparedStatement ps = conn.prepareStatement(
					"select pval from tig_pairs, tig_users where (pkey = ?) AND (user_id = 'db-properties') AND (tig_pairs.uid = tig_users.uid)");
			ResultSet rs;

			ps.setString(1, key.toLowerCase());
			rs = ps.executeQuery();

			if (rs.next()) {
				result = rs.getString(1);
			}

			return result;
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigGetPassword(String userId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select c.value from tig_users u " + " inner join tig_user_credentials c on c.uid = u.uid " +
							" where " + " u.user_id = ? " + " and c.mechanism = 'PLAIN' " +
							" and c.username = 'default'");

			ps.setString(1, userId.toLowerCase());
			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigGetUserDBUid(String userId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("select uid from tig_users where lower(user_id) = ?");

			ps.setString(1, userId.toLowerCase());
			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigInitdb() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("update tig_users set online_status = 0");

			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigOfflineUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where online_status = 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigOnlineUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where online_status > 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigPutDBProperty(final String key, final String value) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("procedure tigPutDBProperty('" + key + "', '" + value + "') called");
			}

			int result;

			if (tigGetDBProperty(key) != null) {
				PreparedStatement ps = conn.prepareStatement(
						"update tig_pairs set tig_pairs.pval = ? where (pkey = ?) and uid = (select uid from tig_users where tig_users.user_id = 'db-properties')");

				ps.setString(1, value);
				ps.setString(2, key);
				result = ps.executeUpdate();
			} else {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(
						"select uid from tig_users where lower(user_id) = lower('db-properties')");
				if (!rs.next()) {
					rs.close();
					tigAddUser("db-properties", null, new ResultSet[1]);
				} else {
					rs.close();
				}

				PreparedStatement ps = conn.prepareStatement(
						"insert into tig_pairs (pkey, pval, uid, nid) select ?, ?, tu.uid, tn.nid from tig_users tu left join tig_nodes tn on tn.uid=tu.uid where (user_id = 'db-properties' and tn.node='root' ) ");

				ps.setString(1, key);
				Clob c = conn.createClob();
				c.setString(1, value);
				ps.setClob(2, c);
//				ps.setString(2, value);
				result = ps.executeUpdate();
			}

			if (result != 1) {
				log.severe("Error on put properties");
			}
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigRemoveUser(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps3 = conn.prepareStatement("select uid from tig_users where lower(user_id) = ?");

			ps3.setString(1, userId.toLowerCase());

			ResultSet rs = ps3.executeQuery();

			if (!rs.next()) {
				return;
			}

			long uid = rs.getLong(1);
			PreparedStatement ps1 = conn.prepareStatement("delete from tig_pairs where uid = ?");

			ps1.setLong(1, uid);
			ps1.executeUpdate();

			PreparedStatement ps2 = conn.prepareStatement("delete from tig_nodes where uid = ?");

			ps2.setLong(1, uid);
			ps2.executeUpdate();

			PreparedStatement ps4 = conn.prepareStatement("delete from tig_user_credentials where uid = ?");
			ps4.setLong(1, uid);
			ps4.executeUpdate();

			PreparedStatement ps = conn.prepareStatement("delete from tig_users where uid = ?");

			ps.setLong(1, uid);
			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigSetComponentVersion(final String name, final String version) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Setting component: {0} version to: {1}", new Object[]{name, version});
			}

			int result;

			PreparedStatement psComp = conn.prepareStatement(GET_VERSION);
			psComp.setString(1, name.toLowerCase());

			ResultSet rs = psComp.executeQuery();

			if (rs.next()) {
				final String updateSql = "update tig_schema_versions set version = ? where (component = ?)";
				PreparedStatement ps = conn.prepareStatement(updateSql);

				ps.setString(1, version);
				ps.setString(2, name);
				result = ps.executeUpdate();
			} else {
				final String insertSql = "insert into tig_schema_versions (component, version, last_update) VALUES (?, ?, current timestamp) ";
				PreparedStatement ps = conn.prepareStatement(insertSql);

				ps.setString(1, name);
				ps.setString(2, version);
				result = ps.executeUpdate();
			}

			if (result != 1) {
				log.severe("Error on Setting version");
			}
		} catch (SQLException e) {

//			 e.printStackTrace();
//			 log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigUpdateAccountStatus(final String user, final int status) throws SQLException {
		try (Connection conn = DriverManager.getConnection("jdbc:default:connection")) {
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			PreparedStatement ps = conn.prepareStatement(
					"UPDATE tig_users SET account_status = ? WHERE lower(user_id) = ?");
			ps.setInt(1, status);
			ps.setString(2, user);
			ps.executeUpdate();
		}
	}

	public static void tigUpdateLoginTime(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"update tig_users set last_login = current timestamp where lower(user_id) =  ?");

			ps.setString(1, userId.toLowerCase());
			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigUpdatePairs(long nid, long uid, String key, Clob value) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select 1 from tig_pairs where nid = ? and uid = ? and pkey = ?");

			ps.setLong(1, nid);
			ps.setLong(2, uid);
			ps.setString(3, key);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				PreparedStatement ps1 = conn.prepareStatement(
						"update tig_pairs set pval = ? where nid = ? and uid = ? and pkey = ?");

				ps1.setClob(1, value);
				ps1.setLong(2, nid);
				ps1.setLong(3, uid);
				ps1.setString(4, key);

				ps1.executeUpdate();
			} else {
				PreparedStatement ps1 = conn.prepareStatement(
						"insert into tig_pairs (nid, uid, pkey, pval) values (?, ?, ?, ?)");

				ps1.setLong(1, nid);
				ps1.setLong(2, uid);
				ps1.setString(3, key);
				ps1.setClob(4, value);

				ps1.executeUpdate();
			}

		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigUpdatePasswordPlainPw(String userId, String userPw) throws SQLException {
		String passwordEncoding = Optional.ofNullable(tigGetDBProperty("password-encoding")).orElse("PLAIN");
		String encodedPassword = encodePassword(passwordEncoding, userId, userPw);
		tigUserCredentialUpdate(userId, Credentials.DEFAULT_USERNAME, passwordEncoding, encodedPassword);
	}

	public static void tigUpdatePasswordPlainPwRev(String userPw, String userId) throws SQLException {
		tigUpdatePasswordPlainPw(userId, userPw);
	}

	public static void tigUserCredentialRemove(String userId, String username) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("select uid from tig_users where lower(user_id) =  ?");

			ps.setString(1, userId.toLowerCase());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long uid = rs.getLong(1);

				ps = conn.prepareStatement("delete from tig_user_credentials where uid = ? and username = ?");
				ps.setLong(1, uid);
				ps.setString(2, username);
				ps.execute();
			}
		} finally {
			conn.close();
		}
	}

	public static void tigUserCredentialUpdate(String userId, String username, String mechanism, String value)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("select uid from tig_users where lower(user_id) =  ?");

			ps.setString(1, userId.toLowerCase());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long uid = rs.getLong(1);

				ps = conn.prepareStatement(
						"select 1 from tig_user_credentials where uid = ? and username = ? and mechanism = ?");
				ps.setLong(1, uid);
				ps.setString(2, username);
				ps.setString(3, mechanism);
				rs = ps.executeQuery();
				if (rs.next()) {
					ps = conn.prepareStatement(
							"update tig_user_credentials set value = ? where uid = ? and username = ? and mechanism = ?");
					ps.setString(1, value);
					ps.setLong(2, uid);
					ps.setString(3, username);
					ps.setString(4, mechanism);
					ps.executeUpdate();
				} else {
					ps = conn.prepareStatement(
							"insert into tig_user_credentials (uid, username, mechanism, value) values (?,?,?,?)");
					ps.setLong(1, uid);
					ps.setString(2, username);
					ps.setString(3, mechanism);
					ps.setString(4, value);
					ps.execute();
				}
			}
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigUserCredentialsGet(String userId, String username, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"select c.mechanism, c.value, u.account_status from tig_users u " +
							" inner join tig_user_credentials c on c.uid = u.uid " +
							" where lower(u.user_id) = ? and c.username = ?");
			ps.setString(1, userId.toLowerCase());
			ps.setString(2, username);

			data[0] = ps.executeQuery();
		} finally {
			conn.close();
		}
	}

	public static void tigUserLoginPlainPw(String userId, String userPw, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String passwordEncoding = Optional.ofNullable(tigGetDBProperty("password-encoding")).orElse("PLAIN");
			String encodedPassword = encodePassword(passwordEncoding, userId, userPw);
			PreparedStatement ps = conn.prepareStatement(
					"select u.user_id from tig_users u inner join tig_user_credentials c on c.uid = u.uid where (u.account_status > 0) AND ( lower(u.user_id) = ?) " +
							" AND c.username = '" + Credentials.DEFAULT_USERNAME +
							"' AND c.mechanism = ? AND c.value = ?");

			ps.setString(1, userId.toLowerCase());
			ps.setString(2, passwordEncoding);
			ps.setString(3, userPw);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				PreparedStatement x = conn.prepareStatement("values '" + userId + "'");

				data[0] = x.executeQuery();

				PreparedStatement flps = conn.prepareStatement(
						"update tig_users set online_status = online_status + 1, last_login = current timestamp where lower(user_id) =  ?");

				flps.setString(1, userId.toLowerCase());
				flps.executeUpdate();
			} else {
				PreparedStatement x = conn.prepareStatement("values '-'");

				data[0] = x.executeQuery();

				PreparedStatement flps = conn.prepareStatement(
						"update tig_users set failed_logins = failed_logins + 1 where lower(user_id) = ?");

				flps.setString(1, userId.toLowerCase());
				flps.executeUpdate();
			}
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigUserLogout(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement(
					"update tig_users set online_status = online_status - 1, last_logout = current timestamp where lower(user_id) =  ?");

			ps.setString(1, userId.toLowerCase());
			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	public static void tigUserUsernamesGet(String userId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("select distinct c.username from tig_users u " +
																 " inner join tig_user_credentials c on c.uid = u.uid " +
																 " where lower(u.user_id) = ?");
			ps.setString(1, userId.toLowerCase());

			data[0] = ps.executeQuery();
		} finally {
			conn.close();
		}
	}
}

