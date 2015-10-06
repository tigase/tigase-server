package tigase.xmpp.impl.roster;

import static org.junit.Assert.*;

import org.junit.Test;

import tigase.xmpp.JID;

public class RosterElementTest {

	@Test
	public void testSetName() {
		RosterElement e = new RosterElement(JID.jidInstanceNS("a@b.c"), null, new String[] {}, null);
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
