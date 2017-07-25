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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

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
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import tigase.cert.CertCheckResult;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.Certificate;

/**
 * Describe class TLSWrapper here.
 * <p>
 * <p>
 * Created: Sat Mar 5 09:13:29 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BcTLSWrapper
		implements TLSWrapper {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	protected static final Logger log = Logger.getLogger(BcTLSWrapper.class.getName());
	protected final BcTlsCrypto crypto;
	protected final SecureRandom random;
	private final String[] enabledCiphers;
	private final String[] enabledProtocols;
	private final String hostname;
	private final DefaultTlsServer server;
	private final TlsServerProtocol serverProtocol;
	private final boolean wantClientAuth;
	private int appBuffSize = 0;
	private org.bouncycastle.tls.Certificate bcCert;
	private String debugId = null;
	private TLSEventHandler eventHandler = null;
	private HandshakeStatus handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
	private boolean needClientAuth;
	private int netBuffSize = 0;
	private AsymmetricKeyParameter privateKey;
	private SSLEngineResult tlsEngineResult = null;
	private byte[] tlsUnique;

	public BcTLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, String hostname, int port,
						final boolean clientMode, final boolean wantClientAuth, final boolean needClientAuth,
						String[] enabledCiphers, String[] enabledProtocols) {
		random = new SecureRandom();
		crypto = new BcTlsCrypto(random);
		this.hostname = hostname;
		this.enabledCiphers = enabledCiphers;
		this.enabledProtocols = enabledProtocols;
		this.needClientAuth = needClientAuth;
		this.wantClientAuth = wantClientAuth;

		if (clientMode) {
			log.warning("Client mode is not supported yet.");
			throw new RuntimeException("Client mode is not supported yet.");
		}

		if (needClientAuth || wantClientAuth) {
			log.warning("Client authentication is not supported yet.");
			throw new RuntimeException("Client auth is not supported yet.");
		}

		try {
			loadKeys();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot load keys", e);
		}

		this.serverProtocol = new TlsServerProtocol();
		this.server = new DefaultTlsServer(crypto) {
			@Override
			protected TlsCredentialedDecryptor getRSAEncryptionCredentials() {
				System.out.println("S: getRSAEncryptionCredentials");

				org.bouncycastle.tls.Certificate crt = bcCert;
				AsymmetricKeyParameter pk = privateKey;
				return new BcDefaultTlsCredentialedDecryptor(crypto, crt, pk);
			}

			@Override
			protected TlsCredentialedSigner getRSASignerCredentials() {
				System.out.println("S: getRSASignerCredentials");

				TlsCryptoParameters crpP = new TlsCryptoParameters(context);
				AsymmetricKeyParameter pk = privateKey;
				org.bouncycastle.tls.Certificate crt = bcCert;
				SignatureAndHashAlgorithm alg = new SignatureAndHashAlgorithm(HashAlgorithm.sha1,
																			  SignatureAlgorithm.rsa);
				return new BcDefaultTlsCredentialedSigner(crpP, crypto, pk, crt, alg);
			}

			@Override
			public void notifyHandshakeComplete() throws IOException {
				handshakeStatus = HandshakeStatus.FINISHED;
				super.notifyHandshakeComplete();
				BcTLSWrapper.this.tlsUnique = context.exportChannelBinding(ChannelBinding.tls_unique);
				if (eventHandler != null) {
					eventHandler.handshakeCompleted(BcTLSWrapper.this);
				}
			}

			@Override
			public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException {
				System.out.println("S: notifySecureRenegotiation " + secureRenegotiation);

				// This is required, since the default implementation throws an error if secure reneg is not
				// supported
			}
		};

		netBuffSize = 10240;
		appBuffSize = 10240;
		this.eventHandler = eventHandler;

		try {
			serverProtocol.accept(server);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Method description
	 */
	@Override
	public int bytesConsumed() {
		return tlsEngineResult.bytesConsumed();
	}

	/**
	 * Method description
	 *
	 * @throws SSLException
	 */
	@Override
	public void close() throws SSLException {
		try {
			serverProtocol.close();
		} catch (IOException e) {
			throw new SSLException(e);
		}
	}

	/**
	 * Gets unwrapped (decrypted) data and copy it to out bufer.
	 *
	 * @param out buffer to write decrypted data.
	 */
	private void copyPlainData(final ByteBuffer out) {
		final byte buff[] = new byte[10240];
		int dataLen;

		while ((dataLen = serverProtocol.readInput(buff, 0, buff.length)) > 0) {
			if (dataLen > 0) {
				System.err.println("C->S: "+dataLen);
				out.put(buff, 0, dataLen);
			}
		}
	}

	/**
	 * Gets (wrapped) data ready to send to client to copy it to out buffer.
	 *
	 * @param out buffer to write data.
	 */
	private void copyWrappedData(final ByteBuffer out) {
		final byte buff[] = new byte[10240];
		int dataLen;
		while ((dataLen = serverProtocol.readOutput(buff, 0, buff.length)) > 0) {
			if (dataLen > 0) {
				System.err.println("S->C: "+dataLen);
				out.put(buff, 0, dataLen);
			}
		}
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

	/**
	 * Method description
	 */
	@Override
	public int getAppBuffSize() {
		return appBuffSize;
	}

	private byte[] getBytes(final ByteBuffer buff) {
		byte[] tmp;
		if (buff != null) {
			buff.flip();
			tmp = new byte[buff.remaining()];
			buff.get(tmp);
			buff.compact();
			buff.flip();
		} else {
			tmp = null;
		}
		return tmp;
	}

	@Override
	public CertCheckResult getCertificateStatus(boolean revocationEnabled, SSLContextContainerIfc sslContextContainer) {
		// TODO
		return CertCheckResult.none;
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		return handshakeStatus;
	}

	@Override
	public java.security.cert.Certificate[] getLocalCertificates() {
		try {
			final CertificateFactory factory = CertificateFactory.getInstance("RSA");
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			bcCert.encode(b);
			InputStream inputStream = new ByteArrayInputStream(b.toByteArray());
			return factory.generateCertPath(inputStream)
					.getCertificates()
					.toArray(new java.security.cert.Certificate[]{});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot create Certificate", e);
		}
	}

	/**
	 * Method description
	 */
	@Override
	public int getNetBuffSize() {
		return netBuffSize;
	}

	/**
	 * Method description
	 */
	@Override
	public int getPacketBuffSize() {
		// TODO ??? what should be here?
		return 10240;
	}

	@Override
	public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
		return null;
	}

	/**
	 * Method description
	 */
	@Override
	public TLSStatus getStatus() {
		if (serverProtocol.isClosed()) {
			return TLSStatus.CLOSED;
		}
		TLSStatus status = TLSStatus.OK;
		return status;
	}

	@Override
	public byte[] getTlsUniqueBindingData() {
		// Because of Java API limitations it always returns null.
		return null;
	}

	@Override
	public boolean isClientMode() {
		return false;
	}

	@Override
	public boolean isNeedClientAuth() {
		return needClientAuth;
	}

	private void loadKeys() throws Exception {
		KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");
		keypairGen.initialize(2048, random);
		final KeyPair keypair = keypairGen.generateKeyPair();
		this.privateKey = PrivateKeyFactory.createKey(keypair.getPrivate().getEncoded());
		this.bcCert = gen(keypair);
	}

	/**
	 * Method <code>resizeApplicationBuffer</code> is used to perform
	 */
	private ByteBuffer resizeApplicationBuffer(ByteBuffer net, ByteBuffer app) {

		// if (appBuffSize > app.remaining()) {
		// if (net.remaining() > app.remaining()) {
		// if (appBuffSize > app.capacity() - app.remaining()) {
		// if (log.isLoggable(Level.FINE)) {
		// log.fine("Resizing tlsInput to " + (appBuffSize + app.capacity()) +
		// " bytes.");
		// }
		//
		// ByteBuffer bb = ByteBuffer.allocate(app.capacity() + appBuffSize);
		int newSize = app.capacity() * 2;
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Resizing tlsInput to {0} bytes, {1}", new Object[]{newSize, debugId});
		}

		ByteBuffer bb = ByteBuffer.allocate(newSize);

		// bb.clear();
		bb.order(app.order());
		app.flip();
		bb.put(app);

		return bb;

		// } else {
		//
		// return app;
		// } // end of else
	}

	/**
	 * Method description
	 *
	 * @param id
	 */
	@Override
	public void setDebugId(String id) {
		debugId = id;
	}

	/**
	 * Method description
	 *
	 * @param net
	 * @param app
	 *
	 * @throws SSLException
	 */
	@Override
	public ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException {
		ByteBuffer out = app;
		out.order(app.order());

		byte[] tmp = getBytes(net);

		try {
			if (tmp != null && tmp.length > 0) {
				serverProtocol.offerInput(tmp);
			}

			copyPlainData(app);

		} catch (IOException e) {
			e.printStackTrace();
			throw new SSLException(e);
		}

		return out;
	}

	@Override
	public boolean wantClientAuth() {
		return wantClientAuth;
	}

	/**
	 * Method description
	 *
	 * @param app
	 * @param net
	 *
	 * @throws SSLException
	 */
	@Override
	public void wrap(final ByteBuffer app, final ByteBuffer net) throws SSLException {
		byte[] tmp = getBytes(app);
		try {
			if (tmp != null && tmp.length > 0) {
				serverProtocol.writeApplicationData(tmp, 0, tmp.length);
			}

			serverProtocol.flush();

			copyWrappedData(net);

		} catch (IOException e) {
			e.printStackTrace();
			throw new SSLException(e);
		}
	}

}