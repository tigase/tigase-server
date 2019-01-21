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
package tigase.server.monitor;

import tigase.annotations.TigaseDeprecated;
import tigase.conf.ConfigurationException;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.stats.StatisticsList;

import javax.script.Bindings;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created: Jun 17, 2010 10:14:23 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 * @deprecated Use {@link  tigase.monitor.MonitorComponent} instead.
 */
@Deprecated
@TigaseDeprecated(since = "8.0.0")
public class MonitorComponent
		extends AbstractMessageReceiver {

	private static final Logger log = Logger.getLogger(MonitorComponent.class.getName());

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);

		return defs;
	}

	@Override
	public String getDiscoCategoryType() {
		return "generic";
	}

	@Override
	public String getDiscoDescription() {
		return "Monitor";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
	}

	@Override
	public void processPacket(Packet packet) {
	}

	@Override
	public int processingInThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public int processingOutThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);
	}
}
