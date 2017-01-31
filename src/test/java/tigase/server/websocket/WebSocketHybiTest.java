/*
 * WebSocketHybiTest.java
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
public class WebSocketHybiTest extends TestCase {
	
	private WebSocketHybi impl;
	
	@Override
	protected void setUp() throws Exception {
		impl = new WebSocketHybi();
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
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHybi() }) {

			@Override
			protected void writeBytes(ByteBuffer data) {
				tmp.put(data);
			}

		};
		io.maskingKey = new byte[4];
		impl.encodeFrameAndWrite(io, buf);
		tmp.flip();
		ByteBuffer tmp1 = maskFrame(tmp);
		ByteBuffer decoded = impl.decodeFrame(io, tmp1);
		Assert.assertArrayEquals("Data before encoding do not match data after decoding", input.getBytes(), decoded.array());
	}

	private ByteBuffer maskFrame(ByteBuffer data) {
		ByteBuffer tmp = ByteBuffer.allocate(1024);
		byte[] header = new byte[2];
		data.get(header);
		header[header.length - 1] = (byte) (header[header.length - 1] | 0x80);
		tmp.put(header);
		byte[] mask = { 0x00, 0x00, 0x00, 0x00 };
		tmp.put(mask);
		byte b;
		while (data.hasRemaining()) {
			b = data.get();
			b = (byte) (b ^ 0x00);
			tmp.put(b);
		}
		tmp.flip();
		return tmp;
	}


	@Test
	public void testHandshakeFail() throws NoSuchAlgorithmException, IOException {
		final ByteBuffer tmp = ByteBuffer.allocate(2048);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHybi() }) {

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
		Assert.assertFalse("Handshake succeeded", impl.handshake(io, params, bytes));
	}
	
	@Test
	public void testHandshakeOK() throws NoSuchAlgorithmException, IOException {
		final ByteBuffer tmp = ByteBuffer.allocate(2048);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHybi() }) {

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
		params.put("Sec-WebSocket-Key", "some random data as a key");
		params.put("Sec-WebSocket-Protocol", "xmpp");
		byte[] bytes = new byte[10];
		bytes[0] = '\r';
		bytes[1] = '\n';
		Assert.assertTrue("Handshake failed", impl.handshake(io, params, bytes));
	}	

	@Test
	public void testTwoWebSocketTextFramesInSingleTcpFrame() throws Exception {
		String input1 = "<test-data><subdata/></test-data>";
		String input2 = "<test2/>";
		ByteBuffer frame1 = generateIncomingFrame(input1);
		ByteBuffer frame2 = generateIncomingFrame(input2);

		ByteBuffer tmp = ByteBuffer.allocate(frame1.remaining() + frame2.remaining());
		tmp.put(frame1);
		tmp.put(frame2);
		tmp.flip();

		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHybi() });
		io.maskingKey = new byte[4];
		ByteBuffer decoded = impl.decodeFrame(io, tmp);
		Assert.assertArrayEquals("Data of first frame before encoding do not match data after decoding", input1.getBytes(), decoded.array());
		decoded = impl.decodeFrame(io, tmp);
		Assert.assertArrayEquals("Data of second frame before encoding do not match data after decoding", input2.getBytes(), decoded.array());
	}

	@Test
	public void testTwoWebSocketFramesPingAndTextFrameInSingleTcpFrame() throws Exception {
		String input2 = "<test-data><subdata/></test-data>";
		ByteBuffer frame1 = ByteBuffer.allocate(20);
		frame1.put((byte) 0x89);
		frame1.put((byte) 0x04);
		frame1.put(new byte[] { 0x00, 0x00, 0x00, 0x00 });
		frame1.flip();
		frame1 = maskFrame(frame1);
		ByteBuffer frame2 = generateIncomingFrame(input2);

		ByteBuffer tmp = ByteBuffer.allocate(frame1.remaining() + frame2.remaining());
		tmp.put(frame1);
		tmp.put(frame2);
		tmp.flip();

		ByteBuffer tmp2 = ByteBuffer.allocate(1024);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHybi() }) {
			@Override
			protected void writeBytes(ByteBuffer data) {
				tmp2.put(data);
			}
		};
		io.maskingKey = new byte[4];
		ByteBuffer decoded = impl.decodeFrame(io, tmp);
		Assert.assertNotNull(decoded);
		Assert.assertArrayEquals("Data of first frame before encoding do not match data after decoding", new byte[0], decoded.array());
		tmp2.flip();
		Assert.assertNotEquals("PONG frame not sent!", 0, tmp2.remaining());
		assertEquals("PONG frame not sent!", (byte) 0x8A, tmp2.get(0));

		decoded = impl.decodeFrame(io, tmp);
		Assert.assertArrayEquals("Data of second frame before encoding do not match data after decoding", input2.getBytes(), decoded.array());

	}

	private ByteBuffer generateIncomingFrame(String input) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(input.getBytes());
		final ByteBuffer tmp = ByteBuffer.allocate(1024);
		WebSocketXMPPIOService<Object> io = new WebSocketXMPPIOService<Object>(new WebSocketProtocolIfc[]{ new WebSocketHybi() }) {

			@Override
			protected void writeBytes(ByteBuffer data) {
				tmp.put(data);
			}

		};
		io.maskingKey = new byte[4];
		impl.encodeFrameAndWrite(io, buf);
		tmp.flip();
		return maskFrame(tmp);
	}
}
