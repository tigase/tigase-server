/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.monitor.tasks;

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.InfoTask;
import tigase.monitor.MonitorComponent;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.datetime.TimestampHelper;
import tigase.xml.Element;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;

@Bean(name = "memory-checker-task", parent = MonitorComponent.class, active = true)
public class MemoryCheckerTask
		extends AbstractConfigurableTimerTask
		implements InfoTask, Initializable {

	public final static String HEAP_MEMORY_MONITOR_EVENT_NAME = "tigase.monitor.tasks.HeapMemoryMonitorEvent";
	public final static String NONHEAP_MEMORY_MONITOR_EVENT_NAME = "tigase.monitor.tasks.NonHeapMemoryMonitorEvent";
	private final static TimestampHelper dtf = new TimestampHelper();
	private final HashSet<String> triggeredEvents = new HashSet<>();
	@Inject
	private MonitorComponent component;
	@Inject
	private EventBus eventBus;
	/**
	 * Percent
	 */
	@ConfigField(desc = "Max Heap Mem Usage Threshold [%]")
	private int maxHeapMemUsagePercentThreshold = 90;
	/**
	 * Percent
	 */
	@ConfigField(desc = "Max Non-Heap Mem Usage Threshold [%]")
	private int maxNonHeapMemUsagePercentThreshold = 90;
	@Inject
	private MonitorRuntime runtime;

	@Override
	public Form getCurrentConfiguration() {
		Form form = super.getCurrentConfiguration();

		form.addField(Field.fieldTextSingle("maxHeapMemUsagePercentThreshold",
											String.valueOf(maxHeapMemUsagePercentThreshold),
											"Alarm when heap mem usage is bigger than [%]"));

		form.addField(Field.fieldTextSingle("maxNonHeapMemUsagePercentThreshold",
											String.valueOf(maxNonHeapMemUsagePercentThreshold),
											"Alarm when non-heap mem usage is bigger than [%]"));

		return form;
	}

	public int getMaxHeapMemUsagePercentThreshold() {
		return maxHeapMemUsagePercentThreshold;
	}

	public void setMaxHeapMemUsagePercentThreshold(Integer maxHeapMemUsagePercentThreshold) {
		this.maxHeapMemUsagePercentThreshold = maxHeapMemUsagePercentThreshold;
	}

	public int getMaxNonHeapMemUsagePercentThreshold() {
		return maxNonHeapMemUsagePercentThreshold;
	}

	public void setMaxNonHeapMemUsagePercentThreshold(Integer maxNonHeapMemUsagePercentThreshold) {
		this.maxNonHeapMemUsagePercentThreshold = maxNonHeapMemUsagePercentThreshold;
	}

	@Override
	public Form getTaskInfo() {
		Form result = new Form("", "Memory Information", "");
		result.addField(Field.fieldTextSingle("heapMemMax", Long.toString(runtime.getHeapMemMax()), "Heap Memory Max"));
		result.addField(
				Field.fieldTextSingle("heapMemUsed", Long.toString(runtime.getHeapMemUsed()), "Heap Memory Used"));
		result.addField(Field.fieldTextSingle("heapMemUsedPercentage", Float.toString(runtime.getHeapMemUsage()),
											  "Heap Memory Used [%]"));
		result.addField(Field.fieldTextSingle("nonHeapMemMax", Long.toString(runtime.getNonHeapMemMax()),
											  "Non-Heap Memory Max"));
		result.addField(Field.fieldTextSingle("nonHeapMemUsed", Long.toString(runtime.getNonHeapMemUsed()),
											  "Non-Heap Memory Used"));
		result.addField(Field.fieldTextSingle("nonHeapMemUsedPercentage", Float.toString(runtime.getNonHeapMemUsage()),
											  "Non-Heap Memory Used [%]"));
		result.addField(Field.fieldTextSingle("directMemUsed", Long.toString(runtime.getDirectMemUsed()),
											  "Direct Memory Used"));

		return result;
	}

	@Override
	public void initialize() {
		super.initialize();
		eventBus.registerEvent(MemoryCheckerTaskEvent.class, "Fired when HEAP memory is too low", false);
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field heapMemUsage = form.get("maxHeapMemUsagePercentThreshold");
		if (heapMemUsage != null) {
			this.maxHeapMemUsagePercentThreshold = Integer.parseInt(heapMemUsage.getValue());
		}
		Field nonHeapMemUsage = form.get("maxNonHeapMemUsagePercentThreshold");
		if (nonHeapMemUsage != null) {
			this.maxNonHeapMemUsagePercentThreshold = Integer.parseInt(nonHeapMemUsage.getValue());
		}
		super.setNewConfiguration(form);
	}

	@Override
	protected void run() {
		float curHeapMemUsagePercent = runtime.getHeapMemUsage();
		float curNonHeapMemUsagePercent = runtime.getNonHeapMemUsage();
		if (curHeapMemUsagePercent >= maxHeapMemUsagePercentThreshold) {
			MemoryCheckerTaskEvent event = new MemoryCheckerTaskEvent(HEAP_MEMORY_MONITOR_EVENT_NAME,
																	  "Fired when HEAP memory is too low",
																	  curHeapMemUsagePercent, curNonHeapMemUsagePercent,
																	  runtime.getHeapMemMax(), runtime.getHeapMemUsed(),
																	  runtime.getNonHeapMemMax(),
																	  runtime.getNonHeapMemUsed(),
																	  runtime.getDirectMemUsed(),
																	  "Heap memory usage is higher than " + maxHeapMemUsagePercentThreshold +
											  								 " and it equals " + curHeapMemUsagePercent);

			if (!triggeredEvents.contains(event.getName())) {
				eventBus.fire(event);
				triggeredEvents.add(event.getName());
			}
		} else {
			triggeredEvents.remove(HEAP_MEMORY_MONITOR_EVENT_NAME);
		}

		if (curNonHeapMemUsagePercent >= maxNonHeapMemUsagePercentThreshold) {
			MemoryCheckerTaskEvent event = new MemoryCheckerTaskEvent(NONHEAP_MEMORY_MONITOR_EVENT_NAME,
																	  "Fired when Non-HEAP memory is too low",
																	  curHeapMemUsagePercent, curNonHeapMemUsagePercent,
																	  runtime.getHeapMemMax(), runtime.getHeapMemUsed(),
																	  runtime.getNonHeapMemMax(),
																	  runtime.getNonHeapMemUsed(),
																	  runtime.getDirectMemUsed(),
																	  "Heap memory usage is higher than " + maxNonHeapMemUsagePercentThreshold +
																			  " and it equals " + curNonHeapMemUsagePercent);
			if (!triggeredEvents.contains(event.getName())) {
				eventBus.fire(event);
				triggeredEvents.add(event.getName());
			}
		} else {
			triggeredEvents.remove(NONHEAP_MEMORY_MONITOR_EVENT_NAME);
		}
	}
	static class MemoryCheckerTaskEvent
			extends TasksEvent {

		float heapMemUsage;
		float nonHeapMemUsage;
		long heapMemMax;
		long heapMemUsed;
		long nonHeapMemMax;
		long nonHeapMemUsed;
		long directMemUsed;
		String message;

		public MemoryCheckerTaskEvent(String name, String description, float heapMemUsage, float nonHeapMemUsage,
									  long heapMemMax, long heapMemUsed, long nonHeapMemMax, long nonHeapMemUsed,
									  long directMemUsed, String message) {
			super(name, description);
			this.heapMemUsage = heapMemUsage;
			this.nonHeapMemUsage = nonHeapMemUsage;
			this.heapMemMax = heapMemMax;
			this.heapMemUsed = heapMemUsed;
			this.nonHeapMemMax = nonHeapMemMax;
			this.nonHeapMemUsed = nonHeapMemUsed;
			this.directMemUsed = directMemUsed;
			this.message = message;

		}

		@Override
		public Map<String, String> getAdditionalData() {
			// @formatter:off
			return Map.of(
				"heapMemUsage", Float.toString(heapMemUsage),
				"nonHeapMemUsage", Float.toString(nonHeapMemUsage),
				"heapMemMax", Long.toString(heapMemMax),
				"heapMemUsed", Long.toString(heapMemUsed),
				"nonHeapMemMax", Long.toString(nonHeapMemMax),
				"nonHeapMemUsed", Long.toString(nonHeapMemUsed),
				"directMemUsed", Long.toString(directMemUsed),
				"message", message
			);
			// @formatter:onn
		}

		public float getHeapMemUsage() {
			return heapMemUsage;
		}

		public float getNonHeapMemUsage() {
			return nonHeapMemUsage;
		}

		public long getHeapMemMax() {
			return heapMemMax;
		}

		public long getHeapMemUsed() {
			return heapMemUsed;
		}

		public long getNonHeapMemMax() {
			return nonHeapMemMax;
		}

		public long getNonHeapMemUsed() {
			return nonHeapMemUsed;
		}

		public long getDirectMemUsed() {
			return directMemUsed;
		}

		public String getMessage() {
			return message;
		}
	}
}
