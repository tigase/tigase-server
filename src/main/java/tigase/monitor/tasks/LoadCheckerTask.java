package tigase.monitor.tasks;

import java.util.Date;
import java.util.HashSet;

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.InfoTask;
import tigase.monitor.MonitorComponent;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.DateTimeFormatter;
import tigase.xml.Element;

@Bean(name = "load-checker-task")
public class LoadCheckerTask extends AbstractConfigurableTimerTask implements InfoTask {

	public static final String MONITOR_EVENT_NAME = "tigase.monitor.tasks.LoadAverageMonitorEvent";
	private final static DateTimeFormatter dtf = new DateTimeFormatter();
	private final HashSet<String> triggeredEvents = new HashSet<String>();
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
		result.addField(Field.fieldTextSingle("averageLoad", Double.toString(runtime.getLoadAverage()), "Load Average"));

		return result;
	}

	@Override
	protected void run() {
		double curAverageLoad = runtime.getLoadAverage();
		if (curAverageLoad >= averageLoadThreshold) {
			Element event = new Element(MONITOR_EVENT_NAME);
			event.addChild(new Element("timestamp", "" + dtf.formatDateTime(new Date())));
			event.addChild(new Element("hostname", component.getDefHostName().toString()));
			event.addChild(new Element("averageLoad", Double.toString(curAverageLoad)));
			event.addChild(new Element("message", "Average Load is higher than " + averageLoadThreshold + " and it is equals "
					+ Double.toString(curAverageLoad)));

			if (!triggeredEvents.contains(event.getName())) {
				eventBus.fire(event);
				triggeredEvents.add(event.getName());
			}
		} else {
			triggeredEvents.remove(MONITOR_EVENT_NAME);
		}
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field tmp = form.get("averageLoadThreshold");
		if (tmp != null) {
			this.averageLoadThreshold = Long.parseLong(tmp.getValue());
		}

		super.setNewConfiguration(form);
	}

}
