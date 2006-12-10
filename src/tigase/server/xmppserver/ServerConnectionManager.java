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
import java.util.LinkedList;
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
import tigase.server.XMPPService;
import tigase.util.Algorithms;
import tigase.util.DNSResolver;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;

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

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	private String[] hostnames = HOSTNAMES_PROP_VAL;

	/**
	 * Services connected and autorized/autenticated
	 */
	private Map<String, XMPPIOService> servicesByHost_Type =
		new HashMap<String, XMPPIOService>();

	/**
	 * Services connected and autorized/autenticated
	 */
	private Map<String, XMPPIOService> handshakingByHost_Type =
		new HashMap<String, XMPPIOService>();

	/**
	 * Services which are in process of connecting
	 */
	private Set<String> connectingByHost_Type = new HashSet<String>();

	/**
	 * Normal packets between users on different servers
	 */
	private Map<String, Queue<Packet>> waitingPackets =
		new ConcurrentSkipListMap<String, Queue<Packet>>();

	/**
	 * Controll packets for s2s connection establishing
	 */
	private Map<String, Queue<Packet>> waitingControlPackets =
		new ConcurrentSkipListMap<String, Queue<Packet>>();

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
				if (serv != null) {
					writePacketToSocket(serv, packet);
				} else {
					addWaitingPacket(cid, packet, waitingPackets);
				} // end of if (serv != null) else
			}
		} // end of else
	}

	private void addWaitingPacket(String cid, Packet packet,
		Map<String, Queue<Packet>> waitingPacketsMap) {

		synchronized (connectingByHost_Type) {
			boolean connecting = connectingByHost_Type.contains(cid);
			if (!connecting) {
				String localhost = JID.getNodeHost(packet.getFrom());
				String remotehost = JID.getNodeHost(packet.getTo());
				if (connecting = openNewServerConnection(localhost, remotehost)) {
					connectingByHost_Type.add(cid);
				}
			} // end of if (serv == null)
			if (connecting) {
				Queue<Packet> queue = waitingPacketsMap.get(cid);
				if (queue == null) {
					queue = new ConcurrentLinkedQueue<Packet>();
					waitingPacketsMap.put(cid, queue);
				} // end of if (queue == null)
				queue.offer(packet);
			} // end of if (connecting)
			else {
				log.warning("Discarding packet: " + packet.getStringData());
			} // end of if (connecting) else
		}
	}

	private boolean openNewServerConnection(String localhost,
		String remotehost) {

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
			startService(port_props);
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
				p.getElement().getXMLNS().equals("jabber:server:dialback")) {
				Queue<Packet> results = new LinkedList<Packet>();
				processDialback(p, serv, results);
				for (Packet res: results) {
					String cid = res.getTo();
					XMPPIOService sender = handshakingByHost_Type.get(cid);
					if (sender != null) {
						log.finest("cid: " + cid
							+ ", writing packet to socket: " + res.getStringData());
						writePacketToSocket(sender, res);
					} else {
						// I am assuming here that it can't happen that the packet is
						// to accept channel and it doesn't exist
						addWaitingPacket(cid, res, waitingControlPackets);
					} // end of else
				} // end of for (Packet p: results)
			} else {
				addOutPacket(p);
			} // end of else
		} // end of while ()
 		return null;
	}

	private void processCommand(final Packet packet) {
		XMPPIOService serv = getXMPPIOService(packet);
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

		switch (serv.connectionType()) {
		case connect:
			// It must be always set for connect connection type
			String remote_hostname =
				(String)serv.getSessionData().get("remote-hostname");
			String local_hostname =
				(String)serv.getSessionData().get("local-hostname");
			String cid = getConnectionId(local_hostname, remote_hostname,
				ConnectionType.connect);
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

			StringBuilder sb = new StringBuilder();
			sb.append(elem.toString());
			// Attach also all controll packets which are wating to send
			Packet p = null;
			Queue<Packet> waiting =	waitingControlPackets.get(cid);
			if (waiting != null) {
				while ((p = waiting.poll()) != null) {
					log.finest("Sending packet: " + p.getStringData());
					sb.append(p.getStringData());
				} // end of while (p = waitingPackets.remove(ipAddress) != null)
			} // end of if (waiting != null)
			log.finest("cid: " + (String)serv.getSessionData().get("cid")
				+ ", sending: " + sb.toString());
			return sb.toString();
		case accept:
			log.finer("Stream opened: " + attribs.toString());
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
		default:
			log.severe("Warning, program shouldn't reach that point.");
			break;
		} // end of switch (serv.connectionType())
		return null;
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
	}

// 	protected String getUniqueId(IOService serv) {
// 		return JID.getJID(null, serv.getRemoteAddress(),
// 			serv.connectionType().toString());
// 	}

// 	protected String getServiceId(Packet packet) {
// 		return JID.getNodeHost(packet.getTo());
// 	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		hostnames = HOSTNAMES_PROP_VAL;
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
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

		addRouting("*");
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

	public void serviceStopped(final IOService service) {
		super.serviceStopped(service);
		String local_hostname =
			(String)service.getSessionData().get("local-hostname");
		String remote_hostname =
			(String)service.getSessionData().get("remote-hostname");
		String cid = getConnectionId(local_hostname, remote_hostname,
			service.connectionType());
		servicesByHost_Type.remove(cid);
		handshakingByHost_Type.remove(cid);
		connectingByHost_Type.remove(cid);
		log.fine("s2s stopped: " + cid);
		// Some servers close just 1 of dialback connections and even though
		// other connection is still open they don't accept any data on that
		// connections. So the solution is: if one of pair connection is closed
		// close the other connection as well.
		// Find other connection:
		String other_id = null;
		switch (service.connectionType()) {
		case accept:
			other_id = getConnectionId(local_hostname, remote_hostname,
				ConnectionType.connect);
			break;
		case connect:
		default:
			other_id = getConnectionId(local_hostname, remote_hostname,
				ConnectionType.accept);
			break;
		} // end of switch (service.connectionType())
		XMPPIOService other_service = servicesByHost_Type.get(other_id);
		if (other_service == null) {
			other_service = handshakingByHost_Type.get(other_id);
		} // end of if (other_service == null)
		if (other_service != null) {
			log.fine("Stopping other service: " + other_id);
// 			servicesByHost_Type.remove(other_id);
// 			handshakingByHost_Type.remove(other_id);
// 			connectingByHost_Type.remove(other_id);
			try {
				other_service.stop();
			} catch (IOException e) {	} // end of try-catch
		} // end of if (other_service != null)
	}

	public void handleDialbackSuccess(final String connect_jid) {
		log.finest("handleDialbackSuccess: connect_jid="+connect_jid);
		Packet p = null;
		XMPPIOService serv = servicesByHost_Type.get(connect_jid);
		Queue<Packet> waiting =	waitingPackets.get(connect_jid);
		if (waiting != null) {
			while ((p = waiting.poll()) != null) {
				log.finest("Sending packet: " + p.getStringData());
				writePacketToSocket(serv, p);
			} // end of while (p = waitingPackets.remove(ipAddress) != null)
		} // end of if (waiting != null)
	}

	public void processDialback(Packet packet, XMPPIOService serv,
		Queue<Packet> results) {

		String local_hostname = JID.getNodeHost(packet.getElemTo());
		String remote_hostname = JID.getNodeHost(packet.getElemFrom());
		String connect_jid = getConnectionId(local_hostname, remote_hostname,
			ConnectionType.connect);
		String accept_jid = getConnectionId(local_hostname, remote_hostname,
			ConnectionType.accept);

		// <db:result>
		if (packet.getElemName().equals("db:result")) {
			if (packet.getType() == null) {
				// db:result with key to validate from accept connection
				// Assuming this is the first packet from that connection which
				// tells us for what domain this connection is we have to map
				// somehow this IP address to hostname
				sharedSessionData.put(accept_jid + "-session-id",
					serv.getSessionData().get(serv.SESSION_ID_KEY));
				sharedSessionData.put(accept_jid + "-dialback-key",
					packet.getElemCData());
				serv.getSessionData().put("local-hostname", local_hostname);
				serv.getSessionData().put("remote-hostname", remote_hostname);
				handshakingByHost_Type.put(accept_jid, serv);

				// <db:result> with CDATA containing KEY
				Element elem = new Element("db:verify", packet.getElemCData(),
					new String[] {"id", "to", "from"},
					new String[] {(String)serv.getSessionData().get(serv.SESSION_ID_KEY),
												packet.getElemFrom(),
												packet.getElemTo()});
				Packet result = new Packet(elem);
				result.setTo(connect_jid);
				results.offer(result);
			} else {
				// <db:result> with type 'valid' or 'invalid'
				// It means that session has been validated now....
				switch (packet.getType()) {
				case valid:
					servicesByHost_Type.put(connect_jid,
						handshakingByHost_Type.remove(connect_jid));
					connectingByHost_Type.remove(connect_jid);
					handleDialbackSuccess(connect_jid);
					servicesByHost_Type.put(accept_jid,
						handshakingByHost_Type.remove(accept_jid));
					connectingByHost_Type.remove(accept_jid);
					break;
				case invalid:
				default:
					break;
				} // end of switch (packet.getType())
			} // end of if (packet.getType() != null) else
		} // end of if (packet != null && packet.getElemName().equals("db:result"))

		// <db:verify> with type 'valid' or 'invalid'
		if (packet.getElemName().equals("db:verify")) {
			if (packet.getType() == null) {
				if (packet.getElemId() != null && packet.getElemCData() != null) {

					log.fine("Verifying dialback - " + packet.getStringData());

					final String id = packet.getElemId();
					final String key = packet.getElemCData();

					final String local_key =
						(String)sharedSessionData.get(connect_jid+"-dialback-key");

					log.fine("Local key for cid=" + connect_jid + " is " + local_key);

					Packet result = null;

					if (key.equals(local_key)) {
						result = packet.swapElemFromTo(StanzaType.valid);
					} else {
						result = packet.swapElemFromTo(StanzaType.invalid);
					} // end of if (key.equals(local_key)) else
					result.getElement().setCData(null);
					result.setTo(accept_jid);
					results.offer(result);
				} // end of if (packet.getElemName().equals("db:verify"))
			}	else {
				Element elem = new Element("db:result",
					new String[] {"type", "to", "from"},
					new String[] {packet.getType().toString(),
												packet.getElemFrom(), packet.getElemTo()});
				Packet result = new Packet(elem);
				result.setTo(accept_jid);
				results.offer(result);
			} // end of if (packet.getType() == null) else
		} // end of if (packet != null && packet.getType() != null)

	}

}
