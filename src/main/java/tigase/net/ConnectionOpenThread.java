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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.Port;

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

  private static final Logger log =
		Logger.getLogger("tigase.net.ConnectionOpenThread");
	/**
	 * <code>RECEIVE_BUFFER_SIZE</code> defines a size for TCP/IP packets.
	 * XMPP data packets are quite small usually, below 1kB so we don't need
	 * big TCP/IP data buffers.
	 */
	private static final int RECEIVE_BUFFER_SIZE = 2*1024;

  protected long accept_counter = 0;

	private static ConnectionOpenThread acceptThread = null;

  private boolean stopping = false;
  private Selector selector = null;

  private ConcurrentLinkedQueue<ConnectionOpenListener> waiting =
    new ConcurrentLinkedQueue<ConnectionOpenListener>();

	/**
	 * Creates a new <code>ConnectionOpenThread</code> instance.
	 *
	 */
	private ConnectionOpenThread() {
		try {
			selector = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		} // end of try-catch
	}

	public static ConnectionOpenThread getInstance() {
		if (acceptThread == null) {
			acceptThread = new ConnectionOpenThread();
			Thread thrd = new Thread(acceptThread);
			thrd.setName("ConnectionOpenThread");
			thrd.start();
			log.finer("ConnectionOpenThread started.");
		} // end of if (acceptThread == null)
		return acceptThread;
	}

	public void addConnectionOpenListener(ConnectionOpenListener al) {
    waiting.offer(al);
		selector.wakeup();
	}

	public void removeConnectionOpenListener(ConnectionOpenListener al) {
		for (SelectionKey key: selector.keys()) {
			if (al == key.attachment()) {
				try {
					key.cancel();
					SelectableChannel channel = key.channel();
					channel.close();
				} catch (Exception e) {
					log.log(Level.WARNING,
						"Exception during removing connection listener.", e);
				}
				break;
			}
		}
	}

	private void addPort(ConnectionOpenListener al)
		throws IOException {

		if (al.getIfcs() == null || al.getIfcs().length == 0
			|| al.getIfcs()[0].equals("ifc") || al.getIfcs()[0].equals("*")) {
			addISA(new InetSocketAddress(al.getPort()), al);
		} // end of if (ip == null || ip.equals(""))
		else {
			for (String ifc: al.getIfcs()) {
				addISA(new InetSocketAddress(ifc, al.getPort()), al);
			} // end of for ()
		} // end of if (ip == null || ip.equals("")) else
	}

	private void addISA(InetSocketAddress isa, ConnectionOpenListener al)
		throws IOException {
		switch (al.getConnectionType()) {
		case accept:
			log.finest("Setting up 'accept' channel...");
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.socket().setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
			ssc.configureBlocking(false);
			ssc.socket().bind(isa);
			ssc.register(selector, SelectionKey.OP_ACCEPT, al);
			break;
		case connect:
			log.finest("Setting up 'connect' channel for: "
				+ isa.getAddress() + "/" + isa.getPort());
			SocketChannel sc = SocketChannel.open();
			sc.socket().setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
			sc.configureBlocking(false);
			sc.connect(isa);
			sc.register(selector, SelectionKey.OP_CONNECT, al);
			break;
		default:
			log.warning("Unknown connection type: " + al.getConnectionType());
			break;
		} // end of switch (al.getConnectionType())
	}

  private void addAllWaiting() throws IOException {

    ConnectionOpenListener al = null;
    while ((al = waiting.poll()) != null) {
			try {
				addPort(al);
			} catch (SocketException e) {
				log.warning("Error: " + e + " creating connection for: " + al.getPort());
			} // end of try-catch
    } // end of for ()

  }

	public void run() {

    while (!stopping) {
      try {
        selector.select();
				//        Set<SelectionKey> selected_keys = selector.selectedKeys();
				//        for (SelectionKey sk : selected_keys) {
        for (Iterator i = selector.selectedKeys().iterator(); i.hasNext();) {
					SelectionKey sk = (SelectionKey)i.next();
					i.remove();
					SocketChannel sc = null;
					if ((sk.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						ServerSocketChannel nextReady = (ServerSocketChannel)sk.channel();
						sc = nextReady.accept();
						log.finest("OP_ACCEPT");
					} // end of if (sk.readyOps() & SelectionKey.OP_ACCEPT)
					if ((sk.readyOps() & SelectionKey.OP_CONNECT) != 0) {
						sk.cancel();
						sc = (SocketChannel)sk.channel();
						log.finest("OP_CONNECT");
					} // end of if (sk.readyOps() & SelectionKey.OP_ACCEPT)
					if (sc != null) {
						// We have to catch exception here as sometimes socket is closed
						// or connection is broken before we start configuring it here
						// then whatever we do on the socket it throws an exception
						try {
							sc.configureBlocking(false);
							sc.socket().setSoLinger(false, 0);
							sc.socket().setReuseAddress(true);
							log.finer("Registered new client socket: "+sc);
							ConnectionOpenListener al = (ConnectionOpenListener)sk.attachment();
							al.accept(sc);
						} catch (java.net.SocketException e) {
							log.log(Level.INFO,
								"Soket closed instantly after it had been opened?", e);
						}
					} else {
						log.warning("Can't obtain socket channel from selection key.");
					} // end of if (sc != null) else
          ++accept_counter;
        }
				addAllWaiting();
      } catch (IOException e) {
        log.log(Level.SEVERE, "Server I/O error.", e);
        //stopping = true;
      } // end of catch
      catch (Exception e) {
        log.log(Level.SEVERE,
          "Other service exception.", e);
        //stopping = true;
      } // end of catch
    }
  }

	public void start() {
    Thread t = new Thread(this);
    t.setName("ConnectionOpenThread");
    t.start();
	}

	public void stop() {
		stopping = true;
		selector.wakeup();
	}

} // ConnectionOpenThread
