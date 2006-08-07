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
import java.util.logging.SocketHandler;

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
public class TelnetServer implements SampleSocketThread.SocketHandler {

  private static final Logger log =	Logger.getLogger("tigase.io.TelnetServer");
  private static final Charset coder = Charset.forName("UTF-8");

	private SampleSocketThread reader = null;
	private IOInterface iosock = null;

	/**
	 * Creates a new <code>TelnetServer</code> instance.
	 *
	 */
	public TelnetServer(int port) throws IOException {
		reader = new SampleSocketThread(this);
		reader.start();
		reader.addForAccept(new InetSocketAddress(port));
	}

	public void handleIOInterface(IOInterface ioifc) throws IOException {
		ByteBuffer socketInput =
			ByteBuffer.allocate(ioifc.getSocketChannel().socket().getReceiveBufferSize());
		ByteBuffer tmpBuffer = ioifc.read(socketInput);
		if (ioifc.bytesRead() > 0) {
			tmpBuffer.flip();
			CharBuffer cb = coder.decode(tmpBuffer);
			tmpBuffer.clear();
			if (cb != null) {
				System.out.print(new String(cb.array()));
			} // end of if (cb != null)
		} // end of if (socketIO.bytesRead() > 0)
		reader.addIOInterface(ioifc);
	}

	public void handleSocketAccept(SocketChannel sc) throws IOException {
		iosock = new SocketIO(sc);
		reader.addIOInterface(iosock);
	}

	public void run() throws IOException {
		InputStreamReader reader = new InputStreamReader(System.in);
		char[] buff = new char[1];
		for (;;) {
			reader.read(buff);
			if (iosock != null) {
				ByteBuffer dataBuffer = coder.encode(CharBuffer.wrap(buff));
				iosock.write(dataBuffer);
			} // end of if (ioscok != null)
			else {
				System.err.println("Can't write to socket, no open socket.");
			} // end of if (ioscok != null) else
		} // end of for (;;)
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {
		int port = 7777; // Port number to provide service on
		TelnetServer server = new TelnetServer(port);
		server.run();
	}

} // Telnetserver
