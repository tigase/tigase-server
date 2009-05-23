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
package tigase.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	private Queue<ByteBuffer> dataToSend =
		new ConcurrentLinkedQueue<ByteBuffer>();

  private SocketChannel channel = null;
  private int bytesRead = 0;

  /**
   * Creates a new <code>SocketIO</code> instance.
   *
	 * @param sock
	 * @throws IOException
	 */
  public SocketIO(final SocketChannel sock) throws IOException {
    channel = sock;
		channel.configureBlocking(false);
		channel.socket().setSoLinger(false, 0);
		channel.socket().setReuseAddress(true);
  }

	@Override
  public SocketChannel getSocketChannel() {
    return channel;
  }

	@Override
  public void stop() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stop called.");
		}
    channel.close();
  }

	@Override
  public boolean isConnected() {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Is connected: " + channel.isConnected());
		}
    return channel.isConnected();
  }

	@Override
  public int write(final ByteBuffer buff) throws IOException {
//     int result = 0;
//     while (buff.hasRemaining()) {
//       final int res = channel.write(buff);
//       if (res == -1) {
//         throw new EOFException("Channel has been closed.");
//       } // end of if (res == -1)
//       result += res;
//     } // end of while (out.hasRemaining())
//     log.finer("Wrote to channel " + result + " bytes.");
//     return result;
		if (buff != null) {
			dataToSend.offer(buff);
		}
		ByteBuffer dataBuffer = dataToSend.peek();
    int result = channel.write(dataBuffer);
		if (result == -1) {
			throw new EOFException("Channel has been closed.");
		} // end of if (res == -1)
		if (!dataBuffer.hasRemaining()) {
			dataToSend.poll();
		}
		if (log.isLoggable(Level.FINER)) {
			log.finer("Wrote to channel " + result + " bytes.");
		}
    return result;
  }

	@Override
  public ByteBuffer read(final ByteBuffer buff) throws IOException {
    bytesRead = channel.read(buff);
		if (log.isLoggable(Level.FINER)) {
			log.finer("Read from channel " + bytesRead + " bytes.");
		}
    if (bytesRead == -1) {
      throw new EOFException("Channel has been closed.");
    } // end of if (result == -1)
    return buff;
  }

	@Override
  public int bytesRead() {
    return bytesRead;
  }

	@Override
	public int getInputPacketSize() throws IOException {
		return channel.socket().getReceiveBufferSize();
	}

	@Override
	public boolean waitingToSend() {
		return dataToSend.size() > 0;
	}


} // SocketIO
