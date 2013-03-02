/*
 * WebSocketXMPPIOService.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



package tigase.server.websocket;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.Base64;

import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class implements basic support for WebSocket protocol. It extends
 * XMPPIOService so it can be used instead of XMPPIOService in
 * ClientConnectionManager to allow web clients to connect to it without using
 * BOSH extension.
 *
 * @param <RefObject>
 */
public class WebSocketXMPPIOService<RefObject>
				extends XMPPIOService<RefObject> {
	private static final String BAD_REQUEST    = "HTTP/1.0 400 Bad request\r\n\r\n";
	private static final String CONNECTION_KEY = "Connection";
	private static final String GUID           = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private static final String HOST_KEY       = "Host";
	private static final Logger log            =
		Logger.getLogger(WebSocketXMPPIOService.class.getCanonicalName());
	private static final String ORIGIN_KEY = "Origin";

	/* static variables used by WebSocket protocol */
	private static final String RESPONSE_HEADER =
		"HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" + "Access-Control-Allow-Origin: *\r\n" +
		"Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
		"Access-Control-Allow-Headers: Content-Type\r\n" +
		"Access-Control-Max-Age: 86400\r\n";
	private static final String WS_ACCEPT_KEY   = "Sec-WebSocket-Accept";
	private static final String WS_KEY_KEY      = "Sec-WebSocket-Key";
	private static final String WS_PROTOCOL_KEY = "Sec-WebSocket-Protocol";
	private static final String WS_VERSION_KEY  = "Sec-WebSocket-Version";

	//~--- fields ---------------------------------------------------------------

	private byte[] buf        = null;
	private long frameLength  = -1;
	private byte[] maskingKey = null;
	private int pos           = 0;
	private int version       = 0;

	private byte[] partialFrame = null;
	
	// internal properties
	private boolean websocket = false;
	private boolean started   = false;

	//~--- methods --------------------------------------------------------------

	/**
	 * Custom implementation of readData function which decodes WebSocket
	 * protocol frames
	 *
	 * @return
	 * @throws IOException
	 */
	@Override
	protected char[] readData() throws IOException {
		ByteBuffer cb = super.readBytes();

		if (cb == null) {
			return null;
		}
		if (websocket) {
			// handling partialy decoded frame
			if (partialFrame != null) {
				ByteBuffer oldtmp = cb;
				cb = ByteBuffer.allocate(partialFrame.length + oldtmp.remaining());
				cb.order(oldtmp.order());
				cb.put(partialFrame);
				cb.put(oldtmp);
				cb.flip();
				oldtmp.clear();
				partialFrame = null;
				
			}
			
			// data needs to be decoded fully not just first frame!!
			ByteBuffer tmp = ByteBuffer.allocate(cb.remaining());	
			// here we got buffer overflow
			ByteBuffer decoded = null;
			while (cb.hasRemaining() && (decoded = decodeFrame(cb)) != null) {
				//decoded = decodeFrame(cb);
				if (decoded != null && decoded.hasRemaining()) {
					tmp.put(decoded);
				}
			}
			
			// handling data which were not decoded - not complete data
			if (cb.hasRemaining()) {
				partialFrame = new byte[cb.remaining()];
				cb.get(partialFrame);
			}
			
			// compact buffer after reading all frames
			cb.compact();
			
			if (tmp.capacity() > 0) {
				tmp.flip();
			}
			cb = tmp; 
		}
		if (started) {
			return decode(cb);
		}
		if ((pos == 0) && (cb.get(0) != (byte) 'G')) {
			started = true;

			return decode(cb);
		}
		if (buf == null) {
			buf = new byte[1024];
		}
		try {
			int read = cb.remaining();

			cb.get(buf, pos, read);
			pos += read;
			cb.compact();
			if ((pos > 100) &&
					(((buf[pos - 1] == '\n') && (buf[pos - 1] == buf[pos - 3])) ||
					 ((buf[pos - 9] == '\n') && (buf[pos - 9] == buf[pos - 11])))) {
				started = true;
				processWebSocketHandshake();
				websocket = true;
				buf       = null;
			}
		} catch (Exception ex) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "exception processing websocket handshake", ex);
			}
			this.forceStop();
		}

		return null;
	}

	/**
	 * Custom implementation of writeData function which encodes data
	 * in WebSocket protocol frames
	 *
	 * @param data
	 */
	@Override
	protected void writeData(final String data) {

		// Try to lock the data writing method
		// If cannot lock and nothing to send, just leave
		boolean locked = writeInProgress.tryLock();

		// Otherwise wait.....
		if (!locked) {
			if (data == null) {
				return;
			}
			
			writeInProgress.lock();
		}
		try {
			if (websocket) {
				try {
					if (data != null) {					
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "sending data = {0}", data);
						}

						ByteBuffer buf = encode(data);
						int size = buf.remaining();

						// set type as finally part (0x80) of message of type text (0x01)
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "sending encoded data size = {0}", size);
						}

						ByteBuffer bbuf = createFrameHeader((byte) 0x81, size);

						// send frame header
						writeBytes(bbuf);

						// send frame content
						writeBytes(buf);
						buf.compact();
					}
					else {
						writeBytes(null);
					}
				} catch (Exception ex) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "exception writing data", ex);
					}
					forceStop();
				}
			} else {
				super.writeData(data);
			}
		} finally {
			writeInProgress.unlock();
		}
	}

	/**
	 * Process data from internal temporary buffer used to decode HTTP request
	 * used by WebSocket protocol to switch protocol to WebSocket protocol
	 *
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private void processWebSocketHandshake() throws NoSuchAlgorithmException, IOException {
		HashMap<String, String> headers = new HashMap<String, String>();
		int i                           = 0;

		while (buf[i] != '\n') {
			i++;
		}
		i++;
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "parsing request = \n{0}", new String(buf));
		}

		StringBuilder builder = new StringBuilder(64);
		String key            = null;

		for (; i < pos; i++) {
			switch (buf[i]) {
			case ':' :
				if (key == null) {
					key     = builder.toString();
					builder = new StringBuilder(64);
					i++;
				} else {
					builder.append((char) buf[i]);
				}

				break;

			case '\r' :
				headers.put(key, builder.toString());
				key     = null;
				builder = new StringBuilder(64);
				if (buf[i + 2] == '\r') {
					i += 3;
				} else {
					i++;
				}

				break;

			default :
				builder.append((char) buf[i]);
			}
		}
		if (!headers.containsKey(CONNECTION_KEY) ||
				!headers.get(CONNECTION_KEY).contains("Upgrade")) {
			writeRawData(BAD_REQUEST);

			return;
		}
		if (!headers.containsKey(WS_PROTOCOL_KEY) ||
				!headers.get(WS_PROTOCOL_KEY).contains("xmpp")) {
			writeRawData(BAD_REQUEST);

			return;
		}

		StringBuilder response = new StringBuilder(RESPONSE_HEADER.length() * 2);

		response.append(RESPONSE_HEADER);
		if (headers.containsKey(WS_VERSION_KEY)) {
			version = Integer.parseInt(headers.get(WS_VERSION_KEY));
			key     = headers.get(WS_KEY_KEY) + GUID;

			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] resp      = md.digest(key.getBytes());

			response.append(WS_PROTOCOL_KEY);
			response.append(": xmpp\r\n");
			response.append(WS_ACCEPT_KEY + ": ");
			response.append(Base64.encode(resp));
			response.append("\r\n");
			response.append("\r\n");
			maskingKey = new byte[4];
			writeRawData(response.toString());
		}
	}

	/**
	 * Decode data encoded in WebSocket frames from buffer
	 *
	 * @param buf
	 * @return
	 */
	private ByteBuffer decodeFrame(ByteBuffer buf) {
		if (!buf.hasRemaining()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("no content remainging to process");
			}

			return null;
		}

		boolean masked = false;
		byte type      = 0x00;
		int position   = buf.position();
		
		if (frameLength == -1) {
			type = buf.get();
			if ((type & 0x08) == 0x08) {

				// close request
				if (log.isLoggable(Level.FINEST)) {
					log.finest("closing connection due to client request");
				}
				forceStop();

				return null;
			}

			byte b2 = buf.get();

			// check if content is masked
			masked = (b2 & 0x80) == 0x80;

			// ignore sign bit
			frameLength = (b2 & 0x7F);
			if (frameLength > 125) {

				// if frame length is bigger than 125 then
				// if is 126 - size is short
				// is is 127 - size is long
				frameLength = (frameLength == 126)
											? buf.getShort()
											: buf.getLong();
			}
			if (masked) {

				// if content is masked get masking key
				buf.get(maskingKey);
			}
		}

		ByteBuffer unmasked = null;

		if (buf.remaining() >= frameLength) {
			byte[] data = new byte[(int) frameLength];

			buf.get(data);

			// if content is masked then unmask content
			if (masked) {
				for (int i = 0; i < data.length; i++) {
					data[i] = (byte) (data[i] ^ maskingKey[i % 4]);
				}
			}
			unmasked    = ByteBuffer.wrap(data);
			frameLength = -1;
		}
		else {
			// not enought data so reset buffer position
			buf.position(position);
			frameLength = -1;
			return null;
		}	
		
		if (frameLength == -1) {

			// we need to ignore pong frame
			if ((type & 0x0A) == 0x0A) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("ignoring pong frame");
				}
				unmasked = null;
			}

			// if it ping request send pong response
			else if ((type & 0x09) == 0x09) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("sending response on ping frame");
				}
				type = (byte) (((byte) (type ^ 0x09)) | 0x0A);
				try {
					ByteBuffer header = createFrameHeader(type, unmasked.remaining());

					writeInProgress.lock();
					writeBytes(header);
					writeBytes(unmasked);
				} finally {
					writeInProgress.unlock();
				}
				unmasked = null;
			}
		}

		return unmasked;
	}

	/**
	 * Create WebSocket frame header with specific type and size
	 *
	 * @param type
	 * @param size
	 * @return
	 */
	private ByteBuffer createFrameHeader(byte type, int size) {
		ByteBuffer bbuf = ByteBuffer.allocate(9);

		bbuf.put(type);
		if (size <= 125) {
			bbuf.put((byte) size);
		} else if (size <= 0xFFFF) {
			bbuf.put((byte) 0x7E);
			bbuf.putShort((short) size);
		} else {
			bbuf.put((byte) 0x7F);
			bbuf.putLong((long) size);
		}
		bbuf.flip();

		return bbuf;
	}

	/**
	 * Decode data from buffer to chars array
	 *
	 * @param tmpBuffer
	 * @return
	 * @throws MalformedInputException
	 */
	private char[] decode(ByteBuffer tmpBuffer) throws MalformedInputException {
		if (tmpBuffer == null) {
			return null;
		}

		char[] result = null;

		// Restore the partial bytes for multibyte UTF8 characters
		if (partialCharacterBytes != null) {
			ByteBuffer oldTmpBuffer = tmpBuffer;

			tmpBuffer = ByteBuffer.allocate(partialCharacterBytes.length +
																			oldTmpBuffer.remaining() + 2);
			tmpBuffer.put(partialCharacterBytes);
			tmpBuffer.put(oldTmpBuffer);
			tmpBuffer.flip();
			oldTmpBuffer.clear();
			partialCharacterBytes = null;
		}
		if (cb.capacity() < tmpBuffer.remaining() * 4) {
			cb = CharBuffer.allocate(tmpBuffer.remaining() * 4);
		}

		CoderResult cr = decoder.decode(tmpBuffer, cb, false);

		if (cr.isMalformed()) {
			throw new MalformedInputException(tmpBuffer.remaining());
		}
		if (cb.remaining() > 0) {
			cb.flip();
			result = new char[cb.remaining()];
			cb.get(result);
		}
		if (cr.isUnderflow() && (tmpBuffer.remaining() > 0)) {

			// Save the partial bytes of a multibyte character such that they
			// can be restored on the next read.
			partialCharacterBytes = new byte[tmpBuffer.remaining()];
			tmpBuffer.get(partialCharacterBytes);
		}
		tmpBuffer.clear();
		cb.clear();

		return result;
	}

	/**
	 * Encode string into buffer
	 *
	 * @param data
	 * @return
	 * @throws CharacterCodingException
	 */
	private ByteBuffer encode(String data) throws CharacterCodingException {
		ByteBuffer dataBuffer = null;

		encoder.reset();

		// dataBuffer = encoder.encode(CharBuffer.wrap(data, idx_start,
		// idx_offset));
		dataBuffer = encoder.encode(CharBuffer.wrap(data));
		encoder.flush(dataBuffer);

		// dataBuffer.flip();
		return dataBuffer;
	}
}


//~ Formatted in Tigase Code Convention on 13/02/19
