/*
 * WebSocketHybi.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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

import tigase.util.Base64;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.websocket.WebSocketXMPPIOService.State.closing;

/**
 * Class implements Hybi (RFC compatible) version of WebSocket protocol specification
 * which is used in connection handshaking as well as in frameing/deframing of
 * data sent over WebSocket connection
 * 
 * @see <a href="http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-13">WebSocket HyBi specification</a>
 * 
 * @author andrzej
 */
public class WebSocketHybi implements WebSocketProtocolIfc {

	private static final Logger log = Logger.getLogger(WebSocketHybi.class.getCanonicalName());
	
	public static final String ID = "hybi";
	
	private static final String GUID           = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";	
	private static final String RESPONSE_HEADER =
		"HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" + "Access-Control-Allow-Origin: *\r\n" +
		// Removed header below as it creates issues with connectivity using IE11 
//		"Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
		"Access-Control-Allow-Headers: Content-Type\r\n" +
		"Access-Control-Max-Age: 86400\r\n";

	private static final String WS_ACCEPT_KEY   = "Sec-WebSocket-Accept";
	private static final String WS_KEY_KEY      = "Sec-WebSocket-Key";

	private static final String CLOSE_CODE = "close-code";
	private static final boolean ALLOW_UNMASKED_FROM_CLIENT = Boolean.getBoolean("ws-allow-unmasked-frames");
	private static final int PROTOCOL_ERROR = 1003;

	private static byte[] EMPTY = new byte[0];

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public boolean handshake(WebSocketXMPPIOService service, Map<String, String> headers, byte[] buf) throws NoSuchAlgorithmException, IOException {
		if (!headers.containsKey(WS_VERSION_KEY)) {
			return false;
		}
		
		StringBuilder response = new StringBuilder(RESPONSE_HEADER.length() * 2);
		response.append(RESPONSE_HEADER);

		int version = Integer.parseInt(headers.get(WS_VERSION_KEY));
		String key = headers.get(WS_KEY_KEY) + GUID;

		MessageDigest md = MessageDigest.getInstance("SHA1");
		byte[] resp = md.digest(key.getBytes());

		response.append(WS_PROTOCOL_KEY).append(": ");
		if (headers.get(WS_PROTOCOL_KEY).contains("xmpp-framing")) {
			response.append("xmpp-framing");
		} else {
			response.append("xmpp");
		}
		response.append("\r\n");
		response.append(WS_ACCEPT_KEY + ": ");
		response.append(Base64.encode(resp));
		response.append("\r\n");
		response.append("\r\n");
		service.maskingKey = new byte[4];
		service.writeRawData(response.toString());
			
		return true;
	}

	@Override
	public ByteBuffer decodeFrame(WebSocketXMPPIOService service, ByteBuffer buf) {
		if (!buf.hasRemaining()) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: {0}, no content remainging to process", new Object[] { service });
			}

			return null;
		}

		boolean masked = false;
		byte type      = 0x00;
		int position   = buf.position();
		ByteBuffer unmasked = null;
		
		try {
			if (service.frameLength == -1) {
				type = buf.get();
				if ((type & 0x0F) == 0x08) {

					// close request
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket: {0}, closing connection due to client request {1}", 
								new Object[] { service, String.format("%02X ", type) });
					}
					service.setState(closing);
					closeConnection(service, null);
					//service.forceStop();

					return null;
				}

				byte b2 = buf.get();

				// check if content is masked
				masked = (b2 & 0x80) == 0x80;
				if (!masked && !ALLOW_UNMASKED_FROM_CLIENT) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket: {0}, closing connection due to protocol error - unmasked frame sent by client {1}",
								new Object[] { service, String.format("%02X ", type) });
					}
					closeConnection(service, PROTOCOL_ERROR);
					//service.forceStop();
					return null;
				}


				// ignore sign bit
				service.frameLength = (b2 & 0x7F);
				if (service.frameLength > 125) {
					// if frame length is bigger than 125 then
					// if is 126 - size is short (unsigned short)
					// is is 127 - size is long
					service.frameLength = (service.frameLength == 126)
							? (buf.getShort() & 0xffff)
							: buf.getLong();
				}
				if (masked) {

					// if content is masked get masking key
					buf.get(service.maskingKey);
				}
			}

			if (buf.remaining() >= service.frameLength) {
				byte[] data = new byte[(int) service.frameLength];

				buf.get(data);

				// if content is masked then unmask content
				if (masked) {
					byte[] maskingKey = service.maskingKey;
					for (int i = 0; i < data.length; i++) {
						data[i] = (byte) (data[i] ^ maskingKey[i % 4]);
					}
				}
				unmasked = ByteBuffer.wrap(data);
				service.frameLength = -1;
			} else {
				// not enought data so reset buffer position
				buf.position(position);
				service.frameLength = -1;
				return null;
			}

			if (service.frameLength == -1) {

				// we need to ignore pong frame
				if ((type & 0x0A) == 0x0A) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket: {0}, ignoring pong frame", new Object[] { service });
					}
					// We are returning empty byte buffer to make sure other frames remaining in buffer will be processed
					unmasked = ByteBuffer.wrap(EMPTY);
				} // if it ping request send pong response
				else if ((type & 0x09) == 0x09) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket: {0}, sending response on ping frame", new Object[] { service });
					}
					type = (byte) (((byte) (type ^ 0x09)) | 0x0A);
					try {
						ByteBuffer header = createFrameHeader(type, unmasked.remaining());

						service.writeInProgress.lock();
						service.writeBytes(header);
						service.writeBytes(unmasked);
					} finally {
						service.writeInProgress.unlock();
					}
					// We are returning empty byte buffer to make sure other frames remaining in buffer will be processed
					unmasked = ByteBuffer.wrap(EMPTY);
				}
			}
		} catch (BufferUnderflowException ex) {
			// if for some reason we do not have full frame header then we need to 
			// reset buffer to original position and wait for the rest of data
			buf.position(position);
			service.frameLength = -1;
			unmasked = null;
		}

		return unmasked;
	}

	@Override
	public void encodeFrameAndWrite(WebSocketXMPPIOService service, ByteBuffer buf) throws IOException {
		int size = buf.remaining();

		// set type as finally part (0x80) of message of type text (0x01)
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Socket: {0}, sending encoded data size = {1}", new Object[] { service, size });
		}

		ByteBuffer bbuf = createFrameHeader((byte) 0x81, size);

		// send frame header
		service.writeBytes(bbuf);

		service.writeBytes(buf);							
	}
	
	@Override
	public void closeConnection(WebSocketXMPPIOService service) {
		if (!service.isConnected())
			return;

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Socket: {0}, sending close frame", service);
		}

		service.setState(WebSocketXMPPIOService.State.closed);
		Integer code = (Integer) service.getSessionData().get(CLOSE_CODE);
		int len = 0;
		if (code != null) {
			len += 2;
		}
		ByteBuffer bbuf = createFrameHeader((byte) 0x88, len);
		service.writeBytes(bbuf);
		if (code != null) {
			ByteBuffer buf = ByteBuffer.allocate(2);
			buf.putShort(code.shortValue());
			buf.flip();
			service.writeBytes(buf);
		}
	}

	private void closeConnection(WebSocketXMPPIOService service, Integer code) {
		if (code != null)
			service.getSessionData().put(CLOSE_CODE, code);
		switch (service.getState()) {
			case closing:
				service.setState(WebSocketXMPPIOService.State.closed);
				break;
			case handshaked:
				service.setState(WebSocketXMPPIOService.State.closing);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Create WebSocket frame header with specific type and size
	 *
	 * @param type
	 * @param size
	 * 
	 */
	private ByteBuffer createFrameHeader(byte type, int size) {
		ByteBuffer bbuf = ByteBuffer.allocate(12);

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
}
