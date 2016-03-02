/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.io;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.Map;

/**
 * Interface implemented by classes responsible for keeping SSL certificates in memory
 *
 * Created by andrzej on 29.02.2016.
 */
public interface CertificateContainerIfc {

	String CERTIFICATE_CONTAINER_CLASS_KEY = "cert-container-class";
	String CERTIFICATE_CONTAINER_CLASS_VAL = CertificateContainer.class.getCanonicalName();

	/**
	 * Method <code>addCertificates</code> allows to add more certificates at
	 * run time after the container has bee already initialized. This is to
	 * avoid server restart if there are certificates updates or new
	 * certificates for new virtual domain. The method should add new
	 * certificates or replace existing one if there is already a certificate
	 * for a domain.
	 *
	 * @param params
	 *            a <code>Map</code> value with configuration parameters.
	 * @throws CertificateParsingException
	 */
	void addCertificates(Map<String, String> params) throws CertificateParsingException;

	/**
	 * Method <code>createCertificate</code> allows to generate self-signed
	 * certificate for passed domain name.s
	 *
	 * @param domain
	 *            domain for which certificate should be generated
	 * @return an array of <code>KeyManager</code> containing generated certificate
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws SignatureException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeyException
	 * @throws IOException
	 * @throws UnrecoverableKeyException
	 * @throws KeyStoreException
	 */
	KeyManager[] createCertificate(String domain) throws NoSuchAlgorithmException, CertificateException, SignatureException, NoSuchProviderException, InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException;

	/**
	 * Method to retrieve default alias of certificate to use when domain is <code>null</code>
	 * @return default alias
	 */
	String getDefCertAlias();

	/**
	 * Method returns array of <code>KeyManager</code> with certificate for domain
	 * or <code>null</code> if there is no certificate for domain
	 *
	 * @param domain
	 * @return
	 */
	KeyManager[] getKeyManagers(String domain);

	TrustManager[] getTrustManagers();

	KeyStore getTrustStore();

	/**
	 * Method used to pass parameters to initialize instance of class
	 *
	 * @param params
	 */
	void init(Map<String,Object> params);
}
