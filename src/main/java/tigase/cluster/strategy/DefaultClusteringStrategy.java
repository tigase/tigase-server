/*
 * DefaultClusteringStrategy.java
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



package tigase.cluster.strategy;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.CommandListener;
import tigase.cluster.api.CommandListenerAbstract;
import tigase.cluster.api.SessionManagerClusteredIfc;
import tigase.cluster.strategy.cmd.PacketForwardCmd;

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Presence;

import tigase.stats.StatisticsList;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

import static tigase.cluster.api.SessionManagerClusteredIfc.SESSION_FOUND_KEY;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import tigase.server.Priority;

/**
 * Created: May 13, 2009 9:53:44 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 *
 * @param <E>
 */
public class DefaultClusteringStrategy<E extends ConnectionRecordIfc>
				extends DefaultClusteringStrategyAbstract<E> {
	/** Field description */
	public static final String CONNECTION_ID = "connectionId";

	/** Field description */
	public static final String RESOURCE = "resource";

	/** Field description */
	public static final String SM_ID = "smId";

	/** Field description */
	public static final String USER_ID = "userId";

	/** Field description */
	public static final String  XMPP_SESSION_ID      = "xmppSessionId";
	private static final String AUTH_TIME            = "auth-time";
	private static final String INITIAL_PRESENCE_KEY = "cluster-initial-presence";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(DefaultClusteringStrategy.class
			.getName());
	private static final String PRESENCE_TYPE_INITIAL = "initial";
	private static final String PRESENCE_TYPE_KEY     = "presence-type";
	private static final String PRESENCE_TYPE_UPDATE  = "update";
	private static final String USER_CONNECTED_CMD    = "user-connected-sm-cmd";
	private static final String USER_PRESENCE_CMD     = "user-presence-sm-cmd";

	//~--- fields ---------------------------------------------------------------

	// Simple random generator, we do not need a strong randomization here.
	// Just enough to ensure better traffic distribution
	private Random rand = new Random();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public DefaultClusteringStrategy() {
		super();
		addCommandListener(new UserPresenceCommand(USER_PRESENCE_CMD));
		addCommandListener(new UserConnectedCommand(USER_CONNECTED_CMD));
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void nodeConnected(JID node) {
	}

	@Override
	public void nodeDisconnected(JID node) {
	}

	@Override
	public void handleLocalPacket(Packet packet, XMPPResourceConnection conn) {
		if (packet.getElemName() == Presence.ELEM_NAME) {
			try {
				if ((packet.getStanzaFrom() != null) &&!conn.isUserId(packet.getStanzaFrom()
						.getBareJID())) {
					return;
				}

				boolean             initPresence = conn.getSessionData(INITIAL_PRESENCE_KEY) ==
						null;
				Map<String, String> params = prepareConnectionParams(conn);

				if (initPresence) {
					conn.putSessionData(INITIAL_PRESENCE_KEY, INITIAL_PRESENCE_KEY);
					params.put(PRESENCE_TYPE_KEY, PRESENCE_TYPE_INITIAL);
				} else {
					params.put(PRESENCE_TYPE_KEY, PRESENCE_TYPE_UPDATE);
				}

				Element   presence = packet.getElement();    // conn.getPresence();
				List<JID> cl_nodes = getNodesForPacketForward(sm.getComponentId(), null, Packet
						.packetInstance(presence));

				if ((cl_nodes != null) && (cl_nodes.size() > 0)) {

					// ++clusterSyncOutTraffic;
					cluster.sendToNodes(USER_PRESENCE_CMD, params, presence, sm.getComponentId(),
							null, cl_nodes.toArray(new JID[cl_nodes.size()]));
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
			}
		}
		super.handleLocalPacket(packet, conn);
	}

	@Override
	public void handleLocalResourceBind(XMPPResourceConnection conn) {
		try {
			Map<String, String> params = prepareConnectionParams(conn);
			List<JID> cl_nodes = getNodesConnected();

			// ++clusterSyncOutTraffic;
			cluster.sendToNodes(USER_CONNECTED_CMD, params, sm.getComponentId(), cl_nodes
					.toArray(new JID[cl_nodes.size()]));
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
		}
	}	
	
	@Override
	public void handleLocalUserLogout(BareJID userId, XMPPResourceConnection conn) {
		try {
			if (!conn.isAuthorized()) {
				return;
			}

			Element presence = conn.getPresence();

			if (presence == null) {
				presence = new Element(Presence.ELEM_NAME);
				presence.setXMLNS(Presence.CLIENT_XMLNS);
			} else {
				presence = presence.clone();
			}
			presence.setAttribute("from", conn.getJID().toString());
			presence.setAttribute("type", StanzaType.unavailable.name());

			Map<String, String> params   = prepareConnectionParams(conn);
			List<JID>           cl_nodes = getNodesConnected();

			if ((cl_nodes != null) && (cl_nodes.size() > 0)) {

				// ++clusterSyncOutTraffic;
				cluster.sendToNodes(USER_PRESENCE_CMD, params, presence, sm.getComponentId(),
						null, cl_nodes.toArray(new JID[cl_nodes.size()]));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
		}
	}
	
	//~--- get methods ----------------------------------------------------------

	@Override
	public List<JID> getNodesForPacketForward(JID fromNode, Set<JID> visitedNodes,
			Packet packet) {
		if (visitedNodes != null) {
			List<JID> result = selectNodes(fromNode, visitedNodes);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Visited nodes not null: {0}, selecting new node: {1}, for packet: {2}",
						new Object[] { visitedNodes,
						result, packet });
			}

			return result;
		}

		// Presence status change set by the user have a special treatment:
		if ((packet.getElemName() == "presence") && (packet.getType() != StanzaType.error) &&
				(packet.getStanzaFrom() != null) && (packet.getStanzaTo() == null)) {
			List<JID> result = getNodesConnected();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Presence packet found: {0}, selecting all nodes: {1}",
						new Object[] { packet,
						result });
			}

			return result;
		}
		if (isSuitableForForward(packet)) {
			List<JID> result = selectNodes(fromNode, visitedNodes);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Visited nodes null, selecting new node: {0}, for packet: {1}",
						new Object[] { result,
						packet });
			}

			return result;
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet not suitable for forwarding: {0}", new Object[] {
						packet });
			}

			return null;
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * A utility method used to prepare a Map of data with user session data
	 * before it can be sent over to another cluster node. This is supposed to
	 * contain all the user's session essential information which directly
	 * identify user's resource and network connection. This information allows to
	 * detect two different user's connection made for the same resource. This may
	 * happen if both connections are established to different nodes.
	 *
	 * @param conn
	 *          is user's XMPPResourceConnection for which Map structure is
	 *          prepare.
	 *
	 * @return a Map structure with all user's connection essential data.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 */
	protected Map<String, String> prepareConnectionParams(XMPPResourceConnection conn)
					throws NotAuthorizedException, NoConnectionIdException {
		Map<String, String> params = new LinkedHashMap<String, String>();

		params.put(USER_ID, conn.getBareJID().toString());
		params.put(RESOURCE, conn.getResource());
		params.put(CONNECTION_ID, conn.getConnectionId().toString());
		params.put(XMPP_SESSION_ID, conn.getSessionId());
		params.put(AUTH_TIME, "" + conn.getAuthTime());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for conn: {0}, result: ", new Object[] { conn,
					params });
		}

		return params;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method takes the data received from other cluster node and creates a
	 * ConnectionRecord with all essential connection information. This might be
	 * used later to identify user's XMPPResourceConnection or use the clustering
	 * strategy API.
	 *
	 * @param node
	 * @param data
	 * @return
	 */
	protected ConnectionRecordIfc getConnectionRecord(JID node, Map<String, String> data) {
		BareJID             userId       = BareJID.bareJIDInstanceNS(data.get(USER_ID));
		String              resource     = data.get(RESOURCE);
		JID                 jid          = JID.jidInstanceNS(userId, resource);
		String              sessionId    = data.get(XMPP_SESSION_ID);
		JID                 connectionId = JID.jidInstanceNS(data.get(CONNECTION_ID));
		ConnectionRecordIfc rec = this
				.getConnectionRecordInstance();    // new ConnectionRecord(node, jid, sessionId, connectionId);

		rec.setRecordFields(node, jid, sessionId, connectionId);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "ConnectionRecord created: {0}", new Object[] { rec });
		}

		return rec;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * @param fromNode
	 * @param visitedNodes
	 *
	 */
	private List<JID> selectNodes(JID fromNode, Set<JID> visitedNodes) {
		List<JID> result = null;
		List<JID> cl_nodes_list = getNodesConnected();
		int       size   = cl_nodes_list.size();

		if (size == 0) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No connected cluster nodes found, returning null");
			}

			return null;
		}

		int idx = rand.nextInt(size);

		if ((visitedNodes == null) || (visitedNodes.size() == 0)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No visited nodes yet, trying random idx: " + idx);
			}
			try {
				result = Collections.singletonList(cl_nodes_list.get(idx));
			} catch (IndexOutOfBoundsException ioobe) {

				// This may happen if the node disconnected in the meantime....
				try {
					result = Collections.singletonList(cl_nodes_list.get(0));
				} catch (IndexOutOfBoundsException ioobe2) {

					// Yes, this may happen too if there were only 2 nodes before
					// disconnect....
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								"IndexOutOfBoundsException twice! Should not happen very often, returning null");
					}
				}
			}
		} else {
			for (JID jid : cl_nodes_list) {
				if (!visitedNodes.contains(jid)) {
					result = Collections.singletonList(jid);

					break;
				}
			}

			// If all nodes visited already. We have to either send it back to the
			// first node
			// or if this is the first node return null
			if ((result == null) &&!sm.getComponentId().equals(fromNode)) {
				result = Collections.singletonList(fromNode);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"All nodes visited, sending it back to the first node: " + result);
				}
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "List of result nodes: " + result);
		}

		return result;
	}

	//~--- inner classes --------------------------------------------------------

	private class UserConnectedCommand
					extends CommandListenerAbstract {
		/**
		 * Constructs ...
		 *
		 *
		 * @param name
		 */
		public UserConnectedCommand(String name) {
			super(name, Priority.CLUSTER);
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
				String> data, Queue<Element> packets)
				throws ClusterCommandException {

			// ++clusterSyncInTraffic;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode,
						visitedNodes, data, packets });
			}

			// Queue<Packet> results = new ArrayDeque<Packet>(10);
			ConnectionRecordIfc rec = getConnectionRecord(fromNode, data);

			// strategy.usersConnected(results, rec);
			// addOutPackets(results);
			// There is one more thing....
			// If the new connection is for the same resource we have here then the
			// old connection must be destroyed.
			XMPPSession session = sm.getXMPPSessions().get(rec.getUserJid().getBareJID());

			if (session != null) {
				XMPPResourceConnection conn = session.getResourceForResource(rec.getUserJid()
						.getResource());

				if (conn != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest(
								"Duplicate resource connection, logingout the older connection: " + rec);
					}
					try {
						Packet cmd = Command.CLOSE.getPacket(sm.getComponentId(), conn
								.getConnectionId(), StanzaType.set, conn.nextStanzaId());
						Element err_el = new Element("conflict");

						err_el.setXMLNS("urn:ietf:params:xml:ns:xmpp-streams");
						cmd.getElement().getChild("command").addChild(err_el);
						sm.fastAddOutPacket(cmd);
					} catch (Exception ex) {

						// TODO Auto-generated catch block
						ex.printStackTrace();
					}
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("User connected jid: " + rec.getUserJid() + ", fromNode: " + fromNode);
			}
		}
	}


	private class UserPresenceCommand
					extends CommandListenerAbstract {
		/**
		 * Constructs ...
		 *
		 *
		 * @param name
		 */
		public UserPresenceCommand(String name) {
			super(name, Priority.CLUSTER);
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
				String> data, Queue<Element> packets)
				throws ClusterCommandException {

			// ++clusterSyncInTraffic;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode,
						visitedNodes, data, packets });
			}

			ConnectionRecordIfc rec  = getConnectionRecord(fromNode, data);
			XMPPSession         session = sm.getXMPPSessions().get(rec.getUserJid()
					.getBareJID());
			Element             elem = packets.poll();

			// Notify strategy about presence update
//    strategy.presenceUpdate(elem, rec);
			// Update all user's resources with the new presence
			if (session != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "User's {0} XMPPSession found: {1}", new Object[] { rec
							.getUserJid().getBareJID(),
							session });
				}
				for (XMPPResourceConnection conn : session.getActiveResources()) {
					Element conn_presence = conn.getPresence();

					if (conn.isAuthorized() && conn.isResourceSet() && (conn_presence != null)) {
						try {

							// Send user's presence from remote connection to local connection
							Packet presence = Packet.packetInstance(elem);

							presence.setPacketTo(conn.getConnectionId());
							sm.fastAddOutPacket(presence);

							// Send user's presence from local connection to remote connection
							// but only if this was an initial presence
							if ((data != null) && PRESENCE_TYPE_INITIAL.equals(data.get(
									PRESENCE_TYPE_KEY))) {
								presence = Packet.packetInstance(conn_presence);
								presence.setPacketTo(rec.getConnectionId());
								sm.fastAddOutPacket(presence);
							}
						} catch (Exception ex) {

							// TODO Auto-generated catch block
							ex.printStackTrace();
						}
					}
				}
				sm.processPresenceUpdate(session, elem);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"No user session for presence update: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
							new Object[] { fromNode,
							visitedNodes, data, packets });
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("User presence jid: " + rec.getUserJid() + ", fromNode: " + fromNode);
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
