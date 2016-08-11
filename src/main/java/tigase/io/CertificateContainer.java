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

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
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
 *
 * Created by andrzej on 29.02.2016.
 */
@Bean(name = "certificate-container", parent = Kernel.class, exportable = true)
public class CertificateContainer implements CertificateContainerIfc, Initializable {

	private static final Logger log = Logger.getLogger(CertificateContainer.class.getCanonicalName());
	private static final EventBus eventBus = EventBusFactory.getInstance();

	public final static String PER_DOMAIN_CERTIFICATE_KEY = "virt-hosts-cert-";
	public final static String SNI_DISABLE_KEY = "sni-disable";

	private String email = "admin@tigase.org";
	private String o = "Tigase.org";
	private String ou = "XMPP Service";

	private Map<String, KeyManagerFactory> kmfs = new ConcurrentSkipListMap<String, KeyManagerFactory>();
	private KeyManager[] kms = new KeyManager[] { new SniKeyManager() };
	private X509TrustManager[] tms = new X509TrustManager[] { new FakeTrustManager() };
	private KeyStore trustKeyStore = null;

	@ConfigField(desc = "Alias for default certificate")
	private String def_cert_alias = DEFAULT_DOMAIN_CERT_VAL;
	private File[] certsDirs = null;
	private char[] emptyPass = new char[0];

	@ConfigField(desc = "Disable SNI support")
	private boolean sniDisable = false;
	@ConfigField(desc = "Location of server SSL certificates")
	private String[] sslCertsLocation = { SERVER_CERTS_LOCATION_VAL };
	@ConfigField(desc = "Location of trusted certificates")
	private String[] trustedCertsDir = { TRUSTED_CERTS_DIR_VAL };

	@ConfigField(desc = "Custom certificates")
	private Map<String,String> customCerts = new HashMap<>();

	private KeyManagerFactory addCertificateEntry(CertificateEntry entry, String alias, boolean store)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
		KeyStore keys = KeyStore.getInstance("JKS");

		keys.load(null, emptyPass);
		keys.setKeyEntry(alias, entry.getPrivateKey(), emptyPass, CertificateUtil.sort(entry.getCertChain()));


		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

		kmf.init(keys, emptyPass);
		kmfs.put(alias, kmf);

		if (store) {
			CertificateUtil.storeCertificate(new File(certsDirs[0], alias + ".pem").toString(), entry);
		}

		return kmf;
	}

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
			try {
				CertificateEntry entry = CertificateUtil.parseCertificate(new CharArrayReader(pemCert.toCharArray()));

				addCertificateEntry(entry, alias, saveToDisk);

				eventBus.fire(new CertificateChanged(alias));
			} catch (Exception ex) {
				throw new CertificateParsingException("Problem adding a new certificate.", ex);
			}
		}
	}

	@Override
	public KeyManager[] createCertificate(String alias) throws NoSuchAlgorithmException, CertificateException, SignatureException,
			NoSuchProviderException, InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException {
		KeyPair keyPair = CertificateUtil.createKeyPair(1024, "secret");
		X509Certificate cert = CertificateUtil.createSelfSignedCertificate(email, alias, ou, o, null, null, null,
				keyPair);
		CertificateEntry entry = new CertificateEntry();

		entry.setPrivateKey(keyPair.getPrivate());
		entry.setCertChain(new Certificate[]{cert});
		KeyManager[] kms = addCertificateEntry(entry, alias, true).getKeyManagers();
		log.log(Level.WARNING, "Auto-generated certificate for domain: {0}", alias);
		return kms;
	}

	@Override
	public String getDefCertAlias() {
		return def_cert_alias;
	}

	@Override
	public KeyManager[] getKeyManagers(String hostname) {
		if (hostname == null && !sniDisable)
			return kms;

		String alias = hostname;
		if (alias == null)
			alias = getDefCertAlias();

		KeyManagerFactory kmf = SSLContextContainerAbstract.find(kmfs, alias);
		return (kmf == null) ? null : kmf.getKeyManagers();
	}

	private Map<String, File> findPredefinedCertificates(Map<String, Object> params) {
		final Map<String, File> result = new HashMap<String, File>();
		if (params == null)
			return result;

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
					log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}", new Object[] { alias,
							entry.getValue() });
				} catch (Exception ex) {
					log.log(Level.WARNING, "Cannot load certficate from file: " + entry.getValue(), ex);
				}
			}

			for (String pemDir : pemDirs) {
				log.log(Level.CONFIG, "Loading server certificates from PEM directory: {0}", pemDir);
				certsDirs[++certsDirsIdx] = new File(pemDir);

				for (File file : certsDirs[certsDirsIdx].listFiles(new PEMFileFilter())) {
					try {
						CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
						String alias = file.getName();
						if (alias.endsWith(".pem"))
							alias = alias.substring(0, alias.length() - 4);

						addCertificateEntry(certEntry, alias, false);
						log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}", new Object[] { alias,
								file });
					} catch (Exception ex) {
						log.log(Level.WARNING, "Cannot load certficate from file: " + file, ex);
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "There was a problem initializing SSL certificates.", ex);
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

	private void loadTrustedCerts(String[] trustLocations) {
		int counter = 0;
		long start = System.currentTimeMillis();

		List<X509Certificate> acceptedIssuers = new ArrayList<>(200);
		try {
			trustKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustKeyStore.load(null, emptyPass);

			final File trustStoreFile = new File(System.getProperty("java.home")
					+ "/lib/security/cacerts".replace('/', File.separatorChar));
			final File userStoreFile = new File("~/.keystore");

			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Looking for trusted certs in: {0}", trustStoreFile);

			if (trustStoreFile.exists()) {
				log.log(Level.CONFIG, "Loading trustKeyStore from location: {0}", trustStoreFile);
				InputStream in = new FileInputStream(trustStoreFile);
				trustKeyStore.load(in, null);
				in.close();
			}

			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Looking for trusted certs in: {0}", userStoreFile);

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
							log.log(Level.WARNING, "Problem loading certificate from file: {0}", file);
						}
					}
				}
			}

		} catch (Exception ex) {
			log.log(Level.WARNING, "An error loading trusted certificates", ex);
		}

		try {
			if (!trustKeyStore.aliases().hasMoreElements()) {
				log.log(Level.CONFIG, "No Trusted Anchors!!! Creating temporary trusted CA cert!");
				KeyPair keyPair = CertificateUtil.createKeyPair(1024, "secret");
				X509Certificate cert = CertificateUtil.createSelfSignedCertificate("fake_local@tigase", "fake one", "none",
						"none", "none", "none", "US", keyPair);
				trustKeyStore.setCertificateEntry("generated fake CA", cert);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't generate fake trusted CA certificate", e);
		}

		tms = new X509TrustManager[] { new FakeTrustManager(
				acceptedIssuers.toArray(new X509Certificate[acceptedIssuers.size()])) };

		long seconds = (System.currentTimeMillis() - start) / 1000;

		log.log(Level.CONFIG, "Loaded {0} trust certificates, it took {1} seconds.", new Object[] { counter, seconds });
	}

	@Override
	public void initialize() {
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
					log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}", new Object[] { alias,
							entry.getValue() });
				} catch (Exception ex) {
					log.log(Level.WARNING, "Cannot load certficate from file: " + entry.getValue(), ex);
				}
			}

			for (String pemDir : pemDirs) {
				log.log(Level.CONFIG, "Loading server certificates from PEM directory: {0}", pemDir);
				certsDirs[++certsDirsIdx] = new File(pemDir);

				for (File file : certsDirs[certsDirsIdx].listFiles(new PEMFileFilter())) {
					try {
						CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
						String alias = file.getName();
						if (alias.endsWith(".pem"))
							alias = alias.substring(0, alias.length() - 4);

						addCertificateEntry(certEntry, alias, false);
						log.log(Level.CONFIG, "Loaded server certificate for domain: {0} from file: {1}", new Object[] { alias,
								file });
					} catch (Exception ex) {
						log.log(Level.WARNING, "Cannot load certficate from file: " + file, ex);
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "There was a problem initializing SSL certificates.", ex);
		}

		// It may take a while, let's do it in background
		new Thread() {
			@Override
			public void run() {
				loadTrustedCerts(trustedCertsDir);
			}
		}.start();

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

	private class PEMFileFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			if (pathname.isFile()
					&& (pathname.getName().endsWith(".pem") || pathname.getName().endsWith(".PEM")
					|| pathname.getName().endsWith(".crt") || pathname.getName().endsWith(".CRT")
					|| pathname.getName().endsWith(".cer") || pathname.getName().endsWith(".CER"))) {
				return true;
			}

			return false;
		}
	}

	private static class FakeTrustManager implements X509TrustManager {
		private X509Certificate[] issuers = null;

		// ~--- constructors
		// -------------------------------------------------------

		/**
		 * Constructs ...
		 *
		 */
		FakeTrustManager() {
			this(new X509Certificate[0]);
		}

		/**
		 * Constructs ...
		 *
		 *
		 * @param ai
		 */
		FakeTrustManager(X509Certificate[] ai) {
			issuers = ai;
		}

		// ~--- methods

		// Implementation of javax.net.ssl.X509TrustManager

		@Override
		public void checkClientTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {
		}

		// ~--- get methods

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return issuers;
		}
	}

	private class SniKeyManager extends X509ExtendedKeyManager {

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
		 * Method retrieves requested server name from ExtendedSSLSession and
		 * uses it to return proper alias for server certificate
		 *
		 * @param session
		 * @return
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
			if (hostname != null && (getCertificateChain(hostname) != null
					&& getPrivateKey(hostname) != null)) {
				return hostname;
			} else {
				return def_cert_alias;
			}
		}

		/**
		 * Using passed alias method searches for proper KeyManagerFactory to
		 * return proper certificate chain for alias
		 *
		 * @param alias
		 * @return
		 */
		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			if (alias == null)
				alias = def_cert_alias;
			KeyManagerFactory kmf = SSLContextContainerAbstract.find(kmfs, alias);
			if (kmf == null) {
				alias = def_cert_alias;
				kmf = SSLContextContainer.find(kmfs, alias);
			}
			return ((X509KeyManager) kmf.getKeyManagers()[0]).getCertificateChain(alias);
		}

		/**
		 * Using passed alias method searches for proper KeyManagerFactory to
		 * return proper private key for alias
		 *
		 * @param alias
		 * @return
		 */
		@Override
		public PrivateKey getPrivateKey(String alias) {
			if (alias == null)
				alias = def_cert_alias;
			KeyManagerFactory kmf = SSLContextContainerAbstract.find(kmfs, alias);
			if (kmf == null) {
				alias = def_cert_alias;
				kmf = SSLContextContainer.find(kmfs, alias);
			}
			return ((X509KeyManager) kmf.getKeyManagers()[0]).getPrivateKey(alias);
		}

	}

}
