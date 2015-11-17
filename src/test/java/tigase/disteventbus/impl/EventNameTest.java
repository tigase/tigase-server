package tigase.disteventbus.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Created by bmalkow on 17.11.2015.
 */
public class EventNameTest {

	@Test
	public void testEquals() throws Exception {
		assertEquals(new EventName(null, null), new EventName(null, null));
		assertEquals(new EventName(null, "2"), new EventName(null, "2"));
		assertEquals(new EventName("1", "2"), new EventName("1", "2"));

		assertNotEquals(new EventName(null, null), new EventName("2", "2"));
		assertNotEquals(new EventName(null, null), new EventName(null, "2"));
		assertNotEquals(new EventName(null, null), new EventName("2", null));
		assertNotEquals(new EventName("1", null), new EventName("2", "2"));
		assertNotEquals(new EventName(null, "2"), new EventName("2", "2"));
		assertNotEquals(new EventName("2", "1"), new EventName("2", "2"));
	}
}