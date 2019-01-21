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

import tigase.kernel.beans.Inject;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by andrzej on 29.02.2016.
 */
public abstract class SSLContextContainerAbstract
		implements SSLContextContainerIfc {

	private static final Logger log = Logger.getLogger(SSLContextContainerAbstract.class.getCanonicalName());

	@Inject
	private final CertificateContainerIfc certificateContainer;

	private SecureRandom secureRandom = new SecureRandom();

	/**
	 * Generic method responsible for lookup of value in <code>Map</code> where passed key is domain name and in
	 * <code>Map</code> wildcard name may be used as a key.
	 *
	 * @param data
	 * @param key
	 * @param <T>
	 *
	 * @return
	 */
	public static <T> T find(Map<String, T> data, String key) {
		if (data.containsKey(key)) {
			return data.get(key);
		}

		// should be faster than code commented below
		// in case when there is no value at all
		int idx = key.indexOf(".");
		if (idx >= 0) {
			String asteriskKey = "*" + key.substring(idx);
			T value = data.get(asteriskKey);
			if (value != null) {
				data.put(key, value);
				return value;
			}
		}

		return null;
	}

	public SSLContextContainerAbstract(CertificateContainerIfc certContainer) {
		this.certificateContainer = certContainer;
	}

	@Override
	public void addCertificates(Map<String, String> params) throws CertificateParsingException {
		this.certificateContainer.addCertificates(params);
	}

	protected KeyManager[] createCertificate(String alias) throws Exception {
		return certificateContainer.createCertificate(alias);
	}

	/**
	 * Common method used to create SSLContext instance based on provided parameters
	 *
	 * @param protocol
	 * @param hostname
	 * @param alias
	 * @param clientMode
	 * @param tms
	 *
	 * @return
	 *
	 * @throws Exception
	 */
	protected SSLHolder createContextHolder(String protocol, String hostname, String alias, boolean clientMode,
											TrustManager[] tms) throws Exception {
		SSLContext sslContext = null;

		KeyManager[] kms = getKeyManagers(hostname);
		if (kms == null) {
			// if there is no KeyManagerFactory for domain then we can create
			// new empty context as we have no certificate for this domain
			if (clientMode) {
				sslContext = SSLContext.getInstance(protocol);
				sslContext.init(null, tms, secureRandom);
				return new SSLHolder(tms, sslContext, null);
			}

			kms = createCertificate(alias);
		}

		X509Certificate crt = null;
		if (kms.length > 0 && kms[0] instanceof X509KeyManager) {
			X509KeyManager km = (X509KeyManager) kms[0];
			X509Certificate[] zzz = km.getCertificateChain(hostname);
			crt = zzz.length == 0 ? null : zzz[zzz.length - 1];
		}

		sslContext = SSLContext.getInstance(protocol);
		sslContext.init(kms, tms, secureRandom);

		return new SSLHolder(tms, sslContext, crt);
	}

	protected String getDefCertAlias() {
		return certificateContainer.getDefCertAlias();
	}

	protected KeyManager[] getKeyManagers(String hostname) {
		return certificateContainer.getKeyManagers(hostname);
	}

	@Override
	public SSLContext getSSLContext(String protocol, String hostname, boolean clientMode) {
		return getSSLContext(protocol, hostname, clientMode, null);
	}

	protected TrustManager[] getTrustManagers() {
		return certificateContainer.getTrustManagers();
	}

	@Override
	public KeyStore getTrustStore() {
		return (certificateContainer != null) ? certificateContainer.getTrustStore() : null;
	}

	protected class SSLHolder {

		final X509Certificate domainCertificate;
		final SSLContext sslContext;
		final TrustManager[] tms;

		public SSLHolder(TrustManager[] tms, SSLContext sslContext, X509Certificate domainCertificate) {
			this.tms = tms;
			this.sslContext = sslContext;
			this.domainCertificate = domainCertificate;
		}

		public SSLContext getSSLContext() {
			return sslContext;
		}

		public boolean isValid(TrustManager[] tms) {
			return tms == this.tms;
		}
	}

}
