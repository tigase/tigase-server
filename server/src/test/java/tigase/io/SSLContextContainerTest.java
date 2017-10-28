/*
 * SSLContextContainerTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

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

}
