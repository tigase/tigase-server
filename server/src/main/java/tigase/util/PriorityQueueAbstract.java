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

//~--- classes ----------------------------------------------------------------

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
public abstract class PriorityQueueAbstract<E> {

	/** Field description */
	public static final String NONPRIORITY_QUEUE = "nonpriority-queue";

	/** Field description */
	public static final String QUEUE_IMPLEMENTATION = "queue-implementation";

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param maxPriority
	 * @param maxSize
	 */
	public abstract void init(int maxPriority, int maxSize);

	// public boolean offer(E element, int priority, String owner) {

	/**
	 * Method description
	 *
	 *
	 * @param element
	 * @param priority
	 *
	 * 
	 */
	public abstract boolean offer(E element, int priority);

	// public void put(E element, int priority, String owner) throws InterruptedException {

	/**
	 * Method description
	 *
	 *
	 * @param element
	 * @param priority
	 *
	 * @throws InterruptedException
	 */
	public abstract void put(E element, int priority) throws InterruptedException;

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param maxSize
	 */
	public abstract void setMaxSize(int maxSize);

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public abstract int[] size();

	// public E take(String owner) throws InterruptedException {

	/**
	 * Method description
	 *
	 *
	 * 
	 *
	 * @throws InterruptedException
	 */
	public abstract E take() throws InterruptedException;

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public abstract int totalSize();

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param maxPriority
	 * @param maxSize
	 * @param <E>
	 *
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static <E> PriorityQueueAbstract<E> getPriorityQueue(int maxPriority, int maxSize) {
		PriorityQueueAbstract<E> result = null;
		String queue_class = System.getProperty(QUEUE_IMPLEMENTATION, null);

		if ((queue_class == null) || queue_class.isEmpty()) {
			if (Boolean.getBoolean(NONPRIORITY_QUEUE)) {
				result = new NonpriorityQueue<E>(maxSize);
			} else {
				result = new PriorityQueueRelaxed<E>(maxPriority, maxSize);
			}
		} else {
			try {
				result = (PriorityQueueAbstract<E>) Class.forName(queue_class).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

			result.init(maxPriority, maxSize);
		}

//  System.out.println("Initialized queue implementation: " + result.getClass().getName());
		return result;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
