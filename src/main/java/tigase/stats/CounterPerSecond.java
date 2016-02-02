/*
 * CounterPerSecond.java
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
import tigase.util.EverySecond;

/**
 *
 * @author andrzej
 */
public class CounterPerSecond extends CounterValue implements EverySecond {
		
	private long last_second_counter = 0;
	
	private long per_second = 0;

	public CounterPerSecond(String name, Level level) {
		super(name, level);
	}
		
	@Override
	public synchronized void everySecond() {
		per_second = counter - last_second_counter;
		last_second_counter = counter;
	}		
		
	public long getPerSecond() {
		return per_second;
	}
	
	public void getStatistics(String compName, StatisticsList list) {
		if (list.checkLevel(level)) {
			list.add(compName, name + " last second", per_second, level);
		}
	}
		
}
