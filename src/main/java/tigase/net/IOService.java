/*
 * IOService.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.net;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;
import tigase.io.*;
import tigase.stats.StatisticsList;
import tigase.util.IOListener;
import tigase.xmpp.JID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>IOService</code> offers thread safe
 * <code>call()</code> method execution, however you must be prepared that other
 * methods can be called simultaneously like
 * <code>stop()</code>,
 * <code>getProtocol()</code> or
 * <code>isConnected()</code>. <br> It is recommended that developers extend
 * <code>AbsractServerService</code> rather then implement
 * <code>ServerService</code> interface directly. <p> If you directly implement
 * <code>ServerService</code> interface you must take care about
 * <code>SocketChannel</code> I/O, queuing tasks, processing results and thread
 * safe execution of
 * <code>call()</code> method. If you however extend
 * <code>IOService</code> class all this basic operation are implemented and you
 * have only to take care about parsing data received from network socket.
 * Parsing data is expected to be implemented in
 * <code>parseData(char[] data)</code> method. </p>
 *
 * <p> Created: Tue Sep 28 23:00:34 2004 </p>
 *
 * @param <RefObject> is a reference object stored by this service. This is e
 * reference to higher level data object keeping more information about the
 * connection.
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class IOService<RefObject>
				implements Callable<IOService<?>>, TLSEventHandler,
				IOListener {
	/**
	 * Field description
	 */
	public static final String CERT_CHECK_RESULT = "cert-check-result";
	
	/**
	 * Field description
	 */
	public static final String CERT_REQUIRED_DOMAIN = "cert-required-domain";

	/**
	 * Field description
	 */
	public static final String HOSTNAME_KEY = "hostname-key";

	/**
	 * Field description
	 */
	public static final String PORT_TYPE_PROP_KEY = "type";

	/**
	 * This is key used to store session ID in temporary session data
	 * storage. As it is used by many components it is required that all
	 * components access session ID with this constant.
	 */
	public static final String SESSION_ID_KEY = "sessionID";

	/** Field description */
	public static final String SSL_PROTOCOLS_KEY = "ssl-protocols";

	/**
	 * Variable
	 * <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(IOService.class.getName());
	private static final long   MAX_ALLOWED_EMPTY_CALLS = 1000;

	//~--- fields ---------------------------------------------------------------

	/**
	 * The saved partial bytes for multi-byte UTF-8 characters between reads
	 */
	protected byte[]       partialCharacterBytes = null;
	private JID            connectionId          = null;
	private ConnectionType connectionType        = null;
	private JID            dataReceiver          = null;

	/**
	 * Field description
	 */
	private long   empty_read_call_count = 0;
	private String id                    = null;

	/**
	 * This variable keeps the time of last transfer in any direction it is
	 * used to help detect dead connections.
	 */
	private long                                    lastTransferTime = 0;
	private String                                  local_address    = null;
	private long[]                                  rdData           = new long[60];
	private RefObject                               refObject        = null;
	private String                                  remote_address   = null;
	private IOServiceListener<IOService<RefObject>> serviceListener  = null;
	private boolean socketServiceReady = false;

	/**
	 * <code>socketInput</code> buffer keeps data read from socket.
	 */
	private ByteBuffer                    socketInput     = null;
	private int                           socketInputSize = 2048;
	private IOInterface                   socketIO        = null;
	private boolean                       stopping        = false;
	private long[]                        wrData          = new long[60];
	private ConcurrentMap<String, Object> sessionData = new ConcurrentHashMap<String,
			Object>(4, 0.75f, 4);

	// properties from block below should not be used without proper knowledge
	// ----- BEGIN ---------------------------------------------------------------

	/** Field description */
	protected CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();

	/** Field description */
	protected CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

	/** Field description */
	protected CharBuffer        cb              = CharBuffer.allocate(2048);
	private final ReentrantLock writeInProgress = new ReentrantLock();
	private final ReentrantLock readInProgress  = new ReentrantLock();
	private TrustManager[]      x509TrustManagers;
	private int bufferLimit = 0;

	private Certificate peerCertificate;


	private byte[] tlsUniqueId;
	private Certificate localCertificate;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method
	 * <code>accept</code> is used to perform
	 *
	 * @param socketChannel a
	 * <code>SocketChannel</code> value
	 *
	 * @throws IOException
	 */
	public void accept(final SocketChannel socketChannel) throws IOException {
		try {
			if (socketChannel.isConnectionPending()) {
				socketChannel.finishConnect();
			}    // end of if (socketChannel.isConnecyionPending())
			socketIO = new SocketIO(socketChannel);
		} catch (IOException e) {
			String host = (String) sessionData.get("remote-hostname");

			if (host == null) {
				host = (String) sessionData.get("remote-host");
			}

			String sock_str = null;

			try {
				sock_str = socketChannel.socket().toString();
			} catch (Exception ex) {
				sock_str = ex.toString();
			}
			log.log(Level.FINER,
					"Problem connecting to remote host: {0}, address: {1}, socket: {2} - exception: {3}, session data: {4}",
					new Object[] { host,
					remote_address, sock_str, e, sessionData });

			throw e;
		}
		socketInputSize = socketIO.getSocketChannel().socket().getReceiveBufferSize();
		socketInput     = ByteBuffer.allocate(socketInputSize);
		socketInput.order(byteOrder());

		Socket sock = socketIO.getSocketChannel().socket();

		local_address  = sock.getLocalAddress().getHostAddress();
		remote_address = sock.getInetAddress().getHostAddress();
		id = local_address + "_" + sock.getLocalPort() + "_" + remote_address + "_" + sock
				.getPort();
		setLastTransferTime();
	}

	@Override
	public IOService<?> call() throws IOException {
		writeData(null);

		boolean readLock = true;

		if (stopping) {
			stop();
		} else {
			readLock = readInProgress.tryLock();
			if (readLock) {
				try {
					processSocketData();
					if ((receivedPackets() > 0) && (serviceListener != null)) {
						serviceListener.packetsReady(this);
					}    // end of if (receivedPackets.size() > 0)
				} finally {
					readInProgress.unlock();
					if (!isConnected()) {
						// added to sooner detect disconnection of peer - ie. client
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "{0}, stopping connection due to the fact that it was disconnected, forceStop()", toString());
						}
						forceStop();
					}
				}
			}
		}

		return readLock && socketServiceReady
				? this
				: null;
	}
	
	@Override
	public boolean checkBufferLimit(int bufferSize) {
		return (bufferLimit == 0 || bufferSize <= bufferLimit);
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public ConnectionType connectionType() {
		return this.connectionType;
	}

	/**
	 * Method description
	 *
	 */
	public void forceStop() {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Socket: {0}, Force stop called...", socketIO);
		}
		try {
			if ((socketIO != null) && socketIO.isConnected()) {
				synchronized (socketIO) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Calling stop on: {0}", socketIO);
					}
					socketIO.stop();
				}
			}
		} catch (Exception e) {

			// Well, do nothing, we are closing the connection anyway....
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: " + socketIO +
						", Exception while stopping service: " + connectionId, e);
			}
		} finally {
			if (serviceListener != null) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Calling stop on the listener: {0}", serviceListener);
				}

				IOServiceListener<IOService<RefObject>> tmp = serviceListener;

				serviceListener = null;

				// The temp can still be null if the forceStop is called concurrently
				if (tmp != null) {
					tmp.serviceStopped(this);
				}
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Service listener is null: {0}", socketIO);
				}
			}
		}
	}

	@Override
	public void handshakeCompleted(TLSWrapper wrapper) {
		String reqCertDomain = (String) getSessionData().get(CERT_REQUIRED_DOMAIN);
		CertCheckResult certCheckResult = wrapper.getCertificateStatus(false);
		if (reqCertDomain != null) { 
			// if reqCertDomain is set then verify if certificate got from server
			// is allowed for reqCertDomain
			try {
				Certificate[] certs = wrapper.getTlsEngine().getSession()
						.getPeerCertificates();
				if (certs != null && certs.length > 0) {
					Certificate peerCert = certs[0];
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, TLS handshake veryfing if certificate from connection matches domain {1}", 
								new Object[]{this, reqCertDomain});
					}
					if (!CertificateUtil.verifyCertificateForDomain((X509Certificate) peerCert, reqCertDomain)) {
						certCheckResult = CertCheckResult.invalid;
					}
				}
			} catch (Exception e) {
				certCheckResult = CertCheckResult.invalid;
			}
		}
		sessionData.put(CERT_CHECK_RESULT, certCheckResult);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, TLS handshake completed: {1}", new Object[]{this,
					certCheckResult});
		}
		if (!wrapper.getTlsEngine().getUseClientMode()) {
			this.tlsUniqueId = wrapper.getTlsUniqueBindingData();
			try {
				Certificate[] certs = wrapper.getTlsEngine().getSession().getLocalCertificates();
				this.localCertificate = certs == null || certs.length == 0 ? null : certs[0];
			} catch (Exception e) {
				this.localCertificate = null;
				log.log(Level.WARNING, "Cannot get local certificate", e);
			}
		}
		if (!wrapper.getTlsEngine().getUseClientMode() && (wrapper.getTlsEngine().getWantClientAuth() || wrapper.getTlsEngine().getNeedClientAuth())) {
			try {
				Certificate[] certs = wrapper.getTlsEngine().getSession()
						.getPeerCertificates();
				this.peerCertificate = certs[certs.length - 1];

			} catch (SSLPeerUnverifiedException e) {
				this.peerCertificate = null;
			} catch (Exception e) {
				this.peerCertificate = null;
				log.log(Level.WARNING, "Problem with extracting subjectAltName", e);
			}
		}
		serviceListener.tlsHandshakeCompleted(this);
	}

	/**
	 * Method description
	 *
	 *
	 * @throws IOException
	 */
	public abstract void processWaitingPackets() throws IOException;

	/**
	 * Method description
	 *
	 *
	 * @param clientMode
	 *
	 * @throws IOException
	 */
	public void startSSL(boolean clientMode, boolean wantClientAuth, boolean needClientAuth) throws IOException {
		if (socketIO instanceof TLSIO) {
			throw new IllegalStateException("SSL mode is already activated.");
		}

		String tls_hostname = null;
		int port = 0;
		if (clientMode) {
			tls_hostname = (String) this.getSessionData().get("remote-host");
			if (tls_hostname == null) 
				tls_hostname = (String) this.getSessionData().get("remote-hostname");
			port = ((InetSocketAddress) socketIO.getSocketChannel().getRemoteAddress()).getPort();
		}
		SSLContext sslContext;

		if (x509TrustManagers != null) {
			sslContext = TLSUtil.getSSLContext("SSL", tls_hostname, clientMode, x509TrustManagers);
		} else {
			sslContext = TLSUtil.getSSLContext("SSL", tls_hostname, clientMode);
		}

		TLSWrapper wrapper = new TLSWrapper(sslContext, this, tls_hostname, port, clientMode, wantClientAuth, needClientAuth);

		socketIO = new TLSIO(socketIO, wrapper, byteOrder());
		setLastTransferTime();
		encoder.reset();
		decoder.reset();
	}

	/**
	 * Method description
	 *
	 *
	 * @param clientMode
	 *
	 * @throws IOException
	 */
	public void startTLS(boolean clientMode, boolean wantClientAuth, boolean needClientAuth) throws IOException {
		if (socketIO.checkCapabilities(TLSIO.TLS_CAPS)) {
			throw new IllegalStateException("TLS mode is already activated " + connectionId);
		}

		// This should not take more then 100ms
	int counter = 0;

		while (isConnected() && waitingToSend() && (++counter < 10)) {
			writeData(null);
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {}
		}
		if (counter >= 10) {
			stop();
		} else {
			String tls_hostname = (String) sessionData.get(HOSTNAME_KEY);
			int port = 0;
			if (clientMode) {
				port = ((InetSocketAddress) socketIO.getSocketChannel().getRemoteAddress()).getPort();
				if (tls_hostname == null) {
					tls_hostname = (String) this.getSessionData().get("remote-host");
					if (tls_hostname == null) 
						tls_hostname = (String) this.getSessionData().get("remote-hostname");
				}
			}
			
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Starting TLS for domain: {1}", new Object[] { this,
						tls_hostname });
			}

			SSLContext sslContext;

			if (x509TrustManagers != null) {
				sslContext = TLSUtil.getSSLContext("TLS", tls_hostname, clientMode,
						x509TrustManagers);
			} else {
				sslContext = TLSUtil.getSSLContext("TLS", tls_hostname, clientMode);
			}

			TLSWrapper wrapper = new TLSWrapper(sslContext, this, tls_hostname, port, clientMode, wantClientAuth, needClientAuth);

			socketIO = new TLSIO(socketIO, wrapper, byteOrder());
			setLastTransferTime();
			encoder.reset();
			decoder.reset();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param level
	 */
	public void startZLib(int level) {
		if (socketIO.checkCapabilities(ZLibIO.ZLIB_CAPS)) {
			throw new IllegalStateException("ZLIB mode is already activated.");
		}
		socketIO = new ZLibIO(socketIO, level);
		((ZLibIO) socketIO).setIOListener(this);
	}

	/**
	 * Describe
	 * <code>stop</code> method here.
	 *
	 */
	public void stop() {
		if ((socketIO != null) && socketIO.waitingToSend()) {
			stopping = true;
		} else {
			forceStop();
		}
	}

	@Override
	public String toString() {
		return getConnectionId() + ", type: " + connectionType + ", Socket: " + socketIO;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public boolean waitingToRead() {
		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public boolean waitingToSend() {
		return socketIO.waitingToSend();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int waitingToSendSize() {
		return socketIO.waitingToSendSize();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * 
	 */
	public long getBuffOverflow(boolean reset) {
		return socketIO.getBuffOverflow(reset);
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * 
	 */
	public long getBytesReceived(boolean reset) {
		return socketIO.getBytesReceived(reset);
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * 
	 */
	public long getBytesSent(boolean reset) {
		return socketIO.getBytesSent(reset);
	}

	/**
	 * @return the connectionId
	 */
	public JID getConnectionId() {
		return connectionId;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public JID getDataReceiver() {
		return this.dataReceiver;
	}

	/**
	 * This method returns the time of last transfer in any direction
	 * through this service. It is used to help detect dead connections.
	 *
	 * 
	 */
	public long getLastTransferTime() {
		return lastTransferTime;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getLocalAddress() {
		return local_address;
	}

	public byte[] getTlsUniqueId() {
		return tlsUniqueId;
	}

	/**
	 * Method returns local port of opened socket
	 * 
	 * @return 
	 */
	public int getLocalPort() {
		Socket sock = socketIO.getSocketChannel().socket();
		return sock.getLocalPort();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long[] getReadCounters() {
		return rdData;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public RefObject getRefObject() {
		return refObject;
	}

	/**
	 * Returns a remote IP address for the TCP/IP connection.
	 *
	 *
	 * @return a remote IP address for the TCP/IP connection.
	 */
	public String getRemoteAddress() {
		return remote_address;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public ConcurrentMap<String, Object> getSessionData() {
		return sessionData;
	}

	public int getSocketInputSize() {
		return socketInputSize;
	}

	/**
	 * Method
	 * <code>getSocketChannel</code> is used to perform
	 *
	 * @return a
	 * <code>SocketChannel</code> value
	 */
	public SocketChannel getSocketChannel() {
		return socketIO.getSocketChannel();
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 * @param reset
	 */
	public void getStatistics(StatisticsList list, boolean reset) {
		if (socketIO != null) {
			socketIO.getStatistics(list, reset);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getTotalBuffOverflow() {
		return socketIO.getTotalBuffOverflow();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getTotalBytesReceived() {
		return socketIO.getTotalBytesReceived();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getTotalBytesSent() {
		return socketIO.getTotalBytesSent();
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getUniqueId() {
		return id;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long[] getWriteCounters() {
		return wrData;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public TrustManager[] getX509TrustManagers() {
		return x509TrustManagers;
	}

	/**
	 * Describe
	 * <code>isConnected</code> method here.
	 *
	 * @return a
	 * <code>boolean</code> value
	 */
	public boolean isConnected() {
		boolean result = (socketIO == null)
				? false
				: socketIO.isConnected();

		if (log.isLoggable(Level.FINEST)) {

			// if (socketIO.getSocketChannel().socket().getLocalPort() == 5269) {
			// Throwable thr = new Throwable();
			//
			// thr.fillInStackTrace();
			// log.log(Level.FINEST, "Socket: " + socketIO + ", Connected: " + result,
			// thr);
			// }
			log.log(Level.FINEST, "Socket: {0}, Connected: {1}, id: {2}", new Object[] {
					socketIO,
					result, connectionId });
		}

		return result;
	}

	//~--- set methods ----------------------------------------------------------

	public void setBufferLimit(int bufferLimit) {
		this.bufferLimit = bufferLimit;
	}
	
	/**
	 * @param connectionId the connectionId to set
	 */
	public void setConnectionId(JID connectionId) {
		this.connectionId = connectionId;
		socketIO.setLogId(connectionId.toString());
	}

	/**
	 * Method description
	 *
	 *
	 * @param address
	 */
	public void setDataReceiver(JID address) {
		this.dataReceiver = address;
	}

	/**
	 * Method description
	 *
	 *
	 * @param sl
	 */

	// @SuppressWarnings("unchecked")
	public void setIOServiceListener(IOServiceListener<IOService<RefObject>> sl) {
		this.serviceListener = sl;
	}

	/**
	 * Method description
	 *
	 *
	 * @param refObject
	 */
	public void setRefObject(RefObject refObject) {
		this.refObject = refObject;
	}

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	public void setSessionData(Map<String, Object> props) {

		// Sometimes, some values are null which is allowed in the original Map
		// however, ConcurrentHashMap does not allow nulls as value so we have
		// to copy Maps carefully.
		sessionData = new ConcurrentHashMap<String, Object>(props.size());
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getValue() != null) {
				sessionData.put(entry.getKey(), entry.getValue());
			}
		}
		connectionType = ConnectionType.valueOf(sessionData.get(PORT_TYPE_PROP_KEY)
				.toString());
	}

	/**
	 * Method description
	 *
	 *
	 * @param trustManager
	 */
	public void setX509TrustManagers(TrustManager[] trustManager) {
		this.x509TrustManagers = trustManager;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	protected ByteOrder byteOrder() {
		return ByteOrder.BIG_ENDIAN;
	}

	/**
	 * Describe
	 * <code>debug</code> method here.
	 *
	 * @param msg a
	 * <code>char[]</code> value
	 * @return a
	 * <code>boolean</code> value
	 */
	protected boolean debug(final char[] msg) {
		if (msg != null) {
			System.out.print(new String(msg));

			// log.finest("\n" + new String(msg) + "\n");
		}    // end of if (msg != null)

		return true;
	}

	/**
	 * Describe
	 * <code>debug</code> method here.
	 *
	 * @param msg a
	 * <code>String</code> value
	 * @param prefix
	 * @return a
	 * <code>boolean</code> value
	 */
	protected boolean debug(final String msg, final String prefix) {
		if (log.isLoggable(Level.FINEST)) {
			if ((msg != null) && (msg.trim().length() > 0)) {
				String log_msg = "\n" + ((connectionType() != null)
						? connectionType().toString()
						: "null-type") + " " + prefix + "\n" + msg + "\n";

				// System.out.print(log_msg);
				log.finest(log_msg);
			}
		}

		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @throws IOException
	 */
	protected abstract void processSocketData() throws IOException;

	/**
	 * Method description
	 *
	 *
	 * 
	 *
	 * @throws IOException
	 */
	protected ByteBuffer readBytes() throws IOException {
		setLastTransferTime();
		if (log.isLoggable(Level.FINEST) && (empty_read_call_count > 10)) {
			Throwable thr = new Throwable();

			thr.fillInStackTrace();
			log.log(Level.FINEST, "Socket: " + socketIO, thr);
		}
		try {
			ByteBuffer tmpBuffer = socketIO.read(socketInput);

			if (socketIO.bytesRead() > 0) {
				empty_read_call_count = 0;

				return tmpBuffer;
			} else {
				if ((++empty_read_call_count) > MAX_ALLOWED_EMPTY_CALLS && (!writeInProgress
						.isLocked())) {
					log.log(Level.WARNING,
							"Socket: {0}, Max allowed empty calls excceeded, closing connection.",
							socketIO);
					forceStop();
				}
			}
		} catch (BufferUnderflowException ex) {
			resizeInputBuffer();

			return readBytes();
		} catch (Exception eof) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: " + socketIO + ", Exception reading data", eof);
			}

			// eof.printStackTrace();
			forceStop();
		}

		return null;
	}

	/**
	 * Method description
	 *
	 */
	protected void readCompleted() {
		decoder.reset();
	}

	/**
	 * Describe
	 * <code>readData</code> method here.
	 *
	 * @return a
	 * <code>char[]</code> value
	 * @exception IOException if an error occurs
	 */
	protected char[] readData() throws IOException {
		setLastTransferTime();
		if (log.isLoggable(Level.FINEST) && (empty_read_call_count > 10)) {
			Throwable thr = new Throwable();

			thr.fillInStackTrace();
			log.log(Level.FINEST, "Socket: " + socketIO, thr);
		}

		// Generally it should not happen as it is called only in
		// call() which has concurrent call protection.
		// synchronized (socketIO) {
		try {

			// resizeInputBuffer();
			// Maybe we can shrink the input buffer??
			if ((socketInput.capacity() > socketInputSize) && (socketInput.remaining() ==
					socketInput.capacity())) {

				// Yes, looks like we can
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Socket: {0}, Resizing socketInput down to {1} bytes.",
							new Object[] { socketIO,
							socketInputSize });
				}
				socketInput = ByteBuffer.allocate(socketInputSize);
				socketInput.order(byteOrder());
				cb = CharBuffer.allocate(socketInputSize * 4);
			}

			// if (log.isLoggable(Level.FINEST)) {
			// log.finer("Before read from socket.");
			// log.finer("socketInput.capacity()=" + socketInput.capacity());
			// log.finer("socketInput.remaining()=" + socketInput.remaining());
			// log.finer("socketInput.limit()=" + socketInput.limit());
			// log.finer("socketInput.position()=" + socketInput.position());
			// }
			ByteBuffer tmpBuffer = socketIO.read(socketInput);

			if (socketIO.bytesRead() > 0) {
				empty_read_call_count = 0;

				char[] result = null;

				// There might be some characters read from the network
				// but the buffer may still be null or empty because there might
				// be not enough data to decode TLS or compressed buffer.
				if (tmpBuffer != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket: {0}, Reading network binary data: {1}",
								new Object[] { socketIO,
								socketIO.bytesRead() });
					}

					// Restore the partial bytes for multibyte UTF8 characters
					if (partialCharacterBytes != null) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Reloading partial bytes " + partialCharacterBytes.length);
						}

						ByteBuffer oldTmpBuffer = tmpBuffer;

						tmpBuffer = ByteBuffer.allocate(partialCharacterBytes.length + oldTmpBuffer
								.remaining() + 2);
						tmpBuffer.order(byteOrder());
						tmpBuffer.put(partialCharacterBytes);
						tmpBuffer.put(oldTmpBuffer);
						tmpBuffer.flip();
						oldTmpBuffer.clear();
						partialCharacterBytes = null;
					}

					// if (log.isLoggable(Level.FINEST)) {
					// log.finer("Before decoding data");
					// log.finer("socketInput.capacity()=" + socketInput.capacity());
					// log.finer("socketInput.remaining()=" + socketInput.remaining());
					// log.finer("socketInput.limit()=" + socketInput.limit());
					// log.finer("socketInput.position()=" + socketInput.position());
					// log.finer("tmpBuffer.capacity()=" + tmpBuffer.capacity());
					// log.finer("tmpBuffer.remaining()=" + tmpBuffer.remaining());
					// log.finer("tmpBuffer.limit()=" + tmpBuffer.limit());
					// log.finer("tmpBuffer.position()=" + tmpBuffer.position());
					// log.finer("cb.capacity()=" + cb.capacity());
					// log.finer("cb.remaining()=" + cb.remaining());
					// log.finer("cb.limit()=" + cb.limit());
					// log.finer("cb.position()=" + cb.position());
					// }
					// tmpBuffer.flip();
					if (cb.capacity() < tmpBuffer.remaining() * 4) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Socket: {0}, resizing character buffer to: {1}",
									new Object[] { socketIO,
									tmpBuffer.remaining() });
						}
						cb = CharBuffer.allocate(tmpBuffer.remaining() * 4);
					}

					CoderResult cr = decoder.decode(tmpBuffer, cb, false);

					if (cr.isMalformed()) {
						throw new MalformedInputException(tmpBuffer.remaining());
					}
					if (cb.remaining() > 0) {
						cb.flip();
						result = new char[cb.remaining()];
						cb.get(result);
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Socket: {0}, Decoded character data: {1}",
									new Object[] { socketIO,
									new String(result) });
						}

						// if (log.isLoggable(Level.FINEST)) {
						// log.finer("Just after decoding.");
						// log.finer("tmpBuffer.capacity()=" + tmpBuffer.capacity());
						// log.finer("tmpBuffer.remaining()=" + tmpBuffer.remaining());
						// log.finer("tmpBuffer.limit()=" + tmpBuffer.limit());
						// log.finer("tmpBuffer.position()=" + tmpBuffer.position());
						// log.finer("cb.capacity()=" + cb.capacity());
						// log.finer("cb.remaining()=" + cb.remaining());
						// log.finer("cb.limit()=" + cb.limit());
						// log.finer("cb.position()=" + cb.position());
						// }
					}
					if (cr.isUnderflow() && (tmpBuffer.remaining() > 0)) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Socket: {0}, UTF-8 decoder data underflow: {1}",
									new Object[] { socketIO,
									tmpBuffer.remaining() });
						}

						// Save the partial bytes of a multibyte character such that they
						// can be restored on the next read.
						partialCharacterBytes = new byte[tmpBuffer.remaining()];
						tmpBuffer.get(partialCharacterBytes);
					}
					tmpBuffer.clear();
					cb.clear();

					// if (log.isLoggable(Level.FINEST)) {
					// log.finer("Before return from method.");
					// log.finer("tmpBuffer.capacity()=" + tmpBuffer.capacity());
					// log.finer("tmpBuffer.remaining()=" + tmpBuffer.remaining());
					// log.finer("tmpBuffer.limit()=" + tmpBuffer.limit());
					// log.finer("tmpBuffer.position()=" + tmpBuffer.position());
					// log.finer("cb.capacity()=" + cb.capacity());
					// log.finer("cb.remaining()=" + cb.remaining());
					// log.finer("cb.limit()=" + cb.limit());
					// log.finer("cb.position()=" + cb.position());
					// }
					return result;
				}
			} else {

				// Detecting infinite read 0 bytes
				// sometimes it happens that the connection has been lost
				// and the select thinks there are some bytes waiting for reading
				// and 0 bytes are read
				if ((++empty_read_call_count) > MAX_ALLOWED_EMPTY_CALLS && (!writeInProgress
						.isLocked())) {
					log.log(Level.WARNING,
							"Socket: {0}, Max allowed empty calls excceeded, closing connection.",
							socketIO);
					forceStop();
				}
			}
		} catch (BufferUnderflowException ex) {

			// Obtain more inbound network data for src,
			// then retry the operation.
			resizeInputBuffer();

			return null;

			// } catch (MalformedInputException ex) {
			//// This happens after TLS initialization sometimes, maybe reset helps
			// decoder.reset();
		} catch (Exception eof) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: " + socketIO + ", Exception reading data", eof);
			}

			// eof.printStackTrace();
			forceStop();
		}    // end of try-catch

		// }
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	protected abstract int receivedPackets();

	/**
	 * Method description
	 *
	 *
	 * @param data
	 */
	protected void writeBytes(ByteBuffer data) {

		// Try to lock the data writing method
		boolean locked = writeInProgress.tryLock();

		// If cannot lock and nothing to send, just leave
		if (!locked && (data == null)) {
			return;
		}

		// Otherwise wait.....
		if (!locked) {
			writeInProgress.lock();
		}

		// Avoid concurrent calls here (one from call() and another from
		// application)
		try {
			if ((data != null) && data.hasRemaining()) {
				int length = data.remaining();

				socketIO.write(data);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Socket: {0}, wrote: {1}", new Object[] { socketIO,
							length });
				}

				// idx_start = idx_offset;
				// idx_offset = Math.min(idx_start + out_buff_size, data.length());
				// }
				setLastTransferTime();

				// addWritten(data.length());
				empty_read_call_count = 0;
			} else {
				if (socketIO.waitingToSend()) {
					socketIO.write(null);
					setLastTransferTime();
					empty_read_call_count = 0;
				}
			}
		} catch (Exception e) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Data writing exception " + connectionId, e);
			}
			forceStop();
		} finally {
			writeInProgress.unlock();
		}
	}

	/**
	 * Describe
	 * <code>writeData</code> method here.
	 *
	 * @param data a
	 * <code>String</code> value
	 */
	protected void writeData(final String data) {

		// Try to lock the data writing method
		boolean locked = writeInProgress.tryLock();

		// If cannot lock and nothing to send, just leave
		if (!locked && (data == null)) {
			return;
		}

		// Otherwise wait.....
		if (!locked) {
			writeInProgress.lock();
		}

		// Avoid concurrent calls here (one from call() and another from
		// application)
		try {
			if ((data != null) && (data.length() > 0)) {
				if (log.isLoggable(Level.FINEST)) {
					if (data.length() < 256) {
						log.log(Level.FINEST, "Socket: {0}, Writing data ({1}): {2}", new Object[] {
								socketIO,
								data.length(), data });
					} else {
						log.log(Level.FINEST, "Socket: {0}, Writing data: {1}", new Object[] {
								socketIO,
								data.length() });
					}
				}

				ByteBuffer dataBuffer = null;

				// int out_buff_size = data.length();
				// int idx_start = 0;
				// int idx_offset = Math.min(idx_start + out_buff_size, data.length());
				//
				// while (idx_start < data.length()) {
				// String data_str = data.substring(idx_start, idx_offset);
				// if (log.isLoggable(Level.FINEST)) {
				// log.finest("Writing data_str (" + data_str.length() + "), idx_start="
				// + idx_start + ", idx_offset=" + idx_offset + ": " + data_str);
				// }
				encoder.reset();

				// dataBuffer = encoder.encode(CharBuffer.wrap(data, idx_start,
				// idx_offset));
				dataBuffer = encoder.encode(CharBuffer.wrap(data));
				encoder.flush(dataBuffer);
				socketIO.write(dataBuffer);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Socket: {0}, wrote: {1}", new Object[] { socketIO,
							data.length() });
				}

				// idx_start = idx_offset;
				// idx_offset = Math.min(idx_start + out_buff_size, data.length());
				// }
				setLastTransferTime();

				// addWritten(data.length());
				empty_read_call_count = 0;
			} else {
				if (socketIO.waitingToSend()) {
					socketIO.write(null);
					setLastTransferTime();
					empty_read_call_count = 0;
				}
			}
		} catch (Exception e) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Data writing exception " + connectionId, e);
			}
			forceStop();
		} finally {
			writeInProgress.unlock();
		}
	}
	
	protected boolean isSocketServiceReady() {
		return socketServiceReady;
	}
	
	protected void setSocketServiceReady(boolean value) {
		this.socketServiceReady = value;
	}	

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 * 
	 */
	protected boolean isInputBufferEmpty() {
		return (socketInput != null) && (socketInput.remaining() == socketInput.capacity());
	}

	//~--- methods --------------------------------------------------------------

	private void resizeInputBuffer() throws IOException {
		int netSize = socketIO.getInputPacketSize();

		// Resize buffer if needed.
		// if (netSize > socketInput.remaining()) {
		if (netSize > socketInput.capacity() - socketInput.remaining()) {

			// int newSize = netSize + socketInput.capacity();
			int newSize = socketInput.capacity() * 2;
			if (bufferLimit > 0 && newSize > bufferLimit && socketInput.capacity() < bufferLimit) {
				newSize = bufferLimit;
			}
			if (!checkBufferLimit(bufferLimit)) {
				throw new IOException("Input buffer size limit exceeded");
			}

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Socket: {0}, Resizing socketInput to {1} bytes.",
						new Object[] { socketIO,
						newSize });
			}

			ByteBuffer b = ByteBuffer.allocate(newSize);

			b.order(byteOrder());
			b.put(socketInput);
			socketInput = b;
		} else {

			// if (log.isLoggable(Level.FINEST)) {
			// log.finer("     Before flip()");
			// log.finer("input.capacity()=" + socketInput.capacity());
			// log.finer("input.remaining()=" + socketInput.remaining());
			// log.finer("input.limit()=" + socketInput.limit());
			// log.finer("input.position()=" + socketInput.position());
			// }
			// socketInput.flip();
			// if (log.isLoggable(Level.FINEST)) {
			// log.finer("     Before compact()");
			// log.finer("input.capacity()=" + socketInput.capacity());
			// log.finer("input.remaining()=" + socketInput.remaining());
			// log.finer("input.limit()=" + socketInput.limit());
			// log.finer("input.position()=" + socketInput.position());
			// }
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Socket: {0}, Compacting socketInput.", socketIO);
			}
			socketInput.compact();

			// if (log.isLoggable(Level.FINEST)) {
			// log.finer("     After compact()");
			// log.finer("input.capacity()=" + socketInput.capacity());
			// log.finer("input.remaining()=" + socketInput.remaining());
			// log.finer("input.limit()=" + socketInput.limit());
			// log.finer("input.position()=" + socketInput.position());
			// }
		}
	}

	//~--- set methods ----------------------------------------------------------

	private void setLastTransferTime() {
		lastTransferTime = System.currentTimeMillis();
	}

	public Certificate getPeerCertificate() {
		return peerCertificate;
	}

	public Certificate getLocalCertificate() {
		return localCertificate;
	}



}    // IOService


//~ Formatted in Tigase Code Convention on 13/05/29
