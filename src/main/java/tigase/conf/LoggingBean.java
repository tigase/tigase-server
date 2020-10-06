/*
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
package tigase.conf;

import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.server.Packet;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.log.LogFormatter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Stream;

/**
 * Created by andrzej on 02.04.2017.
 */
public class LoggingBean
		implements ConfigurationChangedAware {

	@ConfigField(desc = "Debug", alias = "debug")
	private String[] debug;
	@ConfigField(desc = "Debug packages", alias = "debug-packages")
	private String[] debugPackages;
	@ConfigField(desc = "Handlers")
	private HashMap<String, HashMap<String, Object>> handlers = new HashMap<>();
	@ConfigField(desc = "Loggers")
	private HashMap<String, HashMap<String, Object>> loggers = new HashMap<>();
	@ConfigField(desc = "Packet full debug", alias = "packet-debug-full")
	private boolean packetFullDebug = Packet.FULL_DEBUG;
	@ConfigField(desc = "Root handlers")
	private String[] rootHandlers = new String[0];
	@ConfigField(desc = "Root level")
	private Level rootLevel = Level.CONFIG;
	@ConfigField(desc = "Log thread dump on shutdown", alias = "shutdown-thread-dump")
	private boolean shutdownThreadDump = true;

	private final static Logger log = Logger.getLogger(LoggingBean.class.getName());

	public LoggingBean() {
		HashMap<String, HashMap<String, Object>> loggers = new HashMap<>();
		loggers.computeIfAbsent("tigase.kernel.core.Kernel", (name) -> {
			HashMap<String, Object> props = new HashMap<>();
			props.put("level", "CONFIG");
			return props;
		});
		setLoggers(loggers);
		setHandlers(new HashMap<>());

		rootHandlers = new String[]{ConsoleHandler.class.getCanonicalName(), FileHandler.class.getCanonicalName()};
	}

	public synchronized Map<String,Level> getPackageLoggingLevels() {
		// how to merge debug with loggers? shouldn't that be unified??
		// should we move "tigase" to "debug" and leave rest with loggers??
		// we should also prepare a good merging mechanism
		Stream<String> debugStream = Stream.concat(
				Optional.ofNullable(debug).map(Arrays::stream).orElse(Stream.empty()).map(s -> "tigase." + s),
				Optional.ofNullable(debugPackages).map(Arrays::stream).orElse(Stream.empty()));

		Map<String, Level> result = new HashMap<>();
		debugStream.forEach(p -> result.put(p, Level.ALL));

		for (String p : loggers.keySet()) {
			Optional.ofNullable(loggers.get(p)).map(v -> v.get("level")).filter(String.class::isInstance).map(v -> {
				try {
					return Level.parse((String) v);
				} catch (IllegalArgumentException ex) {
					return null;
				}
			}).filter(Objects::nonNull).ifPresent(level -> result.put(p, level));
		}

		log.log(Level.FINE, "Currently configured loggers: " + result);

		return result;
	}

	public synchronized void setPackageLoggingLevel(String packageName, Level level) throws RuntimeException {
		log.log(Level.CONFIG, "Setting log level for package: {0} to {1}", new Object[]{packageName, level});
		if (packageName.startsWith("tigase.")) {
			String part = packageName.substring("tigase.".length());
			Optional.ofNullable(debug)
					.map(Arrays::stream)
					.map(s -> s.filter(name -> !part.equals(name)))
					.map(s -> s.toArray(String[]::new))
					.ifPresent(value -> this.debug = value);
		}
		Optional.ofNullable(debugPackages)
				.map(Arrays::stream)
				.map(s -> s.filter(name -> !packageName.equals(name)))
				.map(s -> s.toArray(String[]::new))
				.ifPresent(value -> this.debugPackages = value);

		HashMap<String, Object> value = loggers.get(packageName);
		if (value == null) {
			if (level != Level.OFF) {
				value = new HashMap<>();
				value.put("level", level.getName());
				loggers.put(packageName, value);
			}
		} else {
			if (level == Level.OFF) {
				if (value.size() == 1) {
					loggers.remove(packageName);
				} else {
					value.remove("level");
				}
			} else {
				value.put("level", level.getName());
			}
		}

		beanConfigurationChanged(Collections.emptySet());
	}

	public void setLoggers(HashMap<String, HashMap<String, Object>> loggers) {
		loggers.compute("tigase", (name, props) -> {
			if (props == null) {
				props = new HashMap<>();
			}
			props.putIfAbsent("useParentHandlers", true);
			return props;
		});
		this.loggers = loggers;
	}

	public void setHandlers(HashMap<String, HashMap<String, Object>> handlers) {
		handlers.compute(ConsoleHandler.class.getCanonicalName(), (cls, props) -> {
			if (props == null) {
				props = new HashMap<>();
			}
			props.putIfAbsent("level", Level.WARNING);
			props.putIfAbsent("formatter", LogFormatter.class.getCanonicalName());
			return props;
		});

		handlers.compute(FileHandler.class.getCanonicalName(), (cls, props) -> {
			if (props == null) {
				props = new HashMap<>();
			}
			props.putIfAbsent("level", Level.ALL);
			props.putIfAbsent("append", true);
			props.putIfAbsent("count", 5);
			props.putIfAbsent("limit", 10000000);
			props.putIfAbsent("formatter", LogFormatter.class.getCanonicalName());
			props.putIfAbsent("pattern", "logs/tigase.log");
			return props;
		});
		this.handlers = handlers;
	}

	public boolean getPacketFullDebug() {
		return Packet.FULL_DEBUG;
	}

	public void setPacketFullDebug(boolean packetFullDebug) {
		this.packetFullDebug = packetFullDebug;
		Packet.FULL_DEBUG = packetFullDebug;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		StringBuilder sb = new StringBuilder();

		sb.append(".level=").append(rootLevel.getName()).append("\n");

		Stream.concat(Optional.ofNullable(debug).stream().flatMap(Arrays::stream).map(s -> "tigase." + s),
					  Optional.ofNullable(debugPackages).stream().flatMap(Arrays::stream))
				.forEach(name -> sb.append(name).append(".level=").append(Level.ALL).append("\n"));

		loggers.forEach((name, props) -> {
			props.forEach((key, value) -> {
				log.log(Level.CONFIG, "Setting log level for loggerName: {0} to: {1}", new Object[]{name, props});
				sb.append(name).append(".").append(key).append("=");
				if (value instanceof Collection) {
					Collection col = (Collection) value;
					boolean first = true;
					for (Object item : col) {
						if (!first) {
							sb.append(" ");
						} else {
							first = false;
						}
						sb.append(item);
					}
				} else {
					sb.append(value);
				}
				sb.append("\n");
			});
		});
		handlers.forEach((name, props) -> {
			props.forEach((key, value) -> {
				sb.append(name).append(".").append(key).append("=").append(value).append("\n");
			});
		});
		sb.append("handlers=");
		if (rootHandlers != null) {
			for (int i = 0; i < rootHandlers.length; i++) {
				if (i > 0) {
					sb.append(" ");
				}
				sb.append(rootHandlers[i]);
			}
		}

		byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
		try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
			LogManager.getLogManager().reset();
			LogManager.getLogManager().updateConfiguration(in, (k) -> k.endsWith(".handlers")
																	  ? ((o, n) -> (o == null ? n : o))
																	  : ((o, n) -> n));
			log.log(Level.CONFIG, "Initialised LogManager with configuration: {0}", new Object[]{sb});
		} catch (IOException ex) {
			throw new RuntimeException("Failed to load logging configuration", ex);
		}
	}

	public boolean isShutdownThreadDump() {
		return MonitorRuntime.getMonitorRuntime().isShutdownThreadDump();
	}

	public void setShutdownThreadDump(boolean shutdownThreadDump) {
		MonitorRuntime.getMonitorRuntime().setShutdownThreadDump(shutdownThreadDump);
	}
}
