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

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//~--- interfaces -------------------------------------------------------------

/**
 * Interface StatisticsProviderMBean
 *
 * @author kobit
 */
public interface StatisticsProviderMBean {

	/**
	 * Operation exposed for management
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	public Map<String, String> getAllStats(int level);

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getCLIOQueueSize();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float[] getCLPacketsPerSecHistory();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getCLQueueSize();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getCPUUsage();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float[] getCPUUsageHistory();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getCPUsNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getClusterCacheSize();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getClusterCompressionRatio();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getClusterNetworkBytes();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getClusterNetworkBytesPerSecond();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getClusterPackets();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getClusterPacketsPerSec();

	/**
	 * Operation exposed for management
	 * @param compName The component name to provide statistics for
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	public Map<String, String> getComponentStats(String compName, int level);

///**
// * Operation exposed for management
// *
// * @return java.util.Map<String, String>
// */
//public Map getAllStats();

	/**
	 * Get Attribute exposed for management
	 *
	 * @return
	 */
	public List<String> getComponentsNames();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getConnectionsNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int[] getConnectionsNumberHistory();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getHeapMemUsage();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float[] getHeapUsageHistory();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getIQAuthNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getIQOtherNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getIQOtherNumberPerSec();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getMessagesNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getMessagesNumberPerSec();

	/**
	 * Get Attribute exposed for management
	 *
	 * @return
	 */
	public String getName();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getNonHeapMemUsage();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getPresencesNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getPresencesNumberPerSec();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getProcesCPUTime();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getQueueOverflow();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getQueueSize();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getSMPacketsNumber();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float getSMPacketsNumberPerSec();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public float[] getSMPacketsPerSecHistory();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getSMQueueSize();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getServerConnections();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int[] getServerConnectionsHistory();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getSystemDetails();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getUptime();
	
	public Map<String, LinkedList<Object>> getStatsHistory(String[] statsKeys);

	/**
	 * @param array
	 * @return
	 */
	public Map<String, Object> getCurStats(String[] statsKeys);

}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
