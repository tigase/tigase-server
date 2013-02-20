/*
 * ConnectionManager.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

import tigase.net.ConnectionOpenListener;
import tigase.net.ConnectionOpenThread;
import tigase.net.ConnectionType;
import tigase.net.SocketThread;
import tigase.net.SocketType;

import tigase.server.script.CommandIfc;

import tigase.stats.StatisticsList;

import tigase.util.DataTypes;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPIOServiceListener;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.SocketException;

import java.nio.channels.SocketChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;

import javax.script.Bindings;

/**
 * Describe class ConnectionManager here.
 *
 *
 * Created: Sun Jan 22 22:52:58 2006
 *
 * @param <IO>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConnectionManager<IO extends XMPPIOService<?>>
				extends AbstractMessageReceiver
				implements XMPPIOServiceListener<IO> {
	/** Field description */
	public static final String HT_TRAFFIC_THROTTLING_PROP_KEY =
		"--cm-ht-traffic-throttling";

	/** Field description */
	public static final String HT_TRAFFIC_THROTTLING_PROP_VAL =
		"xmpp:25k:0:disc,bin:200m:0:disc";

	/** Field description. */
	public static final String NET_BUFFER_HT_PROP_KEY = "--net-buff-high-throughput";

	/** Field description. */
	public static final String NET_BUFFER_ST_PROP_KEY = "--net-buff-standard";

	/** Field description. */
	public static final String PORT_LOCAL_HOST_PROP_KEY = "local-host";

	/** Field description */
	public static final String ST_TRAFFIC_THROTTLING_PROP_KEY = "--cm-traffic-throttling";

	/** Field description */
	public static final String ST_TRAFFIC_THROTTLING_PROP_VAL =
		"xmpp:2500:0:disc,bin:20m:0:disc";

	/** Field description */
	public static final String TRAFFIC_THROTTLING_PROP_KEY = "traffic-throttling";

	/** Field description */
	protected static final long LAST_MINUTE_BIN_LIMIT_PROP_VAL = 20000000L;

	/** Field description */
	protected static final long LAST_MINUTE_PACKETS_LIMIT_PROP_VAL = 2500L;

	/** Field description */
	protected static final String MAX_INACTIVITY_TIME = "max-inactivity-time";

	/** Field description */
	protected static final String MAX_RECONNECTS_PROP_KEY = "max-reconnects";

	/** Field description */
	protected static final int NET_BUFFER_HT_PROP_VAL = 64 * 1024;

	/** Field description */
	protected static final String NET_BUFFER_PROP_KEY = "net-buffer";

	/** Field description */
	protected static final int NET_BUFFER_ST_PROP_VAL = 2 * 1024;

	/** Field description */
	protected static final String PORT_CLASS_PROP_KEY = "class";

	/** Field description */
	protected static final String PORT_IFC_PROP_KEY = "ifc";

	/** Field description */
	protected static final String PORT_KEY = "port-no";

	/** Field description */
	protected static final String PORT_REMOTE_HOST_PROP_KEY = "remote-host";

	/** Field description */
	protected static final String PORT_REMOTE_HOST_PROP_VAL = "localhost";

	/** Field description */
	protected static final String PORT_SOCKET_PROP_KEY = "socket";

	/** Field description */
	protected static final String PORT_TYPE_PROP_KEY = "type";

	/** Field description */
	protected static final String PROP_KEY = "connections/";

	/** Field description */
	protected static final long TOTAL_BIN_LIMIT_PROP_VAL = 0L;

	/** Field description */
	protected static final long TOTAL_PACKETS_LIMIT_PROP_VAL = 0L;

	/** Field description */
	protected static final String WHITE_CHAR_ACK_PROP_KEY = "white-char-ack";

	/** Field description */
	protected static final String XMPP_ACK_PROP_KEY   = "xmpp-ack";
	private static final Logger log                   =
		Logger.getLogger(ConnectionManager.class.getName());
	private static ConnectionOpenThread connectThread = ConnectionOpenThread.getInstance();

	/** Field description */
	protected static final boolean XMPP_ACK_PROP_VAL = false;

	/** Field description */
	protected static final boolean WHITE_CHAR_ACK_PROP_VAL = false;

	/** Field description */
	protected static final String PORTS_PROP_KEY = PROP_KEY + "ports";

	/** Field description */
	protected static final boolean TLS_USE_PROP_VAL = true;
	//J-
	/** Field description */
	protected static final String TLS_PROP_KEY = PROP_KEY + "tls/";

	/** Field description */
	protected static final String TLS_USE_PROP_KEY = TLS_PROP_KEY + "use";

	/** Field description */
	protected static final boolean TLS_REQUIRED_PROP_VAL = false;

	/** Field description */
	protected static final String TLS_REQUIRED_PROP_KEY = TLS_PROP_KEY + "required";
  //J+

	//~--- fields ---------------------------------------------------------------

	/** Field description. */
	public String[] PORT_IFC_PROP_VAL                    = { "*" };
	private long bytesReceived                           = 0;
	private long bytesSent                               = 0;
	private int services_size                            = 0;
	private long socketOverflow                          = 0;
	private Thread watchdog                              = null;
	private long watchdogRuns                            = 0;
	private long watchdogStopped                         = 0;
	private long watchdogTests                           = 0;
	private boolean white_char_ack                       = WHITE_CHAR_ACK_PROP_VAL;
	private boolean xmpp_ack                             = XMPP_ACK_PROP_VAL;
	private LinkedList<Map<String, Object>> waitingTasks = new LinkedList<Map<String,
																													 Object>>();
	private long total_packets_limit                 = TOTAL_PACKETS_LIMIT_PROP_VAL;
	private long total_bin_limit                     = TOTAL_BIN_LIMIT_PROP_VAL;
	private ConcurrentHashMap<String, IO> services   = new ConcurrentHashMap<String, IO>();
	private Set<ConnectionListenerImpl> pending_open =
		Collections.synchronizedSet(new HashSet<ConnectionListenerImpl>());;
	private long maxInactivityTime                  = getMaxInactiveTime();
	private long last_minute_packets_limit          = LAST_MINUTE_PACKETS_LIMIT_PROP_VAL;
	private long last_minute_bin_limit              = LAST_MINUTE_BIN_LIMIT_PROP_VAL;
	private IOServiceStatisticsGetter ioStatsGetter = new IOServiceStatisticsGetter();
	private boolean initializationCompleted         = false;

	/** Field description */
	protected int net_buffer = NET_BUFFER_ST_PROP_VAL;

	/** Field description */
	protected long connectionDelay       = 2 * SECOND;
	private LIMIT_ACTION xmppLimitAction = LIMIT_ACTION.DISCONNECT;

	//~--- constant enums -------------------------------------------------------

	/**
	 * Describe class <code>LIMIT_ACTION</code> here.
	 *
	 */
	public static enum LIMIT_ACTION { DISCONNECT, DROP_PACKETS; }

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 *
	 * @return
	 */
	public abstract Queue<Packet> processSocketData(IO serv);

	/**
	 * Method description
	 *
	 *
	 * @param port_props
	 */
	public abstract void reconnectionFailed(Map<String, Object> port_props);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	protected abstract long getMaxInactiveTime();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	protected abstract IO getXMPPIOServiceInstance();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	protected String getDefTrafficThrottling() {
		String result = ST_TRAFFIC_THROTTLING_PROP_VAL;

		if (isHighThroughput()) {
			result = HT_TRAFFIC_THROTTLING_PROP_VAL;
		}

		return result;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public synchronized void everyMinute() {
		super.everyMinute();

		// This variable used to provide statistics gets off on a busy
		// services as it is handled in methods called concurrently by
		// many threads. While accuracy of this variable is not critical
		// for the server functions, statistics should be as accurate as
		// possible to provide valuable metrics data.
		// So in the watchdog thread we re-synchronize this number
		int tmp = services.size();

		services_size = tmp;
		doForAllServices(ioStatsGetter);
	}

	/**
	 * Method description
	 *
	 *
	 * @param ht_def_key
	 * @param ht_dev_val
	 * @param st_def_key
	 * @param st_def_val
	 * @param prop_key
	 * @param prop_val_class
	 * @param params
	 * @param props
	 * @param <T>
	 */
	protected <T> void checkHighThroughputProperty(String ht_def_key, T ht_dev_val,
					String st_def_key, T st_def_val, String prop_key, Class<T> prop_val_class,
					Map<String, Object> params, Map<String, Object> props) {
		T tmp          = st_def_val;
		String str_tmp = null;

		if (isHighThroughput()) {
			tmp     = ht_dev_val;
			str_tmp = (String) params.get(ht_def_key);
		} else {
			tmp     = st_def_val;
			str_tmp = (String) params.get(st_def_key);
		}
		if (prop_val_class.isAssignableFrom(Integer.class)) {
			tmp = prop_val_class.cast(DataTypes.parseNum(str_tmp, Integer.class,
							(Integer) tmp));
		}
		if (prop_val_class.isAssignableFrom(Long.class)) {
			tmp = prop_val_class.cast(DataTypes.parseNum(str_tmp, Long.class, (Long) tmp));
		}
		if (prop_val_class.isAssignableFrom(String.class)) {
			tmp = prop_val_class.cast(str_tmp);
		}
		props.put(prop_key, tmp);

		// log.warning("Set property: (" + prop_key + ", " + tmp + ")");
	}

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
		log.log(Level.CONFIG, "{0} defaults: {1}", new Object[] { getName(),
						params.toString() });

		Map<String, Object> props = super.getDefaults(params);

		props.put(TLS_USE_PROP_KEY, TLS_USE_PROP_VAL);
		checkHighThroughputProperty(NET_BUFFER_HT_PROP_KEY, NET_BUFFER_HT_PROP_VAL,
																NET_BUFFER_ST_PROP_KEY, NET_BUFFER_ST_PROP_VAL,
																NET_BUFFER_PROP_KEY, Integer.class, params, props);
		checkHighThroughputProperty(HT_TRAFFIC_THROTTLING_PROP_KEY,
																getDefTrafficThrottling(),
																ST_TRAFFIC_THROTTLING_PROP_KEY,
																getDefTrafficThrottling(), TRAFFIC_THROTTLING_PROP_KEY,
																String.class, params, props);

		int[] ports      = null;
		String ports_str = (String) params.get("--" + getName() + "-ports");

		if (ports_str != null) {
			String[] ports_stra = ports_str.split(",");

			ports = new int[ports_stra.length];

			int k = 0;

			for (String p : ports_stra) {
				try {
					ports[k++] = Integer.parseInt(p);
				} catch (Exception e) {
					log.warning("Incorrect ports default settings: " + p);
				}
			}
		}

		int ports_size = 0;

		if (ports != null) {
			log.config("Port settings preset: " + Arrays.toString(ports));
			for (int port : ports) {
				putDefPortParams(props, port, SocketType.plain);
			}        // end of for (int i = 0; i < idx; i++)
			props.put(PORTS_PROP_KEY, ports);
		} else {
			int[] plains = getDefPlainPorts();

			if (plains != null) {
				ports_size += plains.length;
			}        // end of if (plains != null)

			int[] ssls = getDefSSLPorts();

			if (ssls != null) {
				ports_size += ssls.length;
			}        // end of if (ssls != null)
			if (ports_size > 0) {
				ports = new int[ports_size];
			}        // end of if (ports_size > 0)
			if (ports != null) {
				int idx = 0;

				if (plains != null) {
					idx = plains.length;
					for (int i = 0; i < idx; i++) {
						ports[i] = plains[i];
						putDefPortParams(props, ports[i], SocketType.plain);
					}    // end of for (int i = 0; i < idx; i++)
				}      // end of if (plains != null)
				if (ssls != null) {
					for (int i = idx; i < idx + ssls.length; i++) {
						ports[i] = ssls[i - idx];
						putDefPortParams(props, ports[i], SocketType.ssl);
					}    // end of for (int i = 0; i < idx + ssls.length; i++)
				}      // end of if (ssls != null)
				props.put(PORTS_PROP_KEY, ports);
			}        // end of if (ports != null)
		}

		String acks = (String) params.get(XMPP_STANZA_ACK);

		if (acks != null) {
			String[] acks_arr = acks.split(",");

			for (String ack_type : acks_arr) {
				if (STANZA_WHITE_CHAR_ACK.equals(ack_type)) {
					white_char_ack = true;
				}
				if (STANZA_XMPP_ACK.equals(ack_type)) {
					xmpp_ack = true;
				}
			}
		}
		props.put(WHITE_CHAR_ACK_PROP_KEY, white_char_ack);
		props.put(XMPP_ACK_PROP_KEY, xmpp_ack);
		props.put(MAX_INACTIVITY_TIME, getMaxInactiveTime() / SECOND);

		return props;
	}

	/**
	 * Generates the component statistics.
	 *
	 * @param list
	 *          is a collection to put the component statistics in.
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "Open connections", services_size, Level.INFO);
		if (list.checkLevel(Level.FINEST) || (services.size() < 1000)) {
			int waitingToSendSize = 0;

			for (IO serv : services.values()) {
				waitingToSendSize += serv.waitingToSendSize();
			}
			list.add(getName(), "Waiting to send", waitingToSendSize, Level.FINE);
		}
		list.add(getName(), "Bytes sent", bytesSent, Level.FINE);
		list.add(getName(), "Bytes received", bytesReceived, Level.FINE);
		list.add(getName(), "Socket overflow", socketOverflow, Level.FINE);
		list.add(getName(), "Watchdog runs", watchdogRuns, Level.FINER);
		list.add(getName(), "Watchdog tests", watchdogTests, Level.FINE);
		list.add(getName(), "Watchdog stopped", watchdogStopped, Level.FINE);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * This method can be overwritten in extending classes to get a different
	 * packets distribution to different threads. For PubSub, probably better
	 * packets distribution to different threads would be based on the sender
	 * address rather then destination address.
	 *
	 * @param packet
	 * @return
	 */
	@Override
	public int hashCodeForPacket(Packet packet) {
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}
		if (packet.getTo() != null) {
			return packet.getTo().hashCode();
		}

		return super.hashCodeForPacket(packet);
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
		binds.put(CommandIfc.SERVICES_MAP, services);
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void initializationCompleted() {
		super.initializationCompleted();
		initializationCompleted = true;
		for (Map<String, Object> params : waitingTasks) {
			reconnectService(params, connectionDelay);
		}
		waitingTasks.clear();
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 *
	 * @throws IOException
	 */
	@Override
	public void packetsReady(IO serv) throws IOException {

		// Under a high load data, especially lots of packets on a single
		// connection it may happen that one threads started processing
		// socketData and then another thread reads more packets which
		// may take over earlier data depending on a thread scheduler used.
		// synchronized (serv) {
		if (checkTrafficLimits(serv)) {
			writePacketsToSocket(serv, processSocketData(serv));
		}

		// }
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 *
	 * @return
	 */
	@SuppressWarnings("empty-statement")
	public boolean checkTrafficLimits(IO serv) {
		boolean xmppLimitHit = false;

		if (last_minute_packets_limit > 0) {
			xmppLimitHit = (serv.getPacketsReceived(false) >= last_minute_packets_limit) ||
										 (serv.getPacketsSent(false) >= last_minute_packets_limit);
		}
		if (!xmppLimitHit && (total_packets_limit > 0)) {
			xmppLimitHit = (serv.getTotalPacketsReceived() >= total_packets_limit) ||
										 (serv.getTotalPacketsSent() >= total_packets_limit);
		}
		if (xmppLimitHit) {
			Level level = Level.FINER;

			if (isHighThroughput()) {
				level = Level.WARNING;
			}
			switch (xmppLimitAction) {
			case DROP_PACKETS :
				if (log.isLoggable(level)) {
					log.log(level,
									"[[{0}]] XMPP Limits exceeded on connection {1}" +
									" dropping pakcets: {2}", new Object[] { getName(),
									serv, serv.getReceivedPackets() });
				}
				while (serv.getReceivedPackets().poll() != null);

				break;

			default :
				if (log.isLoggable(level)) {
					log.log(level,
									"[[{0}]] XMPP Limits exceeded on connection {1}" +
									" stopping, packets dropped: {2}", new Object[] { getName(),
									serv, serv.getReceivedPackets() });
				}
				serv.forceStop();

				break;
			}

			return false;
		}

		boolean binLimitHit = false;
		long bytesSent      = serv.getBytesSent(false);
		long bytesReceived  = serv.getBytesReceived(false);

		if (last_minute_bin_limit > 0) {
			binLimitHit = (bytesSent >= last_minute_bin_limit) ||
										(bytesReceived >= last_minute_bin_limit);
		}

		long totalSent     = serv.getTotalBytesSent();
		long totalReceived = serv.getTotalBytesReceived();

		if (!binLimitHit && (total_bin_limit > 0)) {
			binLimitHit = (totalReceived >= total_bin_limit) || (totalSent >= total_bin_limit);
		}
		if (binLimitHit) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
								"[[{0}]] Binary Limits exceeded ({1}:{2}:{3}:{4}) on" +
								" connection {5} stopping, packets dropped: {6}", new Object[] {
					getName(), bytesSent, bytesReceived, totalSent, totalReceived, serv,
					serv.getReceivedPackets()
				});
			}
			serv.forceStop();

			return false;
		}

		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {
		writePacketToSocket(packet);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int processingInThreads() {
		return Runtime.getRuntime().availableProcessors() * 4;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int processingOutThreads() {
		return Runtime.getRuntime().availableProcessors() * 4;
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void release() {

		// delayedTasks.cancel();
		releaseListeners();
		super.release();
	}

	/**
	 * Method description
	 *
	 *
	 * @param service
	 */
	@TODO(note = "Do something if service with the same unique ID is already started, " +
							 "possibly kill the old one...")
	public void serviceStarted(final IO service) {

		// synchronized(services) {
		String id = getUniqueId(service);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "[[{0}]] Connection started: {1}", new Object[] { getName(),
							service });
		}

		IO serv = services.get(id);

		if (serv != null) {
			if (serv == service) {
				log.log(Level.WARNING,
								"{0}: That would explain a lot, adding the same service twice, ID: {1}",
								new Object[] { getName(),
															 serv });
			} else {

				// Is it at all possible to happen???
				// let's log it for now....
				log.log(Level.WARNING,
								"{0}: Attempt to add different service with the same ID: {1}",
								new Object[] { getName(),
															 service });

				// And stop the old service....
				serv.stop();
			}
		}
		services.put(id, service);
		++services_size;

		// }
	}

	/**
	 *
	 * @param service
	 * @return
	 */
	@Override
	public boolean serviceStopped(IO service) {

		// Hopefuly there is no exception at this point, but just in case...
		// This is a very fresh code after all
		try {
			ioStatsGetter.check(service);
		} catch (Exception e) {
			log.log(Level.INFO,
							"Nothing serious to worry about but please notify the developer.", e);
		}

		// synchronized(service) {
		String id = getUniqueId(service);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "[[{0}]] Connection stopped: {1}", new Object[] { getName(),
							service });
		}

		// id might be null if service is stopped in accept method due to
		// an exception during establishing TCP/IP connection
		// IO serv = (id != null ? services.get(id) : null);
		if (id != null) {
			boolean result = services.remove(id, service);

			if (result) {
				--services_size;
			} else if (log.isLoggable(Level.FINER)) {

				// Is it at all possible to happen???
				// let's log it for now....
				log.log(Level.FINER, "[[{0}]] Attempt to stop incorrect service: {1}",
								new Object[] { getName(),
															 service });
			}

			return result;
		}

		return false;

		// }
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
		watchdog = new Thread(new Watchdog(), "Watchdog - " + name);
		watchdog.setDaemon(true);
		watchdog.start();
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
		if (props.get(MAX_INACTIVITY_TIME) != null) {
			maxInactivityTime = (Long) props.get(MAX_INACTIVITY_TIME) * SECOND;
		}
		if (props.get(WHITE_CHAR_ACK_PROP_KEY) != null) {
			white_char_ack = (Boolean) props.get(WHITE_CHAR_ACK_PROP_KEY);
		}
		if (props.get(XMPP_ACK_PROP_KEY) != null) {
			xmpp_ack = (Boolean) props.get(XMPP_ACK_PROP_KEY);
		}
		if (props.get(NET_BUFFER_PROP_KEY) != null) {
			net_buffer = (Integer) props.get(NET_BUFFER_PROP_KEY);
		}
		if (props.get(TRAFFIC_THROTTLING_PROP_KEY) != null) {
			String[] tmp = ((String) props.get(TRAFFIC_THROTTLING_PROP_KEY)).split(",");

			for (String tmp_s : tmp) {
				String[] tmp_thr = tmp_s.split(":");

				if (tmp_thr[0].equalsIgnoreCase("xmpp")) {
					last_minute_packets_limit = DataTypes.parseNum(tmp_thr[1], Long.class,
									LAST_MINUTE_PACKETS_LIMIT_PROP_VAL);
					log.warning(getName() + " last_minute_packets_limit = " +
											last_minute_packets_limit);
					total_packets_limit = DataTypes.parseNum(tmp_thr[2], Long.class,
									TOTAL_PACKETS_LIMIT_PROP_VAL);
					log.warning(getName() + " total_packets_limit = " + total_packets_limit);
					if (tmp_thr[3].equalsIgnoreCase("disc")) {
						xmppLimitAction = LIMIT_ACTION.DISCONNECT;
					}
					if (tmp_thr[3].equalsIgnoreCase("drop")) {
						xmppLimitAction = LIMIT_ACTION.DROP_PACKETS;
					}
				}
				if (tmp_thr[0].equalsIgnoreCase("bin")) {
					last_minute_bin_limit = DataTypes.parseNum(tmp_thr[1], Long.class,
									LAST_MINUTE_BIN_LIMIT_PROP_VAL);
					log.warning(getName() + " last_minute_bin_limit = " + last_minute_bin_limit);
					total_bin_limit = DataTypes.parseNum(tmp_thr[2], Long.class,
									TOTAL_BIN_LIMIT_PROP_VAL);
					log.warning(getName() + " total_bin_limit = " + total_bin_limit);
				}
			}
		}
		if (props.size() == 1) {

			// If props.size() == 1, it means this is a single property update and
			// ConnectionManager does not support it yet.
			return;
		}
		releaseListeners();

		int[] ports = (int[]) props.get(PORTS_PROP_KEY);

		if (ports != null) {
			for (int i = 0; i < ports.length; i++) {
				Map<String, Object> port_props = new LinkedHashMap<String, Object>(20);

				for (Map.Entry<String, Object> entry : props.entrySet()) {
					if (entry.getKey().startsWith(PROP_KEY + ports[i])) {
						int idx    = entry.getKey().lastIndexOf('/');
						String key = entry.getKey().substring(idx + 1);

						log.log(Level.CONFIG, "Adding port property key: {0}={1}", new Object[] { key,
										entry.getValue() });
						port_props.put(key, entry.getValue());
					}    // end of if (entry.getKey().startsWith())
				}      // end of for ()
				port_props.put(PORT_KEY, ports[i]);
				addWaitingTask(port_props);

				// reconnectService(port_props, startDelay);
			}        // end of for (int i = 0; i < ports.length; i++)
		}          // end of if (ports != null)
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param ios
	 * @param p
	 *
	 * @return
	 */
	public boolean writePacketToSocket(IO ios, Packet p) {
		if (ios != null) {
			if (log.isLoggable(Level.FINER) &&!log.isLoggable(Level.FINEST)) {
				log.log(Level.FINER, "{0}, Processing packet: {1}, type: {2}", new Object[] { ios,
								p.getElemName(), p.getType() });
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Writing packet: {1}", new Object[] { ios, p });
			}

			// synchronized (ios) {
			ios.addPacketToSend(p);
			if (ios.writeInProgress.tryLock()) {
				try {
					ios.processWaitingPackets();
					SocketThread.addSocketService(ios);

					return true;
				} catch (Exception e) {
					log.log(Level.WARNING, ios + "Exception during writing packets: ", e);
					try {
						ios.stop();
					} catch (Exception e1) {
						log.log(Level.WARNING, ios + "Exception stopping XMPPIOService: ", e1);
					}    // end of try-catch
				} finally {
					ios.writeInProgress.unlock();
				}
			}

			// }
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Can''t find service for packet: <{0}> {1}, service id: {2}",
								new Object[] { p.getElemName(),
															 p.getTo(), getServiceId(p) });
			}
		}    // end of if (ios != null) else

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param packets
	 */
	public void writePacketsToSocket(IO serv, Queue<Packet> packets) {
		if (serv != null) {

			// synchronized (serv) {
			if ((packets != null) && (packets.size() > 0)) {
				Packet p = null;

				while ((p = packets.poll()) != null) {
					if (log.isLoggable(Level.FINER) &&!log.isLoggable(Level.FINEST)) {
						log.log(Level.FINER, "{0}, Processing packet: {1}, type: {2}",
										new Object[] { serv,
																	 p.getElemName(), p.getType() });
					}
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Writing packet: {1}", new Object[] { serv, p });
					}
					serv.addPacketToSend(p);
				}      // end of for ()
				try {
					serv.processWaitingPackets();
					SocketThread.addSocketService(serv);
				} catch (Exception e) {
					log.log(Level.WARNING, serv + "Exception during writing packets: ", e);
					try {
						serv.stop();
					} catch (Exception e1) {
						log.log(Level.WARNING, serv + "Exception stopping XMPPIOService: ", e1);
					}    // end of try-catch
				}      // end of try-catch
			}

			// }
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Can't find service for packets: [{0}] ", packets);
			}
		}          // end of if (ios != null) else
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn
	 */
	protected void addWaitingTask(Map<String, Object> conn) {
		if (initializationCompleted) {
			reconnectService(conn, connectionDelay);
		} else {
			waitingTasks.add(conn);
		}
	}

	/**
	 * Returns number of active network connections (IOServices).
	 *
	 * @return number of active network connections (IOServices).
	 */
	protected int countIOServices() {
		return services.size();
	}

	/**
	 * Perform a given action defined by ServiceChecker for all active IOService
	 * objects (active network connections).
	 *
	 * @param checker
	 *          is a <code>ServiceChecker</code> instance defining an action to
	 *          perform for all IOService objects.
	 */
	protected void doForAllServices(ServiceChecker<IO> checker) {
		for (IO service : services.values()) {
			checker.check(service);
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	protected int[] getDefPlainPorts() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	protected int[] getDefSSLPorts() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param port
	 *
	 * @return
	 */
	protected Map<String, Object> getParamsForPort(int port) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 */
	protected String getServiceId(Packet packet) {
		return getServiceId(packet.getTo());
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	protected String getServiceId(JID jid) {
		return jid.getResource();
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 *
	 * @return
	 */
	protected String getUniqueId(IO serv) {
		return serv.getUniqueId();
	}

	/**
	 * Method description
	 *
	 *
	 * @param serviceId
	 *
	 * @return
	 */
	protected IO getXMPPIOService(String serviceId) {
		return services.get(serviceId);
	}

	/**
	 * Method description
	 *
	 *
	 * @param p
	 *
	 * @return
	 */
	protected IO getXMPPIOService(Packet p) {
		return services.get(getServiceId(p));
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	protected boolean isHighThroughput() {
		return false;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 *
	 * @param p
	 * @return
	 */
	protected boolean writePacketToSocket(Packet p) {
		IO ios = getXMPPIOService(p);

		if (ios != null) {
			return writePacketToSocket(ios, p);
		} else {
			return false;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param p
	 * @param serviceId
	 *
	 * @return
	 */
	protected boolean writePacketToSocket(Packet p, String serviceId) {
		IO ios = getXMPPIOService(serviceId);

		if (ios != null) {
			return writePacketToSocket(ios, p);
		} else {
			return false;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param ios
	 * @param data
	 */
	protected void writeRawData(IO ios, String data) {
		try {
			ios.writeRawData(data);
			SocketThread.addSocketService(ios);
		} catch (Exception e) {
			log.log(Level.WARNING, ios + "Exception during writing data: " + data, e);
			try {
				ios.stop();
			} catch (Exception e1) {
				log.log(Level.WARNING, ios + "Exception stopping XMPPIOService: ", e1);
			}    // end of try-catch
		}
	}

	private void putDefPortParams(Map<String, Object> props, int port, SocketType sock) {
		log.log(Level.CONFIG, "Generating defaults for port: {0}", port);
		props.put(PROP_KEY + port + "/" + PORT_TYPE_PROP_KEY, ConnectionType.accept);
		props.put(PROP_KEY + port + "/" + PORT_SOCKET_PROP_KEY, sock);
		props.put(PROP_KEY + port + "/" + PORT_IFC_PROP_KEY, PORT_IFC_PROP_VAL);
		props.put(PROP_KEY + port + "/" + PORT_REMOTE_HOST_PROP_KEY,
							PORT_REMOTE_HOST_PROP_VAL);
		props.put(PROP_KEY + port + "/" + TLS_REQUIRED_PROP_KEY, TLS_REQUIRED_PROP_VAL);

		Map<String, Object> extra = getParamsForPort(port);

		if (extra != null) {
			for (Map.Entry<String, Object> entry : extra.entrySet()) {
				props.put(PROP_KEY + port + "/" + entry.getKey(), entry.getValue());
			}    // end of for ()
		}      // end of if (extra != null)
	}

	private void reconnectService(final Map<String, Object> port_props, long delay) {
		if (log.isLoggable(Level.FINER)) {
			String cid = "" + port_props.get("local-hostname") + "@" +
									 port_props.get("remote-hostname");

			log.log(Level.FINER,
							"Reconnecting service for: {0}, scheduling next try in {1}secs, cid: {2}",
							new Object[] { getName(),
														 delay / 1000, cid });
		}
		addTimerTask(new TimerTask() {
			@Override
			public void run() {
				String host = (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);

				if (host == null) {
					host = (String) port_props.get("remote-hostname");
				}

				int port = (Integer) port_props.get(PORT_KEY);

				if (log.isLoggable(Level.FINE)) {
					log.log(
							Level.FINE,
							"Reconnecting service for component: {0}, to remote host: {1} on port: {2}",
							new Object[] { getName(),
														 host, port });
				}
				startService(port_props);
			}
		}, delay);
	}

	private void releaseListeners() {
		for (ConnectionListenerImpl cli : pending_open) {
			connectThread.removeConnectionOpenListener(cli);
		}
		pending_open.clear();
	}

	private void startService(Map<String, Object> port_props) {
		if (port_props == null) {
			throw new NullPointerException("port_props cannot be null.");
		}

		ConnectionListenerImpl cli = new ConnectionListenerImpl(port_props);

		if (cli.getConnectionType() == ConnectionType.accept) {
			pending_open.add(cli);
		}
		connectThread.addConnectionOpenListener(cli);
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void stop() {
		this.releaseListeners();

		// when stopping connection manager we need to stop all active connections as well
		for (IO service : services.values()) {
			service.forceStop();
		}
		super.stop();
	}

	//~--- inner classes --------------------------------------------------------

	private class ConnectionListenerImpl
					implements ConnectionOpenListener {
		private Map<String, Object> port_props = null;

		//~--- constructors -------------------------------------------------------

		private ConnectionListenerImpl(Map<String, Object> port_props) {
			this.port_props = port_props;
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param sc
		 */
		@Override
		public void accept(SocketChannel sc) {
			String cid = "" + port_props.get("local-hostname") + "@" +
									 port_props.get("remote-hostname");

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Accept called for service: {0}", cid);
			}

			IO serv = getXMPPIOServiceInstance();

			serv.setIOServiceListener(ConnectionManager.this);
			serv.setSessionData(port_props);
			try {
				serv.accept(sc);
				if (getSocketType() == SocketType.ssl) {
					serv.startSSL(false);
				}    // end of if (socket == SocketType.ssl)
				serviceStarted(serv);
				SocketThread.addSocketService(serv);
			} catch (SocketException e) {
				if (getConnectionType() == ConnectionType.connect) {

					// Accept side for component service is not ready yet?
					// Let's wait for a few secs and try again.
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Problem reconnecting the service: {0}, cid: {1}",
										new Object[] { serv,
																	 cid });
					}

					boolean reconnect  = false;
					Integer reconnects = (Integer) port_props.get(MAX_RECONNECTS_PROP_KEY);

					if (reconnects != null) {
						int recon = reconnects.intValue();

						if (recon != 0) {
							port_props.put(MAX_RECONNECTS_PROP_KEY, (--recon));
							reconnect = true;
						}    // end of if (recon != 0)
					}
					if (reconnect) {
						reconnectService(port_props, connectionDelay);
					} else {
						reconnectionFailed(port_props);
					}
				} else {

					// Ignore
				}
			} catch (Exception e) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Can not accept connection cid: " + cid, e);
				}
				log.log(Level.WARNING, "Can not accept connection.", e);
				serv.stop();
			}          // end of try-catch
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public ConnectionType getConnectionType() {
			String type = null;

			if (port_props.get(PORT_TYPE_PROP_KEY) == null) {
				log.warning(getName() + ": connection type is null: " +
										port_props.get(PORT_KEY).toString());
			} else {
				type = port_props.get(PORT_TYPE_PROP_KEY).toString();
			}

			return ConnectionType.valueOf(type);
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String[] getIfcs() {
			return (String[]) port_props.get(PORT_IFC_PROP_KEY);
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getPort() {
			return (Integer) port_props.get(PORT_KEY);
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getReceiveBufferSize() {
			return net_buffer;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public SocketType getSocketType() {
			return SocketType.valueOf(port_props.get(PORT_SOCKET_PROP_KEY).toString());
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getTrafficClass() {
			if (isHighThroughput()) {
				return IPTOS_THROUGHPUT;
			} else {
				return DEF_TRAFFIC_CLASS;
			}
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return port_props.toString();
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getSRVType() {
			String type = (String) this.port_props.get("srv-type");

			if ((type == null) || type.isEmpty()) {
				return null;
			}

			return type;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getRemoteHostname() {
			if (port_props.containsKey(PORT_REMOTE_HOST_PROP_KEY)) {
				return (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);
			}

			return (String) port_props.get("remote-hostname");
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public InetSocketAddress getRemoteAddress() {
			return (InetSocketAddress) port_props.get("remote-address");
		}
	}


	private class IOServiceStatisticsGetter
					implements ServiceChecker<IO> {
		private StatisticsList list = new StatisticsList(Level.ALL);

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param service
		 */
		@Override
		public synchronized void check(IO service) {
			bytesReceived  += service.getBytesReceived(true);
			bytesSent      += service.getBytesSent(true);
			socketOverflow += service.getBuffOverflow(true);
			service.getPacketsReceived(true);
			service.getPacketsSent(true);

			// service.getStatistics(list, true);
			// bytesReceived += list.getValue("socketio", "Bytes received", -1l);
			// bytesSent += list.getValue("socketio", "Bytes sent", -1l);
			// socketOverflow += list.getValue("socketio", "Buffers overflow", -1l);
		}
	}


	/**
	 * Looks in all established connections and checks whether any of them is
	 * dead....
	 *
	 */
	private class Watchdog
					implements Runnable {
		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			while (true) {
				try {

					// Sleep...
					Thread.sleep(10 * MINUTE);
					++watchdogRuns;

					// Walk through all connections and check whether they are
					// really alive...., try to send space for each service which
					// is inactive for hour or more and close the service
					// on Exception
					doForAllServices(new ServiceChecker<IO>() {
						@Override
						public void check(final XMPPIOService service) {
							try {
								if (null != service) {
									long curr_time    = System.currentTimeMillis();
									long lastTransfer = service.getLastTransferTime();

									if (curr_time - lastTransfer >= maxInactivityTime) {

										// Stop the service is max keep-alive time is exceeded
										// for non-active connections.
										if (log.isLoggable(Level.INFO)) {
											log.log(Level.INFO,
															"{0}: Max inactive time exceeded, stopping: {1}",
															new Object[] { getName(),
																						 service });
										}
										++watchdogStopped;
										service.stop();
									} else {
										if (curr_time - lastTransfer >= (29 * MINUTE)) {

											// At least once an hour check if the connection is
											// still alive.
											service.writeRawData(" ");
											++watchdogTests;
										}
									}
								}
							} catch (Exception e) {

								// Close the service....
								try {
									if (service != null) {
										log.info(getName() + "Found dead connection, stopping: " + service);
										++watchdogStopped;
										service.forceStop();
									}
								} catch (Exception ignore) {

									// Do nothing here as we expect Exception to be thrown here...
								}
							}
						}
					});
				} catch (InterruptedException e) {    /* Do nothing here */
				}
			}
		}
	}
}    // ConnectionManager


//~ Formatted in Tigase Code Convention on 13/02/18
