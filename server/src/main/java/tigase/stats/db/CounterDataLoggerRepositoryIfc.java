/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2018 "Tigase, Inc." <office@tigase.com>
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
 */

package tigase.stats.db;

import tigase.db.DataSource;
import tigase.db.DataSourceAware;

public interface CounterDataLoggerRepositoryIfc<DS extends DataSource>
		extends DataSourceAware<DS> {

	void addStatsLogEntry(String hostname, float cpu_usage, float mem_usage, long uptime, int vhosts, long sm_packets,
	                      long muc_packets, long pubsub_packets, long c2s_packets, long ws2s_packets, long s2s_packets,
	                      long ext_packets, long presences, long messages, long iqs, long registered, int c2s_conns,
	                      int ws2s_conns, int bosh_conns, int s2s_conns, int sm_sessions, int sm_connections);

}
