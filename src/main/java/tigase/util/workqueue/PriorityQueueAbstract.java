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
package tigase.util.workqueue;

import tigase.annotations.TigaseDeprecated;
import tigase.sys.TigaseRuntime;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Works like a LinkedBlockingQueue using the put() and take() methods but with an additional priority integer
 * parameter. The elemnt returned from take() will honor the priority in such a way that all elements of a lower
 * priority will be returned before any elemens of a higher priority.
 * <br>
 * Modified proposition taken from Noa Resare: http://resare.com/noa/ref/MultiPrioQueue.java
 *
 * @param <E>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class PriorityQueueAbstract<E> {

	public static final String NONPRIORITY_QUEUE = "nonpriority-queue";
	public static final String QUEUE_IMPLEMENTATION = "queue-implementation";
	private static final Logger log = Logger.getLogger(PriorityQueueAbstract.class.getName());

	@SuppressWarnings("unchecked")
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public static <E> PriorityQueueAbstract<E> getPriorityQueue(int maxPriority, int maxSize) {
		Class<? extends PriorityQueueAbstract> result = null;
		String queue_class = System.getProperty(QUEUE_IMPLEMENTATION, null);

		if ((queue_class == null) || queue_class.isEmpty()) {
			if (Boolean.getBoolean(NONPRIORITY_QUEUE)) {
				result = NonpriorityQueue.class;
			} else {
				result = PriorityQueueRelaxed.class;
			}
		} else {
			try {
				result = (Class<? extends PriorityQueueAbstract>) Class.forName(queue_class);
			} catch (Exception e) {
				log.log(Level.FINEST, "Error crating priority queue", e);
				TigaseRuntime.getTigaseRuntime()
						.shutdownTigase(new String[]{
								"Error: Could not instantiate or initialize priority queue of class: " + queue_class,
								"Got exception: " + e.getMessage()});
			}
		}

		return getPriorityQueue(maxPriority, maxSize, result);
	}

	public static <E> PriorityQueueAbstract<E> getPriorityQueue(int maxPriority, int maxSize,
																Class<? extends PriorityQueueAbstract> queueClass) {
		try {
			PriorityQueueAbstract<E> result = queueClass.newInstance();
			result.init(maxPriority, maxSize);
			log.log(Level.CONFIG, "Initialized queue implementation: " + result.getClass().getName());
			return result;
		} catch (Exception e) {
			log.log(Level.FINEST, "Error crating priority queue", e);
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{
							"Error: Could not instantiate or initialize priority queue of class: " + queueClass,
							"Got exception: " + e.getMessage()});
		}
		return null;
	}

	public abstract void init(int maxPriority, int maxSize);

	public abstract boolean offer(E element, int priority);

	public abstract void put(E element, int priority) throws InterruptedException;

	public abstract void setMaxSize(int maxSize);

	public abstract int[] size();

	public abstract E take() throws InterruptedException;

	public abstract int totalSize();

}
