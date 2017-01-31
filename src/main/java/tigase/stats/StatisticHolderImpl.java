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
public class StatisticHolderImpl extends Counter implements StatisticHolder {
	
//	private String prefix = null;
//	
//	private long last_hour_packets = 0;
//	private long last_minute_packets = 0;
//	private long last_second_packets = 0;
//	private long packets_per_hour = 0;
//	private long packets_per_minute = 0;
//	private long packets_per_second = 0;
//	private long requestsOk = 0;
	private long avgProcessingTime = 0;		
	
	public StatisticHolderImpl() {
		super("NULL", Level.FINEST);
	}
	
	public StatisticHolderImpl(String name) {
		super(name, Level.FINEST);
	}	

	@Override
	public void statisticExecutedIn(long executionTime) {
		avgProcessingTime = (avgProcessingTime + executionTime) / 2;
		inc();
	}
	
	@Override
	public void getStatistics(String compName, StatisticsList list) {
		super.getStatistics(compName, list);
		list.add(compName, getName() +"/Average processing time", avgProcessingTime, Level.FINE);
	}	
	
	@Override
	public void setStatisticsPrefix(String prefix) {
		setName(prefix);
	}
	
	
}
