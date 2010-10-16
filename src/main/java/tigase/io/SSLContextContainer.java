
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 of the License.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. Look for COPYING file in the top folder.
* If not, see http://www.gnu.org/licenses/.
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.io;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;

//~--- JDK imports ------------------------------------------------------------

import java.io.CharArrayReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

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
import java.security.spec.InvalidKeySpecException;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import javax.security.auth.x500.X500Principal;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 15, 2010 2:40:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SSLContextContainer implements SSLContextContainerIfc {
	private static final Logger log = Logger.getLogger(SSLContextContainer.class.getName());

	//~--- fields ---------------------------------------------------------------

	private File certsDir = null;
	private String def_cert_alias = null;
	private String email = "admin@tigase.org";
	private char[] emptyPass = new char[0];
	private String o = "Tigase.org";
	private String ou = "XMPP Service";
	private SecureRandom secureRandom = null;
	private Map<String, SSLContext> sslContexts = new ConcurrentSkipListMap<String, SSLContext>();
	private Map<String, KeyManagerFactory> kmfs = new ConcurrentSkipListMap<String,
		KeyManagerFactory>();
	private X509TrustManager[] tms = new X509TrustManager[] { new FakeTrustManager() };

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @throws CertificateParsingException
	 */
	@Override
	public void addCertificates(Map<String, String> params) throws CertificateParsingException {
		String pemCert = params.get(PEM_CERTIFICATE_KEY);
		String saveToDiskVal = params.get(CERT_SAVE_TO_DISK_KEY);
		boolean saveToDisk = (saveToDiskVal != null) && saveToDiskVal.equalsIgnoreCase("true");

		if (pemCert != null) {
			try {
				CertificateEntry entry =
					CertificateUtil.parseCertificate(new CharArrayReader(pemCert.toCharArray()));
				X509Certificate cert = (X509Certificate) entry.getCertChain()[0];
				String alias = params.get(CERT_ALIAS_KEY);

				if (alias == null) {
					alias = getCertAlias(cert, null);
				}

				addCertificateEntry(entry, alias, saveToDisk);
				sslContexts.remove(alias);
			} catch (Exception ex) {
				throw new CertificateParsingException("Problem adding a new certificate.", ex);
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param protocol
	 * @param hostname
	 *
	 * @return
	 */
	@Override
	public SSLContext getSSLContext(String protocol, String hostname) {
		SSLContext sslContext = null;

		try {
			String alias = hostname;

			if (alias == null) {
				alias = def_cert_alias;
			}    // end of if (hostname == null)

			sslContext = sslContexts.get(alias);

			if (sslContext == null) {
				KeyManagerFactory kmf = kmfs.get(alias);

				if (kmf == null) {
					KeyPair keyPair = CertificateUtil.createKeyPair(1024, "secret");
					X509Certificate cert = CertificateUtil.createSelfSignedCertificate(email, hostname, ou,
						o, null, null, null, keyPair);
					CertificateEntry entry = new CertificateEntry();

					entry.setPrivateKey(keyPair.getPrivate());
					entry.setCertChain(new Certificate[] { cert });
					kmf = addCertificateEntry(entry, alias, true);
					log.log(Level.WARNING, "Auto-generated certificate for alias: {0}", alias);
				}

				sslContext = SSLContext.getInstance(protocol);
				sslContext.init(kmf.getKeyManagers(), tms, secureRandom);
				sslContexts.put(alias, sslContext);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE,
					"Can not initialize SSLContext for domain: " + hostname + ", protocol: " + protocol, e);
			sslContext = null;
		}

		return sslContext;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 */
	@Override
	public void init(Map<String, Object> params) {
		try {
			def_cert_alias = (String) params.get(DEFAULT_DOMAIN_CERT_KEY);

			if (def_cert_alias == null) {
				def_cert_alias = DEFAULT_DOMAIN_CERT_VAL;
			}

			String pemDir = (String) params.get(SERVER_CERTS_LOCATION_KEY);

			if (pemDir == null) {
				pemDir = SERVER_CERTS_LOCATION_VAL;
			}

			log.log(Level.CONFIG, "Loading server certificates from PEM directory: {0}", pemDir);
			certsDir = new File(pemDir);
			secureRandom = new SecureRandom();
			secureRandom.nextInt();

			for (File file : certsDir.listFiles(new PEMFileFilter())) {
				try {
					CertificateEntry certEntry = CertificateUtil.loadCertificate(file);
					X509Certificate cert = (X509Certificate) certEntry.getCertChain()[0];
					String alias = getCertAlias(cert, file);

					addCertificateEntry(certEntry, alias, false);
					log.log(Level.CONFIG, "Loaded server certificate for alias: {0}", alias);
				} catch (Exception ex) {
					log.log(Level.WARNING, "Cannot load certficate from file: " + file, ex);
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "There was a problem initializing SSL certificates.", ex);
		}
	}

	private KeyManagerFactory addCertificateEntry(CertificateEntry entry, String alias, boolean store)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
			UnrecoverableKeyException {
		KeyStore keys = KeyStore.getInstance("JKS");

		keys.load(null, emptyPass);
		keys.setKeyEntry(alias, entry.getPrivateKey(), emptyPass, entry.getCertChain());

		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

		kmf.init(keys, emptyPass);
		kmfs.put(alias, kmf);

		if (store) {
			CertificateUtil.storeCertificate(new File(certsDir, alias + ".pem").toString(), entry);
		}

		return kmf;
	}

	//~--- get methods ----------------------------------------------------------

	private String getCertAlias(X509Certificate cert, File file) {
		String name = CertificateUtil.getCertCName(cert);

		if (name != null) {
			return name;
		}

		// Cannot get alias from certificate, let's try to get it from file name
		return file.getName().substring(0, file.getName().length() - 4);
	}

	//~--- inner classes --------------------------------------------------------

	private static class FakeTrustManager implements X509TrustManager {
		private X509Certificate[] acceptedIssuers = null;

		//~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 *
		 */
		public FakeTrustManager() {}

		/**
		 * Constructs ...
		 *
		 *
		 * @param ai
		 */
		public FakeTrustManager(X509Certificate[] ai) {
			acceptedIssuers = ai;
		}

		//~--- methods ------------------------------------------------------------

		// Implementation of javax.net.ssl.X509TrustManager

		/**
		 * Method description
		 *
		 *
		 * @param x509CertificateArray
		 * @param string
		 *
		 * @throws CertificateException
		 */
		@Override
		public void checkClientTrusted(final X509Certificate[] x509CertificateArray,
				final String string)
				throws CertificateException {}

		/**
		 * Method description
		 *
		 *
		 * @param x509CertificateArray
		 * @param string
		 *
		 * @throws CertificateException
		 */
		@Override
		public void checkServerTrusted(final X509Certificate[] x509CertificateArray,
				final String string)
				throws CertificateException {}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return acceptedIssuers;
		}
	}


	private class PEMFileFilter implements FileFilter {

		/**
		 * Method description
		 *
		 *
		 * @param pathname
		 *
		 * @return
		 */
		@Override
		public boolean accept(File pathname) {
			if (pathname.isFile()
					&& (pathname.getName().endsWith(".pem") || pathname.getName().endsWith(".PEM"))) {
				return true;
			}

			return false;
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
