/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.stats.db;

import tigase.db.DataSource;
import tigase.db.DataSourceAware;

public interface CounterDataLoggerRepositoryIfc<DS extends DataSource>
		extends DataSourceAware<DS> {

	public static final String BOSH_CONNS_COL = "bosh_conns";
	public static final String C2S_CONNS_COL = "c2s_conns";
	public static final String WS2S_CONNS_COL = "ws2s_conns";
	public static final String C2S_PACKETS_COL = "c2s_packets";
	public static final String CPU_USAGE_COL = "cpu_usage";
	public static final String EXT_PACKETS_COL = "ext_packets";
	public static final String HOSTNAME_COL = "hostname";
	public static final String IQS_COL = "iqs";
	public static final String MEM_USAGE_COL = "mem_usage";
	public static final String MESSAGES_COL = "messages";
	public static final String MUC_PACKETS_COL = "muc_packets";
	public static final String PRESENCES_COL = "presences";
	public static final String PUBSUB_PACKETS_COL = "pubsub_packets";
	public static final String S2S_CONNS_COL = "s2s_conns";
	public static final String S2S_PACKETS_COL = "s2s_packets";
	public static final String WS2S_PACKETS_COL = "ws2s_packets";
	public static final String SM_PACKETS_COL = "sm_packets";
	public static final String SM_SESSIONS_COL = "sm_sessions";
	public static final String SM_CONNECTIONS_COL = "sm_connections";
	public static final String STATS_TABLE = "tig_stats_log";
	public static final String UPTIME_COL = "uptime";
	public static final String VHOSTS_COL = "vhosts";
	public static final String REGISTERED_COL = "registered";

	void addStatsLogEntry(String hostname, float cpu_usage, float mem_usage, long uptime, int vhosts, long sm_packets,
	                      long muc_packets, long pubsub_packets, long c2s_packets, long ws2s_packets, long s2s_packets,
	                      long ext_packets, long presences, long messages, long iqs, long registered, int c2s_conns,
	                      int ws2s_conns, int bosh_conns, int s2s_conns, int sm_sessions, int sm_connections);
}
