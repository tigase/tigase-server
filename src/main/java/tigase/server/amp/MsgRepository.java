/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.amp;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import tigase.db.MsgRepositoryIfc;
import tigase.db.TigaseDBException;
import tigase.osgi.ModulesManagerImpl;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

/**
 *
 * @author andrzej
 */
public abstract class MsgRepository<T> implements MsgRepositoryIfc {
	
	public static final long MSGS_STORE_LIMIT_VAL = 100;
	public static final String MSGS_STORE_LIMIT_KEY = "store-limit";
	
	protected static final int MAX_QUEUE_SIZE = 1000;
	
	private static final Map<String, MsgRepositoryIfc> repos =
			new ConcurrentSkipListMap<String, MsgRepositoryIfc>();
	

	public static MsgRepositoryIfc getInstance(String cls, String id_string) throws TigaseDBException {
		try {
			String key = cls + "#" + id_string;
			MsgRepositoryIfc result = repos.get(key);

			if (result == null) {
				result = (MsgRepositoryIfc) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repos.put(key, result);
			}

			return result;
		} catch (Exception ex) {
			throw new TigaseDBException("Could not create instance of " + cls + " for uri " + id_string, ex);
		}
	}

	protected SimpleParser parser = SingletonFactory.getParserInstance();	
	protected long earliestOffline = Long.MAX_VALUE;
	protected DelayQueue<MsgDBItem> expiredQueue = new DelayQueue<MsgDBItem>();
		
	protected abstract void loadExpiredQueue(int max);
	protected abstract void loadExpiredQueue(Date expired);
	protected abstract void deleteMessage(T db_id);
	
	/**
	 * Method description
	 * 
	 * @param time
	 * @param delete
	 * 
	 */
	@Override
	public Element getMessageExpired(long time, boolean delete) {
		if (expiredQueue.size() == 0) {

			// If the queue is empty load it with some elements
			loadExpiredQueue(MAX_QUEUE_SIZE);
		} else {

			// If the queue is not empty, check whether recently saved off-line
			// message
			// is due to expire sooner then the head of the queue.
			MsgDBItem item = expiredQueue.peek();

			if ((item != null) && (earliestOffline < item.expired.getTime())) {

				// There is in fact off-line message due to expire sooner then the head
				// of the
				// queue. Load all off-line message due to expire sooner then the first
				// element
				// in the queue.
				loadExpiredQueue(item.expired);
			}
		}

		MsgDBItem item = null;

		while (item == null) {
			try {
				item = expiredQueue.take();
			} catch (InterruptedException ex) {
			}
		}

		if (delete) {
			deleteMessage(item.db_id);
		}

		return item.msg;
	}
	
	// ~--- inner classes --------------------------------------------------------

	protected class MsgDBItem implements Delayed {
		public final T db_id;
		public final Date expired;
		public final Element msg;

		// ~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 * 
		 * @param db_id
		 * @param msg
		 * @param expired
		 */
		public MsgDBItem(T db_id, Element msg, Date expired) {
			this.db_id = db_id;
			this.msg = msg;
			this.expired = expired;
		}

		// ~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 * 
		 * @param o
		 * 
		 */
		@Override
		public int compareTo(Delayed o) {
			return (int) (getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
		}

		// ~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 * 
		 * @param unit
		 * 
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(expired.getTime() - System.currentTimeMillis(),
					TimeUnit.MILLISECONDS);
		}
	}	
}
