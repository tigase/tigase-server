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

import java.util.*;

/**
 * Created by bmalkow on 04.12.2015.
 */
public class DMapTest {

	@Test
	public void testBasicOperations() {
		final Set<String> removedItems = new HashSet<>();
		final Map<String, String> addedItems = new HashMap<>();
		final boolean[] cleared = new boolean[]{false};

		final DMap.DMapListener listener = new DMap.DMapListener() {
			@Override
			public void onClear(DMap map) {
				cleared[0] = true;
			}

			@Override
			public void onPut(DMap map, Object key, Object value) {
				Assert.assertNull(addedItems.put((String) key, (String) value));
			}

			@Override
			public void onPutAll(DMap map, Map m) {
				addedItems.putAll(m);
			}

			@Override
			public void onRemove(DMap map, Object key) {
				Assert.assertTrue(removedItems.add((String) key));
			}
		};

		Map<String, String> tmp = new HashMap<>();
		tmp.put("7", "seven");
		tmp.put("8", "eight");
		tmp.put("9", "nine");
		tmp.put("A", "ten");

		Map<String, String> map = new DMap<>("test", listener, String.class, String.class);
		map.put("1", "one");
		map.put("2", "two");
		map.put("3", "three");
		map.put("4", "four");
		map.put("5", "five");
		map.put("6", "six");

		Assert.assertEquals(6, map.size());
		Assert.assertEquals(map.size(), addedItems.size());
		Assert.assertTrue(
				map.values().containsAll(addedItems.values()) && addedItems.values().containsAll(map.values()));

		map.putAll(tmp);

		Assert.assertEquals(10, map.size());
		Assert.assertTrue(map.values().containsAll(addedItems.values()));
		Assert.assertTrue(addedItems.values().containsAll(map.values()));

		map.remove("1");
		map.remove("2");

		Assert.assertEquals(8, map.size());
		Assert.assertEquals(2, removedItems.size());

		try {
			Iterator<String> itK = map.keySet().iterator();
			final String elK = itK.next();
			itK.remove();
			Assert.fail("Should be blocked!");
		} catch (UnsupportedOperationException e) {
		}

		Assert.assertEquals(8, map.size());
		Assert.assertEquals(2, removedItems.size());

		try {
			Iterator<Map.Entry<String, String>> itE = map.entrySet().iterator();
			final Map.Entry<String, String> elE = itE.next();
			itE.remove();
			Assert.fail("Should be blocked!");
		} catch (UnsupportedOperationException e) {
		}

		Assert.assertEquals(8, map.size());
		Assert.assertEquals(2, removedItems.size());

		try {
			Iterator<String> itV = map.values().iterator();
			String elV = itV.next();
			itV.remove();
			Assert.fail("Should be blocked!");
		} catch (UnsupportedOperationException e) {
		}

		Assert.assertEquals(8, map.size());
		Assert.assertEquals(2, removedItems.size());

		Assert.assertFalse(cleared[0]);
		map.clear();
		Assert.assertTrue(cleared[0]);
		Assert.assertEquals(0, map.size());

	}

}