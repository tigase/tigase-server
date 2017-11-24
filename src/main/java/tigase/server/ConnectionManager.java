/*
 * ConnectionManager.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;
import tigase.conf.ConfigurationException;
import tigase.net.*;
import tigase.server.script.CommandIfc;
import tigase.server.xmppclient.XMPPIOProcessor;
import tigase.stats.StatisticsList;
import tigase.util.DataTypes;
import tigase.xml.Element;
import tigase.xmpp.*;

import javax.script.Bindings;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.XMPPIOService.DOM_HANDLER;


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
	public static final String HT_TRAFFIC_THROTTLING_PROP_KEY =
			"--cm-ht-traffic-throttling";

	public static final String HT_TRAFFIC_THROTTLING_PROP_VAL =
			"xmpp:50k:0:disc,bin:400m:0:disc";

	public static final String NET_BUFFER_HT_PROP_KEY = "--net-buff-high-throughput";

	public static final String NET_BUFFER_ST_PROP_KEY = "--net-buff-standard";

	public static final String PORT_LOCAL_HOST_PROP_KEY = "local-host";

	public static final String ST_TRAFFIC_THROTTLING_PROP_KEY = "--cm-traffic-throttling";

	public static final String ST_TRAFFIC_THROTTLING_PROP_VAL =
			"xmpp:2500:0:disc,bin:20m:0:disc";

	public static final String TRAFFIC_THROTTLING_PROP_KEY = "traffic-throttling";

	protected static final long LAST_MINUTE_BIN_LIMIT_PROP_VAL = 20000000L;

	protected static final long LAST_MINUTE_PACKETS_LIMIT_PROP_VAL = 2500L;

	protected static final String MAX_INACTIVITY_TIME = "max-inactivity-time";

	protected static final String MAX_RECONNECTS_PROP_KEY = "max-reconnects";

	protected static final int NET_BUFFER_HT_PROP_VAL = 64 * 1024;

	protected static final String NET_BUFFER_PROP_KEY = "net-buffer";

	protected static final int NET_BUFFER_ST_PROP_VAL = 2 * 1024;

	protected static final int NET_BUFFER_LIMIT_HT_PROP_VAL = 20*1024*1024;

	protected static final String NET_BUFFER_LIMIT_PROP_KEY= "net-buffer-limit";

	protected static final int NET_BUFFER_LIMIT_ST_PROP_VAL = 2*1024*1024;
	/**
	 * Key name of the system property for configuration protection
	 * from system overload and DOS attack.
	 */
	public static final String ELEMENTS_NUMBER_LIMIT_PROP_KEY = "elements-number-limit";

	/**
	 * Default value for the system property for configuration protection
	 * from system overload and DOS attack.
	 */
	public static int ELEMENTS_NUMBER_LIMIT_PROP_VAL = 1000;

	protected static final String PORT_CLASS_PROP_KEY = "class";

	protected static final String PORT_IFC_PROP_KEY = "ifc";

	protected static final String PORT_LISTENING_DELAY_KEY = "port-delay-listening";
	protected static final boolean PORT_LISTENING_DELAY_DEF = false;

	protected static final String PORT_KEY = "port-no";

	protected static final String PORT_REMOTE_HOST_PROP_KEY = "remote-host";

	protected static final String PORT_REMOTE_HOST_PROP_VAL = "localhost";

	protected static final String PORT_SOCKET_PROP_KEY = "socket";

	protected static final String PORT_TYPE_PROP_KEY = "type";

	protected static final String PROP_KEY = "connections/";

	protected static final long TOTAL_BIN_LIMIT_PROP_VAL = 0L;

	protected static final long TOTAL_PACKETS_LIMIT_PROP_VAL = 0L;

	protected static final String WHITE_CHAR_ACK_PROP_KEY = "white-char-ack";

	protected static final String       XMPP_ACK_PROP_KEY = "xmpp-ack";
	private static final Logger         log = Logger.getLogger(ConnectionManager.class
			.getName());
	private static ConnectionOpenThread connectThread = ConnectionOpenThread.getInstance();

	protected static final boolean XMPP_ACK_PROP_VAL = false;

	protected static final boolean WHITE_CHAR_ACK_PROP_VAL = false;

	protected static final String PORTS_PROP_KEY = PROP_KEY + "ports";

	protected static final boolean TLS_USE_PROP_VAL = true;
	//J-
	protected static final String TLS_PROP_KEY = PROP_KEY + "tls/";

	protected static final String TLS_USE_PROP_KEY = TLS_PROP_KEY + "use";

	protected static final boolean TLS_REQUIRED_PROP_VAL = false;

	protected static final String TLS_REQUIRED_PROP_KEY = TLS_PROP_KEY + "required";

	protected static final String WATCHDOG_DELAY = "watchdog_delay";
	protected static final String WATCHDOG_TIMEOUT = "watchdog_timeout";
	protected static final String WATCHDOG_PING_TYPE_KEY = "watchdog_ping_type";

	protected static final Element pingElement = new Element( "iq",
								new Element[] { new Element( "ping", new String[] { "xmlns" }, new String[] { "urn:xmpp:ping" } ) },
								new String[] { "type", "id" },
								new String[] { "get", "tigase-ping" } );

	//~--- fields ---------------------------------------------------------------

	public String[]                         PORT_IFC_PROP_VAL = { "*" };
	private long                            bytesReceived     = 0;
	private long                            bytesSent         = 0;
	private int                             services_size     = 0;
	private long                            socketOverflow    = 0;
	private Thread                          watchdog          = null;
	private long                            watchdogRuns      = 0;
	private long                            watchdogStopped   = 0;
	private long                            watchdogTests     = 0;
	protected long                          watchdogDelay     = 10 * MINUTE; // 600 000
	protected long                          watchdogTimeout   = 29 * MINUTE; // 1 740 000
	private boolean                         white_char_ack    = WHITE_CHAR_ACK_PROP_VAL;
	private boolean                         xmpp_ack          = XMPP_ACK_PROP_VAL;
	private LinkedList<Map<String, Object>> waitingTasks = new LinkedList<Map<String,
			Object>>();
	private long                          total_packets_limit =
			TOTAL_PACKETS_LIMIT_PROP_VAL;
	private long                          total_bin_limit     = TOTAL_BIN_LIMIT_PROP_VAL;
	/**
	 * Protection from the system overload and DOS attack. We want to limit number
	 * of elements created within a single XMPP stanza.
	 *
	 */
	protected int elements_number_limit = 0;
	private ConcurrentHashMap<String, IO> services = new ConcurrentHashMap<String, IO>();
	private Set<ConnectionListenerImpl>   pending_open = Collections.synchronizedSet(
			new HashSet<ConnectionListenerImpl>());;
	private long                      maxInactivityTime       = getMaxInactiveTime();
	private long                      last_minute_packets_limit =
			LAST_MINUTE_PACKETS_LIMIT_PROP_VAL;
	private long                      last_minute_bin_limit =
			LAST_MINUTE_BIN_LIMIT_PROP_VAL;
	private int net_buffer_limit = 0;
	private IOServiceStatisticsGetter ioStatsGetter = new IOServiceStatisticsGetter();
	private boolean                   initializationCompleted = false;
	protected XMPPIOProcessor[]       processors              = new XMPPIOProcessor[0];

	protected int net_buffer = NET_BUFFER_ST_PROP_VAL;

    protected long connectionDelay = 2 * SECOND;
    private LIMIT_ACTION xmppLimitAction = LIMIT_ACTION.DISCONNECT;
    protected WATCHDOG_PING_TYPE watchdogPingType = WATCHDOG_PING_TYPE.WHITESPACE;
    protected boolean delayPortListening = false;

	//~--- constant enums -------------------------------------------------------

	/**
	 * Describe class <code>LIMIT_ACTION</code> here.
	 *
	 */
	public static enum LIMIT_ACTION { DISCONNECT, DROP_PACKETS; }
	/** Holds possible types of ping to be used in watchdog service for detection
	 * of broken connections */
	public static enum WATCHDOG_PING_TYPE { WHITESPACE, XMPP; }


	//~--- methods --------------------------------------------------------------

	public boolean checkTrafficLimits(IO serv) {
		boolean xmppLimitHit = false;

		if (last_minute_packets_limit > 0) {
			xmppLimitHit = (serv.getPacketsReceived(false) >= last_minute_packets_limit)
//							|| (serv.getPacketsSent(false) >= last_minute_packets_limit)
							;
		}
		if (!xmppLimitHit && (total_packets_limit > 0)) {
			xmppLimitHit = (serv.getTotalPacketsReceived() >= total_packets_limit)
//							|| (serv.getTotalPacketsSent() >= total_packets_limit)
							;
		}
		if (xmppLimitHit) {
			Level level = Level.FINER;

			if (isHighThroughput()) {
				level = Level.WARNING;
			}
			switch (xmppLimitAction) {
			case DROP_PACKETS :
				if (log.isLoggable(level)) {
					log.log(level, "[[{0}]] XMPP Limits exceeded on connection {1}" +
							" dropping pakcets: {2}", new Object[] { getName(),
							serv, serv.getReceivedPackets() });
				}
				while (serv.getReceivedPackets().poll() != null);

				break;

			default :
				if (log.isLoggable(level)) {
					log.log(level, "[[{0}]] XMPP Limits exceeded on connection {1}" +
							" stopping, packets dropped: {2}", new Object[] { getName(),
							serv, serv.getReceivedPackets() });
				}
				serv.forceStop();

				break;
			}

			return false;
		}

		boolean binLimitHit   = false;
		long    bytesSent     = serv.getBytesSent(false);
		long    bytesReceived = serv.getBytesReceived(false);

		if (last_minute_bin_limit > 0) {
			binLimitHit = (bytesSent >= last_minute_bin_limit) || (bytesReceived >=
					last_minute_bin_limit);
		}

		long totalSent     = serv.getTotalBytesSent();
		long totalReceived = serv.getTotalBytesReceived();

		if (!binLimitHit && (total_bin_limit > 0)) {
			binLimitHit = (totalReceived >= total_bin_limit) || (totalSent >= total_bin_limit);
		}
		if (binLimitHit) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "[[{0}]] Binary Limits exceeded ({1}:{2}:{3}:{4}) on" +
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

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CommandIfc.SERVICES_MAP, services);
	}

	@Override
	public void initializationCompleted() {
		if (isInitializationComplete()) {

			// Do we really need to do this again?
			return;
		}
		super.initializationCompleted();
		initializationCompleted = true;

        if (!delayPortListening) {
            connectWaitingTasks();
        }
    }

    protected void connectWaitingTasks() {
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "Connecting waitingTasks: {0}",
                    new Object[]{waitingTasks});
        }

        for (Map<String, Object> params : waitingTasks) {
            reconnectService(params, connectionDelay);
        }
        waitingTasks.clear();
        if (null != watchdog && Thread.State.NEW.equals(watchdog.getState())) {
            watchdog.start();
        }
        delayPortListening = false;
    }

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

	}

	@Override
	public int processingInThreads() {
		return Runtime.getRuntime().availableProcessors() * 4;
	}

	@Override
	public int processingOutThreads() {
		return Runtime.getRuntime().availableProcessors() * 4;
	}

	@Override
	public void processPacket(Packet packet) {
		writePacketToSocket(packet);
	}

	public abstract Queue<Packet> processSocketData(IO serv);

	/**
	 * Processes undelivered packets
	 * @param packet
	 * @param stamp - timestamp when packet was received to be written to XMPPIOService
	 * @param errorMessage
	 */
	public abstract boolean processUndeliveredPacket(Packet packet, Long stamp, String errorMessage);

	public abstract void reconnectionFailed(Map<String, Object> port_props);

	@Override
	public void release() {

		// delayedTasks.cancel();
		releaseListeners();
		super.release();
	}

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
				log.log(Level.FINE,
						"{0}: Attempt to add different service with the same ID: {1}; old: {2} (stopped)", new Object[] {
						getName(), service, serv });
				// And stop the old service....
				serv.stop();
			}
		}
		services.put(id, service);
		++services_size;

		// }
	}

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
				
				Queue<Packet> undeliveredPackets = service.getWaitingPackets();
				Packet p = null;
				while ((p = undeliveredPackets.poll()) != null) {
					processUndeliveredPacket(p, null, null);
				}
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

	}

	@Override
	public void stop() {
		this.releaseListeners();

		// when stopping connection manager we need to stop all active connections as well
		for (IO service : services.values()) {
			service.forceStop();
		}
		super.stop();
	}

	public void updateConnectionDetails(Map<String, Object> port_props) {}

	public void writePacketsToSocket(IO serv, Queue<Packet> packets) {
		if (serv != null) {

			// synchronized (serv) {
			if ((packets != null) && (packets.size() > 0)) {
				Packet p = null;

				while ((p = packets.poll()) != null) {
					if (log.isLoggable(Level.FINER) &&!log.isLoggable(Level.FINEST)) {
						log.log(Level.FINER, "{0}, Processing packet: {1}, type: {2}", new Object[] {
								serv,
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
	 * @param ios
	 * @param p
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
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

			// if packet is added to waiting packets queue then we can assume it is sent
			// as if it will fail it will be returned as error by serviceStopped method
			ios.addPacketToSend(p);
			if (ios.writeInProgress.tryLock()) {
				try {
					ios.processWaitingPackets();
					SocketThread.addSocketService(ios);
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
			return true;
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Can''t find service for packet: <{0}> {1}, service id: {2}",
						new Object[] { p.getElemName(),
						p.getTo(), getServiceId(p) });
			}
		}    // end of if (ios != null) else

		return false;
	}

	@Override
	public String xmppStreamError(IO serv, List<Element> err_el) {
		StreamError streamError	= StreamError.getByCondition(err_el.get(0).getName());	
		
		for (XMPPIOProcessor proc : processors) {
			proc.streamError(serv, streamError);
		}		
		return "<stream:error>" + err_el.get(0).toString() + "</stream:error>";
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		log.log(Level.CONFIG, "{0} defaults: {1}", new Object[] { getName(),
				params.toString() });

		Map<String, Object> props = super.getDefaults(params);

		props.put(TLS_USE_PROP_KEY, TLS_USE_PROP_VAL);
		checkHighThroughputProperty(NET_BUFFER_HT_PROP_KEY, NET_BUFFER_HT_PROP_VAL,
				NET_BUFFER_ST_PROP_KEY, NET_BUFFER_ST_PROP_VAL, NET_BUFFER_PROP_KEY,
				Integer.class, params, props);
		checkHighThroughputProperty(HT_TRAFFIC_THROTTLING_PROP_KEY,
				getDefTrafficThrottling(), ST_TRAFFIC_THROTTLING_PROP_KEY,
				getDefTrafficThrottling(), TRAFFIC_THROTTLING_PROP_KEY, String.class, params,
				props);

		props.put(NET_BUFFER_LIMIT_PROP_KEY, isHighThroughput()
				? NET_BUFFER_LIMIT_HT_PROP_VAL : NET_BUFFER_LIMIT_ST_PROP_VAL);

		if ( params.get( "--" + ELEMENTS_NUMBER_LIMIT_PROP_KEY ) != null ){
			elements_number_limit = Integer.valueOf( (String)params.get( "--" + ELEMENTS_NUMBER_LIMIT_PROP_KEY ) );
		} else {
			elements_number_limit = ELEMENTS_NUMBER_LIMIT_PROP_VAL;
		}
		props.put( ELEMENTS_NUMBER_LIMIT_PROP_KEY, elements_number_limit );

		if ( params.get( "--" + WATCHDOG_DELAY ) != null ){
			watchdogDelay = Integer.valueOf( (String)params.get( "--" + WATCHDOG_DELAY ) );
		}
		props.put( WATCHDOG_DELAY, watchdogDelay);

		if ( params.get( "--" + WATCHDOG_TIMEOUT ) != null ){
			watchdogTimeout =  Integer.valueOf( (String)params.get( "--" + WATCHDOG_TIMEOUT ) );
		}
		props.put( WATCHDOG_TIMEOUT, watchdogTimeout);

		WATCHDOG_PING_TYPE pingtype = WATCHDOG_PING_TYPE.WHITESPACE;
		if ( params.get( "--" + WATCHDOG_PING_TYPE_KEY ) != null ){
			String type = (String)params.get( "--" + WATCHDOG_PING_TYPE_KEY );
			pingtype = WATCHDOG_PING_TYPE.valueOf( type.toUpperCase() );
		}
		props.put( WATCHDOG_PING_TYPE_KEY, pingtype);

		int[]  ports     = null;
		String ports_str = (String) params.get("--" + getName() + "-ports");

		if (ports_str != null) {
			String[] ports_stra = ports_str.split(",");

			ports = new int[ports_stra.length];

			int k = 0;

			for (String p : ports_stra) {
				try {
					ports[k++] = Integer.parseInt(p);
				} catch (NumberFormatException e) {
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

		props.put(PORT_LISTENING_DELAY_KEY,PORT_LISTENING_DELAY_DEF);

		return props;
	}

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
		for (XMPPIOProcessor proc : processors) {
			proc.getStatistics(list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param serviceId
	 *
	 *
	 *
	 * @return a value of <code>IO</code>
	 */
	public IO getXMPPIOService(String serviceId) {
		return services.get(serviceId);
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setName(String name) {
		super.setName(name);
		setupWatchdogThread();
	}

	protected void setupWatchdogThread() {
		watchdog = new Thread( newWatchdog(), "Watchdog - " + getName());
		watchdog.setDaemon(true);
	}

	protected Watchdog newWatchdog() {
		return new Watchdog();
	}
	
	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
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
		if (props.get(NET_BUFFER_LIMIT_PROP_KEY) != null) {
			net_buffer_limit = (Integer) props.get(NET_BUFFER_LIMIT_PROP_KEY);
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

		if ( props.get (ELEMENTS_NUMBER_LIMIT_PROP_KEY ) != null ){
			elements_number_limit = (int) props.get (ELEMENTS_NUMBER_LIMIT_PROP_KEY );
		}

		if ( props.get( WATCHDOG_DELAY ) != null ){
			watchdogDelay =  (long) props.get( WATCHDOG_DELAY );
		}
		if ( props.get( WATCHDOG_TIMEOUT ) != null ){
			watchdogTimeout =  (long) props.get( WATCHDOG_TIMEOUT );
		}
		if ( props.get( WATCHDOG_PING_TYPE_KEY ) != null ){
			String value = String.valueOf( props.get( WATCHDOG_PING_TYPE_KEY ) );
			watchdogPingType = WATCHDOG_PING_TYPE.valueOf( value.toUpperCase() );
		}
		if (props.get(PORT_LISTENING_DELAY_KEY) != null) {
			log.log(Level.CONFIG, "Setting delaying opening ports of component: {0} to: {1} (was: {2})",
					new Object[]{getName(), props.get(PORT_LISTENING_DELAY_KEY), delayPortListening});
			delayPortListening = (Boolean) props.get(PORT_LISTENING_DELAY_KEY);

			if (delayPortListening) {
				log.log(Level.WARNING, "Delaying opening ports of component: {0}", getName());
			}
		}

		if (props.size() == 1) {

			// If props.size() == 1, it means this is a single property update and
			// ConnectionManager does not support it yet.
			return;
		}
		if (isInitializationComplete()) {

			// Do we really need to do this again?
			// Looks like reconfiguration for the port is not working correctly anyway
			// so for now we do not want to do it.
			return;
		}
		releaseListeners();

		int[] ports = (int[]) props.get(PORTS_PROP_KEY);

		if (ports != null) {
			for (int i = 0; i < ports.length; i++) {

				Map<String, Object> port_props = new LinkedHashMap<>(20);
				for (Map.Entry<String, Object> entry : props.entrySet()) {
					if (entry.getKey().startsWith(PROP_KEY + ports[i])) {
						int    idx = entry.getKey().lastIndexOf('/');
						String key = entry.getKey().substring(idx + 1);

						log.log(Level.CONFIG, "Adding port property key: {0}={1}", new Object[] { key,
								entry.getValue() });
						port_props.put(key, entry.getValue());
					}    // end of if (entry.getKey().startsWith())
				}      // end of for ()
				port_props.put(PORT_KEY, ports[i]);
				if (port_props.containsKey(PORT_TYPE_PROP_KEY)
						&& !(port_props.get(PORT_TYPE_PROP_KEY) instanceof ConnectionType)) {
					Object val = port_props.get(PORT_TYPE_PROP_KEY);
					port_props.put(PORT_TYPE_PROP_KEY, ConnectionType.valueOf(val.toString()));
				}
				if (port_props.containsKey(PORT_SOCKET_PROP_KEY)
						&& !(port_props.get(PORT_SOCKET_PROP_KEY) instanceof SocketType)) {
					Object val = port_props.get(PORT_SOCKET_PROP_KEY);
					port_props.put(PORT_SOCKET_PROP_KEY, SocketType.valueOf(val.toString()));
				}
                addWaitingTask(port_props);
				// reconnectService(port_props, startDelay);
			}        // end of for (int i = 0; i < ports.length; i++)
		}          // end of if (ports != null)
	}

	//~--- methods --------------------------------------------------------------

	protected void addWaitingTask(Map<String, Object> conn) {

        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "Adding waiting task: {0}, initializationCompleted: {1}, delayPortListening: {2}, to: {3}",
                    new Object[]{conn, initializationCompleted, delayPortListening, waitingTasks});
        }

        if (initializationCompleted && !delayPortListening) {
            reconnectService(conn, connectionDelay);
        } else {
            waitingTasks.add(conn);
        }
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
		T tmp = st_def_val;
		String str_tmp = null;

		if (isHighThroughput()) {
			tmp = ht_dev_val;
			str_tmp = (String) params.get(ht_def_key);
		} else {
			tmp = st_def_val;
			str_tmp = (String) params.get(st_def_key);
		}
		if (tmp == null)
			tmp = st_def_val;

		if (str_tmp != null) {
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
		}
		props.put(prop_key, tmp);

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

	/**
	 *
	 * @param p
	 *
	 *
	 * @return a value of <code>boolean</code>
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
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	protected boolean writePacketToSocket(Packet p, String serviceId) {
		IO ios = getXMPPIOService(serviceId);

		if (ios != null) {
			return writePacketToSocket(ios, p);
		} else {
			return false;
		}
	}

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

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int[]</code>
	 */
	protected int[] getDefPlainPorts() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int[]</code>
	 */
	protected int[] getDefSSLPorts() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	protected String getDefTrafficThrottling() {
		String result = ST_TRAFFIC_THROTTLING_PROP_VAL;

		if (isHighThroughput()) {
			result = HT_TRAFFIC_THROTTLING_PROP_VAL;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	protected abstract long getMaxInactiveTime();

	/**
	 * Method description
	 *
	 *
	 * @param port
	 *
	 *
	 *
	 * @return a value of {@code Map<String,Object>}
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
	 *
	 *
	 * @return a value of <code>String</code>
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
	 *
	 *
	 * @return a value of <code>String</code>
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
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	protected String getUniqueId(IO serv) {
		return serv.getUniqueId();
	}

	/**
	 * Method description
	 *
	 *
	 * @param p
	 *
	 *
	 *
	 * @return a value of <code>IO</code>
	 */
	protected IO getXMPPIOService(Packet p) {
		String id = getServiceId(p);

		if (id != null) {
			return services.get(id);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>IO</code>
	 */
	protected abstract IO getXMPPIOServiceInstance();

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	protected boolean isHighThroughput() {
		return false;
	}

	//~--- methods --------------------------------------------------------------

	protected void socketAccepted(IO serv, SocketType type) {
	}
	
	private void putDefPortParams(Map<String, Object> props, int port, SocketType sock) {
		log.log(Level.CONFIG, "Generating defaults for port: {0,number,#}", port);
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
			String cid = "" + port_props.get("local-hostname") + "@" + port_props.get(
					"remote-hostname");

			log.log(Level.FINER,
					"Reconnecting service for: {0}, scheduling next try in {1}secs, cid: {2}, props: {3}",
					new Object[] { getName(), delay / 1000, cid, port_props });
		}
		addTimerTask(new tigase.util.TimerTask() {
			@Override
			public void run() {
				String host = (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);

				if (host == null) {
					host = (String) port_props.get("remote-hostname");
				}

				int port = (Integer) port_props.get(PORT_KEY);

				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE,
							"Reconnecting service for component: {0}, to remote host: {1} on port: {2,number,#}",
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

	//~--- inner classes --------------------------------------------------------

	private class ConnectionListenerImpl
					implements ConnectionOpenListener {
		private Map<String, Object> port_props = null;

		//~--- constructors -------------------------------------------------------

		private ConnectionListenerImpl(Map<String, Object> port_props) {
			this.port_props = port_props;
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public void accept(SocketChannel sc) {
			String cid = "" + port_props.get("local-hostname") + "@" + port_props.get(
					"remote-hostname");

			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "Accept called for service: {0}, port_props: {1}", new Object[] {cid, port_props} );
			}

			IO serv = getXMPPIOServiceInstance();
			serv.setBufferLimit( net_buffer_limit );

			( (XMPPDomBuilderHandler) serv.getSessionData().get( DOM_HANDLER ) ).setElementsLimit( elements_number_limit );

			serv.setIOServiceListener(ConnectionManager.this);
			serv.setSessionData(port_props);
			try {
				serv.accept(sc);
				socketAccepted(serv, getSocketType());
				if (getSocketType() == SocketType.ssl) {
					serv.startSSL(false, false, false);
				}    // end of if (socket == SocketType.ssl)
				serviceStarted(serv);
				SocketThread.addSocketService(serv);
			} catch (Exception e) {
				if (getConnectionType() == ConnectionType.connect) {

					// Accept side for component service is not ready yet?
					// Let's wait for a few secs and try again.
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Problem reconnecting the service: {0}, port_props: {1}, exception: {2}",
								new Object[] { serv, port_props, e });
					}
					updateConnectionDetails(port_props);

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

//      } catch (Exception e) {
//        if (log.isLoggable(Level.FINEST)) {
//          log.log(Level.FINEST, "Can not accept connection cid: " + cid, e);
//        }
//        log.log(Level.WARNING, "Can not accept connection.", e);
//        serv.stop();
			}          // end of try-catch
		}

		@Override
		public String toString() {
			return port_props.toString();
		}

		//~--- get methods --------------------------------------------------------

		@Override
		public ConnectionType getConnectionType() {
			String type = null;

			if (port_props.get(PORT_TYPE_PROP_KEY) == null) {
				log.warning(getName() + ": connection type is null: " + port_props.get(PORT_KEY)
						.toString());
			} else {
				type = port_props.get(PORT_TYPE_PROP_KEY).toString();
			}

			return ConnectionType.valueOf(type);
		}

		@Override
		public String[] getIfcs() {
			return (String[]) port_props.get(PORT_IFC_PROP_KEY);
		}

		@Override
		public int getPort() {
			return (Integer) port_props.get(PORT_KEY);
		}

		@Override
		public int getReceiveBufferSize() {
			return net_buffer;
		}

		@Override
		public InetSocketAddress getRemoteAddress() {
			return (InetSocketAddress) port_props.get("remote-address");
		}

		@Override
		public String getRemoteHostname() {
			if (port_props.containsKey(PORT_REMOTE_HOST_PROP_KEY)) {
				return (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);
			}

			return (String) port_props.get("remote-hostname");
		}

		@Override
		public SocketType getSocketType() {
			return SocketType.valueOf(port_props.get(PORT_SOCKET_PROP_KEY).toString());
		}

		@Override
		public String getSRVType() {
			String type = (String) this.port_props.get("srv-type");

			if ((type == null) || type.isEmpty()) {
				return null;
			}

			return type;
		}

		@Override
		public int getTrafficClass() {
			if (isHighThroughput()) {
				return IPTOS_THROUGHPUT;
			} else {
				return DEF_TRAFFIC_CLASS;
			}
		}
	}


	private class IOServiceStatisticsGetter
					implements ServiceChecker<IO> {
		private StatisticsList list = new StatisticsList(Level.ALL);

		//~--- methods ------------------------------------------------------------

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
	 * Class looks in all established connections and checks whether any of them
	 * is dead by performing either whitspace or XMPP ping. If client fails to
	 * respond within defined time then the service is stopped.
	 *
	 */
	protected class Watchdog
					implements Runnable {

		Packet pingPacket;
		
		protected long getDurationSinceLastTransfer(final XMPPIOService service) {
			long curr_time = System.currentTimeMillis();
			long lastTransfer;
			switch (watchdogPingType) {
				case XMPP:
					lastTransfer = service.getLastXmppPacketReceiveTime();
					break;
				case WHITESPACE:
				default:
					lastTransfer = service.getLastTransferTime();
					break;
			}
			return curr_time - lastTransfer;
		}
		
		@Override
		public void run() {
			while (true) {
				try {

					// Sleep...
					Thread.sleep(watchdogDelay);
					++watchdogRuns;

					executeWatchdog();
				} catch (InterruptedException e) {    /* Do nothing here */
				}
			}

		}

		private void executeWatchdog() {
			/** Walk through all connections and check whether they are really
			 * alive. Depending on the configuration send either whitespace or
			 * XMPP ping if the service is inactive for the configured period of
			 * time
			 */
			doForAllServices(new ServiceChecker<IO>() {
				@Override
				public void check(final XMPPIOService service) {
					try {
						if ( null != service ){
							if ( log.isLoggable( Level.FINEST ) ){
								log.log( Level.FINEST,
										 "Testing service: {0}, sinceLastTransfer: {1}, maxInactivityTime: {2}, watchdogTimeout: {3}, watchdogDelay: {4}, watchdogPingType: {5} ",
										 new Object[] { service, getDurationSinceLastTransfer( service ), maxInactivityTime, watchdogTimeout, watchdogDelay, watchdogPingType } );
							}
							long sinceLastTransfer = getDurationSinceLastTransfer(service);
							if ( sinceLastTransfer >= maxInactivityTime ){

								// Stop the service if max keep-alive time is exceeded
								// for non-active connections.
								if ( log.isLoggable( Level.INFO ) ){
									log.log( Level.INFO,
											 "{0}: Max inactive time exceeded, stopping: {1} ( sinceLastTransfer: {2}, maxInactivityTime: {3}, watchdogTimeout: {4}, watchdogDelay: {5}, watchdogPingType: {6} )",
											 new Object[] { getName(), service, getDurationSinceLastTransfer( service ),
															maxInactivityTime, watchdogTimeout, watchdogDelay, watchdogPingType } );
								}
								++watchdogStopped;
								service.forceStop();
							} else {
								if ( sinceLastTransfer >= ( watchdogTimeout ) ){

									/** At least once every configured timings check if the
									 * connection is still alive with the use of configured
									 * ping type. */
									switch ( watchdogPingType ) {
										case XMPP:
											pingPacket = Iq.packetInstance( pingElement.clone(),
																			JID.jidInstanceNS( (String) service.getSessionData().get( XMPPIOService.HOSTNAME_KEY ) ),
																			JID.jidInstanceNS( service.getUserJid() ) );
											if (log.isLoggable(Level.FINEST)) {
												log.log(Level.FINEST, "{0}, sending XMPP ping {1}", new Object[] { service, pingPacket });
											}
											if ( !writePacketToSocket( (IO) service, pingPacket ) ){
												// writing failed, stopp service
												++watchdogStopped;
												service.forceStop();
											}
											break;

										case WHITESPACE:
											if (log.isLoggable(Level.FINEST)) {
												log.log(Level.FINEST, "Sending whitespace ping for service {0}", new Object[] { service });
											}
											service.writeRawData( " " );
											break;
									}
									++watchdogTests;
								}
							}
						}
					} catch ( IOException e ) {

						// Close the service
						try {
							if ( service != null ){
								log.info( getName() + "Found dead connection, stopping: " + service );
								++watchdogStopped;
								service.forceStop();
							}
						} catch ( Exception ignore ) {
							// Do nothing here as we expect Exception to be thrown here...
						}
					}
				}
			} );
		}

	}
}    // ConnectionManager


//~ Formatted in Tigase Code Convention on 13/10/15
