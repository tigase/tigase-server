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

import java.util.Map;

@Bean(name = "load-checker-task", parent = MonitorComponent.class, active = true)
public class LoadCheckerTask
		extends AbstractConfigurableTimerTask
		implements InfoTask, Initializable {

	public static final String MONITOR_EVENT_NAME = "tigase.monitor.tasks.LoadAverageMonitorEvent";
	private final static TimestampHelper dtf = new TimestampHelper();
	private boolean triggered = false;
	@ConfigField(desc = "Average Load Threshold")
	private long averageLoadThreshold = 10;
	@Inject
	private MonitorComponent component;
	@Inject
	private EventBus eventBus;
	@Inject
	private MonitorRuntime runtime;

	public long getAverageLoadThreshold() {
		return averageLoadThreshold;
	}

	public void setAverageLoadThreshold(Long averageLoadThreshold) {
		this.averageLoadThreshold = averageLoadThreshold;
	}

	@Override
	public Form getCurrentConfiguration() {
		Form form = super.getCurrentConfiguration();

		form.addField(Field.fieldTextSingle("averageLoadThreshold", String.valueOf(averageLoadThreshold),
											"Alarm when AverageLoad is bigger than"));

		return form;
	}

	@Override
	public Form getTaskInfo() {
		Form result = new Form("", "Load Information", "");
		result.addField(
				Field.fieldTextSingle("averageLoad", Double.toString(runtime.getLoadAverage()), "Load Average"));

		return result;
	}

	@Override
	public void initialize() {
		super.initialize();
		eventBus.registerEvent(LoadCheckerTaskEvent.class, "Fired when load is too high", false);
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field tmp = form.get("averageLoadThreshold");
		if (tmp != null) {
			this.averageLoadThreshold = Long.parseLong(tmp.getValue());
		}

		super.setNewConfiguration(form);
	}

	@Override
	protected void run() {
		double curAverageLoad = runtime.getLoadAverage();
		if (curAverageLoad >= averageLoadThreshold) {
			final LoadCheckerTaskEvent event = new LoadCheckerTaskEvent(curAverageLoad);
			if (!triggered) {
				eventBus.fire(event);
				triggered = true;
			}
		} else {
			triggered = false;
		}
	}

	static class LoadCheckerTaskEvent
			extends TasksEvent {

		double averageLoad;

		/**
		 * Empty constructor to be able to serialize/deserialize event
		 */
		public LoadCheckerTaskEvent(String name, String description) {
			super(name, description);
		}

		public LoadCheckerTaskEvent(double averageLoad) {
			super("DiskUsageEvent", "Fired when load is too high");
			this.averageLoad = averageLoad;
		}

		@Override
		public Map<String, String> getAdditionalData() {
			// @formatter:off
			return Map.of("averageLoad", "" + averageLoad);
			// @formatter:onn
		}

		public double getAverageLoad() {
			return averageLoad;
		}
	}
}
