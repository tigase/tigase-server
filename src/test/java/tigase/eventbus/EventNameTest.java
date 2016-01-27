package tigase.eventbus;

import static org.junit.Assert.*;

import org.junit.Test;

public class EventNameTest {

	@Test
	public void testEquals() throws Exception {
		assertEquals(new EventName("pl.ttt"), new EventName("pl", "ttt"));

		assertEquals(new EventName("*.ttt"), new EventName(null, "ttt"));
		assertEquals(new EventName("pl.*"), new EventName("pl", null));
		assertEquals(new EventName("*.*"), new EventName(null, null));

		assertEquals(new EventName(".*"), new EventName("", null));
		assertEquals(new EventName("ttt"), new EventName("", "ttt"));

		assertEquals(new EventName(null, null), new EventName(null, null));
		assertEquals(new EventName("2", null), new EventName("2", null));
		assertEquals(new EventName("2", "1"), new EventName("2", "1"));

		assertNotEquals(new EventName(null, null), new EventName("2", "2"));
		assertNotEquals(new EventName(null, null), new EventName("2", null));
		assertNotEquals(new EventName(null, null), new EventName(null, "2"));
		assertNotEquals(new EventName(null, "1"), new EventName("2", "2"));
		assertNotEquals(new EventName("2", null), new EventName("2", "2"));
		assertNotEquals(new EventName("1", "2"), new EventName("2", "2"));
	}

	@Test
	public void testGetters() throws Exception {
		EventName e = new EventName("net.tigase", "Event");
		assertEquals("net.tigase", e.getPackage());
		assertEquals("Event", e.getName());

		e = new EventName("net.tigase.Event");
		assertEquals("net.tigase", e.getPackage());
		assertEquals("Event", e.getName());

		e = new EventName("net.tigase", null);
		assertEquals("net.tigase", e.getPackage());
		assertNull(e.getName());

		e = new EventName(null, "Event");
		assertNull(e.getPackage());
		assertEquals("Event", e.getName());
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("net.tigase.Event", new EventName("net.tigase", "Event").toString());
		assertEquals("net.tigase.*", new EventName("net.tigase", null).toString());
		assertEquals("*.Event", new EventName(null, "Event").toString());
		assertEquals("Event", new EventName("", "Event").toString());
		assertEquals("*.*", new EventName(null, null).toString());
	}

	@Test
	public void testToStringInt() throws Exception {
		assertEquals("net.tigase.Event", EventName.toString("net.tigase", "Event"));
		assertEquals("net.tigase.*", EventName.toString("net.tigase", null));
		assertEquals("*.Event", EventName.toString(null, "Event"));
		assertEquals("Event", EventName.toString("", "Event"));
		assertEquals("*.*", EventName.toString(null, null));
	}
}