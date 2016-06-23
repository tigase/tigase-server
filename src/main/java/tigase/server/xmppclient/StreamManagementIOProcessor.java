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

import tigase.net.IOServiceListener;
import tigase.net.SocketThread;
import tigase.server.*;
import tigase.stats.StatisticsList;
import tigase.util.TimerTask;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.StreamError;
import tigase.xmpp.XMPPIOService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final String ACK_REQUEST_COUNT_KEY = "ack-request-count";
	private static final int DEF_ACK_REQUEST_COUNT_VAL = 10;
	private static final String[] DELAY_PATH = { Message.ELEM_NAME, "delay" };
	private static final String DELAY_XMLNS = "urn:xmpp:delay";
	private static final String INGORE_UNDELIVERED_PRESENCE_KEY = "ignore-undelivered-presence";
	private static final String IN_COUNTER_KEY = XMLNS + "_in";
	private static final String MAX_RESUMPTION_TIMEOUT_KEY = XMLNS + "_resumption-timeout";
	private static final String MAX_RESUMPTION_TIMEOUT_PROP_KEY = "max-resumption-timeout";
	private static final String OUT_COUNTER_KEY = XMLNS + "_out";
	private static final String RESUMPTION_TASK_KEY = XMLNS + "_resumption-task";
	private static final String RESUMPTION_TIMEOUT_PROP_KEY = "resumption-timeout";
	private static final String RESUMPTION_TIMEOUT_START_KEY = "resumption-timeout-start";
	private static final String STREAM_ID_KEY = XMLNS + "_stream_id";
	
	private static final Element[] FEATURES = { new Element("sm", new String[] { "xmlns" },
			new String[] { XMLNS }) };
	
	private static final SimpleDateFormat formatter;
	private final ConcurrentHashMap<String,XMPPIOService> services = new ConcurrentHashMap<String,XMPPIOService>();
	
	private boolean ignoreUndeliveredPresence = true;
	private int max_resumption_timeout = 15 * 60;
	private int resumption_timeout = 60;
	private int ack_request_count = DEF_ACK_REQUEST_COUNT_VAL;
	
	private ConnectionManager connectionManager;
		
	static {
		formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}	
	
	/**
	 * Method returns true if XMPPIOService has enabled SM.
	 * 
	 * @param service
	 * 
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
				OutQueue outQueue = newOutQueue();
				service.getSessionData().putIfAbsent(OUT_COUNTER_KEY, outQueue);
				service.getSessionData().putIfAbsent(IN_COUNTER_KEY, newCounter());
				
				
				String id = null;
				String location = null;
				int timeout = resumption_timeout;

				if (resumption_timeout > 0 && packet.getElement().getAttributeStaticStr(RESUME_ATTR) != null) {
					outQueue.setResumptionEnabled(true);
					String maxStr = packet.getElement().getAttributeStaticStr(MAX_ATTR);
					if (maxStr != null) {
						timeout = Math.min(max_resumption_timeout, Integer.parseInt(maxStr));
					}
					id = UUID.randomUUID().toString();
					location = connectionManager.getDefHostName().toString();
					service.getSessionData().putIfAbsent(STREAM_ID_KEY, id);
					service.getSessionData().put(MAX_RESUMPTION_TIMEOUT_KEY, timeout);
					
					services.put(id, service);
				}
				try {
					service.writeRawData("<" + ENABLED_NAME + " xmlns='" + XMLNS + "'"
							+ ( id != null ? " id='" + id + "' " + RESUME_ATTR + "='true' "+ MAX_ATTR + "='" + timeout + "'" : "" )
							+ ( location != null ? " " + LOCATION_ATTR + "='" + location + "'" : "" ) + " />");
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0}, started StreamManagement with resumption timeout set to = {1}", 
								new Object[] { service.toString(), (id != null ? resumption_timeout : null) });
					}
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, service.toString() + ", exception during sending <enabled/>, stopping...", ex);
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
						log.log(Level.FINE, service.toString() + ", exception while resuming stream for user " 
								+ service.getUserJid() + " with id " + id, ex);
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
				if (outQueue != null) {
					outQueue.ack(val);
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0}, outQueue already null while processing: {1}", new Object[] { service, packet });
					}
				}
			}
			else if (packet.getElemName() == REQ_NAME) {
				int value = ((Counter) service.getSessionData().get(IN_COUNTER_KEY)).get();
				
				try {
					service.writeRawData("<" + ACK_NAME + " xmlns='" + XMLNS 
							+ "' " + H_ATTR + "='" + String.valueOf(value) + "'/>");
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, service.toString() + ", exception during sending <a/> as "
								+ "response for <r/>, not stopping serivce as it "
								+ "will be stopped after processing all incoming data...", ex);
					}
					//service.forceStop();
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
		
		return service.getSessionData().containsKey(RESUMPTION_TASK_KEY);
	}
	
	@Override
	public void packetsSent(XMPPIOService service) throws IOException {
		if (!isEnabled(service))
			return;
		
		OutQueue outQueue = (OutQueue) service.getSessionData().get(OUT_COUNTER_KEY);		
		if (outQueue != null && shouldRequestAck(service, outQueue)) {
			service.writeRawData("<" + REQ_NAME + " xmlns='" + XMLNS + "' />");
		}
	}
	
	/**
	 * Override this method to define a custom behaviour for request ack.
	 * The default implementation will request an ack if there are more than {@link #ack_request_count}
	 * packets waiting, so you probably want to OR your behaviour with this.
	 */
	protected boolean shouldRequestAck(XMPPIOService service, OutQueue outQueue) {
		return outQueue.waitingForAck() >= ack_request_count;
	}
	
	@Override
	public void processCommand(XMPPIOService service, Packet pc) {
		String cmdId = Command.getFieldValue(pc, "cmd");
		if ("stream-moved".equals(cmdId)) {
			String newConn = Command.getFieldValue(pc, "new-conn-jid");
			
			String id = (String) service.getSessionData().get(STREAM_ID_KEY);
			
			
			JID newConnJid = JID.jidInstanceNS(newConn);
			XMPPIOService newService = connectionManager.getXMPPIOService(newConnJid.getResource());
			
			// if connection was closed during resumption, then close
			// old connection as it would not be able to resume 
			if (newService != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "stream for user {2} moved from {0} to {1}", new Object[] { 
						service.getConnectionId(), newService.getConnectionId(), newService.getUserJid() });
				}
				try {
					newService.setUserJid(service.getUserJid());
					Counter inCounter = (Counter) newService.getSessionData().get(IN_COUNTER_KEY);
					newService.writeRawData("<" + RESUMED_NAME + " xmlns='" + XMLNS + "' " + PREVID_ATTR + "='" 
							+ id + "' " + H_ATTR + "='" + inCounter.get() + "' />");

					service.getSessionData().put("stream-closed", "stream-closed");
					services.put(id, newService);
					
					// resending packets thru new connection
					OutQueue outQueue = (OutQueue) newService.getSessionData().get(OUT_COUNTER_KEY);
					List<OutQueue.Entry> packetsToResend = new ArrayList<OutQueue.Entry>(outQueue.getQueue());
					for (OutQueue.Entry entry : packetsToResend) {
						Packet packetToResend = entry.getPacketWithStamp();
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "{0}, resuming stream with id = {1} resending unacked packet = {2}",
									new Object[] { service, id, packetToResend });
						}
						newService.addPacketToSend(packetToResend);
					}
										
					// if there is any packet waiting we need to write them to socket
					// and to do that we need to call processWaitingPackets();
					if (!packetsToResend.isEmpty()) {
						if (newService.writeInProgress.tryLock()) {
							try {
								newService.processWaitingPackets();
								SocketThread.addSocketService(newService);
							} catch (Exception e) {
								log.log(Level.WARNING, newService + "Exception during writing packets: ", e);
								try {
									newService.stop();
								} catch (Exception e1) {
									log.log(Level.WARNING, newService + "Exception stopping XMPPIOService: ", e1);
								}    // end of try-catch
							} finally {
								newService.writeInProgress.unlock();
							}
						}
					}
				}
				catch (IOException ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "could not confirm session resumption for user = " 
								+ newService.getUserJid(), ex);
					}
					
					// remove new connection if resumption failed
					services.remove(id, service);
					services.remove(id, newService);
				}
			}	
			else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "no new service available for user {0} to resume from {1},"
							+ " already closed?", new Object[] { service.getUserJid(), service });
				}
			}
			
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "closing old service {0} for user {1}", new Object[] { service, 
					service.getUserJid()});
			}
			
			// stopping old service
			connectionManager.serviceStopped(service);
		}
	}
	
	@Override
	public boolean serviceStopped(XMPPIOService service, boolean streamClosed) {
		if (!isEnabled(service)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, service stopped - StreamManagement disabled", new Object[] { service });
			}
			return false;
		}

		String id = (String) service.getSessionData().get(STREAM_ID_KEY);
		
		if (streamClosed) {
			service.getSessionData().remove(STREAM_ID_KEY);
		}
				
//		try {
//			throw new Exception();
//		} catch (Throwable ex) {
//			log.log(Level.WARNING, "resumption timeout started, stream close = " + streamClosed, ex);
//			ex.printStackTrace();
//		}
		Long resumptionTimeoutStart = (Long) service.getSessionData().get(RESUMPTION_TIMEOUT_START_KEY);
		if (resumptionTimeoutStart != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, service stopped - checking resumption timeout", new Object[] { service });
			}
			// if resumptionTimeoutStart is set let's check if resumption was 
			// not started for longer time than twice value of resumption_timeout
			if (((System.currentTimeMillis() - resumptionTimeoutStart) > (2 * resumption_timeout * 1000))
				|| streamClosed) {
				// if so we should assume that resumption failed so we should 
				// send errors, remove reference to service and stop this service
				services.remove(id, service);
				service.clearWaitingPackets();
				connectionManager.serviceStopped(service);
				// for case in which task was started but later </stream:stream> was found in remaining data
				TimerTask timerTask = (TimerTask) service.getSessionData().get(RESUMPTION_TASK_KEY);
				if (timerTask != null)
					timerTask.cancel();
				sendErrorsForQueuedPackets(service);	
			}
			return false;
		}
				
		// some buggy client (ie. Psi) may close stream without sending stream 
		// close which forces us to thread this stream as broken and waiting for 
		// resumption but those clients are not compatible with XEP-0198 and 
		// resumption so this should not happen
		if (isResumptionEnabled(service)) {
			if (service.getSessionData().getOrDefault(XMPPIOService.STREAM_CLOSING, false) == Boolean.TRUE) {
				services.remove(id, service);
				connectionManager.serviceStopped(service);
				sendErrorsForQueuedPackets(service);
				return false;
			}

			if (!services.containsKey(id)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, service stopped - resumption enabled but service not available", new Object[] { service });
				}
				return false;
			}

			// ConnectionManager must not be notified about closed connection
			// but connection needs to be closed so this is this case we still 
			// return false to call forceStop but we remove IOServiceListener
			service.setIOServiceListener((IOServiceListener) null);
				
			int resumptionTimeout = (Integer) service.getSessionData().get(MAX_RESUMPTION_TIMEOUT_KEY);
			synchronized (service) {
				if (!service.getSessionData().containsKey(RESUMPTION_TASK_KEY)) {					
					TimerTask timerTask = new ResumptionTimeoutTask(service);
					service.getSessionData().put(RESUMPTION_TASK_KEY, timerTask);
					connectionManager.addTimerTask(timerTask, resumptionTimeout * 1000);
					
					// set timestamp of begining of resumption to be able to detect
					// if something went wrong during resumption and service is
					// still kept in connection manager services as active service
					// after twice as long as resumption timeout
					service.getSessionData().put(RESUMPTION_TIMEOUT_START_KEY, System.currentTimeMillis());
					service.clearWaitingPackets();
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, service stopped - resumption enabled and timeout started", new Object[] { service });
					}
				}
			}
			
			return false;
		}
		else if (id != null) {
			services.remove(id, service);
		}

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, service stopped - resumption disabled, sending unacked packets", new Object[] { service });
		}		
		
		service.clearWaitingPackets();
		connectionManager.serviceStopped(service);
		sendErrorsForQueuedPackets(service);
		return false;
	}

	@Override
	public void setProperties(Map<String,Object> props) {
		if (props.containsKey(MAX_RESUMPTION_TIMEOUT_PROP_KEY)) {
			this.max_resumption_timeout = (Integer) props.get(MAX_RESUMPTION_TIMEOUT_PROP_KEY);
		}
		if (props.containsKey(RESUMPTION_TIMEOUT_PROP_KEY)) {
			this.resumption_timeout = (Integer) props.get(RESUMPTION_TIMEOUT_PROP_KEY);
		}
		if (props.containsKey(INGORE_UNDELIVERED_PRESENCE_KEY)) {
			this.ignoreUndeliveredPresence = (Boolean) props.get(INGORE_UNDELIVERED_PRESENCE_KEY);
		}
		if (props.containsKey(ACK_REQUEST_COUNT_KEY)) {
			this.ack_request_count = (Integer) props.get(ACK_REQUEST_COUNT_KEY);
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
		if (oldService == null || !isSameUser(oldService, service)) {
			// should send failed!
			service.writeRawData("<failed xmlns='" + XMLNS + "'>" 
					+ "<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
					+ "</failed>");				
			return;	
		}

		// if stream has resource binded then we should not resume
		if (service.getUserJid() != null && JID.jidInstanceNS(service.getUserJid()).getResource() != null) {
			service.writeRawData("<failed xmlns='" + XMLNS + "'>"
					+ "<unexpected-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
					+ "</failed>");
			return;
		}
		
		if (services.remove(id, oldService)) {
			synchronized (oldService) {
				TimerTask timerTask = (TimerTask) oldService.getSessionData().remove(RESUMPTION_TASK_KEY);
				if (timerTask != null) {
					timerTask.cancel();
				}
				oldService.getSessionData().put(RESUMPTION_TASK_KEY, true);
				oldService.clearWaitingPackets();
			}

			// get old out queue
			OutQueue outQueue = (OutQueue) oldService.getSessionData().get(OUT_COUNTER_KEY);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "{0}, resuming stream with id = {1} with {2} packets waiting for ack, local h = {3} and remote h = {4}",
						new Object[] { service, id, outQueue.waitingForAck(), outQueue.get(), h });
			}
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
			cmd.setPacketFrom(service.getConnectionId());
			cmd.setPacketTo(service.getDataReceiver());
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
	 * Verifies if connections are authenticate for same bare jid
	 * 
	 * @param oldService
	 * @param newService
	 * @return true - only when bare jids are the same
	 */
	private boolean isSameUser(XMPPIOService oldService, XMPPIOService newService) {
		if (oldService.getUserJid() == null || newService.getUserJid() == null)
			return false;
		
		JID oldUserJid = JID.jidInstanceNS(oldService.getUserJid());
		JID newUserJid = JID.jidInstanceNS(newService.getUserJid());
		
		return oldUserJid.getBareJID().equals(newUserJid.getBareJID());
	}
	
	/**
	 * Method responsible for sending recipient-unavailable error for all not acked packets
	 * 
	 * @param service 
	 */
	private void sendErrorsForQueuedPackets(XMPPIOService service) {
		service.clearWaitingPackets();
		
		OutQueue outQueue = (OutQueue) service.getSessionData().remove(OUT_COUNTER_KEY);		
		if (outQueue != null) {
			OutQueue.Entry e = null;
			
			while ((e = outQueue.queue.poll()) != null) {
				connectionManager.processUndeliveredPacket(e.getPacketWithStamp(), e.stamp, null);
			}
		}
	}

	@Override
	public void getStatistics(StatisticsList list) {
	}

	@Override
	public void streamError(XMPPIOService service, StreamError streamErrorName) {
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
				//service.getSessionData().put(SERVICE_STOP_ALLOWED_KEY, true);
				service.clearWaitingPackets();
				connectionManager.serviceStopped(service);
				sendErrorsForQueuedPackets(service);
			}
		}
		
	}
	
	protected Counter newCounter() {
		return new Counter();
	}
	
	protected OutQueue newOutQueue() {
		return new OutQueue();
	}
	
	/**
	 * Counter class implements proper counter with overflow from 2^32-1 to 0
	 */
	public static class Counter {
		
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
		 * 
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
		
		private final ArrayDeque<Entry> queue = new ArrayDeque<Entry>();
		
		private boolean resumptionEnabled = false;
		
		/**
		 * Append packet to waiting for ack queue
		 * 
		 * @param packet 
		 */
		public void append(Packet packet) {
			if (!packet.wasProcessedBy(XMLNS)) {
				packet.processedBy(XMLNS);

				queue.offer(new Entry(packet));
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
		
		public void setResumptionEnabled(boolean enabled) {
			resumptionEnabled = enabled;
		}
		
		/**
		 * Returns size of queue containing packets waiting for ack
		 * 
		 * 
		 */
		public int waitingForAck() {
			return queue.size();
		}
		
		/**
		 * Method returns internal queue with packets waiting for ack - use testing 
		 * only!
		 * 
		 * 
		 */
		protected ArrayDeque<Entry> getQueue() {
			return queue;
		}
		
		public class Entry {
			private final Packet packet;
			private final long stamp = System.currentTimeMillis();
			
			public Entry(Packet packet) {
				this.packet = packet;
			}
			
			public Packet getPacketWithStamp() {
				if (packet.getElemName() != Iq.ELEM_NAME && !packet.isXMLNSStaticStr(DELAY_PATH, DELAY_XMLNS)) {
					String stamp = null;
					synchronized (formatter) {
						stamp = formatter.format(this.stamp);
					}
					String from = packet.getStanzaTo() != null ? packet.getStanzaTo().getDomain() : packet.getPacketTo().getDomain();
					Element x = new Element( "delay", new String[] {
						"from", "stamp", "xmlns" }, new String[] { from, stamp, "urn:xmpp:delay" } );
					packet.getElement().addChild(x);					
				}
				return packet;
			}
		}
	}
}
