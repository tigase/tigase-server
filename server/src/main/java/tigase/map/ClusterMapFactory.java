/*
 * ClusterMapFactory.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 */

package tigase.map;

import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.TypesConverter;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterMapFactory {

	private static ClusterMapFactory instance;
	private final TypesConverter typesConverter = new DefaultTypesConverter();
	private final ConcurrentHashMap<String, DMap> maps = new ConcurrentHashMap<>();
	private EventBus eventBus;
	private final DMap.DMapListener mapListener = new DMap.DMapListener() {
		@Override
		public void onClear(String mapID) {
			MapClearEvent event = new MapClearEvent();
			event.setUid(mapID);
			eventBus.fire(event);
		}

		@Override
		public void onPut(String mapID, Object key, Object value) {
			ElementAddEvent event = new ElementAddEvent();
			event.setUid(mapID);
			event.setKey(typesConverter.toString(key));
			event.setValue(typesConverter.toString(value));
			eventBus.fire(event);
		}

		@Override
		public void onPutAll(String mapID, Map<?, ?> m) {
			for (Map.Entry<?, ?> en : m.entrySet()) {
				ElementAddEvent event = new ElementAddEvent();
				event.setUid(mapID);
				event.setKey(typesConverter.toString(en.getKey()));
				event.setValue(typesConverter.toString(en.getValue()));
				eventBus.fire(event);
			}
		}

		@Override
		public void onRemove(String mapID, Object key) {
			ElementRemoveEvent event = new ElementRemoveEvent();
			event.setUid(mapID);
			event.setKey(typesConverter.toString(key));
			eventBus.fire(event);
		}
	};

	ClusterMapFactory() {
		this.eventBus = EventBusFactory.getInstance();
		this.eventBus.registerAll(this);
	}

	public static final ClusterMapFactory get() {
		if (instance == null) {
			instance = new ClusterMapFactory();
		}
		return instance;
	}

	public <K, V> Map<K, V> createMap(final String type, final Class<K> keyClass, final Class<V> valueClass,
			final String... params) {
		final String uid = UUID.randomUUID().toString();

		NewMapCreatedEvent event = new NewMapCreatedEvent();
		event.setType(type);
		event.setUid(uid);
		event.setKeyClass(keyClass);
		event.setValueClass(valueClass);
		event.setParams(params);
		eventBus.fire(event);

		DMap<K, V> map = maps.computeIfAbsent(uid, (u) -> new DMap<K, V>(uid, type, this.mapListener, keyClass, valueClass));

		return map;
	}

	public void destroyMap(Map map) {
		if (map instanceof DMap) {
			MapDestroyEvent event = new MapDestroyEvent();
			event.setUid(((DMap) map).mapID);
			event.setType(((DMap) map).type);

			eventBus.fire(event);
			this.maps.remove(((DMap) map).mapID, map);
		}
	}

	private void fireOnMapCreated(Map map, String type, String... parameters) {
		MapCreatedEvent event = new MapCreatedEvent(map, type, parameters);
		eventBus.fire(event);
	}

	private void fireOnMapDestroyed(final Map map, final String type) {
		MapDestroyedEvent event = new MapDestroyedEvent(map, type);
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
		map.clearNoEvent();
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onMapDestroyed(MapDestroyEvent event) {
		final String uid = event.getUid();
		DMap map = this.maps.remove(uid);
		if (map != null) {
			fireOnMapDestroyed(map, map.type);
		}
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onMapElementAdd(ElementAddEvent event) {
		final String uid = event.getUid();
		final DMap map = this.maps.get(uid);

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

		String k = event.getKey();
		Object key = typesConverter.convert(k, map.keyClass);
		map.removeNoEvent(key);
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	void onNewMapCreated(final NewMapCreatedEvent event) {
		final String uid = event.getUid();
		final String type = event.getType();
		final Class keyClass = event.getKeyClass();
		final Class valueClass = event.getValueClass();

		String[] parameters = event.getParams();

		DMap map = new DMap(uid, type, mapListener, keyClass, valueClass);
		maps.put(uid, map);

		fireOnMapCreated(map, type, parameters);
	}

	public static class NewMapCreatedEvent implements Serializable {

		private String type;
		private String uid;
		private Class keyClass;
		private Class valueClass;
		private String[] params;

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

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
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

	public static class MapDestroyEvent implements Serializable {

		private String uid;
		private String type;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}
	}

	public static class MapClearEvent implements Serializable {

		private String uid;

		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}
	}

	public static class ElementRemoveEvent implements Serializable {

		private String uid;
		private String key;

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

	public static class ElementAddEvent implements Serializable {

		private String uid;
		private String key;
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

}
