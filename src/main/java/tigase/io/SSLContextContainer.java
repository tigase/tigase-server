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

import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.vhosts.*;
import tigase.xml.Element;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created: Oct 15, 2010 2:40:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = "sslContextContainer", parent = ConnectionManager.class, active = true)
public class SSLContextContainer
		extends SSLContextContainerAbstract {

	// Workaround for TLS/SSL bug in new JDK used with new version of
	// nss library see also:
	// http://stackoverflow.com/q/10687200/427545
	// http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=b509d9cb5d8164d90e6731f5fc44?bug_id=6928796
	/* @formatter:off */
	private static final String EPHEMERAL_DH_KEYSIZE_KEY = "jdk.tls.ephemeralDHKeySize";
	private static final String EPHEMERAL_DH_KEYSIZE_VALUE = "4096";
	private static final String[] TLS_WORKAROUND_CIPHERS = new String[]{"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
																		"SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
																		"SSL_DHE_DSS_WITH_DES_CBC_SHA",
																		"SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
																		"SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
																		"SSL_DHE_RSA_WITH_DES_CBC_SHA",
																		"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
																		"SSL_RSA_EXPORT_WITH_RC4_40_MD5",
																		"SSL_RSA_WITH_3DES_EDE_CBC_SHA",
																		"SSL_RSA_WITH_DES_CBC_SHA",
																		"SSL_RSA_WITH_RC4_128_MD5",
																		"SSL_RSA_WITH_RC4_128_SHA",
																		"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
																		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
																		"TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
																		"TLS_RSA_WITH_AES_128_CBC_SHA"};
	/* @formatter:on */

	private static final String[] HARDENED_SECURE_FORBIDDEN_CIPHERS = new String[]{"^.*(_(MD5|SHA1)$|RC4_.*$)",
																				   "^(TLS_RSA_WITH_AES.*$)"};
	private static final String[] HARDENED_STRICT_FORBIDDEN_CIPHERS = new String[]{"^.*_AES_128_.*$"};

	private static final String[] HARDENED_SECURE_FORBIDDEN_PROTOCOLS = new String[]{"SSL", "SSLv2", "SSLv3"};
	private static final String[] HARDENED_STRICT_FORBIDDEN_PROTOCOLS = new String[]{"SSLv2Hello", "TLSv1", "TLSv1.1"};
	private static final Logger log = Logger.getLogger(SSLContextContainer.class.getName());

	public enum HARDENED_MODE {
		global,
		relaxed,
		secure,
		strict;

		public static HARDENED_MODE getDefault() {
			return secure;
		}

		static String[] stringValues() {
			return EnumSet.allOf(HARDENED_MODE.class).stream().map(HARDENED_MODE::name).toArray(String[]::new);
		}
	}

	private static String[] HARDENED_RELAXED_CIPHERS;
	private static String[] HARDENED_RELAXED_PROTOCOLS;
	private static String[] HARDENED_SECURE_CIPHERS;
	private static String[] HARDENED_SECURE_PROTOCOLS;
	private static String[] HARDENED_STRICT_CIPHERS;
	private static String[] HARDENED_STRICT_PROTOCOLS;

	static {
		System.setProperty(EPHEMERAL_DH_KEYSIZE_KEY, EPHEMERAL_DH_KEYSIZE_VALUE);
		try {
			final SSLContext sslContext = SSLContext.getDefault();
			SSLEngine tmpEngine = sslContext.createSSLEngine();
			tmpEngine.setUseClientMode(false);
			log.config("Supported protocols: " +
							   markEnabled(tmpEngine.getEnabledProtocols(), tmpEngine.getSupportedProtocols()));
			log.config("Supported ciphers: " +
							   markEnabled(tmpEngine.getEnabledCipherSuites(), tmpEngine.getSupportedCipherSuites()));

			HARDENED_RELAXED_PROTOCOLS = tmpEngine.getEnabledProtocols();
			HARDENED_SECURE_PROTOCOLS = subtractItemsFromCollection(HARDENED_RELAXED_PROTOCOLS,
																	HARDENED_SECURE_FORBIDDEN_PROTOCOLS);
			HARDENED_STRICT_PROTOCOLS = subtractItemsFromCollection(HARDENED_SECURE_PROTOCOLS,
																	HARDENED_STRICT_FORBIDDEN_PROTOCOLS);
			log.config("RELAXED protocols: " + Arrays.toString(HARDENED_RELAXED_PROTOCOLS));
			log.config("SECURE protocols: " + Arrays.toString(HARDENED_SECURE_PROTOCOLS));
			log.config("STRICT protocols: " + Arrays.toString(HARDENED_STRICT_PROTOCOLS));

			HARDENED_RELAXED_CIPHERS = tmpEngine.getEnabledCipherSuites();
			HARDENED_SECURE_CIPHERS = subtractItemsFromCollection(HARDENED_RELAXED_CIPHERS,
																  HARDENED_SECURE_FORBIDDEN_CIPHERS);
			HARDENED_STRICT_CIPHERS = subtractItemsFromCollection(HARDENED_SECURE_CIPHERS,
																  HARDENED_STRICT_FORBIDDEN_CIPHERS);
			log.config("RELAXED ciphers: " + Arrays.toString(HARDENED_RELAXED_CIPHERS));
			log.config("SECURE ciphers: " + Arrays.toString(HARDENED_SECURE_CIPHERS));
			log.config("STRICT ciphers: " + Arrays.toString(HARDENED_STRICT_CIPHERS));
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.WARNING, "Can't determine supported protocols", e);
		}
	}

	@Inject
	protected EventBus eventBus = EventBusFactory.getInstance();
	protected Map<String, SSLHolder> sslContexts = new ConcurrentSkipListMap<>();
	@Inject(nullAllowed = true)
	protected VHostManagerIfc vHostManager = null;
	@ConfigField(desc = "Enabled TLS/SSL ciphers", alias = "tls-enabled-ciphers")
	private String[] enabledCiphers;
	@ConfigField(desc = "Enabled TLS/SSL protocols", alias = "tls-enabled-protocols")
	private String[] enabledProtocols;
	@ConfigField(desc = "TLS/SSL hardened mode", alias = "hardened-mode")
	private HARDENED_MODE hardenedMode = HARDENED_MODE.secure;
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

	private static String[] subtractItemsFromCollection(String[] input, String[] itemsToRemove) {
		return Arrays.stream(input)
				.filter(c -> !Arrays.stream(itemsToRemove)
						.map(Pattern::compile)
						.map(pat -> pat.matcher(c))
						.map(Matcher::matches)
						.anyMatch(e -> e))
				.toArray(String[]::new);
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
	 */
	public SSLContextContainer(CertificateContainerIfc certContainer) {
		this(certContainer, null);
	}

	/**
	 * Constructor used to create instances for every ConnectionManager so that every connection manager can have
	 * different TrustManagers and SSLContext instance will still be cached.
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
											   needClientAuth, getEnabledCiphers(tls_hostname),
											   getEnabledProtocols(tls_hostname));
		return new TLSIO(socketIO, wrapper, byteOrder);
	}

	@Override
	public String[] getEnabledCiphers(String domain) {
		if (enabledCiphers != null && enabledCiphers.length != 0) {
			return enabledCiphers;
		} else if (tlsJdkNssBugWorkaround) {
			return TLS_WORKAROUND_CIPHERS;
		} else {
			HARDENED_MODE mode = getHardenedMode(domain);
			switch (mode) {
				case strict:
					return HARDENED_STRICT_CIPHERS;
				case secure:
					return HARDENED_SECURE_CIPHERS;
				case relaxed:
					return HARDENED_RELAXED_CIPHERS;
				default:
					return null;
			}
		}
	}

	public void setEnabledCiphers(String[] enabledCiphers) {
		if (log.isLoggable(Level.CONFIG)) {
			log.config("Enabled ciphers: " + (enabledCiphers == null ? "default" : Arrays.toString(enabledCiphers)));
		}
		this.enabledCiphers = enabledCiphers;
	}

	@Override
	public String[] getEnabledProtocols(String domain) {
		if (enabledProtocols != null && enabledProtocols.length != 0) {
			return enabledProtocols;
		} else {
			HARDENED_MODE mode = getHardenedMode(domain);
			switch (mode) {
				case strict:
					return HARDENED_STRICT_PROTOCOLS;
				case secure:
					return HARDENED_SECURE_PROTOCOLS;
				case relaxed:
					return HARDENED_RELAXED_PROTOCOLS;
				default:
					return null;
			}
		}
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

	public void setHardenedMode(HARDENED_MODE hardenedMode) {
		this.hardenedMode = hardenedMode;
		if (HARDENED_MODE.relaxed.equals(hardenedMode)) {
			System.clearProperty(EPHEMERAL_DH_KEYSIZE_KEY);
		}
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

	private HARDENED_MODE getHardenedMode(String domain) {
		HARDENED_MODE mode = HARDENED_MODE.secure;
		if (domain != null && vHostManager != null) {
			final VHostItem vHostItem = vHostManager.getVHostItem(domain);
			if (vHostItem != null) {
				HardenedModeVHostItemExtension extension = vHostItem.getExtension(HardenedModeVHostItemExtension.class);
				if (extension != null) {
					mode = extension.getMode();
				}
			}
		}
		log.log(Level.INFO, "Using hardened-mode: {0} for domain: {1}", new String[]{String.valueOf(mode), domain});
		return HARDENED_MODE.global.equals(mode) ? hardenedMode : mode;
	}

	private void invalidateContextHolder(SSLHolder holder, String alias) throws Exception {
		sslContexts.remove(alias);
		createCertificate(alias);
	}

	/**
	 * Method handles <code>CertificateChanged</code> event emitted by CertificateContainer and removes cached instance
	 * of SSLContext for domain for which certificate has changed.
	 */
	@HandleEvent
	private void onCertificateChange(CertificateContainer.CertificateChanged event) {
		String alias = event.getAlias();
		sslContexts.remove(alias);
	}

	private boolean validateDomainCertificate(final SSLHolder holder, final String alias) throws Exception {
		// for self-signed certificates only
		if (holder != null && holder.domainCertificate != null &&
				holder.domainCertificate.getIssuerDN().equals(holder.domainCertificate.getSubjectDN())) {
			try {
				holder.domainCertificate.checkValidity();
			} catch (CertificateException e) {
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Certificate for domain: {0} is not valid, exception: {1}, certificate: {2}",
							new String[]{alias, String.valueOf(e), String.valueOf(holder.domainCertificate)});
				}
				invalidateContextHolder(holder, alias);
				return false;
			}
		}

		return true;
	}

	public static class HardenedModeVHostItemExtension
			extends AbstractVHostItemExtension<HardenedModeVHostItemExtension>
			implements VHostItemExtensionBackwardCompatible<HardenedModeVHostItemExtension> {

		public static final String ID = "hardened-mode";

		private HARDENED_MODE mode = HARDENED_MODE.secure;

		public static HARDENED_MODE parseHardenedModeFromString(String modeString) {
			HARDENED_MODE m = HARDENED_MODE.secure;
			try {
				m = HARDENED_MODE.valueOf(modeString);
			} catch (IllegalArgumentException e) {
				String legacyOption = modeString.trim().toLowerCase();
				if ("true".equals(legacyOption)) {
					m = HARDENED_MODE.secure;
				} else if ("false".equals(legacyOption)) {
					m = HARDENED_MODE.relaxed;
				}
			}
			return m;
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public void initFromElement(Element item) {
			mode = parseHardenedModeFromString(item.getAttributeStaticStr(getId()));
		}

		@Override
		public void initFromCommand(String prefix, Packet packet) throws IllegalArgumentException {
			mode = parseHardenedModeFromString(Command.getFieldValue(packet, prefix));
		}

		public HARDENED_MODE getMode() {
			return mode;
		}

		@Override
		public String toDebugString() {
			return ID + ": " + mode;
		}

		@Override
		public Element toElement() {
			if (mode == null || mode.equals(HARDENED_MODE.getDefault())) {
				return null;
			}

			Element el = new Element(getId());
			el.setAttribute(getId(), String.valueOf(mode));
			return el;
		}

		@Override
		public void addCommandFields(String prefix, Packet packet, boolean forDefault) {
			Element commandEl = packet.getElemChild(Command.COMMAND_EL, Command.XMLNS);
			DataForm.addFieldValue(commandEl, getId(), String.valueOf(getMode()), getId(), HARDENED_MODE.stringValues(),
								   HARDENED_MODE.stringValues(), DataForm.FieldType.ListSingle.value());
		}

		@Override
		public void initFromData(Map<String, Object> data) {
			HARDENED_MODE val = (HARDENED_MODE) data.remove(getId());
			if (val != null) {
				mode = val;
			}
		}

		@Override
		public HardenedModeVHostItemExtension mergeWithDefaults(HardenedModeVHostItemExtension defaults) {
			return mode == HARDENED_MODE.global ? defaults : this;
		}
	}

	@Bean(name = HardenedModeVHostItemExtension.ID, parent = VHostItemExtensionManager.class, active = true)
	public static class HardenedModeVHostItemExtensionProvider
			implements VHostItemExtensionProvider<HardenedModeVHostItemExtension> {

		@Override
		public String getId() {
			return HardenedModeVHostItemExtension.ID;
		}

		@Override
		public Class<HardenedModeVHostItemExtension> getExtensionClazz() {
			return HardenedModeVHostItemExtension.class;
		}
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

