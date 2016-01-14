package tigase.db.derby;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.Algorithms;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.security.MessageDigest;

import java.sql.*;

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Class description
 *
 *
 * @version        5.1.0, 2010.09.11 at 02:08:58 BST
 * @author         Artur Hefczyc
 */
public class StoredProcedures {
	private static final Logger log = Logger.getLogger(StoredProcedures.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigActiveAccounts(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where account_status > 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param parentNid
	 * @param uid
	 * @param node
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigAddNode(long parentNid, long uid, String node, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("insert into tig_nodes (parent_nid, uid, node) values (?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);

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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param userPw
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigAddUser(String userId, String userPw, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);


		try {
			// check whether user exists
			PreparedStatement ps_check = conn.prepareStatement("select uid from tig_users where lower(user_id)=?");
			ps_check.setString(1, userId.toLowerCase());
			ResultSet rs_check = ps_check.executeQuery();
			if (rs_check.next()) {
				return;
			}
			PreparedStatement ps =
				conn.prepareStatement("insert into tig_users (user_id, user_pw) values (?, ?)",
					Statement.RETURN_GENERATED_KEYS);

			ps.setString(1, userId);
			ps.setString(2, userPw);
			ps.executeUpdate();

			ResultSet rs = ps.getGeneratedKeys();

			rs.next();

			long generatedKey = rs.getLong(1);
			PreparedStatement ps3 = conn.prepareStatement("select uid from tig_users where uid=?");

			ps3.setLong(1, generatedKey);
			data[0] = ps3.executeQuery();

			PreparedStatement ps2 =
				conn.prepareStatement("insert into tig_nodes (parent_nid, uid, node) values (NULL, ?, 'root')");

			ps2.setLong(1, generatedKey);
			ps2.executeUpdate();

			if (null == userPw) {
				PreparedStatement ps4 =
						conn.prepareStatement("update tig_users set account_status = -1 where uid = ?");

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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param userPw
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigAddUserPlainPw(String userId, String userPw, ResultSet[] data)
			throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);

		tigAddUser(userId, encp, data);
	}

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigAllUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws SQLException
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 *
	 * @throws SQLException
	 */
	public static void tigDisableAccount(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("update tig_users set account_status = 0 where lower(user_id) = ?");

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

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigDisabledAccounts(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where account_status = 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 *
	 * @throws SQLException
	 */
	public static void tigEnableAccount(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("update tig_users set account_status = 1 where lower(user_id) = ?");

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

	/**
	 * Method description
	 *
	 *
	 * @param key
	 *
	 * 
	 *
	 * @throws SQLException
	 */
	public static String tigGetDBProperty(final String key) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String result = null;

			if (log.isLoggable(Level.FINEST)) {
				log.finest("function tigGetDBProperty('" + key + "') called");
			}

			PreparedStatement ps =
				conn.prepareStatement("select pval from tig_pairs, tig_users where (pkey = ?) AND (user_id = 'db-properties') AND (tig_pairs.uid = tig_users.uid)");
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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigGetPassword(String userId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_pw from tig_users where lower(user_id) = ?");

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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param data
	 *
	 * @throws SQLException
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @throws SQLException
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigOfflineUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where online_status = 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigOnlineUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where online_status > 0");

			data[0] = ps.executeQuery();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param value
	 *
	 * @throws SQLException
	 */
	public static void tigPutDBProperty(final String key, final String value) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("procedure tigPutDBProperty('" + key + "', '" + value + "') called");
			}

			int result;

			if (tigGetDBProperty(key) != null) {
				PreparedStatement ps =
					conn.prepareStatement("update tig_pairs set tig_pairs.pval = ? where (pkey = ?) and uid = (select uid from tig_users where tig_users.user_id = 'db-properties')");

				ps.setString(1, value);
				ps.setString(2, key);
				result = ps.executeUpdate();
			} else {
				PreparedStatement ps =
					conn.prepareStatement("insert into tig_pairs (pkey, pval, uid, nid) select ?, ?, tu.uid, tn.nid from tig_users tu left join tig_nodes tn on tn.uid=tu.uid where (user_id = 'db-properties' and tn.node='root' ) ");

				ps.setString(1, key);
				Clob c = conn.createClob();
				c.setString( 1, value);
				ps.setClob( 2, c);
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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 *
	 * @throws SQLException
	 */
	public static void tigRemoveUser(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps3 = conn.prepareStatement("select uid from tig_users where lower(user_id) = ?");

			ps3.setString(1, userId.toLowerCase());

			ResultSet rs = ps3.executeQuery();

			rs.next();

			long uid = rs.getLong(1);
			PreparedStatement ps1 = conn.prepareStatement("delete from tig_pairs where uid = ?");

			ps1.setLong(1, uid);
			ps1.executeUpdate();

			PreparedStatement ps2 = conn.prepareStatement("delete from tig_nodes where uid = ?");

			ps2.setLong(1, uid);
			ps2.executeUpdate();

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

        /**
         *
         * @param nid
         * @param uid
         * @param key
         * @param value
         * @throws SQLException
         */
        public static void tigUpdatePairs(long nid, long uid, String key, Clob value) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps = conn.prepareStatement("select 1 from tig_pairs where nid = ? and uid = ? and pkey = ?");

                        ps.setLong(1, nid);
                        ps.setLong(2, uid);
                        ps.setString(3, key);

                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                                PreparedStatement ps1 = conn.prepareStatement("update tig_pairs set pval = ? where nid = ? and uid = ? and pkey = ?");

                                ps1.setClob(1, value);
                                ps1.setLong(2, nid);
                                ps1.setLong(3, uid);
                                ps1.setString(4, key);

                                ps1.executeUpdate();
                        }
                        else {
                                PreparedStatement ps1 = conn.prepareStatement("insert into tig_pairs (nid, uid, pkey, pval) values (?, ?, ?, ?)");

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


	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param userPw
	 *
	 * @throws SQLException
	 */
	public static void tigUpdatePassword(String userId, String userPw) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("update tig_users set user_pw = ? where lower(user_id) = ?");

			ps.setString(1, userPw);
			ps.setString(2, userId.toLowerCase());
			ps.executeUpdate();
		} catch (SQLException e) {

			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			throw e;
		} finally {
			conn.close();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param userPw
	 *
	 * @throws SQLException
	 */
	public static void tigUpdatePasswordPlainPw(String userId, String userPw) throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);

		tigUpdatePassword(userId, encp);
	}

	/**
	 * Method description
	 *
	 *
	 * @param userPw
	 * @param userId
	 *
	 * @throws SQLException
	 */
	public static void tigUpdatePasswordPlainPwRev(String userPw, String userId) throws SQLException {
		tigUpdatePasswordPlainPw(userId, userPw);
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param userPw
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigUserLogin(String userId, String userPw, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("select user_id from tig_users where (account_status > 0) AND ( lower(user_id) = ?) AND (user_pw = ?)");

			ps.setString(1, userId.toLowerCase());
			ps.setString(2, userPw);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				PreparedStatement x = conn.prepareStatement("values '" + userId + "'");

				data[0] = x.executeQuery();

				PreparedStatement flps =
					conn.prepareStatement("update tig_users set online_status = online_status + 1, last_login = current timestamp where lower(user_id) =  ?");

				flps.setString(1, userId.toLowerCase());
				flps.executeUpdate();
			} else {
				PreparedStatement x = conn.prepareStatement("values '-'");

				data[0] = x.executeQuery();

				PreparedStatement flps =
					conn.prepareStatement("update tig_users set failed_logins = failed_logins + 1 where lower(user_id) = ?");

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

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param userPw
	 * @param data
	 *
	 * @throws SQLException
	 */
	public static void tigUserLoginPlainPw(String userId, String userPw, ResultSet[] data)
			throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);

		tigUserLogin(userId, encp, data);
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 *
	 * @throws SQLException
	 */
	public static void tigUserLogout(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement ps =
				conn.prepareStatement("update tig_users set online_status = online_status - 1, last_logout = current timestamp where lower(user_id) =  ?");

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
				md.update(data.getBytes());
			}

			byte[] digest = md.digest();

			return Algorithms.bytesToHex(digest);
		} catch (Exception e) {
			throw new RuntimeException("Error on encoding password", e);
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
