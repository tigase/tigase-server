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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>JDBCTask</code> implements tasks for cyclic retrieving stanzas from
 * database and sending them to the StanzaHandler object.
 * <br>
 * Database table format:
 * <ul>
 * <li><b>id</b> - numerical unique record indetifier.</li>
 * <li><b>stanza</b> - text field containing valid XML data with XMPP stanza to
 * send.</li>
 * </ul>
 * Any record in this table is treated the same way - Tigase assmes it contains
 * valid XML data with XMPP stanza to send. No other data are allowed in this
 * table. All stanzas must be complete including correct <em>"from"</em>
 * and <em>"to"</em> attriutes.
 * <br>
 * By default it looks for stanzas in <code>xmpp_stanza</code> table but you can
 * specify different table name in connection string. Sample connection string:
 * <pre>jdbc:mysql://localhost/tigasedb?user=tigase&amp;password=pass&amp;table=xmpp_stanza</pre>
 * <br>
 * Created: Fri Apr 20 12:10:55 2007
 * <br>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JDBCTask extends SenderTask {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.ssender.JDBCTask");

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
	 * <code>get_all_stanzas</code> is a prepared statement for retrieving all
	 * stanzas from database:
	 * <pre>select id, stanza from <tableName></pre>
	 */
	private PreparedStatement get_all_stanzas = null;
	/**
	 * <code>remove_stanza</code> is a prepared statement for query removing
	 * processed stanzas from database:
	 * <pre>delete from <tableName> where id = ?</pre>
	 */
	private PreparedStatement remove_stanza = null;
	/**
	 * <code>conn_valid_st</code> is a prepared statement keeping query used
	 * to validate connection to database:
	 * <pre>select 1</pre>
	 */
	private PreparedStatement conn_valid_st = null;

	/**
	 * <code>tableName</code> keeps a table name where are stanza packets waiting
	 * for sending. Default is <code>xmpp_stanza</code>
	 */
	private String tableName = "xmpp_stanza";

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
	private void findTableName(String db_str) {
		String[] params = db_str.split("&");
		for (String par: params) {
			if (par.startsWith("table=")) {
				tableName = par.substring("table=".length(), par.length());
				break;
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

		query = "select id, stanza from " + tableName;
		get_all_stanzas = conn.prepareStatement(query);

		query = "delete from " + tableName + " where id = ?";
		remove_stanza = conn.prepareStatement(query);
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
		findTableName(db_conn);

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
			get_all_stanzas.close();
			remove_stanza.close();
			conn.close();
		} catch (Exception e) {
			// Ignore.
		}
		return result;
	}

	/**
	 * <code>run</code> method is where all task work is done.
	 */
	public void run() {
		ResultSet rs = null;
		try {
			checkConnection();
			rs = get_all_stanzas.executeQuery();
			// Place to store all data ids to remove them later
			Set<Long> ids_to_delete = new HashSet<Long>();
			while (rs.next()) {
				// Collect all data ids to remove them later
				// it can be done simultanously as most JDBC drivers
				// don't support concurrent query execution
				// I would need to establish another connection to database
				ids_to_delete.add(rs.getLong("id"));
				// Handle stanza to the StanzaSender....
				String stanza = rs.getString("stanza");
				handler.handleStanza(stanza);
   				if (log.isLoggable(Level.FINEST)) {
    				log.finest("Sent stanza found in database: " + stanza);
                }
			}
			// Remove all processed stanzas
			for (long id: ids_to_delete) {
				remove_stanza.setLong(1, id);
				remove_stanza.executeUpdate();
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

}
