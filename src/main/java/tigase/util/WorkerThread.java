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

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	protected static final Logger log = Logger.getLogger(WorkerThread.class.getName());

	//~--- fields ---------------------------------------------------------------

	private long averageProcessingTime = 0;
	private PriorityQueueAbstract<QueueItem> queue = null;
	private long runsCnt = 0;
	private boolean stopped = false;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param queue
	 *
	 * @return
	 */
	public abstract WorkerThread getNewInstance(PriorityQueueAbstract<QueueItem> queue);

	//~--- methods --------------------------------------------------------------

//{
//  WorkerThread worker = new WorkerThread();
//  worker.setQueue(queue);
//  return worker;
//}

	/**
	 * Method description
	 *
	 *
	 * @param item
	 */
	public abstract void process(QueueItem item);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getAverageProcessingTime() {
		return averageProcessingTime;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getRunsCounter() {
		return runsCnt;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void run() {
		QueueItem item = null;

		while ( !stopped) {
			try {
				item = queue.take();

				long start = System.currentTimeMillis();

				process(item);

				long end = System.currentTimeMillis() - start;

				if (end > 0) {
					averageProcessingTime = (averageProcessingTime + end) / 2;
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception during packet processing: " + item.packet.toString(), e);
			}

			++runsCnt;
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param queue
	 */
	public void setQueue(PriorityQueueAbstract<QueueItem> queue) {
		this.queue = queue;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
