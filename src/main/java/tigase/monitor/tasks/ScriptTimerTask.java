package tigase.monitor.tasks;

import javax.script.ScriptException;

import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.Inject;
import tigase.monitor.ConfigurableTask;
import tigase.monitor.TimerTaskService;
import tigase.util.TimerTask;

public class ScriptTimerTask extends ScriptTask implements ConfigurableTask {

	private long delay = 1000l;

	private String script;

	private String scriptExtension;

	protected long taskDelay;

	@Inject(bean = "timerTaskService")
	private TimerTaskService timerTaskService;

	private final TimerTask worker = new TimerTask() {

		@Override
		public void run() {
			try {
				ScriptTimerTask.super.run(script, scriptExtension);
			} catch (ScriptException e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public Form getCurrentConfiguration() {
		Form f = new Form("form", "Script configuration", null);

		f.addField(Field.fieldTextSingle("delay", String.valueOf(delay), "Delay"));

		return f;
	}

	public void run(String script, String scriptExtension, long delay) throws ScriptException {
		this.script = script;
		this.scriptExtension = scriptExtension;
		this.delay = delay;

		timerTaskService.addTimerTask(worker, 1000l, this.delay);
	}

	@Override
	public void setNewConfiguration(Form form) {
		Long d = form.getAsLong("delay");
		try {
			if (d != delay) {
				worker.cancel();
				this.delay = d;
				timerTaskService.addTimerTask(worker, 1000l, this.delay);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
