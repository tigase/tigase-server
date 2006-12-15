/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Describe class SocketIO here.
 *
 *
 * Created: Sat May 14 07:18:30 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SocketIO implements IOInterface {

  private static Logger log = Logger.getLogger("tigase.io.SocketIO");

  private SocketChannel channel = null;
  private int bytesRead = 0;

  /**
   * Creates a new <code>SocketIO</code> instance.
   *
   */
  public SocketIO(final SocketChannel sock) throws IOException {
    channel = sock;
		channel.configureBlocking(false);
		channel.socket().setSoLinger(false, 0);
		channel.socket().setReuseAddress(true);
  }

  public SocketChannel getSocketChannel() {
    return channel;
  }

  public void stop() throws IOException {
    channel.close();
  }

  public boolean isConnected() {
    return channel.isConnected();
  }

  public int write(final ByteBuffer buff) throws IOException {
    int result = 0;
    while (buff.hasRemaining()) {
      final int res = channel.write(buff);
      if (res == -1) {
        throw new EOFException("Channel has been closed.");
      } // end of if (res == -1)
      result += res;
    } // end of while (out.hasRemaining())
    log.fine("Wrote to channel " + result + " bytes.");
    return result;
  }

  public ByteBuffer read(final ByteBuffer buff) throws IOException {
    bytesRead = channel.read(buff);
    log.finest("Read from channel " + bytesRead + " bytes.");
    if (bytesRead == -1) {
      throw new EOFException("Channel has been closed.");
    } // end of if (result == -1)
    return buff;
  }

  public int bytesRead() {
    return bytesRead;
  }

	public int getInputPacketSize() throws IOException {
		return channel.socket().getReceiveBufferSize();
	}

} // SocketIO
