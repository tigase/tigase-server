/*
 * WebSocketHixie76.java
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

import tigase.net.SocketType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class implements Hixie-76 version of WebSocket protocol specification
 * which is used in connection handshaking as well as in frameing/deframing of
 * data sent over WebSocket connection
 * 
 * @see <a href="https://tools.ietf.org/html/draft-hixie-thewebsocketprotocol-76">WebSocket Hixie-76 specification</a>
 *
 * @author andrzej
 */
public class WebSocketHixie76 implements WebSocketProtocolIfc {
	
	private static final Logger log = Logger.getLogger(WebSocketHixie76.class.getCanonicalName());

	public static final String ID = "hixie-76";
	
	private static final String RESPONSE_HEADER =
		"HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: WebSocket\r\n" +
		"Connection: Upgrade\r\n" + "Access-Control-Allow-Origin: *\r\n" +
		"Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
		"Access-Control-Allow-Headers: Content-Type\r\n" +
		"Access-Control-Max-Age: 86400\r\n";	
	
	private static final String HOST_KEY		= "Host";
	private static final String ORIGIN_KEY		= "Origin";
	private static final String WS_KEY1_KEY		= "Sec-WebSocket-Key1";
	private static final String WS_KEY2_KEY		= "Sec-WebSocket-Key2";
	private static final String WS_ORIGIN_KEY	= "Sec-WebSocket-Origin";
	private static final String WS_LOCATION_KEY	= "Sec-WebSocket-Location";	
	
	private static final byte[] FRAME_HEADER = new byte[] { (byte) 0x00 };
	private static final byte[] FRAME_FOOTER = new byte[] { (byte) 0xFF };
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public boolean handshake(WebSocketXMPPIOService service, Map<String, String> headers, byte[] buf) throws NoSuchAlgorithmException, IOException {
		if (headers.containsKey(WS_VERSION_KEY)) {
			return false;
		}	

		byte[] secBufArr = new byte[16];
		Long secKey1 = decodeHyxie76SecKey(headers.get(WS_KEY1_KEY));
		Long secKey2 = decodeHyxie76SecKey(headers.get(WS_KEY2_KEY));
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "WS-KEY1 = {0}", secKey1);
			log.log(Level.FINEST, "WS-KEY2 = {0}", secKey2);
		}
		uintToBytes(secBufArr, 0, secKey1);
		uintToBytes(secBufArr, 4, secKey2);
		if (buf[buf.length - 9] != '\n') {
			throw new IOException("buf[len-9] != \\n!!");
		}
		for (int j = 8; j > 0; j--) {
			secBufArr[8 + 8 - j] = buf[buf.length - j];
		}

		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] resp = md.digest(secBufArr);

		StringBuilder response = new StringBuilder(RESPONSE_HEADER.length() * 2);
		response.append(RESPONSE_HEADER);

		response.append("Content-Length: ").append(resp.length).append("\r\n");
		response.append(WS_PROTOCOL_KEY).append(": ");
		if (headers.get(WS_PROTOCOL_KEY).contains("xmpp-framing")) {
			response.append("xmpp-framing");
		} else {
			response.append("xmpp");
		}
		response.append("\r\n");		
		if (headers.containsKey(ORIGIN_KEY)) {
			response.append(WS_ORIGIN_KEY).append(": ").append(headers.get(ORIGIN_KEY)).append("\r\n");
		}

		boolean ssl = SocketType.ssl == ((SocketType) service.getSessionData().get("socket"));
		int localPort = service.getLocalPort();
		String location = (ssl ? "wss://" : "ws://")
				+ headers.get(HOST_KEY) + (((ssl && localPort == 443) || (!ssl && localPort == 80) || headers.get(HOST_KEY).contains(":")) ? "" : (":" + localPort)) + "/";
		response.append(WS_LOCATION_KEY).append(": ").append(location).append("\r\n");

		response.append("\r\n");

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "sending response = \n{0}", response.toString());
		}

		byte[] respBytes = response.toString().getBytes();
		ByteBuffer out = ByteBuffer.allocate(respBytes.length + 16);
		out.put(respBytes);
		out.put(resp);
		out.flip();
		service.writeBytes(out);

		return true;
	}

	@Override
	public ByteBuffer decodeFrame(WebSocketXMPPIOService service, ByteBuffer buf) {
		if (!buf.hasRemaining()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("no content remainging to process");
			}

			return null;
		}
		
		int position   = buf.position();
		byte type = buf.get();
		
		log.finest("read type = " + type);
		
		if ((type & 0x80) != 0x80) {
			int idx = position + 1;
			int remaining = buf.remaining();
			log.finest("remaining = " + remaining + " on position " + position);
			while ((remaining - ((idx-position))) >= 0) {
				log.finest("checking byte at " + idx + " = " + buf.get(idx));
				if (buf.get(idx) != ((byte) 0xFF)) {
					idx++;
					continue;
				}
				log.finest("found data of " + ((idx-position)-1) + " bytes");
				byte[] data = new byte[(idx-position)-1];
				buf.get(data);
				buf.position(buf.position()+1);
				log.finest("read data = " + new String(data));
				return ByteBuffer.wrap(data);
			}
			buf.position(position);
			return null;
		} else {
			long len = 0;
			byte b = 0;
			while (((b=buf.get()) & 0x80 ) == 0x80) {
				len = (len * 128) + (b & 0x7f);
			}
			len = (len * 128) + (b & 0x7f);
			
			if (len == 0) {
				// close request
				if (log.isLoggable(Level.FINEST)) {
					log.finest("closing connection due to client request");
				}
				service.setState(WebSocketXMPPIOService.State.closed);
				//service.forceStop();

				return null;				
			}
			
			if (buf.remaining() < len) {
				buf.position(position);
				return null;
			}
			
			byte[] data = new byte[(int) len];
			buf.get(data);
			return ByteBuffer.wrap(data);
		}
	}

	@Override
	public void encodeFrameAndWrite(WebSocketXMPPIOService service, ByteBuffer buf) throws IOException {
		service.writeBytes(ByteBuffer.wrap(FRAME_HEADER));
		service.writeBytes(buf);
		service.writeBytes(ByteBuffer.wrap(FRAME_FOOTER));
	}

	@Override
	public void closeConnection(WebSocketXMPPIOService service) {
		if (!service.isConnected())
			return;

		service.setState(WebSocketXMPPIOService.State.closed);
		service.writeBytes(ByteBuffer.wrap(new byte[] { (byte) 0xFF, (byte) 0x00 }));
	}
	
	private void uintToBytes(byte[] arr, int offset, long val) {
		for (int i=3; i>=0; i--) {
			arr[offset + i] = (byte) (val % 256);
			val = val / 256;
		}
	}
	
	private Long decodeHyxie76SecKey(String data) {
		long result = 0;
		int spaces = 0;
		
		for (char ch : data.trim().toCharArray()) {
			switch (ch) {
				case '0':
					result = result * 10 + 0;
					break;
				case '1':
					result = result * 10 + 1;
					break;
				case '2':
					result = result * 10 + 2;
					break;
				case '3':
					result = result * 10 + 3;
					break;
				case '4':
					result = result * 10 + 4;
					break;
				case '5':
					result = result * 10 + 5;
					break;
				case '6':
					result = result * 10 + 6;
					break;
				case '7':
					result = result * 10 + 7;
					break;
				case '8':
					result = result * 10 + 8;
					break;
				case '9':
					result = result * 10 + 9;
					break;
				case ' ':
					spaces++;
					break;
				default:
					break;
			}
		}
		
		return result / spaces;
	}	
}
