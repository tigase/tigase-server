/*
 * BoshSession.java
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



package tigase.server.bosh;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.bosh.Constants.CacheAction;
import tigase.server.xmppclient.SeeOtherHostIfc.Phase;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;

import tigase.util.TigaseStringprepException;
import tigase.util.TimerTask;
import tigase.xml.Element;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static tigase.server.bosh.Constants.*;


/**
 * Describe class BoshSession here.
 *
 *
 * Created: Tue Jun 5 18:07:23 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshSession {
	private static final String IQ_ELEMENT_NAME = "iq";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger              log = Logger.getLogger(BoshSession.class
			.getName());
	private static final String              MESSAGE_ELEMENT_NAME  = "message";
	private static final String              PRESENCE_ELEMENT_NAME = "presence";
	private static final long                SECOND                = 1000;
	private static final TimerTaskComparator timerTaskComparator =
			new TimerTaskComparator();

	//~--- fields ---------------------------------------------------------------

	// ~--- fields ---------------------------------------------------------------
	private BoshSessionCache cache                = null;
	private long             cache_reload_counter = 0;

	/**
	 * <code>current_rid</code> is the table with body rids which are waiting for
	 * replies.
	 */
	private long[]                 currentRids           = null;
	private JID                    dataReceiver          = null;
	private String                 domain                = null;
	private String                 hostname              = null;
	private BoshSessionTaskHandler handler               = null;
	private int[]                  hashCodes             = null;
	private BoshTask               inactivityTimer       = null;
	private long                   previous_received_rid = -1;
	private BoshSendQueueTask      queueTask             = null;
	private String[]               replace_with = {
			"$1&lt;a href=\"http://$2\" target=\"_blank\"&gt;$2&lt;/a&gt;",
			"$1&lt;a href=\"$2\" target=\"_blank\"&gt;$2&lt;/a&gt;", };
	private int    rids_head = 0;
	private int    rids_tail = 0;
	private String sessionId = null;
	private UUID   sid       = null;

	// Old connections which might be reused in keep-alive mode.
	// Requests have been responded to so in most cases the connection should
	// be closed unless it is reused in keep-alive mode.
	// Normally there should be no more than max 2 elements in the queue.
	private Queue<BoshIOService> old_connections = new LinkedBlockingQueue<BoshIOService>(
			4);

	// Active connections with pending requests received
	private ConcurrentSkipListMap<BoshTask, BoshIOService> connections =
			new ConcurrentSkipListMap<BoshTask, BoshIOService>(timerTaskComparator);
	private JID userJid = null;

	// private enum TimedTask { EMPTY_RESP, STOP };
	// private Map<TimerTask, TimedTask> task_enum =
	// new LinkedHashMap<TimerTask, TimedTask>();
	// private EnumMap<TimedTask, TimerTask> enum_task =
	// new EnumMap<TimedTask, TimerTask>(TimedTask.class);
	private Set<BoshTask> waitTimerSet = new ConcurrentSkipListSet<BoshTask>(
			timerTaskComparator);
	private Queue<Element> waiting_packets = null;//new ConcurrentLinkedQueue<Element>();
	private boolean        terminate       = false;
	private long           min_polling     = MIN_POLLING_PROP_VAL;
	private long           max_wait        = MAX_WAIT_DEF_PROP_VAL;
	private long           max_pause       = MAX_PAUSE_PROP_VAL;
	private long           max_inactivity  = MAX_INACTIVITY_PROP_VAL;
	private int            max_batch_size  = MAX_BATCH_SIZE_VAL;
	private Pattern[]      links_regexs = { Pattern.compile("([^>/\";]|^)(www\\.[^ ]+)",
			Pattern.CASE_INSENSITIVE),
			Pattern.compile("([^\">;]|^)(http://[^ ]+)", Pattern.CASE_INSENSITIVE), };
	private int     hold_requests       = HOLD_REQUESTS_PROP_VAL;
	private String  content_type        = CONTENT_TYPE_DEF;
	private int     concurrent_requests = CONCURRENT_REQUESTS_PROP_VAL;
	private boolean cache_on            = false;
	private long    batch_queue_timeout = BATCH_QUEUE_TIMEOUT_VAL;
	private long    last_send_time;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>BoshSession</code> instance.
	 *
	 *
	 * @param def_domain
	 * @param dataReceiver
	 * @param handler
	 */
	public BoshSession(String def_domain, JID dataReceiver,
			BoshSessionTaskHandler handler, String hostname, int maxWaitingPackets) {
		this.sid            = UUID.randomUUID();
		this.domain         = def_domain;
		this.dataReceiver   = dataReceiver;
		this.handler        = handler;
		this.last_send_time = System.currentTimeMillis();
		this.hostname       = hostname;
		this.waiting_packets = new LinkedBlockingQueue(maxWaitingPackets);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	public void close() {
		terminate = true;
		processPacket(null, null);
		closeAllConnections();
	}

	/**
	 * Method description
	 *
	 *
	 * @param bios
	 */
	public void disconnected(BoshIOService bios) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Disconnected called for: " + bios.getUniqueId());
		}
		if ((bios != null) && (bios.getWaitTimer() != null)) {
			handler.cancelTask(bios.getWaitTimer());
			connections.remove(bios.getWaitTimer());
		}
		if (inactivityTimer != null) {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER, getSid(),
																"Canceling inactivityTimer: " + bios != null ? bios.getUniqueId() : "n/a" } );
			}
		}
		if (connections.isEmpty()) {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER, getSid(),
																"Setting inactivityTimer for " + max_inactivity
																+ " on: " + bios != null ? bios.getUniqueId() : "n/a"} );
			}

			inactivityTimer = handler.scheduleTask(this, max_inactivity * SECOND);
		}
	}
	public void init(Packet packet, BoshIOService service, long max_wait, long min_polling,
			long max_inactivity, int concurrent_requests, int hold_requests, long max_pause,
			int max_batch_size, long batch_queue_timeout, Queue<Packet> out_results) {

		init( packet, service, max_wait, min_polling, max_inactivity,
					concurrent_requests, hold_requests, max_pause, max_batch_size,
					batch_queue_timeout, out_results, false );
	}


	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param service
	 * @param max_wait
	 * @param min_polling
	 * @param max_inactivity
	 * @param concurrent_requests
	 * @param hold_requests
	 * @param max_pause
	 * @param max_batch_size
	 * @param batch_queue_timeout
	 * @param out_results
	 */
	protected void init(Packet packet, BoshIOService service, long max_wait, long min_polling,
			long max_inactivity, int concurrent_requests, int hold_requests, long max_pause,
			int max_batch_size, long batch_queue_timeout, Queue<Packet> out_results,
			boolean preBindEnabled) {
		String cache_action = packet.getAttributeStaticStr(CACHE_ATTR);

		if ((cache_action != null) && cache_action.equals(CacheAction.on.toString())) {
			cache    = new BoshSessionCache();
			cache_on = true;
			log.fine("BoshSessionCache set to ON");
		}
		hashCodes   = new int[(this.concurrent_requests + 1) * 5];
		currentRids = new long[(this.concurrent_requests + 1) * 5];
		for (int i = 0; i < currentRids.length; i++) {
			currentRids[i] = -1;
			hashCodes[i]   = -1;
		}

		long   wait_l = max_wait;
		String wait_s = packet.getAttributeStaticStr(WAIT_ATTR);

		if (wait_s != null) {
			try {
				wait_l = Long.parseLong(wait_s);
			} catch (NumberFormatException e) {
				wait_l = max_wait;
			}
		}
		this.max_wait = Math.min(wait_l, max_wait);

		// this.max_wait = wait_l;
		int    hold_i  = hold_requests;
		String tmp_str = packet.getAttributeStaticStr(HOLD_ATTR);

		if (tmp_str != null) {
			try {
				hold_i = Integer.parseInt(tmp_str);
			} catch (NumberFormatException e) {
				hold_i = hold_requests;
			}
		}
		tmp_str = packet.getAttributeStaticStr(RID_ATTR);
		if (tmp_str != null) {
			try {
				previous_received_rid    = Long.parseLong(tmp_str);
				currentRids[rids_head++] = previous_received_rid;
			} catch (NumberFormatException e) {}
		}
		this.hold_requests = Math.max(hold_i, hold_requests);
		if (packet.getAttributeStaticStr(TO_ATTR) != null) {
			this.domain = packet.getAttributeStaticStr(TO_ATTR);
		}
		this.max_batch_size      = max_batch_size;
		this.batch_queue_timeout = batch_queue_timeout;
		this.min_polling         = min_polling;
		this.max_inactivity      = max_inactivity;
		this.concurrent_requests = concurrent_requests;
		this.max_pause           = max_pause;
		if (packet.getAttributeStaticStr(CONTENT_ATTR) != null) {
			content_type = packet.getAttributeStaticStr(CONTENT_ATTR);
		}

		String lang = packet.getAttributeStaticStr(LANG_ATTR);

		if (lang == null) {
			lang = "en";
		}

		Element body = new Element(BODY_EL_NAME, new String[] {
			WAIT_ATTR, INACTIVITY_ATTR, POLLING_ATTR, REQUESTS_ATTR, HOLD_ATTR, MAXPAUSE_ATTR,
			SID_ATTR, VER_ATTR, FROM_ATTR, SECURE_ATTR, "xmpp:version", "xmlns:xmpp",
			"xmlns:stream"
		}, new String[] {
			Long.toString(this.max_wait),
			Long.toString(this.max_inactivity),
			Long.toString(this.min_polling),
			Integer.toString(this.concurrent_requests),
			Integer.valueOf(this.hold_requests).toString(),
			Long.valueOf(this.max_pause).toString(), this.sid.toString(), BOSH_VERSION,
			this.domain, "true", "1.0", "urn:xmpp:xbosh", "http://etherx.jabber.org/streams"
		});
		
		if (this.hostname != null) {
			body.addAttribute(HOST_ATTR, this.hostname);
		}

		sessionId = UUID.randomUUID().toString();
		body.setAttribute(AUTHID_ATTR, sessionId);
		if (getCurrentRidTail() > 0) {
			body.setAttribute(ACK_ATTR, "" + takeCurrentRidTail());
		}
		JID userId = null;
		try {
			userId = (packet.getAttributeStaticStr(Packet.FROM_ATT) != null)
					? JID.jidInstance(packet.getAttributeStaticStr(Packet.FROM_ATT))
					: null;

			if (userId != null) {
				BareJID hostJid = handler.getSeeOtherHostForJID(packet, userId.getBareJID(), Phase.OPEN);

				if (hostJid != null) {
					Element error        = new Element("stream:error");
					Element seeOtherHost = handler.getSeeOtherHostError( packet, hostJid);

					seeOtherHost.setXMLNS("urn:ietf:params:xml:ns:xmpp-streams");
					error.addChild(seeOtherHost);
					body.addChild(error);
				}
			}
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(BoshSession.class.getName()).log(Level.SEVERE, null, ex);
		}
		body.setXMLNS(BOSH_XMLNS);
		// we are just doing session pre-bind, ignore sending back


		// service.writeRawData(body.toString());
		Packet streamOpen = Command.STREAM_OPENED.getPacket(handler.getJidForBoshSession(this),
				null, StanzaType.set, UUID.randomUUID().toString(), Command.DataType.submit);

		Command.addFieldValue(streamOpen, SESSION_ID_ATTR, sessionId);
		Command.addFieldValue(streamOpen, "hostname", domain);
		Command.addFieldValue(streamOpen, LANG_ATTR, lang);

		if (null != service ) {
			service.setContentType(content_type);
			sendBody(service, body);
		}
		if ( preBindEnabled ){

			inactivityTimer = handler.scheduleTask(this, max_inactivity * SECOND);
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																getSid(), "Setting inactivityTimer for " + max_inactivity
																+ " on " + service } );
			}

			
			if ( null != userId ){
				Command.addFieldValue( streamOpen, USER_ID_ATTR, userId.toString() );
			}
			String ridString = packet.getAttributeStaticStr( RID_ATTR );
			if ( null != ridString ){
				long rid = Long.valueOf( ridString );
				processRid( rid, null );
			}
			Command.addFieldValue( streamOpen, PRE_BIND_ATTR, String.valueOf( preBindEnabled ) );
		}

		handler.addOutStreamOpen(streamOpen, this);

		// out_results.offer(streamOpen);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param out_results
	 */
	public synchronized void processPacket(Packet packet, Queue<Packet> out_results) {
		if (packet != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("[" + connections.size() + "] Processing packet: " + packet
						.toString());
			}
			if (filterInPacket(packet)) {
				if (!waiting_packets.offer(packet.getElement())) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.INFO, "waiting_packets queue exceeded, dropping packet: " + packet.toString());
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("[" + connections.size() + "] In packet filtered: " + packet
							.toString());
				}
			}
		}
		if ((!connections.isEmpty()) && ((!waiting_packets.isEmpty()) || terminate)) {
			long currentTime = System.currentTimeMillis();

			if (terminate || (waiting_packets.size() >= max_batch_size) || (currentTime -
					last_send_time) > batch_queue_timeout) {
				Map.Entry<BoshTask, BoshIOService> entry = connections.pollFirstEntry();
				BoshIOService                      serv  = entry.getValue();

				sendBody(serv, null);
			} else {

				// @todo - change it to use value from settings
				if (queueTask == null) {
					queueTask = handler.scheduleSendQueueTask(this, batch_queue_timeout);
				}
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param service
	 * @param out_results
	 */
	public synchronized void processSocketPacket(Packet packet, BoshIOService service,
			Queue<Packet> out_results) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + connections.size() + "] Processing socket packet: " + packet
					.toString());
		}

		BoshTask waitTimer = service.getWaitTimer();

		if (waitTimer != null) {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																getSid(), "Canceling waitTimer: " + service.getUniqueId() } );
			}
			handler.cancelTask(waitTimer);
		}
		if (inactivityTimer != null) {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																getSid(), "Canceling inactivityTimer: " + service.getUniqueId() } );
			}
			handler.cancelTask(inactivityTimer);
		}
		if ((packet.getElemName() == BODY_EL_NAME) && (packet.getXMLNS() == BOSH_XMLNS)) {
			List<Element> children  = packet.getElemChildrenStaticStr(BODY_EL_PATH);
			boolean       duplicate = false;

			if (packet.getAttributeStaticStr(RID_ATTR) != null) {
				try {
					long rid = Long.parseLong(packet.getAttributeStaticStr(RID_ATTR));

					if (isDuplicateRid(rid, children)) {
						log.info("Discovered duplicate client connection, trying to close the " +
								"old one with RID: " + rid);

						Element body = getBodyElem();

						body.setAttribute("type", StanzaType.terminate.toString());
						sendBody(service, body);

						return;
					}
					service.setRid(rid);
					duplicate = isDuplicateMessage(rid, children);
					if (!duplicate) {
						processRid(rid, children);
					}
				} catch (NumberFormatException e) {
					log.warning("Incorrect RID value: " + packet.getAttributeStaticStr(RID_ATTR));
				}
			}
			service.setContentType(content_type);
			service.setSid(sid);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting waitTimer for " + max_wait + ": " + getSid());
			}
			waitTimer = handler.scheduleTask(this, max_wait * SECOND);
			service.setWaitTimer(waitTimer);
			connections.put(waitTimer, service);
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																getSid(), "Scheduling waitTimer: " + service.getUniqueId() } );
			}
			if (!duplicate) {
				if ((packet.getType() != null) && (packet.getType() == StanzaType.terminate)) {

					// We are preparing for session termination.
					// Some client send IQ stanzas with private data to store some
					// settings so some confirmation stanzas might be sent back
					// let's give the client a few secs for session termination
					max_inactivity = 2;    // Max pause changed to 2 secs
					terminate      = true;

					Packet command = Command.STREAM_CLOSED.getPacket(handler.getJidForBoshSession(this),
							getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
					if (userJid != null) {
						Command.addFieldValue(command, "user-jid", userJid.toString());
					}	
					handler.addOutStreamClosed(command, this, true);

					// out_results.offer(command);
				}
				if ((packet.getAttributeStaticStr(RESTART_ATTR) != null) && packet
						.getAttributeStaticStr(RESTART_ATTR).equals("true")) {
					log.fine("Found stream restart instruction: " + packet.toString());
					out_results.offer(Command.GETFEATURES.getPacket(null, null, StanzaType.get,
							"restart1", null));
				}
				if (packet.getAttributeStaticStr(CACHE_ATTR) != null) {
					try {
						CacheAction action = CacheAction.valueOf(packet.getAttributeStaticStr(
								CACHE_ATTR));

						if (cache_on || (action == CacheAction.on)) {
							processCache(action, packet);
						}
					} catch (IllegalArgumentException e) {
						log.warning("Incorrect cache action: " + packet.getAttributeStaticStr(
								CACHE_ATTR));
					}
				} else {
					if (children != null) {
						for (Element el : children) {
							try {
								if (el.getXMLNS().equals(BOSH_XMLNS)) {
									el.setXMLNS(XMLNS_CLIENT_VAL);
								}

								Packet result = Packet.packetInstance(el);

								if (filterOutPacket(result)) {
									if (log.isLoggable(Level.FINEST)) {
										log.finest("Sending out packet: " + result.toString());
									}
									out_results.offer(result);
								} else {
									if (log.isLoggable(Level.FINEST)) {
										log.finest("Out packet filtered: " + result.toString());
									}
								}
							} catch (TigaseStringprepException ex) {
								log.warning(
										"Packet addressing problem, stringprep processing failed, dropping: " +
										el);
							}
						}
					}
				}
				if (terminate) {
					Packet command = Command.STREAM_CLOSED.getPacket(handler.getJidForBoshSession(this),
							getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
					if (userJid != null) {
						Command.addFieldValue(command, "user-jid", userJid.toString());
					}
					handler.addOutStreamClosed(command, this, true);
				}
			} else {
				log.info("Duplicated packet: " + packet.toString());
			}
		} else {
			log.warning("[" + connections.size() + "] Unexpected packet from the network: " +
					packet.toString());

			String er_msg = "Invalid body element";

			if (packet.getElemName() != BODY_EL_NAME) {
				er_msg += ", incorrect root element name, use " + BODY_EL_NAME;
			}
			if (packet.getXMLNS() != BOSH_XMLNS) {
				er_msg += ", incorrect xmlns, use " + BOSH_XMLNS;
			}
			try {
				Packet error = Authorization.BAD_REQUEST.getResponseMessage(packet, er_msg, true);

				if (!waiting_packets.offer(error.getElement())) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.INFO, "waiting_packets queue exceeded, dropping packet: " + error.toString());
				}
				
				terminate = true;

				Packet command = Command.STREAM_CLOSED.getPacket(handler.getJidForBoshSession(this),
						getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
				if (userJid != null) {
					Command.addFieldValue(command, "user-jid", userJid.toString());
				}	
				handler.addOutStreamClosed(command, this, true);

				command = Command.STREAM_FINISHED.getPacket(handler.getJidForBoshSession(this),
						getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
				if (userJid != null) {
					Command.addFieldValue(command, "user-jid", userJid.toString());
				}	
				handler.addOutStreamClosed(command, this, false);
				
				// out_results.offer(command);
			} catch (PacketErrorTypeException e) {
				log.info("Error type and incorrect from bosh client? Ignoring...");
			}
		}

		// Send packets waiting in queue...
		processPacket(null, out_results);
		if (connections.size() > hold_requests) {
			BoshIOService serv = connections.pollFirstEntry().getValue();

			sendBody(serv, null);
		}
	}

	/**
	 * Method description
	 *
	 */
	public synchronized void sendWaitingPackets() {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("trying to send waiting packets from queue of " + getSid() +
					" after timer = " + waiting_packets.size());
		}
		if (!waiting_packets.isEmpty()) {
			Map.Entry<BoshTask, BoshIOService> entry = connections.pollFirstEntry();

			if (entry == null) {
				return;
			}

			BoshIOService serv = entry.getValue();

			sendBody(serv, null);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param out_results
	 * @param tt
	 *
	 * 
	 */
	public boolean task(Queue<Packet> out_results, TimerTask tt) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "task called for {0}, inactivityTimer = {1}, tt = {2}", new Object[] { getSid(), inactivityTimer, tt });
		}
		if (tt == inactivityTimer) {
			if (connections.size() > 0) {
				if (log.isLoggable(Level.FINEST)) {
					String conns = "";
					for (BoshIOService serv : connections.values()) {
						if (!conns.isEmpty()) {
							conns += ", ";
						}
						conns += "[" + serv.toString() + "]";
					}

					if ( log.isLoggable( Level.FINEST ) ){
						log.log( Level.FINEST, "{0} : {1} ({2})",
										 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																		getSid(), "ignoring inactivityTimer" } );
					}
				}
				return false;
			}
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																getSid(), "inactivityTimer fired" } );
			}
			for (BoshTask waitTimer : waitTimerSet) {
				if (waitTimer != null) {
			if ( log.isLoggable( Level.FINEST ) ){
						log.log( Level.FINEST, "{0} : {1} ({2})",
										 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.TIMER,
																		getSid(), "Canceling waitTimer" } );
					}

					handler.cancelTask(waitTimer);
				}
			}
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "{0} : {1} ({2})",
								 new Object[] { BoshConnectionManager.BOSH_OPERATION_TYPE.REMOVE,
																getSid(), "Closing session, inactivity timeout expired" } );
			}
			// we need to set packetFrom as it is later used as packet from and to
			// pick thread on which it will be processed
			Packet command = Command.STREAM_CLOSED.getPacket(handler.getJidForBoshSession(this), 
					getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
			if (userJid != null) {
				Command.addFieldValue(command, "user-jid", userJid.toString());
			}			

			handler.addOutStreamClosed(command, this, true);

			for (Element packet : waiting_packets) {
				try {

					// Do not send stream:features back with an error
					if (packet.getName() != "stream:features") {
//						out_results.offer(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(
//								Packet.packetInstance(packet), "Bosh = disconnected", true));
						Packet p = Packet.packetInstance(packet);
						// we need to set packetTo as it is later used as packet from and to
						// pick thread on which it will be processed
						p.setPacketTo(handler.getJidForBoshSession(this));
						p.setPacketFrom(getDataReceiver());
						handler.processUndeliveredPacket(p, null, "Bosh = disconnected");
					}
				} catch (TigaseStringprepException ex) {
					log.warning(
							"Packet addressing problem, stringprep processing failed, dropping: " +
							packet);
//				} catch (PacketErrorTypeException e) {
//					log.info("Packet processing exception: " + e);
				}
			}
			
			command = Command.STREAM_FINISHED.getPacket(handler.getJidForBoshSession(this), 
					getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
			if (userJid != null) {
				Command.addFieldValue(command, "user-jid", userJid.toString());
			}			

			handler.addOutStreamClosed(command, this, false);
			
			closeAllConnections();

			// out_results.offer(command);
			return true;
		}

		BoshIOService serv = connections.remove(tt);

		if (serv != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("waitTimer fired: " + getSid());
			}
			sendBody(serv, null);
		}

		return false;
	}

	/**
	 * Method description
	 *
	 */
	public void terminateBoshSession() {
		terminate = true;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public JID getDataReceiver() {
		return dataReceiver;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public UUID getSid() {
		return sid;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param dataReceiver
	 */
	public void setDataReceiver(JID dataReceiver) {
		this.dataReceiver = dataReceiver;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 */
	public void setUserJid(String jid) {
		userJid = JID.jidInstanceNS(jid);
	}

	//~--- methods --------------------------------------------------------------

	private Element applyFilters(Element packet) {
		Element result = packet.clone();

		if (result.getName() == MESSAGE_ELEMENT_NAME) {
			String body = result.getCDataStaticStr(Message.MESSAGE_BODY_PATH);

			if (body != null) {
				int count = 0;

				// for (Pattern reg: links_regexs) {
				// body = reg.matcher(body).replaceAll(replace_with[count++]);
				// }
				result.getChild("body").setCData(body);
			}
		}

		return result;
	}

	private void closeAllConnections() {
		for (BoshIOService conn : old_connections) {
			conn.stop();
		}
		for (BoshIOService conn : connections.values()) {
			conn.stop();
		}
	}

	private boolean filterInPacket(Packet packet) {
		if (cache_on) {
			processAutomaticCache(packet);
		}

		return true;
	}

	private boolean filterOutPacket(Packet packet) {
		if (cache_on && (packet.getElemName() == MESSAGE_ELEMENT_NAME)) {
			cache.addToMessage(packet.getElement());
		}

		return true;
	}

	private void processAutomaticCache(Packet packet) {
		if (packet.getElemName() == PRESENCE_ELEMENT_NAME) {
			cache.addPresence(packet.getElement());
		}
		if (packet.getElemName() == MESSAGE_ELEMENT_NAME) {
			cache.addFromMessage(packet.getElement());
		}
		if (packet.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, "jabber:iq:roster")) {
			cache.addRoster(packet.getElement());
		}
		if (packet.isXMLNSStaticStr(Iq.IQ_BIND_PATH, "urn:ietf:params:xml:ns:xmpp-bind")) {
			cache.set(BoshSessionCache.RESOURCE_BIND_ID, Collections.singletonList(packet
					.getElement()));
		}
	}

	private void processCache(CacheAction action, Packet packet) {
		++cache_reload_counter;

		int           packet_counter = 0;
		List<Element> children       = packet.getElemChildrenStaticStr(BODY_EL_PATH);
		String        cache_id       = packet.getAttributeStaticStr(CACHE_ID_ATTR);
		List<Element> cache_res      = null;

		switch (action) {
		case on :
			if (cache == null) {
				cache = new BoshSessionCache();
			}
			cache_on = true;
			log.fine("BoshSessionCache set to ON");

			break;

		case off :
			cache_on = false;
			log.fine("BoshSessionCache set to OFF");

			break;

		case set :
			cache.set(cache_id, children);

			break;

		case add :
			cache.add(cache_id, children);

			break;

		case get :
			cache_res = cache.get(cache_id);

			break;

		case remove :
			cache.remove(cache_id);

			break;

		case get_all :
			cache_res = cache.getAll();
			retireAllOldConnections();

			break;

		default :
			log.warning("Unknown cache action: " + action.toString());

			break;
		}
		if (cache_res != null) {
			for (Element elem : cache_res) {
				elem.addAttribute("reload-counter", "" + cache_reload_counter);
				elem.addAttribute("packet-counter", "" + (++packet_counter));
				if (!waiting_packets.offer(elem)) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.INFO, "waiting_packets queue exceeded, dropping packet: " + elem.toString());
				}

			}
		}
	}

	private void processRid(long rid, List<Element> packets) {
		synchronized (currentRids) {
			if ((previous_received_rid + 1) != rid) {
				log.log(Level.FINER, "Incorrect packet order, last_rid={0}, current_rid={1}",
						new Object[] { previous_received_rid,
						rid });
			}
			if ((packets != null) && (!packets.isEmpty())) {
				StringBuilder sb = new StringBuilder();

				for (Element elem : packets) {
					sb.append(elem.toString());
				}
				hashCodes[rids_head] = sb.toString().hashCode();
			} else {
				hashCodes[rids_head] = -1;
			}
			previous_received_rid    = rid;
			currentRids[rids_head++] = rid;
			if (rids_head >= currentRids.length) {
				rids_head = 0;
			}
		}
	}

	private void retireAllOldConnections() {
		while (connections.size() > 1) {
			Map.Entry<BoshTask, BoshIOService> entry = connections.pollFirstEntry();

			handler.cancelTask(entry.getKey());

			BoshIOService serv = entry.getValue();

			if (serv != null) {
				retireConnectionService(serv);
			} else {
				if (log.isLoggable(Level.WARNING)) {
					log.warning("connections queue size is greater than 1 but poll returns null" +
							getSid());
				}
			}
		}
	}

	private void retireConnectionService(BoshIOService serv) {
		if (!old_connections.contains(serv)) {
			while (!old_connections.offer(serv)) {
				BoshIOService old_serv = old_connections.poll();

				if (old_serv != null) {
					old_serv.stop();
				} else {
					if (log.isLoggable(Level.WARNING)) {
						log.warning("old_connections queue is empty but can not add new element!: " +
								getSid());
					}

					break;
				}
			}
		}
		serv.setSid(null);
		disconnected(serv);
	}

	private synchronized void sendBody(BoshIOService serv, Element body_par) {
		if (queueTask != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Canceling queue timer: " + getSid());
			}
			handler.cancelSendQueueTask(queueTask);
			queueTask = null;
		}
		last_send_time = System.currentTimeMillis();

		BoshTask timer = serv.getWaitTimer();

		if (timer != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Canceling waitTimer: " + getSid());
			}
			handler.cancelTask(timer);
		} else {
			log.fine("No waitTimer for the Bosh connection! " + serv);
		}

		Element body = body_par;

		if (body == null) {
			body = getBodyElem();

			long rid = takeCurrentRidTail();

			if (rid > 0) {
				body.setAttribute(ACK_ATTR, "" + rid);
			}
			if (!waiting_packets.isEmpty()) {

				// body.addChild(applyFilters(waiting_packets.poll()));
				// Make sure the XMLNS is set correctly for all stanzas to avoid
				// namespace confusion:
				// http://forum.ag-software.de/thread/969
				Element stanza = waiting_packets.poll();

				if (stanza.getXMLNS() == null) {
					stanza.setXMLNS(XMLNS_CLIENT_VAL);
				}
				body.addChild(stanza);
				while ((!waiting_packets.isEmpty()) && (body.getChildren().size() <
						max_batch_size)) {

					// body.addChild(applyFilters(waiting_packets.poll()));
					stanza = waiting_packets.poll();
					if (stanza.getXMLNS() == null) {
						stanza.setXMLNS(XMLNS_CLIENT_VAL);
					}
					body.addChild(stanza);
				}
			}
		}

		if ( body.getChild( "stream:error" ) != null ){
			body.addAttribute( "condition", "remote-stream-error" );
			body.addAttribute( "type", "terminate" );
			body.addAttribute( "xmlns:stream", "http://etherx.jabber.org/streams" );
			this.terminate = true;
		}

		try {
			if (terminate) {
				body.setAttribute("type", StanzaType.terminate.toString());
			}
			handler.writeRawData(serv, body.toString());
			retireConnectionService(serv);

		} catch (Exception e) {
			log.log(Level.WARNING, "[" + connections.size() +
					"] Exception during writing to socket", e);
		}
	}

	private long takeCurrentRidTail() {
		synchronized (currentRids) {
			int idx = rids_tail++;

			if (rids_tail >= currentRids.length) {
				rids_tail = 0;
			}

			return currentRids[idx];
		}
	}

	//~--- get methods ----------------------------------------------------------

	private Element getBodyElem() {
		Element body = new Element(BODY_EL_NAME, new String[] { FROM_ATTR, SECURE_ATTR,
				"xmpp:version", "xmlns:xmpp", "xmlns:stream" }, new String[] { this.domain,
				"true", "1.0", "urn:xmpp:xbosh", "http://etherx.jabber.org/streams" });

		if (this.hostname != null) {
			body.addAttribute(HOST_ATTR, this.hostname);
		}
		
		body.setXMLNS(BOSH_XMLNS);

		return body;
	}

	private long getCurrentRidTail() {
		synchronized (currentRids) {
			return currentRids[rids_tail];
		}
	}

	private boolean isDuplicateMessage(long rid, List<Element> packets) {
		synchronized (currentRids) {
			int hashCode = -1;

			if ((packets != null) && (!packets.isEmpty())) {
				StringBuilder sb = new StringBuilder();

				for (Element elem : packets) {
					sb.append(elem.toString());
				}
				hashCode = sb.toString().hashCode();
			}
			if (hashCode == -1) {
				return false;
			}
			for (int i = 0; i < currentRids.length; ++i) {
				if (rid == currentRids[i]) {
					return hashCode == hashCodes[i];
				}
			}
		}

		return false;
	}

	private boolean isDuplicateRid(long rid, List<Element> packets) {
		synchronized (currentRids) {
			int hashCode = -1;

			if ((packets != null) && (!packets.isEmpty())) {
				StringBuilder sb = new StringBuilder();

				for (Element elem : packets) {
					sb.append(elem.toString());
				}
				hashCode = sb.toString().hashCode();
			}
			for (int i = 0; i < currentRids.length; ++i) {
				if (rid == currentRids[i]) {
					return hashCode != hashCodes[i];
				}
			}
		}

		return false;
	}

	//~--- inner classes --------------------------------------------------------

	private static class TimerTaskComparator
					implements Comparator<BoshTask> {
		@Override
		public int compare(BoshTask o1, BoshTask o2) {
			if (o1.timerOrder > o2.timerOrder) {
				return 1;
			}
			if (o1.timerOrder < o2.timerOrder) {
				return -1;
			}

			return 0;
		}
	}
}
