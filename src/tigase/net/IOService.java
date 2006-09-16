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
 * $Author$
 * $Date$
 */

package tigase.net;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
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

  private IOInterface socketIO = null;
	private String sslId = null;
	private String id = null;
	private ConnectionType connectionType = null;
	private String local_address = null;
	private String remote_address = null;

	private IOServiceListener serviceListener = null;

	private ConcurrentMap<String, Object> sessionData =
		new ConcurrentHashMap<String, Object>();

	/**
   * <code>socketInput</code> buffer keeps data read from socket.
   */
  private ByteBuffer socketInput = null;

  private static final Charset coder = Charset.forName("UTF-8");

  public void setSSLId(final String id) {
    sslId = id;
  }

  public synchronized void startSSL(final boolean clientMode)
    throws IOException {

		socketIO = new TLSIO(socketIO,
			new TLSWrapper(TLSUtil.getSSLContext(sslId, "SSL"), clientMode));
  }

  public synchronized void startTLS(final boolean clientMode)
    throws IOException {

		socketIO = new TLSIO(socketIO,
			new TLSWrapper(TLSUtil.getSSLContext(sslId, "TLS"), clientMode));
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
  public boolean isConnected() {
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
    socketInput =
      ByteBuffer.allocate(socketChannel.socket().getReceiveBufferSize());
		Socket sock = socketIO.getSocketChannel().socket();
		local_address = sock.getLocalAddress().getHostAddress();
		remote_address = sock.getInetAddress().getHostAddress();
		id = local_address + "_" + sock.getLocalPort()
			+ "_" + remote_address + "_" + sock.getPort();
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
  public void stop() throws IOException {
    socketIO.stop();
		serviceListener.serviceStopped(this);
  }

  /**
   * Method <code>run</code> is used to perform
   *
   */
  @TODO(note="Maybe we can do more intelligent locking.")
  public IOService call() throws IOException {
		// It is not safe to call below function here....
		// It might be already executing in different thread...
		// and we don't want to put any locking or synchronization
		//		processWaitingPackets();
		processSocketData();
		if (receivedPackets() > 0) {
			serviceListener.packetsReady(this);
		} // end of if (receivedPackets.size() > 0)
    return this;
  }

	public abstract void processWaitingPackets() throws IOException;

	protected abstract void processSocketData() throws IOException;

	protected abstract int receivedPackets();

  /**
   * Describe <code>readData</code> method here.
   *
   * @return a <code>char[]</code> value
   * @exception IOException if an error occurs
   */
  protected char[] readData() throws IOException {
    CharBuffer cb = null;
    try {
      ByteBuffer tmpBuffer = socketIO.read(socketInput);
      if (socketIO.bytesRead() > 0) {
        tmpBuffer.flip();
        cb = coder.decode(tmpBuffer);
        tmpBuffer.clear();
      } // end of if (socketIO.bytesRead() > 0)
    } catch (Exception eof) {
      try { stop(); } catch (Exception e) { } // NOPMD
    } // end of try-catch
    return cb != null ? cb.array() : null;
  }

  /**
   * Describe <code>writeData</code> method here.
   *
   * @param data a <code>String</code> value
   * @exception IOException if an error occurs
   */
  protected synchronized void writeData(final String data) throws IOException {
    if (data != null && data.length() > 0) {
			ByteBuffer dataBuffer = null;
      dataBuffer = coder.encode(CharBuffer.wrap(data));
      socketIO.write(dataBuffer);
    } // end of if (data == null || data.equals("")) else
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
