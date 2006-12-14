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

package tigase.server.xmppcomponent;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.net.ConnectionType;
import tigase.net.IOService;
import tigase.server.ConnectionManager;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.util.Algorithms;
import tigase.util.JID;
import tigase.xmpp.XMPPIOService;

/**
 * Class ComponentConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentConnectionManager extends ConnectionManager {
	//	implements XMPPService {

  public static final String SECRET_PROP_KEY = "secret";
  public static final String SECRET_PROP_VAL =	"someSecret";
	public static final String COMPONENT_NAME_KEY = "comp-name";
	public static final String COMPONENT_NAME_VAL = "comp_1.localhost";

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppcomponent.ComponentConnectionManager");

	public void processPacket(Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		writePacketToSocket(packet.packRouted());
	}

	public Queue<Packet> processSocketData(XMPPIOService serv) {
// 		String id,
// 		ConcurrentMap<String, Object> sessionData, Queue<Packet> packets) {

		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			if (p.isRouted()) {
				p = p.unpackRouted();
			} // end of if (p.isRouted())
			addOutPacket(p);
		} // end of while ()
		return null;
	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();

		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
	}

	protected int[] getDefPlainPorts() {
		return new int[] {5555};
	}

	protected Map<String, Object> getParamsForPort(int port) {
    Map<String, Object> defs = new TreeMap<String, Object>();
		defs.put(SECRET_PROP_KEY, SECRET_PROP_VAL);
		defs.put(COMPONENT_NAME_KEY, COMPONENT_NAME_VAL);
		return defs;
	}

	protected String getUniqueId(IOService serv) {
		String addr =
			(String)serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);
		return addr;
		// 		return serv.getRemoteHost();
	}

	public void serviceStopped(final IOService service) {
		super.serviceStopped(service);
		Map<String, Object> sessionData = service.getSessionData();
		String addr =	(String)sessionData.get(PORT_REMOTE_HOST_PROP_KEY);
		removeRouting(addr);
		ConnectionType type = service.connectionType();
		if (type == ConnectionType.connect) {
			reconnectService(sessionData, connectionDelay);
		} // end of if (type == ConnectionType.connect)
		//		removeRouting(serv.getRemoteHost());
	}

	protected String getServiceId(Packet packet) {
		return JID.getNodeHost(packet.getTo());
	}

	public void serviceStarted(final IOService service) {
		super.serviceStarted(service);
		log.finest("c2c connection opened: " + service.getRemoteAddress()
			+ ", type: " + service.connectionType().toString()
			+ ", id=" + service.getUniqueId());
// 		String addr =
// 			(String)service.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);
// 		addRouting(addr);
		//		addRouting(serv.getRemoteHost());
		switch (service.connectionType()) {
		case connect:
			// Send init xmpp stream here
			XMPPIOService serv = (XMPPIOService)service;
			String compName =
				(String)service.getSessionData().get(COMPONENT_NAME_KEY);
			String data =
				"<stream:stream"
				+ " xmlns='jabber:component:accept'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " to='" + compName + "'>"
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

	public String xmppStreamOpened(XMPPIOService service,
		Map<String, String> attribs) {

		switch (service.connectionType()) {
		case connect: {
			String id = attribs.get("id");
			service.getSessionData().put(service.SESSION_ID_KEY, id);
			String secret =
				(String)service.getSessionData().get(SECRET_PROP_KEY);
			try {
				String digest = Algorithms.hexDigest(id, secret, "SHA");
				return "<handshake>" + digest + "</handshake>";
			} catch (NoSuchAlgorithmException e) {
				log.log(Level.SEVERE, "Can not generate digest for pass phrase.", e);
				return null;
			}
		}
		case accept: {
			log.finer("Stream opened: " + attribs.toString());
			String hostname = attribs.get("to");
			String id = UUID.randomUUID().toString();
			service.getSessionData().put(service.SESSION_ID_KEY, id);
			return "<stream:stream"
				+ " xmlns='jabber:component:accept'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " from='" + hostname + "'"
				+ " id='" + id + "'"
				+ ">";
		}
		default:
			// Do nothing, more data should come soon...
			break;
		} // end of switch (service.connectionType())
		return null;
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
	}

}
