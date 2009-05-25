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
package tigase.xmpp;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.net.IOService;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

/**
 * Describe class XMPPIOService here.
 *
 *
 * Created: Tue Feb  7 07:15:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPIOService extends IOService {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.XMPPIOService");

  private XMPPDomBuilderHandler domHandler = null;
	private SimpleParser parser = SingletonFactory.getParserInstance();
	private XMPPIOServiceListener serviceListener = null;

  /**
   * The <code>waitingPackets</code> queue keeps data which have to be processed.
   */
  private ConcurrentLinkedQueue<Packet> waitingPackets =
    new ConcurrentLinkedQueue<Packet>();

  /**
   * The <code>readyPackets</code> queue keeps data which have been already
   * processed and they are actual processing results.
   */
  private ConcurrentLinkedQueue<Packet> receivedPackets =
    new ConcurrentLinkedQueue<Packet>();

	private String xmlns = null;

//   /**
//    * Variable <code>lock</code> keeps reference to object lock.
//    * It supports multi-threaded processing and can be called simultanously from
//    * many threads. It is not recommended however as lock prevents most of
//    * methods to be executed concurrently as they process data received from
//    * socket and the data should be processed in proper order.
//    */
	//private Lock writeLock = new ReentrantLock();
	//private Lock readLock = new ReentrantLock();

	//private boolean streamClosed = false;

	/**
	 * Creates a new <code>XMPPIOService</code> instance.
	 *
	 */
	public XMPPIOService() {
		domHandler = new XMPPDomBuilderHandler(this);
	}

	public void setXMLNS(String xmlns) {
		this.xmlns = xmlns;
	}

	public String getXMLNS() {
		return this.xmlns;
	}

	public void setIOServiceListener(XMPPIOServiceListener sl) {
		this.serviceListener = sl;
		super.setIOServiceListener(sl);
	}

	protected void xmppStreamOpened(Map<String, String> attribs) {
		if (serviceListener != null) {
			String response = serviceListener.streamOpened(this, attribs);
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending data: " + response);
				}
				writeRawData(response);
				if (response != null && response.endsWith("</stream:stream>")) {
					stop();
				} // end of if (response.endsWith())
			} catch (IOException e) {
				log.warning("Error sending stream open data: " + e);
				forceStop();
			}
		}
	}

	public void xmppStreamOpen(final String data) {
    try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending data: " + data);
			}
			writeRawData(data);
			assert debug(data, "--SENT:");
		} catch (IOException e) {
			log.warning("Error sending stream open data: " + e);
			forceStop();
    }
	}

	/**
   * Describe <code>stop</code> method here.
   *
   * @exception IOException if an error occurs
   */
	@Override
  public void stop() {
// 		if (!streamClosed) {
// 			streamClosed = true;
// 			serviceListener.xmppStreamClosed(this);
// 		} // end of if (!streamClosed)
		super.stop();
  }

	protected void xmppStreamClosed() {
		//streamClosed = true;
		if (serviceListener != null) {
			serviceListener.streamClosed(this);
		}
    try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending data: </stream:stream>");
			}
			writeRawData("</stream:stream>");
		} catch (IOException e) {
			log.warning("Error sending stream closed data: " + e);
    }
// 		try {
// 			stop();
// 		} catch (IOException e) {
// 			log.warning("Error stopping service: " + e);
// 		} // end of try-catch
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

  /**
   * Describe <code>processWaitingPackets</code> method here.
   *
	 * @throws IOException
	 */
	@Override
  public void processWaitingPackets() throws IOException {
		Packet packet = null;
		while ((packet = waitingPackets.poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending packet: " + packet.toString());
			}
			writeRawData(packet.getStringData());
			if (log.isLoggable(Level.FINEST)) {
				log.finest("SENT: " + packet.getStringData());
			}
		} // end of while (packet = waitingPackets.poll() != null)
  }

	public void writeRawData(String data) throws IOException {
    // We change state of this object in this method
    // It can be called by many threads simultanously
    // so we need to make it thread-safe
// 		writeLock.lock();
//		try {
			writeData(data);
//		} finally {
// 			writeLock.unlock();
//		}
	}

	/**
   * Describe <code>processSocketData</code> method here.
   *
   * @exception IOException if an error occurs
   */
	@Override
  protected void processSocketData() throws IOException {
    // We change state of this object in this method
    // It can be called by many threads simultanously
    // so we need to make it thread-safe
		//log.finer("About to read socket data.");
		// Correction:
		// The design is that this method should not be called concurrently by
		// multiple threads. However it may happen in some specific cases.
		// There is a 'non-blocking' synchronization in IOService.call() method
		// implemented instead.
// 		readLock.lock();
//    try {
			if (isConnected()) {
				char[] data = readData();
				while (data != null && data.length > 0) {
					// Yes check again if we are still connected as
					// servce might be disconnected during data read
					if (isConnected()) {
						if (data != null) {
							if (log.isLoggable(Level.FINEST)) {
								log.finest("READ:\n" + new String(data));
							}
							// This is log for debuging only,
							// in normal mode don't even call below code
							assert debug(new String(data), "--RECEIVED:");
							try {
								parser.parse(domHandler, data, 0, data.length);
								if (domHandler.parseError()) {
									log.warning("Data parsing error: " + new String(data));
									domHandler = new XMPPDomBuilderHandler(this);
								}
								Queue<Element> elems = domHandler.getParsedElements();
								if (elems.size() > 0) {
									readCompleted();
								}
								Element elem = null;
								while ((elem = elems.poll()) != null) {
									//	assert debug(elem.toString() + "\n");
									//log.finer("Read element: " + elem.getName());
									if (log.isLoggable(Level.FINEST)) {
										log.finest("Read packet: " + elem.toString());
									}
									//							System.out.print(elem.toString());
									addReceivedPacket(new Packet(elem));
								} // end of while ((elem = elems.poll()) != null)
							} catch (Exception ex) {
								log.log(Level.INFO, "Incorrect XML data: " + new String(data) +
												", stopping connection: " + getUniqueId() +
												", exception: ", ex);
								forceStop();
							} // end of try-catch
						} else {
							if (log.isLoggable(Level.FINEST)) {
								log.finest("Nothing read!!");
							}
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Service disconnected during read");
						}
						forceStop();
					}
					data = readData();
				}
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.fine("function called when the service is not connected! forceStop()");
				}
				forceStop();
			}
//    } finally {
//			readLock.unlock();
//    }
  }

	@Override
	protected int receivedPackets() {
		return receivedPackets.size();
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

} // XMPPIOService
