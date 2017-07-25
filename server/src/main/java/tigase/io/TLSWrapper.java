package tigase.io;

import tigase.cert.CertCheckResult;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;

public interface TLSWrapper {

	int bytesConsumed();

	void close() throws SSLException;

	int getAppBuffSize();

	CertCheckResult getCertificateStatus(boolean revocationEnabled, SSLContextContainerIfc sslContextContainer);

	SSLEngineResult.HandshakeStatus getHandshakeStatus();

	Certificate[] getLocalCertificates();

	int getNetBuffSize();

	int getPacketBuffSize();

	Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException;

	TLSStatus getStatus();

	byte[] getTlsUniqueBindingData();

	boolean isClientMode();

	boolean isNeedClientAuth();

	void setDebugId(String id);

	ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException;

	boolean wantClientAuth();

	void wrap(ByteBuffer app, ByteBuffer net) throws SSLException;
}
