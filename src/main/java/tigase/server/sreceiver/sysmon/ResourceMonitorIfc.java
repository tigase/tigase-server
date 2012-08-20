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

package tigase.server.sreceiver.sysmon;

import java.util.Queue;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xmpp.JID;

/**
 * Created: Dec 10, 2008 12:12:27 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ResourceMonitorIfc {

	public static final long SECOND = 1000;
	public static final long MINUTE = 60*SECOND;
	public static final long INTERVAL_10SECS = 10 * SECOND;
	public static final long INTERVAL_1MIN = MINUTE;
	public static final long INTERVAL_1HOUR = 60 * MINUTE;
	public static final long INTERVAL_1DAY = 24 * INTERVAL_1HOUR;

	public void init(JID jid, float treshold, SystemMonitorTask smTask);

	public void destroy();

	public void check10Secs(Queue<Packet> results);

	public void check1Day(Queue<Packet> results);

	public void check1Hour(Queue<Packet> results);

	public void check1Min(Queue<Packet> results);

	public String getState();

	public String commandsHelp();

	public String runCommand(String[] command);

	public boolean isMonitorCommand(String command);

	void getStatistics(StatisticsList list);

}
