/*
 *   Tigase Jabber/XMPP Server
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;

import tigase.server.Packet;

import tigase.util.SimpleCache;

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class MessageArchiveDB here.
 *
 *
 * Created: Fri Feb 29 22:34:29 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageArchiveDB {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(MessageArchiveDB.class.getName());
	private static final long LONG_NULL = 0;
	private static final String ADD_JID_QUERY = "insert into tig_ma_jid (jid) values (?)";
	private static final String GET_JID_ID_QUERY = "select ma_j_id from tig_ma_jid where jid = ?";
	private static final String GET_JID_IDS_QUERY =
		"select * from tig_ma_jid where (jid = ?) or (jid = ?)";
	private static final String ADD_THREAD_QUERY = "insert into tig_ma_thread (thread) values (?)";
	private static final String GET_THREAD_ID_QUERY =
		"select ma_t_id from tig_ma_thread where (thread = ?)";
	private static final String ADD_SUBJECT_QUERY = "insert into tig_ma_subject (subject) values (?)";
	private static final String GET_SUBJECT_ID_QUERY =
		"select ma_s_id from tig_ma_subject where (subject = ?)";

	//~--- fields ---------------------------------------------------------------

	private Map<String, Object> cache = null;

//private PreparedStatement save_message_st = null;
//private PreparedStatement get_msg_for_jid_st = null;
//private PreparedStatement get_jid_id_st = null;
//private PreparedStatement get_jids_id_st = null;
//private PreparedStatement add_jid_st = null;
//private PreparedStatement add_thread_st = null;
//private PreparedStatement get_thread_id_st = null;
//private PreparedStatement add_subject_st = null;
//private PreparedStatement get_subject_id_st = null;
	private int cacheSize = 10000;
	private long cacheTime = 60 * 60 * 1000;
	private DataRepository data_repo = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param with_jid
	 * @param timestamp
	 * @param limit
	 */
	public void getMessages(String jid, String with_jid, Date timestamp, int limit) {}

	//~--- methods --------------------------------------------------------------

	/**
	 * <code>initRepository</code> method is doing lazy initialization with database.
	 * Connection to database will be established during the first authentication
	 * request.
	 *
	 * @param conn_str a <code>String</code> value of database connection string.
	 * The string must also contain database user name and password if required
	 * for connection.
	 * @param params
	 * @exception SQLException if an error occurs during access database. It won't
	 * happen however as in this method we do simple variable assigment.
	 */
	public void initRepository(String conn_str, Map<String, String> params) throws SQLException {
		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);
			data_repo.initPreparedStatement(ADD_JID_QUERY, ADD_JID_QUERY);
			data_repo.initPreparedStatement(GET_JID_ID_QUERY, GET_JID_ID_QUERY);
			data_repo.initPreparedStatement(GET_JID_IDS_QUERY, GET_JID_IDS_QUERY);
			data_repo.initPreparedStatement(ADD_THREAD_QUERY, ADD_THREAD_QUERY);
			data_repo.initPreparedStatement(GET_THREAD_ID_QUERY, GET_THREAD_ID_QUERY);
			data_repo.initPreparedStatement(ADD_SUBJECT_QUERY, ADD_SUBJECT_QUERY);
			data_repo.initPreparedStatement(GET_SUBJECT_ID_QUERY, GET_SUBJECT_ID_QUERY);
		} catch (Exception ex) {}

		cache = Collections.synchronizedMap(new SimpleCache<String, Object>(10000, 60 * 1000));
	}

	/**
	 * Method description
	 *
	 *
	 * @param message
	 * @param full_content
	 * @param defLang
	 *
	 * @throws SQLException
	 */
	public void saveMessage(Packet message, boolean full_content, String defLang)
			throws SQLException {
		BareJID from_str = message.getStanzaFrom().getBareJID();
		BareJID to_str = message.getStanzaTo().getBareJID();
		long[] ids = getJidsIds(from_str.toString(), to_str.toString());
		long from_id = ((ids[0] != LONG_NULL) ? ids[0] : addJidID(from_str.toString()));
		long to_id = ((ids[1] != LONG_NULL) ? ids[1] : addJidID(to_str.toString()));
		String thread = message.getElemCData("/message/thread");
		long thread_id = LONG_NULL;
		String subject = message.getElemCData("/message/subject");
		long subject_id = LONG_NULL;

		if ((thread != null) &&!thread.trim().isEmpty()) {
			thread_id = addThreadID(thread);
		}

		if ((subject != null) &&!subject.trim().isEmpty()) {
			subject_id = addSubjectID(subject);
		}
	}

	private long addJidID(String jid) throws SQLException {
		PreparedStatement add_jid_st = data_repo.getPreparedStatement(null, ADD_JID_QUERY);

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
			log.log(Level.WARNING, "I have just added new jid but it was not found.... {0}", jid);

			return LONG_NULL;
		}
	}

	private long addSubjectID(String subject) throws SQLException {
		long result = getSubjectID(subject);

		if (result != LONG_NULL) {
			return result;
		}

		PreparedStatement add_subject_st = data_repo.getPreparedStatement(null, ADD_SUBJECT_QUERY);

		synchronized (add_subject_st) {
			add_subject_st.setString(1, subject);
			add_subject_st.executeUpdate();
		}

		// This is not the most effective solution but this method shouldn't be
		// called very often so the perfrmance impact should be insignificant.
		result = getSubjectID(subject);

		return result;
	}

	private long addThreadID(String thread) throws SQLException {
		long result = getThreadID(thread);

		if (result != LONG_NULL) {
			return result;
		}

		PreparedStatement add_thread_st = data_repo.getPreparedStatement(null, ADD_THREAD_QUERY);

		synchronized (add_thread_st) {
			add_thread_st.setString(1, thread);
			add_thread_st.executeUpdate();
		}

		// This is not the most effective solution but this method shouldn't be
		// called very often so the perfrmance impact should be insignificant.
		result = getThreadID(thread);

		return result;
	}

	//~--- get methods ----------------------------------------------------------

	private long[] getJidsIds(String... jids) throws SQLException {
		ResultSet rs = null;

		try {
			long[] results = new long[jids.length];

			for (int i = 0; i < results.length; i++) {
				results[i] = LONG_NULL;
			}

			if (jids.length == 1) {
				PreparedStatement get_jid_id_st = data_repo.getPreparedStatement(null, GET_JID_ID_QUERY);

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
				PreparedStatement get_jids_id_st = data_repo.getPreparedStatement(null, GET_JID_IDS_QUERY);

				synchronized (get_jids_id_st) {
					for (int i = 0; i < jids.length; i++) {
						get_jids_id_st.setString(i + 1, jids[i]);
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
			data_repo.release(null, rs);
		}

		// return results;
	}

	private long getSubjectID(String subject) throws SQLException {
		ResultSet rs = null;

		try {
			PreparedStatement get_subject_id_st = data_repo.getPreparedStatement(null, GET_SUBJECT_ID_QUERY);

			synchronized (get_subject_id_st) {
				get_subject_id_st.setString(1, subject);
				rs = get_subject_id_st.executeQuery();

				if (rs.next()) {
					return rs.getLong("ma_s_id");
				}
			}
		} finally {
			data_repo.release(null, rs);
		}

		return LONG_NULL;
	}

	private long getThreadID(String thread) throws SQLException {
		ResultSet rs = null;

		try {
			PreparedStatement get_thread_id_st = data_repo.getPreparedStatement(null, GET_THREAD_ID_QUERY);

			synchronized (get_thread_id_st) {
				get_thread_id_st.setString(1, thread);
				rs = get_thread_id_st.executeQuery();

				if (rs.next()) {
					return rs.getLong("ma_t_id");
				}
			}
		} finally {
			data_repo.release(null, rs);
		}

		return LONG_NULL;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
