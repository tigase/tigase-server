package tigase.monitor.tasks;

import static org.junit.Assert.*;
import static tigase.monitor.tasks.ConnectionsTask.createAlarmEvent;

import org.junit.Test;

import tigase.xml.Element;

public class UsersDisconnectTaskTest {

	@Test
	public void testCreateAlarmEvent() throws Exception {
		Element e = createAlarmEvent(100, 200, 10, 50);
		assertNotNull(e);
		assertEquals(100, Integer.parseInt(e.getCData(new String[] { "UsersDisconnected", "disconnections" })));
		assertEquals(50f, Float.parseFloat(e.getCData(new String[] { "UsersDisconnected", "disconnectionsPercent" })), 0);

		e = createAlarmEvent(99, 250, 10, 50);
		assertNotNull(e);
		assertEquals(151, Integer.parseInt(e.getCData(new String[] { "UsersDisconnected", "disconnections" })));
		assertEquals(60.4, Float.parseFloat(e.getCData(new String[] { "UsersDisconnected", "disconnectionsPercent" })), 0.01);

		e = createAlarmEvent(0, 99, 10, 50);
		assertNotNull(e);
		assertEquals(99, Integer.parseInt(e.getCData(new String[] { "UsersDisconnected", "disconnections" })));
		assertEquals(100.0, Float.parseFloat(e.getCData(new String[] { "UsersDisconnected", "disconnectionsPercent" })), 0.01);

		e = createAlarmEvent(99, 250, 152, 50);
		assertNull(e);

		e = createAlarmEvent(99, 250, 10, 61);
		assertNull(e);

		e = createAlarmEvent(250, 0, 10, 50);
		assertNull(e);

		e = createAlarmEvent(0, 0, 10, 50);
		assertNull(e);

		e = createAlarmEvent(1, 1, 10, 50);
		assertNull(e);

		e = createAlarmEvent(0, 1, 10, 50);
		assertNull(e);
	}
}