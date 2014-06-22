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

package tigase.util;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Apr 21, 2009 8:50:50 PM
 *
 * @param <E>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ProcessingThreads<E extends WorkerThread> {
	private static final Logger log = Logger.getLogger(ProcessingThreads.class.getName());

	//~--- fields ---------------------------------------------------------------

	private long droppedPackets = 0;

	// private int maxQueueSize = 10000;
	private String name = null;

	// Packets are put in queues in such a way that all packets for the same
	// user end-up in the same queue. This is important in some cases as
	// packet processing order does matter in some cases, especially for
	// roster processing.
	// Therefore it is also recommended that there is a single thread for
	// each queue but we can ditribute load increasing number of queues.
	// private int numQueues = 2;
	private int numWorkerThreads = 1;
	private ArrayList<E> workerThreads = null;

	//~--- constructors ---------------------------------------------------------

//private ArrayList<PriorityQueueAbstract<QueueItem>> queues =
//  new ArrayList<PriorityQueueAbstract<QueueItem>>(1);

	/**
	 * Constructs ...
	 *
	 *
	 * @param worker
	 * @param numWorkerThreads
	 * @param maxQueueSize
	 * @param name
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings({ "unchecked" })
	public ProcessingThreads(E worker, int numWorkerThreads, int maxQueueSize, String name)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		// this.numQueues = numQueues;
		// this.maxQueueSize = maxQueueSize;
		this.numWorkerThreads = numWorkerThreads;
		workerThreads = new ArrayList<E>(numWorkerThreads);
		this.name = name;

//  for (int i = 0; i < numQueues; i++) {
		// LinkedBlockingQueue<QueueItem> queue = new LinkedBlockingQueue<QueueItem>(maxQueueSize);
//  queues.add(queue);
		for (int j = 0; j < numWorkerThreads; j++) {
			WorkerThread t = worker.getNewInstance();

			t.setQueueMaxSize(maxQueueSize);
			t.setDaemon(true);
			t.setName(name + " Queue Worker " + j);
			t.start();
			workerThreads.add((E) t);
			log.log(Level.FINEST, "Created worker thread: {0}, queueSize: {1}", new Object[] {t.getName(), maxQueueSize});
		}

//  }
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param processor
	 * @param packet
	 * @param conn
	 *
	 *
	 */
	public boolean addItem(XMPPProcessorIfc processor, Packet packet, XMPPResourceConnection conn) {
		boolean ret = false;
		QueueItem item = new QueueItem(processor, packet, conn);

		try {
			if ((item.getConn() != null) && item.getConn().isAuthorized()) {

				// Queueing packets per user...
				ret = workerThreads.get(Math.abs(conn.getJID().getBareJID().hashCode())
						% numWorkerThreads).offer(item);

//      ret = queues.get(Math.abs(conn.getJID().getBareJID().hashCode()
//          % numQueues)).offer(item, packet.getPriority().ordinal());
			} else {
				if (packet.getPacketFrom() != null) {

					// Queueing packets per user's connection...
					ret = workerThreads.get(Math.abs(packet.getPacketFrom().hashCode())
							% numWorkerThreads).offer(item);
				} else {

					// Otherwise per destination address
					// If the packet elemTo is set then used it, otherwise just packetTo:
					if (packet.getStanzaTo() != null) {
						ret = workerThreads.get(Math.abs(packet.getStanzaTo().getBareJID().hashCode())
								% numWorkerThreads).offer(item);

//          ret = queues.get(Math.abs(packet.getStanzaTo().hashCode() % numQueues)).offer(item,
//              packet.getPriority().ordinal());
					} else {
						ret = workerThreads.get(Math.abs(packet.getTo().hashCode())).offer(item);

//          ret = queues.get(Math.abs(packet.getTo().hashCode() % numQueues)).offer(item,
//              packet.getPriority().ordinal());
					}
				}
			}
		} catch (Exception e) {

			// This should not happen, but just in case until we are sure all
			// cases are catched.
			// Otherwise per destination address
			// If the packet elemTo is set then used it, otherwise just packetTo:
			if (packet.getStanzaTo() != null) {
				ret = workerThreads.get(Math.abs(packet.getStanzaTo().getBareJID().hashCode())
						% numWorkerThreads).offer(item);
			} else {
				ret = workerThreads.get(Math.abs(packet.getTo().hashCode()) % numWorkerThreads).offer(item);
			}

			// ret = nullQueue.offer(item, packet.getPriority().ordinal());
		}

		if ( !ret) {
			++droppedPackets;

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet dropped due to queue overflow: {0}", packet);
			}
		}

		return ret;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 */
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

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public long getDroppedPackets() {
		return droppedPackets;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public String getName() {
		return name;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public int getTotalQueueSize() {
		int ret = 0;

		for (E pq : workerThreads) {
			ret += pq.size();
		}

		return ret;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public int getTotalRuns() {
		int ret = 0;

		for (WorkerThread workerThread : workerThreads) {
			ret += workerThread.getRunsCounter();
		}

		return ret;
	}

        public void shutdown() {
                for (WorkerThread workerThread : workerThreads) {
                        workerThread.shutdown();
                }
        }
///**
// * Method description
// *
// *
// *
// */
//public E getWorkerThread() {
//  try {
//    return workerThreads.getFirst();
//  } catch (Exception e) {
//    return null;
//  }
//}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
