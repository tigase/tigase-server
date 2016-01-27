package tigase.eventbus;

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