/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.amp;

//~--- non-JDK imports --------------------------------------------------------

import java.security.NoSuchAlgorithmException;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.db.DataRepository;
import tigase.db.MsgRepositoryIfc;
import tigase.db.RepositoryFactory;
import tigase.db.UserNotFoundException;
import tigase.server.Packet;
import tigase.util.Algorithms;
import tigase.util.SimpleCache;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

//~--- classes ----------------------------------------------------------------

/**
 * Created: May 3, 2010 5:28:02 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MsgRepository implements MsgRepositoryIfc {
	private static final Logger log = Logger.getLogger(MsgRepository.class.getName());
	private static final String MSG_TABLE = "msg_history";
	private static final String MSG_ID_COLUMN = "msg_id";
	private static final String MSG_TIMESTAMP_COLUMN = "ts";
	private static final String MSG_EXPIRED_COLUMN = "expired";
	private static final String MSG_FROM_UID_COLUMN = "sender_uid";
	private static final String MSG_TO_UID_COLUMN = "receiver_uid";
	private static final String MSG_BODY_COLUMN = "message";
	private static final String HISTORY_FLAG_COLUMN = "history_enabled";
	private static final String JID_TABLE = "user_jid";
	private static final String JID_ID_COLUMN = "jid_id";
	private static final String JID_SHA_COLUMN = "jid_sha";
	private static final String JID_COLUMN = "jid";
	/* @formatter:off */
	private static final String CREATE_MSG_TABLE = 
		"create table " + MSG_TABLE + " ( "	+ "  " + 
		  MSG_ID_COLUMN + " serial," + "  " + 
		  MSG_TIMESTAMP_COLUMN + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + "  " + 
		  MSG_EXPIRED_COLUMN + " DATETIME," + "  " + 
		  MSG_FROM_UID_COLUMN + " bigint unsigned," + "  " + 
		  MSG_TO_UID_COLUMN + " bigint unsigned NOT NULL," + "  " + 
		  MSG_BODY_COLUMN + " varchar(4096) NOT NULL," + "  " + 
		  " key (" + MSG_EXPIRED_COLUMN + "), " + 
		  " key (" + MSG_FROM_UID_COLUMN + ", " + MSG_TO_UID_COLUMN + ")," + 
		  " key (" + MSG_TO_UID_COLUMN + ", " + MSG_FROM_UID_COLUMN + "))";
	private static final String CREATE_JID_TABLE = 
		"create table " + JID_TABLE + " ( " + "  " + 
		  JID_ID_COLUMN + " serial," + "  " + 
		  JID_SHA_COLUMN + " char(128) NOT NULL," + "  " + 
		  JID_COLUMN + " varchar(2049) NOT NULL," + "  " + 
		  HISTORY_FLAG_COLUMN + " int default 0," + 
		  " primary key (" + JID_ID_COLUMN + ")," + 
		  " unique key " + JID_SHA_COLUMN + " (" + JID_SHA_COLUMN + ")," + 
		  " key " + JID_COLUMN + " (" + JID_COLUMN + "(765)))";
	private static final String MSG_INSERT_QUERY = 
		"insert into " + MSG_TABLE + " ( " + 
		  MSG_EXPIRED_COLUMN + ", " + 
		  MSG_FROM_UID_COLUMN + ", " + 
		  MSG_TO_UID_COLUMN + ", " + 
		  MSG_BODY_COLUMN + ") values (?, ?, ?, ?)";
	private static final String MSG_SELECT_TO_JID_QUERY = 
		"select * from " + MSG_TABLE + " where " + MSG_TO_UID_COLUMN + " = ?";
	private static final String MSG_DELETE_TO_JID_QUERY = 
		"delete from " + MSG_TABLE + " where " + MSG_TO_UID_COLUMN + " = ?";
	private static final String MSG_DELETE_ID_QUERY = 
		"delete from " + MSG_TABLE + " where " + MSG_ID_COLUMN + " = ?";
	private static final String MSG_SELECT_EXPIRED_QUERY = 
		"select * from " + MSG_TABLE + " where expired is not null order by expired";
	private static final String MSG_SELECT_EXPIRED_BEFORE_QUERY = 
		"select * from " + MSG_TABLE + " where expired is not null and expired <= ? order by expired";
	private static final String GET_USER_UID_DEF_QUERY = 
		"select " + 
		  JID_ID_COLUMN + ", " + 
		  JID_COLUMN + 
		" from " + JID_TABLE + " where " + JID_SHA_COLUMN + " = ?";
        private static final String MSG_COUNT_FOR_TO_AND_FROM_QUERY_DEF =
                "select count(*) from " + MSG_TABLE + " where " + MSG_TO_UID_COLUMN + " = ? and " + MSG_FROM_UID_COLUMN + " = ?";
	private static final String ADD_USER_JID_ID_QUERY = 
		"insert into " + JID_TABLE + " ( " + JID_SHA_COLUMN + ", " + JID_COLUMN + ") values (?, ?)";
	/* @formatter:on */
	private static final String GET_USER_UID_PROP_KEY = "user-uid-query";
        private static final String MSGS_STORE_LIMIT_KEY = "store-limit";
        private static final String MSGS_COUNT_LIMIT_PROP_KEY = "count-limit-query";
        private static final long MSGS_STORE_LIMIT_VAL = 100;
	private static final int MAX_UID_CACHE_SIZE = 100000;
	private static final long MAX_UID_CACHE_TIME = 3600000;
	private static final Map<String, MsgRepository> repos =
			new ConcurrentSkipListMap<String, MsgRepository>();
	private static final int MAX_QUEUE_SIZE = 1000;

	// ~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;
	private long earliestOffline = Long.MAX_VALUE;
	private SimpleParser parser = SingletonFactory.getParserInstance();
	private String uid_query = GET_USER_UID_DEF_QUERY;
        private String msg_count_for_limit_query = MSG_COUNT_FOR_TO_AND_FROM_QUERY_DEF;
        private long msgs_store_limit = MSGS_STORE_LIMIT_VAL;
	private boolean initialized = false;
	private Map<BareJID, Long> uids_cache = Collections
			.synchronizedMap(new SimpleCache<BareJID, Long>(MAX_UID_CACHE_SIZE,
					MAX_UID_CACHE_TIME));
	private DelayQueue<MsgDBItem> expiredQueue = new DelayQueue<MsgDBItem>();

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * @param id_string
	 * @return
	 */
	public static MsgRepository getInstance(String id_string) {
		MsgRepository result = repos.get(id_string);

		if (result == null) {
			result = new MsgRepository();
			repos.put(id_string, result);
		}

		return result;
	}

	/**
	 * Method description
	 * 
	 * @param time
	 * @param delete
	 * @return
	 */
	@Override
	public Element getMessageExpired(long time, boolean delete) {
		if (expiredQueue.size() == 0) {

			// If the queue is empty load it with some elements
			loadExpiredQueue(MAX_QUEUE_SIZE);
		} else {

			// If the queue is not empty, check whether recently saved off-line
			// message
			// is due to expire sooner then the head of the queue.
			MsgDBItem item = expiredQueue.peek();

			if ((item != null) && (earliestOffline < item.expired.getTime())) {

				// There is in fact off-line message due to expire sooner then the head
				// of the
				// queue. Load all off-line message due to expire sooner then the first
				// element
				// in the queue.
				loadExpiredQueue(item.expired);
			}
		}

		MsgDBItem item = null;

		while (item == null) {
			try {
				item = expiredQueue.take();
			} catch (InterruptedException ex) {
			}
		}

		if (delete) {
			deleteMessage(item.db_id);
		}

		return item.msg;
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * @param conn_str
	 * @param map
	 * @throws SQLException
	 */
	public void initRepository(String conn_str, Map<String, String> map)
			throws SQLException {
		if (initialized) {
			return;
		}

		initialized = true;
		log.log(Level.INFO, "Initializing dbAccess for db connection url: {0}", conn_str);
                
		if (map != null) {
			String query = map.get(GET_USER_UID_PROP_KEY);

			if (query != null) {
				uid_query = query;
			}
                             
                        query = map.get(MsgRepository.MSGS_COUNT_LIMIT_PROP_KEY);
                        
                        if (query != null) {
                                msg_count_for_limit_query = query;
                        }
                        
                        String msgs_store_limit_str = map.get(MSGS_STORE_LIMIT_KEY);
                        
                        if (msgs_store_limit_str != null) {
                                msgs_store_limit = Long.parseLong(msgs_store_limit_str);
                        }
		}

		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, map);

			// Check if DB is correctly setup and contains all required tables.
			checkDB();
			data_repo.initPreparedStatement(uid_query, uid_query);
			data_repo.initPreparedStatement(MSG_INSERT_QUERY, MSG_INSERT_QUERY);
			data_repo.initPreparedStatement(MSG_SELECT_TO_JID_QUERY, MSG_SELECT_TO_JID_QUERY);
			data_repo.initPreparedStatement(MSG_DELETE_TO_JID_QUERY, MSG_DELETE_TO_JID_QUERY);
			data_repo.initPreparedStatement(MSG_DELETE_ID_QUERY, MSG_DELETE_ID_QUERY);
			data_repo.initPreparedStatement(MSG_SELECT_EXPIRED_QUERY, MSG_SELECT_EXPIRED_QUERY);
			data_repo.initPreparedStatement(MSG_SELECT_EXPIRED_BEFORE_QUERY,
					MSG_SELECT_EXPIRED_BEFORE_QUERY);
                        data_repo.initPreparedStatement(msg_count_for_limit_query,
                                        msg_count_for_limit_query);
			data_repo.initPreparedStatement(ADD_USER_JID_ID_QUERY, ADD_USER_JID_ID_QUERY);
		} catch (Exception e) {

			// Ignore for now....
		}
	}

	/**
	 * Method description
	 * 
	 * @param to
	 * @param delete
	 * @return
	 * @throws UserNotFoundException
	 */
	@Override
	public Queue<Element> loadMessagesToJID(JID to, boolean delete)
			throws UserNotFoundException {
		Queue<Element> result = null;
		ResultSet rs = null;

		try {
			long to_uid = getUserUID(to.getBareJID());

			if (to_uid < 0) {
				throw new UserNotFoundException("User: " + to + " was not found in database.");
			}

			PreparedStatement select_to_jid_st =
					data_repo.getPreparedStatement(to.getBareJID(), MSG_SELECT_TO_JID_QUERY);

			synchronized (select_to_jid_st) {
				select_to_jid_st.setLong(1, to_uid);
				rs = select_to_jid_st.executeQuery();

				StringBuilder sb = new StringBuilder(1000);

				while (rs.next()) {
					sb.append(rs.getString(MSG_BODY_COLUMN));
				}

				if (sb.length() > 0) {
					DomBuilderHandler domHandler = new DomBuilderHandler();

					parser.parse(domHandler, sb.toString().toCharArray(), 0, sb.length());
					result = domHandler.getParsedElements();
				}
			}

			if (delete) {
				PreparedStatement delete_to_jid_st =
						data_repo.getPreparedStatement(to.getBareJID(), MSG_DELETE_TO_JID_QUERY);

				synchronized (delete_to_jid_st) {
					delete_to_jid_st.setLong(1, to_uid);
					delete_to_jid_st.executeUpdate();
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages for user: " + to, e);
		} finally {
			data_repo.release(null, rs);
		}

		return result;
	}

	/**
	 * Method description
	 * 
	 * @param from
	 * @param to
	 * @param expired
	 * @param msg
	 * @throws UserNotFoundException
	 */
	@Override
	public void storeMessage(JID from, JID to, Date expired, Element msg)
			throws UserNotFoundException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Storring expired: {0} message: {1}", new Object[] { expired,
					Packet.elemToString(msg) });
		}

                ResultSet rs = null;
                try {
			long from_uid = getUserUID(from.getBareJID());

			if (from_uid < 0) {
				from_uid = addUserJID(from.getBareJID());
			}

			long to_uid = getUserUID(to.getBareJID());

			if (to_uid < 0) {
				to_uid = addUserJID(to.getBareJID());
			}

                        long count = 0;
                        PreparedStatement count_msgs_st = 
                                        data_repo.getPreparedStatement(to.getBareJID(), msg_count_for_limit_query);
                        
                        synchronized (count_msgs_st) {
                                count_msgs_st.setLong(1, to_uid);
                                count_msgs_st.setLong(2, from_uid);
                                
                                rs = count_msgs_st.executeQuery();
                                
                                if (rs.next()) {
                                        count = rs.getLong(1);
                                }
                        }
                        
                        if (msgs_store_limit <= count) {
                                if (log.isLoggable(Level.FINEST)) {
                                        log.log(Level.FINEST, "Message store limit ({0}) exceeded for message: {1}",
                                                new Object[]{msgs_store_limit, Packet.elemToString(msg)});
                                }
                                return;
                        }
                        
			PreparedStatement insert_msg_st =
					data_repo.getPreparedStatement(to.getBareJID(), MSG_INSERT_QUERY);

			synchronized (insert_msg_st) {
				if (expired == null) {
					insert_msg_st.setNull(1, Types.TIMESTAMP);
				} else {
					Timestamp time = new Timestamp(expired.getTime());

					insert_msg_st.setTimestamp(1, time);
				}

				if (from_uid <= 0) {
					insert_msg_st.setNull(2, Types.BIGINT);
				} else {
					insert_msg_st.setLong(2, from_uid);
				}

				insert_msg_st.setLong(3, to_uid);
				// TODO: deal with messages bigger than the database can fit....
				insert_msg_st.setString(4, msg.toString());
				insert_msg_st.executeUpdate();
			}

			if (expired != null) {
				if (expired.getTime() < earliestOffline) {
					earliestOffline = expired.getTime();
				}

				if (expiredQueue.size() == 0) {
					loadExpiredQueue(1);
				}
			}
		} catch (DataTruncation dte) {
			log.log(Level.FINE, "Data truncated for message from {0} to {1}", new Object[] {
					from, to });
                        
                        data_repo.release(null, rs);
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	private long addUserJID(BareJID bareJID) throws SQLException, UserNotFoundException {
		try {
			String jid_sha = Algorithms.hexDigest(bareJID.toString(), "", "SHA");
			PreparedStatement add_jid_id_st =
					data_repo.getPreparedStatement(bareJID, ADD_USER_JID_ID_QUERY);

			synchronized (add_jid_id_st) {
				add_jid_id_st.setString(1, jid_sha);
				add_jid_id_st.setString(2, bareJID.toString());
				add_jid_id_st.executeUpdate();
			}

			// // Give it some time or following select won't find a new entry MySQL
			// bug?
			// Thread.sleep(100);
			// }catch (InterruptedException ex) {
			//
			// // Do nothing
		} catch (NoSuchAlgorithmException ex) {
			log.log(Level.WARNING, "Configuration error or code bug: ", ex);

			return -1;
		}

		return getUserUID(bareJID);
	}

	private void checkDB() throws SQLException {
		data_repo.checkTable(JID_TABLE, CREATE_JID_TABLE);
		data_repo.checkTable(MSG_TABLE, CREATE_MSG_TABLE);
	}

	private void deleteMessage(long msg_id) {
		try {
			PreparedStatement delete_id_st =
					data_repo.getPreparedStatement(null, MSG_DELETE_ID_QUERY);

			synchronized (delete_id_st) {
				delete_id_st.setLong(1, msg_id);
				delete_id_st.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem removing entry from DB: ", e);
		}
	}

	// ~--- get methods ----------------------------------------------------------

	private long getUserUID(BareJID user_id) throws SQLException, UserNotFoundException {
		Long cache_res = uids_cache.get(user_id);

		if (cache_res != null) {
			return cache_res.longValue();
		} // end of if (result != null)

		ResultSet rs = null;
		long result = -1;
		String jid_sha;

		try {
			jid_sha = Algorithms.hexDigest(user_id.toString(), "", "SHA");
		} catch (NoSuchAlgorithmException ex) {
			log.log(Level.WARNING, "Configuration error or code bug: ", ex);

			return -1;
		}

		try {
			PreparedStatement uid_st = data_repo.getPreparedStatement(user_id, uid_query);

			synchronized (uid_st) {
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
			}

			// if (result <= 0) {
			// throw new UserNotFoundException("User does not exist: " + user_id);
			// } // end of if (isnext) else
		} finally {
			data_repo.release(null, rs);
		}

		if (result > 0) {
			uids_cache.put(user_id, result);
		}

		return result;
	}

	// ~--- methods --------------------------------------------------------------

	private void loadExpiredQueue(int min_elements) {
		ResultSet rs = null;

		try {
			PreparedStatement select_expired_st =
					data_repo.getPreparedStatement(null, MSG_SELECT_EXPIRED_QUERY);

			synchronized (select_expired_st) {
				rs = select_expired_st.executeQuery();

				DomBuilderHandler domHandler = new DomBuilderHandler();
				int counter = 0;

				while (rs.next()
						&& ((expiredQueue.size() < MAX_QUEUE_SIZE) || (counter++ < min_elements))) {
					String msg_str = rs.getString(MSG_BODY_COLUMN);

					parser.parse(domHandler, msg_str.toCharArray(), 0, msg_str.length());

					Queue<Element> elems = domHandler.getParsedElements();
					Element msg = elems.poll();

					if (msg == null) {
						log.log(Level.INFO,
								"Something wrong, loaded offline message from DB but parsed no "
										+ "XML elements: {0}", msg_str);
					} else {
						Timestamp ts = rs.getTimestamp(MSG_EXPIRED_COLUMN);
						MsgDBItem item = new MsgDBItem(rs.getLong(MSG_ID_COLUMN), msg, ts);

						expiredQueue.offer(item);
					}
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages from db: ", e);
		} finally {
			data_repo.release(null, rs);
		}

		earliestOffline = Long.MAX_VALUE;
	}

	private void loadExpiredQueue(Date expired) {
		ResultSet rs = null;

		try {
			if (expiredQueue.size() > 100 * MAX_QUEUE_SIZE) {
				expiredQueue.clear();
			}

			PreparedStatement select_expired_before_st =
					data_repo.getPreparedStatement(null, MSG_SELECT_EXPIRED_BEFORE_QUERY);

			synchronized (select_expired_before_st) {
				select_expired_before_st.setTimestamp(1, new Timestamp(expired.getTime()));
				rs = select_expired_before_st.executeQuery();

				DomBuilderHandler domHandler = new DomBuilderHandler();
				int counter = 0;

				while (rs.next() && (counter++ < MAX_QUEUE_SIZE)) {
					String msg_str = rs.getString(MSG_BODY_COLUMN);

					parser.parse(domHandler, msg_str.toCharArray(), 0, msg_str.length());

					Queue<Element> elems = domHandler.getParsedElements();
					Element msg = elems.poll();

					if (msg == null) {
						log.log(Level.INFO,
								"Something wrong, loaded offline message from DB but parsed no "
										+ "XML elements: {0}", msg_str);
					} else {
						Timestamp ts = rs.getTimestamp(MSG_EXPIRED_COLUMN);
						MsgDBItem item = new MsgDBItem(rs.getLong(MSG_ID_COLUMN), msg, ts);

						expiredQueue.offer(item);
					}
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages from db: ", e);
		} finally {
			data_repo.release(null, rs);
		}

		earliestOffline = Long.MAX_VALUE;
	}

	// ~--- inner classes --------------------------------------------------------

	private class MsgDBItem implements Delayed {
		private long db_id = -1;
		private Date expired = null;
		private Element msg = null;

		// ~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 * 
		 * @param db_id
		 * @param msg
		 * @param expired
		 */
		public MsgDBItem(long db_id, Element msg, Date expired) {
			this.db_id = db_id;
			this.msg = msg;
			this.expired = expired;
		}

		// ~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 * 
		 * @param o
		 * @return
		 */
		@Override
		public int compareTo(Delayed o) {
			return (int) (getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
		}

		// ~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 * 
		 * @param unit
		 * @return
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(expired.getTime() - System.currentTimeMillis(),
					TimeUnit.MILLISECONDS);
		}
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
