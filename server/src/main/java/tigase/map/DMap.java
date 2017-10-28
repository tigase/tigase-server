/*
 * DMap.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DMap<K, V> implements Map<K, V> {

	final String mapID;

	final DMapListener listener;

	final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();

	final Class<K> keyClass;

	final Class<V> valueClass;

	final String type;

	public DMap(String mapID, String type, DMapListener listener, final Class<K> keyClass, final Class<V> valueClass) {
		this.listener = listener;
		this.mapID = mapID;
		this.type = type;
		this.keyClass = keyClass;
		this.valueClass = valueClass;
	}

	@Override
	public void clear() {
		this.listener.onClear(this.mapID);
		map.clear();
	}

	public void clearNoEvent() {
		map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new SetWrapper<>(map.entrySet());
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return new SetWrapper<>(map.keySet());
	}

	@Override
	public V put(K key, V value) {
		this.listener.onPut(this.mapID, key, value);
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		this.listener.onPutAll(this.mapID, m);
		map.putAll(m);
	}

	public void putNoEvent(K key, V value) {
		map.put(key, value);
	}

	@Override
	public V remove(Object key) {
		listener.onRemove(this.mapID, key);
		return map.remove(key);
	}

	public V removeNoEvent(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public String toString() {
		return map.toString();
	}

	@Override
	public Collection<V> values() {
		return new CollectionWrapper<>(map.values());
	}

	interface DMapListener {

		void onClear(String mapID);

		void onPut(String mapID, Object key, Object value);

		void onPutAll(String mapID, Map<?, ?> m);

		void onRemove(String mapID, Object key);

	}

}
