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
	 * @return {@code java.util.Map<String, String>}
	 */
	public Map<String, String> getAllStats(int level);

	public int getCLIOQueueSize();

	public float[] getCLPacketsPerSecHistory();

	public int getCLQueueSize();

	public int getClusterCacheSize();

	public float getClusterCompressionRatio();

	public long getClusterNetworkBytes();

	public float getClusterNetworkBytesPerSecond();

	public long getClusterPackets();

	public float getClusterPacketsPerSec();


	/**
	 * Get Attribute exposed for management
	 *
	 *
	 *
	 * @return a value of {@code List<String>}
	 */
	public List<String> getComponentsNames();

	/**
	 * Operation exposed for management
	 * @param compName The component name to provide statistics for
	 * @param level Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return {@code java.util.Map<String, String>}
	 */
	public Map<String, String> getComponentStats(String compName, int level);

	public int getConnectionsNumber();

	public int[] getConnectionsNumberHistory();

	public int getCPUsNumber();

	public float getCPUUsage();

	public float[] getCPUUsageHistory();

	public Map<String, Object> getCurStats(String[] statsKeys);

	public long getDirectMemUsed();

	public long[] getDirectMemUsedHistory();

	public float getHeapMemUsage();

	public float[] getHeapUsageHistory();

	public long getIQAuthNumber();

	public long getIQOtherNumber();

	public float getIQOtherNumberPerSec();

	public long getMessagesNumber();

	public float getMessagesNumberPerSec();

	public String getName();

	public float getNonHeapMemUsage();

	public long getPresencesNumber();

	public float getPresencesNumberPerSec();

	public long getProcesCPUTime();

	public long getQueueOverflow();

	public int getQueueSize();

	public int getServerConnections();

	public int[] getServerConnectionsHistory();

	public long getSMPacketsNumber();

	public float getSMPacketsNumberPerSec();

	public float[] getSMPacketsPerSecHistory();

	public int getSMQueueSize();

	public Map<String, LinkedList<Object>> getStatsHistory(String[] statsKeys);

	public String getSystemDetails();

	public long getUptime();
}
