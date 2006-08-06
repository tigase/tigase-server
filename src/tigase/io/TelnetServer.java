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
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is sample class demonstrating how to use <code>tigase.io</code> library
 * for TLS/SSL server connection. This is simple telnet server class which can
 * be run to receive plain connections or SSL connections.
 *
 *
 * Created: Sun Aug  6 22:27:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TelnetServer {

  private static final Logger log =	Logger.getLogger("tigase.io.TelnetServer");
  private static final Charset coder = Charset.forName("UTF-8");

	private ReaderThread reader = null;

	/**
	 * Creates a new <code>TelnetServer</code> instance.
	 *
	 */
	public TelnetServer() {

	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) {

	}

	protected class ReaderThread extends Thread {

		private Selector readerSelector = null;

		public ReaderThread(IOInterface io) throws Exception {
			super();
			setName("ReaderThread");
			readerSelector = Selector.open();
			io.getSocketChannel().register(readerSelector, SelectionKey.OP_READ, io);
		}

		public void run() {
      for (;;) {
				try {
					readerSelector.select();
					for (Iterator i = readerSelector.selectedKeys().iterator(); i.hasNext();) {
						SelectionKey sk = (SelectionKey)i.next();
						i.remove();
						// Read what is wating for reading
						IOInterface s = (IOInterface)sk.attachment();
						ByteBuffer socketInput =
							ByteBuffer.allocate(s.getSocketChannel().socket().getReceiveBufferSize());
						ByteBuffer tmpBuffer = s.read(socketInput);
						if (s.bytesRead() > 0) {
							tmpBuffer.flip();
							CharBuffer cb = coder.decode(tmpBuffer);
							tmpBuffer.clear();
							if (cb != null) {
								System.out.print(new String(cb.array()));
							} // end of if (cb != null)
						} // end of if (socketIO.bytesRead() > 0)
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
				}
      } // end of for ()
		}

	}

} // Telnetserver
