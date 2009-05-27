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

package tigase.util;

import java.util.ArrayList;
import java.util.LinkedList;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Created: Apr 21, 2009 8:50:50 PM
 *
 * @param <E>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ProcessingThreads<E extends WorkerThread> {

	private LinkedList<E> workerThreads =	new LinkedList<E>();
	private ArrayList<PriorityQueue<QueueItem>> queues =
					new ArrayList<PriorityQueue<QueueItem>>();

	// Packets are put in queues in such a way that all packets for the same
	// user end-up in the same queue. This is important in some cases as
	// packet processing order does matter in some cases, especially for
	// roster processing.
	// Therefore it is also recommended that there is a single thread for
	// each queue but we can ditribute load increasing number of queues.
	private int numQueues = 2;
	private int numWorkerThreads = 1;
	private int maxQueueSize = 10000;
	private String name = null;

	private long droppedPackets = 0;

	@SuppressWarnings({"unchecked"})
	public ProcessingThreads(E worker, int numQueues,
					int numWorkerThreads, int maxQueueSize, String name) {
		this.numQueues = numQueues;
		this.maxQueueSize = maxQueueSize;
		this.numWorkerThreads = numWorkerThreads;
		this.name = name;

		for (int i = 0; i < numQueues; i++) {
			queues.add(new PriorityQueue<QueueItem>(Priority.values().length,
							maxQueueSize));
			for (int j = 0; j < numWorkerThreads; j++) {
				WorkerThread t = worker.getNewInstance(queues.get(i));
				t.setDaemon(true);
				t.setName(name + " Queue " + i + " Worker " + j);
				t.start();
				workerThreads.add((E)t);
			}
		}

	}

	public E getWorkerThread() {
		try {
			return workerThreads.getFirst();
		} catch (Exception e) {
			return null;
		}
	}

	public String getName() {
		return name;
	}

	public int getTotalQueueSize() {
		int ret = 0;
		for (PriorityQueue<QueueItem> pq : queues) {
			ret += pq.totalSize();
		}
		return ret;
	}

	public int getTotalRuns() {
		int ret = 0;
		for (WorkerThread workerThread : workerThreads) {
			ret += workerThread.getRunsCounter();
		}
		return ret;
	}

	public long getDroppedPackets() {
		return droppedPackets;
	}

	public long getAverageProcessingTime() {
		long average = 0;
		int counters = 0;
		for (WorkerThread workerThread : workerThreads) {
			if (workerThread.getAverageProcessingTime() > 0) {
				average += workerThread.getAverageProcessingTime();
				++counters;
			}
		}
		if (counters > 0) {
			return average / counters;
		} else {
			return 0;
		}
	}

	public boolean addItem(Packet packet, XMPPResourceConnection conn) {
		boolean ret = false;
		QueueItem item = new QueueItem();
		item.conn = conn;
		item.packet = packet;
		try {
			// Queueing packets per user...
			ret = queues.get(Math.abs(conn.getUserId().hashCode() %
							numQueues)).offer(item, packet.getPriority().ordinal());
		} catch (Exception e) {
			// Otherwise per destination address
			ret = queues.get(Math.abs(packet.getFrom().hashCode() %
							numQueues)).offer(item, packet.getPriority().ordinal());
		//ret = nullQueue.offer(item, packet.getPriority().ordinal());
		}
		if (!ret) {
			++droppedPackets;
		}
		return ret;
	}

}
