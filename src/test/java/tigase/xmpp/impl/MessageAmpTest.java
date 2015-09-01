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
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.*;

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
public class MessageAmpTest extends ProcessorTestCase {
	
	private static final String XMLNS = "http://jabber.org/protocol/amp";

	private JID ampJid;
	private MessageAmp messageAmp;
	
	@Before
	@Override
	public void setUp() throws Exception {
		messageAmp = new MessageAmp();
		Map<String,Object> settings = new HashMap<String,Object>();
		ampJid = JID.jidInstance("amp@example1.com");
		settings.put("amp-jid", ampJid.toString());
		messageAmp.init(settings);
		super.setUp();
	}	
	
	@After
	@Override
	public void tearDown() throws Exception {
		messageAmp = null;
		super.tearDown();
	}		
	
	@Test
	public void testMessageProcessingWithAmp() throws Exception {
		JID senderJid = JID.jidInstance("sender@example.com/res-1");
		JID recipientJid = JID.jidInstance("recipient@example.com/res-2");
		XMPPResourceConnection senderSession = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), senderJid);
		XMPPResourceConnection recipientSession = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), recipientJid);
		
		Element messageEl = new Element("message", 
				new String[] { "from", "to", "type" }, 
				new String[] { senderJid.toString(), recipientJid.toString(), "chat" });
		messageEl.addChild(new Element("body", "Test 123"));
		Element ampEl = new Element("amp", new String[] { "xmlns" }, new String[] { XMLNS });
		ampEl.addChild(new Element("rule", new String[] { "condition", "action", "value" }, 
				new String[] { "expire-at", "drop", "2024-01-01T00:00:00Z" }));
		messageEl.addChild(ampEl);
		Packet message = Packet.packetInstance(messageEl);
		message.setPacketFrom(senderSession.getConnectionId());
		
		Map<String,Object> settings = new HashMap<>();
		Queue<Packet> results = new ArrayDeque<>();
		assertFalse(messageAmp.preProcess(message, senderSession, null, results, settings));
		assertEquals(0, results.size());
		
		messageAmp.process(message, senderSession, null, results, settings);
		assertEquals(1, results.size());
		message = results.poll();
		assertEquals(senderSession.getConnectionId().toString(), message.getAttributeStaticStr("from-conn-id"));
		assertEquals(ampJid, message.getPacketTo());
		
		message.getElement().removeAttribute("from-conn-id");
		message.setPacketTo(null);
		assertFalse(messageAmp.preProcess(message, senderSession, null, results, settings));
		assertEquals(0, results.size());		
		
		assertTrue(messageAmp.preProcess(message, recipientSession, null, results, settings));
		assertEquals(1, results.size());	
		message = results.poll();
		assertEquals(null, message.getAttributeStaticStr("from-conn-id"));
		assertEquals(ampJid, message.getPacketTo());
		
		message.setPacketFrom(ampJid);
		message.setPacketTo(null);
		assertFalse(messageAmp.preProcess(message, recipientSession, null, results, settings));
		assertEquals(0, results.size());		
		
		messageAmp.process(message, recipientSession, null, results, settings);
		assertEquals(1, results.size());
		message = results.poll();
		assertEquals(null, message.getAttributeStaticStr("from-conn-id"));
		assertEquals(recipientSession.getConnectionId(), message.getPacketTo());
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
		messageAmp.process(packet, null, null, results, null);
		assertTrue("no error was generated", !results.isEmpty());
		assertTrue("generated result is not an error", results.poll().getType().equals( StanzaType.error));


		// testing silently ignoring error responses
		results.clear();
		final HashMap<String, Object> settings = new HashMap<String,Object>();
		settings.put( "silently-ignore-message", "true");
		messageAmp.init(settings);

		messageAmp.process(packet, null, null, results, null);
		assertTrue("result was generated", results.isEmpty());


	}
	@Test
	public void testMessageProcessingWithoutAmp() throws Exception {
		JID senderJid = JID.jidInstance("sender@example.com/res-1");
		JID recipientJid = JID.jidInstance("recipient@example.com/res-2");
		XMPPResourceConnection senderSession = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), senderJid);
		XMPPResourceConnection recipientSession = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), recipientJid);
		
		Element messageEl = new Element("message", 
				new String[] { "from", "to", "type" }, 
				new String[] { senderJid.toString(), recipientJid.toString(), "chat" });
		messageEl.addChild(new Element("body", "Test 123"));		
		
		Packet message = Packet.packetInstance(messageEl);
		message.setPacketFrom(senderSession.getConnectionId());
		
		Map<String,Object> settings = new HashMap<>();
		Queue<Packet> results = new ArrayDeque<>();
		assertFalse(messageAmp.preProcess(message, senderSession, null, results, settings));
		assertEquals(0, results.size());
		
		messageAmp.process(message, senderSession, null, results, settings);
		assertEquals(1, results.size());
		message = results.poll();
		assertEquals(null, message.getAttributeStaticStr("from-conn-id"));
		assertEquals(null, message.getPacketFrom());
		assertEquals(null, message.getPacketTo());

		assertFalse(messageAmp.preProcess(message, recipientSession, null, results, settings));
		assertEquals(0, results.size());		
		
		messageAmp.process(message, recipientSession, null, results, settings);
		assertEquals(1, results.size());
		message = results.poll();
		assertEquals(null, message.getAttributeStaticStr("from-conn-id"));
		assertEquals(recipientSession.getConnectionId(), message.getPacketTo());
	}
}
