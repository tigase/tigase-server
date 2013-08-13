/*
 * ClusterControllerIfc.java
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



package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author Artur Hefczyc Created Mar 16, 2011
 */
public interface ClusterControllerIfc {
	/** Field description */
	public static final String DELIVER_CLUSTER_PACKET_CMD = "deliver-cluster-packet-cmd";

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	void handleClusterPacket(Element packet);

	/**
	 * @param addr
	 */
	void nodeConnected(String addr);

	/**
	 * @param addr
	 */
	void nodeDisconnected(String addr);

	/**
	 * Method description
	 *
	 *
	 * @param listener
	 */
	void removeCommandListener(CommandListener listener);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param data
	 * @param packets
	 * @param fromNode
	 * @param visitedNodes
	 * @param toNodes
	 */
	void sendToNodes(String command, Map<String, String> data, Queue<Element> packets,
			JID fromNode, Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param packets
	 * @param fromNode
	 * @param visitedNodes
	 * @param toNodes
	 */
	void sendToNodes(String command, Queue<Element> packets, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param data
	 * @param fromNode
	 * @param visitedNodes
	 * @param toNodes
	 */
	void sendToNodes(String command, Map<String, String> data, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param data
	 * @param fromNode
	 * @param toNodes
	 */
	void sendToNodes(String command, Map<String, String> data, JID fromNode,
			JID... toNodes);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param fromNode
	 * @param toNodes
	 */
	void sendToNodes(String command, JID fromNode, JID... toNodes);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param packet
	 * @param fromNode
	 * @param visitedNodes
	 * @param toNodes
	 */
	void sendToNodes(String command, Element packet, JID fromNode, Set<JID> visitedNodes,
			JID... toNodes);

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param data
	 * @param packet
	 * @param fromNode
	 * @param visitedNodes
	 * @param toNodes
	 */
	void sendToNodes(String command, Map<String, String> data, Element packet,
			JID fromNode, Set<JID> visitedNodes, JID... toNodes);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param listener
	 */
	void setCommandListener(CommandListener listener);
}


//~ Formatted in Tigase Code Convention on 13/07/06
