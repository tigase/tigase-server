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
package tigase.xmpp.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tigase.component.PacketWriter;
import tigase.component.responses.AsyncCallback;
import tigase.db.DataSource;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.amp.db.MsgRepository;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static tigase.Assert.assertElementEquals;

/**
 * Created by andrzej on 03.01.2017.
 */
public class PushNotificationsTest
		extends ProcessorTestCase {

	private MsgRepository msgRepository;
	private PushNotifications pushNotifications;
	private JID pushServiceJid = JID.jidInstanceNS("push.example.com");
	private JID recipientJid;
	private JID senderJid;

	@Before
	@Override
	public void setUp() throws Exception {
		recipientJid = JID.jidInstanceNS("recipient-" + UUID.randomUUID() + "@example.com/res-1");
		senderJid = JID.jidInstanceNS("sender-" + UUID.randomUUID() + "@example.com/res-1");
		super.setUp();
		registerLocalBeans(getKernel());
		pushNotifications = getInstance(PushNotifications.class);
		msgRepository = getInstance(MsgRepository.class);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void test_initialState() throws Exception {
		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		Queue<Packet> results = new ArrayDeque<>();
		pushNotifications.notifyNewOfflineMessage(packet, null, results, new HashMap<>());

		assertEquals(0, results.size());
	}

	@Test
	public void test_enable() throws Exception {
		Element iqEl = new Element("iq", new Element[]{new Element("enable", new String[]{"xmlns", "jid", "node"},
																   new String[]{"urn:xmpp:push:0",
																				pushServiceJid.toString(),
																				"push-node"})},
								   new String[]{"type", "id"}, new String[]{"set", UUID.randomUUID().toString()});

		XMPPResourceConnection session = getSession(
				JID.jidInstanceNS("c2s@example.com/" + UUID.randomUUID().toString()), recipientJid);

		Packet iq = Packet.packetInstance(iqEl);
		iq.setPacketFrom(session.getConnectionId());
		Queue<Packet> results = new ArrayDeque<>();
		pushNotifications.process(iq, session, null, results, new HashMap<>());
		assertEquals(1, results.size());
		Packet result = results.poll();
		assertEquals("wrong result = " + result, StanzaType.result, result.getType());

		assertNotNull(getInstance(UserRepository.class).getData(recipientJid.getBareJID(), "urn:xmpp:push:0",
																pushServiceJid + "/push-node"));
		Map<String, Element> settings = pushNotifications.getPushServices(recipientJid.getBareJID());
		assertNotNull(settings);
		assertEquals(1, settings.size());
		assertElementEquals(new Element("settings", new String[]{"jid", "node"},
										new String[]{pushServiceJid.toString(), "push-node"}),
							settings.get(pushServiceJid + "/push-node"));

		settings = pushNotifications.getPushServices(session);
		assertNotNull(settings);
		assertEquals(1, settings.size());
		assertElementEquals(new Element("settings", new String[]{"jid", "node"},
										new String[]{pushServiceJid.toString(), "push-node"}),
							settings.get(pushServiceJid + "/push-node"));
	}

	@Test
	public void test_disable() throws Exception {
		Element iqEl = new Element("iq", new Element[]{new Element("disable", new String[]{"xmlns", "jid", "node"},
																   new String[]{"urn:xmpp:push:0",
																				pushServiceJid.toString(),
																				"push-node"})},
								   new String[]{"type", "id"}, new String[]{"set", UUID.randomUUID().toString()});

		XMPPResourceConnection session = getSession(
				JID.jidInstanceNS("c2s@example.com/" + UUID.randomUUID().toString()), recipientJid);
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  new Element("settings", new String[]{"jid", "node"},
															  new String[]{pushServiceJid.toString(),
																		   "push-node"}).toString());

		Packet iq = Packet.packetInstance(iqEl);
		iq.setPacketFrom(session.getConnectionId());
		Queue<Packet> results = new ArrayDeque<>();
		pushNotifications.process(iq, session, null, results, new HashMap<>());
		assertEquals(1, results.size());
		Packet result = results.poll();
		assertEquals("wrong result = " + result, StanzaType.result, result.getType());

		assertNull(getInstance(UserRepository.class).getData(recipientJid.getBareJID(), "urn:xmpp:push:0",
															 pushServiceJid + "/push-node"));


		Map<String, Element> settings = pushNotifications.getPushServices(recipientJid.getBareJID());
		assertNotNull(settings);
		assertEquals(0, settings.size());
		
		settings = pushNotifications.getPushServices(session);
		assertNotNull(settings);
		assertEquals(0, settings.size());
	}

	@Test
	public void test_notificationGeneration() throws Exception {
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  new Element("settings", new String[]{"jid", "node"},
															  new String[]{pushServiceJid.toString(),
																		   "push-node"}).toString());

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<Packet> results = new ArrayDeque<>();
		pushNotifications.notifyNewOfflineMessage(packet, null, results, new HashMap<>());

		assertEquals(1, results.size());

		Packet expNotification = PushNotificationHelper.createPushNotification(pushServiceJid, recipientJid,
																			   "push-node",
																			   PushNotificationHelper.createNotification(
																					   1, senderJid, msgBody));

		assertElementEquals(expNotification.getElement(), results.poll().getElement());

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
						  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		results = new ArrayDeque<>();
		pushNotifications.notifyNewOfflineMessage(packet, null, results, new HashMap<>());

		assertEquals(1, results.size());

		expNotification = PushNotificationHelper.createPushNotification(pushServiceJid, recipientJid, "push-node",
																		PushNotificationHelper.createNotification(2,
																												  senderJid,
																												  msgBody));

		assertElementEquals(expNotification.getElement(), results.poll().getElement());
	}

	@Test
	public void test_notificationGenerationForMUCwhenOnline() throws Exception {
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  new Element("settings", new String[]{"jid", "node"},
															  new String[]{pushServiceJid.toString(),
																		   "push-node"}).toString());

		XMPPResourceConnection session = this.getSession(JID.jidInstanceNS(UUID.randomUUID().toString(), recipientJid.getDomain()), recipientJid);

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		Queue<Packet> results = new ArrayDeque<>();
		pushNotifications.process(packet, session, null, results, new HashMap<>());

		assertEquals(0, results.size());

		Element presence = new Element("presence");
		session.setPresence(presence);

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
						  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results = new ArrayDeque<>();
		pushNotifications.process(packet, session, null, results, new HashMap<>());

		assertEquals(0, results.size());

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns", "type"},
						  new String[]{"jabber:client", "groupchat"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results = new ArrayDeque<>();
		pushNotifications.process(packet, session, null, results, new HashMap<>());

		assertEquals(1, results.size());

	}

	protected void registerLocalBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean("sess-man").asInstance(this.getSessionManagerHandler()).setActive(true).exportable().exec();//.asClass(DummySessionManager.class).setActive(true).exportable().exec();
		kernel.registerBean(MessageAmp.class).setActive(true).exportable().exec();
		kernel.registerBean("msgRepository").asClass(MsgRepositoryIfcImpl.class).exportable().exec();
		kernel.registerBean(PushNotifications.class).setActive(true).exec();
	}

	public static class MsgRepositoryIfcImpl
			extends MsgRepository {

		private final Queue<Packet> stored = new ArrayDeque();

		public MsgRepositoryIfcImpl() {
		}

		@Override
		public Element getMessageExpired(long time, boolean delete) {
			throw new UnsupportedOperationException(
					"Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete)
				throws UserNotFoundException {
			Queue<Element> res = new LinkedList<Element>();
			for (Packet pac : stored) {
				res.add(pac.getElement());
			}
			return res;
		}

		@Override
		public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo)
				throws UserNotFoundException {
			return stored.offer(Packet.packetInstance(msg, from, to));
		}

		@Override
		public Map<Enum, Long> getMessagesCount(JID to) throws UserNotFoundException {
			return stored.stream().collect(Collectors.groupingBy(packet -> {
				switch (packet.getElemName()) {
					case "message":
						return MSG_TYPES.message;
					case "presence":
						return MSG_TYPES.presence;
					default:
						return MSG_TYPES.none;
				}
			}, Collectors.counting()));
		}

		@Override
		public List<Element> getMessagesList(JID to) throws UserNotFoundException {
			return null;
		}

		@Override
		public int deleteMessagesToJID(List db_ids, XMPPResourceConnection session) throws UserNotFoundException {
			return 0;
		}

		@Override
		public Queue<Element> loadMessagesToJID(List db_ids, XMPPResourceConnection session, boolean delete,
												OfflineMessagesProcessor proc) throws UserNotFoundException {
			return null;
		}

		public Queue<Packet> getStored() {
			return stored;
		}

		@Override
		public void setDataSource(DataSource dataSource) {

		}

		@Override
		protected void loadExpiredQueue(int max) {

		}

		@Override
		protected void loadExpiredQueue(Date expired) {

		}

		@Override
		protected void deleteMessage(Object db_id) {

		}
	}

	public static class DummyPacketWriter implements PacketWriter {

		private Queue<Packet> outQueue = new ArrayDeque<>();

		public DummyPacketWriter() {}

		public Queue<Packet> getOutQueue() {
			return outQueue;
		}

		@Override
		public void write(Collection<Packet> packets) {
			outQueue.addAll(packets);
		}

		@Override
		public void write(Packet packet) {
			outQueue.offer(packet);
		}

		@Override
		public void write(Packet packet, AsyncCallback callback) {
			throw new UnsupportedOperationException("Method not implemented!");
		}
	}

}
