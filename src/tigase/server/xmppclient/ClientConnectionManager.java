/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.util.JID;
import tigase.util.RoutingsContainer;
import tigase.xml.Element;
import tigase.xmpp.IqType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Class ClientConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClientConnectionManager extends ConnectionManager
	implements XMPPService {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppclient.ClientConnectionManager");

	private static final String ROUTINGS_PROP_KEY = "routings";
	public static final String ROUTING_MODE_PROP_KEY = "multi-mode";
	public static final boolean ROUTING_MODE_PROP_VAL = true;
	public static final String ROUTING_ENTRY_PROP_KEY = ".+@localhost";
	public static final String ROUTING_ENTRY_PROP_VAL = "session_1@localhost";

	private RoutingsContainer routings = null;

	private Map<String, XMPPProcessorIfc> processors =
		new TreeMap<String, XMPPProcessorIfc>();

	public void processPacket(final Packet packet) {
		log.finest("Processing packet: " + packet.getStringData());
		Packet pc = packet;
		if (packet.isRouted()) {
			pc = packet.unpackRouted();
		} // end of if (packet.isRouted())
		if (pc.isCommand()) {
			processCommand(pc, packet);
		} // end of if (pc.isCommand())
		else {
			writePacketToSocket(pc);
		} // end of else
	}

	private void processCommand(final Packet packet, final Packet routed) {
		Packet pc = packet;
		if (routed != null) {
			pc = routed;
		} // end of if (routed != null)
		switch (packet.getCommand()) {
		case GETFEATURES:
			if (packet.getType() == IqType.result) {
				String features = getFeatures(getXMPPSession(pc));
				Element elem_features = new Element("stream:features");
				elem_features.setCData(features);
				elem_features.addChildren(packet.getElement().getChildren());
				Packet result = new Packet(elem_features);
				result.setTo(packet.getTo());
				writePacketToSocket(result);
			} // end of if (pc.getType() == IqType.get)
			break;
		case STREAM_CLOSED:

			break;
		case GETDISCO:

			break;
		case CLOSE:
			XMPPIOService serv = getXMPPIOService(pc);
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
					+ pc.getStringData()
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
			log.finest("Processing socket data: " + p.getStringData());
			p.setFrom(JID.getJID(getName(), getDefHostName(), id));
			p.setTo(routings.computeRouting(p.getElemTo()));
			addOutPacket(p);
			// 			results.offer(new Packet(new Element("OK")));
		} // end of while ()
// 		return results;
		return null;
	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
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
	}

	private XMPPResourceConnection getXMPPSession(Packet p) {
		XMPPIOService serv = getXMPPIOService(p);
		return (XMPPResourceConnection)serv.getSessionData().get("xmpp-session");
	}

	private String getFeatures(XMPPResourceConnection session) {
		StringBuilder sb = new StringBuilder();
		for (XMPPProcessorIfc proc: processors.values()) {
			String[] features = proc.supStreamFeatures(session);
			if (features != null) {
				for (String f: features) {
					sb.append(f);
				} // end of for ()
			} // end of if (features != null)
		} // end of for ()
		return sb.toString();
	}

	protected int[] getDefPlainPorts() {
		return new int[] {5222};
	}

	protected int[] getDefSSLPorts() {
		return new int[] {5223};
	}

	protected String getDefPortClass() {
		return "tigase.xmpp.XMPPClient";
	}

	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {
		log.finer("Stream opened: " + attribs.toString());
		final String hostname = attribs.get("to");
		final String id = UUID.randomUUID().toString();
		StringBuilder sb = new StringBuilder();
		sb.append("<session-id>" + id + "</session-id>");
		if (hostname != null) {
			sb.append("<hostname>" + hostname + "</hostname>");
		} // end of if (hostname != null)
		serv.getSessionData().put(serv.SESSION_ID, id);
		addOutPacket(Command.STREAM_OPENED.getPacket(
									 JID.getJID(getName(), getDefHostName(), getUniqueId(serv)),
									 routings.computeRouting(hostname), IqType.set, "sess1",
									 sb.toString()));
		if (attribs.get("version") != null) {
			addOutPacket(Command.GETFEATURES.getPacket(
										 JID.getJID(getName(), getDefHostName(), getUniqueId(serv)),
										 routings.computeRouting(null), IqType.get, "sess1"));
		} // end of if (attribs.get("version") != null)
		return "<stream:stream version='1.0' xml:lang='en'"
			+ " to='kobit'"
			+ " id='" + id + "'"
			+ " xmlns='jabber:client'"
			+ " xmlns:stream='http://etherx.jabber.org/streams'>";
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
		addOutPacket(Command.STREAM_CLOSED.getPacket(
									 JID.getJID(getName(), getDefHostName(), getUniqueId(serv)),
									 routings.computeRouting(null), IqType.set, "sess1"));
	}

}
