/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.server.ssender;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Message;
import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.XMLUtils;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//import java.sql.Date;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * <code>DrupalForumTask</code> implements tasks for cyclic retrieving new
 * posts on selected <a href="http://drupal.org/">Drupal</a> forum.
 * It detects both new forum topics and new comments for forum topics.
 * Then it can sends this to one selected JID. Thus it should be used toghether
 * with StanzaReceiver task which can distribute this informatin to all interested
 * (subscribed) users.
 * <br>
 * You have to specify forum ID for monitoring in connection string as well as
 * destination JID where forum posts have to be sent. It is not very useful to
 * send post to just one person so to ditribute forum posts to biger number of
 * users this task should be paired with <code>StanzaReceiver</code> task which
 * can distribute it to all interested users.
 * Sample connection string:
 * <pre>jdbc:mysql://localhost/tigasedb?user=tigase&amp;password=pass&amp;forum=3&amp;jid=nick@domain.com</pre>
 * <br>
 * Created: Fri Apr 20 12:10:55 2007
 * <br>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DrupalForumTask extends SenderTask {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.server.ssender.DrupalForumTask");
	private static final long SECOND = 1000;

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
	 * <code>forumId</code> variable keeps drupal numerical forum ID which
	 * has to be monitored for new posts.
	 */
	private long forumId = -1;

	/**
	 * <code>get_new_comments</code> is a prepared statement for retrieving all
	 * new comments from drupal forum.
	 */
	private PreparedStatement get_new_comments = null;

	/**
	 * <code>get_new_topics</code> is a prepared statement for retrieving all
	 * new topics for drupal forum.
	 */
	private PreparedStatement get_new_topics = null;

	/**
	 * <code>handler</code> is a reference to object processing stanza
	 * read from database.
	 */
	private StanzaHandler handler = null;

	/**
	 * <code>jid</code> keeps destination address where all new posts from
	 * the forum are sent.
	 *
	 */
	private JID jid = null;

	/**
	 * <code>lastCheck</code> keeps time of last forum comments check so it
	 * gets only new posts.
	 *
	 */
	protected long lastCommentsCheck = -1;

	/**
	 * <code>lastConnectionValidated</code> variable keeps time where the
	 * connection was validated for the last time.
	 */
	private long lastConnectionValidated = 0;

	/**
	 * <code>lastCheck</code> keeps time of last forum topics check so it
	 * gets only new posts.
	 *
	 */
	protected long lastTopicsCheck = -1;

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean cancel() {
		boolean result = super.cancel();

		try {
			conn_valid_st.close();
			get_new_topics.close();
			get_new_comments.close();
			conn.close();
		} catch (Exception e) {

			// Ignore.
		}

		return result;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getInitString() {
		return db_conn;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void init(StanzaHandler handler, String initString) throws IOException {
		this.handler = handler;
		db_conn = initString;

		try {
			findExtraParams(db_conn);
		} catch (TigaseStringprepException ex) {
			throw new IOException("Destination address problem, stringprep processing failed", ex);
		}

		lastTopicsCheck = System.currentTimeMillis() / SECOND;
		lastCommentsCheck = System.currentTimeMillis() / SECOND;

		try {
			initRepo();
		} catch (SQLException e) {
			throw new IOException("Problem initializing SenderTask.", e);
		}
	}

	@Override
	public void run() {

//  log.info("Task " + getName()
//      + ", timestamp = " + lastCommentsCheck
// //      + ", getTime() = " + lastCommentsCheck.getTime()
//    + ", System.currentTimeMillis() = " + System.currentTimeMillis());
		Queue<Packet> results = getNewPackets();

		handler.handleStanzas(results);
	}

	//~--- get methods ----------------------------------------------------------

	protected Queue<Packet> getNewPackets() {
		Queue<Packet> results = new ArrayDeque<Packet>();

		newTopics(results);
		newComments(results);

		return results;
	}

	//~--- methods --------------------------------------------------------------

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

	/**
	 * <code>findTableName</code> method parses database connection string to find
	 * table name where stanza packets are waiting for sending.
	 *
	 * @param db_str a <code>String</code> value
	 */
	private void findExtraParams(String db_str) throws TigaseStringprepException {
		String[] params = db_str.split("&");

		for (String par : params) {
			if (par.startsWith("jid=")) {
				jid = JID.jidInstance(par.substring("jid=".length(), par.length()));
			}

			if (par.startsWith("forum")) {
				try {
					forumId = Long.parseLong(par.substring("forum=".length(), par.length()));
				} catch (NumberFormatException e) {
					forumId = -1;
					log.warning("Forum ID number is incorrect: "
							+ par.substring("forum=".length(), par.length()));
				}
			}
		}
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
		query = "select users.name as name, node_revisions.title as title,"
				+ " node_revisions.body as body" + " from forum, node_revisions, users where"
					+ " (status = 1) and (node_revisions.timestamp > ?)"
						+ " and forum.tid = ? and users.uid = node_revisions.uid"
							+ " and node_revisions.vid = forum.vid;";
		get_new_topics = conn.prepareStatement(query);
		query = "select name, thread, subject, comment " + "from comments where"
				+ " (status = 0) and (timestamp > ?)"
					+ " and (nid in (select nid from forum where tid = ?));";
		get_new_comments = conn.prepareStatement(query);
	}

	private void newComments(Queue<Packet> results) {
		ResultSet rs = null;

		try {
			checkConnection();
			log.info("New comment check, timestamp = " + lastCommentsCheck);
			get_new_comments.setLong(1, lastCommentsCheck);
			lastCommentsCheck = System.currentTimeMillis() / SECOND;
			get_new_comments.setLong(2, forumId);
			rs = get_new_comments.executeQuery();

			while (rs.next()) {
				String name = rs.getString("name");
				String thread = rs.getString("thread");
				String subject = rs.getString("subject");
				String comment = rs.getString("comment");
				Packet msg = Message.getMessage(getName(), jid, StanzaType.normal,
											 "New comment by " + name + ":\n\n" + XMLUtils.escape(comment),
											 XMLUtils.escape(subject), thread, null);

				log.fine("Sending new comment: " + msg.toString());
				results.offer(msg);
			}
		} catch (SQLException e) {

			// Let's ignore it for now.
			log.log(Level.WARNING, "Error retrieving stanzas from database: ", e);

			// It should probably do kind of auto-stop???
			// if so just uncomment below line:
			// this.cancel();
		} finally {
			release(rs);
			rs = null;
		}
	}

	private void newTopics(Queue<Packet> results) {
		ResultSet rs = null;

		try {
			checkConnection();
			get_new_topics.setLong(1, lastTopicsCheck);
			lastTopicsCheck = System.currentTimeMillis() / SECOND;
			get_new_topics.setLong(2, forumId);
			rs = get_new_topics.executeQuery();

			while (rs.next()) {
				String name = rs.getString("name");
				String title = rs.getString("title");
				String body = rs.getString("body");
				Packet msg = Message.getMessage(getName(), jid, StanzaType.normal,
											 "New post by " + name + ":\n\n" + XMLUtils.escape(body),
											 XMLUtils.escape(title), null, null);

				log.fine("Sending new topic: " + msg.toString());
				results.offer(msg);
			}
		} catch (SQLException e) {

			// Let's ignore it for now.
			log.log(Level.WARNING, "Error retrieving stanzas from database: ", e);

			// It should probably do kind of auto-stop???
			// if so just uncomment below line:
			// this.cancel();
		} finally {
			release(rs);
			rs = null;
		}
	}

	/**
	 * <code>release</code> method releases some SQL query variables.
	 *
	 * @param rs a <code>ResultSet</code> value
	 */
	private void release(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) {}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
