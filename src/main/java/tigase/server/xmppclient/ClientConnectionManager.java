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

package tigase.server.xmppclient;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.disco.XMPPService;
import tigase.util.DNSResolver;
import tigase.util.JIDUtils;
import tigase.util.RoutingsContainer;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
//import tigase.net.IOService;
import tigase.net.SocketReadThread;

/**
 * Class ClientConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClientConnectionManager extends ConnectionManager<XMPPIOService> {
	//	implements XMPPService {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppclient.ClientConnectionManager");

	private static final String XMLNS = "jabber:client";

	private static final String ROUTINGS_PROP_KEY = "routings";
	private static final String ROUTING_MODE_PROP_KEY = "multi-mode";
	private static final boolean ROUTING_MODE_PROP_VAL = true;
	private static final String ROUTING_ENTRY_PROP_KEY = ".+";
	private static final String ROUTING_ENTRY_PROP_VAL = DEF_SM_NAME + "@localhost";

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	protected RoutingsContainer routings = null;
	protected Set<String> hostnames = new TreeSet<String>();

	private Map<String, XMPPProcessorIfc> processors =
		new ConcurrentSkipListMap<String, XMPPProcessorIfc>();

	public void processPacket(final Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		if (packet.isCommand() && packet.getCommand() != Command.OTHER) {
			processCommand(packet);
		} else {
			writePacketToSocket(packet);
		} // end of else
	}

	protected void processCommand(final Packet packet) {
		XMPPIOService serv = getXMPPIOService(packet);
		switch (packet.getCommand()) {
		case GETFEATURES:
			if (packet.getType() == StanzaType.result) {
				List<Element> features = getFeatures(getXMPPSession(packet));
				Element elem_features = new Element("stream:features");
				elem_features.addChildren(features);
				elem_features.addChildren(Command.getData(packet));
				Packet result = new Packet(elem_features);
				result.setTo(packet.getTo());
				writePacketToSocket(result);
			} // end of if (packet.getType() == StanzaType.get)
			break;
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
					Element proceed = Command.getData(packet, "proceed", null);
					Packet p_proceed = new Packet(proceed);
 					SocketReadThread readThread = SocketReadThread.getInstance();
 					readThread.removeSocketService(serv);
					//					writePacketToSocket(serv, p_proceed);
 					serv.addPacketToSend(p_proceed);
 					serv.processWaitingPackets();
					serv.startTLS(false);
					//					serv.call();
 					readThread.addSocketService(serv);
				} catch (IOException e) {
					log.warning("Error starting TLS: " + e);
				} // end of try-catch
			} else {
				log.warning("Can't find sevice for STARTTLS command: " +
					packet.getStringData());
			} // end of else
			break;
		case REDIRECT:
			String command_sessionId = Command.getFieldValue(packet, "session-id");
			String newAddress = packet.getFrom();
			String old_receiver = changeDataReceiver(packet, newAddress,
				command_sessionId, serv);
			if (old_receiver != null) {
				log.fine("Redirecting data for sessionId: " + command_sessionId
					+ ", to: " + newAddress);
				Packet response = packet.commandResult(null);
				Command.addFieldValue(response, "session-id", command_sessionId);
				Command.addFieldValue(response, "action", "close");
				response.getElement().setAttribute("to", old_receiver);
				addOutPacket(response);
				response = packet.commandResult(null);
				Command.addFieldValue(response, "session-id", command_sessionId);
				Command.addFieldValue(response, "action", "activate");
				response.getElement().setAttribute("to", newAddress);
				addOutPacket(response);
			} else {
				log.finest("Connection for REDIRECT command does not exist, ignoring "
					+ "packet: " + packet.toString());
			}
			break;
		case STREAM_CLOSED:

			break;
		case GETDISCO:

			break;
		case CLOSE:
			if (serv != null) {
				serv.stop();
			} else {
				log.fine("Attempt to stop non-existen service for packet: "
					+ packet.getStringData()
					+ ", Service already stopped?");
			} // end of if (serv != null) else
			break;
		default:
			break;
		} // end of switch (pc.getCommand())
	}

	protected String changeDataReceiver(Packet packet, String newAddress,
		String command_sessionId, XMPPIOService serv) {
		if (serv != null) {
			String serv_sessionId =
          (String)serv.getSessionData().get(serv.SESSION_ID_KEY);
			if (serv_sessionId.equals(command_sessionId)) {
				String old_receiver = serv.getDataReceiver();
				serv.setDataReceiver(newAddress);
				return old_receiver;
			} else {
				log.warning("Incorrect session ID, ignoring data redirect for: "
					+ newAddress + ", expected: " + serv_sessionId
					+ ", received: " + command_sessionId);
			}
		}
		return null;
	}

	public Queue<Packet> processSocketData(XMPPIOService serv) {

		String id = getUniqueId(serv);
		//String hostname = (String)serv.getSessionData().get(serv.HOSTNAME_KEY);

		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			p.setFrom(getFromAddress(id));
			p.setTo(serv.getDataReceiver());
// 			p.setTo(serv.getDataReceiver() != null ? serv.getDataReceiver()
// 				: routings.computeRouting(hostname));
			addOutPacket(p);
			// 			results.offer(new Packet(new Element("OK")));
		} // end of while ()
// 		return results;
		return null;
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		if (params.get(GEN_VIRT_HOSTS) != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		}
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		Boolean r_mode = (Boolean)params.get(getName() + "/" + ROUTINGS_PROP_KEY
			+ "/" + ROUTING_MODE_PROP_KEY);
		if (r_mode == null) {
			props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY,
				ROUTING_MODE_PROP_VAL);
			// If the server is configured as connection manager only node then
			// route packets to SM on remote host where is default routing
			// for external component.
			// Otherwise default routing is to SM on localhost
			if (params.get("config-type").equals(GEN_CONFIG_CS)
				&& params.get(GEN_EXT_COMP) != null) {
				String[] comp_params = ((String)params.get(GEN_EXT_COMP)).split(",");
				props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
					DEF_SM_NAME + "@" + comp_params[1]);
			} else {
				props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
					DEF_SM_NAME + "@" + DNSResolver.getDefaultHostname());
			}
		}
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		boolean routing_mode =
			(Boolean)props.get(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY);
		routings = new RoutingsContainer(routing_mode);
		int idx = (ROUTINGS_PROP_KEY + "/").length();
		for (Map.Entry<String, Object> entry: props.entrySet()) {
			if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/")
				&& !entry.getKey().equals(ROUTINGS_PROP_KEY + "/" +
					ROUTING_MODE_PROP_KEY)) {
				routings.addRouting(entry.getKey().substring(idx),
					(String)entry.getValue());
			} // end of if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/"))
		} // end of for ()
		String[] hnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		clearRoutings();
		hostnames.clear();
		for (String host: hnames) {
			addRouting(getName() + "@" + host);
			hostnames.add(host);
		} // end of for ()
	}

	private XMPPResourceConnection getXMPPSession(Packet p) {
		XMPPIOService serv = getXMPPIOService(p);
		return serv == null ? null :
			(XMPPResourceConnection)serv.getSessionData().get("xmpp-session");
	}

	private List<Element> getFeatures(XMPPResourceConnection session) {
		List<Element> results = new LinkedList<Element>();
		for (XMPPProcessorIfc proc: processors.values()) {
			Element[] features = proc.supStreamFeatures(session);
			if (features != null) {
				results.addAll(Arrays.asList(features));
			} // end of if (features != null)
		} // end of for ()
		return results;
	}

	protected int[] getDefPlainPorts() {
		return new int[] {5222};
	}

	protected int[] getDefSSLPorts() {
		return new int[] {5223};
	}

	private String getFromAddress(String id) {
		return JIDUtils.getJID(getName(), getDefHostName(), id);
	}

	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {
		log.finer("Stream opened: " + attribs.toString());
		final String hostname = attribs.get("to");
		String lang = attribs.get("xml:lang");
		if (lang == null) {
			lang = "en";
		}
		String id = (String)serv.getSessionData().get(serv.SESSION_ID_KEY);
		if (id == null) {
			id = UUID.randomUUID().toString();
			serv.getSessionData().put(serv.SESSION_ID_KEY, id);
			serv.setXMLNS(XMLNS);
		}
		if (hostname == null) {
			return "<?xml version='1.0'?><stream:stream"
				+ " xmlns='" + XMLNS + "'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " id='" + id + "'"
				+ " from='" + getDefHostName() + "'"
        + " version='1.0' xml:lang='en'>"
				+ "<stream:error>"
				+ "<improper-addressing xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
		} // end of if (hostname == null)

		if (!hostnames.contains(hostname)) {
			return "<?xml version='1.0'?><stream:stream"
				+ " xmlns='" + XMLNS + "'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " id='" + id + "'"
				+ " from='" + getDefHostName() + "'"
        + " version='1.0' xml:lang='en'>"
				+ "<stream:error>"
				+ "<host-unknown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
		} // end of if (!hostnames.contains(hostname))

// 		try {
			writeRawData(serv, "<?xml version='1.0'?><stream:stream"
				+ " xmlns='" + XMLNS + "'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " from='" + hostname + "'"
				+ " id='" + id + "'"
				+ " version='1.0' xml:lang='en'>");
			serv.getSessionData().put(serv.HOSTNAME_KEY, hostname);
			serv.setDataReceiver(routings.computeRouting(hostname));
			Packet streamOpen = Command.STREAM_OPENED.getPacket(
				getFromAddress(getUniqueId(serv)),
				serv.getDataReceiver(), StanzaType.set, "sess1", "submit");
			Command.addFieldValue(streamOpen, "session-id", id);
			Command.addFieldValue(streamOpen, "hostname", hostname);
			Command.addFieldValue(streamOpen, "xml:lang", lang);
			addOutPacket(streamOpen);
			if (attribs.get("version") != null) {
				addOutPacket(Command.GETFEATURES.getPacket(
					getFromAddress(getUniqueId(serv)),
					serv.getDataReceiver(), StanzaType.get, "sess2", null));
			} // end of if (attribs.get("version") != null)
// 		} catch (IOException e) {
// 			serv.stop();
// 		}

		return null;
	}

	public void serviceStopped(XMPPIOService service) {
		super.serviceStopped(service);
		if (service.getXMLNS() == XMLNS) {
			//		XMPPIOService serv = (XMPPIOService)service;
			if (service.getDataReceiver() != null) {
				Packet command = Command.STREAM_CLOSED.getPacket(
					getFromAddress(getUniqueId(service)),
					service.getDataReceiver(), StanzaType.set, "sess1");
				addOutPacket(command);
				log.fine("Service stopped, sending packet: " + command.getStringData());
			} else {
				log.fine("Service stopped, before stream:stream received");
			}
		}
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. Let's assume user should send something
	 * at least once every 24 hours....
	 *
	 * @return a <code>long</code> value
	 */
	protected long getMaxInactiveTime() {
		return 24*HOUR;
	}

	protected XMPPIOService getXMPPIOServiceInstance() {
		return new XMPPIOService();
	}

}
