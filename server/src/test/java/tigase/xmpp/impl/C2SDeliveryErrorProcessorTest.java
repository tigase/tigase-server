/*
 * C2SDeliveryErrorProcessorTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

/**
 *
 * @author andrzej
 */
public class C2SDeliveryErrorProcessorTest extends ProcessorTestCase {

	private static final SimpleDateFormat formatter;
	
	static {
		formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}		
	
	private Message messageProcessor = new Message();
	
	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}	
	
	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	@Test
	public void test() throws Exception {
		Element packetEl = null;
		Packet packet = null;
		JID from = JID.jidInstance("from@example.com/res");
		JID to = JID.jidInstance("to@example.com");
		
		
		packetEl = new Element("iq", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packet = Packet.packetInstance(packetEl);

		Packet error = C2SDeliveryErrorProcessor.makeDeliveryError(packet, 12L);
		Element deliveryError = C2SDeliveryErrorProcessor.getDeliveryError(error);
		assertNotNull(deliveryError);
		assertEquals("12", deliveryError.getAttributeStaticStr("stamp"));
		
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packet = Packet.packetInstance(packetEl);
		
		error = C2SDeliveryErrorProcessor.makeDeliveryError(packet, 12L);
		deliveryError = C2SDeliveryErrorProcessor.getDeliveryError(error);
		assertNotNull(deliveryError);
		assertEquals("12", deliveryError.getAttributeStaticStr("stamp"));		
	}
	
	@Test
	public void testPreprocessingNotSupportedPackets() throws Exception {
		Element packetEl = null;
		Packet packet = null;
		JID from = JID.jidInstance("from@example.com/res");
		JID to = JID.jidInstance("to@example.com");
		
		
		packetEl = new Element("iq", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packet = Packet.packetInstance(packetEl);
		assertFalse(C2SDeliveryErrorProcessor.preProcess(packet, null, null, null, null, messageProcessor));

		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packet = Packet.packetInstance(packetEl);
		assertFalse(C2SDeliveryErrorProcessor.preProcess(packet, null, null, null, null, messageProcessor));	

		// packet is still not supported as session is null
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns" }, new String[] { "http://tigase.org/delivery-error" }));
		packet = Packet.packetInstance(packetEl);
		assertFalse(C2SDeliveryErrorProcessor.preProcess(packet, null, null, null, null, messageProcessor));		
	}	

	@Test
	public void testPreprocessingWithDeliveryErrorForBareJID() throws Exception {
		Element packetEl = null;
		Packet packet = null;
		JID from = JID.jidInstance("from@example.com/res");
		JID to = JID.jidInstance("to@example.com");
		JID toRes1 = to.copyWithResource("res1");
		JID toRes2 = to.copyWithResource("res2");
		
		XMPPResourceConnection sessionToRes2 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), toRes2);
		Thread.sleep(1);
		String stampBefore = String.valueOf(System.currentTimeMillis());
		Thread.sleep(1);
		XMPPResourceConnection sessionToRes1 = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), toRes1);
		Thread.sleep(1);
		String stampAfter = String.valueOf(System.currentTimeMillis());
		
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns" }, new String[] { "http://tigase.org/delivery-error" }));
		packet = Packet.packetInstance(packetEl);
		assertFalse(C2SDeliveryErrorProcessor.preProcess(packet, sessionToRes1, null, null, null, messageProcessor));
		
		sessionToRes1.setPriority(10);
		sessionToRes1.setPresence(new Element("presence"));
		
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns" }, new String[] { "http://tigase.org/delivery-error" }));
		packet = Packet.packetInstance(packetEl);
		assertTrue(C2SDeliveryErrorProcessor.preProcess(packet, sessionToRes1, null, null, null, messageProcessor));
		
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns" }, new String[] { "http://tigase.org/delivery-error" }));
		packetEl.addChild(new Element("delay", new String[] { "xmlns" }, new String[] { "urn:xmpp:delay" }));
		packet = Packet.packetInstance(packetEl);
		assertTrue(C2SDeliveryErrorProcessor.preProcess(packet, sessionToRes1, null, null, null, messageProcessor));

		Queue<Packet> results = new ArrayDeque();
		results.clear();
		
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns", "stamp" }, new String[] { "http://tigase.org/delivery-error", stampBefore }));
		packet = Packet.packetInstance(packetEl);
		assertTrue(C2SDeliveryErrorProcessor.preProcess(packet, sessionToRes1, null, results, null, messageProcessor));
		assertEquals(1, results.size());
		Packet result = results.poll();
		assertEquals(sessionToRes1.getConnectionId(), result.getPacketTo());

		sessionToRes2.setPriority(10);
		sessionToRes2.setPresence(new Element("presence"));
		results.clear();
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns", "stamp" }, new String[] { "http://tigase.org/delivery-error", stampBefore }));
		packet = Packet.packetInstance(packetEl);
		assertTrue(C2SDeliveryErrorProcessor.preProcess(packet, sessionToRes1, null, results, null, messageProcessor));
		assertEquals(1, results.size());
		result = results.peek();
		assertEquals(sessionToRes1.getConnectionId(), result.getPacketTo());	
		C2SDeliveryErrorProcessor.filter(packet, sessionToRes1, null, results, null);
		assertEquals(1, results.size());
		result = results.peek();
		assertFalse(C2SDeliveryErrorProcessor.isDeliveryError(result));
		
		
		results.clear();
		packetEl = new Element("message", new String[] { "id", "from", "to" }, 
				new String[] { UUID.randomUUID().toString(), from.toString(), to.toString() });
		packetEl.addChild(new Element("delivery-error", new String[] { "xmlns", "stamp" }, new String[] { "http://tigase.org/delivery-error", stampAfter }));
		packet = Packet.packetInstance(packetEl);
		assertTrue(C2SDeliveryErrorProcessor.preProcess(packet, sessionToRes1, null, results, null, messageProcessor));
		assertEquals(0, results.size());
	}	
	
}
