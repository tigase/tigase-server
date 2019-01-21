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
package tigase.stats;

import tigase.db.util.RepositoryVersionAware;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.stats.db.CounterDataLoggerRepoBean;
import tigase.util.dns.DNSResolverFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Created: Apr 20, 2010 6:39:05 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = "counter-data-logger", parent = StatisticsCollector.class, active = false)
public class CounterDataLogger
		implements StatisticsArchivizerIfc, ConfigurationChangedAware, Initializable, RegistrarBean {

	private static final Logger log = Logger.getLogger(CounterDataLogger.class.getName());
	protected static String defaultHostname;
	@ConfigField(desc = "Frequency")
	private long frequency = -1;

	private long last_c2s_packets = 0;
	private long last_ext_packets = 0;
	private long last_iqs = 0;
	private long last_messages = 0;
	private long last_muc_packets = 0;
	private long last_presences = 0;
	private long last_pubsub_packets = 0;
	private long last_s2s_packets = 0;
	private long last_sm_packets = 0;
	private long last_ws2s_packets = 0;
	@Inject
	private CounterDataLoggerRepoBean repository;

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		defaultHostname = DNSResolverFactory.getInstance().getDefaultHost();
	}

	@Override
	public void execute(StatisticsProvider sp) {
		long c2s_packets = sp.getCompPackets("c2s");
		long ws2s_packets = sp.getCompPackets("ws2s");
		long ext_packets = sp.getCompPackets("ext");
		long iqs = sp.getCompIqs("sess-man");
		long messages = sp.getCompMessages("sess-man");
		long muc_packets = sp.getCompPackets("muc");
		long presences = sp.getCompPresences("sess-man");
		long pubsub_packets = sp.getCompPackets("pubsub");
		long s2s_packets = sp.getCompPackets("s2s");
		long sm_packets = sp.getSMPacketsNumber();
		int sm_connections = sp.getStats("sess-man", "Open user connections", 0);
		int sm_sessions = sp.getStats("sess-man", "Open user sessions", 0);

		repository.addStatsLogEntry(defaultHostname,
		                            sp.getCPUUsage(),
		                            sp.getHeapMemUsage(),
		                            sp.getUptime(),
		                            sp.getStats("vhost-man", "Number of VHosts", 0),
		                            sm_packets - last_sm_packets,
		                            muc_packets - last_muc_packets,
		                            pubsub_packets - last_pubsub_packets,
		                            c2s_packets - last_c2s_packets,
		                            ws2s_packets - last_ws2s_packets,
		                            s2s_packets - last_s2s_packets,
		                            ext_packets - last_ext_packets,
		                            presences - last_presences,
		                            messages - last_messages, iqs - last_iqs,
		                            sp.getRegistered(),
		                            sp.getCompConnections("c2s"),
		                            sp.getCompConnections("ws2s"),
		                            sp.getCompConnections("bosh"),
		                            sp.getCompConnections("s2s"),
		                            sm_connections,
		                            sm_sessions);
		last_c2s_packets = c2s_packets;
		last_ws2s_packets = ws2s_packets;
		last_ext_packets = ext_packets;
		last_iqs = iqs;
		last_messages = messages;
		last_muc_packets = muc_packets;
		last_presences = presences;
		last_pubsub_packets = pubsub_packets;
		last_s2s_packets = s2s_packets;
		last_sm_packets = sm_packets;
	}

	@Override
	public long getFrequency() {
		return frequency;
	}

	@Override
	public void initialize() {
		beanConfigurationChanged(Collections.emptyList());
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public void release() {
	}

}
