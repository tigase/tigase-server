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
package tigase.map;

import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.eventbus.impl.EventName;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.TypesConverter;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClusterMapFactory {

	private final static EventName NEWMAP_EVENT_NAME = new EventName(NewMapCreatedEvent.class);
	private static ClusterMapFactory instance;
	private final Logger log = Logger.getLogger(this.getClass().getName());
	private final ConcurrentHashMap<String, DMap> maps = new ConcurrentHashMap<>();
	private final TypesConverter typesConverter = new DefaultTypesConverter();
	private EventBus eventBus;
	private final DMap.DMapListener mapListener = new DMap.DMapListener() {
		@Override
		public void onClear(DMap map) {
			MapClearEvent event = new MapClearEvent();
			event.setUid(map.getUid());
			eventBus.fire(event);
		}

		@Override
		public void onPut(DMap map, Object key, Object value) {
			ElementAddEvent event = new ElementAddEvent();
			event.setUid(map.getUid());
			event.setKey(typesConverter.toString(key));
			event.setValue(typesConverter.toString(value));
			eventBus.fire(event);
		}

		@Override
		public void onPutAll(DMap map, Map<?, ?> m) {
			for (Map.Entry<?, ?> en : m.entrySet()) {
				ElementAddEvent event = new ElementAddEvent();
				event.setUid(map.getUid());
				event.setKey(typesConverter.toString(en.getKey()));
				event.setValue(typesConverter.toString(en.getValue()));
				eventBus.fire(event);
			}
		}

		@Override
		public void onRemove(DMap map, Object key) {
			ElementRemoveEvent event = new ElementRemoveEvent();
			event.setUid(map.getUid());
			event.setKey(typesConverter.toString(key));
			eventBus.fire(event);
		}
	};

	public static final ClusterMapFactory get() {
		if (instance == null) {
			instance = new ClusterMapFactory();
		}
		return instance;
	}

	ClusterMapFactory() {
		this.eventBus = EventBusFactory.getInstance();
		this.eventBus.registerAll(this);
	}

	public <K, V> Map<K, V> createMap(final String uid, final Class<K> keyClass, final Class<V> valueClass,
									  final String... params) {

		NewMapCreatedEvent event = new NewMapCreatedEvent();
		event.setUid(uid);
		event.setKeyClass(keyClass);
		event.setValueClass(valueClass);
		event.setParams(params);
		eventBus.fire(event);

		DMap<K, V> map = maps.computeIfAbsent(uid, (u) -> new DMap<K, V>(uid, this.mapListener, keyClass, valueClass));

		return map;
	}

	public void destroyMap(Map map) {
		if (map instanceof DMap) {
			MapDestroyEvent event = new MapDestroyEvent();
			event.setUid(((DMap) map).getUid());

			eventBus.fire(event);
			this.maps.remove(((DMap) map).getUid(), map);
		}
	}

	private void fireOnMapCreated(Map map, String uid, String... parameters) {
		MapCreatedEvent event = new MapCreatedEvent(map, uid, parameters);
		eventBus.fire(event);
	}

	private void fireOnMapDestroyed(final Map map, final String uid) {
		MapDestroyedEvent event = new MapDestroyedEvent(map, uid);
		eventBus.fire(event);
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public <K, V> Map<K, V> getMap(String uid) {
		return this.maps.get(uid);
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onMapClear(MapClearEvent event) {
		final String uid = event.getUid();
		DMap map = this.maps.get(uid);
		if (map == null) {
			log.log(Level.FINE, "No map '" + uid + "' created on this node! Ignoring MapClear event.");
			return;
		}
		map.clearNoEvent();
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onMapDestroyed(MapDestroyEvent event) {
		final String uid = event.getUid();
		DMap map = this.maps.remove(uid);
		if (map != null) {
			fireOnMapDestroyed(map, map.uid);
		}
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onMapElementAdd(ElementAddEvent event) {
		final String uid = event.getUid();
		final DMap map = this.maps.get(uid);

		if (map == null) {
			log.log(Level.FINE, "No map '" + uid + "' created on this node! Ignoring ElementAdd item event.");
			return;
		}

		String k = event.getKey();
		String v = event.getValue();

		Object key = typesConverter.convert(k, map.keyClass);
		Object value = typesConverter.convert(v, map.valueClass);

		map.putNoEvent(key, value);
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onMapElementRemove(ElementRemoveEvent event) {
		final String uid = event.getUid();
		DMap map = this.maps.get(uid);

		if (map == null) {
			log.log(Level.FINE, "No map '" + uid + "' created on this node! Ignoring ElementRemove item event.");
			return;
		}

		String k = event.getKey();
		Object key = typesConverter.convert(k, map.keyClass);
		map.removeNoEvent(key);
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onNewMapCreated(final NewMapCreatedEvent event) {
		final String uid = event.getUid();
		if (!maps.containsKey(uid)) {
			final Class keyClass = event.getKeyClass();
			final Class valueClass = event.getValueClass();

			String[] parameters = event.getParams();

			DMap map = new DMap(uid, mapListener, keyClass, valueClass);
			maps.put(uid, map);
			fireOnMapCreated(map, uid, parameters);
		} else {
			DMap map = this.maps.get(uid);
			mapListener.onPutAll(map, map);
		}
	}

	public static class ElementAddEvent
			implements Serializable {

		private String key;
		private String uid;
		private String value;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class ElementRemoveEvent
			implements Serializable {

		private String key;
		private String uid;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}
	}

	public static class MapClearEvent
			implements Serializable {

		private String uid;

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}
	}

	public static class MapDestroyEvent
			implements Serializable {

		private String uid;

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}
	}

	public static class NewMapCreatedEvent
			implements Serializable {

		private Class keyClass;
		private String[] params;
		private String uid;
		private Class valueClass;

		public Class getKeyClass() {
			return keyClass;
		}

		public void setKeyClass(Class keyClass) {
			this.keyClass = keyClass;
		}

		public String[] getParams() {
			return params;
		}

		public void setParams(String[] params) {
			this.params = params;
		}

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}

		public Class getValueClass() {
			return valueClass;
		}

		public void setValueClass(Class valueClass) {
			this.valueClass = valueClass;
		}

	}

}
