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
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import tigase.server.Packet;
import tigase.server.sreceiver.AbstractReceiverTask;
import tigase.xmpp.StanzaType;

/**
 * Created: Dec 6, 2008 8:12:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SystemMonitorTask extends AbstractReceiverTask {

	private static final String TASK_TYPE = "System Monitor";
	private static final String TASK_HELP =
		"This is a system monitor task." +
		" It monitor system resources usage and sends notifications" +
		" to subscribed users. It allos responds to your messages with" +
		" a simple reply message. This is to ensure the monitor works.";
	private static final long SECOND = 1000;
	private long interval = 10*SECOND;
	private int historySize = 100;

	private long lastCPUUsage = 0;
	private double[] cpuUsage = new double[historySize];
	private int cpuUsageIdx = 0;
	private double[] loadAverage = new double[historySize];
	private int loadAverageIdx = 0;
	private boolean cpuWarningSent = false;
	private boolean heapWarningSent = false;
	private boolean nonHeapWarningSent = false;

	private double recentCpu(int histCheck) {
		double recentCpu = 0;
		int idx = cpuUsageIdx;
		for (int i = 0; i < histCheck; i++) {
			idx -= i;
			if (idx < 0) {
				idx = cpuUsage.length;
			}
			recentCpu += cpuUsage[idx];
		}
		return recentCpu / histCheck;
	}

	private enum command { help; };

	private Timer tasks = null;

	@Override
	public void init(Queue<Packet> results) {
		super.init(results);
		tasks = new Timer("SystemMonitorTask", true);
		tasks.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				monitor();
			}
		}, interval, interval);
	}

	@Override
	public void destroy(Queue<Packet> results) {
		super.destroy(results);
		tasks.cancel();
		tasks = null;
	}

	public String getType() {
		return TASK_TYPE;
	}

	public String getHelp() {
		return TASK_HELP;
	}

	private String commandsHelp() {
		return "Available commands are:\n"
			+ "//help - display this help info\n";
	}

	private boolean isPostCommand(Packet packet) {
		String body = packet.getElemCData("/message/body");
		if (body != null) {
			for (command comm: command.values()) {
				if (body.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	private void runCommand(Packet packet, Queue<Packet> results) {
		String body = packet.getElemCData("/message/body");
		String[] body_split = body.split(" |\n|\r");
		command comm = command.valueOf(body_split[0].substring(2));
		switch (comm) {
		case help:
			results.offer(Packet.getMessage(packet.getElemFrom(),
					packet.getElemTo(), StanzaType.chat, commandsHelp(),
					"Commands description", null));
			break;
		}
	}

	@Override
	protected void processMessage(Packet packet, Queue<Packet> results) {
		if (isPostCommand(packet)) {
			runCommand(packet, results);
		} else {
			String body = packet.getElemCData("/message/body");
			results.offer(Packet.getMessage(packet.getElemFrom(),
					packet.getElemTo(), StanzaType.normal,
					"This is response to your message: [" + body + "]",
					"Response", null));
		}
	}

	private int setValueInArr(double[] arr, int idx, double val) {
		arr[idx] = val;
		if (idx >= (arr.length-1)) {
			idx = 0;
		} else {
			++idx;
		}
		return idx;
	}

	private void monitor() {
		ThreadMXBean thBean = ManagementFactory.getThreadMXBean();
		long cpuTime = 0;
		for (long thid : thBean.getAllThreadIds()) {
			cpuTime += thBean.getThreadCpuTime(thid);
		}
		long tmpCPU = lastCPUUsage;
		lastCPUUsage = cpuTime;
		cpuTime -= tmpCPU;
		double totalUsage = (new Long(cpuTime).doubleValue() / 1000000) /
						new Long(interval).doubleValue();
		cpuUsageIdx = setValueInArr(cpuUsage, cpuUsageIdx, totalUsage);
		loadAverageIdx = setValueInArr(loadAverage, loadAverageIdx,
						ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
		MemoryUsage heap =
						ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		MemoryUsage nonHeap =
						ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
		double used = new Long(heap.getUsed()).doubleValue();
		double max = new Long(heap.getMax()).doubleValue();
		double percentUsedHeap = used/max;
		long percentUsedNonHeap = (nonHeap.getUsed()*100)/nonHeap.getMax();
		Queue<Packet> results = new LinkedList<Packet>();
		if (percentUsedHeap > 0.9) {
			if (!heapWarningSent) {
				heapWarningSent = true;
				NumberFormat format = NumberFormat.getPercentInstance();
				format.setMaximumFractionDigits(2);
				Packet packet = Packet.getMessage("", getJID(), StanzaType.normal,
								"WARNING!\n" +
								"High usage of heap memory: " +
								format.format(percentUsedHeap) + " KB",
								"System Monitor Alert", null);
				processMessage(packet, results);
			}
		} else {
			heapWarningSent = false;
		}
		if (percentUsedNonHeap > 0.9) {
			if (!nonHeapWarningSent) {
				nonHeapWarningSent = true;
				NumberFormat format = NumberFormat.getPercentInstance();
				format.setMaximumFractionDigits(2);
				Packet packet = Packet.getMessage("", getJID(), StanzaType.normal,
								"WARNING!\n" +
								"High usage of non-heap memory: " +
								format.format(percentUsedNonHeap) + " KB",
								"System Monitor Alert", null);
				processMessage(packet, results);
			}
		} else {
			nonHeapWarningSent = false;
		}
		if ((totalUsage > 0.9) && (recentCpu(6) > 0.9)) {
			if (!cpuWarningSent) {
				cpuWarningSent = true;
				NumberFormat format = NumberFormat.getPercentInstance();
				format.setMaximumFractionDigits(2);
				Packet packet = Packet.getMessage("", getJID(), StanzaType.normal,
								"WARNING!\n" +
								"High CPU usage in last minute: " +
								format.format(recentCpu(6)) + " KB",
								"System Monitor Alert", null);
				processMessage(packet, results);
			}
		} else {
			cpuWarningSent = false;
		}
		for (Packet packet : results) {
			addOutPacket(packet);
		}
	}

}
