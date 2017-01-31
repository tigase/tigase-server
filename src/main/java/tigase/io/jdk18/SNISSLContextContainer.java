/*
 * SNISSLContextContainer.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */

package tigase.io.jdk18;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import tigase.io.SSLContextContainer;
import static tigase.io.SSLContextContainer.find;

/**
 * Implementation of SSLContextContainer which may be used since JDK 1.8 to
 * provide support for Server Name Indication (SNI) extension of SSL/TLS protocol
 * which indicates server name the client is attempting to connect during 
 * handshaking. This implementation searches for proper SSL certificate for 
 * passed server name and uses this certificate if it is available, if not it
 * falls back to use of default certificate.
 *
 * @author andrzej
 */
public class SNISSLContextContainer extends SSLContextContainer {
	
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
			Set<String> aliases = sslContexts.keySet();
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
			KeyManagerFactory kmf = find( kmfs, alias );
			if ( kmf == null ){
				alias = def_cert_alias;
				kmf = SSLContextContainer.find( kmfs, alias );
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
			KeyManagerFactory kmf = find(kmfs, alias);
			if ( kmf == null ){
				alias = def_cert_alias;
				kmf = SSLContextContainer.find( kmfs, alias );
			}

			return ((X509KeyManager) kmf.getKeyManagers()[0]).getPrivateKey(alias);
		}
		
	}

	public SNISSLContextContainer() {
		kms = new X509KeyManager[] { new SniKeyManager() };
	}
}
