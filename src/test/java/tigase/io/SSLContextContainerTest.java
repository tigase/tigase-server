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

import java.util.*;

import static org.junit.Assert.*;
import static tigase.io.SSLContextContainerAbstract.getSpareDomainNamesToRemove;

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
	public void testFindMuc() {
		final HashMap<String, String> domains = new HashMap<String, String>();
		domains.put("*.tigase.org", "*.tigase.org");

		assertEquals("*.tigase.org", SSLContextContainer.find(domains, "tigase.org"));
		assertEquals("*.tigase.org", SSLContextContainer.find(domains, "muc.tigase.org"));

		domains.put("tigase.org", "tigase.org");

		assertEquals("tigase.org", SSLContextContainer.find(domains, "tigase.org"));
		assertEquals("*.tigase.org", SSLContextContainer.find(domains, "muc.tigase.org"));
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

	@Test
	public void testGetSpareDomainNamesToRemove() {

		final Set<String> contexts = new TreeSet<>();
		contexts.add("one.com");
		contexts.add("push.one.com");
		contexts.add("muc.one.com");
		contexts.add("sub.push.one.com");
		contexts.add("*.one.com");
		contexts.add("two.com");
		contexts.add("a.two.com");
		contexts.add("*.two.com");
		contexts.add("three.com");
		contexts.add("*.three.com");
		contexts.add("push.three.com");
		contexts.add("muc.three.com");

		final Set<String> domains = new HashSet<>(Arrays.asList("one.com", "*.one.com", "two.com"));

		final Set<String> spareDomainNamesToRemove = getSpareDomainNamesToRemove(contexts, domains);

		// basically only domains that match the wildcard but not the others - those will be overwritten directly by "put"
		assertFalse(spareDomainNamesToRemove.contains("one.com"));
		assertTrue(spareDomainNamesToRemove.contains("push.one.com"));
		assertTrue(spareDomainNamesToRemove.contains("muc.one.com"));
		assertFalse(spareDomainNamesToRemove.contains("sub.push.one.com"));
		assertFalse(spareDomainNamesToRemove.contains("*.one.com"));
		assertFalse(spareDomainNamesToRemove.contains("two.com"));
		assertFalse(spareDomainNamesToRemove.contains("a.two.com"));
		assertFalse(spareDomainNamesToRemove.contains("*.two.com"));
		assertFalse(spareDomainNamesToRemove.contains("three.com"));
		assertFalse(spareDomainNamesToRemove.contains("*.three.com"));
		assertFalse(spareDomainNamesToRemove.contains("push.three.com"));
		assertFalse(spareDomainNamesToRemove.contains("muc.three.com"));

		spareDomainNamesToRemove.forEach(contexts::remove);

		assertTrue(contexts.contains("one.com"));
		assertFalse(contexts.contains("push.one.com"));
		assertFalse(contexts.contains("muc.one.com"));
		assertTrue(contexts.contains("sub.push.one.com"));
		assertTrue(contexts.contains("*.one.com"));
		assertTrue(contexts.contains("two.com"));
		assertTrue(contexts.contains("a.two.com"));
		assertTrue(contexts.contains("*.two.com"));
		assertTrue(contexts.contains("three.com"));
		assertTrue(contexts.contains("*.three.com"));
		assertTrue(contexts.contains("push.three.com"));
		assertTrue(contexts.contains("muc.three.com"));

		contexts.addAll(domains);

		assertTrue(contexts.contains("one.com"));
		assertFalse(contexts.contains("push.one.com"));
		assertFalse(contexts.contains("muc.one.com"));
		assertTrue(contexts.contains("sub.push.one.com"));
		assertTrue(contexts.contains("*.one.com"));
		assertTrue(contexts.contains("two.com"));
		assertTrue(contexts.contains("a.two.com"));
		assertTrue(contexts.contains("*.two.com"));
		assertTrue(contexts.contains("three.com"));
		assertTrue(contexts.contains("*.three.com"));
		assertTrue(contexts.contains("push.three.com"));
		assertTrue(contexts.contains("muc.three.com"));
	}

	@Test
	public void testGetSpareDomainNamesToRemoveSimple() {

		final Set<String> contexts = new TreeSet<>();
		contexts.add("one.com");
		contexts.add("two.com");

		final Set<String> domains = new HashSet<>(Arrays.asList("one.com", "*.one.com"));

		final Set<String> spareDomainNamesToRemove = getSpareDomainNamesToRemove(contexts, domains);

		// basically only domains that match the wildcard but not the others - those will be overwritten directly by "put"
		assertTrue(spareDomainNamesToRemove.isEmpty());

		spareDomainNamesToRemove.forEach(contexts::remove);

		assertTrue(contexts.contains("one.com"));
		assertFalse(contexts.contains("*.one.com"));
		assertTrue(contexts.contains("two.com"));

		contexts.addAll(domains);

		assertTrue(contexts.contains("one.com"));
		assertTrue(contexts.contains("*.one.com"));
		assertTrue(contexts.contains("two.com"));
	}

	@Test
	public void testGetSpareDomainNamesToRemoveAtlantiscity() {

		final Set<String> contexts = new TreeSet<>();
		contexts.add("atlantiscity");
		contexts.add("firefly");

		final Set<String> domains = new HashSet<>(Arrays.asList("atlantiscity", "*.atlantiscity"));

		final Set<String> spareDomainNamesToRemove = getSpareDomainNamesToRemove(contexts, domains);

		// basically only domains that match the wildcard but not the others - those will be overwritten directly by "put"
		assertTrue(spareDomainNamesToRemove.isEmpty());

		spareDomainNamesToRemove.forEach(contexts::remove);

		assertTrue(contexts.contains("atlantiscity"));
		assertFalse(contexts.contains("*.atlantiscity"));
		assertTrue(contexts.contains("firefly"));

		contexts.addAll(domains);

		assertTrue(contexts.contains("atlantiscity"));
		assertTrue(contexts.contains("*.atlantiscity"));
		assertTrue(contexts.contains("firefly"));
	}

	@Test
	public void testGetSpareDomainNamesToRemoveLongDomains() {

		final Set<String> contexts = new TreeSet<>();
		contexts.add("chat.example.com");
		contexts.add("muc.chat.example.com");

		final Set<String> domains = new HashSet<>(Arrays.asList("chat.example.com", "*.chat.example.com"));

		final Set<String> spareDomainNamesToRemove = getSpareDomainNamesToRemove(contexts, domains);

		// basically only domains that match the wildcard but not the others - those will be overwritten directly by "put"
		assertEquals(1, spareDomainNamesToRemove.size());
		assertTrue(spareDomainNamesToRemove.contains("muc.chat.example.com"));

		spareDomainNamesToRemove.forEach(contexts::remove);

		assertTrue(contexts.contains("chat.example.com"));
		assertFalse(contexts.contains("*.chat.example.com"));

		contexts.addAll(domains);

		assertTrue(contexts.contains("chat.example.com"));
		assertTrue(contexts.contains("*.chat.example.com"));
	}

	@Test
	public void testRemoveMatchedMuc() {
		final HashMap<String, String> contexts = new HashMap<String, String>();
		contexts.put("one.com", "one.com");
		contexts.put("muc.one.com", "muc.one.com");
		contexts.put("*.one.com", "*.one.com");

		final Set<String> domains = new HashSet<>(Arrays.asList("one.com", "*.one.com"));

		SSLContextContainer.removeMatchedDomains(contexts, domains);

		assertFalse(contexts.containsKey("one.com"));
		assertFalse(contexts.containsKey("muc.one.com"));
		assertFalse(contexts.containsKey("*.one.com"));
		assertTrue(contexts.isEmpty());
	}

}
