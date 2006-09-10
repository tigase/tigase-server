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


//import tigase.net.IOService;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.AppConfigurationEntry;
import tigase.auth.CommitHandler;
import tigase.auth.TigaseConfiguration;
import tigase.db.UserRepository;
import tigase.net.ConnectionType;
import tigase.net.IOService;
import tigase.net.SocketReadThread;
import tigase.net.SocketType;
import tigase.server.ConnectionManager;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.ProcessorFactory;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

/**
 * Class ServerConnectionManager
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServerConnectionManager extends ConnectionManager
	implements CommitHandler {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppserver.ServerConnectionManager");

	public static final String COMPONENTS_PROP_KEY = "components";
	public static final String[] COMPONENTS_PROP_VAL =
	{
		"urn:ietf:params:xml:ns:xmpp-sasl", "jabber:iq:version",
		"jabber:iq:stats", "starttls", "jabber:server:dialback"
	};
	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static final String[] HOSTNAMES_PROP_VAL =
	{"localhost", "tigase.org", "hefczyc.net"};
	public static final String SECURITY_PROP_KEY = "security";
	public static final String AUTHENTICATION_IDS_PROP_KEY = "authentication-ids";
	public static final String[] AUTHENTICATION_IDS_PROP_VAL = {"dialback"};
	public static final String DIALBACK_CLASS_PROP_KEY = "dialback/class";
	public static final String DIALBACK_CLASS_PROP_VAL =
		"tigase.auth.DialbackAuth";
	public static final String DIALBACK_FLAG_PROP_KEY = "dialback/flag";
	public static final String DIALBACK_FLAG_PROP_VAL =	"sufficient";

	private String[] hostnames = HOSTNAMES_PROP_VAL;

	//	implements XMPPService {

	private Map<String, XMPPProcessorIfc> processors =
		new ConcurrentSkipListMap<String, XMPPProcessorIfc>();

	private Map<String, XMPPSession> sessionsByHostIP =
		new ConcurrentSkipListMap<String, XMPPSession>();

	// Normal packets between users on different servers
	private Map<String, Packet> waitingPackets =
		new ConcurrentSkipListMap<String, Packet>();

	// Controll packets for s2s connection establishing
	private Map<String, Packet> waitingControllPackets =
		new ConcurrentSkipListMap<String, Packet>();

	// 	private Map<String, XMPPResourceConnection> connectionsByHostIP_ConnType =
// 		new ConcurrentSkipListMap<String, XMPPResourceConnection>();

	public void processPacket(Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		if (packet.isCommand()) {
			processCommand(packet);
		} else {
			try {
				String ipAddress = JID.getNodeHostIP(packet.getTo());
				String hostName = JID.getNodeHost(packet.getTo());
				log.finest("Remote server IP address is: " + ipAddress);
				XMPPSession session =	sessionsByHostIP.get(ipAddress);
				if (session == null) {
					waitingPackets.put(ipAddress, packet);
					// Open new s2s connection
					connectRemoteServer(ipAddress, hostName, SocketType.plain);
				} // end of if (session == null)
				else {
					writePacketToSocket(packet, JID.getJID(null, ipAddress,
							ConnectionType.connect.toString()));
				} // end of if (session == null) else
			} // end of try
			catch (UnknownHostException e) {
				log.warning("UnknownHostException: " + e);
			} // end of try-catch
		} // end of else
	}

	private void connectRemoteServer(String ipAddress, String hostName,
		SocketType socket) {

		Map<String, Object> port_props = new TreeMap<String, Object>();
		port_props.put("remote-ip", ipAddress);
		if (hostName != null) {
			port_props.put("remote-hostname", hostName);
		} // end of if (hostName != null)
		port_props.put("ifc", new String[] {ipAddress});
		port_props.put("socket", socket);
		port_props.put("type", ConnectionType.connect);
		switch (socket) {
		case plain:
			port_props.put("port-no", 5269);
			break;
		case ssl:
			port_props.put("port-no", 5270);
			break;
		default:
			break;
		} // end of switch (socket)
		startService(port_props);

	}

	public Queue<Packet> processSocketData(String id,
		ConcurrentMap<String, Object> sessionData, Queue<Packet> packets) {
 		Queue<Packet> results = new LinkedList<Packet>();
		Packet p = null;
		while ((p = packets.poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			String ipAddress = (String)sessionData.get("remote-ip");
			String connType = sessionData.get("type").toString();
			XMPPSession session =	sessionsByHostIP.get(ipAddress);
			XMPPResourceConnection conn =
				session.getResourceForJID(JID.getJID(null, ipAddress, connType));
			if (!walk(p, conn, p.getElement(), results)) {
				addOutPacket(p);
			}	else {
				for (Packet res: results) {
					XMPPIOService serv = getXMPPIOService(res.getTo());
					if (serv != null) {
						writePacketToSocket(serv, res);
					} else {
						// Ups service is not started yet....
						waitingControllPackets.put(res.getTo(), res);
						String ip_address = null;
						try {
							ip_address = JID.getNodeHostIP(res.getElemTo());
						} catch (UnknownHostException e) {
							ip_address = JID.getNodeHost(res.getElemTo());
						} // end of try-catch
						String hostName = JID.getNodeHost(res.getElemTo());
						connectRemoteServer(ip_address, hostName,	SocketType.plain);
					} // end of else
				} // end of for (Packet p: results)
			} // end of else
		} // end of while ()
 		return null;
	}

	private boolean walk(final Packet packet,
		final XMPPResourceConnection connection, final Element elem,
		final Queue<Packet> results) {
		boolean result = false;
		for (XMPPProcessorIfc proc: processors.values()) {
			String xmlns = elem.getXMLNS();
			log.finest("Element XMLNS="+xmlns);
			log.finest("Processor: id="+proc.id()
				+", supElements="+Arrays.toString(proc.supElements())
				+", supNamespaces="+Arrays.toString(proc.supNamespaces()));
			if (xmlns == null) { xmlns = "jabber:server";	}
			if (proc.isSupporting(elem.getName(), xmlns)) {
				log.finest("XMPPProcessorIfc: "+proc.getClass().getSimpleName()+
					" ("+proc.id()+")"+"\n Request: "+elem.toString());
				proc.process(packet, connection, results);
				result = true;
			} // end of if (proc.isSupporting(elem.getName(), elem.getXMLNS()))
		} // end of for ()
		Collection<Element> children = elem.getChildren();
		if (children != null) {
			for (Element child: children) {
				result |= walk(packet, connection, child, results);
			} // end of for (Element child: children)
		} // end of if (children != null)
		return result;
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
		String remote_ip = serv.getRemoteAddress();
		String conn_jid = JID.getJID(null, remote_ip,
			serv.connectionType().toString());
		XMPPSession session = sessionsByHostIP.get(remote_ip);
		XMPPResourceConnection conn =	session.getResourceForJID(conn_jid);
		XMPPProcessorIfc proc = processors.get("jabber:server:dialback");
		switch (serv.connectionType()) {
		case connect:
			String remote_id = attribs.get("id");
			conn.setSessionId(remote_id);
			if (proc != null) {
				Queue<Packet> results = new LinkedList<Packet>();
				proc.process(null, conn, results);
				StringBuilder sb = new StringBuilder();
				for (Packet p: results) {
					sb.append(p.getStringData());
				} // end of for (Packet p: results)
				// Attach also all controll packets which are wating to send
				Packet p = null;
				while ((p = waitingControllPackets.remove(conn_jid)) != null) {
					log.finest("Sending packet: " + p.getStringData());
					sb.append(p.getStringData());
				} // end of while (p = waitingPackets.remove(ipAddress) != null)
				return sb.toString();
// 				try {
// 					writePacketsToSocket(serv, results);
// 				} // end of try
// 				catch (IOException e) {
// 					log.log(Level.WARNING, "Can not send response to remote server.", e);
// 				} // end of try-catch
			} // end of if (proc != null)
			else {
				// Server MUST generate an <invalid-namespace/> stream error condition
				// and terminate both the XML stream and the underlying TCP connection.
			} // end of if (proc != null) else
			break;
		case accept:
			log.finer("Stream opened: " + attribs.toString());
			final String id = UUID.randomUUID().toString();
			serv.getSessionData().put(serv.SESSION_ID_KEY, id);
			conn.setSessionId(id);
			return "<stream:stream"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " xmlns='jabber:server'"
				+ (proc != null ? " xmlns:db='jabber:server:dialback'" : "")
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

	protected String getUniqueId(IOService serv) {
		return JID.getJID(null, serv.getRemoteAddress(),
			serv.connectionType().toString());
	}

// 	protected String getServiceId(Packet packet) {
// 		return JID.getNodeHost(packet.getTo());
// 	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(COMPONENTS_PROP_KEY, COMPONENTS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTHENTICATION_IDS_PROP_KEY,
			AUTHENTICATION_IDS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + DIALBACK_CLASS_PROP_KEY,
			DIALBACK_CLASS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + DIALBACK_FLAG_PROP_KEY,
			DIALBACK_FLAG_PROP_VAL);
		return props;
	}

	protected int[] getDefPlainPorts() {
		return new int[] {5269};
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		String[] components = (String[])props.get(COMPONENTS_PROP_KEY);
		processors.clear();
		for (String comp_id: components) {
			XMPPProcessorIfc proc = ProcessorFactory.getProcessor(comp_id);
			processors.put(comp_id, proc);
			log.config("Added processor: " + proc.getClass().getSimpleName()
				+ " for component id: " + comp_id);
		} // end of for (String comp_id: components)
		hostnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		if (hostnames == null || hostnames.length == 0) {
			log.warning("Hostnames definition is empty, setting 'localhost'");
			hostnames = new String[] {"localhost"};
		} // end of if (hostnames == null || hostnames.length == 0)

		String[] auth_ids = (String[])props.get(SECURITY_PROP_KEY + "/" +
			AUTHENTICATION_IDS_PROP_KEY);
		TigaseConfiguration authConfig = TigaseConfiguration.getConfiguration();
		for (String id: auth_ids) {
			String class_name = (String)props.get(SECURITY_PROP_KEY + "/" + id + "/class");
			String flag = (String)props.get(SECURITY_PROP_KEY + "/" + id + "/flag");
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(CommitHandler.COMMIT_HANDLER_KEY, this);
			AppConfigurationEntry ace =
				new AppConfigurationEntry(class_name, parseFlag(flag), options);
			authConfig.putAppConfigurationEntry(id,
				new AppConfigurationEntry[] {ace});
			log.config("Added security module: " + class_name
				+ " for auth id: " + id + ", flag: " + flag);
		} // end of for ()
		addRouting("*");
	}

	private LoginModuleControlFlag parseFlag(final String flag) {
		if (flag.equalsIgnoreCase("REQUIRED"))
			return LoginModuleControlFlag.REQUIRED;
		else if (flag.equalsIgnoreCase("REQUISITE"))
			return LoginModuleControlFlag.REQUISITE;
		else if (flag.equalsIgnoreCase("SUFFICIENT"))
			return LoginModuleControlFlag.SUFFICIENT;
		else if (flag.equalsIgnoreCase("OPTIONAL"))
			return LoginModuleControlFlag.OPTIONAL;
		return null;
	}

	public void serviceStarted(final IOService service) {
		super.serviceStarted(service);
		log.finest("s2s connection opened: " + service.getRemoteAddress());
		initSession(service);
		switch (service.connectionType()) {
		case connect:
			// Send init xmpp stream here
			XMPPIOService serv = (XMPPIOService)service;
			XMPPProcessorIfc proc = processors.get("jabber:server:dialback");
			serv.xmppStreamOpen("<stream:stream"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " xmlns='jabber:server'"
				+ (proc != null ? " xmlns:db='jabber:server:dialback'" : "")
				+ ">"
				);
			break;
		default:
			// Do nothing, more data should come soon...
			break;
		} // end of switch (service.connectionType())
	}

	private void initSession(final IOService service) {
		String remote_ip = service.getRemoteAddress();
		service.getSessionData().put("remote-ip", remote_ip);
		String conn_jid = JID.getJID(null, remote_ip,
			service.connectionType().toString());
		log.finest("remote_ip="+remote_ip+", conn_jid="+conn_jid);
		XMPPSession session = sessionsByHostIP.get(remote_ip);
		if (session == null) {
			log.finest("No session found for "+remote_ip+" IP address, creating new one.");
			session = new XMPPSession(remote_ip);
			sessionsByHostIP.put(remote_ip, session);
		} // end of if (session == null)
		else {
			log.finest("Found old session for "+remote_ip+" IP address.");
		} // end of else
		XMPPResourceConnection conn =	session.getResourceForJID(conn_jid);
		//XMPPResourceConnection conn =	connectionsByHostIP_ConnType.get(conn_jid);
		if (conn != null) { conn.streamClosed(); } // end of if (conn != null)
		conn = new XMPPResourceConnection(service.getUniqueId(), null);
		conn.setResource(service.connectionType().toString());
		conn.putSessionData("remote-hostname",
			service.getSessionData().get("remote-hostname"));
		conn.setDomain(hostnames[0]);
		log.finest("Setting variables for connection:"
			+ " resource=" + service.connectionType().toString()
			+ ", remote-hostname=" + service.getSessionData().get("remote-hostname")
			+ ", domain=" + hostnames[0]);
		session.addResourceConnection(conn);
// 		if (service.connectionType() == ConnectionType.accept) {
// 			String jid = JID.getJID(null, remote_ip,
// 				ConnectionType.connect.toString());
// 			if (session.getResourceForJID(jid) == null) {
// 				connectRemoteServer(remote_ip,
// 					(String)service.getSessionData().get("remote-hostname"),
// 					SocketType.plain);
// 			} // end of if (session.getResourceForJID(jid) == null)
// 		} // end of if (service.connectionType() == ConnectionType.accept)
	}

	public void serviceStopped(final IOService service) {
		super.serviceStopped(service);
	}

	public void handleLoginCommit(final String userName,
		final XMPPResourceConnection conn) {
		final String ipAddress = conn.getParentSession().getUserName();
		final String jid = JID.getJID(null, ipAddress, conn.getResource());
		Packet p = null;
		log.finest("handleLoginCommit: ipAddress="+ipAddress
			+", jid="+jid);
		while ((p = waitingPackets.remove(ipAddress)) != null) {
			log.finest("Sending packet: " + p.getStringData());
			writePacketToSocket(p, jid);
		} // end of while (p = waitingPackets.remove(ipAddress) != null)
	}

	public void handleLogout(final String userName,
		final XMPPResourceConnection conn) {
		
	}

}
