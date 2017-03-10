/*
 * WebSocketProtocolIfc.java
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Interface which needs to be implemented by any implemention of version of
 * WebSocket protocol. 
 * 
 * Currently we have stable version but there were older not compatible with 
 * current so it may be that new will come in future - also not compatible.
 * 
 * @author andrzej
 */
public interface WebSocketProtocolIfc {
	
	/**
	 * HTTP header used by WebSocket to pass used version of WebSocket protocol
	 * from client to server
	 */
    public static final String WS_VERSION_KEY  = "Sec-WebSocket-Version";
	/**
	 * HTTP header which contains name of subprotocol which should be used over
	 * established WebSocket connection
	 */
	static final String WS_PROTOCOL_KEY = "Sec-WebSocket-Protocol";
	
	/**
	 * Method to retrieve string identifier of implementation of protcol version
	 * 
	 * @return 
	 */
	String getId();
	
	/**
	 * Method responsible for handshaking of WebSocket using proper version of
	 * protocol.
	 * 
	 * @param service
	 * @param headers
	 * @param buf
	 * @return false - if implementation is not able to handshake using this
	 *					version of protocol, in other case return true
	 * @throws NoSuchAlgorithmException
	 * @throws IOException 
	 */
	boolean handshake(WebSocketXMPPIOService service, Map<String,String> headers, byte[] buf) throws NoSuchAlgorithmException, IOException;
	
	/**
	 * Method responsible for decoding data received from socket and returning 
	 * data after extracting it from WebSocket frame.
	 * 
	 * @param service
	 * @param buf
	 * @return decoded data or null if not full frame is available in input buffer
	 */
	ByteBuffer decodeFrame(WebSocketXMPPIOService service, ByteBuffer buf);
	
	/**
	 * Method encodes data into WebSocket frame and writes it to passed service
	 * 
	 * @param service
	 * @param buf
	 * @throws IOException
	 */
	void encodeFrameAndWrite(WebSocketXMPPIOService service, ByteBuffer buf) throws IOException;
	
	/**
	 * Method closes connection by sending close frame
	 * @param service
	 */
	void closeConnection(WebSocketXMPPIOService service);
	
}
