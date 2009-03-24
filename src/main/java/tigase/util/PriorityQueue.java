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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import tigase.server.AbstractMessageReceiver;

/**
 * Works like a LinkedBlockingQueue using the put() and take() methods but
 * with an additional priority integer parameter. The elemnt returned from
 * take() will honor the priority in such a way that all elements of a lower
 * priority will be returned before any elemens of a higher priority.
 * 
 * Modified proposition taken from Noa Resare:
 * http://resare.com/noa/ref/MultiPrioQueue.java
 *
 * @param <E> 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PriorityQueue<E> {

  /**
   * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log =
					Logger.getLogger(AbstractMessageReceiver.class.getName());

	private LinkedBlockingQueue<E>[] qs = null;
	private int lowestNonEmpty = Integer.MAX_VALUE;

	@SuppressWarnings({"unchecked"})
	public PriorityQueue(int maxPriority, int maxSize) {
		qs = new LinkedBlockingQueue[maxPriority + 1];
		for (int i = 0; i < qs.length; i++) {
			qs[i] = new LinkedBlockingQueue<E>(maxSize);
		}
	}

	public void setMaxSize(int maxSize) {
		for (int i = 0; i < qs.length; i++) {
			// We don't want to lose any data so the new size must
			// be enough to keep all exising elements
			LinkedBlockingQueue<E> oldQueue = qs[i];
			int newSize = Math.max(oldQueue.size(), maxSize);
			qs[i] = new LinkedBlockingQueue<E>(newSize);
			qs[i].addAll(oldQueue);
		}
	}

	//public void put(E element, int priority, String owner) throws InterruptedException {
	public void put(E element, int priority) throws InterruptedException {
		//add(element, priority, true, owner);
		add(element, priority, true);
	}

	//public boolean offer(E element, int priority, String owner) {
	public boolean offer(E element, int priority) {
		try {
			//return add(element, priority, false, owner);
			return add(element, priority, false);
		} catch (InterruptedException e) {
			log.warning("This should not happen, this is non-blocking operation.");
			return false;
		}
	}

	// TODO: Reduce synchronization but be carefull many threads are writing
	// to queues at the same time.
	//private boolean add(E element, int priority, boolean blocking, String owner)
	private boolean add(E element, int priority, boolean blocking)
					throws InterruptedException {
		if (priority < 0 || qs.length <= priority) {
			throw new IllegalArgumentException("parameter priority must be " +
							"between 0 and " + (qs.length - 1));
		}
		boolean result = true;
		LinkedBlockingQueue<E> q = qs[priority];
		if (blocking) {
			q.put(element);
//			log.finest("[" + owner + "] " + priority + " B element added: " + element.toString());
			//log.finest("" + priority + " B element added: " + element.toString());
		} else {
			result = q.offer(element);
			//log.finest("" + priority + " B element added: " + element.toString());
//			log.finest("[" + owner + "] " + priority + " NB element added: " +
//							element.toString() + ", result: " + result +
//							", lowestNonEmpty: " + lowestNonEmpty + ", size: " + q.size());
		}
		synchronized (this) {
			if (result) {
				if (priority < lowestNonEmpty) {
//					log.finest("[" + owner + "] setting new priority from " +
//									lowestNonEmpty + ", to: " + priority);
					lowestNonEmpty = priority;
				}
				this.notify();
			}
		}
		if (!result) {
			if (priority < qs.length - 1) {
				//result = add(element, priority + 1, blocking, owner);
				result = add(element, priority + 1, blocking);
			}
		}
		return result;
	}

	//public E take(String owner) throws InterruptedException {
	public E take() throws InterruptedException {
		E e = null;
		while (e == null) {
			synchronized (this) {
				try {
					// Safeguard for "java wait spurious wakeup", google for it
					// if you don't know it. I have just learned about it too...
					while (lowestNonEmpty == Integer.MAX_VALUE) {
//						log.finest("[" + owner + "] waiting...");
						this.wait();
					}
				} catch (InterruptedException ex) {}
				LinkedBlockingQueue<E> q = qs[lowestNonEmpty];
				//log.finest("" + lowestNonEmpty + " taking from queue: ");
				e = q.poll();
//				if (e != null) {
//					log.finest("[" + owner + "] " + lowestNonEmpty + " element read: " + e.toString());
//				} else {
//					log.finest("[" + owner + "] " + lowestNonEmpty + " NULL element read!");
//				}
				if (e == null || q.isEmpty()) {
					lowestNonEmpty = findNextNonEmpty();
//					log.finest("[" + owner + "] " + "new lowestNonEmpty: " + lowestNonEmpty);
				}
			}
		}
		return e;
	}

	private int findNextNonEmpty() {
		for (int i = 0; i < qs.length; i++) {
			if (!qs[i].isEmpty()) {
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}

	public int[] size() {
		int[] result = new int[qs.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = qs[i].size();
		}
		return result;
	}

	public int totalSize() {
		int result = 0;
		for (int i = 0; i < qs.length; i++) {
			result += qs[i].size();
		}
		return result;
	}

}
