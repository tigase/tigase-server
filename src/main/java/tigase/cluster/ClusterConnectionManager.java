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

import tigase.cluster.api.*;
import tigase.cluster.repo.ClusterRepoItem;
import tigase.cluster.repo.ClusterRepoItemEvent;
import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.TigaseDBException;
import tigase.db.beans.DataSourceBean;
import tigase.db.comp.AbstractSDComponentRepositoryBean;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.ComponentRepositoryDataSourceAware;
import tigase.db.comp.RepositoryChangeListenerIfc;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.net.ConnectionType;
import tigase.net.SocketType;
import tigase.server.ConnectionManager;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ServiceChecker;
import tigase.stats.MaxDailyCounterQueue;
import tigase.stats.StatisticsList;
import tigase.sys.TigaseRuntime;
import tigase.util.Algorithms;
import tigase.util.common.TimerTask;
import tigase.util.datetime.TimeUtils;
import tigase.util.reflection.ReflectionHelper;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;

/**
 * Class ClusterConnectionManager
 * <br>
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Bean(name = "cl-comp", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
@ClusterModeRequired(active = true)
public class ClusterConnectionManager
		extends ConnectionManager<XMPPIOService<Object>>
		implements ClusteredComponentIfc, RepositoryChangeListenerIfc<ClusterRepoItem>, ClusterConnectionHandler {

	public static final int SOCKET_BUFFER_CL_PROP_VAL = 128 * 1024;

	public static final String CLUSTER_CONNECTIONS_PER_NODE_PROP_KEY = "cluster-connections-per-node";

	public static final int CLUSTER_CONNECTIONS_PER_NODE_VAL = 5;

	public static final String CLUSTER_CONTR_ID_PROP_KEY = "cluster-controller-id";

	public static final String COMPRESS_STREAM_PROP_KEY = "compress-stream";

	public static final String CONNECT_ALL_PAR = "--cluster-connect-all";

	public static final String CONNECT_ALL_PROP_KEY = "connect-all";

	public static final String NON_CLUSTER_TRAFFIC_ALLOWED_PROP_KEY = "non-cluster-traffic-allowed";
	public static final boolean NON_CLUSTER_TRAFFIC_ALLOWED_PROP_VAL = true;
	public static final String IDENTITY_TYPE_KEY = "identity-type";
	public static final String IDENTITY_TYPE_VAL = "generic";
	public static final String PORT_ROUTING_TABLE_PROP_KEY = "routing-table";
	public static final String RETURN_SERVICE_DISCO_KEY = "service-disco";
	public static final String SECRET_PROP_KEY = "secret";
	public static final String XMLNS = "tigase:cluster";
	public static final boolean RETURN_SERVICE_DISCO_VAL = true;
	public static final boolean CONNECT_ALL_PROP_VAL = false;
	public static final boolean COMPRESS_STREAM_PROP_VAL = false;
	public final static String EVENTBUS_REPOSITORY_NOTIFICATIONS_ENABLED_KEY = "eventbus-repository-notifications";
	public final static boolean EVENTBUS_REPOSITORY_NOTIFICATIONS_ENABLED_VALUE = false;
	private static final Logger log = Logger.getLogger(ClusterConnectionManager.class.getName());

	public static enum REPO_ITEM_UPDATE_TYPE {
		ADDED,
		UPDATED,
		REMOVED
	}
	/**
	 * Default value for the system property for configuration protection from system overload and DOS attack.
	 */
	public static int ELEMENTS_NUMBER_LIMIT_CLUSTER_PROP_VAL = 100 * 1000;

	@Inject
	private ClusterControllerIfc clusterController = null;
	private tigase.eventbus.EventListener<ClusterInitializedEvent> clusterEventHandler = null;
	@ConfigField(desc = "Compress stream", alias = COMPRESS_STREAM_PROP_KEY)
	private boolean compress_stream = COMPRESS_STREAM_PROP_VAL;
	@ConfigField(desc = "Connect to all nodes", alias = CONNECT_ALL_PROP_KEY)
	private boolean connect_all = CONNECT_ALL_PROP_VAL;
	// private long packetsSent = 0;
	// private long packetsReceived = 0;
	@Inject
	private ClusterConnectionSelectorIfc connectionSelector = null;
	private Map<String, ClusterConnection> connectionsPool = new ConcurrentSkipListMap<>();
	@Inject
	private DataSourceBean dataSourceBean = null;
	@Inject
	private EventBus eventBus = null;
	private String identity_type = IDENTITY_TYPE_VAL;
	private boolean initialClusterConnectedDone = false;
	// private String cluster_controller_id = null;
	private IOServiceStatisticsGetter ioStatsGetter = new IOServiceStatisticsGetter();
	private long[] lastDay = new long[24];
	private int lastDayIdx = 0;
	private long[] lastHour = new long[60];
	private int lastHourIdx = 0;
	private MaxDailyCounterQueue<Integer> maxNodes = new MaxDailyCounterQueue<>(31);
	private int maxNodesWithinLastWeek = 0;
	private int nodesNo = 0;
	@ConfigField(desc = "Allow non cluster traffic over cluster connection", alias = NON_CLUSTER_TRAFFIC_ALLOWED_PROP_KEY)
	private boolean nonClusterTrafficAllowed = true;
	@ConfigField(desc = "Number of connections to open per node", alias = "connections-per-node")
	private int per_node_conns = CLUSTER_CONNECTIONS_PER_NODE_VAL;
	@Inject
	private ComponentRepository<ClusterRepoItem> repo = null;
	private final TimerTask repoReloadTimerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				if (repo != null) {
					repo.reload();
				}
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Items reloading failed", ex);
			}
		}
	};
	private CommandListener sendPacket = new SendPacket(ClusterControllerIfc.DELIVER_CLUSTER_PACKET_CMD);
	private long servConnectedTimeouts = 0;
	private long totalNodeDisconnects = 0;

	public ClusterConnectionManager() {
		super(SOCKET_BUFFER_CL_PROP_VAL);
		serviceConnectedTimeout = 10;
		elements_number_limit = ELEMENTS_NUMBER_LIMIT_CLUSTER_PROP_VAL;
		if (getDefHostName().toString().equalsIgnoreCase("localhost")) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Tigase is running in Clustered Mode yet the hostname",
												 "of the machine was resolved to *localhost* which will cause",
												 "malfunctioning of Tigase in clustered environment!", "",
												 "To prevent further issues with the clustering Tigase will be shutdown.",
												 "",
												 "Please make sure that FQDN hostname of the machine is set correctly",
												 "and restart the server."});
		}

		connectionDelay = 5 * SECOND;

		watchdogPingType = WATCHDOG_PING_TYPE.XMPP;
		watchdogDelay = 30 * SECOND;
		watchdogTimeout = -1 * SECOND;
	}

	@Override
	protected boolean enableServiceConnectedTimeout(XMPPIOService<Object> service) {
		return true;
	}

	@Override
	public int hashCodeForPacket(Packet packet) {

		// If this is a cluster packet let's try to do a bit more smart hashing
		// based on the stanza from/to addresses
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME) {

			// TODO: Look for a simpler, more efficient algorithm to distribute
			// cluster packets among different threads.
			// This looks like an overkill to me, however I don't see any better way
			ClusterElement clel = new ClusterElement(packet.getElement());

			// If there is no XMPP stanzas with an address inside the cluster packet,
			// we can try Map data and User ID inside it if it exists.
			String userId = clel.getMethodParam("userId");

			if (userId != null) {
				return userId.hashCode();
			}

			Queue<Element> children = clel.getDataPackets();

			if ((children != null) && (children.size() > 0)) {
				Element child = children.peek();
				String stanzaAdd = child.getAttributeStaticStr(Packet.TO_ATT);

				if (stanzaAdd != null) {
					return stanzaAdd.hashCode();
				} else {

					// This might be user's initial presence. In such a case we take
					// stanzaFrom instead
					stanzaAdd = child.getAttributeStaticStr(Packet.FROM_ATT);
					if (stanzaAdd != null) {
						return stanzaAdd.hashCode();
					} else {

						// This may happen for some cluster packets, like:
						// resp-sync-online-sm-cmd and this is correct
						log.log(Level.FINE, "No stanzaTo or from for cluster packet: {0}", packet);
					}
				}
			}
		}

		// There is a separate connection to each cluster node, ideally we want to
		// process packets in a separate thread for each connection, so let's try
		// to get the hash code by the destination node address
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}

		return packet.getTo() != null ? packet.getTo().hashCode() : packet.toString().hashCode();
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put("clusterCM", this);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
	}

	@Override
	public void itemAdded(ClusterRepoItem repoItem) {
		log.log(Level.INFO, "Loaded repoItem: {0}", repoItem.toString());

		String host = repoItem.getHostname();

		boolean isCorrect = false;
		try {
			InetAddress addr = InetAddress.getByName(host);

			// we ignore any local addresses
			isCorrect = !addr.isAnyLocalAddress() && !addr.isLoopbackAddress() &&
					!(NetworkInterface.getByInetAddress(addr) != null);
			if (!isCorrect && log.isLoggable(Level.CONFIG)) {
				log.log(Level.CONFIG, "ClusterRepoItem of local machine, skipping connection attempt: {0}", repoItem);
			}
		} catch (UnknownHostException | SocketException ex) {
			log.log(Level.WARNING, "Incorrect ClusterRepoItem, skipping connection attempt: " + repoItem, ex);
		}

		if (isCorrect) {
			for (int i = 0; i < per_node_conns; ++i) {
				log.log(Level.CONFIG, "Trying to connect to cluster node: {0}", host);

				Map<String, Object> port_props = new LinkedHashMap<String, Object>(12);

				port_props.put(SECRET_PROP_KEY, repoItem.getPassword());
				port_props.put(PORT_LOCAL_HOST_PROP_KEY, getDefHostName());
				port_props.put(PORT_TYPE_PROP_KEY, ConnectionType.connect);
				port_props.put(PORT_SOCKET_PROP_KEY, SocketType.plain);
				port_props.put(PORT_REMOTE_HOST_PROP_KEY, host);
				port_props.put(PORT_IFC_PROP_KEY, new String[]{host});
				port_props.put(MAX_RECONNECTS_PROP_KEY, 99999999);
				port_props.put(PORT_KEY, repoItem.getPortNo());
				addWaitingTask(port_props);
			}

			sendEvent(REPO_ITEM_UPDATE_TYPE.ADDED, repoItem);
			// reconnectService(port_props, connectionDelay);
		}
	}

	@Override
	public void itemRemoved(ClusterRepoItem item) {
		// remove and close all cluster connections to item which is now gone
		ClusterConnection clusterConnection = connectionsPool.get(item.getHostname());
		if (clusterConnection != null && clusterConnection.size() > 0) {
			for (XMPPIOService service : clusterConnection.getConnections()) {
				clusterConnection.removeConn(service);
				// in most cases those connections should already be closed
				service.stop();
			}
		}
		sendEvent(REPO_ITEM_UPDATE_TYPE.REMOVED, item);
	}

	@Override
	public void itemUpdated(ClusterRepoItem item) {
		sendEvent(REPO_ITEM_UPDATE_TYPE.UPDATED, item);
	}

	@Override
	public void nodeConnected(String node) {
		super.nodeConnected(node);

		maxNodes.add(getNodesConnectedWithLocal().size());
		maxNodesWithinLastWeek = maxNodes.getMaxValueInRange(7).orElse(-1);
	}

	@Override
	public synchronized void everyHour() {
		super.everyHour();

		maxNodes.add(getNodesConnectedWithLocal().size());
		maxNodesWithinLastWeek = maxNodes.getMaxValueInRange(7).orElse(-1);
	}

	@Override
	public void nodeDisconnected(String node) {
		super.nodeDisconnected(node);

		maxNodes.add(getNodesConnectedWithLocal().size());
		maxNodesWithinLastWeek = maxNodes.getMaxValueInRange(7).orElse(-1);
	}

	@Override
	public int processingInThreads() {

		// TODO: The number of threads should be equal or greater to number of
		// cluster nodes.
		// This should work well as far as nodesNo is initialized before this
		// method is called which is true only during program startup time.
		// In case of reconfiguration or new node joining this might not be
		// the case. Low priority issue though.
		return Math.max(Runtime.getRuntime().availableProcessors(), nodesNo) * 8;
	}

	@Override
	public int processingOutThreads() {

		// TODO: The number of threads should be equal or greater to number of
		// cluster nodes.
		// This should work well as far as nodesNo is initialized before this
		// method is called which is true only during program startup time.
		// In case of reconfiguration or new node joining this might not be
		// the case. Low priority issue though.
		return Math.max(Runtime.getRuntime().availableProcessors(), nodesNo) * 8;
	}

	@Override
	public void processOutPacket(Packet packet) {
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME) {
			clusterController.handleClusterPacket(packet.getElement());
		} else {

			// This should, actually, not happen. Let's log it here
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Unexpected packet on cluster connection: {0}", packet);
			}
			super.processOutPacket(packet);
		}
	}

	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}
		if ((packet.getStanzaTo() != null) && packet.getStanzaTo().equals(getComponentId())) {
			try {
				addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, "Not implemented", true));
			} catch (PacketErrorTypeException e) {
				log.log(Level.WARNING, "Packet processing exception: {0}", e);
			}

			return;
		}
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME || packet.getElemName() == "route") {
			writePacketToSocket(packet);
		} else {

			if (nonClusterTrafficAllowed) {
				writePacketToSocket(packet.packRouted());
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Unexpected packet for the cluster connetcion: {0}", packet);
				}
			}

		}
	}

	@Override
	public Queue<Packet> processSocketData(XMPPIOService<Object> serv) {
		Packet p = null;

		while ((p = serv.getReceivedPackets().poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing socket data: {0}", p);
			}
			if (p.getElemName().equals("handshake")) {
				processHandshake(p, serv);
			} else {
				if (p.getAttributeStaticStr(new String[]{Iq.ELEM_NAME, "ping"}, "xmlns") == "urn:xmpp:ping" && p.getStanzaTo() != null &&
						getDefHostName().getDomain().equals(p.getStanzaTo().getDomain()) && p.getStanzaFrom() != null &&
						p.getStanzaFrom().getDomain().equals(serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY))) {
					// received PING between cluster nodes to confirm connectivity
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Received XMPP ping [{0}]", serv);
					}
					serv.getSessionData().put("lastConnectivityCheck", System.currentTimeMillis());
					continue;
				}

				// ++packetsReceived;
				Packet result = p;

				if (p.isRouted()) {

					try {
						result = p.unpackRouted();
					} catch (TigaseStringprepException ex) {
						log.log(Level.WARNING, "Packet stringprep addressing problem, dropping packet: {0}", p);

						return null;
					}
				}    // end of if (p.isRouted())
				addOutPacket(result);
			}
		}        // end of while ()

		return null;
	}

	@Override
	public boolean processUndeliveredPacket(Packet packet, Long stamp, String errorMessage) {
		// readd packet - this may be good as we would retry to send packet
		// which delivery failed due to IO error
		try {
			addPacket(packet);
		} catch (NullPointerException ex) {
			log.log(Level.WARNING, "could not redeliver cluster packet on broken cluster connection:", packet.toString());
		}
		return true;
	}

	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {

		// TODO: handle this somehow
	}

	public int schedulerThreads() {
		return 4;
	}

	@Override
	public void serviceStarted(XMPPIOService<Object> serv) {
		if (!repoReloadTimerTask.isScheduled()) {
			addTimerTaskWithTimeout(repoReloadTimerTask, 0, 15 * SECOND);
		}

		super.serviceStarted(serv);
		log.log(Level.INFO, "Cluster connection opened: {0}, type: {1}, id={2}",
				new Object[]{serv.getRemoteAddress(), serv.connectionType().toString(), serv.getUniqueId()});
		if (compress_stream) {
			log.log(Level.INFO, "Starting stream compression for: {0}", serv.getUniqueId());
			serv.startZLib(Deflater.BEST_COMPRESSION);
		}
		switch (serv.connectionType()) {
			case connect:

				// Send init xmpp stream here
				String remote_host = (String) serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);

				serv.getSessionData().put(XMPPIOService.HOSTNAME_KEY, getDefHostName().toString());
				serv.getSessionData()
						.put(PORT_ROUTING_TABLE_PROP_KEY,
							 new String[]{remote_host, ".*@" + remote_host, ".*\\." + remote_host});

				String data = "<stream:stream" + " xmlns='" + XMLNS + "'" +
						" xmlns:stream='http://etherx.jabber.org/streams'" + " from='" + getDefHostName() + "'" +
						" to='" + remote_host + "'" + ">";

				log.log(Level.INFO, "cid: {0}, sending: {1}",
						new Object[]{(String) serv.getSessionData().get("cid"), data});
				serv.xmppStreamOpen(data);

				break;

			default:

				// Do nothing, more data should come soon...
				break;
		}    // end of switch (service.connectionType())
	}

	@Override
	public boolean serviceStopped(XMPPIOService<Object> service) {
		boolean result = super.serviceStopped(service);

		// Make sure it runs just once for each disconnect
		if (result) {
			Map<String, Object> sessionData = service.getSessionData();
			String[] routings = (String[]) sessionData.get(PORT_ROUTING_TABLE_PROP_KEY);
			String addr = (String) sessionData.get(PORT_REMOTE_HOST_PROP_KEY);
			ClusterConnection conns = connectionsPool.get(addr);

			if (conns == null) {
				conns = new ClusterConnection(addr);
				connectionsPool.put(addr, conns);
			}

			synchronized (conns) {
				int size = conns.size();

				conns.removeConn(service);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"serviceStopped: result={0} / size={1} / connPool={2} / serv={3} / conns={4} / connsSize: {5}, / type={6}",
							new Object[]{result, size, connectionsPool, service, conns, conns.size(), service.connectionType()});
				}

				if (size != 0 && conns.size() == 0) {
					if (routings != null) {
						updateRoutings(routings, false);
					}

					// removeRouting(serv.getRemoteHost());
					log.log(Level.INFO, "Disconnected from: {0}", addr);
					updateServiceDiscoveryItem(addr, addr, XMLNS + " disconnected", true);
					clusterController.nodeDisconnected(addr);
				}
			}

			ConnectionType type = service.connectionType();

			if (type == ConnectionType.connect) {
				// make sure that item exists, as in other case there is no point to reconnect
				ClusterRepoItem item = repo.getItem(addr);
				if (item != null) {
					addWaitingTask(sessionData);
				}
			}    // end of if (type == ConnectionType.connect)
			++totalNodeDisconnects;

			int hour = TimeUtils.getHourNow();

			if (lastDayIdx != hour) {
				lastDayIdx = hour;
				lastDay[hour] = 0;
				Arrays.fill(lastHour, 0);
			}
			++lastDay[hour];

			int minute = TimeUtils.getMinuteNow();

			++lastHour[minute];
		}

		return result;
	}

	public void setRepo(ComponentRepository<ClusterRepoItem> repo) {
		if (this.repo != null) {
			this.repo.removeRepoChangeListener(this);
		}
		this.repo = repo;
		if (this.repo != null) {
			this.repo.addRepoChangeListener(this);
		}
	}

	@Override
	public void tlsHandshakeCompleted(XMPPIOService<Object> service) {
	}

	@Override
	public void updateConnectionDetails(Map<String, Object> port_props) {
		String host = (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);
		ClusterRepoItem item = repo.getItem(host);

		if (item != null) {
			port_props.put(SECRET_PROP_KEY, item.getPassword());
			port_props.put(PORT_KEY, item.getPortNo());
		} else {
			port_props.put(MAX_RECONNECTS_PROP_KEY, 0);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "ClusterRepoItem: {0}, port_props: {1}", new Object[]{item, port_props});
		}
	}

	@Override
	public void xmppStreamClosed(XMPPIOService<Object> serv) {
		log.info("Stream closed.");
	}

	@Override
	public String[] xmppStreamOpened(XMPPIOService<Object> service, Map<String, String> attribs) {
		log.log(Level.INFO, "Stream opened: {0}, service: {1}", new Object[]{attribs, service});
		switch (service.connectionType()) {
			case connect: {
				String id = attribs.get("id");

				service.getSessionData().put(XMPPIOService.SESSION_ID_KEY, id);

				ClusterRepoItem item = repo.getItem(getDefHostName().getDomain());
				String secret = item.getPassword();

				try {
					String digest = Algorithms.hexDigest(id, secret, "SHA");

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Calculating digest: id={0}, secret={1}, digest={2}, item={3}",
								new Object[]{id, secret, digest, item});
					}

					return new String[] { "<handshake>" + digest + "</handshake>" };
				} catch (NoSuchAlgorithmException e) {
					log.log(Level.SEVERE, "Can not generate digest for pass phrase.", e);

					return null;
				}
			}

			case accept: {
				String remote_host = attribs.get("from");

				service.getSessionData().put(XMPPIOService.HOSTNAME_KEY, getDefHostName().toString());
				service.getSessionData().put(PORT_REMOTE_HOST_PROP_KEY, remote_host);
				service.getSessionData()
						.put(PORT_ROUTING_TABLE_PROP_KEY,
							 new String[]{remote_host, ".*@" + remote_host, ".*\\." + remote_host});

				String id = UUID.randomUUID().toString();

				service.getSessionData().put(XMPPIOService.SESSION_ID_KEY, id);
				updateConnectionDetails(service.getSessionData());

				return new String[] { "<stream:stream" + " xmlns='" + XMLNS + "'" +
						" xmlns:stream='http://etherx.jabber.org/streams'" + " from='" + getDefHostName() + "'" +
						" to='" + remote_host + "'" + " id='" + id + "'" + ">" };
			}

			default:

				// Do nothing, more data should come soon...
				break;
		}    // end of switch (service.connectionType())

		return null;
	}

	@Override
	public String getDiscoCategoryType() {
		return identity_type;
	}

	@Override
	public String getDiscoDescription() {
		return "Cluster connection manager";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "Total disconnects", totalNodeDisconnects, Level.FINE);
		list.add(getName(), "Service connected time-outs", servConnectedTimeouts, Level.FINE);
		list.add(getName(), "Last day disconnects", Arrays.toString(lastDay), Level.FINE);
		list.add(getName(), "Last hour disconnects", Arrays.toString(lastHour), Level.FINE);
		ioStatsGetter.reset();
		doForAllServices(ioStatsGetter);
		list.add(getName(), "Average compression ratio", ioStatsGetter.getAverageCompressionRatio(), Level.FINE);
		list.add(getName(), "Average decompression ratio", ioStatsGetter.getAverageDecompressionRatio(), Level.FINE);
		list.add(getName(), "Waiting to send", ioStatsGetter.getWaitingToSend(), Level.FINE);

		list.add(getName(), "Max daily cluster nodes count in last month", maxNodes, Level.INFO);
		list.add(getName(), "Max nodes count within last week", maxNodesWithinLastWeek, Level.INFO);

		if ((!list.checkLevel(Level.INFO)) && getNodesConnected().size() > 0) {
			// in FINEST level every component will provide this data
			list.add(getName(), "Known cluster nodes", getNodesConnected().size(), Level.INFO);
		}

		// list.add(getName(), StatisticType.MSG_RECEIVED_OK.getDescription(),
		// packetsReceived,
		// Level.FINE);
		// list.add(getName(), StatisticType.MSG_SENT_OK.getDescription(),
		// packetsSent,
		// Level.FINE);
	}

	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
		super.setClusterController(cl_controller);
		clusterController = cl_controller;
		clusterController.removeCommandListener(sendPacket);
		clusterController.setCommandListener(sendPacket);
	}

	@Override
	public void start() {
		super.start();

		if (clusterEventHandler == null) {
			clusterEventHandler = (ClusterInitializedEvent event) -> {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Setting initialClusterConnectedDone to true (was: {0})",
							initialClusterConnectedDone);
				}
				initialClusterConnectedDone = true;
				eventBus.removeListener(clusterEventHandler);
			};
		}

		eventBus.addListener(ClusterInitializedEvent.class, clusterEventHandler);
	}

	@Override
	public void stop() {
		super.stop();
		eventBus.removeListener(clusterEventHandler);
		clusterEventHandler = null;
	}

	boolean isInitialClusterConnectedDone() {
		return initialClusterConnectedDone;
	}

	@Override
	protected void serviceConnected(XMPPIOService<Object> serv) {
		String[] routings = (String[]) serv.getSessionData().get(PORT_ROUTING_TABLE_PROP_KEY);
		String addr = (String) serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);

		ClusterConnection conns = connectionsPool.get(addr);

		if (conns == null) {
			conns = new ClusterConnection(addr);
			connectionsPool.put(addr, conns);
		}

		synchronized (conns) {
			int size = conns.size();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "New service connected: size = {0} / connectionsPool={1} / serv={2} / conns={3}, connsSize={4}",
						new Object[]{size, connectionsPool, serv, conns, conns.size()});
			}

			// setting userJid to hostname of remote cluster node
			serv.setUserJid((String) serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY));

			conns.addConn(serv);
			if (size == 0 && conns.size() > 0) {
				updateRoutings(routings, true);
				log.log(Level.INFO, "Connected to: {0}", addr);
				updateServiceDiscoveryItem(addr, addr, XMLNS + " connected", true);
				clusterController.nodeConnected(addr);
			}
		}

		try {
			// initial cluster connection done
			int connectedSize = getNodesConnected().size();
			int repoSize = repo.allItems().size();
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"All repo nodes connected! Connected: {0}, repo size: {1}, initialClusterConnectedDone: {2}",
						new Object[]{connectedSize, repoSize, initialClusterConnectedDone});
			}

			synchronized (this) {
				if (!initialClusterConnectedDone && (repoSize <= 1 || repoSize > 1 && connectedSize >= repoSize - 1)) {
					initialClusterConnectedDone = true;

					eventBus.fire(new ClusterInitializedEvent());
				}
			}
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "There was an error while reading size of cluster repository", e);
		}


		super.serviceConnected(serv);
	}

	@Override
	protected boolean writePacketToSocket(Packet p) {

		// ++packetsSent;
		String ip = p.getTo().getDomain();
		ClusterConnection conns = connectionsPool.get(ip);

		XMPPIOService<Object> serv = connectionSelector.selectConnection(p, conns);
		if (serv != null) {
			return super.writePacketToSocket(serv, p);
		} else {
			log.log(Level.WARNING, "No cluster connection to send a packet: {0}", p);

			return false;
		}
	}

	@Override
	protected int[] getDefPlainPorts() {
		if (repo == null) {
			return new int[]{ClusterRepoItem.PORT_NO_PROP_VAL};
		}
		ClusterRepoItem item = repo.getItem(getDefHostName().getDomain());

		return new int[]{item.getPortNo()};
	}

	@Override
	protected String getDefTrafficThrottling() {
		return "xmpp:25m:0:disc,bin:20000m:0:disc";
	}

	@Override
	protected long getMaxInactiveTime() {
		return 3 * MINUTE;
	}

	@Override
	protected Integer getMaxQueueSize(int def) {
		return def * 10;
	}

	@Override
	protected Map<String, Object> getParamsForPort(int port) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>(10);

		defs.put(PORT_TYPE_PROP_KEY, ConnectionType.accept);
		defs.put(PORT_SOCKET_PROP_KEY, SocketType.plain);
		defs.put(PORT_IFC_PROP_KEY, PORT_IFC_PROP_VAL);

		return defs;
	}

	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new XMPPIOService<>();
	}

	@Override
	protected boolean isHighThroughput() {
		return true;
	}

	private void sendEvent(REPO_ITEM_UPDATE_TYPE action, ClusterRepoItem item) {

		// either RepositoryItem was wrong or EventBus is not enabled - skiping broadcasting the event;
		if (eventBus == null || item == null) {
			return;
		}

		ClusterRepoItemEvent event = new ClusterRepoItemEvent(item, action);
		eventBus.fire(event);
	}

	private void processHandshake(Packet p, XMPPIOService<Object> serv) {

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing handshake: packet={0} / service={1} / sessionData={2}",
					new Object[]{p, serv, serv.getSessionData()});
		}

		String serv_addr = (String) serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);
		try {
			InetAddress addr = InetAddress.getByName(serv_addr);

			// we ignore any local addresses
			if ((addr.isAnyLocalAddress() || addr.isLoopbackAddress()) ||
					NetworkInterface.getByInetAddress(addr) != null) {
				log.log(Level.WARNING, "Cluster handshake received from this instance, terminating: {0}", serv_addr);
				serv.stop();
				return;
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Cluster handshake received from this instance, terminating: " + serv_addr, ex);
			serv.stop();
		}

		switch (serv.connectionType()) {
			case connect: {
				String data = p.getElemCData();

				if (data == null) {
					serviceConnected(serv);
				} else {
					log.log(Level.WARNING, "Incorrect packet received: {0}", p);
				}

				break;
			}

			case accept: {
				String digest = p.getElemCData();
				String id = (String) serv.getSessionData().get(XMPPIOService.SESSION_ID_KEY);
				String secret = (String) serv.getSessionData().get(SECRET_PROP_KEY);

				try {
					String loc_digest = Algorithms.hexDigest(id, secret, "SHA");

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Calculating digest: secret={0}, digest={1}, loc_digest={2}, sessionData={3}",
								new Object[]{secret, digest, loc_digest, serv.getSessionData()});
					}
					if ((digest != null) && digest.equals(loc_digest)) {
						Packet resp = Packet.packetInstance(new Element("handshake"), null, null);

						writePacketToSocket(serv, resp);
						serviceConnected(serv);
					} else {
						if (secret == null) {
							log.log(Level.WARNING,
									"Remote hostname not found in local configuration or time difference between cluster nodes is too big. Connection not accepted: {0}",
									serv);

							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST,
										"Remote hostname not found in local configuration or time difference between cluster nodes is too big. Connection not accepted! Remote host: {0}, sessionData: {1}, repoItem: {2}, service: {3}",
										new Object[]{serv_addr, serv.getSessionData(), repo.getItem(serv_addr), serv});
							}

						} else {
							log.log(Level.WARNING, "Handshaking password doesn''t match, disconnecting: {0}", serv);

							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.WARNING,
										"Handshaking password doesn''t match, disconnecting! Remote host: {0}, sessionData: {1}, repoItem: {2}, service: {3}",
										new Object[]{serv_addr, serv.getSessionData(), repo.getItem(serv_addr), serv});

							}

						}
						serv.stop();
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "Handshaking error.", e);
				}

				break;
			}

			default:

				// Do nothing, more data should come soon...
				break;
		}    // end of switch (service.connectionType())
	}

	private void updateRoutings(String[] routings, boolean add) {
		if (add) {
			for (String route : routings) {
				try {
					addRegexRouting(route);
				} catch (Exception e) {
					log.log(Level.WARNING, "Can not add regex routing ''{0}'' : {1}", new Object[]{route, e});
				}
			}
		} else {
			for (String route : routings) {
				try {
					removeRegexRouting(route);
				} catch (Exception e) {
					log.log(Level.WARNING, "Can not remove regex routing ''{0}'' : {1}", new Object[]{route, e});
				}
			}
		}
	}

	public static class ClusterInitializedEvent implements EventBusEvent {

		public ClusterInitializedEvent() {

		}

	}

	@Bean(name = "clConRepositoryBean", parent = ClusterConnectionManager.class, active = true)
	public static class DefClConRepositoryBean
			extends AbstractSDComponentRepositoryBean<ClusterRepoItem> {

		private static DataSourceHelper.Matcher matcher = (Class clazz) -> {
			return ReflectionHelper.classMatchesClassWithParameters(clazz, ComponentRepositoryDataSourceAware.class,
																	new Type[]{ClusterRepoItem.class,
																			   DataSource.class});
		};
		private ComponentRepository<ClusterRepoItem> repo = null;

		@Override
		protected Class<? extends ComponentRepositoryDataSourceAware<ClusterRepoItem, DataSource>> findClassForDataSource(
				DataSource dataSource) throws DBInitException {
			Class cls = DataSourceHelper.getDefaultClass(ComponentRepository.class, dataSource.getResourceUri(),
														 matcher);
			return (Class<ComponentRepositoryDataSourceAware<ClusterRepoItem, DataSource>>) cls;
		}

	}

	private class IOServiceStatisticsGetter
			implements ServiceChecker<XMPPIOService<Object>> {

		private int clIOQueue = 0;
		private float compressionRatio = 0f;
		private int counter = 0;
		private float decompressionRatio = 0f;
		private StatisticsList list = new StatisticsList(Level.ALL);

		@Override
		public void check(XMPPIOService<Object> service) {
			service.getStatistics(list, true);
			compressionRatio += list.getValue("zlibio", "Average compression rate", -1f);
			decompressionRatio += list.getValue("zlibio", "Average decompression rate", -1f);
			++counter;
			clIOQueue += service.waitingToSendSize();
		}

		public void reset() {

			// Statistics are reset on the low socket level instead. This way we do
			// not loose
			// any stats in case of the disconnection.
			// bytesReceived = 0;
			// bytesSent = 0;
			clIOQueue = 0;
			counter = 0;
			compressionRatio = 0f;
			decompressionRatio = 0f;
		}

		public float getAverageCompressionRatio() {
			return compressionRatio / counter;
		}

		public float getAverageDecompressionRatio() {
			return decompressionRatio / counter;
		}

		public int getWaitingToSend() {
			return clIOQueue;
		}
	}

	private class SendPacket
			extends CommandListenerAbstract {

		private SendPacket(String name) {
			super(name, null);
		}

		@Override
		public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String, String> data,
								   Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[]{fromNode, visitedNodes, data, packets});
			}
			for (Element element : packets) {
				try {
					addPacketNB(Packet.packetInstance(element));

					// writePacketToSocket();
				} catch (TigaseStringprepException ex) {
					log.log(Level.WARNING, "Stringprep exception for packet: {0}", element);
				}
			}
		}
	}
	
	protected class Watchdog
			extends ConnectionManager.Watchdog {

		@Override
		protected long getDurationSinceLastTransfer(final XMPPIOService service) {
			Long lastTransfer = (Long) service.getSessionData().get("lastConnectivityCheck");
			if (lastTransfer == null) {
				service.getSessionData().put("lastConnectivityCheck", System.currentTimeMillis() - watchdogTimeout);
				return watchdogTimeout;
			}
			return System.currentTimeMillis() - lastTransfer;
		}

	}
}

