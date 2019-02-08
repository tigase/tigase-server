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

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.io.SSLContextContainerIfc.*;

/**
 * Class used to keep SSL certificates loaded in memory. To get instance use getter from TLSUtil class.
 * <br>
 * Created by andrzej on 29.02.2016.
 */
@Bean(name = "certificate-container", parent = Kernel.class, active = true, exportable = true)
public class CertificateContainer
		implements CertificateContainerIfc, Initializable, UnregisterAware {

	public final static String PER_DOMAIN_CERTIFICATE_KEY = "virt-hosts-cert-";
	public final static String SNI_DISABLE_KEY = "sni-disable";
	private static final Logger log = Logger.getLogger(CertificateContainer.class.getCanonicalName());
	private static final EventBus eventBus = EventBusFactory.getInstance();
	private Map<String, CertificateEntry> cens = new ConcurrentSkipListMap<String, CertificateEntry>();
	private File[] certsDirs = null;
	@ConfigField(desc = "Custom certificates", alias = "custom-certificates")
	private Map<String, String> customCerts = new HashMap<>();
	@ConfigField(desc = "Alias for default certificate", alias = DEFAULT_DOMAIN_CERT_KEY)
	private String def_cert_alias = DEFAULT_DOMAIN_CERT_VAL;
	private String email = "admin@tigase.org";
	private char[] emptyPass = new char[0];
	private Map<String, KeyManagerFactory> kmfs = new ConcurrentSkipListMap<String, KeyManagerFactory>();
	private KeyManager[] kms = new KeyManager[]{new SniKeyManager()};
	private String o = "Tigase.org";
	private String ou = "XMPP Service";
	@ConfigField(desc = "Disable SNI support", alias = SNI_DISABLE_KEY)
	private boolean sniDisable = false;
	@ConfigField(desc = "Location of server SSL certificates", alias = SERVER_CERTS_LOCATION_KEY)
	private String[] sslCertsLocation = {SERVER_CERTS_LOCATION_VAL};
	private X509TrustManager[] tms = new X509TrustManager[]{new FakeTrustManager()};
	private KeyStore trustKeyStore = null;
	@ConfigField(desc = "Location of trusted certificates", alias = TRUSTED_CERTS_DIR_KEY)
	private String[] trustedCertsDir = {TRUSTED_CERTS_DIR_VAL};

	@Override
	public void addCertificates(Map<String, String> params) throws CertificateParsingException {
		String pemCert = params.get(PEM_CERTIFICATE_KEY);
		String saveToDiskVal = params.get(CERT_SAVE_TO_DISK_KEY);
		boolean saveToDisk = (saveToDiskVal != null) && saveToDiskVal.equalsIgnoreCase("true");
		final String alias = params.get(CERT_ALIAS_KEY);

		if (alias == null) {
			throw new RuntimeException("Certificate alias must be specified");
		}

		if (pemCert != null) {
			addCertificate(alias, pemCert, saveToDisk, true);
		}
	}

	@Override
	public KeyManager[] createCertificate(String alias)
			throws NoSuchAlgorithmException, CertificateException, SignatureException, NoSuchProviderException,
				   InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException {
		final KeyManagerFactory keyManagerFactory = createCertificateKmf(alias);
		KeyManager[] kms = keyManagerFactory.getKeyManagers();
		log.log(Level.WARNING, "Auto-generated certificate for domain: {0}", alias);
		return kms;
	}

	@Override
	public String getDefCertAlias() {
		return def_cert_alias;
	}

	public CertificateEntry getCertificateEntry(String hostname) {
		String alias = hostname;
		if (alias == null) {
			alias = getDefCertAlias();
		}

		CertificateEntry c = SSLContextContainerAbstract.find(cens, alias);
		return c;
	}

	@Override
	public KeyManager[] getKeyManagers(String hostname) {
		if (hostname == null && !sniDisable) {
			return kms;
		}

		String alias = hostname;
		if (alias == null) {
			alias = getDefCertAlias();
		}

		KeyManagerFactory kmf = SSLContextContainerAbstract.find(kmfs, alias);
		return (kmf == null) ? null : kmf.getKeyManagers();
	}

	@Override
	public TrustManager[] getTrustManagers() {
		return tms;
	}

	@Override
	public KeyStore getTrustStore() {
		return trustKeyStore;
	}

	@Override
	public void init(Map<String, Object> params) {
		try {
			def_cert_alias = (String) params.get(DEFAULT_DOMAIN_CERT_KEY);

			if (def_cert_alias == null) {
				def_cert_alias = DEFAULT_DOMAIN_CERT_VAL;
			}

			if (params.containsKey(SNI_DISABLE_KEY)) {
				sniDisable = (Boolean) params.get(SNI_DISABLE_KEY);
			} else {
				sniDisable = false;
			}

			String pemD = (String) params.get(SERVER_CERTS_LOCATION_KEY);

			if (pemD == null) {
				pemD = SERVER_CERTS_LOCATION_VAL;
			}

			String[] pemDirs = pemD.split(",");

			certsDirs = new File[pemDirs.length];

			int certsDirsIdx = -1;

			Map<String, File> predefined = findPredefinedCertificates(params);
			log.log(Level.CONFIG, "Loading predefined server certificates");
			for (final Map.Entry<String, File> entry : predefined.entrySet()) {
				try {
					CertificateEntry certEntry = CertificateUtil.loadCertificate(entry.getValue());
					String alias = entry.getKey();
					addCertificateEntry(certEntry, alias, false);
					log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}",
							new Object[]{alias, entry.getValue()});
				} catch (Exception ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Cannot load certficate from file: " + entry.getValue(), ex);
					}
					log.log(Level.WARNING, "Cannot load certficate from file: " + entry.getValue());
				}
			}

			for (String pemDir : pemDirs) {
				log.log(Level.CONFIG, "Loading server certificates from PEM directory: {0}", pemDir);
				final File directory = new File(pemDir);
				if (!directory.exists()) {
					continue;
				}
				certsDirs[++certsDirsIdx] = directory;

				for (File file : certsDirs[certsDirsIdx].listFiles(new PEMFileFilter())) {
					try {
						CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
						String alias = file.getName();
						if (alias.endsWith(".pem")) {
							alias = alias.substring(0, alias.length() - 4);
						}

						addCertificateEntry(certEntry, alias, false);
						log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}",
								new Object[]{alias, file});
					} catch (Exception ex) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Cannot load certficate from file: " + file, ex);
						}
						log.log(Level.WARNING, "Cannot load certficate from file: " + file);
					}
				}
			}
		} catch (Exception ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "There was a problem initializing SSL certificates.", ex);
			}
			log.log(Level.WARNING, "There was a problem initializing SSL certificates.");
		}

		String trustLoc = (String) params.get(TRUSTED_CERTS_DIR_KEY);

		if (trustLoc == null) {
			trustLoc = TRUSTED_CERTS_DIR_VAL;
		}

		final String[] trustLocations = trustLoc.split(",");

		// It may take a while, let's do it in background
		new Thread() {
			@Override
			public void run() {
				loadTrustedCerts(trustLocations);
			}
		}.start();
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		try {
			String[] pemDirs = sslCertsLocation;
			certsDirs = new File[pemDirs.length];

			int certsDirsIdx = -1;

			Map<String, String> predefined = customCerts;
			log.log(Level.CONFIG, "Loading predefined server certificates");
			for (final Map.Entry<String, String> entry : predefined.entrySet()) {
				try {
					File file = new File(entry.getValue());
					CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
					String alias = entry.getKey();
					addCertificateEntry(certEntry, alias, false);
					log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}",
							new Object[]{alias, entry.getValue()});
				} catch (Exception ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "" + " from file: " + entry.getValue(), ex);
					}
					log.log(Level.WARNING, "" + " from file: " + entry.getValue());
				}
			}

			for (String pemDir : pemDirs) {
				log.log(Level.CONFIG, "Loading server certificates from PEM directory: {0}", pemDir);
				certsDirs[++certsDirsIdx] = new File(pemDir);

				for (File file : certsDirs[certsDirsIdx].listFiles(new PEMFileFilter())) {
					try {
						CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
						String alias = file.getName();
						if (alias.endsWith(".pem")) {
							alias = alias.substring(0, alias.length() - 4);
						}

						addCertificateEntry(certEntry, alias, false);
						log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}",
								new Object[]{alias, file});
					} catch (Exception ex) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Cannot load certficate from file: " + file, ex);
						}
						log.log(Level.WARNING, "Cannot load certficate from file: " + file + ": " + ex.getLocalizedMessage());
					}
				}
			}
		} catch (Exception ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "There was a problem initializing SSL certificates.", ex);
			}
			log.log(Level.WARNING, "There was a problem initializing SSL certificates.");
		}

		// It may take a while, let's do it in background
		new Thread() {
			@Override
			public void run() {
				loadTrustedCerts(trustedCertsDir);
			}
		}.start();

	}

	private KeyManagerFactory addCertificateEntry(CertificateEntry entry, String alias, boolean store)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
				   UnrecoverableKeyException {
		KeyStore keys = KeyStore.getInstance("JKS");

		keys.load(null, emptyPass);
		keys.setKeyEntry(alias, entry.getPrivateKey(), emptyPass, CertificateUtil.sort(entry.getCertChain()));

		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

		kmf.init(keys, emptyPass);
		kmfs.put(alias, kmf);
		cens.put(alias, entry);

		if (store) {
			CertificateUtil.storeCertificate(new File(certsDirs[0], alias + ".pem").toString(), entry);
		}

		return kmf;
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@HandleEvent
	public void certificateChange(CertificateChange event) {
		if (event.isLocal()) {
			return;
		}

		try {
			addCertificate(event.getAlias(), event.getPemCertificate(), event.isSaveToDisk(), false);
		} catch (CertificateParsingException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Failed to update certificate for " + event.getAlias(), ex);
			}
			log.log(Level.WARNING, "Failed to update certificate for " + event.getAlias());
		}
	}

	private void addCertificate(String alias, String pemCert, boolean saveToDisk, boolean notifyCluster) throws CertificateParsingException {
		try {
			CertificateEntry entry = CertificateUtil.parseCertificate(new CharArrayReader(pemCert.toCharArray()));

			addCertificateEntry(entry, alias, saveToDisk);
			if (notifyCluster) {
				eventBus.fire(new CertificateChange(alias, pemCert, saveToDisk));
			}

			eventBus.fire(new CertificateChanged(alias));
		} catch (Exception ex) {
			throw new CertificateParsingException("Problem adding a new certificate.", ex);
		}
	}

	private KeyManagerFactory createCertificateKmf(String alias)
			throws NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException,
				   NoSuchProviderException, SignatureException, KeyStoreException, UnrecoverableKeyException {
		CertificateEntry entry = CertificateUtil.createSelfSignedCertificate(email, alias, ou, o, null, null, null,
																			 () -> CertificateUtil.createKeyPair(1024,
																												 "secret"));
		return addCertificateEntry(entry, alias, true);
	}

	private Map<String, File> findPredefinedCertificates(Map<String, Object> params) {
		final Map<String, File> result = new HashMap<String, File>();
		if (params == null) {
			return result;
		}

		Iterator<String> it = params.keySet().iterator();
		while (it.hasNext()) {
			String t = it.next();
			if (t.startsWith(PER_DOMAIN_CERTIFICATE_KEY)) {
				String domainName = t.substring(PER_DOMAIN_CERTIFICATE_KEY.length());
				File f = new File(params.get(t).toString());

				result.put(domainName, f);
			}
		}

		return result;

	}

	private void loadTrustedCerts(String[] trustLocations) {
		int counter = 0;
		long start = System.currentTimeMillis();

		List<X509Certificate> acceptedIssuers = new ArrayList<>(200);
		try {
			trustKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustKeyStore.load(null, emptyPass);

			final File trustStoreFile = new File(
					System.getProperty("java.home") + "/lib/security/cacerts".replace('/', File.separatorChar));
			final File userStoreFile = new File("~/.keystore");

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Looking for trusted certs in: {0}", trustStoreFile);
			}

			if (trustStoreFile.exists()) {
				log.log(Level.CONFIG, "Loading trustKeyStore from location: {0}", trustStoreFile);
				InputStream in = new FileInputStream(trustStoreFile);
				trustKeyStore.load(in, null);
				in.close();
			}

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Looking for trusted certs in: {0}", userStoreFile);
			}

			if (userStoreFile.exists()) {
				log.log(Level.CONFIG, "Loading trustKeyStore from location: {0}", userStoreFile);
				InputStream in = new FileInputStream(userStoreFile);
				trustKeyStore.load(in, null);
				in.close();
			}

			log.log(Level.CONFIG, "Loading trustKeyStore from locations: {0}", Arrays.toString(trustLocations));
			for (String location : trustLocations) {
				File root = new File(location);
				File[] files = root.listFiles(new PEMFileFilter());

				if (files != null) {
					for (File file : files) {
						try {
							CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
							Certificate[] chain = certEntry.getCertChain();

							if (chain != null) {
								for (Certificate cert : chain) {
									if (cert instanceof X509Certificate) {
										X509Certificate crt = (X509Certificate) cert;
										String alias = crt.getSubjectX500Principal().getName();

										trustKeyStore.setCertificateEntry(alias, crt);
										acceptedIssuers.add(crt);
										log.log(Level.FINEST, "Imported certificate: {0}", alias);
										++counter;
									}
								}
							}
						} catch (Exception e) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Problem loading certificate from file: " + file, e);
							}
							log.log(Level.WARNING, "Problem loading certificate from file: {0}", file);
						}
					}
				}
			}

		} catch (Exception ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "An error loading trusted certificates", ex);
			}
			log.log(Level.WARNING, "An error loading trusted certificates");
		}

		try {
			if (!trustKeyStore.aliases().hasMoreElements()) {
				log.log(Level.CONFIG, "No Trusted Anchors!!! Creating temporary trusted CA cert!");
				CertificateEntry entry = CertificateUtil.createSelfSignedCertificate("fake_local@tigase", "fake one",
																					 "none", "none", "none", "none",
																					 "US",
																					 () -> CertificateUtil.createKeyPair(
																							 1024, "secret"));
				trustKeyStore.setCertificateEntry("generated fake CA", entry.getCertChain()[0]);
			}
		} catch (Exception e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Can't generate fake trusted CA certificate", e);
			}
			log.log(Level.WARNING, "Can't generate fake trusted CA certificate");
		}

		tms = new X509TrustManager[]{
				new FakeTrustManager(acceptedIssuers.toArray(new X509Certificate[acceptedIssuers.size()]))};

		long seconds = (System.currentTimeMillis() - start) / 1000;

		log.log(Level.CONFIG, "Loaded {0} trust certificates, it took {1} seconds.", new Object[]{counter, seconds});
	}

	private static class FakeTrustManager
			implements X509TrustManager {

		private X509Certificate[] issuers = null;

		// ~--- constructors
		// -------------------------------------------------------


		FakeTrustManager() {
			this(new X509Certificate[0]);
		}


		FakeTrustManager(X509Certificate[] ai) {
			issuers = ai;
		}

		// Implementation of javax.net.ssl.X509TrustManager

		@Override
		public void checkClientTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return issuers;
		}
	}

	public static class CertificateChange
			implements Serializable {

		private String alias;
		private String pemCert;
		private boolean saveToDisk;
		private transient boolean local = false;

		/**
		 * Empty constructor to be able to serialize/deserialize event
		 */
		public CertificateChange() {
		}

		public CertificateChange(String alias, String pemCert, boolean saveToDisk) {
			this.alias = alias;
			this.pemCert = pemCert;
			this.saveToDisk = saveToDisk;
			this.local = true;
		}

		public String getAlias() {
			return alias;
		}

		public String getPemCertificate() {
			return pemCert;
		}

		public boolean isLocal() {
			return local;
		}

		public boolean isSaveToDisk() {
			return saveToDisk;
		}
	}
	
	public class CertificateChanged {

		private String alias;

		public CertificateChanged(String alias) {
			this.alias = alias;
		}

		public String getAlias() {
			return alias;
		}
	}

	private class PEMFileFilter
			implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && (pathname.getName().endsWith(".pem") || pathname.getName().endsWith(".PEM") ||
					pathname.getName().endsWith(".crt") || pathname.getName().endsWith(".CRT") ||
					pathname.getName().endsWith(".cer") || pathname.getName().endsWith(".CER"));

		}
	}

	private class SniKeyManager
			extends X509ExtendedKeyManager {

		@Override
		public String[] getClientAliases(String string, Principal[] prncpls) {
			return null;
		}

		@Override
		public String chooseClientAlias(String[] strings, Principal[] prncpls, Socket socket) {
			return null;
		}

		@Override
		public String[] getServerAliases(String string, Principal[] prncpls) {
			Set<String> aliases = kmfs.keySet();
			return aliases.toArray(new String[aliases.size()]);
		}

		@Override
		public String chooseServerAlias(String string, Principal[] prncpls, Socket socket) {
			if (socket instanceof SSLSocket) {
				ExtendedSSLSession session = (ExtendedSSLSession) ((SSLSocket) socket).getSession();

				return chooseServerAlias(session);
			}
			return null;
		}

		@Override
		public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
			ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();

			return chooseServerAlias(session);
		}

		/**
		 * Using passed alias method searches for proper KeyManagerFactory to return proper certificate chain for alias
		 */
		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			KeyManagerFactory kmf;
			if (alias == null) {
				alias = def_cert_alias;
			}
			kmf = SSLContextContainerAbstract.find(kmfs, alias);
			if (kmf == null) {
				alias = def_cert_alias;
				kmf = SSLContextContainer.find(kmfs, alias);
			}
			// we still don't have kmf so it's unknown domain, we should create and use default
			if (kmf == null) {
				try {
					kmf = createCertificateKmf(alias);
				} catch (Exception e) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Failed to create certificate for alias: " + alias, e);
					}
					log.log(Level.WARNING, "Failed to create certificate for alias: " + alias);
				}
			}

			return kmf != null ? ((X509KeyManager) kmf.getKeyManagers()[0]).getCertificateChain(alias) : null;
		}

		/**
		 * Using passed alias method searches for proper KeyManagerFactory to return proper private key for alias
		 */
		@Override
		public PrivateKey getPrivateKey(String alias) {
			KeyManagerFactory kmf;
			if (alias == null) {
				alias = def_cert_alias;
			}
			kmf = SSLContextContainerAbstract.find(kmfs, alias);
			if (kmf == null) {
				alias = def_cert_alias;
				kmf = SSLContextContainer.find(kmfs, alias);
			}
			// we still don't have kmf so it's unknown domain, we should create and use default
			if (kmf == null) {
				try {
					kmf = createCertificateKmf(alias);
				} catch (Exception e) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Failed to create certificate for alias: " + alias, e);
					}
					log.log(Level.WARNING, "Failed to create certificate for alias: " + alias);
				}
			}

			return kmf != null ? ((X509KeyManager) kmf.getKeyManagers()[0]).getPrivateKey(alias) : null;
		}

		/**
		 * Method retrieves requested server name from ExtendedSSLSession and uses it to return proper alias for server
		 * certificate
		 */
		private String chooseServerAlias(ExtendedSSLSession session) {
			// Pick first SNIHostName in the list of SNI names.
			String hostname = null;
			for (SNIServerName name : session.getRequestedServerNames()) {
				if (name.getType() == StandardConstants.SNI_HOST_NAME) {
					hostname = ((SNIHostName) name).getAsciiName();
					break;
				}
			}

			// If we got given a hostname over SNI, check if we have a cert and
			// key for that hostname. If so, we use it.
			// Otherwise, we fall back to the default certificate.
			if (hostname != null && (getCertificateChain(hostname) != null && getPrivateKey(hostname) != null)) {
				return hostname;
			} else {
				return def_cert_alias;
			}
		}

	}

}
