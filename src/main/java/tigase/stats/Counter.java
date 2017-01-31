/*
 * Counter.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */
package tigase.stats;

import java.util.logging.Level;

/**
 *
 * @author andrzej
 */
public class Counter extends CounterValue {
		
	private long last_hour_counter = 0;
	private long last_minute_counter = 0;
	private long last_second_counter = 0;
	
	private long per_hour = 0;
	private long per_minute = 0;
	private long per_second = 0;

	public Counter(String name, Level level) {
		super(name, level);
	}
	
	public synchronized void everyHour() {
		per_hour = counter - last_hour_counter;
		last_hour_counter = counter;
	}
	
	public synchronized void everyMinute() {
		per_minute = counter - last_minute_counter;
		last_minute_counter = counter;
	}
	
	public synchronized void everySecond() {
		per_second = counter - last_second_counter;
		last_second_counter = counter;
	}		
	
	public long getPerHour() {
		return per_hour;
	}
	
	public long getPerMinute() {
		return per_minute;
	}
	
	public long getPerSecond() {
		return per_second;
	}
	
	public void getStatistics(String compName, StatisticsList list) {
		if (list.checkLevel(level)) {
			list.add(compName, name + " last hour", per_hour, level);
			list.add(compName, name + " last minute", per_minute, level);
			list.add(compName, name + " last second", per_second, level);
		}
	}
	
}
