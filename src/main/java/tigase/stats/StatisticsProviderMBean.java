/*
 * StatisticsProviderMBean.java
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



package tigase.stats;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Interface StatisticsProviderMBean
 *
 * @author kobit
 */
public interface StatisticsProviderMBean {
	/**
	 * Operation exposed for management
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 *  java.util.Map<String, String>
	 *
	 * @return a value of Map<String,String>
	 */
	public Map<String, String> getAllStats(int level);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getCLIOQueueSize();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float[]
	 */
	public float[] getCLPacketsPerSecHistory();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getCLQueueSize();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getClusterCacheSize();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getClusterCompressionRatio();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getClusterNetworkBytes();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getClusterNetworkBytesPerSecond();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getClusterPackets();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getClusterPacketsPerSec();

///**
// * Operation exposed for management
// *
// *  java.util.Map<String, String>
// */
//public Map getAllStats();

	/**
	 * Get Attribute exposed for management
	 *
	 *
	 *
	 * @return a value of List<String>
	 */
	public List<String> getComponentsNames();

	/**
	 * Operation exposed for management
	 * @param compName The component name to provide statistics for
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 *  java.util.Map<String, String>
	 *
	 * @return a value of Map<String,String>
	 */
	public Map<String, String> getComponentStats(String compName, int level);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getConnectionsNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int[]
	 */
	public int[] getConnectionsNumberHistory();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getCPUsNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getCPUUsage();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float[]
	 */
	public float[] getCPUUsageHistory();

	/**
	 *
	 * @param statsKeys
	 *
	 *
	 * @return a value of Map<String,Object>
	 */
	public Map<String, Object> getCurStats(String[] statsKeys);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getHeapMemUsage();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float[]
	 */
	public float[] getHeapUsageHistory();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getIQAuthNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getIQOtherNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getIQOtherNumberPerSec();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getMessagesNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getMessagesNumberPerSec();

	/**
	 * Get Attribute exposed for management
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getName();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getNonHeapMemUsage();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getPresencesNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getPresencesNumberPerSec();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getProcesCPUTime();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getQueueOverflow();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getQueueSize();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getServerConnections();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int[]
	 */
	public int[] getServerConnectionsHistory();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getSMPacketsNumber();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getSMPacketsNumberPerSec();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of float[]
	 */
	public float[] getSMPacketsPerSecHistory();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getSMQueueSize();

	/**
	 * Method description
	 *
	 *
	 * @param statsKeys
	 *
	 * @return a value of Map<String,LinkedList<Object>>
	 */
	public Map<String, LinkedList<Object>> getStatsHistory(String[] statsKeys);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getSystemDetails();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getUptime();
}


//~ Formatted in Tigase Code Convention on 13/08/28
