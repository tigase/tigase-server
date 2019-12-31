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

package tigase.server.xmppserver;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Using Javas's SSLSocket class to connect to openssl made server
 *
 * [1] - default openss:
 * openssl s_server -key privkey.pem -cert cert.pem -CAfile fullchain.pem -accept 5269
 *
 * [2] - ejabberd recommended cipers
 * openssl s_server -key privkey.pem -cert cert.pem -CAfile fullchain.pem -accept 5269 -cipher "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256" -debug
 */
@Ignore
public class SSLSocketDirectTest
		extends SSLTestAbstract {

	static String hostname = "wojtek-local.tigase.eu";
	static int port = 5269;

	private static void reopenStream(SSLSocket socket) throws IOException, InterruptedException {
		InputStream is = new BufferedInputStream(socket.getInputStream());
		OutputStream os = new BufferedOutputStream(socket.getOutputStream());
		final PrintWriter writer = new PrintWriter(os, true);
		writer.println("<stream:stream" + " xmlns:stream='http://etherx.jabber.org/streams' " +
							   " xmlns:xml='http://www.w3.org/XML/1998/namespace' xmlns='jabber:client' " + " to='" +
							   hostname + "'" + " version='1.0'>");
//		final InputStreamReader reader = new InputStreamReader(is);
//		BufferedReader br = new BufferedReader(reader);
//		System.out.println(br.lines().collect(Collectors.joining()));
	}

	@BeforeClass
	public static void setup() {
		getSslDebugString().ifPresent(debug -> System.setProperty("javax.net.debug", debug));

		log = Logger.getLogger("tigase");
		configureLogger(log, Level.INFO);

		hostname = System.getProperty("test-hostname");
		final String portProperty = System.getProperty("test-port");
		try {
			port = Integer.parseInt(portProperty);
		} catch (NumberFormatException e) {
			log.log(Level.INFO, () -> "parsing portProperty: " + portProperty + " failed");
		}
	}

	public static void testSSLSocketConnection(String[] protocols) {

		try (final SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostname, port)) {
			if (protocols != null) {
				socket.setEnabledProtocols(protocols);
			}
			log.log(Level.INFO, () -> "Socket enabled protocols: " + Arrays.toString(socket.getEnabledProtocols()));
			socket.addHandshakeCompletedListener(event -> log.log(Level.INFO, "Connected using: " +
					event.getSession().getProtocol() + ", with cipher: " + event.getCipherSuite() +
					", enabled protocols: " + Arrays.toString(event.getSocket().getEnabledProtocols())));

			socket.startHandshake();
			Assert.assertTrue(socket.isConnected());
			reopenStream(socket);
		} catch (Exception e) {
			Assert.fail("exception: " + e);
		}
	}

	@Test
	public void testSSLSocketConnection_defaults() {
		testSSLSocketConnection(null);
	}

	@Test()
	public void testSSLSocketConnection_TLS13_only() {
		testSSLSocketConnection(new String[]{"TLSv1.3"});
	}

	@Test
	public void testSSLSocketConnection_default_w_TLS13_w_SSLv2Hello() {
		// "SSLv2Hello" not failing for constricted cipher list [2] when TLS1.3 is enabled?
		testSSLSocketConnection(new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv2Hello"});
	}

	@Test
	public void testSSLSocketConnection_default_w_TLS13_wo_SSLv2Hello() {
		testSSLSocketConnection(new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"});
	}

	@Test
	public void testSSLSocketConnection_default_wo_TLS13_w_SSLv2Hello() {
		// "SSLv2Hello" failing?! for constricted openssl cipher list [2] and with TLS1.3 disabled
		testSSLSocketConnection(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv2Hello"});
	}
}
