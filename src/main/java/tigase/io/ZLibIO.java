/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import tigase.util.ZLibWrapper;

/**
 * Created: Jul 29, 2009 11:58:02 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ZLibIO implements IOInterface {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static Logger log = Logger.getLogger(ZLibIO.class.getName());

	private IOInterface io = null;
	private ZLibWrapper zlib = null;

	public ZLibIO(final IOInterface ioi, final int level) {
		this.io = ioi;
		zlib = new ZLibWrapper();
	}

	@Override
	public SocketChannel getSocketChannel() {
    return io.getSocketChannel();
	}

	@Override
	public void stop() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stop called...");
		}
    io.stop();
		zlib.end();
	}

	@Override
	public boolean isConnected() {
    return io.isConnected();
	}

	@Override
	public int write(ByteBuffer buff)	throws IOException {
		if (buff == null) {
			return io.write(null);
		}
		ByteBuffer compressed_buff = zlib.compress(buff);
		//System.out.println("Compression rate: " + zlib.lastCompressionRate());
		return io.write(compressed_buff);
	}

	@Override
	public ByteBuffer read(ByteBuffer buff)	throws IOException {
		ByteBuffer tmpBuffer = io.read(buff);
		if (io.bytesRead() > 0) {
			ByteBuffer decompressed_buff = zlib.decompress(tmpBuffer);
			// The buffer is reused to it needs to be cleared before it can be
			// used again.
			tmpBuffer.clear();
			//System.out.println("Decompression rate: " + zlib.lastDecompressionRate());
			return decompressed_buff;
		}
		return null;
	}

	@Override
	public int bytesRead() {
    return io.bytesRead();
	}

	@Override
	public int getInputPacketSize()
					throws IOException {
		return io.getInputPacketSize();
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
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

}
