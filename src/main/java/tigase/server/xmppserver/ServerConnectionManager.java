/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.xmppserver;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.UserRepository;
import tigase.net.ConnectionType;
import tigase.net.IOService;
import tigase.net.SocketReadThread;
import tigase.net.SocketType;
import tigase.server.ConnectionManager;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.disco.XMPPService;
import tigase.util.Algorithms;
import tigase.util.DNSResolver;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.Authorization;
import tigase.stats.StatRecord;

/**
 * Class ServerConnectionManager
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServerConnectionManager extends ConnectionManager {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppserver.ServerConnectionManager");

	private static final String DIALBACK_XMLNS = "jabber:server:dialback";
	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};
	public static final String MAX_PACKET_WAITING_TIME_PROP_KEY =
		"max-packet-waiting-time";
	public static final long MAX_PACKET_WAITING_TIME_PROP_VAL = 5*MINUTE;

	private String[] hostnames = HOSTNAMES_PROP_VAL;

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
	private Map<String, XMPPIOService> servicesByHost_Type =
		new HashMap<String, XMPPIOService>();

	/**
	 * Services which are in process of establishing s2s connection
	 */
	private Map<String, XMPPIOService> handshakingByHost_Type =
		new HashMap<String, XMPPIOService>();

	/**
	 * Connections IDs (cids) of services which are in process of connecting
	 */
	private Set<String> connectingByHost_Type = new HashSet<String>();

	/**
	 * Normal packets between users on different servers
	 */
	private Map<String, ServerPacketQueue> waitingPackets =
		new ConcurrentSkipListMap<String, ServerPacketQueue>();

	/**
	 * Controll packets for s2s connection establishing
	 */
	private Map<String, ServerPacketQueue> waitingControlPackets =
		new ConcurrentSkipListMap<String, ServerPacketQueue>();

	/**
	 * Data shared between sessions. Some servers (like google for example)
	 * use different IP address for outgoing and ingoing data and as sessions
	 * are identified by IP address we have to create 2 separate sessions
	 * objects for such server. These sessions have to share session ID and
	 * dialback key.
	 */
	private Map<String, Object> sharedSessionData =
		new ConcurrentSkipListMap<String, Object>();

	public void processPacket(Packet packet) {
// 		log.finer("Processing packet: " + packet.getElemName()
// 			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		if (packet.isCommand()) {
			processCommand(packet);
		} else {
			String cid = getConnectionId(packet);
			log.finest("Connection ID is: " + cid);
			synchronized(servicesByHost_Type) {
				XMPPIOService serv = servicesByHost_Type.get(cid);
				if (serv == null || !writePacketToSocket(serv, packet)) {
					addWaitingPacket(cid, packet, waitingPackets);
				} // end of if (serv != null) else
			}
		} // end of else
	}

	private void addWaitingPacket(String cid, Packet packet,
		Map<String, ServerPacketQueue> waitingPacketsMap) {

		synchronized (connectingByHost_Type) {
			boolean connecting = connectingByHost_Type.contains(cid);
			if (!connecting) {
				String localhost = JID.getNodeNick(cid);
				String remotehost = JID.getNodeHost(cid);
				boolean reconnect = (packet == null);
				if (connecting =
					openNewServerConnection(localhost, remotehost, reconnect)) {
					connectingByHost_Type.add(cid);
				} else {
					// Can't establish connection...., unknown host??
					waitingPacketsMap.remove(cid);
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
					if (!packet.getElement().getXMLNS().equals(DIALBACK_XMLNS)
						&& (packet.getType() != StanzaType.error
							|| packet.getErrorCondition() == null
							|| !packet.getErrorCondition().equals(
								Authorization.REMOTE_SERVER_NOT_FOUND.getCondition()))) {
						addOutPacket(
							Authorization.REMOTE_SERVER_NOT_FOUND.getResponseMessage(packet,
								"S2S - destination host not found", true));
					}
				}
			} // end of if (serv == null)
			if (connecting) {
				// The packet may be null if first try to connect to remote
				// server failed and now Tigase is retrying to connect
				if (packet != null) {
					ServerPacketQueue queue = waitingPacketsMap.get(cid);
					if (queue == null) {
						queue = new ServerPacketQueue();
						waitingPacketsMap.put(cid, queue);
					} // end of if (queue == null)
					queue.offer(packet);
				}
			} // end of if (connecting) else
		}
	}

// 	private void dumpCurrentStack(StackTraceElement[] stack) {
// 		StringBuilder sb = new StringBuilder();
// 		for (StackTraceElement st_el: stack) {
// 			sb.append("\n" + st_el.toString());
// 		}
// 		log.finest(sb.toString());
// 	}

	private boolean openNewServerConnection(String localhost,
		String remotehost, boolean reconnect) {

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
			String cid =
				getConnectionId(localhost, remotehost, ConnectionType.connect);
			port_props.put("cid", cid);
			log.finest("STARTING new connection: " + cid);
			if (reconnect) {
				reconnectService(port_props, 15*SECOND);
			} else {
				startService(port_props);
			}
			return true;
		} catch (UnknownHostException e) {
			log.warning("UnknownHostException for host: " + remotehost);
			return false;
		} // end of try-catch

	}

	private String getConnectionId(String localhost, String remotehost,
		ConnectionType connection) {
		return JID.getJID(localhost, remotehost, connection.toString());
	}

	private String getConnectionId(Packet packet) {
		return JID.getJID(JID.getNodeHost(packet.getFrom()),
			JID.getNodeHost(packet.getTo()),
			ConnectionType.connect.toString());
	}

	private String getConnectionId(XMPPIOService service) {
		String local_hostname =
			(String)service.getSessionData().get("local-hostname");
		String remote_hostname =
			(String)service.getSessionData().get("remote-hostname");
		String cid = getConnectionId(local_hostname,
			(remote_hostname != null ? remote_hostname : "NULL"),
			service.connectionType());
		return cid;
	}

// 	private String getHandshakingId(String localhost, String remotehost) {
// 		return JID.getJID(localhost, remotehost, null);
// 	}

// 	private String getHandshakingId(Packet packet) {
// 		return JID.getJID(packet.getFrom(), packet.getTo(), null);
// 	}

	public Queue<Packet> processSocketData(XMPPIOService serv) {
		Queue<Packet> packets = serv.getReceivedPackets();
		Packet p = null;
		while ((p = packets.poll()) != null) {
// 			log.finer("Processing packet: " + p.getElemName()
// 				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());

			if (p.getElement().getXMLNS() != null &&
				p.getElement().getXMLNS().equals(DIALBACK_XMLNS)) {
				Queue<Packet> results = new LinkedList<Packet>();
				processDialback(p, serv, results);
				for (Packet res: results) {
					String cid = res.getTo();
					log.finest("Sending dialback result: " + res.getStringData()
						+ " to " + cid);
					XMPPIOService sender = handshakingByHost_Type.get(cid);
					if (sender == null) {
						sender = servicesByHost_Type.get(cid);
					}
					log.finest("cid: " + cid
						+ ", writing packet to socket: " + res.getStringData());
					if (sender == null || !writePacketToSocket(sender, res)) {
						// I am assuming here that it can't happen that the packet is
						// to accept channel and it doesn't exist
						addWaitingPacket(cid, res, waitingControlPackets);
					} // end of else
				} // end of for (Packet p: results)
			} else {
				if (p.getElemName().equals("error")) {
					processStreamError(p, serv);
					return null;
				} else {
					if (checkPacket(p, serv)) {
						log.finest("Adding packet out: " + p.getStringData());
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
		Queue<Packet> waiting =	waitingPackets.remove(cid);
		if (waiting != null) {
			Packet p = null;
			while ((p = waiting.poll()) != null) {
				log.finest("Sending packet back: " + p.getStringData());
				addOutPacket(author.getResponseMessage(p, "S2S - not delivered", true));
			} // end of while (p = waitingPackets.remove(ipAddress) != null)
		} // end of if (waiting != null)
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
		if (!JID.getNodeHost(packet_from).equals(remote_hostname)) {
			generateStreamError("invalid-from", serv);
			return false;
		}
		String local_hostname =	(String)serv.getSessionData().get("local-hostname");
		if (!JID.getNodeHost(packet_to).equals(local_hostname)) {
			generateStreamError("host-unknown", serv);
			return false;
		}
		return true;
	}

	private void processCommand(final Packet packet) {
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
	}

	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {

		log.finer("Stream opened: " + attribs.toString());

		switch (serv.connectionType()) {
		case connect: {
			// It must be always set for connect connection type
			String remote_hostname =
				(String)serv.getSessionData().get("remote-hostname");
			String local_hostname =
				(String)serv.getSessionData().get("local-hostname");
			String cid = getConnectionId(local_hostname, remote_hostname,
				ConnectionType.connect);
			log.finest("Stream opened for: " + cid);
			handshakingByHost_Type.put(cid, serv);
			String remote_id = attribs.get("id");
			sharedSessionData.put(cid+"-session-id", remote_id);
			String uuid = UUID.randomUUID().toString();
			String key = null;
			try {
				key = Algorithms.hexDigest(remote_id, uuid, "SHA");
			} catch (NoSuchAlgorithmException e) {
				key = uuid;
			} // end of try-catch
			sharedSessionData.put(cid+"-dialback-key", key);
			Element elem = new Element("db:result", key);
			elem.addAttribute("to", remote_hostname);
			elem.addAttribute("from", local_hostname);
			elem.addAttribute("xmlns:db", DIALBACK_XMLNS);

			StringBuilder sb = new StringBuilder();
			// Attach also all controll packets which are wating to send
			Packet p = null;
			Queue<Packet> waiting =	waitingControlPackets.get(cid);
			if (waiting != null) {
				while ((p = waiting.poll()) != null) {
					log.finest("Sending packet: " + p.getStringData());
					sb.append(p.getStringData());
				} // end of while (p = waitingPackets.remove(ipAddress) != null)
			} // end of if (waiting != null)
			sb.append(elem.toString());
			log.finest("cid: " + (String)serv.getSessionData().get("cid")
				+ ", sending: " + sb.toString());
			return sb.toString();
		}
		case accept: {
			String remote_hostname =
				(String)serv.getSessionData().get("remote-hostname");
			String local_hostname =
				(String)serv.getSessionData().get("local-hostname");
			String cid = getConnectionId(
				(local_hostname != null ? local_hostname : "NULL"),
				(remote_hostname != null ? remote_hostname : "NULL"),
				ConnectionType.accept);
			log.finest("Stream opened for: " + cid);
			if (remote_hostname != null) {
				log.fine("Opening stream for already established connection...., trying to turn on TLS????");
			}

			String id = UUID.randomUUID().toString();
			// We don't know hostname yet so we have to save session-id in
			// connection temp data
			serv.getSessionData().put(serv.SESSION_ID_KEY, id);
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

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed: " + getConnectionId(serv));
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		// Usually we want the server to do s2s for the external component too:
		if (params.get("--virt-hosts") != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get("--virt-hosts")).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		}
		String ext_comp = (String)params.get("--ext-comp");
		if (ext_comp != null) {
			String[] comp_params = ext_comp.split(",");
			HOSTNAMES_PROP_VAL = Arrays.copyOf(HOSTNAMES_PROP_VAL,
				HOSTNAMES_PROP_VAL.length + 1);
			HOSTNAMES_PROP_VAL[HOSTNAMES_PROP_VAL.length - 1] = comp_params[1];
		}
		hostnames = HOSTNAMES_PROP_VAL;
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(MAX_PACKET_WAITING_TIME_PROP_KEY,
			MAX_PACKET_WAITING_TIME_PROP_VAL);
		return props;
	}

	protected int[] getDefPlainPorts() {
		return new int[] {5269};
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		hostnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		if (hostnames == null || hostnames.length == 0) {
			log.warning("Hostnames definition is empty, setting 'localhost'");
			hostnames = new String[] {"localhost"};
		} // end of if (hostnames == null || hostnames.length == 0)
		Arrays.sort(hostnames);

		addRouting("*");
		maxPacketWaitingTime = (Long)props.get(MAX_PACKET_WAITING_TIME_PROP_KEY);
	}

	public void serviceStarted(final IOService service) {
		super.serviceStarted(service);
		log.finest("s2s connection opened: " + service.getRemoteAddress()
			+ ", type: " + service.connectionType().toString()
			+ ", id=" + service.getUniqueId());
		switch (service.connectionType()) {
		case connect:
			// Send init xmpp stream here
			XMPPIOService serv = (XMPPIOService)service;
			String data = "<stream:stream"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " xmlns='jabber:server'"
				+ " xmlns:db='jabber:server:dialback'"
				+ ">";
			log.finest("cid: " + (String)serv.getSessionData().get("cid")
				+ ", sending: " + data);
			serv.xmppStreamOpen(data);
			break;
		default:
			// Do nothing, more data should come soon...
			break;
		} // end of switch (service.connectionType())
	}

	public List<StatRecord> getStatistics() {
		List<StatRecord> stats = super.getStatistics();
		stats.add(new StatRecord(getName(), "Open s2s connections", "int",
				servicesByHost_Type.size(), Level.INFO));
		int waiting = 0;
		for (Queue<Packet> q: waitingPackets.values()) {
			waiting += q.size();
		}
		stats.add(new StatRecord(getName(), "Packets queued", "int",
				waiting, Level.INFO));
		stats.add(new StatRecord(getName(), "Connecting s2s connections", "int",
				connectingByHost_Type.size(), Level.FINE));
		stats.add(new StatRecord(getName(), "Handshaking s2s connections", "int",
				handshakingByHost_Type.size(), Level.FINER));
// 		StringBuilder sb = new StringBuilder("Handshaking: ");
// 		for (IOService serv: handshakingByHost_Type.values()) {
// 			sb.append("\nService ID: " + getUniqueId(serv)
// 				+ ", local-hostname: " + serv.getSessionData().get("local-hostname")
// 				+ ", remote-hostname: " + serv.getSessionData().get("remote-hostname")
// 				+ ", is-connected: " + serv.isConnected()
// 				+ ", connection-type: " + serv.connectionType());
// 		}
// 		log.finest(sb.toString());
		LinkedList<String> waiting_qs = new LinkedList<String>();
		for (Map.Entry<String, ServerPacketQueue> entry: waitingPackets.entrySet()) {
			if (entry.getValue().size() > 0) {
				waiting_qs.add(entry.getKey() + ":  " + entry.getValue().size());
			}
		}
		if (waiting_qs.size() > 0) {
			stats.add(new StatRecord(getName(), "Packets queued for each connection",
					waiting_qs,	Level.FINER));
		}
		ArrayList<String> all_s2s = new ArrayList<String>(servicesByHost_Type.keySet());
		Collections.sort(all_s2s);
		stats.add(new StatRecord(getName(), "s2s connections", all_s2s,	Level.FINEST));
		return stats;
	}

	public void serviceStopped(final IOService service) {
		super.serviceStopped(service);
		String local_hostname =
			(String)service.getSessionData().get("local-hostname");
		String remote_hostname =
			(String)service.getSessionData().get("remote-hostname");
		if (remote_hostname == null) {
			// There is something wrong...
			// It may happen only when remote host connecting to Tigase
			// closed connection before it send any db:... packet
			// so remote domain is not known.
			// Let's do nothing for now.
			log.info("remote-hostname is NULL, local-hostname: " + local_hostname
				+ ", local address: " + service.getLocalAddress()
				+ ", remote address: " + service.getRemoteAddress());
			return;
		}
		String cid = getConnectionId(local_hostname, remote_hostname,
			service.connectionType());
		boolean stopped = false;
		IOService serv = servicesByHost_Type.get(cid);
		// This checking is necessary due to specific s2s behaviour which
		// I don't fully understand yet, possible bug in my s2s implementation
		if (serv == service) {
			stopped = true;
			servicesByHost_Type.remove(cid);
		} else {
			log.info("Stopped non-active service for CID: " + cid);
		}
		serv = handshakingByHost_Type.get(cid);
		// This checking is necessary due to specific s2s behaviour which
		// I don't fully understand yet, possible bug in my s2s implementation
		if (!stopped && serv == service) {
			stopped = true;
			handshakingByHost_Type.remove(cid);
			connectingByHost_Type.remove(cid);
			waitingControlPackets.remove(cid);
		} else {
			if (!stopped) {
				log.info("Stopped non-handshaking service for CID: " + cid);
			}
		}
		if (serv == null && connectingByHost_Type.contains(cid)) {
			connectingByHost_Type.remove(cid);
			waitingControlPackets.remove(cid);
			stopped = true;
		}
		if (!stopped) {
			return;
		}
		log.fine("s2s stopped: " + cid);
		// Some servers close just 1 of dialback connections and even though
		// other connection is still open they don't accept any data on that
		// connections. So the solution is: if one of pair connection is closed
		// close the other connection as well.
		// Find other connection:

		// Hm it doesn't work very well, let's comment it out for now.

// 		String other_id = null;
// 		switch (service.connectionType()) {
// 		case accept:
// 			other_id = getConnectionId(local_hostname, remote_hostname,
// 				ConnectionType.connect);
// 			break;
// 		case connect:
// 		default:
// 			other_id = getConnectionId(local_hostname, remote_hostname,
// 				ConnectionType.accept);
// 			break;
// 		} // end of switch (service.connectionType())
// 		XMPPIOService other_service = servicesByHost_Type.get(other_id);
// 		if (other_service == null) {
// 			other_service = handshakingByHost_Type.get(other_id);
// 		} // end of if (other_service == null)
// 		if (other_service != null) {
// 			log.fine("Stopping other service: " + other_id);
// // 			servicesByHost_Type.remove(other_id);
// // 			handshakingByHost_Type.remove(other_id);
// // 			connectingByHost_Type.remove(other_id);
// 			other_service.stop();
// 		} // end of if (other_service != null)
		ServerPacketQueue waiting =	waitingPackets.get(cid);
		if (waiting != null && waiting.size() > 0) {
			if (System.currentTimeMillis() - waiting.creationTime
				> maxPacketWaitingTime) {
				bouncePacketsBack(Authorization.REMOTE_SERVER_TIMEOUT, cid);
			} else {
				addWaitingPacket(cid, null, waitingPackets);
			}
		}
	}

	public void handleDialbackSuccess(final String connect_jid) {
		log.finest("handleDialbackSuccess: connect_jid="+connect_jid);
		Packet p = null;
		XMPPIOService serv = servicesByHost_Type.get(connect_jid);
		ServerPacketQueue waiting = waitingPackets.remove(connect_jid);
		if (waiting != null) {
			while ((p = waiting.poll()) != null) {
				log.finest("Sending packet: " + p.getStringData());
				writePacketToSocket(serv, p);
			} // end of while (p = waitingPackets.remove(ipAddress) != null)
		} // end of if (waiting != null)
	}

	private void generateStreamError(String error_el, XMPPIOService serv) {
		Element error = new Element("stream:error",
			new Element[] {
				new Element(error_el,
					new String[] {"xmlns"},
					new String[] {"urn:ietf:params:xml:ns:xmpp-streams"})
			}, null, null);
		try {
			serv.writeRawData(error.toString());
			serv.writeRawData("</stream:stream>");
			serv.stop();
		} catch (Exception e) {
			serv.stop();
		}
	}

	private void initServiceMaping(String local_hostname, String remote_hostname,
		String cid, XMPPIOService serv) {
		// Assuming this is the first packet from that connection which
		// tells us for what domain this connection is we have to map
		// somehow this IP address to hostname
		XMPPIOService old_serv = (handshakingByHost_Type.get(cid) != null ?
			handshakingByHost_Type.get(cid) : servicesByHost_Type.get(cid));
		if (old_serv != serv) {
			serv.getSessionData().put("local-hostname", local_hostname);
			serv.getSessionData().put("remote-hostname", remote_hostname);
			handshakingByHost_Type.put(cid, serv);
			if (old_serv != null) {
				log.finest("Stopping old connection for: " + cid);
				old_serv.stop();
			}
		}
	}

	public synchronized void processDialback(Packet packet, XMPPIOService serv,
		Queue<Packet> results) {

		log.finest("DIALBACK - " + packet.getStringData());

		String local_hostname = JID.getNodeHost(packet.getElemTo());
		// Check whether this is correct local host name...
		if (Arrays.binarySearch(hostnames, local_hostname) < 0) {
			// Ups, this hostname is not served by this server, return stream
			// error and close the connection....
			generateStreamError("host-unknown", serv);
			return;
		}
		String remote_hostname = JID.getNodeHost(packet.getElemFrom());
		String connect_jid = getConnectionId(local_hostname, remote_hostname,
			ConnectionType.connect);
		String accept_jid = getConnectionId(local_hostname, remote_hostname,
			ConnectionType.accept);

		// <db:result>
		if (packet.getElemName().equals("result")) {
			if (packet.getType() == null) {
				if (packet.getElemCData() != null) {
					// db:result with key to validate from accept connection
					sharedSessionData.put(accept_jid + "-session-id",
						serv.getSessionData().get(serv.SESSION_ID_KEY));
					sharedSessionData.put(accept_jid + "-dialback-key",
						packet.getElemCData());
					initServiceMaping(local_hostname, remote_hostname, accept_jid, serv);

					// <db:result> with CDATA containing KEY
					Element elem = new Element("db:verify", packet.getElemCData(),
						new String[] {"id", "to", "from", "xmlns:db"},
						new String[] {(String)serv.getSessionData().get(serv.SESSION_ID_KEY),
													packet.getElemFrom(),
													packet.getElemTo(),
													DIALBACK_XMLNS});
					Packet result = new Packet(elem);
					result.setTo(connect_jid);
					results.offer(result);
				} else {
					// Incorrect dialback packet, it happens for some servers....
					// I don't know yet what software they use.
					// Let's just disconnect and signal unrecoverable conection error
					log.finer("Incorrect diablack packet: " + packet.getStringData());
					bouncePacketsBack(Authorization.SERVICE_UNAVAILABLE, connect_jid);
					generateStreamError("bad-format", serv);
				}
			} else {
				// <db:result> with type 'valid' or 'invalid'
				// It means that session has been validated now....
				XMPPIOService connect_serv = handshakingByHost_Type.get(connect_jid);
				switch (packet.getType()) {
				case valid:
					log.finer("Connection: " + connect_jid
						+ " is valid, adding to available services.");
					servicesByHost_Type.put(connect_jid, connect_serv);
					handshakingByHost_Type.remove(connect_jid);
					connectingByHost_Type.remove(connect_jid);
					waitingControlPackets.remove(connect_jid);
					handleDialbackSuccess(connect_jid);
					break;
				default:
					log.finer("Connection: " + connect_jid + " is invalid!! Stopping...");
					connect_serv.stop();
					break;
				} // end of switch (packet.getType())
			} // end of if (packet.getType() != null) else
		} // end of if (packet != null && packet.getElemName().equals("db:result"))

		// <db:verify> with type 'valid' or 'invalid'
		if (packet.getElemName().equals("verify")) {
			if (packet.getType() == null) {
				// When type is NULL then it means this packet contains
				// data for verification
				if (packet.getElemId() != null && packet.getElemCData() != null) {
					// This might be the first dialback packet from remote server
					initServiceMaping(local_hostname, remote_hostname, accept_jid, serv);

					// Yes data for verification are available in packet
					final String id = packet.getElemId();
					final String key = packet.getElemCData();

					final String local_key =
						(String)sharedSessionData.get(connect_jid+"-dialback-key");

					log.fine("Local key for cid=" + connect_jid + " is " + local_key);

					Element result_el = new Element("db:verify",
						new String[] {"to", "from", "id", "xmlns:db"},
						new String[] {packet.getElemFrom(), packet.getElemTo(),
													packet.getElemId(), DIALBACK_XMLNS});
					Packet result = new Packet(result_el);

					if (key.equals(local_key)) {
						log.finer("Verification for " + accept_jid
							+ " succeeded, sending valid.");
						result_el.setAttribute("type", "valid");
						//result = packet.swapElemFromTo(StanzaType.valid);
					} else {
						log.finer("Verification for " + accept_jid
							+ " failed, sending invalid.");
						result_el.setAttribute("type", "invalid");
						//result = packet.swapElemFromTo(StanzaType.invalid);
					} // end of if (key.equals(local_key)) else
					result.setTo(accept_jid);
					log.finest("Adding result packet: " + result.getStringData()
						+ " to " + result.getTo());
					results.offer(result);
				} // end of if (packet.getElemName().equals("db:verify"))
			}	else {
				// Type is not null so this is packet with verification result.
				// If the type is valid it means accept connection has been
				// validated and we can now receive data from this channel.

				Element elem = new Element("db:result",
					new String[] {"type", "to", "from", "xmlns:db"},
					new String[] {packet.getType().toString(),
												packet.getElemFrom(), packet.getElemTo(),
												DIALBACK_XMLNS});

				XMPPIOService accept_serv = handshakingByHost_Type.remove(accept_jid);
				if (accept_serv == null) {
					accept_serv = servicesByHost_Type.get(accept_jid);
				} else {
					connectingByHost_Type.remove(accept_jid);
					waitingControlPackets.remove(accept_jid);
				}

				if (accept_serv == null) {
					// UPS, no such connection do send a packet, I give up
					log.fine("Connection closed before handshaking completed: "
						+ accept_jid
						+ ", can't send packet: " + elem.toString());
					return;
				}
				try {
					accept_serv.writeRawData(elem.toString());
					switch (packet.getType()) {
					case valid:
						log.finer("Received " + packet.getType().toString()
							+ " validation result, adding connection to active services.");
						servicesByHost_Type.put(accept_jid, accept_serv);
						break;
					default:
						// Ups, verification failed, let's stop the service now.
						log.finer("Received " + packet.getType().toString()
							+ " validation result, stopping service, closing connection.");
						accept_serv.writeRawData("</stream:stream>");
						accept_serv.stop();
						break;
					}
				} catch (Exception e) {
					accept_serv.stop();
				}

			} // end of if (packet.getType() == null) else
		} // end of if (packet != null && packet.getType() != null)

	}

	private class ServerPacketQueue extends ConcurrentLinkedQueue<Packet> {
		private static final long serialVersionUID = 1L;

		/**
		 * Keeps the creation time. After some time the queue and all
		 * packets waiting to send should become outdated and they
		 * should be returned to sender and no more attempts to connect
		 * to the remote server should be performed.
		 */
		private long creationTime = 0;

		private ServerPacketQueue() {
			super();
			creationTime = System.currentTimeMillis();
		}
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. Let's assume s2s should send something
	 * at least once every 15 minutes....
	 *
	 * @return a <code>long</code> value
	 */
	protected long getMaxInactiveTime() {
		return 15*MINUTE;
	}

}
