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
package tigase.server.xmppclient;

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.vhosts.VHostItem;
import tigase.xmpp.XMPPIOService;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "client-trust-manager-factory", parent = ClientConnectionManager.class, active = true)
public class ClientTrustManagerFactory {

	public static final String CA_CERT_PATH = "clientCertCA";

	public static final String CERT_REQUIRED_KEY = "clientCertRequired";

	private final static char[] EMPTY_PASS = new char[0];

	private static final Logger log = Logger.getLogger(ClientTrustManagerFactory.class.getName());

	private final ArrayList<X509Certificate> acceptedIssuers = new ArrayList<X509Certificate>();

	private final TrustManager[] emptyTrustManager;
	private final KeyStore keystore;
	private final ConcurrentHashMap<VHostItem, TrustManager[]> trustManagers = new ConcurrentHashMap<>();
	@ConfigField(desc = "CA for client certificate", alias = "clientCertCA")
	private String clientCertCA;
	@ConfigField(desc = "Is client certificate required")
	private boolean clientCertRequired = false;
	private TrustManager[] defaultTrustManagers;
	private TrustManagerFactory tmf;

	public ClientTrustManagerFactory() {
		this.emptyTrustManager = new TrustManager[]{new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		}};
		try {
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(null, EMPTY_PASS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			this.tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

	}

	public void setClientCertCA(String clientCertCA) {
		this.clientCertCA = clientCertCA;
		if (clientCertCA != null) {
			defaultTrustManagers = loadTrustedCert(clientCertCA);
		} else {
			defaultTrustManagers = null;
		}
	}

	public TrustManager[] getManager(final VHostItem vHost) {
		TrustManager[] result = trustManagers.get(vHost);

		if (result == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Creating new TrustManager for VHost " + vHost);
			}

			result = defaultTrustManagers;
			String path = vHost.getData(CA_CERT_PATH);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("CA cert path=" + path + " for VHost " + vHost);
			}
			if (path != null) {
				TrustManager[] tmp = loadTrustedCert(path);
				if (tmp != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Using custom TrustManager for VHost " + vHost);
					}
					result = tmp;
					trustManagers.put(vHost, result);
				}
			}
		} else if (log.isLoggable(Level.FINEST)) {
			log.finest("Found TrustManager for VHost " + vHost);
		}

		return result;
	}

	public TrustManager[] getManager(final XMPPIOService<Object> serv) {
		return isActive() ? emptyTrustManager : null;
	}

	public boolean isActive() {
		return acceptedIssuers.size() > 0;
	}

	public boolean isTlsNeedClientAuthEnabled(final VHostItem vhost) {
		Boolean result = vhost.getData(CERT_REQUIRED_KEY);
		if (result == null) {
			result = clientCertRequired;
		}
		return result;
	}

	public boolean isTlsWantClientAuthEnabled(final VHostItem vhost) {
		TrustManager[] tmp = getManager(vhost);
		return tmp != null && tmp.length > 0;
	}

	protected X509Certificate[] getAcceptedIssuers() {
		return acceptedIssuers.toArray(new X509Certificate[]{});
	}

	protected TrustManager[] loadTrustedCert(String caCertFile) {
		try {
			CertificateEntry certEntry = CertificateUtil.loadCertificate(caCertFile);
			Certificate[] chain = certEntry.getCertChain();

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Loaded certificate from file " + caCertFile + " : " + certEntry);
			}

			if (chain != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Loaded cert chain: " + Arrays.toString(chain));
				}
				for (Certificate cert : chain) {
					if (cert instanceof X509Certificate) {
						X509Certificate crt = (X509Certificate) cert;
						String alias = crt.getSubjectX500Principal().getName();

						if (log.isLoggable(Level.FINEST)) {
							log.finest("Adding certificate to keystore: alias=" + alias + "; cert=" + crt);
						}

						keystore.setCertificateEntry(alias, crt);
						acceptedIssuers.add(crt);
					}
				}
			}
			tmf.init(keystore);
			return tmf.getTrustManagers();
			// this.saslExternalAvailable = true;
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't create TrustManager with certificate from file.", e);
			throw new RuntimeException(e);
		}
	}

}
