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
package tigase.vhosts;

import org.junit.*;
import tigase.TestLogger;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.LoggingBean;
import tigase.db.TigaseDBException;
import tigase.db.xml.XMLRepository;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.util.dns.DNSEntry;
import tigase.util.dns.DNSResolverDefault;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.dns.DNSResolverIfc;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static tigase.vhosts.VHostItemImpl.*;

public class VHostJDBCRepositoryTest {

	final static String mainVHost = "domain.com";
	final static String defaultConfigDomainName = "default";
	protected static Kernel kernel;
	static Logger log;
	static XMLRepository repository;
	static TestVHostJDBCRepository vHostJDBCRepository;

	@AfterClass
	public static void resetDNSResolver() {
		DNSResolverFactory.setDnsResolverClassName(DNSResolverDefault.class.getCanonicalName());
	}

	@BeforeClass
	public static void setup() {

		DNSResolverFactory.setDnsResolverClassName(PassThroughDNSResolver.class.getName());

		log = TestLogger.getLogger(VHostJDBCRepositoryTest.class);
		TestLogger.configureLogger(log, Level.ALL);

		TestLogger.configureLogger(Logger.getLogger("tigase"), Level.OFF);

		Map<String, Object> props = new HashMap<>();
		props.put("name", "VHost");
		props.put("default-virtual-host", mainVHost);

		kernel = new Kernel();
		kernel.setName("VHost");
		kernel.setForceAllowNull(true);
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		final DSLBeanConfiguratorWithBackwardCompatibility config = kernel.getInstance(
				DSLBeanConfiguratorWithBackwardCompatibility.class);
		config.setProperties(props);
		kernel.registerBean("vHostJDBCRepository")
				.asClass(TestVHostJDBCRepository.class)
				.exportable()
				.setActive(true)
				.exec();
		kernel.registerBean(VHostItemExtensionManager.class).exportable().setActive(true).exec();
		kernel.registerBean(VHostItemDefaults.class).exportable().setActive(true).exec();
		kernel.registerBean("repo").asClass(XMLRepository.class).exportable().setActive(true).exec();
		kernel.registerBean("logging").asClass(LoggingBean.class).setActive(true).setPinned(true).exec();

		try {
			final LoggingBean loggingBean = kernel.getInstance(LoggingBean.class);
			loggingBean.setPacketFullDebug(true);
			final VHostItemExtensionManager extensionManager = kernel.getInstance(VHostItemExtensionManager.class);
			extensionManager.setProviders(new VHostItemExtensionProvider[]{new TestVHostExtensionProvider()});
			repository = kernel.getInstance(XMLRepository.class);
			repository.initRepository("memory://", new ConcurrentHashMap<>());
			vHostJDBCRepository = kernel.getInstance(TestVHostJDBCRepository.class);
		} catch (Exception ex) {
			ex.printStackTrace();
			log.log(Level.WARNING, ex, () -> "There was an error setting up test");
		}

//		TestLogger.configureLogger(Logger.getLogger("tigase"), Level.FINE);
	}

	@After
	public void cleanup() {
		vHostJDBCRepository.removeItem(defaultConfigDomainName);
	}

	@Before
	public void setupDefault() {
		final VHostItem instance = vHostJDBCRepository.getItemInstance();
		instance.setKey(defaultConfigDomainName);
		vHostJDBCRepository.addItem(instance);
	}

	@Test
	public void testDomainNameCases() throws TigaseStringprepException {
		String domain = UUID.randomUUID().toString();
		VHostItem vHostItem = new VHostItemImpl(domain);
		vHostJDBCRepository.addItem(vHostItem);
		assertEquals(vHostItem, vHostJDBCRepository.getItem(domain.toUpperCase()));
	}

	@Test
	public void testMinimalItemFromCommand() throws TigaseDBException, TigaseStringprepException {
		String domain = UUID.randomUUID().toString();
		Element x = Command.createIqCommand(JID.jidInstanceNS("test@domain.com"), null, StanzaType.set,
											UUID.randomUUID().toString(), "x", Command.DataType.submit);
		Packet packet = Packet.packetInstance(x);
		Command.addFieldValue(packet, HOSTNAME_LABEL, domain);
		Command.addCheckBoxField(packet, ENABLED_LABEL, true);
		packet.initVars();

		final VHostItem domainItemInstance = vHostJDBCRepository.getItemInstance();
		domainItemInstance.initFromCommand(packet);

		final String validateItem = vHostJDBCRepository.validateItem(domainItemInstance);
		System.out.println(validateItem);
		assertNull(validateItem);
	}

	@Test
	public void testItemLoading() throws TigaseDBException, TigaseStringprepException {
		String domain = UUID.randomUUID().toString();
		final VHostItem domainItemInstance = vHostJDBCRepository.getItemInstance();
		domainItemInstance.setKey(domain);
		vHostJDBCRepository.addItem(domainItemInstance);
		Optional<Element> defaultVHostElement = getVHostElementFromRepository(defaultConfigDomainName);
		assertTrue(defaultVHostElement.isPresent());
		assertNull(defaultVHostElement.get().getChildren());
		Optional<Element> domainVHostElement = getVHostElementFromRepository(domain);
		assertTrue(domainVHostElement.isPresent());
		assertNull(domainVHostElement.get().getChildren());

		log.fine("By default extension is enabled for both domain and as a default");
		verifyDomainStateInRepository(domain, true);
		verifyDomainStateInStore(domain, true);
		verifyDomainStateInRepository(defaultConfigDomainName, true);
		verifyDomainStateInStore(defaultConfigDomainName, true);
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("let's disable extension");
		setExtensionStateForDomain(domain, false);

		log.fine("extension is still enabled in 'default' it should be true here (effective value in Wrapper)");
		verifyDomainStateInRepository(domain, true);
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("...but in repository we store current, correct domain setting");
		verifyDomainStateInStore(domain, false);

		log.fine("let's disable extension for default domain");
		setExtensionStateForDomain(defaultConfigDomainName, false);

		log.fine("it should now be disabled everywhere");
		verifyDomainStateInRepository(defaultConfigDomainName, false);
		verifyDomainStateInStore(defaultConfigDomainName, false);
		verifyDomainStateInRepository(domain, false);
		verifyDomainStateInStore(domain, false);
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("let's enabled it again for domain");
		setExtensionStateForDomain(domain, true);

		log.fine("it should be enabled for domain (including repository)");
		verifyDomainStateInRepository(domain, true);
		verifyDomainStateInStore(domain, true);
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("...but still disabled for default");
		verifyDomainStateInRepository(defaultConfigDomainName, false);
		verifyDomainStateInStore(defaultConfigDomainName, false);
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

	}

	@Test
	public void testTLSSettings() throws TigaseDBException, TigaseStringprepException {
		String domain = UUID.randomUUID().toString();
		final VHostItem domainItemInstance = vHostJDBCRepository.getItemInstance();
		domainItemInstance.setKey(domain);
		vHostJDBCRepository.addItem(domainItemInstance);
		Optional<Element> defaultVHostElement = getVHostElementFromRepository(defaultConfigDomainName);
		assertTrue(defaultVHostElement.isPresent());
		assertNull(defaultVHostElement.get().getChildren());
		Optional<Element> domainVHostElement = getVHostElementFromRepository(domain);
		assertTrue(domainVHostElement.isPresent());
		assertNull(domainVHostElement.get().getChildren());

		log.fine("By default TLS is enabled for both domain and as a default");
		assertTrue(vHostJDBCRepository.getItem(defaultConfigDomainName).isTlsRequired());
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		setTLSRequiredStateForDomain(domain, false);

		log.fine("given, that TLS is still enabled in 'default' it should be true here (effective value in Wrapper)");
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("let's disable TLS for default domain");
		setTLSRequiredStateForDomain(defaultConfigDomainName, false);

		log.fine("it should now be disabled everywhere");
		assertFalse(vHostJDBCRepository.getItem(defaultConfigDomainName).isTlsRequired());
		assertFalse(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("let's enabled it again for domain");
		setTLSRequiredStateForDomain(domain, true);

		log.fine("it should be enabled for domain");
		assertTrue(vHostJDBCRepository.getItem(domain).isTlsRequired());

		log.fine("...but still disabled for default");
		assertFalse(vHostJDBCRepository.getItem(defaultConfigDomainName).isTlsRequired());
	}

	Optional<Element> getVHostElementFromRepository(String domain) throws TigaseDBException {
		assertNotNull(domain);
		String items_list = repository.getData(vHostJDBCRepository.getRepoUser(),
											   vHostJDBCRepository.getItemsListPKey());
		if (!items_list.isEmpty()) {
			DomBuilderHandler domHandler = new DomBuilderHandler();
			SimpleParser parser = SingletonFactory.getParserInstance();

			parser.parse(domHandler, items_list);

			Queue<Element> elems = domHandler.getParsedElements();
			return elems.stream().filter(element -> domain.equals(element.getAttributeStaticStr("hostname"))).findAny();
		} else {
			return Optional.empty();
		}
	}

	private void verifyDomainStateInRepository(String domain, boolean state) throws TigaseDBException {
		final VHostItem item = vHostJDBCRepository.getItem(domain);
		TestVHostExtension extension = item.getExtension(TestVHostExtension.class);
		log.log(Level.FINE, "Verifying state: '" + state + "' for domain: '" + domain + "' with item: " + item +
				"; Testing extension: " + extension);
		assertNotNull(extension);
		assertEquals(state, extension.isEnabled());
		final String itemString = item.toString();
		if (state) {
			assertTrue(itemString.contains("TestVHostExtension(enabled: true)") ||
							   !itemString.contains("TestVHostExtension"));
		} else {
			assertTrue(itemString.contains("TestVHostExtension(enabled: false)"));
		}
	}

	private void verifyDomainStateInStore(String domain, boolean state) throws TigaseDBException {
		Optional<Element> domainVHostElement = getVHostElementFromRepository(domain);
		assertTrue(domainVHostElement.isPresent());
		Element testElementInVHost = domainVHostElement.get().getChild("test");
		log.log(Level.FINE, "Verifying in store state: " + state + " for domain: " + domain + " with element: " +
				domainVHostElement);
		if (state) {
			assertNull(testElementInVHost);
		} else {
			assertNotNull(testElementInVHost);
			assertEquals(String.valueOf(state), testElementInVHost.getAttributeStaticStr("enabled"));
		}
	}

	private VHostItem setExtensionStateForDomain(String domain, boolean state) throws TigaseStringprepException {
		log.log(Level.FINE, "Setting domain: '" + domain + "' extension state to: " + state);
		Element x = Command.createIqCommand(JID.jidInstanceNS("test@domain.com"), null, StanzaType.set,
											UUID.randomUUID().toString(), "x", Command.DataType.submit);
		Packet packet = Packet.packetInstance(x);
		Command.addFieldValue(packet, HOSTNAME_LABEL, domain);
		TestVHostExtension extension = new TestVHostExtension(state);
		extension.addCommandFields(extension.getId(), packet, false);
		packet.initVars();
		VHostItem vHostItem = vHostJDBCRepository.getItemInstance();
		vHostItem.initFromCommand(packet);
		vHostJDBCRepository.addItem(vHostItem);
		return vHostItem;
	}

	private VHostItem setTLSRequiredStateForDomain(String domain, boolean state) throws TigaseStringprepException {
		log.log(Level.FINE, "Setting domain: '" + domain + "' TLS-required state to: " + state);
		Element x = Command.createIqCommand(JID.jidInstanceNS("test@domain.com"), null, StanzaType.set,
											UUID.randomUUID().toString(), "x", Command.DataType.submit);
		Packet packet = Packet.packetInstance(x);
		Command.addFieldValue(packet, HOSTNAME_LABEL, domain);
		Command.addCheckBoxField(packet, TLS_REQUIRED_LABEL, state);
		packet.initVars();
		VHostItem vHostItem = vHostJDBCRepository.getItemInstance();
		vHostItem.initFromCommand(packet);
		vHostJDBCRepository.addItem(vHostItem);
		return vHostItem;
	}

	public static class PassThroughDNSResolver
			implements DNSResolverIfc {

		public PassThroughDNSResolver() {
		}

		@Override
		public DNSEntry[] getHostSRV_Entries(String hostname) throws UnknownHostException {
			return new DNSEntry[]{new DNSEntry(hostname, hostname),
								  new DNSEntry(defaultConfigDomainName, defaultConfigDomainName)};
		}

		@Override
		public String getDefaultHost() {
			return defaultConfigDomainName;
		}

		@Override
		public String[] getHostIPs(String s) throws UnknownHostException {
			return new String[]{s, defaultConfigDomainName};
		}
	}

	public static class TestVHostExtension
			extends AbstractVHostItemExtension<TestVHostExtension> {

		public static final String ID = "test";
		private final static boolean ENABLED_DEFAULT_VAL = true;
		private boolean enabled = true;

		public TestVHostExtension() {
		}

		public TestVHostExtension(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return enabled;
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public void initFromElement(Element item) {
			final String enabledAttr = item.getAttributeStaticStr("enabled");
			this.enabled = enabledAttr == null ? ENABLED_DEFAULT_VAL : Boolean.parseBoolean(enabledAttr);
		}

		@Override
		public void initFromCommand(String prefix, Packet packet) throws IllegalArgumentException {
			Optional.ofNullable(Command.getFieldValue(packet, prefix + "-enabled"))
					.ifPresent(s -> enabled = Boolean.parseBoolean(s));
		}

		@Override
		public String toDebugString() {
			return "enabled: " + enabled;
		}

		@Override
		public Element toElement() {
			return enabled == ENABLED_DEFAULT_VAL
				   ? null
				   : new Element(ID, new String[]{"enabled"}, new String[]{String.valueOf(enabled)});
		}

		@Override
		public void addCommandFields(String prefix, Packet packet, boolean forDefault) {
			Element commandEl = packet.getElemChild(Command.COMMAND_EL, Command.XMLNS);
			DataForm.addFieldValue(commandEl, prefix + "-enabled", String.valueOf(enabled), "boolean",
								   "Extension Enabled");
		}

		@Override
		public TestVHostExtension mergeWithDefaults(TestVHostExtension defaults) {
			return new TestVHostExtension(this.enabled || defaults.enabled);
		}
	}

	public static class TestVHostExtensionProvider
			implements VHostItemExtensionProvider<TestVHostExtension> {

		@Override
		public String getId() {
			return TestVHostExtension.ID;
		}

		@Override
		public Class<TestVHostExtension> getExtensionClazz() {
			return TestVHostExtension.class;
		}
	}

	public static class TestVHostJDBCRepository
			extends VHostJDBCRepository {

		public TestVHostJDBCRepository() {
		}

		@Override
		public void setAutoloadTimer(long delay) {
			super.setAutoloadTimer(0);
		}

		@Override
		public void setAutoReloadInterval(long autoLoadInterval) {
			super.setAutoReloadInterval(0);
		}

		void reinitialiseRepository() {
			items.clear();
		}

	}
}
