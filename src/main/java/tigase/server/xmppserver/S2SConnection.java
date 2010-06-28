
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 of the License.
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 14, 2010 1:19:55 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SConnection implements Comparable<S2SConnection> {
	private static final Logger log = Logger.getLogger(S2SConnection.class.getName());

	//~--- fields ---------------------------------------------------------------

	private ConnectionHandlerIfc<S2SIOService> handler = null;
	private String ipAddress = null;
	private S2SIOService service = null;
	private OutgoingState conn_state = OutgoingState.NULL;

	/**
	 * Control packets for s2s connection establishing
	 */
	private ConcurrentLinkedQueue<Packet> waitingControlPackets =
		new ConcurrentLinkedQueue<Packet>();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param handler
	 * @param ip
	 */
	public S2SConnection(ConnectionHandlerIfc<S2SIOService> handler, String ip) {
		this.handler = handler;
		this.ipAddress = ip;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packetInstance
	 */
	public void addControlPacket(Packet packetInstance) {
		waitingControlPackets.add(packetInstance);
	}

	/**
	 * Method description
	 *
	 *
	 * @param o
	 *
	 * @return
	 */
	@Override
	public int compareTo(S2SConnection o) {
		return hashCode() - o.hashCode();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getIPAddress() {
		return ipAddress;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public S2SIOService getS2SIOService() {
		return service;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isConnected() {
		return (service != null) && service.isConnected();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	public void sendAllControlPackets() {
		if (log.isLoggable(Level.FINEST)) {
			for (Packet packet : waitingControlPackets) {
				log.log(Level.FINEST, "Sending on connection: {0} control packet: {1}",
						new Object[] { service,
						packet });
			}
		}

		handler.writePacketsToSocket(service, waitingControlPackets);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	public boolean sendPacket(Packet packet) throws IOException {
		return handler.writePacketToSocket(service, packet);
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void setS2SIOService(S2SIOService serv) {
		this.service = serv;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
