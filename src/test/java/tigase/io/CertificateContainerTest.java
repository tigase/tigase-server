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
import tigase.cert.CertificateUtil;
import tigase.eventbus.EventBusFactory;
import tigase.io.repo.CertificateItem;
import tigase.io.repo.CertificateRepository;
import tigase.kernel.AbstractKernelWithUserRepositoryTestCase;
import tigase.kernel.core.Kernel;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.cert.CertificateUtil.*;
import static tigase.io.CertificateContainerIfc.CertificateEntity;

public class CertificateContainerTest extends AbstractKernelWithUserRepositoryTestCase {

	private static final Logger LOGGER = Logger.getLogger(CertificateContainerTest.class.getName());
	private final String domain = "example.com";
	private CertificateContainer certificateContainer;
	private SSLContextContainer sslContextContainer;

	@Test
	public void testRegularDomain() throws Exception {
		testDomain(domain, true);
	}

	@Test
	public void testWildcardDomain() throws Exception {
		testDomain("push." + domain, true);
	}

	@Test
	public void testUpperCaseDomain() throws Exception {
		testDomain(domain.toUpperCase(), true);
	}

	@Test
	public void testNonexistentDomain() throws Exception {
		testDomain("xmpp.org", false);
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
	public void setup() throws CertificateException, NoSuchAlgorithmException, IOException, SignatureException,
							   NoSuchProviderException, InvalidKeyException {
		certificateContainer = getKernel().getInstance(CertificateContainer.class);
		sslContextContainer = getKernel().getInstance(SSLContextContainer.class);

		CertificateEntry selfSignedCertificate = createSelfSignedCertificate("test@mail.com", "*." + domain, "OU", "O",
																			 "City", "State", "Country",
																			 () -> createKeyPair(1024, "secret"));

		var pemCertificate = exportToPemFormat(selfSignedCertificate);

		var certificateEntity = new CertificateEntity(pemCertificate, domain, false, false);
		certificateContainer.addCertificates(certificateEntity);
	}

	private void testDomain(String hostname, boolean expectsExist) throws Exception {
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


		// FIXME: rework this test to take into consideration auto-generation of the certificate…
		if (expectsExist) {
			Assert.assertEquals(certificateEntry.getCertChain()[0], ssl.domainCertificate);
			Assert.assertTrue(cNname.contains(domain));
		} else {
			Assert.assertFalse(cNname.contains(domain));
		}
	}

	public static class TestCertificateContainerWithoutStore extends CertificateContainer {

		public TestCertificateContainerWithoutStore() {

		}

		@Override
		void storeCertificateToFile(CertificateEntry entry, String filename)
				throws CertificateEncodingException, IOException {
			throw new RuntimeException(new IOException("We tried storing certificate to file, even though we shouldn't"));
		}
	}

	public static class TestCertificateRepositoryWithoutStore extends CertificateRepository {

		public TestCertificateRepositoryWithoutStore() {
		}

		@Override
		public void addItem(CertificateItem item) {
			super.addItem(item);
//			throw new RuntimeException(new IOException("We tried storing certificate to repository, even though we shouldn't"));
		}

		@Override
		public void store() {
			super.store();
//			throw new RuntimeException(new IOException("We tried storing certificate to repository, even though we shouldn't"));
		}
	}
}