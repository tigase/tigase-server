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
  /**
   * <code>tlsOutput</code> buffer keeps data encoded by tlsWrapper.
   *
   */
  private ByteBuffer tlsOutput = null;

//   /**
//    * Creates a new <code>TLSIO</code> instance.
//    *
//    */
//   public TLSIO(final SocketChannel sock) {
//     io = new SocketIO(sock);
//     tlsWrapper = new TLSWrapper("TLS");
//     tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
//     tlsOutput = ByteBuffer.allocate(tlsWrapper.getNetBuffSize());
//   }

  public TLSIO(final IOInterface ioi, final TLSWrapper wrapper)
		throws IOException {
		io = ioi;
    tlsWrapper = wrapper;
    tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
    tlsOutput = ByteBuffer.allocate(tlsWrapper.getNetBuffSize());
		log.finer("TLS Socket created.");
		if (tlsWrapper.isClientMode()) {
			log.finer("TLS - client mode, starting handshaking now...");
			write(ByteBuffer.allocate(0));
			log.finer("Handshaking completed, you can send data now.");
		} // end of if (tlsWrapper.isClientMode())
  }

  private ByteBuffer decodeData(final ByteBuffer input) throws IOException {
    TLSStatus stat = null;
    do_loop:
    do {
      input.flip();
			log.finer("Decoding data: " + input.remaining());
      tlsInput = tlsWrapper.unwrap(input, tlsInput);
      if (input.hasRemaining()) {
        input.compact();
      } // end of if (input.hasRemaining())
      switch (tlsWrapper.getStatus()) {
      case NEED_WRITE:
        write(ByteBuffer.allocate(0));
        break;
      case UNDERFLOW:
        break do_loop;
      case CLOSED:
        if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
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
		if (bytesRead() > 0) {
			log.finer("Read bytes: " + bytesRead());
      return decodeData(buff);
    } else {
      return null;
    } // end of else
  }

  public int write(final ByteBuffer buff) throws IOException {
    int result = 0;
    ByteBuffer tlsBuffer = null;
    log.finer("TLS - Writing data, remaining: " + buff.remaining());
    do {
      tlsBuffer = ByteBuffer.allocate(Math.min(buff.remaining(),
          tlsWrapper.getAppBuffSize()));
      // How to do it more efficiently???
      // Or how to get from source buffer only specific number of bytes??
      while (tlsBuffer.hasRemaining() && buff.hasRemaining()) {
        tlsBuffer.put(buff.get());
        ++result;
      } // end of while (tmpBuffer.hasRemaining() && dataBuffer.hasRemaining())
      tlsBuffer.flip();
      tlsWrapper.wrap(tlsBuffer, tlsOutput);
      if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
        throw new EOFException("Socket has been closed.");
      } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
      tlsOutput.flip();
      io.write(tlsOutput);
      tlsOutput.clear();
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
    io.stop();
    tlsWrapper.close();
  }

  public SocketChannel getSocketChannel() {
    return io.getSocketChannel();
  }

  public int bytesRead() {
    return io.bytesRead();
  }

} // TLSIO
