/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.net;

//~--- non-JDK imports --------------------------------------------------------

import tigase.io.BufferUnderflowException;
import tigase.io.IOInterface;
import tigase.io.SocketIO;
import tigase.io.TLSIO;
import tigase.io.TLSUtil;
import tigase.io.TLSWrapper;
import tigase.io.ZLibIO;

import tigase.stats.StatisticsList;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.net.Socket;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * <code>IOService</code> offers thread safe
 * <code>call()</code> method execution, however you must be prepared that other
 * methods can be called simultanously like <code>stop()</code>,
 * <code>getProtocol()</code> or <code>isConnected()</code>.
 * <br/>It is recommended that developers extend <code>AbsractServerService</code>
 * rather then implement <code>ServerService</code> interface directly.
 * <p>If you directly implement <code>ServerService</code> interface you must
 * take care about <code>SocketChannel</code> I/O, queuing tasks, processing
 * results and thread safe execution of <code>call()</code> method. If you
 * however extend <code>IOService</code> class all this basic
 * operation are implemented and you have only to take care about parsing data
 * received from network socket. Parsing data is expected to be implemented in
 * <code>parseData(char[] data)</code> method.</p>
 *
 * <p>
 * Created: Tue Sep 28 23:00:34 2004
 * </p>
 * @param <RefObject> is a reference object stored by this service. This is e reference
 * to higher level data object keeping more information about the connection.
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class IOService<RefObject> implements Callable<IOService> {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.net.IOService");

	/**
	 * This is key used to store session ID in temporary session data storage.
	 * As it is used by many components it is required that all components access
	 * session ID with this constant.
	 */
	public static final String SESSION_ID_KEY = "sessionID";

	/** Field description */
	public static final String PORT_TYPE_PROP_KEY = "type";

	/** Field description */
	public static final String HOSTNAME_KEY = "hostname-key";
	private static final long MAX_ALLOWED_EMPTY_CALLS = 1000;

	//~--- fields ---------------------------------------------------------------

	private ConnectionType connectionType = null;
	private JID dataReceiver = null;

	/** Field description */
	private long empty_read_call_count = 0;
	private String id = null;

	/**
	 * This variable keeps the time of last transfer in any direction
	 * it is used to help detect dead connections.
	 */
	private long lastTransferTime = 0;
	private String local_address = null;
	private long[] rdData = new long[60];
	private RefObject refObject = null;
	private String remote_address = null;
	private IOServiceListener<IOService<RefObject>> serviceListener = null;
	private IOInterface socketIO = null;

	/**
	 * <code>socketInput</code> buffer keeps data read from socket.
	 */
	private ByteBuffer socketInput = null;
	private int socketInputSize = 2048;
	private boolean stopping = false;
	private long[] wrData = new long[60];
	private ConcurrentMap<String, Object> sessionData = new ConcurrentHashMap<String, Object>(4,
		0.75f, 4);
	private CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
	private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
	private final AtomicInteger writeInProgress = new AtomicInteger(0);
	private final AtomicBoolean readInProgress = new AtomicBoolean(false);

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @throws IOException
	 */
	public abstract void processWaitingPackets() throws IOException;

	protected abstract void processSocketData() throws IOException;

	protected abstract int receivedPackets();

	/**
	 * Method <code>accept</code> is used to perform
	 *
	 * @param socketChannel a <code>SocketChannel</code> value
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

			log.log(Level.INFO, "Problem connecting to remote host: {0}, address: {1} - exception: {2}",
					new Object[] { host,
					remote_address, e });

			throw e;
		}

		socketInputSize = socketIO.getSocketChannel().socket().getReceiveBufferSize();
		socketInput = ByteBuffer.allocate(socketInputSize);

		Socket sock = socketIO.getSocketChannel().socket();

		local_address = sock.getLocalAddress().getHostAddress();
		remote_address = sock.getInetAddress().getHostAddress();
		id = local_address + "_" + sock.getLocalPort() + "_" + remote_address + "_" + sock.getPort();
		setLastTransferTime();
	}

	/**
	 * Method <code>run</code> is used to perform
	 *
	 *
	 * @return
	 * @throws IOException
	 */
	@Override
	public IOService call() throws IOException {

		// It is not safe to call below function here....
		// It might be already executing in different thread...
		// and we don't want to put any locking or synchronization
		// processWaitingPackets();
		// Non-blocking 'kind of' synchronization.
		// This method is executed for both reading and writing from/to
		// network socket. It tries to do everything in one go, however
		// If either reading or writing is in progress then it should skip
		// the step.
		if (writeInProgress.compareAndSet(0, 1)) {
			try {
				writeData(null);
			} finally {
				writeInProgress.decrementAndGet();
			}
		}

		if (stopping) {
			stop();
		} else {
			if (readInProgress.compareAndSet(false, true)) {
				try {
					processSocketData();

					if ((receivedPackets() > 0) && (serviceListener != null)) {
						serviceListener.packetsReady(this);
					}    // end of if (receivedPackets.size() > 0)
				} finally {
					readInProgress.set(false);
				}
			}
		}

		return this;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
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
			if (socketIO != null) {
				synchronized (socketIO) {
					socketIO.stop();
				}
			}
		} catch (Exception e) {

			// Well, do nothing, we are closing the connection anyway....
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Socket: " + socketIO + ", Exception while stopping service: " + getUniqueId(), e);
			}
		} finally {
			if (serviceListener != null) {
				IOServiceListener<IOService<RefObject>> tmp = serviceListener;

				serviceListener = null;
				tmp.serviceStopped(this);
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public JID getDataReceiver() {
		return this.dataReceiver;
	}

	/**
	 * This method returns the time of last transfer in any direction
	 * through this service. It is used to help detect dead connections.
	 * @return
	 */
	public long getLastTransferTime() {
		return lastTransferTime;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getLocalAddress() {
		return local_address;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long[] getReadCounters() {
		return rdData;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
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
	 * @return
	 */
	public ConcurrentMap<String, Object> getSessionData() {
		return sessionData;
	}

	/**
	 * Method <code>getSocketChannel</code> is used to perform
	 *
	 * @return a <code>SocketChannel</code> value
	 */
	public SocketChannel getSocketChannel() {
		return socketIO.getSocketChannel();
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	public void getStatistics(StatisticsList list) {
		if (socketIO != null) {
			socketIO.getStatistics(list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getUniqueId() {
		return id;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long[] getWriteCounters() {
		return wrData;
	}

	/**
	 * Describe <code>isConnected</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 */
	public boolean isConnected() {
		boolean result = (socketIO == null) ? false : socketIO.isConnected();

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Socket: " + socketIO + ", Connected: " + result);
		}

		return result;
	}

	//~--- set methods ----------------------------------------------------------

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
		sessionData = new ConcurrentHashMap<String, Object>(props);
		connectionType = ConnectionType.valueOf(sessionData.get(PORT_TYPE_PROP_KEY).toString());
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param clientMode
	 *
	 * @throws IOException
	 */
	public void startSSL(final boolean clientMode) throws IOException {
		if (socketIO instanceof TLSIO) {
			throw new IllegalStateException("SSL mode is already activated.");
		}

		TLSWrapper wrapper = new TLSWrapper(TLSUtil.getSSLContext("SSL",
			(String) sessionData.get(HOSTNAME_KEY)), null, clientMode);

		socketIO = new TLSIO(socketIO, wrapper);
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
	public void startTLS(final boolean clientMode) throws IOException {
		if (socketIO instanceof TLSIO) {
			throw new IllegalStateException("TLS mode is already activated.");
		}

		TLSWrapper wrapper = new TLSWrapper(TLSUtil.getSSLContext("TLS",
			(String) sessionData.get(HOSTNAME_KEY)), null, clientMode);

		socketIO = new TLSIO(socketIO, wrapper);
		setLastTransferTime();
		encoder.reset();
		decoder.reset();
	}

	/**
	 * Method description
	 *
	 *
	 * @param level
	 */
	public void startZLib(int level) {
		socketIO = new ZLibIO(socketIO, level);
	}

	/**
	 * Describe <code>stop</code> method here.
	 *
	 */
	public void stop() {
		if ((socketIO != null) && socketIO.waitingToSend()) {
			stopping = true;
		} else {
			forceStop();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return this.getUniqueId() + ", type: " + connectionType + ", Socket: " + socketIO;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean waitingToSend() {
		return socketIO.waitingToSend();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int waitingToSendSize() {
		return socketIO.waitingToSendSize();
	}

	/**
	 * Describe <code>debug</code> method here.
	 *
	 * @param msg a <code>char[]</code> value
	 * @return a <code>boolean</code> value
	 */
	protected boolean debug(final char[] msg) {
		if (msg != null) {
			System.out.print(new String(msg));

			// log.finest("\n" + new String(msg) + "\n");
		}    // end of if (msg != null)

		return true;
	}

	/**
	 * Describe <code>debug</code> method here.
	 *
	 * @param msg a <code>String</code> value
	 * @return a <code>boolean</code> value
	 */
	protected boolean debug(final String msg, final String prefix) {
		if (log.isLoggable(Level.FINEST)) {
			if ((msg != null) && (msg.trim().length() > 0)) {
				String log_msg = "\n"
					+ ((connectionType() != null) ? connectionType().toString() : "null-type") + " " + prefix
					+ "\n" + msg + "\n";

				// System.out.print(log_msg);
				log.finest(log_msg);
			}
		}

		return true;
	}

	protected void readCompleted() {
		decoder.reset();
	}

	/**
	 * Describe <code>readData</code> method here.
	 *
	 * @return a <code>char[]</code> value
	 * @exception IOException if an error occurs
	 */
	protected char[] readData() throws IOException {
		setLastTransferTime();

		CharBuffer cb = null;

		// Generally it should not happen as it is called only in
		// call() which has concurrent call protection.
//  synchronized (socketIO) {
		try {

			// resizeInputBuffer();
			// Maybe we can shring the packet??
			if ((socketInput.remaining() == socketInput.capacity())
					&& (socketInput.capacity() > socketInputSize)) {

				// Yes, looks like we can
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Socket: {0}, Resizing socketInput down to {1} bytes.",
							new Object[] { socketIO,
							socketIO.getInputPacketSize() });
				}

				socketInput = ByteBuffer.allocate(socketInputSize);
			}

			ByteBuffer tmpBuffer = socketIO.read(socketInput);

			if (socketIO.bytesRead() > 0) {
				empty_read_call_count = 0;

				// There might be some characters read from the network
				// but the buffer may still be null or empty because there might
				// be not enough data to decode TLS or compressed buffer.
				if (tmpBuffer != null) {

					// tmpBuffer.flip();
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Socket: " + socketIO + ", Reading network binary data: " + tmpBuffer.remaining());
					}

					cb = decoder.decode(tmpBuffer);

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Socket: " + socketIO + ", Decoded character data: " + cb.array().length);
					}

					// TODO: maybe compact instead????
					tmpBuffer.clear();

					// socketInput.compact();
					// addRead(cb.array().length);
				}
			} else {

				// Detecting infinite read 0 bytes
				// sometimes it happens that the connection has been lost
				// and the select thinks there are some bytes waiting for reading
				// and 0 bytes are read
				if ((++empty_read_call_count) > MAX_ALLOWED_EMPTY_CALLS && (writeInProgress.get() == 0)) {
					log.warning("Socket: " + socketIO
							+ ", Max allowed empty calls excceeded, closing connection.");
					forceStop();
				}
			}
		} catch (BufferUnderflowException ex) {

			// Obtain more inbound network data for src,
			// then retry the operation.
			resizeInputBuffer();

			return null;

//    } catch (MalformedInputException ex) {
//      // This happens after TLS initialization sometimes, maybe reset helps
//      decoder.reset();
		} catch (Exception eof) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Socket: " + socketIO + ", Exception reading data" + eof);
			}

			// eof.printStackTrace();
			forceStop();
		}    // end of try-catch

//  }
		return (cb != null) ? cb.array() : null;
	}

	/**
	 * Describe <code>writeData</code> method here.
	 *
	 * @param data a <code>String</code> value
	 */
	protected void writeData(final String data) {
		writeInProgress.incrementAndGet();

		// Avoid concurrent calls here (one from call() and another from
		// application)
		synchronized (writeInProgress) {
			try {
				if ((data != null) && (data.length() > 0)) {
					if (log.isLoggable(Level.FINEST)) {
						if (data.length() < 256) {
							log.log(Level.FINEST, "Socket: {0}, Writing data ({1}): {2}", new Object[] { socketIO,
									data.length(), data });
						} else {
							log.log(Level.FINEST, "Socket: {0}, Writing data: {1}", new Object[] { socketIO,
									data.length() });
						}
					}

					ByteBuffer dataBuffer = null;

//        int out_buff_size = data.length();
//        int idx_start = 0;
//        int idx_offset = Math.min(idx_start + out_buff_size, data.length());
//
//        while (idx_start < data.length()) {
//        String data_str = data.substring(idx_start, idx_offset);
//        if (log.isLoggable(Level.FINEST)) {
//        log.finest("Writing data_str (" + data_str.length() + "), idx_start="
//            + idx_start + ", idx_offset=" + idx_offset + ": " + data_str);
//        }
					encoder.reset();

//        dataBuffer = encoder.encode(CharBuffer.wrap(data, idx_start, idx_offset));
					dataBuffer = encoder.encode(CharBuffer.wrap(data));
					encoder.flush(dataBuffer);
					socketIO.write(dataBuffer);

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket: {0}, wrote: {1}", new Object[] { socketIO,
								data.length() });
					}

//        idx_start = idx_offset;
//        idx_offset = Math.min(idx_start + out_buff_size, data.length());
//          }
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
				forceStop();
			}
		}

		writeInProgress.decrementAndGet();
	}

	private void resizeInputBuffer() throws IOException {
		int netSize = socketIO.getInputPacketSize();

		// Resize buffer if needed.
//  if (netSize > socketInput.remaining()) {
		if (netSize > socketInput.capacity() - socketInput.remaining()) {

			// int newSize = netSize + socketInput.capacity();
			int newSize = socketInput.capacity() + socketInputSize;

			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Socket: {0}, Resizing socketInput to {1} bytes.",
						new Object[] { socketIO,
						newSize });
			}

			ByteBuffer b = ByteBuffer.allocate(newSize);

			b.put(socketInput);
			socketInput = b;
		} else {

//    if (log.isLoggable(Level.FINEST)) {
//      log.finer("     Before flip()");
//      log.finer("input.capacity()=" + socketInput.capacity());
//      log.finer("input.remaining()=" + socketInput.remaining());
//      log.finer("input.limit()=" + socketInput.limit());
//      log.finer("input.position()=" + socketInput.position());
//    }
//    socketInput.flip();
//    if (log.isLoggable(Level.FINEST)) {
//      log.finer("     Before compact()");
//      log.finer("input.capacity()=" + socketInput.capacity());
//      log.finer("input.remaining()=" + socketInput.remaining());
//      log.finer("input.limit()=" + socketInput.limit());
//      log.finer("input.position()=" + socketInput.position());
//    }
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Socket: {0}, Compacting socketInput.", socketIO);
			}

			socketInput.compact();

//    if (log.isLoggable(Level.FINEST)) {
//      log.finer("     After compact()");
//      log.finer("input.capacity()=" + socketInput.capacity());
//      log.finer("input.remaining()=" + socketInput.remaining());
//      log.finer("input.limit()=" + socketInput.limit());
//      log.finer("input.position()=" + socketInput.position());
//    }
		}
	}

	//~--- set methods ----------------------------------------------------------

	private void setLastTransferTime() {
		lastTransferTime = System.currentTimeMillis();
	}
}    // IOService


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
