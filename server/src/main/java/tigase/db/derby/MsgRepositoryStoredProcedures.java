/*
 * MsgRepositoryStoredProcedures.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */

package tigase.db.derby;

import tigase.util.Algorithms;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 22.03.2017.
 */
public class MsgRepositoryStoredProcedures {

	private static final Logger log = Logger.getLogger(StoredProcedures.class.getName());

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static void migrateFromOldSchema() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select 1 from SYS.SYSTABLES where tablename = UPPER('msg_history')");
			boolean hasTable = rs.next();
			rs.close();

			if (!hasTable)
				return;

			stmt.execute("insert into tig_offline_messages (receiver, receiver_sha1, sender, sender_sha1, msg_type, ts, message, expired ) " +
								 "select r.jid, r.jid_sha, s.jid, s.jid_sha, m.msg_type, m.ts, m.message, m.expired " +
								 "from msg_history m " +
								 "inner join user_jid r on r.jid_id = m.receiver_uid " +
								 "left join user_jid s on s.jid_id = m.sender_uio");

			stmt.execute("drop table user_jid");
			stmt.execute("drop table msg_history");
		} catch (SQLException e) {
			log.log(Level.WARNING, "Migration of data failed", e);
			// e.printStackTrace();
			// log.log(Level.SEVERE, "SP error", e);
			//throw e;
		} finally {
			conn.close();
		}
	}

	public static void addMessage(String receiver, String sender, Integer type, Timestamp ts, String message, Timestamp expired, Long limit, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			String senderSha1 = Algorithms.hexDigest(sender.toString(), "", "SHA");
			if (limit != 0) {
				PreparedStatement stmt = conn.prepareStatement("select count(1) from tig_offline_messages where receiver_sha1 = ? and sender_sha1 = ?");
				stmt.setString(1, receiverSha1);
				stmt.setString(2, senderSha1);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					long count = rs.getLong(1);
					if (count > limit) {
						rs.close();
						return;
					}
				}
				rs.close();
			}

			PreparedStatement stmt = conn.prepareStatement("insert into tig_offline_messages (receiver, receiver_sha1, sender, sender_sha1, msg_type, ts, message, expired ) " +
																   "values (?, ?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, receiver.toString());
			stmt.setString(2, receiverSha1);
			stmt.setString(3, sender.toString());
			stmt.setString(4, senderSha1);
			stmt.setInt(5, type.intValue());
			stmt.setTimestamp(6, ts);
			stmt.setString(7, message);
			if (expired == null) {
				stmt.setNull(8, Types.TIMESTAMP);
			} else {
				stmt.setTimestamp(8, expired);
			}

			stmt.executeUpdate();
			data[0] = stmt.getGeneratedKeys();
			
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void getMessages(String receiver, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement(
					"select om.message, om.msg_id" + " from tig_offline_messages om" + " where om.receiver_sha1 = ?");
			stmt.setString(1, receiverSha1);

			data[0] = stmt.executeQuery();
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void getMessagesByIds(String receiver, String msgId1, String msgId2, String msgId3, String msgId4, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement("select om.message, om.msg_id" +
														   " from tig_offline_messages om" +
																   " where om.receiver_sha1 = ?" +
																   " and (" +
																   " (? is not null and om.msg_id = ?)\n" +
																   " or (? is not null and om.msg_id = ?)" +
																   " or (? is not null and om.msg_id = ?)" +
																   " or (? is not null and om.msg_id = ?)" +
																   " )");
			stmt.setString(1, receiverSha1);
			stmt.setString(2, msgId1);
			stmt.setString(3, msgId1);
			stmt.setString(4, msgId2);
			stmt.setString(5, msgId2);
			stmt.setString(6, msgId3);
			stmt.setString(7, msgId3);
			stmt.setString(8, msgId4);
			stmt.setString(9, msgId4);

			data[0] = stmt.executeQuery();
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void getMessagesCount(String receiver, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement("select om.msg_type, count(om.msg_type)" +
																   " from tig_offline_messages om" +
																   " where om.receiver_sha1 = ?" +
																   " group by om.msg_type");
			stmt.setString(1, receiverSha1);

			data[0] = stmt.executeQuery();
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void listMessages(String receiver, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement("select om.msg_id, om.msg_type, om.sender" +
																   " from tig_offline_messages om" +
																   " where om.receiver_sha1 = ?");
			stmt.setString(1, receiverSha1);

			data[0] = stmt.executeQuery();
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void deleteMessages(String receiver, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement("delete from tig_offline_messages where receiver_sha1 = ?");
			stmt.setString(1, receiverSha1);

			int affectedRows = stmt.executeUpdate();

			Statement stmt1 = conn.createStatement();
			data[0] = stmt1.executeQuery("select " + affectedRows + " from sysibm.sysdummy1");
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void deleteMessagesByIds(String receiver, String msgId1, String msgId2, String msgId3, String msgId4, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			String receiverSha1 = Algorithms.hexDigest(receiver.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement("delete from tig_offline_messages" +
																   " where receiver_sha1 = ?" +
																   " and (" +
																   " (? is not null and msg_id = ?)\n" +
																   " or (? is not null and msg_id = ?)" +
																   " or (? is not null and msg_id = ?)" +
																   " or (? is not null and msg_id = ?)" +
																   " )");
			stmt.setString(1, receiverSha1);
			stmt.setString(2, msgId1);
			stmt.setString(3, msgId1);
			stmt.setString(4, msgId2);
			stmt.setString(5, msgId2);
			stmt.setString(6, msgId3);
			stmt.setString(7, msgId3);
			stmt.setString(8, msgId4);
			stmt.setString(9, msgId4);


			int affectedRows = stmt.executeUpdate();

			Statement stmt1 = conn.createStatement();
			data[0] = stmt1.executeQuery("select " + affectedRows + " from sysibm.sysdummy1");
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			conn.close();
		}
	}

	public static void deleteMessage(Long msgId)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement("delete from tig_offline_messages where msg_id = ?");
			stmt.setLong(1, msgId);

			int affectedRows = stmt.executeUpdate();
		} finally {
			conn.close();
		}
	}

	public static void getExpiredMessages(int limit, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement("select om.msg_id, om.expired, om.message" +
																   " from tig_offline_messages om" +
																   " where om.expired is not null" +
																   " order by om.expired asc");
			stmt.setMaxRows(limit);

			data[0] = stmt.executeQuery();
		} finally {
			conn.close();
		}
	}

	public static void getExpiredMessagesBefore(Timestamp before, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement("select om.msg_id, om.expired, om.message" +
																   " from tig_offline_messages om" +
																   " where om.expired is not null and (? is null or om.expired <= ?)" +
																   " order by om.expired asc");
			if (before == null) {
				stmt.setNull(1, Types.TIMESTAMP);
				stmt.setNull(2, Types.TIMESTAMP);
			} else {
				stmt.setTimestamp(1, before);
				stmt.setTimestamp(2, before);
			}
			data[0] = stmt.executeQuery();
		} finally {
			conn.close();
		}
	}


}
