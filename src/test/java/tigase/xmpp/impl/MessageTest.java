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
import java.util.Queue;
import java.util.UUID;

import org.junit.After;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;

/**
 *
 * @author andrzej
 */
public class MessageTest extends ProcessorTestCase {
	
	private Message messageProcessor;
	
	@Before
	@Override
	public void setUp() throws Exception {
		messageProcessor = new Message();
		messageProcessor.init(new HashMap<String,Object>());
		super.setUp();
	}
	
	@After
	@Override
	public void tearDown() throws Exception {
		messageProcessor = null;
		super.tearDown();
	}	

	@Test
	public void testSilentlyIgnoringMessages() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");

		// testing default behaviour - error message
		Element packetEl = new Element("message", new String[] { "from", "to" },
				new String[] { "remote-user@test.com/res1", res1.toString() });
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, null, null, results, null);
		assertTrue("no error was generated", !results.isEmpty());
		assertTrue("generated result is not an error", results.poll().getType().equals( StanzaType.error));


		// testing silently ignoring error responses
		results.clear();
		final HashMap<String, Object> settings = new HashMap<String,Object>();
		settings.put( "silently-ignore-message", "true");
		messageProcessor.init(settings);

		messageProcessor.process(packet, null, null, results, null);
		assertTrue("result was generated", results.isEmpty());
	}
	
	@Test
	public void testResourceSelectionForMessageDeliveryMethods() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(res1, res1);
		XMPPResourceConnection session2 = getSession(res2, res2);
		
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		assertEquals(Collections.emptyList(), messageProcessor.getConnectionsForMessageDelivery(session1));
		
		assertFalse("found XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		session1.setPriority(1);
		assertFalse("found XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		
		session1.setPresence(new Element("presence"));
		assertTrue("could not find XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		
		assertEquals(Arrays.asList(session1), messageProcessor.getConnectionsForMessageDelivery(session2));
		
		session2.setPresence(new Element("presence"));
		assertTrue("could not find XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		assertEquals(Arrays.asList(session1, session2), messageProcessor.getConnectionsForMessageDelivery(session2));
	}
	
	@Test
	public void testResourceSelectionForMessageDeliveryForBareJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		assertEquals(Collections.emptyList(), messageProcessor.getConnectionsForMessageDelivery(session1));
		
		Element packetEl = new Element("message", new String[] { "type", "from", "to" },
				new String[] { "chat", "remote-user@test.com/res1", userJid.toString() });
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session2, null, results, null);
		assertTrue("generated result even than no resource had nonnegative priority", results.isEmpty());
		
		session1.setPriority(1);
		results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session2, null, results, null);
		assertTrue("generated result even than no resource had nonnegative priority", results.isEmpty());
		
		session1.setPresence(new Element("presence"));
		assertTrue("could not find XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session2, null, results, null);		
		assertEquals("not generated result even than 1 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session1.getConnectionId()), collectPacketTo(results));
		
		session2.setPresence(new Element("presence"));
		assertTrue("could not find XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session1, null, results, null);		
		assertEquals("not generated result even than 2 resource had nonnegative priority", 2, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session1.getConnectionId(), session2.getConnectionId()), 
				collectPacketTo(results));		
	}	
	
	@Test
	public void testResourceSelectionForMessageDeliveryForFullJid() throws Exception {
		BareJID userJid = BareJID.bareJIDInstance("user1@example.com");
		JID res1 = JID.jidInstance(userJid, "res1");
		JID res2 = JID.jidInstance(userJid, "res2");
		XMPPResourceConnection session1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res1);
		XMPPResourceConnection session2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), res2);
		
		assertEquals(Arrays.asList(session1, session2), session1.getActiveSessions());
		assertEquals(Collections.emptyList(), messageProcessor.getConnectionsForMessageDelivery(session1));
		
		Element packetEl = new Element("message", new String[] { "type", "from", "to" },
				new String[] { "chat", "remote-user@test.com/res1", res1.toString() });
		Packet packet = Packet.packetInstance(packetEl);
		Queue<Packet> results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session2, null, results, null);
		assertEquals("not generated result even than no resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session1.getConnectionId()), collectPacketTo(results));
		
		session1.setPriority(1);
		results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session2, null, results, null);
		assertEquals("not generated result even than no resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session1.getConnectionId()), collectPacketTo(results));
		
		session1.setPresence(new Element("presence"));
		assertTrue("could not find XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session2, null, results, null);		
		assertEquals("not generated result even than 1 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session1.getConnectionId()), collectPacketTo(results));
		
		session2.setPresence(new Element("presence"));
		assertTrue("could not find XMPPResourceConnection for delivery of message", 
				messageProcessor.hasConnectionForMessageDelivery(session1));
		results = new ArrayDeque<Packet>();
		messageProcessor.process(packet, session1, null, results, null);		
		assertEquals("not generated result even than 2 resource had nonnegative priority", 1, results.size());
		assertEquals("packet sent to wrong jids", Arrays.asList(session1.getConnectionId()), collectPacketTo(results));
	}	

	
	protected List<JID> collectPacketTo(Queue<Packet> packets) {
		List<JID> result = new ArrayList<JID>();
		Packet p;
		while ((p = packets.poll()) != null) {
			result.add(p.getPacketTo());
		}
		return result;
	}
}
