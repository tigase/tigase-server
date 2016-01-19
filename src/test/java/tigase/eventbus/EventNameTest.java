package tigase.eventbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Created by bmalkow on 18.01.2016.
 */
public class EventNameTest {

	@Test
	public void testEquals() throws Exception {
		assertEquals(new EventName("pl.ttt"), new EventName("ttt", "pl"));

		assertEquals(new EventName("*.ttt"), new EventName("ttt", null));
		assertEquals(new EventName("pl.*"), new EventName(null, "pl"));
		assertEquals(new EventName("*.*"), new EventName(null, null));

		assertEquals(new EventName(".*"), new EventName(null, ""));
		assertEquals(new EventName("ttt"), new EventName("ttt", ""));

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

	@Test
	public void testToString() throws Exception {
		assertEquals("net.tigase.Event", new EventName("Event", "net.tigase").toString());
		assertEquals("net.tigase.*", new EventName(null, "net.tigase").toString());
		assertEquals("*.Event", new EventName("Event", null).toString());
		assertEquals("Event", new EventName("Event", "").toString());
		assertEquals("*.*", new EventName(null, null).toString());
	}
}