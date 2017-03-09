/*
 * IOUtil.java
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

package tigase.net;

import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.nio.ch.DirectBuffer;

/**
 *
 * @author andrzej
 */
public class IOUtil {

	private static final Logger log = Logger.getLogger(IOUtil.class.getCanonicalName());
	
	private static final String DIRECT_BUFFER_DEFAULT_SIZE = "direct-buffer-default-size";
	
	private static final ThreadLocal<BufferCache> buffers = new ThreadLocal<BufferCache>();
	
	private static int bufferSize;
	
	static {
		Integer size = Integer.getInteger(DIRECT_BUFFER_DEFAULT_SIZE);
		if (size == null) {
			Socket socket = new Socket();
			try {
				// using socket native buffer size for best performance
				bufferSize = java.lang.Math.max(socket.getReceiveBufferSize(), socket.getSendBufferSize());
			}
			catch(SocketException ex) {
				// cloud not get default value from system - setting to 64k
				bufferSize = 64 * 1024;
			}
		}
		else {
			bufferSize = size;
		}
		
		log.log(Level.CONFIG, "using direct byte buffers with size {0} per buffer", bufferSize);
	}
	
	public static ByteBuffer getDirectBuffer(int size) {
		BufferCache cache = buffers.get();
		if (cache == null) {
			cache = new BufferCache(bufferSize);
			buffers.set(cache);
		}
		
		return cache.get(size);		
	}
	
	public static void returnDirectBuffer(ByteBuffer buf) {
		BufferCache cache = buffers.get();
		if (cache != null) {
			cache.offer(buf);
		}
		else {
			log.log(Level.SEVERE, "returning direct buffer to cache, but no cache found");
		}
	}
	
	private static class BufferCache {
	
		private int count = 1;
		private final ByteBuffer buffer;
		
		public BufferCache(int size) {
			buffer = ByteBuffer.allocateDirect(size);
		}		
		
		public ByteBuffer get(int size) {
			if (count == 1 && size <= buffer.capacity()) {
				count = 0;
				buffer.limit(size);
				return buffer;
			}
			else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "allocating buffer with size = {0}", size);
				}
				return ByteBuffer.allocateDirect(size);
			}
		}
		
		public void offer(ByteBuffer buffer) {
			if (this.buffer == buffer) {
				count = 1;
				buffer.rewind();
			}
			else {
				freeBuffer(buffer);
			}
		}
				
		private void freeBuffer(ByteBuffer buf) {
			((DirectBuffer) buf).cleaner().clean();
		}
		
	}
	
}
