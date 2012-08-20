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

package tigase.server.filters;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.PacketFilterIfc;
import tigase.server.QueueType;

import tigase.stats.StatisticsList;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 8, 2009 1:47:31 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PacketCounter implements PacketFilterIfc {
	private long clusterCounter = 0;
	private long[] iqCounters = new long[1];
	private int lastNodeNo = -1;
	private Logger log = Logger.getLogger(this.getClass().getName());
	private long msgCounter = 0;
	private String name = null;
	private long presCounter = 0;
	private long otherCounter = 0;
	private QueueType qType = null;
	private ConcurrentHashMap<String, Integer> iqCounterIdx =
			new ConcurrentHashMap<String, Integer>();

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param packet
	 * 
	 * @return
	 */
	@Override
	public Packet filter(Packet packet) {
		if (packet.getElemName() == "message") {
			++msgCounter;

			return packet;
		}

		if (packet.getElemName() == "presence") {
			++presCounter;

			return packet;
		}

		if (packet.getElemName() == "cluster") {
			++clusterCounter;

			return packet;
		}
		
		++otherCounter;

		if (packet.getElemName() == "iq") {
			String xmlns = ((Iq) packet).getIQXMLNS();

			incIQCounter((xmlns != null) ? xmlns : ((Iq) packet).getIQChildName());
		}
		return packet;
	}

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		list.add(name, qType.name() + " messages", msgCounter, Level.FINER);
		list.add(name, qType.name() + " presences", presCounter, Level.FINER);
		list.add(name, qType.name() + " cluster", clusterCounter, Level.FINER);
		list.add(name, qType.name() + " other", otherCounter, Level.FINER);
		list.add(name, qType.name() + " IQ no XMLNS", iqCounters[0], Level.FINER);

		long iqs = iqCounters[0];

		for (Entry<String, Integer> iqCounter : iqCounterIdx.entrySet()) {
			list.add(name, qType.name() + " IQ " + iqCounter.getKey(),
					iqCounters[iqCounter.getValue()], Level.FINER);
			iqs += iqCounters[iqCounter.getValue()];
		}

		list.add(name, qType.name() + " IQ", iqs, Level.FINER);
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param name
	 * @param qType
	 */
	@Override
	public void init(String name, QueueType qType) {
		this.name = name;
		this.qType = qType;
	}

	private synchronized void incIQCounter(String xmlns) {
		if (xmlns == null) {
			++iqCounters[0];
		} else {
			Integer idx = iqCounterIdx.get(xmlns);

			if (idx == null) {
				iqCounters = Arrays.copyOf(iqCounters, iqCounters.length + 1);
				idx = iqCounters.length - 1;
				iqCounterIdx.put(xmlns, idx);
			}

			++iqCounters[idx];
		}
	}
}
