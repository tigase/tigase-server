/*
 * ScheduledTask.java
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
package tigase.component;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.util.TimerTask;

import java.time.Duration;
import java.util.Collection;

/**
 * Abstract class extending TimerTask which allows easy configuration and usage of TimerTask as a bean
 *
 * Created by andrzej on 09.08.2016.
 */
public abstract class ScheduledTask extends TimerTask implements ConfigurationChangedAware, Initializable, UnregisterAware {

	@ConfigField(desc = "Execution period", alias = "period")
	private Duration period;

	@ConfigField(desc = "Delay", alias = "delay")
	private Duration delay;

	@Inject(bean = "service")
	private AbstractKernelBasedComponent component;

	public ScheduledTask(Duration delay, Duration period) {
		this.period = period;
		this.delay = delay;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (changedFields.contains("period") || changedFields.contains("delay")) {
			initialize();
		}
	}

	@Override
	public void initialize() {
		if (component == null || !component.isInitializationComplete()) {
			return;
		}

		cancel();

		if (delay == null && period == null)
			return;

		if (delay != null && delay.isZero() && period != null && period.isZero())
			return;

		if (delay != null) {
			if (period != null) {
				component.addTimerTask(this, delay.toMillis(), period.toMillis());
			} else {
				component.addTimerTask(this, delay.toMillis());
			}
		}
	}

	@Override
	public void beforeUnregister() {
		cancel();
	}
}
