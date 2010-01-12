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
import java.util.Map;
import java.util.Date;
import java.util.Collections;
import java.util.logging.Logger;
import tigase.server.Packet;

import tigase.util.JDBCAbstract;
import tigase.xml.Element;

import tigase.util.SimpleCache;
import tigase.xmpp.BareJID;

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

	private static final long LONG_NULL = 0;

	private PreparedStatement save_message_st = null;
	private PreparedStatement get_msg_for_jid_st = null;
	private PreparedStatement get_jid_id_st = null;
	private PreparedStatement get_jids_id_st = null;
	private PreparedStatement add_jid_st = null;
	private PreparedStatement add_thread_st = null;
	private PreparedStatement get_thread_id_st = null;
	private PreparedStatement add_subject_st = null;
	private PreparedStatement get_subject_id_st = null;

	private int cacheSize = 10000;
	private long cacheTime = 60*60*1000;
	private Map<String, Object> cache = null;

	/**
	 * <code>initPreparedStatements</code> method initializes internal
	 * database connection variables such as prepared statements.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	protected void initPreparedStatements() throws SQLException {
		super.initPreparedStatements();
		String query = "insert into tig_ma_jid (jid) values (?);";
		add_jid_st = prepareStatement(query);

		query = "select ma_j_id from tig_ma_jid where jid = ?;";
		get_jid_id_st = prepareStatement(query);

		query = "select * from tig_ma_jid where (jid = ?) or (jid = ?);";
		get_jids_id_st = prepareStatement(query);

		query = "insert into tig_ma_thread (thread) values (?);";
		add_thread_st = prepareStatement(query);

		query = "select ma_t_id from tig_ma_thread where (thread = ?);";
		get_thread_id_st = prepareStatement(query);

		query = "insert into tig_ma_subject (subject) values (?);";
		add_subject_st = prepareStatement(query);

		query = "select ma_s_id from tig_ma_subject where (subject = ?);";
		get_subject_id_st = prepareStatement(query);
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
		cache =
      Collections.synchronizedMap(new SimpleCache<String, Object>(10000, 60*1000));
	}

	private long[] getJidsIds(String ... jids) throws SQLException {
		ResultSet rs = null;
		try {
			checkConnection();
			long[] results = new long[jids.length];
			for (int i = 0; i < results.length; i++) {
				results[i] = LONG_NULL;
			}
			if (jids.length == 1) {
				synchronized (get_jid_id_st) {
					get_jid_id_st.setString(1, jids[0]);
					rs = get_jid_id_st.executeQuery();
					if (rs.next()) {
						results[0] = rs.getLong("ma_j_id");
						return results;
					}
				}
				return null;
			} else {
				synchronized (get_jids_id_st) {
					for (int i = 0; i < jids.length; i++) {
						get_jids_id_st.setString(i+1, jids[i]);
					}
					rs = get_jids_id_st.executeQuery();
					int cnt = 0;
					while (rs.next()) {
						String db_jid = rs.getString("jid");
						for (int i = 0; i < jids.length; i++) {
							if (db_jid.equals(jids[i])) {
								results[i] = rs.getLong("ma_j_id");
								++cnt;
							}
						}
					}
					if (cnt > 0) {
						return results;
					} else {
						return null;
					}
				}
			}
		} finally {
			release(null, rs);
		}
		//return results;
	}

	private long addJidID(String jid) throws SQLException {
		checkConnection();
		synchronized (add_jid_st) {
			add_jid_st.setString(1, jid);
			add_jid_st.executeUpdate();
		}
		// This is not the most effective solution but this method shouldn't be
		// called very often so the perfrmance impact should be insignificant.
		long[] jid_ids = getJidsIds(jid);
		if (jid_ids != null) {
			return jid_ids[0];
		} else {
			// That should never happen here, but just in case....
			log.warning("I have just added new jid but it was not found.... " + jid);
			return LONG_NULL;
		}
	}

	private long getThreadID(String thread)
    throws SQLException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (get_thread_id_st) {
				get_thread_id_st.setString(1, thread);
				rs = get_thread_id_st.executeQuery();
				if (rs.next()) {
					return rs.getLong("ma_t_id");
				}
			}
		} finally {
			release(null, rs);
		}
		return LONG_NULL;
	}

	private long getSubjectID(String subject)
    throws SQLException {
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (get_subject_id_st) {
				get_subject_id_st.setString(1, subject);
				rs = get_subject_id_st.executeQuery();
				if (rs.next()) {
					return rs.getLong("ma_s_id");
				}
			}
		} finally {
			release(null, rs);
		}
		return LONG_NULL;
	}

	private long addThreadID(String thread)
    throws SQLException {
		long result = getThreadID(thread);
		if (result != LONG_NULL) {
			return result;
		}
		synchronized (add_thread_st) {
			add_thread_st.setString(1, thread);
			add_thread_st.executeUpdate();
		}
		// This is not the most effective solution but this method shouldn't be
		// called very often so the perfrmance impact should be insignificant.
		result = getThreadID(thread);
		return result;
	}

	private long addSubjectID(String subject)
    throws SQLException {
		long result = getSubjectID(subject);
		if (result != LONG_NULL) {
			return result;
		}
		synchronized (add_subject_st) {
			add_subject_st.setString(1, subject);
			add_subject_st.executeUpdate();
		}
		// This is not the most effective solution but this method shouldn't be
		// called very often so the perfrmance impact should be insignificant.
		result = getSubjectID(subject);
		return result;
	}

	public void saveMessage(Packet message, boolean full_content, String defLang)
    throws SQLException {
		BareJID from_str = message.getStanzaFrom().getBareJID();
		BareJID to_str = message.getStanzaTo().getBareJID();
		long[] ids = getJidsIds(from_str.toString(), to_str.toString());
		long from_id = (ids[0] != LONG_NULL ? ids[0] : addJidID(from_str.toString()));
		long to_id = (ids[1] != LONG_NULL ? ids[1] : addJidID(to_str.toString()));

		String thread = message.getElemCData("/message/thread");
		long thread_id = LONG_NULL;
		String subject = message.getElemCData("/message/subject");
		long subject_id = LONG_NULL;
		if (thread != null && !thread.trim().isEmpty()) {
			thread_id = addThreadID(thread);
		}
		if (subject != null && !subject.trim().isEmpty()) {
			subject_id = addSubjectID(subject);
		}
	}

	public void getMessages(String jid, String with_jid, Date timestamp, int limit) {
		
	}

}
