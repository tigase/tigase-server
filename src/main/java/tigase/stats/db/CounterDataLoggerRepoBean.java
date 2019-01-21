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

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.beans.MDRepositoryBean;
import tigase.db.beans.MDRepositoryBeanWithStatistics;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.stats.CounterDataLogger;

@Bean(name = "repository", active = true, parent = CounterDataLogger.class)
public class CounterDataLoggerRepoBean
		extends MDRepositoryBeanWithStatistics<CounterDataLoggerRepositoryIfc>
		implements CounterDataLoggerRepositoryIfc {

	public CounterDataLoggerRepoBean() {
		super(CounterDataLoggerRepositoryIfc.class
		);
	}

	@Override
	public void addStatsLogEntry(String hostname, float cpu_usage, float mem_usage, long uptime, int vhosts,
	                             long sm_packets, long muc_packets, long pubsub_packets, long c2s_packets,
	                             long ws2s_packets, long s2s_packets, long ext_packets, long presences, long messages,
	                             long iqs, long registered, int c2s_conns, int ws2s_conns, int bosh_conns,
	                             int s2s_conns, int sm_sessions, int sm_connections) {
		getRepository("default").addStatsLogEntry(hostname, cpu_usage, mem_usage, uptime, vhosts, sm_packets,
		                                          muc_packets, pubsub_packets, c2s_packets, ws2s_packets, s2s_packets,
		                                          ext_packets, presences, messages, iqs, registered, c2s_conns,
		                                          ws2s_conns, bosh_conns, s2s_conns, sm_sessions, sm_connections);
	}

	@Override
	protected Class<? extends CounterDataLoggerRepositoryIfc> findClassForDataSource(DataSource dataSource)
			throws DBInitException {
		return DataSourceHelper.getDefaultClass(CounterDataLoggerRepositoryIfc.class, dataSource.getResourceUri());
	}

	@Override
	public Class<?> getDefaultBeanClass() {
		return CounterDataLoggerConfigBean.class;
	}

	@Override
	public void register(Kernel kernel) {
		super.register(kernel);
	}

	@Override
	public void setDataSource(DataSource ds) {
		// nothing to do..
	}

	@Override
	public void unregister(Kernel kernel) {
		super.unregister(kernel);
	}

	public static class CounterDataLoggerConfigBean
			extends MDRepositoryConfigBean<CounterDataLoggerRepositoryIfc> {

	}
}
