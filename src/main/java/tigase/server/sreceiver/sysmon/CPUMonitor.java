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

package tigase.server.sreceiver.sysmon;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import tigase.server.Packet;

/**
 * Created: Dec 10, 2008 12:27:15 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CPUMonitor extends AbstractMonitor {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
					Logger.getLogger(CPUMonitor.class.getName());

	private int historySize = 100;
	private long lastCpuUsage = 0;
	private long lastCpuChecked = 0;
	private double[] cpuUsage = new double[historySize];
	private int cpuUsageIdx = 0;
	private double[] loadAverage = new double[historySize];
	private int loadAverageIdx = 0;
	private ThreadMXBean thBean = null;
	private OperatingSystemMXBean osBean = null;
	private NumberFormat format = NumberFormat.getPercentInstance();

	private String checkForDeadLock() {
		long[] tids = thBean.findDeadlockedThreads();
		if (tids != null && tids.length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("Locked threads " + tids.length + ":\n");
			Set<Long> tidSet = new LinkedHashSet<Long>();
			for (long tid : tids) {
				tidSet.add(tid);
			}
			ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
			while (rootGroup.getParent() != null)
				rootGroup = rootGroup.getParent();
			int allThreadsCount = thBean.getThreadCount();
			Thread[] allThreads = new Thread[allThreadsCount];
			rootGroup.enumerate(allThreads, true);
			for (Thread thread : allThreads) {
				if (tidSet.contains(thread.getId())) {
					ThreadInfo threadInfo = thBean.getThreadInfo(thread.getId());
					sb.append("Locked thread [" + thread.getId() + "] " +
									threadInfo.getThreadName() + " on " +
									threadInfo.getLockInfo().toString() +
									", locked synchronizers: " +
									Arrays.toString(threadInfo.getLockedSynchronizers()) +
									", locked monitors: " +
									Arrays.toString(threadInfo.getLockedMonitors()) +
									" by [" + threadInfo.getLockOwnerId() + "] " +
									threadInfo.getLockOwnerName()).append('\n');
					StackTraceElement[] ste = thread.getStackTrace();
					for (StackTraceElement stackTraceElement : ste) {
						sb.append("  " + stackTraceElement.toString()).append('\n');
					}
				}
			}
			return sb.toString();
		}
		return null;
	}

	private enum command {
		maxthread(" - Returns information about the most active thread."),
		mth(" - Short version of the command above."),
		allthreads(" [ex] - display all threads information, with 'ex' parameters it prints extended information.");

		private String helpText = null;

		private command(String helpText) {
			this.helpText = helpText;
		}

		public String getHelp() {
			return helpText;
		}

	};

	private String getStackTrace(Map<Thread,StackTraceElement[]> map, long id) {
		for (Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
			if (entry.getKey().getId() == id) {
				StringBuilder sb = new StringBuilder();
				for (StackTraceElement stelem : entry.getValue()) {
					sb.append(stelem.toString() + "\n");
				}
				return sb.toString();
			}
		}
		return null;
	}

	private String getThreadInfo(long thid, boolean stack) {
		ThreadInfo ti = thBean.getThreadInfo(thid);
		if (ti != null) {
			Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
			long maxCpu = thBean.getThreadCpuTime(thid);
			double totalUsage = (new Long(maxCpu).doubleValue() / 1000000) /
							new Long(System.currentTimeMillis()).doubleValue();
			StringBuilder sb =
							new StringBuilder("Thread: " + ti.getThreadName() +
							", ID: " + ti.getThreadId());
			sb.append(", CPU usage: " + format.format(totalUsage) + "\n");
			if (stack) {
				sb.append(ti.toString());
				sb.append(getStackTrace(map, thid));
			}
			return sb.toString();
		} else {
			return "ThreadInfo is null...";
		}
	}

	@Override
	public String runCommand(String[] com) {
		command comm = command.valueOf(com[0].substring(2));
		switch (comm) {
			case mth:
			case maxthread:
				if (com.length > 1) {
					try {
						long thid = Long.parseLong(com[1]);
						return getThreadInfo(thid, true);
					} catch (Exception e) {
						return "Incorrect Thread ID";
					}
				}
				long maxCpu = 0;
				long id = 0;
				for (long thid : thBean.getAllThreadIds()) {
					if (thBean.getThreadCpuTime(thid) >= maxCpu) {
						maxCpu = thBean.getThreadCpuTime(thid);
						id = thid;
					}
				}
				return getThreadInfo(id, true);
			case allthreads:
				boolean extend = false;
				if (com.length > 1 && com[1].equals("ex")) {
					extend = true;
				}
				StringBuilder sb = new StringBuilder("All threads information:\n");
				for (long thid : thBean.getAllThreadIds()) {
					sb.append(getThreadInfo(thid, extend));
				}
				return sb.toString();
		}
		return null;
	}

	@Override
	public String commandsHelp() {
		StringBuilder sb = new StringBuilder();
		for (command comm : command.values()) {
			sb.append("//" + comm.name() + comm.getHelp() + "\n");
		}
		return sb.toString();
	}

	@Override
	public boolean isMonitorCommand(String com) {
		if (com != null) {
			for (command comm: command.values()) {
				if (com.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void init(String jid, double treshold, SystemMonitorTask smTask) {
		super.init(jid, treshold, smTask);
		thBean = ManagementFactory.getThreadMXBean();
		osBean = ManagementFactory.getOperatingSystemMXBean();
		format.setMaximumFractionDigits(2);
		if (thBean.isCurrentThreadCpuTimeSupported()) {
			thBean.setThreadCpuTimeEnabled(true);
		} else {
			log.warning("Current thread CPU Time is NOT supported.");
		}
		if (thBean.isThreadContentionMonitoringSupported()) {
			thBean.setThreadContentionMonitoringEnabled(true);
		} else {
			log.warning("Thread contention monitoring is NOT supported.");
		}
	}

	@Override
	public void check10Secs(Queue<Packet> results) {
		long cpuTime = 0;
		for (long thid : thBean.getAllThreadIds()) {
			cpuTime += thBean.getThreadCpuTime(thid);
		}
		long tmpCPU = lastCpuUsage;
		lastCpuUsage = cpuTime;
		cpuTime -= tmpCPU;
		double totalUsage = (new Long(cpuTime).doubleValue() / 1000000) /
						new Long(System.currentTimeMillis() - lastCpuChecked).doubleValue();
		lastCpuChecked = System.currentTimeMillis();
		cpuUsageIdx = setValueInArr(cpuUsage, cpuUsageIdx, totalUsage);
		loadAverageIdx = setValueInArr(loadAverage, loadAverageIdx,
						osBean.getSystemLoadAverage());
		if ((totalUsage > treshold) && (recentCpu(6) > treshold)) {
			prepareWarning("High CPU usage, current: " + format.format(totalUsage) +
							", last minute: " +
							format.format(recentCpu(6)), results, this);
		} else {
			if (totalUsage < (treshold * 0.75)) {
				prepareCalmDown("CPU usage is now low again, current: " +
								format.format(totalUsage) + ", last minute: " +
								format.format(recentCpu(6)), results, this);
			}
		}
		String result = checkForDeadLock();
		if (result != null) {
			System.out.println("Dead-locked threads:\n" + result);
			prepareWarning("Dead-locked threads:\n" + result, results, this);
		}
	}

	private double recentCpu(int histCheck) {
		double recentCpu = 0;
		int start = cpuUsageIdx - histCheck;
		if (start < 0) {
			start = cpuUsage.length - start;
		}
		for (int i = 0; i < histCheck; i++) {
			int idx = (start + i) % cpuUsage.length;
			recentCpu += cpuUsage[idx];
		}
		return recentCpu / histCheck;
	}

	@Override
	public String getState() {
		int idx = cpuUsageIdx-1;
		if (idx < 0) {
			idx = cpuUsage.length-1;
		}
		NumberFormat formd = NumberFormat.getNumberInstance();
		formd.setMaximumFractionDigits(4);
		return "Current CPU usage is: " + format.format(cpuUsage[idx]) +
						", Last minute CPU usage is: " + format.format(recentCpu(6)) +
						", Load average is: " + formd.format(loadAverage[idx]) + "\n";
	}

	@Override
	public void destroy() {
		// Nothing to destroy
	}

}
