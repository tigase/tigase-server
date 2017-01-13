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
import tigase.conf.ConfigurationException;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.ComponentInfo;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.XMPPServer;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.StatisticsList;
import tigase.sys.TigaseRuntime;
import tigase.util.DNSResolverFactory;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;

import javax.script.Bindings;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class SessionManagerClusteredOld
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
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

	private enum STATUS {CONNECETED, DISCONNECTED};

	//~--- fields ---------------------------------------------------------------

	private ClusterControllerIfc  clusterController = null;
	private ComponentInfo         cmpInfo           = null;
	private JID                   my_address        = null;
	private JID                   my_hostname       = null;
	private int                   nodesNo           = 0;
	private ClusteringStrategyIfc strategy          = null;

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean containsJid(BareJID jid) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for jid: {0}", jid);
		}

		return super.containsJid(jid) || strategy.containsJid(jid);
	}

	@Override
	public synchronized void everySecond() {
		super.everySecond();
		if (strategy != null) {
			strategy.everySecond();
		}
	}

	@Override
	public synchronized void everyMinute() {
		super.everyMinute();
		if (strategy != null) {
			strategy.everyMinute();
		}
	}

	@Override
	public synchronized void everyHour() {
		super.everyHour();
		if (strategy != null) {
			strategy.everyHour();
		}
	}

	@Override
	public boolean fastAddOutPacket(Packet packet) {
		return super.fastAddOutPacket(packet);
	}

	@Override
	public void handleLocalPacket(Packet packet, XMPPResourceConnection conn) {
		if (strategy != null) {
			strategy.handleLocalPacket(packet, conn);
		}
	}

	@Override
	public void handleLogin(BareJID userId, XMPPResourceConnection conn) {
		super.handleLogin(userId, conn);
		strategy.handleLocalUserLogin(userId, conn);
	}

	@Override
	public void handleLogout(BareJID userId, XMPPResourceConnection conn) {
		// Exception here should not normally happen, but if it does, then
		// consequences might be severe, let's catch it then
		try {
			if (conn.isAuthorized() && conn.isResourceSet()) {
				strategy.handleLocalUserLogout(userId, conn);
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "This should not happen, check it out!, ", ex);
		}
		super.handleLogout(userId, conn); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handleResourceBind(XMPPResourceConnection conn) {
		super.handleResourceBind(conn);
		strategy.handleLocalResourceBind(conn);
	}
	
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CLUSTER_STRATEGY_VAR, strategy);
	}

	@Override
	public void onNodeConnected(JID jid) {
		super.onNodeConnected(jid);

		if (!getComponentId().equals(jid)) {
			strategy.nodeConnected(jid);
			
			sendAdminNotification( jid.getDomain(), STATUS.CONNECETED );
		}
	}

	@Override
	public void onNodeDisconnected(JID jid) {
		super.onNodeDisconnected(jid);

		if (!getComponentId().equals(jid)) {
			strategy.nodeDisconnected(jid);

			// Not sure what to do here, there might be still packets
			// from the cluster node waiting....
			// delTrusted(jid);
			
			sendAdminNotification(jid.toString(), STATUS.DISCONNECTED);
		}
	}

	@Override
	public int processingInThreads() {
		return Math.max(nodesNo, super.processingInThreads());
	}

	@Override
	public int processingOutThreads() {
		return Math.max(nodesNo, super.processingOutThreads());
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br>
	 * 
	 * This is a standard component method for processing packets. The method
	 * takes care of cases where the packet cannot be processed locally, in such a
	 * case it is forwarded to another node.
	 *
	 * @param packet to be processed
	 */
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Received packet: {0}", packet);
		}

		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");

		} else {
			XMPPResourceConnection conn = getXMPPResourceConnection(packet);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Ressource connection found: {0}", conn);
			}

			boolean clusterOK = strategy.processPacket(packet, conn);

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

	}

	@Override
	public void processPacket(Packet packet, XMPPResourceConnection conn) {
		super.processPacket(packet, conn);
	}

	@Override
	public void processPresenceUpdate(XMPPSession session, Element packet) {
		super.processPresenceUpdate(session, packet);
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public ComponentInfo getComponentInfo() {
		cmpInfo = super.getComponentInfo();
		cmpInfo.getComponentData().put("ClusteringStrategy", (strategy != null)
				? strategy.getClass()
				: null);

		return cmpInfo;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
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

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props          = super.getDefaults(params);
		String              strategy_class = (String) params.get(STRATEGY_CLASS_PROPERTY);

		if (strategy_class == null) {
			strategy_class = STRATEGY_CLASS_PROP_VAL;
		}
		props.put(STRATEGY_CLASS_PROP_KEY, strategy_class);
		try {
			ClusteringStrategyIfc strat_tmp = (ClusteringStrategyIfc) ModulesManagerImpl.getInstance()
					.forName(strategy_class)
					.newInstance();
			Map<String, Object> strat_defs = strat_tmp.getDefaults(params);

			if (strat_defs != null) {
				props.putAll(strat_defs);
			}
		} catch (NoClassDefFoundError e) {
			log.log(Level.SEVERE, "Can't instantiate clustering strategy for class: " + strategy_class);
			if (e.getMessage().contains("licence")) {
				final String[] msg = {
				                      "ERROR! ACS strategy was enabled with following class configuration",
				                      "--sm-cluster-strategy-class=tigase.server.cluster.strategy.OnlineUsersCachingStrategy",
				                      "but required libraries are missing!",
				                      "",
				                      "Please make sure that all tigase-acs*.jar and licence-lib.jar",
				                      "files are available in the classpath or disable ACS strategy!",
				                      "(by commenting out above line)",
				                      "",
				                      "For more information please peruse ACS documentation.",
				                      };
				TigaseRuntime.getTigaseRuntime().shutdownTigase(msg);
			} else {
				throw new NoClassDefFoundError("Can not instantiate clustering strategy for class");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not instantiate clustering strategy for class: " +
					strategy_class, e);
		}

		String[] local_domains = DNSResolverFactory.getInstance().getDefaultHosts();

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

	}


	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		if (strategy != null ) {
			strategy.getStatistics(list);
		}
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
	public XMPPResourceConnection getXMPPResourceConnection(Packet p) {
		return super.getXMPPResourceConnection(p);
	}

	@Override
	public ConcurrentHashMap<JID, XMPPResourceConnection> getXMPPResourceConnections() {
		return connectionsByFrom;
	}

	@Override
	public ConcurrentHashMap<BareJID, XMPPSession> getXMPPSessions() {
		return sessionsByNodeId;
	}

	@Override
	public boolean hasCompleteJidsInfo() {
		return strategy.hasCompleteJidsInfo();
	}

	@Override
	public boolean hasXMPPResourceConnectionForConnectionJid(JID connJid) {
		return this.connectionsByFrom.containsKey(connJid);
	}
	
	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
		super.setClusterController(cl_controller);
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

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		if (props.get(STRATEGY_CLASS_PROP_KEY) != null) {
			String strategy_class = (String) props.get(STRATEGY_CLASS_PROP_KEY);

			try {
				// we should not replace instance of ClusteringStrategyIfc if it
				// is not required as instance may contain data!!
				ClusteringStrategyIfc strategy_tmp = strategy;
				if (strategy == null || !strategy_class.equals(strategy.getClass().getCanonicalName())) {
					Class<?> cls = ModulesManagerImpl.getInstance().forName(strategy_class);
					strategy_tmp = (ClusteringStrategyIfc) cls.newInstance();
				}
				strategy_tmp.setSessionManagerHandler(this);
				strategy_tmp.setProperties(props);

				// strategy_tmp.init(getName());
				strategy = strategy_tmp;
				strategy.setSessionManagerHandler(this);
				log.log(Level.CONFIG, "Loaded SM strategy: {0}", strategy_class);

				if (clusterController != null) {
					strategy.setClusterController(clusterController);
				}
			} catch (NoClassDefFoundError e) {
				log.log(Level.SEVERE, "Can't instantiate clustering strategy for class: " + strategy_class);
				if (e.getMessage().contains("licence")) {
					final String[] msg = {"ERROR! ACS strategy was enabled with following class configuration",
					                      "--sm-cluster-strategy-class=tigase.server.cluster.strategy.OnlineUsersCachingStrategy",
					                      "but required libraries are missing!",
					                      "",
					                      "Please make sure that all tigase-acs*.jar and licence-lib.jar",
					                      "files are available in the classpath or disable ACS strategy!",
					                      "(by commenting out above line)",
					                      "",
					                      "For more information please peruse ACS documentation.",
					                      };
					TigaseRuntime.getTigaseRuntime().shutdownTigase(msg);
				} else {
					throw new NoClassDefFoundError("Can not instantiate clustering strategy for class");
				}
			} catch (Exception e) {
				if (!XMPPServer.isOSGi()) {
					log.log(Level.SEVERE, "Cannot instance clustering strategy class: " +
							strategy_class, e);
				}
				throw new ConfigurationException("Can not instantiate clustering strategy for class: " +
					strategy_class);		
			}
		}
		super.setProperties(props);
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
	 * @param conn {@link XMPPResourceConnection} to be closed
	 * @param closeOnly whether to perform additional processing before closing
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

	@Override
	protected void xmppStreamMoved(XMPPResourceConnection conn, JID oldConnId, JID newConnId) {
		try {
			strategy.handleLocalUserChangedConnId(conn.getBareJID(), conn, oldConnId, newConnId);
		} catch (Exception ex) {
			log.log(Level.WARNING, "This should not happen, check it out!, ", ex);
		}
		super.xmppStreamMoved(conn, oldConnId, newConnId);
	}

	private void sendAdminNotification(String node, STATUS stat) {
		String message = "Cluster ";
		String subject = null;

		if (node != null) {
			message += "node " + node + " ";
		}

		switch ( stat ) {
			case CONNECETED:
				message += "connected to ";
//				subject = "New cluster node connected";
				break;
			case DISCONNECTED:
				message += "disconnected from ";
//				subject = "Cluster node disconnected";
				break;

		}

		message += getDefHostName() + " (" + new Date() + ")";

		Packet p_msg = Message.getMessage(my_address, my_hostname, StanzaType.chat,
				message, subject, "cluster_status_update", newPacketId(null));

		sendToAdmins(p_msg);

	}
}

