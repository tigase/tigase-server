/*
 * ClusterController.java
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



package tigase.cluster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.ClusterElement;
import tigase.cluster.api.CommandListener;

import tigase.conf.Configurable;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.stats.StatisticsList;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Describe class ClusterController here.
 *
 *
 * Created: Mon Jun 9 20:03:28 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterController
				extends AbstractComponentRegistrator<ClusteredComponentIfc>
				implements Configurable, ClusterControllerIfc {
	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";

	/** Field description */
	public static final String  MY_DOMAIN_NAME_PROP_VAL = "localhost";
	private static final Logger log = Logger.getLogger(ClusterController.class.getName());

	//~--- fields ---------------------------------------------------------------

	private ConcurrentSkipListMap<String, CommandListener> commandListeners =
			new ConcurrentSkipListMap<String, CommandListener>();
	private AtomicLong currId = new AtomicLong(1L);

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentAdded(ClusteredComponentIfc component) {
		component.setClusterController(this);
		updateServiceDiscoveryItem(getName(), component.getName(), "Component: " + component
				.getName(), true);
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentRemoved(ClusteredComponentIfc component) {}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * tigase.cluster.api.ClusterControllerIfc#handleClusterPacket(tigase.xml.
	 * Element)
	 */

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void handleClusterPacket(Element packet) {
		ClusterElement  clel    = new ClusterElement(packet);
		CommandListener cmdList = commandListeners.get(clel.getMethodName());

		if (cmdList != null) {
			clel.addVisitedNode(JID.jidInstanceNS(packet.getAttributeStaticStr(Packet.TO_ATT)));

			Map<String, String> data         = clel.getAllMethodParams();
			Set<JID>            visitedNodes = clel.getVisitedNodes();
			Queue<Element>      packets      = clel.getDataPackets();

			try {
				cmdList.executeCommand(clel.getFirstNode(), visitedNodes, data, packets);

				// TODO Send result back (possibly)
			} catch (ClusterCommandException ex) {

				// TODO Send error back
				ex.printStackTrace();
			}
		} else {
			log.log(Level.WARNING, "Missing CommandListener for cluster method: {0}", clel
					.getMethodName());
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	public void nodeConnected(String node) {
		for (ClusteredComponentIfc comp : components.values()) {
			comp.nodeConnected(node);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	public void nodeDisconnected(String node) {
		for (ClusteredComponentIfc comp : components.values()) {
			comp.nodeDisconnected(node);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param results
	 */
	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * tigase.cluster.api.ClusterControllerIfc#removeCommandListener(java.lang
	 * .String, tigase.cluster.api.CommandListener)
	 */

	/**
	 * Method description
	 *
	 *
	 * @param listener
	 */
	@Override
	public void removeCommandListener(CommandListener listener) {
		commandListeners.remove(listener.getName(), listener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * java.util.Map, tigase.xml.Element, tigase.xmpp.JID, java.util.List,
	 * tigase.xmpp.JID[])
	 */

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
	@Override
	public void sendToNodes(String command, Map<String, String> data,
			Queue<Element> packets, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
		CommandListener packetSender = commandListeners.get(DELIVER_CLUSTER_PACKET_CMD);

		if (packetSender == null) {
			log.log(Level.SEVERE, "Misconfiguration or packaging error, can not send a " +
					"cluster packet! No CommandListener for " + DELIVER_CLUSTER_PACKET_CMD);

			return;
		}

		Queue<Element> results = new ArrayDeque<Element>();

		// TODO: Maybe more optimal would be creating the object once and then clone
		// it? However, the 'to' parameter must be double-checked whether all
		// internal states are set properly for each different to parameter
		for (JID to : toNodes) {
			ClusterElement clel = ClusterElement.createClusterMethodCall(fromNode, to,
					StanzaType.set, command, data);

			clel.addVisitedNodes(visitedNodes);
			clel.addDataPackets(packets);

			Element result = clel.getClusterElement(nextId());

			results.offer(result);
		}
		try {
			packetSender.executeCommand(null, null, null, results);
		} catch (ClusterCommandException ex) {

			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * tigase.xml.Element, tigase.xmpp.JID, java.util.List, tigase.xmpp.JID[])
	 */

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
	@Override
	public void sendToNodes(String command, Queue<Element> packets, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes) {
		sendToNodes(command, null, packets, fromNode, visitedNodes, toNodes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * java.util.Map, tigase.xmpp.JID, java.util.List, tigase.xmpp.JID[])
	 */

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
	@Override
	public void sendToNodes(String command, Map<String, String> data, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes) {
		sendToNodes(command, data, (Queue<Element>) null, fromNode, visitedNodes, toNodes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * java.util.Map, tigase.xmpp.JID, tigase.xmpp.JID[])
	 */

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param data
	 * @param fromNode
	 * @param toNodes
	 */
	@Override
	public void sendToNodes(String command, Map<String, String> data, JID fromNode,
			JID... toNodes) {
		sendToNodes(command, data, (Queue<Element>) null, fromNode, null, toNodes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * tigase.xmpp.JID, tigase.xmpp.JID[])
	 */

	/**
	 * Method description
	 *
	 *
	 * @param command
	 * @param fromNode
	 * @param toNodes
	 */
	@Override
	public void sendToNodes(String command, JID fromNode, JID... toNodes) {
		sendToNodes(command, null, (Queue<Element>) null, fromNode, null, toNodes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * tigase.xml.Element, tigase.xmpp.JID, java.util.List, tigase.xmpp.JID[])
	 */

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
	@Override
	public void sendToNodes(String command, Element packet, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes) {
		sendToNodes(command, null, packet, fromNode, visitedNodes, toNodes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.ClusterControllerIfc#sendToNodes(java.lang.String,
	 * java.util.Map, tigase.xml.Element, tigase.xmpp.JID, java.util.List,
	 * tigase.xmpp.JID[])
	 */

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
	@Override
	public void sendToNodes(String command, Map<String, String> data, Element packet,
			JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
		Queue<Element> packets = new ArrayDeque<Element>();

		packets.offer(packet);
		sendToNodes(command, data, packets, fromNode, visitedNodes, toNodes);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoCategoryType() {
		return "load";
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return "Server clustering";
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 *
	 * @return
	 */
	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof ClusteredComponentIfc;
	}

	//~--- set methods ----------------------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * tigase.cluster.api.ClusterControllerIfc#setCommandListener(java.lang.String
	 * , tigase.cluster.api.CommandListener)
	 */

	/**
	 * Method description
	 *
	 *
	 * @param listener
	 */
	@Override
	public void setCommandListener(CommandListener listener) {
		commandListeners.put(listener.getName(), listener);
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);
	}

	//~--- methods --------------------------------------------------------------

	private String nextId() {
		return "cl-" + currId.incrementAndGet();
	}
}


//~ Formatted in Tigase Code Convention on 13/07/06
