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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Apr 21, 2009 9:02:57 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class WorkerThread extends Thread {

  /**
   * Variable <code>log</code> is a class logger.
   */
  protected static final Logger log =
					Logger.getLogger(WorkerThread.class.getName());

	private boolean stopped = false;
	private long averageProcessingTime = 0;
	private PriorityQueue<QueueItem> queue = null;
	private long runsCnt = 0;

	public abstract WorkerThread getNewInstance(PriorityQueue<QueueItem> queue);
//	{
//		WorkerThread worker = new WorkerThread();
//		worker.setQueue(queue);
//		return worker;
//	}

	public void setQueue(PriorityQueue<QueueItem> queue) {
		this.queue = queue;
	}

	public abstract void process(QueueItem item);

	@Override
	public void run() {
		QueueItem item = null;
		while (!stopped) {
			try {
				item = queue.take();
				long start = System.currentTimeMillis();
				process(item);
				averageProcessingTime =
								(averageProcessingTime + (System.currentTimeMillis() - start)) /
								2;
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception during packet processing: " +
								item.packet.toString(), e);
			}
			++runsCnt;
		}
	}

  public long getAverageProcessingTime() {
		return averageProcessingTime;
	}

	public long getRunsCounter() {
		return runsCnt;
	}

}
