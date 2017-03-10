/*
 * WebSocketXMPPIOService.java
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.xmpp.XMPPIOService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
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
	private static final Logger log            =
		Logger.getLogger(WebSocketXMPPIOService.class.getCanonicalName());

	private static final String CLOSE_EL = "close";
	private static final String OPEN_EL = "open";
	private static final String XMLNS_FRAMING = "urn:ietf:params:xml:ns:xmpp-framing";
	
	public static enum WebSocketXMPPSpec {
		hybi,
		xmpp
	}

	public enum State {
		// begining state - used when we retrieve data from
		// connection to detect if connection is able to be
		// upgraded to WebSocket
		handshaking,
		// after WebSocket handshake is completed - can send data
		handshaked,
		// when server sent close request
		closing,
		// when server sent close request and received
		// close request from client
		closed
	}

	/* static variables used by WebSocket protocol */
	
	//~--- fields ---------------------------------------------------------------

	protected long frameLength  = -1;
	protected byte[] maskingKey = null;
	private byte[] partialData = null;
	
	// internal properties
	//private boolean websocket = false;
	private State state = State.handshaking;
	private boolean started   = false;

	private final WebSocketProtocolIfc[] protocols;
	private WebSocketProtocolIfc protocol = null;
	private WebSocketXMPPSpec webSocketXMPPSpec = WebSocketXMPPSpec.hybi;
	
	public WebSocketXMPPIOService(WebSocketProtocolIfc[] enabledProtocols) {
		this.protocols = enabledProtocols;
	}

	@Override
	public void stop() {
		protocol.closeConnection(this);
		super.stop(); //To change body of generated methods, choose Tools | Templates.
	}

	protected State getState() {
		return state;
	}

	protected void setState(State state) {
		this.state = state;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	protected void addReceivedPacket(Packet packet) {
		if (packet.getXMLNS() == XMLNS_FRAMING) {
			// it is framing packet, so it should be <open/> or <close/>
			if (packet.getElemName() == OPEN_EL) {
				webSocketXMPPSpec = WebSocketXMPPSpec.xmpp;
				xmppStreamOpened(packet.getElement().getAttributes());
				return;
			} else if (packet.getElemName() == CLOSE_EL) {
				xmppStreamClosed();
				return;
			}
		}
		super.addReceivedPacket(packet); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	protected void processSocketData() throws IOException {
		State oldState = state;
		super.processSocketData();
		if (state != oldState && oldState == State.handshaked) {
			protocol.closeConnection(this);
		}
	}

	protected WebSocketXMPPSpec getWebSocketXMPPSpec() {
		return webSocketXMPPSpec;
	}
	
	@Override
	protected String prepareStreamClose() {
		if (webSocketXMPPSpec == WebSocketXMPPSpec.hybi)
			return "</stream:stream>";
		return "<close xmlns='urn:ietf:params:xml:ns:xmpp-framing' />";
	}		

	@Override
	protected char[] readData() throws IOException {
		ByteBuffer cb = super.readBytes();

		if (cb == null) {
			return null;
		}

		// handling partialy decoded frame
		if (partialData != null) {
			ByteBuffer oldtmp = cb;
			cb = ByteBuffer.allocate(partialData.length + oldtmp.remaining());
			cb.order(oldtmp.order());
			cb.put(partialData);
			cb.put(oldtmp);
			cb.flip();
			oldtmp.clear();
			partialData = null;
		}
		
		if (state != State.handshaking) {

			
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
				partialData = new byte[cb.remaining()];
				cb.get(partialData);
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
		
		if (!cb.hasRemaining()) {
			return null;
		}
		
		try {
/*			if (!started && (cb.get(0) != (byte) 'G')) {
				started = true;

				return decode(cb);
			}*/
			
			int remaining = cb.remaining();
			byte[] buf = new byte[remaining];
			
			cb.get(buf, 0, remaining);
			//pos += read;
			cb.compact();
			int pos = remaining;
			if ((pos > 100) &&
					(((buf[pos - 1] == '\n') && (buf[pos - 1] == buf[pos - 3])) ||
					 ((buf[pos - 9] == '\n') && (buf[pos - 9] == buf[pos - 11])))) {
				started = true;
				processWebSocketHandshake(buf);
				//websocket = true;
				if (protocol != null)
					state = State.handshaked;
			}
			else {
				partialData = buf;
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
			if (state != State.handshaking) {
				try {
					if (data != null) {					
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "sending data = {0}", data);
						}

						ByteBuffer buf = encode(data);
						protocol.encodeFrameAndWrite(this, buf);

						//buf.compact();
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
	private void processWebSocketHandshake(byte[] buf) throws NoSuchAlgorithmException, IOException {
		HashMap<String, String> headers = new HashMap<String, String>();
		int i = parseHttpHeaders(buf, headers);

		if (!headers.containsKey(CONNECTION_KEY) ||
				!headers.get(CONNECTION_KEY).contains("Upgrade")) {
			writeRawData(BAD_REQUEST);
			
			dumpHeaders(headers);
			forceStop();
			return;
		}
		if (!headers.containsKey(WebSocketProtocolIfc.WS_PROTOCOL_KEY) ||
				!headers.get(WebSocketProtocolIfc.WS_PROTOCOL_KEY).contains("xmpp")) {
			writeRawData(BAD_REQUEST);

			dumpHeaders(headers);
			forceStop();
			return;
		}

		i=0;
		while (protocol == null && i < protocols.length) {
			if (protocols[i].handshake(this, headers, buf)) {
				protocol = protocols[i];
			} else {
				i++;
			}
		}
		
		if (protocol == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "could not find implementation for WebSocket protocol for {0}", this);
			}
			dumpHeaders(headers);
			forceStop();
		}
	}

	protected int parseHttpHeaders(byte[] buf, Map<String, String> headers) {
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

		boolean skipWhitespace = false;
		for (; i < buf.length; i++) {
			switch (buf[i]) {
				case ':':
					if (key == null) {
						key = builder.toString().trim();
						builder = new StringBuilder(64);
						skipWhitespace = true;
					} else {
						builder.append((char) buf[i]);
					}

					break;

				case '\r':
					headers.put(key, builder.toString().trim());
					key = null;
					builder = new StringBuilder(64);
					if (buf[i + 2] == '\r') {
						i += 3;
					} else {
						i++;
					}

					break;

				case ' ':
				case '\t':
					if (!skipWhitespace) {
						builder.append((char) buf[i]);
					}
					break;

				default:
					skipWhitespace = false;
					builder.append((char) buf[i]);
			}
		}
		return i;
	}
	
	@Override
	protected void writeBytes(ByteBuffer data) {
		super.writeBytes(data);
	}

	/**
	 * Decode data encoded in WebSocket frames from buffer
	 *
	 * @param buf
	 * 
	 */
	private ByteBuffer decodeFrame(ByteBuffer buf) {
		return protocol.decodeFrame(this, buf);
	}

	/**
	 * Decode data from buffer to chars array
	 *
	 * @param tmpBuffer
	 * 
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
	 * 
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
	
	public void dumpHeaders(Map<String,String> headers) {
		if (log.isLoggable(Level.FINEST)) {
			StringBuilder builder = new StringBuilder(1000);
			for(Map.Entry<String,String> entry : headers.entrySet()) {
				builder.append("KEY = ");
				builder.append(entry.getKey());
				builder.append("VALUE = ");
				builder.append(entry.getValue());
				builder.append('\n');
			}
			
			log.log(Level.FINEST, "received headers = \n{0}", builder.toString());
		}		
	}
	
}


//~ Formatted in Tigase Code Convention on 13/02/19
