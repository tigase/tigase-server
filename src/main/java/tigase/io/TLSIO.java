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

//~--- non-JDK imports --------------------------------------------------------

import tigase.stats.StatisticsList;

//~--- JDK imports ------------------------------------------------------------

import java.io.EOFException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	private static Logger log = Logger.getLogger(TLSIO.class.getName());

	//~--- fields ---------------------------------------------------------------

	private IOInterface io = null;

	/**
	 * <code>tlsInput</code> buffer keeps data decoded from tlsWrapper.
	 */
	private ByteBuffer tlsInput = null;

	/**
	 * <code>tlsWrapper</code> is a TLS wrapper for connections requiring TLS
	 * protocol.
	 */
	private TLSWrapper tlsWrapper = null;

	//~--- constructors ---------------------------------------------------------

///**
// * Creates a new <code>TLSIO</code> instance.
// *
// */
//public TLSIO(final SocketChannel sock) {
//  io = new SocketIO(sock);
//  tlsWrapper = new TLSWrapper("TLS");
//  tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
//}

	/**
	 * Constructs ...
	 *
	 *
	 * @param ioi
	 * @param wrapper
	 *
	 * @throws IOException
	 */
	public TLSIO(final IOInterface ioi, final TLSWrapper wrapper) throws IOException {
		io = ioi;
		tlsWrapper = wrapper;
		tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());

		if (log.isLoggable(Level.FINER)) {
			log.finer("TLS Socket created, connected: " + io.isConnected());
		}

		if (tlsWrapper.isClientMode()) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("TLS - client mode, starting handshaking now...");
			}

			write(ByteBuffer.allocate(0));

			if (log.isLoggable(Level.FINER)) {
				log.finer("Handshaking completed, you can send data now.");
			}
		}    // end of if (tlsWrapper.isClientMode())
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int bytesRead() {
		return io.bytesRead();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	@Override
	public int getInputPacketSize() throws IOException {
		return tlsWrapper.getPacketBuffSize();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public SocketChannel getSocketChannel() {
		return io.getSocketChannel();
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		if (io != null) {
			io.getStatistics(list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean isConnected() {
		return io.isConnected();
	}

	/**
	 * Method description
	 *
	 *
	 * @param addr
	 *
	 * @return
	 */
	@Override
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param buff
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {
		ByteBuffer tmpBuffer = io.read(buff);

		if (io.bytesRead() > 0) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Read bytes: " + io.bytesRead());
			}

			return decodeData(tmpBuffer);
		} else {
			return null;
		}    // end of else
	}

	/**
	 * Method description
	 *
	 *
	 * @throws IOException
	 */
	@Override
	public void stop() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stop called...");

			// Thread.dumpStack();
		}

		io.stop();
		tlsWrapper.close();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "TLS: " + io.toString();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean waitingToSend() {
		return io.waitingToSend();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int waitingToSendSize() {
		return io.waitingToSendSize();
	}

	/**
	 * Method description
	 *
	 *
	 * @param buff
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	@Override
	public int write(ByteBuffer buff) throws IOException {
		TLSStatus stat = tlsWrapper.getStatus();

		// The loop below falls into infinite loop for some reason.
		// Let's try to detect it here and recover.
		// Looks like for some reason tlsWrapper.getStatus() sometimes starts to return
		// NEED_READ status all the time and the loop never ends.
		int loop_cnt = 0;
		int max_loop_runs = 1000;

		while (((stat == TLSStatus.NEED_WRITE) || (stat == TLSStatus.NEED_READ))
				&& (++loop_cnt < max_loop_runs)) {
			switch (stat) {
				case NEED_WRITE :
					writeBuff(ByteBuffer.allocate(0));

					break;

				case NEED_READ :

					// I wonder if some real data can be read from the socket here (and we would
					// loose the data) or this is just TLS stuff here.....
					ByteBuffer rbuff = read(ByteBuffer.allocate(tlsWrapper.getNetBuffSize()));

					break;

//      case CLOSED:
//        if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
//          if (log.isLoggable(Level.FINER)) {
//            log.finer("TLS Socket closed...");
//          }
//          throw new EOFException("Socket has been closed.");
//        } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
//        break;
				default :
			}

			stat = tlsWrapper.getStatus();
		}

		if (loop_cnt > (max_loop_runs / 2)) {
			log.log(Level.WARNING,
					"Infinite loop detected in write(buff) TLS code, tlsWrapper.getStatus(): {0}",
						tlsWrapper.getStatus());

			// Let's close the connection now
			throw new EOFException("Socket has been closed due to TLS problems.");
		}

		if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("TLS Socket closed...");
			}

			throw new EOFException("Socket has been closed.");
		}    // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)

		int result = -1;

		if (buff == null) {
			result = io.write(null);
		} else {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "TLS - Writing data, remaining: {0}", buff.remaining());
			}

			result = writeBuff(buff);
		}

//  if (isRemoteAddress("81.142.228.219")) {
//    log.warning("TLS - Writing data, remaining: " + buff.remaining());
//  }
//  if (isRemoteAddress("81.142.228.219")) {
//    log.warning("TLS - written: " + result);
//  }
		return result;
	}

	private ByteBuffer decodeData(ByteBuffer input) throws IOException {
		TLSStatus stat = null;

		// input.flip();
		// do_loop:
		do {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Decoding data: " + input.remaining());
				log.finer("input.capacity()=" + input.capacity());
				log.finer("input.remaining()=" + input.remaining());
				log.finer("input.limit()=" + input.limit());
				log.finer("input.position()=" + input.position());
			}

			tlsInput = tlsWrapper.unwrap(input, tlsInput);

			if (log.isLoggable(Level.FINEST)) {
				int netSize = tlsWrapper.getPacketBuffSize();

				log.finer("tlsWrapper.getStatus() = " + tlsWrapper.getStatus().name());
				log.finer("PacketBuffSize=" + netSize);
				log.finer("input.capacity()=" + input.capacity());
				log.finer("input.remaining()=" + input.remaining());
				log.finer("input.limit()=" + input.limit());
				log.finer("input.position()=" + input.position());
				log.finer("tlsInput.capacity()=" + tlsInput.capacity());
				log.finer("tlsInput.remaining()=" + tlsInput.remaining());
				log.finer("tlsInput.limit()=" + tlsInput.limit());
				log.finer("tlsInput.position()=" + tlsInput.position());
			}

//    if (input.hasRemaining()) {
//      input.compact();
//    } // end of if (input.hasRemaining())
			switch (tlsWrapper.getStatus()) {
				case NEED_WRITE :
					writeBuff(ByteBuffer.allocate(0));

					break;

				case UNDERFLOW :

//        if (log.isLoggable(Level.FINER) && !log.isLoggable(Level.FINEST)) {
//          int netSize = tlsWrapper.getPacketBuffSize();
//          log.finer("tlsWrapper.getStatus() = UNDERFLOW");
//          log.finer("PacketBuffSize=" + netSize);
//          log.finer("input.capacity()=" + input.capacity());
//          log.finer("input.remaining()=" + input.remaining());
//          log.finer("input.limit()=" + input.limit());
//          log.finer("input.position()=" + input.position());
//          log.finer("tlsInput.capacity()=" + tlsInput.capacity());
//          log.finer("tlsInput.remaining()=" + tlsInput.remaining());
//          log.finer("tlsInput.limit()=" + tlsInput.limit());
//          log.finer("tlsInput.position()=" + tlsInput.position());
//        }
					// Obtain more inbound network data for src,
					// then retry the operation.
					throw new BufferUnderflowException();

				case CLOSED :

//        if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
					if (log.isLoggable(Level.FINER)) {
						log.finer("TLS Socket closed...");
					}

					throw new EOFException("Socket has been closed.");

//      } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
//      break do_loop;
//      break;
				default :
					break;
			}    // end of switch (tlsWrapper.getStatus())

			stat = tlsWrapper.getStatus();
		} while (((stat == TLSStatus.NEED_READ) || (stat == TLSStatus.OK)) && input.hasRemaining());

		if (input.hasRemaining()) {
			input.rewind();
		} else {
			input.clear();
		}

		tlsInput.flip();

		return tlsInput;
	}

	private int writeBuff(ByteBuffer buff) throws IOException {
		int result = 0;
		int wr = 0;

		// The loop below falls into infinite loop for some reason.
		// Let's try to detect it here and recover.
		// -- After some tests....
		// Looks like the cause has been detected. Sometimes the loop
		// below is executed a few times for some reason. It happens that
		// the tlsWarpper.getStatus() returns NEED_READ and it doesn't
		// accept any more data from the input buffer.
		// The proper handling would need reading from the socket to
		// reset TLS to the correct state, but this involves another problems.
		// What to do with possible user data received in such a call?
		// It happens extremely rarely and is hard to diagnose. Let's leave it
		// as it is now which just causes such connections to be closed.
		int loop_cnt = 0;
		int max_loop_runs = 1000;

		do {
			if (tlsWrapper.getStatus() == TLSStatus.NEED_READ) {

				// I wonder if some real data can be read from the socket here (and we would
				// loose the data) or this is just TLS stuff here.....
				ByteBuffer rbuff = read(ByteBuffer.allocate(tlsWrapper.getNetBuffSize()));
			}

			ByteBuffer tlsOutput = ByteBuffer.allocate(tlsWrapper.getNetBuffSize());

			// Not sure if this is really needed, I guess not...
			tlsOutput.clear();
			tlsWrapper.wrap(buff, tlsOutput);

			if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
				throw new EOFException("Socket has been closed.");
			}    // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)

			tlsOutput.flip();
			wr = io.write(tlsOutput);
			result += wr;
		} while (buff.hasRemaining() && (++loop_cnt < max_loop_runs));

		if (loop_cnt > (max_loop_runs / 2)) {
			log.warning("Infinite loop detected in writeBuff(buff) TLS code, "
					+ "tlsWrapper.getStatus(): " + tlsWrapper.getStatus());

			// Let's close the connection now
			throw new EOFException("Socket has been closed due to TLS problems.");
		}

		if (tlsWrapper.getStatus() == TLSStatus.NEED_WRITE) {
			writeBuff(ByteBuffer.allocate(0));
		}    // end of if ()

		return result;
	}
}    // TLSIO


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
