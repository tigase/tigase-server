package tigase.server.xmppclient;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostItem.DataType;
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

	private TrustManager[] defaultTrustManagers;

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

	private static String VHOST_TRUST_MANAGER_KEY = "VHOST_TRUST_MANAGER";


	public TrustManager[] getManager(final VHostItem vHost) {
		TrustManager[] result = vHost.getData(VHOST_TRUST_MANAGER_KEY);

		if (result == null) {
			result = defaultTrustManagers;
			String path = vHost.getData(CA_CERT_PATH);
			if (path != null) {
				TrustManager[] tmp = loadTrustedCert(path);
				if (tmp != null) {
					result = tmp;
					vHost.setData(VHOST_TRUST_MANAGER_KEY, result);
				}
			}
		}

		return result;
	}

	public boolean isActive() {
		return acceptedIssuers.size() > 0;
	}

	public boolean isTlsNeedClientAuthEnabled(final VHostItem vhost) {
		Boolean result = vhost.getData(CERT_REQUIRED_KEY);
		if (result == null)
			result = peerCertificateRequired;
		return result;
	}

	public boolean isTlsWantClientAuthEnabled(final VHostItem vhost) {
		TrustManager[] tmp = getManager(vhost);
		return tmp != null && tmp.length > 0;
	}

	protected TrustManager[] loadTrustedCert(String caCertFile) {
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
			return tmf.getTrustManagers();
			// this.saslExternalAvailable = true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(CERT_REQUIRED_KEY)) {
			this.peerCertificateRequired = (Boolean) props.get(CERT_REQUIRED_KEY);
		}
		if (props.containsKey(CA_CERT_PATH)) {
			this.defaultTrustManagers = loadTrustedCert((String) props.get(CA_CERT_PATH));
		}
	}

}
