/*
 * StatisticHolderImpl.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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
public class StatisticHolderImpl implements StatisticHolder {
	
	private String prefix = null;
	
	private long last_hour_packets = 0;
	private long last_minute_packets = 0;
	private long last_second_packets = 0;
	private long packets_per_hour = 0;
	private long packets_per_minute = 0;
	private long packets_per_second = 0;
	private long requestsOk = 0;
	private long avgProcessingTime = 0;		
	
	@Override
	public void statisticExecutedIn(long executionTime) {
		avgProcessingTime = (avgProcessingTime + executionTime) / 2;
		++requestsOk;
	}
	
	@Override
	public synchronized void everyHour() {
		packets_per_hour = requestsOk - last_hour_packets;
		last_hour_packets = requestsOk;
	}
	
	@Override
	public synchronized void everyMinute() {
		packets_per_minute = requestsOk - last_minute_packets;
		last_minute_packets = requestsOk;
	}
	
	@Override
	public synchronized void everySecond() {
		packets_per_second = requestsOk - last_second_packets;
		last_second_packets = requestsOk;
	}		
	
	@Override
	public void getStatistics(String compName, StatisticsList list) {
		if (list.checkLevel(Level.FINEST)) {
			list.add(compName, prefix +"/Last hour packets", packets_per_hour, Level.FINEST);
			list.add(compName, prefix +"/Last minute packets", packets_per_minute, Level.FINEST);
			list.add(compName, prefix +"/Last second packets", packets_per_second, Level.FINEST);
		}
		list.add(compName, prefix +"/Average processing time", avgProcessingTime, Level.FINE);
	}	
	
	@Override
	public void setStatisticsPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	
}
