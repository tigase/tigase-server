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
import tigase.cert.CertCheckResult;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.LoggingBean;
import tigase.eventbus.EventBusFactory;
import tigase.io.CertificateContainer;
import tigase.io.SSLContextContainer;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.server.ConnectionManager;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.server.xmppserver.proc.StartTLS;
import tigase.util.dns.DNSEntry;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostItemImpl;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static tigase.net.IOService.CERT_CHECK_RESULT;

class S2SConnManAbstractTest
		extends SSLTestAbstract {

	private static CID cid;
	private static S2SConnManTest.S2SConnectionHandlerImpl handler = null;
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
//		kernel.registerBean(Dialback.class).exportable().setActive(false).exec();
//		kernel.registerBean(SaslExternal.class).setActive(false).exec();
		kernel.registerBean("vHostManager")
				.asClass(S2SConnManTest.DummyVHostManager.class)
				.exportable()
				.setActive(true)
				.exec();
		kernel.registerBean(SSLContextContainer.class).exportable().setActive(true).exec();
		kernel.registerBean("service").asClass(S2SConnManTest.S2SConnectionHandlerImpl.class).setActive(true).exec();
		kernel.registerBean("logging").asClass(LoggingBean.class).setActive(true).setPinned(true).exec();
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
			final LoggingBean loggingBean = kernel.getInstance(LoggingBean.class);
			loggingBean.setPacketFullDebug(true);
			handler = kernel.getInstance(S2SConnManTest.S2SConnectionHandlerImpl.class);
			handler.start();
			dumpConfiguration(config);
		} catch (Exception ex) {
			log.log(Level.WARNING, ex, () -> "There was an error setting up test");
		}

		configureLogger(log, Level.ALL);
	}

	protected static void setupCID(String localHostname, String remoteHostname) {
		cid = new CID(localHostname, remoteHostname);
		final S2SConnManTest.DummyVHostManager instance = (S2SConnManTest.DummyVHostManager) kernel.getInstance(
				VHostManagerIfc.class);
		if (instance.getVHostItem(localHostname) == null) {
			instance.addVhost(localHostname);
		}
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

	protected void testS2STigaseConnectionManager(String[] protocols) {
		testS2STigaseConnectionManager(protocols,
									   certCheckResult -> Assert.assertEquals(CertCheckResult.trusted, certCheckResult),
									   Assert::assertTrue);
	}

	protected void testS2STigaseConnectionManager(String[] protocols, Consumer<CertCheckResult> certificateCheckResult, Consumer<Boolean> authenticatedConsumer) {
		try {
			final SSLContextContainer context = kernel.getInstance(SSLContextContainer.class);
			setupSslContextContainer(context, SSLContextContainer.HARDENED_MODE.secure, protocols, null);
			testConnectionForCID(cid, certificateCheckResult, authenticatedConsumer);
		} catch (Exception e) {
			log.log(Level.FINE, "Error running test", e);
			Assert.fail("exception: " + e);
		}
	}

	private void testConnectionForCID(CID cid, Consumer<CertCheckResult> certificateCheckResult, Consumer<Boolean> authenticatedConsumer) throws NotLocalhostException, LocalhostException, InterruptedException {
		final fastCIDConnections connections = handler.createNewCIDConnections(cid);
		connections.openConnections();

		S2SIOService s2SIOService = null;
		int delayRetryLimit = 30;
		boolean connected = false;
		boolean authenticated = false;
		CertCheckResult trusted = CertCheckResult.none;
		do {
			TimeUnit.MILLISECONDS.sleep(250);
			s2SIOService = connections.getS2SIOService();
			if (s2SIOService != null) {
				connected = s2SIOService.isConnected();
				authenticated = s2SIOService.isAuthenticated();
				trusted = (CertCheckResult) s2SIOService.getSessionData().get(CERT_CHECK_RESULT);
			}
		} while ((s2SIOService == null || !connected || !authenticated || !CertCheckResult.trusted.equals(trusted)) &&
				delayRetryLimit-- > 0);
		log.log(Level.INFO, cid + ": isConnected(): " + connected);
		log.log(Level.INFO, cid + ": isAuthenticated(): " + authenticated);
		log.log(Level.INFO, cid + ": getSessionData().get(CERT_CHECK_RESULT): " + trusted);
		Assert.assertTrue(connected);
		certificateCheckResult.accept(trusted);

		// it will fail when testing locally as it's not possible to perform dialback that way without mapping domain
		// domain to local machine
		authenticatedConsumer.accept(authenticated);

		try {
			final Packet packet = Iq.packetInstance("iq", cid.getLocalHost(), cid.getRemoteHost(), StanzaType.get);
			final Element iqElement = packet.getElement();
			iqElement.setAttribute("id", UUID.randomUUID().toString());
			final Element query = new Element("query");
			query.setXMLNS("jabber:iq:version");
			iqElement.addChild(query);

			handler.addPacketNB(packet);
		} catch (Exception e) {

		}

//		TimeUnit.SECONDS.sleep(15);

		handler.serviceStopped(s2SIOService);
	}

	/**
	 * Dummy {@code VHostManagerIfc} implementation, mostly to avoid exceptions in Dialback processor
	 */
	public static class DummyVHostManager
			implements VHostManagerIfc {

		Map<String, VHostItem> items = new ConcurrentHashMap<>();

		public DummyVHostManager() {
		}

		public void addVhost(String vhost) {

			try {
				VHostItem item = new VHostItemImpl(vhost);
				items.put(vhost, item);
			} catch (TigaseStringprepException e) {
				log.log(Level.WARNING, "Adding VHost failed", e);
			}
		}

		@Override
		public boolean isLocalDomain(String domain) {
			return false;
		}

		@Override
		public boolean isLocalDomainOrComponent(String domain) {
			return false;
		}

		@Override
		public boolean isAnonymousEnabled(String domain) {
			return false;
		}

		@Override
		public ServerComponent[] getComponentsForLocalDomain(String domain) {
			return new ServerComponent[0];
		}

		@Override
		public ServerComponent[] getComponentsForNonLocalDomain(String domain) {
			return new ServerComponent[0];
		}

		@Override
		public VHostItem getVHostItem(String domain) {
			return items.get(domain);
		}

		@Override
		public VHostItem getVHostItemDomainOrComponent(String domain) {
			return items.get(domain);
		}

		@Override
		public void addComponentDomain(String domain) {

		}

		@Override
		public void removeComponentDomain(String domain) {

		}

		@Override
		public BareJID getDefVHostItem() {
			return items.values()
					.stream()
					.map(VHostItem::getVhost)
					.map(JID::toString)
					.map(BareJID::bareJIDInstanceNS)
					.findFirst()
					.orElse(BareJID.bareJIDInstanceNS("not@available"));
		}

		@Override
		public List<JID> getAllVHosts() {
			return items.values().stream().map(VHostItem::getVhost).collect(Collectors.toList());
		}
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
		public Queue<Packet> processSocketData(S2SIOService serv) {
			final Queue<Packet> receivedPackets = serv.getReceivedPackets();
			if (receivedPackets != null) {
				receivedPackets.forEach(x -> log.log(Level.INFO, "Received packet: " + x));
			}
			return super.processSocketData(serv);
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
				log.log(Level.FINE, "Remote host not found: " + cid.getRemoteHost() + ", for: " + cid, ex);
				Assert.fail("Remote host not found: " + cid.getRemoteHost() + ", for: " + cid);
			}
		}
	}
}
