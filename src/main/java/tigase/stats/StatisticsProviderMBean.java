/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.stats;

import java.util.List;
import java.util.Map;

/**
 * Interface StatisticsProviderMBean
 *
 * @author kobit
 */
public interface StatisticsProviderMBean
{

//	/**
//	 * Operation exposed for management
//	 *
//	 * @return java.util.Map<String, String>
//	 */
//	public Map getAllStats();

	/**
	 * Get Attribute exposed for management
	 *
	 * @return
	 */
	public List getComponentsNames();

	/**
	 * Get Attribute exposed for management
	 *
	 * @return
	 */
	public String getName();

	/**
	 * Operation exposed for management
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	public Map<String, String> getAllStats(int level);

	/**
	 * Operation exposed for management
	 * @param compName The component name to provide statistics for
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	public Map<String, String> getComponentStats(String compName, int level);

  public int getCPUsNumber();

	public long getUptime();

	public long getProcesCPUTime();

	public int getConnectionsNumber();

	public int getClusterCacheSize();

	public int getQueueSize();

	public int getSMQueueSize();

	public int getCLQueueSize();

	public int getCLIOQueueSize();

	public long getQueueOverflow();

	public long getSMPacketsNumber();

	public long getClusterPackets();

	public long getMessagesNumber();

	public long getPresencesNumber();

	public float getSMPacketsNumberPerSec();

	public float getClusterPacketsPerSec();

	public float getMessagesNumberPerSec();

	public float getPresencesNumberPerSec();

	public long getIQOtherNumber();

	public float getIQOtherNumberPerSec();

	public long getIQAuthNumber();

	public float getCPUUsage();

	public float getHeapMemUsage();

	public String getSystemDetails();
	
}


