/*
 * CIDConnections.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

import tigase.net.ConnectionType;
import tigase.net.SocketType;
import tigase.util.DNSEntry;
import tigase.util.DNSResolverFactory;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Jun 14, 2010 12:32:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CIDConnections {
	private static final Logger log              =
		Logger.getLogger(CIDConnections.class.getName());
//	private static final Timer outgoingOpenTasks = new Timer("S2S outgoing open tasks",
//																									 true);
	// TODO: #1195 - estimate proper default value 
	private static int outgoingOpenTasksSize = Runtime.getRuntime().availableProcessors();
	
	private static ScheduledExecutorService outgoingOpenTasks = 
			Executors.newScheduledThreadPool(outgoingOpenTasksSize);

	public static void setOutgoingOpenThreadsSize(int size) {
		if (outgoingOpenTasksSize != size) {
			outgoingOpenTasksSize = size;
			ScheduledExecutorService scheduler = outgoingOpenTasks;
			outgoingOpenTasks = Executors.newScheduledThreadPool(outgoingOpenTasksSize);
			scheduler.shutdown();
		}
	}
	
	//~--- fields ---------------------------------------------------------------

	private CID cid                                       = null;
	private S2SConnectionSelector connectionSelector      = null;
	private long firstWaitingTime                         = 0;
	private S2SConnectionHandlerIfc<S2SIOService> handler = null;
	private int max_in_conns                              = 4;
	private int max_out_conns                             = 4;
	private int max_out_conns_per_ip                      = 2;
	private long max_waiting_time                         = 15 * 60 * 1000;
	private boolean testMode                              = Boolean.getBoolean("test");
	private ReentrantLock sendInProgress                  = new ReentrantLock();
	private AtomicBoolean outgoingOpenInProgress          = new AtomicBoolean(false);
	private Set<S2SConnection> outgoing_handshaking       =
		new ConcurrentSkipListSet<S2SConnection>();
	private Set<S2SConnection> outgoing = new ConcurrentSkipListSet<S2SConnection>();
	private Set<S2SConnection> incoming = new ConcurrentSkipListSet<S2SConnection>();

	/**
	 * (SessionID, dbKey) pairs
	 */
	private Map<String, String> dbKeys                   =
		new ConcurrentSkipListMap<String, String>();
	private ConcurrentLinkedQueue<Packet> waitingPackets =
		new ConcurrentLinkedQueue<Packet>();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param cid
	 * @param handler
	 * @param selector
	 * @param maxInConns
	 * @param maxOutConns
	 * @param maxOutConnsPerIP
	 * @param max_waiting_time
	 */
	public CIDConnections(CID cid, S2SConnectionHandlerIfc<S2SIOService> handler,
												S2SConnectionSelector selector, int maxInConns, int maxOutConns,
												int maxOutConnsPerIP, long max_waiting_time) {
		this.cid                  = cid;
		this.handler              = handler;
		this.connectionSelector   = selector;
		this.max_in_conns         = maxInConns;
		this.max_out_conns        = maxOutConns;
		this.max_out_conns_per_ip = maxOutConnsPerIP;
		this.max_waiting_time     = max_waiting_time;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	public void resetOutgoingInProgress() {
		outgoingOpenInProgress.set(false);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public boolean getOutgoingInProgress() {
		return outgoingOpenInProgress.get();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param sessId
	 * @param key
	 */
	public void addDBKey(String sessId, String key) {
		dbKeys.put(sessId, key);
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void addIncoming(S2SIOService serv) {
		S2SConnection s2s_conn = serv.getS2SConnection();

		if (s2s_conn == null) {
			s2s_conn = new S2SConnection(handler, serv.getRemoteAddress());
			s2s_conn.setS2SIOService(serv);
			serv.setS2SConnection(s2s_conn);
		}

		CID cid = (CID) serv.getSessionData().get("cid");
		if (cid != null) {
			// using additional mapping/masking of incoming connections to allow 
			// usage of intermediate server also for incoming connections
			String serverName = handler.getServerNameForDomain(cid.getRemoteHost());
			serv.getSessionData().put(S2SIOService.CERT_REQUIRED_DOMAIN, serverName);
		}
		
		// TODO: check if this should be moved inside the IF
		incoming.add(s2s_conn);
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void connectionAuthenticated(S2SIOService serv, CID cid) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "{0}, connection is authenticated.", serv);
		}
		serv.addCID(cid);
		if (serv.connectionType() == ConnectionType.connect) {

			// Release the 'lock'
			outgoingOpenInProgress.set(false);

			S2SConnection s2s_conn = serv.getS2SConnection();

			outgoing_handshaking.remove(s2s_conn);
			outgoing.add(s2s_conn);
			sendPacket(null);
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param sessionId
	 */
	public void connectionAuthenticated(String sessionId, CID cid) {
		S2SConnection s2s_conn = getS2SConnectionForSessionId(sessionId);

		if (s2s_conn != null) {
			connectionAuthenticated(s2s_conn.getS2SIOService(), cid);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void connectionStopped(S2SIOService serv) {
		S2SConnection s2s_conn = serv.getS2SConnection();

		if (s2s_conn == null) {
			log.log(Level.INFO, "s2s_conn not set for serv: {0}", serv);

			return;
		}
		if (serv.getSessionId() != null) {
			dbKeys.remove(serv.getSessionId());
		}
		switch (serv.connectionType()) {
		case connect :

			// Release the 'lock'
			outgoingOpenInProgress.set(false);
			outgoing.remove(s2s_conn);
			outgoing_handshaking.remove(s2s_conn);
			if (!waitingPackets.isEmpty()) {
				checkOpenConnections();
			}

			break;

		case accept :
			incoming.remove(s2s_conn);

			break;

		default :
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param key_sessionId
	 *
	 * 
	 */
	public String getDBKey(String key_sessionId) {
		return dbKeys.get(key_sessionId);
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getDBKeysCount() {
		return dbKeys.size();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getIncomingCount() {
		int result = 0;

		for (S2SConnection s2SConnection : incoming) {
			if (s2SConnection.isConnected()) {
				++result;
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getIncomingTLSCount() {
		int result = 0;

		for (S2SConnection s2SConnection : incoming) {
			S2SIOService serv = s2SConnection.getS2SIOService();

			if (serv.isConnected() &&
					(serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT) != null)) {
				++result;
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getMaxOutConns() {
		return this.max_out_conns;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getMaxOutConnsPerIP() {
		return this.max_out_conns_per_ip;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getOutgoingCount() {
		int result = 0;

		for (S2SConnection s2SConnection : outgoing) {
			if (s2SConnection.isConnected()) {
				++result;
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getOutgoingHandshakingCount() {
		int result = 0;

		for (S2SConnection s2SConnection : outgoing_handshaking) {
			if (s2SConnection.isConnected()) {
				++result;
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getOutgoingTLSCount() {
		int result = 0;

		for (S2SConnection s2SConnection : outgoing) {
			S2SIOService serv = s2SConnection.getS2SIOService();

			if (serv.isConnected() &&
					(serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT) != null)) {
				++result;
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param sessionId
	 *
	 * 
	 */
	public S2SConnection getS2SConnectionForSessionId(String sessionId) {
		S2SConnection s2s_conn = null;

		for (S2SConnection s2sc : incoming) {
			if ((s2sc.getS2SIOService() != null) &&
					sessionId.equals(s2sc.getS2SIOService().getSessionId())) {
				s2s_conn = s2sc;

				break;
			}
		}
		if (s2s_conn == null) {
			for (S2SConnection s2sc : outgoing) {
				if ((s2sc.getS2SIOService() != null) &&
						sessionId.equals(s2sc.getS2SIOService().getSessionId())) {
					s2s_conn = s2sc;

					break;
				}
			}
		}
		if (s2s_conn == null) {
			for (S2SConnection s2sc : outgoing_handshaking) {
				if ((s2sc.getS2SIOService() != null) &&
						sessionId.equals(s2sc.getS2SIOService().getSessionId())) {
					s2s_conn = s2sc;

					break;
				}
			}
		}

		return s2s_conn;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getWaitingControlCount() {
		int result = 0;

		for (S2SConnection s2sc : incoming) {
			result += s2sc.getWaitingControlCount();
		}
		for (S2SConnection s2sc : outgoing) {
			result += s2sc.getWaitingControlCount();
		}
		for (S2SConnection s2sc : outgoing_handshaking) {
			result += s2sc.getWaitingControlCount();
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getWaitingCount() {
		return waitingPackets.size();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param port_props
	 */
	public void reconnectionFailed(Map<String, Object> port_props) {
		S2SConnection s2s_conn =
			(S2SConnection) port_props.get(S2SIOService.S2S_CONNECTION_KEY);

		if (s2s_conn == null) {
			log.log(Level.INFO, "s2s_conn not set for serv: {0}", port_props);

			return;
		}

		ConnectionType type = (ConnectionType) port_props.get("type");

		if (type != null) {
			switch (type) {
			case connect :

				// Release the 'lock'
				outgoingOpenInProgress.set(false);
				outgoing.remove(s2s_conn);
				outgoing_handshaking.remove(s2s_conn);
				if (!this.waitingPackets.isEmpty()) {
					checkOpenConnections();
				}

				break;

			case accept :
				this.incoming.remove(s2s_conn);

				break;

			default :
			}
		} else {
			log.log(Level.INFO, "ConnectionType not set for serv: {0}", port_props);
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param sessionId
	 * @param packet
	 * 
	 */
	public boolean sendControlPacket(String sessionId, Packet packet) {

		// Seraching for a correct connection
		// TODO: speed it up somehow, maybe verify can be only sent to incoming
		// and result to outgoing? Check it out
		S2SConnection s2s_conn = getS2SConnectionForSessionId(sessionId);

		if (s2s_conn != null) {
			s2s_conn.addControlPacket(packet);
			s2s_conn.sendAllControlPackets();

			return true;
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
								"Control packet: {0} could not be sent as there is no connection " +
								"for the session id: {1}", new Object[] { packet,
								sessionId });
			}

			return false;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param verify_req
	 */
	public void sendHandshakingOnly(final Packet verify_req) {
		outgoingOpenTasks.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					// using additional domain name mapping to allow usage of intermediate server
					String serverName = handler.getServerNameForDomain(cid.getRemoteHost());
					
					DNSEntry dns_entry     = DNSResolverFactory.getInstance().getHostSRV_Entry(serverName);
					S2SConnection s2s_conn = new S2SConnection(handler, dns_entry.getIp());

					s2s_conn.addControlPacket(verify_req);

					Map<String, Object> port_props = new TreeMap<String, Object>();
					port_props.put(S2SIOService.CERT_REQUIRED_DOMAIN, serverName);

					port_props.put(S2SIOService.HANDSHAKING_ONLY_KEY,
												 S2SIOService.HANDSHAKING_ONLY_KEY);

					// it looks like we are sending verify requests only on handshaking-only
					// connection so there is only one domain for verification
					port_props.put(S2SIOService.HANDSHAKING_DOMAIN_KEY,
												 verify_req.getStanzaTo().toString());
					initNewConnection(dns_entry.getIp(), dns_entry.getPort(), s2s_conn, port_props);
				} catch (UnknownHostException ex) {
					log.log(Level.INFO, "Remote host not found: " + cid.getRemoteHost(), ex);
				}
			}
		}, 0, TimeUnit.MILLISECONDS);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	public void sendPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Sending packets.");
		}
		if (packet != null) {
			if ((firstWaitingTime == 0) || waitingPackets.isEmpty()) {
				firstWaitingTime = System.currentTimeMillis();
			}
			waitingPackets.offer(packet);
		}
		if (sendInProgress.tryLock()) {
			try {
				boolean packetSent = false;
				Packet waiting     = null;

				while ((waiting = waitingPackets.peek()) != null) {
					S2SConnection s2s_conn = getOutgoingConnection(waiting);

					if (s2s_conn != null) {
						try {
							if (s2s_conn.isConnected()) {
								packetSent = s2s_conn.sendPacket(waiting);
								waitingPackets.poll();
								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "Packet: {0} sent over connection: {1}",
												new Object[] { waiting,
																			 s2s_conn.getS2SIOService() });
								}
							}							
							else {
								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "There was a closed connection available - removing "
											+ "connection {0} from set of active connections", s2s_conn);
								}
								outgoing.remove(s2s_conn);
							}
						} catch (Exception ex) {
							log.log(Level.FINE,
											"A problem sending packet, connection broken? Retrying later. {0}",
											waiting);
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST,
											"There is no connection available to send the packet: {0}",
											waiting);
						}

						break;
					}
				}
				if (!packetSent) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
										"No packet could be sent, trying to open more connections: {0}", cid);
					}
					checkOpenConnections();
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
										"Some packets were sent, not trying to open more connections: {0}",
										cid);
					}
				}
			} finally {
				sendInProgress.unlock();
			}
		}
	}

	private void checkOpenConnections() {
		if (outgoingOpenInProgress.compareAndSet(false, true)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Scheduling task for openning a new connection for: {0}",
								cid);
			}
			outgoingOpenTasks.schedule(new Runnable() {
				@Override
				public void run() {
					boolean result = false;

					try {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST,
											"Running scheduled task for openning a new connection for: {0}",
											cid);
						}
						result = openOutgoingConnections();
					} catch (Exception e) {
						log.log(Level.WARNING,
										"uncaughtException in the connection opening thread: ", e);
					} finally {}
					if (!result) {
						outgoingOpenInProgress.set(false);
					}
				}
			}, 0, TimeUnit.MILLISECONDS);
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Outgoing open in progress, skipping for: {0}", cid);
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	private int getOpenForIP(String ip) {
		int result = 0;

		for (S2SConnection s2SConnection : outgoing) {
			if (ip.equals(s2SConnection.getIPAddress())) {
				++result;
			}
		}
		for (S2SConnection s2SConnection : outgoing_handshaking) {
			if (ip.equals(s2SConnection.getIPAddress())) {
				++result;
			}
		}

		return result;
	}

	private S2SConnection getOutgoingConnection(Packet packet) {
		return connectionSelector.selectConnection(packet, outgoing);
	}

	//~--- methods --------------------------------------------------------------

	private void initNewConnection(String ip, int port, S2SConnection s2s_conn,
																 Map<String, Object> port_props) {
		outgoing_handshaking.add(s2s_conn);
		port_props.put(S2SIOService.S2S_CONNECTION_KEY, s2s_conn);
		port_props.put("remote-ip", ip);
		port_props.put("local-hostname", cid.getLocalHost());
		port_props.put("remote-hostname", cid.getRemoteHost());
		port_props.put("ifc", new String[] { ip });
		port_props.put("socket", SocketType.plain);
		port_props.put("type", ConnectionType.connect);
		port_props.put("srv-type", "_xmpp-server._tcp");
		port_props.put("port-no", port);
		port_props.put("cid", cid);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "STARTING new connection: {0}", cid);
			log.log(Level.FINEST, "{0} connection params: {1}",
							new Object[] { cid, port_props });
		}
		handler.initNewConnection(port_props);
	}

	private boolean openOutgoingConnections() {
		boolean result = false;

		try {

			// Check whether all active connections are still active
			for (S2SConnection out_conn : outgoing) {
				if (!out_conn.isConnected()) {
					outgoing.remove(out_conn);
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Removing inactive connection: {0}", out_conn);
					}
				}
			}
			if (firstWaitingTime + max_waiting_time <= System.currentTimeMillis()) {
				sendPacketsBack();
				firstWaitingTime = 0;
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "S2S Timeout expired, sending back: {0}", waitingPackets);
				}

				return result;
			}

			int all_outgoing = outgoing.size() + outgoing_handshaking.size();

			if (all_outgoing >= max_out_conns) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
									"Exceeded max number of outgoing connections, not doing anything: {0}",
									all_outgoing);
				}

				return result;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Checking DNS for host: {0} for: {1}",
								new Object[] { cid.getRemoteHost(),
															 cid });
			}

			// During TTS automated tests we send ping for 200 non-existen domains. On some
			// configurations DNS check for 200 non-existen domains takes forever, so here we
			// have a shortcut to speed the test up.
			// To be sure we do this only for vhosts without a '.' character which are used
			// during TTS tests.
			if (testMode) {
				if (cid.getRemoteHost().startsWith("vhost-") &&
						!cid.getRemoteHost().contains(".")) {
					throw new UnknownHostException(cid.getRemoteHost());
				}
			}

			// using additional domain name mapping to allow usage of intermediate server
			String serverName = handler.getServerNameForDomain(cid.getRemoteHost());
					
			// Check DNS entries
			DNSEntry[] dns_entries = DNSResolverFactory.getInstance().getHostSRV_Entries(serverName);

			// Activate 'missing' connections
			for (DNSEntry dNSEntry : dns_entries) {
				int openForIP = getOpenForIP(dNSEntry.getIp());

				for (int i = openForIP; i < max_out_conns_per_ip; i++) {
					if (dNSEntry.getIp().equals("127.0.0.1")) {

						// DNS misconfiguration for the remote server (icq.jabber.cz for
						// example)
						// Now we assume: UnknownHostException
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, "DNS misconfiguration for domain: {0}, for: {1}",
											new Object[] { cid.getRemoteHost(),
																		 cid });
						}

						throw new UnknownHostException("DNS misconfiguration for domain: " +
																					 cid.getRemoteHost());
					}

					// Create a new connection
					S2SConnection s2s_conn         = new S2SConnection(handler, dNSEntry.getIp());
					Map<String, Object> port_props = new TreeMap<String, Object>();
					port_props.put(S2SIOService.CERT_REQUIRED_DOMAIN, serverName);

					initNewConnection(dNSEntry.getIp(), dNSEntry.getPort(), s2s_conn, port_props);
					result = true;
					if (++all_outgoing >= max_out_conns) {
						return result;
					}
				}
			}
		} catch (UnknownHostException ex) {
			log.log(Level.INFO,
							"Remote host not found: " + cid.getRemoteHost() + ", for: " + cid, ex);
			sendPacketsBack();
		}

		return result;
	}

	private void sendPacketsBack() {
		Packet p = null;

		while ((p = waitingPackets.poll()) != null) {
			try {
				handler.addOutPacket(Authorization.REMOTE_SERVER_NOT_FOUND.getResponseMessage(p,
								"S2S - destination host not found", true));
			} catch (PacketErrorTypeException e) {
				log.log(Level.WARNING, "Packet: {0} processing exception: {1}",
								new Object[] { p.toString(),
															 e });
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/02/18
