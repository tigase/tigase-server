/*
 * SocketIO.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Queue;
import tigase.net.IOUtil;

/**
 * Describe class SocketIO here.
 *
 *
 * Created: Sat May 14 07:18:30 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SocketIO
				implements IOInterface {
	private static final Logger log                          =
		Logger.getLogger(SocketIO.class.getName());
	private static final int MAX_USER_IO_QUEUE_SIZE_PROP_DEF = 1000;

	// ~--- fields ---------------------------------------------------------------
	private static final String MAX_USER_IO_QUEUE_SIZE_PROP_KEY = "max-user-io-queue-size";

	//~--- fields ---------------------------------------------------------------

	private long buffOverflow            = 0;
	private int bytesRead                = 0;
	private long bytesReceived           = 0;
	private long bytesSent               = 0;
	private SocketChannel channel        = null;
	private Queue<ByteBuffer> dataToSend = null;
	private String logId                 = null;
	private String remoteAddress         = null;
	private long totalBuffOverflow       = 0;
	private long totalBytesReceived      = 0;
	private long totalBytesSent          = 0;

	//~--- constructors ---------------------------------------------------------

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
		channel.socket().setKeepAlive(true);
		remoteAddress = channel.socket().getInetAddress().getHostAddress();
		if (channel.socket().getTrafficClass() == ConnectionOpenListener.IPTOS_THROUGHPUT) {
			dataToSend = new LinkedBlockingQueue<ByteBuffer>(100000);
		} else {
			int queue_size = Integer.getInteger(MAX_USER_IO_QUEUE_SIZE_PROP_KEY,
												 MAX_USER_IO_QUEUE_SIZE_PROP_DEF);

			dataToSend = new LinkedBlockingQueue<ByteBuffer>(queue_size);
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public int bytesRead() {
		return bytesRead;
	}

	@Override
	public boolean checkCapabilities(String caps) {
		return false;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public int getInputPacketSize() throws IOException {
		return channel.socket().getReceiveBufferSize();
	}

	@Override
	public SocketChannel getSocketChannel() {
		return channel;
	}

	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		list.add("socketio", "Bytes sent", bytesSent, Level.FINE);
		list.add("socketio", "Bytes received", bytesReceived, Level.FINE);
		list.add("socketio", "Buffers overflow", buffOverflow, Level.FINE);
		list.add("socketio", "Total bytes sent", totalBytesSent, Level.FINE);
		list.add("socketio", "Total bytes received", totalBytesReceived, Level.FINE);
		list.add("socketio", "Ttoal buffers overflow", totalBuffOverflow, Level.FINE);
		if (reset) {
			bytesSent     = 0;
			bytesReceived = 0;
			buffOverflow  = 0;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * 
	 */
	public long getBytesSent(boolean reset) {
		long tmp = bytesSent;

		if (reset) {
			bytesSent = 0;
		}

		return tmp;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getTotalBytesSent() {
		return totalBytesSent;
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * 
	 */
	public long getBytesReceived(boolean reset) {
		long tmp = bytesReceived;

		if (reset) {
			bytesReceived = 0;
		}

		return tmp;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getTotalBytesReceived() {
		return totalBytesReceived;
	}

	@Override
	public long getBuffOverflow(boolean reset) {
		long tmp = buffOverflow;

		if (reset) {
			buffOverflow = 0;
		}

		return tmp;
	}

	@Override
	public long getTotalBuffOverflow() {
		return totalBuffOverflow;
	}

	@Override
	public boolean isConnected() {

		// More correct would be calling both methods, however in the Tigase
		// all SocketChannels are connected before SocketIO is created.
		// return channel.isOpen() && channel.isConnected();
		return channel.isOpen();
	}

	@Override
	public boolean isRemoteAddress(String addr) {
		return remoteAddress.equals(addr);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public ByteBuffer read(final ByteBuffer buff) throws IOException {
		ByteBuffer tmp = IOUtil.getDirectBuffer(buff.remaining());
		try {
			bytesRead = channel.read(tmp);
			tmp.flip();
			if (bytesRead > 0) {
				buff.put(tmp);
			}
		} finally {
			IOUtil.returnDirectBuffer(tmp);
		}
		
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Read from channel {0} bytes, {1}", new Object[] { bytesRead,
							toString() });
		}
		if (bytesRead == -1) {
			// we need to close channel but we should not throw exception as
			// in other way we will lose data from higher level internal buffers
			channel.close();
//			throw new EOFException("Channel has been closed.");
		}    // end of if (result == -1)
		if (bytesRead > 0) {
			buff.flip();
			bytesReceived      += bytesRead;
			totalBytesReceived += bytesRead;
		}

		return buff;
	}

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

	@Override
	public String toString() {
		return logId + ((channel == null)
										? null
										: channel.socket());
	}

	@Override
	public boolean waitingToSend() {
		return isConnected() && (dataToSend.size() > 0);
	}

	@Override
	public int waitingToSendSize() {
		return dataToSend.size();
	}

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
		if ((buff != null) && buff.hasRemaining()) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "SOCKET - Writing data, remaining: {0}, {1}",
								new Object[] { buff.remaining(),
															 toString() });
			}
			if (!dataToSend.offer(buff)) {
				++buffOverflow;
				++totalBuffOverflow;
			}
		}

		int result            = 0;
		ByteBuffer dataBuffer = null;
		
		// we are processing all buffers one by one to reduce need for direct 
		// memory, and we use our own cache of DirectByteBuffers as cache from JDK
		// may keep up to 1024 buffers for single thread!!
		while ((dataBuffer = dataToSend.peek()) != null) {
			int pos = dataBuffer.position();
			int lim = dataBuffer.limit();
			int rem = (pos <= lim ? lim - pos : 0);
			int res = 0;
			
			ByteBuffer tmp = IOUtil.getDirectBuffer(rem);
			try {
				tmp.put(dataBuffer);
				tmp.flip();
				dataBuffer.position(pos);
				res = channel.write(tmp);
				if (res > 0) {
					dataBuffer.position(pos + res);
				}
			} finally {
				IOUtil.returnDirectBuffer(tmp);
			}
			
			if (res == -1) {
				throw new EOFException("Channel has been closed.");
			} else {
				result += res;
			}
			
			if (!dataBuffer.hasRemaining()) {
				dataToSend.poll();
			}
			else {
				break;
			}
		}
		
		
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Wrote to channel {0} bytes, {1}", new Object[] { result,
							toString() });
		}

		// if (isRemoteAddress("81.142.228.219")) {
		// log.warning("Wrote to channel " + result + " bytes.");
		// }
		bytesSent      += result;
		totalBytesSent += result;

		return result;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setLogId(String logId) {
		this.logId = logId + " ";
	}		
}    // SocketIO
