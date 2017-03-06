/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */
package tigase.xmpp.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tigase.db.DBInitException;
import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;
import tigase.server.Packet;
import tigase.server.amp.JDBCMsgRepository;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

import java.util.*;

import static org.junit.Assert.*;
import static tigase.db.RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY;

/**
 *
 * @author andrzej
 */
public class OfflineMessagesTest extends ProcessorTestCase {
	
	private OfflineMessages offlineProcessor;
	private MsgRepositoryIfcImpl msgRepo;
	
	@Before
	@Override
	public void setUp() throws Exception {
		msgRepo = new MsgRepositoryIfcImpl();
		offlineProcessor = new OfflineMessages() {
			@Override
			protected MsgRepositoryIfc getMsgRepoImpl(NonAuthUserRepository repo, XMPPResourceConnection conn) {
				return msgRepo;
			}
		};
		offlineProcessor.init(new HashMap<String,Object>());
		super.setUp();
	}
	
	@After
	@Override
	public void tearDown() throws Exception {
		offlineProcessor = null;
		msgRepo = null;
		super.tearDown();
	}	

	@Test
	public void testStorageOfflineMessageForBareJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		
		Element packetEl = new Element("message", new String[] { "type", "from", "to" },
				new String[] { "chat", "remote-user@test.com/res1", userJid.toString() });
		packetEl.addChild(new Element("body", "Test message"));
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		offlineProcessor.postProcess(packet, session1, null, results, null);
		assertTrue("generated result even than no result should be generated", results.isEmpty());
		assertTrue("no message stored, while it should be stored", !msgRepo.getStored().isEmpty());
		
		msgRepo.getStored().clear();
		
		session1.setPriority(1);
		results = new ArrayDeque<Packet>();
		offlineProcessor.postProcess(packet, session1, null, results, null);
		assertTrue("generated result even than no result should be generated", results.isEmpty());
		assertTrue("no message stored, while it should be stored", !msgRepo.getStored().isEmpty());
		
		msgRepo.getStored().clear();	
		
		session1.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		offlineProcessor.postProcess(packet, session1, null, results, null);
		assertTrue("generated result even than no result should be generated", results.isEmpty());
		assertTrue("message stored, while it should not be stored", msgRepo.getStored().isEmpty());		
		
		msgRepo.getStored().clear();
	}	
	
	@Test
	public void testStorageOfflineMessageForFullJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		
		Element packetEl = new Element("message", new String[] { "type", "from", "to" },
				new String[] { "chat", "remote-user@test.com/res1", res2.toString() });
		packetEl.addChild(new Element("body", "Test message"));
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		offlineProcessor.postProcess(packet, session1, null, results, null);
		assertTrue("generated result even than no result should be generated", results.isEmpty());
		assertTrue("message stored, while it should not be stored", msgRepo.getStored().isEmpty());
		
		msgRepo.getStored().clear();
		
		session1.setPriority(1);
		results = new ArrayDeque<Packet>();
		offlineProcessor.postProcess(packet, session1, null, results, null);
		assertTrue("generated result even than no result should be generated", results.isEmpty());
		assertTrue("message stored, while it should not be stored", msgRepo.getStored().isEmpty());
		
		msgRepo.getStored().clear();	
		
		session1.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		offlineProcessor.postProcess(packet, session1, null, results, null);
		assertTrue("generated result even than no result should be generated", results.isEmpty());
		assertTrue("message stored, while it should not be stored", msgRepo.getStored().isEmpty());
		
		msgRepo.getStored().clear();
	}		


	@Test
	public void testRestorePacketForOffLineUser() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);

		assertEquals(Arrays.asList(session1), session1.getActiveSessions());

		Element packetEl = new Element("iq", new String[] { "type", "from", "to", "time" },
				new String[] { "set", "dip1@test.com/res1", userJid.toString(), "1440810935000" });
		packetEl.addChild(new Element("jingle", new String[] { "action", "sid", "offline", "xmlns" },
				new String[] { "session-terminate", UUID.randomUUID().toString(), "true", "urn:xmpp:jingle:1" }));
		Packet packet = Packet.packetInstance(packetEl);
		msgRepo.storeMessage( packet.getFrom(), packet.getTo(), null, packet.getElement(), null);

		packetEl = new Element("iq", new String[] { "type", "from", "to", "time" },
				new String[] { "set", "dip1@test.com/res1", userJid.toString(), "1440810972000" });
		packetEl.addChild(new Element("jingle", new String[] { "action", "sid", "offline", "xmlns" },
				new String[] { "session-terminate", UUID.randomUUID().toString(), "true", "urn:xmpp:jingle:1" }));
		packet = Packet.packetInstance(packetEl);
		msgRepo.storeMessage( packet.getFrom(), packet.getTo(), null, packet.getElement(), null);

		assertTrue("no message stored, while it should be stored", !msgRepo.getStored().isEmpty());

		Queue<Packet> restorePacketForOffLineUser = offlineProcessor.restorePacketForOffLineUser( session1, msgRepo );

		assertEquals("number of restored messages differ!", restorePacketForOffLineUser.size(), 2);

		msgRepo.getStored().clear();
	}

	@Test
	public void testLoadOfflineMessages() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		
		Element presenceEl = new Element("presence", new String[] { "from", "to" }, new String[] { res1.toString(), res2.toString() });
		Packet packet = Packet.packetInstance(presenceEl);
		assertFalse(offlineProcessor.loadOfflineMessages(packet, session1));
		
		presenceEl = new Element("presence", new String[] { "from" }, new String[] { res1.toString() });
		packet = Packet.packetInstance(presenceEl);
		assertTrue(offlineProcessor.loadOfflineMessages(packet, session1));	
	}	
	
	@Test
	public void testIsAllowedForOfflineStorage() throws Exception {
		Packet packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("body", "Test message")
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));
		assertTrue(offlineProcessor.isAllowedForOfflineStorage(packet));
		
		packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("storeMe1", new String[] { "xmlns" }, new String[] { "custom_xmlns" })
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));		
		
		assertFalse(offlineProcessor.isAllowedForOfflineStorage(packet));
		
		Map<String,Object> settings = new HashMap<>();
		settings.put("msg-store-offline-paths", new String[] {
			"/message/storeMe1[custom_xmlns]",
			"/message/storeMe2",
			"-/message/noStore1"
		});
		offlineProcessor.init(settings);	

		assertTrue(offlineProcessor.isAllowedForOfflineStorage(packet));

		packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("storeMe2", new String[] { "xmlns" }, new String[] { "custom_xmlns" })
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));		

		assertTrue(offlineProcessor.isAllowedForOfflineStorage(packet));
		
		packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("storeMe3", new String[] { "xmlns" }, new String[] { "custom_xmlns" })
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));	
		
		assertFalse(offlineProcessor.isAllowedForOfflineStorage(packet));	
		
		packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("body", "Test message 123"),
			new Element("no-store", new String[] { "xmlns" }, new String[] { "urn:xmpp:hints" })
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));
		
		assertFalse(offlineProcessor.isAllowedForOfflineStorage(packet));
		
		packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("noStore1"), new Element("body", "body of message")
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));	
		assertFalse(offlineProcessor.isAllowedForOfflineStorage(packet));
		
		packet = Packet.packetInstance(new Element("message", new Element[]{
			new Element("body", "body of message")
		}, new String[] { "from", "to" }, new String[] { "from@example.com/res1", "to@example.com/res2" }));	
		assertTrue(offlineProcessor.isAllowedForOfflineStorage(packet));
	}
	
	private static class MsgRepositoryIfcImpl implements MsgRepositoryIfc {

		private final Queue<Packet> stored = new ArrayDeque();
		JDBCMsgRepository jdbcMsgRepository = null;

		public MsgRepositoryIfcImpl() {
//			try {
//				Map<String,String> params = new HashMap<>();
//				params.put(DATA_REPO_POOL_SIZE_PROP_KEY,"1");
//
//				jdbcMsgRepository = new JDBCMsgRepository();
//				String jdbcuri = "jdbc:";
//				jdbcMsgRepository.initRepository(jdbcuri, params);
//			} catch (DBInitException e) {
//				e.printStackTrace();
//			}
		}

		@Override
		public Element getMessageExpired(long time, boolean delete) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete) throws UserNotFoundException {
			if (jdbcMsgRepository != null ) {
				return jdbcMsgRepository.loadMessagesToJID(session,delete);
			} else {
				Queue<Element> res = new LinkedList<Element>();
				for (Packet pac : stored) {
					res.add(pac.getElement());
				}
				return res;
			}
		}
		
		@Override
		public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo) throws UserNotFoundException {
			if (jdbcMsgRepository != null ) {
				return jdbcMsgRepository.storeMessage(from, to, expired, msg, userRepo);
			} else {
				return stored.offer(Packet.packetInstance(msg, from, to));
			}
		}
		
		@Override
		public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		}
		
		public Queue<Packet> getStored() {
			return stored;
		}
	}
	
}
