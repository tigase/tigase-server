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
package tigase.io;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Describe class SimpleReaderThread here.
 *
 *
 * Created: Sun Aug  6 22:34:40 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SimpleReaderThread extends Thread {

  private static final Logger log =
		Logger.getLogger("tigase.io.SimpleReaderThread");

  private boolean stopping = false;

  private final ConcurrentLinkedQueue<IOInterface> waiting =
    new ConcurrentLinkedQueue<IOInterface>();
  private final ConcurrentLinkedQueue<IOInterface> for_removal =
    new ConcurrentLinkedQueue<IOInterface>();

  private Selector clientsSel = null;
	private IOInterfaceHandler handler = null;

	/**
	 * Creates a new <code>SimpleReaderThread</code> instance.
	 *
	 */
	public SimpleReaderThread(IOInterfaceHandler handler) throws IOException {
		this.handler = handler;
		clientsSel = Selector.open();
		setName("SimpleReaderThread");
	}

	public void addIOInterface(IOInterface s) {
    waiting.offer(s);
    clientsSel.wakeup();
	}

	public void removeIOInterface(IOInterface s) {
		SelectionKey key = s.getSocketChannel().keyFor(clientsSel);
		if (key != null && key.attachment() == s) {
			key.cancel();
		} // end of if (key != null)
	}

  private void addAllWaiting() throws IOException {
    IOInterface s = null;
    while ((s = waiting.poll()) != null) {
      final SocketChannel sc = s.getSocketChannel();
      try {
        sc.register(clientsSel, SelectionKey.OP_READ, s);
			} catch (Exception e) {
        // Ignore such channel
      } // end of try-catch
    } // end of for ()
  }

	// Implementation of java.lang.Runnable

	/**
	 * Describe <code>run</code> method here.
	 *
	 */
	public void run() {
    while (!stopping) {
      try {
        clientsSel.select();
        for (Iterator i = clientsSel.selectedKeys().iterator(); i.hasNext();) {
					SelectionKey sk = (SelectionKey)i.next();
					i.remove();
          IOInterface s = (IOInterface)sk.attachment();
					sk.cancel();
					handler.handleIOInterface(s);
        }
				// Clean-up cancelled keys...
        clientsSel.selectNow();
        addAllWaiting();
      } catch (Exception e) {
        log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
        stopping = true;
      }
    }
    System.err.println("SimpleReaderThread stopped!");
    System.exit(2);
	}

	public interface IOInterfaceHandler {

		void handleIOInterface(IOInterface ioIfc);

	}

} // SimpleReaderThread
