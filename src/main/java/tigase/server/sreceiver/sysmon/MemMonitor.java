/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.sreceiver.sysmon;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xmpp.JID;

/**
 * Created: Dec 10, 2008 1:23:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MemMonitor extends AbstractMonitor
				implements NotificationListener {

	private static Logger log =
					Logger.getLogger("tigase.server.sreceiver.sysmon.MemMonitor");

	private MemoryMXBean memoryMXBean = null;

	@Override
	public void destroy() {
		memoryMXBean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter)memoryMXBean;
		List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
		for (MemoryPoolMXBean memoryPoolMXBean : memPools) {
			try {
				emitter.removeNotificationListener(this, null, memoryPoolMXBean);
			} catch (Exception e) {	}
		}
	}

	@Override
	public void init(JID jid, float treshold, SystemMonitorTask smTask) {
		super.init(jid, treshold, smTask);
		memoryMXBean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter)memoryMXBean;
		List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
		for (MemoryPoolMXBean memoryPoolMXBean : memPools) {
			try {
				emitter.removeNotificationListener(this, null, memoryPoolMXBean);
			} catch (Exception e) {	}
			MemoryUsage memUsage = memoryPoolMXBean.getUsage();
			if (memUsage != null) {
				if (memoryPoolMXBean.isUsageThresholdSupported()) {
					emitter.addNotificationListener(this, null, memoryPoolMXBean);
					long memUsageTreshold =
									new Double(new Long(memUsage.getMax()).doubleValue() *
									treshold).longValue();
					memoryPoolMXBean.setUsageThreshold(memUsageTreshold);
					log.config("Setting treshold: " + memUsageTreshold +
									" for memory pool: " + memoryPoolMXBean.getName() +
									", type: " + memoryPoolMXBean.getType().toString() +
									", memMax: " + memUsage.getMax() +
									", memUsed: " + memUsage.getUsed() +
									", config treeshold: " + treshold);
					if (memUsage.getUsed() > memUsageTreshold) {
						Notification not = new Notification(
										MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
										this, 1);
						handleNotification(not, memoryPoolMXBean);
					}
				} else {
					log.config("Memory pool name: " + memoryPoolMXBean.getName() +
									", type: " + memoryPoolMXBean.getType().toString() +
									" usage threshold is not supported.");
				}
			} else {
				log.config("Memory pool name: " + memoryPoolMXBean.getName() +
								", type: " + memoryPoolMXBean.getType().toString() +
								" is invalid.");
			}
		}
	}

	@Override
	public void handleNotification(Notification note, Object handback) {
		if (note.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
			log.info("Usage threshold exceeded, sending notification.");
			NumberFormat format = NumberFormat.getIntegerInstance();
			if (format instanceof DecimalFormat) {
				DecimalFormat decf = (DecimalFormat) format;
				decf.applyPattern(decf.toPattern() + " KB");
			}
			MemoryPoolMXBean memoryPoolMXBean = (MemoryPoolMXBean) handback;
			String message = "Threshold " +
							format.format(memoryPoolMXBean.getUsageThreshold() / 1024) +
							" for memory pool: " + memoryPoolMXBean.getName() +
							", type: " + memoryPoolMXBean.getType().toString() +
							" exceeded.";
			sendWarningOut(message, handback);
		}
	}

	@Override
	public String getState() {
		NumberFormat format = NumberFormat.getIntegerInstance();
		if (format instanceof DecimalFormat) {
			DecimalFormat decf = (DecimalFormat) format;
			decf.applyPattern(decf.toPattern() + " KB");
		}
		NumberFormat formp = NumberFormat.getPercentInstance();
		formp.setMaximumFractionDigits(2);
		StringBuilder sb = new StringBuilder();
		List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
		for (MemoryPoolMXBean memoryPoolMXBean : memPools) {
			MemoryUsage memUsage = memoryPoolMXBean.getUsage();
			if (memUsage != null) {
				sb.append("Memory pool: " + memoryPoolMXBean.getName() +
								", type: " + memoryPoolMXBean.getType().toString() +
								", usage: " + format.format(memUsage.getUsed()/1024) +
								" of " + format.format(memUsage.getMax()/1024) +
								" - " +
								formp.format(new Long(memUsage.getUsed()).doubleValue()/
								new Long(memUsage.getMax()).doubleValue()));
				if (memoryPoolMXBean.isUsageThresholdSupported()) {
					sb.append(", treshold: " +
									format.format(memoryPoolMXBean.getUsageThreshold() / 1024));
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@Override
	public void getStatistics(StatisticsList list) {
    super.getStatistics(list);
	}

	private static int GC_INTERVAL = 40;
	private int gc_cnt = new Random(System.currentTimeMillis()).nextInt(GC_INTERVAL);
	@Override
	public void check1Min(Queue<Packet> results) {
		if (++gc_cnt >= GC_INTERVAL) {
			Runtime.getRuntime().gc();
			gc_cnt = 0;
		}
	}



}
