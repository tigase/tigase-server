/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Describe class SSLContextContainer here.
 *
 *
 * Created: Mon Jan 23 14:47:55 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SSLContextContainer {

  private static final Logger log =
		Logger.getLogger("tigase.io.SSLContextContainer");

	private SecureRandom secureRandom = null;
	private Map<String, SSLContext> sslContexts =
		new HashMap<String, SSLContext>();
	private KeyManagerFactory kmf = null;
	private TrustManagerFactory tmf = null;

	public SSLContextContainer(String k_store, String k_passwd,
		String t_store, String t_passwd) {

		log.config("Initializing SSL library...");
		final char[] keys_password = k_passwd.toCharArray();
		final char[] trusts_password = t_passwd.toCharArray();
		try {
			final KeyStore keys = KeyStore.getInstance("JKS");
			keys.load(new	FileInputStream(k_store),	keys_password);
			final KeyStore trusts = KeyStore.getInstance("JKS");
			trusts.load(new	FileInputStream(t_store), trusts_password);

			kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keys, keys_password);
			tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(trusts);

			secureRandom = new SecureRandom();
			secureRandom.nextInt();
		} // end of try
		catch (Exception e) {
			System.out.println("Can not initialize SSL library: " + e); // NOPMD
			log.log(Level.SEVERE, "Can not initialize SSL library", e);
			System.exit(1);
		} // end of try-catch
	}

	public SSLContext getSSLContext(final String protocol) {
		SSLContext sslContext = sslContexts.get(protocol);
		if (sslContext == null) {
			try {
				sslContext = SSLContext.getInstance(protocol);
				sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
					secureRandom);
				sslContexts.put(protocol, sslContext);
				log.config("Created SSL context for: " + sslContext.getProtocol());
			} // end of try
			catch (Exception e) {
				log.log(Level.SEVERE, "Can not initialize SSLContext", e);
				sslContext = null;
			} // end of try-catch
		} // end of if (sslContext == null)
		return sslContext;
	}

} // SSLContextContainer
