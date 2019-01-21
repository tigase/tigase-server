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

import org.junit.Assert;
import org.junit.Test;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventListener;
import tigase.eventbus.impl.EventBusImplementation;

import java.util.Map;

public class ClusterMapFactoryTest {

	@Test
	public void testCreateMap() throws Exception {
		final ClusterMapFactory factory = new ClusterMapFactory();
		factory.setEventBus(new EventBusImplementation());
		final EventBus eventBus = factory.getEventBus();

		final Object[] createdEvent = new Object[]{null};
		eventBus.addListener(ClusterMapFactory.NewMapCreatedEvent.class,
							 new EventListener<ClusterMapFactory.NewMapCreatedEvent>() {
								 @Override
								 public void onEvent(ClusterMapFactory.NewMapCreatedEvent event) {
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
		factory.setEventBus(new EventBusImplementation());
		final EventBus eventBus = factory.getEventBus();

		final Object[] destroyedEvent = new Object[]{null};
		eventBus.addListener(ClusterMapFactory.MapDestroyEvent.class,
							 new EventListener<ClusterMapFactory.MapDestroyEvent>() {
								 @Override
								 public void onEvent(ClusterMapFactory.MapDestroyEvent event) {
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
		factory.setEventBus(new EventBusImplementation());
		final EventBus eventBus = factory.getEventBus();

		final boolean[] received = new boolean[]{false};

		final Map<String, String> map = factory.createMap("test", String.class, String.class);

		eventBus.addListener(ClusterMapFactory.ElementAddEvent.class,
							 new EventListener<ClusterMapFactory.ElementAddEvent>() {
								 @Override
								 public void onEvent(ClusterMapFactory.ElementAddEvent event) {
									 received[0] = true;
									 Assert.assertEquals("kluczyk", event.getKey());
									 Assert.assertEquals("wartosc", event.getValue());
									 Assert.assertEquals(((DMap) map).getUid(), event.getUid());
								 }
							 });

		map.put("kluczyk", "wartosc");

		Thread.sleep(100);

		Assert.assertTrue(received[0]);
	}

	@Test
	public void testRemoteCreatedMap() throws Exception {
		ClusterMapFactory.NewMapCreatedEvent eventCreate = new ClusterMapFactory.NewMapCreatedEvent();
		eventCreate.setUid("test");
		eventCreate.setKeyClass(java.lang.String.class);
		eventCreate.setValueClass(java.lang.String.class);
		eventCreate.setParams(new String[]{"1", "2"});

		final ClusterMapFactory factory = new ClusterMapFactory();
		factory.setEventBus(new EventBusImplementation());
		final EventBus eventBus = factory.getEventBus();

		final Map[] maps = new Map[]{null};
		eventBus.addListener(MapCreatedEvent.class, new EventListener<MapCreatedEvent>() {
			@Override
			public void onEvent(MapCreatedEvent e) {
				maps[0] = e.getMap();
				Assert.assertEquals("test", e.getUid());
				Assert.assertArrayEquals(new String[]{"1", "2"}, e.getParameters());
			}
		});

		factory.onNewMapCreated(eventCreate);

		Thread.sleep(100);

		Assert.assertNotNull("It seems map was not created", maps[0]);
		Assert.assertEquals("test", ((DMap) maps[0]).uid);

		ClusterMapFactory.ElementAddEvent eventAdd = new ClusterMapFactory.ElementAddEvent();
		eventAdd.setUid("test");
		eventAdd.setKey("xKEY");
		eventAdd.setValue("xVALUE");
		factory.onMapElementAdd(eventAdd);

		eventAdd = new ClusterMapFactory.ElementAddEvent();
		eventAdd.setUid("test");
		eventAdd.setKey("yKEY");
		eventAdd.setValue("yVALUE");

		factory.onMapElementAdd(eventAdd);

		Assert.assertEquals("xVALUE", maps[0].get("xKEY"));
		Assert.assertEquals("yVALUE", maps[0].get("yKEY"));
		Assert.assertEquals(2, maps[0].size());

		ClusterMapFactory.ElementRemoveEvent eventDel = new ClusterMapFactory.ElementRemoveEvent();
		eventDel.setUid("test");
		eventDel.setKey("xKEY");
		factory.onMapElementRemove(eventDel);

		Assert.assertNull(maps[0].get("xKEY"));
		Assert.assertEquals(1, maps[0].size());

		ClusterMapFactory.MapClearEvent eventClear = new ClusterMapFactory.MapClearEvent();
		eventClear.setUid("test");
		factory.onMapClear(eventClear);

		Assert.assertEquals(0, maps[0].size());

		final boolean[] received = new boolean[]{false};
		eventBus.addListener(MapDestroyedEvent.class, new EventListener<MapDestroyedEvent>() {
			@Override
			public void onEvent(MapDestroyedEvent event) {
				Assert.assertEquals(maps[0], event.getMap());
				received[0] = true;
			}
		});

		Assert.assertNotNull(factory.getMap("test"));

		ClusterMapFactory.MapDestroyEvent eventDestroy = new ClusterMapFactory.MapDestroyEvent();
		eventDestroy.setUid("test");
		factory.onMapDestroyed(eventDestroy);

		Assert.assertNull(factory.getMap("test"));
		Thread.sleep(100);
		Assert.assertTrue(received[0]);
	}
}