/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.cluster.api;

import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author Artur Hefczyc Created Mar 16, 2011
 */
public interface ClusterControllerIfc {

	public static final String DELIVER_CLUSTER_PACKET_CMD = "deliver-cluster-packet-cmd";

	/**
	 * Method handles cluster packet received from cluster connection.
	 *
	 * @param packet which should be handled
	 */
	void handleClusterPacket(Element packet);

	/**
	 * Method is called on cluster node connection event. This is a notification to the component that a new cluster
	 * node has connected.
	 *
	 * @param addr is a hostname of a cluster node generating the event.
	 */
	void nodeConnected(String addr);

	/**
	 * Method is called on cluster node disconnection event. This is a notification to the component that there was
	 * network connection lost to one of the cluster nodes.
	 *
	 * @param addr is a hostname of a cluster node generating the event.
	 */
	void nodeDisconnected(String addr);

	void removeCommandListener(CommandListener listener);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param data additional data to be included in the packet
	 * @param packets collection of elements to be send to desired nodes
	 * @param fromNode address of the source node
	 * @param visitedNodes list of all already visited nodes
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, Map<String, String> data, Queue<Element> packets, JID fromNode,
					 Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param packets collection of elements to be send to desired nodes
	 * @param fromNode address of the source node
	 * @param visitedNodes list of all already visited nodes
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, Queue<Element> packets, JID fromNode, Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param data additional data to be included in the packet
	 * @param fromNode address of the source node
	 * @param visitedNodes list of all already visited nodes
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, Map<String, String> data, JID fromNode, Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param data additional data to be included in the packet
	 * @param fromNode address of the source node
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, Map<String, String> data, JID fromNode, JID... toNodes);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param fromNode address of the source node
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, JID fromNode, JID... toNodes);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param packet collection of elements to be send to desired nodes
	 * @param fromNode address of the source node
	 * @param visitedNodes list of all already visited nodes
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, Element packet, JID fromNode, Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method which sends command to desired nodes
	 *
	 * @param command ID string of the command
	 * @param data additional data to be included in the packet
	 * @param packet element to be send to desired nodes
	 * @param fromNode address of the source node
	 * @param visitedNodes list of all already visited nodes
	 * @param toNodes list of nodes to which packet should be sent
	 */
	void sendToNodes(String command, Map<String, String> data, Element packet, JID fromNode, Set<JID> visitedNodes,
					 JID... toNodes);

	void setCommandListener(CommandListener listener);
}

