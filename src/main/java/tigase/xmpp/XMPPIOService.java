/*
 * XMPPIOService.java
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



package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.net.IOService;

import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Describe class XMPPIOService here.
 *
 *
 * Created: Tue Feb 7 07:15:02 2006
 *
 * @param <RefObject>
 *          is a reference object stored by this service. This is e reference to
 *          higher level data object keeping more information about the
 *          connection.
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPIOService<RefObject>
				extends IOService<RefObject> {
	/** Field description */
	public static final String ACK_NAME = "ack";

	/** Field description */
	public static final String CROSS_DOMAIN_POLICY_FILE_PROP_KEY =
		"cross-domain-policy-file";

	/** Field description */
	public static final String CROSS_DOMAIN_POLICY_FILE_PROP_VAL =
		"etc/cross-domain-policy.xml";

	/** Field description */
	public static final String ID_ATT = "id";

	/** Field description */
	public static final String REQ_NAME       = "req";
	private static String cross_domain_policy = null;

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(XMPPIOService.class.getName());

	//~--- fields ---------------------------------------------------------------

	private XMPPDomBuilderHandler<RefObject> domHandler = null;

	/** Field description */
	protected SimpleParser parser                 = SingletonFactory.getParserInstance();
	private long packetsReceived                  = 0;
	private long packetsSent                      = 0;
	private long req_idx                          = 0;
	@SuppressWarnings("rawtypes")
	private XMPPIOServiceListener serviceListener = null;
	private long totalPacketsReceived             = 0;
	private long totalPacketsSent                 = 0;

	/**
	 * The <code>waitingPackets</code> queue keeps data which have to be
	 * processed.
	 */
	private ConcurrentLinkedQueue<Packet> waitingPackets =
		new ConcurrentLinkedQueue<Packet>();
	private ConcurrentSkipListMap<String, Packet> waitingForAck =
		new ConcurrentSkipListMap<String, Packet>();
	private boolean white_char_ack = false;
	private String xmlns           = null;
	private boolean xmpp_ack       = false;
	private boolean strict_ack     = false;

	/**
	 * The <code>readyPackets</code> queue keeps data which have been already
	 * processed and they are actual processing results.
	 */
	private ConcurrentLinkedQueue<Packet> receivedPackets =
		new ConcurrentLinkedQueue<Packet>();
	private boolean firstPacket = true;

	/** Field description */
	public ReentrantLock writeInProgress = new ReentrantLock();

	//~--- constructors ---------------------------------------------------------

	// ~--- constructors ---------------------------------------------------------
	// /**
	// * Variable <code>lock</code> keeps reference to object lock.
	// * It supports multi-threaded processing and can be called simultaneously
	// from
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
		super();
		domHandler = new XMPPDomBuilderHandler<RefObject>(this);
		if (cross_domain_policy == null) {
			String file_name = System.getProperty(CROSS_DOMAIN_POLICY_FILE_PROP_KEY,
													 CROSS_DOMAIN_POLICY_FILE_PROP_VAL);

			try {
				BufferedReader br = new BufferedReader(new FileReader(file_name));
				String line       = br.readLine();
				StringBuilder sb  = new StringBuilder();

				while (line != null) {
					sb.append(line);
					line = br.readLine();
				}
				sb.append('\0');
				br.close();
				cross_domain_policy = sb.toString();
			} catch (Exception ex) {
				log.log(Level.WARNING, "Problem reading cross domain poicy file: " + file_name,
								ex);
			}
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param white_char_ack
	 * @param xmpp_ack
	 * @param strict
	 */
	public void setAckMode(boolean white_char_ack, boolean xmpp_ack, boolean strict) {
		this.white_char_ack = white_char_ack;
		this.xmpp_ack       = xmpp_ack;
		this.strict_ack     = strict;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * @return
	 */
	public long getPacketsSent(boolean reset) {
		long tmp = packetsSent;

		if (reset) {
			packetsSent = 0;
		}

		return tmp;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getTotalPacketsSent() {
		return totalPacketsSent;
	}

	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 * @return
	 */
	public long getPacketsReceived(boolean reset) {
		long tmp = packetsReceived;

		if (reset) {
			packetsReceived = 0;
		}

		return tmp;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getTotalPacketsReceived() {
		return totalPacketsReceived;
	}

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method <code>addPacketToSend</code> adds new data which will be processed
	 * during next run. Data are kept in proper order like in <em>FIFO</em> queue.
	 *
	 * @param packet
	 *          a <code>Packet</code> value of data to process.
	 */
	public void addPacketToSend(Packet packet) {
		if (xmpp_ack) {
			String req = "" + (++req_idx);

			packet.getElement().addChild(new Element(REQ_NAME, new String[] { ID_ATT },
							new String[] { req }));
			waitingForAck.put(req, packet);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Added req {1} for packet: {2}",
								new Object[] { toString(),
															 req, packet });
			}
		}
		++packetsSent;
		++totalPacketsSent;
		waitingPackets.offer(packet);
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods ----------------------------------------------------------

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
	public Map<String, Packet> getWaitingForAct() {
		for (Packet p : waitingForAck.values()) {
			Element req = p.getElement().getChild(REQ_NAME);

			if (req == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
									"{0}, Missing req element in waiting for ACK packet: {1}",
									new Object[] { toString(),
																 p });
				}
			} else {
				p.getElement().removeChild(req);
			}
		}

		return waitingForAck;
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

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	/**
	 * Describe <code>processWaitingPackets</code> method here.
	 *
	 * @throws IOException
	 */
	@Override
	public void processWaitingPackets() throws IOException {
		Packet packet = null;

		// int cnt = 0;
		// while ((packet = waitingPackets.poll()) != null && (cnt < 1000)) {
		while ((packet = waitingPackets.poll()) != null) {

			// ++cnt;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Sending packet: {1}", new Object[] { toString(),
								packet });
			}
			writeRawData(packet.getElement().toString());
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, SENT: {1}", new Object[] { toString(),
								packet.getElement().toString() });
			}
		}    // end of while (packet = waitingPackets.poll() != null)
	}

	//~--- set methods ----------------------------------------------------------

	// ~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param servList
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setIOServiceListener(XMPPIOServiceListener servList) {
		this.serviceListener = servList;
		super.setIOServiceListener(servList);
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

	// ~--- methods --------------------------------------------------------------

	/**
	 * Describe <code>stop</code> method here.
	 *
	 */
	@Override
	public void stop() {

		// if (!streamClosed) {
		// streamClosed = true;
		// serviceListener.xmppStreamClosed(this);
		// } // end of if (!streamClosed)
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
		// writeLock.lock();
		// try {
		writeData(data);

		// } finally {
		// writeLock.unlock();
		// }
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
				log.log(Level.FINEST, "{0}, Sending data: {1}",
								new Object[] { toString(), data });
			}
			writeRawData(data);
			assert debug(data, "--SENT:");
		} catch (IOException e) {
			log.log(Level.WARNING, "{0}, Error sending stream open data: {1}",
							new Object[] { toString(),
														 e });
			forceStop();
		}
	}

	/**
	 * Method <code>addReceivedPacket</code> puts processing results to queue. The
	 * processing results are usually data (messages) which has been just received
	 * from socket.
	 *
	 * @param packet
	 *          a <code>Packet</code> value of processing results.
	 */
	protected void addReceivedPacket(final Packet packet) {
		if (firstPacket) {
			if ("policy-file-request" == packet.getElemName()) {
				log.fine("Got flash cross-domain request" + packet);
				if (cross_domain_policy != null) {
					try {
						writeRawData(cross_domain_policy);
					} catch (Exception ex) {
						log.log(Level.INFO, "Can't send cross-domain policy: ", ex);
					}
					log.log(Level.FINER, "Cross-domain policy sent: {1}", cross_domain_policy);
				} else {
					log.log(Level.FINER, "No cross-domain policy defined to sent.");
				}

				return;
			}
			firstPacket = false;
		}
		if (packet.getElemName() == ACK_NAME) {
			String ack_id = packet.getAttributeStaticStr(ID_ATT);
		} else {
			sendAck(packet);
			++packetsReceived;
			++totalPacketsReceived;
			receivedPackets.offer(packet);
		}
	}

	/**
	 * Describe <code>processSocketData</code> method here.
	 *
	 * @exception IOException
	 *              if an error occurs
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
		// readLock.lock();
		// try {
		if (isConnected()) {
			char[] data = readData();

			while (isConnected() && (data != null) && (data.length > 0)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, READ:\n{1}", new Object[] { toString(),
									new String(data) });
				}

				boolean disconnect = checkData(data);

				if (disconnect) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0}, checkData says disconnect: {1}",
										new Object[] { toString(),
																	 new String(data) });
					} else {
						log.log(Level.WARNING, "{0}, checkData says disconnect", toString());
					}
					forceStop();

					return;

					// domHandler = new XMPPDomBuilderHandler<RefObject>(this);
				}

				// This is log for debugging only,
				// in normal mode don't even call below code
				assert debug(new String(data), "--RECEIVED:");

				Element elem = null;

				try {
					parser.parse(domHandler, data, 0, data.length);
					if (domHandler.parseError()) {
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "{0}, Data parsing error: {1}",
											new Object[] { toString(),
																		 new String(data) });
						} else {
							log.log(Level.WARNING, "{0}, data parsing error, stopping connection",
											toString());
						}
						forceStop();

						return;

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
							log.log(Level.FINEST, "{0}, Read packet: {1}", new Object[] { toString(),
											elem });
						}

						// System.out.print(elem.toString());
						Packet pack = Packet.packetInstance(elem);

						addReceivedPacket(pack);
						sendAck(pack);
					}    // end of while ((elem = elems.poll()) != null)
				} catch (TigaseStringprepException ex) {
					log.log(Level.INFO,
									toString() + ", Incorrect to/from JID format for stanza: " +
									elem.toString(), ex);
				} catch (Exception ex) {
					log.log(Level.INFO,
									toString() + ", Incorrect XML data: " + new String(data) +
									", stopping connection: " + getConnectionId() + ", exception: ", ex);
					forceStop();
				}    // end of try-catch
				data = readData();
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
								"{0}, function called when the service is not connected! forceStop()",
								toString());
			}
			forceStop();
		}

		// } finally {
		// readLock.unlock();
		// }
	}

	private void sendAck(Packet packet) {

		// If stanza receiving confirmation is configured, try to send confirmation
		// back
		if (white_char_ack || xmpp_ack) {
			String ack = null;

			if (white_char_ack) {

				// If confirming via white space is enabled then prepare space ack.
				ack = " ";
			}
			if (xmpp_ack) {
				Element req = packet.getElement().getChild(REQ_NAME);

				if (req != null) {
					packet.getElement().removeChild(req);

					String req_val = req.getAttributeStaticStr(ID_ATT);

					if (req_val != null) {

						// XMPP ack might be enabled in configuration but the client may not
						// support it. In such a case we do not send XMPP ack.
						ack = "<" + ACK_NAME + " " + ID_ATT + "=\"" + req_val + "\"/>";
					}
				}
			}
			if (ack != null) {
				try {
					writeRawData(ack);
					log.log(Level.FINEST, "Sent ack confirmation: '" + ack + "'");
				} catch (Exception ex) {
					forceStop();
					log.log(Level.FINE, "Can't send ack confirmation: '" + ack + "'", ex);
				}
			}
		}
	}

	/**
	 *
	 * @param data
	 *   @return
	 *
	 * @throws IOException
	 */
	public boolean checkData(char[] data) throws IOException {

		// by default do nothing and return false
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected int receivedPackets() {
		return receivedPackets.size();
	}

	/**
	 * Method description
	 *
	 */
	@SuppressWarnings({ "unchecked" })
	protected void xmppStreamClosed() {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Received STREAM-CLOSE from the client", toString());
		}
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Sending data: </stream:stream>", toString());
			}
			writeRawData("</stream:stream>");
		} catch (IOException e) {
			log.log(Level.INFO, "{0}, Error sending stream closed data: {1}",
							new Object[] { toString(),
														 e });
		}

		// streamClosed = true;
		if (serviceListener != null) {
			serviceListener.xmppStreamClosed(this);
		}

		// try {
		// stop();
		// } catch (IOException e) {
		// log.warning("Error stopping service: " + e);
		// } // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param attribs
	 */
	@SuppressWarnings({ "unchecked" })
	protected void xmppStreamOpened(Map<String, String> attribs) {
		if (serviceListener != null) {
			String response = serviceListener.xmppStreamOpened(this, attribs);

			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Sending data: {1}", new Object[] { toString(),
									response });
				}
				writeRawData(response);
				processWaitingPackets();
				if ((response != null) && response.endsWith("</stream:stream>")) {
					stop();
				}    // end of if (response.endsWith())
			} catch (IOException e) {
				log.log(Level.WARNING, "{0}, Error sending stream open data: {1}",
								new Object[] { toString(),
															 e });
				forceStop();
			}
		}
	}
}    // XMPPIOService



// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com


//~ Formatted in Tigase Code Convention on 13/03/04
