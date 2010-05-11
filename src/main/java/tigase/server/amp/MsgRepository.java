
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

import tigase.db.MsgRepositoryIfc;
import tigase.db.UserNotFoundException;

import tigase.util.JDBCAbstract;
import tigase.util.SimpleCache;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: May 3, 2010 5:28:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MsgRepository extends JDBCAbstract implements MsgRepositoryIfc {
	private static final Logger log = Logger.getLogger(MsgRepository.class.getName());
	private static final String MSG_TABLE = "msg_history";
	private static final String MSG_ID_COLUMN = "msg_id";
	private static final String MSG_TIMESTAMP_COLUMN = "ts";
	private static final String MSG_EXPIRED_COLUMN = "expired";
	private static final String MSG_FROM_UID_COLUMN = "sender_uid";
	private static final String MSG_TO_UID_COLUMN = "receiver_uid";
	private static final String MSG_BODY_COLUMN = "message";
	private static final String CREATE_MSG_TABLE = "create table " + MSG_TABLE + " ( " + "  "
		+ MSG_ID_COLUMN + " serial," + "  " + MSG_TIMESTAMP_COLUMN
		+ " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + "  " + MSG_EXPIRED_COLUMN + " DATETIME,"
		+ "  " + MSG_FROM_UID_COLUMN + " bigint unsigned NOT NULL," + "  " + MSG_TO_UID_COLUMN
		+ " bigint unsigned NOT NULL," + "  " + MSG_BODY_COLUMN + " varchar(4096) NOT NULL,"
		+ " key (" + MSG_EXPIRED_COLUMN + "), " + " key (" + MSG_FROM_UID_COLUMN + ", "
		+ MSG_TO_UID_COLUMN + ")," + " key (" + MSG_TO_UID_COLUMN + ", " + MSG_FROM_UID_COLUMN
		+ "))";
	private static final String MSG_INSERT_QUERY = "insert into " + MSG_TABLE + " ( "
		+ MSG_EXPIRED_COLUMN + ", " + MSG_FROM_UID_COLUMN + ", " + MSG_TO_UID_COLUMN + ", "
		+ MSG_BODY_COLUMN + ") values (?, ?, ?, ?)";
	private static final String MSG_SELECT_TO_JID_QUERY = "select * from " + MSG_TABLE
		+ " where " + MSG_TO_UID_COLUMN + " = ?";
	private static final String MSG_DELETE_TO_JID_QUERY = "delete from " + MSG_TABLE + " where "
		+ MSG_TO_UID_COLUMN + " = ?";
	private static final String GET_USER_UID_PROP_KEY = "user-uid-query";
	private static final String GET_USER_UID_DEF_QUERY = "{ call TigGetUserDBUid(?) }";
	private static final int MAX_UID_CACHE_SIZE = 100000;
	private static final long MAX_UID_CACHE_TIME = 3600000;
	private static final Map<String, MsgRepository> repos = new ConcurrentSkipListMap<String,
		MsgRepository>();

	//~--- fields ---------------------------------------------------------------

	private PreparedStatement delete_to_jid_st = null;
	private PreparedStatement insert_msg_st = null;
	private SimpleParser parser = SingletonFactory.getParserInstance();
	private PreparedStatement select_to_jid_st = null;
	private String uid_query = GET_USER_UID_DEF_QUERY;
	private PreparedStatement uid_st = null;
	private boolean initialized = false;
	private Map<BareJID, Long> uids_cache = Collections.synchronizedMap(new SimpleCache<BareJID,
		Long>(MAX_UID_CACHE_SIZE, MAX_UID_CACHE_TIME));

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id_string
	 *
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

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param conn_str
	 * @param map
	 *
	 * @throws SQLException
	 */
	@Override
	public void initRepository(String conn_str, Map<String, String> map) throws SQLException {
		if (initialized) {
			return;
		}

		initialized = true;
		log.info("Initializing dbAccess for db connection url: " + conn_str);

		if (map != null) {
			String query = map.get(GET_USER_UID_PROP_KEY);

			if (query != null) {
				uid_query = query;
			}
		}

		setResourceUri(conn_str);

		try {

			// This may fail if not required tables have been created yet.
			checkConnection();
		} catch (Exception e) {

			// Ignore for now....
		} finally {

			// Check if DB is correctly setup and contains all required tables.
			checkDB();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param time
	 * @param delete
	 *
	 * @return
	 */
	@Override
	public Queue<Element> loadMessagesExpired(long time, boolean delete) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param to
	 * @param delete
	 *
	 * @return
	 * @throws UserNotFoundException
	 */
	@Override
	public Queue<Element> loadMessagesToJID(JID to, boolean delete)
			throws UserNotFoundException {
		Queue<Element> result = null;
		ResultSet rs = null;

		try {
			checkConnection();

			long to_uid = getUserUID(to.getBareJID());

			synchronized (select_to_jid_st) {
				select_to_jid_st.setLong(1, to_uid);
				rs = select_to_jid_st.executeQuery();

				StringBuilder sb = new StringBuilder();

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
				synchronized (delete_to_jid_st) {
					delete_to_jid_st.setLong(1, to_uid);
					delete_to_jid_st.executeUpdate();
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages for user: " + to, e);
		} finally {
			release(null, rs);
		}

		return result;
	}

	/**
	 * Method description
	 *
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
		try {
			checkConnection();

			long from_uid = getUserUID(from.getBareJID());
			long to_uid = getUserUID(to.getBareJID());

			synchronized (insert_msg_st) {
				if (expired == null) {
					insert_msg_st.setNull(1, Types.TIME);
				} else {
					insert_msg_st.setTime(1, new Time(expired.getTime()));
				}

				insert_msg_st.setLong(2, from_uid);
				insert_msg_st.setLong(3, to_uid);
				insert_msg_st.setString(4, msg.toString());
				insert_msg_st.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	@Override
	protected void initPreparedStatements() throws SQLException {
		super.initPreparedStatements();
		uid_st = prepareQuery(uid_query);
		insert_msg_st = prepareQuery(MSG_INSERT_QUERY);
		select_to_jid_st = prepareQuery(MSG_SELECT_TO_JID_QUERY);
		delete_to_jid_st = prepareQuery(MSG_DELETE_TO_JID_QUERY);
	}

	private void checkDB() throws SQLException {
		ResultSet rs = null;

		try {
			String CHECK_TABLE_QUERY = "select count(*) from ";
			PreparedStatement checkTableSt = prepareStatement(CHECK_TABLE_QUERY + MSG_TABLE);

			rs = checkTableSt.executeQuery();

			if (rs.next()) {
				long count = rs.getLong(1);

				log.info("DB table " + MSG_TABLE + " OK, items: " + count);
			}
		} catch (Exception e) {
			PreparedStatement createTable = prepareStatement(CREATE_MSG_TABLE);

			createTable.executeUpdate();
		} finally {
			release(null, rs);
			rs = null;
		}
	}

	//~--- get methods ----------------------------------------------------------

	private long getUserUID(BareJID user_id) throws SQLException, UserNotFoundException {
		Long cache_res = uids_cache.get(user_id);

		if (cache_res != null) {
			return cache_res.longValue();
		}    // end of if (result != null)

		ResultSet rs = null;
		long result = -1;

		try {
			synchronized (uid_st) {
				uid_st.setString(1, user_id.toString());
				rs = uid_st.executeQuery();

				if (rs.next()) {
					result = rs.getLong(1);
				}
			}

			if (result <= 0) {
				throw new UserNotFoundException("User does not exist: " + user_id);
			}    // end of if (isnext) else
		} finally {
			release(null, rs);
		}

		uids_cache.put(user_id, result);

		return result;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
