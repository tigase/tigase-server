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
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.util.Queue;
import tigase.server.Packet;
import tigase.xmpp.StanzaType;

/**
 * Created: Dec 10, 2008 12:27:15 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CPUMonitor extends AbstractMonitor {

	private int historySize = 100;
	private long lastCPUUsage = 0;
	private double[] cpuUsage = new double[historySize];
	private int cpuUsageIdx = 0;
	private double[] loadAverage = new double[historySize];
	private int loadAverageIdx = 0;
	private ThreadMXBean thBean = null;
	private OperatingSystemMXBean osBean = null;
	private NumberFormat format = NumberFormat.getPercentInstance();

	@Override
	public void init(String jid, double treshold, SystemMonitorTask smTask) {
		super.init(jid, treshold, smTask);
		thBean = ManagementFactory.getThreadMXBean();
		osBean = ManagementFactory.getOperatingSystemMXBean();
		format.setMaximumFractionDigits(2);
	}


	@Override
	public void check10Secs(Queue<Packet> results) {
		long cpuTime = 0;
		for (long thid : thBean.getAllThreadIds()) {
			cpuTime += thBean.getThreadCpuTime(thid);
		}
		long tmpCPU = lastCPUUsage;
		lastCPUUsage = cpuTime;
		cpuTime -= tmpCPU;
		double totalUsage = (new Long(cpuTime).doubleValue() / 1000000) /
						new Long(INTERVAL_10SECS).doubleValue();
		cpuUsageIdx = setValueInArr(cpuUsage, cpuUsageIdx, totalUsage);
		loadAverageIdx = setValueInArr(loadAverage, loadAverageIdx,
						osBean.getSystemLoadAverage());
		if ((totalUsage > treshold) && (recentCpu(6) > treshold)) {
			prepareWarning("High CPU usage in last minute: " +
							format.format(recentCpu(6)), results, this);
		} else {
			prepareCalmDown("CPU usage is now low again: " + 
							format.format(totalUsage), results, this);
		}
	}

	private double recentCpu(int histCheck) {
		double recentCpu = 0;
		int idx = cpuUsageIdx;
		for (int i = 0; i < histCheck; i++) {
			idx -= i;
			if (idx < 0) {
				idx = cpuUsage.length-1;
			}
			recentCpu += cpuUsage[idx];
		}
		return recentCpu / histCheck;
	}

	public String getState() {
		int idx = cpuUsageIdx-1;
		if (idx < 0) {
			idx = cpuUsage.length-1;
		}
		NumberFormat formd = NumberFormat.getNumberInstance();
		formd.setMaximumFractionDigits(4);
		return "Current CPU usage is: " + format.format(cpuUsage[idx]) +
						", Last minute CPU usage is: " + format.format(recentCpu(6)) +
						", Load average is: " + formd.format(loadAverage[idx]);
	}

}
