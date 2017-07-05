/*
 * MsgBroadcastRepositoryStoredProcedures.java
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

import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * Created by andrzej on 24.03.2017.
 */
public class MsgBroadcastRepositoryStoredProcedures {

	public static void addMessage(String msgId, Timestamp expired, String msg)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		ResultSet rs = null;
		try {
			PreparedStatement stmt = conn.prepareStatement("select id from tig_broadcast_messages where id = ?");
			stmt.setString(1, msgId);
			rs = stmt.executeQuery();

			if (rs.next()) {
				return;
			}
			rs.close();

			stmt = conn.prepareStatement("insert into tig_broadcast_messages (id, expired, msg) values (?,?,?)");
			stmt.setString(1, msgId);
			stmt.setTimestamp(2, expired);
			stmt.setString(3, msg);
			stmt.executeUpdate();
		} finally {
			if (rs != null) {
				rs.close();
			}
			conn.close();
		}
	}

	public static void addMessageRecipient(String msgId, String jid)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		ResultSet rs = null;
		try {
			String jidSha1 = Algorithms.hexDigest(jid.toString(), "", "SHA");
			PreparedStatement stmt = conn.prepareStatement("select jid_id from tig_broadcast_jids where jid_sha1 = ?");
			stmt.setString(1, jidSha1);
			rs = stmt.executeQuery();
			long jidId = -1;
			if (rs.next()) {
				jidId = rs.getLong(1);
			}
			rs.close();

			if (jidId < 0) {
				stmt = conn.prepareStatement("insert into tig_broadcast_jids (jid, jid_sha1) values (?,?)");
				stmt.setString(1, jid);
				stmt.setString(2, jidSha1);
				stmt.executeUpdate();
				rs = stmt.getGeneratedKeys();
				if (rs.next()) {
					jidId = rs.getLong(1);
				}
				rs.close();
			}


			stmt = conn.prepareStatement("select 1 from tig_broadcast_recipients where 	msg_id = ? and jid_id = ?");
			stmt.setString(1, msgId);
			stmt.setLong(2, jidId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				return;
			}
			rs.close();

			stmt = conn.prepareStatement("insert into tig_broadcast_recipients (msg_id, jid_id) values (?,?)");
			stmt.setString(1, msgId);
			stmt.setLong(2, jidId);

			stmt.executeUpdate();
		} catch (NoSuchAlgorithmException e) {
			throw new SQLException(e);
		} finally {
			if (rs != null) {
				rs.close();
			}
			conn.close();
		}
	}

	public static void getMessages(Timestamp expired, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"select id, expired, msg from tig_broadcast_messages where expired >= ?");
			stmt.setTimestamp(1, expired);
			data[0] = stmt.executeQuery();
		} finally {
			conn.close();
		}
	}

	public static void getMessageRecipients(String msgId, ResultSet[] data)
			throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"select j.jid from tig_broadcast_recipients r inner join tig_broadcast_jids j on j.jid_id = r.jid_id where r.msg_id = ?");
			stmt.setString(1, msgId);
			data[0] = stmt.executeQuery();
		} finally {
			conn.close();
		}
	}

}
