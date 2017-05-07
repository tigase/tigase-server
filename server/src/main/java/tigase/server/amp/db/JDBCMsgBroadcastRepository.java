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
import tigase.db.Repository;
import tigase.db.Schema;
import tigase.kernel.beans.config.ConfigField;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xmpp.BareJID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.amp.db.JDBCMsgRepository.Meta;

/**
 * Created by andrzej on 15.03.2016.
 */
@Meta( isDefault=true, supportedUris = { "jdbc:[^:]+:.*" } )
@Repository.SchemaId(id = Schema.SERVER_SCHEMA_ID, name = Schema.SERVER_SCHEMA_NAME)
public class JDBCMsgBroadcastRepository extends MsgBroadcastRepository<Long,DataRepository> {

	private static final Logger log = Logger.getLogger(JDBCMsgBroadcastRepository.class.getCanonicalName());

	@ConfigField(desc = "Query to add message to broadcast messsages", alias = "add-message")
	private String BROADCAST_ADD_MESSAGE = "{ call Tig_BroadcastMessages_AddMessage(?,?,?) }";
	@ConfigField(desc = "Query to add message recipient to broadcast message", alias = "add-message-recipient")
	private String BROADCAST_ADD_MESSAGE_RECIPIENT = "{ call Tig_BroadcastMessages_AddMessageRecipient(?,?) }";
	@ConfigField(desc = "Query to load not expired broadcast messages", alias = "get-messages")
	private String BROADCAST_GET_MESSAGES = "{ call Tig_BroadcastMessages_GetMessages(?) }";
	@ConfigField(desc = "Query to load recipients of broadcast message", alias = "get-message-recipients")
	private String BROADCAST_GET_MESSAGE_RECIPIENTS = "{ call Tig_BroadcastMessages_GetMessageRecipients(?) }";

	private DataRepository data_repo = null;

	@Override
	public void setDataSource(DataRepository data_repo) {
		try {
			data_repo.initPreparedStatement(BROADCAST_ADD_MESSAGE, BROADCAST_ADD_MESSAGE);
			data_repo.initPreparedStatement(BROADCAST_ADD_MESSAGE_RECIPIENT, BROADCAST_ADD_MESSAGE_RECIPIENT);
			data_repo.initPreparedStatement(BROADCAST_GET_MESSAGES, BROADCAST_GET_MESSAGES);
			data_repo.initPreparedStatement(BROADCAST_GET_MESSAGE_RECIPIENTS, BROADCAST_GET_MESSAGE_RECIPIENTS);
		} catch (SQLException ex) {
			log.log(Level.WARNING, "MsgRepository not initialized due to exception", ex);
		}

		this.data_repo = data_repo;
	}

	@Override
	public void loadMessagesToBroadcast() {
		try {
			Set<String> oldMessages = new HashSet<String>(broadcastMessages.keySet());

			ResultSet rs = null;
			PreparedStatement stmt = data_repo.getPreparedStatement(null, BROADCAST_GET_MESSAGES);

			synchronized (stmt) {
				try {
					Timestamp ts = new Timestamp(System.currentTimeMillis());
					System.out.println("loading expiring after " + ts);
					stmt.setTimestamp(1, ts);
					rs = stmt.executeQuery();

					DomBuilderHandler domHandler = new DomBuilderHandler();
					while (rs.next()) {
						String msgId = rs.getString(1);
						System.out.println("loaded msg with id = " + msgId);
						oldMessages.remove(msgId);
						if (broadcastMessages.containsKey(msgId))
							continue;

						Date expire = rs.getTimestamp(2);
						char[] msgChars = rs.getString(3).toCharArray();

						parser.parse(domHandler, msgChars, 0, msgChars.length);

						Queue<Element> elems = domHandler.getParsedElements();
						Element msg = elems.poll();
						if (msg == null) {
							System.out.println("not adding - msg is null!");
							continue;
						}

						broadcastMessages.put(msgId, new BroadcastMsg(null, msg, expire));
					}
					System.out.println("message loading finished!");
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
				stmt = data_repo.getPreparedStatement(null, BROADCAST_GET_MESSAGE_RECIPIENTS);
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
			PreparedStatement stmt = data_repo.getPreparedStatement(recipient, BROADCAST_ADD_MESSAGE);
			synchronized (stmt) {
				stmt.setString(1, id);
				stmt.setTimestamp(2, new Timestamp(expire.getTime()));
				stmt.setString(3, msg.toString());
				stmt.executeUpdate();
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem with updating broadcast message", ex);
		}
	}

	@Override
	protected void ensureBroadcastMessageRecipient(String id, BareJID recipient) {
		try {
			PreparedStatement stmt = data_repo.getPreparedStatement(recipient, BROADCAST_ADD_MESSAGE_RECIPIENT);
			synchronized (stmt) {
				stmt.setString(1, id);
				stmt.setString(2, recipient.toString());
				stmt.executeUpdate();
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem with updating broadcast message", ex);
		}
	}

}
