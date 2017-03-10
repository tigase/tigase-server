
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.monitor;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.stats.StatisticsList;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

//~--- interfaces -------------------------------------------------------------

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
	 */
	public String commandsHelp();

	/**
	 * Method description
	 *
	 */
	public void destroy();

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getState();

	/**
	 * Method description
	 *
	 *
	 * @param command
	 *
	 * 
	 */
	public boolean isMonitorCommand(String command);

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param command
	 *
	 * 
	 */
	public String runCommand(String[] command);

	//~--- get methods ----------------------------------------------------------

	void getStatistics(StatisticsList list);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
