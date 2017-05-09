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
import tigase.db.*;
import tigase.db.util.SchemaLoader;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.ProcessorTestCase;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by andrzej on 22.03.2017.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractMsgRepositoryTest<DS extends DataSource, T> extends ProcessorTestCase {

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

	protected static String emoji = "\uD83D\uDE97\uD83D\uDCA9\uD83D\uDE21";
	protected boolean checkEmoji = true;
	protected DS dataSource;
	protected MsgRepository repo;
	private JID sender;
	private JID recipient;
	private XMPPResourceConnection recipientSession;

	protected DS prepareDataSource() throws DBInitException, IllegalAccessException, InstantiationException {
		DataSource dataSource = DataSourceHelper.getDefaultClass(DataSource.class, uri).newInstance();//RepositoryFactory.getRepoClass(DataSource.class, uri).newInstance();
		dataSource.initRepository(uri, new HashMap<>());
		return (DS) dataSource;
	}

	@BeforeClass
	public static void loadSchema() {
		if (uri.startsWith("jdbc:")) {
			SchemaLoader loader = SchemaLoader.newInstance("jdbc");
			SchemaLoader.Parameters params = loader.createParameters();
			params.parseUri(uri);
			params.setDbRootCredentials(null, null);
			loader.init(params);
			loader.validateDBConnection();
			loader.validateDBExists();
			Assert.assertEquals(SchemaLoader.Result.ok, loader.loadSchema(Schema.SERVER_SCHEMA_ID, "7.2.0"));
			loader.postInstallation();
			loader.shutdown();
		}
	}
	
	@Before
	public void setup() throws Exception {
		super.setUp();
		dataSource = prepareDataSource();
		repo = (MsgRepository) DataSourceHelper.getDefaultClass(MsgRepositoryIfc.class, uri).newInstance();
		try {
			repo.setDataSource(dataSource);
		} catch (RuntimeException ex) {
			throw new RepositoryException(ex);
		}
		ReentrantLock lock = new ReentrantLock();
		repo.setCondition(lock, lock.newCondition());
		sender = JID.jidInstance("sender-" + UUID.randomUUID(), "example.com", "resource-1");
		recipient = JID.jidInstance("recipient-" + UUID.randomUUID(), "example.com", "resource-1");
		recipientSession = getSession(recipient, recipient);
	}

	@After
	public void tearDown() throws Exception {
		repo.deleteMessagesToJID(null, recipientSession);
		repo = null;
		super.tearDown();
	}

	@AfterClass
	public static void cleanDerby() {
		if (uri.contains("jdbc:derby:")) {
			File f = new File("derby_test");
			if (f.exists()) {
				if (f.listFiles() != null) {
					Arrays.asList(f.listFiles()).forEach(f2 -> {
						if (f2.listFiles() != null) {
							Arrays.asList(f2.listFiles()).forEach(f3 -> f3.delete());
						}
						f2.delete();
					});
				}
				f.delete();
			}
		}
	}

	@Test
	public void testStorageOfOfflineMessage()
			throws UserNotFoundException, NotAuthorizedException, TigaseStringprepException {
		List<Packet> messages = new ArrayList<>();
		for (int i=0; i<5; i++) {
			Packet message = Message.getMessage(sender, recipient, StanzaType.chat, generateRandomBody(), null, null, UUID.randomUUID().toString());
			assertTrue(repo.storeMessage(sender, recipient, null, message.getElement(), null));
			messages.add(message);
		}

		Map<Enum, Long> count = repo.getMessagesCount(recipient);
		assertEquals(1, count.size());
		assertNotNull(count.get(MsgRepository.MSG_TYPES.message));
		assertEquals(5, count.get(MsgRepository.MSG_TYPES.message).longValue());

		List<Element> list = repo.getMessagesList(recipient);
		assertNotNull(list);
		assertEquals(5, list.size());
		for (Element item : list) {
			assertEquals(recipient.getBareJID().toString(), item.getAttributeStaticStr("jid"));
			assertNotNull(item.getAttributeStaticStr("node"));
			assertEquals("message", item.getAttributeStaticStr("type"));
			assertEquals(sender.getBareJID().toString(), item.getAttributeStaticStr("name"));
		}

		List<String> msgIds = list.stream()
				.map(item -> item.getAttributeStaticStr("node"))
				.collect(Collectors.toList());

		Queue<Element> loaded = repo.loadMessagesToJID(recipientSession, false);

		for (Packet message : messages) {
			Element el1 = message.getElement();
			Element el2 = loaded.poll();
			assertNotNull(el2);
			assertEquals(el1.getAttributeStaticStr("id"), el2.getAttributeStaticStr("id"));
			assertEquals(el1.getChildStaticStr("body"), el2.getChildStaticStr("body"));
		}

		loaded = repo.loadMessagesToJID(msgIds.subList(0, 3), recipientSession, false, null);
		for (Packet message : messages.subList(0,3)) {
			Element el1 = message.getElement();
			Element el2 = loaded.poll();
			assertNotNull(el2);
			assertEquals(el1.getAttributeStaticStr("id"), el2.getAttributeStaticStr("id"));
			assertEquals(el1.getChildStaticStr("body"), el2.getChildStaticStr("body"));
		}

		repo.deleteMessage(getMsgId(msgIds.remove(0)));
		
		loaded = repo.loadMessagesToJID(msgIds.subList(0, 3), recipientSession, false, null);
		for (Packet message : messages.subList(1,4)) {
			Element el1 = message.getElement();
			Element el2 = loaded.poll();
			assertNotNull(el2);
			assertEquals(el1.getAttributeStaticStr("id"), el2.getAttributeStaticStr("id"));
			assertEquals(el1.getChildStaticStr("body"), el2.getChildStaticStr("body"));
		}

		count = repo.getMessagesCount(recipient);
		assertEquals(1, count.size());
		assertNotNull(count.get(MsgRepository.MSG_TYPES.message));
		assertEquals(4, count.get(MsgRepository.MSG_TYPES.message).longValue());

		repo.loadMessagesToJID(recipientSession, true);
		count = repo.getMessagesCount(recipient);
		assertEquals(0, count.size());
	}

	@Test
	public void testStorageOfOfflineMessageWithExpiration1() throws UserNotFoundException, NotAuthorizedException, TigaseStringprepException {
		try {
			Date expire = new Date(System.currentTimeMillis() - 60 * 1000);

			List<Packet> messages = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				Packet message = Message.getMessage(sender, recipient, StanzaType.chat, generateRandomBody(), null, null,
													UUID.randomUUID().toString());
				assertTrue(repo.storeMessage(sender, recipient, expire, message.getElement(), null));
				messages.add(message);
			}

			Map<Enum, Long> count = repo.getMessagesCount(recipient);
			assertEquals(1, count.size());
			assertNotNull(count.get(MsgRepository.MSG_TYPES.message));
			assertEquals(5, count.get(MsgRepository.MSG_TYPES.message).longValue());

			Element elem = repo.getMessageExpired(System.currentTimeMillis(), true);
			assertNotNull(elem);

			count = repo.getMessagesCount(recipient);
			assertEquals(1, count.size());
			assertNotNull(count.get(MsgRepository.MSG_TYPES.message));
			assertEquals(4, count.get(MsgRepository.MSG_TYPES.message).longValue());

			repo.expiredQueue.clear();
			repo.earliestOffline = Long.MAX_VALUE;

			elem = repo.getMessageExpired(System.currentTimeMillis(), true);
			assertNotNull(elem);
			count = repo.getMessagesCount(recipient);
			assertEquals(1, count.size());
			assertNotNull(count.get(MsgRepository.MSG_TYPES.message));
			assertEquals(3, count.get(MsgRepository.MSG_TYPES.message).longValue());

			repo.earliestOffline = Long.MAX_VALUE;

			elem = repo.getMessageExpired(System.currentTimeMillis(), true);
			assertNotNull(elem);
			count = repo.getMessagesCount(recipient);
			assertEquals(1, count.size());
			assertNotNull(count.get(MsgRepository.MSG_TYPES.message));
			assertEquals(2, count.get(MsgRepository.MSG_TYPES.message).longValue());
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
	
	protected abstract <T> T getMsgId(String msgIdStr);

	protected String generateRandomBody() {
		String body = "Body " + UUID.randomUUID().toString();
		if (checkEmoji) {
			body += emoji;
		}
		return body;
	}

}
