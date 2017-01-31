/*
 * ConnectionOpenThread.java
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



package tigase.net;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.net.*;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Describe class ConnectionOpenThread here.
 *
 *
 * Created: Wed Jan 25 23:51:28 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConnectionOpenThread
				implements Runnable {
	/**
	 *
	 */
	public static final long def_5222_throttling = 200;

	/** Field description */
	public static final long def_5223_throttling = 50;

	/** Field description */
	public static final long def_5269_throttling = 100;

	/** Field description */
	public static final long            def_5280_throttling = 1000;
	private static ConnectionOpenThread acceptThread        = null;
	private static final Logger         log = Logger.getLogger(ConnectionOpenThread.class
			.getName());

	/** Field description */
	public static Map<Integer, PortThrottlingData> throttling =
			new ConcurrentHashMap<Integer, PortThrottlingData>(10);

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected long                                        accept_counter = 0;
	private Selector                                      selector       = null;
	private boolean                                       stopping       = false;
	private Timer                                         timer          = null;
	private ConcurrentLinkedQueue<ConnectionOpenListener> waiting =
			new ConcurrentLinkedQueue<ConnectionOpenListener>();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>ConnectionOpenThread</code> instance.
	 *
	 */
	private ConnectionOpenThread() {
		timer = new Timer("Connections open timer", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (PortThrottlingData portData : throttling.values()) {
					portData.lastSecondConnections = 0;
				}
			}
		}, 1000, 1000);
		try {
			selector = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		}    // end of try-catch
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param al
	 */
	public void addConnectionOpenListener(ConnectionOpenListener al) {
		waiting.offer(al);
		selector.wakeup();
	}

	/**
	 * Method description
	 *
	 *
	 * @param al
	 */
	public void removeConnectionOpenListener(ConnectionOpenListener al) {
		for (SelectionKey key : selector.keys()) {
			if (al == key.attachment()) {
				try {
					key.cancel();

					SelectableChannel channel = key.channel();

					channel.close();
				} catch (Exception e) {
					log.log(Level.WARNING, "Exception during removing connection listener.", e);
				}

				break;
			}
		}
	}

	@Override
	public void run() {
		while (!stopping) {
			try {
				int select = selector.select();

				if ( log.isLoggable( Level.FINEST ) ){
					log.log( Level.FINEST, "Selected: " + select + " from selector: " + selector );
				}

				for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
					SelectionKey sk = (SelectionKey) i.next();

					i.remove();

					SocketChannel sc        = null;
					boolean       throttled = false;
					int           port_no   = 0;

					if ((sk.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();

						port_no = nextReady.socket().getLocalPort();
						sc      = nextReady.accept();
						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_ACCEPT");
						}

						PortThrottlingData port_throttling = throttling.get(port_no);

						if (port_throttling != null) {
							++port_throttling.lastSecondConnections;
							if (port_throttling.lastSecondConnections > port_throttling.throttling) {
								if (log.isLoggable(Level.INFO)) {
									log.log(Level.INFO,
											"New connections throttling level {0} exceeded, closing: {0}", new Object[] {port_throttling.lastSecondConnections, sc});
								}
								sc.close();
								sc        = null;
								throttled = true;
							}
						} else {

							// Hm, this should not happen actually
							log.log(Level.WARNING, "Throttling not configured for port: {0}", port_no);
						}
					}    // end of if (sk.readyOps() & SelectionKey.OP_ACCEPT)
					if ((sk.readyOps() & SelectionKey.OP_CONNECT) != 0) {
						sk.cancel();
						sc = (SocketChannel) sk.channel();
						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_CONNECT");
						}
					}    // end of if (sk.readyOps() & SelectionKey.OP_ACCEPT)
					if (sc != null) {

						// We have to catch exception here as sometimes socket is closed
						// or connection is broken before we start configuring it here
						// then whatever we do on the socket it throws an exception
						try {
							sc.configureBlocking(false);
							sc.socket().setSoLinger(false, 0);
							sc.socket().setReuseAddress(true);
							if (log.isLoggable(Level.FINER)) {
								log.log(Level.FINER, "Registered new client socket: {0}", sc);
							}

							ConnectionOpenListener al = (ConnectionOpenListener) sk.attachment();

							sc.socket().setTrafficClass(al.getTrafficClass());
							sc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
							al.accept(sc);
						} catch (java.net.SocketException e) {
							log.log(Level.INFO, "Socket closed instantly after it had been opened?", e);

							ConnectionOpenListener al = (ConnectionOpenListener) sk.attachment();

							al.accept(sc);
						}
					} else {
						log.log(Level.INFO,
								"Can not obtain socket channel from selection key, throttling activated = {0}, for port: {1}",
								new Object[] { throttled, port_no });
					}    // end of if (sc != null) else
					++accept_counter;
				}
				addAllWaiting();
			} catch (IOException e) {
				log.log(Level.SEVERE, "Server I/O error.", e);

				// stopping = true;
			}        // end of catch
					catch (Exception e) {
				log.log(Level.SEVERE, "Other service exception.", e);

				// stopping = true;
			}        // end of catch
		}
	}

	/**
	 * Method description
	 *
	 */
	public void start() {
		Thread t = new Thread(this);

		t.setName("ConnectionOpenThread");
		t.start();
	}

	/**
	 * Method description
	 *
	 */
	public void stop() {
		stopping = true;
		selector.wakeup();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public static ConnectionOpenThread getInstance() {

		// Long new_throttling = Long.getLong("new-connections-throttling");
//  if (new_throttling != null) {
//    throttling = new_throttling;
//    log.log(Level.WARNING, "New connections throttling set to: {0}", throttling);
//  }
		if (acceptThread == null) {
			acceptThread = new ConnectionOpenThread();

			Thread thrd = new Thread(acceptThread);

			thrd.setName("ConnectionOpenThread");
			thrd.start();
			if (log.isLoggable(Level.FINER)) {
				log.finer("ConnectionOpenThread started.");
			}
		}    // end of if (acceptThread == null)

		return acceptThread;
	}

	//~--- methods --------------------------------------------------------------

	private void addAllWaiting() throws IOException {
		ConnectionOpenListener al = null;

		while ((al = waiting.poll()) != null) {
			try {
				addPort(al);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error: creating connection for: " + al, e);
				al.accept(null);
			}    // end of try-catch
		}      // end of for ()
	}

	private void addISA(InetSocketAddress isa, ConnectionOpenListener al)
					throws IOException {
		switch (al.getConnectionType()) {
		case accept :
			long port_throttling = getThrottlingForPort(isa.getPort());

			throttling.put(isa.getPort(), new PortThrottlingData(port_throttling));
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Setting up throttling for the port {0} to {1} connections per second. isa: {2}",
						new Object[] { isa.getPort(), port_throttling, isa });
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting up 'accept' channel...");
			}

			ServerSocketChannel ssc = ServerSocketChannel.open();

			ssc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
			ssc.configureBlocking(false);
			ssc.socket().bind(isa, (int) (port_throttling));
			ssc.register(selector, SelectionKey.OP_ACCEPT, al);

			break;

		case connect :
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Setting up ''connect'' channel for: {0}/{1}",
						new Object[] { isa.getAddress(),
						isa.getPort() });
			}

			SocketChannel sc = SocketChannel.open();

			sc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
			sc.socket().setTrafficClass(al.getTrafficClass());
			sc.configureBlocking(false);
			sc.connect(isa);
			sc.register(selector, SelectionKey.OP_CONNECT, al);

			break;

		default :
			log.log(Level.WARNING, "Unknown connection type: {0}", al.getConnectionType());

			break;
		}    // end of switch (al.getConnectionType())
	}

	private void addPort(ConnectionOpenListener al) throws IOException {
		if ((al.getConnectionType() == ConnectionType.connect) && (al.getRemoteAddress() !=
				null)) {
			addISA(al.getRemoteAddress(), al);
		} else if ((al.getIfcs() == null) || (al.getIfcs().length == 0) || al.getIfcs()[0]
				.equals("ifc") || al.getIfcs()[0].equals("*")) {
			addISA(new InetSocketAddress(al.getPort()), al);
		} else {
			for (String ifc : al.getIfcs()) {
				addISA(new InetSocketAddress(ifc, al.getPort()), al);
			}    // end of for ()
		}      // end of if (ip == null || ip.equals("")) else
	}

	//~--- get methods ----------------------------------------------------------

	private long getThrottlingForPort(int port) {
		long result = def_5222_throttling;

		switch (port) {
		case 5223 :
			result = def_5223_throttling;

			break;

		case 5269 :
			result = def_5269_throttling;

			break;

		case 5280 :
			result = def_5280_throttling;

			break;
		}

		String throttling_prop = System.getProperty("new-connections-throttling");

		if (throttling_prop != null) {
			String[] all_ports_thr = throttling_prop.split(",");

			for (String port_thr : all_ports_thr) {
				String[] port_thr_ar = port_thr.split(":");

				if (port_thr_ar.length == 2) {
					try {
						int port_no = Integer.parseInt(port_thr_ar[0]);

						if (port_no == port) {
							return Long.parseLong(port_thr_ar[1]);
						}
					} catch (Exception e) {

						// bad configuration
						log.log(Level.WARNING,
								"Connections throttling configuration error, bad format, " +
								"check the documentation for a correct syntax, " +
								"port throttling config: {0}", port_thr);
					}
				} else {

					// bad configuration
					log.log(Level.WARNING,
							"Connections throttling configuration error, bad format, " +
							"check the documentation for a correct syntax, " +
							"port throttling config: {0}", port_thr);
				}
			}
		}

		return result;
	}

	//~--- inner classes --------------------------------------------------------

	private class PortThrottlingData {
		/** Field description */
		protected long lastSecondConnections = 0;

		/** Field description */
		protected long throttling;

		//~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 *
		 *
		 * @param throttling_prop
		 */
		private PortThrottlingData(long throttling_prop) {
			throttling = throttling_prop;
		}
	}
}    // ConnectionOpenThread


//~ Formatted in Tigase Code Convention on 13/07/06
