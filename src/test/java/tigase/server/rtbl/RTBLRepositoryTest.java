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
import tigase.db.TigaseDBException;
import tigase.eventbus.impl.EventBusImplementation;
import tigase.kernel.AbstractKernelWithUserRepositoryTestCase;
import tigase.kernel.core.Kernel;
import tigase.util.Algorithms;
import tigase.xmpp.jid.BareJID;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static junit.framework.TestCase.*;

public class RTBLRepositoryTest extends AbstractKernelWithUserRepositoryTestCase {

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventBus").asInstance(new EventBusImplementation()).exec();
		kernel.registerBean(RTBLRepository.class).exec();
	}

	private BareJID pubsubJid = BareJID.bareJIDInstanceNS("test@localhost");
	private String node = "test";

	private String hash = "SHA-256";

	private static final int limit = 10;

	@Test
	public void test() throws TigaseDBException, NoSuchFieldException, IllegalAccessException, InterruptedException {
		RTBLRepository repository = getInstance(RTBLRepository.class);

		assertEquals(0, repository.getBlockLists().size());

		repository.add(pubsubJid, node, hash);
		int i = 0;
		while (i < limit && repository.getBlockLists().size() != 1) {
			Thread.sleep(100);
		}
		assertEquals(1, repository.getBlockLists().size());

		BareJID user = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain");
		repository.update(pubsubJid, node, RTBLRepository.Action.add, Algorithms.sha256(user.toString()));
		i = 0;
		while (i < limit && !repository.isBlocked(user)) {
			Thread.sleep(100);
		}
		assertTrue(repository.isBlocked(user));

		Field f = RTBLRepository.class.getDeclaredField("cache");
		f.setAccessible(true);
		Map cache = (Map) f.get(repository);
		cache.clear();

		assertEquals(0, repository.getBlockLists().size());
		assertFalse(repository.isBlocked(user));

		repository.reload();
		assertEquals(1, repository.getBlockLists().size());
		RTBL rtbl = repository.getBlockList(pubsubJid, node);
		assertNotNull(rtbl);
		assertEquals(hash, rtbl.getHash());
		assertTrue(repository.isBlocked(user));

		BareJID user2 = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain");
		rtbl = new RTBL(rtbl.getKey(), hash, Set.of(Algorithms.sha256(user.toString()), Algorithms.sha256(user2.toString())));
		repository.update(rtbl);
		i = 0;
		while (i < limit && !repository.isBlocked(user)) {
			Thread.sleep(100);
		}

		assertTrue(repository.isBlocked(user));
		assertTrue(repository.isBlocked(user2));

		rtbl = new RTBL(rtbl.getKey(), hash, Set.of(Algorithms.sha256(user2.toString())));
		repository.update(rtbl);
		i = 0;
		while (i < limit && !repository.isBlocked(user2)) {
			Thread.sleep(100);
		}

		assertFalse(repository.isBlocked(user));
		assertTrue(repository.isBlocked(user2));
	}
}
