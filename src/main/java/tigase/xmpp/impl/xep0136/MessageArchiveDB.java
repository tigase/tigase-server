/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.xmpp.impl.xep0136;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Date;
import java.util.logging.Logger;

import tigase.util.JDBCAbstract;
import tigase.util.JIDUtils;
import tigase.xml.Element;


/**
 * Describe class MessageArchiveDB here.
 *
 *
 * Created: Fri Feb 29 22:34:29 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageArchiveDB extends JDBCAbstract {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.xep0136.MessageArchiveDB");

	private PreparedStatement save_message_st = null;
	private PreparedStatement get_msg_for_jid_st = null;
	private PreparedStatement get_jid_id_st = null;
	private PreparedStatement get_jids_id_st = null;
	private PreparedStatement add_jid_id_st = null;

	/**
	 * <code>initPreparedStatements</code> method initializes internal
	 * database connection variables such as prepared statements.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	protected void initPreparedStatements() throws SQLException {
		super.initPreparedStatements();
		String query = "insert into tig_ma_jid (jid) values (?);";
		add_jid_id_st = prepareStatement(query);

		query = "select ma_j_id from tig_ma_jid where jid = ?;";
		get_jid_id_st = prepareStatement(query);

		query = "select * from tig_ma_jid where (jid = ?) or (jid = ?);";
		get_jids_id_st = prepareStatement(query);
	}

	/**
	 * <code>initRepository</code> method is doing lazy initialization with database.
	 * Connection to database will be established during the first authentication
	 * request.
	 *
	 * @param conn_str a <code>String</code> value of database connection string.
	 * The string must also contain database user name and password if required
	 * for connection.
	 * @exception SQLException if an error occurs during access database. It won't
	 * happen however as in this method we do simple variable assigment.
	 */
	public void initRepository(String conn_str, Map<String, String> params)
    throws SQLException {
		setResourceUri(conn_str);
	}

	private long[] getJidsIds(String ... jids) throws SQLException {
		checkConnection();
		long[] results = null;
		if (jids.length == 1) {

		} else {

		}
		return results;
	}

	private long addJidID(String jid) throws SQLException {
		checkConnection();
		synchronized (add_jid_id_st) {
			add_jid_id_st.setString(1, JIDUtils.getNodeID(jid));
			add_jid_id_st.executeUpdate();
		}
		// This is not the most effective solution but this methd shouldn't be
		// called very often so the perfrmance impact should be insignificant.
		long[] jid_ids = getJidsIds(jid);
		if (jid_ids != null) {
			return jid_ids[0];
		} else {
			// That should never happen here, but just in case....
			log.warning("I have just added new jid but it was not found.... " + jid);
			return -1;
		}
	}

	public void saveMessage(Element message, boolean full_content)
    throws SQLException {
		String from_str = JIDUtils.getNodeID(message.getAttribute("from"));
		String to_str = JIDUtils.getNodeID(message.getAttribute("to"));
	}

	public void getMessages(String jid, String with_jid, Date timestamp, int limit) {
		
	}

}
