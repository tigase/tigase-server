/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2011 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.cluster.api;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import tigase.xml.Element;
import tigase.xmpp.JID;

/**
 * @author Artur Hefczyc Created Mar 16, 2011
 */
public interface ClusterControllerIfc {

	public static final String DELIVER_CLUSTER_PACKET_CMD = "deliver-cluster-packet-cmd";

	/**
	 * @param addr
	 */
	void nodeDisconnected(String addr);

	/**
	 * @param addr
	 */
	void nodeConnected(String addr);

	void handleClusterPacket(Element packet);

	void sendToNodes(String command, Map<String, String> data, Queue<Element> packets,
			JID fromNode, Set<JID> visitedNodes, JID... toNodes);

	void sendToNodes(String command, Queue<Element> packets, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes);

	void sendToNodes(String command, Map<String, String> data, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes);

	void
			sendToNodes(String command, Map<String, String> data, JID fromNode, JID... toNodes);

	void sendToNodes(String command, JID fromNode, JID... toNodes);

	void setCommandListener(String command, CommandListener listener);

	void removeCommandListener(String command, CommandListener listener);

	void sendToNodes(String command, Element packet, JID fromNode, Set<JID> visitedNodes,
			JID... toNodes);

	void sendToNodes(String command, Map<String, String> data, Element packet,
			JID fromNode, Set<JID> visitedNodes, JID... toNodes);

}
