/**
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

import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.TimerTaskService;
import tigase.util.common.TimerTask;

public abstract class AbstractConfigurableTimerTask
		extends AbstractConfigurableTask
		implements UnregisterAware {

	private final static String PERIOD_VAR = "x-task#period";
	private final TimerTask worker = new TimerTask() {

		@Override
		public void run() {
			AbstractConfigurableTimerTask.this.run();
		}
	};
	@ConfigField(desc = "Task execute period [ms]")
	private long period = 1000l;
	@Inject(bean = "timerTaskService")
	private TimerTaskService timerTaskService;

	@Override
	public void beforeUnregister() {
		setEnabled(false);
	}

	@Override
	public Form getCurrentConfiguration() {
		Form f = super.getCurrentConfiguration();

		f.addField(Field.fieldTextSingle(PERIOD_VAR, "" + period, "Period [ms]"));

		return f;
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long value) {
		if (this.period != value) {
			this.period = value;
			if (isEnabled()) {
				worker.cancel();
				timerTaskService.addTimerTask(worker, 1000l, period);
			}
		}
	}

	public TimerTaskService getTimerTaskService() {
		return timerTaskService;
	}

	public void setTimerTaskService(TimerTaskService timerTaskService) {
		this.timerTaskService = timerTaskService;
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field f = form.get(PERIOD_VAR);
		if (f != null) {
			long value = Long.parseLong(f.getValue());
			setPeriod(value);
		}

		super.setNewConfiguration(form);
	}

	@Override
	protected void disable() {
		super.disable();
		worker.cancel();
	}

	@Override
	protected void enable() {
		super.enable();
		timerTaskService.addTimerTask(worker, 1000l, period);
	}

	protected abstract void run();

}
