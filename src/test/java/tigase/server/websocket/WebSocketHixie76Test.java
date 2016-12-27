/*
 * WebSocketHixie76Test.java
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

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author andrzej
 */
public class WebSocketHixie76Test extends TestCase {
	
	private WebSocketHixie76 impl;
	
	@Override
	protected void setUp() throws Exception {
		impl = new WebSocketHixie76();
	}

	@Override
	protected void tearDown() throws Exception {
		impl = null;
	}
	
	@Test
	public void testFrameEncodingDecoding() throws IOException {
		String input = "<test-data><subdata/></test-data>";
		ByteBuffer buf = ByteBuffer.wrap(input.getBytes());
		final ByteBuffer tmp = ByteBuffer.allocate(1024);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHixie76() }) {

			@Override
			protected void writeBytes(ByteBuffer data) {
				tmp.put(data);
			}

		};
		impl.encodeFrameAndWrite(io, buf);
		tmp.flip();
		ByteBuffer decoded = impl.decodeFrame(io, tmp);
		Assert.assertArrayEquals("Data before encoding do not match data after decoding", input.getBytes(), decoded.array());
	}
	
	@Test
	public void testHandshakeOK() throws NoSuchAlgorithmException, IOException {
		final ByteBuffer tmp = ByteBuffer.allocate(2048);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHixie76() }) {

			@Override
			protected void writeBytes(ByteBuffer data) {
				tmp.put(data);
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};		
		Map<String,String> params = new HashMap<String,String>();
		params.put("Sec-WebSocket-Key1", "1C2J899_05  6  !  M 9    ^4");
		params.put("Sec-WebSocket-Key2", "23 2ff0M_E0#.454X23");
		params.put("Sec-WebSocket-Protocol", "xmpp");
		byte[] bytes = new byte[10];
		bytes[0] = '\r';
		bytes[1] = '\n';
		Assert.assertTrue("Handshake failed", impl.handshake(io, params, bytes));
	}
	
	@Test
	public void testHandshakeFail() throws NoSuchAlgorithmException, IOException {
		final ByteBuffer tmp = ByteBuffer.allocate(2048);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHixie76() }) {

			@Override
			protected void writeBytes(ByteBuffer data) {
				tmp.put(data);
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};		
		Map<String,String> params = new HashMap<String,String>();
		params.put("Sec-WebSocket-Version", "13");
		params.put("Sec-WebSocket-Protocol", "xmpp");		
		byte[] bytes = new byte[10];
		bytes[0] = '\r';
		bytes[1] = '\n';
		Assert.assertFalse("Handshake succeeded", impl.handshake(io, params, bytes));
	}
}
