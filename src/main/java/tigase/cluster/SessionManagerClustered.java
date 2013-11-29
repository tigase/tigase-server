/*
 * SessionManagerClustered.java
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

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.SessionManagerClusteredIfc;
import tigase.cluster.strategy.ClusteringStrategyIfc;

import tigase.server.ComponentInfo;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;

import tigase.stats.StatisticsList;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

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
public class SessionManagerClustered
				extends SessionManager
				implements ClusteredComponentIfc, SessionManagerClusteredIfc {
	/** Field description */
	public static final String CLUSTER_STRATEGY_VAR = "clusterStrategy";

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";

	/** Field description */
	public static final String STRATEGY_CLASS_PROP_KEY = "sm-cluster-strategy-class";

	/** Field description */
	public static final String STRATEGY_CLASS_PROP_VAL =
			"tigase.cluster.strategy.DefaultClusteringStrategy";

	/** Field description */
	public static final String STRATEGY_CLASS_PROPERTY = "--sm-cluster-strategy-class";

	/** Field description */
	public static final int SYNC_MAX_BATCH_SIZE = 1000;

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(SessionManagerClustered.class
			.getName());

	//~--- fields ---------------------------------------------------------------

	private ClusterControllerIfc  clusterController = null;
	private ComponentInfo         cmpInfo           = null;
	private JID                   my_address        = null;
	private JID                   my_hostname       = null;
	private int                   nodesNo           = 0;
	private ClusteringStrategyIfc strategy          = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * The method checks whether the given JID is known to the installation,
	 * either user connected to local machine or any of the cluster nodes. False
	 * result does not mean the user is not connected. It means the method does
	 * not know anything about the JID. Some clustering strategies may not cache
	 * online users information.
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
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean fastAddOutPacket(Packet packet) {
		return super.fastAddOutPacket(packet);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param conn
	 */
	@Override
	public void handleLocalPacket(Packet packet, XMPPResourceConnection conn) {
		if (strategy != null) {
			strategy.handleLocalPacket(packet, conn);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param conn
	 */
	@Override
	public void handleLogin(BareJID userId, XMPPResourceConnection conn) {
		super.handleLogin(userId, conn);
		strategy.handleLocalUserLogin(userId, conn);
	}

	/**
	 * Initialize a mapping of key/value pairs which can be used in scripts
	 * loaded by the server
	 *
	 * @param binds A mapping of key/value pairs, all of whose keys are Strings.
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CLUSTER_STRATEGY_VAR, strategy);
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
		sendAdminNotification("Cluster node '" + node + "' disconnected (" + (new Date()) +
				")", "Cluster node disconnected: " + node, node);
	}

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
			XMPPResourceConnection conn = getXMPPResourceConnection(packet);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Ressource connection found: {0}", conn);
			}

			boolean clusterOK = strategy.processPacket(packet, conn);

			// clTm = System.currentTimeMillis() - startTime;
			if (conn == null) {
				if (isBrokenPacket(packet) || processAdminsOrDomains(packet)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Ignoring/dropping packet: {0}", packet);
					}
				} else {

					// Process is as packet to offline user only if there are no other
					// nodes for the packet to be processed.
					if (!clusterOK) {

						// Process packet for offline user
						processPacket(packet, (XMPPResourceConnection) null);
					}
				}
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

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param conn
	 */
	@Override
	public void processPacket(Packet packet, XMPPResourceConnection conn) {
		super.processPacket(packet, conn);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session is a <code>XMPPSession</code>
	 * @param packet is a <code>Element</code>
	 */
	@Override
	public void processPresenceUpdate(XMPPSession session, Element packet) {
		super.processPresenceUpdate(session, packet);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Allows to obtain various informations about components
	 *
	 * @return information about particular component
	 */
	@Override
	public ComponentInfo getComponentInfo() {
		cmpInfo = super.getComponentInfo();
		cmpInfo.getComponentData().put("ClusteringStrategy", (strategy != null)
				? strategy.getClass()
				: null);

		return cmpInfo;
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
		Map<String, Object> props          = super.getDefaults(params);
		String              strategy_class = (String) params.get(STRATEGY_CLASS_PROPERTY);

		if (strategy_class == null) {
			strategy_class = STRATEGY_CLASS_PROP_VAL;
		}
		props.put(STRATEGY_CLASS_PROP_KEY, strategy_class);
		try {
			ClusteringStrategyIfc strat_tmp = (ClusteringStrategyIfc) Class.forName(
					strategy_class).newInstance();
			Map<String, Object> strat_defs = strat_tmp.getDefaults(params);

			if (strat_defs != null) {
				props.putAll(strat_defs);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not instantiate clustering strategy for class: " +
					strategy_class, e);
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

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoDescription() {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "disco description from SM Clustered");
		}

		String result;

		result = super.getDiscoDescription();
		result += " clustered, ";
		result += strategy;

		return result;

//  return super.getDiscoDescription() + " clustered, " + strategy;
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

	/**
	 * Method description
	 *
	 *
	 *
	 * @param p
	 *
	 *
	 *
	 * @return a value of <code>XMPPResourceConnection</code>
	 */
	@Override
	public XMPPResourceConnection getXMPPResourceConnection(Packet p) {
		return super.getXMPPResourceConnection(p);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>ConcurrentHashMap<JID,XMPPResourceConnection></code>
	 */
	@Override
	public ConcurrentHashMap<JID, XMPPResourceConnection> getXMPPResourceConnections() {
		return connectionsByFrom;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>ConcurrentHashMap<BareJID,XMPPSession></code>
	 */
	@Override
	public ConcurrentHashMap<BareJID, XMPPSession> getXMPPSessions() {
		return sessionsByNodeId;
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

	//~--- set methods ----------------------------------------------------------

	/**
	 * Set's the configures the cluster controller object for cluster
	 * communication and API.
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
		clusterController = cl_controller;
		if (strategy != null) {
			strategy.setClusterController(clusterController);
		}

//  clusterController.removeCommandListener(USER_CONNECTED_CMD, userConnected);
//  clusterController.removeCommandListener(USER_DISCONNECTED_CMD, userDisconnected);
//  clusterController.removeCommandListener(USER_PRESENCE_CMD, userPresence);
//  clusterController.removeCommandListener(REQUEST_SYNCONLINE_CMD, requestSyncOnline);
//  clusterController.removeCommandListener(RESPOND_SYNCONLINE_CMD, respondSyncOnline);
//  clusterController.setCommandListener(USER_CONNECTED_CMD, userConnected);
//  clusterController.setCommandListener(USER_DISCONNECTED_CMD, userDisconnected);
//  clusterController.setCommandListener(USER_PRESENCE_CMD, userPresence);
//  clusterController.setCommandListener(REQUEST_SYNCONLINE_CMD, requestSyncOnline);
//  clusterController.setCommandListener(RESPOND_SYNCONLINE_CMD, respondSyncOnline);
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
		if (props.get(STRATEGY_CLASS_PROP_KEY) != null) {
			String strategy_class = (String) props.get(STRATEGY_CLASS_PROP_KEY);

			try {
				ClusteringStrategyIfc strategy_tmp = (ClusteringStrategyIfc) Class.forName(
						strategy_class).newInstance();

				strategy_tmp.setSessionManagerHandler(this);
				strategy_tmp.setProperties(props);

				// strategy_tmp.init(getName());
				strategy = strategy_tmp;
				strategy.setSessionManagerHandler(this);
				log.log(Level.CONFIG, "Loaded SM strategy: {0}", strategy_class);

				// strategy.nodeConnected(getComponentId());
				addTrusted(getComponentId());
				if (clusterController != null) {
					strategy.setClusterController(clusterController);
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "Cannot instance clustering strategy class: " +
						strategy_class, e);
			}
		}
		updateServiceEntity();
		try {
			if (props.get(MY_DOMAIN_NAME_PROP_KEY) != null) {
				my_hostname = JID.jidInstance((String) props.get(MY_DOMAIN_NAME_PROP_KEY));
				my_address = JID.jidInstance(getName(), (String) props.get(
						MY_DOMAIN_NAME_PROP_KEY), null);
			}
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING,
					"Creating component source address failed stringprep processing: {0}@{1}",
					new Object[] { getName(),
					my_hostname });
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * The method intercept user's disconnect event. On user disconnect the method
	 * takes a list of cluster nodes from the strategy and sends a notification to
	 * all those nodes about the event.
	 *
	 * @see SessionManager#closeSession
	 *
	 * @param conn
	 * @param closeOnly
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
				BareJID userId = conn.getBareJID();

				strategy.handleLocalUserLogout(userId, conn);
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

	private void sendAdminNotification(String msg, String subject, String node) {
		String message = msg;

		if (node != null) {
			message = msg + "\n";
		}

		int cnt = 0;

		message += node + " connected to " + getDefHostName();

		Packet p_msg = Message.getMessage(my_address, my_hostname, StanzaType.normal,
				message, subject, "xyz", newPacketId(null));

		sendToAdmins(p_msg);
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
