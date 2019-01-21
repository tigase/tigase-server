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
package tigase.stats;

import java.util.logging.Level;

/**
 * @author andrzej
 */
public class CounterPerSecond
		extends CounterValue {

	private long last_second_counter = 0;

	private long per_second = 0;

	public CounterPerSecond(String name, Level level) {
		super(name, level);
	}

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
