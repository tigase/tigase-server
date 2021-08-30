/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.cluster;

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.SessionManagerClusteredIfc;
import tigase.cluster.strategy.ClusteringStrategyIfc;
import tigase.cluster.strategy.ConnectionRecordIfc;
import tigase.eventbus.FillRoutedEvent;
import tigase.eventbus.RouteEvent;
import tigase.eventbus.component.stores.Subscription;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.Command;
import tigase.server.ComponentInfo;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.server.xmppsession.UserSessionEvent;
import tigase.server.xmppsession.UserSessionEventWithProcessorResultWriter;
import tigase.stats.StatisticsList;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class SessionManagerClusteredOld
 * <br>
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = "sess-man", parent = Kernel.class, active = true, exportable = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode})
@ClusterModeRequired(active = true)
public class SessionManagerClustered
		extends SessionManager
		implements ClusteredComponentIfc, SessionManagerClusteredIfc {

	public static final String CLUSTER_STRATEGY_VAR = "clusterStrategy";

	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";

	public static final String STRATEGY_CLASS_PROP_KEY = "sm-cluster-strategy-class";

	public static final String STRATEGY_CLASS_PROP_VAL = "tigase.cluster.strategy.DefaultClusteringStrategy";

	public static final String STRATEGY_CLASS_PROPERTY = "--sm-cluster-strategy-class";

	public static final int SYNC_MAX_BATCH_SIZE = 1000;

	private static final Logger log = Logger.getLogger(SessionManagerClustered.class.getName());
	private ClusterControllerIfc clusterController = null;

	;

	private ComponentInfo cmpInfo = null;
	@ConfigField(desc = "Component own internal JID")
	private JID my_address;
	@ConfigField(desc = "Server domain name")
	private JID my_hostname;
	private int nodesNo = 0;
	@Inject
	private ClusteringStrategyIfc strategy = null;
	public SessionManagerClustered() {
		String[] local_domains = DNSResolverFactory.getInstance().getDefaultHosts();

		String my_domain = local_domains[0];

		try {
			my_hostname = JID.jidInstance(my_domain);
			my_address = JID.jidInstance(getName(), my_domain, null);
		} catch (TigaseStringprepException e) {
			log.log(Level.WARNING, "Creating component source address failed stringprep processing: {0}@{1}",
					new Object[]{getName(), my_hostname});
		}
	}

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
	protected void checkSingleUserConnectionsLimit(XMPPResourceConnection conn) {
		// this call should not be forwarded, in cluster strategy should take care of that in handleLocalResourceBind()
		Integer limit = getSingleUserConnectionsLimit();
		if (limit != null) {
			try {
				List<ConnectionRecordIfc> conns;
				if (strategy.hasCompleteJidsInfo() && (conns = strategy.getConnectionRecordsByCreationTime(conn.getBareJID())) != null) {
					if (conns.size() > limit) {
						List<ConnectionRecordIfc> toDisconnect = conns.subList(0, conns.size() - limit);
						for (ConnectionRecordIfc rec : toDisconnect) {
							try {
								Packet cmd = Command.CLOSE.getPacket(getComponentId(), rec.getConnectionId(),
																	 StanzaType.set, conn.nextStanzaId());
								Element err_el = new Element("resource-constraint");

								err_el.setXMLNS("urn:ietf:params:xml:ns:xmpp-streams");
								cmd.getElement().getChild("command").addChild(err_el);
								fastAddOutPacket(cmd);
							} catch (Exception ex) {

								// TODO Auto-generated catch block
								log.log(Level.WARNING, "Error executing cluster command", ex);
							}
						}
					}
				} else {
					// if strategy is not aware of all user connections, apply restrictions on single node..
					super.checkSingleUserConnectionsLimit(conn);
				}
			} catch (NotAuthorizedException ex) {
				// if connection is not authorized, we can skip it
				log.log(Level.INFO, "Exception during closing old connection, ignoring.", ex);
			}
		}
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

			sendAdminNotification(jid.getDomain(), STATUS.CONNECTED);
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

			sendAdminNotification(jid.getDomain(), STATUS.DISCONNECTED);
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
	 * <br>
	 * <br>
	 * <br>
	 * This is a standard component method for processing packets. The method takes care of cases where the packet
	 * cannot be processed locally, in such a case it is forwarded to another node.
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
			if (messageArchive != null) {
				messageArchive.generateStableId(packet);
			}

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

	@Override
	public ComponentInfo getComponentInfo() {
		cmpInfo = super.getComponentInfo();
		cmpInfo.getComponentData().put("ClusteringStrategy", (strategy != null) ? strategy.getClass() : null);

		return cmpInfo;
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * If the installation knows about user's JID, that he is connected to the system, then this method returns all
	 * user's connection IDs. As an optimization we can forward packets to all user's connections directly from a single
	 * node.
	 *
	 * @param jid a user's JID for whom we query information.
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
			log.log(Level.FINEST, "Called for jid: {0}, results: {1}", new Object[]{jid, Arrays.toString(ids)});
		}

		return ids;
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
		if (strategy != null) {
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

	@FillRoutedEvent
	protected boolean fillRoutedUserSessionWithProcessorResultWriter(UserSessionEventWithProcessorResultWriter event) {
		event.setPacketWriter(this::addOutPackets);
		return true;
	}

	@FillRoutedEvent
	protected boolean fillRoutedUserSessionEvent(UserSessionEvent event) {
		XMPPSession session = getSession(event.getUserJid().getBareJID());
		if (session != null && (event.getUserJid().getResource() == null ||
				session.getResourceForResource(event.getUserJid().getResource()) != null)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "for event {0} setting session to {1}", new Object[]{event, session});
			}
			event.setSession(session);
			return true;
		} else {
			return false;
		}
	}

	@RouteEvent
	protected Collection<Subscription> routeUserSessionEvent(UserSessionEvent event,
															 Collection<Subscription> subscriptions) {
		if (strategy.hasCompleteJidsInfo()) {
			Set<ConnectionRecordIfc> records = strategy.getConnectionRecords(event.getUserJid().getBareJID());
			if (records == null) {
				records = Collections.emptySet();
			}
			if (event.getUserJid().getResource() != null) {
				Iterator<ConnectionRecordIfc> it = records.iterator();
				while (it.hasNext()) {
					if (!it.next().getUserJid().equals(event.getUserJid())) {
						it.remove();
					}
				}
			}
			Iterator<Subscription> it = subscriptions.iterator();
			while (it.hasNext()) {
				Subscription s = it.next();
				if (!s.isInClusterSubscription()) {
					continue;
				}

				boolean remove = true;

				for (ConnectionRecordIfc rec : records) {
					if (rec.getNode().getDomain().equals(s.getJid().getDomain())) {
						remove = false;
					}
				}

				if (remove) {
					it.remove();
				}
			}
		}
		return subscriptions;
	}

	/**
	 * The method intercept user's disconnect event. On user disconnect the method takes a list of cluster nodes from
	 * the strategy and sends a notification to all those nodes about the event.
	 *
	 * @param conn {@link XMPPResourceConnection} to be closed
	 * @param closeOnly whether to perform additional processing before closing
	 *
	 * @see SessionManager#closeSession
	 */
	@Override
	protected void closeSession(XMPPResourceConnection conn, boolean closeOnly) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for conn: {0}, closeOnly: {1}", new Object[]{conn, closeOnly});
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
		String message = String.format("Cluster node %1$s %2$s: %3$s (%4$s)", String.valueOf(node), stat.message,
									   String.valueOf(getDefHostName().getDomain()), LocalDateTime.now().toString());

		final JID from = vHostManager != null ? JID.jidInstance(vHostManager.getDefVHostItem()) : this.my_address;
		Packet p_msg = Message.getMessage(from, my_hostname, StanzaType.chat, message, null,
										  "cluster_status_update", newPacketId(null));
		sendToAdmins(p_msg);

	}

	private enum STATUS {
		CONNECTED("connected to"),
		DISCONNECTED("disconnected from");

		String message;

		STATUS(String message) {
			this.message = message;
		}
	}
}

