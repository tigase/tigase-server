/*
 * MobileV3Test.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
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
 *
 */

package tigase.xmpp.impl;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import org.junit.After;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

/**
 *
 * @author andrzej
 */
public class MobileV3Test extends ProcessorTestCase {
	
	private MobileV3 mobileV3;
	private SessionManagerHandler loginHandler;
	
	@Before
	@Override
	public void setUp() throws Exception {
		mobileV3 = new MobileV3();
		mobileV3.init(new HashMap<String,Object>());
		super.setUp();
	}
	
	@After
	@Override
	public void tearDown() throws Exception {
		mobileV3 = null;
		super.tearDown();
	}
	
	@Test
	public void testRecipientDisabledFor2ResourcesMessage() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		Packet p = Packet.packetInstance("message", "sender-1@localhost/res1", recp1.toString(), StanzaType.chat);
		p.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(p);
		Packet[] expected = results.toArray(new Packet[0]);
		mobileV3.filter(p, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
	}
	
	@Test
	public void testRecipientEnabledFor2ResourcesMessage() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		
		enableMobileV3(session1, recp1);
		
		Packet p = Packet.packetInstance("message", "sender-1@localhost/res1", recp1.toString(), StanzaType.chat);
		p.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(p);
		Packet[] expected = results.toArray(new Packet[0]);
		mobileV3.filter(p, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
	}

	@Test
	public void testRecipientDisabledFor2ResourcesPresence() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		Packet p = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.chat);
		p.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(p);
		Packet[] expected = results.toArray(new Packet[0]);
		mobileV3.filter(p, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
	}

	@Test
	public void testRecipientEnabledFor2ResourcesPresence() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		
		enableMobileV3(session1, recp1);
		
		Packet presence = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.available);
		presence.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(presence);
		Packet[] expected = new Packet[0];// results.toArray(new Packet[0]);
		mobileV3.filter(presence, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
	}

	@Test
	public void testRecipientEnabledFor2Resources2Presences() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		
		enableMobileV3(session1, recp1);
		
		Packet presence = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.available);
		presence.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(presence);
		Packet[] expected = new Packet[0];// results.toArray(new Packet[0]);
		mobileV3.filter(presence, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
		
		presence = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.available);
		presence.setPacketTo(connId1);
		results = new ArrayDeque<Packet>();
		results.offer(presence);
		expected = new Packet[0];// results.toArray(new Packet[0]);
		mobileV3.filter(presence, session1, null, results);
		processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
	
		results.clear();
		Packet p = Packet.packetInstance("message", "sender-1@localhost/res1", recp1.toString(), StanzaType.chat);
		p.setPacketTo(connId1);
		results.offer(p);
		Packet p1 = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.error);
		p1.setPacketTo(connId1);		
		results.offer(p1);
		expected = new Packet[] { presence, p, p1 };
		mobileV3.filter(p, session1, null, results);
		processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);		
	}	
	
	
	@Test
	public void testRecipientEnabledFor2Resources() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		
		enableMobileV3(session1, recp1);
		
		Packet presence = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.available);
		presence.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(presence);
		Packet[] expected = new Packet[0];// results.toArray(new Packet[0]);
		mobileV3.filter(presence, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
		
		results.clear();
		Packet p = Packet.packetInstance("message", "sender-1@localhost/res1", recp1.toString(), StanzaType.chat);
		p.setPacketTo(connId1);
		results.offer(p);
		Packet p1 = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.error);
		p1.setPacketTo(connId1);		
		results.offer(p1);
		expected = new Packet[] { presence, p, p1 };
		mobileV3.filter(p, session1, null, results);
		processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);		
	}	
	
	@Test
	public void testRecipientEnabledFor2ResourcesMixed() throws TigaseStringprepException, NotAuthorizedException {
		String recipient = "recipient-1@localhost";
		JID recp1 = JID.jidInstanceNS(recipient + "/res1");
		JID recp2 = JID.jidInstanceNS(recipient + "/res2");
		JID connId1 = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
		JID connId2 = JID.jidInstanceNS("c2s@localhost/recipient1-res2");
		XMPPResourceConnection session1 = getSession(connId1, recp1);
		getSession(connId2, recp2);
		
		enableMobileV3(session1, recp1);
		
		Packet presence = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.available);
		presence.setPacketTo(connId1);
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		results.offer(presence);
		Packet[] expected = new Packet[0];// results.toArray(new Packet[0]);
		mobileV3.filter(presence, session1, null, results);
		Packet[] processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);

		results.clear();
		Packet m1 = Packet.packetInstance("message", recp2.toString(), recp1.toString(), StanzaType.chat);
		Element receivedEl = new Element("received", new String[] { "xmlns" }, new String[] { "urn:xmpp:carbons:2" });
		Element forwardedEl = new Element("forwarded", new String[] {"xmlns" }, new String[] { "urn:xmpp:forward:0" });
		forwardedEl.addChild(new Element("message", new String[] { "from", "to" }, new String[] { recp2.toString(), "sender-1@localhost/res1" }));
		receivedEl.addChild(forwardedEl);
		m1.getElement().addChild(receivedEl);
		m1.setPacketTo(connId1);
		results.offer(m1);
		expected = new Packet[0];
		mobileV3.filter(m1, session1, null, results);
		processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);			
		
		results.clear();
		presence = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.available);
		presence.setPacketTo(connId1);
		results.offer(presence);
		expected = new Packet[0];// results.toArray(new Packet[0]);
		mobileV3.filter(presence, session1, null, results);
		processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);
	
		results.clear();
		Packet p = Packet.packetInstance("message", "sender-1@localhost/res1", recp1.toString(), StanzaType.chat);
		p.setPacketTo(connId1);
		results.offer(p);
		Packet p1 = Packet.packetInstance("presence", "sender-1@localhost/res1", recp1.toString(), StanzaType.error);
		p1.setPacketTo(connId1);		
		results.offer(p1);
		expected = new Packet[] { presence, m1, p, p1 };
		mobileV3.filter(p, session1, null, results);
		processed = results.toArray(new Packet[0]);
		Assert.assertArrayEquals(expected, processed);		
	}	

	private Queue<Packet> enableMobileV3(XMPPResourceConnection session, JID userJid) throws TigaseStringprepException {
		Packet p = Packet.packetInstance("iq", userJid.toString(), userJid.toString(), StanzaType.set);
		p.getElement().addChild(new Element("mobile", new String[] { "xmlns", "enable" }, 
				new String[] { "http://tigase.org/protocol/mobile#v3", "true" }));
		ArrayDeque<Packet> results = new ArrayDeque<Packet>();
		mobileV3.process(p, session, null, results, null);
		return results;
	}
	
}
