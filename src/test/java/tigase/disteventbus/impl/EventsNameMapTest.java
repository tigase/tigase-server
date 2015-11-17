package tigase.disteventbus.impl;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Created by bmalkow on 17.11.2015.
 */
public class EventsNameMapTest {

	@Test
	public void test01() throws Exception {
		EventsNameMap<String> map = new EventsNameMap<String>();
		map.put(null, null, "null-null");
		map.put(null, null, "null-null2");
		map.put(null, "2", "null-2");
		map.put("1", null, "1-null");
		map.put("1", "2", "1-2");
		map.put("1", "2", "1-2_2");

		map.put("a0", "b0", "U");
		map.put("a1", "b0", "U");
		map.put("a0", "b1", "U");
		map.put("a1", "b1", "U");

		assertEquals(7, map.getAllData().size());
		assertEquals(8, map.getAllListenedEvents().size());

		assertEquals(2, map.get("1", "2").size());
		assertThat(map.get("1", "2"), CoreMatchers.hasItem("1-2"));
		assertThat(map.get("1", "2"), CoreMatchers.hasItem("1-2_2"));
		assertThat(map.get("1", null), CoreMatchers.hasItem("1-null"));
		assertThat(map.get(null, "2"), CoreMatchers.hasItem("null-2"));
		assertThat(map.get(null, null), CoreMatchers.hasItem("null-null"));
		assertEquals(2, map.get(null, null).size());

		map.delete("1", "2", "1-2_2");
		assertEquals(1, map.get("1", "2").size());

		assertThat(map.get("1", "2"), CoreMatchers.hasItem("1-2"));
		assertThat(map.get("1", "2"), not(CoreMatchers.hasItem("1-2_2")));

		assertEquals(6, map.getAllData().size());
		assertEquals(8, map.getAllListenedEvents().size());

		map.delete("U");

		assertEquals(5, map.getAllData().size());
		assertEquals(4, map.getAllListenedEvents().size());

	}
}