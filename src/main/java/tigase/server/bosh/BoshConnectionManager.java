/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.bosh;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.ReceiverEventHandler;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPIOService;
import tigase.server.xmppclient.ClientConnectionManager;

import tigase.xmpp.PacketErrorTypeException;
import static tigase.server.bosh.Constants.*;

/**
 * Describe class BoshConnectionManager here.
 *
 *
 * Created: Sat Jun  2 12:24:29 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshConnectionManager extends ClientConnectionManager
	implements BoshSessionTaskHandler {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.bosh.BoshConnectionManager");

	private static final String ROUTINGS_PROP_KEY = "routings";
	private static final String ROUTING_MODE_PROP_KEY = "multi-mode";
	private static final boolean ROUTING_MODE_PROP_VAL = true;
	private static final String ROUTING_ENTRY_PROP_KEY = ".+";
	private static final String ROUTING_ENTRY_PROP_VAL = DEF_SM_NAME + "@localhost";

	private static final int DEF_PORT_NO = 5280;
	private int[] PORTS = {DEF_PORT_NO};
// 	private static final String HOSTNAMES_PROP_KEY = "hostnames";
// 	private String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

//	private RoutingsContainer routings = null;
// 	private Set<String> hostnames = new TreeSet<String>();
	private long max_wait = MAX_WAIT_DEF_PROP_VAL;
	private long min_polling = MIN_POLLING_PROP_VAL;
	private long max_inactivity = MAX_INACTIVITY_PROP_VAL;
	private int concurrent_requests = CONCURRENT_REQUESTS_PROP_VAL;
	private int hold_requests = HOLD_REQUESTS_PROP_VAL;
	private long max_pause = MAX_PAUSE_PROP_VAL;
	private ReceiverEventHandler stoppedHandler = newStoppedHandler();
	private ReceiverEventHandler startedHandler = newStartedHandler();

	private final Map<UUID, BoshSession> sessions =
		new LinkedHashMap<UUID, BoshSession>();

// 	public void processPacket(Packet packet) {
// 		log.finer("Processing packet: " + packet.getElemName()
// 			+ ", type: " + packet.getType());
// 		log.finest("Processing packet: " + packet.toString());
// 		if (packet.isCommand() && packet.getCommand() != Command.OTHER) {
// 			processCommand(packet);
// 		} else {
// 			writePacketToSocket(packet);
// 		}
// 	}

	protected BoshSession getBoshSession(String jid) {
		UUID sid = UUID.fromString(JIDUtils.getNodeResource(jid));
		return sessions.get(sid);
	}

	@Override
	protected boolean writePacketToSocket(Packet packet) {
		BoshSession session = getBoshSession(packet.getTo());
		if (session != null) {
			synchronized (session) {
				Queue<Packet> out_results = new LinkedList<Packet>();
				session.processPacket(packet, out_results);
				addOutPackets(out_results, session);
			}
			return true;
		} else {
			log.info("Session does not exist for packet: " + packet.toString());
			return false;
		}
	}

	@Override
	protected void processCommand(Packet packet) {
		BoshSession session = getBoshSession(packet.getTo());
		switch (packet.getCommand()) {
			case CLOSE:
				if (session != null) {
					log.fine("Closing session for command CLOSE: " + session.getSid());
					session.close();
					sessions.remove(session.getSid());
				} else {
					log.info("Session does not exist for packet: " + packet.toString());
				}
				break;
			case CHECK_USER_CONNECTION:
				if (session != null) {
					// It's ok, the session has been found, respond with OK.
					addOutPacket(packet.okResult((String)null, 0));
				} else {
					// Session is no longer active, respond with an error.
					try {
						addOutPacket(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
										"Connection gone.", false));
					} catch (PacketErrorTypeException e) {
						// Hm, error already, ignoring...
						log.info("Error packet is not really expected here: " +
										packet.toString());
					}
				}
				break;
		default:
			super.processCommand(packet);
			break;
		} // end of switch (pc.getCommand())
	}

	@Override
	protected String changeDataReceiver(Packet packet, String newAddress,
		String command_sessionId, XMPPIOService<Object> serv) {
		BoshSession session = getBoshSession(packet.getTo());
		if (session != null) {
			String sessionId = session.getSessionId();
			if (sessionId.equals(command_sessionId)) {
				String old_receiver = session.getDataReceiver();
				session.setDataReceiver(newAddress);
				return old_receiver;
			} else {
				log.info("Incorrect session ID, ignoring data redirect for: "
					+ newAddress);
			}
		}
		return null;
	}

	@Override
	public Queue<Packet> processSocketData(XMPPIOService<Object> srv) {
		BoshIOService serv = (BoshIOService)srv;
		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			Queue<Packet> out_results = new LinkedList<Packet>();
			BoshSession bs = null;
			String sid_str = null;
			synchronized (sessions) {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Processing packet: " + p.getElemName() +
									", type: " + p.getType());
				}
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Processing socket data: " + p.toString());
				}
				sid_str = p.getAttribute(SID_ATTR);
				UUID sid = null;
				if (sid_str == null) {
					String hostname = p.getAttribute("to");
					if (hostname != null && isLocalDomain(hostname)) {
						bs = new BoshSession(getDefHostName(),
										routings.computeRouting(hostname), this);
						sid = bs.getSid();
						sessions.put(sid, bs);
					} else {
						log.info("Invalid hostname. Closing invalid connection");
						try {
							serv.sendErrorAndStop(Authorization.NOT_ALLOWED, p,
											"Invalid hostname.");
						} catch (Exception e) {
							log.log(Level.WARNING,
											"Problem sending invalid hostname error for sid =  " +
											sid, e);
						}
					}
				} else {
					sid = UUID.fromString(sid_str);
					bs = sessions.get(sid);
				}
			}
			try {
				if (bs != null) {
					synchronized (bs) {
						if (sid_str == null) {
							bs.init(p, serv, max_wait, min_polling, max_inactivity,
											concurrent_requests, hold_requests, max_pause, out_results);
						} else {
							bs.processSocketPacket(p, serv, out_results);
						}
					}
				} else {
					log.info(
									"There is no session with given SID. Closing invalid connection");
					serv.sendErrorAndStop(Authorization.ITEM_NOT_FOUND, p, "Invalid SID");
				}
				addOutPackets(out_results, bs);
			} catch (Exception e) {
				log.log(Level.WARNING,
					"Problem processing socket data for sid =  " + sid_str,	e);
			}
			//addOutPackets(out_results);
		} // end of while ()
		return null;
	}

	private void addOutPackets(Queue<Packet> out_results, BoshSession bs) {
		for (Packet res: out_results) {
			res.setFrom(getFromAddress(bs.getSid().toString()));
			res.setTo(bs.getDataReceiver());
			addOutPacket(res);
		}
		out_results.clear();
	}

	private String getFromAddress(String id) {
		return JIDUtils.getJID(getName(), getDefHostName(), id);
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		props.put(MAX_WAIT_DEF_PROP_KEY, MAX_WAIT_DEF_PROP_VAL);
		props.put(MIN_POLLING_PROP_KEY, MIN_POLLING_PROP_VAL);
		props.put(MAX_INACTIVITY_PROP_KEY, MAX_INACTIVITY_PROP_VAL);
		props.put(CONCURRENT_REQUESTS_PROP_KEY, CONCURRENT_REQUESTS_PROP_VAL);
		props.put(HOLD_REQUESTS_PROP_KEY, HOLD_REQUESTS_PROP_VAL);
		props.put(MAX_PAUSE_PROP_KEY, MAX_PAUSE_PROP_VAL);
		return props;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		max_wait = (Long)props.get(MAX_WAIT_DEF_PROP_KEY);
		min_polling  = (Long)props.get(MIN_POLLING_PROP_KEY);
		max_inactivity = (Long)props.get(MAX_INACTIVITY_PROP_KEY);
		concurrent_requests = (Integer)props.get(CONCURRENT_REQUESTS_PROP_KEY);
		hold_requests = (Integer)props.get(HOLD_REQUESTS_PROP_KEY);
		max_pause = (Long)props.get(MAX_PAUSE_PROP_KEY);
	}

	@Override
	protected int[] getDefPlainPorts() {
		return PORTS;
	}

	@Override
	protected int[] getDefSSLPorts() {
		return null;
	}

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

	public void serviceStarted(BoshIOService service) {
		super.serviceStarted(service);
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. we shoulnd not really close external component
	 * connection at all, so let's say something like: 1000 days...
	 *
	 * @return a <code>long</code> value
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 10*MINUTE;
	}

	public void xmppStreamClosed(BoshIOService serv) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream closed.");
		}
	}

	public String xmppStreamOpened(BoshIOService serv,
		Map<String, String> attribs) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Ups, what just happened? Stream open. Hey, this is a Bosh connection manager. c2s and s2s are not supported on the same port as Bosh yet.");
		}
		return "<?xml version='1.0'?><stream:stream"
				+ " xmlns='jabber:client'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " id='1'"
				+ " from='" + getDefHostName() + "'"
        + " version='1.0' xml:lang='en'>"
				+ "<stream:error>"
				+ "<invalid-namespace xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
        + "<text xmlns='urn:ietf:params:xml:ns:xmpp-streams' xml:lang='langcode'>"
        + "Ups, what just happened? Stream open. Hey, this is a Bosh connection manager. c2s and s2s are not supported on the same port... yet."
        + "</text>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
	}

	@Override
	protected BoshIOService getXMPPIOServiceInstance() {
		return new BoshIOService();
	}

	@Override
	public void writeRawData(BoshIOService ios, String data) {
		super.writeRawData(ios, data);
	}


	private Timer boshTasks = new Timer("BoshTasks");

	@Override
	public TimerTask scheduleTask(BoshSession bs, long delay) {
		BoshTask bt = new BoshTask(bs);
		boshTasks.schedule(bt, delay);
		return bt;
	}

	@Override
	public void cancelTask(TimerTask tt) {
		tt.cancel();
	}

	private class BoshTask extends TimerTask {

		private BoshSession bs = null;

		public BoshTask(BoshSession bs) {
			this.bs = bs;
		}

		@Override
		public void run() {
			Queue<Packet> out_results = new LinkedList<Packet>();
			if (bs.task(out_results, this)) {
				log.fine("Closing session for BS task: " + bs.getSid());
				sessions.remove(bs.getSid());
			}
			addOutPackets(out_results, bs);
		}

	}

	/**
	 *
	 * @param packet
	 * @param bs
	 * @return
	 */
	@Override
	public boolean addOutStreamOpen(Packet packet, BoshSession bs) {
		packet.setFrom(getFromAddress(bs.getSid().toString()));
		packet.setTo(bs.getDataReceiver());
		return addOutPacketWithTimeout(packet, startedHandler, 15l, TimeUnit.SECONDS);
	}

	@Override
	public boolean addOutStreamClosed(Packet packet, BoshSession bs) {
		packet.setFrom(getFromAddress(bs.getSid().toString()));
		packet.setTo(bs.getDataReceiver());
		return addOutPacketWithTimeout(packet, stoppedHandler, 15l, TimeUnit.SECONDS);
	}

	@Override
	protected ReceiverEventHandler newStartedHandler() {
		return new StartedHandler();
	}

	@Override
	public String getDiscoDescription() {
		return "Bosh connection manager";
	}

	@Override
	public String getDiscoCategory() {
		return "c2s";
	}

	private class StartedHandler implements ReceiverEventHandler {

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

		@Override
		public void responseReceived(Packet packet, Packet response) {
			// We are now ready to ask for features....
			addOutPacket(Command.GETFEATURES.getPacket(packet.getFrom(),
							packet.getTo(), StanzaType.get, UUID.randomUUID().toString(),
							null));
		}

	}



}
