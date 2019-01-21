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
package tigase.monitor.tasks;

import org.junit.Test;
import tigase.xml.Element;

import static org.junit.Assert.*;
import static tigase.monitor.tasks.ConnectionsTask.createAlarmEvent;

public class UsersDisconnectTaskTest {

	@Test
	public void testCreateAlarmEvent() throws Exception {
		Element e = createAlarmEvent(100, 200, 10, 50);

		assertNotNull(e);
		assertEquals(100, Integer.parseInt(
				e.getCData(new String[]{"tigase.monitor.tasks.UsersDisconnected", "disconnections"})));
		assertEquals(50f, Float.parseFloat(
				e.getCData(new String[]{"tigase.monitor.tasks.UsersDisconnected", "disconnectionsPercent"})), 0);

		e = createAlarmEvent(99, 250, 10, 50);
		assertNotNull(e);
		assertEquals(151, Integer.parseInt(
				e.getCData(new String[]{"tigase.monitor.tasks.UsersDisconnected", "disconnections"})));
		assertEquals(60.4, Float.parseFloat(
				e.getCData(new String[]{"tigase.monitor.tasks.UsersDisconnected", "disconnectionsPercent"})), 0.01);

		e = createAlarmEvent(0, 99, 10, 50);
		assertNotNull(e);
		assertEquals(99, Integer.parseInt(
				e.getCData(new String[]{"tigase.monitor.tasks.UsersDisconnected", "disconnections"})));
		assertEquals(100.0, Float.parseFloat(
				e.getCData(new String[]{"tigase.monitor.tasks.UsersDisconnected", "disconnectionsPercent"})), 0.01);

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