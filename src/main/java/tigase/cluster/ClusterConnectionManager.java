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

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

import tigase.net.ConnectionType;

//import tigase.net.IOService;
import tigase.net.SocketType;

import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.server.ServiceChecker;

import tigase.stats.StatisticsList;

import tigase.util.Algorithms;
import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;
import tigase.util.TimeUtils;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.net.UnknownHostException;

import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;

import javax.script.Bindings;

//~--- classes ----------------------------------------------------------------

/**
 * Class ClusterConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterConnectionManager extends ConnectionManager<XMPPIOService<Object>>
		implements ClusteredComponent {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(ClusterConnectionManager.class.getName());

	/** Field description */
	public static final String SECRET_PROP_KEY = "secret";

	/** Field description */
	public static final String PORT_LOCAL_HOST_PROP_KEY = "local-host";

	/** Field description */
	public static final String PORT_ROUTING_TABLE_PROP_KEY = "routing-table";

	/** Field description */
	public static final String RETURN_SERVICE_DISCO_KEY = "service-disco";

	/** Field description */
	public static final boolean RETURN_SERVICE_DISCO_VAL = true;

	/** Field description */
	public static final String IDENTITY_TYPE_KEY = "identity-type";

	/** Field description */
	public static final String IDENTITY_TYPE_VAL = "generic";

	/** Field description */
	public static final String CONNECT_ALL_PAR = "--cluster-connect-all";

	/** Field description */
	public static final String CONNECT_ALL_PROP_KEY = "connect-all";

	/** Field description */
	public static final String CLUSTER_CONTR_ID_PROP_KEY = "cluster-controller-id";

	/** Field description */
	public static final boolean CONNECT_ALL_PROP_VAL = false;

	/** Field description */
	public static final String COMPRESS_STREAM_PROP_KEY = "compress-stream";

	/** Field description */
	public static final boolean COMPRESS_STREAM_PROP_VAL = false;

	/** Field description */
	public static final String XMLNS = "tigase:cluster";
	private static final String SERVICE_CONNECTED_TIMER = "service-connected-timer";

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	public int[] PORTS = { 5277 };

	/** Field description */
	public String[] PORT_IFC_PROP_VAL = { "*" };

	/** Field description */
	public String SECRET_PROP_VAL = "someSecret";
	private ClusterController clusterController = null;

	// private boolean notify_admins = NOTIFY_ADMINS_PROP_VAL;
	// private String[] admins = new String[] {};
	private String cluster_controller_id = null;
	private IOServiceStatisticsGetter ioStatsGetter = new IOServiceStatisticsGetter();

//private ServiceEntity serviceEntity = null;
	// private boolean service_disco = RETURN_SERVICE_DISCO_VAL;
	private String identity_type = IDENTITY_TYPE_VAL;
	private boolean connect_all = CONNECT_ALL_PROP_VAL;
	private boolean compress_stream = COMPRESS_STREAM_PROP_VAL;
	private long[] lastDay = new long[24];
	private int lastDayIdx = 0;
	private long[] lastHour = new long[60];
	private int lastHourIdx = 0;

//private LinkedHashMap<String, LinkedHashMap<Long, Packet>> waiting_packs =
//   new LinkedHashMap<String, LinkedHashMap<Long, Packet>>();
//private LinkedHashMap<String, Long> sent_rids = new LinkedHashMap<String, Long>();
//private LinkedHashMap<String, Long> recieved_rids =
//   new LinkedHashMap<String, Long>();
//private LinkedHashMap<String, Long> recieved_acks =
//   new LinkedHashMap<String, Long>();
	private int nodesNo = 0;
	private long servConnectedTimeouts = 0;
	private long totalNodeDisconnects = 0;

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
		Map<String, Object> props = super.getDefaults(params);

		props.put(RETURN_SERVICE_DISCO_KEY, RETURN_SERVICE_DISCO_VAL);
		props.put(IDENTITY_TYPE_KEY, IDENTITY_TYPE_VAL);

		if ((params.get(CONNECT_ALL_PAR) == null)
				||!((String) params.get(CONNECT_ALL_PAR)).equals("true")) {
			props.put(CONNECT_ALL_PROP_KEY, false);
		} else {
			props.put(CONNECT_ALL_PROP_KEY, true);
		}

		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String) params.get(CLUSTER_NODES)).split(",");

			for (int i = 0; i < cl_nodes.length; i++) {
				cl_nodes[i] = BareJID.parseJID(cl_nodes[i])[1];
			}

			nodesNo = cl_nodes.length;
			props.put(CLUSTER_NODES_PROP_KEY, cl_nodes);
		} else {
			props.put(CLUSTER_NODES_PROP_KEY, new String[] { getDefHostName().getDomain() });
		}

		props.put(CLUSTER_CONTR_ID_PROP_KEY, DEF_CLUST_CONTR_NAME + "@" + getDefHostName());
		props.put(COMPRESS_STREAM_PROP_KEY, COMPRESS_STREAM_PROP_VAL);

//  props.put(NOTIFY_ADMINS_PROP_KEY, NOTIFY_ADMINS_PROP_VAL);
//  if (params.get(GEN_ADMINS) != null) {
//    admins = ((String)params.get(GEN_ADMINS)).split(",");
//  } else {
//    admins = new String[] { "admin@localhost" };
//  }
//  props.put(ADMINS_PROP_KEY, admins);
		return props;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoCategoryType() {
		return identity_type;
	}

//private void updateServiceDiscovery(String jid, String name) {
//  ServiceEntity item = new ServiceEntity(jid, null, name);
//  //item.addIdentities(new ServiceIdentity("component", identity_type, name));
//  log.info("Modifing service-discovery info: " + item.toString());
//  serviceEntity.addItems(item);
//}
//
//@Override
//public Element getDiscoInfo(String node, String jid) {
//  if (jid != null && getName().equals(JIDUtils.getNodeNick(jid))) {
//    return serviceEntity.getDiscoInfo(node);
//  }
//  return null;
//}
//
//@Override
//public  List<Element> getDiscoFeatures() { return null; }
//
//@Override
//public List<Element> getDiscoItems(String node, String jid) {
//  if (getName().equals(JIDUtils.getNodeNick(jid))) {
//    return serviceEntity.getDiscoItems(node, null);
//  } else {
//    return Arrays.asList(serviceEntity.getDiscoItem(null,
//        JIDUtils.getNodeID(getName(), jid)));
//  }
//}
//

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return XMLNS + " " + getName();
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "Total disconnects", totalNodeDisconnects, Level.FINE);
		list.add(getName(), "Service connected time-outs", servConnectedTimeouts, Level.FINE);
		list.add(getName(), "Last day disconnects", Arrays.toString(lastDay), Level.FINE);
		list.add(getName(), "Last hour disconnects", Arrays.toString(lastHour), Level.FINE);
		ioStatsGetter.reset();
		doForAllServices(ioStatsGetter);
		list.add(getName(), "Average compression ratio", ioStatsGetter.getAverageCompressionRatio(),
				Level.FINE);
		list.add(getName(), "Average decompression ratio",
				ioStatsGetter.getAverageDecompressionRatio(), Level.FINE);
		list.add(getName(), "Bytes sent", ioStatsGetter.getBytesSent(), Level.FINE);
		list.add(getName(), "Bytes received", ioStatsGetter.getBytesReceived(), Level.FINE);
		list.add(getName(), "Waiting to send", ioStatsGetter.getWaitingToSend(), Level.FINE);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * This method can be overwritten in extending classes to get a different
	 * packets distribution to different threads. For PubSub, probably better
	 * packets distribution to different threads would be based on the
	 * sender address rather then destination address.
	 * @param packet
	 * @return
	 */
	@Override
	public int hashCodeForPacket(Packet packet) {

		// There is a separate connection to each cluster node, ideally we want to
		// process packets in a separate thread for each connection, so let's try
		// to get the hash code by the destination node address
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}

		return packet.getTo().hashCode();
	}

	/**
	 * Method description
	 *
	 *
	 * @param binds
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put("clusterCM", this);
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	@Override
	public void nodeConnected(String node) {}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	@Override
	public void nodeDisconnected(String node) {}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}

		if ((packet.getStanzaTo() != null) && packet.getStanzaTo().equals(getComponentId())) {
			try {
				addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet,
						"Not implemented", true));
			} catch (PacketErrorTypeException e) {
				log.log(Level.WARNING, "Packet processing exception: {0}", e);
			}

			return;
		}

		writePacketToSocket(packet.packRouted());

//  if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME) {
//    writePacketToSocket(packet);
//  } else {
//    writePacketToSocket(packet.packRouted());
//  }
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 *
	 * @return
	 */
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
				Packet result = p;

				if (p.isRouted()) {

//        processReceivedRid(p, serv);
//        processReceivedAck(p, serv);
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

	/**
	 *
	 * @return
	 */
	@Override
	@TODO(note = "The number of threads should be equal or greater to number of cluster nodes.")
	public int processingThreads() {

		// This should work well as far as nodesNo is initialized before this
		// method is called which is true only during program startup time.
		// In case of reconfiguration or new node joining this might not be
		// the case. Low priority issue though.
		return Math.max(Runtime.getRuntime().availableProcessors(), nodesNo);
	}

	/**
	 * Method description
	 *
	 *
	 * @param port_props
	 */
	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {

		// TODO: handle this somehow
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void serviceStarted(XMPPIOService<Object> serv) {
		ServiceConnectedTimer task = new ServiceConnectedTimer(serv);

		serv.getSessionData().put(SERVICE_CONNECTED_TIMER, task);
		addTimerTask(task, 10, TimeUnit.SECONDS);
		super.serviceStarted(serv);
		log.log(Level.INFO, "cluster connection opened: {0}, type: {1}, id={2}",
				new Object[] { serv.getRemoteAddress(),
				serv.connectionType().toString(), serv.getUniqueId() });

		if (compress_stream) {
			log.log(Level.INFO, "Starting stream compression for: {0}", serv.getUniqueId());
			serv.startZLib(Deflater.BEST_COMPRESSION);
		}

//  String addr =
//    (String)service.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);
//  addRouting(addr);
		// addRouting(serv.getRemoteHost());
		switch (serv.connectionType()) {
			case connect :

				// Send init xmpp stream here
				// XMPPIOService serv = (XMPPIOService)service;
				String remote_host = (String) serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);

				serv.getSessionData().put(XMPPIOService.HOSTNAME_KEY, remote_host);
				serv.getSessionData().put(PORT_ROUTING_TABLE_PROP_KEY, new String[] { remote_host,
						".*@" + remote_host, ".*\\." + remote_host });

				String data = "<stream:stream" + " xmlns='" + XMLNS + "'"
					+ " xmlns:stream='http://etherx.jabber.org/streams'" + " from='" + getDefHostName() + "'"
					+ " to='" + remote_host + "'" + ">";

				log.log(Level.INFO, "cid: {0}, sending: {1}",
						new Object[] { (String) serv.getSessionData().get("cid"),
						data });
				serv.xmppStreamOpen(data);

				break;

			default :

				// Do nothing, more data should come soon...
				break;
		}    // end of switch (service.connectionType())
	}

	/**
	 * Method description
	 *
	 *
	 * @param service
	 *
	 * @return
	 */
	@Override
	public boolean serviceStopped(XMPPIOService<Object> service) {
		boolean result = super.serviceStopped(service);

		// Make sure it runs just once for each disconnect
		if (result) {
			Map<String, Object> sessionData = service.getSessionData();
			String[] routings = (String[]) sessionData.get(PORT_ROUTING_TABLE_PROP_KEY);

			if (routings != null) {
				updateRoutings(routings, false);
			}

			ConnectionType type = service.connectionType();

			if (type == ConnectionType.connect) {
				addWaitingTask(sessionData);

				// reconnectService(sessionData, connectionDelay);
			}    // end of if (type == ConnectionType.connect)

			// removeRouting(serv.getRemoteHost());
			String addr = (String) sessionData.get(PORT_REMOTE_HOST_PROP_KEY);

			log.log(Level.INFO, "Disonnected from: {0}", addr);
			updateServiceDiscoveryItem(addr, addr, XMLNS + " disconnected", true);
			clusterController.nodeDisconnected(addr);

//    Map<String, String> method_params = new LinkedHashMap<String, String>();
//    method_params.put("disconnected", addr);
//    addOutPacket(new Packet(ClusterElement.createClusterMethodCall(
//            getComponentId(), cluster_controller_id,
//            StanzaType.set, ClusterMethods.UPDATE_NODES.toString(),
//            method_params).getClusterElement()));
			++totalNodeDisconnects;

			int hour = TimeUtils.getHourNow();

			if (lastDayIdx != hour) {
				lastDayIdx = hour;
				lastDay[hour] = 0;
				Arrays.fill(lastHour, 0);
			}

			++lastDay[hour];

			int minute = TimeUtils.getMinuteNow();

//    if (lastHourIdx != minute) {
//      lastHourIdx = minute;
//      lastHour[minute] = 0;
//    }
			++lastHour[minute];
		}

		return result;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterController cl_controller) {
		this.clusterController = cl_controller;
	}

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);

		// service_disco = (Boolean)props.get(RETURN_SERVICE_DISCO_KEY);
		identity_type = (String) props.get(IDENTITY_TYPE_KEY);
		compress_stream = (Boolean) props.get(COMPRESS_STREAM_PROP_KEY);

		// serviceEntity = new ServiceEntity(getName(), "external", "XEP-0114");
//  serviceEntity = new ServiceEntity(XMLNS + " " + getName(), null, XMLNS);
//  serviceEntity.addIdentities(
//    new ServiceIdentity("component", identity_type, XMLNS + " " + getName()));
		connect_all = (Boolean) props.get(CONNECT_ALL_PROP_KEY);
		cluster_controller_id = (String) props.get(CLUSTER_CONTR_ID_PROP_KEY);

//  notify_admins = (Boolean)props.get(NOTIFY_ADMINS_PROP_KEY);
//  admins = (String[])props.get(ADMINS_PROP_KEY);
		connectionDelay = 5 * SECOND;

		String[] cl_nodes = (String[]) props.get(CLUSTER_NODES_PROP_KEY);
		int[] ports = (int[]) props.get(PORTS_PROP_KEY);

		if (ports != null) {
			PORTS = ports;
		}

		if (cl_nodes != null) {
			nodesNo = cl_nodes.length;

			for (String node : cl_nodes) {
				String host = BareJID.parseJID(node)[1];

				log.log(Level.CONFIG, "Found cluster node host: {0}", host);

				if ( !host.equals(getDefHostName().getDomain())
						&& ((host.hashCode() > getDefHostName().hashCode()) || connect_all)) {
					log.log(Level.CONFIG, "Trying to connect to cluster node: {0}", host);

					Map<String, Object> port_props = new LinkedHashMap<String, Object>(12);

					port_props.put(SECRET_PROP_KEY, SECRET_PROP_VAL);
					port_props.put(PORT_LOCAL_HOST_PROP_KEY, getDefHostName());
					port_props.put(PORT_TYPE_PROP_KEY, ConnectionType.connect);
					port_props.put(PORT_SOCKET_PROP_KEY, SocketType.plain);
					port_props.put(PORT_REMOTE_HOST_PROP_KEY, host);
					port_props.put(PORT_IFC_PROP_KEY, new String[] { host });
					port_props.put(MAX_RECONNECTS_PROP_KEY, 99999999);
					port_props.put(PORT_KEY, PORTS[0]);
					addWaitingTask(port_props);

					// reconnectService(port_props, connectionDelay);
				}
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param service
	 */
	@Override
	public void tlsHandshakeCompleted(XMPPIOService<Object> service) {}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void xmppStreamClosed(XMPPIOService<Object> serv) {
		log.info("Stream closed.");
	}

	/**
	 * Method description
	 *
	 *
	 * @param service
	 * @param attribs
	 *
	 * @return
	 */
	@Override
	public String xmppStreamOpened(XMPPIOService<Object> service, Map<String, String> attribs) {
		log.log(Level.INFO, "Stream opened: {0}", attribs);

		switch (service.connectionType()) {
			case connect : {
				String id = attribs.get("id");

				service.getSessionData().put(XMPPIOService.SESSION_ID_KEY, id);

				String secret = (String) service.getSessionData().get(SECRET_PROP_KEY);

				try {
					String digest = Algorithms.hexDigest(id, secret, "SHA");

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Calculating digest: id={0}, secret={1}, digest={2}",
								new Object[] { id,
								secret, digest });
					}

					return "<handshake>" + digest + "</handshake>";
				} catch (NoSuchAlgorithmException e) {
					log.log(Level.SEVERE, "Can not generate digest for pass phrase.", e);

					return null;
				}
			}

			case accept : {
				String remote_host = attribs.get("from");

				service.getSessionData().put(XMPPIOService.HOSTNAME_KEY, remote_host);
				service.getSessionData().put(PORT_REMOTE_HOST_PROP_KEY, remote_host);
				service.getSessionData().put(PORT_ROUTING_TABLE_PROP_KEY, new String[] { remote_host,
						".*@" + remote_host, ".*\\." + remote_host });

				String id = UUID.randomUUID().toString();

				service.getSessionData().put(XMPPIOService.SESSION_ID_KEY, id);

				return "<stream:stream" + " xmlns='" + XMLNS + "'"
						+ " xmlns:stream='http://etherx.jabber.org/streams'" + " from='" + getDefHostName()
							+ "'" + " to='" + remote_host + "'" + " id='" + id + "'" + ">";
			}

			default :

				// Do nothing, more data should come soon...
				break;
		}    // end of switch (service.connectionType())

		return null;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	protected int[] getDefPlainPorts() {
		return PORTS;
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. we shoulnd not really close external component
	 * connection at all, so let's say something like: 1000 days...
	 *
	 * @return a <code>long</code> value
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 1000 * 24 * HOUR;
	}

	@Override
	protected Integer getMaxQueueSize(int def) {
		return def * 10;
	}

	@Override
	protected Map<String, Object> getParamsForPort(int port) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>(10);

		defs.put(SECRET_PROP_KEY, SECRET_PROP_VAL);
		defs.put(PORT_TYPE_PROP_KEY, ConnectionType.accept);
		defs.put(PORT_SOCKET_PROP_KEY, SocketType.plain);
		defs.put(PORT_IFC_PROP_KEY, PORT_IFC_PROP_VAL);

		return defs;
	}

	@Override
	protected String getServiceId(Packet packet) {
		try {
			return DNSResolver.getHostIP(packet.getTo().getDomain());
		} catch (UnknownHostException e) {
			log.log(Level.WARNING, "Uknown host exception for address: {0}", packet.getTo().getDomain());

			return packet.getTo().getDomain();
		}
	}

	@Override
	protected String getUniqueId(XMPPIOService<Object> serv) {

		// return (String)serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);
		return serv.getRemoteAddress();
	}

	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new XMPPIOService<Object>();
	}

	@Override
	protected boolean isHighThroughput() {
		return true;
	}

	//~--- methods --------------------------------------------------------------

	protected void serviceConnected(XMPPIOService<Object> serv) {
		String[] routings = (String[]) serv.getSessionData().get(PORT_ROUTING_TABLE_PROP_KEY);

		updateRoutings(routings, true);

		String addr = (String) serv.getSessionData().get(PORT_REMOTE_HOST_PROP_KEY);

		log.log(Level.INFO, "Connected to: {0}", addr);
		updateServiceDiscoveryItem(addr, addr, XMLNS + " connected", true);
		clusterController.nodeConnected(addr);

		ServiceConnectedTimer task =
			(ServiceConnectedTimer) serv.getSessionData().get(SERVICE_CONNECTED_TIMER);

		if (task == null) {
			log.log(Level.WARNING, "Missing service connected timer task: {0}", serv);
		} else {
			task.cancel();
		}

//  Map<String, String> method_params = new LinkedHashMap<String, String>();
//  method_params.put("connected", addr);
//  addOutPacket(new Packet(ClusterElement.createClusterMethodCall(
//        getComponentId(), cluster_controller_id,
//        StanzaType.set, ClusterMethods.UPDATE_NODES.toString(),
//        method_params).getClusterElement()));
//  synchronized (waiting_packs) {
//    LinkedHashMap<Long, Packet> waiting_packets =
//       waiting_packs.get(serv.getRemoteAddress());
//    if (waiting_packets == null) {
//      waiting_packets = new LinkedHashMap<Long, Packet>();
//      waiting_ack.put(serv.getRemoteAddress(), waiting_packets);
//    }
//  }
	}

	@Override
	protected boolean writePacketToSocket(Packet p) {

//  long rid = ++send_rid;
//  p.getElement().setAttribute("rid", ""+rid);
//  synchronized (waiting_packs) {
//    LinkedHashMap<Long, Packet> waiting_packets =
//       waiting_packs.get(getServiceId(p));
//    if (waiting_packets == null) {
//      waiting_packets = new LinkedHashMap<Long, Packet>();
//      waiting_ack.put(getServiceId(p), waiting_packets);
//    }
//    waiting_packets.put(rid, p);
//  }
		return super.writePacketToSocket(p);
	}

//private void processReceivedAck(Packet packet, XMPPIOService serv) {
//  String ack_str = packet.getAttribute("ack");
//  if (ack_str == null) {
//    log.warning("ack attribute is null for packet: " + packet.toString()
//      + ", please update all cluster nodes.");
//  } else {
//    try {
//      long r_ack = Long.parseLong(ack_str);
//      synchronized (waiting_packs) {
//        LinkedHashMap<Long, Packet> waiting_packets =
//           waiting_packs.get(serv.getRemoteAddress());
//        if (waiting_packets == null) {
//          log.warning("Checking ACK and waiting_packets is null for packet: " +
//            packet);
//          return;
//        }
//        long last_ack = received_acks.get(serv.getRemoteAddress());
//        if (r_ack == (++last_ack)) {
//          received_acks.put(serv.getRemoteAddress(), r_ack);
//          Packet p = waiting_packets.remove(r_ack);
//          if (p == null) {
//            log.warning("Packet for r_ack = " + r_ack + " not found...");
//          }
//        } else {
//        }
//      }
//    } catch (NumberFormatException e) {
//      log.warning("Incorrect ack value in packet: " + packet.toString());
//    }
//  }
//}
//private void processReceivedRid(Packet packet, XMPPIOService serv) {
//  String rid_str = packet.getAttribute("rid");
//  if (rid_str == null) {
//    log.warning("rid attribute is null for packet: " + packet.toString()
//      + ", please update all cluster nodes.");
//  } else {
//    try {
//      long r_rid = Long.parseLong(rid_str);
//    } catch (NumberFormatException e) {
//      log.warning("Incorrect rid value in packet: " + packet.toString());
//    }
//  }
//}
	private void processHandshake(Packet p, XMPPIOService<Object> serv) {
		switch (serv.connectionType()) {
			case connect : {
				String data = p.getElemCData();

				if (data == null) {
					serviceConnected(serv);
				} else {
					log.log(Level.WARNING, "Incorrect packet received: {0}", p);
				}

				break;
			}

			case accept : {
				String digest = p.getElemCData();
				String id = (String) serv.getSessionData().get(XMPPIOService.SESSION_ID_KEY);
				String secret = (String) serv.getSessionData().get(SECRET_PROP_KEY);

				try {
					String loc_digest = Algorithms.hexDigest(id, secret, "SHA");

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Calculating digest: id={0}, secret={1}, digest={2}",
								new Object[] { id,
								secret, loc_digest });
					}

					if ((digest != null) && digest.equals(loc_digest)) {
						Packet resp = Packet.packetInstance(new Element("handshake"), null, null);

						writePacketToSocket(serv, resp);
						serviceConnected(serv);
					} else {
						log.warning("Handshaking password doesn't match, disconnecting...");
						serv.stop();
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "Handshaking error.", e);
				}

				break;
			}

			default :

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
					log.log(Level.WARNING, "Can not add regex routing ''{0}'' : {1}", new Object[] { route,
							e });
				}
			}
		} else {
			for (String route : routings) {
				try {
					removeRegexRouting(route);
				} catch (Exception e) {
					log.log(Level.WARNING, "Can not remove regex routing ''{0}'' : {1}", new Object[] { route,
							e });
				}
			}
		}
	}

	//~--- inner classes --------------------------------------------------------

	private class IOServiceStatisticsGetter implements ServiceChecker {
		private long bytesReceived = 0;
		private long bytesSent = 0;
		private int clIOQueue = 0;
		private float compressionRatio = 0f;
		private int counter = 0;
		private float decompressionRatio = 0f;
		private StatisticsList list = new StatisticsList(Level.ALL);

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param service
		 */
		@Override
		public void check(XMPPIOService service) {
			service.getStatistics(list);
			compressionRatio += list.getValue("zlibio", "Average compression rate", -1f);
			decompressionRatio += list.getValue("zlibio", "Average decompression rate", -1f);
			++counter;
			bytesReceived += list.getValue("socketio", "Bytes received", -1l);
			bytesSent += list.getValue("socketio", "Bytes sent", -1l);
			clIOQueue += service.waitingToSendSize();
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public float getAverageCompressionRatio() {
			return compressionRatio / counter;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public float getAverageDecompressionRatio() {
			return decompressionRatio / counter;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public long getBytesReceived() {
			return bytesReceived;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public long getBytesSent() {
			return bytesSent;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getWaitingToSend() {
			return clIOQueue;
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 */
		public void reset() {
			bytesReceived = 0;
			bytesSent = 0;
			compressionRatio = 0f;
			decompressionRatio = 0f;
			counter = 0;
			clIOQueue = 0;
		}
	}


	private class ServiceConnectedTimer extends TimerTask {
		private XMPPIOService<Object> serv = null;

		//~--- constructors -------------------------------------------------------

		private ServiceConnectedTimer(XMPPIOService<Object> serv) {
			this.serv = serv;
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			++servConnectedTimeouts;
			log.log(Level.INFO, "ServiceConnectedTimer timeout expired, closing connection: {0}", serv);
			serv.forceStop();
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
