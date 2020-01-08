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
package tigase.io;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class SSLContextContainerTest {

	@Test
	public void testFind() {
		final HashMap<String, String> domains = new HashMap<String, String>();
		domains.put("one.com", "one.com");
		domains.put("a.two.com", "a.two.com");
		domains.put("*.two.com", "*.two.com");

		assertEquals("one.com", SSLContextContainer.find(domains, "one.com"));
		assertNull(SSLContextContainer.find(domains, "tone.com"));
		assertNull(SSLContextContainer.find(domains, "zero.com"));
		assertEquals("a.two.com", SSLContextContainer.find(domains, "a.two.com"));
		assertEquals("*.two.com", SSLContextContainer.find(domains, "b.two.com"));
		assertEquals("*.two.com", SSLContextContainer.find(domains, "b.two.com"));
		assertNull(SSLContextContainer.find(domains, "btwo.com"));
		assertEquals("*.two.com", SSLContextContainer.find(domains, ".two.com"));
	}

	@Test
	public void testRemoveMatched() {
		final HashMap<String, String> contexts = new HashMap<String, String>();
		contexts.put("one.com", "one.com");
		contexts.put("push.one.com", "push.one.com");
		contexts.put("sub.push.one.com", "sub.push.one.com");
		contexts.put("*.one.com", "*.one.com");
		contexts.put("a.two.com", "a.two.com");
		contexts.put("*.two.com", "*.two.com");

		final Set<String> domains = new HashSet<>(Arrays.asList("one.com", "*.one.com", "two.com"));

		SSLContextContainer.removeMatchedDomains(contexts, domains);

		assertFalse(contexts.containsKey("one.com"));
		assertFalse(contexts.containsKey("push.one.com"));
		assertTrue(contexts.containsKey("sub.push.one.com"));
		assertFalse(contexts.containsKey("*.one.com"));
		assertTrue(contexts.containsKey("a.two.com"));
		assertTrue(contexts.containsKey("*.two.com"));
	}

}
