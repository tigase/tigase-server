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

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.db.comp.RepositoryChangeListenerIfc;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusEvent;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.io.repo.CertificateItem;
import tigase.io.repo.CertificateRepository;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
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
		implements CertificateContainerIfc,
				   Initializable,
				   UnregisterAware,
				   RegistrarBean,
				   RepositoryChangeListenerIfc<CertificateItem> {

	public final static String PER_DOMAIN_CERTIFICATE_KEY = "virt-hosts-cert-";
	public final static String SNI_DISABLE_KEY = "sni-disable";
	private static final Logger log = Logger.getLogger(CertificateContainer.class.getCanonicalName());
	private static final EventBus eventBus = EventBusFactory.getInstance();
	@Inject(nullAllowed = true)
	CertificateRepository repository;
	private Map<String, CertificateEntry> cens = new ConcurrentSkipListMap<>();
	@ConfigField(desc = "Custom certificates", alias = "custom-certificates")
	private Map<String, String> customCerts = new HashMap<>();
	@ConfigField(desc = "Alias for default certificate", alias = DEFAULT_DOMAIN_CERT_KEY)
	private String def_cert_alias = DEFAULT_DOMAIN_CERT_VAL;
	private File defaultCertDirectory = null;
	private String email = "admin@tigase.org";
	private char[] emptyPass = new char[0];
	@ConfigField(desc = "Whether generated certificate should be wildcard")
	private boolean generateWildcardCertificate = true;
	private Map<String, KeyManagerFactory> kmfs = new ConcurrentSkipListMap<String, KeyManagerFactory>();
	private KeyManager[] kms = new KeyManager[]{new SniKeyManager()};
	private String o = "Tigase.org";
	private String ou = "XMPP Service";
	@ConfigField(desc = "Remove root CA (efectively self-signed) certificate from chain")
	private boolean removeRootCACertificate = true;
	@ConfigField(desc = "Disable SNI support", alias = SNI_DISABLE_KEY)
	private boolean sniDisable = false;
	@ConfigField(desc = "Location of server SSL certificates", alias = SERVER_CERTS_LOCATION_KEY)
	private String[] sslCertsLocation = {SERVER_CERTS_LOCATION_VAL};
	private X509TrustManager[] tms = new X509TrustManager[]{new FakeTrustManager()};
	private KeyStore trustKeyStore = null;
	@ConfigField(desc = "Location of trusted certificates", alias = TRUSTED_CERTS_DIR_KEY)
	private String[] trustedCertsDir = {TRUSTED_CERTS_DIR_VAL};

	private static Set<String> getAllCNames(Certificate certificate) {
		Set<String> altDomains = new TreeSet<>();
		if (certificate instanceof X509Certificate) {
			X509Certificate certX509 = (X509Certificate) certificate;
			String certCName = CertificateUtil.getCertCName(certX509);
			if (certCName != null) {
				altDomains.add(certCName);
			}
			List<String> certAltCName = CertificateUtil.getCertAltCName(certX509);
			altDomains.addAll(certAltCName);
		}
		return altDomains;
	}

	public void setRepository(CertificateRepository repository) {
		if (repository != null) {
			log.log(Level.WARNING,
					"CertificateRepository configured! No certificate will be loaded from the local filesystem!");
		}
		this.repository = repository;
	}

	@Override
	public void addCertificates(Map<String, String> params) throws CertificateParsingException {
		String pemCert = params.get(PEM_CERTIFICATE_KEY);
		String saveToDiskVal = params.get(CERT_SAVE_TO_DISK_KEY);
		boolean saveToDisk = (saveToDiskVal != null) && saveToDiskVal.equalsIgnoreCase("true");

		String useAsDefaultVal = params.get(DEFAULT_DOMAIN_CERT_KEY);
		boolean useAsDefault = (useAsDefaultVal != null) && useAsDefaultVal.equalsIgnoreCase("true");

		final String alias = params.get(CERT_ALIAS_KEY);

		if (alias == null) {
			throw new RuntimeException("Certificate alias must be specified");
		}

		if (pemCert != null) {
			addCertificate(alias, pemCert, saveToDisk, true);
			if (useAsDefault) {
				addCertificate(def_cert_alias, pemCert, saveToDisk, true);
			}
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
		String alias = hostname == null ? getDefCertAlias() : hostname.toLowerCase();
		return SSLContextContainerAbstract.find(cens, alias);
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

			if (repository != null) {
				loadCertificatesFromRepository();
			} else {

				String pemD = (String) params.get(SERVER_CERTS_LOCATION_KEY);

				if (pemD == null) {
					pemD = SERVER_CERTS_LOCATION_VAL;
				}

				String[] pemDirs = pemD.split(",");

				defaultCertDirectory = getDefaultCertDirectory(pemDirs);

				// we should first load available files and then override it with user configured mapping
				loadCertificatesFromDirectories(pemDirs, false);

				Map<String, String> predefined = findPredefinedCertificates(params);
				loadPredefinedCertificates(predefined, false);
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
	public void itemAdded(CertificateItem item) {
		try {
			addCertificateEntry(item.getCertificateEntry(), item.getAlias(), false);
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem adding certificate while reloading from repository", e);
		}
	}

	@Override
	public void itemUpdated(CertificateItem item) {
		try {
			addCertificateEntry(item.getCertificateEntry(), item.getAlias(), false);
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem adding certificate while reloading from repository", e);
		}
	}

	@Override
	public void itemRemoved(CertificateItem item) {
		kmfs.remove(item.getAlias());
		cens.remove(item.getAlias());
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		try {
			if (repository != null) {
				loadCertificatesFromRepository();
				if (repository.isMoveFromFilesystemToRepository()) {
					loadCertificatesFromDirectories(sslCertsLocation, true);
					loadPredefinedCertificates(customCerts, true);
					for (Map.Entry<String, CertificateEntry> certificates : cens.entrySet()) {
						repository.addItem(new CertificateItem(certificates.getKey(), certificates.getValue()));
					}
				}
			} else {
				String[] pemDirs = sslCertsLocation;
				defaultCertDirectory = getDefaultCertDirectory(pemDirs);
				// we should first load available files and then override it with user configured mapping
				loadCertificatesFromDirectories(pemDirs, false);
				loadPredefinedCertificates(customCerts, false);
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

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@HandleEvent
	public void certificateChange(CertificateChange event) {
		if (event.isLocal()) {
			return;
		}

		addCertificate(event.getAlias(), event.getPemCertificate(), event.isSaveToDisk());
		if (repository != null) {
			repository.reload();
		}
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	KeyManagerFactory addCertificateEntry(CertificateEntry entry, String alias, boolean store)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
				   UnrecoverableKeyException {
		log.log(Level.FINEST, "Adding certificate entry for alias: {0}. Saving to disk: {1}, entry: {2}",
				new Object[]{alias, store, entry});
		PrivateKey privateKey = entry.getPrivateKey();
		Certificate[] certChain = CertificateUtil.sort(entry.getCertChain());

		if (removeRootCACertificate) {
			log.log(Level.FINEST, "Removing RootCA from certificate chain.");
			certChain = CertificateUtil.removeRootCACertificate(certChain);
		}

		KeyManagerFactory kmf = getKeyManagerFactory(alias, privateKey, certChain);
		kmfs.put(alias, kmf);
		cens.put(alias, entry);

		if (!def_cert_alias.equals(alias)) {
			Optional<Certificate> certificate = entry.getCertificate();
			if (certificate.isPresent()) {
				Set<String> domains = getAllCNames(certificate.get());
				log.log(Level.FINEST,
						"Certificate present with domains: {0}. Replacing in collections, kmfs domains: {1}, cens domains: {2}. Certificate: {3}",
						new Object[]{domains, kmfs.keySet(), cens.keySet(), certificate.get()});
				SSLContextContainerAbstract.removeMatchedDomains(kmfs, domains);
				SSLContextContainerAbstract.removeMatchedDomains(cens, domains);
				log.log(Level.FINEST,
						"Certificate present with domains: {0}. Collections after domain removal, kmfs domains: {1}, cens domains: {2}",
						new Object[]{domains, kmfs.keySet(), cens.keySet()});
				for (String domain : domains) {
					kmf = getKeyManagerFactory(domain, privateKey, certChain);
					kmfs.put(domain, kmf);
					cens.put(domain, entry);
				}
			}
		}

		if (store) {
			String filename = alias.startsWith("*.") ? alias.substring(2) : alias;
			if (repository != null) {
				final CertificateItem item = new CertificateItem(filename, entry);
				log.log(Level.FINEST, "Storing to repository, certificate entry for alias: {0} with SerialNumber: {1}",
						new Object[]{alias, item.getSerialNumber()});
				repository.addItem(item);
			} else {
				storeCertificateToFile(entry, filename);
			}
		}

		return kmf;
	}

	private void loadCertificatesFromRepository() {
		if (repository != null) {
			for (CertificateItem item : repository.allItems()) {
				CertificateEntry certificate = item.getCertificateEntry();
				String alias = item.getAlias();
				try {
					addCertificateEntry(certificate, alias, false);
				} catch (Exception ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Cannot load certficate from repository: " + item.getKey(), ex);
					}
					log.log(Level.WARNING, "Cannot load certficate from repository: " + item.getKey());
				}
			}
		}
	}

	private void loadCertificatesFromDirectories(String[] pemDirs, boolean moveFileToBackup) {
		for (String pemDir : pemDirs) {
			log.log(Level.CONFIG, "Loading server certificates from PEM directory: {0}", pemDir);
			final File directory = new File(pemDir);
			if (!directory.exists()) {
				continue;
			}

			final File[] files = directory.listFiles(new PEMFileFilter());
			// let's add certificates from most top-level to most nested domains (with the assumption that
			// if there is a file for subdomain then there was an explicit effort to generat cert for that
			// subdomain.
			Arrays.sort(files, Comparator.comparingInt(fn -> fn.getName().split("\\.").length));
			for (File file : files) {
				loadCertificateFromFile(file, moveFileToBackup);
			}
		}
	}

	private void loadCertificateFromFile(File file, boolean moveFileToBackup) {
		String alias = file.getName();
		if (alias.endsWith(".pem")) {
			alias = alias.substring(0, alias.length() - 4);
		}

		loadCertificateFromFile(file, alias, moveFileToBackup);
	}

	private void loadCertificateFromFile(File file, String alias, boolean moveFileToBackup) {
		try {
			CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
			addCertificateEntry(certEntry, alias, false);
			Set<String> domains = certEntry.getCertificate()
					.map(CertificateContainer::getAllCNames)
					.orElse(Collections.emptySet());
			log.log(Level.CONFIG, "Loaded server certificate for domain: {0} (altCNames: {1}) from file: {2}",
					new Object[]{alias, String.join(", ", domains), file});
			if (moveFileToBackup) {
				Path target = null;
				try {
					target = file.toPath().resolveSibling(file.toPath().getFileName() + ".bak");
					Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
					log.log(Level.CONFIG, "Made backup of file: {0} to: {1}", new Object[]{file, target});
				} catch (Exception e) {
					log.log(Level.INFO, "Making file from: {0} to: {1} failed!", new Object[]{file, target, e});
				}
			}
		} catch (Exception ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Cannot load certficate from file: " + file, ex);
			}
			log.log(Level.WARNING, "Cannot load certficate from file: " + file + ", " + ex.getMessage());
		}
	}

	private void loadPredefinedCertificates(Map<String, String> predefined, boolean moveFileToBackup) {
		log.log(Level.CONFIG, "Loading predefined server certificates");
		for (final Map.Entry<String, String> entry : predefined.entrySet()) {
			File file = new File(entry.getValue());
			String alias = entry.getKey();
			loadCertificateFromFile(file, alias, moveFileToBackup);
		}
	}

	private File getDefaultCertDirectory(String[] pemDirs) {
		final File file = Arrays.stream(pemDirs)
				.map(Paths::get)
				.map(Path::toFile)
				.filter(File::exists)
				.findFirst()
				.orElse(Paths.get(pemDirs[0]).toFile());
		log.log(Level.CONFIG, () -> "Setting default directory for storing certificates to: " + file.getAbsolutePath());
		return file;
	}

	private void storeCertificateToFile(CertificateEntry entry, String filename)
			throws CertificateEncodingException, IOException {
		final String path = new File(defaultCertDirectory, filename + ".pem").toString();
		CertificateUtil.storeCertificate(path, entry);
	}

	private KeyManagerFactory getKeyManagerFactory(String domain, PrivateKey privateKey, Certificate[] certChain)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
				   UnrecoverableKeyException {
		KeyStore keys = KeyStore.getInstance("JKS");
		keys.load(null, emptyPass);
		keys.setKeyEntry(domain, privateKey, emptyPass, certChain);

		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keys, emptyPass);
		return kmf;
	}

	private void addCertificate(String alias, String pemCertificate, boolean saveToDisk) {
		try {
			addCertificate(alias, pemCertificate, saveToDisk, false);
		} catch (CertificateParsingException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Failed to update certificate for " + alias, ex);
			}
			log.log(Level.WARNING, "Failed to update certificate for " + alias + ", " + ex);
		}
	}

	private void addCertificate(String alias, String pemCert, boolean saveToDisk, boolean notifyCluster)
			throws CertificateParsingException {
		try {
			log.log(Level.FINEST, "Adding new certificate with alias: {0}. Saving to disk: {1}, notify cluster: {2}",
					new Object[]{alias, saveToDisk, notifyCluster});
			CertificateEntry entry = CertificateUtil.parseCertificate(new CharArrayReader(pemCert.toCharArray()));

			addCertificateEntry(entry, alias, saveToDisk);
			if (notifyCluster) {
				eventBus.fire(new CertificateChange(alias, pemCert, saveToDisk));
			}
			Optional<Certificate> certificate = entry.getCertificate();
			Set<String> domains = certificate.map(CertificateContainer::getAllCNames).orElse(Collections.emptySet());

			eventBus.fire(new CertificateChanged(alias, domains));
			log.log(Level.INFO,
					"Certificate with alias: {0} for domains: {1} added. Saving to disk: {2}, notify cluster: {3}",
					new Object[]{alias, domains, saveToDisk, notifyCluster});
		} catch (Exception ex) {
			throw new CertificateParsingException("Problem adding a new certificate (" + ex.getMessage() + ")", ex);
		}
	}

	private KeyManagerFactory createCertificateKmf(String alias)
			throws NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException,
				   NoSuchProviderException, SignatureException, KeyStoreException, UnrecoverableKeyException {
		alias = !def_cert_alias.equals(alias) && !alias.startsWith("*.") && generateWildcardCertificate ? "*." + alias : alias;
		CertificateEntry entry = CertificateUtil.createSelfSignedCertificate(email, alias, ou, o, null, null, null,
																			 () -> CertificateUtil.createKeyPair(1024,
																												 "secret"));
		return addCertificateEntry(entry, alias, true);
	}

	private Map<String, String> findPredefinedCertificates(Map<String, Object> params) {
		final Map<String, String> result = new HashMap<>();
		if (params == null) {
			return result;
		}

		for (String t : params.keySet()) {
			if (t.startsWith(PER_DOMAIN_CERTIFICATE_KEY)) {
				String domainName = t.substring(PER_DOMAIN_CERTIFICATE_KEY.length());
				result.put(domainName, params.get(t).toString());
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
				log.log(Level.FINEST, "Can''t generate fake trusted CA certificate", e);
			}
			log.log(Level.WARNING, "Can''t generate fake trusted CA certificate");
		}

		tms = new X509TrustManager[]{
				new FakeTrustManager(acceptedIssuers.toArray(new X509Certificate[acceptedIssuers.size()]))};

		long seconds = (System.currentTimeMillis() - start) / 1000;

		log.log(Level.CONFIG, "Loaded {0} trust certificates, it took {1} seconds.", new Object[]{counter, seconds});
	}

	/**
	 * Event indicating certificate change that will be distributed in the cluster.
	 */
	public static class CertificateChange
			implements Serializable, EventBusEvent {

		private String alias;
		private transient boolean local = false;
		private String pemCert;
		private boolean saveToDisk;

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

	private static class FakeTrustManager
			implements X509TrustManager {

		private X509Certificate[] issuers = null;

		FakeTrustManager() {
			this(new X509Certificate[0]);
		}

		FakeTrustManager(X509Certificate[] ai) {
			issuers = ai;
		}

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

	public class CertificateChanged implements EventBusEvent {

		Set<String> domains = new ConcurrentSkipListSet<>();
		private String alias;

		public CertificateChanged(String alias, Set<String> domains) {
			this.alias = alias;
			if (domains != null) {
				this.domains.addAll(domains);
			}
		}

		public String getAlias() {
			return alias;
		}

		public Set<String> getDomains() {
			return domains;
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
