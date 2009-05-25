/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.cluster;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;

//import tigase.cluster.methodcalls.SessionTransferMC;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.ConnectionStatus;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.XMPPSession;
import tigase.annotations.TODO;
import tigase.xmpp.impl.Presence;


/**
 * Class SessionManagerClusteredOld
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManagerClustered extends SessionManager
	implements ClusteredComponent {

	public static final String STRATEGY_CLASS_PROPERTY = "--sm-cluster-strategy-class";
	public static final String STRATEGY_CLASS_PROP_KEY = "cluster-strategy-class";
	public static final String STRATEGY_CLASS_PROP_VAL =
					"tigase.cluster.strategy.SMNonCachingAllNodes";

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger(SessionManagerClustered.class.getName());

	private static final String USER_ID = "userId";
	private static final String SM_ID = "smId";
	private static final String CREATION_TIME = "creationTime";
	private static final String ERROR_CODE = "errorCode";

	private static final String XMPP_SESSION_ID = "xmppSessionId";
	private static final String RESOURCE = "resource";
	private static final String CONNECTION_ID = "connectionId";
	private static final String PRIORITY = "priority";
	private static final String TOKEN = "token";
	private static final String TRANSFER = "transfer";
	private static final String AUTH_TIME = "auth-time";
	private static final String CLUSTER_BROADCAST = "cluster-broadcast";

	private Timer delayedTasks = null;
	private ClusteringStrategyIfc strategy = null;

	@SuppressWarnings("unchecked")
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Received packet: " + packet.toString());
		}
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME &&
						packet.getElement().getXMLNS() == ClusterElement.XMLNS) {
			if (isTrusted(packet.getElemFrom())) {
				processClusterPacket(packet);
			} else {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "Cluster packet from untrusted source: " +
									packet.toString());
				}
			}
			return;
		}

		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");
			// No more processing is needed for command packet
			return;
		} // end of if (pc.isCommand())
		XMPPResourceConnection conn = getXMPPResourceConnection(packet);
		if (conn == null
			&& (isBrokenPacket(packet) || processAdminsOrDomains(packet)
				|| sentToNextNode(packet))) {
			return;
		}
		processPacket(packet, conn);
	}

	@SuppressWarnings("unchecked")
	protected void processPacket(ClusterElement packet) {
		List<Element> elems = packet.getDataPackets();
		//String packet_from = packet.getDataPacketFrom();
		if (elems != null && elems.size() > 0) {
			for (Element elem: elems) {
				Packet el_packet = new Packet(elem);
				XMPPResourceConnection conn = getXMPPResourceConnection(el_packet);
				if (conn != null || !sentToNextNode(packet)) {
					processPacket(el_packet, conn);
				}
			}
		} else {
			log.finest("Empty packets list in the cluster packet: "
				+ packet.toString());
		}
	}

	@TODO(note="Possible performance bottleneck if there are many users with multiple connections to different nodes.")
	protected void processClusterPacket(Packet packet) {
		Queue<Packet> results = new LinkedList<Packet>();
		final ClusterElement clel = new ClusterElement(packet.getElement());
		switch (packet.getType()) {
		case set:
			if (clel.getMethodName() == null) {
				processPacket(clel);
			}
			if (ClusterMethods.USER_CONNECTED.name().equals(clel.getMethodName())) {
				String userId = clel.getMethodParam(USER_ID);
				String resource = clel.getMethodParam(RESOURCE);
				XMPPSession session = getSession(userId);
				if (session != null && session.getResourceForResource(resource) == null) {
					String connectionId = clel.getMethodParam(CONNECTION_ID);
					String xmpp_sessionId = clel.getMethodParam(XMPP_SESSION_ID);
					String domain = JIDUtils.getNodeHost(userId);
					XMPPResourceConnection res_con =
									loginUserSession(connectionId, domain, userId, resource,
									ConnectionStatus.REMOTE, xmpp_sessionId);
					if (res_con != null) {
						List<Element> packs = clel.getDataPackets();
						for (Element elem : packs) {
							if (elem.getName() == Presence.PRESENCE_ELEMENT_NAME) {
								res_con.setPresence(elem);
							}
						}
						res_con.putSessionData(SM_ID, packet.getElemFrom());
						updateUserResources(res_con, results);
						for (XMPPResourceConnection xrc : session.getActiveResources()) {
							if (xrc.getConnectionStatus() != ConnectionStatus.REMOTE) {
								broadcastUserConnected(xrc, packet.getElemFrom());
							}
						}
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Added remote session for: " + userId +
											", from: " + packet.getElemFrom());
						}
					} else {
						if (log.isLoggable(Level.INFO)) {
							log.info("Couldn't create user session for: " + userId +
											", resource: " + resource +
											", connectionId: " + connectionId);
						}
					}
				} else {
					// Ignoring this, nothing special to do.
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Ignoring USER_CONNECTED for: " + userId +
										", from: " + packet.getElemFrom());
					}
				}
				strategy.userConnected(userId + "/" + resource, packet.getElemFrom(),
								results);
			}
			if (ClusterMethods.USER_DISCONNECTED.name().equals(clel.getMethodName())) {
				String userId = clel.getMethodParam(USER_ID);
				String resource = clel.getMethodParam(RESOURCE);
				XMPPSession session = getSession(userId);
				if (session != null) {
					String connectionId = clel.getMethodParam(CONNECTION_ID);
					// Possible performance bottleneck if there are many users with
					// multiple connections to different nodes. If all disconnect at
					// the same time we might have a problem here.
					closeConnection(connectionId, true);
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Removed remote session for: " + userId +
										", from: " + packet.getElemFrom());
					}
				}
				strategy.userDisconnected(userId + "/" + resource, packet.getElemFrom(),
								results);
			}
			break;
		case result:
			break;
		case error:
			String from = packet.getElemFrom();
			clel.addVisitedNode(from);
			processPacket(clel);
			break;
		default:
			break;
		}
		addOutPackets(results);
	}

	protected void updateUserResources(XMPPResourceConnection session,
					Queue<Packet> results) {
		try {
			Element pres_update = session.getPresence();
			for (XMPPResourceConnection conn : session.getActiveSessions()) {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Update presence change to: " + conn.getJID());
				}
				if (conn != session && conn.isResourceSet() &&
								conn.getConnectionStatus() != ConnectionStatus.REMOTE) {
					if (pres_update != null) {
						pres_update = pres_update.clone();
					} else {
						pres_update = new Element(Presence.PRESENCE_ELEMENT_NAME);
					}
					pres_update.setAttribute("from", session.getJID());
					pres_update.setAttribute("to", conn.getUserId());
					Packet pack_update = new Packet(pres_update);
					pack_update.setTo(conn.getConnectionId());
					results.offer(pack_update);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Skipping presence update to: " + conn.getJID());
					}
				} // end of else
			} // end of for (XMPPResourceConnection conn: sessions)
		} catch (NotAuthorizedException ex) {
			log.warning("This should not happen, unless the connection has been stopped in a concurrent thread: " +
							session.getConnectionId());
		}
	}

	protected boolean sentToNextNode(ClusterElement clel) {
		String userId = clel.getMethodParam(USER_ID);
		ClusterElement next_clel = ClusterElement.createForNextNode(clel,
			strategy.getNodesForJid(userId), getComponentId());
		if (next_clel != null) {
			fastAddOutPacket(new Packet(next_clel.getClusterElement()));
			return true;
		} else {
			return false;
		}
	}

	protected boolean sentToNextNode(Packet packet) {
		String userId = JIDUtils.getNodeID(packet.getElemTo());
		String cluster_node = getFirstClusterNode(userId);
		if (cluster_node != null) {
			String sess_man_id = getComponentId();
			ClusterElement clel = new ClusterElement(sess_man_id, cluster_node,
							StanzaType.set, packet);
			clel.addVisitedNode(sess_man_id);
			fastAddOutPacket(new Packet(clel.getClusterElement()));
			return true;
		}
		return false;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		String strategy_class = (String) props.get(STRATEGY_CLASS_PROP_KEY);
		try {
			ClusteringStrategyIfc strategy_tmp =
							(ClusteringStrategyIfc) Class.forName(strategy_class).newInstance();
			strategy_tmp.setProperties(props);
			//strategy_tmp.init(getName());
			strategy = strategy_tmp;
		} catch (Exception e) {
			log.log(Level.SEVERE,
							"Can not create VHost repository instance for class: " +
							strategy_class, e);
		}
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		String strategy_class = (String)params.get(STRATEGY_CLASS_PROPERTY);
		if (strategy_class == null) {
			strategy_class = STRATEGY_CLASS_PROP_VAL;
		}
		props.put(STRATEGY_CLASS_PROP_KEY, strategy_class);
		try {
			ClusteringStrategyIfc strat_tmp =
							(ClusteringStrategyIfc) Class.forName(strategy_class).newInstance();
			Map<String, Object> strat_defs = strat_tmp.getDefaults(params);
			if (strat_defs != null) {
				props.putAll(strat_defs);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE,
							"Can not instantiate VHosts repository for class: " +
							strategy_class, e);
		}
		return props;
	}

	@Override
	public void nodesConnected(Set<String> node_hostnames) {
		log.fine("Nodes connected: " + node_hostnames.toString());
		for (String host : node_hostnames) {
			String jid = getName() + "@" + host;
			addTrusted(jid);
			strategy.nodeConnected(jid);
		}
	}

	@Override
	public void nodesDisconnected(Set<String> node_hostnames) {
		log.fine("Nodes disconnected: " + node_hostnames.toString());
		for (String host : node_hostnames) {
			String jid = getName() + "@" + host;
			strategy.nodeDisconnected(jid);
			// Not sure what to do here, there might be still packets
			// from the cluster node waiting....
			//delTrusted(jid);
		}
	}

	protected String getFirstClusterNode(String userId) {
		String cluster_node = null;
		String[] nodes = strategy.getNodesForJid(userId);
		for (String node: nodes) {
			if (!node.equals(getComponentId())) {
				cluster_node = node;
				break;
			}
		}
		return cluster_node;
	}

	@Override
	protected void closeSession(XMPPResourceConnection conn, boolean closeOnly) {
		if (conn.getConnectionStatus() != ConnectionStatus.REMOTE &&
						conn.isAuthorized()) {
			try {
				String connectionId = conn.getConnectionId();
				String userId = conn.getUserId();
				String resource = conn.getResource();
				Map<String, String> params =
								new LinkedHashMap<String, String>();
				params.put(CONNECTION_ID, connectionId);
				params.put(USER_ID, userId);
				params.put(RESOURCE, resource);
				String[] cl_nodes = strategy.getAllNodes();
				for (String node : cl_nodes) {
					if (!node.equals(getComponentId())) {
						Element check_session_el =
										ClusterElement.createClusterMethodCall(getComponentId(),
										node, StanzaType.set,
										ClusterMethods.USER_DISCONNECTED.toString(), params).
										getClusterElement();
						fastAddOutPacket(new Packet(check_session_el));
					}
				}
			} catch (Exception ex) {
				log.log(Level.WARNING,
								"Problem sending user disconnect broadcast for: " + 
								conn.getConnectionId(), ex);
			}
		}
		XMPPSession parentSession = conn.getParentSession();
		super.closeSession(conn, closeOnly);
		if (conn.getConnectionStatus() != ConnectionStatus.REMOTE &&
						parentSession != null &&
						parentSession.getActiveResourcesSize() ==
						parentSession.getResSizeForConnStatus(ConnectionStatus.REMOTE)) {
			List<XMPPResourceConnection> conns = parentSession.getActiveResources();
			for (XMPPResourceConnection xrc : conns) {
				String connId = xrc.getConnectionId();
				super.closeConnection(xrc.getConnectionId(), true);
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Closed remote connection: " + connId);
				}
			}
		}
	}

	private void broadcastUserConnected(XMPPResourceConnection conn,
					String ... cl_nodes) {
		try {
			String userId = conn.getUserId();
			String xmpp_sessionId = conn.getSessionId();
			String connectionId = conn.getConnectionId();
			String resource = conn.getResource();
			long authTime = conn.getAuthTime();
			Map<String, String> params = new LinkedHashMap<String, String>();
			params.put(USER_ID, userId);
			params.put(XMPP_SESSION_ID, xmpp_sessionId);
			params.put(RESOURCE, resource);
			params.put(CONNECTION_ID, connectionId);
			params.put(AUTH_TIME, "" + authTime);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending user: " + userId +
								" session, resource: " + resource +
								", xmpp_sessionId: " + xmpp_sessionId +
								", connectionId: " + connectionId);
			}
			Element presence = conn.getPresence();
			for (String node : cl_nodes) {
				if (!node.equals(getComponentId())) {
					ClusterElement clel = ClusterElement.createClusterMethodCall(
									getComponentId(), node, StanzaType.set,
									ClusterMethods.USER_CONNECTED.toString(), params);
					if (presence != null) {
						clel.addDataPacket(presence);
					}
					Element check_session_el = clel.getClusterElement();
					fastAddOutPacket(new Packet(check_session_el));
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Problem with broadcast user connected message for: " +
							conn.getConnectionId(), e);
		}
	}

	@Override
	public void handlePresenceSet(XMPPResourceConnection conn) {
		super.handlePresenceSet(conn);
		if (conn.getConnectionStatus() == ConnectionStatus.REMOTE) {
			return;
		}
		if (conn.getSessionData(CLUSTER_BROADCAST) == null) {
			String[] cl_nodes = strategy.getAllNodes();
			broadcastUserConnected(conn, cl_nodes);
			conn.putSessionData(CLUSTER_BROADCAST, CLUSTER_BROADCAST);
		}
	}

	@Override
	public void release() {
		delayedTasks.cancel();
		super.release();
	}

	@Override
	public void start() {
		super.start();
		delayedTasks = new Timer("SM Cluster Delayed Tasks", true);
	}

}
