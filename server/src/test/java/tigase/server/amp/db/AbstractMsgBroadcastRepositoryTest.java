/*
 * AbstractMsgBroadcastRepositoryTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
 *
 */

package tigase.server.amp.db;

import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;
import org.junit.runners.model.Statement;
import tigase.component.exceptions.RepositoryException;
import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.RepositoryFactory;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.BareJID;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by andrzej on 24.03.2017.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractMsgBroadcastRepositoryTest<DS extends DataSource> {

	protected static String uri = System.getProperty("testDbUri");

	@ClassRule
	public static TestRule rule = new TestRule() {
		@Override
		public Statement apply(Statement stmnt, Description d) {
			if (uri == null) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						Assume.assumeTrue("Ignored due to not passed DB URI!", false);
					}
				};
			}
			return stmnt;
		}
	};

	protected DS dataSource;
	protected MsgBroadcastRepository repo;
	private static BareJID jid;
	private static String msgId;
	private static Element msg;

	protected DS prepareDataSource() throws DBInitException, IllegalAccessException, InstantiationException {
		DataSource dataSource = RepositoryFactory.getRepoClass(DataSource.class, uri).newInstance();
		dataSource.initRepository(uri, new HashMap<>());
		return (DS) dataSource;
	}

	@BeforeClass
	public static void init() throws TigaseStringprepException {
		jid = BareJID.bareJIDInstance("broadcast-" + UUID.randomUUID(), "example.com");
		msgId = "test-" + UUID.randomUUID().toString();
		msg = new Element("message");
		msg.addChild(new Element("body", "Testing broadcast messages"));
	}

	@Before
	public void setup() throws Exception {
		dataSource = prepareDataSource();
		repo = DataSourceHelper.getDefaultClass(MsgBroadcastRepository.class, uri).newInstance();
		try {
			repo.setDataSource(dataSource);
		} catch (RuntimeException ex) {
			throw new RepositoryException(ex);
		}
	}

	@After
	public void tearDown() throws Exception {
		repo = null;
	}

	@Test
	public void test1_addingBroadcastMessage() throws InterruptedException {
		Date expire = new Date(System.currentTimeMillis() + (60 * 1000 * 5));
		assertTrue("Not added message to broadcast list!", repo.updateBroadcastMessage(msgId, msg, expire, jid));
		assertNotNull("Not found message with id = " + msgId, repo.getBroadcastMsg(msgId));
		repo.loadMessagesToBroadcast();
		assertNotNull("Not found message with id = " + msgId, repo.getBroadcastMsg(msgId));
	}

	@Test
	public void test2_loadingBroadcastMessagesAndAddingBroadcastMessageRecipient() throws InterruptedException {
		Thread.sleep(1000);
		repo.loadMessagesToBroadcast();
		assertNotNull("Not found message with id = " + msgId, repo.getBroadcastMsg(msgId));

		assertFalse("Added message instead of adding message recipient!", repo.updateBroadcastMessage(msgId, null, null, jid));
	}
	
}
