
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.stats;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Apr 20, 2010 6:39:05 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CounterDataLogger implements StatisticsArchivizerIfc {
	private static final Logger log = Logger.getLogger(CounterDataLogger.class.getName());

	/** Field description */
	public static final String DB_URL_PROP_KEY = "db-url";

	/** Field description */
	public static final String STATS_TABLE = "tig_stats_log";

	/**
	 * SQL float UNSIGNED
	 */
	public static final String CPU_USAGE_COL = "cpu_usage";

	/**
	 * SQL float UNSIGNED
	 */
	public static final String MEM_USAGE_COL = "mem_usage";

	/**
	 * SQL BIGINT
	 */
	public static final String UPTIME_COL = "uptime";

	/**
	 * SQL INT UNSIGNED
	 */
	public static final String VHOSTS_COL = "vhosts";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String SM_PACKETS_COL = "sm_packets";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String MUC_PACKETS_COL = "muc_packets";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String PUBSUB_PACKETS_COL = "pubsub_packets";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String C2S_PACKETS_COL = "c2s_packets";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String S2S_PACKETS_COL = "s2s_packets";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String EXT_PACKETS_COL = "ext_packets";

	/**
	 * SQL INT UNSIGNED
	 */
	public static final String C2S_CONNS_COL = "c2s_conns";

	/**
	 * SQL INT UNSIGNED
	 */
	public static final String S2S_CONNS_COL = "s2s_conns";

	/**
	 * SQL INT UNSIGNED
	 */
	public static final String BOSH_CONNS_COL = "bosh_conns";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String PRESENCES_COL = "presences";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String MESSAGES_COL = "messages";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String IQS_COL = "iqs";

	/**
	 * SQL BIGINT UNSIGNED
	 */
	public static final String REGISTERED_COL = "registered";
	private static final String CREATE_STATS_TABLE = "create table " + STATS_TABLE + " ( "
		+ " lid serial," + " ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + " " + CPU_USAGE_COL
		+ " float unsigned not null default 0," + " " + MEM_USAGE_COL
		+ " float unsigned not null default 0," + " " + UPTIME_COL + " bigint not null default 0,"
		+ " " + VHOSTS_COL + " int unsigned not null default 0," + " " + SM_PACKETS_COL
		+ " bigint unsigned not null default 0," + " " + MUC_PACKETS_COL
		+ " bigint unsigned not null default 0," + " " + PUBSUB_PACKETS_COL
		+ " bigint unsigned not null default 0," + " " + C2S_PACKETS_COL
		+ " bigint unsigned not null default 0," + " " + S2S_PACKETS_COL
		+ " bigint unsigned not null default 0," + " " + EXT_PACKETS_COL
		+ " bigint unsigned not null default 0," + " " + PRESENCES_COL
		+ " bigint unsigned not null default 0," + " " + MESSAGES_COL
		+ " bigint unsigned not null default 0," + " " + IQS_COL
		+ " bigint unsigned not null default 0," + " " + REGISTERED_COL
		+ " bigint unsigned not null default 0," + " " + C2S_CONNS_COL
		+ " int unsigned not null default 0," + " " + S2S_CONNS_COL
		+ " int unsigned not null default 0," + " " + BOSH_CONNS_COL
		+ " int unsigned not null default 0," + " primary key(ts))";
	private static final String STATS_INSERT_QUERY = "insert into " + STATS_TABLE + "("
		+ CPU_USAGE_COL + ", " + MEM_USAGE_COL + ", " + UPTIME_COL + ", " + VHOSTS_COL + ", "
		+ SM_PACKETS_COL + ", " + MUC_PACKETS_COL + ", " + PUBSUB_PACKETS_COL + ", " + C2S_PACKETS_COL
		+ ", " + S2S_PACKETS_COL + ", " + EXT_PACKETS_COL + ", " + PRESENCES_COL + ", " + MESSAGES_COL
		+ ", " + IQS_COL + ", " + REGISTERED_COL + ", " + C2S_CONNS_COL + ", " + S2S_CONNS_COL + ", "
		+ BOSH_CONNS_COL + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	//~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;
	private long last_c2s_packets = 0;
	private long last_ext_packets = 0;
	private long last_iqs = 0;
	private long last_messages = 0;
	private long last_muc_packets = 0;
	private long last_presences = 0;
	private long last_pubsub_packets = 0;
	private long last_s2s_packets = 0;
	private long last_sm_packets = 0;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cpu_usage
	 * @param mem_usage
	 * @param uptime
	 * @param vhosts
	 * @param sm_packets
	 * @param muc_packets
	 * @param pubsub_packets
	 * @param c2s_packets
	 * @param s2s_packets
	 * @param ext_packets
	 * @param presences
	 * @param messages
	 * @param iqs
	 * @param registered
	 * @param c2s_conns
	 * @param s2s_conns
	 * @param bosh_conns
	 *
	 */
	public void addStatsLogEntry(float cpu_usage, float mem_usage, long uptime, int vhosts,
			long sm_packets, long muc_packets, long pubsub_packets, long c2s_packets, long s2s_packets,
				long ext_packets, long presences, long messages, long iqs, long registered, int c2s_conns,
					int s2s_conns, int bosh_conns) {
		try {
			PreparedStatement insert_stats = data_repo.getPreparedStatement(STATS_INSERT_QUERY);

			synchronized (insert_stats) {
				insert_stats.setFloat(1, ((cpu_usage >= 0f) ? cpu_usage : 0f));
				insert_stats.setFloat(2, mem_usage);
				insert_stats.setLong(3, uptime);
				insert_stats.setInt(4, vhosts);
				insert_stats.setLong(5, sm_packets);
				insert_stats.setLong(6, muc_packets);
				insert_stats.setLong(7, pubsub_packets);
				insert_stats.setLong(8, c2s_packets);
				insert_stats.setLong(9, s2s_packets);
				insert_stats.setLong(10, ext_packets);
				insert_stats.setLong(11, presences);
				insert_stats.setLong(12, messages);
				insert_stats.setLong(13, iqs);
				insert_stats.setLong(14, registered);
				insert_stats.setInt(15, c2s_conns);
				insert_stats.setInt(16, s2s_conns);
				insert_stats.setInt(17, bosh_conns);
				insert_stats.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param sp
	 */
	@Override
	public void execute(StatisticsProvider sp) {
		long c2s_packets = sp.getCompPackets("c2s");
		long ext_packets = sp.getCompPackets("ext");
		long iqs = sp.getCompIqs("sess-man");
		long messages = sp.getCompMessages("sess-man");
		long muc_packets = sp.getCompPackets("muc");
		long presences = sp.getCompPresences("sess-man");
		long pubsub_packets = sp.getCompPackets("pubsub");
		long s2s_packets = sp.getCompPackets("s2s");
		long sm_packets = sp.getSMPacketsNumber();

		addStatsLogEntry(sp.getCPUUsage(), sp.getHeapMemUsage(), sp.getUptime(),
				sp.getStats("vhost-man", "Number of VHosts", 0), sm_packets - last_sm_packets,
					muc_packets - last_muc_packets, pubsub_packets - last_pubsub_packets,
						c2s_packets - last_c2s_packets, s2s_packets - last_s2s_packets,
							ext_packets - last_ext_packets, presences - last_presences, messages - last_messages,
								iqs - last_iqs, sp.getRegistered(), sp.getCompConnections("c2s"),
									sp.getCompConnections("s2s"), sp.getCompConnections("bosh"));
		last_c2s_packets = c2s_packets;
		last_ext_packets = ext_packets;
		last_iqs = iqs;
		last_messages = messages;
		last_muc_packets = muc_packets;
		last_presences = presences;
		last_pubsub_packets = pubsub_packets;
		last_s2s_packets = s2s_packets;
		last_sm_packets = sm_packets;
	}

	/**
	 * Method description
	 *
	 *
	 * @param archivizerConf
	 */
	@Override
	public void init(Map<String, Object> archivizerConf) {
		try {
			initRepository((String) archivizerConf.get(DB_URL_PROP_KEY), null);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Cannot initialize connection to database: ", ex);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn_str
	 * @param map
	 *
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws DBInitException
	 */
	public void initRepository(String conn_str, Map<String, String> map)
			throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException,
			DBInitException {
		log.log(Level.INFO, "Initializing dbAccess for db connection url: {0}", conn_str);
		data_repo = RepositoryFactory.getDataRepository(null, conn_str, map);
		data_repo.initPreparedStatement(STATS_INSERT_QUERY, STATS_INSERT_QUERY);

		// Check if DB is correctly setup and contains all required tables.
		checkDB();
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void release() {}

	private void checkDB() throws SQLException {
		ResultSet rs = null;
		Statement st = null;

		try {
			if ( !data_repo.checkTable(STATS_TABLE)) {
				st = data_repo.createStatement();
				st.executeUpdate(CREATE_STATS_TABLE);
			}
		} finally {
			data_repo.release(st, rs);
			rs = null;
			st = null;
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
