package tigase.map;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tigase.disteventbus.CombinedEventBus;
import tigase.disteventbus.EventBus;
import tigase.disteventbus.xmlbus.EventHandler;
import tigase.xml.Element;

public class ClusterMapFactoryTest {

	@Test
	public void testCreateMap() throws Exception {
		final ClusterMapFactory factory = new ClusterMapFactory();
		factory.setEventBus(new CombinedEventBus());
		final EventBus eventBus = factory.getEventBus();

		final Element[] createdEvent = new Element[] { null };
		eventBus.addHandler("NewMapCreated", "tigase:clustered:map", new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				Assert.assertNull(createdEvent[0]);
				createdEvent[0] = event;
			}
		});

		Map<String, String> map = factory.createMap("test", String.class, String.class, "1", "2", "3");

		Thread.sleep(100);

		Assert.assertNotNull(createdEvent[0]);

	}

	@Test
	public void testDestroyMap() throws Exception {
		final ClusterMapFactory factory = new ClusterMapFactory();
		factory.setEventBus(new CombinedEventBus());
		final EventBus eventBus = factory.getEventBus();

		final Element[] destroyedEvent = new Element[] { null };
		eventBus.addHandler("MapDestroyed", "tigase:clustered:map", new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				Assert.assertNull(destroyedEvent[0]);
				destroyedEvent[0] = event;
			}
		});

		final Map<String, String> map = factory.createMap("test2", String.class, String.class, "1", "2", "3");

		factory.destroyMap(map);

		Thread.sleep(100);

		Assert.assertNotNull(destroyedEvent[0]);
	}

	@Test
	public void testPutToMap() throws Exception {
		final ClusterMapFactory factory = new ClusterMapFactory();
		factory.setEventBus(new CombinedEventBus());
		final EventBus eventBus = factory.getEventBus();

		final boolean[] received = new boolean[] { false };

		final Map<String, String> map = factory.createMap("test", String.class, String.class);

		eventBus.addHandler("ElementAdd", "tigase:clustered:map", new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				received[0] = true;
				Assert.assertEquals("kluczyk", event.getCData(new String[] { "ElementAdd", "item", "key" }));
				Assert.assertEquals("wartosc", event.getCData(new String[] { "ElementAdd", "item", "value" }));
				Assert.assertEquals(((DMap) map).mapID, event.getCData(new String[] { "ElementAdd", "uid" }));
			}
		});

		map.put("kluczyk", "wartosc");

		Thread.sleep(100);

		Assert.assertTrue(received[0]);
	}

	@Test
	public void testRemoteCreatedMap() throws Exception {
		Element eventCreate = new Element("NewMapCreated", new String[] { "xmlns" }, new String[] { "tigase:clustered:map" });
		eventCreate.addChild(new Element("type", "test"));
		eventCreate.addChild(new Element("uid", "1-2-3"));
		eventCreate.addChild(new Element("keyClass", "java.lang.String"));
		eventCreate.addChild(new Element("valueClass", "java.lang.String"));
		eventCreate.addChild(new Element("param", "1"));
		eventCreate.addChild(new Element("param", "2"));

		final ClusterMapFactory factory = new ClusterMapFactory();
		factory.setEventBus(new CombinedEventBus());
		final EventBus eventBus = factory.getEventBus();

		final Map[] maps = new Map[] { null };
		eventBus.addHandler(MapCreatedEventHandler.MapCreatedEvent.class, new MapCreatedEventHandler() {
			@Override
			public void onMapCreated(Map map, String type, String... parameters) {
				maps[0] = map;
				Assert.assertEquals("test", type);
				Assert.assertArrayEquals(new String[] { "1", "2" }, parameters);
			}
		});

		factory.onNewMapCreated(eventCreate);

		Thread.sleep(100);

		Assert.assertNotNull("It seems map was not created", maps[0]);
		Assert.assertEquals("test", ((DMap) maps[0]).type);

		Element eventAdd = new Element("ElementAdd", new String[] { "xmlns" }, new String[] { "tigase:clustered:map" });
		eventAdd.addChild(new Element("uid", "1-2-3"));
		Element i = new Element("item");
		i.addChild(new Element("key", "xKEY"));
		i.addChild(new Element("value", "xVALUE"));
		eventAdd.addChild(i);
		i = new Element("item");
		i.addChild(new Element("key", "yKEY"));
		i.addChild(new Element("value", "yVALUE"));
		eventAdd.addChild(i);

		factory.onMapElementAdd(eventAdd);

		Assert.assertEquals("xVALUE", maps[0].get("xKEY"));
		Assert.assertEquals("yVALUE", maps[0].get("yKEY"));
		Assert.assertEquals(2, maps[0].size());

		Element eventDel = new Element("ElementRemove", new String[] { "xmlns" }, new String[] { "tigase:clustered:map" });
		eventDel.addChild(new Element("uid", "1-2-3"));
		i = new Element("item");
		i.addChild(new Element("key", "xKEY"));
		eventDel.addChild(i);
		factory.onMapElementRemove(eventDel);

		Assert.assertNull(maps[0].get("xKEY"));
		Assert.assertEquals(1, maps[0].size());

		Element eventClear = new Element("MapClear", new String[] { "xmlns" }, new String[] { "tigase:clustered:map" });
		eventClear.addChild(new Element("uid", "1-2-3"));
		factory.onMapClear(eventClear);

		Assert.assertEquals(0, maps[0].size());

		final boolean[] received = new boolean[] { false };
		eventBus.addHandler(MapDestroyedEventHandler.MapDestroyedEvent.class, new MapDestroyedEventHandler() {
			@Override
			public void onMapDestroyed(Map mapX, String type) {
				Assert.assertEquals(maps[0], mapX);
				received[0] = true;
			}
		});

		Assert.assertNotNull(factory.getMap("1-2-3"));

		Element eventDestroy = new Element("MapDestroyed", new String[] { "xmlns" }, new String[] { "tigase:clustered:map" });
		eventDestroy.addChild(new Element("uid", "1-2-3"));
		factory.onMapDestroyed(eventDestroy);

		Assert.assertNull(factory.getMap("1-2-3"));
		Thread.sleep(100);
		Assert.assertTrue(received[0]);
	}
}