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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusterElement;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.CommandListener;
import tigase.conf.Configurable;
import tigase.conf.ConfigurationException;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.server.ServerComponent;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

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

	@Override
	public void componentAdded(ClusteredComponentIfc component) {
		// we are not passing wrapper to ClusterConnectionManager as we need to
		// check later if it's command is added
		if (component instanceof ClusterConnectionManager) {
			component.setClusterController(this);
		} else {
			Wrapper wrapper = new Wrapper(this, component);
			component.setClusterController(wrapper);
		}
		updateServiceDiscoveryItem(getName(), component.getName(), "Component: " + component
				.getName(), true);
	}

	@Override
	public void componentRemoved(ClusteredComponentIfc component) {}

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
	
	@Override
	public void nodeConnected(String node) {
		super.nodeConnected(node);
		for (ClusteredComponentIfc comp : components.values()) {
			comp.nodeConnected(node);
		}
	}

	@Override
	public void nodeDisconnected(String node) {
		super.nodeDisconnected(node);
		for (ClusteredComponentIfc comp : components.values()) {
			comp.nodeDisconnected(node);
		}
	}

	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {}

	@Override
	public void removeCommandListener(CommandListener listener) {
		removeCommandListener(listener.getName(), listener);
	}

	@Override
	public void sendToNodes(String command, Map<String, String> data,
			Queue<Element> packets, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
		// this command should not be prefixed as we passed original instance instead of 
		// wrapper to ClusterConnectionManager
		CommandListener packetSender = commandListeners.get(DELIVER_CLUSTER_PACKET_CMD);

		if (packetSender == null) {
			log.log(Level.SEVERE, "Misconfiguration or packaging error, can not send a " +
					"cluster packet! No CommandListener for " + DELIVER_CLUSTER_PACKET_CMD);

			return;
		}

		Queue<Element> results = new ArrayDeque<Element>();

		// retrive listener for command and it's priority to if available
		CommandListener listener = commandListeners.get(command);
		Priority priority = listener != null ? listener.getPriority() : null;
		
		// TODO: Maybe more optimal would be creating the object once and then clone
		// it? However, the 'to' parameter must be double-checked whether all
		// internal states are set properly for each different to parameter
		for (JID to : toNodes) {
			ClusterElement clel = ClusterElement.createClusterMethodCall(fromNode, to,
					StanzaType.set, command, data);

			// set priority to ClusterElement so it will get proper priority for processing
			if (priority != null)
				clel.setPriority(priority);
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

	@Override
	public void sendToNodes(String command, Queue<Element> packets, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes) {
		sendToNodes(command, null, packets, fromNode, visitedNodes, toNodes);
	}

	@Override
	public void sendToNodes(String command, Map<String, String> data, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes) {
		sendToNodes(command, data, (Queue<Element>) null, fromNode, visitedNodes, toNodes);
	}

	@Override
	public void sendToNodes(String command, Map<String, String> data, JID fromNode,
			JID... toNodes) {
		sendToNodes(command, data, (Queue<Element>) null, fromNode, null, toNodes);
	}

	@Override
	public void sendToNodes(String command, JID fromNode, JID... toNodes) {
		sendToNodes(command, null, (Queue<Element>) null, fromNode, null, toNodes);
	}

	@Override
	public void sendToNodes(String command, Element packet, JID fromNode,
			Set<JID> visitedNodes, JID... toNodes) {
		sendToNodes(command, null, packet, fromNode, visitedNodes, toNodes);
	}

	@Override
	public void sendToNodes(String command, Map<String, String> data, Element packet,
			JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
		Queue<Element> packets = new ArrayDeque<Element>();

		packets.offer(packet);
		sendToNodes(command, data, packets, fromNode, visitedNodes, toNodes);
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);

		return defs;
	}

	@Override
	public String getDiscoCategoryType() {
		return "load";
	}

	@Override
	public String getDiscoDescription() {
		return "Cluster controller";
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return (component instanceof ClusteredComponentIfc) && !(component instanceof ClusterControllerIfc);
	}

	@Override
	public void setCommandListener(CommandListener listener) {
		setCommandListener(listener.getName(), listener);
	}

	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Override
	public void setProperties(Map<String, Object> properties) throws ConfigurationException {
		super.setProperties(properties);
	}

	//~--- methods --------------------------------------------------------------

	private String nextId() {
		return "cl-" + currId.incrementAndGet();
	}
	
	private void removeCommandListener(String name, CommandListener listener) {
		commandListeners.remove(name, listener);
	}
	
	private void setCommandListener(String name, CommandListener listener) {
		commandListeners.put(name, listener);
	}
	
	private class Wrapper implements ClusterControllerIfc {

		private final ClusterController controller;
		private final ClusteredComponentIfc component;
		private final String name;
		
		public Wrapper(ClusterController controller, ClusteredComponentIfc component) {
			this.controller = controller;
			this.component = component;
			name = component.getName();
		}
		
		@Override
		public void handleClusterPacket(Element packet) {
			controller.handleClusterPacket(packet);
		}

		@Override
		public void nodeConnected(String addr) {
			throw new UnsupportedOperationException("This method should not be called."); 
		}

		@Override
		public void nodeDisconnected(String addr) {
			throw new UnsupportedOperationException("This method should not be called.");
		}

		@Override
		public void removeCommandListener(CommandListener listener) {
			String name = this.name + "-" + listener.getName();
			controller.removeCommandListener(name, listener);
		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, Queue<Element> packets, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, data, packets, fromNode, visitedNodes, toNodes);
		}

		@Override
		public void sendToNodes(String command, Queue<Element> packets, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, packets, fromNode, visitedNodes, toNodes);
		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, data, fromNode, visitedNodes, toNodes);
		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, JID fromNode, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, data, fromNode, toNodes);
		}

		@Override
		public void sendToNodes(String command, JID fromNode, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, fromNode, toNodes);
		}

		@Override
		public void sendToNodes(String command, Element packet, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, packet, fromNode, visitedNodes, toNodes);
		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, Element packet, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {
			command = name + "-" + command;
			controller.sendToNodes(command, data, packet, fromNode, visitedNodes, toNodes);
		}

		@Override
		public void setCommandListener(CommandListener listener) {
			String name = this.name + "-" + listener.getName();
			controller.setCommandListener(name, listener);
		}
		
	}
}


//~ Formatted in Tigase Code Convention on 13/10/15
