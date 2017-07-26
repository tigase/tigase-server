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
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

public class Experiment {

	final static SecureRandom random = new SecureRandom();

	final static BcTlsCrypto crypto = new BcTlsCrypto(random);

	private static final byte[] cp(byte[] buff, int len) {
		byte[] res = new byte[len];
		System.arraycopy(buff, 0, res, 0, len);
		return res;
	}

	public static Certificate gen(KeyPair keypair) throws Exception {

// fill in certificate fields
		X500Name subject = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, "stackoverflow.com").build();
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

		Certificate zz = new Certificate(arr);

		return zz;
	}

	public static AsymmetricKeyParameter getKeys(KeyPair k) throws IOException {
		return PrivateKeyFactory.createKey(k.getPrivate().getEncoded());
	}

	public static void main(String[] args) throws Exception {

		// create keypair
		KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");
		keypairGen.initialize(2048, random);
		final KeyPair keypair = keypairGen.generateKeyPair();
		final AsymmetricKeyParameter privateKey = getKeys(keypair);

		final Certificate bcCert = gen(keypair);

		// ----

		PemObject pem;
		try (PemReader reader = new PemReader(new FileReader("/Users/bmalkow/tmp/malkowscy.net.pem"))) {
			pem = reader.readPemObject();
		}
		System.out.println("type=" + pem.getType());
		System.out.println("headers=" + pem.getHeaders());

		final TlsClientProtocol clientProtocol = new TlsClientProtocol();
		DefaultTlsClient client = new DefaultTlsClient(crypto) {
			public TlsAuthentication getAuthentication() throws IOException {
				return new ServerOnlyTlsAuthentication() {

					@Override
					public void notifyServerCertificate(Certificate serverCertificate) throws IOException {
						System.out.println("C: notifyServerCertificate: " + serverCertificate);
					}

				};
			}

			@Override
			public void notifyHandshakeComplete() throws IOException {
				super.notifyHandshakeComplete();
				System.out.println("C: notifyHandshakeComplete");
				byte[] cb = context.exportChannelBinding(ChannelBinding.tls_unique);
				System.out.println("C: tls_unique=" + new String(Hex.encode(cb)));
			}
		};

		// ---------------------------------------
		// ---------------------------------------
		// ---------------------------------------
		// ---------------------------------------

		final TlsServerProtocol serverProtocol = new TlsServerProtocol();
		final DefaultTlsServer server = new DefaultTlsServer(crypto) {
			@Override
			protected TlsCredentialedDecryptor getRSAEncryptionCredentials() {
				System.out.println("S: getRSAEncryptionCredentials");

				Certificate crt = bcCert;
				AsymmetricKeyParameter pk = privateKey;
				return new BcDefaultTlsCredentialedDecryptor(crypto, crt, pk);
			}

			@Override
			protected TlsCredentialedSigner getRSASignerCredentials() {
				System.out.println("S: getRSASignerCredentials");

				TlsCryptoParameters crpP = new TlsCryptoParameters(context);
				AsymmetricKeyParameter pk = privateKey;
				Certificate crt = bcCert;
				SignatureAndHashAlgorithm alg = new SignatureAndHashAlgorithm(HashAlgorithm.sha1,
																			  SignatureAlgorithm.rsa);
				return new BcDefaultTlsCredentialedSigner(crpP, crypto, pk, crt, alg);
			}

			@Override
			public void notifyHandshakeComplete() throws IOException {
				super.notifyHandshakeComplete();
				System.out.println("S: notifyHandshakeComplete ");
				byte[] cb = context.exportChannelBinding(ChannelBinding.tls_unique);
				System.out.println("S: tls_unique=" + new String(Hex.encode(cb)));
			}

			@Override
			public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException {
				System.out.println("S: notifySecureRenegotiation " + secureRenegotiation);

				// This is required, since the default implementation throws an error if secure reneg is not
				// supported
			}
		};

		clientProtocol.connect(client);
		serverProtocol.accept(server);

		final long[] counter = new long[]{0, 0};

		Thread worker = new Thread("SSLWorker") {
			@Override
			public void run() {
				final byte[] buff = new byte[1024];

				while (counter[1] == 0) {
					try {
						++counter[0];
						int receivedFromClient = serverProtocol.readInput(buff, 0, buff.length);
						if (receivedFromClient > 0) {
							System.out.println("S << " + new String(cp(buff, receivedFromClient)));
						}

						int receivedFromServer = clientProtocol.readInput(buff, 0, buff.length);
						if (receivedFromServer > 0) {
							System.out.println("C << " + new String(cp(buff, receivedFromServer)));
						}

						int sentFromClient = clientProtocol.readOutput(buff, 0, buff.length);
						if (sentFromClient > 0) {
							byte[] b = cp(buff, sentFromClient);
							System.out.println("C->S: " + sentFromClient + " wrapped bytes: "+Hex.toHexString(b));
							serverProtocol.offerInput(b);
						}

						int sentFromSeerver = serverProtocol.readOutput(buff, 0, buff.length);
						if (sentFromSeerver > 0) {
							byte[] b = cp(buff, sentFromSeerver);
							System.out.println("S->C: " + sentFromSeerver + " wrapped bytes: "+Hex.toHexString(b));
							clientProtocol.offerInput(b);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				System.out.println("WORKER IS DEAD!");
			}
		};
		worker.start();

		Thread.sleep(1000);
		System.out.println("---");
		System.out.println("counter=" + counter[0]);
//		counter[1] = 1;
		Thread.sleep(1000);

		byte[] buff = "Application data sent by client to server".getBytes();
		clientProtocol.writeApplicationData(buff, 0, buff.length);
		clientProtocol.flush();

		buff = "Application data sent by server to client".getBytes();
		serverProtocol.writeApplicationData(buff, 0, buff.length);
		serverProtocol.flush();

		System.out.println("counter=" + counter[0]);
		Thread.sleep(2000);
		System.out.println("counter=" + counter[0]);
		System.out.println("---");

		// Stop worker
		counter[1] = 1;
	}

}
