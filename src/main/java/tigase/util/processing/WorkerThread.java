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
package tigase.util.processing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Apr 21, 2009 9:02:57 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class WorkerThread
		extends Thread {

	protected static final Logger log = Logger.getLogger(WorkerThread.class.getName());

	private long averageProcessingTime = 0;

	//private PriorityQueueAbstract<QueueItem> queue = null;
	private LinkedBlockingQueue<QueueItem> queue = null;
	private long runsCnt = 0;
	private boolean stopped = false;

	public abstract WorkerThread getNewInstance();

	public abstract void process(QueueItem item);

	public long getAverageProcessingTime() {
		return averageProcessingTime;
	}

	public long getRunsCounter() {
		return runsCnt;
	}

	public boolean offer(QueueItem item) {
		return queue.offer(item);
	}

	@Override
	public void run() {
		QueueItem item = null;

		while (!stopped) {
			try {
				item = queue.take();

				long start = System.currentTimeMillis();

				process(item);

				long end = System.currentTimeMillis() - start;

				if (end > 0) {
					averageProcessingTime = (averageProcessingTime + end) / 2;
				}
			} catch (Exception e) {
				if (!stopped) {
					log.log(Level.SEVERE,
							this.getClass().getName() + ",(" + getName() + ") Exception during packet processing: " +
									item.getPacket(), e);
				}
			}

			++runsCnt;
		}
	}

	public void setQueueMaxSize(int maxSize) {
		LinkedBlockingQueue<QueueItem> oldQueue = queue;

		queue = new LinkedBlockingQueue<QueueItem>(maxSize);

		if (oldQueue != null) {
			queue.addAll(oldQueue);
		}
	}

	public int size() {
		return queue.size();
	}

	public void shutdown() {
		stopped = true;
		try {
			this.interrupt();
		} catch (Exception ex) {
		}
	}

}

