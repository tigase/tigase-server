/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.stats;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Generic class used by MD connection pools and repositories to wrap and measure method execution times.
 *
 * Created by andrzej on 15.12.2016.
 */
public class StatisticsInvocationHandler<S>
		implements InvocationHandler {

	private final String name;
	private final S instance;
	private final Class[] monitoredIfcs;

	private final ConcurrentHashMap<Method, MethodStatistics> statistics = new ConcurrentHashMap<>();

	public StatisticsInvocationHandler(String name, S instance, Class... monitoredIfcs) {
		this.name = name;
		this.instance = instance;
		this.monitoredIfcs = monitoredIfcs;
		for (Class clazz : this.monitoredIfcs) {
			for (Method m : clazz.getDeclaredMethods()) {
				statistics.put(m, new MethodStatistics(m));
			}
		}
	}

	public void everyHour() {
		statistics.values().forEach(MethodStatistics::everyHour);
	}

	public void everyMinute() {
		statistics.values().forEach(MethodStatistics::everyMinute);
	}

	public void everySecond() {
		statistics.values().forEach(MethodStatistics::everySecond);
	}

	public void getStatistics(String compName, String prefix, StatisticsList list) {
		String subprefix = (prefix != null) ? (prefix + "/" + name) : name;
		statistics.values().forEach(methodStatistics -> methodStatistics.getStatistics(compName, subprefix, list));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		MethodStatistics methodStatistics = statistics.get(method);
		if (methodStatistics != null) {
			long start = System.currentTimeMillis();

			try {
				return method.invoke(this.instance, args);
			} catch (Throwable ex) {
				methodStatistics.executionFailed();
				if (ex instanceof UndeclaredThrowableException) {
					ex = ((UndeclaredThrowableException) ex).getUndeclaredThrowable();
				}
				if (ex instanceof InvocationTargetException) {
					throw ((InvocationTargetException) ex).getTargetException();
				} else {
					throw ex;
				}
			} finally {
				methodStatistics.updateExecutionTime(System.currentTimeMillis() - start);
			}
		} else {
			try {
				return method.invoke(this.instance, args);
			} catch (Throwable ex) {
				if (ex instanceof UndeclaredThrowableException) {
					ex = ((UndeclaredThrowableException) ex).getUndeclaredThrowable();
				}
				if (ex instanceof InvocationTargetException) {
					throw ((InvocationTargetException) ex).getTargetException();
				} else {
					throw ex;
				}
			}
		}
	}

	public static class MethodStatistics {

		private final Method method;

		private long executions_counter = 0;
		private long exceptions_counter = 0;

		private long last_hour_counter = 0;
		private long last_minute_counter = 0;
		private long last_second_counter = 0;

		private long per_hour = 0;
		private long per_minute = 0;
		private long per_second = 0;

		private long avgProcessingTime = 0;

		public MethodStatistics(Method method) {
			this.method = method;
		}

		public synchronized void everyHour() {
			per_hour = executions_counter - last_hour_counter;
			last_hour_counter = executions_counter;
		}

		public synchronized void everyMinute() {
			per_minute = executions_counter - last_minute_counter;
			last_minute_counter = executions_counter;
		}

		public synchronized void everySecond() {
			per_second = executions_counter - last_second_counter;
			last_second_counter = executions_counter;
		}

		public void getStatistics(String compName, String prefix, StatisticsList list) {
			String name = method.getName();
			if (list.checkLevel(Level.FINEST)) {
				list.add(compName, prefix + "/" + name + "/Excutions last hour", per_hour, Level.FINEST);
				list.add(compName, prefix + "/" + name + "/Excutions last minute", per_minute, Level.FINEST);
				list.add(compName, prefix + "/" + name + "/Excutions last second", per_second, Level.FINEST);
			}
			list.add(compName, prefix + "/" + name + "/Average processing time", avgProcessingTime, Level.FINE);
			list.add(compName, prefix + "/" + name + "/Executions", executions_counter, Level.FINE);
			list.add(compName, prefix + "/" + name + "/Exceptions during execution", exceptions_counter, Level.FINE);
		}

		public void updateExecutionTime(long executionTime) {
			executions_counter++;
			avgProcessingTime = (avgProcessingTime + executionTime) / 2;
		}

		public void executionFailed() {
			exceptions_counter++;
		}

	}

}
