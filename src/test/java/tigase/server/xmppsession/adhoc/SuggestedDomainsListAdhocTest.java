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
package tigase.server.xmppsession.adhoc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.eventbus.EventBusFactory;
import tigase.form.Form;
import tigase.io.CertificateContainer;
import tigase.io.SSLContextContainer;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.server.ConnectionManager;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.CIDConnections;
import tigase.server.xmppserver.KnownDomainsListProvider;
import tigase.server.xmppserver.S2SConnManAbstractTest;
import tigase.server.xmppserver.S2SConnManTest;
import tigase.server.xmppserver.S2SIOService;
import tigase.server.xmppserver.S2SRandomSelector;
import tigase.server.xmppserver.proc.AuthenticatorSelectorManager;
import tigase.vhosts.DefaultAwareVHostManagerIfc;
import tigase.vhosts.DummyVHostManager;
import tigase.vhosts.VHostManagerIfc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SuggestedDomainsListAdhocTest {

	public static Kernel kernel;
	private static AuthenticatorSelectorManager authenticatorSelectorManager;
	private static CID cid;
	private static DummyVHostManager dummyVHostManager;
	private static S2SConnManTest.S2SConnectionHandlerImpl handler = null;
	private static SuggestedDomainsListAdhoc suggestedDomainsListAdhoc;

	protected static void addCID(String localHostname, String remoteHostname) {
		var cid = new CID(localHostname, remoteHostname);
		var fastCIDConnections = new S2SConnManAbstractTest.fastCIDConnections(cid, handler);
		authenticatorSelectorManager.authenticateConnection(new S2SIOService(), fastCIDConnections, cid);

		if (dummyVHostManager.getVHostItem(localHostname) == null) {
			dummyVHostManager.addVhost(localHostname);
		}
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
		kernel.registerBean(KnownDomainsListProvider.class).exportable().setActive(true).exec();
		kernel.registerBean(AuthenticatorSelectorManager.class).exportable().setActive(true).exec();
		kernel.registerBean("vHostManager").asClass(DummyVHostManager.class).exportable().setActive(true).exec();
		kernel.registerBean(SSLContextContainer.class).exportable().setActive(true).exec();
		kernel.registerBean("service").asClass(S2SConnManTest.S2SConnectionHandlerImpl.class).setActive(true).exec();
		kernel.registerBean(SuggestedDomainsListAdhoc.class).setActive(true).exec();
		return config;
	}

	@Test
	public void prepareForm() {
		final Form form = suggestedDomainsListAdhoc.prepareForm();
		var knownRemoteDomains = Arrays.asList(form.getAsStrings("knownRemoteDomains"));
		var localDomains = Arrays.asList(form.getAsStrings("localDomains"));

		Assert.assertTrue(knownRemoteDomains.contains("tigase.org"));
		Assert.assertTrue(knownRemoteDomains.contains("tigase.net"));
		Assert.assertTrue(knownRemoteDomains.contains("tigase.im"));
		Assert.assertTrue(knownRemoteDomains.contains("sure.im"));
		Assert.assertTrue(knownRemoteDomains.contains("jabber.today"));

		Assert.assertTrue(localDomains.contains("my-other-local-domain.org"));
		Assert.assertTrue(localDomains.contains("my-local-domain.net"));
		Assert.assertFalse(localDomains.contains("default"));
	}

	@Before
	public void setUp() throws Exception {
		prepareKernel();

		authenticatorSelectorManager = kernel.getInstance(AuthenticatorSelectorManager.class);
		dummyVHostManager = (DummyVHostManager) kernel.getInstance(DefaultAwareVHostManagerIfc.class);
		handler = kernel.getInstance(S2SConnManTest.S2SConnectionHandlerImpl.class);
		suggestedDomainsListAdhoc = kernel.getInstance(SuggestedDomainsListAdhoc.class);

		dummyVHostManager.addVhost("default");

		addCID("my-local-domain.net", "tigase.org");
		addCID("my-local-domain.net", "tigase.net");
		addCID("my-local-domain.net", "tigase.im");
		addCID("my-local-domain.net", "sure.im");
		addCID("my-other-local-domain.org", "tigase.org");
		addCID("my-other-local-domain.org", "tigase.net");
		addCID("my-other-local-domain.org", "tigase.im");
		addCID("my-other-local-domain.org", "jabber.today");
	}
}