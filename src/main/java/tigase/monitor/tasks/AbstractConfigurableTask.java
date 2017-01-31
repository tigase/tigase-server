package tigase.monitor.tasks;

import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.UnregisterAware;
import tigase.monitor.ConfigurableTask;
import tigase.monitor.MonitorTask;

public abstract class AbstractConfigurableTask implements MonitorTask, ConfigurableTask, UnregisterAware {

	private final static String ENABLED_VAR = "x-task#enabled";

	private boolean enabled = false;

	@Override
	public void beforeUnregister() {
		setEnabled(false);
	}

	protected void disable() {
	}

	protected void enable() {
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

}
