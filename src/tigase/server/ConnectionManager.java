/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
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
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.io.TLSUtil;
import tigase.net.ConnectionOpenListener;
import tigase.net.ConnectionOpenThread;
import tigase.net.ConnectionType;
import tigase.net.IOService;
import tigase.net.SocketReadThread;
import tigase.net.SocketType;
import tigase.util.JID;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPIOServiceListener;
import tigase.annotations.TODO;

/**
 * Describe class ConnectionManager here.
 *
 *
 * Created: Sun Jan 22 22:52:58 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConnectionManager extends AbstractMessageReceiver
	implements XMPPIOServiceListener {

	private static final Logger log =
    Logger.getLogger("tigase.server.ConnectionManager");

	public static final String PORT_KEY = "port-no";
	public static final String PROP_KEY = "connections/";
	public static final String PORTS_PROP_KEY = PROP_KEY + "ports";
	public static final String PORT_TYPE_PROP_KEY = "type";
	public static final String PORT_SOCKET_PROP_KEY = "socket";
	public static final String PORT_IFC_PROP_KEY = "ifc";
	public static final String[] PORT_IFC_PROP_VAL = {"*"};
	public static final String PORT_CLASS_PROP_KEY = "class";
	public static final String PORT_REMOTE_HOST_PROP_KEY = "remote-host";
	public static final String PORT_REMOTE_HOST_PROP_VAL = "localhost";
	public static final String TLS_PROP_KEY = PROP_KEY + "tls/";
	public static final String TLS_USE_PROP_KEY = TLS_PROP_KEY + "use";
	public static final boolean TLS_USE_PROP_VAL = true;
	public static final String TLS_REQUIRED_PROP_KEY = "tls/required";
	public static final boolean TLS_REQUIRED_PROP_VAL = false;
	public static final String TLS_KEYS_STORE_PROP_KEY =
		TLS_PROP_KEY + "keys-store";
	public static final String TLS_KEYS_STORE_PROP_VAL = "certs/rsa-keystore";
	public static final String TLS_KEYS_STORE_PASSWD_PROP_KEY =
		TLS_PROP_KEY + "keys-store-password";
	public static final String TLS_KEYS_STORE_PASSWD_PROP_VAL =	"keystore";
	public static final String TLS_TRUSTS_STORE_PASSWD_PROP_KEY =
		TLS_PROP_KEY + "trusts-store-password";
	public static final String TLS_TRUSTS_STORE_PASSWD_PROP_VAL =	"truststore";
	public static final String TLS_TRUSTS_STORE_PROP_KEY =
		TLS_PROP_KEY + "trusts-store";
	public static final String TLS_TRUSTS_STORE_PROP_VAL = "certs/truststore";

	private static ConnectionOpenThread connectThread =
		ConnectionOpenThread.getInstance();
	private static SocketReadThread readThread = SocketReadThread.getInstance();
	private static Timer delayedTasks = new Timer("DelayedTasks", true);
	private Map<String, IOService> services =
		new ConcurrentSkipListMap<String, IOService>();
	protected static long connectionDelay = 2000;

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		props.put(TLS_USE_PROP_KEY, TLS_USE_PROP_VAL);
		props.put(TLS_KEYS_STORE_PROP_KEY, TLS_KEYS_STORE_PROP_VAL);
		props.put(TLS_KEYS_STORE_PASSWD_PROP_KEY, TLS_KEYS_STORE_PASSWD_PROP_VAL);
		props.put(TLS_TRUSTS_STORE_PROP_KEY, TLS_TRUSTS_STORE_PROP_VAL);
		props.put(TLS_TRUSTS_STORE_PASSWD_PROP_KEY,
			TLS_TRUSTS_STORE_PASSWD_PROP_VAL);

		int ports_size = 0;
		int[] ports = null;
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
		return props;
	}

	private void putDefPortParams(Map<String, Object> props, int port,
		SocketType sock) {
		props.put(PROP_KEY + port + "/" + PORT_TYPE_PROP_KEY,
			ConnectionType.accept);
		props.put(PROP_KEY + port + "/" + PORT_SOCKET_PROP_KEY,	sock);
		props.put(PROP_KEY + port + "/" + PORT_IFC_PROP_KEY, PORT_IFC_PROP_VAL);
		//		props.put(PROP_KEY + port + "/" + PORT_CLASS_PROP_KEY, getDefPortClass());
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

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		int[] ports = (int[])props.get(PORTS_PROP_KEY);
		if (ports != null) {
			for (int i = 0; i < ports.length; i++) {
				Map<String, Object> port_props = new TreeMap<String, Object>();
				for (Map.Entry<String, Object> entry : props.entrySet()) {
					if (entry.getKey().startsWith(PROP_KEY + ports[i])) {
						int idx = entry.getKey().lastIndexOf('/');
						String key = entry.getKey().substring(idx + 1);
						log.config("Adding port property key: " + key);
						port_props.put(key, entry.getValue());
					} // end of if (entry.getKey().startsWith())
				} // end of for ()
				port_props.put(PORT_KEY, ports[i]);
				reconnectService(port_props, connectionDelay);
			} // end of for (int i = 0; i < ports.length; i++)
		} // end of if (ports != null)
    if ((Boolean)props.get(TLS_USE_PROP_KEY)) {
			TLSUtil.configureSSLContext(getName(),
				(String)props.get(TLS_KEYS_STORE_PROP_KEY),
				(String)props.get(TLS_KEYS_STORE_PASSWD_PROP_KEY),
				(String)props.get(TLS_TRUSTS_STORE_PROP_KEY),
				(String)props.get(TLS_TRUSTS_STORE_PASSWD_PROP_KEY));
    } // end of if (use.equalsIgnoreCase())
	}

	protected void startService(Map<String, Object> port_props) {
		ConnectionListenerImpl cli = new ConnectionListenerImpl(port_props);
		connectThread.addConnectionOpenListener(cli);
	}

	protected void reconnectService(final Map<String, Object> port_props,
		long delay) {
		log.finer("Reconnecting service for: " + getName()
			+ ", scheduling next try in " + (delay / 1000) + "secs");
		delayedTasks.schedule(new TimerTask() {
				public void run() {
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

	protected String getDefPortClass() {
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
	public void packetsReady(IOService s) throws IOException {
		log.finest("packetsReady called");
		XMPPIOService serv = (XMPPIOService)s;
		writePacketsToSocket(serv, processSocketData(serv));
// 		writePacketsToSocket(s,
// 			processSocketData(getUniqueId(s), s.getSessionData(),
// 				((XMPPIOService)s).getReceivedPackets()));
	}

	protected void writePacketsToSocket(IOService s, Queue<Packet> packets)
		throws IOException {
		if (packets != null && packets.size() > 0) {
			Packet p = null;
			while ((p = packets.poll()) != null) {
				((XMPPIOService)s).addPacketToSend(p);
			} // end of for ()
			s.processWaitingPackets();
		}
	}

	protected void writePacketToSocket(IOService ios, Packet p) {
		if (ios != null) {
			((XMPPIOService)ios).addPacketToSend(p);
			try {
				ios.processWaitingPackets();
			} catch (Exception e) {
				log.log(Level.WARNING, "Exception during writing packets: ", e);
				try {
					ios.stop();
				} catch (Exception e1) {
					log.log(Level.WARNING, "Exception stopping XMPPIOService: ", e1);
				} // end of try-catch
			} // end of try-catch
		} // end of if (ios != null)
		else {
			log.info("Can't find service for packet: <"
				+ p.getElemName() + "> " + p.getTo()
				+ ", service id: " + getServiceId(p));
		} // end of if (ios != null) else
	}

	protected void writePacketToSocket(Packet p) {
		log.finer("Processing packet: " + p.getElemName()
			+ ", type: " + p.getType());
		log.finest("Writing packet to: " + p.getTo());
		IOService ios = getXMPPIOService(p);
		writePacketToSocket(ios, p);
	}

	protected void writePacketToSocket(Packet p, String serviceId) {
		log.finer("Processing packet: " + p.getElemName()
			+ ", type: " + p.getType());
		log.finest("Writing packet to: " + p.getTo());
		IOService ios = getXMPPIOService(serviceId);
		writePacketToSocket(ios, p);
	}

	protected XMPPIOService getXMPPIOService(String serviceId) {
		return (XMPPIOService)services.get(serviceId);
	}

	protected XMPPIOService getXMPPIOService(Packet p) {
		return (XMPPIOService)services.get(getServiceId(p));
	}

	public void processPacket(Packet packet) {
		writePacketToSocket(packet);
	}

	public abstract Queue<Packet> processSocketData(XMPPIOService serv);

	public void serviceStopped(final IOService service) {
		synchronized(service) {
			log.finer(">>" + getName() +
				"<< Connection stopped: " + getUniqueId(service));
			services.remove(getUniqueId(service));
		}
	}

	@TODO(note="Do something if service with the same unique ID is already started, possibly kill the old one...")
	public void serviceStarted(final IOService service) {
		synchronized(services) {
			log.finer(">>" + getName() +
				"<< Connection started: " + getUniqueId(service));
			services.put(getUniqueId(service), service);
		}
	}

	protected String getUniqueId(IOService serv) {
		return serv.getUniqueId();
	}

	protected String getServiceId(Packet packet) {
		return JID.getNodeResource(packet.getTo());
	}

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
			return
				ConnectionType.valueOf(port_props.get(PORT_TYPE_PROP_KEY).toString());
		}

		public SocketType getSocketType() {
			return SocketType.valueOf(port_props.get(PORT_SOCKET_PROP_KEY).toString());
		}

		public void accept(SocketChannel sc) {
			XMPPIOService serv = new XMPPIOService();
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
				Long reconnects = (Long)port_props.get("reconnects");
				if (reconnects != null) {
					long recon = reconnects.longValue();
					if (recon != 0) {
						port_props.put("reconnects", (--recon));
						reconnectService(port_props, connectionDelay);
					} // end of if (recon != 0)
				} // end of if (reconnects != null)
			} catch (Exception e) {
				log.log(Level.WARNING, "Can not accept connection.", e);
			} // end of try-catch
		}

	}

} // ConnectionManager
