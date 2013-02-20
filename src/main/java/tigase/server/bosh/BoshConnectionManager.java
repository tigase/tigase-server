/*
 * BoshConnectionManager.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
import tigase.server.Packet;
import tigase.server.ReceiverTimeoutHandler;
import tigase.server.xmppclient.ClientConnectionManager;

import tigase.stats.StatisticsList;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;

import static tigase.server.bosh.Constants.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Describe class BoshConnectionManager here.
 *
 *
 * Created: Sat Jun 2 12:24:29 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshConnectionManager
				extends ClientConnectionManager
				implements BoshSessionTaskHandler {
	private static final int DEF_PORT_NO = 5280;

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log =
		Logger.getLogger(BoshConnectionManager.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int[] PORTS      = { DEF_PORT_NO };
	private long min_polling = MIN_POLLING_PROP_VAL;

	// private static final String HOSTNAMES_PROP_KEY = "hostnames";
	// private String[] HOSTNAMES_PROP_VAL = {"localhost", "hostname"};
	// private RoutingsContainer routings = null;
	// private Set<String> hostnames = new TreeSet<String>();
	private long max_wait                         = MAX_WAIT_DEF_PROP_VAL;
	private long max_pause                        = MAX_PAUSE_PROP_VAL;
	private long max_inactivity                   = MAX_INACTIVITY_PROP_VAL;
	private int hold_requests                     = HOLD_REQUESTS_PROP_VAL;
	private int concurrent_requests               = CONCURRENT_REQUESTS_PROP_VAL;
	private ReceiverTimeoutHandler stoppedHandler = newStoppedHandler();
	private ReceiverTimeoutHandler startedHandler = newStartedHandler();
	private int max_batch_size                    = MAX_BATCH_SIZE_VAL;
	private long bosh_session_close_delay         = BOSH_SESSION_CLOSE_DELAY_DEF_VAL;
	private long batch_queue_timeout              = BATCH_QUEUE_TIMEOUT_VAL;

	// This should be actually a multi-thread save variable.
	// Changing it to

	/** Field description */
	protected final Map<UUID, BoshSession> sessions = new ConcurrentSkipListMap<UUID,
																											BoshSession>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet.toString());
		}
		super.processPacket(packet);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param bs
	 *
	 * @return
	 */
	@Override
	public boolean addOutStreamClosed(Packet packet, BoshSession bs) {
		packet.setPacketFrom(getFromAddress(bs.getSid().toString()));
		packet.setPacketTo(bs.getDataReceiver());
		packet.initVars(packet.getPacketFrom(), packet.getPacketTo());
		bs.close();
		if (log.isLoggable(Level.FINEST)) {
			log.finest("closing BOSH session with sid = " + bs.getSid().toString());
		}
		sessions.remove(bs.getSid());

		return addOutPacketWithTimeout(packet, stoppedHandler, 15l, TimeUnit.SECONDS);
	}

	/**
	 *
	 * @param packet
	 * @param bs
	 * @return
	 */
	@Override
	public boolean addOutStreamOpen(Packet packet, BoshSession bs) {
		packet.initVars(getFromAddress(bs.getSid().toString()), bs.getDataReceiver());

		return addOutPacketWithTimeout(packet, startedHandler, 15l, TimeUnit.SECONDS);
	}

	/**
	 * Method description
	 *
	 *
	 * @param tt
	 */
	@Override
	public void cancelTask(BoshTask tt) {
		tt.cancel();
	}

	/**
	 * Method description
	 *
	 *
	 * @param tt
	 */
	@Override
	public void cancelSendQueueTask(BoshSendQueueTask tt) {
		tt.cancel();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);

		props.put(MAX_WAIT_DEF_PROP_KEY, MAX_WAIT_DEF_PROP_VAL);
		props.put(MIN_POLLING_PROP_KEY, MIN_POLLING_PROP_VAL);
		props.put(MAX_INACTIVITY_PROP_KEY, MAX_INACTIVITY_PROP_VAL);
		props.put(CONCURRENT_REQUESTS_PROP_KEY, CONCURRENT_REQUESTS_PROP_VAL);
		props.put(HOLD_REQUESTS_PROP_KEY, HOLD_REQUESTS_PROP_VAL);
		props.put(MAX_PAUSE_PROP_KEY, MAX_PAUSE_PROP_VAL);
		props.put(MAX_BATCH_SIZE_KEY, MAX_BATCH_SIZE_VAL);
		props.put(BATCH_QUEUE_TIMEOUT_KEY, BATCH_QUEUE_TIMEOUT_VAL);

		return props;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoCategoryType() {
		return "c2s";
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return "Bosh connection manager";
	}

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param srv
	 *
	 * @return
	 */
	@Override
	public Queue<Packet> processSocketData(XMPPIOService<Object> srv) {
		BoshIOService serv = (BoshIOService) srv;
		Packet p           = null;

		while ((p = serv.getReceivedPackets().poll()) != null) {
			Queue<Packet> out_results = new ArrayDeque<Packet>(2);
			BoshSession bs            = null;
			String sid_str            = null;

			synchronized (sessions) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Processing packet: {0}, type: {1}",
									new Object[] { p.getElemName(),
																 p.getType() });
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Processing socket data: {0}", p);
				}
				sid_str = p.getAttribute(SID_ATTR);

				UUID sid = null;

				if (sid_str == null) {
					String hostname = p.getAttribute("to");

					if ((hostname != null) && isLocalDomain(hostname)) {
						bs = new BoshSession(getDefVHostItem().getDomain(),
																 JID.jidInstanceNS(routings.computeRouting(hostname)),
																 this);
						sid = bs.getSid();
						sessions.put(sid, bs);
					} else {
						log.info("Invalid hostname. Closing invalid connection");
						try {
							serv.sendErrorAndStop(Authorization.NOT_ALLOWED, p, "Invalid hostname.");
						} catch (Exception e) {
							log.log(Level.WARNING,
											"Problem sending invalid hostname error for sid =  " + sid, e);
						}
					}
				} else {
					sid = UUID.fromString(sid_str);
					bs  = sessions.get(sid);
				}
			}
			try {
				if (bs != null) {
					synchronized (bs) {
						if (sid_str == null) {
							bs.init(p, serv, max_wait, min_polling, max_inactivity,
											concurrent_requests, hold_requests, max_pause, max_batch_size,
											batch_queue_timeout, out_results);
						} else {
							bs.processSocketPacket(p, serv, out_results);
						}
					}
				} else {
					log.info("There is no session with given SID. Closing invalid connection");
					serv.sendErrorAndStop(Authorization.ITEM_NOT_FOUND, p, "Invalid SID");
				}
				addOutPackets(out_results, bs);
			} catch (Exception e) {
				log.log(Level.WARNING, "Problem processing socket data for sid =  " + sid_str, e);
			}

			// addOutPackets(out_results);
		}    // end of while ()

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param bs
	 * @param delay
	 *
	 * @return
	 */
	@Override
	public BoshTask scheduleTask(BoshSession bs, long delay) {
		BoshTask bt = new BoshTask(bs, this);

		addTimerTask(bt, delay);

		// boshTasks.schedule(bt, delay);
		return bt;
	}

	/**
	 * Method description
	 *
	 *
	 * @param bs
	 * @param delay
	 *
	 * @return
	 */
	@Override
	public BoshSendQueueTask scheduleSendQueueTask(final BoshSession bs, long delay) {
		BoshSendQueueTask bt = new BoshSendQueueTask(bs);

		addTimerTask(bt, delay);

		// boshTasks.schedule(bt, delay);
		return bt;
	}

	/**
	 * Method description
	 *
	 *
	 * @param service
	 */
	public void serviceStarted(BoshIOService service) {
		super.serviceStarted(service);
	}

	/**
	 * Method description
	 *
	 *
	 * @param service
	 */
	public void serviceStopped(BoshIOService service) {
		super.serviceStopped(service);

		UUID sid = service.getSid();

		if (sid != null) {
			BoshSession bs = sessions.get(sid);

			if (bs != null) {
				bs.disconnected(service);
			}
		}
	}

	//~--- set methods ----------------------------------------------------------

	// ~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		if (props.get(MAX_WAIT_DEF_PROP_KEY) != null) {
			max_wait = (Long) props.get(MAX_WAIT_DEF_PROP_KEY);
			log.info("Setting max_wait to: " + max_wait);
		}
		if (props.get(MIN_POLLING_PROP_KEY) != null) {
			min_polling = (Long) props.get(MIN_POLLING_PROP_KEY);
			log.info("Setting min_polling to: " + min_polling);
		}
		if (props.get(MAX_INACTIVITY_PROP_KEY) != null) {
			max_inactivity = (Long) props.get(MAX_INACTIVITY_PROP_KEY);
			log.info("Setting max_inactivity to: " + max_inactivity);
		}
		if (props.get(CONCURRENT_REQUESTS_PROP_KEY) != null) {
			concurrent_requests = (Integer) props.get(CONCURRENT_REQUESTS_PROP_KEY);
			log.info("Setting concurrent_requests to: " + concurrent_requests);
		}
		if (props.get(HOLD_REQUESTS_PROP_KEY) != null) {
			hold_requests = (Integer) props.get(HOLD_REQUESTS_PROP_KEY);
			log.info("Setting hold_requests to: " + hold_requests);
		}
		if (props.get(MAX_PAUSE_PROP_KEY) != null) {
			max_pause = (Long) props.get(MAX_PAUSE_PROP_KEY);
			log.info("Setting max_pause to: " + max_pause);
		}
		if (props.get(MAX_BATCH_SIZE_KEY) != null) {
			max_batch_size = (Integer) props.get(MAX_BATCH_SIZE_KEY);
			log.info("Setting max_batch_size to: " + max_batch_size);
		}
		if (props.get(BATCH_QUEUE_TIMEOUT_KEY) != null) {
			batch_queue_timeout = (Long) props.get(BATCH_QUEUE_TIMEOUT_KEY);
			log.info("Setting batch_queue_timeout to: " + batch_queue_timeout);
		}
	}

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param ios
	 * @param data
	 */
	@Override
	public void writeRawData(BoshIOService ios, String data) {
		super.writeRawData(ios, data);
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void xmppStreamClosed(BoshIOService serv) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream closed.");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param attribs
	 *
	 * @return
	 */
	public String xmppStreamOpened(BoshIOService serv, Map<String, String> attribs) {
		if (log.isLoggable(Level.FINE)) {
			log.fine(
					"Ups, what just happened? Stream open. Hey, this is a Bosh connection manager." +
					" c2s and s2s are not supported on the same port as Bosh yet.");
		}

		return "<?xml version='1.0'?><stream:stream" + " xmlns='jabber:client'" +
					 " xmlns:stream='http://etherx.jabber.org/streams'" + " id='1'" + " from='" +
					 getDefVHostItem() + "'" + " version='1.0' xml:lang='en'>" + "<stream:error>" +
					 "<invalid-namespace xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
					 "<text xmlns='urn:ietf:params:xml:ns:xmpp-streams' xml:lang='langcode'>" +
					 "Ups, what just happened? Stream open. Hey, this is a Bosh connection manager. " +
					 "c2s and s2s are not supported on the same port... yet." + "</text>" +
					 "</stream:error>" + "</stream:stream>";
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param fromJID
	 *
	 * @return
	 */
	@Override
	public BareJID getSeeOtherHostForJID(BareJID fromJID) {
		if (see_other_host_strategy == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("no see-other-host implementation set");
			}

			return null;
		}

		BareJID see_other_host = see_other_host_strategy.findHostForJID(fromJID,
															 getDefHostName());

		if (log.isLoggable(Level.FINEST)) {
			log.finest("using = " + see_other_host_strategy.getClass().getCanonicalName() +
								 "for jid = " + fromJID.toString() + " got = " + ((see_other_host != null)
							? see_other_host.toString()
							: "null"));
		}

		return ((see_other_host != null) &&!see_other_host.equals(getDefHostName()))
					 ? see_other_host
					 : null;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param newAddress
	 * @param command_sessionId
	 * @param serv
	 *
	 * @return
	 */
	@Override
	protected JID changeDataReceiver(Packet packet, JID newAddress,
																	 String command_sessionId, XMPPIOService<Object> serv) {
		BoshSession session = getBoshSession(packet.getTo());

		if (session != null) {
			String sessionId = session.getSessionId();

			if (sessionId.equals(command_sessionId)) {
				JID old_receiver = session.getDataReceiver();

				session.setDataReceiver(newAddress);

				return old_receiver;
			} else {
				log.info("Incorrect session ID, ignoring data redirect for: " + newAddress);
			}
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods ----------------------------------------------------------
	// public void processPacket(Packet packet) {
	// log.finer("Processing packet: " + packet.getElemName()
	// + ", type: " + packet.getType());
	// log.finest("Processing packet: " + packet.toString());
	// if (packet.isCommand() && packet.getCommand() != Command.OTHER) {
	// processCommand(packet);
	// } else {
	// writePacketToSocket(packet);
	// }
	// }

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	protected BoshSession getBoshSession(JID jid) {
		UUID sid = UUID.fromString(jid.getResource());

		return sessions.get(sid);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected int[] getDefPlainPorts() {
		return PORTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected int[] getDefSSLPorts() {
		return null;
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time for
	 * inactive connection. For Bosh it does not make sense to keep the idle
	 * connection longer than 10 minutes.
	 *
	 * @return a <code>long</code> value
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 10 * MINUTE;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected BoshIOService getXMPPIOServiceInstance() {
		return new BoshIOService();
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		if (list.checkLevel(Level.FINEST)) {

			// Be careful here, the size() for this map is expensive to count
			list.add(getName(), "Bosh sessions", sessions.size(), Level.FINEST);
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected ReceiverTimeoutHandler newStartedHandler() {
		return new StartedHandler();
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	protected void processCommand(Packet packet) {
		BoshSession session = getBoshSession(packet.getTo());

		switch (packet.getCommand()) {
		case CLOSE :
			if (session != null) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Closing session for command CLOSE: {0}",
									session.getSid());
				}
				if (bosh_session_close_delay > 0) {
					try {
						Thread.sleep(bosh_session_close_delay);
					} catch (InterruptedException ex) {

						// Intentionally left blank
					}
				}
				session.close();
				sessions.remove(session.getSid());
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Session does not exist for packet: {0}", packet)
					;
				}
			}

			break;

		case CHECK_USER_CONNECTION :
			if (session != null) {

				// It's ok, the session has been found, respond with OK.
				addOutPacket(packet.okResult((String) null, 0));
			} else {

				// Session is no longer active, respond with an error.
				try {
					addOutPacket(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
									"Connection gone.", false));
				} catch (PacketErrorTypeException e) {

					// Hm, error already, ignoring...
					log.log(Level.INFO, "Error packet is not really expected here: {0}", packet);
				}
			}

			break;

		default :
			super.processCommand(packet);

			break;
		}    // end of switch (pc.getCommand())
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 */
	@Override
	protected boolean writePacketToSocket(Packet packet) {
		BoshSession session = getBoshSession(packet.getTo());

		if (session != null) {
			synchronized (session) {
				Queue<Packet> out_results = new ArrayDeque<Packet>();

				session.processPacket(packet, out_results);
				addOutPackets(out_results, session);
			}

			return true;
		} else {
			log.info("Session does not exist for packet: " + packet.toString());

			return false;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param out_results
	 * @param bs
	 */
	protected void addOutPackets(Queue<Packet> out_results, BoshSession bs) {
		for (Packet res : out_results) {
			res.setPacketFrom(getFromAddress(bs.getSid().toString()));
			res.setPacketTo(bs.getDataReceiver());
			if (res.getCommand() != null) {
				switch (res.getCommand()) {
				case STREAM_CLOSED :
				case GETFEATURES :
					res.initVars(res.getPacketFrom(), res.getPacketTo());

					break;

				default :

				// Do nothing...
				}
			}
			addOutPacket(res);
		}
		out_results.clear();
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods ----------------------------------------------------------
	private JID getFromAddress(String id) {
		return JID.jidInstanceNS(getName(), getDefHostName().getDomain(), id);
	}

	//~--- inner classes --------------------------------------------------------

	// ~--- inner classes --------------------------------------------------------
	private class StartedHandler
					implements ReceiverTimeoutHandler {
		/**
		 * Method description
		 *
		 *
		 * @param packet
		 * @param response
		 */
		@Override
		public void responseReceived(Packet packet, Packet response) {

			// We are now ready to ask for features....
			addOutPacket(Command.GETFEATURES.getPacket(packet.getFrom(), packet.getTo(),
							StanzaType.get, UUID.randomUUID().toString(), null));
		}

		/**
		 * Method description
		 *
		 *
		 * @param packet
		 */
		@Override
		public void timeOutExpired(Packet packet) {

			// If we still haven't received confirmation from the SM then
			// the packet either has been lost or the server is overloaded
			// In either case we disconnect the connection.
			log.warning("No response within time limit received for a packet: " +
									packet.toString());

			BoshSession session = getBoshSession(packet.getFrom());

			if (session != null) {
				log.fine("Closing session for timeout: " + session.getSid());
				session.close();
				sessions.remove(session.getSid());
			} else {
				log.info("Session does not exist for packet: " + packet.toString());
			}
		}
	}
}



// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com


//~ Formatted in Tigase Code Convention on 13/02/16
