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

import tigase.net.ConnectionOpenListener;

import tigase.stats.StatisticsList;

//~--- JDK imports ------------------------------------------------------------

import java.io.EOFException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	private static final Logger log = Logger.getLogger(SocketIO.class.getName());

	// ~--- fields ---------------------------------------------------------------

	private static final String MAX_USER_IO_QUEUE_SIZE_PROP_KEY = "max-user-io-queue-size";
	private static final int MAX_USER_IO_QUEUE_SIZE_PROP_DEF = 1000;
	private int bytesRead = 0;
	private SocketChannel channel = null;
	private Queue<ByteBuffer> dataToSend = null;
	private String remoteAddress = null;
	private String logId = null;

	private long totalBuffOverflow = 0;
	private long totalBytesSent = 0;
	private long totalBytesReceived = 0;
	private long buffOverflow = 0;
	private long bytesSent = 0;
	private long bytesReceived = 0;


	// ~--- constructors ---------------------------------------------------------

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
		remoteAddress = channel.socket().getInetAddress().getHostAddress();

		if (channel.socket().getTrafficClass() == ConnectionOpenListener.IPTOS_THROUGHPUT) {
			dataToSend = new LinkedBlockingQueue<ByteBuffer>(100000);
		} else {
			int queue_size =
					Integer.getInteger(MAX_USER_IO_QUEUE_SIZE_PROP_KEY,
							MAX_USER_IO_QUEUE_SIZE_PROP_DEF);
			dataToSend = new LinkedBlockingQueue<ByteBuffer>(queue_size);
		}
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public int bytesRead() {
		return bytesRead;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param caps
	 * 
	 * @return
	 */
	@Override
	public boolean checkCapabilities(String caps) {
		return false;
	}

	// ~--- get methods ----------------------------------------------------------

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
		return channel.socket().getReceiveBufferSize();
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public SocketChannel getSocketChannel() {
		return channel;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param list
	 * @param reset
	 */
	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		list.add("socketio", "Bytes sent", bytesSent, Level.FINE);
		list.add("socketio", "Bytes received", bytesReceived, Level.FINE);
		list.add("socketio", "Buffers overflow", buffOverflow, Level.FINE);
		list.add("socketio", "Total bytes sent", totalBytesSent, Level.FINE);
		list.add("socketio", "Total bytes received", totalBytesReceived, Level.FINE);
		list.add("socketio", "Ttoal buffers overflow", totalBuffOverflow, Level.FINE);

		if (reset) {
			bytesSent = 0;
			bytesReceived = 0;
			buffOverflow = 0;
		}
	}

	public long getBytesSent(boolean reset) {
		long tmp = bytesSent;
		if (reset) {
			bytesSent = 0;
		}
		return tmp;
	}

	public long getTotalBytesSent() {
		return totalBytesSent;
	}

	public long getBytesReceived(boolean reset) {
		long tmp = bytesReceived;
		if (reset) {
			bytesReceived = 0;
		}
		return tmp;
	}

	public long getTotalBytesReceived() {
		return totalBytesReceived;
	}

	public long getBuffOverflow(boolean reset) {
		long tmp = buffOverflow;
		if (reset) {
			buffOverflow = 0;
		}
		return tmp;
	}

	public long getTotalBuffOverflow() {
		return totalBuffOverflow;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public boolean isConnected() {

		// More correct would be calling both methods, however in the Tigase
		// all SocketChannels are connected before SocketIO is created.
		// return channel.isOpen() && channel.isConnected();
		return channel.isOpen();
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
		return remoteAddress.equals(addr);
	}

	// ~--- methods --------------------------------------------------------------

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
	public ByteBuffer read(final ByteBuffer buff) throws IOException {
		bytesRead = channel.read(buff);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Read from channel {0} bytes, {1}", new Object[] { bytesRead,
					toString() });
		}

		if (bytesRead == -1) {
			throw new EOFException("Channel has been closed.");
		} // end of if (result == -1)

		if (bytesRead > 0) {
			buff.flip();
			bytesReceived += bytesRead;
			totalBytesReceived += bytesRead;
		}

		return buff;
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
			log.finest("Stop called " + toString());
		}

		// if (isRemoteAddress("81.142.228.219")) {
		// log.warning("Stop called.");
		// }
		channel.close();
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		return logId + ((channel == null) ? null : channel.socket());
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public boolean waitingToSend() {
		return isConnected() && (dataToSend.size() > 0);
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public int waitingToSendSize() {
		return dataToSend.size();
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
	public int write(final ByteBuffer buff) throws IOException {

		// int result = 0;
		// while (buff.hasRemaining()) {
		// final int res = channel.write(buff);
		// if (res == -1) {
		// throw new EOFException("Channel has been closed.");
		// } // end of if (res == -1)
		// result += res;
		// } // end of while (out.hasRemaining())
		// log.finer("Wrote to channel " + result + " bytes.");
		// return result;
		if (buff != null && buff.hasRemaining()) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "SOCKET - Writing data, remaining: {0}, {1}", new Object[] {
						buff.remaining(), toString() });
			}

			if (!dataToSend.offer(buff)) {
				++buffOverflow;
				++totalBuffOverflow;
			}
		}

		int result = 0;
		ByteBuffer dataBuffer = null;

		if (dataToSend.size() > 1) {
			ByteBuffer[] buffs = dataToSend.toArray(new ByteBuffer[dataToSend.size()]);
			long res = channel.write(buffs);

			if (res == -1) {
				throw new EOFException("Channel has been closed.");
			}

			if (res > 0) {
				result += res;

				for (ByteBuffer byteBuffer : buffs) {
					if (!byteBuffer.hasRemaining()) {
						dataToSend.poll();
					} else {
						break;
					}
				}
			}
		} else {
			if ((dataBuffer = dataToSend.peek()) != null) {
				int res = channel.write(dataBuffer);

				if (res == -1) {
					throw new EOFException("Channel has been closed.");
				} else {
					result += res;
				}

				if (!dataBuffer.hasRemaining()) {
					dataToSend.poll();
				}
			}
		}

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Wrote to channel {0} bytes, {1}", new Object[] { result,
					toString() });
		}

		// if (isRemoteAddress("81.142.228.219")) {
		// log.warning("Wrote to channel " + result + " bytes.");
		// }
		bytesSent += result;
		totalBytesSent += result;

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.io.IOInterface#setLogId(java.lang.String)
	 */
	@Override
	public void setLogId(String logId) {
		this.logId = logId + " ";
	}
} // SocketIO

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
