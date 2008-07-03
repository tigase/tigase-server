/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.server.ssender;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import java.sql.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.xmpp.StanzaType;
import tigase.xml.XMLUtils;

/**
 * <code>DrupalCommentsTask</code> implements tasks for cyclic retrieving new
 * comments in the Drupal database.
 * Then it can sends this to one selected JID. Thus it should be used toghether
 * with StanzaReceiver task which can distribute this informatin to all interested
 * (subscribed) users.
 * <p>
 * You have to specify destination JID where comments have to be sent.
 * It is not very useful to
 * send post to just one person so to ditribute forum posts to biger number of
 * users this task should be paired with <code>StanzaReceiver</code> task which
 * can distribute it to all interested users.
 * Sample connection string:
 * <pre>jdbc:mysql://localhost/tigasedb?user=tigase&amp;password=pass&amp;id=nick@domain.com</pre>
 * </p>
 * <p>
 * Created: Fri Apr 20 12:10:55 2007
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DrupalCommentsTask extends SenderTask {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.ssender.DrupalCommentsTask");

	private static final long SECOND = 1000;

	/**
	 * <code>handler</code> is a reference to object processing stanza
	 * read from database.
	 */
	private StanzaHandler handler = null;
	/**
	 * <code>db_conn</code> field stores database connection string.
	 */
	private String db_conn = null;
	/**
	 * <code>conn</code> variable keeps connection to database.
	 */
	private Connection conn = null;
	/**
	 * <code>get_new_comments</code> is a prepared statement for retrieving all
	 * new comments from drupal forum.
	 */
	private PreparedStatement get_new_comments = null;
	/**
	 * <code>conn_valid_st</code> is a prepared statement keeping query used
	 * to validate connection to database:
	 * <pre>select 1</pre>
	 */
	private PreparedStatement conn_valid_st = null;

	/**
	 * <code>jid</code> keeps destination address where all new posts from
	 * the forum are sent.
	 *
	 */
	private String jid = null;

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
	 * <code>connectionValidateInterval</code> is kind of constant keeping minimum
	 * interval for validating connection to database.
	 */
	private long connectionValidateInterval = 1000*60;

	/**
	 * <code>release</code> method releases some SQL query variables.
	 *
	 * @param rs a <code>ResultSet</code> value
	 */
	private void release(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) { }
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
			} // end of if ()
		} catch (Exception e) {
			initRepo();
		} // end of try-catch
		return true;
	}

	/**
	 * <code>findTableName</code> method parses database connection string to find
	 * table name where stanza packets are waiting for sending.
	 *
	 * @param db_str a <code>String</code> value
	 */
	private void findExtraParams(String db_str) {
		String[] params = db_str.split("&");
		for (String par: params) {
			if (par.startsWith("jid=")) {
				jid = par.substring("jid=".length(), par.length());
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

		query = "select name, thread, subject, comment "
			+ "from comments where"
			+ " (status = 0) and (timestamp > ?);";
		get_new_comments = conn.prepareStatement(query);
	}

	/**
	 * <code>init</code> method is a task specific initialization rountine.
	 *
	 * @param handler a <code>StanzaHandler</code> value is a reference to object
	 * which handles all stanza retrieved from data source. The handler is
	 * responsible for delivering stanza to destination address.
	 * @param initString a <code>String</code> value is an initialization string
	 * for this task. For example database tasks would expect database connection
	 * string here, filesystem task would expect directory here.
	 * @exception IOException if an error occurs during task or data storage
	 * initialization.
	 */
	public void init(StanzaHandler handler, String initString) throws IOException {
		this.handler = handler;
		db_conn = initString;
		findExtraParams(db_conn);
		lastCommentsCheck = System.currentTimeMillis() / SECOND;

		try {
			initRepo();
		} catch (SQLException e) {
			throw new IOException("Problem initializing SenderTask.", e);
		}
	}

	/**
	 * <code>getInitString</code> method returns initialization string passed
	 * to it in <code>init()</code> method.
	 *
	 * @return a <code>String</code> value of initialization string.
	 */
	public String getInitString() {
		return db_conn;
	}

	public boolean cancel() {
		boolean result = super.cancel();
		try {
			conn_valid_st.close();
			get_new_comments.close();
			conn.close();
		} catch (Exception e) {
			// Ignore.
		}
		return result;
	}

	private void newComments(Queue<Packet> results) {
		ResultSet rs = null;
		try {
			checkConnection();
			//			log.info("timestamp = " + lastCommentsCheck.toString());
			get_new_comments.setLong(1, lastCommentsCheck);
			lastCommentsCheck = System.currentTimeMillis() / SECOND;
			rs = get_new_comments.executeQuery();
			while (rs.next()) {
				String name = rs.getString("name");
				String thread = rs.getString("thread");
				String subject = rs.getString("subject");
				String comment = rs.getString("comment");
				Packet msg = Packet.getMessage(jid, getName(), StanzaType.normal,
					"New comment by " + name + ":\n\n" + XMLUtils.escape(comment),
					XMLUtils.escape(subject), thread);
				log.fine("Sending new comment: " + msg.toString());
				results.offer(msg);
			}
		} catch (SQLException e) {
			// Let's ignore it for now.
			log.log(Level.WARNING, "Error retrieving stanzas from database: ", e);
			// It should probably do kind of auto-stop???
			// if so just uncomment below line:
			//this.cancel();
		} finally {
			release(rs);
			rs = null;
		}
	}

	protected Queue<Packet> getNewPackets() {
		Queue<Packet> results = new LinkedList<Packet>();
		newComments(results);
		return results;
	}

	/**
	 * <code>run</code> method is where all task work is done.
	 */
	public void run() {
// 		log.info("Task " + getName()
//  			+ ", timestamp = " + lastCommentsCheck
// // 			+ ", getTime() = " + lastCommentsCheck.getTime()
// 			+ ", System.currentTimeMillis() = " + System.currentTimeMillis());
		Queue<Packet> results = getNewPackets();
		handler.handleStanzas(results);
	}

}
