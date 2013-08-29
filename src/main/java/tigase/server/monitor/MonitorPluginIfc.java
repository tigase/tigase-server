/*
 * MonitorPluginIfc.java
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

import tigase.server.Packet;

import tigase.stats.StatisticsList;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

/**
 * Created: Jun 17, 2010 11:59:13 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface MonitorPluginIfc {
	/**
	 * Method description
	 *
	 *
	 * @param results
	 */
	public void check10Secs(Queue<Packet> results);

	/**
	 * Method description
	 *
	 *
	 * @param results
	 */
	public void check1Day(Queue<Packet> results);

	/**
	 * Method description
	 *
	 *
	 * @param results
	 */
	public void check1Hour(Queue<Packet> results);

	/**
	 * Method description
	 *
	 *
	 * @param results
	 */
	public void check1Min(Queue<Packet> results);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String commandsHelp();

	/**
	 * Method description
	 *
	 */
	public void destroy();

	/**
	 * Method description
	 *
	 *
	 * @param command
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String runCommand(String[] command);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getState();

	/**
	 * Method description
	 *
	 *
	 * @param command
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isMonitorCommand(String command);

	/**
	 * Method description
	 *
	 *
	 * @param list is a <code>StatisticsList</code>
	 */
	void getStatistics(StatisticsList list);
}


//~ Formatted in Tigase Code Convention on 13/08/28
