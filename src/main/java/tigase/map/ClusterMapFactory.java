package tigase.map;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.xmlbus.EventHandler;
import tigase.kernel.TypesConverter;
import tigase.xml.Element;

public class ClusterMapFactory {

	private final static String MAP_XMLNS = "tigase:clustered:map";

	private final static String MAP_CREATED_EVENT_NAME = "NewMapCreated";

	private final static String MAP_DESTROYED_EVENT_NAME = "MapDestroyed";

	private final static String MAP_CLEAR_EVENT_NAME = "MapClear";

	private final static String ELEMENT_REMOVE_EVENT_NAME = "ElementRemove";

	private final static String ELEMENT_ADD_EVENT_NAME = "ElementAdd";
	private static ClusterMapFactory instance;
	private final ConcurrentHashMap<String, DMap> maps = new ConcurrentHashMap<>();
	private EventBus eventBus;

	private final DMap.DMapListener mapListener = new DMap.DMapListener() {
		@Override
		public void onClear(String mapID) {
			Element event = new Element(MAP_CLEAR_EVENT_NAME, new String[]{"xmlns"}, new String[]{MAP_XMLNS});
			event.addChild(new Element("uid", mapID));
			eventBus.fire(event);
		}

		@Override
		public void onPut(String mapID, Object key, Object value) {
			Element event = new Element(ELEMENT_ADD_EVENT_NAME, new String[]{"xmlns"}, new String[]{MAP_XMLNS});
			event.addChild(new Element("uid", mapID));

			Element item = new Element("item");
			item.addChild(new Element("key", TypesConverter.toString(key)));
			item.addChild(new Element("value", TypesConverter.toString(value)));
			event.addChild(item);

			eventBus.fire(event);
		}

		@Override
		public void onPutAll(String mapID, Map<?, ?> m) {
			Element event = new Element(ELEMENT_ADD_EVENT_NAME, new String[]{"xmlns"}, new String[]{MAP_XMLNS});
			event.addChild(new Element("uid", mapID));

			for (Map.Entry<?, ?> en : m.entrySet()) {
				Element item = new Element("item");
				item.addChild(new Element("key", TypesConverter.toString(en.getKey())));
				item.addChild(new Element("value", TypesConverter.toString(en.getValue())));
				event.addChild(item);
			}
			eventBus.fire(event);
		}

		@Override
		public void onRemove(String mapID, Object key) {
			Element event = new Element(ELEMENT_REMOVE_EVENT_NAME, new String[]{"xmlns"}, new String[]{MAP_XMLNS});
			event.addChild(new Element("uid", mapID));
			Element item = new Element("item");
			item.addChild(new Element("key", TypesConverter.toString(key)));
			event.addChild(item);
			eventBus.fire(event);
		}
	};

	ClusterMapFactory() {
		this.eventBus = EventBusFactory.getInstance();
		this.eventBus.addHandler(MAP_CREATED_EVENT_NAME, MAP_XMLNS, new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				if (event.getAttributeStaticStr("remote") != null) {
					ClusterMapFactory.this.onNewMapCreated(event);
				}
			}
		});
		this.eventBus.addHandler(MAP_DESTROYED_EVENT_NAME, MAP_XMLNS, new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				if (event.getAttributeStaticStr("remote") != null) {
					ClusterMapFactory.this.onMapDestroyed(event);
				}
			}
		});
		this.eventBus.addHandler(MAP_CLEAR_EVENT_NAME, MAP_XMLNS, new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				if (event.getAttributeStaticStr("remote") != null) {
					ClusterMapFactory.this.onMapClear(event);
				}
			}
		});
		this.eventBus.addHandler(ELEMENT_ADD_EVENT_NAME, MAP_XMLNS, new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				if (event.getAttributeStaticStr("remote") != null) {
					ClusterMapFactory.this.onMapElementAdd(event);
				}
			}
		});
		this.eventBus.addHandler(ELEMENT_REMOVE_EVENT_NAME, MAP_XMLNS, new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				if (event.getAttributeStaticStr("remote") != null) {
					ClusterMapFactory.this.onMapElementRemove(event);
				}
			}
		});
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

		Element event = new Element(MAP_CREATED_EVENT_NAME, new String[]{"xmlns"}, new String[]{MAP_XMLNS});
		event.addChild(new Element("type", type));
		event.addChild(new Element("uid", uid));
		event.addChild(new Element("keyClass", keyClass.getName()));
		event.addChild(new Element("valueClass", valueClass.getName()));
		if (params != null) {
			for (String param : params) {
				event.addChild(new Element("param", param));
			}
		}
		eventBus.fire(event);

		DMap<K, V> map = new DMap<K, V>(uid, type, this.mapListener, keyClass, valueClass);
		maps.put(uid, map);

		return map;
	}

	public void destroyMap(Map map) {
		if (map instanceof DMap) {
			Element event = new Element(MAP_DESTROYED_EVENT_NAME, new String[]{"xmlns"}, new String[]{MAP_XMLNS});
			event.addChild(new Element("uid", ((DMap) map).mapID));
			event.addChild(new Element("type", ((DMap) map).type));
			eventBus.fire(event);
			this.maps.remove(((DMap) map).mapID);
		}
	}

	private void fireOnMapCreated(Map map, String type, String... parameters) {
		MapCreatedEventHandler.MapCreatedEvent event = new MapCreatedEventHandler.MapCreatedEvent(map, type, parameters);
		eventBus.fire(event);
	}

	private void fireOnMapDestroyed(final Map map, final String type) {
		MapDestroyedEventHandler.MapDestroyedEvent event = new MapDestroyedEventHandler.MapDestroyedEvent(map, type);
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

	void onMapClear(Element event) {
		final String uid = event.getCData(new String[]{MAP_CLEAR_EVENT_NAME, "uid"});
		DMap map = this.maps.get(uid);
		map.clearNoEvent();
	}

	void onMapDestroyed(Element event) {
		final String uid = event.getCData(new String[]{MAP_DESTROYED_EVENT_NAME, "uid"});
		DMap map = this.maps.remove(uid);
		if (map != null) {
			fireOnMapDestroyed(map, map.type);
		}
	}

	void onMapElementAdd(Element event) {
		final String uid = event.getCData(new String[]{ELEMENT_ADD_EVENT_NAME, "uid"});
		final DMap map = this.maps.get(uid);

		List<Element> items = event.findChildren(e -> e.getName().equals("item"));
		for (Element el : items) {
			String k = el.getCData(new String[]{"item", "key"});
			String v = el.getCData(new String[]{"item", "value"});

			Object key = TypesConverter.convert(k, map.keyClass);
			Object value = TypesConverter.convert(v, map.valueClass);

			map.putNoEvent(key, value);
		}

	}

	void onMapElementRemove(Element event) {
		final String uid = event.getCData(new String[]{ELEMENT_REMOVE_EVENT_NAME, "uid"});
		DMap map = this.maps.get(uid);

		List<Element> items = event.findChildren(e -> e.getName().equals("item"));
		for (Element el : items) {
			String k = el.getCData(new String[]{"item", "key"});
			Object key = TypesConverter.convert(k, map.keyClass);
			map.removeNoEvent(key);
		}
	}

	void onNewMapCreated(final Element event) {
		final String uid = event.getCData(new String[]{MAP_CREATED_EVENT_NAME, "uid"});
		final String type = event.getCData(new String[]{MAP_CREATED_EVENT_NAME, "type"});
		final Class keyClass = TypesConverter.convert(event.getCData(new String[]{MAP_CREATED_EVENT_NAME, "keyClass"}),
				Class.class);
		final Class valueClass = TypesConverter.convert(event.getCData(new String[]{MAP_CREATED_EVENT_NAME, "valueClass"}),
				Class.class);


		String[] parameters =
				event.mapChildren(element -> element.getName().equals("param"), element -> element.getCData()).toArray(new String[]{});

		DMap map = new DMap(uid, type, mapListener, keyClass, valueClass);
		maps.put(uid, map);

		fireOnMapCreated(map, type, parameters);
	}

}
