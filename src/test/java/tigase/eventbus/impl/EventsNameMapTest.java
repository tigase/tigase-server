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
package tigase.eventbus.impl;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by bmalkow on 17.11.2015.
 */
public class EventsNameMapTest {

	@Test
	public void test01() throws Exception {
		EventsNameMap<String> map = new EventsNameMap<String>();
		map.put(null, null, "null-null");
		map.put(null, null, "null-null2");
		map.put("2", null, "null-2");
		map.put(null, "1", "1-null");
		map.put("2", "1", "1-2");
		map.put("2", "1", "1-2_2");

		map.put("b0", "a0", "U");
		map.put("b0", "a1", "U");
		map.put("b1", "a0", "U");
		map.put("b1", "a1", "U");

		assertEquals(7, map.getAllData().size());
		assertEquals(8, map.getAllListenedEvents().size());

		assertEquals(2, map.get("2", "1").size());
		assertThat(map.get("2", "1"), CoreMatchers.hasItem("1-2"));
		assertThat(map.get("2", "1"), CoreMatchers.hasItem("1-2_2"));
		assertThat(map.get(null, "1"), CoreMatchers.hasItem("1-null"));
		assertThat(map.get("2", null), CoreMatchers.hasItem("null-2"));
		assertThat(map.get(null, null), CoreMatchers.hasItem("null-null"));
		assertEquals(2, map.get(null, null).size());

		map.delete("2", "1", "1-2_2");
		assertEquals(1, map.get("2", "1").size());

		assertThat(map.get("2", "1"), CoreMatchers.hasItem("1-2"));
		assertThat(map.get("2", "1"), not(CoreMatchers.hasItem("1-2_2")));

		assertEquals(6, map.getAllData().size());
		assertEquals(8, map.getAllListenedEvents().size());

		map.delete("U");

		assertEquals(5, map.getAllData().size());
		assertEquals(4, map.getAllListenedEvents().size());

	}
}