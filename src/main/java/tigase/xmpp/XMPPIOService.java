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

//~--- non-JDK imports --------------------------------------------------------

import tigase.net.IOService;

import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class XMPPIOService here.
 *
 *
 * Created: Tue Feb  7 07:15:02 2006
 *
 * @param <RefObject> is a refrence object stored by this service. This is e reference
 * to higher level data object keeping more information about the connection.
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPIOService<RefObject> extends IOService<RefObject> {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.xmpp.XMPPIOService");

	//~--- fields ---------------------------------------------------------------

	private XMPPDomBuilderHandler<RefObject> domHandler = null;
	protected SimpleParser parser = SingletonFactory.getParserInstance();
	private XMPPIOServiceListener serviceListener = null;
	private boolean authenticated = false;

	/**
	 * The <code>waitingPackets</code> queue keeps data which have to be processed.
	 */
	private ConcurrentLinkedQueue<Packet> waitingPackets = new ConcurrentLinkedQueue<Packet>();

	/**
	 * The <code>readyPackets</code> queue keeps data which have been already
	 * processed and they are actual processing results.
	 */
	private ConcurrentLinkedQueue<Packet> receivedPackets = new ConcurrentLinkedQueue<Packet>();
	private String xmlns = null;

	//~--- constructors ---------------------------------------------------------

///**
// * Variable <code>lock</code> keeps reference to object lock.
// * It supports multi-threaded processing and can be called simultanously from
// * many threads. It is not recommended however as lock prevents most of
// * methods to be executed concurrently as they process data received from
// * socket and the data should be processed in proper order.
// */
	// private Lock writeLock = new ReentrantLock();
	// private Lock readLock = new ReentrantLock();
	// private boolean streamClosed = false;

	/**
	 * Creates a new <code>XMPPIOService</code> instance.
	 *
	 */
	public XMPPIOService() {
		domHandler = new XMPPDomBuilderHandler<RefObject>(this);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method <code>addPacketToSend</code> adds new data which will be processed
	 * during next run.
	 * Data are kept in proper order like in <em>FIFO</em> queue.
	 *
	 * @param packet a <code>Packet</code> value of data to process.
	 */
	public void addPacketToSend(Packet packet) {
		waitingPackets.offer(packet);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public Queue<Packet> getReceivedPackets() {
		return receivedPackets;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getXMLNS() {
		return this.xmlns;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	//~--- methods --------------------------------------------------------------

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
				log.finest(toString() + ", Sending packet: " + packet);
			}

			writeRawData(packet.getElement().toString());

			if (log.isLoggable(Level.FINEST)) {
				log.finest(toString() + ", SENT: " + packet.getElement().toString());
			}
		}    // end of while (packet = waitingPackets.poll() != null)
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param authenticated
	 */
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	/**
	 * Method description
	 *
	 *
	 * @param sl
	 */
	@SuppressWarnings({ "unchecked" })
	public void setIOServiceListener(XMPPIOServiceListener sl) {
		this.serviceListener = sl;
		super.setIOServiceListener(sl);
	}

	/**
	 * Method description
	 *
	 *
	 * @param xmlns
	 */
	public void setXMLNS(String xmlns) {
		this.xmlns = xmlns;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Describe <code>stop</code> method here.
	 *
	 */
	@Override
	public void stop() {

//  if (!streamClosed) {
//    streamClosed = true;
//    serviceListener.xmppStreamClosed(this);
//  } // end of if (!streamClosed)
		super.stop();
	}

	/**
	 * Method description
	 *
	 *
	 * @param data
	 *
	 * @throws IOException
	 */
	public void writeRawData(String data) throws IOException {

		// We change state of this object in this method
		// It can be called by many threads simultanously
		// so we need to make it thread-safe
//  writeLock.lock();
//  try {
		writeData(data);

//  } finally {
//    writeLock.unlock();
//  }
	}

	/**
	 * Method description
	 *
	 *
	 * @param data
	 */
	public void xmppStreamOpen(final String data) {
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(toString() + ", Sending data: " + data);
			}

			writeRawData(data);
			assert debug(data, "--SENT:");
		} catch (IOException e) {
			log.warning(toString() + ", Error sending stream open data: " + e);
			forceStop();
		}
	}

	/**
	 * Method <code>addReceivedPacket</code> puts processing results to queue.
	 * The processing results are usually data (messages) which has been
	 * just received from socket.
	 *
	 * @param packet a <code>Packet</code> value of processing results.
	 */
	protected void addReceivedPacket(final Packet packet) {
		receivedPackets.offer(packet);
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
		// log.finer("About to read socket data.");
		// Correction:
		// The design is that this method should not be called concurrently by
		// multiple threads. However it may happen in some specific cases.
		// There is a 'non-blocking' synchronization in IOService.call() method
		// implemented instead.
//  readLock.lock();
//  try {
		if (isConnected()) {
			char[] data = readData();

			while ((data != null) && (data.length > 0)) {

				// Yes check again if we are still connected as
				// servce might be disconnected during data read
				if (isConnected()) {
					if (data != null) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest(toString() + ", READ:\n" + new String(data));
						}

						// This is log for debuging only,
						// in normal mode don't even call below code
						assert debug(new String(data), "--RECEIVED:");

						Element elem = null;

						try {
							parser.parse(domHandler, data, 0, data.length);

							if (domHandler.parseError()) {
								if (log.isLoggable(Level.FINE)) {
									log.fine(toString() + ", Data parsing error: " + new String(data));
								} else {
									log.warning(toString() + ", data parsing error, stopping connection");
								}

								forceStop();

								// domHandler = new XMPPDomBuilderHandler<RefObject>(this);
							}

							Queue<Element> elems = domHandler.getParsedElements();

							if (elems.size() > 0) {
								readCompleted();
							}

							while ((elem = elems.poll()) != null) {

								// assert debug(elem.toString() + "\n");
								// log.finer("Read element: " + elem.getName());
								if (log.isLoggable(Level.FINEST)) {
									log.finest(toString() + ", Read packet: " + elem.toString());
								}

								// System.out.print(elem.toString());
								addReceivedPacket(Packet.packetInstance(elem));
							}    // end of while ((elem = elems.poll()) != null)
						} catch (TigaseStringprepException ex) {
							log.log(Level.INFO,
									toString() + ", Incorrect to/from JID format for stanza: "
										+ elem.toString(), ex);
						} catch (Exception ex) {
							log.log(Level.INFO,
									toString() + ", Incorrect XML data: " + new String(data)
										+ ", stopping connection: " + getUniqueId() + ", exception: ", ex);
							forceStop();
						}    // end of try-catch
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest(toString() + ", Nothing read!!");
						}
					}
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.finest(toString() + ", Service disconnected during read");
					}

					forceStop();
				}

				data = readData();
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.fine(toString()
						+ ", function called when the service is not connected! forceStop()");
			}

			forceStop();
		}

//  } finally {
//    readLock.unlock();
//  }
	}

	@Override
	protected int receivedPackets() {
		return receivedPackets.size();
	}

	@SuppressWarnings({ "unchecked" })
	protected void xmppStreamClosed() {
		if (log.isLoggable(Level.FINEST)) {
			log.finest(toString() + ", Received STREAM-CLOSE from the client");
		}

		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(toString() + ", Sending data: </stream:stream>");
			}

			writeRawData("</stream:stream>");
		} catch (IOException e) {
			log.info(toString() + ", Error sending stream closed data: " + e);
		}

		// streamClosed = true;
		if (serviceListener != null) {
			serviceListener.xmppStreamClosed(this);
		}

//  try {
//    stop();
//  } catch (IOException e) {
//    log.warning("Error stopping service: " + e);
//  } // end of try-catch
	}

	@SuppressWarnings({ "unchecked" })
	protected void xmppStreamOpened(Map<String, String> attribs) {
		if (serviceListener != null) {
			String response = serviceListener.xmppStreamOpened(this, attribs);

			try {
				if (log.isLoggable(Level.FINEST)) {
					log.finest(toString() + ", Sending data: " + response);
				}

				writeRawData(response);

				if ((response != null) && response.endsWith("</stream:stream>")) {
					stop();
				}    // end of if (response.endsWith())
			} catch (IOException e) {
				log.warning(toString() + ", Error sending stream open data: " + e);
				forceStop();
			}
		}
	}
}    // XMPPIOService


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
