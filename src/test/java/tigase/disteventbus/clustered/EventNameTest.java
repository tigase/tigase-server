package tigase.disteventbus.clustered;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import tigase.disteventbus.EventBusFactory;
import tigase.xml.Element;

/**
 * Created by bmalkow on 17.11.2015.
 */
public class EventNameTest {

	@Test
	public void testEquals() throws Exception {

		Element event = new Element("EventName", new String[] { "xmlns" }, new String[] { "tigase:demo" });
		event.addChild(new Element("sample_value", "1"));

		EventBusFactory.getInstance().fire(event);

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