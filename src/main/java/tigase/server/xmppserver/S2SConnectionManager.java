
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

import tigase.server.ConnectionManager;
import tigase.server.Packet;

import tigase.stats.StatisticsList;

import tigase.util.Algorithms;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.security.NoSuchAlgorithmException;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 14, 2010 11:59:38 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SConnectionManager extends ConnectionManager<S2SIOService>
		implements ConnectionHandlerIfc<S2SIOService> {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(S2SConnectionManager.class.getName());
	private static final String XMLNS_SERVER_VAL = "jabber:server";
	private static final String XMLNS_CLIENT_VAL = "jabber:client";
	private static final String XMLNS_DB_VAL = "jabber:server:dialback";
	private static final String RESULT_EL_NAME = "result";
	private static final String VERIFY_EL_NAME = "verify";
	protected static final String DB_RESULT_EL_NAME = "db:result";
	protected static final String DB_VERIFY_EL_NAME = "db:verify";
	private static final String XMLNS_DB_ATT = "xmlns:db";

	/** Field description */
	public static final String MAX_PACKET_WAITING_TIME_PROP_KEY = "max-packet-waiting-time";

	/** Field description */
	public static final String MAX_CONNECTION_INACTIVITY_TIME_PROP_KEY = "max-inactivity-time";

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
	public static final int MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL = 4;

	/** Field description */
	public static final int MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL = 1;

	/** Field description */
	public static final long MAX_PACKET_WAITING_TIME_PROP_VAL = 7 * MINUTE;

	/** Field description */
	public static final long MAX_CONNECTION_INACTIVITY_TIME_PROP_VAL = 15 * MINUTE;

	//~--- fields ---------------------------------------------------------------

	private S2SConnectionSelector connSelector = null;

	/**
	 * <code>maxPacketWaitingTime</code> keeps the maximum time packets
	 * can wait for sending in ServerPacketQueue. Packets are put in the
	 * queue only when connection to remote server is not established so
	 * effectively this timeout specifies the maximum time for connecting
	 * to remote server. If this time is exceeded then no more reconnecting
	 * attempts are performed and packets are sent back with error information.
	 *
	 * Default TCP/IP timeout is 300 seconds so we can follow this convention
	 * but administrator can set different timeout in server configuration.
	 */
	private long maxPacketWaitingTime = MAX_PACKET_WAITING_TIME_PROP_VAL;
	private int maxOUTTotalConnections = MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL;
	private int maxOUTPerIPConnections = MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL;
	private long maxInactivityTime = MAX_CONNECTION_INACTIVITY_TIME_PROP_VAL;
	private int maxINConnections = MAX_INCOMING_CONNECTIONS_PROP_VAL;

	/**
	 * Outgoing and incoming connections for a given domains pair (localdomain, remotedomain)
	 */
	private Map<CID, CIDConnections> cidConnections = new ConcurrentHashMap<CID,
		CIDConnections>(10000);

	//~--- methods --------------------------------------------------------------

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

		props.put(MAX_PACKET_WAITING_TIME_PROP_KEY, MAX_PACKET_WAITING_TIME_PROP_VAL / SECOND);
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
			long total_outgoing_handshaking = 0;
			long total_incoming = 0;
			long total_dbKeys = 0;
			long total_waiting = 0;
			long total_waiting_control = 0;

			for (Map.Entry<CID, CIDConnections> cid_conn : cidConnections.entrySet()) {
				int outgoing = cid_conn.getValue().getOutgoingCount();
				int outgoing_handshaking = cid_conn.getValue().getOutgoingHandshakingCount();
				int incoming = cid_conn.getValue().getIncomingCount();
				int dbKeys = cid_conn.getValue().getDBKeysCount();
				int waiting = cid_conn.getValue().getWaitingCount();
				int waiting_control = cid_conn.getValue().getWaitingControlCount();

				log.log(Level.FINEST,
						"CID: {0}, OUT: {1}, OUT_HAND: {2}, IN: {3}, dbKeys: {4}, "
							+ "waiting: {5}, waiting_control: {6}", new Object[] {
					cid_conn.getKey(), outgoing, outgoing_handshaking, incoming, dbKeys, waiting,
					waiting_control
				});
				total_outgoing += outgoing;
				total_outgoing_handshaking += outgoing_handshaking;
				total_incoming += incoming;
				total_dbKeys += dbKeys;
				total_waiting += waiting;
				total_waiting_control += waiting_control;
			}

			list.add(getName(), "Total outgoing", total_outgoing, Level.FINEST);
			list.add(getName(), "Total outgoing handshaking", total_outgoing_handshaking,
					Level.FINEST);
			list.add(getName(), "Total incoming", total_incoming, Level.FINEST);
			list.add(getName(), "Total DB keys", total_dbKeys, Level.FINEST);
			list.add(getName(), "Total waiting", total_waiting, Level.FINEST);
			list.add(getName(), "Total control waiting", total_waiting_control, Level.FINEST);
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

		// Calculate hash code from the destination domain name to make sure packets for
		// a single domain are processed by the same thread to avoid race condition
		// creating new connection data structures for a destination domain
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().getDomain().hashCode();
		}

		// Otherwise, it might be a control packet which can be processed by single thread
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

		if ((packet.getStanzaTo() == null) || packet.getStanzaTo().getDomain().trim().isEmpty()) {
			log.log(Level.WARNING,
					"Missing ''to'' attribute, ignoring packet...{0}"
						+ "\n This most likely happens due to missconfiguration of components"
							+ " domain names.", packet);

			return;
		}

		if ((packet.getStanzaFrom() == null)
				|| packet.getStanzaFrom().getDomain().trim().isEmpty()) {
			log.log(Level.WARNING, "Missing ''from'' attribute, ignoring packet...{0}", packet);

			return;
		}

		// Check whether addressing is correct:
		String to_hostname = packet.getStanzaTo().getDomain();

		// We don't send packets to local domains trough s2s, there
		// must be something wrong with configuration
		if (isLocalDomainOrComponent(to_hostname)) {

			// Ups, remote hostname is the same as one of local hostname??
			// Internal loop possible, we don't want that....
			// Let's send the packet back....
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Packet addresses to localhost, I am not processing it: {0}",
						packet);
			}

			try {
				addOutPacket(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"S2S - not delivered. Server missconfiguration.", true));
			} catch (PacketErrorTypeException e) {
				log.log(Level.WARNING, "Packet processing exception: {0}", e);
			}

			return;
		}

		// I think from_hostname needs to be different from to_hostname at
		// this point... or s2s doesn't make sense
		String from_hostname = packet.getStanzaFrom().getDomain();

		// All hostnames go through String.intern()
		if (to_hostname == from_hostname) {
			log.log(Level.WARNING, "Dropping incorrect packet - from_hostname == to_hostname: {0}",
					packet);

			return;
		}

		CID cid = getConnectionId(from_hostname, to_hostname);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Connection ID is: {0}", cid);
		}

		CIDConnections cid_conns = getCIDConnections(cid);

		if (cid_conns == null) {
			cid_conns = createNewCIDConnections(cid);
		}

		Packet server_packet = packet.copyElementOnly();

		server_packet.getElement().removeAttribute("xmlns");
		cid_conns.sendPacket(server_packet);
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

		while ((p = packets.poll()) != null) {

//    log.finer("Processing packet: " + p.getElemName()
//      + ", type: " + p.getType());
			if (p.getXMLNS() == XMLNS_SERVER_VAL) {
				p.getElement().setXMLNS(XMLNS_CLIENT_VAL);
			}

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Processing socket data: {1}", new Object[] { serv, p });
			}

			if (p.getXMLNS() == XMLNS_DB_VAL) {
				processDialback(p, serv);
			} else {
				if (p.getElemName() == "error") {
					processStreamError(p, serv);

					return null;
				} else {
					if (checkPacket(p, serv)) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "{0}, Adding packet out: {1}", new Object[] { serv, p });
						}

						addOutPacket(p);
					} else {
						return null;
					}
				}
			}    // end of else
		}      // end of while ()

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
	 * @param serv
	 */
	@Override
	public void serviceStarted(S2SIOService serv) {
		super.serviceStarted(serv);
		log.log(Level.FINEST, "s2s connection opened: {0}", serv);

		switch (serv.connectionType()) {
			case connect :
				CID cid = (CID) serv.getSessionData().get("cid");

				// Send init xmpp stream here
				// XMPPIOService serv = (XMPPIOService)service;
				String data = "<stream:stream" + " xmlns:stream='http://etherx.jabber.org/streams'"
					+ " xmlns='jabber:server'" + " xmlns:db='jabber:server:dialback'" + " from='"
					+ cid.getLocalHost() + "'" + " to='" + cid.getRemoteHost() + "'" + ">";

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, sending: {1}", new Object[] { serv, data });
				}

				S2SConnection s2s_conn =
					(S2SConnection) serv.getSessionData().get(S2SIOService.S2S_CONNECTION_KEY);

				if (s2s_conn == null) {
					log.log(Level.WARNING,
							"Protocol error s2s_connection not set for outgoing connection: {0}", serv);
					serv.stop();
				} else {
					s2s_conn.setS2SIOService(serv);
					serv.setS2SConnection(s2s_conn);
				}

				serv.xmppStreamOpen(data);

				break;

			default :

				// Do nothing, more data should come soon...
				break;
		}    // end of switch (service.connectionType())
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
			CID cid = (CID) serv.getSessionData().get("cid");

			if (cid == null) {
				if (serv.connectionType() == ConnectionType.connect) {
					log.log(Level.WARNING, "Protocol error cid not set for outgoing connection: {0}",
							serv);
				}

				return result;
			}

			CIDConnections cid_conns = getCIDConnections(cid);

			if (cid_conns == null) {
				log.log(Level.WARNING,
						"Protocol error cid_conns not found for outgoing connection: {0}", serv);

				return result;
			} else {
				cid_conns.connectionStopped(serv);
			}
		}

		return result;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		maxPacketWaitingTime = (Long) props.get(MAX_PACKET_WAITING_TIME_PROP_KEY) * SECOND;
		maxInactivityTime = (Long) props.get(MAX_CONNECTION_INACTIVITY_TIME_PROP_KEY) * SECOND;
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

	//~--- methods --------------------------------------------------------------

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

//  if ((remote_hostname == null) || (local_hostname == null)) {
//    generateStreamError(serv.connectionType() == ConnectionType.accept,
//        "improper-addressing", serv);
//
//    return null;
//  }
//
//  if ( !this.isLocalDomainOrComponent(local_hostname)) {
//    generateStreamError(serv.connectionType() == ConnectionType.accept, "host-unknown",
//        serv);
//
//    return null;
//  }
		CID cid = (CID) serv.getSessionData().get("cid");

		if (cid == null) {
			String remote_hostname = attribs.get("from");
			String local_hostname = attribs.get("to");

			if ((remote_hostname != null) && (local_hostname != null)) {
				cid = getConnectionId(local_hostname, remote_hostname);
			}
		}

		CIDConnections cid_conns = getCIDConnections(cid);

		switch (serv.connectionType()) {
			case connect : {

				// It must be always set for connect connection type
				String remote_id = attribs.get("id");

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Connect Stream opened for: {1}, session id{2}",
							new Object[] { serv,
							cid, remote_id });
				}

				if (cid_conns == null) {

					// This should actually not happen. Let's be clear here about handling unexpected
					// cases.
					log.log(Level.WARNING, "{0} This might be a bug in s2s code, should not happen."
							+ " Missing CIDConnections for stream open to ''connect'' service type.", serv);
					generateStreamError(false, "internal-server-error", serv);

					return null;
				}

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, stream open for cid: {1}, outgoint: {2}, incoming: {3}",
							new Object[] { serv,
							cid, cid_conns.getOutgoingCount(), cid_conns.getIncomingCount() });
				}

				serv.setSessionId(remote_id);

				String uuid = UUID.randomUUID().toString();
				String key = null;

				try {
					key = Algorithms.hexDigest(remote_id, uuid, "SHA");
				} catch (NoSuchAlgorithmException e) {
					key = uuid;
				}    // end of try-catch

				serv.setDBKey(key);
				cid_conns.addDBKey(remote_id, key);

				if ( !serv.isHandshakingOnly()) {
					Element elem = new Element(DB_RESULT_EL_NAME, key, new String[] { XMLNS_DB_ATT },
						new String[] { XMLNS_DB_VAL });

					serv.getS2SConnection().addControlPacket(Packet.packetInstance(elem,
							JID.jidInstanceNS(cid.getLocalHost()), JID.jidInstanceNS(cid.getRemoteHost())));
				}

				serv.getS2SConnection().sendAllControlPackets();

				return null;
			}

			case accept : {
				String id = UUID.randomUUID().toString();

				serv.setSessionId(id);

				String stream_open = "<stream:stream"
					+ " xmlns:stream='http://etherx.jabber.org/streams'" + " xmlns='jabber:server'"
					+ " xmlns:db='jabber:server:dialback'" + " id='" + id + "'";

				if (cid != null) {
					stream_open += " from='" + cid.getLocalHost() + "'" + " to='" + cid.getRemoteHost()
							+ "'";

					if (cid_conns == null) {
						cid_conns = createNewCIDConnections(cid);
					}

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Accept Stream opened for: {1}, session id: {2}",
								new Object[] { serv,
								cid, id });
					}

					serv.getSessionData().put("cid", cid);
					cid_conns.addIncoming(serv);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"{0}, Accept Stream opened for unknown CID, session id: {1}",
									new Object[] { serv,
								id });
					}
				}

				stream_open += ">";

				return stream_open;
			}

			default :
				log.log(Level.SEVERE, "{0}, Warning, program shouldn't reach that point.", serv);

				break;
		}    // end of switch (serv.connectionType())

		return null;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	protected int[] getDefPlainPorts() {
		return new int[] { 5269 };
	}

	protected String getLocalDBKey(CID cid, String key, String key_sessionId,
			String asking_sessionId) {
		CIDConnections cid_conns = getCIDConnections(cid);

		return (cid_conns == null) ? null : cid_conns.getDBKey(key_sessionId);
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

	//~--- methods --------------------------------------------------------------

	protected boolean sendVerifyResult(String elem_name, CID cid, boolean valid,
			String key_sessionId, String serv_sessionId) {
		CIDConnections cid_conns = getCIDConnections(cid);

		if (cid_conns != null) {
			Packet verify_valid = getValidResponse(elem_name, cid, key_sessionId, valid, null);

			return cid_conns.sendControlPacket(serv_sessionId, verify_valid);
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"Can''t find CID connections for cid: {0}, can't send verify response.", cid);
			}
		}

		return false;
	}

	private boolean checkPacket(Packet p, S2SIOService serv) {
		if ((p.getStanzaFrom() == null) || (p.getStanzaFrom().getDomain().trim().isEmpty())
				|| (p.getStanzaTo() == null) || p.getStanzaTo().getDomain().trim().isEmpty()) {
			generateStreamError(false, "improper-addressing", serv);

			return false;
		}

		CID cid = getConnectionId(p.getStanzaTo().getDomain(), p.getStanzaFrom().getDomain());

		// String remote_hostname = (String) serv.getSessionData().get("remote-hostname");
		if ( !serv.isAuthenticated(cid)) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER,
						"{0}, Invalid hostname from the remote server for packet: "
							+ "{1}, authenticated domains for this connection: {2}", new Object[] { serv,
						p, serv.getCIDs() });
			}

			generateStreamError(false, "invalid-from", serv);

			return false;
		}

		return true;
	}

	private CIDConnections createNewCIDConnections(CID cid) {
		CIDConnections cid_conns = new CIDConnections(cid, this, connSelector, maxINConnections,
			maxOUTTotalConnections, maxOUTPerIPConnections, maxPacketWaitingTime);

		cidConnections.put(cid, cid_conns);

		return cid_conns;
	}

	private void generateStreamError(boolean initStream, String error_el, S2SIOService serv) {
		String strError = "";

		if (initStream) {
			strError += "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS_SERVER_VAL + "'"
					+ " xmlns:stream='http://etherx.jabber.org/streams'" + " id='tigase-server-error'"
						+ " from='" + getDefHostName() + "'" + " xml:lang='en'>";
		}

		strError += "<stream:error>" + "<" + error_el
				+ " xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" + "</stream:error>"
					+ "</stream:stream>";

		try {
			writeRawData(serv, strError);
			serv.stop();
		} catch (Exception e) {
			serv.forceStop();
		}
	}

	//~--- get methods ----------------------------------------------------------

	private CIDConnections getCIDConnections(CID cid) {
		if (cid == null) {
			return null;
		}

		return cidConnections.get(cid);
	}

	private CID getConnectionId(String localhost, String remotehost) {
		return new CID(localhost, remotehost);
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

		Packet result = Packet.packetInstance(elem, JID.jidInstanceNS(cid.getLocalHost()),
			JID.jidInstanceNS(cid.getRemoteHost()));

		return result;
	}

	//~--- methods --------------------------------------------------------------

	private void processDialback(Packet p, S2SIOService serv) {

		// Get the cid for which the connection has been created, the cid calculated
		// from the packet may be different though if the remote server tries to multiplexing
		CID cid = (CID) serv.getSessionData().get("cid");

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, DIALBACK packet: {1}, CID: {2}", new Object[] { serv, p,
					cid });
		}

		CIDConnections cid_conns = getCIDConnections(cid);

		// Some servers (ejabberd) do not send from/to attributes in the stream:open which
		// violates the spec, they seem not to care though, so here we handle the case.
		if (cid == null) {

			// This actually can only happen for 'accept' connection type
			// what we did not get in stream open we can get from here
			cid = getConnectionId(p.getStanzaTo().getDomain(), p.getStanzaFrom().getDomain());
			cid_conns = getCIDConnections(cid);

			if (cid_conns == null) {
				cid_conns = createNewCIDConnections(cid);
			}

			serv.getSessionData().put("cid", cid);
			cid_conns.addIncoming(serv);
		}

		if (cid == null) {
			log.log(Level.WARNING,
					"{0} This might be a bug in the code or there was no stream "
						+ "open on this connection, CID is not set.", serv);
			generateStreamError(false, "reset", serv);

			return;
		}

		if (cid_conns == null) {

			// Hm, this should not happen, is it a bug?
			log.log(Level.INFO, "{0} CID connections not found for cid: {1}, packet: {2}",
					new Object[] { serv,
					cid, p });
			generateStreamError(false, "reset", serv);

			return;
		}

		String remote_key = p.getElemCData();

		// Dummy dialback implementation for now....
		if ((p.getElemName() == RESULT_EL_NAME) || (p.getElemName() == DB_RESULT_EL_NAME)) {
			if (p.getType() == null) {
				String conn_sessionId = serv.getSessionId();
				Packet verify_req = this.getValidResponse(DB_VERIFY_EL_NAME, cid, conn_sessionId,
					null, p.getElemCData());

				cid_conns.sendHandshakingOnly(verify_req);
			} else {
				if (p.getType() == StanzaType.valid) {
					serv.addCID(getConnectionId(p.getStanzaTo().getDomain(),
							p.getStanzaFrom().getDomain()));
					cid_conns.connectionAuthenticated(serv);
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								"Invalid result for DB authentication: {0}, stopping connection: {1}",
									new Object[] { cid,
								serv });
					}

					serv.stop();
				}
			}
		}

		if ((p.getElemName() == VERIFY_EL_NAME) || (p.getElemName() == DB_VERIFY_EL_NAME)) {
			if (p.getType() == null) {
				String local_key = getLocalDBKey(cid, remote_key, p.getStanzaId(),
					serv.getSessionId());

				if (local_key == null) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER,
								"The key is not available for connection: {0} maybe it is "
									+ "located on a different node...", cid);
					}
				} else {
					sendVerifyResult(DB_VERIFY_EL_NAME, cid, local_key.equals(remote_key),
							p.getStanzaId(), serv.getSessionId());
				}
			} else {
				sendVerifyResult(DB_RESULT_EL_NAME, cid, (p.getType() == StanzaType.valid), null,
						p.getStanzaId());
				cid_conns.connectionAuthenticated(p.getStanzaId());

				if (serv.isHandshakingOnly()) {
					serv.stop();
				}
			}
		}
	}

	private void processStreamError(Packet p, S2SIOService serv) {
		serv.stop();
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
