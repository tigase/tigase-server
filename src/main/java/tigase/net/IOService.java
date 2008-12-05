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

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.annotations.TODO;
import tigase.io.IOInterface;
import tigase.io.SocketIO;
import tigase.io.TLSIO;
import tigase.io.TLSUtil;
import tigase.io.TLSWrapper;
import tigase.io.BufferUnderflowException;
import tigase.util.TimeUtils;

/**
 * <code>IOService</code> offers thread thread safe
 * <code>call()</code> method execution, however you must be prepared that other
 * methods can be called simultanously like <code>stop()</code>,
 * <code>getProtocol()</code> or <code>isConnected()</code>.
 * <br/>It is recomended that developers extend <code>AbsractServerService</code>
 * rather then implement <code>ServerService</code> interface directly.
 * <p>If you directly implement <code>ServerService</code> interface you must
 * take care about <code>SocketChannel</code> I/O, queueing tasks, processing
 * results and thread safe execution of <code>call()</code> method. If you
 * however extend <code>IOService</code> class all this basic
 * operation are implemented and you have only to take care about parsing data
 * received from network socket. Parsing data is expected to be implemented in
 * <code>parseData(char[] data)</code> method.</p>
 *
 * <p>
 * Created: Tue Sep 28 23:00:34 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class IOService implements Callable<IOService> {

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
	public static final String PORT_TYPE_PROP_KEY = "type";
	public static final String HOSTNAME_KEY = "hostname-key";

  private IOInterface socketIO = null;
	private String sslId = null;
	private String id = null;
	private ConnectionType connectionType = null;
	private String local_address = null;
	private String remote_address = null;
  /**
	 * This variable keeps the time of last transfer in any direction
	 * it is used to help detect dead connections.
   */
	private long lastTransferTime = 0;
	private boolean stopping = false;

	private long[] rdData = new long[60];
	private long[] wrData = new long[60];
	private int lastMinuteRd = 0;
	private int lastMinuteWr = 0;

	private IOServiceListener serviceListener = null;

	private ConcurrentMap<String, Object> sessionData =
		new ConcurrentHashMap<String, Object>();

	/**
   * <code>socketInput</code> buffer keeps data read from socket.
   */
  private ByteBuffer socketInput = null;

  private CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
  private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

	private String dataReceiver = null;

	private void addRead(long read) {
		int minute = TimeUtils.getMinuteNow();
		if (lastMinuteRd != minute) {
			lastMinuteRd = minute;
			rdData[minute] = 0;
		}
		rdData[minute] += read;
	}

	public long[] getReadCounters() {
		return rdData;
	}

	private void addWritten(long wrote) {
		int minute = TimeUtils.getMinuteNow();
		if (lastMinuteWr != minute) {
			lastMinuteWr = minute;
			wrData[minute] = 0;
		}
		wrData[minute] += wrote;
	}

	public long[] getWriteCounters() {
		return wrData;
	}

	public void setDataReceiver(String address) {
		this.dataReceiver = address;
	}

	public String getDataReceiver() {
		return this.dataReceiver;
	}

  public void setSSLId(final String id) {
    sslId = id;
  }

  /**
	 * This method returns the time of last transfer in any direction
	 * through this service. It is used to help detect dead connections.
   */
	public long getLastTransferTime() {
		return lastTransferTime;
	}

	private void setLastTransferTime() {
		lastTransferTime = System.currentTimeMillis();
	}

  public void startSSL(final boolean clientMode)
    throws IOException {

		TLSWrapper wrapper = new TLSWrapper(TLSUtil.getSSLContext(sslId, "SSL",
				(String)sessionData.get(HOSTNAME_KEY)), null, clientMode);
		socketIO = new TLSIO(socketIO, wrapper);
		setLastTransferTime();
  }

  public void startTLS(final boolean clientMode)
    throws IOException {

		TLSWrapper wrapper = new TLSWrapper(TLSUtil.getSSLContext(sslId, "TLS",
				(String)sessionData.get(HOSTNAME_KEY)), null, clientMode);
		socketIO = new TLSIO(socketIO, wrapper);
		setLastTransferTime();
  }

	public void setIOServiceListener(IOServiceListener sl) {
		this.serviceListener = sl;
	}

	public String getUniqueId() {
		return id;
	}

	public ConnectionType connectionType() {
		return this.connectionType;
	}

	public ConcurrentMap<String, Object> getSessionData() {
		return sessionData;
	}

	public void setSessionData(Map<String, Object> props) {
		sessionData = new ConcurrentHashMap<String, Object>(props);
		connectionType =
			ConnectionType.valueOf(sessionData.get(PORT_TYPE_PROP_KEY).toString());
	}

  /**
   * Describe <code>isConnected</code> method here.
   *
   * @return a <code>boolean</code> value
   */
  public synchronized boolean isConnected() {
		log.finest("socketIO = " + socketIO);
		return socketIO == null ? false : socketIO.isConnected();
	}

	public String getRemoteAddress() {
		return remote_address;
	}

	public String getLocalAddress() {
		return local_address;
	}

	/**
   * Method <code>accept</code> is used to perform
   *
   * @param socketChannel a <code>SocketChannel</code> value
   */
  public void accept(final SocketChannel socketChannel)
    throws IOException {
		if (socketChannel.isConnectionPending()) {
			socketChannel.finishConnect();
		} // end of if (socketChannel.isConnecyionPending())
    socketIO = new SocketIO(socketChannel);
    socketInput = ByteBuffer.allocate(socketIO.getInputPacketSize());
		Socket sock = socketIO.getSocketChannel().socket();
		local_address = sock.getLocalAddress().getHostAddress();
		remote_address = sock.getInetAddress().getHostAddress();
		id = local_address + "_" + sock.getLocalPort()
			+ "_" + remote_address + "_" + sock.getPort();
		setLastTransferTime();
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
   * Describe <code>stop</code> method here.
   *
   * @exception IOException if an error occurs
   */
  public void stop() {
		if (socketIO != null && socketIO.waitingToSend()) {
			stopping = true;
		} else {
			forceStop();
		}
  }

	public synchronized void forceStop() {
		log.finer("Force stop called...");
		try {
			if (socketIO != null) {
				socketIO.stop();
			}
		} catch (Exception e) {
			// Well, do nothing, we are closing the connection anyway....
		} finally {
			if (serviceListener != null) {
				IOServiceListener tmp = serviceListener;
				serviceListener = null;
				tmp.serviceStopped(this);
			}
		}
	}

  /**
   * Method <code>run</code> is used to perform
   *
   */
  public IOService call() throws IOException {
		// It is not safe to call below function here....
		// It might be already executing in different thread...
		// and we don't want to put any locking or synchronization
		//		processWaitingPackets();
		writeData(null);
		if (stopping) {
			stop();
		} else {
			processSocketData();
			if (receivedPackets() > 0 && serviceListener != null) {
				serviceListener.packetsReady(this);
			} // end of if (receivedPackets.size() > 0)
		}
    return this;
  }

	public abstract void processWaitingPackets() throws IOException;

	protected abstract void processSocketData() throws IOException;

	protected abstract int receivedPackets();

	private void resizeInputBuffer() throws IOException {
		int netSize = socketIO.getInputPacketSize();
		// Resize buffer if needed.
		if (netSize > socketInput.remaining()) {
			log.fine("Resizing buffer to "
				+ (netSize + socketInput.capacity())
				+ " bytes.");
			ByteBuffer b = ByteBuffer.allocate(netSize+socketInput.capacity());
			b.put(socketInput);
			socketInput = b;
		}
	}

	protected void readCompleted() {
		decoder.reset();
	}

	private long empty_read_call_count = 0;
	private static final long MAX_ALLOWED_EMPTY_CALLS = 100;

	/**
   * Describe <code>readData</code> method here.
   *
   * @return a <code>char[]</code> value
   * @exception IOException if an error occurs
   */
  protected char[] readData() throws IOException {
		setLastTransferTime();
    CharBuffer cb = null;
		synchronized (decoder) {
			try {
				//			resizeInputBuffer();
				ByteBuffer tmpBuffer = socketIO.read(socketInput);
				if (socketIO.bytesRead() > 0) {
					empty_read_call_count = 0;
					tmpBuffer.flip();
					cb = decoder.decode(tmpBuffer);
					tmpBuffer.clear();
					addRead(cb.array().length);
				} else {
					// Detecting infinite read 0 bytes
					// sometimes it happens that the connection has been lost
					// and the select thinks there are some bytes waiting for reading
					// and 0 bytes are read
					if ((++empty_read_call_count) > MAX_ALLOWED_EMPTY_CALLS) {
						log.warning("Max allowed empty calls excceeded, closing connection.");
						forceStop();
					}
				}
			} catch (BufferUnderflowException underfl) {
				// Obtain more inbound network data for src,
				// then retry the operation.
				resizeInputBuffer();
				return null;
			} catch (Exception eof) {
				//			eof.printStackTrace();
				forceStop();
			} // end of try-catch
		}
    return cb != null ? cb.array() : null;
  }

	public boolean waitingToSend() {
		return socketIO.waitingToSend();
	}

	/**
   * Describe <code>writeData</code> method here.
   *
   * @param data a <code>String</code> value
   * @exception IOException if an error occurs
   */
  protected void writeData(final String data) {
		synchronized (encoder) {
			try {
				if (data != null && data.length() > 0) {
					log.finest("Writing data: " + data);
					ByteBuffer dataBuffer = null;
					encoder.reset();
					dataBuffer = encoder.encode(CharBuffer.wrap(data));
					encoder.flush(dataBuffer);
					socketIO.write(dataBuffer);

					setLastTransferTime();
					addWritten(data.length());
				} else {
					if (socketIO.waitingToSend()) {
						socketIO.write(null);

						setLastTransferTime();
					}
				}
				empty_read_call_count = 0;
			} catch (Exception e) {
				forceStop();
			}
		}
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
			//      log.finest("\n" + new String(msg) + "\n");
    } // end of if (msg != null)
    return true;
  }

  /**
   * Describe <code>debug</code> method here.
   *
   * @param msg a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  protected boolean debug(final String msg, final String prefix) {
    if (msg != null && msg.trim().length() > 0) {
			String log_msg = "\n"
				+ (connectionType() != null ?	connectionType().toString() : "null-type")
				+ " " + prefix + "\n" + msg + "\n";
				//			System.out.print(log_msg);
				log.finest(log_msg);
    }
    return true;
  }

}// IOService
