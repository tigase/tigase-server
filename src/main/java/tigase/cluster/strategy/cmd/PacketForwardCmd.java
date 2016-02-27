/*
 * PacketForwardCmd.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.strategy.cmd;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.CommandListenerAbstract;
import tigase.cluster.api.SessionManagerClusteredIfc;
import tigase.cluster.SessionManagerClustered;
import tigase.cluster.strategy.DefaultClusteringStrategyAbstract;

import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import tigase.server.Priority;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/06/22
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class PacketForwardCmd
				extends CommandListenerAbstract {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(PacketForwardCmd.class.getName());

	public static final String PACKET_FROM_KEY = "packet-from";

	//~--- fields ---------------------------------------------------------------

	private DefaultClusteringStrategyAbstract strategy;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 *
	 * @param name
	 * @param strategy
	 */
	public PacketForwardCmd(String name, DefaultClusteringStrategyAbstract strategy) {
		super(name, Priority.HIGH);
		this.strategy = strategy;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
			String> data, Queue<Element> packets)
					throws ClusterCommandException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
					new Object[] { fromNode,
					visitedNodes, data, packets });
		}
		if ((packets != null) && (packets.size() > 0)) {
			SessionManagerClusteredIfc sm = getSM();
			for (Element elem : packets) {
				try {
					Packet                 el_packet = Packet.packetInstance(elem);
					String packetFromStr  = data.get(PACKET_FROM_KEY);
					if (packetFromStr != null)
						el_packet.setPacketFrom(JID.jidInstanceNS(packetFromStr));
					XMPPResourceConnection conn      = sm.getXMPPResourceConnection(el_packet);
					Map<String, String>    locdata   = null;

					if (conn != null) {
						locdata = new LinkedHashMap<String, String>();
						if (data != null) {
							locdata.putAll(data);
						}
						data.put(SessionManagerClusteredIfc.SESSION_FOUND_KEY, sm.getComponentId()
								.toString());
					}

					// The commented if below causes the packet to stop being forwarded
					// if it reached a host on which there is a user session to handle
					// it.
					// This is incorrect though, as there might be multiple users'
					// connections
					// to different nodes and each node should receive the packet.
					// if (conn != null || !sendToNextNode(fromNode, visitedNodes, data,
					// Packet.packetInstance(elem))) {
					// Instead, always send the packet to next node:
					boolean isSent;

					isSent = strategy.sendToNextNode(fromNode, visitedNodes, data, Packet
							.packetInstance(elem));

					// If there is a user session for the packet, process it
					if (conn != null) {

						// Hold on! If this is the first node (fromNode) it means the
						// packet was already processed here....
						if (!sm.getComponentId().equals(fromNode)) {
							sm.processPacket(el_packet, conn);
						} else {

							// Ignore the packet, it has been processed already
						}
					} else {

						// No user session, but if this is the first node the packet has
						// returned, so maybe this is a packet for offline storage?
						if (sm.getComponentId().equals(fromNode)) {

							// However it could have been processed on another node already
							if ((data == null) || (data.get(SessionManagerClustered
									.SESSION_FOUND_KEY) == null)) {
								sm.processPacket(el_packet, conn);
							}
						}
					}
				} catch (TigaseStringprepException ex) {
					log.warning("Addressing problem, stringprep failed for packet: " + elem);
				}
			}
		} else {
			log.finest("Empty packets list in the forward command");
		}
	}
	
	private SessionManagerClusteredIfc getSM() {
		return strategy.getSM();
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
