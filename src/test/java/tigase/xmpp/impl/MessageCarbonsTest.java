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

import org.junit.Before;
import org.junit.Test;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author andrzej
 */
public class MessageCarbonsTest
		extends ProcessorTestCase {

	private MessageCarbons carbonsProcessor;

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean(MessageDeliveryLogic.class).exec();
		kernel.registerBean(MessageCarbons.class).setActive(true).exec();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		carbonsProcessor = getInstance(MessageCarbons.class);
	}

	@Test
	public void testDisco() throws Exception {
		assertTrue(Arrays.stream(carbonsProcessor.supDiscoFeatures(null))
						   .anyMatch(el -> "urn:xmpp:carbons:2".equals(el.getAttributeStaticStr("var"))));
		assertTrue(Arrays.stream(carbonsProcessor.supDiscoFeatures(null))
						   .anyMatch(el -> "urn:xmpp:carbons:rules:0".equals(el.getAttributeStaticStr("var"))));
	}

	@Test
	public void testDeliveryOfMessageWithBody() throws Exception {
		Packet packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body") }, new String[] { "type"}, new String[] {"chat"}));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
		packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body") }, new String[] { "type"}, new String[] {"normal"}));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
		packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body") }, null, null));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
		packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body") }, new String[] { "type"}, new String[] {"groupchat"}));
		assertFalse(carbonsProcessor.shouldSendCarbons(packet, null));
	}

	@Test
	public void testDeliveryOfMessageWithIMPayload() throws Exception {
		Packet packet = Packet.packetInstance(new Element("message"));
		assertFalse(carbonsProcessor.shouldSendCarbons(packet, null));

		packet.getElement().setChildren(List.of(new Element("received", new String[]{"xmlns"}, new String[] {"urn:xmpp:receipts"})));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
		packet.getElement().setChildren(List.of(new Element("active", new String[]{"xmlns"}, new String[] {"http://jabber.org/protocol/chatstates"})));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
		packet.getElement().setChildren(List.of(new Element("received", new String[]{"xmlns"}, new String[] {"urn:xmpp:chat-markers:0"})));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
	}

	@Test
	public void testDeliveryOfMucMessage_Groupchat() throws Exception {
		Packet packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body") }, new String[] { "type"}, new String[] {"groupchat"}));
		assertFalse(carbonsProcessor.shouldSendCarbons(packet, null));
	}

	@Test
	public void testDeliveryOfMucMessage_DirectInvitation() throws Exception {
		Packet packet = Packet.packetInstance(new Element("message"));
		packet.getElement()
				.setChildren(List.of(new Element("x", new Element[]{new Element("invite")}, new String[]{"xmlns"},
												 new String[]{"http://jabber.org/protocol/muc#user"})));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
	}

	@Test
	public void testDeliveryOfMucMessage_SentPM() throws Exception {
		JID sender = JID.jidInstanceNS("user@domain.com/res-1");
		JID recipient = JID.jidInstanceNS("room@muc.domain.com/Julia");
		XMPPResourceConnection session = getSession(sender, sender);
		Packet packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body"), new Element("x", new String[]{"xmlns"}, new String[]{"http://jabber.org/protocol/muc#user"}) }, new String[] { "type"}, new String[] {"chat"}), sender, recipient);
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, session));
	}

	@Test
	public void testDeliveryOfMucMessage_ReceivedPM() throws Exception {
		JID recipient = JID.jidInstanceNS("user@domain.com/res-1");
		JID sender = JID.jidInstanceNS("room@muc.domain.com/Julia");
		XMPPResourceConnection session = getSession(recipient, recipient);
		Packet packet = Packet.packetInstance(new Element("message", new Element[] { new Element("body"), new Element("x", new String[]{"xmlns"}, new String[]{"http://jabber.org/protocol/muc#user"}) }, new String[] { "type"}, new String[] {"chat"}), sender, recipient);
		assertFalse(carbonsProcessor.shouldSendCarbons(packet, session));
	}

	@Test
	public void testDeliveryOfMucMessage_MediatedInvitation() throws Exception {
		Packet packet = Packet.packetInstance(new Element("message"));
		packet.getElement().setChildren(List.of(new Element("x", new String[]{"xmlns"}, new String[] {"jabber:x:conference"})));
		assertTrue(carbonsProcessor.shouldSendCarbons(packet, null));
	}

	@Test
	public void testResourceSelectionForMessageDeliveryForBareJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
													 res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
													 res2);

		session2.putSessionData(MessageCarbons.XMLNS + "-enabled", true);
		Map<JID, Boolean> enabled = new HashMap<>();
		enabled.put(res2, true);
		session2.putCommonSessionData(MessageCarbons.XMLNS + "-resources", enabled);
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());

		Element packetEl = new Element("message", new String[]{"type", "from", "to"},
									   new String[]{"chat", "remote-user@test.com/res1", userJid.toString()});
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session2, null, results, null);
		assertEquals("generated result even than no resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		session1.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session2, null, results, null);
		assertEquals("not generated result even than 1 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		session2.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("not generated result even than 2 resource had nonnegative priority", 0, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(), collectStanzaTo(results));
	}

	@Test
	public void testResourceSelectionForMessageDeliveryForFullJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
													 res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
													 res2);

		session2.putSessionData(MessageCarbons.XMLNS + "-enabled", true);
		Map<JID, Boolean> enabled = new HashMap<>();
		enabled.put(res2, true);
		session2.putCommonSessionData(MessageCarbons.XMLNS + "-resources", enabled);
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());

		Element packetEl = new Element("message", new String[]{"type", "from", "to"},
									   new String[]{"chat", "remote-user@test.com/res1", res1.toString()});
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("generated result even than no resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		session1.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("not generated result even than 1 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		session2.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("not generated result even than 2 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		results = new ArrayDeque<Packet>();
		Packet packet1 = packet.copyElementOnly();
		packet1.getElement().addChild(new Element("no-copy", new String[]{"xmlns"}, new String[]{"urn:xmpp:hints"}));
		carbonsProcessor.process(packet1, session1, null, results, null);
		assertEquals("generated result even that no-copy was sent", 0, results.size());
		assertEquals("packet sent to wrong jids", Collections.EMPTY_LIST, collectStanzaTo(results));

	}

	@Test
	public void testResourceSelectionForMessageDeliveryForFullJid_NotChat() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
													 res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
													 res2);

		session2.putSessionData(MessageCarbons.XMLNS + "-enabled", true);
		Map<JID, Boolean> enabled = new HashMap<>();
		enabled.put(res2, true);
		session2.putCommonSessionData(MessageCarbons.XMLNS + "-resources", enabled);
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());

		Element packetEl = new Element("message", new String[]{"from", "to"},
									   new String[]{"remote-user@test.com/res1", res1.toString()});
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("generated result even than no resource had nonnegative priority", 0, results.size());
		assertEquals("packet sent to wrong jids", Collections.emptyList(), collectStanzaTo(results));

		packetEl = new Element("message", new String[]{"from", "to"},
							   new String[]{"remote-user@test.com/res1", res1.toString()});
		packetEl.addChild(new Element("received", new String[] {"xmlns"}, new String[] {"urn:xmpp:receipts"}));
		packet = Packet.packetInstance(packetEl);
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("generated result even than no resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		session1.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("not generated result even than 1 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		session2.setPresence(new Element("presence"));
		results = new ArrayDeque<Packet>();
		carbonsProcessor.process(packet, session1, null, results, null);
		assertEquals("not generated result even than 2 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session2.getJID()), collectStanzaTo(results));

		results = new ArrayDeque<Packet>();
		Packet packet1 = packet.copyElementOnly();
		packet1.getElement().addChild(new Element("no-copy", new String[]{"xmlns"}, new String[]{"urn:xmpp:hints"}));
		carbonsProcessor.process(packet1, session1, null, results, null);
		assertEquals("generated result even that no-copy was sent", 0, results.size());
		assertEquals("packet sent to wrong jids", Collections.EMPTY_LIST, collectStanzaTo(results));

	}

	protected List<JID> collectStanzaTo(Queue<Packet> packets) {
		List<JID> result = new ArrayList<JID>();
		Packet p;
		while ((p = packets.poll()) != null) {
			result.add(p.getStanzaTo());
		}
		return result;
	}
}
