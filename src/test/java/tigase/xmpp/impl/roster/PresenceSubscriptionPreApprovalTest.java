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
package tigase.xmpp.impl.roster;

import org.junit.Before;
import org.junit.Test;
import tigase.db.TigaseDBException;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.PolicyViolationException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.DummyVHostManager;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.PresenceSubscription;
import tigase.xmpp.impl.ProcessorTestCase;
import tigase.xmpp.jid.JID;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PresenceSubscriptionPreApprovalTest
		extends ProcessorTestCase {

	private JID connID;
	private JID userJID;
	private JID buddyJID;
	private XMPPResourceConnection session;
	private PresenceSubscription presenceSubscription;
	private RosterAbstract rosterUtil;
	
	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		//kernel.registerBean("sess-man").asClass(DummySessionManager.class).setActive(true).exportable().exec();
		kernel.registerBean(PresenceSubscription.class).exec();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		Field f = RosterFactory.class.getDeclaredField("shared");
		f.setAccessible(true);
		f.set(null, RosterFactory.newRosterInstance(RosterFactory.ROSTER_IMPL_PROP_VAL));
		super.setUp();
		registerBeans(getKernel());
		var vHostManager = getInstance(DummyVHostManager.class);
		vHostManager.addVhost("localhost");
		presenceSubscription = getInstance(PresenceSubscription.class);
		rosterUtil = RosterFactory.getRosterImplementation(true);
		connID = JID.jidInstanceNS(UUID.randomUUID().toString(), "localhost");
		userJID = JID.jidInstanceNS(UUID.randomUUID().toString(), "localhost");
		buddyJID = JID.jidInstanceNS(UUID.randomUUID().toString(), "localhost");
		session = getSession(connID, userJID, true);
	}
	
	@Test
	public void test_nonePreApproval_noRosterItem() throws XMPPException, TigaseStringprepException, TigaseDBException {
		Queue<Packet> results = processPresence(userJID, buddyJID, StanzaType.subscribed);
		System.out.println(results);

		assertTrue(rosterUtil.isPreApproved(session, buddyJID));
		assertEquals(RosterAbstract.SubscriptionType.none_pre_approved, getSubscription(buddyJID));
	}

	@Test
	public void test_nonePreApproval_withRosterItem()
			throws XMPPException, TigaseStringprepException, TigaseDBException, PolicyViolationException {
		rosterUtil.addBuddy(session, buddyJID, "test", null, RosterAbstract.SubscriptionType.none, null);
		Queue<Packet> results = processPresence(userJID, buddyJID, StanzaType.subscribed);
		System.out.println(results);
		assertTrue(rosterUtil.isPreApproved(session, buddyJID));
		assertEquals(RosterAbstract.SubscriptionType.none_pre_approved, getSubscription(buddyJID));
	}

	@Test
	public void test_subscribingBothPreapproval() throws XMPPException, TigaseStringprepException, TigaseDBException {
		Queue<Packet> results = processPresence(userJID, buddyJID, StanzaType.subscribe);
		assertEquals(StanzaType.subscribe, results.poll().getType());

		results = processPresence(userJID, buddyJID, StanzaType.subscribed);
		assertTrue(rosterUtil.isPreApproved(session, buddyJID));
		assertEquals(RosterAbstract.SubscriptionType.none_pending_out_pre_approved, getSubscription(buddyJID));

		results = processPresence(buddyJID, userJID, StanzaType.subscribed);
		assertEquals(RosterAbstract.SubscriptionType.to_pre_approved, getSubscription(buddyJID));

		results = processPresence(buddyJID, userJID, StanzaType.subscribe);
		assertEquals(RosterAbstract.SubscriptionType.both, getSubscription(buddyJID));
	}

	@Test
	public void test_subscribingBothPreapproval2() throws XMPPException, TigaseStringprepException, TigaseDBException {
		Queue<Packet> results = processPresence(userJID, buddyJID, StanzaType.subscribe);
		assertEquals(StanzaType.subscribe, results.poll().getType());

		results = processPresence(userJID, buddyJID, StanzaType.subscribed);
		assertTrue(rosterUtil.isPreApproved(session, buddyJID));
		assertEquals(RosterAbstract.SubscriptionType.none_pending_out_pre_approved, getSubscription(buddyJID));

		results = processPresence(buddyJID, userJID, StanzaType.subscribe);
		assertEquals(RosterAbstract.SubscriptionType.from_pending_out, getSubscription(buddyJID));

		results = processPresence(buddyJID, userJID, StanzaType.subscribed);
		assertEquals(RosterAbstract.SubscriptionType.both, getSubscription(buddyJID));
	}

	private RosterAbstract.SubscriptionType getSubscription(JID jid)
			throws TigaseDBException, NotAuthorizedException {
		return rosterUtil.getBuddySubscription(session, jid);
	}

	private Queue<Packet> processPresence(JID from, JID to, StanzaType stanzaType)
			throws XMPPException {
		Packet packet = Packet.packetInstance(new Element("presence", new String[] {"type"}, new String[]{stanzaType.name()}), from, to);
		Queue<Packet> results = new ArrayDeque<>();
		presenceSubscription.process(packet, session, null, results, new HashMap<>());
		return results;
	}

}
