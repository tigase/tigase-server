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
package tigase.xmpp.impl.roster;

import org.junit.Test;
import tigase.xmpp.jid.JID;

import static org.junit.Assert.*;

public class RosterElementTest {

	@Test
	public void testSetName() {
		RosterElement e = new RosterElement(JID.jidInstanceNS("a@b.c"), null, new String[]{});
		assertNull(e.getName());
		assertTrue(e.isModified());

		e.getRosterElement();
		assertFalse(e.isModified());

		e.setName(null);
		assertFalse(e.isModified());
		assertNull(e.getName());

		e.setName("jeff");
		assertTrue(e.isModified());
		assertEquals("jeff", e.getName());

		e.getRosterElement();
		assertFalse(e.isModified());

		e.setName("jeff");
		assertFalse(e.isModified());
		assertEquals("jeff", e.getName());

		e.setName("bob");
		assertTrue(e.isModified());
		assertEquals("bob", e.getName());

		e.getRosterElement();
		assertFalse(e.isModified());

		e.setName(null);
		assertTrue(e.isModified());
		assertNull(e.getName());

		e.getRosterElement();
		assertFalse(e.isModified());

		e.setName(null);
		assertFalse(e.isModified());
		assertNull(e.getName());
	}

}
