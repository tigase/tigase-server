package tigase.server.xmppclient;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.vhosts.VHostItem;
import tigase.xmpp.XMPPIOService;

public class ClientTrustManagerFactory {

	public static final String CA_CERT_PATH = "clientCertCA";

	public static final String CERT_REQUIRED_KEY = "clientCertRequired";

	private final static char[] EMPTY_PASS = new char[0];

	private static final Logger log = Logger.getLogger(ClientTrustManagerFactory.class.getName());

	private final ArrayList<X509Certificate> acceptedIssuers = new ArrayList<X509Certificate>();

	private TrustManager[] defaultTrustManagers;

	private final TrustManager[] emptyTrustManager;

	private final KeyStore keystore;

	private boolean peerCertificateRequired = false;

	private TrustManagerFactory tmf;

	private final ConcurrentHashMap<VHostItem, TrustManager[]> trustManagers = new ConcurrentHashMap<VHostItem, TrustManager[]>();

	public ClientTrustManagerFactory() {
		this.emptyTrustManager = new TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
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

	public TrustManager[] getManager(final VHostItem vHost) {
		TrustManager[] result = trustManagers.get(vHost);

		if (result == null) {
			if (log.isLoggable(Level.FINEST))
				log.finest("Creating new TrustManager for VHost " + vHost);

			result = defaultTrustManagers;
			String path = vHost.getData(CA_CERT_PATH);
			if (log.isLoggable(Level.FINEST))
				log.finest("CA cert path=" + path + " for VHost " + vHost);
			if (path != null) {
				TrustManager[] tmp = loadTrustedCert(path);
				if (tmp != null) {
					if (log.isLoggable(Level.FINEST))
						log.finest("Using custom TrustManager for VHost " + vHost);
					result = tmp;
					trustManagers.put(vHost, result);
				}
			}
		} else if (log.isLoggable(Level.FINEST))
			log.finest("Found TrustManager for VHost " + vHost);

		return result;
	}

	public TrustManager[] getManager(final XMPPIOService<Object> serv) {
		return isActive() ? emptyTrustManager : null;
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

			if (log.isLoggable(Level.FINEST))
				log.finest("Loaded certificate from file " + caCertFile + " : " + certEntry);

			if (chain != null) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Loaded cert chain: " + Arrays.toString(chain));
				for (Certificate cert : chain) {
					if (cert instanceof X509Certificate) {
						X509Certificate crt = (X509Certificate) cert;
						String alias = crt.getSubjectX500Principal().getName();

						if (log.isLoggable(Level.FINEST))
							log.finest("Adding certificate to keystore: alias=" + alias + "; cert=" + crt);

						keystore.setCertificateEntry(alias, crt);
						acceptedIssuers.add(crt);
					}
				}
			}
			tmf.init(keystore);
			return tmf.getTrustManagers();
			// this.saslExternalAvailable = true;
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't create TrustManager with certificate from file.", e);
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
