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
package tigase.server.rtbl;

import org.junit.Test;
import tigase.util.Algorithms;
import tigase.xmpp.jid.BareJID;

import java.util.Collections;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

public class RTBLTest {

	private BareJID pubsubJid = BareJID.bareJIDInstanceNS("test@localhost");
	private String node = "test";

	private String hash = "SHA-256";

	@Test
	public void testCreation() {
		RTBL rtbl = new RTBL(pubsubJid, node, hash, Collections.emptySet());
		assertEquals(pubsubJid, rtbl.getJID());
		assertEquals(node, rtbl.getNode());
		assertEquals(hash, rtbl.getHash());
		assertEquals(0, rtbl.getBlocked().size());
	}

	@Test
	public void testUserBlocking() {
		RTBL rtbl = new RTBL(pubsubJid, node, hash, Collections.emptySet());
		BareJID user1 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain-1");
		BareJID user2 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain-1");
		BareJID user3 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain-2");
		String userHash = Algorithms.sha256(user1.toString());
		rtbl.getBlocked().add(userHash);

		assertTrue(rtbl.isBlocked(user1));
		assertFalse(rtbl.isBlocked(user2));
		assertFalse(rtbl.isBlocked(user3));
	}

	@Test
	public void testDomainBlocking() {
		RTBL rtbl = new RTBL(pubsubJid, node, hash, Collections.emptySet());
		BareJID user1 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain-1");
		BareJID user2 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain-1");
		BareJID user3 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain-2");
		String domainHash = Algorithms.sha256(user1.getDomain().toString());
		rtbl.getBlocked().add(domainHash);

		assertTrue(rtbl.isBlocked(user1));
		assertTrue(rtbl.isBlocked(user1));
		assertFalse(rtbl.isBlocked(user3));
	}
}
