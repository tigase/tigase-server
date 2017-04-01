/*
 * PacketCounter.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.server.filters;

import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.PacketFilterIfc;
import tigase.server.QueueType;
import tigase.stats.StatisticsList;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PacketCounter
		implements PacketFilterIfc {

	private final static String DETAILED_OTHER_STATISTICS_KEY = "detailed-other-statistics";
	private final TypeCounter iqCounter = new TypeCounter("IQ");
	private final Map<String, TypeCounter> otherCounters = new ConcurrentHashMap<>();
	private long clusterCounter = 0;
	private boolean detailedOtherStat = true;
	private long msgCounter = 0;
	private String name = null;
	private long otherCounter = 0;
	private long presCounter = 0;
	private QueueType qType = null;
	private long total = 0;

	public PacketCounter() {
		final String tmp = System.getProperty(DETAILED_OTHER_STATISTICS_KEY);
		if (null != tmp) {
			detailedOtherStat = Boolean.valueOf(tmp);
		}
	}

	public PacketCounter(boolean detailedOtherStat) {
		this.detailedOtherStat = detailedOtherStat;
	}

	@Override
	public Packet filter(Packet packet) {
		total++;
		final String elemName = packet.getElemName();
		if (elemName == "message") {
			++msgCounter;
		} else if (elemName == "presence") {
			++presCounter;
		} else if (elemName == "cluster") {
			++clusterCounter;
		} else if (elemName == "iq") {
			String xmlns = ((Iq) packet).getIQXMLNS();
			iqCounter.incrementCounter((xmlns != null) ? xmlns : ((Iq) packet).getIQChildName());
		} else {
			++otherCounter;

			if (detailedOtherStat) {
				String xmlns = packet.getXMLNS() != null ? packet.getXMLNS() : "no XMLNS";
				String element = elemName;

				TypeCounter counter = otherCounters.get(xmlns);
				if (counter == null) {
					counter = new TypeCounter("other " + xmlns);
					otherCounters.put(xmlns, counter);
				}
				counter.incrementCounter(element);
			}
		}
		return packet;
	}

	@Override
	public void getStatistics(StatisticsList list) {
		list.add(name, qType.name() + " processed", total, Level.FINER);
		list.add(name, qType.name() + " processed messages", msgCounter, Level.FINER);
		list.add(name, qType.name() + " processed presences", presCounter, Level.FINER);
		list.add(name, qType.name() + " processed cluster", clusterCounter, Level.FINER);
		list.add(name, qType.name() + " processed other", otherCounter, Level.FINER);

		iqCounter.getStatistics(list);

		if (detailedOtherStat & list.checkLevel(Level.FINEST)) {
			otherCounters.values().forEach(typeCounter -> typeCounter.getStatistics(list));
		}
	}

	@Override
	public void init(String name, QueueType qType) {
		this.name = name;
		this.qType = qType;
	}

	private class MutableLong {

		long value = 0;

		private long get() {
			return value;
		}

		private void increment() {
			++value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

	}

	private class TypeCounter {

		private final Map<String, MutableLong> counter = new ConcurrentHashMap<>();
		private final String counterName;
		private final MutableLong total = new MutableLong();
		private final MutableLong withoutValue = new MutableLong();

		public TypeCounter(String name) {
			this.counterName = name;
		}

		public Map<String, MutableLong> getCounter() {
			return counter;
		}

		public void getStatistics(StatisticsList list) {
			list.add(name, qType.name() + " processed " + counterName, total.get(), Level.FINEST);
			if (this.withoutValue.get() > 0) {
				list.add(name, qType.name() + " processed " + counterName + " no XMLNS", this.withoutValue.get(),
				         Level.FINEST);
			}
			for (Entry<String, MutableLong> xmlnsValues : counter.entrySet()) {
				list.add(name, qType.name() + " processed " + counterName + " " + xmlnsValues.getKey(),
				         xmlnsValues.getValue().get(), Level.FINEST);

			}
		}

		public long getTotal() {
			return total.get();
		}

		synchronized public void incrementCounter(String param) {
			total.increment();
			if (param == null) {
				withoutValue.increment();
			} else {
				MutableLong count = counter.get(param);
				if (count == null) {
					count = new MutableLong();
					counter.put(param, count);
				}
				count.increment();
			}
		}
	}
}
