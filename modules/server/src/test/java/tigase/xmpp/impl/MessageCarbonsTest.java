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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

/**
 *
 * @author andrzej
 */
public class MessageCarbonsTest extends ProcessorTestCase {

	private MessageCarbons carbonsProcessor;
	
	@Before
	@Override
	public void setUp() throws Exception {
		carbonsProcessor = new MessageCarbons();
		carbonsProcessor.init(new HashMap<String,Object>());
		super.setUp();
	}
	
	@After
	@Override
	public void tearDown() throws Exception {
		carbonsProcessor = null;
		super.tearDown();
	}		
	
	@Test
	public void testResourceSelectionForMessageDeliveryForBareJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		session2.putSessionData(MessageCarbons.XMLNS + "-enabled", true);
		Map<JID,Boolean> enabled = new HashMap<>();
		enabled.put(res2, true);
		session2.putCommonSessionData(MessageCarbons.XMLNS + "-resources", enabled);
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		
		Element packetEl = new Element("message", new String[] { "type", "from", "to" },
				new String[] { "chat", "remote-user@test.com/res1", userJid.toString() });
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
		assertEquals("packet sent to wrong jids", Arrays.asList(), 
				collectStanzaTo(results));		
	}	
	
	@Test
	public void testResourceSelectionForMessageDeliveryForFullJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		session2.putSessionData(MessageCarbons.XMLNS + "-enabled", true);
		Map<JID,Boolean> enabled = new HashMap<>();
		enabled.put(res2, true);
		session2.putCommonSessionData(MessageCarbons.XMLNS + "-resources", enabled);
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		
		Element packetEl = new Element("message", new String[] { "type", "from", "to" },
				new String[] { "chat", "remote-user@test.com/res1", res1.toString() });
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
		packet1.getElement().addChild(new Element("no-copy", new String[] { "xmlns" }, new String[] { "urn:xmpp:hints" }));
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
