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

//~--- non-JDK imports --------------------------------------------------------

import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;

//~--- JDK imports ------------------------------------------------------------

import java.nio.ByteBuffer;

import java.security.cert.Certificate;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class TLSWrapper here.
 *
 *
 * Created: Sat Mar  5 09:13:29 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TLSWrapper {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(TLSWrapper.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int appBuffSize = 0;
	private String debugId = null;

	// private String protocol = null;
	private TLSEventHandler eventHandler = null;
	private int netBuffSize = 0;
	private SSLEngine tlsEngine = null;
	private SSLEngineResult tlsEngineResult = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>TLSWrapper</code> instance.
	 *
	 *
	 * @param sslc
	 * @param eventHandler
	 * @param clientMode
	 */
	public TLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, boolean clientMode) {
		tlsEngine = sslc.createSSLEngine();

		if ( !clientMode) {
			tlsEngine.getSSLParameters().setWantClientAuth(true);
		}

		tlsEngine.setUseClientMode(clientMode);
		netBuffSize = tlsEngine.getSession().getPacketBufferSize();
		appBuffSize = tlsEngine.getSession().getApplicationBufferSize();
		this.eventHandler = eventHandler;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int bytesConsumed() {
		return tlsEngineResult.bytesConsumed();
	}

	/**
	 * Method description
	 *
	 *
	 * @throws SSLException
	 */
	public void close() throws SSLException {
		tlsEngine.closeOutbound();

		// tlsEngine.closeInbound();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getAppBuffSize() {
		return appBuffSize;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param revocationEnabled
	 * @return
	 */
	public CertCheckResult getCertificateStatus(boolean revocationEnabled) {
		Certificate[] peerChain = null;

		try {
			peerChain = tlsEngine.getSession().getPeerCertificates();
		} catch (SSLPeerUnverifiedException ex) {

			// This normally happens when the peer is in a client mode and does not
			// send any certificate, even though we set: setWantClientAuth(true)
			return CertCheckResult.none;
		}

		try {
			return CertificateUtil.validateCertificate(peerChain, TLSUtil.getTrustStore(),
					revocationEnabled);
		} catch (Exception ex) {
			log.log(Level.WARNING, "Problem validating certificate", ex);
		}

		return CertCheckResult.invalid;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getNetBuffSize() {
		return netBuffSize;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getPacketBuffSize() {
		return tlsEngine.getSession().getPacketBufferSize();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public TLSStatus getStatus() {
		TLSStatus status = null;

		if ((tlsEngineResult != null) && (tlsEngineResult.getStatus() == Status.BUFFER_UNDERFLOW)) {
			status = TLSStatus.UNDERFLOW;

			// status = TLSStatus.NEED_READ;
		}        // end of if (tlsEngine.getStatus() == Status.BUFFER_UNDERFLOW)
				else {
			if ((tlsEngineResult != null) && (tlsEngineResult.getStatus() == Status.CLOSED)) {
				status = TLSStatus.CLOSED;
			}      // end of if (tlsEngine.getStatus() == Status.BUFFER_UNDERFLOW)
					else {
				switch (tlsEngine.getHandshakeStatus()) {
					case NEED_WRAP :
						status = TLSStatus.NEED_WRITE;

						break;

					case NEED_UNWRAP :
						status = TLSStatus.NEED_READ;

						break;

					default :
						status = TLSStatus.OK;

						break;
				}    // end of switch (tlsEngine.getHandshakeStatus())
			}
		}        // end of else

		return status;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isClientMode() {
		return tlsEngine.getUseClientMode();
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id
	 */
	public void setDebugId(String id) {
		debugId = id;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param net
	 * @param app
	 *
	 * @return
	 *
	 * @throws SSLException
	 */
	public ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException {
		ByteBuffer out = app;

		tlsEngineResult = tlsEngine.unwrap(net, out);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"{0}, unwrap() tlsEngineRsult.getStatus() = {1}, "
						+ "tlsEngineRsult.getHandshakeStatus() = {2}", new Object[] { debugId,
					tlsEngineResult.getStatus(), tlsEngineResult.getHandshakeStatus() });
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
						new Object[] { tlsEngine.getHandshakeStatus(),
						debugId });
			}
		}

		return out;
	}

	/**
	 * Method description
	 *
	 *
	 * @param app
	 * @param net
	 *
	 * @throws SSLException
	 */
	public void wrap(ByteBuffer app, ByteBuffer net) throws SSLException {
		tlsEngineResult = tlsEngine.wrap(app, net);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"{0}, tlsEngineRsult.getStatus() = {1}, tlsEngineRsult.getHandshakeStatus() = {2}",
						new Object[] { debugId,
					tlsEngineResult.getStatus(), tlsEngineResult.getHandshakeStatus() });
		}

		if (tlsEngineResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
			if (eventHandler != null) {
				eventHandler.handshakeCompleted(this);
			}
		}

		if (tlsEngineResult.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			doTasks();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "doTasks(): {0}, {1}", new Object[] { tlsEngine.getHandshakeStatus(),
						debugId });
			}
		}
	}

	private void doTasks() {
		Runnable runnable = null;

		while ((runnable = tlsEngine.getDelegatedTask()) != null) {
			runnable.run();
		}    // end of while ((runnable = engine.getDelegatedTask()) != 0)
	}

	/**
	 * Method <code>resizeApplicationBuffer</code> is used to perform
	 */
	private ByteBuffer resizeApplicationBuffer(ByteBuffer net, ByteBuffer app) {

//  if (appBuffSize > app.remaining()) {
//    if (net.remaining() > app.remaining()) {
//    if (appBuffSize > app.capacity() - app.remaining()) {
//      if (log.isLoggable(Level.FINE)) {
//        log.fine("Resizing tlsInput to " + (appBuffSize + app.capacity()) + " bytes.");
//      }
//
//      ByteBuffer bb = ByteBuffer.allocate(app.capacity() + appBuffSize);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Resizing tlsInput to {0} bytes, {1}",
					new Object[] { (2048 + app.capacity()),
					debugId });
		}

		ByteBuffer bb = ByteBuffer.allocate(app.capacity() + 2048);

		// bb.clear();
		app.flip();
		bb.put(app);

		return bb;

//  } else {
//
//    return app;
//  }    // end of else
	}
}    // TLSWrapper


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
