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

import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class TLSWrapper here.
 * <br>
 * Created: Sat Mar 5 09:13:29 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JcaTLSWrapper
		implements TLSWrapper {

	private static final Logger log = Logger.getLogger(JcaTLSWrapper.class.getName());

	private int appBuffSize = 0;
	private String debugId = null;

	// private String protocol = null;
	private TLSEventHandler eventHandler = null;
	private int netBuffSize = 0;
	private SSLEngine tlsEngine = null;
	private SSLEngineResult tlsEngineResult = null;

	public JcaTLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, String hostname, int port,
						 final boolean clientMode, final boolean wantClientAuth) {
		this(sslc, eventHandler, hostname, port, clientMode, wantClientAuth, false);
	}

	/**
	 * Creates a new <code>TLSWrapper</code> instance.
	 *
	 * @param sslc
	 * @param eventHandler
	 * @param hostname
	 * @param port
	 * @param clientMode
	 * @param wantClientAuth
	 */
	public JcaTLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, String hostname, int port,
						 final boolean clientMode, final boolean wantClientAuth, final boolean needClientAuth) {
		this(sslc, eventHandler, hostname, port, clientMode, wantClientAuth, needClientAuth, null, null);
	}

	public JcaTLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, String hostname, int port,
						 final boolean clientMode, final boolean wantClientAuth, final boolean needClientAuth,
						 String[] enabledCiphers, String[] enabledProtocols) {
		if (clientMode && hostname != null) {
			tlsEngine = sslc.createSSLEngine(hostname, port);
		} else {
			tlsEngine = sslc.createSSLEngine();
		}
		tlsEngine.setUseClientMode(clientMode);

		if (enabledCiphers != null) {
			tlsEngine.setEnabledCipherSuites(enabledCiphers);
		}

		if (enabledProtocols != null) {
			tlsEngine.setEnabledProtocols(enabledProtocols);
		}

		netBuffSize = tlsEngine.getSession().getPacketBufferSize();
		appBuffSize = Math.min(eventHandler.getSocketInputSize(), tlsEngine.getSession().getApplicationBufferSize());
		this.eventHandler = eventHandler;

		if (!clientMode && wantClientAuth) {
			tlsEngine.setWantClientAuth(true);
		}
		if (!clientMode && needClientAuth) {
			tlsEngine.setNeedClientAuth(true);
		}

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Created " + (clientMode ? "client" : "server") + " TLSWrapper. Protocols:" +
							   (tlsEngine.getEnabledProtocols() == null
								? " default"
								: Arrays.toString(tlsEngine.getEnabledProtocols())) + "; Ciphers:" +
							   (tlsEngine.getEnabledCipherSuites() == null
								? " default"
								: Arrays.toString(tlsEngine.getEnabledCipherSuites())));
		}

	}

	@Override
	public int bytesConsumed() {
		return tlsEngineResult.bytesConsumed();
	}

	@Override
	public void close() throws SSLException {
		tlsEngine.closeOutbound();
		tlsEngine.getSession().invalidate();
		// tlsEngine.closeInbound();
	}

	@Override
	public int getAppBuffSize() {
		return appBuffSize;
	}

	@Override
	public CertCheckResult getCertificateStatus(boolean revocationEnabled, SSLContextContainerIfc sslContextContainer) {
		Certificate[] peerChain = null;

		try {
			peerChain = tlsEngine.getSession().getPeerCertificates();
		} catch (SSLPeerUnverifiedException ex) {

			// This normally happens when the peer is in a client mode and does
			// not
			// send any certificate, even though we set: setWantClientAuth(true)
			return CertCheckResult.none;
		}

		try {
			return CertificateUtil.validateCertificate(peerChain, sslContextContainer.getTrustStore(),
													   revocationEnabled);
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem validating certificate", ex);
		}

		return CertCheckResult.invalid;
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		return tlsEngine.getHandshakeStatus();
	}

	@Override
	public Certificate[] getLocalCertificates() {
		return tlsEngine.getSession().getLocalCertificates();
	}

	@Override
	public int getNetBuffSize() {
		return netBuffSize;
	}

	@Override
	public int getPacketBuffSize() {
		return tlsEngine.getSession().getPacketBufferSize();
	}

	@Override
	public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
		return tlsEngine.getSession().getPeerCertificates();
	}

	@Override
	public TLSStatus getStatus() {
		TLSStatus status = null;

		if ((tlsEngineResult != null) && (tlsEngineResult.getStatus() == Status.BUFFER_UNDERFLOW)) {
			status = TLSStatus.UNDERFLOW;

			// status = TLSStatus.NEED_READ;
		} // end of if (tlsEngine.getStatus() == Status.BUFFER_UNDERFLOW)
		else {
			if ((tlsEngineResult != null) && (tlsEngineResult.getStatus() == Status.CLOSED)) {
				status = TLSStatus.CLOSED;
			} // end of if (tlsEngine.getStatus() == Status.BUFFER_UNDERFLOW)
			else {
				switch (tlsEngine.getHandshakeStatus()) {
					case NEED_WRAP:
						status = TLSStatus.NEED_WRITE;

						break;

					case NEED_UNWRAP:
						status = TLSStatus.NEED_READ;

						break;

					default:
						status = TLSStatus.OK;

						break;
				} // end of switch (tlsEngine.getHandshakeStatus())
			}
		} // end of else

		return status;
	}

	@Override
	public byte[] getTlsUniqueBindingData() {
		// Because of Java API limitations it always returns null.
		return null;
	}

	@Override
	public boolean isClientMode() {
		return tlsEngine.getUseClientMode();
	}

	@Override
	public boolean isNeedClientAuth() {
		return tlsEngine.getNeedClientAuth();
	}

	@Override
	public void setDebugId(String id) {
		debugId = id;
	}

	@Override
	public ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException {
		ByteBuffer out = app;

		out.order(app.order());
		tlsEngineResult = tlsEngine.unwrap(net, out);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"{0}, unwrap() tlsEngineRsult.getStatus() = {1}, " + "tlsEngineRsult.getHandshakeStatus() = {2}",
					new Object[]{debugId, tlsEngineResult.getStatus(), tlsEngineResult.getHandshakeStatus()});
		}

		if (tlsEngineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
			if (eventHandler != null) {
				eventHandler.handshakeCompleted(this);
			}
		}

		if (tlsEngineResult.getStatus() == Status.BUFFER_OVERFLOW) {
			out = resizeApplicationBuffer(net, out);
			tlsEngineResult = tlsEngine.unwrap(net, out);
		}

		if (tlsEngineResult.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			doTasks();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "unwrap() doTasks(), handshake: {0}, {1}",
						new Object[]{tlsEngine.getHandshakeStatus(), debugId});
			}
		}

		return out;
	}

	@Override
	public boolean wantClientAuth() {
		return tlsEngine.getWantClientAuth();
	}

	@Override
	public void wrap(ByteBuffer app, ByteBuffer net) throws SSLException {
		tlsEngineResult = tlsEngine.wrap(app, net);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, tlsEngineRsult.getStatus() = {1}, tlsEngineRsult.getHandshakeStatus() = {2}",
					new Object[]{debugId, tlsEngineResult.getStatus(), tlsEngineResult.getHandshakeStatus()});
		}

		if (tlsEngineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
			if (eventHandler != null) {
				eventHandler.handshakeCompleted(this);
			}
		}

		if (tlsEngineResult.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			doTasks();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "doTasks(): {0}, {1}", new Object[]{tlsEngine.getHandshakeStatus(), debugId});
			}
		}
	}

	private void doTasks() {
		Runnable runnable = null;

		while ((runnable = tlsEngine.getDelegatedTask()) != null) {
			runnable.run();
		} // end of while ((runnable = engine.getDelegatedTask()) != 0)
	}

	/**
	 * Method <code>resizeApplicationBuffer</code> is used to perform buffer resizing
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

}