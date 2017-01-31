/*
 * StreamManagementIOProcessorTest.java
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
package tigase.server.xmppclient;

import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.xmppclient.StreamManagementIOProcessor.OutQueue;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

/**
 * Test class for StreamManagementIOProcessor class.
 * 
 * Currently tests if acking works correctly for overflow in OutQueue counter. 
 * 
 * @author andrzej
 */
public class StreamManagementIOProcessorTest extends TestCase {
	
	private static final String[] DELAY_PATH = { Message.ELEM_NAME, "delay" };
	private static final String DELAY_XMLNS = "urn:xmpp:delay";
		
	@Test
	public void testValidateOutQueueOverflowToZero() {
		OutQueue queue = new OutQueue();
		int start = Integer.MAX_VALUE-10;
		queue.setCounter(start-1);
		
		int packetToAck = start+5;
		
		for (int i=start; i<=Integer.MAX_VALUE && i > 0; i++) {
			try {
				Packet p = Packet.packetInstance(new Element("message", new String[] { "id", "from", "to" },
						new String[] { String.valueOf(i), "from@example.com", "to@example.com" }));
				queue.append(p);
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		queue.ack(packetToAck);
		
		int size = queue.waitingForAck();
		assertEquals(5, size);
		assertEquals(queue.getQueue().peek().getPacketWithStamp().getElement().getAttributeStaticStr("id"), String.valueOf(packetToAck+1));
		queue.getQueue().forEach((OutQueue.Entry e) -> 
				assertTrue(e.getPacketWithStamp().isXMLNSStaticStr(DELAY_PATH, DELAY_XMLNS))
		);
	}		

	@Test
	public void testValidateOutQueueOverflowOverZero() {
		OutQueue queue = new OutQueue();
		int start = Integer.MAX_VALUE-5;
		queue.setCounter(start-1);
		int packetToAck = start + 3;
		
		for (int i=start; i<=Integer.MAX_VALUE && i > 0; i++) {
			try {
				Packet p = Packet.packetInstance(new Element("message", new String[] { "id", "from", "to" },
						new String[] { String.valueOf(i), "from@example.com", "to@example.com" }));
				queue.append(p);
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		for (int i=0; i<3; i++) {
			try {
				Packet p = Packet.packetInstance(new Element("message", new String[] { "id", "from", "to" },
						new String[] { String.valueOf(i), "from@example.com", "to@example.com" }));
				queue.append(p);
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		queue.ack(packetToAck);
		
		int size = queue.waitingForAck();
		assertEquals(5, size);
		assertEquals(queue.getQueue().peek().getPacketWithStamp().getElement().getAttributeStaticStr("id"), String.valueOf(packetToAck+1));
		queue.getQueue().forEach((OutQueue.Entry e) ->
				assertTrue(e.getPacketWithStamp().isXMLNSStaticStr(DELAY_PATH, DELAY_XMLNS))
		);
	}
	
	@Test
	public void testNoDelayForIq() {
		OutQueue queue = new OutQueue();
		int start = Integer.MAX_VALUE-10;
		queue.setCounter(start-1);
		
		int packetToAck = start+5;
		
		for (int i=start; i<=Integer.MAX_VALUE && i > 0; i++) {
			try {
				Packet p = Packet.packetInstance(new Element("iq", new String[] { "id", "from", "to" },
						new String[] { String.valueOf(i), "from@example.com", "to@example.com" }));
				queue.append(p);
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		queue.getQueue().forEach((OutQueue.Entry e) ->
				assertFalse(e.getPacketWithStamp().isXMLNSStaticStr(new String[] { "iq", "delay" }, DELAY_XMLNS))
		);
	}
	
	@Test
	public void testForceDelayForResumption() {
		OutQueue queue = new OutQueue();
		
		try {
			Packet p = Packet.packetInstance(new Element("message", new Element[]{
				new Element("delay", new String[]{"xmlns", "stamp"}, new String[]{DELAY_XMLNS, "2015-07-10T00:00:00.000Z"})
			}, new String[]{"id", "from", "to"},
					new String[]{String.valueOf("id-1"), "from@example.com", "to@example.com"}));
			queue.append(p);
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		queue.getQueue().forEach((OutQueue.Entry e) ->
				assertEquals(1, e.getPacketWithStamp().getElement().findChildren((Element el) -> el.getName() == "delay").size())
		);		

		queue.getQueue().clear();
		
		queue.setResumptionEnabled(true);
		
		try {
			Packet p = Packet.packetInstance(new Element("message", new Element[]{
				new Element("delay", new String[]{"xmlns", "stamp"}, new String[]{DELAY_XMLNS, "2015-07-10T00:00:00.000Z"})
			}, new String[]{"id", "from", "to"},
					new String[]{String.valueOf("id-1"), "from@example.com", "to@example.com"}));
			queue.append(p);
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		queue.getQueue().forEach((OutQueue.Entry e) ->
				assertEquals(1, e.getPacketWithStamp().getElement().findChildren((Element el) -> el.getName() == "delay").size())
		);		

		queue.getQueue().clear();
		
		try {
			Packet p = Packet.packetInstance(new Element("iq", new String[]{"id", "from", "to"},
					new String[]{String.valueOf("id-2"), "from@example.com", "to@example.com"}));
			queue.append(p);
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(StreamManagementIOProcessorTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		queue.getQueue().forEach((OutQueue.Entry e) ->
				assertFalse(e.getPacketWithStamp().isXMLNSStaticStr(new String[] { "iq", "delay" }, DELAY_XMLNS))
		);
	}	
}
