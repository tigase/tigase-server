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
package tigase.xmpp.impl.push;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tigase.db.*;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.impl.EventBusImplementation;
import tigase.kernel.core.Kernel;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.PolicyViolationException;
import tigase.server.amp.db.MsgRepository;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.MessageDeliveryLogic;
import tigase.xmpp.impl.OfflineMessages;
import tigase.xmpp.impl.ProcessorTestCase;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;
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
		Field f = RosterFactory.class.getDeclaredField("shared");
		f.setAccessible(true);
		f.set(null, RosterFactory.newRosterInstance(RosterFactory.ROSTER_IMPL_PROP_VAL));
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

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());
	}

	@Test
	public void test_enable() throws Exception {
		XMPPResourceConnection session = getSession(
				JID.jidInstanceNS("c2s@example.com/" + UUID.randomUUID().toString()), recipientJid);

		enable(session,null);
	}
	
	protected void enable(XMPPResourceConnection session, Consumer<Element> consumer) throws Exception {
		Element enableEl = new Element("enable", new String[]{"xmlns", "jid", "node"},
									   new String[]{"urn:xmpp:push:0",
													pushServiceJid.toString(),
													"push-node"});
		if (consumer != null) {
			consumer.accept(enableEl);
		}
		Element iqEl = new Element("iq", new Element[]{ enableEl },
								   new String[]{"type", "id"}, new String[]{"set", UUID.randomUUID().toString()});


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

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		Packet expNotification = PushNotificationHelper.createPushNotification(pushServiceJid, recipientJid,
																			   "push-node",
																			   PushNotificationHelper.createPlainNotification(
																					   1, senderJid, msgBody));

		assertElementEquals(expNotification.getElement(), results.poll().packet.getElement());

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
						  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		results.clear();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		expNotification = PushNotificationHelper.createPushNotification(pushServiceJid, recipientJid, "push-node",
																		PushNotificationHelper.createPlainNotification(2,
																												  senderJid,
																												  msgBody));

		assertElementEquals(expNotification.getElement(), results.poll().packet.getElement());
	}

	@Test
	public void test_notificationGenerationForOMEMO() throws Exception {
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  new Element("settings", new String[]{"jid", "node"},
															  new String[]{pushServiceJid.toString(),
																		   "push-node"}).toString());

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		msg.addChild(new Element("encrypted", new String[]{"xmlns"}, new String[]{"eu.siacs.conversations.axolotl"}));
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		Packet expNotification = PushNotificationHelper.createPushNotification(pushServiceJid, recipientJid,
																			   "push-node",
																			   PushNotificationHelper.createPlainNotification(
																					   1, senderJid, msgBody));

		assertElementEquals(expNotification.getElement(), results.poll().packet.getElement());

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
						  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		results.clear();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		expNotification = PushNotificationHelper.createPushNotification(pushServiceJid, recipientJid, "push-node",
																		PushNotificationHelper.createPlainNotification(2,
																													   senderJid,
																													   "New secure message. Open to see the message."));

		assertElementEquals(expNotification.getElement(), results.poll().packet.getElement());
	}

	@Test
	public void test_notificationGenerationForMUCwhenOnlineOLD() throws Exception {
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

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());

		Element presence = new Element("presence");
		session.setPresence(presence);

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
						  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results.clear();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns", "type"},
						  new String[]{"jabber:client", "groupchat"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results.clear();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());
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

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());

		Element presence = new Element("presence");
		session.setPresence(presence);

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
						  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results.clear();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns", "type"},
						  new String[]{"jabber:client", "groupchat"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results.clear();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		enable(session, enableEl -> {
			enableEl.withElement("groupchat", "tigase:push:filter:groupchat:0", mucEl -> {
				mucEl.withElement("room", roomEl -> {
					roomEl.setAttribute("jid", senderJid.getBareJID().toString());
					roomEl.setAttribute("allow", "always");
				});
			});
		});

		msgBody = "Message body " + UUID.randomUUID().toString() + " - my_nick";
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns", "type"},
						  new String[]{"jabber:client", "groupchat"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results.clear();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		enable(session, enableEl -> {
			enableEl.withElement("groupchat", "tigase:push:filter:groupchat:0", mucEl -> {
				mucEl.withElement("room", roomEl -> {
					roomEl.setAttribute("jid", senderJid.getBareJID().toString());
					roomEl.setAttribute("allow", "mentioned");
					roomEl.setAttribute("nick", "my_nick");
				});
			});
		});

		msgBody = "Message body " + UUID.randomUUID().toString() + " - my_nick";
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns", "type"},
						  new String[]{"jabber:client", "groupchat"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		results.clear();
		pushNotifications.process(packet, session, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());
	}

	@Test
	public void testMutingNotifications() throws TigaseDBException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		settings.withElement("muted", "tigase:push:filter:muted:0", mutedEl -> {
			mutedEl.withElement("item", itemEl -> {
				itemEl.setAttribute("jid", senderJid.getBareJID().toString());
			});
		});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());
	}

	@Test
	public void testIgnoreUnknownNotifications()
			throws TigaseDBException, NotAuthorizedException, TigaseStringprepException, PolicyViolationException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		settings.addAttribute("ignore-from-unknown", "true");
//		settings.withElement("ignore-unknown", "tigase:push:ignore-unknown:0", (String) null);
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(0, results.size());

		RosterAbstract roster = RosterFactory.getRosterImplementation(true);
		XMPPResourceConnection session = getSession(JID.jidInstanceNS("test@test/1"), recipientJid);
		roster.addBuddy(session, senderJid.copyWithoutResource(), "Sender 1", null, RosterAbstract.SubscriptionType.both, null);

		session.logout();

		msgBody = "Message body " + UUID.randomUUID().toString();
		msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		results.clear();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());
	}

	@Test
	public void testEncryptedNotification() throws TigaseDBException, NoSuchPaddingException, NoSuchAlgorithmException,
												   InvalidAlgorithmParameterException, InvalidKeyException,
												   BadPaddingException, IllegalBlockSizeException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		SecureRandom random = new SecureRandom();
		byte[] key = new byte[128/8];
		random.nextBytes(key);
		settings.withElement("encrypt", "tigase:push:encrypt:0", el -> {
			el.setCData(Base64.encode(key));
			el.setAttribute("alg", "aes-128-gcm");
		});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		Element notificationElem = results.remove().packet.getElement().findChild(new String[] { "iq", "pubsub", "publish", "item", "notification"});
		Element encrypted = notificationElem.getChild("encrypted", "tigase:push:encrypt:0");
		byte[] enc = Base64.decode(encrypted.getCData());
		byte[] iv = Base64.decode(encrypted.getAttributeStaticStr("iv"));

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
		byte[] decoded = cipher.doFinal(enc);

		assertTrue(new String(decoded).contains(msgBody));
	}

	@Test
	public void testEncryptedJingleNotification() throws TigaseDBException, NoSuchPaddingException, NoSuchAlgorithmException,
												   InvalidAlgorithmParameterException, InvalidKeyException,
												   BadPaddingException, IllegalBlockSizeException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		settings.addChild(new Element("jingle", new String[] {"xmlns", "id"}, new String[] {"tigase:push:jingle:0", "test-1"}));
		SecureRandom random = new SecureRandom();
		byte[] key = new byte[128/8];
		random.nextBytes(key);
		settings.withElement("encrypt", "tigase:push:encrypt:0", el -> {
			el.setCData(Base64.encode(key));
			el.setAttribute("alg", "aes-128-gcm");
		});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		Element msg = new Element("message", new Element[]{new Element("propose", new String[] {"xmlns", "id"}, new String[] {"urn:xmpp:jingle-message:0", "test-1"})}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		Element notificationElem = results.remove().packet.getElement().findChild(new String[] { "iq", "pubsub", "publish", "item", "notification"});
		Element encrypted = notificationElem.getChild("encrypted", "tigase:push:encrypt:0");
		assertEquals("voip", encrypted.getAttributeStaticStr("type"));
		byte[] enc = Base64.decode(encrypted.getCData());
		byte[] iv = Base64.decode(encrypted.getAttributeStaticStr("iv"));

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
		byte[] decoded = cipher.doFinal(enc);

		assertTrue(new String(decoded).contains("test-1"));
	}

	@Test
	public void testEncryptedJingleNotification_reject()
			throws TigaseDBException, NoSuchPaddingException, NoSuchAlgorithmException,
				   InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException,
				   IllegalBlockSizeException, NotAuthorizedException, TigaseStringprepException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		settings.addChild(new Element("jingle", new String[] {"xmlns", "id"}, new String[] {"tigase:push:jingle:0", "test-1"}));
		SecureRandom random = new SecureRandom();
		byte[] key = new byte[128/8];
		random.nextBytes(key);
		settings.withElement("encrypt", "tigase:push:encrypt:0", el -> {
			el.setCData(Base64.encode(key));
			el.setAttribute("alg", "aes-128-gcm");
		});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		Element msg = new Element("message", new Element[]{new Element("reject", new String[] {"xmlns", "id"}, new String[] {"urn:xmpp:jingle-message:0", "test-1"})}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		Packet packet = Packet.packetInstance(msg, recipientJid, senderJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		XMPPResourceConnection session = getSession(JID.jidInstanceNS("test-1", "example.com", "test-1"), recipientJid);
		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, session, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		Element notificationElem = results.remove().packet.getElement().findChild(new String[] { "iq", "pubsub", "publish", "item", "notification"});
		Element encrypted = notificationElem.getChild("encrypted", "tigase:push:encrypt:0");
		assertEquals("voip", encrypted.getAttributeStaticStr("type"));
		byte[] enc = Base64.decode(encrypted.getCData());
		byte[] iv = Base64.decode(encrypted.getAttributeStaticStr("iv"));

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
		byte[] decoded = cipher.doFinal(enc);

		assertTrue(new String(decoded).contains("test-1"));
	}

	@Test
	public void testEncryptedNotificationForOMEMO() throws TigaseDBException, NoSuchPaddingException, NoSuchAlgorithmException,
												   InvalidAlgorithmParameterException, InvalidKeyException,
												   BadPaddingException, IllegalBlockSizeException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		SecureRandom random = new SecureRandom();
		byte[] key = new byte[128/8];
		random.nextBytes(key);
		settings.withElement("encrypt", "tigase:push:encrypt:0", el -> {
			el.setCData(Base64.encode(key));
			el.setAttribute("alg", "aes-128-gcm");
		});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns"},
								  new String[]{"jabber:client"});
		msg.addChild(new Element("encrypted", new String[]{"xmlns"}, new String[]{"eu.siacs.conversations.axolotl"}));
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid);

		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		assertEquals(1, results.size());

		Element notificationElem = results.remove().packet.getElement().findChild(new String[] { "iq", "pubsub", "publish", "item", "notification"});
		Element encrypted = notificationElem.getChild("encrypted", "tigase:push:encrypt:0");
		byte[] enc = Base64.decode(encrypted.getCData());
		byte[] iv = Base64.decode(encrypted.getAttributeStaticStr("iv"));

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
		byte[] decoded = cipher.doFinal(enc);

		assertTrue(new String(decoded).contains("New secure message. Open to see the message."));
	}


	record GroupchatSettings(BareJID jid, Allow allow, String nick) {
		enum Allow {
			always,
			mentioned,
			never
		}
	}
	record Settings(String pushNode, byte[] key, List<GroupchatSettings> groupchatSettings) {
		public static Settings create(List<GroupchatSettings> groupchatSettings) {
			SecureRandom random = new SecureRandom();
			byte[] key = new byte[128/8];
			random.nextBytes(key);
			return new Settings("push-node-" + UUID.randomUUID(), key, groupchatSettings);
		}
	}

	private void setSettingsWithMuc(Settings dto) throws TigaseDBException {
		Element settings = new Element("settings", new String[]{"jid", "node", "priority"},
									   new String[]{pushServiceJid.toString(), dto.pushNode(), "true"});

		settings.withElement("encrypt", "tigase:push:encrypt:0", el -> {
			el.setCData(Base64.encode(dto.key()));
			el.setAttribute("max-size", "3072");
			el.setAttribute("alg", "aes-128-gcm");
		});
		if (dto.groupchatSettings() != null) {
			settings.withElement("groupchat", "tigase:push:filter:groupchat:0", el -> {
				for (GroupchatSettings groupchat : dto.groupchatSettings()) {
					el.withElement("room", roomEl -> {
						roomEl.withAttribute("jid", groupchat.jid().toString())
								.withAttribute("allow", groupchat.allow().name());
						Optional.ofNullable(groupchat.nick()).ifPresent(nick -> el.withAttribute("nick", nick));
					});
				}
			});
		}
		settings.withElement("jingle", el -> {
			el.setXMLNS("tigase:push:jingle:0");
		});
		System.out.println("saving settings: " + settings.toString());
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/" + dto.pushNode(),
												  settings.toString());
	}

	@Test
	public void testEncryptedNotificationForGroupchatOMEMO() throws TigaseDBException, NoSuchPaddingException, NoSuchAlgorithmException,
														   InvalidAlgorithmParameterException, InvalidKeyException,
														   BadPaddingException, IllegalBlockSizeException {
		List<Settings> allSettings = List.of(
				Settings.create(List.of(new GroupchatSettings(senderJid.getBareJID(), GroupchatSettings.Allow.mentioned, "test-123"))),
				Settings.create(List.of(new GroupchatSettings(senderJid.getBareJID(), GroupchatSettings.Allow.never, "test-123"))),
				Settings.create(List.of()),
				Settings.create(List.of(new GroupchatSettings(senderJid.getBareJID(), GroupchatSettings.Allow.always, null)))
		);

		for (Settings settings : allSettings) {
			setSettingsWithMuc(settings);
		}

		String msgBody = "Message body " + UUID.randomUUID().toString();
		Element msg = new Element("message", new Element[]{new Element("body", msgBody)}, new String[]{"xmlns", "type"},
								  new String[]{"jabber:client", "groupchat"});
		msg.addChild(new Element("encrypted", new String[]{"xmlns"}, new String[]{"eu.siacs.conversations.axolotl"}));
		msg.addChild(new Element("store", new String[]{"xmlns"}, new String[]{"urn:xmpp:hints"}));
		Packet packet = Packet.packetInstance(msg, senderJid, recipientJid.copyWithoutResource());

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		OfflineMessages offlineMessages = getInstance(OfflineMessages.class);
		offlineMessages.postProcess(packet, null, null, new ArrayDeque<>(), new HashMap<>());

//		msgRepository.storeMessage(senderJid, recipientJid, new Date(), packet.getElement(), null);
//
//		pushNotifications.notifyNewOfflineMessage(packet, null, new ArrayDeque<>(), new HashMap<>());

		Settings usedSettings = allSettings.get(3);
		
		assertEquals(1, results.size());

		Element notificationElem = results.remove().packet.getElement().findChild(new String[] { "iq", "pubsub", "publish", "item", "notification"});
		System.out.println(notificationElem);
		Element encrypted = notificationElem.getChild("encrypted", "tigase:push:encrypt:0");
		byte[] enc = Base64.decode(encrypted.getCData());
		byte[] iv = Base64.decode(encrypted.getAttributeStaticStr("iv"));

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(usedSettings.key(), "AES"), new GCMParameterSpec(128, iv));
		byte[] decoded = cipher.doFinal(enc);

		assertTrue(new String(decoded).contains("New secure message. Open to see the message."));
	}
	
	@Test
	public void testMessageRetrieved() throws TigaseDBException, NotAuthorizedException, TigaseStringprepException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		settings.setAttribute("priority", "true");
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyOfflineMessagesRetrieved(getSession(recipientJid, recipientJid), new ArrayDeque<>());

		assertEquals(1, results.size());
		assertEquals("low", results.poll().packet.getElemCDataStaticStr(
				new String[]{"iq", "pubsub", "publish", "item", "notification", "priority"}));
//		System.out.println("results:" + results);
	}

	@Test
	public void testMessageRetrieved2() throws TigaseDBException, NotAuthorizedException, TigaseStringprepException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());

		Queue<SessionManagerHandlerImpl.Item> results = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		pushNotifications.notifyOfflineMessagesRetrieved(getSession(recipientJid, recipientJid), new ArrayDeque<>());

		assertEquals(1, results.size());
		assertEquals(null, results.poll().packet.getElemCDataStaticStr(
				new String[]{"iq", "pubsub", "publish", "item", "notification", "priority"}));
//		System.out.println("results:" + results);
	}

	@Test
	public void testPushError() throws TigaseDBException, XMPPException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());
		Element settings1 = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node-1"});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node-1",
												  settings1.toString());
		
		Queue<Packet> results = new ArrayDeque<>();
		Packet message = Message.getMessage(senderJid, recipientJid, StanzaType.chat, "Message " +
				UUID.randomUUID()
						.toString(), null, null, UUID.randomUUID().toString());
		pushNotifications.notifyNewOfflineMessage(message, null, results, new HashMap<>());

		Queue<SessionManagerHandlerImpl.Item> items = getInstance(SessionManagerHandlerImpl.class).getOutQueue();

		assertEquals(items.size(), 2);

		SessionManagerHandlerImpl.Item item = null;
		while ((item = items.poll()) != null) {
			if ("push-node-1".equals(
					item.packet.getAttributeStaticStr(new String[]{"iq", "pubsub", "publish"}, "node"))) {
				item.handler.handle(Authorization.ITEM_NOT_FOUND.getResponseMessage(item.packet, null, false));
			} else {
				item.handler.handle(item.packet.okResult((String) null, 0));
			}
		}

		Map<String, String> data = getInstance(UserRepository.class).getDataMap(recipientJid.getBareJID(), "urn:xmpp:push:0");
		assertNotNull(data);
		assertEquals(1, data.size());
		assertNotNull(data.get(pushServiceJid.toString() + "/push-node"));
	}

	@Test
	public void testPushDisableByComponent() throws TigaseDBException, NotAuthorizedException {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());
		Element settings1 = new Element("settings", new String[]{"jid", "node"},
										new String[]{pushServiceJid.toString(),
													 "push-node-1"});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node-1",
												  settings1.toString());

		Queue<Packet> results = new ArrayDeque<>();
		Packet message = Packet.packetInstance(new Element("message").withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
			pubsubEl.setAttribute("node", "push-node-1");
			pubsubEl.withElement("affiliation", affEl -> {
				affEl.setAttribute("jid", recipientJid.toString());
				affEl.setAttribute("affiliation", "none");
			});
		}), pushServiceJid, recipientJid);
		pushNotifications.processMessage(message, null, new ArrayDeque<>()::offer);
		
		Map<String, String> data = getInstance(UserRepository.class).getDataMap(recipientJid.getBareJID(), "urn:xmpp:push:0");
		assertNotNull(data);
		assertEquals(1, data.size());
		assertNotNull(data.get(pushServiceJid.toString() + "/push-node"));
	}

	@Test
	public void test_accountRemovalNotificationGeneration() throws Exception {
		EventBus eventBus = new EventBusImplementation();
		eventBus.registerAll(pushNotifications);
		try {
			getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
													  pushServiceJid + "/push-node",
													  new Element("settings", new String[]{"jid", "node"},
																  new String[]{pushServiceJid.toString(),
																			   "push-node"}).toString());

			eventBus.fire(new UserRepository.UserBeforeRemovedEvent(recipientJid.getBareJID()));
			Queue<SessionManagerHandlerImpl.Item> items = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
			assertEquals(0, items.size());

			Field f = PushNotifications.class.getDeclaredField("sendAccountRemovalNotification");
			f.setAccessible(true);
			f.set(pushNotifications, true);

			eventBus.fire(new UserRepository.UserBeforeRemovedEvent(recipientJid.getBareJID()));
			items = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
			assertEquals(1, items.size());
			Packet packet = items.poll().getPacket();
			assertTrue(packet.getElement().toString().contains("account-removed"));
		} finally {
			eventBus.unregisterAll(pushNotifications);
		}
	}
	
	@Test
	public void test_accountRemovalNotificationGenerationEncrypted() throws Exception {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{pushServiceJid.toString(),
													"push-node"});
		settings.addChild(new Element("jingle", new String[] {"xmlns", "id"}, new String[] {"tigase:push:jingle:0", "test-1"}));
		SecureRandom random = new SecureRandom();
		byte[] key = new byte[128/8];
		random.nextBytes(key);
		settings.withElement("encrypt", "tigase:push:encrypt:0", el -> {
			el.setCData(Base64.encode(key));
			el.setAttribute("alg", "aes-128-gcm");
		});
		getInstance(UserRepository.class).setData(recipientJid.getBareJID(), "urn:xmpp:push:0",
												  pushServiceJid + "/push-node",
												  settings.toString());
		
		pushNotifications.onUserRemoved(new UserRepository.UserBeforeRemovedEvent(recipientJid.getBareJID()));
		Queue<SessionManagerHandlerImpl.Item> items = getInstance(SessionManagerHandlerImpl.class).getOutQueue();
		assertEquals(0, items.size());

		Field f = PushNotifications.class.getDeclaredField("sendAccountRemovalNotification");
		f.setAccessible(true);
		f.set(pushNotifications, true);

		pushNotifications.onUserRemoved(new UserRepository.UserBeforeRemovedEvent(recipientJid.getBareJID()));
		
		Element notificationElem = items.poll().getPacket().getElement().findChild(new String[] { "iq", "pubsub", "publish", "item", "notification"});
		Element encrypted = notificationElem.getChild("encrypted", "tigase:push:encrypt:0");
		byte[] enc = Base64.decode(encrypted.getCData());
		byte[] iv = Base64.decode(encrypted.getAttributeStaticStr("iv"));

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
		byte[] decoded = cipher.doFinal(enc);

		assertTrue(new String(decoded, StandardCharsets.UTF_8).contains("account-removed"));
	}

	protected void registerLocalBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean("sess-man").asInstance(this.getSessionManagerHandler()).setActive(true).exportable().exec();//.asClass(DummySessionManager.class).setActive(true).exportable().exec();
		kernel.registerBean(MessageDeliveryLogic.class).setActive(true).exportable().exec();
		kernel.registerBean("msgRepository").asClass(MsgRepositoryIfcImpl.class).exportable().exec();
		kernel.registerBean(OfflineMessages.class).setActive(true).exec();
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

}
