/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.io;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLEngineResult;
import java.util.logging.Logger;
import java.nio.ByteBuffer;
import tigase.conf.Configurator;

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
	private static Logger log =
		Logger.getLogger("tigase.io.TLSWrapper");

	private SSLEngine tlsEngine = null;
	private SSLEngineResult tlsEngineResult = null;
	private int netBuffSize = 0;
	private int appBuffSize = 0;
	private String protocol = null;
	/**
	 * Creates a new <code>TLSWrapper</code> instance.
	 *
	 */
	public TLSWrapper(SSLContext sslc, boolean clientMode) {
		tlsEngine = sslc.createSSLEngine();
		tlsEngine.setUseClientMode(clientMode);
		netBuffSize = tlsEngine.getSession().getPacketBufferSize();
		appBuffSize = tlsEngine.getSession().getApplicationBufferSize();
	}

	public boolean isClientMode() {
	 	return tlsEngine.getUseClientMode();
	}

	public void close() throws SSLException {
		tlsEngine.closeOutbound();
		//    tlsEngine.closeInbound();
	}

	public int getNetBuffSize() {
		return netBuffSize;
	}

	public int getAppBuffSize() {
		return appBuffSize;
	}

 	public int getPacketBuffSize() {
 		return tlsEngine.getSession().getPacketBufferSize();
 	}

	public TLSStatus getStatus() {
		TLSStatus status = null;
		if (tlsEngineResult != null &&
			tlsEngineResult.getStatus() == Status.BUFFER_UNDERFLOW) {
			status = TLSStatus.UNDERFLOW;
		} // end of if (tlsEngine.getStatus() == Status.BUFFER_UNDERFLOW)
		else {
			if (tlsEngineResult != null &&
				tlsEngineResult.getStatus() == Status.CLOSED) {
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

	public ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException {
		ByteBuffer out = app;
 		out = resizeApplicationBuffer(out);
 		tlsEngineResult = tlsEngine.unwrap(net, out);
 		log.finest("unwrap() \ntlsEngineRsult.getStatus() = "
 			+ tlsEngineResult.getStatus()
			+ "\ntlsEngineRsult.getHandshakeStatus() = "
			+ tlsEngineResult.getHandshakeStatus());
		if (tlsEngineResult.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			doTasks();
			log.finest("unwrap() doTasks(), handshake: " +
				tlsEngine.getHandshakeStatus());
		}
		return out;
	}

	public void wrap(ByteBuffer app, ByteBuffer net) throws SSLException {
		tlsEngineResult = tlsEngine.wrap(app, net);
		log.finest("wrap() \ntlsEngineRsult.getStatus() = "
			+ tlsEngineResult.getStatus()
			+ "\ntlsEngineRsult.getHandshakeStatus() = "
			+ tlsEngineResult.getHandshakeStatus());
		if (tlsEngineResult.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			doTasks();
			log.finest("wrap() doTasks(): " + tlsEngine.getHandshakeStatus());
		}
	}

	/**
	 * Method <code>resizeApplicationBuffer</code> is used to perform 
	 */
	private ByteBuffer resizeApplicationBuffer(ByteBuffer app) {
		if (app.remaining() < appBuffSize) {
			ByteBuffer bb = ByteBuffer.allocate(app.capacity() + appBuffSize);
			//      bb.clear();
			app.flip();
			bb.put(app);
			return bb;
		} // end of if (appInBuff.remaining < appBuffSize)
		else {
			return app;
		} // end of else
	}

	private void doTasks() {
		Runnable runnable = null;
		while ((runnable = tlsEngine.getDelegatedTask()) != null) {
			runnable.run();
		} // end of while ((runnable = engine.getDelegatedTask()) != 0)
	}

} // TLSWrapper
