/*
 * TigaseRuntime.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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



package tigase.sys;

import tigase.server.XMPPServer;
import tigase.server.monitor.MonitorRuntime;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.lang.management.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Feb 19, 2009 12:15:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public abstract class TigaseRuntime {
	protected static final long SECOND = 1000;
	private static final Logger log    = Logger.getLogger(TigaseRuntime.class.getName());
	protected static final long MINUTE = 60 * SECOND;
	protected static final long HOUR = 60 * MINUTE;

	//~--- fields ---------------------------------------------------------------

	private int              cpus        = Runtime.getRuntime().availableProcessors();
	private float            cpuUsage    = 0F;
	private MemoryPoolMXBean oldMemPool  = null;
	private Map<String,MemoryPoolMXBean> memoryPoolMXBeans  = null;
	private long             prevCputime = 0;
	private long             prevUptime  = 0;

	//~--- constructors ---------------------------------------------------------


	public Map<String, MemoryPoolMXBean> getMemoryPoolMXBeans() {
		return memoryPoolMXBeans;
	}

	public MemoryPoolMXBean getOldMemPool() {
		return oldMemPool;
	}

	protected TigaseRuntime() {
		List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();

		memoryPoolMXBeans = new LinkedHashMap<>(3);

		for (MemoryPoolMXBean memoryPoolMXBean : memPools) {
			if (memoryPoolMXBean.getName().toLowerCase().contains("old")) {
				oldMemPool = memoryPoolMXBean;

				memoryPoolMXBeans.put("old",memoryPoolMXBean);
				log.log(Level.INFO, "Using {0} memory pool for reporting (old) memory usage.", memoryPoolMXBean.getName());
			}
			if (memoryPoolMXBean.getName().toLowerCase().contains("survivor")) {
				memoryPoolMXBeans.put("survivor",memoryPoolMXBean);
				log.log(Level.INFO, "Using {0} memory pool for reporting survivor memory usage.", memoryPoolMXBean.getName());
			}
			if (memoryPoolMXBean.getName().toLowerCase().contains("eden")) {
				memoryPoolMXBeans.put("eden",memoryPoolMXBean);
				log.log(Level.INFO, "Using {0} memory pool for reporting eden memory usage.", memoryPoolMXBean.getName());
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	public abstract void addCPULoadListener(CPULoadListener cpuListener);

	public abstract void addMemoryChangeListener(MemoryChangeListener memListener);

	public abstract void addOnlineJidsReporter(OnlineJidsReporter onlineReporter);

	public abstract void addShutdownHook(ShutdownHook hook);

	//~--- get methods ----------------------------------------------------------

	public abstract JID[] getConnectionIdsForJid(JID jid);

	public int getCPUsNumber() {
		return cpus;
	}

	public ResourceState getCPUState() {
		return ResourceState.GREEN;
	}

	public float getCPUUsage() {
		long currCputime = -1;
		long elapsedCpu  = -1;
		long currUptime  = getUptime();
		long elapsedTime = currUptime - prevUptime;

		if ((prevUptime > 0L) && (elapsedTime > 500L)) {
			currCputime = getProcessCPUTime();
			elapsedCpu  = currCputime - prevCputime;
			cpuUsage    = Math.min(99.99F, elapsedCpu / (elapsedTime * 10000F * cpus));
		}
		if (elapsedTime > 500L) {
			prevUptime  = currUptime;
			prevCputime = currCputime;
		}

		return cpuUsage;
	}

	public long getDirectMemUsed() {
		long                   result   = -1;
		List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();

		for (MemoryPoolMXBean memoryPoolMXBean : memPools) {
			if (memoryPoolMXBean.getName().toLowerCase().contains("direct")) {
				result = memoryPoolMXBean.getUsage().getUsed();

				break;
			}
		}

		return result;
	}

	public String getGcStatistics() {

		// As this is variable and the collectors may change over time we
		// need to re-do it each time
		StringBuilder sb = new StringBuilder();
		List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		for ( GarbageCollectorMXBean gcBean : gcBeans ) {
			if (sb.length() > 0 ){
				sb.append('|');
			}
			sb.append('{');
			sb.append("name=");
			sb.append(gcBean.getName()).append(';');
			sb.append("count=").append(gcBean.getCollectionCount()).append(';');
			sb.append("time=").append(gcBean.getCollectionTime()).append(';');
			if (gcBean.getCollectionCount() > 0) {
				sb.append("avgTime=").append(gcBean.getCollectionTime() / gcBean.getCollectionCount()).append(';');
			} else {
				sb.append("avgTime=").append(0).append(';');
			}
			sb.append("pools=").append(Arrays.asList(gcBean.getMemoryPoolNames()));
			sb.append('}');
		}
		return sb.toString();

	}

	/**
	 * We try to return OLD memory pool size as this is what is the most interesting
	 * to us. If this is not possible then we return total Heap size.
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getHeapMemMax() {
		if (oldMemPool != null) {
			MemoryUsage memUsage = oldMemPool.getUsage();

			return memUsage.getMax();
		}

		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
	}

	public float getHeapMemUsage() {
		return  getHeapMemMax() == -1 ? -1.0F : (getHeapMemUsed() * 100F) / getHeapMemMax();
	}

	/**
	 * We try to return OLD memory pool size as this is what is the most interesting
	 * to us. If this is not possible then we return total Heap used.
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getHeapMemUsed() {
		if (oldMemPool != null) {
			MemoryUsage memUsage = oldMemPool.getUsage();

			return memUsage.getUsed();
		}

		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
	}

	public double getLoadAverage() {
		return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
	}

	public ResourceState getMemoryState() {
		return ResourceState.GREEN;
	}

	public long getNonHeapMemMax() {
		return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
	}

	public float getNonHeapMemUsage() {
		return getNonHeapMemMax() == -1 ? -1.0F : (getNonHeapMemUsed() * 100F) / getNonHeapMemMax();
	}

	public long getNonHeapMemUsed() {
		return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
	}

	public long getProcessCPUTime() {
		long                  result   = 0;
		OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

		if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {

			// The easy way if possible
			com.sun.management.OperatingSystemMXBean sunOSMXBean = (com.sun.management
					.OperatingSystemMXBean) osMXBean;

			result = sunOSMXBean.getProcessCpuTime();
		} else {

			// The hard way...
			ThreadMXBean thBean = ManagementFactory.getThreadMXBean();

			for (long thid : thBean.getAllThreadIds()) {
				result += thBean.getThreadCpuTime(thid);
			}
		}

		return result;
	}

	public int getThreadsNumber() {
		return ManagementFactory.getThreadMXBean().getThreadCount();
	}

	public static TigaseRuntime getTigaseRuntime() {
		return MonitorRuntime.getMonitorRuntime();
	}

	public long getUptime() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	public String getUptimeString() {
		long uptime  = ManagementFactory.getRuntimeMXBean().getUptime();
		long days    = uptime / (24 * HOUR);
		long hours   = (uptime - (days * 24 * HOUR)) / HOUR;
		long minutes = (uptime - (days * 24 * HOUR + hours * HOUR)) / MINUTE;
		long seconds = (uptime - (days * 24 * HOUR + hours * HOUR + minutes * MINUTE)) /
				SECOND;
		StringBuilder sb = new StringBuilder();

		sb.append((days > 0)
				? days + ((days == 1)
				? " day"
				: " days")
				: "");
		if (hours > 0) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(hours + ((hours == 1)
					? " hour"
					: " hours"));
		}
		if ((days == 0) && (minutes > 0)) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(minutes + ((minutes == 1)
					? " min"
					: " mins"));
		}
		if ((days == 0) && (hours == 0) && (seconds > 0)) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(seconds + " sec");
		}

		return sb.toString();
	}

	public abstract boolean hasCompleteJidsInfo();

	public abstract boolean isJidOnline(JID jid);
	
	public abstract boolean isJidOnlineLocally(BareJID jid);
	
	public abstract boolean isJidOnlineLocally(JID jid);

	public String getOldGenName() {
		return (oldMemPool != null ? oldMemPool.getName() : "n/a");
	}

	public void shutdownTigase(String[] msg) {
		shutdownTigase(msg,1);
	}

	public void shutdownTigase(String[] msg, int exitCode) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("\n");
		sb.append("\n");
		sb.append("\n");
		sb.append("  =============================================================================").append("\n");
		for (String line : msg) {
			sb.append("  ").append(line).append("\n");
		}
		sb.append("  =============================================================================").append("\n");
		sb.append("\n");
		sb.append("\n");
		sb.append("\n");
		sb.append("\n");

		if (XMPPServer.isOSGi()) {
			// for some reason System.out.println is not working in OSGi
			log.log(Level.SEVERE, sb.toString());
		} else {
			System.out.println(sb.toString());
		}

		System.exit(exitCode);
	}

	public static void main(String[] args) {
		final TigaseRuntime tigaseRuntime = getTigaseRuntime();
		tigaseRuntime.shutdownTigase(new String[] {"there", "was", "an", "error"});
	}

}
