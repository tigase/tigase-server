/*
 * TigaseRuntime.java
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



package tigase.sys;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.monitor.MonitorRuntime;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created: Feb 19, 2009 12:15:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TigaseRuntime {
	/** Field description */
	protected static final long SECOND = 1000;
	private static final Logger log    = Logger.getLogger(TigaseRuntime.class.getName());

	/** Field description */
	protected static final long MINUTE = 60 * SECOND;

	/** Field description */
	protected static final long HOUR = 60 * MINUTE;

	//~--- fields ---------------------------------------------------------------

	private int              cpus        = Runtime.getRuntime().availableProcessors();
	private float            cpuUsage    = 0F;
	private MemoryPoolMXBean oldMemPool  = null;
	private long             prevCputime = 0;
	private long             prevUptime  = 0;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	protected TigaseRuntime() {
		List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();

		for (MemoryPoolMXBean memoryPoolMXBean : memPools) {
			if (memoryPoolMXBean.getName().toLowerCase().contains("old")) {
				oldMemPool = memoryPoolMXBean;
				log.info("Using OldGen memory pool for reporting memory usage.");

				break;
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cpuListener is a <code>CPULoadListener</code>
	 */
	public abstract void addCPULoadListener(CPULoadListener cpuListener);

	/**
	 * Method description
	 *
	 *
	 * @param memListener is a <code>MemoryChangeListener</code>
	 */
	public abstract void addMemoryChangeListener(MemoryChangeListener memListener);

	/**
	 * Method description
	 *
	 *
	 * @param onlineReporter is a <code>OnlineJidsReporter</code>
	 */
	public abstract void addOnlineJidsReporter(OnlineJidsReporter onlineReporter);

	/**
	 * Method description
	 *
	 *
	 * @param hook is a <code>ShutdownHook</code>
	 */
	public abstract void addShutdownHook(ShutdownHook hook);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid is a <code>JID</code>
	 *
	 * @return a value of <code>JID[]</code>
	 */
	public abstract JID[] getConnectionIdsForJid(JID jid);

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	public int getCPUsNumber() {
		return cpus;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>ResourceState</code>
	 */
	public ResourceState getCPUState() {
		return ResourceState.GREEN;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>float</code>
	 */
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

//  System.out.println("currUptime: " + currUptime +
//          "- prevUptime: " + prevUptime + " = elapsedTime: " + elapsedTime +
//          "\n, currCputime: " + currCputime +
//          " - prevCputime: " + prevCputime + " = elapsedCpu: " + elapsedCpu);
		return cpuUsage;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>float</code>
	 */
	public float getHeapMemUsage() {
		return (getHeapMemUsed() * 100F) / getHeapMemMax();
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

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>double</code>
	 */
	public double getLoadAverage() {
		return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>ResourceState</code>
	 */
	public ResourceState getMemoryState() {
		return ResourceState.GREEN;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getNonHeapMemMax() {
		return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>float</code>
	 */
	public float getNonHeapMemUsage() {
		return (getNonHeapMemUsed() * 100F) / getNonHeapMemMax();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getNonHeapMemUsed() {
		return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	public int getThreadsNumber() {
		return ManagementFactory.getThreadMXBean().getThreadCount();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>TigaseRuntime</code>
	 */
	public static TigaseRuntime getTigaseRuntime() {
		return MonitorRuntime.getMonitorRuntime();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getUptime() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public abstract boolean hasCompleteJidsInfo();

	/**
	 * Method description
	 *
	 *
	 * @param jid is a <code>JID</code>
	 *
	 * @return a value of <code>boolean</code>
	 */
	public abstract boolean isJidOnline(JID jid);
}


//~ Formatted in Tigase Code Convention on 13/11/29
