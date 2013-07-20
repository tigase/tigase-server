/*
 * StreamManagementIOProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPIOService;

/**
 * Class implements XEP-0198 Stream Management
 * 
 * @author andrzej
 */
public class StreamManagementIOProcessor implements XMPPIOProcessor {

	private static final Logger log = Logger.getLogger(StreamManagementIOProcessor.class.getCanonicalName());
	
	public static final String XMLNS = "urn:xmpp:sm:3";
		
	private static final String ACK_NAME = "a";
	private static final String REQ_NAME = "r";
	
	private static final String H_ATTR = "h";
	
	private static final String ENABLE_NAME = "enable";
	private static final String ENABLED_NAME = "enabled";
	
	private static final String STREAM_ID_KEY = XMLNS + "_stream_id";
	private static final String IN_COUNTER_KEY = XMLNS + "_in";
	private static final String OUT_COUNTER_KEY = XMLNS + "_out";
	
	private static final Element[] FEATURES = { new Element("sm", new String[] { "xmlns" },
			new String[] { XMLNS }) };
	
	private boolean resumption_allowed = false;
	private int default_ack_request_count = 10;
	
	private final ConnectionManager connectionManager;
	
	public StreamManagementIOProcessor(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}
	
	/**
	 * Method returns true if XMPPIOService has enabled SM.
	 * 
	 * @param service
	 * @return 
	 */
	public static boolean isEnabled(XMPPIOService service) {
		return service.getSessionData().containsKey(STREAM_ID_KEY);
	}
	
	@Override
	public Element[] supStreamFeatures(XMPPIOService service) {
		// user jid may not be set yet because is is set during resource binding
		// while this feature should be advertised just after authentication of 
		// connection
		/*if (service.getUserJid() == null)
			return null;*/
		
		return FEATURES;
	}
	
	@Override
	public boolean processIncoming(XMPPIOService service, Packet packet) {
		if (!isEnabled(service)) {
			if (packet.getXMLNS() != XMLNS) {
				return false;
			}
			else if (packet.getElemName() == ENABLE_NAME) {
				service.getSessionData().putIfAbsent(IN_COUNTER_KEY, new Counter());
				service.getSessionData().putIfAbsent(OUT_COUNTER_KEY, new OutQueue());
				
				String id = UUID.randomUUID().toString();
				service.getSessionData().putIfAbsent(STREAM_ID_KEY, id);
				
				try {
					service.writeRawData("<" + ENABLED_NAME + " xmlns='" + XMLNS + "'"
							+ ( resumption_allowed ? " id='" + id + "' resume='true'" : "" ) +" />");
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "exception during sending <enabled/>, stopping...", ex);
					}
					service.forceStop();
				}
				return true;
			}
			else {
				return false;
			}
		}
		if (packet.getXMLNS() == XMLNS) {
			if (packet.getElemName() == ACK_NAME) {
				String valStr = packet.getAttributeStaticStr(H_ATTR);
				
				int val = Integer.parseInt(valStr);
				OutQueue outQueue = (OutQueue) service.getSessionData().get(OUT_COUNTER_KEY);
				outQueue.ack(val);
			}
			else if (packet.getElemName() == REQ_NAME) {
				int value = ((Counter) service.getSessionData().get(IN_COUNTER_KEY)).get();
				
				try {
					service.writeRawData("<" + ACK_NAME + " xmlns='" + XMLNS 
							+ "' h='" + String.valueOf(value) + "'/>");
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "exception during sending <a/> as "
								+ "response for <r/>, stopping...", ex);
					}
					service.forceStop();
				}
			}
			return true;
		}
		
		((Counter) service.getSessionData().get(IN_COUNTER_KEY)).inc();
		
		return false;
	}

	@Override
	public boolean processOutgoing(XMPPIOService service, Packet packet) {
		if (!isEnabled(service) || packet.getXMLNS() == XMLNS) {
			return false;
		}
		
		OutQueue outQueue = (OutQueue) service.getSessionData().get(OUT_COUNTER_KEY);		
		outQueue.append(packet);
		
		return false;
	}
	
	@Override
	public void packetsSent(XMPPIOService service) throws IOException {
		if (!isEnabled(service))
			return;
		
		OutQueue outQueue = (OutQueue) service.getSessionData().get(OUT_COUNTER_KEY);		
		if (outQueue.waitingForAck() >= default_ack_request_count) {
			service.writeRawData("<r xmlns='" + XMLNS + "' />");
		}
	}
	
	@Override
	public void serviceStopped(XMPPIOService service) {
		if (!isEnabled(service))
			return;
		
		OutQueue outQueue = (OutQueue) service.getSessionData().remove(OUT_COUNTER_KEY);		
		if (outQueue != null) {
			Packet packet = null;
			
			while ((packet = outQueue.queue.poll()) != null) {
				try {
					connectionManager.processOutPacket(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet, null, true));
				} catch (PacketErrorTypeException ex) {
					log.log(Level.FINER, "exception prepareing request for returning error, data = {0}", packet);
				}
			}
		}
	}
	
	/**
	 * Counter class implements proper counter with overflow from 2^32-1 to 0
	 */
	private static class Counter {
		
		private int counter = 0;
		
		/**
		 * Increment counter
		 */
		public void inc() {
			counter++;
			if (counter < 0)
				counter = 0;
		}
		
		/**
		 * Get value of counter.
		 * 
		 * @return 
		 */
		public int get() {
			return counter;
		}
				
		/**
		 * Sets value of a counter - use only for testing!
		 * 
		 * @param value 
		 */
		protected void setCounter(int value) {
			this.counter = value;
		}
	}
	
	/**
	 * OutQueue class implements queue of outgoing packets waiting for ack
	 * with implementation of removing acked elements when id of acked packet 
	 * is passed
	 */
	public static class OutQueue extends Counter {
		
		private final ArrayDeque<Packet> queue = new ArrayDeque<Packet>();
		
		/**
		 * Append packet to waiting for ack queue
		 * 
		 * @param packet 
		 */
		public void append(Packet packet) {
			queue.offer(packet);
			inc();
		}
		
		/**
		 * Confirm delivery of packets up to count passed as value
		 * 
		 * @param value 
		 */
		public void ack(int value) {			
			int count = get() - value;
			
			if (count < 0) {
				count = (Integer.MAX_VALUE - value) + get() + 1;
			}
			
			while (count < queue.size()) {
				queue.poll();
			}
		}
		
		/**
		 * Returns size of queue containing packets waiting for ack
		 * 
		 * @return 
		 */
		public int waitingForAck() {
			return queue.size();
		}
		
		/**
		 * Method returns internal queue with packets waiting for ack - use testing 
		 * only!
		 * 
		 * @return 
		 */
		protected ArrayDeque<Packet> getQueue() {
			return queue;
		}
		
	}
}
