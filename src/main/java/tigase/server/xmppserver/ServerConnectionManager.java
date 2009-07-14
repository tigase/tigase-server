/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
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

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.net.ConnectionType;
//import tigase.net.IOService;
import tigase.net.SocketType;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.util.Algorithms;
import tigase.util.DNSResolver;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.stats.StatRecord;
import tigase.stats.StatisticsList;

/**
 * Class ServerConnectionManager
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServerConnectionManager extends ConnectionManager<XMPPIOService>
	implements ConnectionHandlerIfc {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppserver.ServerConnectionManager");

	private static final String XMLNS_DB_ATT = "xmlns:db";
	private static final String XMLNS_DB_VAL = "jabber:server:dialback";
	private static final String RESULT_EL_NAME = "result";
	private static final String DB_RESULT_EL_NAME = "db:result";
	private static final String VERIFY_EL_NAME = "verify";
	private static final String DB_VERIFY_EL_NAME = "db:verify";
//	public static final String HOSTNAMES_PROP_KEY = "hostnames";
//	public String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};
	public static final String MAX_PACKET_WAITING_TIME_PROP_KEY =
		"max-packet-waiting-time";
	public static final long MAX_PACKET_WAITING_TIME_PROP_VAL = 7*MINUTE;
	private long new_connection_thread_counter = 0;

//	private String[] hostnames = HOSTNAMES_PROP_VAL;

	/**
	 * <code>maxPacketWaitingTime</code> keeps the maximum time packets
	 * can wait for sending in ServerPacketQueue. Packets are put in the
	 * queue only when connection to remote server is not established so
	 * effectively this timeout specifies the maximum time for connecting
	 * to remote server. If this time is exceeded then no more reconnecting
	 * attempts are performed and packets are sent back with error information.
	 *
	 * Default TCP/IP timeout is 300 seconds to we can follow this convention
	 * but administrator can set different timeout in server configuration.
	 */
	private long maxPacketWaitingTime = MAX_PACKET_WAITING_TIME_PROP_VAL;

	/**
	 * Services connected and autorized/autenticated
	 */
	private Map<String, ServerConnections> connectionsByLocalRemote =
		new ConcurrentSkipListMap<String, ServerConnections>();

	/**
	 * Incoming (accept) services by sessionId. Some servers (EJabberd) opens
	 * many connections for each domain, especially when in cluster mode.
	 */
	private ConcurrentSkipListMap<String, XMPPIOService> incoming =
    new ConcurrentSkipListMap<String, XMPPIOService>();

	private Timer connectionWatchdog = new Timer("s2s connections watchdog");

	protected ServerConnections getServerConnections(String cid) {
		return connectionsByLocalRemote.get(cid);
	}

	protected ServerConnections removeServerConnections(String cid) {
		return connectionsByLocalRemote.remove(cid);
	}

	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toString());
		}
		if (!packet.isCommand() || !processCommand(packet)) {

			if (packet.getElemTo() == null) {
				log.warning("Missing 'to' attribute, ignoring packet..."
					+ packet.toString()
					+ "\n This most likely happens due to missconfiguration of components domain names.");
				return;
			}

			if (packet.getElemFrom() == null) {
				log.warning("Missing 'from' attribute, ignoring packet..."
					+ packet.toString());
				return;
			}

			// Check whether addressing is correct:
			String to_hostname = JIDUtils.getNodeHost(packet.getElemTo());
			// We don't send packets to local domains trough s2s, there
			// must be something wrong with configuration
			if (isLocalDomainOrComponent(to_hostname)) {
				// Ups, remote hostname is the same as one of local hostname??
				// Internal loop possible, we don't want that....
				// Let's send the packet back....
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Packet addresses to localhost, I am not processing it: "
						+ packet.getStringData());
				}
				try {
					addOutPacket(
						Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
							"S2S - not delivered. Server missconfiguration.", true));
				} catch (PacketErrorTypeException e) {
					log.warning("Packet processing exception: " + e);
				}
				return;
			}

			// I think from_hostname needs to be different from to_hostname at
			// this point... or s2s doesn't make sense
			String from_hostname = JIDUtils.getNodeHost(packet.getElemFrom());
			if (to_hostname.equals(from_hostname)) {
				log.warning("Dropping incorrect packet - from_hostname == to_hostname: "
					+ packet.toString());
				return;
			}

			String cid = getConnectionId(packet);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Connection ID is: " + cid);
			}
			ServerConnections serv_conn = getServerConnections(cid);
			if (serv_conn == null
				|| (!serv_conn.sendPacket(packet) && serv_conn.needsConnection())) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Couldn't send packet, creating a new connection.");
				}
				createServerConnection(cid, packet, serv_conn);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Packet seems to be sent correctly: " + packet.toString());
				}
			}
		} // end of else
	}

	private ServerConnections createNewServerConnections(String cid, Packet packet) {
		ServerConnections conns = new ServerConnections(this, cid);
		if (packet != null) {
			if (packet.getElement().getXMLNS() == XMLNS_DB_VAL) {
				conns.addControlPacket(packet);
			} else {
				conns.addDataPacket(packet);
			}
		}
		connectionsByLocalRemote.put(cid, conns);
		return conns;
	}

	/**
	 * Mehtod <code>createServerConnection</code> is called only when a new
	 * connection is needed for any reason for given hostnames combination.
	 *
	 * @param cid a <code>String</code> s2s connection ID (localhost@remotehost)
	 * @param packet a <code>Packet</code> packet to send, should be passed to the
	 * ServerConnections only when it was null.
	 * @param serv_conn a <code>ServerConnections</code> which was called for
	 * the packet.
	 */
	private void createServerConnection(final String cid, final Packet packet,
		final ServerConnections serv_conn) {
		// Spawning a new thread for each new server connection is not the most
		// optimal solution but I have no idea how to do it better and solve
		// the long DNS resolution problem.
		// On the other hand, new server connections are not opened as often
		// so it should not be a big problem. Let's see how it works.
    Thread new_connection_thread = new Thread("NewServerConnection-" +
						(++new_connection_thread_counter)) {
			@Override
			public void run() {
				createServerConnectionInThread(cid, packet, serv_conn);
			}
		};
		new_connection_thread.start();
	}

	private void createServerConnectionInThread(String cid, Packet packet,
		ServerConnections serv_conn) {

		ServerConnections conns = serv_conn;
		if (conns == null) {
			conns = createNewServerConnections(cid, packet);
		}

		String localhost = JIDUtils.getNodeNick(cid);
		String remotehost = JIDUtils.getNodeHost(cid);
		if (openNewServerConnection(localhost, remotehost)) {
			conns.setConnecting();
			new ConnectionWatchdogTask(conns, localhost, remotehost);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Connecting a new s2s service: " + cid);
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Couldn't open a new s2s service: (UknownHost??) " + cid);
			}
			// Can't establish connection...., unknown host??
			Queue<Packet> waitingPackets = conns.getWaitingPackets();
			// Well, is somebody injects a packet with the same sender and
			// receiver domain and this domain is not valid then we have
			// infinite loop here....
			// Let's try to handle this by dropping such packet.
			// It may happen as well that the source domain is different from
			// target domain and both are invalid, what then?
			// The best option would be to drop the packet if it is already an
			// error - remote-server-not-found....
			// For dialback packet just ignore the error completely as it means
			// remote server tries to connect from domain which doesn't exist
			// in DNS so no further action should be performed.
			Packet p = null;
			while ((p = waitingPackets.poll()) != null) {
				if (p.getElement().getXMLNS() != XMLNS_DB_VAL) {
					try {
						addOutPacket(
							Authorization.REMOTE_SERVER_NOT_FOUND.getResponseMessage(p,
							"S2S - destination host not found", true));
					} catch (PacketErrorTypeException e) {
						log.warning("Packet: " + p.toString() + " processing exception: " + e);
					}
				}
			}
			conns.stopAll();
			//connectionsByLocalRemote.remove(cid);
		}
	}

// 	private void dumpCurrentStack(StackTraceElement[] stack) {
// 		StringBuilder sb = new StringBuilder();
// 		for (StackTraceElement st_el: stack) {
// 			sb.append("\n" + st_el.toString());
// 		}
// 		log.finest(sb.toString());
// 	}

	private boolean openNewServerConnection(String localhost, String remotehost) {

		//		dumpCurrentStack(Thread.currentThread().getStackTrace());

		try {
			String ipAddress = DNSResolver.getHostSRV_IP(remotehost);
			Map<String, Object> port_props = new TreeMap<String, Object>();
			port_props.put("remote-ip", ipAddress);
			port_props.put("local-hostname", localhost);
			port_props.put("remote-hostname", remotehost);
			port_props.put("ifc", new String[] {ipAddress});
			port_props.put("socket", SocketType.plain);
			port_props.put("type", ConnectionType.connect);
			port_props.put("port-no", 5269);
			String cid = getConnectionId(localhost, remotehost);
			port_props.put("cid", cid);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("STARTING new connection: " + cid);
			}
			addWaitingTask(port_props);
			//reconnectService(port_props, 5*SECOND);
			return true;
		} catch (UnknownHostException e) {
			log.info("UnknownHostException for host: " + remotehost);
			return false;
		} // end of try-catch

	}

	private String getConnectionId(String localhost, String remotehost) {
		return JIDUtils.getJID(localhost, remotehost, null);
	}

	private String getConnectionId(Packet packet) {
		return JIDUtils.getJID(JIDUtils.getNodeHost(packet.getElemFrom()),
			JIDUtils.getNodeHost(packet.getElemTo()), null);
	}

	private String getConnectionId(XMPPIOService service) {
		String local_hostname =
			(String)service.getSessionData().get("local-hostname");
		String remote_hostname =
			(String)service.getSessionData().get("remote-hostname");
		String cid = getConnectionId(local_hostname,
			(remote_hostname != null ? remote_hostname : "NULL"));
		return cid;
	}

	@Override
	public Queue<Packet> processSocketData(XMPPIOService serv) {
		Queue<Packet> packets = serv.getReceivedPackets();
		Packet p = null;
		while ((p = packets.poll()) != null) {
// 			log.finer("Processing packet: " + p.getElemName()
// 				+ ", type: " + p.getType());
			if (p.getElement().getXMLNS() == null) {
				p.getElement().setXMLNS("jabber:client");
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Processing socket data: " + p.toString());
			}

			if (p.getElement().getXMLNS() == XMLNS_DB_VAL) {
				processDialback(p, serv);
			} else {
				if (p.getElemName() == "error") {
					processStreamError(p, serv);
					return null;
				} else {
					if (checkPacket(p, serv)) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Adding packet out: " + p.getStringData());
						}
						addOutPacket(p);
					} else {
						return null;
					}
				}
			} // end of else
		} // end of while ()
 		return null;
	}

	private void bouncePacketsBack(Authorization author, String cid) {
		ServerConnections serv_conns = getServerConnections(cid);
		if (serv_conns != null) {
			Queue<Packet> waiting =	serv_conns.getWaitingPackets();
			Packet p = null;
			while ((p = waiting.poll()) != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending packet back: " + p.getStringData());
				}
				try {
					addOutPacket(author.getResponseMessage(p, "S2S - not delivered", true));
				} catch (PacketErrorTypeException e) {
					log.info("Packet processing exception: " + e);
				}
			} // end of while (p = waitingPackets.remove(ipAddress) != null)
		} else {
			log.info("No ServerConnections for cid: " + cid);
		}
	}


	private void processStreamError(Packet packet, XMPPIOService serv) {
		Authorization author = Authorization.RECIPIENT_UNAVAILABLE;
		if (packet.getElement().getChild("host-unknown") != null) {
			author = Authorization.REMOTE_SERVER_NOT_FOUND;
		}
		String cid = getConnectionId(serv);
		bouncePacketsBack(author, cid);
		serv.stop();
	}

	private boolean checkPacket(Packet packet, XMPPIOService serv) {
		String packet_from = packet.getElemFrom();
		String packet_to = packet.getElemTo();
		if (packet_from == null || packet_to == null) {
			generateStreamError("improper-addressing", serv);
			return false;
		}
		String remote_hostname =
			(String)serv.getSessionData().get("remote-hostname");
		if (!JIDUtils.getNodeHost(packet_from).equals(remote_hostname)) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Invalid hostname from the remote server, expected: " +
								remote_hostname);
			}
			generateStreamError("invalid-from", serv);
			return false;
		}
		String local_hostname =	(String)serv.getSessionData().get("local-hostname");
		if (!JIDUtils.getNodeHost(packet_to).equals(local_hostname)) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Invalid hostname of the local server, expected: " +
								local_hostname);
			}
			generateStreamError("host-unknown", serv);
			return false;
		}
		String cid = getConnectionId(serv);
		String session_id = (String)serv.getSessionData().get(serv.SESSION_ID_KEY);
		if (!isIncomingValid(session_id)) {
			log.info("Incoming connection hasn't been validated");
			return false;
		}
		return true;
	}

	public boolean isIncomingValid(String session_id) {
		if (session_id == null) {
			return false;
		}
		XMPPIOService serv = incoming.get(session_id);
		if (serv == null || serv.getSessionData().get("valid") == null) {
			return false;
		} else {
			return (Boolean)serv.getSessionData().get("valid");
		}
	}

	private boolean processCommand(final Packet packet) {
		//		XMPPIOService serv = getXMPPIOService(packet);
		switch (packet.getCommand()) {
		case STARTTLS:
			break;
		case STREAM_CLOSED:
			break;
		case GETDISCO:
			break;
		case CLOSE:
			break;
		default:
			break;
		} // end of switch (pc.getCommand())
		return false;
	}

	@Override
	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {

		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream opened: " + attribs.toString());
		}

		switch (serv.connectionType()) {
		case connect: {
			// It must be always set for connect connection type
			String remote_hostname =
				(String)serv.getSessionData().get("remote-hostname");
			String local_hostname =
				(String)serv.getSessionData().get("local-hostname");
			String cid = getConnectionId(local_hostname, remote_hostname);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Stream opened for: " + cid);
			}
			ServerConnections serv_conns = getServerConnections(cid);
			if (serv_conns == null) {
				serv_conns = createNewServerConnections(cid, null);
			}
			serv_conns.addOutgoing(serv);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Counters: ioservices: " + countIOServices()
					+ ", s2s connections: " + countOpenConnections());
			}
			String remote_id = attribs.get("id");
			serv.getSessionData().put(serv.SESSION_ID_KEY, remote_id);
			String uuid = UUID.randomUUID().toString();
			String key = null;
			try {
				key = Algorithms.hexDigest(remote_id, uuid, "SHA");
			} catch (NoSuchAlgorithmException e) {
				key = uuid;
			} // end of try-catch
			serv_conns.putDBKey(remote_id, key);
			Element elem = new Element(DB_RESULT_EL_NAME, key,
				new String[] {"from", "to", XMLNS_DB_ATT},
				new String[] {local_hostname, remote_hostname, XMLNS_DB_VAL});
			serv_conns.addControlPacket(new Packet(elem));
			serv_conns.sendAllControlPackets();
			return null;
		}
		case accept: {
			String remote_hostname =
				(String)serv.getSessionData().get("remote-hostname");
			String local_hostname =
				(String)serv.getSessionData().get("local-hostname");
			String cid = getConnectionId(
				(local_hostname != null ? local_hostname : "NULL"),
				(remote_hostname != null ? remote_hostname : "NULL"));
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Stream opened for: " + cid);
			}
			if (remote_hostname != null) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Opening stream for already established connection...., trying to turn on TLS????");
				}
			}

			String id = UUID.randomUUID().toString();
			// We don't know hostname yet so we have to save session-id in
			// connection temp data
			serv.getSessionData().put(serv.SESSION_ID_KEY, id);
			incoming.put(id, serv);
			return "<stream:stream"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " xmlns='jabber:server'"
				+ " xmlns:db='jabber:server:dialback'"
				+ " id='" + id + "'"
				+ ">"
				;
		}
		default:
			log.severe("Warning, program shouldn't reach that point.");
			break;
		} // end of switch (serv.connectionType())
		return null;
	}

	@Override
	public void xmppStreamClosed(XMPPIOService serv) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream closed: " + getConnectionId(serv));
		}
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
// Usually we want the server to do s2s for the external component too:
//		if (params.get(GEN_VIRT_HOSTS) != null) {
//			HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
//		} else {
//			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
//		}
//		ArrayList<String> vhosts =
//      new ArrayList<String>(Arrays.asList(HOSTNAMES_PROP_VAL));
//		for (Map.Entry<String, Object> entry: params.entrySet()) {
//			if (entry.getKey().startsWith(GEN_EXT_COMP)) {
//				String ext_comp = (String)entry.getValue();
//				if (ext_comp != null) {
//					String[] comp_params = ext_comp.split(",");
//					vhosts.add(comp_params[1]);
//				}
//			}
//			if (entry.getKey().startsWith(GEN_COMP_NAME)) {
//				String comp_name_suffix = entry.getKey().substring(GEN_COMP_NAME.length());
//				String c_name = (String)params.get(GEN_COMP_NAME + comp_name_suffix);
//				for (String vhost: HOSTNAMES_PROP_VAL) {
//					vhosts.add(c_name + "." + vhost);
//				}
//			}
//		}
//		HOSTNAMES_PROP_VAL = vhosts.toArray(new String[0]);
//		hostnames = HOSTNAMES_PROP_VAL;
//		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(MAX_PACKET_WAITING_TIME_PROP_KEY,
			MAX_PACKET_WAITING_TIME_PROP_VAL);
		return props;
	}

	@Override
	protected int[] getDefPlainPorts() {
		return new int[] {5269};
	}

	@Override
	public boolean handlesNonLocalDomains() {
		return true;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
//		hostnames = (String[])props.get(HOSTNAMES_PROP_KEY);
//		if (hostnames == null || hostnames.length == 0) {
//			log.warning("Hostnames definition is empty, setting 'localhost'");
//			hostnames = new String[] {"localhost"};
//		} // end of if (hostnames == null || hostnames.length == 0)
//		Arrays.sort(hostnames);

//		addRouting("*");
		maxPacketWaitingTime = (Long)props.get(MAX_PACKET_WAITING_TIME_PROP_KEY);
	}

	@Override
	public void serviceStarted(XMPPIOService serv) {
		super.serviceStarted(serv);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("s2s connection opened: " + serv.getRemoteAddress()
				+ ", type: " + serv.connectionType().toString()
				+ ", id=" + serv.getUniqueId());
		}
		switch (serv.connectionType()) {
		case connect:
			// Send init xmpp stream here
			//			XMPPIOService serv = (XMPPIOService)service;
			String data = "<stream:stream"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " xmlns='jabber:server'"
				+ " xmlns:db='jabber:server:dialback'"
				+ ">";
			if (log.isLoggable(Level.FINEST)) {
				log.finest("cid: " + (String)serv.getSessionData().get("cid")
					+ ", sending: " + data);
			}
			serv.xmppStreamOpen(data);
			break;
		default:
			// Do nothing, more data should come soon...
			break;
		} // end of switch (service.connectionType())
	}

	private int countOpenConnections() {
		int open_s2s_connections = incoming.size();
		for (Map.Entry<String, ServerConnections> entry:
      connectionsByLocalRemote.entrySet()) {
			ServerConnections conn = entry.getValue();
			if (conn.isOutgoingConnected()) {
				++open_s2s_connections;
			}
		}
		return open_s2s_connections;
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		int waiting_packets = 0;
		int open_s2s_connections = incoming.size();
		int connected_servers = 0;
		int server_connections_instances = connectionsByLocalRemote.size();
		for (Map.Entry<String, ServerConnections> entry : connectionsByLocalRemote.
						entrySet()) {
			ServerConnections conn = entry.getValue();
			waiting_packets += conn.getWaitingPackets().size();
			if (conn.isOutgoingConnected()) {
				++open_s2s_connections;
				++connected_servers;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("s2s instance: " + entry.getKey() +
								", waitingQueue: " + conn.getWaitingPackets().size() +
								", outgoingIsNull(): " + conn.outgoingIsNull() +
								", outgoingActive: " + conn.isOutgoingConnected() +
								", OutgoingState: " + conn.getOutgoingState().toString() +
								", db_keys.size(): " + conn.getDBKeysSize());
			}
		}
		list.add(getName(), "Open s2s connections",
						open_s2s_connections, Level.FINE);
		list.add(getName(), "Packets queued",
						waiting_packets, Level.FINE);
		list.add(getName(), "Connected servers",
						connected_servers, Level.FINE);
		list.add(getName(), "Connection instances",
						server_connections_instances, Level.FINER);
	}

	@Override
	public boolean serviceStopped(XMPPIOService serv) {
		boolean result = super.serviceStopped(serv);
		if (result) {
			switch (serv.connectionType()) {
				case connect:
					String local_hostname =
									(String) serv.getSessionData().get("local-hostname");
					String remote_hostname =
									(String) serv.getSessionData().get("remote-hostname");
					if (remote_hostname == null) {
						// There is something wrong...
						// It may happen only when remote host connecting to Tigase
						// closed connection before it send any db:... packet
						// so remote domain is not known.
						// Let's do nothing for now.
						log.info("remote-hostname is NULL, local-hostname: " +
										local_hostname + ", local address: " +
										serv.getLocalAddress() + ", remote address: " +
										serv.getRemoteAddress());
					} else {
						String cid = getConnectionId(local_hostname, remote_hostname);
						ServerConnections serv_conns = getServerConnections(cid);
						if (serv_conns == null) {
							log.warning("There is no ServerConnections for stopped service: " +
											serv.getUniqueId() + ", cid: " + cid);
							if (log.isLoggable(Level.FINEST)) {
								log.finest("Counters: ioservices: " + countIOServices() +
												", s2s connections: " + countOpenConnections());
							}
							return result;
						}
						serv_conns.serviceStopped(serv);
						Queue<Packet> waiting = serv_conns.getWaitingPackets();
						if (waiting.size() > 0) {
							if (serv_conns.waitingTime() > maxPacketWaitingTime) {
								bouncePacketsBack(Authorization.REMOTE_SERVER_TIMEOUT, cid);
							} else {
								createServerConnection(cid, null, serv_conns);
							}
						}
					}
					break;
				case accept:
					String session_id = (String) serv.getSessionData().get(
									serv.SESSION_ID_KEY);
					if (session_id != null) {
						XMPPIOService rem = incoming.remove(session_id);
						if (rem == null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("No service with given SESSION_ID: " + session_id);
							}
						} else {
							if (log.isLoggable(Level.FINER)) {
								log.finer("Connection removed: " + session_id);
							}
						}
					} else {
						if (log.isLoggable(Level.FINE)) {
							log.fine("session_id is null, didn't remove the connection");
						}
					}
					break;
				default:
					log.severe("Warning, program shouldn't reach that point.");
					break;
			} // end of switch (serv.connectionType())

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Counters: ioservices: " + countIOServices() +
								", s2s connections: " + countOpenConnections());
			}
		}
		return result;
	}

	private void generateStreamError(String error_el, XMPPIOService serv) {
		Element error = new Element("stream:error",
			new Element[] {
				new Element(error_el,
					new String[] {"xmlns"},
					new String[] {"urn:ietf:params:xml:ns:xmpp-streams"})
			}, null, null);
		try {
			writeRawData(serv, error.toString());
// 			serv.writeRawData(error.toString());
// 			serv.writeRawData("</stream:stream>");
			serv.stop();
		} catch (Exception e) {
			serv.forceStop();
		}
	}

	public synchronized void processDialback(Packet packet, XMPPIOService serv) {

		if (log.isLoggable(Level.FINEST)) {
			log.finest("DIALBACK - " + packet.getStringData());
		}

		String local_hostname = JIDUtils.getNodeHost(packet.getElemTo());
		// Check whether this is correct local host name...
		if (!isLocalDomainOrComponent(local_hostname)) {
			// Ups, this hostname is not served by this server, return stream
			// error and close the connection....
			generateStreamError("host-unknown", serv);
			return;
		}
		String remote_hostname = JIDUtils.getNodeHost(packet.getElemFrom());
		// And we don't want to accept any connection which is from remote
		// host name the same as one my localnames.......
		if (isLocalDomainOrComponent(remote_hostname)) {
			// Ups, remote hostname is the same as one of local hostname??
			// fake server or what? internal loop, we don't want that....
			// error and close the connection....
			generateStreamError("host-unknown", serv);
			return;
		}
		String cid = getConnectionId(local_hostname, remote_hostname);
		ServerConnections serv_conns = getServerConnections(cid);
		String session_id = (String)serv.getSessionData().get(serv.SESSION_ID_KEY);
		String serv_local_hostname =
      (String)serv.getSessionData().get("local-hostname");
		String serv_remote_hostname =
      (String)serv.getSessionData().get("remote-hostname");
		String serv_cid = serv_remote_hostname == null ? null
      : getConnectionId(serv_local_hostname, serv_remote_hostname);
		if (serv_cid != null && !cid.equals(serv_cid)) {
			log.info("Somebody tries to reuse connection?"
				+ " old_cid: " + serv_cid + ", new_cid: " + cid);
		}

		// <db:result>
		if ((packet.getElemName() == RESULT_EL_NAME) ||
			(packet.getElemName() == DB_RESULT_EL_NAME)) {
			if (packet.getType() == null) {
				// This is incoming connection with dialback key for verification
				if (packet.getElemCData() != null) {
					// db:result with key to validate from accept connection
					String db_key = packet.getElemCData();
					//initServiceMapping(local_hostname, remote_hostname, accept_jid, serv);

					// <db:result> with CDATA containing KEY
					Element elem = new Element(DB_VERIFY_EL_NAME, db_key,
						new String[] {"id", "to", "from", XMLNS_DB_ATT},
						new String[] {session_id, remote_hostname, local_hostname,
													XMLNS_DB_VAL});
					Packet result = new Packet(elem);
					if (serv_conns == null) {
						serv_conns = createNewServerConnections(cid, null);
					}
					//serv_conns.putDBKey(session_id, db_key);
					serv.getSessionData().put("remote-hostname", remote_hostname);
					serv.getSessionData().put("local-hostname", local_hostname);
					if (log.isLoggable(Level.FINEST)) {
						log.finest("cid: " + cid + ", sessionId: " + session_id
							+ ", Counters: ioservices: " + countIOServices()
							+ ", s2s connections: " + countOpenConnections());
					}
					if (!serv_conns.sendControlPacket(result)
						&& serv_conns.needsConnection()) {
						createServerConnection(cid, result, serv_conns);
					}
				} else {
					// Incorrect dialback packet, it happens for some servers....
					// I don't know yet what software they use.
					// Let's just disconnect and signal unrecoverable conection error
					if (log.isLoggable(Level.FINER)) {
						log.finer("Incorrect diablack packet: " + packet.getStringData());
					}
					bouncePacketsBack(Authorization.SERVICE_UNAVAILABLE, cid);
					generateStreamError("bad-format", serv);
				}
			} else {
				// <db:result> with type 'valid' or 'invalid'
				// It means that session has been validated now....
				//XMPPIOService connect_serv = handshakingByHost_Type.get(connect_jid);
				switch (packet.getType()) {
				case valid:
					if (log.isLoggable(Level.FINER)) {
						log.finer("Connection: " + cid +
										" is valid, adding to available services.");
					}
					serv_conns.handleDialbackSuccess();
					break;
				default:
					if (log.isLoggable(Level.FINER)) {
						log.finer("Connection: " + cid + " is invalid!! Stopping...");
					}
					serv_conns.handleDialbackFailure();
					break;
				} // end of switch (packet.getType())
			} // end of if (packet.getType() != null) else
		} // end of if (packet != null && packet.getElemName().equals("db:result"))

		// <db:verify> with type 'valid' or 'invalid'
		if ((packet.getElemName() == VERIFY_EL_NAME)
			|| (packet.getElemName() == DB_VERIFY_EL_NAME)) {
			if (packet.getElemId() != null) {

				String forkey_session_id = packet.getElemId();
				if (packet.getType() == null) {
					// When type is NULL then it means this packet contains
					// data for verification
					if (packet.getElemId() != null && packet.getElemCData() != null) {
						String db_key = packet.getElemCData();
						// This might be the first dialback packet from remote server
// 						serv.getSessionData().put("remote-hostname", remote_hostname);
// 						serv.getSessionData().put("local-hostname", local_hostname);
// 						serv_conns.addIncoming(session_id, serv);
// 						log.finest("cid: " + cid + ", sessionId: " + session_id
// 							+ ", Counters: ioservices: " + countIOServices()
// 							+ ", s2s connections: " + countOpenConnections());
						//initServiceMapping(local_hostname, remote_hostname, accept_jid, serv);

						String local_key = getLocalDBKey(cid, db_key, forkey_session_id,
							session_id);

						if (local_key == null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("db key is not availablefor session ID: " + forkey_session_id
									+ ", key for validation: " + db_key);
							}
						} else {
							if (log.isLoggable(Level.FINE)) {
								log.fine("Local key for cid=" + cid + " is " + local_key);
							}
							sendVerifyResult(local_hostname, remote_hostname, forkey_session_id,
								db_key.equals(local_key), serv_conns, session_id);
						}
					} // end of if (packet.getElemName().equals("db:verify"))
				}	else {
					// Type is not null so this is packet with verification result.
					// If the type is valid it means accept connection has been
					// validated and we can now receive data from this channel.

					Element elem = new Element(DB_RESULT_EL_NAME,
						new String[] {"type", "to", "from", XMLNS_DB_ATT},
						new String[] {packet.getType().toString(),
													remote_hostname, local_hostname,
													XMLNS_DB_VAL});

					sendToIncoming(forkey_session_id, new Packet(elem));
					validateIncoming(forkey_session_id,
						(packet.getType() == StanzaType.valid));

				} // end of if (packet.getType() == null) else
			} else {
				// Incorrect dialback packet, it happens for some servers....
				// I don't know yet what software they use.
				// Let's just disconnect and signal unrecoverable conection error
				if (log.isLoggable(Level.FINER)) {
					log.finer("Incorrect diablack packet: " + packet.getStringData());
				}
				bouncePacketsBack(Authorization.SERVICE_UNAVAILABLE, cid);
				generateStreamError("bad-format", serv);
			}
		} // end of if (packet != null && packet.getType() != null)

	}

	public boolean sendToIncoming(String session_id, Packet packet) {
		XMPPIOService serv = incoming.get(session_id);
		if (serv != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending to incoming connectin: " + session_id
					+ " packet: " + packet.toString());
			}
			return writePacketToSocket(serv, packet);
		} else {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Trying to send packet: " + packet.toString()
					+ " to nonexisten connection with sessionId: " + session_id);
			}
			return false;
		}
	}

	public void validateIncoming(String session_id, boolean valid) {
		XMPPIOService serv = incoming.get(session_id);
		if (serv != null) {
			serv.getSessionData().put("valid", valid);
			if (!valid) {
				serv.stop();
			}
		}
	}

	protected String getLocalDBKey(String cid, String key, String forkey_sessionId,
		String asking_sessionId) {
		ServerConnections serv_conns = getServerConnections(cid);
		return serv_conns == null ? null : serv_conns.getDBKey(forkey_sessionId);
	}

	protected void sendVerifyResult(String from, String to, String forkey_sessionId,
		boolean valid, ServerConnections serv_conns, String asking_sessionId) {
		String type = (valid ? "valid" : "invalid");
		Element result_el = new Element(DB_VERIFY_EL_NAME,
			new String[] {"from", "to", "id", "type", XMLNS_DB_ATT},
			new String[] {from, to, forkey_sessionId, type, XMLNS_DB_VAL});
		Packet result = new Packet(result_el);
		if (!sendToIncoming(asking_sessionId, result)) {
			log.warning("Can not send verification packet back: " + result.toString());
		}
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. Let's assume s2s should send something
	 * at least once every 15 minutes....
	 *
	 * @return a <code>long</code> value
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 15*MINUTE;
	}

	@Override
	protected boolean isHighThroughput() {
		return true;
	}

	@Override
	protected XMPPIOService getXMPPIOServiceInstance() {
		return new XMPPIOService();
	}

	private static Map<String, ConnectionWatchdogTask> waitingTasks =
					new LinkedHashMap<String, ConnectionWatchdogTask>();

	private class ConnectionWatchdogTask extends TimerTask {

		private ServerConnections conns = null;
		private String localhost = null;
		private String remotehost = null;

		private ConnectionWatchdogTask(ServerConnections conns, String localhost,
						String remotehost) {
			this.conns = conns;
			this.localhost = localhost;
			this.remotehost = remotehost;
			String key = localhost+remotehost;
			ConnectionWatchdogTask task = waitingTasks.get(key);
			if (task != null) {
				task.cancel();
			}
			connectionWatchdog.schedule(this, 2 * MINUTE);
			waitingTasks.put(key, this);
		}

		@Override
		public void run() {
			String key = localhost+remotehost;
			waitingTasks.remove(key);
			if (conns.getOutgoingState() ==
							ServerConnections.OutgoingState.CONNECTING) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Connecting timeout expired, still connecting: " +
									conns.getCID());
				}
				Queue<Packet> waiting = conns.getWaitingPackets();
				if (waiting.size() > 0) {
					if (conns.waitingTime() > maxPacketWaitingTime) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Max packets waiting time expired, sending all back: " +
											conns.getCID());
						}
						conns.stopAll();
						bouncePacketsBack(Authorization.REMOTE_SERVER_TIMEOUT, 
										conns.getCID());
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Reconnecting: " + conns.getCID());
						}
						createServerConnection(conns.getCID(), null, conns);
					}
				} else {
					conns.stopAll();
					if (log.isLoggable(Level.FINEST)) {
						log.finest("No packets waiting in queue, giving up: " + conns.getCID());
					}
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Connecting timeout expired: " + conns.getCID() +
									", connection state is: " + conns.getOutgoingState());
				}
			}
		}

	}

}
