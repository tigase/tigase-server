/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

//~--- non-JDK imports --------------------------------------------------------

import java.io.CharArrayReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 15, 2010 2:40:49 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SSLContextContainer implements SSLContextContainerIfc {

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

	// ~--- fields

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
	
	private static final Logger log = Logger.getLogger(SSLContextContainer.class.getName());
	public final static String PER_DOMAIN_CERTIFICATE_KEY = "virt-hosts-cert-";
	private ArrayList<X509Certificate> acceptedIssuers = new ArrayList<X509Certificate>(200);
	private File[] certsDirs = null;
	protected String def_cert_alias = DEFAULT_DOMAIN_CERT_VAL;
	private String email = "admin@tigase.org";
	private char[] emptyPass = new char[0];
	protected Map<String, KeyManagerFactory> kmfs = new ConcurrentSkipListMap<String, KeyManagerFactory>();
	private String o = "Tigase.org";
	private String ou = "XMPP Service";
	private SecureRandom secureRandom = new SecureRandom();

	// ~--- methods
	// --------------------------------------------------------------

	protected Map<String, SSLContextsHolder> sslContexts = new ConcurrentSkipListMap<>();

	// ~--- get methods
	// ----------------------------------------------------------

	private X509TrustManager[] tms = new X509TrustManager[] { new FakeTrustManager() };
	protected X509KeyManager[] kms = null;

	private KeyStore trustKeyStore = null;

	// ~--- methods
	// --------------------------------------------------------------

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
				sslContexts.remove(alias);
			} catch (Exception ex) {
				throw new CertificateParsingException("Problem adding a new certificate.", ex);
			}
		}
	}

	// ~--- get methods
	// ----------------------------------------------------------

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

	// ~--- inner classes
	// --------------------------------------------------------

	public SSLContext getSSLContext(String protocol, String hostname, boolean clientMode) {
		return getSSLContext(protocol, hostname, clientMode, tms);
	}
	
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
//		for (Entry<String, T> entry : data.entrySet()) {
//			final String k = entry.getKey();
//			if (k.startsWith("*") && key.endsWith(k.substring(1))) {
//				data.put(key, entry.getValue());
//				return entry.getValue();
//			}
//		}

		return null;
	}

	private class SSLContextsHolder {
		private final Map<TrustManager[],SSLContext> sslContexts = new ConcurrentHashMap<>(4);
		
		public SSLContext getSSLContext(TrustManager[] tms) {
			return sslContexts.get(tms);
		}
		
		public void putSSLContext(TrustManager[] tms, SSLContext sslContext) {
			sslContexts.put(tms, sslContext);
		}
	}
	
	@Override
	public SSLContext getSSLContext(String protocol, String hostname, boolean clientMode, TrustManager... tms) {
		SSLContext sslContext = null;

		String alias = hostname;

		try {

			// in client mode we need SSLContext initialized with certificate for proper domain
			// as in other case we would not be able to authenticate to other endpoint by
			// providing proper client certificate for domain
//			if (clientMode) {
//				sslContext = SSLContext.getInstance(protocol);
//				sslContext.init(null, tms, secureRandom);
//				return sslContext;
//			}


			if (alias == null) {
				alias = def_cert_alias;
			} // end of if (hostname == null)

			SSLContextsHolder sslContextsHolder = find(sslContexts, alias);
			if (sslContextsHolder != null)
				sslContext = sslContextsHolder.getSSLContext(tms);
			
			if (sslContext == null) {
				KeyManagerFactory kmf = find(kmfs, alias);

				if (kmf == null) {
					// if there is no KeyManagerFactory for domain then we can create
					// new empty context as we have no certificate for this domain
					if (clientMode) {
						sslContext = SSLContext.getInstance(protocol);
						sslContext.init(null, tms, secureRandom);
						return sslContext;
					}

					KeyPair keyPair = CertificateUtil.createKeyPair(1024, "secret");
					X509Certificate cert = CertificateUtil.createSelfSignedCertificate(email, alias, ou, o, null, null, null,
							keyPair);
					CertificateEntry entry = new CertificateEntry();

					entry.setPrivateKey(keyPair.getPrivate());
					entry.setCertChain(new Certificate[]{cert});
					kmf = addCertificateEntry(entry, alias, true);
					log.log(Level.WARNING, "Auto-generated certificate for domain: {0}", alias);
				}
				
				sslContext = SSLContext.getInstance(protocol);
				sslContext.init((hostname == null && kms != null) ? kms : kmf.getKeyManagers(), tms, secureRandom);
				sslContextsHolder = new SSLContextsHolder();
				sslContextsHolder = sslContexts.putIfAbsent(alias, sslContextsHolder);
				if (sslContextsHolder == null)
					sslContextsHolder = sslContexts.get(alias);
				sslContextsHolder.putSSLContext(tms, sslContext);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not initialize SSLContext for domain: " + alias + ", protocol: " + protocol, e);
			sslContext = null;
		}
                
		return sslContext;
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

			String pemD = (String) params.get(SERVER_CERTS_LOCATION_KEY);

			if (pemD == null) {
				pemD = SERVER_CERTS_LOCATION_VAL;
			}

			String[] pemDirs = pemD.split(",");

			certsDirs = new File[pemDirs.length];

			int certsDirsIdx = -1;

			Map<String, File> predefined = findPredefinedCertificates(params);
			log.log(Level.CONFIG, "Loading predefined server certificates");
			for (final Entry<String, File> entry : predefined.entrySet()) {
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
				final File directory = new File(pemDir);
				if (!directory.exists()) {
					continue;
				}
				certsDirs[++certsDirsIdx] = directory;

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
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
