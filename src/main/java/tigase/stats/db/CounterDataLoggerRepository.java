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

import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.db.util.RepositoryVersionAware;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Repository.Meta(supportedUris = {"jdbc:[^:]+:.*"})
@Repository.SchemaId(id = "counter_data_logger", name = "DB Counter Data Logger")
@RepositoryVersionAware.SchemaVersion(version = "0.0.1")
public class CounterDataLoggerRepository
		implements CounterDataLoggerRepositoryIfc<DataRepository>, RepositoryVersionAware {

	private static final String STATS_INSERT = "insert into tig_stats_log (hostname,cpu_usage,mem_usage,uptime,vhosts,sm_packets,muc_packets,pubsub_packets,c2s_packets,ws2s_packets,s2s_packets,ext_packets,presences,messages,iqs,registered,c2s_conns,ws2s_conns,bosh_conns,s2s_conns,sm_sessions,sm_connections) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	private final Logger log = Logger.getLogger(CounterDataLoggerRepository.class.getName());
	private DataRepository repository;

	@Override
	public void addStatsLogEntry(String hostname, float cpu_usage, float mem_usage, long uptime, int vhosts,
	                             long sm_packets, long muc_packets, long pubsub_packets, long c2s_packets,
	                             long ws2s_packets, long s2s_packets, long ext_packets, long presences,
	                             long messages, long iqs, long registered, int c2s_conns, int ws2s_conns,
	                             int bosh_conns, int s2s_conns, int sm_sessions, int sm_connections) {

		Objects.requireNonNull(hostname);

		try {
			PreparedStatement insert_stats = repository.getPreparedStatement(null, STATS_INSERT);

			synchronized (insert_stats) {
				insert_stats.setString(1, hostname);
				insert_stats.setFloat(2, ((cpu_usage >= 0f) ? cpu_usage : 0f));
				insert_stats.setFloat(3, mem_usage);
				insert_stats.setLong(4, uptime);
				insert_stats.setInt(5, vhosts);
				insert_stats.setLong(6, sm_packets);
				insert_stats.setLong(7, muc_packets);
				insert_stats.setLong(8, pubsub_packets);
				insert_stats.setLong(9, c2s_packets);
				insert_stats.setLong(10, ws2s_packets);
				insert_stats.setLong(11, s2s_packets);
				insert_stats.setLong(12, ext_packets);
				insert_stats.setLong(13, presences);
				insert_stats.setLong(14, messages);
				insert_stats.setLong(15, iqs);
				insert_stats.setLong(16, registered);
				insert_stats.setInt(17, c2s_conns);
				insert_stats.setInt(18, ws2s_conns);
				insert_stats.setInt(19, bosh_conns);
				insert_stats.setInt(20, s2s_conns);
				insert_stats.setInt(21, sm_sessions);
				insert_stats.setInt(22, sm_connections);
				insert_stats.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	@Override
	public void setDataSource(DataRepository repository) {
		try {
			repository.initPreparedStatement(STATS_INSERT, STATS_INSERT);
		} catch (SQLException e) {
			log.log(Level.WARNING, "Could not initialize repository", e);
		}
		this.repository = repository;
	}
}
