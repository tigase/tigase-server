/*
 * ShortNewsPublisher.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



package tigase.server.sreceiver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Message;
import tigase.server.Packet;

import tigase.xml.XMLUtils;

import tigase.xmpp.StanzaType;

import static tigase.server.sreceiver.PropertyConstants.*;

//~--- JDK imports ------------------------------------------------------------

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * This subscription tasks allows you to publish short news on your site.
 * In fact this task behaves exactly as <code>NewsDistributor</code> tasks
 * but it also writes all messages content to a table in database.
 * Web application can then read content from this table and publish it
 * on the Web site.
 *
 * Format of the table is as follows (schema definition in MySQL script):
 * <pre>
 * create table short_news (
 *   -- Automatic record ID
 *   snid            bigint unsigned NOT NULL auto_increment,
 *   -- Automaticly generated timestamp and automaticly updated on change
 *   publishing_time timestamp,
 *   -- Optional news type: 'shorts', 'minis', 'techs', 'funs'....
 *   news_type        varchar(10),
 *   -- Author JID
 *   author          varchar(128) NOT NULL,
 *   -- Short subject - this is short news, right?
 *   subject         varchar(128) NOT NULL,
 *   -- Short news message - this is short news, right?
 *   body            varchar(1024) NOT NULL,
 *   primary key(snid),
 *   key publishing_date (publishing_date),
 *   key author (author)
 * ) default character set utf8;
 * </pre>
 *
 * Created: Sat May 26 10:25:42 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ShortNewsPublisher
				extends RepoRosterTask {
	private static final String DB_CONNECTION_DISPL_NAME = "Database connection string";
	private static final String DB_CONNECTION_PROP_KEY   = "db-connection-string";
	private static final String DB_CONNECTION_PROP_VAL   =
		"jdbc:mysql://localhost/tigase?user=root&password=mypass";
	private static final String DB_TABLE_DISPL_NAME  = "Database table name";
	private static final String DB_TABLE_PROP_KEY    = "db-table";
	private static final String DB_TABLE_PROP_VAL    = "short_news";
	private static final String NEWS_TYPE_DISPL_NAME = "News type";
	private static final String NEWS_TYPE_PROP_KEY   = "news-type";
	private static final String NEWS_TYPE_PROP_VAL   = "minis";
	private static final String TASK_HELP            =
		"This tasks writes all messages to special table in database" +
		" called 'short_news' and notifies all subscribed users about" +
		" new post. Table in database keeps following information about" +
		" post: publishing_time, author, subject, body. This subscription" +
		" task is ideal for publish short news on your Web site." +
		" Users can subscribe to the news just by adding task JID" +
		" to their roster, unsubscribing is equally simple - remove" +
		" JID from roster to stop receiving news. By default subscription" +
		" to this task is moderated."
	;
	private static final String TASK_TYPE = "Short news publisher";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.server.ssender.JDBCTask");

	//~--- fields ---------------------------------------------------------------

	/**
	 * <code>conn</code> variable keeps connection to database.
	 */
	private Connection conn = null;

	/**
	 * <code>conn_valid_st</code> is a prepared statement keeping query used
	 * to validate connection to database:
	 * <pre>select 1</pre>
	 */
	private PreparedStatement conn_valid_st = null;

	/**
	 * <code>connectionValidateInterval</code> is kind of constant keeping minimum
	 * interval for validating connection to database.
	 */
	private long connectionValidateInterval = 1000 * 60;

	/**
	 * <code>db_conn</code> field stores database connection string.
	 */
	private String db_conn = null;

	/**
	 * <code>delete_post</code> is a prepared statement for deleting post
	 * from database.
	 */
	private PreparedStatement delete_post = null;

	/**
	 * <code>insert_post</code> is a prepared statement for inserting new
	 * post to database.
	 */
	private PreparedStatement insert_post = null;

	/**
	 * <code>lastConnectionValidated</code> variable keeps time where the
	 * connection was validated for the last time.
	 */
	private long lastConnectionValidated = 0;

	/**
	 * <code>newsType</code> is a type of news saved to database.
	 */
	private String newsType = "minis";

	/**
	 * <code>tableName</code> keeps a table name where are stanza packets waiting
	 * for sending. Default is <code>xmpp_stanza</code>
	 */
	private String tableName = "short_news";

	/**
	 * <code>update_post</code> is a prepared statement for updating existing
	 * post in database.
	 */
	private PreparedStatement update_post = null;
	;

	//~--- constant enums -------------------------------------------------------

	private enum command { help, delete, update; }

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param results
	 */
	public void destroy(Queue<Packet> results) {
		super.destroy(results);
		try {
			conn_valid_st.close();
			insert_post.close();
			conn.close();
		} catch (Exception e) {

			// Ignore.
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Map<String, PropertyItem> getDefaultParams() {
		Map<String, PropertyItem> defs = super.getDefaultParams();

		defs.put(MESSAGE_TYPE_PROP_KEY,
						 new PropertyItem(MESSAGE_TYPE_PROP_KEY, MESSAGE_TYPE_DISPL_NAME,
															MessageType.NORMAL));
		defs.put(SUBSCR_RESTRICTIONS_PROP_KEY,
						 new PropertyItem(SUBSCR_RESTRICTIONS_PROP_KEY,
															SUBSCR_RESTRICTIONS_DISPL_NAME,
															SubscrRestrictions.MODERATED));
		defs.put(DESCRIPTION_PROP_KEY,
						 new PropertyItem(DESCRIPTION_PROP_KEY, DESCRIPTION_DISPL_NAME,
															"Short news for the Web site..."));
		defs.put(DB_CONNECTION_PROP_KEY,
						 new PropertyItem(DB_CONNECTION_PROP_KEY, DB_CONNECTION_DISPL_NAME,
															DB_CONNECTION_PROP_VAL));
		defs.put(DB_TABLE_PROP_KEY,
						 new PropertyItem(DB_TABLE_PROP_KEY, DB_TABLE_DISPL_NAME, DB_TABLE_PROP_VAL));
		defs.put(NEWS_TYPE_PROP_KEY,
						 new PropertyItem(NEWS_TYPE_PROP_KEY, NEWS_TYPE_DISPL_NAME,
															NEWS_TYPE_PROP_VAL));

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getHelp() {
		return TASK_HELP;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getType() {
		return TASK_TYPE;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param map
	 */
	public void setParams(final Map<String, Object> map) {
		super.setParams(map);

		Map<String, PropertyItem> props = getParams();

		if (map.get(DB_TABLE_PROP_KEY) != null) {
			tableName = (String) map.get(DB_TABLE_PROP_KEY);
			props.put(DB_TABLE_PROP_KEY,
								new PropertyItem(DB_TABLE_PROP_KEY, DB_TABLE_DISPL_NAME, tableName));
		}
		if (map.get(NEWS_TYPE_PROP_KEY) != null) {
			newsType = (String) map.get(NEWS_TYPE_PROP_KEY);
			props.put(NEWS_TYPE_PROP_KEY,
								new PropertyItem(NEWS_TYPE_PROP_KEY, NEWS_TYPE_DISPL_NAME, newsType));
		}
		if (map.get(DB_CONNECTION_PROP_KEY) != null) {
			db_conn = (String) map.get(DB_CONNECTION_PROP_KEY);
			props.put(DB_CONNECTION_PROP_KEY,
								new PropertyItem(DB_CONNECTION_PROP_KEY, DB_CONNECTION_DISPL_NAME,
																 db_conn));
			try {
				initRepo();
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Problem initializing database connection.", e);
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	protected void processMessage(Packet packet, Queue<Packet> results) {
		if (isPostCommand(packet)) {
			runCommand(packet, results);
		} else {
			super.processMessage(packet, results);
			addPost(packet, results);
		}
	}

	private void addPost(Packet packet, Queue<Packet> results) {
		try {
			checkConnection();

			String author  = packet.getStanzaFrom().getBareJID().toString();
			String subject = packet.getElemCDataStaticStr(Message.MESSAGE_SUBJECT_PATH);
			String body    = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);

			if (body != null) {
				if ((subject == null) || (subject.length() == 0)) {
					int dotIdx = body.indexOf('.');

					if (dotIdx > 0) {
						subject = body.substring(0, dotIdx);
					} else {
						subject = "--";
					}
				}
				insert_post.setString(1, author);
				insert_post.setString(2, XMLUtils.unescape(subject));
				insert_post.setString(3, XMLUtils.unescape(body));
				insert_post.executeUpdate();
				results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																				 StanzaType.chat,
																				 "Your post has been successfuly submitted.",
																				 "Short news submitions result.", null,
																				 packet.getStanzaId()));
			} else {

				// if body is null it might be an empty message used for
				// announcing other side that the user has just started typing
				// message, such messages we just ignore
				results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																				 StanzaType.normal,
																				 "Missing body, post has NOT been submitted.",
																				 "Short news submitions result.", null,
																				 packet.getStanzaId()));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Problem inserting new post: " + packet.toString(), e);
			results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																			 StanzaType.normal,
																			 "There was a problem with post submitting: " + e,
																			 "Short news submitions result.", null,
																			 packet.getStanzaId()));
		}
	}

	/**
	 * <code>checkConnection</code> method checks whether connection to database
	 * is still valid, if not it simply reconnect and reinitializes database
	 * backend.
	 *
	 * @return a <code>boolean</code> value
	 * @exception SQLException if an error occurs
	 */
	private boolean checkConnection() throws SQLException {
		try {
			long tmp = System.currentTimeMillis();

			if ((tmp - lastConnectionValidated) >= connectionValidateInterval) {
				conn_valid_st.executeQuery();
				lastConnectionValidated = tmp;
			}    // end of if ()
		} catch (Exception e) {
			initRepo();
		}      // end of try-catch

		return true;
	}

	private String commandsHelp() {
		return "Available commands are:\n" + "//help - display this help info\n" +
					 "//update N - update post number N, posts content to update\n" +
					 "             starts from the next line\n" +
					 "//delete N - remove post number N";
	}

	private void deletePost(long snid) throws SQLException {
		checkConnection();
		delete_post.setLong(1, snid);
		delete_post.executeUpdate();
	}

	/**
	 * <code>initRepo</code> method initializes database backend - connects to
	 * database, creates prepared statements and sets basic variables.
	 *
	 * @exception SQLException if an error occurs
	 */
	private void initRepo() throws SQLException {
		conn = DriverManager.getConnection(db_conn);
		conn.setAutoCommit(true);

		String query = "select 1;";

		conn_valid_st = conn.prepareStatement(query);
		if ((newsType == null) || (newsType.length() == 0)) {
			query = "insert into " + tableName + " (news_type, author, subject, body) " +
							" values (null, ?, ?, ?)";
		} else {
			query = "insert into " + tableName + " (news_type, author, subject, body) " +
							" values ('" + newsType + "', ?, ?, ?)";
		}
		insert_post = conn.prepareStatement(query);
		query       = "delete from " + tableName + " where snid = ?";
		delete_post = conn.prepareStatement(query);
		query       = "update " + tableName + " set subject = ?, body = ?" +
									" where snid = ? ";
		update_post = conn.prepareStatement(query);
	}

	//~--- get methods ----------------------------------------------------------

	private boolean isPostCommand(Packet packet) {
		String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);

		if (body != null) {
			for (command comm : command.values()) {
				if (body.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}

		return false;
	}

	//~--- methods --------------------------------------------------------------

	private void runCommand(Packet packet, Queue<Packet> results) {
		String body         = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);
		String[] body_split = body.split(" |\n|\r");

		try {
			command comm = command.valueOf(body_split[0].substring(2));

			switch (comm) {
			case help :
				results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																				 StanzaType.chat, commandsHelp(),
																				 "Commands description", null,
																				 packet.getStanzaId()));

				break;

			case update :
				updatePost(packet, Long.parseLong(body_split[1]));
				results.offer(
						Message.getMessage(
							packet.getStanzaTo(), packet.getStanzaFrom(), StanzaType.normal,
							"Post " + body_split[1] + " successfuly updated.",
							"Command execution result", null, packet.getStanzaId()));

				break;

			case delete :
				deletePost(Long.parseLong(body_split[1]));
				results.offer(
						Message.getMessage(
							packet.getStanzaTo(), packet.getStanzaFrom(), StanzaType.normal,
							"Post " + body_split[1] + " successfuly deleted.",
							"Command execution result", null, packet.getStanzaId()));

				break;

			default :
				break;
			}
		} catch (Exception e) {
			String error_text = "Hm, something wrong with command executing...: " +
													body_split[0] + ", " + body_split[1] + ", " + e;

			log.log(Level.WARNING, error_text, e);
			results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																			 StanzaType.normal, error_text,
																			 "Problem with command execution", null,
																			 packet.getStanzaId()));
		}
	}

	private void updatePost(Packet packet, long snid) throws SQLException {
		checkConnection();

		String subject = packet.getElemCDataStaticStr(Message.MESSAGE_SUBJECT_PATH);
		String body    = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);

		if ((body != null) && (subject != null)) {
			int idx = body.indexOf('\n');

			if ((idx > 0) && (idx < body.length() - 1) && (body.charAt(idx + 1) == '\r')) {
				++idx;
			}
			update_post.setString(1, XMLUtils.unescape(subject));
			update_post.setString(2, XMLUtils.unescape(body.substring(idx)));
			update_post.setLong(3, snid);
			update_post.executeUpdate();
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/02/19
