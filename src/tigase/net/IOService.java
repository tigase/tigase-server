/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.annotations.TODO;
import tigase.io.IOInterface;
import tigase.io.SocketIO;
import tigase.io.TLSIO;
import tigase.io.TLSUtil;
import tigase.io.TLSWrapper;
import tigase.server.Packet;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

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
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class IOService implements Callable<IOService> {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.net.IOService");

  private IOInterface socketIO = null;
	private String sslId = null;
  private DomBuilderHandler domHandler = new DomBuilderHandler();
	private SimpleParser parser = SingletonFactory.getParserInstance();
	private String id = null;

	private IOServiceListener serviceListener = null;

  /**
   * Variable <code>lock</code> keeps reference to object lock.
   * It supports multi-threaded processing and can be called simultanously from
   * many threads. It is not recommended however as lock prevents most of
   * methods to be executed concurrently as they process data received from
   * socket and the data should be processed in proper order.
   */
  private final Lock writeLock = new ReentrantLock();
	private final Lock readLock = new ReentrantLock();

  /**
   * The <code>waitingPackets</code> queue keeps data which have to be processed.
   */
  private final ConcurrentLinkedQueue<Packet> waitingPackets =
    new ConcurrentLinkedQueue<Packet>();

  /**
   * The <code>readyPackets</code> queue keeps data which have been already
   * processed and they are actual processing results.
   */
  private final ConcurrentLinkedQueue<Packet> receivedPackets =
    new ConcurrentLinkedQueue<Packet>();

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

  public void startSSL() {
    socketIO = new TLSIO(socketIO,
			new TLSWrapper(TLSUtil.getSSLContext(sslId, "SSL")));
  }

  public void startTLS() {
    socketIO = new TLSIO(socketIO,
			new TLSWrapper(TLSUtil.getSSLContext(sslId, "TLS")));
  }

	public void setIOServiceListener(IOServiceListener sl) {
		this.serviceListener = sl;
	}

  /**
   * Method <code>addPacketToSend</code> adds new data which will be processed
   * during next run.
   * Data are kept in proper order like in <em>FIFO</em> queue.
   *
   * @param packet a <code>Packet</code> value of data to process.
   */
  public void addPacketToSend(final Packet packet) {
    waitingPackets.offer(packet);
  }

  public Queue<Packet> getReceivedPackets() {
    return receivedPackets;
  }

	public String getUniqueId() {
		return id;
	}

	public ConcurrentMap<String, Object> getSessionData() {
		return sessionData;
	}

	public void setSessionData(Map<String, Object> props) {
		sessionData = new ConcurrentHashMap<String, Object>(props);
	}

  /**
   * Describe <code>isConnected</code> method here.
   *
   * @return a <code>boolean</code> value
   */
  public boolean isConnected() {
    return socketIO == null ? false : socketIO.isConnected();
  }

	public String getRemoteHost() {
		return
			socketIO.getSocketChannel().socket().getInetAddress().getHostAddress();
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
		InetAddress local = sock.getLocalAddress();
		InetAddress remote = sock.getInetAddress();
		id = local.getHostAddress() + "_" + sock.getLocalPort()
			+ "_" + remote.getHostAddress() + "_" + sock.getPort();
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
    // We change state of this object in this method
    // It can be called by many threads simultanously
    // so we need to make it thread-safe
		processWaitingPackets();
		processSocketData();
		if (receivedPackets.size() > 0) {
			serviceListener.packetsReady(this);
		} // end of if (receivedPackets.size() > 0)
    return this;
  }

  /**
   * Describe <code>processWaitingPackets</code> method here.
   *
   */
  public void processWaitingPackets() throws IOException {
    writeLock.lock();
    try {
			Packet packet = null;
			while ((packet = waitingPackets.poll()) != null) {
				log.finest("Sending packet: " + packet.getStringData());
				writeData(packet.getStringData());
			} // end of while (packet = waitingPackets.poll() != null)
    } finally {
      writeLock.unlock();
    }
  }

	/**
   * Method <code>addReceivedPacket</code> puts processing results to queue.
   * The processing results are usually data (messages) which has been
   * just received from socket.
   *
   * @param packet a <code>Packet</code> value of processing results.
   */
  private void addReceivedPacket(final Packet packet) {
    receivedPackets.offer(packet);
  }

  /**
   * Describe <code>processSocketData</code> method here.
   *
   * @exception IOException if an error occurs
   */
  private void processSocketData() throws IOException {
		readLock.lock();
    try {
			if (isConnected()) {
				final char[] data = readData();
				// This is log for debuging only,
				// in normal mode don't even call below code
				// assert debug(data);

				// Yes check again if we are still connected as
				// servce might be disconnected during data read
				if (isConnected() && data != null) {
					try {
						parser.parse(domHandler, data, 0, data.length);
						Queue<Element> elems = domHandler.getParsedElements();
						Element elem = null;
						while ((elem = elems.poll()) != null) {
							assert debug(elem.toString() + "\n");
							log.finest("Received packet: " + elem.toString());
							addReceivedPacket(new Packet(elem));
						} // end of while ((elem = elems.poll()) != null)
					} // end of try
					catch (Exception ex) {
						ex.printStackTrace();
						try { stop(); } catch (Exception e) { } // NOPMD
					} // end of try-catch
				} // end of if (isConnected() && data != null)
			}
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Describe <code>readData</code> method here.
   *
   * @return a <code>char[]</code> value
   * @exception IOException if an error occurs
   */
  private char[] readData() throws IOException {

    CharBuffer cb = null;
    try {
      ByteBuffer tmpBuffer = socketIO.read(socketInput);
      if (socketIO.bytesRead() > 0) {
        tmpBuffer.flip();
        cb = coder.decode(tmpBuffer);
        tmpBuffer.clear();
      } // end of if (socketIO.bytesRead() > 0)
    } // end of try
    catch (Exception eof) {
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
  private void writeData(final String data) throws IOException {
    ByteBuffer dataBuffer = null;
    if (data != null || data.length() > 0) {
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
  private boolean debug(final char[] msg) {
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
  private boolean debug(final String msg) {
    if (msg != null) {
			System.out.print(msg);
			//      log.finest(msg);
    }
    return true;
  }

}// IOService
