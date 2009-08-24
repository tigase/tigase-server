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

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
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
import tigase.stats.StatisticsList;
import tigase.util.DNSResolver;
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
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";
	public static final int SYNC_MAX_BATCH_SIZE = 1000;

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
	private static final String CL_BR_INITIAL_PRESENCE = "cl-br-init-pres";
	private static final String CL_BR_USER_CONNECTED = "cl-br-user_conn";
	private static final String SYNC_ONLINE_JIDS = "sync-jids";

	private Timer delayedTasks = null;
	private ClusteringStrategyIfc strategy = null;
	private String my_hostname = null;

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
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Ressource connection found: " + conn);
		}
		if (conn == null
			&& (isBrokenPacket(packet) || processAdminsOrDomains(packet)
				|| sendToNextNode(packet))) {
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
				if (conn != null || !sendToNextNode(packet, el_packet.getElemTo())) {
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
		ClusterMethods method = ClusterMethods.parseMethod(clel.getMethodName());
		switch (packet.getType()) {
			case set:
				if (clel.getMethodName() == null) {
					processPacket(clel);
				}
				switch (method) {
					case USER_INITIAL_PRESENCE: {
						String userId = clel.getMethodParam(USER_ID);
						String resource = clel.getMethodParam(RESOURCE);
						XMPPSession session = getSession(userId);
						if (session != null && session.getResourceForResource(resource) ==
								null) {
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
								// Send the presence from the new user connection to all local
								// (non-remote) user connections
								updateUserResources(res_con, results);
								for (XMPPResourceConnection xrc : session.getActiveResources()) {
									if (xrc.getConnectionStatus() != ConnectionStatus.REMOTE &&
											xrc.getPresence() != null) {
										broadcastUserPresence(xrc, packet.getElemFrom());
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
								if (session == null) {
									log.finest("Ignoring USER_INITIAL_PRESENCE for: " + userId +
											", from: " + packet.getElemFrom() +
											", there is no other session for the user on this node.");
								} else {
									if (session.getResourceForResource(resource) != null) {
										log.finest(
												"Ignoring USER_INITIAL_PRESENCE for: " + userId +
												", from: " + packet.getElemFrom() +
												", there is already a session on this node for this resource.");
									} else {
										log.finest("Ignoring USER_INITIAL_PRESENCE for: " + userId +
												", from: " + packet.getElemFrom() +
												", reason unknown, please contact devs.");
									}
								}
							}
						}
						break;
					}
					case USER_CONNECTED: {
						String userId = clel.getMethodParam(USER_ID);
						String resource = clel.getMethodParam(RESOURCE);
						strategy.usersConnected(packet.getElemFrom(),
								results, userId + "/" + resource);
						break;
					}
					case USER_DISCONNECTED: {
						String userId = clel.getMethodParam(USER_ID);
						String resource = clel.getMethodParam(RESOURCE);
						strategy.userDisconnected(userId + "/" + resource,
								packet.getElemFrom(),
								results);
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
						break;
					}
				}
				break;
			case get:
				switch (method) {
					case SYNC_ONLINE:
						// Send back all online users on this node
						Collection<XMPPResourceConnection> conns = connectionsByFrom.values();
						int counter = 0;
						StringBuilder sb = new StringBuilder(40000);
						for (XMPPResourceConnection conn : conns) {
							String jid = null;
							// Exception would be thrown for all not-authenticated yet connection
							// We don't have to worry about them, just ignore all of them
							// They should be synchronized later on using standard cluster
							// notifications.
							try {
								jid = conn.getJID();
							} catch (Exception e) {
								jid = null;
							}
							if (jid != null) {
								sb.append(',').append(jid);
								if (++counter > SYNC_MAX_BATCH_SIZE) {
									// Send a new batch...
									ClusterElement resp = clel.createMethodResponse(getComponentId(),
											StanzaType.result, null);
									resp.addMethodResult(SYNC_ONLINE_JIDS, sb.toString());
									fastAddOutPacket(new Packet(resp.getClusterElement()));
									counter = 0;
									// Not sure which is better, create a new StringBuilder instance
									// or clearing existing up...., let's clear it up for now.
									sb.delete(0, sb.length());
								}
							}
						}
						if (sb.length() > 0) {
							// Send a new batch...
							ClusterElement resp = clel.createMethodResponse(getComponentId(),
									StanzaType.result, null);
							resp.addMethodResult(SYNC_ONLINE_JIDS, sb.toString());
							fastAddOutPacket(new Packet(resp.getClusterElement()));
						}
						break;
					default:
						// Do nothing...
				}
				break;
			case result:
				switch (method) {
					case SYNC_ONLINE:
						// Notify clustering strategy about SYNC_ONLINE response
						String jids = clel.getMethodResultVal(SYNC_ONLINE_JIDS);
						if (jids != null) {
							String[] jidsa = jids.split(",");
							strategy.usersConnected(packet.getElemFrom(), results, jidsa);
						} else {
							log.warning("Sync online packet with empty jids list! Please check this out: " +
									packet.toString());
						}
						break;
					default:
						// Do nothing...
				}
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

	protected void updateUserResources(XMPPResourceConnection res_con,
					Queue<Packet> results) {
		try {
			Element pres_update = res_con.getPresence();
			for (XMPPResourceConnection conn : res_con.getActiveSessions()) {
				try {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Update presence change to: " + conn.getJID());
					}
					if (conn != res_con && conn.isResourceSet() &&
									conn.getConnectionStatus() != ConnectionStatus.REMOTE) {
						if (pres_update != null) {
							pres_update = pres_update.clone();
						} else {
							pres_update = new Element(Presence.PRESENCE_ELEMENT_NAME);
						}
						pres_update.setAttribute("from", res_con.getJID());
						pres_update.setAttribute("to", conn.getUserId());
						Packet pack_update = new Packet(pres_update);
						pack_update.setTo(conn.getConnectionId());
						results.offer(pack_update);
					} else {
						if (log.isLoggable(Level.FINER)) {
							log.finer("Skipping presence update to: " + conn.getJID());
						}
					} // end of else
				} catch (NotAuthorizedException ex) {
					log.warning("This should not happen, unless the connection has been " +
									"stopped in a concurrent thread or has not been authenticated yet: " +
									conn.getConnectionId());
				}
			} // end of for (XMPPResourceConnection conn: sessions)
		} catch (NotAuthorizedException ex) {
			log.warning("User session from another cluster node authentication problem: " +
							res_con.getConnectionId());
		}
	}

	protected boolean sendToNextNode(ClusterElement clel, String userId) {
		ClusterElement next_clel = ClusterElement.createForNextNode(clel,
			strategy.getNodesForJid(userId), getComponentId());
		if (next_clel != null) {
			fastAddOutPacket(new Packet(next_clel.getClusterElement()));
			return true;
		} else {
			String first = clel.getFirstNode();
			if (first != null && !first.equals(getComponentId())) {
				List<Element> packets = clel.getDataPackets();
				Element elem = (packets != null && packets.size() == 1 ? packets.get(0) : null);
				Packet packet = (elem != null ? new Packet(elem) : null);
				if ((packet == null) || (packet.getType() != StanzaType.result &&
								packet.getType() != StanzaType.available &&
								packet.getType() != StanzaType.unavailable &&
								packet.getType() != StanzaType.error &&
								!(packet.getElemName() == "presence" && packet.getType() == null))) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending back to the first node: " + first);
					}
					ClusterElement result = clel.nextClusterNode(first);
					result.addVisitedNode(getComponentId());
					fastAddOutPacket(new Packet(result.getClusterElement()));
				}
				return true;
			} else {
				return false;
			}
		}
	}

	protected boolean sendToNextNode(Packet packet) {
		String cluster_node = getFirstClusterNode(packet.getElemTo());
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Cluster node found: " + cluster_node);
		}
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
							"Can not clustering strategy instance for class: " +
							strategy_class, e);
		}
		my_hostname = (String) props.get(MY_DOMAIN_NAME_PROP_KEY);
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
							"Can not instantiate clustering strategy for class: " +
							strategy_class, e);
		}
		String[] local_domains = DNSResolver.getDefHostNames();
		if (params.get(GEN_VIRT_HOSTS) != null) {
			local_domains = ((String) params.get(GEN_VIRT_HOSTS)).split(",");
		}
//		defs.put(LOCAL_DOMAINS_PROP_KEY, LOCAL_DOMAINS_PROP_VAL);
		props.put(MY_DOMAIN_NAME_PROP_KEY, local_domains[0]);

		return props;
	}

	@Override
	public void nodeConnected(String node) {
		log.fine("Nodes connected: " + node);
		String jid = getName() + "@" + node;
		addTrusted(jid);
		strategy.nodeConnected(jid);
		sendAdminNotification("Cluster node '" + node + "' connected (" +
						(new Date()) + ")", "New cluster node connected: " + node, node);
		if (strategy.needsSync()) {
			requestSync(jid);
		}
	}

	@Override
	public void nodeDisconnected(String node) {
		log.fine("Nodes disconnected: " + node);
		String jid = getName() + "@" + node;
		strategy.nodeDisconnected(jid);
  	// Not sure what to do here, there might be still packets
	  // from the cluster node waiting....
	  // delTrusted(jid);
		sendAdminNotification("Cluster node '" + node + "' disconnected (" +
						(new Date()) + ")", "Cluster node disconnected: " + node, node);
	}

	private void sendAdminNotification(String msg, String subject,
					String node) {
		String message = msg;
		if (node != null) {
			message = msg + "\n";
		}
		int cnt = 0;
		message += node + " connected to " + getDefHostName();
		Packet p_msg = Packet.getMessage(getDefHostName(),
			JIDUtils.getNodeID(getName(), my_hostname), StanzaType.normal, message,
			subject, "xyz");
		sendToAdmins(p_msg);
	}

	protected String getFirstClusterNode(String userJid) {
		String cluster_node = null;
		List<String> nodes = strategy.getNodesForJid(userJid);
		if (nodes != null) {
			for (String node : nodes) {
				if (!node.equals(getComponentId())) {
					cluster_node = node;
					break;
				}
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
				List<String> cl_nodes = strategy.getAllNodes();
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

	private Map<String, String> prepareBroadcastParams(XMPPResourceConnection conn,
					boolean full_details)
					throws NotAuthorizedException	{
		String userId = conn.getUserId();
		String resource = conn.getResource();
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put(USER_ID, userId);
		params.put(RESOURCE, resource);
		if (full_details) {
			String xmpp_sessionId = conn.getSessionId();
			String connectionId = conn.getConnectionId();
			long authTime = conn.getAuthTime();
			params.put(XMPP_SESSION_ID, xmpp_sessionId);
			params.put(CONNECTION_ID, connectionId);
			params.put(AUTH_TIME, "" + authTime);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending user: " + userId +
								" session, resource: " + resource +
								", xmpp_sessionId: " + xmpp_sessionId +
								", connectionId: " + connectionId);
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending user: " + userId +
								" session, resource: " + resource);
			}

		}
		return params;
	}

	private void sendBroadcastPacket(Element elem, String node,
					Map<String, String> params, ClusterMethods methodCall) {
		ClusterElement clel = ClusterElement.createClusterMethodCall(
						getComponentId(), node, StanzaType.set,
						methodCall.toString(), params);
		if (elem != null) {
			clel.addDataPacket(elem);
		}
		Element check_session_el = clel.getClusterElement();
		fastAddOutPacket(new Packet(check_session_el));
	}

	private void broadcastUserPresence(XMPPResourceConnection conn,
					String ... cl_nodes) {
		try {
			Map<String, String> params = prepareBroadcastParams(conn, true);
			Element presence = conn.getPresence();
			for (String node : cl_nodes) {
				if (!node.equals(getComponentId())) {
					sendBroadcastPacket(presence, node, params,
									ClusterMethods.USER_INITIAL_PRESENCE);
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Problem with broadcast user initial presence message for: " +
							conn.getConnectionId() + ", " + conn.getjid(), e);
		}
	}

	private void broadcastUserPresence(XMPPResourceConnection conn,
					List<String> cl_nodes) {
		try {
			Map<String, String> params = prepareBroadcastParams(conn, true);
			Element presence = conn.getPresence();
			if (presence == null) {
				log.log(Level.WARNING, "Something wrong. Initial presence NULL!!",
								Thread.currentThread().getStackTrace());
			}
			for (String node : cl_nodes) {
				if (!node.equals(getComponentId())) {
					sendBroadcastPacket(presence, node, params,
									ClusterMethods.USER_INITIAL_PRESENCE);
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Problem with broadcast user initial presence for: " +
							conn.getConnectionId() + ", " + conn.getjid(), e);
		}
	}

	@Override
	public void handlePresenceSet(XMPPResourceConnection conn) {
		super.handlePresenceSet(conn);
		if (conn.getConnectionStatus() == ConnectionStatus.REMOTE) {
			return;
		}
		if (conn.getSessionData(CL_BR_INITIAL_PRESENCE) == null) {
			conn.putSessionData(CL_BR_INITIAL_PRESENCE, CL_BR_INITIAL_PRESENCE);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Handle presence set for" +
								" Connection ID: " + conn.getConnectionId() +
								", User ID: " + conn.getjid());
			}
			List<String> cl_nodes = strategy.getAllNodes();
			broadcastUserPresence(conn, cl_nodes);
		}
	}

	@Override
	public void handleResourceBind(XMPPResourceConnection conn) {
		super.handleResourceBind(conn);
		if (conn.getConnectionStatus() == ConnectionStatus.REMOTE) {
			return;
		}
		if (conn.getSessionData(CL_BR_USER_CONNECTED) == null) {
			conn.putSessionData(CL_BR_USER_CONNECTED, CL_BR_USER_CONNECTED);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Handle resource bind for" +
								" Connection ID: " + conn.getConnectionId() +
								", User ID: " + conn.getjid());
			}
			List<String> cl_nodes = strategy.getAllNodes();
			try {
				Map<String, String> params = prepareBroadcastParams(conn, false);
				for (String node : cl_nodes) {
					if (!node.equals(getComponentId())) {
						sendBroadcastPacket(null, node, params,
										ClusterMethods.USER_CONNECTED);
					}
				}
			} catch (Exception e) {
				log.log(Level.WARNING,
								"Problem with broadcast user connected for: " +
								conn.getConnectionId() + ", " + conn.getjid(), e);
			}
		} else {
			if (log.isLoggable(Level.WARNING)) {
				log.warning("User resourc-rebind - not implemented yet in the cluster." +
								" Connection ID: " + conn.getConnectionId() +
								", User ID: " + conn.getjid());
			}
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

	@Override
	public void setClusterController(ClusterController cl_controller) {
	}

	/**
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		strategy.getStatistics(list);
	}

//	/**
//	 *
//	 * @return
//	 */
//	@Override
//	public Set<String> getOnlineJids() {
//		return strategy.getOnlineJids();
//	}

	@Override
	public boolean hasCompleteJidsInfo() {
		return strategy.hasCompleteJidsInfo();
	}

	@Override
	public boolean containsJid(String jid) {
		return super.containsJid(jid) || strategy.containsJid(jid);
	}

	private void requestSync(String node) {
		ClusterElement clel = 
				ClusterElement.createClusterMethodCall(getComponentId(), node, StanzaType.get,
				ClusterMethods.SYNC_ONLINE.name(), null);
		fastAddOutPacket(new Packet(clel.getClusterElement()));
	}

}
