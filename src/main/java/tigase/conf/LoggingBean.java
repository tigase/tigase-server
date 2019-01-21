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
package tigase.conf;

import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.server.Packet;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.log.LogFormatter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
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

		Optional.ofNullable(debug).ifPresent(names -> {
			Stream.of(names).forEach(name -> {
				sb.append("tigase.").append(name).append(".level=").append(Level.ALL).append("\n");
			});
		});
		Optional.ofNullable(debugPackages).ifPresent(names -> {
			Stream.of(names).forEach(name -> {
				sb.append(name).append(".level=").append(Level.ALL).append("\n");
			});
		});

		loggers.forEach((name, props) -> {
			props.forEach((key, value) -> {
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

		byte[] data = sb.toString().getBytes(Charset.forName("UTF-8"));
		try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
			LogManager.getLogManager().readConfiguration(in);
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
