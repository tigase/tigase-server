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
package tigase.db;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tigase.db.jdbc.JDBCRepositoryTest;
import tigase.xmpp.jid.BareJID;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractUserRepositoryTest<DS extends DataSource> extends AbstractDataSourceTestCase<DS> {

	private final Logger log;

	protected UserRepository repo;

	public AbstractUserRepositoryTest() {
		log = Logger.getLogger(this.getClass().getCanonicalName());
	}

	@Before
	public void setUp() throws Exception {
		repo = DataSourceHelper.getDefaultClass(UserRepository.class, uri).newInstance();
		if (repo instanceof DataSourceAware) {
			((DataSourceAware) repo).setDataSource(getDataSource());
		}
	}

	@After
	public void tearDown() {
		repo = null;
	}

	@Test
	public void testLongNode() throws InterruptedException, TigaseDBException {
		BareJID user = BareJID.bareJIDInstanceNS("user", "domain");
		repo.addUser(user);
		repo.setData(user, "node1/node2/node3", "key", "value");
		String node3val;
		node3val = repo.getData(user, "node1/node2/node3", "key");
		Assert.assertEquals("String differ from expected!", "value", node3val);
		repo.removeSubnode(user, "node1");
		node3val = repo.getData(user, "node1/node2/node3", "key");
		Assert.assertNull("Node not removed", node3val);
		repo.removeUser(user);
	}

	@Test
	public void testGetData() throws InterruptedException {

		log.log(Level.FINE, "repo: " + repo);
		if (repo != null) {
			LocalDateTime localNow = LocalDateTime.now();
//			getData( null );

			long initalDelay = 5;

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
			final int iter = 50;
			final int threads = 10;

			for (int i = 0; i < threads; i++) {
				scheduler.scheduleAtFixedRate(new RunnableImpl(iter), initalDelay, 100, TimeUnit.MILLISECONDS);
			}

			Thread.sleep(threads * 1000);
		}

	}

	private void getData(BareJID user) {
		if (user == null) {
			user = BareJID.bareJIDInstanceNS("user", "domain");
		}
		log.log(Level.FINE, "retrieve: " + user + " / thread: " + Thread.currentThread().getName());
		try {
//			repo.getData( user, "privacy", "default-list", null );
			repo.addUser(user);
		} catch (UserExistsException ex) {
			log.log(Level.FINE, "User exists, ignore: " + ex.getUserId());
		} catch (TigaseDBException ex) {
			Logger.getLogger(JDBCRepositoryTest.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	private class RunnableImpl
			implements Runnable {

		int count = 0;
		int max = 50;

		public RunnableImpl(int max) {
			this.max = max;
		}

		@Override
		public void run() {
			while (count < max) {
				count++;
				BareJID user;
				user = BareJID.bareJIDInstanceNS(String.valueOf((new Date()).getTime() / 10), "domain");
				getData(user);
			}
		}
	}

}
