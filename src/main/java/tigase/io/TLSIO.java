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

//~--- non-JDK imports --------------------------------------------------------

import tigase.stats.StatisticsList;

import javax.net.ssl.SSLEngineResult;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

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

	/** Field description */
	public static final String TLS_CAPS = "tls-caps";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(TLSIO.class.getName());

	// ~--- fields ---------------------------------------------------------------

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

	// ~--- constructors ---------------------------------------------------------

	// /**
	// * Creates a new <code>TLSIO</code> instance.
	// *
	// */
	// public TLSIO(final SocketChannel sock) {
	// io = new SocketIO(sock);
	// tlsWrapper = new TLSWrapper("TLS");
	// tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
	// }

	/**
	 * Constructs ...
	 *
	 *
	 * @param ioi
	 * @param wrapper
	 *
	 * @throws IOException
	 */
	public TLSIO(final IOInterface ioi, final TLSWrapper wrapper, final ByteOrder order) throws IOException {
		io = ioi;
		tlsWrapper = wrapper;
		tlsWrapper.setDebugId(toString());
		tlsInput = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
                tlsInput.order(order);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "TLS Socket created: {0}", io.toString());
		}

		if (tlsWrapper.isClientMode()) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("TLS - client mode, starting handshaking now...");
			}

			write(ByteBuffer.allocate(0));
		} // end of if (tlsWrapper.isClientMode())
	}

	// ~--- methods --------------------------------------------------------------

	@Override
	public int bytesRead() {
		return io.bytesRead();
	}

	@Override
	public long getBytesSent(boolean reset) {
		return io.getBytesSent(reset);
	}

	@Override
	public long getTotalBytesSent() {
		return io.getTotalBytesSent();
	}

	@Override
	public long getBytesReceived(boolean reset) {
		return io.getBytesReceived(reset);
	}

	@Override
	public long getTotalBytesReceived() {
		return io.getTotalBytesReceived();
	}

	@Override
	public long getBuffOverflow(boolean reset) {
		return io.getBuffOverflow(reset);
	}

	@Override
	public long getTotalBuffOverflow() {
		return io.getTotalBuffOverflow();
	}

	@Override
	public boolean checkCapabilities(String caps) {
		return caps.contains(TLS_CAPS) || io.checkCapabilities(caps);
	}

	// ~--- get methods ----------------------------------------------------------

	@Override
	public int getInputPacketSize() throws IOException {
		return tlsWrapper.getPacketBuffSize();
	}

	@Override
	public SocketChannel getSocketChannel() {
		return io.getSocketChannel();
	}

	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		if (io != null) {
			io.getStatistics(list, reset);
		}
	}

	@Override
	public boolean isConnected() {
		return io.isConnected();
	}

	@Override
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

	// ~--- methods --------------------------------------------------------------

	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {

		// if (log.isLoggable(Level.FINER)) {
		// log.finer("input.capacity()=" + buff.capacity());
		// log.finer("input.remaining()=" + buff.remaining());
		// log.finer("input.limit()=" + buff.limit());
		// log.finer("input.position()=" + buff.position());
		// }
		ByteBuffer tmpBuffer = io.read(buff);

		if (io.bytesRead() > 0) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Read bytes: {0}, {1}", new Object[] { io.bytesRead(),
						toString() });
			}

			return decodeData(tmpBuffer);
		} else {
			if (tlsInput.capacity() > tlsWrapper.getAppBuffSize() && tlsInput.capacity() == tlsInput.remaining()) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Resizing tlsInput to {0} bytes, {1}", new Object[] { tlsWrapper.getAppBuffSize(), toString() });
				}
				ByteBuffer bb = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());

				bb.order(tlsInput.order());

				tlsInput = bb;
			}
			return null;
		} // end of else
	}

	@Override
	public void stop() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stop called..." + toString());

			// Thread.dumpStack();
		}

		io.stop();
		tlsWrapper.close();
	}

	@Override
	public String toString() {
		return "TLS: " + io.toString();
	}

	@Override
	public boolean waitingToSend() {
		return io.waitingToSend();
	}

	@Override
	public int waitingToSendSize() {
		return io.waitingToSendSize();
	}

	@Override
	public int write(ByteBuffer buff) throws IOException {
		TLSStatus stat = tlsWrapper.getStatus();

		// The loop below falls into infinite loop for some reason.
		// Let's try to detect it here and recover.
		// Looks like for some reason tlsWrapper.getStatus() sometimes starts to
		// return
		// NEED_READ status all the time and the loop never ends.
		int loop_cnt = 0;
		int max_loop_runs = 100000;

		boolean breakNow = true;
		
		while (((stat == TLSStatus.NEED_WRITE) || (stat == TLSStatus.NEED_READ))
				&& (++loop_cnt < max_loop_runs)) {
			switch (stat) {
				case NEED_WRITE:
					writeBuff(ByteBuffer.allocate(0));

					break;

				case NEED_READ:
					// We can get NEED_READ TLS status while there are data awaiting to be
					// sent thru network connection - we need to force sending data and to break
					// from this loop
					if (io.waitingToSend()) {
						io.write(null);
						
						// it appears only during handshake so force break only in this case
						if (tlsWrapper.getTlsEngine().getHandshakeStatus() == 
								SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING 
								&& (buff == null || !buff.hasRemaining())) {
							breakNow = true;
						}
					}
					
					// I wonder if some real data can be read from the socket here (and we
					// would
					// loose the data) or this is just TLS stuff here.....
					ByteBuffer rbuff = read(ByteBuffer.allocate(tlsWrapper.getNetBuffSize()));

					break;

				// case CLOSED:
				// if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
				// if (log.isLoggable(Level.FINER)) {
				// log.finer("TLS Socket closed...");
				// }
				// throw new EOFException("Socket has been closed.");
				// } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
				// break;
				default:
			}

			stat = tlsWrapper.getStatus();
		
			// We can get NEED_READ TLS status while there are data awaiting to be
			// sent thru network connection - we need to force sending data and to break
			// from this loop
			if (breakNow) {
				break;
			}
		}

		if (loop_cnt > (max_loop_runs / 2)) {
			log.log(Level.WARNING,
					"Infinite loop detected in write(buff) TLS code, tlsWrapper.getStatus(): {0}, io: {1}",
					new Object[] {tlsWrapper.getStatus() , toString() }
			);

			// Let's close the connection now
			throw new EOFException("Socket has been closed due to TLS problems.");
		}

		if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("TLS Socket closed...");
			}

			throw new EOFException("Socket has been closed.");
		} // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)

		int result = -1;

		if (buff == null) {
			result = io.write(null);
		} else {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "TLS - Writing data, remaining: {0}, {1}", new Object[] {
						buff.remaining(), toString() });
			}

			result = writeBuff(buff);
		}

		// if (isRemoteAddress("81.142.228.219")) {
		// log.warning("TLS - Writing data, remaining: " + buff.remaining());
		// }
		// if (isRemoteAddress("81.142.228.219")) {
		// log.warning("TLS - written: " + result);
		// }
		return result;
	}

	private ByteBuffer decodeData(ByteBuffer input) throws IOException {
		TLSStatus stat = null;
		boolean continueLoop = true;

		// input.flip();
		// do_loop:
		do {

			// if (log.isLoggable(Level.FINER)) {
			// log.finer("Decoding data: " + input.remaining());
			// log.finer("input.capacity()=" + input.capacity());
			// log.finer("input.remaining()=" + input.remaining());
			// log.finer("input.limit()=" + input.limit());
			// log.finer("input.position()=" + input.position());
			// log.finer("tlsInput.capacity()=" + tlsInput.capacity());
			// log.finer("tlsInput.remaining()=" + tlsInput.remaining());
			// log.finer("tlsInput.limit()=" + tlsInput.limit());
			// log.finer("tlsInput.position()=" + tlsInput.position());
			// }
			tlsInput = tlsWrapper.unwrap(input, tlsInput);

			// if (log.isLoggable(Level.FINEST)) {
			// int netSize = tlsWrapper.getPacketBuffSize();
			//
			// log.finer("tlsWrapper.getStatus() = " + tlsWrapper.getStatus().name());
			// log.finer("PacketBuffSize=" + netSize);
			// log.finer("input.capacity()=" + input.capacity());
			// log.finer("input.remaining()=" + input.remaining());
			// log.finer("input.limit()=" + input.limit());
			// log.finer("input.position()=" + input.position());
			// log.finer("tlsInput.capacity()=" + tlsInput.capacity());
			// log.finer("tlsInput.remaining()=" + tlsInput.remaining());
			// log.finer("tlsInput.limit()=" + tlsInput.limit());
			// log.finer("tlsInput.position()=" + tlsInput.position());
			// }
			// if (input.hasRemaining()) {
			// input.compact();
			// }// end of if (input.hasRemaining())
			switch (tlsWrapper.getStatus()) {
				case NEED_WRITE:
					writeBuff(ByteBuffer.allocate(0));

					break;

				case UNDERFLOW:

					// if (log.isLoggable(Level.FINER) && !log.isLoggable(Level.FINEST)) {
					// int netSize = tlsWrapper.getPacketBuffSize();
					// log.finer("tlsWrapper.getStatus() = UNDERFLOW");
					// log.finer("PacketBuffSize=" + netSize);
					// log.finer("input.capacity()=" + input.capacity());
					// log.finer("input.remaining()=" + input.remaining());
					// log.finer("input.limit()=" + input.limit());
					// log.finer("input.position()=" + input.position());
					// log.finer("tlsInput.capacity()=" + tlsInput.capacity());
					// log.finer("tlsInput.remaining()=" + tlsInput.remaining());
					// log.finer("tlsInput.limit()=" + tlsInput.limit());
					// log.finer("tlsInput.position()=" + tlsInput.position());
					// }
					// Obtain more inbound network data for src,
					// then retry the operation.
					// If there is some data ready to read, let's try to read it before we
					// increase
					// the buffer size
					// throw new BufferUnderflowException();
					if (tlsInput.capacity() == tlsInput.remaining()) {
						throw new BufferUnderflowException();
					} else {
						input.compact();
						continueLoop = false;
					}

					break;

				case CLOSED:

					// if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
					if (log.isLoggable(Level.FINER)) {
						log.finer("TLS Socket closed..." + toString());
					}

					//throw new EOFException("Socket has been closed.");

					// } // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)
					// break do_loop;
					// break;
				default:
					break;
			} // end of switch (tlsWrapper.getStatus())

			stat = tlsWrapper.getStatus();
		} while (continueLoop && ((stat == TLSStatus.NEED_READ) || (stat == TLSStatus.OK))
				&& input.hasRemaining());

		if (continueLoop) {
			if (input.hasRemaining()) {
				input.rewind();
			} else {
				input.clear();
			}
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
		int max_loop_runs = 100000;

		do {
			if (tlsWrapper.getStatus() == TLSStatus.NEED_READ) {

				// I wonder if some real data can be read from the socket here (and we
				// would
				// loose the data) or this is just TLS stuff here.....
				ByteBuffer rbuff = read(ByteBuffer.allocate(tlsWrapper.getNetBuffSize()));
			}

			ByteBuffer tlsOutput = ByteBuffer.allocate(tlsWrapper.getNetBuffSize());

			// Not sure if this is really needed, I guess not...
			tlsOutput.clear();
			tlsWrapper.wrap(buff, tlsOutput);

			if (tlsWrapper.getStatus() == TLSStatus.CLOSED) {
				throw new EOFException("Socket has been closed.");
			} // end of if (tlsWrapper.getStatus() == TLSStatus.CLOSED)

			tlsOutput.flip();
			wr = io.write(tlsOutput);
			result += wr;

			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINER, "TLS - Writing data, remaining: {0}, {1}",
															new Object[] {buff.remaining(), toString() } );
			}

		} while (buff.hasRemaining() && (++loop_cnt < max_loop_runs));

		if (loop_cnt > (max_loop_runs / 2)) {
			log.warning("Infinite loop detected in writeBuff(buff) TLS code, "
					+ "tlsWrapper.getStatus(): " + tlsWrapper.getStatus()
									+ ", buff.remaining(): " + buff.remaining() + " io: " + toString() );

			// Let's close the connection now
			throw new EOFException("Socket has been closed due to TLS problems.");
		}

		if (tlsWrapper.getStatus() == TLSStatus.NEED_WRITE) {
			writeBuff(ByteBuffer.allocate(0));
		} // end of if ()

		return result;
	}
	
	@Override
	public void setLogId(String logId) {
		io.setLogId(logId);
	}
} // TLSIO
