/*
 * LRUConcurrentCache.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */

package tigase.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUConcurrentCache<K, V> {

	private final Map<K, V> cache;
	private final int limit;

	public LRUConcurrentCache(final int maxEntries) {
		this.limit = maxEntries;
		this.cache = new LinkedHashMap<K, V>(maxEntries, 0.75F, true) {
			private static final long serialVersionUID = -1236481390177598762L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > maxEntries;
			}
		};
	}

	public void clear() {
		synchronized (cache) {
			cache.clear();
		}
	}

	public boolean containsKey(K key) {
		synchronized (cache) {
			return cache.containsKey(key);
		}
	}

	public V get(K key) {
		synchronized (cache) {
			return cache.get(key);
		}
	}

	public void put(K key, V value) {
		synchronized (cache) {
			cache.put(key, value);
		}
	}

	public V remove(K key) {
		synchronized (cache) {
			return cache.remove(key);
		}
	}

	public int size() {
		return cache.size();
	}

	public int limit() {
		return limit;
	}

	@Override
	public String toString() {
		return "LRUConcurrentCache{" + "cache=" + cache + '}';
	}
}
