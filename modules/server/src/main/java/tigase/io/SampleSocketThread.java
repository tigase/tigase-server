/*
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class SampleSocketThread here.
 *
 *
 * Created: Sun Aug  6 22:34:40 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SampleSocketThread extends Thread {

  private static final Logger log =
		Logger.getLogger("tigase.io.SampleSocketThread");

  private boolean stopping = false;

  private final ConcurrentLinkedQueue<IOInterface> waiting =
    new ConcurrentLinkedQueue<IOInterface>();
	private final ConcurrentLinkedQueue<InetSocketAddress> waiting_accept =
    new ConcurrentLinkedQueue<InetSocketAddress>();
  private final ConcurrentLinkedQueue<IOInterface> for_removal =
    new ConcurrentLinkedQueue<IOInterface>();

  private Selector clientSel = null;
	private SocketHandler handler = null;

	/**
	 * Creates a new <code>SampleSocketThread</code> instance.
	 *
	 *
	 * @param handler
	 * @throws IOException
	 */
	public SampleSocketThread(SocketHandler handler) throws IOException {
		this.handler = handler;
		clientSel = Selector.open();
		setName("SampleSocketThread");
	}

	public void addIOInterface(IOInterface s) {
    waiting.offer(s);
    clientSel.wakeup();
	}

	public void addForAccept(InetSocketAddress isa) {
		waiting_accept.offer(isa);
		clientSel.wakeup();
	}

	public void removeIOInterface(IOInterface s) {
		SelectionKey key = s.getSocketChannel().keyFor(clientSel);
		if (key != null && key.attachment() == s) {
			key.cancel();
		} // end of if (key != null)
	}

  private void addAllWaiting() throws IOException {
    IOInterface s = null;
    while ((s = waiting.poll()) != null) {
      final SocketChannel sc = s.getSocketChannel();
      try {
        sc.register(clientSel, SelectionKey.OP_READ, s);
			} catch (Exception e) {
        // Ignore such channel
      } // end of try-catch
    } // end of for ()
		InetSocketAddress isa = null;
		while ((isa = waiting_accept.poll()) != null) {
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.socket().bind(isa);
			ssc.register(clientSel, SelectionKey.OP_ACCEPT, null);
		} // end of while (isa = waiting_accept.poll() != null)
  }

	// Implementation of java.lang.Runnable

	@Override
	public void run() {
    while (!stopping) {
      try {
        clientSel.select();
        for (Iterator i = clientSel.selectedKeys().iterator(); i.hasNext();) {
					SelectionKey sk = (SelectionKey)i.next();
					i.remove();
					if ((sk.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						ServerSocketChannel nextReady = (ServerSocketChannel)sk.channel();
						SocketChannel sc = nextReady.accept();
            			if (log.isLoggable(Level.FINER)) {
            				log.finer("Registered new client socket: "+sc);
                        }
						handler.handleSocketAccept(sc);
					}
					if ((sk.readyOps() & SelectionKey.OP_CONNECT) != 0) {
						// Not implemented yet
					}
					if ((sk.readyOps() & SelectionKey.OP_READ) != 0) {
						IOInterface s = (IOInterface)sk.attachment();
						sk.cancel();
						handler.handleIOInterface(s);
					}
        }
				// Clean-up cancelled keys...
        clientSel.selectNow();
        addAllWaiting();
      } catch (Exception e) {
        log.log(Level.SEVERE,
					"SampleSocketThread I/O error, can't continue my work.", e);
        stopping = true;
      }
    }
    System.err.println("SampleSocketThread stopped!");
    System.exit(2);
	}

	public interface SocketHandler {

		void handleIOInterface(IOInterface ioIfc) throws IOException;

		void handleSocketAccept(SocketChannel sc) throws IOException;

	}

} // SampleSocketThread
