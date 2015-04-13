package tigase.server.xmppclient;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.xmpp.XMPPIOService;

public class ClientTrustManagerFactory {

	public static final String CA_CERT_PATH = "clientCertCA";

	public static final String CERT_REQUIRED_KEY = "clientCertRequired";

	private final static char[] EMPTY_PASS = new char[0];

	private final ArrayList<X509Certificate> acceptedIssuers = new ArrayList<X509Certificate>();

	private final KeyStore keystore;

	private boolean peerCertificateRequired = false;

	private boolean saslExternalAvailable = false;

	private TrustManagerFactory tmf;

	private X509TrustManager trustManager;

	private final TrustManager[] trustWrapper;

	public ClientTrustManagerFactory() {
		this.trustWrapper = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				trustManager.checkClientTrusted(chain, authType);
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return trustManager.getAcceptedIssuers();
			}
		} };
		try {
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(null, EMPTY_PASS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			this.tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

	}

	protected X509Certificate[] getAcceptedIssuers() {
		return acceptedIssuers.toArray(new X509Certificate[] {});
	}

	public TrustManager[] getManager(final XMPPIOService<Object> serv) {
		return isActive() ? trustWrapper : null;
	}

	public boolean isActive() {
		return acceptedIssuers.size() > 0;
	}

	public boolean isPeerCertificateRequired() {
		return peerCertificateRequired;
	}

	public boolean isSaslExternalAvailable() {
		return saslExternalAvailable;
	}

	protected void loadTrustedCert(String caCertFile) {
		try {
			CertificateEntry certEntry = CertificateUtil.loadCertificate(caCertFile);
			Certificate[] chain = certEntry.getCertChain();

			if (chain != null) {
				for (Certificate cert : chain) {
					if (cert instanceof X509Certificate) {
						X509Certificate crt = (X509Certificate) cert;
						String alias = crt.getSubjectX500Principal().getName();

						keystore.setCertificateEntry(alias, crt);
						acceptedIssuers.add(crt);
					}
				}
			}
			tmf.init(keystore);
			TrustManager[] trustManagers = tmf.getTrustManagers();
			trustManager = (X509TrustManager) trustManagers[0];
			this.saslExternalAvailable = true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(CERT_REQUIRED_KEY)) {
			this.peerCertificateRequired = (Boolean) props.get(CERT_REQUIRED_KEY);
		}
		if (props.containsKey(CA_CERT_PATH)) {
			loadTrustedCert((String) props.get(CA_CERT_PATH));
		}
	}

}
