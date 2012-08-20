/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.sreceiver.sysmon;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import tigase.conf.ConfiguratorOld;
import tigase.stats.StatisticsList;
import tigase.util.LogFormatter;
import tigase.xmpp.JID;

/**
 * Created: Dec 12, 2008 8:31:38 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class LogMonitor extends AbstractMonitor {

	private MonitorHandler monitorHandler = null;
	private MemoryHandlerFlush memoryHandler = null;
	private Map<String, String> monitorLevels =
					new LinkedHashMap<String, String>();
	private int loggerSize = 50;
	private Level levelTreshold = Level.WARNING;
	private int maxLogBuffer = 1000*1000;
	private long lastWarningSent = 0;
	private long logWarings = 0;

	private enum command {
		setlevel(" [package=level] - Sets logging level for specified package."),
		loggersize(" [N] - Sets memory logger size to specified value."),
		leveltreshold(" [level] - Sets level treshold to specified level."),
		logdump(" - retrieves all logs collected in the memory buffer and clears that buffer.");

		private String helpText = null;

		private command(String helpText) {
			this.helpText = helpText;
		}

		public String getHelp() {
			return helpText;
		}

	};

	@Override
	public String commandsHelp() {
		StringBuilder sb = new StringBuilder();
		for (command comm : command.values()) {
			sb.append("//" + comm.name() + comm.getHelp() + "\n");
		}
		return sb.toString();
	}

	@Override
	public String runCommand(String[] com) {
		command comm = command.valueOf(com[0].substring(2));
		switch (comm) {
			case setlevel:
				if (com.length > 1) {
					String[] keyval = com[1].split("=");
					if (keyval.length > 1) {
						String key = (keyval[0].endsWith(".level") ? keyval[0].substring(
										0, keyval[0].length() - 6) : keyval[0]);
						try {
							Level level = Level.parse(keyval[1]);
							Logger.getLogger(key).setLevel(level);
							monitorLevels.put(key + ".level", level.getName());
							return "Level set successfuly: " + key + "=" + level.getName();
						} catch (Exception e) {
							return "Incorrect level name, use: ALL, FINEST, FINER, FINE, " +
											"INFO, WARNING, SEVERE, OFF";
						}
					} else {
						return "Incorrect level setting, use: package=level";
					}
				} else {
					return "Current logging levels are:\n" + getCurrentLevels();
				}
			case loggersize:
				if (com.length > 1) {
					try {
						int newLoggerSize = Integer.parseInt(com[1]);
						loggerSize = newLoggerSize;
						registerHandler();
						return "New logger size successfuly set to: " + loggerSize;
					} catch (Exception e) {
						return "Incorrect logger size: " + com[1];
					}
				} else {
					return "Current memory logger size is: " + loggerSize;
				}
			case leveltreshold:
				if (com.length > 1) {
					return "Setting logging treshold level is not supported yet.";
				} else {
					return "Current logging treshold level is: " + levelTreshold;
				}
			case logdump:
				return "Memory logging buffer content:\n" +
								memoryHandler.pushToString();
		}
		return null;
	}

	@Override
	public boolean isMonitorCommand(String com) {
		if (com != null) {
			for (command comm: command.values()) {
				if (com.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	private void removeHandler() {
		if (memoryHandler != null) {
			Logger.getLogger("").removeHandler(memoryHandler);
		}
	}

	private void registerHandler() {
		removeHandler();
		if (monitorHandler == null) {
			monitorHandler = new MonitorHandler();
			monitorHandler.setLevel(Level.ALL);
		}
		memoryHandler =
						new MemoryHandlerFlush(monitorHandler, loggerSize, levelTreshold);
		memoryHandler.setLevel(Level.ALL);
		Logger.getLogger("").addHandler(memoryHandler);
	}

	@Override
	public void init(JID jid, float treshold, SystemMonitorTask smTask) {
		super.init(jid, treshold, smTask);
		registerHandler();
	}

	@Override
	public void destroy() {
		removeHandler();
	}

	private String getCurrentLevels() {
		String[] configLines = ConfiguratorOld.logManagerConfiguration.split("\n");
		Map<String, String> results = new LinkedHashMap<String, String>();
		for (String string : configLines) {
			if (string.contains(".level") && !string.contains("FileHandler") &&
							!string.contains("ConsoleHandler")) {
				String[] keyval = string.split("=");
				results.put(keyval[0], keyval[1]);
			}
		}
		results.putAll(monitorLevels);
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : results.entrySet()) {
			sb.append(entry.getKey() + "=" + entry.getValue() + "\n");
		}
		return sb.toString();
	}

	@Override
	public String getState() {
		StringBuilder sb = new StringBuilder("Logging levels:\n");
		sb.append(getCurrentLevels());
		sb.append("Monitor parameters:\n");
		sb.append("loggerSize=" + loggerSize + "\n");
		sb.append("levelTreshold=" + levelTreshold + "\n");
		return sb.toString();
	}

	private class MonitorHandler extends Handler {

		private LinkedList<String> logs = new LinkedList<String>();
		private LogFormatter formatter = new LogFormatter();

		@Override
		public synchronized void publish(LogRecord record) {
			String logEntry =
							formatter.format(record).replace('<', '[').replace('>', ']');
			logs.add(logEntry);
		}

		public synchronized String logsToString() {
			StringBuilder sb = new StringBuilder();
			String logEntry = null;
			while (((logEntry = logs.pollLast()) != null) &&
							(sb.length() < maxLogBuffer)) {
				sb.insert(0, logEntry);
			}
			logs.clear();
			String result = sb.length() <= maxLogBuffer ? sb.toString()
							: sb.substring(sb.length() - maxLogBuffer);
			return result;
		}

		@Override
		public synchronized void flush() {
			++logWarings;
			if (System.currentTimeMillis() - lastWarningSent > 5*MINUTE) {
				String logBuff = logsToString();
				// We don't want to flood the system with this in case of
				// some frequent error....
				sendWarningOut(logBuff, null);
				lastWarningSent = System.currentTimeMillis();
			}
		}

		@Override
		public void close() throws SecurityException { }
		
	}

	private class MemoryHandlerFlush extends MemoryHandler {

		MonitorHandler monHandle = null;

		public MemoryHandlerFlush(MonitorHandler target, int size, Level pushLevel) {
			super(target, size, pushLevel);
			monHandle = target;
		}

		public String pushToString() {
			super.push();
			return monHandle.logsToString();
		}

		@Override
		public void push() {
			super.push();
			flush();
		}
	}

	@Override
	public void getStatistics(StatisticsList list) {
    super.getStatistics(list);
		list.add("log-mon", "Log warings", logWarings, Level.FINE);
	}

}
