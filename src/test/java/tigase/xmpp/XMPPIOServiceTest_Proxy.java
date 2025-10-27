/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.xmpp;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Random;

import static tigase.net.IOService.PORT_TYPE_PROP_KEY;
import static tigase.server.ConnectionManager.PORT_PROXY_PROTOCOL_PROP_KEY;

public class XMPPIOServiceTest_Proxy
		extends TestCase {

	private XMPPIOService service;
	private ServerSocketChannel serverSocket;
	private SocketChannel clientSocket;

	@Override
	protected void setUp() throws Exception {
		service = new XMPPIOService();
		service.setSessionData(Map.of(PORT_TYPE_PROP_KEY, "accept", PORT_PROXY_PROTOCOL_PROP_KEY, true));
		serverSocket = ServerSocketChannel.open(StandardProtocolFamily.INET);
		serverSocket = serverSocket.bind(new InetSocketAddress("localhost", 6123 + new Random().nextInt(0, 100)));
		Object lock = new Object();
		new Thread(() -> {
			try {
				SocketChannel channel = serverSocket.accept();
				service.accept(channel, 1024);
			} catch (IOException e) {
			}
			synchronized (lock) {
				lock.notifyAll();
			}
		}).start();
		clientSocket = SocketChannel.open(StandardProtocolFamily.INET);
		clientSocket.connect(serverSocket.getLocalAddress());
		synchronized (lock) {
			lock.wait();
		}
		assertTrue(clientSocket.isOpen());
	}

	@Override
	protected void tearDown() throws Exception {
		serverSocket.close();
		clientSocket.close();
	}

	@Test
	public void testProxy1_IPv4() throws IOException, InterruptedException {
		clientSocket.write(ByteBuffer.wrap("PROXY TCP4 192.168.1.223 192.168.3.22 56342 5222\r\n".getBytes()));
		Thread.sleep(10);
		service.processSocketData();
		assertEquals("192.168.1.223", service.getRemoteAddress());
		assertEquals("192.168.3.22", service.getLocalAddress());
	}

	@Test
	public void testProxy1_IPv6() throws IOException, InterruptedException {
		clientSocket.write(ByteBuffer.wrap("PROXY TCP4 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff eeee:eeee:eeee:eeee:eeee:eeee:eeee:eeee 56342 5222\r\n".getBytes()));
		Thread.sleep(10);
		service.processSocketData();
		assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", service.getRemoteAddress());
		assertEquals("eeee:eeee:eeee:eeee:eeee:eeee:eeee:eeee", service.getLocalAddress());
	}

	@Test
	public void testProxy2_IPv4() throws IOException, InterruptedException {
		String proxy =
				// Preamble
				"0D0A0D0A000D0A515549540A" +
						// V2, PROXY
						"21" +
						// 0x1 : AF_INET    0x1 : STREAM.
						"11" +
						// Address length is 2*4 + 2*2 = 12 bytes.
						// length of remaining header (4+4+2+2 = 12)
						"000C" +
						// uint32_t src_addr; uint32_t dst_addr; uint16_t src_port; uint16_t dst_port;
						"C0A80001" + // 192.168.0.1
						"7f000001" + // 127.0.0.1
						"3039" + // 12345
						"1F90"; // 8080

		byte[] data = new byte[proxy.length() / 2];
		for (int i = 0; i < proxy.length(); i += 2) {
			data[i / 2] = (byte) ((Character.digit(proxy.charAt(i), 16) << 4)
					+ Character.digit(proxy.charAt(i+1), 16));
		}
		clientSocket.write(ByteBuffer.wrap(data));
		Thread.sleep(10);
		service.processSocketData();
		assertEquals("192.168.0.1", service.getRemoteAddress());
		assertEquals("127.0.0.1", service.getLocalAddress());
	}

	@Test
	public void testProxy2_IPv6() throws IOException, InterruptedException {
		String proxy =
				// Preamble
				"0D0A0D0A000D0A515549540A" +
						// V2, PROXY
						"21" +
						// 0x1 : AF_INET6    0x1 : STREAM.
						"21" +
						// Address length is 2*16 + 2*2 = 36 bytes.
						// length of remaining header (16+16+2+2 = 36)
						"0024" +
						// uint8_t src_addr[16]; uint8_t  dst_addr[16]; uint16_t src_port; uint16_t dst_port;
						"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + // ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
						"EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" + // eeee:eeee:eeee:eeee:eeee:eeee:eeee:eeee
						"3039" + // 12345
						"1F90"; // 8080

		byte[] data = new byte[proxy.length() / 2];
		for (int i = 0; i < proxy.length(); i += 2) {
			data[i / 2] = (byte) ((Character.digit(proxy.charAt(i), 16) << 4)
					+ Character.digit(proxy.charAt(i+1), 16));
		}
		clientSocket.write(ByteBuffer.wrap(data));
		Thread.sleep(10);
		service.processSocketData();
		assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", service.getRemoteAddress());
		assertEquals("eeee:eeee:eeee:eeee:eeee:eeee:eeee:eeee", service.getLocalAddress());
	}
}
