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

import tigase.osgi.ModulesManagerImpl;

import java.security.KeyStore;
import java.security.cert.CertificateParsingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.io.CertificateContainerIfc.CERTIFICATE_CONTAINER_CLASS_KEY;
import static tigase.io.CertificateContainerIfc.CERTIFICATE_CONTAINER_CLASS_VAL;
import static tigase.io.SSLContextContainerIfc.SSL_CONTAINER_CLASS_KEY;
import static tigase.io.SSLContextContainerIfc.SSL_CONTAINER_CLASS_VAL;

/**
 * Describe class TLSUtil here.
 * <br>
 * Created: Mon Jan 23 14:21:31 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TLSUtil {

	private static final Logger log = Logger.getLogger(TLSUtil.class.getName());
	private static CertificateContainerIfc certificateContainer = null;
	//private static Map<String, SSLContextContainerIfc> sslContexts =
//  new HashMap<String, SSLContextContainerIfc>();
	private static SSLContextContainerIfc sslContextContainer = null;

	public static void addCertificate(Map<String, String> params) throws CertificateParsingException {
		sslContextContainer.addCertificates(params);
	}

	public static void configureSSLContext(Map<String, Object> params) {
		// we should initialize this only once
		if (sslContextContainer != null) {
			return;
		}

		String sslCC_class = (String) params.get(SSL_CONTAINER_CLASS_KEY);
		String certC_class = (String) params.get(CERTIFICATE_CONTAINER_CLASS_KEY);

		if (sslCC_class == null) {
			sslCC_class = SSL_CONTAINER_CLASS_VAL;
		}
		if (sslCC_class.equals("tigase.io.jdk18.SNISSLContextContainer")) {
			log.log(Level.WARNING, "You are using '" + sslCC_class + "' as " + SSL_CONTAINER_CLASS_KEY + ".\n" +
					"This class is not available as SNI support was moved to SSLContextContainer");
			sslCC_class = SSL_CONTAINER_CLASS_VAL;
		}
		if (sslCC_class.equals("tigase.extras.io.PEMSSLContextContainer")) {
			log.log(Level.WARNING, "You are using '" + sslCC_class + "' as " + SSL_CONTAINER_CLASS_KEY + ".\n" +
					"This class is not available any more. To keep using this feature please replace configuration\n" +
					"of " + SSL_CONTAINER_CLASS_KEY + " to " + sslCC_class + " with " +
					CERTIFICATE_CONTAINER_CLASS_KEY + "\n" + "set to tigase.extras.io.PEMCertificateContainer");
			sslCC_class = SSL_CONTAINER_CLASS_VAL;
			certC_class = "tigase.extras.io.PEMCertificateContainer";
		}
		if (certC_class == null) {
			certC_class = CERTIFICATE_CONTAINER_CLASS_VAL;
		}

		try {
			certificateContainer = (CertificateContainerIfc) ModulesManagerImpl.getInstance()
					.forName(certC_class)
					.newInstance();
			certificateContainer.init(params);
			sslContextContainer = (SSLContextContainerIfc) ModulesManagerImpl.getInstance()
					.forName(sslCC_class)
					.getDeclaredConstructor(CertificateContainerIfc.class)
					.newInstance(certificateContainer);
			//sslContextContainer = (SSLContextContainerIfc) Class.forName(sslCC_class).newInstance();
			sslContextContainer.start();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not initialize SSL Container: " + sslCC_class, e);
			sslContextContainer = null;
		}
	}

	/**
	 * Method returns singleton instance of class implementing CertificateContainterIfc responsible for caching SSL
	 * certificates in memory.
	 *
	 * @return
	 */
	public static CertificateContainerIfc getCertificateContainer() {
		return certificateContainer;
	}

	/**
	 * Method returns singleton instance of class implementing SSLContextContainerIfc responsible for caching SSLContext
	 * instances.
	 * <br>
	 * This instance should be wrapped by new instance of SSLContextContainer if method getSSLContext will be used with
	 * TrustManager array passed!
	 *
	 * @return
	 */
	public static SSLContextContainerIfc getRootSslContextContainer() {
		return sslContextContainer;
	}

	public static KeyStore getTrustStore() {
		return sslContextContainer.getTrustStore();
	}
}    // TLSUtil

