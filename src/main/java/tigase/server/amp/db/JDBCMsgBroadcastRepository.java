/*
 * JDBCMsgBroadcastRepository.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.server.amp.db;

import tigase.db.DataRepository;
import tigase.db.UserNotFoundException;
import tigase.util.Algorithms;
import tigase.util.SimpleCache;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xmpp.BareJID;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.amp.db.JDBCMsgRepository.*;

/**
 * Created by andrzej on 15.03.2016.
 */
public class JDBCMsgBroadcastRepository extends MsgBroadcastRepository<Long,DataRepository> {

	private static final Logger log = Logger.getLogger(JDBCMsgBroadcastRepository.class.getCanonicalName());

	private static final String MYSQL_CREATE_BROADCAST_MSGS_TABLE =
			"create table broadcast_msgs ( " + "  "
					+ "id varchar(128) NOT NULL,  "
					+ "expired datetime NOT NULL,  "
					+ "msg varchar(4096) NOT NULL, "
					+ " primary key (id))";
	private static final String PGSQL_CREATE_BROADCAST_MSGS_TABLE =
			"create table broadcast_msgs ( " + "  "
					+ "id varchar(128) NOT NULL,  "
					+ "expired timestamp NOT NULL,  "
					+ "msg varchar(4096) NOT NULL, "
					+ " primary key (id))";
	private static final String SQLSERVER_CREATE_BROADCAST_MSGS_TABLE =
			"create table broadcast_msgs ( " + "  "
					+ "id varchar(128) NOT NULL,  "
					+ "expired datetime NOT NULL,  "
					+ "msg nvarchar(4000) NOT NULL, "
					+ " primary key (id))";
	private static final String DERBY_CREATE_BROADCAST_MSGS_TABLE =
			"create table broadcast_msgs ( " + "  "
					+ "id varchar(128) NOT NULL,  "
					+ "expired timestamp NOT NULL,  "
					+ "msg varchar(4096) NOT NULL, "
					+ " primary key (id))";

	private static final String MYSQL_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE =
			"create table broadcast_msgs_recipients ( " + "  "
					+ "msg_id varchar(128) NOT NULL,  "
					+ "jid_id bigint unsigned NOT NULL,  "
					+ " primary key (msg_id, jid_id))";
	private static final String PGSQL_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE =
			"create table broadcast_msgs_recipients ( " + "  "
					+ "msg_id varchar(128) NOT NULL,  "
					+ "jid_id bigint NOT NULL,  "
					+ " primary key (msg_id, jid_id))";
	private static final String SQLSERVER_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE =
			"create table broadcast_msgs_recipients ( " + "  "
					+ "msg_id varchar(128) NOT NULL,  "
					+ "jid_id bigint NOT NULL,  "
					+ " primary key (msg_id, jid_id))";
	private static final String DERBY_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE =
			"create table broadcast_msgs_recipients ( " + "  "
					+ "msg_id varchar(128) NOT NULL,  "
					+ "jid_id bigint NOT NULL,  "
					+ " primary key (msg_id, jid_id))";

	private static final String MSG_SELECT_MESSAGES_TO_BROADCAST =
			"select id, expired, msg from broadcast_msgs where expired >= ?";
	private static final String SQLSERVER_MSG_INSERT_MESSAGE_TO_BROADCAST =
			"insert into broadcast_msgs (id, expired, msg) values (?, ?, ?) where not exists (select 1 from broadcast_msgs where id = ?)";
	private static final String SQL_MSG_INSERT_MESSAGE_TO_BROADCAST =
			"insert into broadcast_msgs (id, expired, msg) select ?, ?, ? from (select 1) x where not exists (select 1 from broadcast_msgs where id = ?)";
	private static final String DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST1 =
			"select id from broadcast_msgs where id = ?";
	private static final String DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST2 =
			"insert into broadcast_msgs (id, expired, msg) values (?,?,?)";
	private static final String MSG_SELECT_BROADCAST_RECIPIENTS =
			"select j." + JID_COLUMN + " from broadcast_msgs_recipients r join " + JID_TABLE + " j on j." + JID_ID_COLUMN + " = r.jid_id where r.msg_id = ?";
	private static final String SQLSERVER_MSG_ENSURE_BROADCAT_RECIPIETN =
			"insert into broadcast_msgs_recipients (msg_id, jid_id) values (?, ?) where not exists (select 1 from broadcast_msgs_recipients where msg_id = ? and jid_id = ?)";
	private static final String SQL_MSG_ENSURE_BROADCAT_RECIPIETN =
			"insert into broadcast_msgs_recipients (msg_id, jid_id) select ?, ? from (select 1) x where not exists (select 1 from broadcast_msgs_recipients where msg_id = ? and jid_id = ?)";
	private static final String DERBY_MSG_ENSURE_BROADCAT_RECIPIETN1 =
			"select 1 from broadcast_msgs_recipients where msg_id = ? and jid_id = ?";
	private static final String DERBY_MSG_ENSURE_BROADCAT_RECIPIETN2 =
			"insert into broadcast_msgs_recipients (msg_id, jid_id) values (?, ?)";

	private DataRepository data_repo = null;
	private String uid_query = GET_USER_UID_DEF_QUERY;

	private String msg_insert_message_to_broadcast = SQL_MSG_INSERT_MESSAGE_TO_BROADCAST;
	private String msg_ensure_broadcast_recipient = SQL_MSG_ENSURE_BROADCAT_RECIPIETN;
	private String add_user_jid_id = ADD_USER_JID_ID_QUERY;

	private Map<BareJID, Long> uids_cache = Collections
			.synchronizedMap(new SimpleCache<BareJID, Long>(MAX_UID_CACHE_SIZE,
					MAX_UID_CACHE_TIME));

	@Override
	public void setDataSource(DataRepository data_repo) {
		try {
			switch (data_repo.getDatabaseType()) {
				case sqlserver:
					msg_ensure_broadcast_recipient = SQLSERVER_MSG_ENSURE_BROADCAT_RECIPIETN;
					msg_insert_message_to_broadcast = SQLSERVER_MSG_INSERT_MESSAGE_TO_BROADCAST;
					break;
				default:
					msg_ensure_broadcast_recipient = SQL_MSG_ENSURE_BROADCAT_RECIPIETN;
					msg_insert_message_to_broadcast = SQL_MSG_INSERT_MESSAGE_TO_BROADCAST;
					break;
			}
			switch (data_repo.getDatabaseType()) {
				case mysql:
					add_user_jid_id = ADD_USER_JID_ID_QUERY_MYSQL;
					break;
				case derby:
					add_user_jid_id = ADD_USER_JID_ID_QUERY_DERBY;
					break;
				default:
					add_user_jid_id = ADD_USER_JID_ID_QUERY;
					break;
			}

			// Check if DB is correctly setup and contains all required tables.
			checkDB(data_repo);
			data_repo.initPreparedStatement(uid_query, uid_query);
			data_repo.initPreparedStatement(add_user_jid_id, add_user_jid_id);
			data_repo.initPreparedStatement(MSG_SELECT_BROADCAST_RECIPIENTS, MSG_SELECT_BROADCAST_RECIPIENTS);
			data_repo.initPreparedStatement(MSG_SELECT_MESSAGES_TO_BROADCAST, MSG_SELECT_MESSAGES_TO_BROADCAST);
			if (data_repo.getDatabaseType() == DataRepository.dbTypes.derby) {
				data_repo.initPreparedStatement(DERBY_MSG_ENSURE_BROADCAT_RECIPIETN1, DERBY_MSG_ENSURE_BROADCAT_RECIPIETN1);
				data_repo.initPreparedStatement(DERBY_MSG_ENSURE_BROADCAT_RECIPIETN2, DERBY_MSG_ENSURE_BROADCAT_RECIPIETN2);
				data_repo.initPreparedStatement(DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST1, DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST1);
				data_repo.initPreparedStatement(DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST2, DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST2);
			} else {
				data_repo.initPreparedStatement(msg_ensure_broadcast_recipient, msg_ensure_broadcast_recipient);
				data_repo.initPreparedStatement(msg_insert_message_to_broadcast, msg_insert_message_to_broadcast);
			}
		} catch (SQLException ex) {
			log.log(Level.WARNING, "MsgRepository not initialized due to exception", ex);
		}

		this.data_repo = data_repo;
	}

	/**
	 * Performs database check, creates missing schema if necessary
	 *
	 * @throws SQLException
	 */
	private void checkDB(DataRepository data_repo) throws SQLException {

		Statement stmt = null;

		try {
			DataRepository.dbTypes databaseType = data_repo.getDatabaseType();
			switch ( databaseType ) {
				case mysql:
					data_repo.checkTable( "broadcast_msgs", MYSQL_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", MYSQL_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
				case postgresql:
					data_repo.checkTable( "broadcast_msgs", PGSQL_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", PGSQL_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
				case derby:
					data_repo.checkTable( "broadcast_msgs", DERBY_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", DERBY_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
				case jtds:
				case sqlserver:
					data_repo.checkTable( "broadcast_msgs", SQLSERVER_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", SQLSERVER_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
			}
		} finally {
			data_repo.release( stmt, null );
		}

	}


	@Override
	public void loadMessagesToBroadcast() {
		try {
			Set<String> oldMessages = new HashSet<String>(broadcastMessages.keySet());

			ResultSet rs = null;
			PreparedStatement stmt = data_repo.getPreparedStatement(null, MSG_SELECT_MESSAGES_TO_BROADCAST);

			synchronized (stmt) {
				try {
					stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
					rs = stmt.executeQuery();

					DomBuilderHandler domHandler = new DomBuilderHandler();
					while (rs.next()) {
						String msgId = rs.getString(1);
						oldMessages.remove(msgId);
						if (broadcastMessages.containsKey(msgId))
							continue;

						Date expire = rs.getTimestamp(2);
						char[] msgChars = rs.getString(3).toCharArray();

						parser.parse(domHandler, msgChars, 0, msgChars.length);

						Queue<Element> elems = domHandler.getParsedElements();
						Element msg = elems.poll();
						if (msg == null)
							continue;

						broadcastMessages.put(msgId, new BroadcastMsg(null, msg, expire));
					}
				} finally {
					data_repo.release(null, rs);
				}
			}

			for (String id : oldMessages) {
				broadcastMessages.remove(id);
			}

			rs = null;

			for (String id : broadcastMessages.keySet()) {
				BroadcastMsg bmsg = broadcastMessages.get(id);
				stmt = data_repo.getPreparedStatement(null, MSG_SELECT_BROADCAST_RECIPIENTS);
				synchronized (stmt) {
					try {
						stmt.setString(1, id);
						rs = stmt.executeQuery();
						while (rs.next()) {
							BareJID jid = BareJID.bareJIDInstanceNS(rs.getString(1));
							bmsg.addRecipient(jid);
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			}
		} catch (SQLException ex) {
			log.log(Level.WARNING, "Problem with retrieving broadcast messages", ex);
		}
	}

	@Override
	protected void insertBroadcastMessage(String id, Element msg, Date expire, BareJID recipient) {
		try {
			if (data_repo.getDatabaseType() == DataRepository.dbTypes.derby) {
				boolean exists = false;
				PreparedStatement stmt = data_repo.getPreparedStatement(recipient, DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST1);
				synchronized (stmt) {
					stmt.setString(1, id);
					ResultSet rs = stmt.executeQuery();
					exists = rs.next();
					data_repo.release(null, rs);
				}
				if (!exists) {
					stmt = data_repo.getPreparedStatement(recipient, DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST2);
					synchronized (stmt) {
						stmt.setString(1, id);
						stmt.setTimestamp(2, new Timestamp(expire.getTime()));
						stmt.setString(3, msg.toString());
						stmt.executeUpdate();
					}
				}
			} else {
				PreparedStatement stmt = data_repo.getPreparedStatement(recipient, msg_insert_message_to_broadcast);
				synchronized (stmt) {
					stmt.setString(1, id);
					stmt.setTimestamp(2, new Timestamp(expire.getTime()));
					stmt.setString(3, msg.toString());
					stmt.setString(4, id);
					stmt.executeUpdate();
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem with updating broadcast message", ex);
		}
	}

	@Override
	protected void ensureBroadcastMessageRecipient(String id, BareJID recipient) {
		try {
			long uid = getUserUID(recipient);
			if (uid == -1) {
				uid = addUserJID(recipient);
			}

			if (data_repo.getDatabaseType() == DataRepository.dbTypes.derby) {
				boolean exists = false;
				PreparedStatement stmt = data_repo.getPreparedStatement(recipient, DERBY_MSG_ENSURE_BROADCAT_RECIPIETN1);
				synchronized (stmt) {
					stmt.setString(1, id);
					stmt.setLong(2, uid);
					ResultSet rs = stmt.executeQuery();
					exists = rs.next();
					data_repo.release(null, rs);
				}
				if (!exists) {
					stmt = data_repo.getPreparedStatement(recipient, DERBY_MSG_ENSURE_BROADCAT_RECIPIETN2);
					synchronized (stmt) {
						stmt.setString(1, id);
						stmt.setLong(2, uid);
						stmt.executeUpdate();
					}
				}
			} else {
				PreparedStatement stmt = data_repo.getPreparedStatement(recipient, msg_ensure_broadcast_recipient);
				synchronized (stmt) {
					stmt.setString(1, id);
					stmt.setLong(2, uid);
					stmt.setString(3, id);
					stmt.setLong(4, uid);
					stmt.executeUpdate();
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem with updating broadcast message", ex);
		}
	}

	private long addUserJID(BareJID bareJID) throws SQLException, UserNotFoundException {
		try {
			String jid_sha = Algorithms.hexDigest(bareJID.toString(), "", "SHA");
			PreparedStatement add_jid_id_st =
					data_repo.getPreparedStatement(bareJID, add_user_jid_id);

			synchronized (add_jid_id_st) {
				add_jid_id_st.setString(1, jid_sha);
				add_jid_id_st.setString(2, bareJID.toString());
				add_jid_id_st.setString( 3, jid_sha );
				add_jid_id_st.setString( 4, bareJID.toString() );
				add_jid_id_st.executeUpdate();
			}

		} catch (NoSuchAlgorithmException ex) {
			log.log(Level.WARNING, "Configuration error or code bug: ", ex);

			return -1;
		}

		return getUserUID(bareJID);
	}

	private long getUserUID(BareJID user_id) throws SQLException, UserNotFoundException {
		Long cache_res = uids_cache.get(user_id);

		if (cache_res != null) {
			return cache_res.longValue();
		} // end of if (result != null)

		long result = -1;
		String jid_sha;

		try {
			jid_sha = Algorithms.hexDigest(user_id.toString(), "", "SHA");
		} catch (NoSuchAlgorithmException ex) {
			log.log(Level.WARNING, "Configuration error or code bug: ", ex);

			return -1;
		}

		ResultSet rs = null;
		PreparedStatement uid_st = data_repo.getPreparedStatement(user_id, uid_query);

		synchronized (uid_st) {
			try {
				uid_st.setString(1, jid_sha);
				rs = uid_st.executeQuery();

				if (rs.next()) {
					BareJID res_jid = BareJID.bareJIDInstanceNS(rs.getString(JID_COLUMN));

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Found entry for JID: {0}, DB JID: {1}", new Object[] {
								user_id, res_jid });
					}

					// There is a slight chance that there is the same SHA for 2 different
					// JIDs.
					// Even though it is impossible to store messages for both JIDs right
					// now
					// we have to make sure we don't send offline messages to incorrect
					// person
					if (user_id.equals(res_jid)) {
						result = rs.getLong(JID_ID_COLUMN);
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST,
									"JIDs don't match, SHA conflict? JID: {0}, DB JID: {1}", new Object[] {
											user_id, res_jid });
						}
					}
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "No entry for JID: {0}", user_id);
					}
				}
			} finally {
				data_repo.release(null, rs);
			}
		}

		// if (result <= 0) {
		// throw new UserNotFoundException("User does not exist: " + user_id);
		// } // end of if (isnext) else

		if (result > 0) {
			uids_cache.put(user_id, result);
		}

		return result;
	}
}
