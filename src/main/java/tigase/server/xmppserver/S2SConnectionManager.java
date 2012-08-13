/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.server.xmppserver.proc.Dialback;
import tigase.server.xmppserver.proc.PacketChecker;
import tigase.server.xmppserver.proc.StartTLS;
import tigase.server.xmppserver.proc.StartZlib;
import tigase.server.xmppserver.proc.StreamError;
import tigase.server.xmppserver.proc.StreamFeatures;
import tigase.server.xmppserver.proc.StreamOpen;

import tigase.stats.StatisticsList;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 14, 2010 11:59:38 AM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SConnectionManager extends ConnectionManager<S2SIOService> implements
		S2SConnectionHandlerIfc<S2SIOService> {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger
			.getLogger(S2SConnectionManager.class.getName());
	private static final String XMLNS_SERVER_VAL = "jabber:server";
	private static final String XMLNS_CLIENT_VAL = "jabber:client";
	protected static final String DB_RESULT_EL_NAME = "db:result";
	protected static final String DB_VERIFY_EL_NAME = "db:verify";

	/** Field description */
	public static final String MAX_PACKET_WAITING_TIME_PROP_KEY = "max-packet-waiting-time";

	/** Field description */
	public static final String MAX_CONNECTION_INACTIVITY_TIME_PROP_KEY =
			"max-inactivity-time";

	/** Field description */
	public static final String MAX_INCOMING_CONNECTIONS_PROP_KEY = "max-in-conns";

	/** Field description */
	public static final String MAX_OUT_TOTAL_CONNECTIONS_PROP_KEY = "max-out-total-conns";

	/** Field description */
	public static final String MAX_OUT_PER_IP_CONNECTIONS_PROP_KEY = "max-out-per-ip-conns";

	/** Field description */
	public static final String S2S_CONNECTION_SELECTOR_PROP_KEY = "s2s-conn-selector";

	/** Field description */
	public static final String S2S_CONNECTION_SELECTOR_PROP_VAL =
			"tigase.server.xmppserver.S2SRandomSelector";

	/** Field description */
	public static final int MAX_INCOMING_CONNECTIONS_PROP_VAL = 4;

	/** Field description */
	public static final int MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL = 1;

	/** Field description */
	public static final int MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL = 1;

	/** Field description */
	public static final long MAX_PACKET_WAITING_TIME_PROP_VAL = 7 * MINUTE;

	/** Field description */
	public static final long MAX_CONNECTION_INACTIVITY_TIME_PROP_VAL = 15 * MINUTE;

	public static final String CID_CONNECTIONS_BIND = "cidConnections";

	// ~--- fields ---------------------------------------------------------------

	private S2SConnectionSelector connSelector = null;

	/**
	 * <code>maxPacketWaitingTime</code> keeps the maximum time packets can wait
	 * for sending in ServerPacketQueue. Packets are put in the queue only when
	 * connection to remote server is not established so effectively this timeout
	 * specifies the maximum time for connecting to remote server. If this time is
	 * exceeded then no more reconnecting attempts are performed and packets are
	 * sent back with error information.
	 * 
	 * Default TCP/IP timeout is 300 seconds so we can follow this convention but
	 * administrator can set different timeout in server configuration.
	 */
	private long maxPacketWaitingTime = MAX_PACKET_WAITING_TIME_PROP_VAL;
	private int maxOUTTotalConnections = MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL;
	private int maxOUTPerIPConnections = MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL;
	private long maxInactivityTime = MAX_CONNECTION_INACTIVITY_TIME_PROP_VAL;
	private int maxINConnections = MAX_INCOMING_CONNECTIONS_PROP_VAL;

	/**
	 * Outgoing and incoming connections for a given domains pair (localdomain,
	 * remotedomain)
	 */
	private Map<CID, CIDConnections> cidConnections =
			new ConcurrentHashMap<CID, CIDConnections>(10000);

	/**
	 * List of processors which should handle all traffic incoming from the
	 * network. In most cases if not all, these processors handle just protocol
	 * traffic, all the rest traffic should be passed on to MR.
	 */
	private Map<String, S2SProcessor> processors = new LinkedHashMap<String, S2SProcessor>(
			10);
	private Map<String, S2SProcessor> filters = new LinkedHashMap<String, S2SProcessor>(
			10);

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param binds
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CID_CONNECTIONS_BIND, cidConnections);
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
	public boolean addOutPacket(Packet packet) {
		return super.addOutPacket(packet);
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param task
	 * @param delay
	 * @param unit
	 */
	@Override
	public void addTimerTask(TimerTask task, long delay, TimeUnit unit) {
		super.addTimerTask(task, delay, unit);
	}

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param cid
	 * @param createNew
	 * 
	 * @return
	 * 
	 * @throws LocalhostException
	 * @throws NotLocalhostException
	 */
	@Override
	public CIDConnections getCIDConnections(CID cid, boolean createNew)
			throws NotLocalhostException, LocalhostException {
		CIDConnections result = getCIDConnections(cid);

		if ((result == null) && createNew && (cid != null)) {
			result = createNewCIDConnections(cid);
		}

		return result;
	}

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

		props
				.put(MAX_PACKET_WAITING_TIME_PROP_KEY, MAX_PACKET_WAITING_TIME_PROP_VAL / SECOND);
		props.put(MAX_CONNECTION_INACTIVITY_TIME_PROP_KEY,
				MAX_CONNECTION_INACTIVITY_TIME_PROP_VAL / SECOND);
		props.put(MAX_INCOMING_CONNECTIONS_PROP_KEY, MAX_INCOMING_CONNECTIONS_PROP_VAL);
		props.put(MAX_OUT_TOTAL_CONNECTIONS_PROP_KEY, MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL);
		props.put(MAX_OUT_PER_IP_CONNECTIONS_PROP_KEY, MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL);
		props.put(S2S_CONNECTION_SELECTOR_PROP_KEY, S2S_CONNECTION_SELECTOR_PROP_VAL);

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
		return "s2s";
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return "S2S connection manager";
	}

	/**
	 * 
	 * @param connectionCid
	 * @param keyCid
	 * @param key
	 * @param key_sessionId
	 * @param asking_sessionId
	 * @return
	 */
	@Override
	public String getLocalDBKey(CID connectionCid, CID keyCid, String key,
			String key_sessionId, String asking_sessionId) {
		CIDConnections cid_conns = getCIDConnections(keyCid);
		String result = (cid_conns == null) ? null : cid_conns.getDBKey(key_sessionId);

		if (result == null) {

			// In piggybacking mode the DB key can be available in the connectionCID
			// rather then
			// keyCID
			cid_conns = getCIDConnections(connectionCid);
			result = (cid_conns == null) ? null : cid_conns.getDBKey(key_sessionId);
		}

		return result;
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
		list.add(getName(), "CIDs number", cidConnections.size(), Level.INFO);

		if (list.checkLevel(Level.FINEST)) {
			long total_outgoing = 0;
			long total_outgoing_tls = 0;
			long total_outgoing_handshaking = 0;
			long total_incoming = 0;
			long total_incoming_tls = 0;
			long total_dbKeys = 0;
			long total_waiting = 0;
			long total_waiting_control = 0;

			for (Map.Entry<CID, CIDConnections> cid_conn : cidConnections.entrySet()) {
				int outgoing = cid_conn.getValue().getOutgoingCount();
				int outgoing_tls = cid_conn.getValue().getOutgoingTLSCount();
				int outgoing_handshaking = cid_conn.getValue().getOutgoingHandshakingCount();
				int incoming = cid_conn.getValue().getIncomingCount();
				int incoming_tls = cid_conn.getValue().getIncomingTLSCount();
				int dbKeys = cid_conn.getValue().getDBKeysCount();
				int waiting = cid_conn.getValue().getWaitingCount();
				int waiting_control = cid_conn.getValue().getWaitingControlCount();

				if (log.isLoggable(Level.FINEST)) {

					// Throwable thr = new Throwable();
					//
					// thr.fillInStackTrace();
					// log.log(Level.FINEST, "Called from: ", thr);
					log.log(Level.FINEST,
							"CID: {0}, OUT: {1}, OUT_HAND: {2}, IN: {3}, dbKeys: {4}, "
									+ "waiting: {5}, waiting_control: {6}",
							new Object[] { cid_conn.getKey(), outgoing, outgoing_handshaking, incoming,
									dbKeys, waiting, waiting_control });
				}

				total_outgoing += outgoing;
				total_outgoing_tls += outgoing_tls;
				total_outgoing_handshaking += outgoing_handshaking;
				total_incoming += incoming;
				total_incoming_tls += incoming_tls;
				total_dbKeys += dbKeys;
				total_waiting += waiting;
				total_waiting_control += waiting_control;
			}

			list.add(getName(), "Total outgoing", total_outgoing, Level.FINEST);
			list.add(getName(), "Total outgoing TLS", total_outgoing_tls, Level.FINEST);
			list.add(getName(), "Total outgoing handshaking", total_outgoing_handshaking,
					Level.FINEST);
			list.add(getName(), "Total incoming", total_incoming, Level.FINEST);
			list.add(getName(), "Total incoming TLS", total_incoming_tls, Level.FINEST);
			list.add(getName(), "Total DB keys", total_dbKeys, Level.FINEST);
			list.add(getName(), "Total waiting", total_waiting, Level.FINEST);
			list.add(getName(), "Total control waiting", total_waiting_control, Level.FINEST);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * 
	 * @param serv
	 * @return
	 */
	@Override
	public List<Element> getStreamFeatures(S2SIOService serv) {
		List<Element> results = new ArrayList<Element>(10);

		for (S2SProcessor proc : processors.values()) {
			proc.streamFeatures(serv, results);
		}

		return results;
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public boolean handlesNonLocalDomains() {
		return true;
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
	public int hashCodeForPacket(Packet packet) {

		// Calculate hash code from the destination domain name to make sure packets
		// for
		// a single domain are processed by the same thread to avoid race condition
		// creating new connection data structures for a destination domain
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().getDomain().hashCode();
		}

		// Otherwise, it might be a control packet which can be processed by single
		// thread
		return 1;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param port_props
	 */
	@Override
	public void initNewConnection(Map<String, Object> port_props) {
		addWaitingTask(port_props);
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}

		if ((packet.getStanzaTo() == null)
				|| packet.getStanzaTo().getDomain().trim().isEmpty()) {
			log.log(Level.WARNING, "Missing ''to'' attribute, ignoring packet...{0}"
					+ "\n This most likely happens due to missconfiguration of components"
					+ " domain names.", packet);

			return;
		}

		if ((packet.getStanzaFrom() == null)
				|| packet.getStanzaFrom().getDomain().trim().isEmpty()) {
			log.log(Level.WARNING, "Missing ''from'' attribute, ignoring packet...{0}", packet);

			return;
		}

		String to_hostname = packet.getStanzaTo().getDomain();

		try {

			// Code commented out below is not needed anymore
			// following call below takes care of hostnames checking:
			// getCIDConnections(cid, true);
			// // Check whether addressing is correct:
			//
			// // We don't send packets to local domains trough s2s, there
			// // must be something wrong with configuration
			// if (isLocalDomainOrComponent(to_hostname)) {
			//
			// // Ups, remote hostname is the same as one of local hostname??
			// // Internal loop possible, we don't want that....
			// // Let's send the packet back....
			// if (log.isLoggable(Level.INFO)) {
			// log.log(Level.INFO, "Packet addresses to localhost,"
			// + " I am not processing it: {0}", packet);
			// }
			//
			// addOutPacket(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
			// "S2S - not delivered. Server missconfiguration.", true));
			//
			// return;
			// }
			//
			String from_hostname = packet.getStanzaFrom().getDomain();

			// Code commented out below is not needed anymore
			// following call below takes care of hostnames checking:
			// getCIDConnections(cid, true);
			// // I think from_hostname needs to be different from to_hostname at
			// // this point... or s2s doesn't make sense
			//
			// // All hostnames go through String.intern()
			// if (to_hostname == from_hostname) {
			// log.log(Level.WARNING,
			// "Dropping incorrect packet - from_hostname == to_hostname: {0}",
			// packet);
			//
			// return;
			// }
			CID cid = new CID(from_hostname, to_hostname);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Connection ID is: {0}", cid);
			}

			try {
				CIDConnections cid_conns = getCIDConnections(cid, true);
				Packet server_packet = packet.copyElementOnly();

				server_packet.getElement().removeAttribute("xmlns");
				cid_conns.sendPacket(server_packet);
			} catch (NotLocalhostException e) {
				addOutPacket(Authorization.NOT_ACCEPTABLE
						.getResponseMessage(
								packet,
								"S2S - Incorrect source address - none of any local virtual hosts or components.",
								true));
			} catch (LocalhostException e) {
				addOutPacket(Authorization.NOT_ACCEPTABLE
						.getResponseMessage(
								packet,
								"S2S - Incorrect destinationaddress - one of local virtual hosts or components.",
								true));
			}
		} catch (PacketErrorTypeException e) {
			log.log(Level.WARNING, "Packet processing exception: {0}", e);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param serv
	 * 
	 * @return
	 */
	@Override
	public Queue<Packet> processSocketData(S2SIOService serv) {
		Queue<Packet> packets = serv.getReceivedPackets();
		Packet p = null;
		Queue<Packet> results = new ArrayDeque<Packet>(2);

		while ((p = packets.poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing socket data: {0}", p);
			}

                        if (p.getXMLNS() == null) {
                                p.setXMLNS(XMLNS_SERVER_VAL);
                        }
                        
			boolean processed = false;

			for (S2SProcessor proc : processors.values()) {
				processed |= proc.process(p, serv, results);
				writePacketsToSocket(serv, results);
			}

                        if (!processed) {
                                for (S2SProcessor filter : filters.values()) {
                                        processed |= filter.process(p, serv, results);
                                        writePacketsToSocket(serv, results);                                        
                                }                                
                        }
                        
			if (!processed) {                                
				// Sometimes xmlns is not set for the packet. Usually it does not
				// cause any problems but when the packet is sent over the s2s, ext
				// or cluster connection it may be quite problematic.
				// Let's force jabber:client xmlns for all packets received from s2s
				// connection
				// In theory null does not hurt, but if the packet goes through the
				// cluster
				// connection is gets cluster XMLNS
				if (p.getXMLNS() == XMLNS_SERVER_VAL || p.getXMLNS() == null) {
					p.setXMLNS(XMLNS_CLIENT_VAL);
				}

				try {
					if (isLocalDomainOrComponent(p.getStanzaTo().getDomain())) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "{0}, Adding packet out: {1}",
									new Object[] { serv, p });
						}

						// TODO: not entirely sure if this is a good idea....
						// Let's check it out.
						p.setPermissions(Permissions.REMOTE);
						addOutPacket(p);
					} else {
						try {
							serv.addPacketToSend(Authorization.NOT_ACCEPTABLE.getResponseMessage(p,
									"Not a local virtual domain or component", true));
						} catch (PacketErrorTypeException ex) {
						}
					}
				} catch (Exception e) {
					log.log(Level.INFO, "Unexpected exception for packet: " + p, e);
				}
			}
		} // end of while ()

		return null;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param port_props
	 */
	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {
		CID cid = (CID) port_props.get("cid");

		if (cid == null) {
			log.log(Level.WARNING, "Protocol error cid not set for outgoing connection: {0}",
					port_props);

			return;
		}

		CIDConnections cid_conns = getCIDConnections(cid);

		if (cid_conns == null) {
			log.log(Level.WARNING,
					"Protocol error cid_conns not found for outgoing connection: {0}", port_props);

			return;
		} else {
			cid_conns.reconnectionFailed(port_props);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param elem_name
	 * @param connCid
	 * @param keyCid
	 * @param valid
	 * @param key_sessionId
	 * @param serv_sessionId
	 * @param cdata
	 * @param handshakingOnly
	 * 
	 * @return
	 */
	@Override
	public boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid,
			Boolean valid, String key_sessionId, String serv_sessionId, String cdata,
			boolean handshakingOnly) {
		CIDConnections cid_conns = getCIDConnections(connCid);

		if (cid_conns != null) {
			Packet verify_valid =
					getValidResponse(elem_name, keyCid, key_sessionId, valid, cdata);

			if (handshakingOnly) {
				cid_conns.sendHandshakingOnly(verify_valid);

				return true;
			} else {
				return cid_conns.sendControlPacket(serv_sessionId, verify_valid);
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"Can't find CID connections for cid: {0}, can't send verify response.",
						keyCid);
			}
		}

		return false;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param serv
	 */
	@Override
	public void serviceStarted(S2SIOService serv) {
		super.serviceStarted(serv);
		log.log(Level.FINEST, "s2s connection opened: {0}", serv);

		for (S2SProcessor proc : processors.values()) {
			proc.serviceStarted(serv);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param serv
	 * 
	 * @return
	 */
	@Override
	public boolean serviceStopped(S2SIOService serv) {
		boolean result = super.serviceStopped(serv);

		if (result) {
			for (S2SProcessor proc : processors.values()) {
				proc.serviceStopped(serv);
			}
		}

		return result;
	}

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

		// TODO: Make used processors list a configurable thing
		processors.clear();
		processors.put( Dialback.class.getName(), new Dialback() );
		processors.put( StartTLS.class.getName(), new StartTLS() );
		processors.put( StartZlib.class.getName(), new StartZlib() );
		processors.put( StreamError.class.getName(), new StreamError() );
		processors.put( StreamFeatures.class.getName(), new StreamFeatures() );
		processors.put( StreamOpen.class.getName(), new StreamOpen() );

		for ( S2SProcessor proc : processors.values() ) {
			proc.init( this );
		}

		filters.clear();
		filters.put( PacketChecker.class.getName(), new PacketChecker() );

		for ( S2SProcessor filter : filters.values() ) {
			filter.init( this );
		}

		if ( props.size() == 1 ){
			// If props.size() == 1, it means this is a single property update
			// and this component does not support single property change for the rest
			// of it's settings
			return;
		}

		maxPacketWaitingTime = (Long) props.get(MAX_PACKET_WAITING_TIME_PROP_KEY) * SECOND;
		maxInactivityTime =
				(Long) props.get(MAX_CONNECTION_INACTIVITY_TIME_PROP_KEY) * SECOND;
		maxOUTTotalConnections = (Integer) props.get(MAX_OUT_TOTAL_CONNECTIONS_PROP_KEY);
		maxOUTPerIPConnections = (Integer) props.get(MAX_OUT_PER_IP_CONNECTIONS_PROP_KEY);
		maxINConnections = (Integer) props.get(MAX_INCOMING_CONNECTIONS_PROP_KEY);

		String selector_str = (String) props.get(S2S_CONNECTION_SELECTOR_PROP_KEY);

		try {
			connSelector = (S2SConnectionSelector) Class.forName(selector_str).newInstance();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Incorrect s2s connection selector class provided: {0}",
					selector_str);
			log.log(Level.SEVERE, "Selector initialization exception: ", e);
		}
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param serv
	 */
	@Override
	public void tlsHandshakeCompleted(S2SIOService serv) {
		for (S2SProcessor proc : processors.values()) {
			proc.serviceStarted(serv);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param ios
	 * @param data
	 */
	@Override
	public void writeRawData(S2SIOService ios, String data) {
		super.writeRawData(ios, data);
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param serv
	 */
	@Override
	public void xmppStreamClosed(S2SIOService serv) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "{0}, Stream closed.", new Object[] { serv });
		}

		for (S2SProcessor proc : processors.values()) {
			proc.streamClosed(serv);
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
	@Override
	public String xmppStreamOpened(S2SIOService serv, Map<String, String> attribs) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "{0}, Stream opened: {1}", new Object[] { serv, attribs });
		}

		StringBuilder sb = new StringBuilder(256);

		for (S2SProcessor proc : processors.values()) {
			String res = proc.streamOpened(serv, attribs);

			if (res != null) {
				sb.append(res);
			}
		}

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "{0}, Sending stream open: {1}", new Object[] { serv, sb });
		}

		return (sb.length() == 0) ? null : sb.toString();
	}

	// ~--- get methods ----------------------------------------------------------

	@Override
	protected int[] getDefPlainPorts() {
		return new int[] { 5269 };
	}

	@Override
	protected long getMaxInactiveTime() {
		return maxInactivityTime;
	}

	@Override
	protected S2SIOService getXMPPIOServiceInstance() {
		return new S2SIOService();
	}

	@Override
	protected boolean isHighThroughput() {
		return true;
	}

	// ~--- methods --------------------------------------------------------------

	private CIDConnections createNewCIDConnections(CID cid) throws NotLocalhostException,
			LocalhostException {
		if (!isLocalDomainOrComponent(cid.getLocalHost())) {
			throw new NotLocalhostException("This is not a valid localhost: "
					+ cid.getLocalHost());
		}

		if (isLocalDomainOrComponent(cid.getRemoteHost())) {
			throw new LocalhostException("This is not a valid remotehost: "
					+ cid.getRemoteHost());
		}

		CIDConnections cid_conns =
				new CIDConnections(cid, this, connSelector, maxINConnections,
						maxOUTTotalConnections, maxOUTPerIPConnections, maxPacketWaitingTime);

		cidConnections.put(cid, cid_conns);

		return cid_conns;
	}

	// ~--- get methods ----------------------------------------------------------

	private CIDConnections getCIDConnections(CID cid) {
		if (cid == null) {
			return null;
		}

		return cidConnections.get(cid);
	}

	private Packet getValidResponse(String elem_name, CID cid, String id, Boolean valid,
			String cdata) {
		Element elem = new Element(elem_name);

		if (cdata != null) {
			elem.setCData(cdata);
		}

		if (valid != null) {
			if (valid.booleanValue()) {
				elem.addAttribute("type", "valid");
			} else {
				elem.addAttribute("type", "invalid");
			}
		}

		if (id != null) {
			elem.addAttribute("id", id);
		}

		Packet result =
				Packet.packetInstance(elem, JID.jidInstanceNS(cid.getLocalHost()),
						JID.jidInstanceNS(cid.getRemoteHost()));

		return result;
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
