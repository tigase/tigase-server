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
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.util.encoders.Hex;
import tigase.stats.StatisticsList;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BcTLSIO
		implements IOInterface {

	public static final String TLS_CAPS = "tls-caps";
	private static final Logger log = Logger.getLogger(BcTLSIO.class.getName());
	private final BcTlsCrypto crypto;
	private final String hostname;
	private final SecureRandom random;
	private final DefaultTlsServer server;
	private final TlsServerProtocol serverProtocol;
	private Certificate bcCert;
	private IOInterface io = null;
	private AsymmetricKeyParameter privateKey;
	/**
	 * <code>tlsInput</code> buffer keeps data decoded from tlsWrapper.
	 */
	private ByteBuffer tlsInput = null;
	private byte[] tlsUnique;

	public BcTLSIO(final IOInterface ioi, String hostname, final ByteOrder order) throws IOException {
		this.random = new SecureRandom();
		this.crypto = new BcTlsCrypto(random);
		this.hostname = hostname;
		io = ioi;
		tlsInput = ByteBuffer.allocate(10240);
		tlsInput.order(order);

		this.serverProtocol = new TlsServerProtocol();
		this.server = new DefaultTlsServer(crypto) {
			@Override
			protected TlsCredentialedDecryptor getRSAEncryptionCredentials() {
				System.out.println("S: getRSAEncryptionCredentials");

				org.bouncycastle.tls.Certificate crt = BcTLSIO.this.bcCert;
				AsymmetricKeyParameter pk = BcTLSIO.this.privateKey;
				return new BcDefaultTlsCredentialedDecryptor(crypto, crt, pk);
			}

			@Override
			protected TlsCredentialedSigner getRSASignerCredentials() {
				System.out.println("S: getRSASignerCredentials");

				TlsCryptoParameters crpP = new TlsCryptoParameters(context);
				AsymmetricKeyParameter pk = BcTLSIO.this.privateKey;
				org.bouncycastle.tls.Certificate crt = BcTLSIO.this.bcCert;
				SignatureAndHashAlgorithm alg = new SignatureAndHashAlgorithm(HashAlgorithm.sha1,
																			  SignatureAlgorithm.rsa);
				return new BcDefaultTlsCredentialedSigner(crpP, crypto, pk, crt, alg);
			}

			@Override
			public void notifyHandshakeComplete() throws IOException {
				super.notifyHandshakeComplete();
				BcTLSIO.this.tlsUnique = context.exportChannelBinding(ChannelBinding.tls_unique);
//				if (eventHandler != null) {
//					eventHandler.handshakeCompleted(BcTLSWrapper.this);
//				}
			}

			@Override
			public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException {
				System.out.println("S: notifySecureRenegotiation " + secureRenegotiation);

				// This is required, since the default implementation throws an error if secure reneg is not
				// supported
			}
		};

		try {
			loadKeys();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		serverProtocol.accept(server);

		pumpData();

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "TLS Socket created: {0}", io.toString());
		}
	}

	@Override
	public int bytesRead() {
		return io.bytesRead();
	}

	@Override
	public boolean checkCapabilities(String caps) {
		return caps.contains(TLS_CAPS) || io.checkCapabilities(caps);
	}

	public org.bouncycastle.tls.Certificate gen(KeyPair keypair) throws Exception {

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
		KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");
		keypairGen.initialize(2048, random);
		final KeyPair keypair = keypairGen.generateKeyPair();
		this.privateKey = PrivateKeyFactory.createKey(keypair.getPrivate().getEncoded());
		this.bcCert = gen(keypair);
	}

	private void pumpData() throws IOException {
		final byte buff[] = new byte[10240];
		int counter = 0;
		int resOut;
		int resIn;

		do {
			++counter;
			resOut = 0;
			// copy outgoing data (S->C)
			int dataLen = serverProtocol.readOutput(buff, 0, buff.length);
			if (dataLen > 0) {
				resOut += dataLen;
				ByteBuffer bb = ByteBuffer.wrap(buff, 0, dataLen);
				System.out.println("S->C: " + resOut + " wrapped bytes: " + Hex.toHexString(bb.array()));
				bb.flip();
				io.write(bb);
			}

			// copy received data (C->S)
			resIn = 0;
			ByteBuffer bb = io.read(ByteBuffer.allocate(10240));

			byte[] tmp = getBytes(bb);
			if (tmp != null && tmp.length > 0) {
				resIn += tmp.length;
				System.out.println("C->S: " + resIn + " wrapped bytes: " + Hex.toHexString(tmp));
				serverProtocol.offerInput(tmp);

			}
		} while ((resIn > 0 || resOut > 0) && counter <= 1000);
	}

	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {
		pumpData();
		ByteBuffer result = null;
		byte[] byteBuff = new byte[10240];
		int receivedFromClient = serverProtocol.readInput(byteBuff, 0, byteBuff.length);
		if (receivedFromClient > 0) {
			result = ByteBuffer.wrap(byteBuff, 0, receivedFromClient);
		}

		pumpData();

		return result == null ? ByteBuffer.allocate(0) : result;
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
		int result = -1;

		pumpData();

		byte[] tmp = getBytes(buff);
		try {
			if (tmp != null && tmp.length > 0) {
				serverProtocol.writeApplicationData(tmp, 0, tmp.length);
				result = tmp.length;
			}
			serverProtocol.flush();

			pumpData();

		} catch (IOException e) {
			e.printStackTrace();
			throw new SSLException(e);
		}

		return result;
	}

}