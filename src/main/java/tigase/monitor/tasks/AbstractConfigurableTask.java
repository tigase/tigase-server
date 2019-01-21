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
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.ConfigurableTask;
import tigase.monitor.MonitorTask;

public abstract class AbstractConfigurableTask
		implements MonitorTask, ConfigurableTask, UnregisterAware {

	private final static String ENABLED_VAR = "x-task#enabled";

	@ConfigField(desc = "Enable task")
	private boolean enabled = false;

	@Override
	public void beforeUnregister() {
		setEnabled(false);
	}

	@Override
	public Form getCurrentConfiguration() {
		Form f = new Form("", "Task Configuration", "");

		f.addField(Field.fieldBoolean(ENABLED_VAR, enabled, "Enabled"));
		// f.addField(Field.fieldTextSingle("period", "" + period,
		// "Period [ms]"));

		return f;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		if (enabled && !value) {
			// turning off
			this.enabled = value;
			disable();
		} else if (!enabled && value) {
			// turning on
			this.enabled = value;
			enable();
		}
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field f = form.get(ENABLED_VAR);
		if (f != null) {
			boolean value = Field.getAsBoolean(f);
			setEnabled(value);
		}
	}

	protected void disable() {
	}

	protected void enable() {
	}

}
