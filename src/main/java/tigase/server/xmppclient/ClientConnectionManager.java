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

package tigase.server.xmppclient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.Deflater;
import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.util.DNSResolver;
import tigase.util.JIDUtils;
import tigase.util.RoutingsContainer;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
//import tigase.net.IOService;
import tigase.net.SocketReadThread;
import tigase.server.ReceiverEventHandler;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

/**
 * Class ClientConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClientConnectionManager
		extends ConnectionManager<XMPPIOService<Object>> {
	//	implements XMPPService {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger(ClientConnectionManager.class.getName());

	private static final String XMLNS = "jabber:client";

	private static final String ROUTINGS_PROP_KEY = "routings";
	private static final String ROUTING_MODE_PROP_KEY = "multi-mode";
	private static final boolean ROUTING_MODE_PROP_VAL = true;
	private static final String ROUTING_ENTRY_PROP_KEY = ".+";

	protected RoutingsContainer routings = null;

	private Map<String, XMPPProcessorIfc> processors =
		new ConcurrentSkipListMap<String, XMPPProcessorIfc>();
	private ReceiverEventHandler stoppedHandler = newStoppedHandler();
	private ReceiverEventHandler startedHandler = newStartedHandler();

	/**
	 * This is mostly for testing purpose. We want to investigate massive 
	 * (10k per node) connections drops at the same time during tests with Tsung. 
	 * I suspect this might be due to problems with one of the tsung VMs working 
	 * in the cluster generating load.
	 * If I am right then all disconnects should come from only one or just a few
	 * machines. If I am not right disconnects should be distributed evenly among
	 * all Tsung IPs.
	 */
	private IPMonitor ipMonitor = new IPMonitor();

	@Override
	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Processing packet: " + packet.getElemName()
				+ ", type: " + packet.getType());
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toStringSecure());
		}
		if (packet.isCommand() && packet.getCommand() != Command.OTHER) {
			processCommand(packet);
		} else {
			if (!writePacketToSocket(packet)) {
				// Connection closed or broken, send message back to the SM
				// if this is not IQ result...
				// Ignore also all presence packets with available, unavailble
				if (packet.getType() != StanzaType.result &&
								packet.getType() != StanzaType.available &&
								packet.getType() != StanzaType.unavailable &&
								packet.getType() != StanzaType.error &&
								!(packet.getElemName() == "presence" && packet.getType() == null)) {
					try {
						Packet error =
										Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
										"The user connection is no longer active.", true);
						addOutPacket(error);
					} catch (PacketErrorTypeException e) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest(
											"Ups, already error packet. Dropping it to prevent infinite loop.");
						}
					}
				}
				// In case the SessionManager lost synchronization for any reason, let's
				// notify it that the user connection no longer exists.
				// But in case of mass-disconnects we might have lot's of presences
				// floating around, so just skip sending stream_close for all the
				// offline presences
				if (packet.getType() != StanzaType.unavailable) {
					Packet command = Command.STREAM_CLOSED_UPDATE.getPacket(null, null,
									StanzaType.set, UUID.randomUUID().toString());
					command.setFrom(packet.getTo());
					command.setTo(packet.getFrom());
					// Note! we don't want to receive response to this request, thus
					// STREAM_CLOSED_UPDATE instead of STREAM_CLOSED
					addOutPacket(command);
//				addOutPacketWithTimeout(command, stoppedHandler, 15l, TimeUnit.SECONDS);
					log.fine("Sending a command to close the remote session for non-existen Bosh connection: " +
									command.toStringSecure());
				}
			}
		} // end of else
	}

	protected void processCommand(final Packet packet) {
		XMPPIOService<Object> serv = getXMPPIOService(packet);
		switch (packet.getCommand()) {
			case GETFEATURES:
				if (packet.getType() == StanzaType.result) {
					List<Element> features = getFeatures(getXMPPSession(packet));
					Element elem_features = new Element("stream:features");
					elem_features.addChildren(features);
					elem_features.addChildren(Command.getData(packet));
					Packet result = new Packet(elem_features);
					result.setTo(packet.getTo());
					writePacketToSocket(result);
				} // end of if (packet.getType() == StanzaType.get)
				break;
			case STARTZLIB:
				if (serv != null) {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Starting zlib compression: " + serv.getUniqueId());
					}
					try {
						Element compressed =
										Command.getData(packet, "compressed", null);
						Packet p_compressed = new Packet(compressed);
						SocketReadThread readThread = SocketReadThread.getInstance();
						readThread.removeSocketService(serv);
						//					writePacketToSocket(serv, p_proceed);
						serv.addPacketToSend(p_compressed);
						serv.processWaitingPackets();
						serv.startZLib(Deflater.BEST_COMPRESSION);
						//					serv.call();
						readThread.addSocketService(serv);
					} catch (IOException ex) {
						Logger.getLogger(ClientConnectionManager.class.getName()).
										log(Level.SEVERE, null, ex);
					}
				} else {
					log.warning("Can't find sevice for STARTZLIB command: " +
									packet.getStringData());
				}
				break;
			case STARTTLS:
				if (serv != null) {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Starting TLS for connection: " + serv.getUniqueId());
					}
					try {
						// Note:
						// If you send <proceed> packet to client you must expect
						// instant response from the client with TLS handshaking data
						// before you will call startTLS() on server side.
						// So the initial handshaking data might be lost as they will
						// be processed in another thread reading data from the socket.
						// That's why below code first removes service from reading
						// threads pool and then sends <proceed> packet and starts TLS.
						Element proceed = Command.getData(packet, "proceed", null);
						Packet p_proceed = new Packet(proceed);
						SocketReadThread readThread = SocketReadThread.getInstance();
						readThread.removeSocketService(serv);
						//					writePacketToSocket(serv, p_proceed);
						serv.addPacketToSend(p_proceed);
						serv.processWaitingPackets();
						while (serv.waitingToSend()) {
							serv.writeRawData(null);
							Thread.sleep(10);
						}
						serv.startTLS(false);
						//					serv.call();
						readThread.addSocketService(serv);
					} catch (Exception e) {
						log.warning("Error starting TLS: " + e);
					} // end of try-catch
				} else {
					log.warning("Can't find sevice for STARTTLS command: " +
									packet.getStringData());
				} // end of else
				break;
			case REDIRECT:
				String command_sessionId = Command.getFieldValue(packet, "session-id");
				String newAddress = packet.getFrom();
				String old_receiver = changeDataReceiver(packet, newAddress,
								command_sessionId, serv);
				if (old_receiver != null) {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Redirecting data for sessionId: " + command_sessionId +
										", to: " + newAddress);
					}
					Packet response = null;
					response = packet.commandResult(null);
					Command.addFieldValue(response, "session-id", command_sessionId);
					Command.addFieldValue(response, "action", "activate");
					response.getElement().setAttribute("to", newAddress);
					addOutPacket(response);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Connection for REDIRECT command does not exist, ignoring " +
										"packet: " + packet.toStringSecure());
					}
				}
				break;
			case STREAM_CLOSED:

				break;
			case GETDISCO:

				break;
			case CLOSE:
				if (serv != null) {
					serv.stop();
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Attempt to stop non-existen service for packet: " +
										packet.getStringData() + ", Service already stopped?");
					}
				} // end of if (serv != null) else
				break;
			case CHECK_USER_CONNECTION:
				if (serv != null) {
					// It's ok, the session has been found, respond with OK.
					addOutPacket(packet.okResult((String) null, 0));
				} else {
					// Session is no longer active, respond with an error.
					try {
						addOutPacket(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
										"Connection gone.", false));
					} catch (PacketErrorTypeException e) {
						// Hm, error already, ignoring...
						log.info("Error packet is not really expected here: " +
										packet.toStringSecure());
					}
				}
				break;

			default:
				writePacketToSocket(packet);
				break;
		} // end of switch (pc.getCommand())
	}

	protected String changeDataReceiver(Packet packet, String newAddress,
		String command_sessionId, XMPPIOService<Object> serv) {
		if (serv != null) {
			String serv_sessionId =
          (String)serv.getSessionData().get(XMPPIOService.SESSION_ID_KEY);
			if (serv_sessionId.equals(command_sessionId)) {
				String old_receiver = serv.getDataReceiver();
				serv.setDataReceiver(newAddress);
				return old_receiver;
			} else {
				log.warning("Incorrect session ID, ignoring data redirect for: "
					+ newAddress + ", expected: " + serv_sessionId
					+ ", received: " + command_sessionId);
			}
		}
		return null;
	}

	@Override
	public Queue<Packet> processSocketData(XMPPIOService<Object> serv) {

		String id = getUniqueId(serv);
		//String hostname = (String)serv.getSessionData().get(serv.HOSTNAME_KEY);

		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Processing packet: " + p.getElemName()
					+ ", type: " + p.getType());
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Processing socket data: " + p.toStringSecure());
			}
			p.setFrom(getFromAddress(id));
			String receiver = serv.getDataReceiver();
			if (receiver != null) {
				p.setTo(serv.getDataReceiver());
				addOutPacket(p);
			} else {
				// Hm, receiver is not set yet..., ignoring
			}
			// 			results.offer(new Packet(new Element("OK")));
		} // end of while ()
		return null;
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		Boolean r_mode = (Boolean)params.get(getName() + "/" + ROUTINGS_PROP_KEY
			+ "/" + ROUTING_MODE_PROP_KEY);
		if (r_mode == null) {
			props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY,
				ROUTING_MODE_PROP_VAL);
			// If the server is configured as connection manager only node then
			// route packets to SM on remote host where is default routing
			// for external component.
			// Otherwise default routing is to SM on localhost
			if (params.get("config-type").equals(GEN_CONFIG_CS)
				&& params.get(GEN_EXT_COMP) != null) {
				String[] comp_params = ((String)params.get(GEN_EXT_COMP)).split(",");
				props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
					DEF_SM_NAME + "@" + comp_params[1]);
			} else {
				props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
					DEF_SM_NAME + "@" + DNSResolver.getDefaultHostname());
			}
		}
		return props;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		boolean routing_mode =
			(Boolean)props.get(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY);
		routings = new RoutingsContainer(routing_mode);
		int idx = (ROUTINGS_PROP_KEY + "/").length();
		for (Map.Entry<String, Object> entry: props.entrySet()) {
			if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/")
				&& !entry.getKey().equals(ROUTINGS_PROP_KEY + "/" +
					ROUTING_MODE_PROP_KEY)) {
				routings.addRouting(entry.getKey().substring(idx),
					(String)entry.getValue());
			} // end of if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/"))
		} // end of for ()
	}

	private XMPPResourceConnection getXMPPSession(Packet p) {
		XMPPIOService<Object> serv = getXMPPIOService(p);
		return serv == null ? null :
			(XMPPResourceConnection)serv.getSessionData().get("xmpp-session");
	}

	private List<Element> getFeatures(XMPPResourceConnection session) {
		List<Element> results = new LinkedList<Element>();
		for (XMPPProcessorIfc proc: processors.values()) {
			Element[] features = proc.supStreamFeatures(session);
			if (features != null) {
				results.addAll(Arrays.asList(features));
			} // end of if (features != null)
		} // end of for ()
		return results;
	}

	@Override
	protected int[] getDefPlainPorts() {
		return new int[] {5222};
	}

	@Override
	protected int[] getDefSSLPorts() {
		return new int[] {5223};
	}

	private String getFromAddress(String id) {
		return JIDUtils.getJID(getName(), getDefHostName(), id);
	}

	@Override
	public String xmppStreamOpened(XMPPIOService<Object> serv,
		Map<String, String> attribs) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream opened: " + attribs.toString());
		}
		final String hostname = attribs.get("to");
		String lang = attribs.get("xml:lang");
		if (lang == null) {
			lang = "en";
		}

		if (hostname == null) {
			return "<?xml version='1.0'?><stream:stream"
				+ " xmlns='" + XMLNS + "'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " id='tigase-error-tigase'"
				+ " from='" + getDefHostName() + "'"
        + " version='1.0' xml:lang='en'>"
				+ "<stream:error>"
				+ "<improper-addressing xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
		} // end of if (hostname == null)

		if (!isLocalDomain(hostname)) {
			return "<?xml version='1.0'?><stream:stream"
				+ " xmlns='" + XMLNS + "'"
				+ " xmlns:stream='http://etherx.jabber.org/streams'"
				+ " id='tigase-error-tigase'"
				+ " from='" + getDefHostName() + "'"
        + " version='1.0' xml:lang='en'>"
				+ "<stream:error>"
				+ "<host-unknown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"
				+ "</stream:error>"
				+ "</stream:stream>"
				;
		} // end of if (!hostnames.contains(hostname))

		String id = (String) serv.getSessionData().get(XMPPIOService.SESSION_ID_KEY);
		if (id == null) {
			id = UUID.randomUUID().toString();
			serv.getSessionData().put(XMPPIOService.SESSION_ID_KEY, id);
			serv.setXMLNS(XMLNS);
			serv.getSessionData().put(XMPPIOService.HOSTNAME_KEY, hostname);
			serv.setDataReceiver(routings.computeRouting(hostname));
			writeRawData(serv, "<?xml version='1.0'?><stream:stream" +
							" xmlns='" + XMLNS + "'" +
							" xmlns:stream='http://etherx.jabber.org/streams'" +
							" from='" + hostname + "'" +
							" id='" + id + "'" + " version='1.0' xml:lang='en'>");
			Packet streamOpen = Command.STREAM_OPENED.getPacket(
				getFromAddress(getUniqueId(serv)),
				serv.getDataReceiver(), StanzaType.set, UUID.randomUUID().toString(),
				Command.DataType.submit);
			Command.addFieldValue(streamOpen, "session-id", id);
			Command.addFieldValue(streamOpen, "hostname", hostname);
			Command.addFieldValue(streamOpen, "xml:lang", lang);
			addOutPacketWithTimeout(streamOpen, startedHandler, 45l, TimeUnit.SECONDS);
		} else {
			writeRawData(serv, "<?xml version='1.0'?><stream:stream" +
							" xmlns='" + XMLNS + "'" +
							" xmlns:stream='http://etherx.jabber.org/streams'" +
							" from='" + hostname + "'" +
							" id='" + id + "'" + " version='1.0' xml:lang='en'>");
			addOutPacket(Command.GETFEATURES.getPacket(getFromAddress(getUniqueId(serv)),
							serv.getDataReceiver(), StanzaType.get,
							UUID.randomUUID().toString(),
							null));
		}
		return null;
	}

	@Override
	public boolean serviceStopped(XMPPIOService<Object> service) {
		boolean result = super.serviceStopped(service);
		// It might be a Bosh service in which case it is ignored here.
		if (service.getXMLNS() == XMLNS) {
			//		XMPPIOService serv = (XMPPIOService)service;
			// The method may be called more than one time for a single
			// connection but we want to send a notification just once
			if (result) {
				ipMonitor.addDisconnect(service.getRemoteAddress());
				if (service.getDataReceiver() != null) {
					Packet command = Command.STREAM_CLOSED.getPacket(
									getFromAddress(getUniqueId(service)),
									service.getDataReceiver(), StanzaType.set,
									UUID.randomUUID().toString());
					// In case of mass-disconnects, adjust the timeout properly
					addOutPacketWithTimeout(command, stoppedHandler,
									120l, TimeUnit.SECONDS);
					if (log.isLoggable(Level.FINE)) {
						log.fine("Service stopped, sending packet: " +
										command.getStringData());
					}
//					// For testing only.
//					System.out.println("Service stopped: " + service.getUniqueId());
//					Thread.dumpStack();
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Service stopped, before stream:stream received");
					}
				}
			}
		}
		return result;
	}

	@Override
	public void xmppStreamClosed(XMPPIOService<Object> serv) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream closed: " + serv.getUniqueId());
		}
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. Let's assume user should send something
	 * at least once every 24 hours....
	 *
	 * @return a <code>long</code> value
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 24*HOUR;
	}

	@Override
	public void start() {
		super.start();
		ipMonitor = new IPMonitor();
		ipMonitor.start();
	}

	@Override
  public void stop() {
		super.stop();
		ipMonitor.stopThread();
	}

	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new XMPPIOService<Object>();
	}

	protected ReceiverEventHandler newStoppedHandler() {
		return new StoppedHandler();
	}

	protected ReceiverEventHandler newStartedHandler() {
		return new StartedHandler();
	}

	@Override
	protected Integer getMaxQueueSize(int def) {
		return def * 10;
	}

	/**
	 * This method can be overwritten in extending classes to get a different
	 * packets distribution to different threads. For PubSub, probably better
	 * packets distribution to different threads would be based on the
	 * sender address rather then destination address.
	 * @param packet
	 * @return
	 */
	@Override
	public int hashCodeForPacket(Packet packet) {
		return packet.getTo().hashCode();
	}

	@Override
	public int processingThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public String getDiscoDescription() {
		return "Client connection manager";
	}

	@Override
	public String getDiscoCategory() {
		return "c2s";
	}

	private class StoppedHandler implements ReceiverEventHandler {

		@Override
		public void timeOutExpired(Packet packet) {
			// Ups, doesn't look good, the server is either oveloaded or lost
			// a packet.
			log.info("No response within time limit received for a packet: " +
							packet.toStringSecure());
			addOutPacketWithTimeout(packet, stoppedHandler, 60l, TimeUnit.SECONDS);
		}

		@Override
		public void responseReceived(Packet packet, Packet response) {
			// Great, nothing to worry about.
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Response for stop received...");
			}
		}

	}

	private class StartedHandler implements ReceiverEventHandler {

		@Override
		public void timeOutExpired(Packet packet) {
			// If we still haven't received confirmation from the SM then
			// the packet either has been lost or the server is overloaded
			// In either case we disconnect the connection.
			log.info("No response within time limit received for a packet: " +
							packet.toStringSecure());
			XMPPIOService<Object> serv = getXMPPIOService(packet.getFrom());
			if (serv != null) {
				serv.stop();
			} else {
				log.fine("Attempt to stop non-existen service for packet: "
					+ packet.getStringData() + ", Service already stopped?");
			} // end of if (serv != null) else
		}

		@Override
		public void responseReceived(Packet packet, Packet response) {
			// We are now ready to ask for features....
			addOutPacket(Command.GETFEATURES.getPacket(packet.getFrom(),
							packet.getTo(), StanzaType.get, UUID.randomUUID().toString(),
							null));
		}

	}

}
