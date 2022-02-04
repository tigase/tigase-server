/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.xmpp;

import tigase.annotations.TigaseDeprecated;
import tigase.net.IOService;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.server.xmppclient.StreamManagementIOProcessor;
import tigase.server.xmppclient.XMPPIOProcessor;
import tigase.util.StringUtilities;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.jid.JID;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class XMPPIOService here.
 * <br>
 * Created: Tue Feb 7 07:15:02 2006
 *
 * @param <RefObject> is a reference object stored by this service. This is e reference to higher level data object
 * keeping more information about the connection.
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
public class XMPPIOService<RefObject>
		extends IOService<RefObject> {

	public static final String ACK_NAME = "ack";

	public static final String CROSS_DOMAIN_POLICY_FILE_PROP_KEY = "cross-domain-policy-file";

	public static final String CROSS_DOMAIN_POLICY_FILE_PROP_VAL = "etc/cross-domain-policy.xml";

	/**
	 * Key name of the system property for configuration protection from system overload and DOS attack.
	 */
	public static final String DOM_HANDLER = "XMPPDomBuilderHandler";

	public static final String ID_ATT = "id";

	public static final String REQ_NAME = "req";

	public static final String STREAM_CLOSING = "stream-closing";

	private static final Logger log = Logger.getLogger(XMPPIOService.class.getName());

	public ReentrantLock writeInProgress = new ReentrantLock();
	protected SimpleParser parser = SingletonFactory.getParserInstance();
	protected XMPPIOProcessor[] processors = null;
	private XMPPDomBuilderHandler<RefObject> domHandler = null;
	private boolean firstPacket = true;
	private JID authorisedUserJid = null;
	/**
	 * This variable keeps the time of last received XMPP packet, it is used to help detect dead connections.
	 */
	private long lastXmppPacketReceivedTime = 0;
	private long packetsReceived = 0;
	private long packetsSent = 0;
	/**
	 * The <code>readyPackets</code> queue keeps data which have been already processed and they are actual processing
	 * results.
	 */
	private ConcurrentLinkedQueue<Packet> receivedPackets = new ConcurrentLinkedQueue<Packet>();
	private long req_idx = 0;
	@SuppressWarnings("rawtypes")
	protected XMPPIOServiceListener serviceListener = null;
	private boolean strict_ack = false;
	private long totalPacketsReceived = 0;
	private long totalPacketsSent = 0;
	private ConcurrentSkipListMap<String, Packet> waitingForAck = new ConcurrentSkipListMap<String, Packet>();
	/**
	 * The <code>waitingPackets</code> queue keeps data which have to be processed.
	 */
	private ConcurrentLinkedQueue<Packet> waitingPackets = new ConcurrentLinkedQueue<Packet>();
	private boolean white_char_ack = false;
	private String xmlns = null;
	private boolean xmpp_ack = false;

	/**
	 * Creates a new <code>XMPPIOService</code> instance.
	 */
	public XMPPIOService() {
		super();
		domHandler = new XMPPDomBuilderHandler<>(this);
		getSessionData().put(DOM_HANDLER, domHandler);
	}

	/**
	 * Method <code>addPacketToSend</code> adds new data which will be processed during next run. Data are kept in
	 * proper order like in <em>FIFO</em> queue.
	 *
	 * @param packet a <code>Packet</code> value of data to process.
	 */
	public void addPacketToSend(Packet packet) {

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Added packet to send: {1} [{0}]", new Object[]{toString(), packet});
		}

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

			packet.getElement().addChild(new Element(REQ_NAME, new String[]{ID_ATT}, new String[]{req}));
			waitingForAck.put(req, packet);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Added req {1} for packet: {2} [{0}]", new Object[]{toString(), req, packet});
			}
		}
		if (shouldCountPacket(packet)) {
			++packetsSent;
			++totalPacketsSent;
		}
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
				writeRawData("<stream:error>" + "<policy-violation " + "xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
									 "</stream:error></stream:stream>");
				int counter = 0;

				while (isConnected() && waitingToSend() && (++counter < 10)) {
					writeData(null);
					try {
						Thread.sleep(10);
					} catch (InterruptedException ex) {
					}
				}
			} catch (IOException ex) {
				log.log(Level.FINEST, "Exception sending policy-violation stream error [{0}]", new Object[]{toString()});
			}
			this.forceStop();
			return false;
		}
		return true;
	}

	public boolean checkData(char[] data) throws IOException {

		// by default do nothing and return false
		return false;
	}

	/**
	 * Clears queue of packets waiting to send. In case of connection close this packets may be sent to offline store
	 * but some processors may want stop this from happening - for that they may use this method
	 */
	public void clearWaitingPackets() {
		this.waitingPackets.clear();
	}

	/**
	 * Returns queue with packets waiting to send. For use by ConnectionManager which may need to get undelivered
	 * packets
	 *
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
				log.log(Level.FINEST, "Sending packet: {1} [{0}]", new Object[]{toString(), packet});
			}
			writeRawData(packet.getElement().toString());

			// and after sending it we should remove it to minimalize chances of lost packets
			waitingPackets.poll();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "SENT: {1} [{0}]", new Object[]{toString(), packet.getElement().toString()});
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
		return "jid: " + authorisedUserJid + ", " + super.toString();
	}

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

	public void xmppStreamOpen(final String data) {
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Sending data: {1} [{0}]", new Object[]{toString(), data});
			}
			writeRawData(data);
			assert debug(data, "--SENT:");
		} catch (IOException e) {
			log.log(Level.WARNING, "Error sending stream open data: {1} [{0}]", new Object[]{toString(), e});
			forceStop();
		}
	}

	public long getPacketsReceived(boolean reset) {
		long tmp = packetsReceived;

		if (reset) {
			packetsReceived = 0;
		}

		return tmp;
	}

	public long getPacketsSent(boolean reset) {
		long tmp = packetsSent;

		if (reset) {
			packetsSent = 0;
		}

		return tmp;
	}

	public Queue<Packet> getReceivedPackets() {
		return receivedPackets;
	}

	public long getTotalPacketsReceived() {
		return totalPacketsReceived;
	}

	public long getTotalPacketsSent() {
		return totalPacketsSent;
	}

	public Optional<JID> getAuthorisedUserJid() {
		return Optional.ofNullable(authorisedUserJid);
	}

	public void setAuthorisedUserJid(JID authorisedUserJid) {
		this.authorisedUserJid = authorisedUserJid;
	}

	@Deprecated
	@TigaseDeprecated(removeIn = "9.0.0", since = "8.2.0", note = "#getAuthorisedUserJid should be used instead")
	public String getUserJid() {
		return getAuthorisedUserJid().map(JID::toString).orElse(null);
	}

	@Deprecated
	@TigaseDeprecated(removeIn = "9.0.0", since = "8.2.0", note = "#setAuthorisedUserJid should be used instead")
	public void setUserJid(String jid) {
		this.authorisedUserJid = JID.jidInstanceNS(jid);
	}

	public Map<String, Packet> getWaitingForAct() {
		for (Packet p : waitingForAck.values()) {
			Element req = p.getElement().getChild(REQ_NAME);

			if (req == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Missing req element in waiting for ACK packet: {1} [{0}]",
							new Object[]{toString(), p});
				}
			} else {
				p.getElement().removeChild(req);
			}
		}

		return waitingForAck;
	}

	public String getXMLNS() {
		return this.xmlns;
	}

	public void setXMLNS(String xmlns) {
		this.xmlns = xmlns;
	}

	public void setAckMode(boolean white_char_ack, boolean xmpp_ack, boolean strict) {
		this.white_char_ack = white_char_ack;
		this.xmpp_ack = xmpp_ack;
		this.strict_ack = strict;
	}

	public void setElementLimits(int limit) {

	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setIOServiceListener(XMPPIOServiceListener servList) {
		this.serviceListener = servList;
		super.setIOServiceListener(servList);
	}

	public void setProcessors(XMPPIOProcessor[] processors) {
		this.processors = processors;
	}

	/**
	 * This method returns the time when the last XMPP packet was received, it is used to help detect dead connections.
	 *
	 * @return {@code long} number denoting time when the last XMPP packet was received.
	 */
	public long getLastXmppPacketReceiveTime() {
		return lastXmppPacketReceivedTime;
	}

	/**
	 * Method <code>addReceivedPacket</code> puts processing results to queue. The processing results are usually data
	 * (messages) which has been just received from socket.
	 *
	 * @param packet a <code>Packet</code> value of processing results.
	 */
	protected void addReceivedPacket(final Packet packet) {
		if (firstPacket) {
			if ("policy-file-request" == packet.getElemName()) {
				log.log(Level.FINER, "Got flash cross-domain request" + packet);
				String cross_domain_policy = (this.serviceListener instanceof ConnectionManager)
											 ? ((ConnectionManager) serviceListener).getFlashCrossDomainPolicy()
											 : null;
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
			if (shouldCountPacket(packet)) {
				++packetsReceived;
				++totalPacketsReceived;
			}
			setLastXmppPacketReceiveTime();
			receivedPackets.offer(packet);
		}
	}

	protected boolean shouldCountPacket(Packet packet) {
		return packet.getXMLNS() != StreamManagementIOProcessor.XMLNS;
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
					log.log(Level.FINEST, "READ:{1} [{0}]", new Object[]{toString(), new String(data)});
				}

				boolean disconnect = checkData(data);

				if (disconnect) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "checkData says disconnect: {1} [{0}]",
								new Object[]{toString(), new String(data)});
					} else {
						log.log(Level.INFO, "checkData says disconnect [{0}]", toString());
					}
					forceStop();

					return;

					// domHandler = new XMPPDomBuilderHandler<RefObject>(this);
				}

				// This is log for debugging only,
				// in normal mode don't even call below code
				assert debug(new String(data), "--RECEIVED:");

				try {
					parser.parse(domHandler, data, 0, data.length);
					if (domHandler.parseError()) {
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "Data parsing error: {1} [{0}]",
									new Object[]{toString(), StringUtilities.convertNonPrintableCharactersToLiterals(new String(data))});
						} else {
							log.log(Level.INFO, "Data parsing error, stopping connection [{0}]", toString());
						}
						if (serviceListener != null) {
							Element err = new Element("not-well-formed", new String[]{"xmlns"},
													  new String[]{"urn:ietf:params:xml:ns:xmpp-streams"});
							String streamErrorStr = serviceListener.xmppStreamError(this,
																					Collections.singletonList(err));
							writeRawData(streamErrorStr);
						}
						forceStop();

						return;

						// domHandler = new XMPPDomBuilderHandler<RefObject>(this);
					}

					moveParsedPacketsToReceived(true);
				} catch (Exception ex) {
					log.log(Level.INFO,
							"Incorrect XML data: " + new String(data) + ", stopping connection " + " [" + toString() +
									"] exception: ", ex);
					forceStop();
				} finally {
					if (domHandler.isStreamClosed()) {
						xmppStreamClosed();
					}
				}  // end of try-catch
				data = readData();
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Function called when the service is not connected! forceStop() [{0}]", toString());
			}
			forceStop();
		}
	}

	@Override
	protected int receivedPackets() {
		return receivedPackets.size();
	}

	@SuppressWarnings({"unchecked"})
	protected void xmppStreamClosed() {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Received STREAM-CLOSE from the client [{0}]", toString());
		}

		if (processors != null) {
			for (XMPPIOProcessor processor : processors) {
				processor.serviceStopped(this, true);
			}
		}

		try {
			if (isConnected()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Sending data: </stream:stream>, as socket is still connected [{0}]",
							toString());
				}
				writeRawData(prepareStreamClose());
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Not sending data: </stream:stream>, as socket is already closed [{0}]",
							toString());
				}
			}
		} catch (IOException e) {
			log.log(Level.INFO, "Error sending stream closed data: {1} [{0}]", new Object[]{toString(), e});
		}

		// streamClosed = true;
		if (serviceListener != null) {
			serviceListener.xmppStreamClosed(this);
		}
	}

	@SuppressWarnings({"unchecked"})
	protected void xmppStreamOpened(Map<String, String> attribs) {
		if (serviceListener != null) {
			String[] responses = serviceListener.xmppStreamOpened(this, attribs);

			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Sending data: {1} [{0}]", new Object[]{toString(), (responses != null ? String.join("", responses) : "null")});
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
				log.log(Level.WARNING, "Error sending stream open data: {1} [{0}]", new Object[]{toString(), e});
				forceStop();
			}
		}
	}

	private void sendAck(Packet packet) {

		// If stanza receiving confirmation is configured,
		// try to send confirmation back
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

	protected boolean hasParsedElements() {
		return !domHandler.getParsedElements().isEmpty();
	}

	protected void moveParsedPacketsToReceived(boolean sendAck) {
		Element elem = null;
		Queue<Element> elems = domHandler.getParsedElements();

		if (elems.size() > 0 && sendAck) {
			readCompleted();
		}
		while ((elem = elems.poll()) != null) {
			try {
				// assert debug(elem.toString() + "\n");
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Read packet: {1} [{0}]", new Object[]{toString(), elem});
				}

				// System.out.print(elem.toString());
				Packet pack = Packet.packetInstance(elem);

				addReceivedPacket(pack);
				if (sendAck) {
					sendAck(pack);
				}
			} catch (TigaseStringprepException ex) {
				log.log(Level.INFO, "Incorrect to/from JID format for stanza: " + elem.toString() + " [" + toString() + "]", ex);
			}
		}    // end of while ((elem = elems.poll()) != null)
	}

	/**
	 * This method sets the time of last received XMPP packet, it is used to help detect dead connections.
	 */
	private void setLastXmppPacketReceiveTime() {
		lastXmppPacketReceivedTime = System.currentTimeMillis();
	}
}    // XMPPIOService

