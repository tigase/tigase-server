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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.util.TimerTask;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;

/**
 * Class implements XEP-0198 Stream Management
 * 
 * @author andrzej
 */
public class StreamManagementIOProcessor implements XMPPIOProcessor {

	private static final Logger log = Logger.getLogger(StreamManagementIOProcessor.class.getCanonicalName());
	
	public static final String XMLNS = "urn:xmpp:sm:3";

	// used tag names
	private static final String ACK_NAME = "a";
	private static final String ENABLE_NAME = "enable";
	private static final String ENABLED_NAME = "enabled";
	private static final String REQ_NAME = "r";
	private static final String RESUME_NAME = "resume";
	private static final String RESUMED_NAME = "resumed";
	
	// used attribute names
	private static final String H_ATTR = "h";
	private static final String LOCATION_ATTR = "location";
	private static final String RESUME_ATTR = "resume";
	private static final String MAX_ATTR = "max";
	private static final String PREVID_ATTR = "previd";
		
	// various strings used as key to store data in maps
	private static final String IN_COUNTER_KEY = XMLNS + "_in";
	private static final String MAX_RESUMPTION_TIMEOUT_KEY = XMLNS + "_resumption-timeout";
	private static final String OUT_COUNTER_KEY = XMLNS + "_out";
	private static final String RESUMPTION_TASK_KEY = XMLNS + "_resumption-task";
	private static final String RESUMPTION_TIMEOUT_PROP_KEY = "resumption-timeout";
	private static final String STREAM_ID_KEY = XMLNS + "_stream_id";
	
	private static final Element[] FEATURES = { new Element("sm", new String[] { "xmlns" },
			new String[] { XMLNS }) };
	
	private final ConcurrentHashMap<String,XMPPIOService> services = new ConcurrentHashMap<String,XMPPIOService>();
	
	private int resumption_timeout = 60;
	private int default_ack_request_count = 10;
	
	private ConnectionManager connectionManager;
			
	/**
	 * Method returns true if XMPPIOService has enabled SM.
	 * 
	 * @param service
	 * @return 
	 */
	public static boolean isEnabled(XMPPIOService service) {
		return service.getSessionData().containsKey(IN_COUNTER_KEY);
	}
	
	private static boolean isResumptionEnabled(XMPPIOService service) {
		return service.getSessionData().containsKey(STREAM_ID_KEY);
	}
	
	public StreamManagementIOProcessor() {
	}

	@Override
	public String getId() {
		return XMLNS;
	}
	
	@Override
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
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
				
				
				String id = null;
				int max = resumption_timeout;

				if (resumption_timeout > 0 && packet.getElement().getAttributeStaticStr(RESUME_ATTR) != null) {
					String maxStr = packet.getElement().getAttributeStaticStr(MAX_ATTR);
					if (maxStr != null) {
						max = Math.min(max, Integer.parseInt(maxStr));
					}
					id = UUID.randomUUID().toString();
					service.getSessionData().putIfAbsent(STREAM_ID_KEY, id);
					service.getSessionData().put(MAX_RESUMPTION_TIMEOUT_KEY, max);
					
					services.put(id, service);
				}
				try {
					String location = connectionManager.getDefHostName().toString();
					service.writeRawData("<" + ENABLED_NAME + " xmlns='" + XMLNS + "'"
							+ ( id != null ? " id='" + id + "' " + RESUME_ATTR + "='true' "+ MAX_ATTR + "='" + max + "'" : "" ) 
							+ " " + LOCATION_ATTR + "='" + location + "' />");
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "exception during sending <enabled/>, stopping...", ex);
					}
					service.forceStop();
				}
				return true;
			}
			else if (packet.getElemName() == RESUME_NAME) {
				String h = packet.getElement().getAttributeStaticStr(H_ATTR);
				String id = packet.getElement().getAttributeStaticStr(PREVID_ATTR);
				
				try {
					resumeStream(service, id, Integer.parseInt(h));
				} catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "exception while resuming stream for user " 
								+ service.getUserJid() + " with id " + id, ex);
					}
					
					service.forceStop();
				}
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
							+ "' " + H_ATTR + "='" + String.valueOf(value) + "'/>");
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
			service.writeRawData("<" + REQ_NAME + " xmlns='" + XMLNS + "' />");
		}
	}
	
	@Override
	public void processCommand(XMPPIOService service, Packet pc) {
		String cmdId = Command.getFieldValue(pc, "cmd");
		if ("stream-moved".equals(cmdId)) {
			String newConnJid = Command.getFieldValue(pc, "new-conn-jid");
			
			String id = (String) service.getSessionData().get(STREAM_ID_KEY);
			
			XMPPIOService newService = services.get(id);
			
			// if connection was closed during resumption, then close
			// old connection as it would not be able to resume 
			if (newService != null) {
				try {
					Counter inCounter = (Counter) newService.getSessionData().get(IN_COUNTER_KEY);
					newService.writeRawData("<" + RESUMED_NAME + " xmlns='" + XMLNS + "' " + PREVID_ATTR + "='" 
							+ id + "' " + H_ATTR + "='" + inCounter.get() + "' />");

					// resending packets thru new connection
					OutQueue outQueue = (OutQueue) newService.getSessionData().get(OUT_COUNTER_KEY);
					List<Packet> packetsToResend = new ArrayList<Packet>(outQueue.getQueue());
					for (Packet packet : packetsToResend) {
						newService.addPacketToSend(packet);
					}
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "could not confirm session resumption for user = " 
								+ newService.getUserJid(), ex);
					}
				}
			}	
			
			// stopping old service
			connectionManager.serviceStopped(service);
		}
	}
	
	@Override
	public boolean serviceStopped(XMPPIOService service, boolean streamClosed) {
		if (!isEnabled(service))
			return false;

		if (streamClosed) {
			service.getSessionData().remove(STREAM_ID_KEY);
		}
		
		if (isResumptionEnabled(service)) {
			String id = (String) service.getSessionData().get(STREAM_ID_KEY);
			if (!services.containsKey(id))
				return false;
			
			int resumptionTimeout = (Integer) service.getSessionData().get(MAX_RESUMPTION_TIMEOUT_KEY);
			synchronized (service) {
				if (!service.getSessionData().containsKey(RESUMPTION_TASK_KEY)) {
					TimerTask timerTask = new ResumptionTimeoutTask(service);
					connectionManager.addTimerTask(timerTask, resumptionTimeout * 1000);
				}
			}
			
			return true;
		}
		
		sendErrorsForQueuedPackets(service);
		return false;
	}

	@Override
	public void setProperties(Map<String,Object> props) {
		if (props.containsKey(RESUMPTION_TIMEOUT_PROP_KEY)) {
			this.resumption_timeout = (Integer) props.get(RESUMPTION_TIMEOUT_PROP_KEY);
		}
	}
	
	/**
	 * Method responsible for starting process of stream resumption
	 * 
	 * @param service
	 * @param id
	 * @param h
	 * @throws IOException 
	 */
	private void resumeStream(XMPPIOService service, String id, int h) throws IOException {
		XMPPIOService oldService = services.get(id);
		if (oldService == null || service.getUserJid().equals(oldService.getUserJid()))
			return;
		
		if (services.remove(id, oldService)) {
			synchronized (oldService) {
				TimerTask timerTask = (TimerTask) oldService.getSessionData().remove(RESUMPTION_TASK_KEY);
				if (timerTask != null) {
					timerTask.cancel();
				}
				oldService.getSessionData().put(RESUMPTION_TASK_KEY, true);
			}

			// get old out queue
			OutQueue outQueue = (OutQueue) oldService.getSessionData().get(OUT_COUNTER_KEY);
			outQueue.ack(h);

			// move required data from old XMPPIOService session data to new service session data
			service.getSessionData().put(OUT_COUNTER_KEY, outQueue);
			service.getSessionData().put(MAX_RESUMPTION_TIMEOUT_KEY, 
					oldService.getSessionData().get(MAX_RESUMPTION_TIMEOUT_KEY));
			service.getSessionData().put(IN_COUNTER_KEY, 
					oldService.getSessionData().get(IN_COUNTER_KEY));
			service.getSessionData().put(STREAM_ID_KEY, 
					oldService.getSessionData().get(STREAM_ID_KEY));
			
			// send notification to session manager about change of connection 
			// used for session
			Packet cmd = Command.STREAM_MOVED.getPacket(service.getConnectionId(), 
					service.getDataReceiver(), StanzaType.set, "moved");
			Command.addFieldValue(cmd, "old-conn-jid", oldService.getConnectionId().toString());
			connectionManager.processOutPacket(cmd);
		}
		else {
			// should send failed!
			service.writeRawData("<failed xmlns='" + XMLNS + "'>" 
					+ "<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
					+ "</failed>");
		}
	}
	
	/**
	 * Method responsible for sending recipient-unavailable error for all not acked packets
	 * 
	 * @param service 
	 */
	private void sendErrorsForQueuedPackets(XMPPIOService service) {
		OutQueue outQueue = (OutQueue) service.getSessionData().remove(OUT_COUNTER_KEY);		
		if (outQueue != null) {
			Packet packet = null;
			
			while ((packet = outQueue.queue.poll()) != null) {
				try {
					connectionManager.processOutPacket(Authorization.RECIPIENT_UNAVAILABLE
							.getResponseMessage(packet, null, true));
				} catch (PacketErrorTypeException ex) {
					log.log(Level.FINER, "exception prepareing request for returning error, data = {0}", 
							packet);
				}
			}
		}
	}
	
	/**
	 * ResumptionTimeoutTask class is used for handing of timeout used during 
	 * session resumption
	 */
	private class ResumptionTimeoutTask extends TimerTask {

		private final XMPPIOService service;
		
		public ResumptionTimeoutTask(XMPPIOService service) {
			this.service = service;
		}
		
		@Override
		public void run() {	
			String id = (String) service.getSessionData().get(STREAM_ID_KEY);
			if (services.remove(id, service)) {
				sendErrorsForQueuedPackets(service);
				//service.getSessionData().put(SERVICE_STOP_ALLOWED_KEY, true);
				connectionManager.serviceStopped(service);
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
			if (!packet.wasProcessedBy(XMLNS)) {
				packet.processedBy(XMLNS);
				queue.offer(packet);
				inc();
			}
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
