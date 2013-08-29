/*
 * ZLibIO.java
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

import tigase.stats.StatisticsList;

import tigase.util.ZLibWrapper;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Jul 29, 2009 11:58:02 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ZLibIO
				implements IOInterface {
	/** Field description */
	public static final String ZLIB_CAPS = "zlib-caps";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static Logger log = Logger.getLogger(ZLibIO.class.getName());

	//~--- fields ---------------------------------------------------------------

	private IOInterface io   = null;
	private ZLibWrapper zlib = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param ioi
	 * @param level
	 */
	public ZLibIO(final IOInterface ioi, final int level) {
		this.io = ioi;
		zlib    = new ZLibWrapper();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	@Override
	public int bytesRead() {
		return io.bytesRead();
	}

	/**
	 * Method description
	 *
	 *
	 * @param caps
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean checkCapabilities(String caps) {
		return caps.contains(ZLIB_CAPS) || io.checkCapabilities(caps);
	}

	/**
	 * Method description
	 *
	 *
	 * @param buff
	 *
	 *
	 *
	 *
	 * @return a value of <code>ByteBuffer</code>
	 * @throws IOException
	 */
	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {
		ByteBuffer tmpBuffer = io.read(buff);

		if (io.bytesRead() > 0) {
			ByteBuffer decompressed_buff = zlib.decompress(tmpBuffer);

			// The buffer is reused to it needs to be cleared before it can be
			// used again.
			tmpBuffer.clear();

			// System.out.println("Decompression rate: " + zlib.lastDecompressionRate());
			return decompressed_buff;
		}

		return null;
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
			log.finest("Stop called..." + toString());
		}
		io.stop();
		zlib.end();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String toString() {
		return "ZLIB: " + io.toString();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean waitingToSend() {
		return io.waitingToSend();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
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
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 * @throws IOException
	 */
	@Override
	public int write(ByteBuffer buff) throws IOException {
		if (buff == null) {
			return io.write(null);
		}
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "ZLIB - Writing data, remaining: {0}", buff.remaining());
		}

		ByteBuffer compressed_buff = zlib.compress(buff);

		// System.out.println("Compression rate: " + zlib.lastCompressionRate());
		return io.write(compressed_buff);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param reset is a <code>boolean</code>
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getBuffOverflow(boolean reset) {
		return io.getBuffOverflow(reset);
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset is a <code>boolean</code>
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getBytesReceived(boolean reset) {
		return io.getBytesReceived(reset);
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset is a <code>boolean</code>
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getBytesSent(boolean reset) {
		return io.getBytesSent(reset);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 * @throws IOException
	 */
	@Override
	public int getInputPacketSize() throws IOException {
		return io.getInputPacketSize();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>SocketChannel</code>
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
	 * @param reset
	 */
	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		if (io != null) {
			io.getStatistics(list, reset);
		}
		if (zlib != null) {
			list.add("zlibio", "Average compression rate", zlib.averageCompressionRate(), Level
					.FINE);
			list.add("zlibio", "Average decompression rate", zlib.averageDecompressionRate(),
					Level.FINE);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getTotalBuffOverflow() {
		return io.getTotalBuffOverflow();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getTotalBytesReceived() {
		return io.getTotalBytesReceived();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getTotalBytesSent() {
		return io.getTotalBytesSent();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
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
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

	//~--- set methods ----------------------------------------------------------

	/*
	 *  (non-Javadoc)
	 * @see tigase.io.IOInterface#setLogId(java.lang.String)
	 */

	/**
	 * Method description
	 *
	 *
	 * @param logId is a <code>String</code>
	 */
	@Override
	public void setLogId(String logId) {
		io.setLogId(logId);
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
