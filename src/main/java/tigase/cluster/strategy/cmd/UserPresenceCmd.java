/*
 * UserPresenceCmd.java
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
import tigase.cluster.api.CommandListenerSMAbstract;
import tigase.cluster.strategy.ConnectionRecord;
import tigase.cluster.strategy.DefaultClusteringStrategy;

//import tigase.cluster.strategy.OnlineUsersCachingStrategy;
import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author kobit
 */
public class UserPresenceCmd
				extends CommandListenerSMAbstract<ConnectionRecord,
						DefaultClusteringStrategy<ConnectionRecord>> {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(UserPresenceCmd.class.getName());

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param name
	 * @param strat
	 */
	public UserPresenceCmd(String name, final DefaultClusteringStrategy strat) {
		super(name, strat);
	}

	//~--- methods --------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
	 */

	/**
	 * Method description
	 *
	 *
	 * @param fromNode
	 * @param visitedNodes
	 * @param data
	 * @param packets
	 *
	 * @throws ClusterCommandException
	 */
	@Override
	public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
			String> data, Queue<Element> packets)
					throws ClusterCommandException {
		incSyncInTraffic();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
					new Object[] { fromNode,
					visitedNodes, data, packets });
		}

		ConnectionRecord rec      = getConnectionRecord(fromNode, data);
		Element          presence = packets.peek();

		for (Element elem : packets) {
			try {
				Packet packet = Packet.packetInstance(presence);

				packet.setPacketFrom(rec.getConnectionId());
				getStrategy().getSM().fastAddOutPacket(packet);
			} catch (TigaseStringprepException ex) {
				log.log(Level.WARNING, "Stringprep problem with presence packet: {0}", elem);
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/11/11
