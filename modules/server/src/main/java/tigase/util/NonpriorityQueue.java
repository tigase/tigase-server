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

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.LinkedBlockingQueue;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Feb 9, 2010 11:32:30 AM
 *
 * @param <E>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class NonpriorityQueue<E> extends PriorityQueueAbstract<E> {
	private LinkedBlockingQueue<E> queue = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public NonpriorityQueue() {}

	protected NonpriorityQueue(int maxSize) {
		init(0, maxSize);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public final void init(int maxPriority, int maxSize) {
		queue = new LinkedBlockingQueue<E>(maxSize);
	}

	@Override
	public boolean offer(E element, int priority) {
		return queue.offer(element);
	}

	@Override
	public void put(E element, int priority) throws InterruptedException {
		queue.put(element);
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setMaxSize(int maxSize) {

//  TODO:
//  The code below causes a dead-lock as the take() method waits on the
//  old queue without any chance to be waken up. Up to now I haven't
//   found a way to awake the thread waiting on the take() method.
//     LinkedBlockingQueue<E> oldQueue = queue;
//     int newSize = Math.max(oldQueue.size(), maxSize);
//
//     queue = new LinkedBlockingQueue<E>(newSize);
//     oldQueue.drainTo(queue);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public int[] size() {
		int[] result = new int[1];

		result[0] = queue.size();

		return result;
	}

	@Override
	public E take() throws InterruptedException {
		return queue.take();
	}

	@Override
	public int totalSize() {
		return queue.size();
	}
}
