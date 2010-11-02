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

package tigase.net;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class ConnectionOpenThread here.
 *
 *
 * Created: Wed Jan 25 23:51:28 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConnectionOpenThread implements Runnable {
	private static final Logger log = Logger.getLogger(ConnectionOpenThread.class.getName());
	private static ConnectionOpenThread acceptThread = null;

	/**
	 *
	 */
	public static long throttling = 10000;

	//~--- fields ---------------------------------------------------------------

	protected long accept_counter = 0;
	private long lastSecondConnections = 0;
	private Selector selector = null;
	private boolean stopping = false;
	private Timer timer = null;
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
				lastSecondConnections = 0;
			}
		}, 1000, 1000);

		try {
			selector = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		}    // end of try-catch
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public static ConnectionOpenThread getInstance() {
		Long new_throttling = Long.getLong("new-connections-throttling");

		if (new_throttling != null) {
			throttling = new_throttling;
			log.log(Level.WARNING, "New connections throttling set to: {0}", throttling);
		}

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

	/**
	 * Method description
	 *
	 */
	@Override
	public void run() {
		while ( !stopping) {
			try {
				selector.select();

				// Set<SelectionKey> selected_keys = selector.selectedKeys();
				// for (SelectionKey sk : selected_keys) {
				for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
					SelectionKey sk = (SelectionKey) i.next();

					i.remove();

					SocketChannel sc = null;

					if ((sk.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();

						sc = nextReady.accept();

						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_ACCEPT");
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
						++lastSecondConnections;

						// We have to catch exception here as sometimes socket is closed
						// or connection is broken before we start configuring it here
						// then whatever we do on the socket it throws an exception
						try {
							if (lastSecondConnections <= throttling) {
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
							} else {
								if (log.isLoggable(Level.FINER)) {
									log.log(Level.FINER, "New connections throttling level exceeded, closing: {0}",
											sc);
								}

								sc.close();
							}
						} catch (java.net.SocketException e) {
							log.log(Level.INFO, "Socket closed instantly after it had been opened?", e);
						}
					} else {
						log.warning("Can't obtain socket channel from selection key.");
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

	private void addAllWaiting() throws IOException {
		ConnectionOpenListener al = null;

		while ((al = waiting.poll()) != null) {
			try {
				addPort(al);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error: {0} creating connection for: {1}", new Object[] { e, al });
			}    // end of try-catch
		}      // end of for ()
	}

	private void addISA(InetSocketAddress isa, ConnectionOpenListener al) throws IOException {
		switch (al.getConnectionType()) {
			case accept :
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Setting up 'accept' channel...");
				}

				ServerSocketChannel ssc = ServerSocketChannel.open();

				ssc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
				ssc.configureBlocking(false);
				ssc.socket().bind(isa);
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
		if ((al.getIfcs() == null) || (al.getIfcs().length == 0) || al.getIfcs()[0].equals("ifc")
				|| al.getIfcs()[0].equals("*")) {
			addISA(new InetSocketAddress(al.getPort()), al);
		} else {
			for (String ifc : al.getIfcs()) {
				addISA(new InetSocketAddress(ifc, al.getPort()), al);
			}    // end of for ()
		}      // end of if (ip == null || ip.equals("")) else
	}
}    // ConnectionOpenThread


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
