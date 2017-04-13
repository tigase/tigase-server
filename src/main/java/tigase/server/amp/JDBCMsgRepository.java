/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.amp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.*;
import tigase.db.DataRepository.dbTypes;
import tigase.server.Packet;
import tigase.util.Algorithms;
import tigase.util.SimpleCache;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: May 3, 2010 5:28:02 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Repository.Meta( isDefault=true, supportedUris = { "jdbc:[^:]+:.*" } )
public class JDBCMsgRepository extends MsgRepository<Long> {
	private static final Logger log = Logger.getLogger(JDBCMsgRepository.class.getName());
	private static final String MSG_TABLE = "msg_history";
	private static final String MSG_ID_COLUMN = "msg_id";
	private static final String MSG_TIMESTAMP_COLUMN = "ts";
	private static final String MSG_EXPIRED_COLUMN = "expired";
	private static final String MSG_FROM_UID_COLUMN = "sender_uid";
	private static final String MSG_TO_UID_COLUMN = "receiver_uid";
	private static final String MSG_TYPE_COLUMN = "msg_type";
	private static final String MSG_BODY_COLUMN = "message";
	private static final String HISTORY_FLAG_COLUMN = "history_enabled";
	private static final String JID_TABLE = "user_jid";
	private static final String JID_ID_COLUMN = "jid_id";
	private static final String JID_SHA_COLUMN = "jid_sha";
	private static final String JID_COLUMN = "jid";
	private static final int StatementsCount = 2;
	/* @formatter:off */
	private static final String MYSQL_CREATE_MSG_TABLE =
							"create table " + MSG_TABLE + " ( " + "  "
							+ MSG_ID_COLUMN + " serial," + "  "
							+ MSG_TIMESTAMP_COLUMN + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + "  "
							+ MSG_EXPIRED_COLUMN + " DATETIME," + "  "
							+ MSG_FROM_UID_COLUMN + " bigint unsigned," + "  "
							+ MSG_TO_UID_COLUMN + " bigint unsigned NOT NULL," + "  "
							+ MSG_TYPE_COLUMN + " int NOT NULL default 0," + "  "
							+ MSG_BODY_COLUMN + " varchar(4096) NOT NULL," + "  "
							+ " key (" + MSG_EXPIRED_COLUMN + "), "
							+ " key (" + MSG_FROM_UID_COLUMN + ", " + MSG_TO_UID_COLUMN + "),"
							+ " key (" + MSG_TO_UID_COLUMN + ", " + MSG_FROM_UID_COLUMN + "))"
							+ " ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC;";
	private static final String PGSQL_CREATE_MSG_TABLE =
							"create table " + MSG_TABLE + " ( " + "  "
							+ MSG_ID_COLUMN + " serial," + "  "
							+ MSG_TIMESTAMP_COLUMN + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + "  "
							+ MSG_EXPIRED_COLUMN + " TIMESTAMP," + "  "
							+ MSG_FROM_UID_COLUMN + " bigint," + "  "
							+ MSG_TO_UID_COLUMN + " bigint NOT NULL," + "  "
							+ MSG_TYPE_COLUMN + " int NOT NULL default 0," + "  "
							+ MSG_BODY_COLUMN + " varchar(4096) NOT NULL);"
							+ "create index index_" + MSG_EXPIRED_COLUMN + " on " + MSG_TABLE
							+ " (" + MSG_EXPIRED_COLUMN + ");"
							+ "create index index_" + MSG_FROM_UID_COLUMN + "_" + MSG_TO_UID_COLUMN
							+ " on " + MSG_TABLE + "(" + MSG_FROM_UID_COLUMN + "," + MSG_TO_UID_COLUMN + ");"
							+ "create index index_" + MSG_TO_UID_COLUMN + "_" + MSG_FROM_UID_COLUMN
							+ " on " + MSG_TABLE + "(" + MSG_TO_UID_COLUMN + "," + MSG_FROM_UID_COLUMN + ");";
	private static final String SQLSERVER_CREATE_MSG_TABLE =
							"create table " + MSG_TABLE + " ( " + "  "
							+ MSG_ID_COLUMN + " [bigint] IDENTITY(1,1)," + "  "
							+ MSG_TIMESTAMP_COLUMN + " [datetime] DEFAULT getdate() ," + "  "
							+ MSG_EXPIRED_COLUMN + " [datetime] ," + "  "
							+ MSG_FROM_UID_COLUMN + " bigint," + "  "
							+ MSG_TO_UID_COLUMN + " bigint NOT NULL," + "  "
							+ MSG_TYPE_COLUMN + " int NOT NULL default 0," + "  "
							+ MSG_BODY_COLUMN + " nvarchar(4000) NOT NULL);"
							+ "create index index_" + MSG_EXPIRED_COLUMN + " on " + MSG_TABLE
							+ " (" + MSG_EXPIRED_COLUMN + ");"
							+ "create index index_" + MSG_FROM_UID_COLUMN + "_" + MSG_TO_UID_COLUMN
							+ " on " + MSG_TABLE + "(" + MSG_FROM_UID_COLUMN + "," + MSG_TO_UID_COLUMN + ");"
							+ "create index index_" + MSG_TO_UID_COLUMN + "_" + MSG_FROM_UID_COLUMN
							+ " on " + MSG_TABLE + "(" + MSG_TO_UID_COLUMN + "," + MSG_FROM_UID_COLUMN + ");";
	private static final String DERBY_CREATE_MSG_TABLE =
							"create table " + MSG_TABLE + " ( " + "  "
							+ MSG_ID_COLUMN + " bigint generated by default as identity not null," + "  "
							+ MSG_TIMESTAMP_COLUMN + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + "  "
							+ MSG_EXPIRED_COLUMN + " TIMESTAMP," + "  "
							+ MSG_FROM_UID_COLUMN + " bigint," + "  "
							+ MSG_TO_UID_COLUMN + " bigint NOT NULL," + "  "
							+ MSG_TYPE_COLUMN + " int NOT NULL default 0," + "  "
							+ MSG_BODY_COLUMN + " varchar(4096) NOT NULL);"
							+ "create index index_" + MSG_EXPIRED_COLUMN + " on " + MSG_TABLE
							+ " (" + MSG_EXPIRED_COLUMN + ");"
							+ "create index index_" + MSG_FROM_UID_COLUMN + "_" + MSG_TO_UID_COLUMN
							+ " on " + MSG_TABLE + "(" + MSG_FROM_UID_COLUMN + "," + MSG_TO_UID_COLUMN + ");"
							+ "create index index_" + MSG_TO_UID_COLUMN + "_" + MSG_FROM_UID_COLUMN
							+ " on " + MSG_TABLE + "(" + MSG_TO_UID_COLUMN + "," + MSG_FROM_UID_COLUMN + ");";
	private static final String MYSQL_CREATE_JID_TABLE =
							"create table " + JID_TABLE + " ( " + "  "
							+ JID_ID_COLUMN + " serial," + "  "
							+ JID_SHA_COLUMN + " char(128) NOT NULL," + "  "
							+ JID_COLUMN + " varchar(2049) NOT NULL," + "  "
							+ HISTORY_FLAG_COLUMN + " int default 0,"
							+ " primary key (" + JID_ID_COLUMN + "),"
							+ " unique key " + JID_SHA_COLUMN + " (" + JID_SHA_COLUMN + "),"
							+ " key " + JID_COLUMN + " (" + JID_COLUMN + "(255)))";
	private static final String PGSQL_CREATE_JID_TABLE =
							"create table " + JID_TABLE + " ( " + "  "
							+ JID_ID_COLUMN + " serial," + "  "
							+ JID_SHA_COLUMN + " char(128) NOT NULL," + "  "
							+ JID_COLUMN + " varchar(2049) NOT NULL," + "  "
							+ HISTORY_FLAG_COLUMN + " int default 0,"
							+ " primary key (" + JID_ID_COLUMN + ")); "
							+ "create unique index index_" + JID_SHA_COLUMN + " on " + JID_TABLE
							+ " (" + JID_SHA_COLUMN + "); "
							+ "create unique index index_" + JID_COLUMN + " on " + JID_TABLE
							+ " (" + JID_COLUMN + "); ";
	private static final String SQLSERVER_CREATE_JID_TABLE =
							"create table " + JID_TABLE + " ( " + "  "
							+ JID_ID_COLUMN + " [bigint] IDENTITY(1,1)," + "  "
							+ JID_SHA_COLUMN + " char(128) NOT NULL," + "  "
							+ JID_COLUMN + " nvarchar(2049) NOT NULL," + "  "
							+ HISTORY_FLAG_COLUMN + " int default 0,"
							+ " primary key (" + JID_ID_COLUMN + ")); "
							+ "create unique index index_" + JID_SHA_COLUMN + " on " + JID_TABLE
							+ " (" + JID_SHA_COLUMN + "); "
							+ "create unique index index_" + JID_COLUMN + " on " + JID_TABLE
							+ " (" + JID_COLUMN + "); ";
	private static final String DERBY_CREATE_JID_TABLE =
							"create table " + JID_TABLE + " ( " + "  "
							+ JID_ID_COLUMN + " bigint generated by default as identity not null," + "  "
							+ JID_SHA_COLUMN + " char(128) NOT NULL," + "  "
							+ JID_COLUMN + " varchar(2049) NOT NULL," + "  "
							+ HISTORY_FLAG_COLUMN + " int default 0,"
							+ " primary key (" + JID_ID_COLUMN + ")); "
							+ "create unique index index_" + JID_SHA_COLUMN + " on " + JID_TABLE
							+ " (" + JID_SHA_COLUMN + "); "
							+ "create unique index index_" + JID_COLUMN + " on " + JID_TABLE
							+ " (" + JID_COLUMN + "); ";
	private static final String MSG_INSERT_QUERY =
							"insert into " + MSG_TABLE + " ( "
							+ MSG_EXPIRED_COLUMN + ", "
							+ MSG_FROM_UID_COLUMN + ", "
							+ MSG_TO_UID_COLUMN + ", "
							+ MSG_TYPE_COLUMN + ", "
							+ MSG_BODY_COLUMN + ") values (?, ?, ?, ?, ?)";
	private static final String MSG_SELECT_TO_JID_QUERY =
															"select * from " + MSG_TABLE + " where " + MSG_TO_UID_COLUMN + " = ?";
	private static final String MSG_SELECT_IDS_TO_JID_QUERY =
															MSG_SELECT_TO_JID_QUERY + " AND "+ MSG_ID_COLUMN + " IN ( ";
	private static final String MSG_SELECT_COUNT_TO_JID_QUERY
															= "select " + MSG_TYPE_COLUMN + " , count(" + MSG_TYPE_COLUMN + ") from " + MSG_TABLE
																+ " where " + MSG_TO_UID_COLUMN + " = ? "
																+ " group by " + MSG_TYPE_COLUMN;
	private static final String MSG_SELECT_LIST_TO_JID_QUERY
															= "select " + MSG_ID_COLUMN + ","+ MSG_TYPE_COLUMN + ","+ JID_COLUMN + " from "
																+ MSG_TABLE + "  left join " + JID_TABLE
																+ " ON " + MSG_TABLE + "." + MSG_FROM_UID_COLUMN + "=" +  JID_TABLE + "." + JID_ID_COLUMN
																+ " where " + MSG_TO_UID_COLUMN + " = ? ";
	private static final String MSG_DELETE_TO_JID_QUERY =
															"delete from " + MSG_TABLE + " where " + MSG_TO_UID_COLUMN + " = ?";
	private static final String MSG_DELETE_IDS_TO_JID_QUERY =
															MSG_DELETE_TO_JID_QUERY + " AND " + MSG_ID_COLUMN + " IN ( ";
	private static final String MSG_DELETE_ID_QUERY =
															"delete from " + MSG_TABLE + " where " + MSG_ID_COLUMN + " = ?";
	private static final String MSG_SELECT_EXPIRED_QUERY =
															"select * from " + MSG_TABLE + " where " + MSG_EXPIRED_COLUMN + " is not null order by " + MSG_EXPIRED_COLUMN + " limit ?";
	private static final String DERBY_MSG_SELECT_EXPIRED_QUERY =
															"select * from " + MSG_TABLE + " where " + MSG_EXPIRED_COLUMN + " is not null order by " + MSG_EXPIRED_COLUMN + " FETCH FIRST ? ROWS ONLY";
	private static final String MSSQL_MSG_SELECT_EXPIRED_QUERY =
			"SELECT * FROM ( SELECT " + MSG_TABLE + ".*, ROW_NUMBER() OVER (ORDER BY " + MSG_EXPIRED_COLUMN + " ASC) AS RN FROM " +
					MSG_TABLE + " WHERE " + MSG_EXPIRED_COLUMN + " is not null) AS X WHERE RN <= ?";

	private static final String MSG_SELECT_EXPIRED_BEFORE_QUERY =
															"select * from " + MSG_TABLE + " where " + MSG_EXPIRED_COLUMN + " is not null and " + MSG_EXPIRED_COLUMN + " <= ? order by " + MSG_EXPIRED_COLUMN;

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
	
	private static final String GET_USER_UID_DEF_QUERY =
		"select " + 
		  JID_ID_COLUMN + ", " + 
		  JID_COLUMN + 
		" from " + JID_TABLE + " where " + JID_SHA_COLUMN + " = ?";
        private static final String MSG_COUNT_FOR_TO_AND_FROM_QUERY_DEF =
                "select count(*) from " + MSG_TABLE + " where " + MSG_TO_UID_COLUMN + " = ? and " + MSG_FROM_UID_COLUMN + " = ?";
	private static final String ADD_USER_JID_ID_QUERY =
		"insert into " + JID_TABLE + " ( " + JID_SHA_COLUMN + ", " + JID_COLUMN + ") select ?, ? "
		+ "WHERE NOT EXISTS (SELECT 1 FROM " + JID_TABLE+ " WHERE " + JID_SHA_COLUMN + " = ? AND " + JID_COLUMN +" = ? )";
	private static final String ADD_USER_JID_ID_QUERY_MYSQL =
		"insert into " + JID_TABLE + " ( " + JID_SHA_COLUMN + ", " + JID_COLUMN + ") select ?, ? "
		+ "from dual "
		+ "WHERE NOT EXISTS (SELECT 1 FROM " + JID_TABLE+ " WHERE " + JID_SHA_COLUMN + " = ? AND " + JID_COLUMN +" = ? )";
	private static final String ADD_USER_JID_ID_QUERY_DERBY =
		"insert into " + JID_TABLE + " ( " + JID_SHA_COLUMN + ", " + JID_COLUMN + ") select ?, ? "
		+ "FROM SYSIBM.SYSDUMMY1 "
		+ "WHERE NOT EXISTS (SELECT 1 FROM " + JID_TABLE+ " WHERE " + JID_SHA_COLUMN + " = ? AND " + JID_COLUMN +" = ? )";
	/* @formatter:on */
	private static final String GET_USER_UID_PROP_KEY = "user-uid-query";
	private static final String MSGS_COUNT_LIMIT_PROP_KEY = "count-limit-query";
	private static final int MAX_UID_CACHE_SIZE = 100000;
	private static final long MAX_UID_CACHE_TIME = 3600000;
//	private static final Map<String, JDBCMsgRepository> repos =
//			new ConcurrentSkipListMap<String, JDBCMsgRepository>();

	// ~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;
	private String uid_query = GET_USER_UID_DEF_QUERY;
	private String msg_count_for_limit_query = MSG_COUNT_FOR_TO_AND_FROM_QUERY_DEF;
	private String msg_insert_message_to_broadcast = SQL_MSG_INSERT_MESSAGE_TO_BROADCAST;
	private String msg_ensure_broadcast_recipient = SQL_MSG_ENSURE_BROADCAT_RECIPIETN;
	private String add_user_jid_id = ADD_USER_JID_ID_QUERY;
	private String msg_select_expired = MSG_SELECT_EXPIRED_QUERY;
	private boolean initialized = false;
	private Map<BareJID, Long> uids_cache = Collections
			.synchronizedMap(new SimpleCache<BareJID, Long>(MAX_UID_CACHE_SIZE,
					MAX_UID_CACHE_TIME));

	@Override
	public void initRepository(String conn_str, Map<String, String> map)
			throws DBInitException {
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

			query = map.get(JDBCMsgRepository.MSGS_COUNT_LIMIT_PROP_KEY);

			if (query != null) {
				msg_count_for_limit_query = query;
			}
		}

		super.initRepository(conn_str, map);
		
		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, map);
			switch (data_repo.getDatabaseType()) {
				case sqlserver:
				case jtds:
					msg_ensure_broadcast_recipient = SQLSERVER_MSG_ENSURE_BROADCAT_RECIPIETN;
					msg_insert_message_to_broadcast = SQLSERVER_MSG_INSERT_MESSAGE_TO_BROADCAST;
					msg_select_expired = MSSQL_MSG_SELECT_EXPIRED_QUERY;
					break;
				case derby:
					msg_select_expired = DERBY_MSG_SELECT_EXPIRED_QUERY;
					break;
				default:
					msg_ensure_broadcast_recipient = SQL_MSG_ENSURE_BROADCAT_RECIPIETN;
					msg_insert_message_to_broadcast = SQL_MSG_INSERT_MESSAGE_TO_BROADCAST;
					msg_select_expired = MSG_SELECT_EXPIRED_QUERY;
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
			checkDB();
			data_repo.initPreparedStatement(uid_query, uid_query);
			data_repo.initPreparedStatement(MSG_INSERT_QUERY, MSG_INSERT_QUERY);
			data_repo.initPreparedStatement(MSG_SELECT_TO_JID_QUERY, MSG_SELECT_TO_JID_QUERY);
			data_repo.initPreparedStatement(MSG_SELECT_COUNT_TO_JID_QUERY, MSG_SELECT_COUNT_TO_JID_QUERY);
			data_repo.initPreparedStatement(MSG_SELECT_LIST_TO_JID_QUERY, MSG_SELECT_LIST_TO_JID_QUERY);
			data_repo.initPreparedStatement(MSG_DELETE_TO_JID_QUERY, MSG_DELETE_TO_JID_QUERY);
			data_repo.initPreparedStatement(MSG_DELETE_ID_QUERY, MSG_DELETE_ID_QUERY);
			data_repo.initPreparedStatement(msg_select_expired, msg_select_expired);
			data_repo.initPreparedStatement(MSG_SELECT_EXPIRED_BEFORE_QUERY,
					MSG_SELECT_EXPIRED_BEFORE_QUERY);
			data_repo.initPreparedStatement(msg_count_for_limit_query,
					msg_count_for_limit_query);
			data_repo.initPreparedStatement(add_user_jid_id, add_user_jid_id);
			data_repo.initPreparedStatement(MSG_SELECT_BROADCAST_RECIPIENTS, MSG_SELECT_BROADCAST_RECIPIENTS);
			data_repo.initPreparedStatement(MSG_SELECT_MESSAGES_TO_BROADCAST, MSG_SELECT_MESSAGES_TO_BROADCAST);
			if (data_repo.getDatabaseType() == dbTypes.derby) {
				data_repo.initPreparedStatement(DERBY_MSG_ENSURE_BROADCAT_RECIPIETN1, DERBY_MSG_ENSURE_BROADCAT_RECIPIETN1);
				data_repo.initPreparedStatement(DERBY_MSG_ENSURE_BROADCAT_RECIPIETN2, DERBY_MSG_ENSURE_BROADCAT_RECIPIETN2);
				data_repo.initPreparedStatement(DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST1, DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST1);
				data_repo.initPreparedStatement(DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST2, DERBY_MSG_INSERT_MESSAGE_TO_BROADCAST2);
			} else {
				data_repo.initPreparedStatement(msg_ensure_broadcast_recipient, msg_ensure_broadcast_recipient);
				data_repo.initPreparedStatement(msg_insert_message_to_broadcast, msg_insert_message_to_broadcast);
			}

			for (int i=1; i<= StatementsCount; i++) {
				StringBuilder select = new StringBuilder().append(MSG_DELETE_IDS_TO_JID_QUERY);
				for (int j=1; j<=i; j++) {
					if (j > 1)
						select.append(" , ");
					select.append(" ? ");
				}
				select.append(")");

				data_repo.initPreparedStatement(MSG_DELETE_IDS_TO_JID_QUERY + "_" + i, select.toString());
			}

			for (int i=1; i<= StatementsCount; i++) {
				StringBuilder select = new StringBuilder().append(MSG_SELECT_IDS_TO_JID_QUERY);
				for (int j=1; j<=i; j++) {
					if (j > 1)
						select.append(" , ");
					select.append(" ? ");
				}
				select.append(")");
				data_repo.initPreparedStatement(MSG_SELECT_IDS_TO_JID_QUERY + "_" + i, select.toString());
			}


		} catch (Exception e) {
			log.log(Level.WARNING, "MsgRepository not initialized due to exception", e);
			// Ignore for now....
		}
	}

	@Override
	public Map<Enum,Long> getMessagesCount(JID to) throws UserNotFoundException {

		Map<Enum,Long> result = new HashMap<>(MSG_TYPES.values().length);

		try {

			ResultSet rs = null;

			long to_uid = getUserUID( to.getBareJID() );

			if ( to_uid < 0 ){
				throw new UserNotFoundException( "User: " + to + " was not found in database." );
			}

			// get number of messages
			PreparedStatement number_of_messages
												= data_repo.getPreparedStatement( to.getBareJID(), MSG_SELECT_COUNT_TO_JID_QUERY );

			synchronized ( number_of_messages ) {
				number_of_messages.setLong( 1, to_uid );
				rs = number_of_messages.executeQuery();

				while ( rs.next() ) {
					int msgType = rs.getInt( 1 );
					long msgCount = rs.getLong( 2 );
					result.put( MSG_TYPES.getFromInt( msgType ), msgCount );
				}
			}

		} catch ( SQLException e ) {
			log.log( Level.WARNING, "Problem getting offline messages for user: " + to, e );
		}

		return result;
	}

	@Override
	public List<Element> getMessagesList( JID to) throws UserNotFoundException {
			List<Element> result = new LinkedList<Element>();
			ResultSet rs = null;

			try {
				long to_uid = getUserUID( to.getBareJID() );

				if ( to_uid < 0 ){
					throw new UserNotFoundException( "User: " + to + " was not found in database." );
				}

				PreparedStatement select_messages_list
													= data_repo.getPreparedStatement( to.getBareJID(), MSG_SELECT_LIST_TO_JID_QUERY );

				synchronized (select_messages_list) {
					try {
						select_messages_list.setLong(1, to_uid);

						rs = select_messages_list.executeQuery();

						while (rs.next()) {
							long msgId = rs.getLong(MSG_ID_COLUMN);
							int mType = rs.getInt(MSG_TYPE_COLUMN);
							MSG_TYPES messageType = MSG_TYPES.getFromInt(mType);
							String sender = rs.getString(JID_COLUMN);

							if (msgId != 0 && messageType != MSG_TYPES.none && sender != null) {
								Element item = new Element("item",
										new String[]{"jid", "node", "type", "name"},
										new String[]{to.getBareJID().toString(), String.valueOf(msgId),
											messageType.name(), sender});
								result.add(item);
							}
						}
					} finally {
						data_repo.release(null, rs);
					}
				}

			} catch ( SQLException e ) {
				log.log( Level.WARNING, "Problem getting offline messages for user: " + to, e );
			}
			return result;

	}

	@Override
	public Queue<Element> loadMessagesToJID( List<String> db_ids, XMPPResourceConnection session, boolean delete, OfflineMessagesProcessor proc) throws UserNotFoundException {

			Queue<Element> result = null;
			BareJID to = null;
		
			try {
				to = session.getBareJID();		
				long to_uid = getUserUID( to );

				if ( to_uid < 0 ){
					throw new UserNotFoundException( "User: " + to + " was not found in database." );
				}

				if ( db_ids == null || db_ids.size() == 0 ){
					// fetch
					return loadMessagesToJID( session, delete, proc );
				} else {
					ResultSet rs = null;
					result = new LinkedList<Element>();

					Iterator<String> ids = db_ids.iterator();

					int iters = ( db_ids.size() / StatementsCount ) + 1;
					for ( int i = 0 ; i < iters ; i++ ) {
						int params = ( i == ( iters - 1 ) ) ? db_ids.size() % StatementsCount : StatementsCount;
						if (params == 0 ) continue;
						PreparedStatement select_ids_to_jid_st = data_repo.getPreparedStatement( to,
																																							 MSG_SELECT_IDS_TO_JID_QUERY + "_" + params );

						synchronized ( select_ids_to_jid_st ) {
							try {
								select_ids_to_jid_st.setLong(1, to_uid);
								for (int j = 0; j < params; j++) {
									String id = ids.next();
									select_ids_to_jid_st.setString(j + 2, id);
								}
								rs = select_ids_to_jid_st.executeQuery();
								result.addAll(parseLoadedMessages(proc, rs));
							} finally {
								data_repo.release(null, rs);
							}
						}
					}
				}

				if ( delete ){
					deleteMessagesToJID( null, session );
				}
			} catch ( SQLException e ) {
				log.log( Level.WARNING, "Problem getting offline messages for user: " + to, e );
			} catch (NotAuthorizedException ex) {
				log.log(Level.WARNING, "Session not authorized yet!", ex);				
			}
			return result;

	}

	@Override
	public int deleteMessagesToJID( List<String> db_ids, XMPPResourceConnection session ) throws UserNotFoundException {

		int affectedRows = 0;
		BareJID to = null;
		
		try {
			to = session.getBareJID();
			
			long to_uid = getUserUID( to );

			if ( to_uid < 0 ){
				throw new UserNotFoundException( "User: " + to + " was not found in database." );
			}

			if ( db_ids == null || db_ids.size() == 0 ){
				// purge
				PreparedStatement delete_to_jid_st
													= data_repo.getPreparedStatement( to, MSG_DELETE_TO_JID_QUERY );

				synchronized ( delete_to_jid_st ) {
					delete_to_jid_st.setLong( 1, to_uid );
					affectedRows += delete_to_jid_st.executeUpdate();
				}
			} else {
				Iterator<String> ids = db_ids.iterator();

				int iters = ( db_ids.size() / StatementsCount ) + 1;
					for ( int i = 0 ; i < iters ; i++ ) {
						int params = ( i == ( iters - 1 ) ) ? db_ids.size() % StatementsCount : StatementsCount;
						if (params == 0 ) continue;
					PreparedStatement delete_to_jid_st = data_repo.getPreparedStatement( to,
																																							 MSG_DELETE_IDS_TO_JID_QUERY + "_" + params );

					synchronized ( delete_to_jid_st ) {
						delete_to_jid_st.setLong( 1, to_uid );
						for ( int j = 0 ; j < params ; j++ ) {
							String id = ids.next();
							delete_to_jid_st.setString( j + 2, id );
						}
						affectedRows = affectedRows + delete_to_jid_st.executeUpdate();
					}
				}
			}
		} catch ( SQLException e ) {
			log.log( Level.WARNING, "Problem getting offline messages for user: " + to, e );
		} catch (NotAuthorizedException ex) {
			log.log(Level.WARNING, "Session not authorized yet!", ex);			
		}
		return affectedRows;
	}


	@Override
	public Queue<Element> loadMessagesToJID( XMPPResourceConnection session, boolean delete )
			throws UserNotFoundException {
		return loadMessagesToJID( session, delete, null );
	}

	public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete, OfflineMessagesProcessor proc)
			throws UserNotFoundException {
		Queue<Element> result = null;
		BareJID to = null;
		
		try {
			to = session.getBareJID();
			long to_uid = getUserUID(to);

			if (to_uid < 0) {
				throw new UserNotFoundException("User: " + to + " was not found in database.");
			}

			ResultSet rs = null;
			PreparedStatement select_to_jid_st =
					data_repo.getPreparedStatement(to, MSG_SELECT_TO_JID_QUERY);

			synchronized (select_to_jid_st) {
				try {
					select_to_jid_st.setLong(1, to_uid);
					rs = select_to_jid_st.executeQuery();

					StringBuilder sb = new StringBuilder(1000);

					result = parseLoadedMessages(proc, rs);
				} finally {
					data_repo.release(null, rs);
				}
			}

			if (delete) {
				PreparedStatement delete_to_jid_st =
						data_repo.getPreparedStatement(to, MSG_DELETE_TO_JID_QUERY);

				synchronized (delete_to_jid_st) {
					delete_to_jid_st.setLong(1, to_uid);
					delete_to_jid_st.executeUpdate();
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages for user: " + to, e);
		} catch (NotAuthorizedException ex) {
			log.log(Level.WARNING, "Session not authorized yet!", ex);
		}

		return result;
	}

	protected Queue<Element> parseLoadedMessages( OfflineMessagesProcessor proc, ResultSet rs) throws SQLException {
		StringBuilder sb = new StringBuilder( 1000 );
		Queue<Element> result = new LinkedList<Element>();
		if ( proc == null ){
			while ( rs.next() ) {
				sb.append( rs.getString( MSG_BODY_COLUMN ) );
			}

			if ( sb.length() > 0 ){
				DomBuilderHandler domHandler = new DomBuilderHandler();

				parser.parse( domHandler, sb.toString().toCharArray(), 0, sb.length() );
				result = domHandler.getParsedElements();
			}
		} else {
			result = new LinkedList<Element>();
			while ( rs.next() ) {
				final String msg = rs.getString( MSG_BODY_COLUMN );
				final long msgId = rs.getLong( MSG_ID_COLUMN );

				if ( msg != null ){
					DomBuilderHandler domHandler = new DomBuilderHandler();

					parser.parse( domHandler, msg.toCharArray(), 0, msg.length() );
					final Queue<Element> parsedElements = domHandler.getParsedElements();
					Element msgEl = parsedElements.poll();
					if ( msgEl != null && msgId > 0 ){

						proc.stamp( msgEl, String.valueOf( msgId ) );

						result.add( msgEl );
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo)
			throws UserNotFoundException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Storring expired: {0} message: {1}", new Object[] { expired,
					Packet.elemToString(msg) });
		}

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
			
			long msgs_store_limit = getMsgsStoreLimit(to.getBareJID(), userRepo);
			// If the msgs_store_limit is set to 0, skip the select because the message will be saved anyway
			if (msgs_store_limit != 0) {
				ResultSet rs = null;
				PreparedStatement count_msgs_st
						= data_repo.getPreparedStatement(to.getBareJID(), msg_count_for_limit_query);

				synchronized (count_msgs_st) {
					try {
						count_msgs_st.setLong(1, to_uid);
						count_msgs_st.setLong(2, from_uid);

						rs = count_msgs_st.executeQuery();

						if (rs.next()) {
							count = rs.getLong(1);
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			}

			// The insertion will be skipped if the msgs_store_limit is higher than 0 and it was passed
			if (msgs_store_limit != 0 && msgs_store_limit <= count) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Message store limit ({0}) exceeded for message: {1}",
							new Object[] { msgs_store_limit, Packet.elemToString(msg) });
				}
				return false;
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

				int msg_type;
				try {
					final String name = msg.getName();
					final MSG_TYPES valueOf = MSG_TYPES.valueOf(name);
					msg_type = valueOf.ordinal();
				} catch (IllegalArgumentException e) {
					msg_type = Integer.MAX_VALUE;
				}

				insert_msg_st.setLong(4, msg_type);
				// TODO: deal with messages bigger than the database can fit....
				insert_msg_st.setString(5, msg.toString());
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
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
		return true;
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
			if (data_repo.getDatabaseType() == dbTypes.derby) {
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
			
			if (data_repo.getDatabaseType() == dbTypes.derby) {
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

	/**
	 * Performs database check, creates missing schema if necessary
	 *
	 * @throws SQLException
	 */
	private void checkDB() throws SQLException {

		Statement stmt = null;

		try {
			DataRepository.dbTypes databaseType = data_repo.getDatabaseType();
			switch ( databaseType ) {
				case mysql:
					data_repo.checkTable( JID_TABLE, MYSQL_CREATE_JID_TABLE );
					data_repo.checkTable( MSG_TABLE, MYSQL_CREATE_MSG_TABLE );

					data_repo.checkTable( "broadcast_msgs", MYSQL_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", MYSQL_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
				case postgresql:
					data_repo.checkTable( JID_TABLE, PGSQL_CREATE_JID_TABLE );
					data_repo.checkTable( MSG_TABLE, PGSQL_CREATE_MSG_TABLE );

					data_repo.checkTable( "broadcast_msgs", PGSQL_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", PGSQL_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
				case derby:
					data_repo.checkTable( JID_TABLE, DERBY_CREATE_JID_TABLE );
					data_repo.checkTable( MSG_TABLE, DERBY_CREATE_MSG_TABLE );

					data_repo.checkTable( "broadcast_msgs", DERBY_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", DERBY_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
				case jtds:
				case sqlserver:
					data_repo.checkTable( JID_TABLE, SQLSERVER_CREATE_JID_TABLE );
					data_repo.checkTable( MSG_TABLE, SQLSERVER_CREATE_MSG_TABLE );

					data_repo.checkTable( "broadcast_msgs", SQLSERVER_CREATE_BROADCAST_MSGS_TABLE );
					data_repo.checkTable( "broadcast_msgs_recipients", SQLSERVER_CREATE_BROADCAST_MSGS_RECIPIENTS_TABLE );
					break;
			}

			try {
				stmt = data_repo.createStatement( null );
				stmt.executeQuery( "select " + MSG_TYPE_COLUMN + " from " + MSG_TABLE + " where " + MSG_ID_COLUMN + " = 0" );
			} catch ( SQLException ex ) {
				log.log( Level.INFO, MSG_TABLE + " table was in old version, performing update to add missing column" );
				// if this happens then we have issue with old database schema and missing body columns in MSGS_TABLE
				String alterTable = null;
				try {
					alterTable = "alter table " + MSG_TABLE + " add " + MSG_TYPE_COLUMN + " int NOT NULL default 0";
					if ( stmt == null ){
						stmt = data_repo.createStatement( null );
					}
					stmt.execute( alterTable );
				} catch ( SQLException ex1 ) {
					log.log( Level.SEVERE, "could not alter table " + MSG_TABLE + " to add missing column by SQL:\n"
																 + alterTable, ex1 );
				}
			}
		} finally {
			data_repo.release( stmt, null );
		}

	}

	@Override
	protected void deleteMessage(Long msg_id) {
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

	// ~--- methods --------------------------------------------------------------

	@Override
	protected void loadExpiredQueue(int min_elements) {
		try {
			ResultSet rs = null;
			PreparedStatement select_expired_st =
					data_repo.getPreparedStatement(null, msg_select_expired);

			synchronized (select_expired_st) {
				try {
					select_expired_st.setInt(1, min_elements);
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
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages from db: ", e);
		}

		earliestOffline = Long.MAX_VALUE;
	}

	@Override
	protected void loadExpiredQueue(Date expired) {
		try {
			if (expiredQueue.size() > 100 * MAX_QUEUE_SIZE) {
				expiredQueue.clear();
			}

			ResultSet rs = null;
			PreparedStatement select_expired_before_st =
					data_repo.getPreparedStatement(null, MSG_SELECT_EXPIRED_BEFORE_QUERY);

			synchronized (select_expired_before_st) {
				try {
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
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting offline messages from db: ", e);
		}

		earliestOffline = Long.MAX_VALUE;
	}
}
