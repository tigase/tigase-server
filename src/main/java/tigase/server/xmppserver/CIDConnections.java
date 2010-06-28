
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

import tigase.net.ConnectionType;
import tigase.net.SocketType;

import tigase.server.Packet;

import tigase.util.DNSEntry;
import tigase.util.DNSResolver;

import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

//~--- JDK imports ------------------------------------------------------------

import java.net.UnknownHostException;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 14, 2010 12:32:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CIDConnections {
	private static final Logger log = Logger.getLogger(CIDConnections.class.getName());
	private static final Timer outgoingOpenTasks = new Timer("S2S outgoing open tasks", true);

	//~--- fields ---------------------------------------------------------------

	private CID cid = null;
	private S2SConnectionSelector connectionSelector = null;
	private ConnectionHandlerIfc<S2SIOService> handler = null;
	private int max_in_conns = 4;
	private int max_out_conns = 4;
	private int max_out_conns_per_ip = 2;
	private ReentrantLock outgoingOpenInProgress = new ReentrantLock();
	private Set<S2SConnection> outgoing_handshaking = new ConcurrentSkipListSet<S2SConnection>();
	private Set<S2SConnection> outgoing = new ConcurrentSkipListSet<S2SConnection>();
	private Set<S2SConnection> incoming = new ConcurrentSkipListSet<S2SConnection>();
	private Set<String> dbKeys = new CopyOnWriteArraySet<String>();
	private ConcurrentLinkedQueue<Packet> waitingPackets = new ConcurrentLinkedQueue<Packet>();

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
	 */
	public CIDConnections(CID cid, ConnectionHandlerIfc<S2SIOService> handler,
			S2SConnectionSelector selector, int maxInConns, int maxOutConns, int maxOutConnsPerIP) {
		this.cid = cid;
		this.handler = handler;
		this.connectionSelector = selector;
		this.max_in_conns = maxInConns;
		this.max_out_conns = maxOutConns;
		this.max_out_conns_per_ip = maxOutConnsPerIP;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param key
	 */
	public void addDBKey(String key) {
		dbKeys.add(key);
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void addIncoming(S2SIOService serv) {
		S2SConnection s2s_conn = new S2SConnection(handler, serv.getRemoteAddress());

		s2s_conn.setS2SIOService(serv);
		incoming.add(s2s_conn);
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void connectionAuthenticated(S2SIOService serv) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "{0}, connection is authenticated.", serv);
		}

		S2SConnection s2s_conn =
			(S2SConnection) serv.getSessionData().get(S2SIOService.S2S_CONNECTION_KEY);

		outgoing_handshaking.remove(s2s_conn);
		outgoing.add(s2s_conn);
		sendPacket(null);
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

		switch (serv.connectionType()) {
			case connect :
				this.outgoing.remove(s2s_conn);
				this.outgoing_handshaking.remove(s2s_conn);

				if ( !this.waitingPackets.isEmpty()) {
					checkOpenConnections();
				}

				break;

			case accept :
				this.incoming.remove(s2s_conn);

				break;

			default :
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
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
	 * @return
	 */
	public int getMaxOutConns() {
		return this.max_out_conns;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getMaxOutConnsPerIP() {
		return this.max_out_conns_per_ip;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
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

	//~--- methods --------------------------------------------------------------

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
			waitingPackets.offer(packet);
		}

		boolean packetSent = false;
		Packet waiting = null;

		while ((waiting = waitingPackets.peek()) != null) {
			S2SConnection s2s_conn = getOutgoingConnection(waiting);

			if (s2s_conn != null) {
				try {
					packetSent = s2s_conn.sendPacket(waiting);
					waitingPackets.poll();

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Packet: {0} sent over connection: {1}",
								new Object[] { waiting,
								s2s_conn.getS2SIOService() });
					}
				} catch (Exception ex) {
					log.log(Level.FINE,
							"A problem sending packet, connection broken? Retrying later. {0}", waiting);
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "There is no connection available to send the packet: {0}",
							waiting);
				}

				break;
			}
		}

		if ( !packetSent) {
			checkOpenConnections();
		}
	}

	void reconnectionFailed(Map<String, Object> port_props) {
		S2SConnection s2s_conn = (S2SConnection) port_props.get(S2SIOService.S2S_CONNECTION_KEY);

		if (s2s_conn == null) {
			log.log(Level.INFO, "s2s_conn not set for serv: {0}", port_props);

			return;
		}

		ConnectionType type = (ConnectionType) port_props.get("type");

		if (type != null) {
			switch (type) {
				case connect :
					this.outgoing.remove(s2s_conn);
					this.outgoing_handshaking.remove(s2s_conn);

					if ( !this.waitingPackets.isEmpty()) {
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

	private void checkOpenConnections() {
		if ( !outgoingOpenInProgress.isLocked()) {
			outgoingOpenTasks.schedule(new TimerTask() {
				@Override
				public void run() {
					if (outgoingOpenInProgress.tryLock()) {
						try {
							openOutgoingConnections();
						} catch (Exception e) {
							log.log(Level.WARNING, "uncaughtException in the connection opening thread: ",
									e);
						} finally {

							// Release the 'lock'
							outgoingOpenInProgress.unlock();
						}
					}
				}
			}, 0);
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

	private void openOutgoingConnections() {
		try {

			// Check whether all active connections are still active
			for (S2SConnection out_conn : outgoing) {
				if ( !out_conn.isConnected()) {
					outgoing.remove(out_conn);
				}
			}

			int all_outgoing = outgoing.size() + outgoing_handshaking.size();

			if (all_outgoing >= max_out_conns) {
				return;
			}

			// Check DNS entries
			DNSEntry[] dns_entries = DNSResolver.getHostSRV_Entries(cid.getRemoteHost());

			// Activate 'missing' connections
			for (DNSEntry dNSEntry : dns_entries) {
				int openForIP = getOpenForIP(dNSEntry.getIp());

				for (int i = openForIP; i < max_out_conns_per_ip; i++) {

					// Create a new connection
					S2SConnection s2s_conn = new S2SConnection(handler, dNSEntry.getIp());

					outgoing_handshaking.add(s2s_conn);

					Map<String, Object> port_props = new TreeMap<String, Object>();

					port_props.put(S2SIOService.S2S_CONNECTION_KEY, s2s_conn);
					port_props.put("remote-ip", dNSEntry.getIp());
					port_props.put("local-hostname", cid.getLocalHost());
					port_props.put("remote-hostname", cid.getRemoteHost());
					port_props.put("ifc", new String[] { dNSEntry.getIp() });
					port_props.put("socket", SocketType.plain);
					port_props.put("type", ConnectionType.connect);
					port_props.put("port-no", dNSEntry.getPort());
					port_props.put("cid", cid);
					log.log(Level.FINEST, "STARTING new connection: {0}", cid);
					handler.initNewConnection(port_props);

					if (++all_outgoing >= max_out_conns) {
						return;
					}
				}
			}
		} catch (UnknownHostException ex) {
			log.log(Level.INFO, "Remote host not found: " + cid.getRemoteHost(), ex);

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
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
