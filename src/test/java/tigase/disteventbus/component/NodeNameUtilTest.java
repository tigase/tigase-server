package tigase.disteventbus.component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tigase.disteventbus.impl.EventName;

public class NodeNameUtilTest {

	@Test
	public void testCreateNodeName() throws Exception {
		assertEquals("1|2", NodeNameUtil.createNodeName("1", "2"));
		assertEquals("*|2", NodeNameUtil.createNodeName(null, "2"));
		assertEquals("*|*", NodeNameUtil.createNodeName(null, null));
		assertEquals("1|*", NodeNameUtil.createNodeName("1", null));
	}

	@Test
	public void testParseNodeName() throws Exception {
		assertEquals(new EventName("1", "2"), NodeNameUtil.parseNodeName("1|2"));
		assertEquals(new EventName(null, "2"), NodeNameUtil.parseNodeName("*|2"));
		assertEquals(new EventName(null, null), NodeNameUtil.parseNodeName("*|*"));
		assertEquals(new EventName("1", null), NodeNameUtil.parseNodeName("1|*"));
		assertEquals(new EventName("1", ""), NodeNameUtil.parseNodeName("1|"));
		assertEquals(new EventName("", ""), NodeNameUtil.parseNodeName("|"));
	}
}