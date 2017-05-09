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
package tigase.server.amp.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.util.ExceptionUtilities;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

import java.sql.*;
import java.util.Date;
import java.util.*;
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
@Repository.SchemaId(id = Schema.SERVER_SCHEMA_ID, name = Schema.SERVER_SCHEMA_NAME)
public class JDBCMsgRepository extends MsgRepository<Long,DataRepository> {
	private static final Logger log = Logger.getLogger(JDBCMsgRepository.class.getName());

	@ConfigField(desc = "Query to add message", alias = "add-message-query")
	private String MSGS_ADD_MESSAGE = "{ call Tig_OfflineMessages_AddMessage(?,?,?,?,?,?,?) }";
	@ConfigField(desc = "Query to count messages", alias = "count-messages-query")
	private String MSGS_COUNT_MESSAGES = "{ call Tig_OfflineMessages_GetMessagesCount(?) }";
	@ConfigField(desc = "Query to list messages", alias = "list-messages-query")
	private String MSGS_LIST_MESSAGES = "{ call Tig_OfflineMessages_ListMessages(?) }";
	@ConfigField(desc = "Query to load messages", alias = "get-messages-query")
	private String MSGS_GET_MESSAGES = "{ call Tig_OfflineMessages_GetMessages(?) }";
	@ConfigField(desc = "Query to load messages by ids", alias = "get-messages-by-ids-query")
	private String MSGS_GET_MESSAGES_BY_IDS = "{ call Tig_OfflineMessages_GetMessagesByIds(?,?,?,?,?) }";
	@ConfigField(desc = "Query to delete message", alias = "delete-message-query")
	private String MSGS_DELETE_MESSAGE = "{ call Tig_OfflineMessages_DeleteMessage(?) }";
	@ConfigField(desc = "Query to delete messages", alias = "delete-messages-query")
	private String MSGS_DELETE_MESSAGES = "{ call Tig_OfflineMessages_DeleteMessages(?) }";
	@ConfigField(desc = "Query to delete messages by ids", alias = "delete-messages-by-ids-query")
	private String MSGS_DELETE_MESSAGES_BY_IDS = "{ call Tig_OfflineMessages_DeleteMessagesByIds(?,?,?,?,?) }";
	@ConfigField(desc = "Query to select expired messages", alias = "get-expired-messages-query")
	private String MSGS_GET_EXPIRED_MESSAGES = "{ call Tig_OfflineMessages_GetExpiredMessages(?) }";
	@ConfigField(desc = "Query to select expired messages before passed time", alias = "get-expired-messages-before-query")
	private String MSGS_GET_EXPIRED_MESSAGES_BEFORE = "{ call Tig_OfflineMessages_GetExpiredMessagesBefore(?) }";

//	private static final Map<String, JDBCMsgRepository> repos =
//			new ConcurrentSkipListMap<String, JDBCMsgRepository>();

	// ~--- fields ---------------------------------------------------------------

	protected DataRepository data_repo = null;
	private boolean initialized = false;

	@Override
	public void setDataSource(DataRepository data_repo) {
		try {
			data_repo.initPreparedStatement(MSGS_ADD_MESSAGE, MSGS_ADD_MESSAGE);
			data_repo.initPreparedStatement(MSGS_COUNT_MESSAGES, MSGS_COUNT_MESSAGES);
			data_repo.initPreparedStatement(MSGS_LIST_MESSAGES, MSGS_LIST_MESSAGES);
			data_repo.initPreparedStatement(MSGS_GET_MESSAGES, MSGS_GET_MESSAGES);
			data_repo.initPreparedStatement(MSGS_GET_MESSAGES_BY_IDS, MSGS_GET_MESSAGES_BY_IDS);
			data_repo.initPreparedStatement(MSGS_DELETE_MESSAGE, MSGS_DELETE_MESSAGE);
			data_repo.initPreparedStatement(MSGS_DELETE_MESSAGES, MSGS_DELETE_MESSAGES);
			data_repo.initPreparedStatement(MSGS_DELETE_MESSAGES_BY_IDS, MSGS_DELETE_MESSAGES_BY_IDS);
			data_repo.initPreparedStatement(MSGS_GET_EXPIRED_MESSAGES, MSGS_GET_EXPIRED_MESSAGES);
			data_repo.initPreparedStatement(MSGS_GET_EXPIRED_MESSAGES_BEFORE, MSGS_GET_EXPIRED_MESSAGES_BEFORE);
		} catch (SQLException ex) {
			log.log(Level.WARNING, "MsgRepository not initialized due to exception", ExceptionUtilities.getExceptionRootCause(ex,true));
			throw new RuntimeException("Could not initialize JDBCMsgRepository instance for " + data_repo.getResourceUri(), ex);
		}

		this.data_repo = data_repo;
	}

	@Override
	public void initRepository(String conn_str, Map<String, String> map)
			throws DBInitException {
		if (initialized) {
			return;
		}

		initialized = true;
		log.log(Level.INFO, "Initializing dbAccess for db connection url: {0}", conn_str);

		super.initRepository(conn_str, map);
		
		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, map);
			setDataSource(data_repo);

		} catch (Exception e) {
			log.log(Level.WARNING, "MsgRepository not initialized due to exception", e);
			// Ignore for now....
		}
	}

	@Override
	public Map<Enum,Long> getMessagesCount(JID to) {

		Map<Enum,Long> result = new HashMap<>(MSG_TYPES.values().length);

		try {

			ResultSet rs = null;

			// get number of messages
			PreparedStatement number_of_messages = data_repo.getPreparedStatement( to.getBareJID(), MSGS_COUNT_MESSAGES );

			synchronized ( number_of_messages ) {
				number_of_messages.setString( 1, to.getBareJID().toString() );
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
	public List<Element> getMessagesList( JID to) {
			List<Element> result = new LinkedList<Element>();
			ResultSet rs = null;

			try {
				PreparedStatement select_messages_list
													= data_repo.getPreparedStatement( to.getBareJID(), MSGS_LIST_MESSAGES );

				synchronized (select_messages_list) {
					try {
						select_messages_list.setString(1, to.getBareJID().toString());

						rs = select_messages_list.executeQuery();

						while (rs.next()) {
							long msgId = rs.getLong(1);
							int mType = rs.getInt(2);
							MSG_TYPES messageType = MSG_TYPES.getFromInt(mType);
							String sender = rs.getString(3);

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
				e.printStackTrace();
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

				if ( db_ids == null || db_ids.size() == 0 ){
					// fetch
					return loadMessagesToJID( session, delete, proc );
				} else {
					ResultSet rs = null;
					result = new LinkedList<Element>();

					Iterator<String> ids = db_ids.iterator();

					while (ids.hasNext()) {
						PreparedStatement select_ids_to_jid_st = data_repo.getPreparedStatement( to, MSGS_GET_MESSAGES_BY_IDS);
						synchronized ( select_ids_to_jid_st ) {
							try {
								select_ids_to_jid_st.setString(1, to.toString());
								for (int j = 0; j < 4; j++) {
									String id = ids.hasNext() ? ids.next() : null;
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
			
			if ( db_ids == null || db_ids.size() == 0 ){
				// purge
				PreparedStatement delete_to_jid_st
													= data_repo.getPreparedStatement( to, MSGS_DELETE_MESSAGES );

				ResultSet rs = null;
				synchronized ( delete_to_jid_st ) {
					try {
						delete_to_jid_st.setString(1, to.toString());
						rs = delete_to_jid_st.executeQuery();
						if (rs.next()) {
							affectedRows += rs.getInt(1);
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} else {
				Iterator<String> ids = db_ids.iterator();

				while (ids.hasNext()) {
					PreparedStatement delete_to_jid_st = data_repo.getPreparedStatement( to, MSGS_DELETE_MESSAGES_BY_IDS );
					ResultSet rs = null;
					synchronized ( delete_to_jid_st ) {
						try {
							delete_to_jid_st.setString(1, to.toString());
							for (int j = 0; j < 4; j++) {
								String id = ids.hasNext() ? ids.next() : null;
								delete_to_jid_st.setString(j + 2, id);
							}
							rs = delete_to_jid_st.executeQuery();
							if (rs.next()) {
								affectedRows += rs.getInt(1);
							}
						} finally {
							data_repo.release(null, rs);
						}
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
			
			ResultSet rs = null;
			PreparedStatement select_to_jid_st =
					data_repo.getPreparedStatement(to, MSGS_GET_MESSAGES);

			synchronized (select_to_jid_st) {
				try {
					select_to_jid_st.setString(1, to.toString());
					rs = select_to_jid_st.executeQuery();

					result = parseLoadedMessages(proc, rs);
				} finally {
					data_repo.release(null, rs);
				}
			}

			if (delete) {
				rs = null;
				try {
					PreparedStatement delete_to_jid_st = data_repo.getPreparedStatement(to, MSGS_DELETE_MESSAGES);

					synchronized (delete_to_jid_st) {
						delete_to_jid_st.setString(1, to.toString());
						rs = delete_to_jid_st.executeQuery();
					}
				} finally {
					data_repo.release(null, rs);
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
				sb.append( rs.getString( 1 ) );
			}

			if ( sb.length() > 0 ){
				DomBuilderHandler domHandler = new DomBuilderHandler();

				parser.parse( domHandler, sb.toString().toCharArray(), 0, sb.length() );
				result = domHandler.getParsedElements();
			}
		} else {
			result = new LinkedList<Element>();
			while ( rs.next() ) {
				final String msg = rs.getString( 1 );
				final long msgId = rs.getLong( 2 );

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
			long msgs_store_limit = getMsgsStoreLimit(to.getBareJID(), userRepo);

			PreparedStatement insert_msg_st =
					data_repo.getPreparedStatement(to.getBareJID(), MSGS_ADD_MESSAGE);

			synchronized (insert_msg_st) {
				insert_msg_st.setString(1, to.getBareJID().toString());
				insert_msg_st.setString(2, from.getBareJID().toString());
				int msg_type;
				try {
					final String name = msg.getName();
					final MSG_TYPES valueOf = MSG_TYPES.valueOf(name);
					msg_type = valueOf.ordinal();
				} catch (IllegalArgumentException e) {
					msg_type = Integer.MAX_VALUE;
				}

				insert_msg_st.setInt(3, msg_type);
				insert_msg_st.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
				insert_msg_st.setString(5, msg.toString());

				if (expired == null) {
					insert_msg_st.setNull(6, Types.TIMESTAMP);
				} else {
					Timestamp time = new Timestamp(expired.getTime());

					insert_msg_st.setTimestamp(6, time);
				}
				insert_msg_st.setLong(7, msgs_store_limit);
				
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
	protected void deleteMessage(Long msg_id) {
		try {
			PreparedStatement delete_id_st =
					data_repo.getPreparedStatement(null, MSGS_DELETE_MESSAGE);

			synchronized (delete_id_st) {
				delete_id_st.setLong(1, msg_id);
				delete_id_st.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem removing entry from DB: ", e);
		}
	}
	
	// ~--- methods --------------------------------------------------------------

	@Override
	protected void loadExpiredQueue(int min_elements) {
		try {
			ResultSet rs = null;
			PreparedStatement select_expired_st =
					data_repo.getPreparedStatement(null, MSGS_GET_EXPIRED_MESSAGES);

			synchronized (select_expired_st) {
				try {
					select_expired_st.setInt(1, min_elements);
					rs = select_expired_st.executeQuery();

					DomBuilderHandler domHandler = new DomBuilderHandler();
					int counter = 0;

					while (rs.next()
							&& ((expiredQueue.size() < MAX_QUEUE_SIZE) || (counter++ < min_elements))) {
						MsgDBItem item = parseExpiredMessage(domHandler, rs);
						if (item != null) {
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
					data_repo.getPreparedStatement(null, MSGS_GET_EXPIRED_MESSAGES_BEFORE);

			synchronized (select_expired_before_st) {
				try {
					select_expired_before_st.setTimestamp(1, new Timestamp(expired.getTime()));
					rs = select_expired_before_st.executeQuery();

					DomBuilderHandler domHandler = new DomBuilderHandler();
					int counter = 0;

					while (rs.next() && (counter++ < MAX_QUEUE_SIZE)) {
						MsgDBItem item = parseExpiredMessage(domHandler, rs);
						if (item != null) {
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

	protected MsgDBItem parseExpiredMessage(DomBuilderHandler domHandler, ResultSet rs) throws SQLException {
		String msg_str = rs.getString(3);

		parser.parse(domHandler, msg_str.toCharArray(), 0, msg_str.length());

		Queue<Element> elems = domHandler.getParsedElements();
		Element msg = elems.poll();

		if (msg == null) {
			log.log(Level.INFO,
					"Something wrong, loaded offline message from DB but parsed no "
							+ "XML elements: {0}", msg_str);
			return null;
		} else {
			Timestamp ts = rs.getTimestamp(2);
			return new MsgDBItem(rs.getLong(1), msg, ts);
		}
	}

}
