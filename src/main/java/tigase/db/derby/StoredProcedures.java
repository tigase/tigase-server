package tigase.db.derby;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import tigase.util.Algorithms;

public class StoredProcedures {

	private static final Logger log = Logger.getLogger(StoredProcedures.class.getName());

	private static String encodePassword(String encMethod, String userId, String userPw) {
		if (encMethod != null && "MD5-PASSWORD".equals(encMethod)) {
			return md5(userPw);
		} else if (encMethod != null && "MD5-USERID-PASSWORD".equals(encMethod)) {
			return md5(userId + '\0' + userPw);
		} else {
			return userPw;
		}
	}

	private static String md5(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			if (data != null)
				md.update(data.getBytes());
			byte[] digest = md.digest();
			return Algorithms.bytesToHex(digest);
		} catch (Exception e) {
			throw new RuntimeException("Error on encoding password", e);
		}
	}

	public static void tigActiveAccounts(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where account_status > 0");
		data[0] = ps.executeQuery();
	}

	public static void TigAddNode(Long parentNid, Long uid, String node, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("insert into tig_nodes (parent_nid, uid, node) values (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
		ps.setLong(1, parentNid);
		ps.setLong(2, uid);
		ps.setString(3, node);

		ps.executeUpdate();
		data[0] = ps.getGeneratedKeys();
	}

	public static void tigAddUser(String userId, String userPw, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("insert into tig_users (user_id, user_pw) values (?, ?)",
				Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, userId);
		ps.setString(2, userPw);
		ps.executeUpdate();

		ResultSet rs = ps.getGeneratedKeys();
		data[0] = rs;
		rs.next();
		long generatedKey = rs.getLong(1);

		PreparedStatement ps2 = conn.prepareStatement("insert into tig_nodes (parent_nid, uid, node) values (NULL, ?, 'root')");
		ps2.setLong(1, generatedKey);
		ps2.executeUpdate();
	}

	public static void tigAddUserPlainPw(String userId, String userPw, ResultSet[] data) throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);
		tigAddUser(userId, encp, data);
	}

	public static void tigAllUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users");
		data[0] = ps.executeQuery();
	}

	public static void tigAllUsersCount(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select count(*) into res_cnt from tig_users");
		data[0] = ps.executeQuery();
	}

	public static void tigDisableAccount(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("update tig_users set account_status = 0 where user_id = ?");
		ps.setString(1, userId);
		ps.executeUpdate();
	}

	public static void tigDisabledAccounts(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where account_status = 0");
		data[0] = ps.executeQuery();
	}

	public static void tigEnableAccount(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("update tig_users set account_status = 1 where user_id = ?");
		ps.setString(1, userId);
		ps.executeUpdate();
	}

	public static String tigGetDBProperty(final String key) throws SQLException {
		String result = null;
		log.finest("function tigGetDBProperty('" + key + "') called");
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		PreparedStatement ps = conn.prepareStatement("select pval from tig_pairs, tig_users where (pkey = ?) AND (user_id = 'db-properties') AND (tig_pairs.uid = tig_users.uid)");
		ResultSet rs;
		ps.setString(1, key);
		rs = ps.executeQuery();

		if (rs.next()) {
			result = rs.getString(1);
		}

		return result;
	}

	public static void tigGetPassword(String userId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_pw from tig_users where user_id = ?");
		ps.setString(1, userId);
		data[0] = ps.executeQuery();
	}

	public static void tigGetUserDBUid(String userId, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select uid from tig_users where user_id = ?");
		ps.setString(1, userId);
		data[0] = ps.executeQuery();
	}

	public static void tigInitdb() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("update tig_users set online_status = 0");
		ps.executeUpdate();
	}

	public static void tigOfflineUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where online_status = 0");
		data[0] = ps.executeQuery();
	}

	public static void tigOnlineUsers(ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_id, last_login, last_logout, online_status, failed_logins, account_status from tig_users where online_status > 0");
		data[0] = ps.executeQuery();
	}

	public static void tigPutDBProperty(final String key, final String value) throws SQLException {
		log.finest("procedure tigPutDBProperty('" + key + "', '" + value + "') called");
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		int result;
		if (tigGetDBProperty(key) != null) {
			PreparedStatement ps = conn.prepareStatement("update tig_pairs set tig_pairs.pval = ? where (pkey = ?) and uid = (select uid from tig_users where tig_users.user_id = 'db-properties')");
			ps.setString(1, value);
			ps.setString(2, key);
			result = ps.executeUpdate();
		} else {
			PreparedStatement ps = conn.prepareStatement("insert into tig_pairs (pkey, pval, uid) select ?, ?, uid from tig_users where (user_id = 'db-properties')");
			ps.setString(1, key);
			ps.setString(2, value);
			result = ps.executeUpdate();
		}
		if (result != 1) {
			log.severe("Error on put properties");
		}
	}

	public static void tigRemoveUser(final String userId) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("delete from tig_users where user_id = ?");
		ps.setString(1, userId);
		ps.executeUpdate();
	}

	public static void tigUpdatePassword(String userId, String userPw) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("update tig_users set user_pw = ? where user_id = ?");
		ps.setString(1, userPw);
		ps.setString(2, userId);
		ps.executeUpdate();
	}

	public static void tigUpdatePasswordPlainPw(String userId, String userPw) throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);
		tigUpdatePassword(userId, encp);
	}

	public static void tigUserLogin(String userId, String userPw, ResultSet[] data) throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		PreparedStatement ps = conn.prepareStatement("select user_id from tig_users where (account_status > 0) AND (user_id = ?) AND (user_pw = ?))");
		ps.setString(1, userId);
		ps.setString(2, userPw);
		ResultSet rs = ps.executeQuery();
		data[0] = rs;
		if (rs.next()) {
			PreparedStatement flps = conn.prepareStatement("update tig_users set online_status = online_status + 1, last_login = current timestamp where user_id =  ?");
			flps.setString(1, userId);
			flps.executeUpdate();
		} else {
			PreparedStatement flps = conn.prepareStatement("update tig_users set failed_logins = failed_logins + 1 where user_id = ?");
			flps.setString(1, userId);
			flps.executeUpdate();
		}
	}

	public static void tigUserLoginPlainPw(String userId, String userPw, ResultSet[] data) throws SQLException {
		String encMethod = tigGetDBProperty("password-encoding");
		String encp = encodePassword(encMethod, userId, userPw);
		tigUserLogin(userId, encp, data);
	}
}
