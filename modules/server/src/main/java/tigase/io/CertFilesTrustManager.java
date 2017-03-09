/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev: $
 * Last modified by $Author: kobit $
 * $Date:  $
 */

package tigase.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

/**
 * Class CertFilesTrustManager
 *
 * @author <a href="mailto:bartosz.malkowski@tigase.org">Bartosz Malkowski</a>
 * @version $Rev: $
 */
public class CertFilesTrustManager implements X509TrustManager {

	private static CertificateFactory certificateFactory;

	public static CertFilesTrustManager getInstance(String pathToCertsFiles) throws Exception {
		certificateFactory = CertificateFactory.getInstance("X.509");
		Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();

		File[] files = new File(pathToCertsFiles).listFiles();

		for (File file : files) {
			if (!file.isFile()) {
				continue;
			}
			try {
				X509Certificate cert = loadCertificate(file);
				TrustAnchor ta = new TrustAnchor(cert, null);
				trustAnchors.add(ta);
			} catch (CertificateParsingException e) {}
		}

		CertPathValidator val = CertPathValidator.getInstance(CertPathValidator.getDefaultType());

		PKIXParameters cpp = new PKIXParameters(trustAnchors);
		cpp.setRevocationEnabled(false);
		CertFilesTrustManager tm = new CertFilesTrustManager(val, cpp);
		return tm;
	}

	private static X509Certificate loadCertificate(File certFile) throws Exception {
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(certFile);
			X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(inStream);
			return cert;
		} finally {
			if (inStream != null) inStream.close();
		}
	}

	private PKIXParameters parameters;

	private CertPathValidator validator;

	private CertFilesTrustManager(CertPathValidator val, PKIXParameters cpp) {
		this.validator = val;
		this.parameters = cpp;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkServerTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// TODO Auto-generated method stub
		List<X509Certificate> certs = new ArrayList<X509Certificate>();
		for (X509Certificate certificate : chain) {
			certificate.checkValidity();
			certs.add(certificate);
		}
		CertPath cp = CertificateFactory.getInstance("X.509").generateCertPath(certs);
		try {
			CertPathValidatorResult result = validator.validate(cp, parameters);
			System.out.println(result);
		} catch (CertPathValidatorException e) {
			// e.printStackTrace();
			throw new CertificateException(e);
		} catch (InvalidAlgorithmParameterException e) {
			// e.printStackTrace();
			throw new CertificateException(e);
		}

	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return parameters.getTrustAnchors().toArray(new X509Certificate[] {});
	}
}
