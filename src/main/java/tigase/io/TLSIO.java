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

/**
 * Describe class TLSIO here.
 *
 *
 * Created: Sat May 14 07:43:30 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TLSIO implements IOInterface {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static Logger log = Logger.getLogger("tigase.io.TLSIO");

  private IOInterface io = null;
  /**
   * <code>tlsWrapper</code> is a TLS wrapper for connections requiring TLS
   * protocol.
   */
  private TLSWrapper tlsWrapper = null;
  /**
   * <code>tlsInput</code> buffer keeps data decoded from tlsWrapper.
   */
  private ByteBuffer tlsInput = null;

//   /**
//    * Creates a new <code>TLSIO</code> instance.
//    *
//    */
//   public TLSIO(final SocketChannel sock) {
//     io = new SocketIO(sock);
//     tlsWrapper = new TLSWrapper("TLS");
//     tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
//   }

  public TLSIO(final IOInterface ioi, final TLSWrapper wrapper)
		throws IOException {
		io = ioi;
    tlsWrapper = wrapper;
    tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
		log.finer("TLS Socket created, connected: " + io.isConnected());
		if (tlsWrapper.isClientMode()) {
			log.finer("TLS - client mode, starting handshaking now...");
			write(ByteBuffer.allocate(0));
			log.finer("Handshaking completed, you can send data now.");
		} // end of if (tlsWrapper.isClientMode())
  }

  private ByteBuffer decodeData(ByteBuffer input) throws IOException {
    TLSStatus stat = null;
		input.flip();
    do_loop:
    do {
			log.finer("Decoding data: " + input.remaining());
      tlsInput = tlsWrapper.unwrap(input, tlsInput);
//       if (input.hasRemaining()) {
//         input.compact();
//       } // end of if (input.hasRemaining())
      switch (tlsWrapper.getStatus()) {
      case NEED_WRITE:
        write(ByteBuffer.allocate(0));
        break;
      case UNDERFLOW:
				log.finer("tlsWrapper.getStatus() = UNDERFLOW");
 				int netSize = tlsWrapper.getPacketBuffSize();
				log.finer("PacketBuffSize="+netSize);
				log.finer("input.capacity()="+input.capacity());
				log.finer("tlsInput.capacity()="+tlsInput.capacity());
				log.finer("input.remaining()="+input.remaining());
				log.finer("tlsInput.remaining()="+tlsInput.remaining());
				// Obtain more inbound network data for src,
				// then retry the operation.
				throw new BufferUnderflowException();
      case CLOSED:
        if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
					log.finer("TLS Socket closed...");
          throw new EOFException("Socket has been closed.");
        } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
        break do_loop;
      default:
        break;
      } // end of switch (tlsWrapper.getStatus())
      stat = tlsWrapper.getStatus();
    } while ((stat == TLSStatus.NEED_READ || stat == TLSStatus.OK)
      && input.hasRemaining());
    if (input.hasRemaining()) {
      input.rewind();
    } else {
      input.clear();
    }
    return tlsInput;
  }

  public ByteBuffer read(ByteBuffer buff) throws IOException {
    buff = io.read(buff);
		if (io.bytesRead() > 0) {
			log.finer("Read bytes: " + bytesRead());
      return decodeData(buff);
    } else {
      return null;
    } // end of else
  }

  public int write(final ByteBuffer buff) throws IOException {
		if (buff == null) {
			return io.write(null);
		}
    int result = 0;
    log.finer("TLS - Writing data, remaining: " + buff.remaining());
    do {
			ByteBuffer tlsOutput = ByteBuffer.allocate(tlsWrapper.getNetBuffSize());
      tlsOutput.clear();
      tlsWrapper.wrap(buff, tlsOutput);
      if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
        throw new EOFException("Socket has been closed.");
      } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
      tlsOutput.flip();
			result += io.write(tlsOutput);
    } while (buff.hasRemaining());
    if (tlsWrapper.getStatus() == TLSStatus.NEED_WRITE) {
      write(ByteBuffer.allocate(0));
    } // end of if ()
    return result;
  }

  public boolean isConnected() {
    return io.isConnected();
  }

  public void stop() throws IOException {
		log.finest("Stop called...");
    io.stop();
    tlsWrapper.close();
  }

  public SocketChannel getSocketChannel() {
    return io.getSocketChannel();
  }

  public int bytesRead() {
    return io.bytesRead();
  }

	public int getInputPacketSize() throws IOException {
		return tlsWrapper.getPacketBuffSize();
	}

	public boolean waitingToSend() {
		return io.waitingToSend();
	}

} // TLSIO
