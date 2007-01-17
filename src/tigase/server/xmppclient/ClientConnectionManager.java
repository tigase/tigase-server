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
import tigase.server.XMPPService;
import tigase.util.DNSResolver;
import tigase.util.JID;
import tigase.util.RoutingsContainer;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.net.IOService;
import tigase.net.SocketReadThread;

/**
 * Class ClientConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClientConnectionManager extends ConnectionManager {
	//	implements XMPPService {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppclient.ClientConnectionManager");

	private static final String ROUTINGS_PROP_KEY = "routings";
	public static final String ROUTING_MODE_PROP_KEY = "multi-mode";
	public static final boolean ROUTING_MODE_PROP_VAL = true;
	public static final String ROUTING_ENTRY_PROP_KEY = ".+";
	public static final String ROUTING_ENTRY_PROP_VAL = "session_1@localhost";

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	private RoutingsContainer routings = null;
	private String defHostName = null;
	private Set<String> hostnames = new TreeSet<String>();

	private Map<String, XMPPProcessorIfc> processors =
		new ConcurrentSkipListMap<String, XMPPProcessorIfc>();

	public void processPacket(final Packet packet) {
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
		case GETFEATURES:
			if (packet.getType() == StanzaType.result) {
				List<Element> features = getFeatures(getXMPPSession(packet));
				Element elem_features = new Element("stream:features");
				elem_features.addChildren(features);
				elem_features.addChildren(packet.getElement().getChildren());
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

	public Queue<Packet> processSocketData(XMPPIOService serv) {

		String id = getUniqueId(serv);

		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			p.setFrom(getFromAddress(id));
			p.setTo(routings.computeRouting(p.getElemTo()));
			addOutPacket(p);
			// 			results.offer(new Packet(new Element("OK")));
		} // end of while ()
// 		return results;
		return null;
	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY,
			ROUTING_MODE_PROP_VAL);
		props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
			ROUTING_ENTRY_PROP_VAL);
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
		defHostName = null;
		for (String host: hnames) {
			addRouting(getName() + "@" + host);
			hostnames.add(host);
			if (defHostName == null) {
				defHostName = host;
			} // end of if (defHostName == null)
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
		return JID.getJID(getName(), getDefHostName(), id);
	}

	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {
		log.finer("Stream opened: " + attribs.toString());
		final String hostname = attribs.get("to");
		final String id = UUID.randomUUID().toString();
		if (hostname == null) {
			return "<stream:stream version='1.0' xml:lang='en'"
				+ " from='" + defHostName + "'"
				+ " id='" + id + "'"
				+ " xmlns='jabber:client'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'>"
				+ "<stream:error>"
				+ "<improper-addressing xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
		} // end of if (hostname == null)

		if (!hostnames.contains(hostname)) {
			return "<stream:stream version='1.0' xml:lang='en'"
				+ " from='" + defHostName + "'"
				+ " id='" + id + "'"
				+ " xmlns='jabber:client'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'>"
				+ "<stream:error>"
				+ "<host-unknown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
		} // end of if (!hostnames.contains(hostname))

		serv.getSessionData().put(serv.SESSION_ID_KEY, id);
		serv.getSessionData().put(serv.HOSTNAME_KEY, hostname);
		Packet streamOpen = Command.STREAM_OPENED.getPacket(
			getFromAddress(getUniqueId(serv)),
			routings.computeRouting(hostname), StanzaType.set, "sess1");
		Command.addFieldValue(streamOpen, "session-id", id);
		Command.addFieldValue(streamOpen, "hostname", hostname);
		addOutPacket(streamOpen);
		if (attribs.get("version") != null) {
			addOutPacket(Command.GETFEATURES.getPacket(
					getFromAddress(getUniqueId(serv)),
					routings.computeRouting(null), StanzaType.get, "sess1"));
		} // end of if (attribs.get("version") != null)
		return "<stream:stream version='1.0' xml:lang='en'"
			+ " from='" + hostname + "'"
			+ " id='" + id + "'"
			+ " xmlns='jabber:client'"
			+ " xmlns:stream='http://etherx.jabber.org/streams'>";
	}

	public void serviceStopped(IOService service) {
		super.serviceStopped(service);
		//		XMPPIOService serv = (XMPPIOService)service;
		Packet command = Command.STREAM_CLOSED.getPacket(
			getFromAddress(getUniqueId(service)),
			routings.computeRouting(null), StanzaType.set, "sess1");
		addOutPacket(command);
		log.fine("Service stopped, sending packet: " + command.getStringData());
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
	}

	public String getDefHostName() {
		return defHostName == null ? super.getDefHostName() : defHostName;
	}

}
