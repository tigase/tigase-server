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
package tigase.server.xmppclient;

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.vhosts.*;
import tigase.xml.Element;
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
import java.util.Map;
import java.util.Optional;
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

	protected final TrustManager[] emptyTrustManager;
	private final KeyStore keystore;
	private final ConcurrentHashMap<VHostItem, TrustManager[]> trustManagers = new ConcurrentHashMap<>();
	@ConfigField(desc = "CA for client certificate", alias = "clientCertCA")
	private String clientCertCA;
	@ConfigField(desc = "Is client certificate required")
	private boolean clientCertRequired = false;
	protected TrustManager[] defaultTrustManagers;
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
			ClientTrustVHostItemExtension extension = vHost.getExtension(ClientTrustVHostItemExtension.class);
			String path = extension != null ? extension.getCaCertPath() : null;
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
		ClientTrustVHostItemExtension extension = vhost.getExtension(ClientTrustVHostItemExtension.class);
		if (extension == null || extension.isCertRequired() == null) {
			return clientCertRequired;
		}
		return extension.isCertRequired();
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
			log.log(Level.WARNING, "Can''t create TrustManager with certificate from file.", e);
			throw new RuntimeException(e);
		}
	}

	@Bean(name = "client-trust-extension", parent = VHostItemExtensionManager.class, active = true)
	public static class ClientTrustVHostItemExtensionProvider
			implements VHostItemExtensionProvider<ClientTrustVHostItemExtension> {

		@Override
		public String getId() {
			return ClientTrustVHostItemExtension.ID;
		}

		@Override
		public Class<ClientTrustVHostItemExtension> getExtensionClazz() {
			return ClientTrustVHostItemExtension.class;
		}
	}

	public static class ClientTrustVHostItemExtension
			extends AbstractVHostItemExtension<ClientTrustVHostItemExtension>
			implements VHostItemExtensionBackwardCompatible<ClientTrustVHostItemExtension> {

		protected static final String ID = "client-trust-extension";

		public static final String CA_CERT_PATH = "ca-cert-path";
		public static final String CERT_REQUIRED = "cert-required";

		private String caCertPath;
		private Boolean certRequired = null;

		@Override
		public String getId() {
			return ID;
		}

		public String getCaCertPath() {
			return caCertPath;
		}

		public Boolean isCertRequired() {
			return certRequired;
		}

		@Override
		public void initFromElement(Element item) {
			caCertPath = item.getAttributeStaticStr(CA_CERT_PATH);
			certRequired = Optional.ofNullable(item.getAttributeStaticStr(CERT_REQUIRED))
					.map(Boolean::parseBoolean)
					.orElse(null);
		}

		@Override
		public void initFromCommand(String prefix, Packet packet) throws IllegalArgumentException {
			caCertPath = Optional.ofNullable(Command.getFieldValue(packet, prefix + "-" + CA_CERT_PATH))
					.filter(s -> !s.isEmpty())
					.orElse(null);
			certRequired = Optional.ofNullable(Command.getFieldValue(packet, prefix + "-" + CERT_REQUIRED))
					.map(s -> s.isEmpty() ? null : s)
					.map(Boolean::parseBoolean)
					.orElse(null);
		}

		@Override
		public String toDebugString() {
			return "caCertPath: " + caCertPath + ", certRequired: " + certRequired;
		}

		@Override
		public Element toElement() {
			if ((caCertPath != null && !caCertPath.isEmpty()) || certRequired != null) {
				Element el = new Element(getId());
				if (caCertPath != null) {
					el.addAttribute(CA_CERT_PATH, caCertPath);
				}
				if (certRequired != null) {
					el.addAttribute(CERT_REQUIRED, String.valueOf(certRequired));
				}
				return el;
			} else {
				return null;
			}
		}

		@Override
		public void addCommandFields(String prefix, Packet packet, boolean forDefault) {
			Element commandEl = packet.getElemChild(Command.COMMAND_EL, Command.XMLNS);
			DataForm.addFieldValue(commandEl, prefix + "-" + CA_CERT_PATH, caCertPath, "text-single",
								   "Client Certificate CA");
			addBooleanFieldWithDefaultToCommand(commandEl, prefix + "-" + CERT_REQUIRED, "Client Certificate Required",
												certRequired, forDefault);
		}

		@Override
		public void initFromData(Map<String, Object> data) {
			caCertPath = (String) data.remove(ClientTrustManagerFactory.CA_CERT_PATH);
			certRequired = (Boolean) data.remove(ClientTrustManagerFactory.CERT_REQUIRED_KEY);
		}

		@Override
		public ClientTrustVHostItemExtension mergeWithDefaults(ClientTrustVHostItemExtension defaults) {
			return this;
		}

	}

}
