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

package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

import tigase.net.ConnectionOpenListener;
import tigase.net.ConnectionOpenThread;
import tigase.net.ConnectionType;
import tigase.net.SocketReadThread;
import tigase.net.SocketType;

import tigase.server.script.CommandIfc;

import tigase.stats.StatisticsList;

import tigase.util.DataTypes;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPIOServiceListener;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.net.SocketException;

import java.nio.channels.SocketChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;

//~--- classes ----------------------------------------------------------------

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
		extends AbstractMessageReceiver implements XMPPIOServiceListener<IO> {
	private static final Logger log = Logger.getLogger(ConnectionManager.class.getName());

	/** Field description */
	public static final String NET_BUFFER_ST_PROP_KEY = "--net-buff-standard";

	/** Field description */
	public static final String NET_BUFFER_HT_PROP_KEY = "--net-buff-high-throughput";
	protected static final String PORT_KEY = "port-no";
	protected static final String PROP_KEY = "connections/";
	protected static final String PORTS_PROP_KEY = PROP_KEY + "ports";
	protected static final String PORT_TYPE_PROP_KEY = "type";
	protected static final String PORT_SOCKET_PROP_KEY = "socket";
	protected static final String PORT_IFC_PROP_KEY = "ifc";
	protected static final String PORT_CLASS_PROP_KEY = "class";
	protected static final String PORT_REMOTE_HOST_PROP_KEY = "remote-host";
	protected static final String PORT_REMOTE_HOST_PROP_VAL = "localhost";
	protected static final String TLS_PROP_KEY = PROP_KEY + "tls/";
	protected static final String TLS_USE_PROP_KEY = TLS_PROP_KEY + "use";
	protected static final boolean TLS_USE_PROP_VAL = true;
	protected static final String TLS_REQUIRED_PROP_KEY = TLS_PROP_KEY + "required";
	protected static final boolean TLS_REQUIRED_PROP_VAL = false;

//protected static final String TLS_KEYS_STORE_PROP_KEY = TLS_PROP_KEY + JKS_KEYSTORE_FILE_KEY;
//protected static final String TLS_KEYS_STORE_PROP_VAL = JKS_KEYSTORE_FILE_VAL;
//protected static final String TLS_DEF_CERT_PROP_KEY = TLS_PROP_KEY + DEFAULT_DOMAIN_CERT_KEY;
//protected static final String TLS_DEF_CERT_PROP_VAL = DEFAULT_DOMAIN_CERT_VAL;
//protected static final String TLS_KEYS_STORE_PASSWD_PROP_KEY = TLS_PROP_KEY
//  + JKS_KEYSTORE_PWD_KEY;
//protected static final String TLS_KEYS_STORE_PASSWD_PROP_VAL = JKS_KEYSTORE_PWD_VAL;
//protected static final String TLS_TRUSTS_STORE_PASSWD_PROP_KEY = TLS_PROP_KEY
//  + TRUSTSTORE_PWD_KEY;
//protected static final String TLS_TRUSTS_STORE_PASSWD_PROP_VAL = TRUSTSTORE_PWD_VAL;
//protected static final String TLS_TRUSTS_STORE_PROP_KEY = TLS_PROP_KEY + TRUSTSTORE_FILE_KEY;
//protected static final String TLS_TRUSTS_STORE_PROP_VAL = TRUSTSTORE_FILE_VAL;
//protected static final String TLS_CONTAINER_CLASS_PROP_KEY = TLS_PROP_KEY
//  + SSL_CONTAINER_CLASS_KEY;
//protected static final String TLS_CONTAINER_CLASS_PROP_VAL = SSL_CONTAINER_CLASS_VAL;
//protected static final String TLS_SERVER_CERTS_DIR_PROP_KEY = TLS_PROP_KEY + SERVER_CERTS_DIR_KEY;
//protected static final String TLS_SERVER_CERTS_DIR_PROP_VAL = SERVER_CERTS_DIR_VAL;
//protected static final String TLS_TRUSTED_CERTS_DIR_PROP_KEY = TLS_PROP_KEY
//  + TRUSTED_CERTS_DIR_KEY;
//protected static final String TLS_TRUSTED_CERTS_DIR_PROP_VAL = TRUSTED_CERTS_DIR_VAL;
//protected static final String TLS_ALLOW_SELF_SIGNED_CERTS_PROP_KEY = TLS_PROP_KEY
//  + ALLOW_SELF_SIGNED_CERTS_KEY;
//protected static final String TLS_ALLOW_SELF_SIGNED_CERTS_PROP_VAL = ALLOW_SELF_SIGNED_CERTS_VAL;
//protected static final String TLS_ALLOW_INVALID_CERTS_PROP_KEY = TLS_PROP_KEY
//  + ALLOW_INVALID_CERTS_KEY;
//protected static final String TLS_ALLOW_INVALID_CERTS_PROP_VAL = ALLOW_INVALID_CERTS_VAL;
	protected static final String MAX_RECONNECTS_PROP_KEY = "max-reconnects";
	protected static final String NET_BUFFER_PROP_KEY = "net-buffer";
	protected static final int NET_BUFFER_ST_PROP_VAL = 2 * 1024;
	protected static final int NET_BUFFER_HT_PROP_VAL = 64 * 1024;

	/** Field description */
	public static final String PORT_LOCAL_HOST_PROP_KEY = "local-host";
	private static ConnectionOpenThread connectThread = ConnectionOpenThread.getInstance();
	private static SocketReadThread readThread = SocketReadThread.getInstance();

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	public String[] PORT_IFC_PROP_VAL = { "*" };

	// services.size() call is very slow due to the implementation details
	// Therefore for often size retrieval this value is calculated separately.
	private int services_size = 0;
	private Thread watchdog = null;
	private long watchdogRuns = 0;
	private long watchdogStopped = 0;
	private long watchdogTests = 0;
	private LinkedList<Map<String, Object>> waitingTasks = new LinkedList<Map<String, Object>>();
	private ConcurrentHashMap<String, IO> services = new ConcurrentHashMap<String, IO>();
	private Set<ConnectionListenerImpl> pending_open =
		Collections.synchronizedSet(new HashSet<ConnectionListenerImpl>());;
	protected int net_buffer = NET_BUFFER_ST_PROP_VAL;

//protected long startDelay = 5 * SECOND;
	private boolean initializationCompleted = false;
	protected long connectionDelay = 2 * SECOND;

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

	protected abstract long getMaxInactiveTime();

	protected abstract IO getXMPPIOServiceInstance();

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
		log.log(Level.CONFIG, "{0} defaults: {1}", new Object[] { getName(), params.toString() });

		Map<String, Object> props = super.getDefaults(params);

		props.put(TLS_USE_PROP_KEY, TLS_USE_PROP_VAL);

//  props.put(TLS_DEF_CERT_PROP_KEY, TLS_DEF_CERT_PROP_VAL);
//  props.put(TLS_KEYS_STORE_PROP_KEY, TLS_KEYS_STORE_PROP_VAL);
//  props.put(TLS_KEYS_STORE_PASSWD_PROP_KEY, TLS_KEYS_STORE_PASSWD_PROP_VAL);
//  props.put(TLS_TRUSTS_STORE_PROP_KEY, TLS_TRUSTS_STORE_PROP_VAL);
//  props.put(TLS_TRUSTS_STORE_PASSWD_PROP_KEY, TLS_TRUSTS_STORE_PASSWD_PROP_VAL);
//  props.put(TLS_SERVER_CERTS_DIR_PROP_KEY, TLS_SERVER_CERTS_DIR_PROP_VAL);
//  props.put(TLS_TRUSTED_CERTS_DIR_PROP_KEY, TLS_TRUSTED_CERTS_DIR_PROP_VAL);
//  props.put(TLS_ALLOW_SELF_SIGNED_CERTS_PROP_KEY, TLS_ALLOW_SELF_SIGNED_CERTS_PROP_VAL);
//  props.put(TLS_ALLOW_INVALID_CERTS_PROP_KEY, TLS_ALLOW_INVALID_CERTS_PROP_VAL);
//
//  if (params.get("--" + SSL_CONTAINER_CLASS_KEY) != null) {
//    props.put(TLS_CONTAINER_CLASS_PROP_KEY, (String) params.get("--" + SSL_CONTAINER_CLASS_KEY));
//  } else {
//    props.put(TLS_CONTAINER_CLASS_PROP_KEY, TLS_CONTAINER_CLASS_PROP_VAL);
//  }
		int buffSize = NET_BUFFER_ST_PROP_VAL;

		if (isHighThroughput()) {
			buffSize = DataTypes.parseSizeInt((String) params.get(NET_BUFFER_HT_PROP_KEY),
					NET_BUFFER_HT_PROP_VAL);
		} else {
			buffSize = DataTypes.parseSizeInt((String) params.get(NET_BUFFER_ST_PROP_KEY),
					NET_BUFFER_ST_PROP_VAL);
		}

		props.put(NET_BUFFER_PROP_KEY, buffSize);

		int[] ports = null;
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

		return props;
	}

	/**
	 * Generates the component statistics.
	 * @param list is a collection to put the component statistics in.
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "Open connections", services_size, Level.INFO);

		if (list.checkLevel(Level.FINEST)) {
			int waitingToSendSize = 0;

			for (IO serv : services.values()) {
				waitingToSendSize += serv.waitingToSendSize();
			}

			list.add(getName(), "Waiting to send", waitingToSendSize, Level.FINEST);
		}

		list.add(getName(), "Watchdog runs", watchdogRuns, Level.FINER);
		list.add(getName(), "Watchdog tests", watchdogTests, Level.FINE);
		list.add(getName(), "Watchdog stopped", watchdogStopped, Level.FINE);

//  StringBuilder sb = new StringBuilder("All connected: ");
//  for (IOService serv: services.values()) {
//    sb.append("\nService ID: " + getUniqueId(serv)
//      + ", local-hostname: " + serv.getSessionData().get("local-hostname")
//      + ", remote-hostname: " + serv.getSessionData().get("remote-hostname")
//      + ", is-connected: " + serv.isConnected()
//      + ", connection-type: " + serv.connectionType());
//  }
//  log.finest(sb.toString());
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
		initializationCompleted = true;

		for (Map<String, Object> params : waitingTasks) {
			reconnectService(params, connectionDelay);
		}

		waitingTasks.clear();
	}

	// Implementation of tigase.net.PacketListener
///**
// * Describe <code>packetsReady</code> method here.
// *
// * @param s an <code>IOService</code> value
// * @throws IOException
// */
//@SuppressWarnings({"unchecked"})
//@Override
//public void packetsReady(IOService s) throws IOException {
//  if (log.isLoggable(Level.FINEST)) {
//    log.finest("packetsReady called");
//  }
//  IO serv = (IO)s;
//  packetsReady(serv);
//}

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
		writePacketsToSocket(serv, processSocketData(serv));
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
	public int processingThreads() {
		return Runtime.getRuntime().availableProcessors();
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
	@TODO(note = "Do something if service with the same unique ID is already started, "
			+ "possibly kill the old one...")
	public void serviceStarted(final IO service) {

		// synchronized(services) {
		String id = getUniqueId(service);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "[[{0}]] Connection started: {1}", new Object[] { getName(), service });
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
				log.log(Level.WARNING, "{0}: Attempt to add different service with the same ID: {1}",
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

//@SuppressWarnings({"unchecked"})
//@Override
//public void serviceStopped(IOService s) {
//  IO ios = (IO)s;
//  serviceStopped(ios);
//}

	/**
	 *
	 * @param service
	 * @return
	 */
	@Override
	public boolean serviceStopped(IO service) {

		// synchronized(service) {
		String id = getUniqueId(service);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "[[{0}]] Connection stopped: {1}", new Object[] { getName(), service });
		}

		// id might be null if service is stopped in accept method due to
		// an exception during establishing TCP/IP connection
		// IO serv = (id != null ? services.get(id) : null);
		if (id != null) {
			boolean result = services.remove(id, service);

			if (result) {
				--services_size;
			} else {

				// Is it at all possible to happen???
				// let's log it for now....
				log.log(Level.WARNING, "[[{0}]] Attempt to stop incorrect service: {1}",
						new Object[] { getName(),
						service });
				Thread.dumpStack();
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
		net_buffer = (Integer) props.get(NET_BUFFER_PROP_KEY);
		releaseListeners();

		int[] ports = (int[]) props.get(PORTS_PROP_KEY);

		if (ports != null) {
			for (int i = 0; i < ports.length; i++) {
				Map<String, Object> port_props = new LinkedHashMap<String, Object>(20);

				for (Map.Entry<String, Object> entry : props.entrySet()) {
					if (entry.getKey().startsWith(PROP_KEY + ports[i])) {
						int idx = entry.getKey().lastIndexOf('/');
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

		if ((Boolean) props.get(TLS_USE_PROP_KEY)) {
			Map<String, String> tls_params = new LinkedHashMap<String, String>(20);

//    tls_params.put(SSL_CONTAINER_CLASS_KEY, (String) props.get(TLS_CONTAINER_CLASS_PROP_KEY));
//    tls_params.put(DEFAULT_DOMAIN_CERT_KEY, (String) props.get(TLS_DEF_CERT_PROP_KEY));
//    tls_params.put(JKS_KEYSTORE_FILE_KEY, (String) props.get(TLS_KEYS_STORE_PROP_KEY));
//    tls_params.put(JKS_KEYSTORE_PWD_KEY, (String) props.get(TLS_KEYS_STORE_PASSWD_PROP_KEY));
//    tls_params.put(TRUSTSTORE_FILE_KEY, (String) props.get(TLS_TRUSTS_STORE_PROP_KEY));
//    tls_params.put(TRUSTSTORE_PWD_KEY, (String) props.get(TLS_TRUSTS_STORE_PASSWD_PROP_KEY));
//    tls_params.put(SERVER_CERTS_DIR_KEY, (String) props.get(TLS_SERVER_CERTS_DIR_PROP_KEY));
//    tls_params.put(TRUSTED_CERTS_DIR_KEY, (String) props.get(TLS_TRUSTED_CERTS_DIR_PROP_KEY));
//    tls_params.put(ALLOW_SELF_SIGNED_CERTS_KEY,
//        (String) props.get(TLS_ALLOW_SELF_SIGNED_CERTS_PROP_KEY));
//    tls_params.put(ALLOW_INVALID_CERTS_KEY, (String) props.get(TLS_ALLOW_INVALID_CERTS_PROP_KEY));
//    TLSUtil.configureSSLContext(getName(), tls_params);
		}    // end of if (use.equalsIgnoreCase())
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void start() {
		super.start();

		// delayedTasks = new Timer(getName() + " - delayed connections", true);
	}

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

			try {
				ios.processWaitingPackets();
				readThread.addSocketService(ios);

				return true;
			} catch (Exception e) {
				log.log(Level.WARNING, ios + "Exception during writing packets: ", e);

				try {
					ios.stop();
				} catch (Exception e1) {
					log.log(Level.WARNING, ios + "Exception stopping XMPPIOService: ", e1);
				}    // end of try-catch
			}      // end of try-catch

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
						log.log(Level.FINER, "{0}, Processing packet: {1}, type: {2}", new Object[] { serv,
								p.getElemName(), p.getType() });
					}

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Writing packet: {1}", new Object[] { serv, p });
					}

					serv.addPacketToSend(p);
				}      // end of for ()

				try {
					serv.processWaitingPackets();
					readThread.addSocketService(serv);
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

	protected void addWaitingTask(Map<String, Object> conn) {
		if (initializationCompleted) {
			reconnectService(conn, connectionDelay);
		} else {
			waitingTasks.add(conn);
		}
	}

//@SuppressWarnings({"unchecked"})
//@Override
//public void streamClosed(IO s) {
//  IO serv = (IO)s;
//  xmppStreamClosed(serv);
//}
//
//public abstract void xmppStreamClosed(IO serv);
//
///**
// * The method is called upon XMPP stream open event.
// * @param s is an XMPPIOService object associated with the newly opened network
// * connection.
// * @param attribs is a Map with all attributes found in the XMPP Stream open element.
// * @return A String of raw data which should be sent back to the network
// * connection.
// */
//@SuppressWarnings({"unchecked"})
//@Override
//public String streamOpened(IO s, Map<String, String> attribs) {
//  IO serv = (IO)s;
//  return xmppStreamOpened(serv, attribs);
//}
//
///**
// * Method is called on the new XMPP Stream open event. This method is normally
// * called from streamOpen(...) method.
// * @param s is an IOService object associated with the network connection
// * where the XMPP Stream open event occured.
// * @param attribs is a Map with all attributes found in the XMPP Stream open element.
// * @return A String of raw data which should be sent back to the network
// * connection.
// */
//public abstract String xmppStreamOpened(IO s, Map<String, String> attribs);
//

	/**
	 * Returns number of active network connections (IOServices).
	 * @return number of active network connections (IOServices).
	 */
	protected int countIOServices() {
		return services.size();
	}

	/**
	 * Perform a given action defined by ServiceChecker for all active IOService
	 * objects (active network connections).
	 * @param checker is a <code>ServiceChecker</code> instance defining an action
	 * to perform for all IOService objects.
	 */
	protected void doForAllServices(ServiceChecker checker) {
		for (IO service : services.values()) {
			checker.check(service);
		}
	}

	//~--- get methods ----------------------------------------------------------

	protected int[] getDefPlainPorts() {
		return null;
	}

	protected int[] getDefSSLPorts() {
		return null;
	}

	protected Map<String, Object> getParamsForPort(int port) {
		return null;
	}

	protected String getServiceId(Packet packet) {
		return getServiceId(packet.getTo());
	}

	protected String getServiceId(JID jid) {
		return jid.getResource();
	}

	protected String getUniqueId(IO serv) {
		return serv.getUniqueId();
	}

	protected IO getXMPPIOService(String serviceId) {
		return services.get(serviceId);
	}

	protected IO getXMPPIOService(Packet p) {
		return services.get(getServiceId(p));
	}

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

	protected boolean writePacketToSocket(Packet p, String serviceId) {
		IO ios = getXMPPIOService(serviceId);

		if (ios != null) {
			return writePacketToSocket(ios, p);
		} else {
			return false;
		}
	}

	protected void writeRawData(IO ios, String data) {

		// synchronized (ios) {
		try {
			ios.writeRawData(data);
			readThread.addSocketService(ios);
		} catch (Exception e) {
			log.log(Level.WARNING, ios + "Exception during writing data: " + data, e);

			try {
				ios.stop();
			} catch (Exception e1) {
				log.log(Level.WARNING, ios + "Exception stopping XMPPIOService: ", e1);
			}    // end of try-catch
		}

		// }
	}

	private void putDefPortParams(Map<String, Object> props, int port, SocketType sock) {
		log.log(Level.CONFIG, "Generating defaults for port: {0}", port);
		props.put(PROP_KEY + port + "/" + PORT_TYPE_PROP_KEY, ConnectionType.accept);
		props.put(PROP_KEY + port + "/" + PORT_SOCKET_PROP_KEY, sock);
		props.put(PROP_KEY + port + "/" + PORT_IFC_PROP_KEY, PORT_IFC_PROP_VAL);
		props.put(PROP_KEY + port + "/" + PORT_REMOTE_HOST_PROP_KEY, PORT_REMOTE_HOST_PROP_VAL);
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
			String cid = "" + port_props.get("local-hostname") + "@" + port_props.get("remote-hostname");

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
					log.log(Level.FINE,
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
		ConnectionListenerImpl cli = new ConnectionListenerImpl(port_props);

		if (cli.getConnectionType() == ConnectionType.accept) {
			pending_open.add(cli);
		}

		connectThread.addConnectionOpenListener(cli);
	}

	//~--- inner classes --------------------------------------------------------

	private class ConnectionListenerImpl implements ConnectionOpenListener {
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
		@SuppressWarnings({ "unchecked" })
		@Override
		public void accept(SocketChannel sc) {
			String cid = "" + port_props.get("local-hostname") + "@" + port_props.get("remote-hostname");

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Accept called for service: {0}", cid);
			}

			IO serv = getXMPPIOServiceInstance();

			// serv.setSSLId(getName());
			serv.setIOServiceListener(ConnectionManager.this);
			serv.setSessionData(port_props);

			try {
				serv.accept(sc);

				if (getSocketType() == SocketType.ssl) {
					serv.startSSL(false);
				}    // end of if (socket == SocketType.ssl)

				serviceStarted(serv);
				readThread.addSocketService(serv);
			} catch (SocketException e) {
				if (getConnectionType() == ConnectionType.connect) {

					// Accept side for component service is not ready yet?
					// Let's wait for a few secs and try again.
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Problem reconnecting the service: {0}, cid: {1}",
								new Object[] { serv,
								cid });
					}

					boolean reconnect = false;
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
				log.warning(getName() + ": connection type is null: "
						+ port_props.get(PORT_KEY).toString());
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
	}


	/**
	 * Looks in all established connections and checks whether any of them
	 * is dead....
	 *
	 */
	private class Watchdog implements Runnable {

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
					doForAllServices(new ServiceChecker() {
						@Override
						public void check(final XMPPIOService service) {

							// for (IO service: services.values()) {
							// service = (XMPPIOService)serv;
							try {
								if (null != service) {
									long curr_time = System.currentTimeMillis();
									long lastTransfer = service.getLastTransferTime();

									if (curr_time - lastTransfer >= getMaxInactiveTime()) {

										// Stop the service is max keep-alive time is acceeded
										// for non-active connections.
										if (log.isLoggable(Level.INFO)) {
											log.info(getName() + ": Max inactive time exceeded, stopping: " + service);
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

							// }
						}
					});
				} catch (InterruptedException e) {    /* Do nothing here */
				}
			}
		}
	}
}    // ConnectionManager


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
