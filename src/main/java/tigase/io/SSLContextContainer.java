/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.io.FileInputStream;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Describe class SSLContextContainer here.
 *
 *
 * Created: Mon Jan 23 14:47:55 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SSLContextContainer {

  private static final Logger log =
		Logger.getLogger("tigase.io.SSLContextContainer");

	private SecureRandom secureRandom = null;
	private Map<String, SSLContext> sslContexts =
		new HashMap<String, SSLContext>();
	private Map<String, KeyManagerFactory> kmfs =
		new HashMap<String, KeyManagerFactory>();
// 	private KeyManagerFactory kmf = null;
	private TrustManagerFactory tmf = null;
	private String def_cert_alias = null;

	public SSLContextContainer() {
		log.config("Initializing SSL library (trust all certs mode)...");
		init(null, null, null, null);
	}

	public SSLContextContainer(String k_store, String k_passwd,
		String def_cert_alias) {
		log.config("Initializing SSL library (trust all certs mode)...");
		this.def_cert_alias = def_cert_alias;
		init(k_store, k_passwd, null, null);
	}

	public SSLContextContainer(String k_store, String k_passwd,
		String t_store, String t_passwd, String def_cert_alias) {

		log.config("Initializing SSL library...");
		this.def_cert_alias = def_cert_alias;
		init(k_store, k_passwd, t_store, t_passwd);
	}

	private void init(String k_store, String k_passwd,
		String t_store, String t_passwd) {
		try {
			if (k_store != null && k_passwd != null) {
				final KeyStore keys = KeyStore.getInstance("JKS");
				final char[] keys_password = k_passwd.toCharArray();
				keys.load(new	FileInputStream(k_store),	keys_password);
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(keys, keys_password);
				kmfs.put(null, kmf);
				Enumeration<String> aliases = keys.aliases();
				ArrayList<String> certlist = null;
				if (aliases != null) {
					certlist = new ArrayList<String>();
					while (aliases.hasMoreElements()) {
						String alias = aliases.nextElement();
						if (keys.isCertificateEntry(alias)) {
							certlist.add(alias);
						} // end of if (keys.isCertificateEntry(alias))
					}
				}
				aliases = keys.aliases();
				KeyStore.PasswordProtection pass_param =
					new KeyStore.PasswordProtection(keys_password);
				if (aliases != null) {
					while (aliases.hasMoreElements()) {
						String alias = aliases.nextElement();
						if (keys.isKeyEntry(alias)) {
							KeyStore.Entry entry = keys.getEntry(alias, pass_param);
							//KeyStore.Entry entry = keys.getEntry(alias, null);
							KeyStore alias_keys = KeyStore.getInstance("JKS");
							alias_keys.load(null,	keys_password);
							if (certlist != null) {
								for (String certal: certlist) {
									alias_keys.setCertificateEntry(certal,
										keys.getCertificate(certal));
								} // end of for (String certal: certlist)
							} // end of if (root != null)
							alias_keys.setEntry(alias, entry, pass_param);
							//alias_keys.setEntry(alias, entry, null);
							kmf = KeyManagerFactory.getInstance("SunX509");
							kmf.init(alias_keys, keys_password);
							kmfs.put(alias, kmf);
						} // end of if (!alias.equals("root"))
					} // end of while (aliases.hasMoreElements())
				} // end of if (aliases != null)
			} // end of if (k_store != null && k_passwd != null)

			if (t_store != null && t_passwd != null) {
				final KeyStore trusts = KeyStore.getInstance("JKS");
				final char[] trusts_password = t_passwd.toCharArray();
				trusts.load(new	FileInputStream(t_store), trusts_password);
				tmf = TrustManagerFactory.getInstance("SunX509");
				tmf.init(trusts);
			} // end of if (t_store != null && t_passwd != null)

			secureRandom = new SecureRandom();
			secureRandom.nextInt();
		} // end of try
		catch (Exception e) {
			log.log(Level.SEVERE, "Can not initialize SSL library", e);
		} // end of try-catch
	}

	public SSLContext getSSLContext(final String protocol, String hostname) {
		if (hostname == null) {
			hostname = def_cert_alias;
		} // end of if (hostname == null)
		String map_key = hostname+protocol;
		SSLContext sslContext = sslContexts.get(map_key);
		if (sslContext == null) {
			try {
				sslContext = SSLContext.getInstance(protocol);
				KeyManagerFactory kmf = kmfs.get(hostname);
				if (kmf == null) {
					kmf = kmfs.get(def_cert_alias);
				} // end of if (kmf == null)
				if (kmf != null && tmf != null) {
					sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
						secureRandom);
				} else {
					if (kmf == null) {
						log.warning("No certificate found for host: " + hostname);
					} // end of if (kmf == null)
					sslContext.init(kmf != null ? kmf.getKeyManagers() : null,
						new X509TrustManager[] {new FakeTrustManager()}, secureRandom);
				} // end of if (kmf != null && tmf != null) else
				sslContexts.put(map_key, sslContext);
				log.config("Created SSL context for: " + sslContext.getProtocol());
			} // end of try
			catch (Exception e) {
				log.log(Level.SEVERE, "Can not initialize SSLContext", e);
				sslContext = null;
			} // end of try-catch
		} // end of if (sslContext == null)
		return sslContext;
	}

  private static class FakeTrustManager implements X509TrustManager {

    private X509Certificate[] acceptedIssuers = null;

    public FakeTrustManager(X509Certificate[] ai) { acceptedIssuers = ai; }

    public FakeTrustManager() { }

    // Implementation of javax.net.ssl.X509TrustManager

    public void checkClientTrusted(final X509Certificate[] x509CertificateArray,
      final String string) throws CertificateException { }

    public void checkServerTrusted(final X509Certificate[] x509CertificateArray,
      final String string) throws CertificateException { }

    public X509Certificate[] getAcceptedIssuers() { return acceptedIssuers; }

  }

} // SSLContextContainer
