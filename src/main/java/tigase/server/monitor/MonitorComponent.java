/*
 * MonitorComponent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
 *
 */



package tigase.server.monitor;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;

import tigase.stats.StatisticsList;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import java.util.Map;

import javax.script.Bindings;

/**
 * Created: Jun 17, 2010 10:14:23 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MonitorComponent
				extends AbstractMessageReceiver {
	private static final Logger log = Logger.getLogger(MonitorComponent.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param binds
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	@Override
	public int processingInThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	@Override
	public int processingOutThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 *
	 *
	 * @return a value of <code>Map<String,Object></code>
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoCategoryType() {
		return "generic";
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoDescription() {
		return "Monitor";
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
