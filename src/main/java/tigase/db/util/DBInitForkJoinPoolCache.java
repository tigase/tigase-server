/*
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
package tigase.db.util;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBInitForkJoinPoolCache {

	private static final Logger log = Logger.getLogger(DBInitForkJoinPoolCache.class.getCanonicalName());

	public static final DBInitForkJoinPoolCache shared = new DBInitForkJoinPoolCache();

	private final HashMap<String, Item> cache = new HashMap<String, Item>();
	private Timer timer = null;

	public synchronized ForkJoinPool pool(String key, int concurrency) {
		Item item = cache.get(key);
		if (item == null) {
			item = new Item(key, concurrency);
			cache.put(key, item);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "creating fork-join thread pool for " + key);
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "reusing fork-join thread pool for " + key);
			}
		}
		if (timer == null) {
			timer = new Timer("dbinit-fork-join-pool-cache-timer", true);
		}
		item.restartTimer(timer);
		return item.getPool();
	}

	private synchronized void release(Item item) {
		cache.remove(item.getKey());
		if (cache.isEmpty()) {
			timer.cancel();
			timer = null;
		}
		item.getPool().shutdown();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "releasing fork-join thread pool for " + item.getKey());
		}
	}
	
	private class Item {
		private final String key;
		private final ForkJoinPool pool;
		private TimerTask timerTask;

		public Item(String key, int concurrency) {
			this.key = key;
			this.pool = new ForkJoinPool(concurrency);
		}

		public String getKey() {
			return key;
		}

		public ForkJoinPool getPool() {
			return pool;
		}

		public void restartTimer(Timer timer) {
			if (timerTask != null) {
				timerTask.cancel();
				timerTask = null;
			}
			timerTask = new TimerTask() {
				@Override
				public void run() {
					synchronized (DBInitForkJoinPoolCache.this) {
						if (Item.this.timerTask != this) {
							return;
						}
						DBInitForkJoinPoolCache.this.release(Item.this);
					}
				}
			};
			timer.schedule(timerTask, 60l * 1000);
		}
	}
}
