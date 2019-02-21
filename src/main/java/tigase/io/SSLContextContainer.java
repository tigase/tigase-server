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

import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.ConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Oct 15, 2010 2:40:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "sslContextContainer", parent = ConnectionManager.class, active = true)
public class SSLContextContainer
		extends SSLContextContainerAbstract {

	// Workaround for TLS/SSL bug in new JDK used with new version of
	// nss library see also:
	// http://stackoverflow.com/q/10687200/427545
	// http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=b509d9cb5d8164d90e6731f5fc44?bug_id=6928796
	private static final String[] TLS_WORKAROUND_CIPHERS = new String[]{"SSL_RSA_WITH_RC4_128_MD5",
																		"SSL_RSA_WITH_RC4_128_SHA",
																		"TLS_RSA_WITH_AES_128_CBC_SHA",
																		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
																		"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
																		"SSL_RSA_WITH_3DES_EDE_CBC_SHA",
																		"SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
																		"SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
																		"SSL_RSA_WITH_DES_CBC_SHA",
																		"SSL_DHE_RSA_WITH_DES_CBC_SHA",
																		"SSL_DHE_DSS_WITH_DES_CBC_SHA",
																		"SSL_RSA_EXPORT_WITH_RC4_40_MD5",
																		"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
																		"SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
																		"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
																		"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};

	private static final String[] HARDENED_MODE_FORBIDDEN_CIPHERS = new String[]{
			"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
			"SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
			"TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
			"SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
			"SSL_RSA_WITH_RC4_128_SHA", "TLS_ECDH_ECDSA_WITH_RC4_128_SHA", "TLS_ECDH_RSA_WITH_RC4_128_SHA",
			"SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "TLS_KRB5_WITH_RC4_128_SHA",
			"TLS_KRB5_WITH_RC4_128_MD5", "TLS_KRB5_EXPORT_WITH_RC4_40_SHA", "TLS_KRB5_EXPORT_WITH_RC4_40_MD5"};
	private static final String[] HARDENED_MODE_PROTOCOLS = new String[]{"SSLv2Hello", "TLSv1", "TLSv1.1", "TLSv1.2"};
	private static final Logger log = Logger.getLogger(SSLContextContainer.class.getName());
	private static String[] HARDENED_MODE_CIPHERS;

	static {
		String[] allEnabledCiphers = null;
		try {
			SSLEngine tmpE = SSLContext.getDefault().createSSLEngine();
			allEnabledCiphers = tmpE.getEnabledCipherSuites();
			log.config("Supported protocols: " + markEnabled(tmpE.getEnabledProtocols(), tmpE.getSupportedProtocols()));
			log.config("Supported ciphers: " + markEnabled(allEnabledCiphers, tmpE.getSupportedCipherSuites()));

			ArrayList<String> ciphers = new ArrayList<String>(Arrays.asList(allEnabledCiphers));
			ciphers.removeAll(Arrays.asList(HARDENED_MODE_FORBIDDEN_CIPHERS));
			HARDENED_MODE_CIPHERS = ciphers.toArray(new String[]{});
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.WARNING, "Can't determine supported protocols", e);
		}
	}

	@Inject
	protected EventBus eventBus = EventBusFactory.getInstance();
	protected Map<String, SSLHolder> sslContexts = new ConcurrentSkipListMap<>();
	@ConfigField(desc = "Enabled TLS/SSL ciphers", alias = "tls-enabled-ciphers")
	private String[] enabledCiphers;
	@ConfigField(desc = "Enabled TLS/SSL protocols", alias = "tls-enabled-protocols")
	private String[] enabledProtocols;
	@ConfigField(desc = "TLS/SSL hardened mode", alias = "hardened-mode")
	private boolean hardenedMode = false;
	@Inject(bean = "rootSslContextContainer", type = Root.class, nullAllowed = true)
	private SSLContextContainerIfc parent;
	@ConfigField(desc = "TLS/SSL", alias = "tls-jdk-nss-bug-workaround-active")
	private boolean tlsJdkNssBugWorkaround = false;

	private static String markEnabled(String[] enabled, String[] supported) {
		final List<String> en = enabled == null ? new ArrayList<String>() : Arrays.asList(enabled);
		String result = "";

		if (supported != null) {
			for (int i = 0; i < supported.length; i++) {
				String t = supported[i];
				result += (en.contains(t) ? "(+)" : "(-)");
				result += t;
				if (i + 1 < supported.length) {
					result += ",";
				}
			}
		}

		return result;
	}

	/**
	 * Constructor for bean only
	 */
	public SSLContextContainer() {
		this(null, null);
	}

	/**
	 * Constructor used to create root SSLContextContainer instance which should cache only SSLContext instances where
	 * array of TrustManagers is not set - common for all ConnectionManagers. This instance is kept by TLSUtil class.
	 *
	 * @param certContainer
	 */
	public SSLContextContainer(CertificateContainerIfc certContainer) {
		this(certContainer, null);
	}

	/**
	 * Constructor used to create instances for every ConnectionManager so that every connection manager can have
	 * different TrustManagers and SSLContext instance will still be cached.
	 *
	 * @param certContainer
	 * @param parent
	 */
	public SSLContextContainer(CertificateContainerIfc certContainer, SSLContextContainerIfc parent) {
		super(certContainer);
		this.parent = parent;
	}

	@Override
	public IOInterface createIoInterface(String protocol, String tls_hostname, int port, boolean clientMode,
										 boolean wantClientAuth, boolean needClientAuth, ByteOrder byteOrder,
										 TrustManager[] x509TrustManagers, TLSEventHandler eventHandler,
										 IOInterface socketIO, CertificateContainerIfc certificateContainer)
			throws IOException {
		SSLContext sslContext = getSSLContext(protocol, tls_hostname, clientMode, x509TrustManagers);
		TLSWrapper wrapper = new JcaTLSWrapper(sslContext, eventHandler, tls_hostname, port, clientMode, wantClientAuth,
											   needClientAuth, getEnabledCiphers(), getEnabledProtocols());
		return new TLSIO(socketIO, wrapper, byteOrder);
	}

	@Override
	public String[] getEnabledCiphers() {
		if (enabledCiphers != null && enabledCiphers.length != 0) {
			return enabledCiphers;
		} else if (hardenedMode) {
			return HARDENED_MODE_CIPHERS;
		} else if (tlsJdkNssBugWorkaround) {
			return TLS_WORKAROUND_CIPHERS;
		}
		return null;
	}

	public void setEnabledCiphers(String[] enabledCiphers) {
		if (log.isLoggable(Level.CONFIG)) {
			log.config("Enabled ciphers: " + (enabledCiphers == null ? "default" : Arrays.toString(enabledCiphers)));
		}
		this.enabledCiphers = enabledCiphers;
	}

	@Override
	public String[] getEnabledProtocols() {
		if (enabledProtocols != null && enabledProtocols.length != 0) {
			return enabledProtocols;
		} else if (hardenedMode) {
			return HARDENED_MODE_PROTOCOLS;
		}
		return null;
	}

	public void setEnabledProtocols(String[] enabledProtocols) {
		if (log.isLoggable(Level.CONFIG)) {
			log.config(
					"Enabled protocols: " + (enabledProtocols == null ? "default" : Arrays.toString(enabledProtocols)));
		}
		this.enabledProtocols = enabledProtocols;
	}

	@Override
	public SSLContext getSSLContext(String protocol, String hostname, boolean clientMode, TrustManager[] tms) {
		SSLHolder holder = null;

		String alias = hostname;

		try {
			if (tms == null) {
				if (parent != null) {
					return parent.getSSLContext(protocol, hostname, clientMode, tms);
				} else {
					tms = getTrustManagers();
				}
			}

			if (alias == null) {
				alias = getDefCertAlias();
			} // end of if (hostname == null)

			holder = find(sslContexts, alias);

			if (!validateDomainCertificate(holder, alias)) {
				holder = null;
			}

			if (holder == null || !holder.isValid(tms)) {
				holder = createContextHolder(protocol, hostname, alias, clientMode, tms);
				if (clientMode) {
					return holder.sslContext;
				}

				if (!validateDomainCertificate(holder, alias)) {
					holder = createContextHolder(protocol, hostname, alias, clientMode, tms);
				}

				sslContexts.put(alias, holder);
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not initialize SSLContext for domain: " + alias + ", protocol: " + protocol, e);
			holder = null;
		}

		return holder != null ? holder.getSSLContext() : null;
	}

	@Override
	public KeyStore getTrustStore() {
		KeyStore trustStore = super.getTrustStore();
		if (trustStore == null && parent != null) {
			trustStore = parent.getTrustStore();
		}
		return trustStore;
	}

	private void invalidateContextHolder(SSLHolder holder, String alias) throws Exception {
		sslContexts.remove(alias);
		createCertificate(alias);
	}

	/**
	 * Method handles <code>CertificateChanged</code> event emitted by CertificateContainer and removes cached instance
	 * of SSLContext for domain for which certificate has changed.
	 *
	 * @param event
	 */
	@HandleEvent
	private void onCertificateChange(CertificateContainer.CertificateChanged event) {
		String alias = event.getAlias();
		sslContexts.remove(alias);
	}

	public void setHardenedMode(boolean hardenedMode) {
		if (log.isLoggable(Level.CONFIG)) {
			log.config("Hardened mode is " + (hardenedMode ? "enabled" : "disabled"));
		}
		if (hardenedMode) {
			System.setProperty("jdk.tls.ephemeralDHKeySize", "2048");
		}
		this.hardenedMode = hardenedMode;
	}

	public void setParent(SSLContextContainerIfc parent) {
		log.log(Level.FINE, "setting root = " + parent);
		this.parent = parent;
	}

	public void setTlsJdkNssBugWorkaround(boolean value) {
		if (log.isLoggable(Level.CONFIG)) {
			log.config("Workaround for TLS/SSL bug is " + (value ? "enabled" : "disabled"));
		}
		this.tlsJdkNssBugWorkaround = value;
	}

	@Override
	public void start() {
		eventBus.registerAll(this);
	}

	@Override
	public void stop() {
		eventBus.unregisterAll(this);
	}

	private boolean validateDomainCertificate(final SSLHolder holder, final String alias) throws Exception {
		// for self-signed certificates only
		if (holder != null && holder.domainCertificate != null &&
				holder.domainCertificate.getIssuerDN().equals(holder.domainCertificate.getSubjectDN())) {
			try {
				holder.domainCertificate.checkValidity();
			} catch (CertificateException e) {
				invalidateContextHolder(holder, alias);
				return false;
			}
		}

		return true;
	}

	@Bean(name = "rootSslContextContainer", parent = Kernel.class, active = true, exportable = true)
	public static class Root
			extends SSLContextContainer
			implements Initializable, UnregisterAware {

		public Root() {
			super();
		}

		@Override
		public void beforeUnregister() {
			stop();
		}

		@Override
		public void initialize() {
			start();
		}

		// empty method to ensure that parent will not be injected to root instance
		public void setParent(SSLContextContainerIfc parent) {
			log.log(Level.FINE, "setting root = " + parent);
		}
	}

}

