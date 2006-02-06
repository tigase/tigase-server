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


import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import tigase.server.ConnectionManager;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.xml.Element;
import tigase.util.JID;

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

  public static final String ROUTE_ADDRESS_PROP_KEY = "route-address";
  public static final String ROUTE_ADDRESS_PROP_VAL =	"session_1@localhost";

	private String routeAddress = ROUTE_ADDRESS_PROP_VAL;


	public void processPacket(Packet packet) {
		if (packet.isRouted()) {
			Packet res = packet.unpackRouted();
			res.setTo(packet.getElemTo());
			writePacketToSocket(res);
		} // end of if (packet.isRouted())
		else {
			writePacketToSocket(packet);
		} // end of if (packet.isRouted()) else
	}

	public Queue<Packet> processSocketData(String id,
		ConcurrentMap<String, Object> sessionData, Queue<Packet> packets) {
// 		Queue<Packet> results = new LinkedList<Packet>();
		Packet p = null;
		while ((p = packets.poll()) != null) {
			log.finest("Processing socket data: " + p.getStringData());
			p.setFrom(JID.getJID(getName(), getDefHostName(), id));
			p.setTo(getDefRoutingAddress());
			addOutPacket(p);
			// 			results.offer(new Packet(new Element("OK")));
		} // end of while ()
// 		return results;
		return null;
	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
    props.put(ROUTE_ADDRESS_PROP_KEY, ROUTE_ADDRESS_PROP_VAL);
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		routeAddress = (String)props.get(ROUTE_ADDRESS_PROP_KEY);
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

	protected String getDefRoutingAddress() {
		return routeAddress;
	}

}
