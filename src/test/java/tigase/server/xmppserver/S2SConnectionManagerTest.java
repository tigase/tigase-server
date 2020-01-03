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
import tigase.cert.CertCheckResult;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.eventbus.EventBusFactory;
import tigase.io.CertificateContainer;
import tigase.io.SSLContextContainer;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.server.ConnectionManager;
import tigase.server.xmppserver.proc.Dialback;
import tigase.server.xmppserver.proc.StartTLS;
import tigase.util.dns.DNSEntry;
import tigase.util.dns.DNSResolverFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.net.IOService.CERT_CHECK_RESULT;

/**
 * $ ./scripts/tigase.sh start etc/tigase.conf
 *
 * OR
 *
 * $ /usr/local/sbin/ejabberdctl live
 */
@Ignore
public class S2SConnectionManagerTest
		extends SSLTestAbstract {

	// TODO: done

	private static CID cid;
	private static S2SConnectionHandlerImpl handler = null;
	private static Kernel kernel;

	private static void dumpConfiguration(DSLBeanConfiguratorWithBackwardCompatibility config) throws IOException {
		final StringWriter stringWriter = new StringWriter();

		config.dumpConfiguration(stringWriter);
		System.out.println("Configuration dump:" + stringWriter.toString());
	}

	private static DSLBeanConfiguratorWithBackwardCompatibility prepareKernel() {
		Map<String, Object> props = new HashMap<>();
		props.put("name", "s2s");

		kernel = new Kernel();
		kernel.setName("s2s");
		kernel.setForceAllowNull(true);
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		final DSLBeanConfiguratorWithBackwardCompatibility config = kernel.getInstance(
				DSLBeanConfiguratorWithBackwardCompatibility.class);
		config.setProperties(props);
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean(ConnectionManager.PortsConfigBean.class).exec();
		kernel.registerBean(CIDConnections.CIDConnectionsOpenerService.class).exportable().exec();
		kernel.registerBean(S2SRandomSelector.class).exportable().exec();
		kernel.registerBean(CertificateContainer.class).exportable().exec();
		kernel.registerBean(StartTLS.class).exportable().exec();
		kernel.registerBean(Dialback.class).exportable().setActive(false).exec();
//		kernel.registerBean(SaslExternal.class).setActive(false).exec();
		kernel.registerBean(SSLContextContainer.class).exportable().setActive(true).exec();
		kernel.registerBean("service").asClass(S2SConnectionHandlerImpl.class).setActive(true).exec();
		return config;
	}

	@BeforeClass
	public static void setup() {
		getSslDebugString().ifPresent(debug -> System.setProperty("javax.net.debug", debug));

		log = Logger.getLogger("tigase");
		configureLogger(log, Level.CONFIG);

		final DSLBeanConfiguratorWithBackwardCompatibility config = prepareKernel();

		try {
			final SSLContextContainer context = kernel.getInstance(SSLContextContainer.class);
			handler = kernel.getInstance(S2SConnectionHandlerImpl.class);
			handler.start();
			dumpConfiguration(config);
		} catch (Exception ex) {
			log.log(Level.WARNING, ex, () -> "There was an error setting up test");
		}

		setupCID();

		configureLogger(log, Level.ALL);
	}

	private static void setupCID() {
		String localHostname = System.getProperty("test-local-hostname", "tigase.im");
		String remoteHostname = System.getProperty("test-remote-hostname", "wojtek-local.tigase.eu");
// //		 https://projects.tigase.net/issue/systems-54
//			remoteHostname = "convorb.im";
//			remoteHostname = "404.city";
//			remoteHostname = "frsra.ml";
//			remoteHostname = "jabber.ru";
//			remoteHostname = "jabberix.com";
//			remoteHostname = "jwchat.org";
//			remoteHostname = "vrcshop.com";
//			remoteHostname = "axeos.nl";
		cid = new CID(localHostname, remoteHostname);
	}

	private static void setupSslContextContainer(final SSLContextContainer context,
												 SSLContextContainer.HARDENED_MODE mode, final String[] protocols,
												 final String[] ciphers) {
		if (mode != null) {
			context.setHardenedMode(mode);
		}
		context.setEnabledProtocols(protocols);
		context.setEnabledCiphers(ciphers);
	}

	@Test
	public void testS2STigase_defaults() {
		testS2STigaseConnectionManager(null);
	}

	@Test
	public void testS2STigase_default_w_TLS13_only() {
		testS2STigaseConnectionManager(new String[]{"TLSv1.3"});
	}

	@Test
	public void testS2STigase_default_w_TLS13_w_SSLv2Hello() {
		testS2STigaseConnectionManager(new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv2Hello"});
	}

	@Test
	public void testS2STigase_default_w_TLS13_wo_SSLv2Hello() {
		testS2STigaseConnectionManager(new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"});
	}

	@Test
	public void testS2STigase_default_wo_TLS13_wo_SSLv2Hello() {
		testS2STigaseConnectionManager(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
	}

	@Test
	public void testS2STigase_default_wo_TLS13_w_SSLv2Hello() {
		// "SSLv2Hello" failing?! for constricted openssl cipher list [2]
		testS2STigaseConnectionManager(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv2Hello"});
	}

	private void testS2STigaseConnectionManager(String[] protocols) {
		try {
			final SSLContextContainer context = kernel.getInstance(SSLContextContainer.class);
			setupSslContextContainer(context, SSLContextContainer.HARDENED_MODE.secure, protocols, null);
			testConnectionForCID(cid);
		} catch (Exception e) {
			Assert.fail("exception: " + e);
		}
	}

	private void testConnectionForCID(CID cid) throws NotLocalhostException, LocalhostException, InterruptedException {
		final fastCIDConnections connections = handler.createNewCIDConnections(cid);
		connections.openConnections();

		S2SIOService s2SIOService = null;
		int delayRetryLimit = 25;
		boolean connected = false;
		boolean authenticated = false;
		CertCheckResult trusted = CertCheckResult.none;
		do {
			TimeUnit.MILLISECONDS.sleep(100);
			s2SIOService = connections.getS2SIOService();
			if (s2SIOService != null) {
				connected = s2SIOService.isConnected();
				authenticated = s2SIOService.isAuthenticated();
				trusted = (CertCheckResult) s2SIOService.getSessionData().get(CERT_CHECK_RESULT);
			}
		} while ((s2SIOService == null || !connected || !authenticated || !CertCheckResult.trusted.equals(trusted)) &&
				delayRetryLimit-- > 0);
		log.log(Level.INFO, "handler.getService().isConnected(): " + connected);
		log.log(Level.INFO, "handler.getService().isAuthenticated(): " + authenticated);
		log.log(Level.INFO, "handler.getService().getSessionData().get(CERT_CHECK_RESULT): " + trusted);
		Assert.assertTrue(connected);
		Assert.assertTrue(CertCheckResult.trusted.equals(trusted));
		Assert.assertTrue(authenticated);

		handler.serviceStopped(s2SIOService);
	}

	public static class S2SConnectionHandlerImpl
			extends S2SConnectionManager {

		public S2SConnectionHandlerImpl() {
			connectionDelay = 0;
		}

		@Override
		public HashSet<Integer> getDefPorts() {
			return new HashSet<>();
		}

		@Override
		public fastCIDConnections createNewCIDConnections(CID cid) throws NotLocalhostException, LocalhostException {
			fastCIDConnections conns = new fastCIDConnections(cid, this);
			cidConnections.put(cid, conns);
			return conns;
		}

		@Override
		protected void addWaitingTask(Map<String, Object> conn) {
			reconnectService(conn, connectionDelay);
		}

		private void reconnectService(final Map<String, Object> port_props, long delay) {
			if (log.isLoggable(Level.FINER)) {
				String cid = "" + port_props.get("local-hostname") + "@" + port_props.get("remote-hostname");

				log.log(Level.FINER,
						"Reconnecting service for: {0}, scheduling next try in {1} seconds, cid: {2}, props: {3}",
						new Object[]{getName(), delay / 1000, cid, port_props});
			}
			String host = (String) port_props.get(PORT_REMOTE_HOST_PROP_KEY);

			if (host == null) {
				host = (String) port_props.get("remote-hostname");
			}

			int port = (Integer) port_props.get(PORT_KEY);

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"Reconnecting service for component: {0}, to remote host: {1} on port: {2,number,#}",
						new Object[]{getName(), host, port});
			}
			startService(port_props);
		}
	}

	private static class fastCIDConnections
			extends CIDConnections {

		private final CID cid;
		S2SConnection s2s_conn = null;

		public fastCIDConnections(CID cid, S2SConnectionHandlerIfc<S2SIOService> handler) {
			super(cid, handler, new S2SRandomSelector(), 1, 1, 1, 0);
			this.cid = cid;
		}

		public S2SIOService getS2SIOService() {
			return s2s_conn.getS2SIOService();
		}

		public void openConnections() {
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Checking DNS for host: {0} for: {1}",
							new Object[]{cid.getRemoteHost(), cid});
				}
				final String serverName = cid.getRemoteHost();
				DNSEntry[] dns_entries = DNSResolverFactory.getInstance().getHostSRV_Entries(serverName);
				if (dns_entries != null && dns_entries.length > 0) {
					final DNSEntry dns_entry = dns_entries[0];
					final String ip = dns_entry.getIp();
					s2s_conn = new S2SConnection(handler, ip);
					Map<String, Object> port_props = new TreeMap<>();
					port_props.put(S2SIOService.CERT_REQUIRED_DOMAIN, serverName);
					initNewConnection(ip, dns_entry.getPort(), s2s_conn, port_props);
				}
			} catch (UnknownHostException ex) {
				log.log(Level.INFO, "Remote host not found: " + cid.getRemoteHost() + ", for: " + cid, ex);
			}
		}
	}
}