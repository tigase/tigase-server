/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
	private long[] iqCounters = new long[1];
	private int lastNodeNo = -1;
	private Logger log = Logger.getLogger(this.getClass().getName());
	private long msgCounter = 0;
	private String name = null;
	private long presCounter = 0;
	private QueueType qType = null;
	private ConcurrentHashMap<String, Integer> iqCounterIdx = new ConcurrentHashMap<String,
		Integer>();

	//~--- methods --------------------------------------------------------------

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
		}

		if (packet.getElemName() == "presence") {
			++presCounter;
		}

		if (packet.getElemName() == "iq") {
			String xmlns = ((Iq) packet).getIQXMLNS();

			incIQCounter((xmlns != null) ? xmlns : ((Iq) packet).getIQChildName());
		}

		// TESTING ONLY START
//  try {
//    String node_name = null;
//    List<Element> children = packet.getElemChildren("/iq/pubsub");
//    if (children != null) {
//      for (Element elem : children) {
//        node_name = elem.getAttribute("node");
//        if (node_name != null) {
//          break;
//        }
//      }
//    }
//    if (node_name != null) {
//      String node_no = node_name.substring("node-".length());
//      int no = Integer.parseInt(node_no);
//      if ((lastNodeNo + 1 != no) && (lastNodeNo != no)) {
//        log.warning(name + ":" + qType.name() +
//                ": Incorrect node number, lastNodeNo = " + lastNodeNo +
//                ", current number: " + no + ", or packet: " + packet.toString());
//      }
//      lastNodeNo = no;
//    }
//  } catch (Exception e) {
//    //e.printStackTrace();
//  }
		// TESTING ONLY END
		return packet;
	}

	//~--- get methods ----------------------------------------------------------

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
		list.add(name, qType.name() + " IQ no XMLNS", iqCounters[0], Level.FINEST);

		long iqs = iqCounters[0];

		for (Entry<String, Integer> iqCounter : iqCounterIdx.entrySet()) {
			list.add(name, qType.name() + " IQ " + iqCounter.getKey(),
					iqCounters[iqCounter.getValue()], Level.FINEST);
			iqs += iqCounters[iqCounter.getValue()];
		}

		list.add(name, qType.name() + " IQ", iqs, Level.FINER);
	}

	//~--- methods --------------------------------------------------------------

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


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
