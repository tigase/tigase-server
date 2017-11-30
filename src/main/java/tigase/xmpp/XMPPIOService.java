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
import tigase.server.xmppclient.XMPPIOProcessor;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

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

	/**
	 * Key name of the system property for configuration protection
	 * from system overload and DOS attack.
	 */
	public static final String DOM_HANDLER = "XMPPDomBuilderHandler";

	/** Field description */
	public static final String ID_ATT = "id";

	/** Field description */
	public static final String REQ_NAME            = "req";

	public static final String STREAM_CLOSING        = "stream-closing";

	private static String      cross_domain_policy = null;

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(XMPPIOService.class.getName());

	//~--- fields ---------------------------------------------------------------

	private XMPPDomBuilderHandler<RefObject> domHandler = null;
	
	/** Field description */
	protected SimpleParser        parser = SingletonFactory.getParserInstance();
	private String                jid                  = null;
	private long                  packetsReceived      = 0;
	private long                  packetsSent          = 0;
	protected XMPPIOProcessor[]     processors           = null;
	private long                  req_idx              = 0;
	@SuppressWarnings("rawtypes")
	private XMPPIOServiceListener serviceListener      = null;
	private long                  totalPacketsReceived = 0;
	private long                  totalPacketsSent     = 0;
	/**
	 * This variable keeps the time of last received XMPP packet, it is used to
	 * help detect dead connections.
	 */
	private long                                    lastXmppPacketReceivedTime = 0;

	/**
	 * The <code>waitingPackets</code> queue keeps data which have to be
	 * processed.
	 */
	private ConcurrentLinkedQueue<Packet> waitingPackets =
			new ConcurrentLinkedQueue<Packet>();
	private ConcurrentSkipListMap<String, Packet> waitingForAck =
			new ConcurrentSkipListMap<String, Packet>();
	private boolean white_char_ack = false;
	private String  xmlns          = null;
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

	/**
	 * Creates a new <code>XMPPIOService</code> instance.
	 *
	 */
	public XMPPIOService() {
		super();
		domHandler = new XMPPDomBuilderHandler<>( this );
		getSessionData().put( DOM_HANDLER, domHandler );
		if (cross_domain_policy == null) {
			String file_name = System.getProperty(CROSS_DOMAIN_POLICY_FILE_PROP_KEY,
					CROSS_DOMAIN_POLICY_FILE_PROP_VAL);

			try {
				BufferedReader br   = new BufferedReader(new FileReader(file_name));
				String         line = br.readLine();
				StringBuilder  sb   = new StringBuilder();

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

	//~--- methods --------------------------------------------------------------

	/**
	 * Method <code>addPacketToSend</code> adds new data which will be processed
	 * during next run. Data are kept in proper order like in <em>FIFO</em> queue.
	 *
	 * @param packet
	 *          a <code>Packet</code> value of data to process.
	 */
	public void addPacketToSend(Packet packet) {

		// processing packet using io level processors
		if (processors != null) {
			for (XMPPIOProcessor processor : processors) {
				if (processor.processOutgoing(this, packet)) {
					return;
				}
			}
		}
		if (xmpp_ack) {
			String req = "" + (++req_idx);

			packet.getElement().addChild(new Element(REQ_NAME, new String[] { ID_ATT },
					new String[] { req }));
			waitingForAck.put(req, packet);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Added req {1} for packet: {2}", new Object[] {
						toString(),
						req, packet });
			}
		}
		++packetsSent;
		++totalPacketsSent;
		waitingPackets.offer(packet);
	}

		@Override
	public IOService<?> call() throws IOException {
		IOService<?> io = super.call();
		// needed to send packets added by addPacketToSent when it was not able
		// to acquire lock for write as when this packet would not be followed by
		// next packet then it would stay in waitingPackets queue, however this
		// may slow down processing packets in SocketThread thread.
		if (isConnected() && !waitingPackets.isEmpty() && writeInProgress.tryLock()) {
			try {
				processWaitingPackets();
			} finally {
				writeInProgress.unlock();
			}			
		}
		return io;
	}
	
	@Override
	public boolean checkBufferLimit(int bufferSize) {
		if (!super.checkBufferLimit(bufferSize)) {
			try {
				writeRawData("<stream:error>" + "<policy-violation "
						+ "xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
						+ "</stream:error>");
				writeRawData("</stream:stream>");
				int counter = 0;

				while (isConnected() && waitingToSend() && (++counter < 10)) {
					writeData(null);
					try {
						Thread.sleep(10);
					} catch (InterruptedException ex) {
					}
				}
			}
			catch (IOException ex) {
				log.log(Level.FINEST, "{0}, Exception sending policy-violation stream error", 
						new Object[]{toString()});
			}
			this.forceStop();
			return false;
		}
		return true;
	}	
	
	/**
	 *
	 * @param data
	 *
	 *
	 * @return a value of <code>boolean</code>
	 * @throws IOException
	 */
	public boolean checkData(char[] data) throws IOException {

		// by default do nothing and return false
		return false;
	}

	/**
	 * Clears queue of packets waiting to send.
	 * In case of connection close this packets may be sent to offline store 
	 * but some processors may want stop this from happening - for that they 
	 * may use this method
	 */
	public void clearWaitingPackets() {
		this.waitingPackets.clear();
	}
	
	/**
	 * Returns queue with packets waiting to send. For use by ConnectionManager
	 * which may need to get undelivered packets 
	 * 
	 * @return 
	 */
	public Queue<Packet> getWaitingPackets() {
		return waitingPackets;
	}
	
	@Override
	public void forceStop() {
		boolean stop = false;

		if (processors != null) {
			for (XMPPIOProcessor processor : processors) {
				stop |= processor.serviceStopped(this, false);
			}
		}
		if (!stop) {
			super.forceStop();
		}
	}


	@Override
	public void processWaitingPackets() throws IOException {
		Packet packet = null;

		// int cnt = 0;
		// while ((packet = waitingPackets.poll()) != null && (cnt < 1000)) {
		
		// we should only peek for packet now, and poll it after sending it
		while ((packet = waitingPackets.peek()) != null) {

			// ++cnt;
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Sending packet: {1}", new Object[] { toString(),
						packet });
			}
			writeRawData(packet.getElement().toString());
			
			// and after sending it we should remove it to minimalize chances of lost packets
			waitingPackets.poll();
			
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, SENT: {1}", new Object[] { toString(),
						packet.getElement().toString() });
			}
		}    // end of while (packet = waitingPackets.poll() != null)

		// notify io processors that all waiting packets were sent
		if (processors != null) {
			for (XMPPIOProcessor processor : processors) {
				processor.packetsSent(this);
			}
		}
	}

	@Override
	public void stop() {

		// if (!streamClosed) {
		// streamClosed = true;
		// serviceListener.xmppStreamClosed(this);
		// } // end of if (!streamClosed)
		super.stop();
	}

	@Override
	public String toString() {
		return super.toString() + ", jid: " + jid;
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
				log.log(Level.FINEST, "{0}, Sending data: {1}", new Object[] { toString(),
						data });
			}
			writeRawData(data);
			assert debug(data, "--SENT:");
		} catch (IOException e) {
			log.log(Level.WARNING, "{0}, Error sending stream open data: {1}", new Object[] {
					toString(),
					e });
			forceStop();
		}
	}

	//~--- get methods ----------------------------------------------------------

//	/**
//	 *
//	 * @return
//	 */
//	public int getElementLimits() {
//		return elements_number_limit;
//	}


	/**
	 * Method description
	 *
	 *
	 * @param reset
	 *
	 *
	 *
	 * @return a value of <code>long</code>
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
	 * @param reset
	 *
	 *
	 *
	 * @return a value of <code>long</code>
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
	 *
	 *
	 * @return a value of {@code Queue<Packet>}
	 */
	public Queue<Packet> getReceivedPackets() {
		return receivedPackets;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getTotalPacketsReceived() {
		return totalPacketsReceived;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getTotalPacketsSent() {
		return totalPacketsSent;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getUserJid() {
		return this.jid;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of {@code Map<String,Packet>}
	 */
	public Map<String, Packet> getWaitingForAct() {
		for (Packet p : waitingForAck.values()) {
			Element req = p.getElement().getChild(REQ_NAME);

			if (req == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"{0}, Missing req element in waiting for ACK packet: {1}", new Object[] {
							toString(),
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
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getXMLNS() {
		return this.xmlns;
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


	/**
	 *
	 * @param limit
	 */
	public void setElementLimits(int limit) {

	}

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
	 * @param processors is a <code>XMPPIOProcessor[]</code>
	 */
	public void setProcessors(XMPPIOProcessor[] processors) {
		this.processors = processors;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 */
	public void setUserJid(String jid) {
		this.jid = jid;
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
		if (processors != null) {
			boolean stop = false;

			for (XMPPIOProcessor processor : processors) {
				stop |= processor.processIncoming(this, packet);
			}
			if (stop) {
				return;
			}
		}
		if (packet.getElemName() == ACK_NAME) {
			String ack_id = packet.getAttributeStaticStr(ID_ATT);
		} else {
			sendAck(packet);
			++packetsReceived;
			++totalPacketsReceived;
			setLastXmppPacketReceiveTime();
			receivedPackets.offer(packet);
		}
	}

	protected String prepareStreamClose() {
		return "</stream:stream>";
	}	
	
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
					log.log(Level.FINEST, "{0}, READ:{1}", new Object[] { toString(),
							new String(data) });
				}

				boolean disconnect = checkData(data);

				if (disconnect) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "{0}, checkData says disconnect: {1}", new Object[] {
								toString(),
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
							log.log(Level.FINE, "{0}, Data parsing error: {1}", new Object[] {
									toString(),
									new String(data) });
						} else {
							log.log(Level.WARNING, "{0}, data parsing error, stopping connection",
									toString());
						}
						if (serviceListener != null) {
							Element err = new Element("not-well-formed", new String[] { "xmlns" }, new String[] { "urn:ietf:params:xml:ns:xmpp-streams" });
							String streamErrorStr = serviceListener.xmppStreamError(this, Collections.singletonList(err));
							writeRawData(streamErrorStr);
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
					log.log(Level.INFO, toString() +
							", Incorrect to/from JID format for stanza: " + elem.toString(), ex);
				} catch (Exception ex) {
					log.log(Level.INFO, toString() + ", Incorrect XML data: " + new String(data) +
							", stopping connection: " + getConnectionId() + ", exception: ", ex);
					forceStop();
				} finally {
					if (domHandler.isStreamClosed())
						xmppStreamClosed();
				}  // end of try-catch
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

		if (processors != null) {
			for (XMPPIOProcessor processor : processors) {
				processor.serviceStopped(this, true);
			}
		}
		
		try {
			if (isConnected()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Sending data: </stream:stream>, as socket is still connected", toString());
				}
				writeRawData(prepareStreamClose());
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Not sending data: </stream:stream>, as socket is alreadt closed", toString());
				}				
			}
		} catch (IOException e) {
			log.log(Level.INFO, "{0}, Error sending stream closed data: {1}", new Object[] {
					toString(),
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
			String[] responses = serviceListener.xmppStreamOpened(this, attribs);

			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Sending data: {1}", new Object[] { toString(),
							responses });
				}
				if (responses == null) {
					if (writeInProgress.tryLock()) {
						try {
							writeRawData(null);
							processWaitingPackets();
						} finally {
							writeInProgress.unlock();
						}
					}
				} else {
					writeInProgress.lock();
					try {
						for (String response : responses) {
							writeRawData(response);
						}
						processWaitingPackets();
					} finally {
						writeInProgress.unlock();
					}
				}
				if ((responses != null) && responses[responses.length-1].endsWith("</stream:stream>")) {
					stop();
				}    // end of if (response.endsWith())
			} catch (IOException e) {
				log.log(Level.WARNING, "{0}, Error sending stream open data: {1}", new Object[] {
						toString(),
						e });
				forceStop();
			}
		}
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
	 * This method sets the time of last received XMPP packet, it is used to
	 * help detect dead connections.
	 */
	private void setLastXmppPacketReceiveTime() {
		lastXmppPacketReceivedTime = System.currentTimeMillis();
	}

	/**
	 * This method returns the time when the last XMPP packet was received, it is
	 * used to help detect dead connections.
	 *
	 * @return {@code long} number denoting time when the last XMPP packet was
	 *         received.
	 */
	public long getLastXmppPacketReceiveTime() {
		return lastXmppPacketReceivedTime;
	}
}    // XMPPIOService


//~ Formatted in Tigase Code Convention on 13/09/21
