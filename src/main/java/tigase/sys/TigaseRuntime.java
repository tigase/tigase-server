/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.sys;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Set;
import tigase.monitor.MonitorRuntime;

/**
 * Created: Feb 19, 2009 12:15:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TigaseRuntime {

	protected static final long SECOND = 1000;
	protected static final long MINUTE = 60*SECOND;
	protected static final long HOUR = 60*MINUTE;

	private int cpus = Runtime.getRuntime().availableProcessors();
	private long prevUptime = 0;
	private long prevCputime = 0;
	private float cpuUsage = 0F;

	public static TigaseRuntime getTigaseRuntime() {
		return MonitorRuntime.getMonitorRuntime();
	}
	
	public abstract void addShutdownHook(ShutdownHook hook);

	public abstract void addMemoryChangeListener(MemoryChangeListener memListener);

	public abstract void addCPULoadListener(CPULoadListener cpuListener);

	public abstract void addOnlineJidsReporter(OnlineJidsReporter onlineReporter);

	public abstract boolean hasCompleteJidsInfo();

	public abstract boolean isJidOnline(String jid);

	public ResourceState getMemoryState() {
		return ResourceState.GREEN;
	}

	public ResourceState getCPUState() {
		return ResourceState.GREEN;
	}

	public long getProcessCPUTime() {
		long result = 0;
		OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
		if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
			// The easy way if possible
			com.sun.management.OperatingSystemMXBean sunOSMXBean =
							(com.sun.management.OperatingSystemMXBean)osMXBean;
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

	public long getUptime() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	public String getUptimeString() {
		long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
		long days = uptime / (24 * HOUR);
		long hours = (uptime - (days * 24 * HOUR)) / HOUR;
		long minutes = (uptime - (days * 24 * HOUR + hours * HOUR)) / MINUTE;
		long seconds =
			(uptime - (days * 24 * HOUR + hours * HOUR + minutes * MINUTE)) / SECOND;
		StringBuilder sb = new StringBuilder();
		sb.append(days > 0 ? days + " day, " : "");
		sb.append(hours > 0 ? hours + " hour, " : "");
		sb.append(minutes > 0 ? minutes + " min, " : "");
		sb.append(seconds > 0 ? seconds + " sec" : "");
		return sb.toString();
	}

	public int getCPUsNumber() {
		return cpus;
	}

	public float getCPUUsage() {
		long currCputime = -1;
		long elapsedCpu = -1;
		long currUptime = getUptime();
		long elapsedTime = currUptime - prevUptime;
		if (prevUptime > 0L && elapsedTime > 500L) {
			currCputime = getProcessCPUTime();
			elapsedCpu = currCputime - prevCputime;
			cpuUsage = Math.min(99.99F, elapsedCpu / (elapsedTime * 10000F * cpus));
		}
    if (elapsedTime > 500L) {
			prevUptime = currUptime;
			prevCputime = currCputime;
		}
//		System.out.println("currUptime: " + currUptime +
//						"- prevUptime: " + prevUptime + " = elapsedTime: " + elapsedTime +
//						"\n, currCputime: " + currCputime +
//						" - prevCputime: " + prevCputime + " = elapsedCpu: " + elapsedCpu);
		return cpuUsage;
	}

	public double getLoadAverage() {
		return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
	}

	public int getThreadsNumber() {
		return ManagementFactory.getThreadMXBean().getThreadCount();
	}

	public long getHeapMemMax() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
	}

	public long getHeapMemUsed() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
	}

	public float getHeapMemUsage() {
		return (getHeapMemUsed() * 100F) / getHeapMemMax();
	}

}
