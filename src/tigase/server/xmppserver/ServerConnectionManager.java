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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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
import tigase.net.SocketReadThread;
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

	private Map<String, XMPPResourceConnection> connectionsByHostIP_ConnType =
		new ConcurrentSkipListMap<String, XMPPResourceConnection>();

	public void processPacket(Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		if (packet.isCommand()) {
			processCommand(packet);
		} else {
			writePacketToSocket(packet);
		} // end of else
	}

	private void processCommand(final Packet packet) {
		XMPPIOService serv = getXMPPIOService(packet);
		switch (packet.getCommand()) {
		case STARTTLS:
			if (serv != null) {
				log.finer("Starting TLS for connection: " + serv.getUniqueId());
				try {
					// Note:
					// If you send <proceed> packet to client you must expect
					// instant response from the client with TLS handshaking data
					// before you will call startTLS() on server side.
					// So the initial handshaking data might be lost as they will
					// be processed in another thread reading data from the socket.
					// That's why below code first removes service from reading
					// threads pool and then sends <proceed> packet and starts TLS.
					SocketReadThread readThread = SocketReadThread.getInstance();
					readThread.removeSocketService(serv);
					Element proceed = packet.getElement().getChild("proceed");
					log.finest("Packet: " + packet.getElement().toString());
					Packet p_proceed = new Packet(proceed);
					serv.addPacketToSend(p_proceed);
					serv.processWaitingPackets();
					serv.startTLS(false);
					readThread.addSocketService(serv);
				} catch (IOException e) {
					log.warning("Error starting TLS: " + e);
				} // end of try-catch
			} else {
				log.warning("Can't find sevice for STARTTLS command: " +
					packet.getStringData());
			} // end of else
			break;
		case STREAM_CLOSED:

			break;
		case GETDISCO:

			break;
		case CLOSE:
			if (serv != null) {
				try {
					serv.stop();
				} // end of try
				catch (IOException e) {
					log.log(Level.WARNING, "Error stopping service: ", e);
				} // end of try-catch
			} // end of if (serv != null)
			else {
				log.fine("Attempt to stop non-existen service for packet: "
					+ packet.getStringData()
					+ ", Service already stopped?");
			} // end of if (serv != null) else
			break;
		default:
			break;
		} // end of switch (pc.getCommand())
	}

	public Queue<Packet> processSocketData(String id,
		ConcurrentMap<String, Object> sessionData, Queue<Packet> packets) {
// 		Queue<Packet> results = new LinkedList<Packet>();
		Packet p = null;
		while ((p = packets.poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			p.setFrom(JID.getJID(getName(), getDefHostName(), id));
			p.setTo(p.getElemTo());
			addOutPacket(p);
			// 			results.offer(new Packet(new Element("OK")));
		} // end of while ()
// 		return results;
		return null;
	}

	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {
		String remote_host = serv.getRemoteAddress();
		String conn_jid = JID.getJID(null, remote_host,
			serv.connectionType().toString());
		XMPPProcessorIfc proc = processors.get("jabber:server:dialback");
		String remote_id = attribs.get("id");
		if (remote_id != null) {
			XMPPResourceConnection conn =	connectionsByHostIP_ConnType.get(conn_jid);
			conn.setSessionId(remote_id);
			if (proc != null) {
				Queue<Packet> results = new LinkedList<Packet>();
				proc.process(null, conn, results);
				try {
					writePacketsToSocket(serv, results);
				} // end of try
				catch (IOException e) {
					log.log(Level.WARNING, "Can not send response to remote server.", e);
				} // end of try-catch
			} // end of if (proc != null)
			else {
				// Server MUST generate an <invalid-namespace/> stream error condition
				// and terminate both the XML stream and the underlying TCP connection.
			} // end of if (proc != null) else
			return null;
		} // end of if (remote_id != null)
		else {
			log.finer("Stream opened: " + attribs.toString());
			final String id = UUID.randomUUID().toString();
			serv.getSessionData().put(serv.SESSION_ID_KEY, id);
// 			XMPPSession session = sessionsByHostIP.get(remote_host);
// 			if (session == null) {
// 				session = new XMPPSession(remote_host);
// 				sessionsByHostIP.put(remote_host, session);
// 			} // end of if (session == null)
//  			XMPPResourceConnection conn =	session.getResourceForJID(conn_jid);
			XMPPResourceConnection conn =	connectionsByHostIP_ConnType.get(conn_jid);
 			if (conn != null) { conn.streamClosed(); } // end of if (conn != null)
			conn = new XMPPResourceConnection(serv.getUniqueId(), null);
// 			session.addResourceConnection(conn);
			connectionsByHostIP_ConnType.put(conn_jid, conn);
			return "<stream:stream"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " xmlns='jabber:server'"
				+ (proc != null ? " xmlns:db='jabber:server:dialback'" : "")
				+ " id='" + id + "'"
				+ ">"
				;
		} // end of if (remote_id != null) else
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
	}

	protected String getUniqueId(XMPPIOService serv) {
		return JID.getJID(null, serv.getRemoteAddress(),
			serv.connectionType().toString());
	}

	protected String getServiceId(Packet packet) {
		return JID.getNodeHost(packet.getTo());
	}

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

	public void handleLoginCommit(final String userName,
		final XMPPResourceConnection conn) {
		
	}

	public void handleLogout(final String userName,
		final XMPPResourceConnection conn) {
		
	}

}
