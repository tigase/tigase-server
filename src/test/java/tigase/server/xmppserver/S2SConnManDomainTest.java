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

import org.junit.*;
import tigase.TestLogger;
import tigase.cert.CertCheckResult;
import tigase.server.xmppserver.proc.AuthenticatorSelectorManager;
import tigase.stats.StatRecord;
import tigase.stats.StatisticsList;

import java.util.logging.Level;
import java.util.logging.Logger;

@Ignore
public class S2SConnManDomainTest
		extends S2SConnManAbstractTest {

	@AfterClass
	public static void postStats() {
		final AuthenticatorSelectorManager selector = kernel.getInstance(AuthenticatorSelectorManager.class);
		final StatisticsList list = new StatisticsList(Level.FINEST);
		selector.getStatistics("test", list);
		for (StatRecord statRecord : list) {
			log.log(Level.ALL, statRecord.toString());
		}
	}

	@BeforeClass
	public static void setup() {
		System.setProperty("test-ssl-debug", "false");
		S2SConnManAbstractTest.setup();
		TestLogger.configureLogger(log, Level.INFO);
		log = Logger.getLogger("tigase.server.xmppserver");
		TestLogger.configureLogger(log, Level.FINEST);
	}

	@Test
	public void testS2S_convorb_im() {
		setupCID("tigase.im", "convorb.im");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_404_city() {
		setupCID("tigase.im", "404.city");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_frsra_ml() {
		setupCID("tigase.im", "frsra.ml");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_jabber_ru() {
		setupCID("tigase.im", "jabber.ru");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_jabberix_com() {
		setupCID("tigase.im", "jabberix.com");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_jwchat_org() {
		setupCID("tigase.im", "jwchat.org");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_vrcshop_com() {
		setupCID("tigase.im", "vrcshop.com");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_axeos_nl() {
		setupCID("tigase.im", "axeos.nl");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_tigase_org() {
		setupCID("tigase.im", "tigase.org");
		testS2STigaseConnectionManager(null);
	}

	/**
	 * local tests
	 */
	@Test
	@Ignore
	public void testS2S_puddlejumper_atlantiscity() {
		setupCID("puddlejumper", "atlantiscity");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_jit_si() {
		setupCID("tigase.im", "jit.si");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_jabber_org() {
		setupCID("tigase.im", "jabber.org");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_cluxia_eu() {
		setupCID("tigase.im", "cluxia.eu");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_pouet_ovh() {
		setupCID("tigase.im", "pouet.ovh");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_upload_pouet_ovh() {
		setupCID("tigase.im", "upload.pouet.ovh");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_rsocks_net() {
		// can't connect from 404.im, certificate not trusted ; jabster.pl: ejabberd 18.12.1
		setupCID("tigase.im", "rsocks.net");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_dismail_de() {
		// ejabberd 19.09.57
		setupCID("tigase.im", "dismail.de");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_legalize_li() {
		// can't connect from 404.im, certificate not trusted ; jabster.pl: ejabberd 18.12.1
		setupCID("tigase.im", "legalize.li");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_legaliza_live() {
		// invalid namespace?! / Openfire 4.4.2
		setupCID("tigase.im", "legaliza.live");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_jabber_cz() {
		// invalid namespace?! / Openfire 4.4.2
		setupCID("tigase.im", "jabber.cz");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_xmpp_jp() {
		// invalid namespace?! / Openfire 4.4.2
		setupCID("tigase.im", "xmpp.jp");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_xmpp_uwpx_org() {
		// can't connect from 404.im, certificate not trusted ; jabster.pl: ejabberd, 19.09.1
		setupCID("tigase.im", "xmpp.uwpx.org");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_messaging_one() {
		setupCID("tigase.im", "messaging.one");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_conference_process_one_net() {
		setupCID("tigase.im", "conference.process-one.net");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_shreddox_eu() {
		setupCID("tigase.im", "shreddox.eu");
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2S_xabber_org() {
		setupCID("tigase.im", "xabber.org");
		testS2STigaseConnectionManager(null);
	}

	@Test
	@Ignore
	public void testS2S_expired_badxmpp_eu() {
		setupCID("tigase.im", "expired.badxmpp.eu");
		testS2STigaseConnectionManager(null,
									   certCheckResult -> Assert.assertEquals(CertCheckResult.expired, certCheckResult),
									   Assert::assertFalse);
	}
}
