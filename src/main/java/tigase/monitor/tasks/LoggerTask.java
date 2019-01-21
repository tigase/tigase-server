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

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.MonitorComponent;
import tigase.util.datetime.TimestampHelper;
import tigase.util.log.LogFormatter;
import tigase.xml.Element;

import java.util.Date;
import java.util.LinkedList;
import java.util.logging.*;

@Bean(name = "logger-task", parent = MonitorComponent.class, active = true)
public class LoggerTask
		extends AbstractConfigurableTask
		implements Initializable {

	public static final Logger log = Logger.getLogger(LoggerTask.class.getName());

	protected final static TimestampHelper dtf = new TimestampHelper();
	private static final String LOGGER_MONITOR_EVENT_NAME = "tigase.monitor.tasks.LoggerMonitorEvent";
	@Inject
	protected MonitorComponent component;
	@Inject
	protected EventBus eventBus;
	private long lastWarningSent = 0;
	@ConfigField(desc = "Log Level Threshold")
	private Level levelTreshold = Level.WARNING;
	private long logWarings = 0;
	private int loggerSize = 50;
	private int maxLogBuffer = 1000 * 1000;
	private MemoryHandlerFlush memoryHandler = null;
	private MonitorHandler monitorHandler = null;

	@Override
	public Form getCurrentConfiguration() {
		Form f = super.getCurrentConfiguration();

		Field x = Field.fieldListSingle("levelTreshold", levelTreshold.getName(), "Log level threshold",
										new String[]{Level.SEVERE.getName(), Level.WARNING.getName(),
													 Level.INFO.getName(), Level.CONFIG.getName(), Level.FINE.getName(),
													 Level.FINER.getName(), Level.FINEST.getName(),
													 Level.ALL.getName()},
										new String[]{Level.SEVERE.getName(), Level.WARNING.getName(),
													 Level.INFO.getName(), Level.CONFIG.getName(), Level.FINE.getName(),
													 Level.FINER.getName(), Level.FINEST.getName(),
													 Level.ALL.getName()});
		f.addField(x);

		return f;
	}

	public Level getLevelTreshold() {
		return levelTreshold;
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

		log.log(Level.FINEST, "HAAAAA " + this.levelTreshold);
	}

	@Override
	public void initialize() {
		eventBus.registerEvent(LOGGER_MONITOR_EVENT_NAME, "Fired when logger receives with specific level", false);
	}

	public void sendWarningOut(String logBuff) {
		Element event = new Element(LOGGER_MONITOR_EVENT_NAME);
		event.addChild(new Element("hostname", component.getDefHostName().toString()));
		event.addChild(new Element("timestamp", "" + dtf.format(new Date())));
		event.addChild(new Element("hostname", component.getDefHostName().toString()));
		event.addChild(new Element("log", logBuff));

		eventBus.fire(event);
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field f = form.get("levelTreshold");
		if (f != null) {
			setLevelTreshold(f.getValue());
		}
		super.setNewConfiguration(form);
	}

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

	private class MemoryHandlerFlush
			extends MemoryHandler {

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

	private class MonitorHandler
			extends Handler {

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

}
