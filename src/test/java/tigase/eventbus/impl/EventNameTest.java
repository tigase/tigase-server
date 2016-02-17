/*
 * EventNameTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package tigase.eventbus.impl;

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