package tigase.io;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;
import tigase.stats.StatisticsList;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BcTLSIO
		implements IOInterface {

	public static final String TLS_CAPS = "tls-caps";
	private static final Logger log = Logger.getLogger(BcTLSIO.class.getName());
	private final CertificateContainerIfc certificateContainer;
	private final TrustManager[] clientTrustManagers;
	private final BcTlsCrypto crypto;
	private final TLSEventHandler eventHandler;
	private final String hostname;
	private final boolean needClientAuth;
	private final SecureRandom random;
	private final DefaultTlsServer server;
	private final TlsServerProtocol serverProtocol;
	private final boolean wantClientAuth;
	private Certificate bcCert;
	private int bytesRead = 0;
	private boolean handshakeCompleted = false;
	private IOInterface io = null;
	private Certificate peerCertificate;
	private AsymmetricKeyParameter privateKey;
	/**
	 * <code>tlsInput</code> buffer keeps data decoded from tlsWrapper.
	 */
	private ByteBuffer tlsInput = null;
	private byte[] tlsUnique;

	private TLSWrapper fakeWrapper = new TLSWrapper() {
		@Override
		public int bytesConsumed() {
//			(new RuntimeException("DEBUG")).printStackTrace();
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public void close() throws SSLException {
			try {
				BcTLSIO.this.serverProtocol.close();
			} catch (IOException e) {
				log.log(Level.FINE, "Cannot close Server Protocol", e);
			}
		}

		@Override
		public int getAppBuffSize() {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public CertCheckResult getCertificateStatus(boolean revocationEnabled,
													SSLContextContainerIfc sslContextContainer) {
			java.security.cert.Certificate[] chain;
			try {
				chain = getPeerCertificates();
			} catch (SSLPeerUnverifiedException e) {
				return CertCheckResult.none;
			}

			if (chain == null || chain.length == 0) {
				return CertCheckResult.none;
			}

			try {
				return CertificateUtil.validateCertificate(chain, sslContextContainer.getTrustStore(),
														   revocationEnabled);
			} catch (Exception ex) {
				log.log(Level.WARNING, "Problem validating certificate", ex);
			}

			return CertCheckResult.invalid;
		}

		@Override
		public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
			if (handshakeCompleted) {
				return SSLEngineResult.HandshakeStatus.FINISHED;
			} else {
				return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
			}
		}

		@Override
		public java.security.cert.Certificate[] getLocalCertificates() {
			java.security.cert.Certificate[] c = gen(BcTLSIO.this.bcCert);
			return c;
		}

		@Override
		public int getNetBuffSize() {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public int getPacketBuffSize() {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public java.security.cert.Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
			if (BcTLSIO.this.peerCertificate == null) {
				return null;
			}

			java.security.cert.Certificate[] result = gen(BcTLSIO.this.peerCertificate);

			return result;
		}

		@Override
		public TLSStatus getStatus() {
			return TLSStatus.OK;
		}

		@Override
		public byte[] getTlsUniqueBindingData() {
			return BcTLSIO.this.tlsUnique;
		}

		@Override
		public boolean isClientMode() {
			return false;
		}

		@Override
		public boolean isNeedClientAuth() {
			return BcTLSIO.this.needClientAuth;
		}

		@Override
		public void setDebugId(String id) {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public boolean wantClientAuth() {
			return BcTLSIO.this.wantClientAuth;
		}

		@Override
		public void wrap(ByteBuffer app, ByteBuffer net) throws SSLException {
			throw new RuntimeException("Cannot be used!");
		}
	};

	public BcTLSIO(final CertificateContainerIfc certificateContainer, final TLSEventHandler eventHandler,
				   final IOInterface ioi, String hostname, final ByteOrder order, boolean wantClientAuth,
				   boolean needClientAuth, String[] enabledCiphers, String[] enabledProtocols,
				   TrustManager[] x509TrustManagers) throws IOException {
		this.clientTrustManagers = x509TrustManagers;
		this.wantClientAuth = wantClientAuth;
		this.needClientAuth = needClientAuth;
		this.certificateContainer = certificateContainer;
		this.eventHandler = eventHandler;
		this.random = new SecureRandom();
		this.crypto = new BcTlsCrypto(random);
		this.hostname = hostname;
		io = ioi;
		tlsInput = ByteBuffer.allocate(2048);
		tlsInput.order(order);

		this.serverProtocol = new TlsServerProtocol();
		this.server = new TigaseTlsServer(crypto);

		try {
			loadKeys();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		serverProtocol.accept(server);

		//pumpData();

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "TLS Socket created: {0}", io.toString());
		}
	}

	@Override
	public int bytesRead() {
		return bytesRead;
	}

	@Override
	public boolean checkCapabilities(String caps) {
		return caps.contains(TLS_CAPS) || io.checkCapabilities(caps);
	}

	private X509Certificate[] gen(Certificate chain) {
		if (chain == null) {
			return null;
		}
		try {
			X509Certificate[] result = new X509Certificate[chain.getLength()];

			for (int i = 0; i < chain.getLength(); i++) {
				TlsCertificate c = chain.getCertificateAt(i);
				X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(c.getEncoded()));
				result[i] = cert;
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Certificate gen(final java.security.cert.Certificate[] certChain)
			throws CertificateEncodingException, IOException {
		TlsCertificate[] arr = new TlsCertificate[certChain.length];

		for (int i = 0; i < certChain.length; i++) {
			TlsCertificate cc = crypto.createCertificate(certChain[i].getEncoded());
			arr[i] = cc;
		}
		return new org.bouncycastle.tls.Certificate(arr);
	}

	private org.bouncycastle.tls.Certificate gen(KeyPair keypair) throws Exception {

// fill in certificate fields
		X500Name subject = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, hostname).build();
		byte[] id = new byte[20];
		random.nextBytes(id);
		BigInteger serial = new BigInteger(160, random);
		X509v3CertificateBuilder certificate = new JcaX509v3CertificateBuilder(subject, serial, new Date(), new Date(
				(new Date()).getTime() + 1000 * 60 * 10000), subject, keypair.getPublic());
		certificate.addExtension(Extension.subjectKeyIdentifier, false, id);
		certificate.addExtension(Extension.authorityKeyIdentifier, false, id);
		BasicConstraints constraints = new BasicConstraints(true);
		certificate.addExtension(Extension.basicConstraints, true, constraints.getEncoded());
		KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature);
		certificate.addExtension(Extension.keyUsage, false, usage.getEncoded());
		ExtendedKeyUsage usageEx = new ExtendedKeyUsage(
				new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth});
		certificate.addExtension(Extension.extendedKeyUsage, false, usageEx.getEncoded());

// build BouncyCastle certificate
		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keypair.getPrivate());
		X509CertificateHolder holder = certificate.build(signer);

// convert to JRE certificate
		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		converter.setProvider(new BouncyCastleProvider());
		X509Certificate x509 = converter.getCertificate(holder);

		TlsCertificate c = crypto.createCertificate(holder.getEncoded());
		TlsCertificate[] arr = new TlsCertificate[]{c};

		org.bouncycastle.tls.Certificate zz = new org.bouncycastle.tls.Certificate(arr);

		return zz;
	}

	private Collection<X500Name> getAcceptedIssuers() {
		if (clientTrustManagers != null) {
			ArrayList<X500Name> result = new ArrayList<>();

			for (TrustManager clientTrustManager : clientTrustManagers) {
				if (clientTrustManager instanceof X509TrustManager) {
					X509Certificate[] iss = ((X509TrustManager) clientTrustManager).getAcceptedIssuers();
					for (X509Certificate certificate : iss) {
						X500Name n = new X500Name(certificate.getSubjectDN().toString());
						result.add(n);
					}
				}
			}

			return result;
		}
		return null;
	}

	@Override
	public long getBuffOverflow(boolean reset) {
		return io.getBuffOverflow(reset);
	}

	private byte[] getBytes(final ByteBuffer buff) {
		byte[] tmp;
		if (buff != null) {
//			buff.flip();
			tmp = new byte[buff.remaining()];
			buff.get(tmp);
			buff.compact();
//			buff.flip();
		} else {
			tmp = null;
		}
		return tmp;
	}

	@Override
	public long getBytesReceived(boolean reset) {
		return io.getBytesReceived(reset);
	}

	@Override
	public long getBytesSent(boolean reset) {
		return io.getBytesSent(reset);
	}

	@Override
	public int getInputPacketSize() throws IOException {
		return io.getInputPacketSize();
	}

	@Override
	public SocketChannel getSocketChannel() {
		return io.getSocketChannel();
	}

	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		if (io != null) {
			io.getStatistics(list, reset);
		}
	}

	@Override
	public long getTotalBuffOverflow() {
		return io.getTotalBuffOverflow();
	}

	@Override
	public long getTotalBytesReceived() {
		return io.getTotalBytesReceived();
	}

	@Override
	public long getTotalBytesSent() {
		return io.getTotalBytesSent();
	}

	@Override
	public boolean isConnected() {
		return io.isConnected();
	}

	@Override
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

	private void loadKeys() throws Exception {
		tigase.cert.CertificateEntry kk = certificateContainer.getCertificateEntry(hostname);
		this.privateKey = PrivateKeyFactory.createKey(kk.getPrivateKey().getEncoded());
		this.bcCert = gen(kk.getCertChain());
	}

	private void pumpData() throws IOException {
		int counter = 0;
		int resOut;
		int resIn;

		do {
			++counter;
			resOut = 0;
			// copy outgoing data (S->C)
			int waiting = serverProtocol.getAvailableOutputBytes();
			if (waiting > 0) {
				ByteBuffer bb = ByteBuffer.allocate(waiting);
				int dataLen = serverProtocol.readOutput(bb.array(), 0, bb.array().length);
				if (dataLen > 0) {
					resOut += dataLen;
					// System.out.println("S->C: " + resOut + " wrapped bytes: " + Hex.toHexString(bb.array()));
					bb.position(resOut);
					bb.flip();
					io.write(bb);
				}
			}

			// copy received data (C->S)
			resIn = 0;
			ByteBuffer bb = io.read(tlsInput);

			if (io.bytesRead() > 0) {
				byte[] tmp = getBytes(bb);
				if (tmp != null && tmp.length > 0) {
					resIn += tmp.length;
					// System.out.println("C->S: " + resIn + " wrapped bytes: " + Hex.toHexString(tmp));
					serverProtocol.offerInput(tmp);

				}
			}
		} while ((resIn > 0 || resOut > 0) && counter <= 1000);
	}

	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {
		pumpData();
		bytesRead = serverProtocol.readInput(buff.array(), buff.position(), buff.remaining());
		if (bytesRead > 0) {
			buff.position(buff.position() + bytesRead);
			buff.flip();
		}
		pumpData();

		return buff;
	}

	@Override
	public void setLogId(String logId) {
		io.setLogId(logId);
	}

	@Override
	public void stop() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stop called..." + toString());

			// Thread.dumpStack();
		}

		io.stop();
		serverProtocol.close();
	}

	@Override
	public String toString() {
		return "TLS: " + io.toString();
	}

	@Override
	public boolean waitingToSend() {
		return io.waitingToSend();
	}

	@Override
	public int waitingToSendSize() {
		return io.waitingToSendSize();
	}

	@Override
	public int write(ByteBuffer buff) throws IOException {
		int result;

		pumpData();

		if (buff == null) {
			return io.write(null);
		}

		try {
			serverProtocol.writeApplicationData(buff.array(), buff.position(), buff.remaining());
			result = buff.remaining();
			buff.position(buff.position() + result);
			serverProtocol.flush();

			pumpData();

		} catch (IOException e) {
			e.printStackTrace();
			throw new SSLException(e);
		}

		return result;
	}

	private class TigaseTlsServer
			extends DefaultTlsServer {

		public TigaseTlsServer(TlsCrypto crypto) {
			super(crypto);
		}

		public CertificateRequest getCertificateRequest() throws IOException {
			if (!(needClientAuth || wantClientAuth)) {
				return null;
			}

			short[] certificateTypes = new short[]{ClientCertificateType.rsa_sign, ClientCertificateType.dss_sign,
												   ClientCertificateType.ecdsa_sign};

			Vector serverSigAlgs = null;
			if (TlsUtils.isSignatureAlgorithmsExtensionAllowed(serverVersion)) {
				serverSigAlgs = TlsUtils.getDefaultSupportedSignatureAlgorithms(context);
			}

			Vector certificateAuthorities = new Vector();

			Collection<X500Name> acceptedIssuers = BcTLSIO.this.getAcceptedIssuers();

//      certificateAuthorities.addElement(TlsTestUtils.loadBcCertificateResource("x509-ca-dsa.pem").getSubject());
//      certificateAuthorities.addElement(TlsTestUtils.loadBcCertificateResource("x509-ca-ecdsa.pem").getSubject());
//      certificateAuthorities.addElement(TlsTestUtils.loadBcCertificateResource("x509-ca-rsa.pem").getSubject());

			// All the CA certificates are currently configured with this subject
			//			certificateAuthorities.addElement(new X500Name("CN=BouncyCastle TLS Test CA"));

			if (acceptedIssuers != null) {
				certificateAuthorities.addAll(acceptedIssuers);
			}

			return new CertificateRequest(certificateTypes, serverSigAlgs, certificateAuthorities);
		}

		@Override
		protected TlsCredentialedDecryptor getRSAEncryptionCredentials() {
			org.bouncycastle.tls.Certificate crt = BcTLSIO.this.bcCert;
			AsymmetricKeyParameter pk = BcTLSIO.this.privateKey;
			return new BcDefaultTlsCredentialedDecryptor(crypto, crt, pk);
		}

		@Override
		protected TlsCredentialedSigner getRSASignerCredentials() {
			TlsCryptoParameters crpP = new TlsCryptoParameters(context);
			AsymmetricKeyParameter pk = BcTLSIO.this.privateKey;
			org.bouncycastle.tls.Certificate crt = BcTLSIO.this.bcCert;
			SignatureAndHashAlgorithm alg = new SignatureAndHashAlgorithm(HashAlgorithm.sha1, SignatureAlgorithm.rsa);

			return new BcDefaultTlsCredentialedSigner(crpP, crypto, pk, crt, alg);
		}

		@Override
		public void notifyClientCertificate(Certificate clientCertificate) throws IOException {
			BcTLSIO.this.peerCertificate = clientCertificate;
//				try {
//					X509Certificate[] chain = gen(clientCertificate);
//					if (clientTrustManagers != null) {
//						for (TrustManager ctm : clientTrustManagers) {
//							if (ctm instanceof X509TrustManager) {
//								((X509TrustManager) ctm).checkClientTrusted(chain, "RSA");
//							} else {
//								throw new RuntimeException("Unsupported type of TrustManager " + ctm);
//							}
//						}
//					}
//				} catch (Exception e) {
//					log.log(Level.FINE, "Client certificate is probably untrusted", e);
//					throw new TlsFatalAlert(AlertDescription.certificate_unknown);
//				}
		}

		@Override
		public void notifyHandshakeComplete() throws IOException {
			super.notifyHandshakeComplete();
			BcTLSIO.this.handshakeCompleted = true;
			BcTLSIO.this.tlsUnique = context.exportChannelBinding(ChannelBinding.tls_unique);
			try {
				if (eventHandler != null) {
					eventHandler.handshakeCompleted(fakeWrapper);
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.log(Level.WARNING, "Cannot handle handshakeCompleted handler", e);
				throw new TlsFatalAlert(AlertDescription.internal_error);
			}
		}

		@Override
		public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException {
			// This is required, since the default implementation throws an error if secure reneg is not
			// supported
		}
	}

}