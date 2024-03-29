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
package tigase.xmpp.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.core.Kernel;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author andrzej
 */
public class BlockingCommandTest
		extends ProcessorTestCase {

	private BlockingCommand blockingCommand;
	private JabberIqPrivacy privacy;
	private ArrayDeque<Packet> results;
	private RosterAbstract roster_util;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		getInstance(RosterFactory.Bean.class);
		blockingCommand = getInstance(BlockingCommand.class);
		privacy = getInstance(JabberIqPrivacy.class);
		roster_util = RosterFactory.getRosterImplementation(true);
		results = new ArrayDeque<>();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		roster_util.setEventBus(null);
		blockingCommand = null;
	}

	@Test
	public void testEmptyJID() throws TigaseStringprepException, XMPPException {
		JID connJid = JID.jidInstanceNS("c2s@example.com/test-111");
		JID userJid = JID.jidInstanceNS("user-1@example.com/res-1");
		Element iq = new Element("iq", new String[]{"type"}, new String[]{"set"});
		Element block = new Element("block", new String[]{"xmlns"}, new String[]{BlockingCommand.XMLNS});
		Element item = new Element("item", new String[]{"jid"}, new String[]{""});
		block.addChild(item);
		iq.addChild(block);

		Packet p = Packet.packetInstance(iq, userJid, userJid);

		XMPPResourceConnection sess = getSession(connJid, userJid);
		blockingCommand.process(p, sess, null, results, null);
		assertEquals(1, results.size());
		Packet result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.error, result.getType());

	}
	@Test
	public void testBlockUnblock() throws Exception {
		JID connJid = JID.jidInstanceNS("c2s@example.com/test-111");
		JID userJid = JID.jidInstanceNS("user-1@example.com/res-1");
		XMPPResourceConnection sess = getSession(connJid, userJid);

		String blockJid = "block-1@example.com";

		checkPrivacyJidBlocked(sess, blockJid, false);
		List<String> blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());

		block(sess, blockJid);
		assertEquals(2, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(2, results.size());
		Packet result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid, true);
		blocked = getBlocked(sess);
		assertTrue(blocked.contains(blockJid));

		unblock(sess, blockJid);
		assertEquals(2, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(2, results.size());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid, false);
		blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());
	}

	@Test
	public void testBlockUnblockAll() throws Exception {
		JID connJid = JID.jidInstanceNS("c2s@example.com/test-111");
		JID userJid = JID.jidInstanceNS("user-1@example.com/res-1");
		XMPPResourceConnection sess = getSession(connJid, userJid);

		String blockJid = "block-1@example.com";

		checkPrivacyJidBlocked(sess, blockJid, false);
		List<String> blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());

		block(sess, blockJid);
		assertEquals(2, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(2, results.size());
		Packet result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid, true);
		blocked = getBlocked(sess);
		assertTrue(blocked.contains(blockJid));

		unblock(sess, blockJid);
		assertEquals(2, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(2, results.size());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid, false);
		blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());
	}

	@Test
	public void testBlockUnblockWithPresence() throws Exception {
		JID connJid = JID.jidInstanceNS("c2s@example.com/test-111");
		JID userJid = JID.jidInstanceNS("user-1@example.com/res-1");
		XMPPResourceConnection sess = getSession(connJid, userJid);

		String blockJid = "block-1@example.com";
		roster_util.addBuddy(sess, JID.jidInstance(blockJid), "Block-1", null, null, null);
		roster_util.setBuddySubscription(sess, RosterAbstract.SubscriptionType.both, JID.jidInstance(blockJid));

		checkPrivacyJidBlocked(sess, blockJid, false);
		List<String> blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());

		block(sess, blockJid);
		assertEquals(3, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(3, results.size());
		Packet result = results.poll();
		assertNotNull(result);
		assertEquals(tigase.server.Presence.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.unavailable, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid, true);
		blocked = getBlocked(sess);
		assertTrue(blocked.contains(blockJid));

		unblock(sess, blockJid);
		assertEquals(3, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(3, results.size());
		result = results.poll();
		assertNotNull(result);
		assertEquals(tigase.server.Presence.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.probe, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid, false);
		blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());
	}

	@Test
	public void testBlockUnblockDomainWithPresence() throws Exception {
		JID connJid = JID.jidInstanceNS("c2s@example.com/test-111");
		JID userJid = JID.jidInstanceNS("user-1@example.com/res-1");
		XMPPResourceConnection sess = getSession(connJid, userJid);

		BareJID blockJid = BareJID.bareJIDInstanceNS( "block-1@example.com");
		roster_util.addBuddy(sess, JID.jidInstance(blockJid), "Block-1", null, null, null);
		roster_util.setBuddySubscription(sess, RosterAbstract.SubscriptionType.both, JID.jidInstance(blockJid));

		checkPrivacyJidBlocked(sess, blockJid.getDomain(), false);
		List<String> blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());

		block(sess, blockJid.getDomain());
		assertEquals(3, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(3, results.size());
		Packet result = results.poll();
		assertNotNull(result);
		assertEquals(tigase.server.Presence.ELEM_NAME, result.getElemName());
		assertEquals(JID.jidInstanceNS(blockJid), result.getStanzaTo());
		assertEquals(StanzaType.unavailable, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid.getDomain(), true);
		blocked = getBlocked(sess);
		assertTrue(blocked.contains(blockJid.getDomain()));

		unblock(sess, blockJid.getDomain());
		assertEquals(3, results.size());
		privacy.filter(null, sess, null, results);

		assertEquals(3, results.size());
		result = results.poll();
		assertNotNull(result);
		assertEquals(tigase.server.Presence.ELEM_NAME, result.getElemName());
		assertEquals(JID.jidInstanceNS(blockJid), result.getStanzaTo());
		assertEquals(StanzaType.probe, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.result, result.getType());
		result = results.poll();
		assertNotNull(result);
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		assertEquals(StanzaType.set, result.getType());

		checkPrivacyJidBlocked(sess, blockJid.getDomain(), false);
		blocked = getBlocked(sess);
		assertTrue(blocked == null || blocked.isEmpty());
	}


	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean(RosterFactory.Bean.class).setActive(true).exec();
		kernel.registerBean(JabberIqPrivacy.class).setActive(true).exec();
		kernel.registerBean(BlockingCommand.class).setActive(true).exec();
	}

	private void block(XMPPResourceConnection sess, String jid) throws Exception {
		Element iq = new Element("iq", new String[]{"type"}, new String[]{"set"});
		Element block = new Element("block", new String[]{"xmlns"}, new String[]{BlockingCommand.XMLNS});
		Element item = new Element("item", new String[]{"jid"}, new String[]{jid});
		block.addChild(item);
		iq.addChild(block);
		Packet p = Packet.packetInstance(iq);
		blockingCommand.process(p, sess, null, results, null);
	}

	private void unblock(XMPPResourceConnection sess, String jid) throws Exception {
		Element iq = new Element("iq", new String[]{"type"}, new String[]{"set"});
		Element block = new Element("unblock", new String[]{"xmlns"}, new String[]{BlockingCommand.XMLNS});
		Element item = new Element("item", new String[]{"jid"}, new String[]{jid});
		block.addChild(item);
		iq.addChild(block);
		Packet p = Packet.packetInstance(iq);
		blockingCommand.process(p, sess, null, results, null);
	}

	private void unblockAll(XMPPResourceConnection sess) throws Exception {
		Element iq = new Element("iq", new String[]{"type"}, new String[]{"set"});
		Element block = new Element("unblock", new String[]{"xmlns"}, new String[]{BlockingCommand.XMLNS});
		iq.addChild(block);
		Packet p = Packet.packetInstance(iq);
		blockingCommand.process(p, sess, null, results, null);
	}

	private void checkPrivacyJidBlocked(XMPPResourceConnection sess, String jid, boolean value)
			throws NotAuthorizedException, TigaseDBException {
		List<String> blocked = Privacy.getBlocked(sess);
		if (value) {
			assertTrue(blocked != null && blocked.contains(jid));
		} else {
			assertTrue(blocked == null || !blocked.contains(jid));
		}
	}

	private List<String> getBlocked(XMPPResourceConnection sess) throws XMPPException, TigaseStringprepException {
		Element iq = new Element("iq", new String[]{"type"}, new String[]{"get"});
		Element blocklist = new Element("blocklist", new String[]{"xmlns"}, new String[]{BlockingCommand.XMLNS});
		iq.addChild(blocklist);

		Packet p = Packet.packetInstance(iq);
		blockingCommand.process(p, sess, null, results, null);
		assertEquals(1, results.size());
		Packet result = results.poll();
		assertEquals(Iq.ELEM_NAME, result.getElemName());
		return result.getElement()
				.getChild("blocklist")
				.mapChildren(c -> c.getName() == "item", c -> c.getAttributeStaticStr("jid"));
	}
}
