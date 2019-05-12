/**
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.cert.CertificateUtil.*;
import static tigase.io.SSLContextContainerIfc.*;

public class CertificateContainerTest {

	private static final Logger LOGGER = Logger.getLogger(CertificateContainerTest.class.getName());
	private final String domain = "example.com";
	private CertificateContainer certificateContainer;

	@Test
	public void testRegularDomain() throws Exception {
		testDomain(domain, true);
	}

	@Test
	public void testWildcardDomain() throws Exception {
		testDomain("push." + domain, true);
	}

	@Test
	public void testNonexistentDomain() throws Exception {
		testDomain("xmpp.org", false);
	}

	@Before
	public void setup() throws CertificateException, NoSuchAlgorithmException, IOException, SignatureException,
							   NoSuchProviderException, InvalidKeyException {
		certificateContainer = new CertificateContainer() {

			@Override
			KeyManagerFactory addCertificateEntry(CertificateEntry entry, String alias, boolean store)
					throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
						   UnrecoverableKeyException {
				return super.addCertificateEntry(entry, alias, false);
			}
		};

		Map<String, String> params = new HashMap<>();
		params.put(CERT_ALIAS_KEY, domain);
		CertificateEntry selfSignedCertificate = createSelfSignedCertificate("test@mail.com", "*." + domain, "OU", "O",
																			 "City", "State", "Country",
																			 () -> createKeyPair(1024, "secret"));

		final String pemCertificate = exportToPemFormat(selfSignedCertificate);
		params.put(PEM_CERTIFICATE_KEY, pemCertificate);
		params.put(CERT_SAVE_TO_DISK_KEY, "false");

		certificateContainer.addCertificates(params);
	}

	private void testDomain(String hostname, boolean exists) throws Exception {
		CertificateEntry certificateEntry = certificateContainer.getCertificateEntry(hostname);

		LOGGER.log(Level.INFO, "Certificate for hostname " + hostname + ": " +
				(certificateEntry != null ? certificateEntry.toString(true) : "n/a"));
		if (exists) {
			Assert.assertNotNull(certificateEntry);
		} else {
			Assert.assertNull(certificateEntry);
		}

		SSLContextContainer sslContextContainer = new SSLContextContainer(certificateContainer);
		SSLContextContainerAbstract.SSLHolder ssl = sslContextContainer.createContextHolder("SSL", hostname, hostname,
																							false, new TrustManager[0]);
		Assert.assertNotNull(ssl);
		Assert.assertNotNull(ssl.domainCertificate);
		if (exists) {
			Assert.assertEquals(certificateEntry.getCertChain()[0], ssl.domainCertificate);
			Assert.assertTrue(ssl.domainCertificate.getSubjectDN().getName().contains(domain));
		} else {
			Assert.assertFalse(ssl.domainCertificate.getSubjectDN().getName().contains(domain));
		}
	}

}