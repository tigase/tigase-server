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

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.CommandListener;
import tigase.cluster.strategy.ClusteringStrategyIfc;
import tigase.cluster.strategy.ConnectionRecord;

import tigase.server.Command;
import tigase.server.Message;

import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;

import tigase.stats.StatisticsList;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;

/**
 * Class SessionManagerClusteredOld
 * 
 * 
 * Created: Tue Nov 22 07:07:11 2005
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManagerClustered extends SessionManager implements
		ClusteredComponentIfc {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(SessionManagerClustered.class
			.getName());

	private static final String USER_CONNECTED_CMD = "user-connected-sm-cmd";
	private static final String USER_DISCONNECTED_CMD = "user-disconnected-sm-cmd";
	private static final String USER_PRESENCE_CMD = "user-presence-sm-cmd";
	private static final String PACKET_FORWARD_CMD = "packet-forward-sm-cmd";
	private static final String REQUEST_SYNCONLINE_CMD = "req-sync-online-sm-cmd";
	private static final String RESPOND_SYNCONLINE_CMD = "resp-sync-online-sm-cmd";
	private static final String AUTH_TIME = "auth-time";

	public static final String CLUSTER_STRATEGY_VAR = "clusterStrategy";

	private static final String PRESENCE_ELEMENT_NAME = "presence";

	/** Field description */
	public static final String CONNECTION_ID = "connectionId";

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";

	/** Field description */
	public static final String RESOURCE = "resource";

	/** Field description */
	public static final String SM_ID = "smId";

	/** Field description */
	public static final String STRATEGY_CLASS_PROPERTY = "--sm-cluster-strategy-class";

	/** Field description */
	public static final String STRATEGY_CLASS_PROP_KEY = "sm-cluster-strategy-class";

	/** Field description */
	public static final String STRATEGY_CLASS_PROP_VAL =
			"tigase.cluster.strategy.SMNonCachingAllNodes";

	/** Field description */
	public static final int SYNC_MAX_BATCH_SIZE = 1000;

	/** Field description */
	public static final String USER_ID = "userId";

	/** Field description */
	public static final String XMPP_SESSION_ID = "xmppSessionId";

	private JID my_address = null;
	private JID my_hostname = null;
	private int nodesNo = 0;
	private ClusteringStrategyIfc strategy = null;
	private ClusterControllerIfc clusterController = null;
	private CommandListener userConnected = new UserConnectedCommand();
	private CommandListener userDisconnected = new UserDisconnectedCommand();
	private CommandListener userPresence = new UserPresenceCommand();
	private CommandListener packetForward = new PacketForwardCommand();
	private CommandListener respondSyncOnline = new RespondSyncOnlineCommand();
	private CommandListener requestSyncOnline = new RequestSyncOnlineCommand();

	/**
	 * The method checks whether the given JID is known to the installation,
	 * either user connected to local machine or any of the cluster nodes. False
	 * result does not mean the user is not connected. It means the method does
	 * not know anything about the JID. Some clustering strategies may not cache
	 * online users' information.
	 * 
	 * @param jid
	 *          a user's JID for whom we query information.
	 * 
	 * @return true if the user is known as online to the installation, false if
	 *         the method does not know.
	 */
	@Override
	public boolean containsJid(BareJID jid) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for jid: {0}", jid);
		}
		return super.containsJid(jid) || strategy.containsJid(jid);
	}

	/**
	 * If the installation knows about user's JID, that he is connected to the
	 * system, then this method returns all user's connection IDs. As an
	 * optimization we can forward packets to all user's connections directly from
	 * a single node.
	 * 
	 * @param jid
	 *          a user's JID for whom we query information.
	 * 
	 * @return a list of all user's connection IDs.
	 */
	@Override
	public JID[] getConnectionIdsForJid(BareJID jid) {
		JID[] ids = super.getConnectionIdsForJid(jid);

		if (ids == null) {
			ids = strategy.getConnectionIdsForJid(jid);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for jid: {0}, results: {1}", new Object[] { jid,
					Arrays.toString(ids) });
		}

		return ids;
	}

	/**
	 * Loads the component's default configuration to the configuration management
	 * subsystem.
	 * 
	 * @param params
	 *          is a Map with system-wide default settings found in
	 *          init.properties file or similar location.
	 * 
	 * @return a Map with all default component settings generated from the
	 *         default parameters in init.properties file.
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		String strategy_class = (String) params.get(STRATEGY_CLASS_PROPERTY);

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
			log.log(Level.SEVERE, "Can not instantiate clustering strategy for class: "
					+ strategy_class, e);
		}

		String[] local_domains = DNSResolver.getDefHostNames();

		if (params.get(GEN_VIRT_HOSTS) != null) {
			local_domains = ((String) params.get(GEN_VIRT_HOSTS)).split(",");
		}

		// defs.put(LOCAL_DOMAINS_PROP_KEY, LOCAL_DOMAINS_PROP_VAL);
		props.put(MY_DOMAIN_NAME_PROP_KEY, local_domains[0]);

		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String) params.get(CLUSTER_NODES)).split(",");

			nodesNo = cl_nodes.length;
		}

		return props;
	}

	// private long calcAverage(long[] timings) {
	// long res = 0;
	//
	// for (long ppt : timings) {
	// res += ppt;
	// }
	//
	// long processingTime = res / timings.length;
	// return processingTime;
	// }

	/**
	 * Method generates and returns component's statistics.
	 * 
	 * @param list
	 *          is a collection with statistics to which this component can add
	 *          own metrics.
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		strategy.getStatistics(list);

		// list.add(getName(), "Average commandTime on last " + commandTime.length
		// + " runs [ms]", calcAverage(commandTime), Level.FINE);
		// list.add(getName(), "Average clusterTime on last " + clusterTime.length
		// + " runs [ms]", calcAverage(clusterTime), Level.FINE);
		// list.add(getName(), "Average checkingTime on last " + checkingTime.length
		// + " runs [ms]", calcAverage(checkingTime), Level.FINE);
		// list.add(getName(), "Average smTime on last " + smTime.length +
		// " runs [ms]",
		// calcAverage(smTime), Level.FINE);

	}

	/**
	 * Returns active clustering strategy object.
	 * 
	 * @return active clustering strategy object.
	 */
	public ClusteringStrategyIfc getStrategy() {
		return strategy;
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CLUSTER_STRATEGY_VAR, strategy);
	}

	/**
	 * Method intercepts presence set event generated by presence status received
	 * from a user connected to this node. The presence is then broadcasted to all
	 * nodes given by the strategy.
	 * 
	 * @param conn
	 *          a user's XMPPResourceConnection on which the event occurred.
	 */
	@Override
	public void handlePresenceSet(XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for conn: {0}", new Object[] { conn });
		}
		super.handlePresenceSet(conn);

		try {
			Map<String, String> params = prepareConnectionParams(conn);
			Element presence = conn.getPresence();
			List<JID> cl_nodes =
					strategy.getNodesForPacketForward(Packet.packetInstance(presence));
			if (cl_nodes != null && cl_nodes.size() > 0) {
				clusterController.sendToNodes(USER_PRESENCE_CMD, params, presence,
						getComponentId(), null, cl_nodes.toArray(new JID[cl_nodes.size()]));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
		}
	}

	/**
	 * Method intercepts resource bind event generated for on user's connection.
	 * This event means that the account authentication process has been
	 * successfully completed and now the information about the event can be
	 * distributed to all nodes given by the strategy.
	 * 
	 * @param conn
	 *          a user's XMPPResourceConnection on which the event occurred.
	 */
	@Override
	public void handleResourceBind(XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for conn: {0}", new Object[] { conn });
		}
		super.handleResourceBind(conn);

		try {
			Map<String, String> params = prepareConnectionParams(conn);
			List<JID> cl_nodes = strategy.getNodesForUserConnect(conn.getJID());
			clusterController.sendToNodes(USER_CONNECTED_CMD, params, getComponentId(),
					cl_nodes.toArray(new JID[cl_nodes.size()]));
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
		}
	}

	/**
	 * Method checks whether the clustering strategy has a complete JIDs info.
	 * That is whether the strategy knows about all users connected to all nodes.
	 * Some strategies may choose not to share this information among nodes, hence
	 * the methods returns false. Other may synchronize this information and can
	 * provide it to further optimize cluster traffic.
	 * 
	 * 
	 * @return a true boolean value if the strategy has a complete information
	 *         about all users connected to all cluster nodes.
	 */
	@Override
	public boolean hasCompleteJidsInfo() {
		return strategy.hasCompleteJidsInfo();
	}

	/**
	 * The method is called on cluster node connection event. This is a
	 * notification to the component that a new node has connected to the system.
	 * 
	 * @param node
	 *          is a hostname of a new cluster node connected to the system.
	 */
	@Override
	public void nodeConnected(String node) {
		log.log(Level.FINE, "Nodes connected: {0}", node);

		JID jid = JID.jidInstanceNS(getName(), node, null);

		strategy.nodeConnected(jid);
		sendAdminNotification("Cluster node '" + node + "' connected (" + (new Date()) + ")",
				"New cluster node connected: " + node, node);

		if (strategy.needsSync()) {
			requestSync(jid);
		}
	}

	/**
	 * Method is called on cluster node disconnection event. This is a
	 * notification to the component that there was network connection lost to one
	 * of the cluster nodes.
	 * 
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	@Override
	public void nodeDisconnected(String node) {
		log.log(Level.FINE, "Nodes disconnected: {0}", node);

		JID jid = JID.jidInstanceNS(getName(), node, null);

		strategy.nodeDisconnected(jid);

		// Not sure what to do here, there might be still packets
		// from the cluster node waiting....
		// delTrusted(jid);
		sendAdminNotification("Cluster node '" + node + "' disconnected (" + (new Date())
				+ ")", "Cluster node disconnected: " + node, node);
	}

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
			log.log(Level.FINEST, "Called for conn: {0}, result: ",
					new Object[] { conn, params });
		}

		return params;
	}

	// private int tIdx = 0;
	// private int maxIdx = 100;
	// private long[] commandTime = new long[maxIdx];
	// private long[] clusterTime = new long[maxIdx];
	// private long[] checkingTime = new long[maxIdx];
	// private long[] smTime = new long[maxIdx];

	/**
	 * This is a standard component method for processing packets. The method
	 * takes care of cases where the packet cannot be processed locally, in such a
	 * case it is forwarded to another node.
	 * 
	 * 
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Received packet: {0}", packet);
		}
		// long startTime = System.currentTimeMillis();
		// int idx = tIdx;
		// tIdx = (tIdx + 1) % maxIdx;
		// long cmdTm = 0;
		// long clTm = 0;
		// long chTm = 0;
		// long smTm = 0;

		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");
			// cmdTm = System.currentTimeMillis() - startTime;

		} else {

			List<JID> toNodes = strategy.getNodesForPacketForward(packet);
			if (toNodes != null && toNodes.size() > 0) {
				clusterController.sendToNodes(PACKET_FORWARD_CMD, packet.getElement(),
						getComponentId(), null, toNodes.toArray(new JID[toNodes.size()]));
			}
			// clTm = System.currentTimeMillis() - startTime;

			XMPPResourceConnection conn = getXMPPResourceConnection(packet);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Ressource connection found: {0}", conn);
			}

			if ((conn == null) && (isBrokenPacket(packet) || processAdminsOrDomains(packet))) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Ignoring/dropping packet: {0}", packet);
				}
				// chTm = System.currentTimeMillis() - startTime;

			} else {

				processPacket(packet, conn);
				// smTm = System.currentTimeMillis() - startTime;

			}
		}
		// commandTime[idx] = cmdTm;
		// clusterTime[idx] = clTm;
		// checkingTime[idx] = chTm;
		// smTime[idx] = smTm;
	}

	// /**
	// * Method attempts to send the packet to the next cluster node. Returns true
	// * on successful attempt and false on failure. The true result does not mean
	// * that the packet has been delivered though. Only that it was sent. The
	// send
	// * attempt may fail if there is no more cluster nodes to send the packet or
	// if
	// * the clustering strategy logic decided that the packet does not have to be
	// * sent.
	// *
	// * @param packet
	// * to be sent to a next cluster node
	// * @param visitedNodes
	// * a list of nodes already visited by the packet.
	// * @return true if the packet was sent to next cluster node and false
	// * otherwise.
	// */
	// protected boolean sendToNextNode(Element packet, List<JID> visitedNodes) {
	// boolean result = false;
	// JID nextNode = strategy.selectNextNode(packet, visitedNodes);
	// if (nextNode != null) {
	// clusterController.sendToNodes(PACKET_FORWARD_CMD, packet, getComponentId(),
	// visitedNodes, nextNode);
	// result = true;
	// }
	// if (log.isLoggable(Level.FINEST)) {
	// log.log(Level.FINEST,
	// "Called for packet: {0}, visitedNodes: {1}, result: {2}",
	// new Object[] { packet, visitedNodes, result });
	// }
	// return result;
	// }

	/**
	 * Concurrency control method. Returns preferable number of threads set for
	 * this component.
	 * 
	 * 
	 * @return preferable number of threads set for this component.
	 */
	@Override
	public int processingInThreads() {
		return Math.max(nodesNo, super.processingInThreads());
	}

	/**
	 * Concurrency control method. Returns preferable number of threads set for
	 * this component.
	 * 
	 * 
	 * @return preferable number of threads set for this component.
	 */
	@Override
	public int processingOutThreads() {
		return Math.max(nodesNo, super.processingOutThreads());
	}

	/**
	 * Set's the configures the cluster controller object for cluster
	 * communication and API.
	 * 
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
		clusterController = cl_controller;
		clusterController.removeCommandListener(USER_CONNECTED_CMD, userConnected);
		clusterController.removeCommandListener(USER_DISCONNECTED_CMD, userDisconnected);
		clusterController.removeCommandListener(USER_PRESENCE_CMD, userPresence);
		clusterController.removeCommandListener(PACKET_FORWARD_CMD, packetForward);
		clusterController.removeCommandListener(REQUEST_SYNCONLINE_CMD, requestSyncOnline);
		clusterController.removeCommandListener(RESPOND_SYNCONLINE_CMD, respondSyncOnline);

		clusterController.setCommandListener(USER_CONNECTED_CMD, userConnected);
		clusterController.setCommandListener(USER_DISCONNECTED_CMD, userDisconnected);
		clusterController.setCommandListener(USER_PRESENCE_CMD, userPresence);
		clusterController.setCommandListener(PACKET_FORWARD_CMD, packetForward);
		clusterController.setCommandListener(REQUEST_SYNCONLINE_CMD, requestSyncOnline);
		clusterController.setCommandListener(RESPOND_SYNCONLINE_CMD, respondSyncOnline);
	}

	/**
	 * Standard component's configuration method.
	 * 
	 * 
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);

		String strategy_class = (String) props.get(STRATEGY_CLASS_PROP_KEY);

		try {
			ClusteringStrategyIfc strategy_tmp =
					(ClusteringStrategyIfc) Class.forName(strategy_class).newInstance();

			strategy_tmp.setProperties(props);

			// strategy_tmp.init(getName());
			strategy = strategy_tmp;
			strategy.setSessionManagerHandler(this);
			log.log(Level.CONFIG, "Loaded SM strategy: " + strategy_class);
			// strategy.nodeConnected(getComponentId());
			addTrusted(getComponentId());
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not clustering strategy instance for class: "
					+ strategy_class, e);
		}

		try {
			my_hostname = JID.jidInstance((String) props.get(MY_DOMAIN_NAME_PROP_KEY));
			my_address =
					JID.jidInstance(getName(), (String) props.get(MY_DOMAIN_NAME_PROP_KEY), null);
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING,
					"Creating component source address failed stringprep processing: {0}@{1}",
					new Object[] { getName(), my_hostname });
		}
	}

	/**
	 * The method intercept user's disconnect event. On user disconnect the method
	 * takes a list of cluster nodes from the strategy and sends a notification to
	 * all those nodes about the event.
	 * 
	 * @see tigase.server.xmppsession.SessionManager#closeSession(tigase.xmpp.
	 *      XMPPResourceConnection, boolean)
	 */
	@Override
	protected void closeSession(XMPPResourceConnection conn, boolean closeOnly) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for conn: {0}, closeOnly: {1}", new Object[] { conn,
					closeOnly });
		}
		// Exception here should not normally happen, but if it does, then
		// consequences might be severe, let's catch it then
		try {
			if (conn.isAuthorized() && conn.isResourceSet()) {
				Map<String, String> params = prepareConnectionParams(conn);
				List<JID> cl_nodes = strategy.getNodesForUserDisconnect(conn.getJID());
				clusterController.sendToNodes(USER_DISCONNECTED_CMD, params, getComponentId(),
						cl_nodes.toArray(new JID[cl_nodes.size()]));
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "This should not happen, check it out!, ", ex);
		}

		// Exception here should not normally happen, but if it does, then
		// consequences might be severe, let's catch it then
		try {
			super.closeSession(conn, closeOnly);
		} catch (Exception ex) {
			log.log(Level.WARNING, "This should not happen, check it out!, ", ex);
		}
	}

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
	protected ConnectionRecord getConnectionRecord(JID node, Map<String, String> data) {
		BareJID userId = BareJID.bareJIDInstanceNS(data.get(USER_ID));
		String resource = data.get(RESOURCE);
		JID jid = JID.jidInstanceNS(userId, resource);
		String sessionId = data.get(XMPP_SESSION_ID);
		JID connectionId = JID.jidInstanceNS(data.get(CONNECTION_ID));
		ConnectionRecord rec = new ConnectionRecord(node, jid, sessionId, connectionId);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "ConnectionRecord created: {0}", new Object[] { rec });
		}
		return rec;
	}

	/**
	 * Send synchronization request to a given cluster node. In a response the
	 * remote node should return a list of JIDs for online users on this node.
	 * 
	 * @param node
	 *          is a JID of the target cluster node.
	 */
	protected void requestSync(JID node) {
		clusterController.sendToNodes(REQUEST_SYNCONLINE_CMD, getComponentId(), node);
	}

	private void sendAdminNotification(String msg, String subject, String node) {
		String message = msg;

		if (node != null) {
			message = msg + "\n";
		}

		int cnt = 0;

		message += node + " connected to " + getDefHostName();

		Packet p_msg =
				Message.getMessage(my_address, my_hostname, StanzaType.normal, message, subject,
						"xyz", newPacketId(null));

		sendToAdmins(p_msg);
	}

	private class RespondSyncOnlineCommand implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			// TODO: Implement nodes synchronization
			// // Notify clustering strategy about SYNC_ONLINE response
			// String jids = clel.getMethodResultVal(SYNC_ONLINE_JIDS);
			//
			// if (jids != null) {
			// String[] jidsa = jids.split(",");
			// JID[] jid_j = new JID[jidsa.length];
			// int idx = 0;
			//
			// for (String jid : jidsa) {
			// jid_j[idx++] = JID.jidInstanceNS(jid);
			// }
			//
			// try {
			// strategy.usersConnected(packet.getStanzaFrom(), results, jid_j);
			// } catch (Exception e) {
			// log.log(Level.WARNING,
			// "Problem synchronizing cluster nodes for packet: "
			// + packet, e);
			// }
			// } else {
			// log.warning("Sync online packet with empty jids list! Please check this out: "
			// + packet.toString());
			// }

		}
	}

	private class RequestSyncOnlineCommand implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			// TODO: Implement nodes synchronization

			// // Send back all online users on this node
			// Collection<XMPPResourceConnection> conns = connectionsByFrom.values();
			// int counter = 0;
			// StringBuilder sb = new StringBuilder(40000);
			//
			// for (XMPPResourceConnection conn : conns) {
			// String jid = null;
			//
			// // Exception would be thrown for all not-authenticated yet
			// // connection
			// // We don't have to worry about them, just ignore all of them
			// // They should be synchronized later on using standard cluster
			// // notifications.
			// try {
			// jid = conn.getJID() + "#" + conn.getConnectionId();
			// } catch (Exception e) {
			// jid = null;
			// }
			//
			// if (jid != null) {
			// if (sb.length() == 0) {
			// sb.append(jid);
			// } else {
			// sb.append(',').append(jid);
			// }
			//
			// if (++counter > SYNC_MAX_BATCH_SIZE) {
			//
			// // Send a new batch...
			// ClusterElement resp =
			// clel.createMethodResponse(getComponentId().toString(),
			// StanzaType.result, null);
			//
			// resp.addMethodResult(SYNC_ONLINE_JIDS, sb.toString());
			// fastAddOutPacket(Packet.packetInstance(resp.getClusterElement()));
			// counter = 0;
			//
			// // Not sure which is better, create a new StringBuilder
			// // instance
			// // or clearing existing up...., let's clear it up for now.
			// sb.delete(0, sb.length());
			// }
			// }
			// }
			//
			// if (sb.length() > 0) {
			//
			// // Send a new batch...
			// ClusterElement resp =
			// clel.createMethodResponse(getComponentId().toString(),
			// StanzaType.result, null);
			//
			// resp.addMethodResult(SYNC_ONLINE_JIDS, sb.toString());
			// fastAddOutPacket(Packet.packetInstance(resp.getClusterElement()));
			// }
		}

	}

	private class UserPresenceCommand implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			ConnectionRecord rec = getConnectionRecord(fromNode, data);
			XMPPSession session = getSession(rec.getUserJid().getBareJID());
			Element elem = packets.poll();
			// Notify strategy about presence update
			strategy.presenceUpdate(elem, rec);
			// Update all user's resources with the new presence
			if (session != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "User's {0} XMPPSession found: {1}", new Object[] {
							rec.getUserJid().getBareJID(), session });
				}
				for (XMPPResourceConnection conn : session.getActiveResources()) {
					Element conn_presence = conn.getPresence();
					if (conn.isAuthorized() && conn.isResourceSet() && conn_presence != null) {
						try {
							// Send user's presence from remote connection to local connection
							Packet presence = Packet.packetInstance(elem);
							presence.setPacketTo(conn.getConnectionId());
							fastAddOutPacket(presence);
							// Send user's presence from local connection to remote connection
							presence = Packet.packetInstance(conn_presence);
							presence.setPacketTo(rec.getConnectionId());
							fastAddOutPacket(presence);
						} catch (Exception ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						}
					}
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(
							Level.FINEST,
							"No user session for presence update: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
							new Object[] { fromNode, visitedNodes, data, packets });
				}
			}

			if (log.isLoggable(Level.FINEST)) {
				log.finest("User presence jid: " + rec.getUserJid() + ", fromNode: " + fromNode);
			}
		}
	}

	private class UserConnectedCommand implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			Queue<Packet> results = new ArrayDeque<Packet>(10);
			ConnectionRecord rec = getConnectionRecord(fromNode, data);
			strategy.usersConnected(results, rec);
			addOutPackets(results);
			// There is one more thing....
			// If the new connection is for the same resource we have here then the
			// old
			// connection must be destroyed.
			XMPPSession session = getSession(rec.getUserJid().getBareJID());
			if (session != null) {
				XMPPResourceConnection conn =
						session.getResourceForResource(rec.getUserJid().getResource());
				if (conn != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Duplicate resource connection, logingout the older connection: "
								+ rec);
					}
					try {
						fastAddOutPacket(Command.CLOSE.getPacket(getComponentId(),
								conn.getConnectionId(), StanzaType.set, conn.nextStanzaId()));
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

	private class UserDisconnectedCommand implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			Queue<Packet> results = new ArrayDeque<Packet>(10);
			ConnectionRecord rec = getConnectionRecord(fromNode, data);
			strategy.userDisconnected(results, rec);
			addOutPackets(results);
		}

	}

	private class PacketForwardCommand implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}

			if ((packets != null) && (packets.size() > 0)) {
				for (Element elem : packets) {
					try {
						Packet el_packet = Packet.packetInstance(elem);
						XMPPResourceConnection conn = getXMPPResourceConnection(el_packet);

						if (conn != null) {
							processPacket(el_packet, conn);
						}
					} catch (TigaseStringprepException ex) {
						log.warning("Addressing problem, stringprep failed for packet: " + elem);
					}
				}
			} else {
				log.finest("Empty packets list in the forward command");
			}
		}

	}
}
