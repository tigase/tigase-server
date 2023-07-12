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

import tigase.annotations.TigaseDeprecated;
import tigase.cert.CertificateEntry;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.Map;
import java.util.Objects;

/**
 * Interface implemented by classes responsible for keeping SSL certificates in memory
 * <br>
 * Created by andrzej on 29.02.2016.
 */
public interface CertificateContainerIfc {

	String CERTIFICATE_CONTAINER_CLASS_KEY = "cert-container-class";

	String CERTIFICATE_CONTAINER_CLASS_VAL = CertificateContainer.class.getCanonicalName();

	/**
	 * Method <code>addCertificates</code> allows to add more certificates at run time after the container has bee
	 * already initialized. This is to avoid server restart if there are certificates updates or new certificates for
	 * new virtual domain. The method should add new certificates or replace existing one if there is already a
	 * certificate for a domain.
	 *
	 * @param params a <code>Map</code> value with configuration parameters.
	 *
	 */
	@Deprecated
	@TigaseDeprecated(since = "8.4.0", removeIn = "9.0.0", note = "Method with tigase.io.CertificateContainerIfc.CertificateEntity should be used")
	void addCertificates(Map<String, String> params) throws CertificateParsingException;

	void addCertificates(CertificateEntity certificateEntity) throws CertificateParsingException;

	/**
	 * Method <code>createCertificate</code> allows to generate self-signed certificate for passed domain name.s
	 *
	 * @param domain domain for which certificate should be generated
	 *
	 * @return an array of <code>KeyManager</code> containing generated certificate
	 *
	 */
	KeyManager[] createCertificate(String domain)
			throws NoSuchAlgorithmException, CertificateException, SignatureException, NoSuchProviderException,
				   InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException;

	/**
	 * Method to retrieve default alias of certificate to use when domain is <code>null</code>
	 *
	 * @return default alias
	 */
	String getDefCertAlias();

	CertificateEntry getCertificateEntry(String hostname);

	/**
	 * Method returns array of <code>KeyManager</code> with certificate for domain or <code>null</code> if there is no
	 * certificate for domain
	 */
	KeyManager[] getKeyManagers(String domain);

	TrustManager[] getTrustManagers();

	KeyStore getTrustStore();

	/**
	 * Method used to pass parameters to initialize instance of class
	 *
	 */
	void init(Map<String, Object> params);

	record CertificateEntity(String certificatePem, String alias, boolean storePermanently, boolean useAsDefault) {

		public CertificateEntity {
			Objects.requireNonNull(certificatePem);
			Objects.requireNonNull(alias);
		}

		CertificateEntity withAlias(String alias) {
			return new CertificateEntity(certificatePem, alias, storePermanently, useAsDefault);
		}
		CertificateEntity withDefaultAlias() {
			return new CertificateEntity(certificatePem, SSLContextContainerIfc.DEFAULT_DOMAIN_CERT_VAL, storePermanently, useAsDefault);
		}
	}
}
