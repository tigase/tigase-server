package tigase.monitor.tasks;

import java.util.Date;
import java.util.LinkedList;
import java.util.logging.*;

import tigase.disteventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.monitor.MonitorComponent;
import tigase.util.DateTimeFormatter;
import tigase.util.LogFormatter;
import tigase.xml.Element;

@Bean(name = "logger-task")
public class LoggerTask extends AbstractConfigurableTask {

	private class MemoryHandlerFlush extends MemoryHandler {

		MonitorHandler monHandle = null;

		public MemoryHandlerFlush(MonitorHandler target, int size, Level pushLevel) {
			super(target, size, pushLevel);
			monHandle = target;
		}

		@Override
		public void push() {
			super.push();
			flush();
		}

		public String pushToString() {
			super.push();
			return monHandle.logsToString();
		}
	}

	private class MonitorHandler extends Handler {

		private LogFormatter formatter = new LogFormatter();
		private LinkedList<String> logs = new LinkedList<String>();

		@Override
		public void close() throws SecurityException {
		}

		@Override
		public synchronized void flush() {
			++logWarings;
			if (System.currentTimeMillis() - lastWarningSent > 5 * 60000) {
				String logBuff = logsToString();
				// We don't want to flood the system with this in case of
				// some frequent error....
				sendWarningOut(logBuff);
				lastWarningSent = System.currentTimeMillis();
			}
		}

		public synchronized String logsToString() {
			StringBuilder sb = new StringBuilder();
			String logEntry = null;
			while (((logEntry = logs.pollLast()) != null) && (sb.length() < maxLogBuffer)) {
				sb.insert(0, logEntry);
			}
			logs.clear();
			String result = sb.length() <= maxLogBuffer ? sb.toString() : sb.substring(sb.length() - maxLogBuffer);
			return result;
		}

		@Override
		public synchronized void publish(LogRecord record) {
			String logEntry = formatter.format(record).replace('<', '[').replace('>', ']');
			logs.add(logEntry);
		}

	}

	protected final static DateTimeFormatter dtf = new DateTimeFormatter();

	private static final String LOGGER_MONITOR_EVENT_NAME = "LoggerMonitorEvent";

	@Inject
	protected MonitorComponent component;

	@Inject
	protected EventBus eventBus;

	private long lastWarningSent = 0;

	private Level levelTreshold = Level.WARNING;

	private int loggerSize = 50;

	private long logWarings = 0;

	private int maxLogBuffer = 1000 * 1000;

	private MemoryHandlerFlush memoryHandler = null;

	private MonitorHandler monitorHandler = null;

	@Override
	protected void disable() {
		removeHandler();
		super.disable();
	}

	@Override
	protected void enable() {
		registerHandler();
		super.enable();
	}

	@Override
	public Form getCurrentConfiguration() {
		Form f = super.getCurrentConfiguration();

		Field x = Field.fieldListSingle("levelTreshold", levelTreshold.getName(), "Log level threshold",
				new String[] { Level.SEVERE.getName(), Level.WARNING.getName(), Level.INFO.getName(), Level.CONFIG.getName(),
						Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName(), Level.ALL.getName() },
				new String[] { Level.SEVERE.getName(), Level.WARNING.getName(), Level.INFO.getName(), Level.CONFIG.getName(),
						Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName(), Level.ALL.getName() });

		f.addField(x);

		return f;
	}

	public Level getLevelTreshold() {
		return levelTreshold;
	}

	private void registerHandler() {
		removeHandler();
		if (monitorHandler == null) {
			monitorHandler = new MonitorHandler();
			monitorHandler.setLevel(Level.ALL);
		}
		memoryHandler = new MemoryHandlerFlush(monitorHandler, loggerSize, levelTreshold);
		memoryHandler.setLevel(Level.ALL);
		Logger.getLogger("").addHandler(memoryHandler);
	}

	private void removeHandler() {
		if (memoryHandler != null) {
			Logger.getLogger("").removeHandler(memoryHandler);
		}
	}

	public void sendWarningOut(String logBuff) {
		Element event = new Element(LOGGER_MONITOR_EVENT_NAME, new String[] { "xmlns" },
				new String[] { MonitorComponent.EVENTS_XMLNS });
		event.addChild(new Element("hostname", component.getDefHostName().toString()));
		event.addChild(new Element("timestamp", "" + dtf.formatDateTime(new Date())));
		event.addChild(new Element("hostname", component.getDefHostName().toString()));
		event.addChild(new Element("log", logBuff));

		eventBus.fire(event);
	}

	public void setLevelTreshold(String levelTreshold) {
		boolean reregister = false;
		if (levelTreshold != null) {
			Level v = Level.parse(levelTreshold);
			reregister |= !v.equals(this.levelTreshold);
			this.levelTreshold = v;
		}

		if (reregister) {
			registerHandler();
		}

		System.out.println("HAAAAA " + this.levelTreshold);
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field f = form.get("levelTreshold");
		if (f != null)
			setLevelTreshold(f.getValue());
		super.setNewConfiguration(form);
	}

}
