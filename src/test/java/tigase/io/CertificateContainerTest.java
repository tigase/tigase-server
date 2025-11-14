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
package tigase.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tigase.cert.CertificateEntry;
import tigase.cert.CertificateGenerator;
import tigase.cert.CertificateGeneratorFactory;
import tigase.cert.CertificateUtil;
import tigase.eventbus.EventBusFactory;
import tigase.io.repo.CertificateItem;
import tigase.io.repo.CertificateRepository;
import tigase.kernel.AbstractKernelWithUserRepositoryTestCase;
import tigase.kernel.core.Kernel;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.cert.CertificateUtil.createKeyPair;
import static tigase.cert.CertificateUtil.exportToPemFormat;
import static tigase.io.CertificateContainerIfc.CertificateEntity;

public class CertificateContainerTest
		extends AbstractKernelWithUserRepositoryTestCase {

	private static final Logger LOGGER = Logger.getLogger(CertificateContainerTest.class.getName());
	private final String domain = "example.com";
	private final String mucDomain = "muc." + domain;
	private final String wildcardDomain = "*." + domain;

	private CertificateContainer certificateContainer;
	private SSLContextContainer sslContextContainer;

	@Test
	public void testRegularDomainForExistingCertificate() throws Exception {
		testDomain(domain, domain, true);
	}

	@Test
	public void testSubdomainAgainstWildcardCertificate() throws Exception {
		testDomain("push." + domain, wildcardDomain, true);
	}

	@Test
	public void testUpperCaseDomain() throws Exception {
		testDomain(domain.toUpperCase(), domain, true);
	}

	@Test
	public void testDomainForNonexistentCertificate() throws Exception {
		testDomain("xmpp.org", "xmpp.org", false);
	}

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean(TestCertificateRepositoryWithoutStore.class).setActive(true).exportable().exec();
		kernel.registerBean(TestCertificateContainerWithoutStore.class).exec();
		kernel.registerBean(SSLContextContainer.class).exec();
	}

	@Before
	public void setup() throws GeneralSecurityException, IOException {
		certificateContainer = getKernel().getInstance(CertificateContainer.class);
		sslContextContainer = getKernel().getInstance(SSLContextContainer.class);

		addCertificateForDomain(domain, true);
		addCertificateForDomain("*." + domain, true);
	}

	/**
	 * If wildcard certificate is added it should be used instead of explicit one...
	 */
	@Test
	public void testAddingCertificate() throws GeneralSecurityException, IOException {

		addCertificateForDomain(mucDomain, false);
		addCertificateForDomain(wildcardDomain, true);

		var domainEntry = certificateContainer.getCertificateEntry(domain);
		Assert.assertNotNull(domainEntry);
		Assert.assertEquals(CertificateUtil.getCertCName((X509Certificate) domainEntry.getCertificate().get()), domain);

		var wildcardDomainEntry = certificateContainer.getCertificateEntry(wildcardDomain);
		Assert.assertNotNull(wildcardDomainEntry);
		Assert.assertEquals(CertificateUtil.getCertCName((X509Certificate) wildcardDomainEntry.getCertificate().get()),
		                    wildcardDomain);

		var mucDomainEntry = certificateContainer.getCertificateEntry(mucDomain);
		Assert.assertNotNull(mucDomainEntry);
		// we expect mucDomain as both certificates are valid, so specific is more important than wildcard
		Assert.assertEquals(mucDomain, CertificateUtil.getCertCName((X509Certificate) mucDomainEntry.getCertificate().get()));
	}

	private void testDomain(String hostname, String expectedDomain, boolean expectsExist) throws Exception {
		CertificateEntry certificateEntry = certificateContainer.getCertificateEntry(hostname);

		LOGGER.log(Level.INFO, "Certificate for hostname " + hostname + ": " +
				(certificateEntry != null ? certificateEntry.toString(true) : "n/a"));
		if (expectsExist) {
			Assert.assertNotNull(certificateEntry);
		} else {
			Assert.assertNull(certificateEntry);
		}

		var ssl = sslContextContainer.createContextHolder("SSL", hostname, hostname, false, new TrustManager[0]);

		Assert.assertNotNull(ssl);
		Assert.assertNotNull(ssl.domainCertificate);
		String cNname = CertificateUtil.getCertCName(ssl.domainCertificate);

		if (expectsExist) {
			Assert.assertEquals(certificateEntry.getCertChain()[0], ssl.domainCertificate);
		} else {
			CertificateEntry generatedEntry = certificateContainer.getCertificateEntry(hostname);
			Assert.assertNotNull(generatedEntry);
		}
		Assert.assertEquals(expectedDomain, cNname);
	}

	private void addCertificateForDomain(String domain, boolean includeWildcardSubdomain)
			throws GeneralSecurityException, IOException {
		final CertificateGenerator generator = CertificateGeneratorFactory.getGenerator();
		CertificateEntry selfSignedCertificate = generator.generateSelfSignedCertificateEntry("test@mail.com", domain,
		                                                                                      "OU", "O", "City",
		                                                                                      "State", "Country",
		                                                                                      createKeyPair(1024,
		                                                                                                    "secret"),
		                                                                                      includeWildcardSubdomain);
		var pemCertificate = exportToPemFormat(selfSignedCertificate);
		var certificateEntity = new CertificateEntity(pemCertificate, domain, false, false);
		certificateContainer.addCertificates(certificateEntity);
	}

	public static class TestCertificateContainerWithoutStore
			extends CertificateContainer {

		public TestCertificateContainerWithoutStore() {

		}

		@Override
		void storeCertificateToFile(CertificateEntry entry, String filename)
				throws CertificateEncodingException, IOException {
			throw new RuntimeException(
					new IOException("We tried storing certificate to file, even though we shouldn't"));
		}
	}

	public static class TestCertificateRepositoryWithoutStore
			extends CertificateRepository {

		public TestCertificateRepositoryWithoutStore() {
		}

		@Override
		protected void storeSingleItem(CertificateItem item) {
			LOGGER.log(Level.SEVERE, "Storing certificate to repository (we shouldn't?");
			super.storeSingleItem(item);
		}
	}
}