package tigase.monitor.tasks;

import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.monitor.TimerTaskService;
import tigase.util.TimerTask;

public abstract class AbstractConfigurableTimerTask extends AbstractConfigurableTask implements UnregisterAware {

	private final static String PERIOD_VAR = "x-task#period";

	private long period = 1000l;

	@Inject(bean = "timerTaskService")
	private TimerTaskService timerTaskService;

	private final TimerTask worker = new TimerTask() {

		@Override
		public void run() {
			AbstractConfigurableTimerTask.this.run();
		}
	};

	@Override
	public void beforeUnregister() {
		setEnabled(false);
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

	@Override
	public Form getCurrentConfiguration() {
		Form f = super.getCurrentConfiguration();

		f.addField(Field.fieldTextSingle(PERIOD_VAR, "" + period, "Period [ms]"));

		return f;
	}

	public long getPeriod() {
		return period;
	}

	public TimerTaskService getTimerTaskService() {
		return timerTaskService;
	}

	protected abstract void run();

	@Override
	public void setNewConfiguration(Form form) {
		Field f = form.get(PERIOD_VAR);
		if (f != null) {
			long value = Long.parseLong(f.getValue());
			setPeriod(value);
		}

		super.setNewConfiguration(form);
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

	public void setPeriod(String value) {
		setPeriod(Long.parseLong(value));
	}

	public void setTimerTaskService(TimerTaskService timerTaskService) {
		this.timerTaskService = timerTaskService;
	}

}
