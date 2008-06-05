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

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.annotations.TODO;
import tigase.io.TLSUtil;
import tigase.net.ConnectionOpenListener;
import tigase.net.ConnectionOpenThread;
import tigase.net.ConnectionType;
import tigase.net.IOService;
import tigase.net.SocketReadThread;
import tigase.net.SocketType;
import tigase.stats.StatRecord;
import tigase.util.JIDUtils;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPIOServiceListener;
import java.io.File;

import static tigase.io.SSLContextContainerIfc.*;

/**
 * Describe class ConnectionManager here.
 *
 *
 * Created: Sun Jan 22 22:52:58 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConnectionManager<IO extends XMPPIOService>
	extends AbstractMessageReceiver implements XMPPIOServiceListener {

	private static final Logger log =
    Logger.getLogger("tigase.server.ConnectionManager");

	protected static final String PORT_KEY = "port-no";
	protected static final String PROP_KEY = "connections/";
	protected static final String PORTS_PROP_KEY = PROP_KEY + "ports";
	protected static final String PORT_TYPE_PROP_KEY = "type";
	protected static final String PORT_SOCKET_PROP_KEY = "socket";
	protected static final String PORT_IFC_PROP_KEY = "ifc";
	private static final String[] PORT_IFC_PROP_VAL = {"*"};
	protected static final String PORT_CLASS_PROP_KEY = "class";
	protected static final String PORT_REMOTE_HOST_PROP_KEY = "remote-host";
	protected static final String PORT_REMOTE_HOST_PROP_VAL = "localhost";
	protected static final String TLS_PROP_KEY = PROP_KEY + "tls/";
	protected static final String TLS_USE_PROP_KEY = TLS_PROP_KEY + "use";
	protected static final boolean TLS_USE_PROP_VAL = true;
	protected static final String TLS_REQUIRED_PROP_KEY =
		TLS_PROP_KEY + "required";
	protected static final boolean TLS_REQUIRED_PROP_VAL = false;
	protected static final String TLS_KEYS_STORE_PROP_KEY =
		TLS_PROP_KEY + JKS_KEYSTORE_FILE_KEY;
	protected static final String TLS_KEYS_STORE_PROP_VAL =
		JKS_KEYSTORE_FILE_VAL;
	protected static final String TLS_DEF_CERT_PROP_KEY =
		TLS_PROP_KEY + DEFAULT_DOMAIN_CERT_KEY;
	protected static final String TLS_DEF_CERT_PROP_VAL =
		DEFAULT_DOMAIN_CERT_VAL;
	protected static final String TLS_KEYS_STORE_PASSWD_PROP_KEY =
		TLS_PROP_KEY + JKS_KEYSTORE_PWD_KEY;
	protected static final String TLS_KEYS_STORE_PASSWD_PROP_VAL =
		JKS_KEYSTORE_PWD_VAL;
	protected static final String TLS_TRUSTS_STORE_PASSWD_PROP_KEY =
		TLS_PROP_KEY + TRUSTSTORE_PWD_KEY;
	protected static final String TLS_TRUSTS_STORE_PASSWD_PROP_VAL =
		TRUSTSTORE_PWD_VAL;
	protected static final String TLS_TRUSTS_STORE_PROP_KEY =
		TLS_PROP_KEY + TRUSTSTORE_FILE_KEY;
	protected static final String TLS_TRUSTS_STORE_PROP_VAL =
		TRUSTSTORE_FILE_VAL;
	protected static final String TLS_CONTAINER_CLASS_PROP_KEY =
		TLS_PROP_KEY + SSL_CONTAINER_CLASS_KEY;
	protected static final String TLS_CONTAINER_CLASS_PROP_VAL =
		SSL_CONTAINER_CLASS_VAL;
	protected static final String TLS_SERVER_CERTS_DIR_PROP_KEY =
		TLS_PROP_KEY + SERVER_CERTS_DIR_KEY;
	protected static final String TLS_SERVER_CERTS_DIR_PROP_VAL =
		SERVER_CERTS_DIR_VAL;
	protected static final String TLS_TRUSTED_CERTS_DIR_PROP_KEY =
		TLS_PROP_KEY + TRUSTED_CERTS_DIR_KEY;
	protected static final String TLS_TRUSTED_CERTS_DIR_PROP_VAL =
		TRUSTED_CERTS_DIR_VAL;
	protected static final String TLS_ALLOW_SELF_SIGNED_CERTS_PROP_KEY =
		TLS_PROP_KEY + ALLOW_SELF_SIGNED_CERTS_KEY;
	protected static final String TLS_ALLOW_SELF_SIGNED_CERTS_PROP_VAL =
		ALLOW_SELF_SIGNED_CERTS_VAL;
	protected static final String TLS_ALLOW_INVALID_CERTS_PROP_KEY =
		TLS_PROP_KEY + ALLOW_INVALID_CERTS_KEY;
	protected static final String TLS_ALLOW_INVALID_CERTS_PROP_VAL =
		ALLOW_INVALID_CERTS_VAL;
	protected static final String MAX_RECONNECTS_PROP_KEY = "max-reconnects";

	private static ConnectionOpenThread connectThread =
		ConnectionOpenThread.getInstance();
	private static SocketReadThread readThread = SocketReadThread.getInstance();
	private Timer delayedTasks = null;
	private Thread watchdog = null;
	private Map<String, IO> services =
		new ConcurrentSkipListMap<String, IO>();
	private Set<ConnectionListenerImpl> pending_open =
		Collections.synchronizedSet(new HashSet<ConnectionListenerImpl>());;
	protected long connectionDelay = 2 * SECOND;
	protected long startDelay = 10 * SECOND;

	public void setName(String name) {
		super.setName(name);
		watchdog = new Thread(new Watchdog(), "Watchdog - " + name);
		watchdog.setDaemon(true);
		watchdog.start();
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		props.put(TLS_USE_PROP_KEY, TLS_USE_PROP_VAL);
		props.put(TLS_DEF_CERT_PROP_KEY, TLS_DEF_CERT_PROP_VAL);
		props.put(TLS_KEYS_STORE_PROP_KEY, TLS_KEYS_STORE_PROP_VAL);
		props.put(TLS_KEYS_STORE_PASSWD_PROP_KEY, TLS_KEYS_STORE_PASSWD_PROP_VAL);
		props.put(TLS_TRUSTS_STORE_PROP_KEY, TLS_TRUSTS_STORE_PROP_VAL);
		props.put(TLS_TRUSTS_STORE_PASSWD_PROP_KEY, TLS_TRUSTS_STORE_PASSWD_PROP_VAL);
		props.put(TLS_SERVER_CERTS_DIR_PROP_KEY, TLS_SERVER_CERTS_DIR_PROP_VAL);
		props.put(TLS_TRUSTED_CERTS_DIR_PROP_KEY, TLS_TRUSTED_CERTS_DIR_PROP_VAL);
		props.put(TLS_ALLOW_SELF_SIGNED_CERTS_PROP_KEY,
			TLS_ALLOW_SELF_SIGNED_CERTS_PROP_VAL);
		props.put(TLS_ALLOW_INVALID_CERTS_PROP_KEY, TLS_ALLOW_INVALID_CERTS_PROP_VAL);

		if (params.get("--" + SSL_CONTAINER_CLASS_KEY) != null) {
			props.put(TLS_CONTAINER_CLASS_PROP_KEY,
				(String)params.get("--" + SSL_CONTAINER_CLASS_KEY));
		} else {
			props.put(TLS_CONTAINER_CLASS_PROP_KEY, TLS_CONTAINER_CLASS_PROP_VAL);
		}

		int ports_size = 0;
		int[] ports = (int[])params.get(getName() + "/" + PORTS_PROP_KEY);
		if (ports != null) {
			for (int port: ports) {
				putDefPortParams(props, port, SocketType.plain);
			} // end of for (int i = 0; i < idx; i++)
			props.put(PORTS_PROP_KEY, ports);
		} else {
			int[] plains = getDefPlainPorts();
			if (plains != null) {
				ports_size += plains.length;
			} // end of if (plains != null)
			int[] ssls = getDefSSLPorts();
			if (ssls != null) {
				ports_size += ssls.length;
			} // end of if (ssls != null)
			if (ports_size > 0) {
				ports = new int[ports_size];
			} // end of if (ports_size > 0)
			if (ports != null) {
				int idx = 0;
				if (plains != null) {
					idx = plains.length;
					for (int i = 0; i < idx; i++) {
						ports[i] = plains[i];
						putDefPortParams(props, ports[i], SocketType.plain);
					} // end of for (int i = 0; i < idx; i++)
				} // end of if (plains != null)
				if (ssls != null) {
					for (int i = idx; i < idx + ssls.length; i++) {
						ports[i] = ssls[i-idx];
						putDefPortParams(props, ports[i], SocketType.ssl);
					} // end of for (int i = 0; i < idx + ssls.length; i++)
				} // end of if (ssls != null)
				props.put(PORTS_PROP_KEY, ports);
			} // end of if (ports != null)
		}
		return props;
	}

	private void putDefPortParams(Map<String, Object> props, int port,
		SocketType sock) {
		props.put(PROP_KEY + port + "/" + PORT_TYPE_PROP_KEY,
			ConnectionType.accept);
		props.put(PROP_KEY + port + "/" + PORT_SOCKET_PROP_KEY,	sock);
		props.put(PROP_KEY + port + "/" + PORT_IFC_PROP_KEY, PORT_IFC_PROP_VAL);
		props.put(PROP_KEY + port + "/" + PORT_REMOTE_HOST_PROP_KEY,
			PORT_REMOTE_HOST_PROP_VAL);
		props.put(PROP_KEY + port + "/" + TLS_REQUIRED_PROP_KEY,
			TLS_REQUIRED_PROP_VAL);
		Map<String, Object> extra = getParamsForPort(port);
		if (extra != null) {
			for (Map.Entry<String, Object> entry : extra.entrySet()) {
				props.put(PROP_KEY + port + "/" + entry.getKey(), entry.getValue());
			} // end of for ()
		} // end of if (extra != null)
	}

	private void releaseListeners() {
		for (ConnectionListenerImpl cli: pending_open) {
			connectThread.removeConnectionOpenListener(cli);
		}
		pending_open.clear();
	}

	public void release() {
		delayedTasks.cancel();
		releaseListeners();
		super.release();
	}

	public void start() {
		super.start();
		delayedTasks = new Timer("DelayedTasks", true);
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		releaseListeners();
		int[] ports = (int[])props.get(PORTS_PROP_KEY);
		if (ports != null) {
			for (int i = 0; i < ports.length; i++) {
				Map<String, Object> port_props = new LinkedHashMap<String, Object>();
				for (Map.Entry<String, Object> entry : props.entrySet()) {
					if (entry.getKey().startsWith(PROP_KEY + ports[i])) {
						int idx = entry.getKey().lastIndexOf('/');
						String key = entry.getKey().substring(idx + 1);
						log.config("Adding port property key: "
							+ key + "=" + entry.getValue());
						port_props.put(key, entry.getValue());
					} // end of if (entry.getKey().startsWith())
				} // end of for ()
				port_props.put(PORT_KEY, ports[i]);
				reconnectService(port_props, startDelay);
			} // end of for (int i = 0; i < ports.length; i++)
		} // end of if (ports != null)
    if ((Boolean)props.get(TLS_USE_PROP_KEY)) {
			Map<String, String> tls_params = new LinkedHashMap<String, String>();
			tls_params.put(SSL_CONTAINER_CLASS_KEY,
				(String)props.get(TLS_CONTAINER_CLASS_PROP_KEY));
			tls_params.put(DEFAULT_DOMAIN_CERT_KEY,
				(String)props.get(TLS_DEF_CERT_PROP_KEY));
			tls_params.put(JKS_KEYSTORE_FILE_KEY,
				(String)props.get(TLS_KEYS_STORE_PROP_KEY));
			tls_params.put(JKS_KEYSTORE_PWD_KEY,
				(String)props.get(TLS_KEYS_STORE_PASSWD_PROP_KEY));
			tls_params.put(TRUSTSTORE_FILE_KEY,
				(String)props.get(TLS_TRUSTS_STORE_PROP_KEY));
			tls_params.put(TRUSTSTORE_PWD_KEY,
				(String)props.get(TLS_TRUSTS_STORE_PASSWD_PROP_KEY));
			tls_params.put(SERVER_CERTS_DIR_KEY,
				(String)props.get(TLS_SERVER_CERTS_DIR_PROP_KEY));
			tls_params.put(TRUSTED_CERTS_DIR_KEY,
				(String)props.get(TLS_TRUSTED_CERTS_DIR_PROP_KEY));
			tls_params.put(ALLOW_SELF_SIGNED_CERTS_KEY,
				(String)props.get(TLS_ALLOW_SELF_SIGNED_CERTS_PROP_KEY));
			tls_params.put(ALLOW_INVALID_CERTS_KEY,
				(String)props.get(TLS_ALLOW_INVALID_CERTS_PROP_KEY));
			TLSUtil.configureSSLContext(getName(), tls_params);
    } // end of if (use.equalsIgnoreCase())
	}

	protected void startService(Map<String, Object> port_props) {
		ConnectionListenerImpl cli = new ConnectionListenerImpl(port_props);
		if (cli.getConnectionType() == ConnectionType.accept) {
			pending_open.add(cli);
		}
		connectThread.addConnectionOpenListener(cli);
	}

	protected void reconnectService(final Map<String, Object> port_props,
		long delay) {
		log.finer("Reconnecting service for: " + getName()
			+ ", scheduling next try in " + (delay / 1000) + "secs");
		delayedTasks.schedule(new TimerTask() {
				public void run() {
					String host = (String)port_props.get(PORT_REMOTE_HOST_PROP_KEY);
					int port = (Integer)port_props.get(PORT_KEY);
					log.fine("Reconnecting service for component: " + getName()
						+ ", to remote host: " + host + " on port: " + port);
					startService(port_props);
				}
			}, delay);
	}

	protected int[] getDefPlainPorts() {
		return null;
	}

	protected int[] getDefSSLPorts() {
		return null;
	}

	protected Map<String, Object> getParamsForPort(int port) {
		return null;
	}

	// Implementation of tigase.net.PacketListener

	/**
	 * Describe <code>packetsReady</code> method here.
	 *
	 * @param s an <code>IOService</code> value
	 */
	@SuppressWarnings({"unchecked"})
	public void packetsReady(IOService s) throws IOException {
		log.finest("packetsReady called");
		IO serv = (IO)s;
		packetsReady(serv);
	}

	public void packetsReady(IO serv) throws IOException {
		writePacketsToSocket(serv, processSocketData(serv));
	}

	protected void writePacketsToSocket(IO serv, Queue<Packet> packets) {
		if (packets != null && packets.size() > 0) {
			Packet p = null;
			while ((p = packets.poll()) != null) {
				serv.addPacketToSend(p);
			} // end of for ()
			try {
				serv.processWaitingPackets();
				readThread.addSocketService(serv);
			} catch (Exception e) {
				log.log(Level.WARNING, "Exception during writing packets: ", e);
				try {
					serv.stop();
				} catch (Exception e1) {
					log.log(Level.WARNING, "Exception stopping XMPPIOService: ", e1);
				} // end of try-catch
			} // end of try-catch
		}
	}

	protected boolean writePacketToSocket(IO ios, Packet p) {
		if (ios != null) {
			ios.addPacketToSend(p);
			try {
				ios.processWaitingPackets();
				readThread.addSocketService(ios);
				return true;
			} catch (Exception e) {
				log.log(Level.WARNING, "Exception during writing packets: ", e);
				try {
					ios.stop();
				} catch (Exception e1) {
					log.log(Level.WARNING, "Exception stopping XMPPIOService: ", e1);
				} // end of try-catch
			} // end of try-catch
		} else {
			log.info("Can't find service for packet: <"
				+ p.getElemName() + "> " + p.getTo()
				+ ", service id: " + getServiceId(p));
		} // end of if (ios != null) else
		return false;
	}

	protected void writeRawData(IO ios, String data) {
		try {
			ios.writeRawData(data);
			readThread.addSocketService(ios);
		} catch (Exception e) {
			log.log(Level.WARNING, "Exception during writing data: " + data, e);
			try {
				ios.stop();
			} catch (Exception e1) {
				log.log(Level.WARNING, "Exception stopping XMPPIOService: ", e1);
			} // end of try-catch
		}
	}

	protected void writePacketToSocket(Packet p) {
		log.finer("Processing packet: " + p.getElemName()
			+ ", type: " + p.getType());
		log.finest("Writing packet to: " + p.getTo());
		IO ios = getXMPPIOService(p);
		writePacketToSocket(ios, p);
	}

	protected void writePacketToSocket(Packet p, String serviceId) {
		log.finer("Processing packet: " + p.getElemName()
			+ ", type: " + p.getType());
		log.finest("Writing packet to: " + p.getTo());
		IO ios = getXMPPIOService(serviceId);
		writePacketToSocket(ios, p);
	}

	protected IO getXMPPIOService(String serviceId) {
		return services.get(serviceId);
	}

	protected IO getXMPPIOService(Packet p) {
		return services.get(getServiceId(p));
	}

	public void processPacket(Packet packet) {
		writePacketToSocket(packet);
	}

	public abstract Queue<Packet> processSocketData(IO serv);

	@SuppressWarnings({"unchecked"})
	public void serviceStopped(IOService s) {
		IO ios = (IO)s;
		serviceStopped(ios);
	}

	public void serviceStopped(IO service) {
		synchronized(service) {
			String id = getUniqueId(service);
			log.finer(">>" + getName() + "<< Connection stopped: " + id);
			// id might be null if service is stopped in accept method due to
			// an exception during establishing TCP/IP connection
			IO serv = (id != null ? services.get(id) : null);
			if (serv == service) {
				services.remove(id);
			} else {
				// Is it at all possible to happen???
				// let's log it for now....
				log.warning(">>" + getName()
										+	"<< Attempt to stop incorrect service: " + id);
				Thread.dumpStack();
			}
		}
	}

	@TODO(note="Do something if service with the same unique ID is already started, possibly kill the old one...")
	public void serviceStarted(final IO service) {
		synchronized(services) {
			String id = getUniqueId(service);
			log.finer(">>" + getName() + "<< Connection started: " + id);
			IO serv = services.get(id);
			if (serv != null) {
				if (serv == service) {
					log.warning(getName()
						+ ": That would explain a lot, adding the same service twice, ID: "
						+ id);
				} else {
					// Is it at all possible to happen???
					// let's log it for now....
					log.warning(getName()
						+ ": Attempt to add different service with the same ID: " + id);
					// And stop the old service....
					serv.stop();
				}
			}
			services.put(id, service);
		}
	}

	protected String getUniqueId(IO serv) {
		return serv.getUniqueId();
	}

	protected String getServiceId(Packet packet) {
		return JIDUtils.getNodeResource(packet.getTo());
	}

	@SuppressWarnings({"unchecked"})
	public void streamClosed(XMPPIOService s) {
		IO serv = (IO)s;
		xmppStreamClosed(serv);
	}

	public abstract void xmppStreamClosed(IO serv);

	@SuppressWarnings({"unchecked"})
	public String streamOpened(XMPPIOService s, Map<String, String> attribs) {
		IO serv = (IO)s;
		return xmppStreamOpened(serv, attribs);
	}

	public abstract String xmppStreamOpened(IO s, Map<String, String> attribs);

	public List<StatRecord> getStatistics() {
		List<StatRecord> stats = super.getStatistics();
		stats.add(new StatRecord(getName(), "Open connections", "int",
				services.size(), Level.FINE));
// 		StringBuilder sb = new StringBuilder("All connected: ");
// 		for (IOService serv: services.values()) {
// 			sb.append("\nService ID: " + getUniqueId(serv)
// 				+ ", local-hostname: " + serv.getSessionData().get("local-hostname")
// 				+ ", remote-hostname: " + serv.getSessionData().get("remote-hostname")
// 				+ ", is-connected: " + serv.isConnected()
// 				+ ", connection-type: " + serv.connectionType());
// 		}
// 		log.finest(sb.toString());
		return stats;
	}

	protected abstract IO getXMPPIOServiceInstance();

	private class ConnectionListenerImpl implements ConnectionOpenListener {

		private Map<String, Object> port_props = null;

		private ConnectionListenerImpl(Map<String, Object> port_props) {
			this.port_props = port_props;
		}

		public int getPort() {
			return (Integer)port_props.get(PORT_KEY);
		}

		public String[] getIfcs() {
			return (String[])port_props.get(PORT_IFC_PROP_KEY);
		}

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

		public SocketType getSocketType() {
			return SocketType.valueOf(port_props.get(PORT_SOCKET_PROP_KEY).toString());
		}

		public void accept(SocketChannel sc) {

			IO serv = getXMPPIOServiceInstance();
			serv.setSSLId(getName());
			serv.setIOServiceListener(ConnectionManager.this);
			serv.setSessionData(port_props);
			try {
				serv.accept(sc);
				if (getSocketType() == SocketType.ssl) {
					serv.startSSL(false);
				} // end of if (socket == SocketType.ssl)
				serviceStarted(serv);
				readThread.addSocketService(serv);
			} catch (ConnectException e) {
				// Accept side for component service is not ready yet?
				// Let's wait for a few secs and try again.
				Integer reconnects = (Integer)port_props.get(MAX_RECONNECTS_PROP_KEY);
				if (reconnects != null) {
					int recon = reconnects.intValue();
					if (recon != 0) {
						port_props.put(MAX_RECONNECTS_PROP_KEY, (--recon));
						reconnectService(port_props, connectionDelay);
					} // end of if (recon != 0)
				} else {
					//System.out.println(port_props.toString());
					//e.printStackTrace();
					//serv.stop();
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Can not accept connection.", e);
				serv.stop();
			} // end of try-catch
		}

	}

	protected abstract long getMaxInactiveTime();

	/**
	 * Looks in all established connections and checks whether any of them
	 * is dead....
	 *
	 */
	private class Watchdog implements Runnable {

		private String getCID(IO service) {
			String local_hostname =
				(String)service.getSessionData().get("local-hostname");
			String remote_hostname =
				(String)service.getSessionData().get("remote-hostname");
			return JIDUtils.getJID(
				(local_hostname != null ? local_hostname : "NULL"),
				(remote_hostname != null ? remote_hostname : "NULL"),
				service.connectionType().toString());
		}

		public void run() {
			while (true) {
				try {
					// Sleep for 1 minute
					Thread.sleep(30*MINUTE);
					// Walk through all connections and check whether they are
					// really alive...., try to send space for each service which
					// is inactive for hour or more and close the service
					// on Exception
					long curr_time = System.currentTimeMillis();
					for (IO service: services.values()) {
						//						service = (XMPPIOService)serv;
						long lastTransfer = service.getLastTransferTime();
						try {
							if (curr_time - lastTransfer >= getMaxInactiveTime()) {
								// Stop the service is max keep-alive time is acceeded
								// for non-active connections.
								log.info(getName()
									+ ": Max inactive time exceeded, stopping: "
									+ getUniqueId(service)
									+ ", CID: " + getCID(service));
								service.stop();
							} else {
								if (curr_time - lastTransfer >= HOUR) {
									// At least once an hour check if the connection is
									// still alive.
									service.writeRawData(" ");
								}
							}
						} catch (Exception e) {
							// Close the service....
							try {
								if (service != null) {
									log.info(getName()
										+ "Found dead connection, stopping: "
										+ getUniqueId(service)
										+ ", CID: " + getCID(service));
									service.stop();
								}
							} catch (Exception ignore) {
								// Do nothing here as we expect Exception to be thrown here...
							}
						}
					}
				} catch (InterruptedException e) { /* Do nothing here */ }
			}
		}
	}

} // ConnectionManager
