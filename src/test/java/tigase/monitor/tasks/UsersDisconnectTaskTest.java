/*
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

import java.util.Optional;

import static org.junit.Assert.*;
import static tigase.monitor.tasks.ConnectionsTask.createUserDisconnectedEvent;

public class UsersDisconnectTaskTest {

	@Test
	public void testCreateAlarmEvent() throws Exception {
		Optional<ConnectionsTask.UserDisconnectedEvent> e = createUserDisconnectedEvent(100, 200, 10, 50);

		assertTrue(e.isPresent());

		assertEquals(100, e.get().getDisconnections());
		assertEquals(50f, e.get().getDisconnectionsPercent(), 0);

		e = createUserDisconnectedEvent(99, 250, 10, 50);
		assertTrue(e.isPresent());
		assertEquals(151, e.get().getDisconnections());
		assertEquals(60.4, e.get().getDisconnectionsPercent(), 0.01);

		e = createUserDisconnectedEvent(0, 99, 10, 50);
		assertTrue(e.isPresent());
		assertEquals(99, e.get().getDisconnections());
		assertEquals(100.0, e.get().getDisconnectionsPercent(), 0.01);

		e = createUserDisconnectedEvent(99, 250, 152, 50);
		assertFalse(e.isPresent());

		e = createUserDisconnectedEvent(99, 250, 10, 61);
		assertFalse(e.isPresent());

		e = createUserDisconnectedEvent(250, 0, 10, 50);
		assertFalse(e.isPresent());

		e = createUserDisconnectedEvent(0, 0, 10, 50);
		assertFalse(e.isPresent());

		e = createUserDisconnectedEvent(1, 1, 10, 50);
		assertFalse(e.isPresent());

		e = createUserDisconnectedEvent(0, 1, 10, 50);
		assertFalse(e.isPresent());
	}
}