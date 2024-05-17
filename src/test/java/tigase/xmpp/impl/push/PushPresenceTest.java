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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import tigase.component.PacketWriter;
import tigase.component.responses.AsyncCallback;
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.PolicyViolationException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.MessageDeliveryLogic;
import tigase.xmpp.impl.PresenceState;
import tigase.xmpp.impl.ProcessorTestCase;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PushPresenceTest
		extends ProcessorTestCase {

	private RosterAbstract rosterUtil;
	private PushPresence pushPresence;
	private BareJID userJid;
	private BareJID buddyJid;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		registerLocalBeans(getKernel());
		rosterUtil = RosterFactory.newRosterInstance(RosterFactory.ROSTER_IMPL_PROP_VAL);
		rosterUtil.setEventBus(getInstance(EventBus.class));
		userJid = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain.com");
		buddyJid = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "domain.com");
		pushPresence = getInstance(PushPresence.class);
		pushPresence.setRosterUtil(rosterUtil);
	}

	@After
	public void tearDown() throws Exception {
		getInstance(PushPresence.class).beforeUnregister();
		super.tearDown();
	}
	
	@Test
	public void testSendingPresence_OnPushEnable_NoSubscription() throws TigaseStringprepException, TigaseDBException, NotAuthorizedException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);
		XMPPResourceConnection session = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));
		pushNotifications.enableNotifications(session, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		assertTrue(pushPresence.isPushAvailable(userJid));
		assertEquals(0, writer.getQueue().size());
	}

	@Test
	public void testSendingPresence_OnPushEnable_WithSubscription()
			throws TigaseStringprepException, TigaseDBException, NotAuthorizedException, PolicyViolationException,
				   NoConnectionIdException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);
		XMPPResourceConnection buddySession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(buddyJid, "setter"));
		XMPPResourceConnection userSession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));

		updateRoster(userSession, JID.jidInstance(buddyJid), RosterAbstract.SubscriptionType.both);
		updateRoster(buddySession, JID.jidInstance(userJid), RosterAbstract.SubscriptionType.both);

		pushNotifications.enableNotifications(userSession, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		assertTrue(pushPresence.isPushAvailable(userJid));
		assertEquals(1, writer.getQueue().size());
		assertPresenceAway(writer.getQueue().poll());
	}

	@Test
	public void testSendingPresence_OnPushDisable_NoSubscription() throws TigaseStringprepException, TigaseDBException, NotAuthorizedException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);
		XMPPResourceConnection session = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));
		pushNotifications.enableNotifications(session, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		assertTrue(pushPresence.isPushAvailable(userJid));
		assertEquals(0, writer.getQueue().size());

		pushNotifications.disableNotifications(session, userJid,  JID.jidInstance("push.localhost"), "test-node", writer::write);
		
		assertFalse(pushPresence.isPushAvailable(userJid));
		assertEquals(0, writer.getQueue().size());
	}

	@Test
	public void testSendingPresence_OnPushDisable_WithSubscription()
			throws TigaseStringprepException, TigaseDBException, NotAuthorizedException, PolicyViolationException,
				   NoConnectionIdException, InterruptedException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);

		XMPPResourceConnection buddySession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(buddyJid, "setter"));
		XMPPResourceConnection userSession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));

		pushNotifications.enableNotifications(userSession, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		assertTrue(pushPresence.isPushAvailable(userJid));

		updateRoster(userSession, JID.jidInstance(buddyJid), RosterAbstract.SubscriptionType.both);
		updateRoster(buddySession, JID.jidInstance(userJid), RosterAbstract.SubscriptionType.both);

		System.out.println("packet writer " + writer);
		assertEquals(1, writer.getQueue().size());
		assertEquals(1, writer.getQueue().size());
		assertPresenceAway(writer.getQueue().poll());

		pushNotifications.disableNotifications(userSession, userJid,  JID.jidInstance("push.localhost"), "test-node", writer::write);
		assertEquals(1, writer.getQueue().size());
		assertPresenceUnavailable(writer.getQueue().poll());
	}

	@Test
	public void testSendingPresence_OnPresenceProbe_NoSubscription()
			throws TigaseStringprepException, TigaseDBException, XMPPException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);
		XMPPResourceConnection session = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));
		pushNotifications.enableNotifications(session, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		assertTrue(pushPresence.isPushAvailable(userJid));
		assertEquals(0, writer.getQueue().size());

		PresenceState presenceState = getInstance(PresenceState.class);
		Packet probe = Packet.packetInstance(new Element("presence").withAttribute("type", StanzaType.probe.name())
													 .withAttribute("from", buddyJid.toString())
													 .withAttribute("to", userJid.toString()));
		presenceState.process(probe, null, null, writer.getQueue(), new HashMap<>());
		assertEquals(0, writer.getQueue().size());
	}

	@Test
	public void testSendingPresence_OnSubscriptionAndOnPresenceProbe_WithSubscription()
			throws TigaseStringprepException, TigaseDBException, XMPPException, PolicyViolationException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);
		XMPPResourceConnection userSession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));
		pushNotifications.enableNotifications(userSession, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		XMPPResourceConnection buddySession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(buddyJid, "setter"));

		updateRoster(userSession, JID.jidInstance(buddyJid), RosterAbstract.SubscriptionType.both);
		updateRoster(buddySession, JID.jidInstance(userJid), RosterAbstract.SubscriptionType.both);

		assertTrue(pushPresence.isPushAvailable(userJid));
		// add user roster subscription triggers sending presence
		assertEquals(1, writer.getQueue().size());
		assertPresenceAway(writer.getQueue().poll());
		assertEquals(0, writer.getQueue().size());

		PresenceState presenceState = getInstance(PresenceState.class);
		Packet probe = Packet.packetInstance(new Element("presence").withAttribute("type", StanzaType.probe.name())
													 .withAttribute("from", buddyJid.toString())
													 .withAttribute("to", userJid.toString()));
		presenceState.process(probe, null, null, writer.getQueue(), new HashMap<>());
		assertEquals(1, writer.getQueue().size());
		assertPresenceAway(writer.getQueue().poll());
	}

	@Test
	public void testSendingPresence_OnSubscriptionAndOnRemovingSubscription()
			throws TigaseStringprepException, TigaseDBException, XMPPException, PolicyViolationException {
		TestPacketWriter writer = getInstance(TestPacketWriter.class);
		assertFalse(pushPresence.isPushAvailable(userJid));
		PushNotifications pushNotifications = getInstance(PushNotifications.class);
		XMPPResourceConnection userSession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(userJid, "setter"));
		pushNotifications.enableNotifications(userSession, JID.jidInstance("push.localhost"), "test-node", new Element("enable"), null, writer::write);
		XMPPResourceConnection buddySession = getSession(JID.jidInstance(UUID.randomUUID().toString(), "domain", null), JID.jidInstance(buddyJid, "setter"));

		updateRoster(userSession, JID.jidInstance(buddyJid), RosterAbstract.SubscriptionType.both);
		updateRoster(buddySession, JID.jidInstance(userJid), RosterAbstract.SubscriptionType.both);

		assertTrue(pushPresence.isPushAvailable(userJid));
		// add user roster subscription triggers sending presence
		System.out.println("packet writer " + writer + " - in test");
		assertEquals(1, writer.getQueue().size());
		assertPresenceAway(writer.getQueue().poll());
		assertEquals(0, writer.getQueue().size());

		updateRoster(userSession, JID.jidInstance(buddyJid), RosterAbstract.SubscriptionType.none);
		updateRoster(buddySession, JID.jidInstance(userJid), RosterAbstract.SubscriptionType.none);

		assertEquals(1, writer.getQueue().size());
		assertPresenceUnavailable(writer.getQueue().poll());
	}

	private void assertPresenceUnavailable(Packet presence) {
		assertEquals("presence", presence.getElemName());
		assertEquals(StanzaType.unavailable, presence.getType());
	}

	private void assertPresenceAway(Packet presence) {
		assertEquals("presence", presence.getElemName());
		assertEquals(null, presence.getType());
		assertEquals(JID.jidInstance(buddyJid), presence.getStanzaTo());
		assertEquals("xa", presence.getElemCDataStaticStr(new String[] { "presence", "show"}));
	}

	private void updateRoster(XMPPResourceConnection session, JID buddy, RosterAbstract.SubscriptionType subscriptionType)
			throws TigaseDBException, NotAuthorizedException, PolicyViolationException, NoConnectionIdException {
		rosterUtil.addBuddy(session, buddy, null, null, null);
		rosterUtil.setBuddySubscription(session, subscriptionType, buddy);
		Element new_buddy = rosterUtil.getBuddyItem(session, buddy);
		rosterUtil.updateBuddyChange(session, new ArrayDeque<>(), new_buddy);
		try {
			Thread.sleep(1000);
		} catch (Throwable ex) {}
	}
	
	protected void registerLocalBeans(Kernel kernel) {
		super.registerBeans(kernel);

		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean("sess-man").asInstance(this.getSessionManagerHandler()).setActive(true).exportable().exec();//.asClass(DummySessionManager.class).setActive(true).exportable().exec();
		kernel.registerBean(MessageDeliveryLogic.class).setActive(true).exportable().exec();
		kernel.registerBean("msgRepository").asClass(PushNotificationsTest.MsgRepositoryIfcImpl.class).exportable().exec();
		kernel.registerBean(PushNotifications.class).setActive(true).exec();
		kernel.registerBean("writer").asInstance(new TestPacketWriter()).exec();
		kernel.registerBean(PushPresence.class).setActive(true).exec();
		kernel.registerBean(PresenceState.class).setActive(true).exec();
		System.out.println("registering local beans done.");
	}

	protected static class TestPacketWriter implements PacketWriter {

		private final ArrayDeque<Packet> queue = new ArrayDeque<>();

		public TestPacketWriter() {
			System.out.println("creating new instance " + this + "....");
		}

		@Override
		public synchronized void write(Collection<Packet> packets) {
			queue.addAll(packets);
		}

		@Override
		public synchronized void write(Packet packet) {
			queue.add(packet);
		}

		@Override
		public synchronized void write(Packet packet, AsyncCallback callback) {
			 queue.add(packet);
		}

		public synchronized ArrayDeque<Packet> getQueue() {
			return queue;
		}
	}
}