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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * $ ./scripts/tigase.sh start etc/tigase.conf
 *
 * OR
 *
 * $ /usr/local/sbin/ejabberdctl live
 */
@Ignore
public class S2SConnManTest
		extends S2SConnManAbstractTest {

	@BeforeClass
	public static void setup() {
		S2SConnManAbstractTest.setup();
		setupCID();
	}

	private static void setupCID() {
		String localHostname = localHostname();
		String remoteHostname = System.getProperty("test-remote-hostname", "wojtek-local.tigase.eu");
// //		 https://projects.tigase.net/issue/systems-54
//			remoteHostname = "convorb.im";
//			remoteHostname = "404.city";
//			remoteHostname = "tigase.org";
		setupCID(localHostname, remoteHostname);
	}

	private static String localHostname() {
		return System.getProperty("test-local-hostname", "tigase.im");
	}

	@Test
	public void testS2STigase_defaults() {
		testS2STigaseConnectionManager(localHostname(), null);
	}

	@Test
	public void testS2STigase_default_w_TLS13_only() {
		testS2STigaseConnectionManager(localHostname(), new String[]{"TLSv1.3"});
	}

	@Test
	public void testS2STigase_default_w_TLS13_w_SSLv2Hello() {
		testS2STigaseConnectionManager(localHostname(), new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv2Hello"});
	}

	@Test
	public void testS2STigase_default_w_TLS13_wo_SSLv2Hello() {
		testS2STigaseConnectionManager(localHostname(), new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"});
	}

	@Test
	public void testS2STigase_default_wo_TLS13_wo_SSLv2Hello() {
		testS2STigaseConnectionManager(localHostname(), new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
	}

	@Test
	public void testS2STigase_default_wo_TLS13_w_SSLv2Hello() {
		// "SSLv2Hello" failing?! for constricted openssl cipher list [2]
		testS2STigaseConnectionManager(localHostname(), new String[]{"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv2Hello"});
	}

}
