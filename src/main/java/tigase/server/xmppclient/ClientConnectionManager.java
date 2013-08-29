/*
 * ClientConnectionManager.java
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



package tigase.server.xmppclient;

//~--- non-JDK imports --------------------------------------------------------

import tigase.net.IOService;
import tigase.net.SocketThread;

import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ReceiverTimeoutHandler;

import tigase.util.DNSResolver;
import tigase.util.RoutingsContainer;
import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.zip.Deflater;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Class ClientConnectionManager Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClientConnectionManager
				extends ConnectionManager<XMPPIOService<Object>> {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(ClientConnectionManager.class
			.getName());
	private static final String ROUTING_ENTRY_PROP_KEY     = ".+";
	private static final String ROUTING_MODE_PROP_KEY      = "multi-mode";
	private static final String ROUTINGS_PROP_KEY          = "routings";
	private static final long   SOCKET_CLOSE_WAIT_PROP_DEF = 1;
	private static final String SOCKET_CLOSE_WAIT_PROP_KEY = "socket-close-wait";
	private static final String TLS_WANT_CLIENT_AUTH_ENABLED_KEY =
			"tls-want-client-auth-enabled";
	private static final String  XMLNS                            = "jabber:client";
	private static final boolean TLS_WANT_CLIENT_AUTH_ENABLED_DEF = false;
	private static final boolean ROUTING_MODE_PROP_VAL            = true;

	//~--- fields ---------------------------------------------------------------

	// ~--- fields
	// ---------------------------------------------------------------

	/** Field description */
	protected RoutingsContainer routings = null;

	/** Field description */
	protected SeeOtherHostIfc see_other_host_strategy = null;

//private final Map<String, XMPPProcessorIfc> processors = new ConcurrentHashMap<String,
//    XMPPProcessorIfc>();
	private XMPPIOProcessor[]            processors             = new XMPPIOProcessor[0];
	private final ReceiverTimeoutHandler stoppedHandler         = newStoppedHandler();
	private final ReceiverTimeoutHandler startedHandler         = newStartedHandler();
	private long                         socket_close_wait_time =
			SOCKET_CLOSE_WAIT_PROP_DEF;

	/**
	 * This is mostly for testing purpose. We want to investigate massive (10k
	 * per node) connections drops at the same time during tests with Tsung. I
	 * suspect this might be due to problems with one of the tsung VMs working
	 * in the cluster generating load. If I am right then all disconnects should
	 * come from only one or just a few machines. If I am not right disconnects
	 * should be distributed evenly among all Tsung IPs.
	 */
	private IPMonitor                       ipMonitor = new IPMonitor();
	private final ClientTrustManagerFactory clientTrustManagerFactory =
			new ClientTrustManagerFactory();
	private boolean tlsWantClientAuthEnabled = TLS_WANT_CLIENT_AUTH_ENABLED_DEF;

	//~--- methods --------------------------------------------------------------

	// ~--- methods
	// --------------------------------------------------------------

	/**
	 * This method can be overwritten in extending classes to get a different
	 * packets distribution to different threads. For PubSub, probably better
	 * packets distribution to different threads would be based on the sender
	 * address rather then destination address.
	 *
	 * @param packet
	 *
	 *
	 * @return a value of int
	 */
	@Override
	public int hashCodeForPacket(Packet packet) {
		if ((packet.getPacketFrom() != null) && getComponentId().getBareJID().equals(packet
				.getPacketFrom().getBareJID())) {
			return packet.getPacketFrom().hashCode();
		} else {
			return packet.getTo().hashCode();
		}
	}

	/**
	 * Method description
	 *
	 * @param packet
	 */
	@Override
	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet.toStringSecure());
		}
		if (packet.isCommand() && (packet.getCommand() != Command.OTHER)) {
			processCommand(packet);
		} else {
			if (!writePacketToSocket(packet)) {

				// Connection closed or broken, send message back to the SM
				// if this is not IQ result...
				// Ignore also all presence packets with available, unavailble
				if ((packet.getType() != StanzaType.result) && (packet.getType() != StanzaType
						.available) && (packet.getType() != StanzaType.unavailable) && (packet
						.getType() != StanzaType.error) &&!((packet.getElemName() == "presence") &&
						(packet.getType() == null))) {
					try {
						Packet error = Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
								"The user connection is no longer active.", true);

						addOutPacket(error);
					} catch (PacketErrorTypeException e) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest(
									"Ups, already error packet. Dropping it to prevent infinite loop.");
						}
					}
				}

				// In case the SessionManager lost synchronization for any
				// reason, let's
				// notify it that the user connection no longer exists.
				// But in case of mass-disconnects we might have lot's of
				// presences
				// floating around, so just skip sending stream_close for all
				// the
				// offline presences
				if ((packet.getType() != StanzaType.unavailable) && (packet.getPacketFrom() !=
						null)) {
					if (packet.getStanzaTo() != null) {
						Packet command = Command.STREAM_CLOSED_UPDATE.getPacket(packet.getStanzaTo(),
								packet.getPacketFrom(), StanzaType.set, UUID.randomUUID().toString());

						command.setPacketFrom(packet.getPacketTo());
						command.setPacketTo(packet.getPacketFrom());

						// Note! we don't want to receive response to this
						// request,
						// thus STREAM_CLOSED_UPDATE instead of STREAM_CLOSED
						addOutPacket(command);

						// addOutPacketWithTimeout(command, stoppedHandler, 15l,
						// TimeUnit.SECONDS);
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE,
									"Sending a command to close the remote session for non-existen {0} connection: {1}",
									new Object[] { getName(),
									command.toStringSecure() });
						}
					} else {
						if (log.isLoggable(Level.WARNING)) {
							log.log(Level.FINE,
									"Stream close update without an user JID, skipping for packet: {0}",
									new Object[] { packet });
						}
					}
				}
			}
		}    // end of else
	}

	/**
	 * Method description
	 *
	 * @param serv
	 *
	 *
	 * @return a value of Queue<Packet>
	 */
	@Override
	public Queue<Packet> processSocketData(XMPPIOService<Object> serv) {

		// String id = getUniqueId(serv);
		JID id = serv.getConnectionId();

		// String hostname =
		// (String)serv.getSessionData().get(serv.HOSTNAME_KEY);
		Packet p = null;

		while ((p = serv.getReceivedPackets().poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing socket data: {0} from connection: {1}",
						new Object[] { p.toStringSecure(),
						id });
			}

			// Sometimes xmlns is not set for the packet. Usually it does not
			// cause any problems but when the packet is sent over the s2s, ext
			// or cluster connection it may be quite problematic.
			// Let's force jabber:client xmlns for all packets received from c2s
			// connection
			// Ups, some packets like starttls or sasl-auth have own XMLNS,
			// overwriting it here is not really a good idea. We have to check
			// first
			// if the xmlns is not set and then force it to jabber:client
			if (p.getAttributeStaticStr(Packet.XMLNS_ATT) == null) {
				p.setXMLNS(XMLNS);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "XMLNS set for packet: {0} from connection: {1}",
							new Object[] { p.toStringSecure(),
							id });
				}
			}

			// p.setPacketFrom(getFromAddress(id));
			p.setPacketFrom(id);

			JID receiver = serv.getDataReceiver();

			if (receiver != null) {
				p.setPacketTo(serv.getDataReceiver());
				addOutPacket(p);
			} else {

				// Hm, receiver is not set yet..., ignoring
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO,
							"Hm, receiver is not set yet (misconfiguration error)..., ignoring: {0}, connection: {1}",
							new Object[] { p.toStringSecure(),
							serv });
				}
			}

			// TODO: Implement sending 'req' attributes by the server too
		}    // end of while ()

		return null;
	}

	/**
	 * Method description
	 *
	 * @param port_props
	 */
	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {}

	/**
	 * Method description
	 *
	 *
	 * @param service
	 */
	@Override
	public void serviceStarted(XMPPIOService<Object> service) {
		super.serviceStarted(service);

		String id           = getUniqueId(service);
		JID    connectionId = getFromAddress(id);

		service.setConnectionId(connectionId);
		service.setProcessors(processors);
	}

	/**
	 * Method description
	 *
	 * @param service
	 *
	 *
	 * @return a value of boolean
	 */
	@Override
	public boolean serviceStopped(XMPPIOService<Object> service) {
		boolean result = super.serviceStopped(service);

		xmppStreamClosed(service);

		return result;
	}

	// ~--- methods
	// --------------------------------------------------------------

	/**
	 * Method description
	 */
	@Override
	public void start() {
		super.start();
		ipMonitor = new IPMonitor();
		ipMonitor.start();
	}

	/**
	 * Method description
	 */
	@Override
	public void stop() {
		super.stop();
		ipMonitor.stopThread();
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void tlsHandshakeCompleted(XMPPIOService<Object> serv) {
		if ((serv.getPeersJIDsFromCert() != null) && clientTrustManagerFactory.isActive()) {
			Packet clientAuthCommand = Command.CLIENT_AUTH.getPacket(serv.getConnectionId(),
					serv.getDataReceiver(), StanzaType.set, this.newPacketId("c2s-"), Command
					.DataType.submit);
			final String id = (String) serv.getSessionData().get(IOService.SESSION_ID_KEY);

			Command.addFieldValue(clientAuthCommand, "session-id", id);
			Command.addFieldValue(clientAuthCommand, "peer-certificate", "true");
			Command.addFieldMultiValue(clientAuthCommand, "jids", serv.getPeersJIDsFromCert());
			addOutPacket(clientAuthCommand);
		}
	}

	/**
	 * Method description
	 *
	 * @param serv
	 */
	@Override
	public void xmppStreamClosed(XMPPIOService<Object> serv) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Stream closed: {0}", serv.getConnectionId());
		}

		// It might be a Bosh service in which case it is ignored here.
		// The method may be called more than one time for a single
		// connection but we want to send a notification just once
		if ((serv.getXMLNS() == XMLNS) && (serv.getSessionData().get("stream-closed") ==
				null)) {
			serv.getSessionData().put("stream-closed", "stream-closed");
			ipMonitor.addDisconnect(serv.getRemoteAddress());
			if (serv.getDataReceiver() != null) {
				Packet command = Command.STREAM_CLOSED.getPacket(serv.getConnectionId(), serv
						.getDataReceiver(), StanzaType.set, UUID.randomUUID().toString());
				String userJid = serv.getUserJid();

				if (userJid != null) {
					Command.addFieldValue(command, "user-jid", userJid);
				}

				// In case of mass-disconnects, adjust the timeout properly
				addOutPacketWithTimeout(command, stoppedHandler, 120l, TimeUnit.SECONDS);
				log.log(Level.FINE, "Service stopped, sending packet: {0}", command);

				//// For testing only.
				// System.out.println("Service stopped: " +
				// service.getUniqueId());
				// Thread.dumpStack();
				//// For testing only.
				// System.out.println("Service stopped: " +
				// service.getUniqueId());
				// Thread.dumpStack();
			} else {
				log.fine("Service stopped, before stream:stream received");
			}
			serv.stop();
		}
	}

	/**
	 * Method description
	 *
	 * @param serv
	 * @param attribs
	 *
	 *
	 * @return a value of String
	 */
	@Override
	public String xmppStreamOpened(XMPPIOService<Object> serv, Map<String,
			String> attribs) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Stream opened: {0}", attribs);
		}

		String       lang     = attribs.get("xml:lang");
		final String hostname = attribs.get("to");
		final String from     = attribs.get("from");
		BareJID      fromJID  = null;

		if (from != null) {
			try {
				fromJID = BareJID.bareJIDInstance(from);
			} catch (TigaseStringprepException ex) {
				log.log(Level.CONFIG, "From JID violates RFC6122 (XMPP:Address Format): ", ex);

				return "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS + "'" +
						" xmlns:stream='http://etherx.jabber.org/streams'" +
						" id='tigase-error-tigase'" + " from='" + getDefVHostItem() + "'" +
						" version='1.0' xml:lang='en'>" + "<stream:error>" +
						"<improper-addressing xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
						"</stream:error>" + "</stream:stream>";
			}    // end of: try-catch
		}      // end of: if (from != null) {
		if (lang == null) {
			lang = "en";
		}
		if (hostname == null) {
			return "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS + "'" +
					" xmlns:stream='http://etherx.jabber.org/streams'" +
					" id='tigase-error-tigase'" + " from='" + getDefVHostItem() + "'" +
					" version='1.0' xml:lang='en'>" + "<stream:error>" +
					"<improper-addressing xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
					"</stream:error>" + "</stream:stream>";
		}    // end of if (hostname == null)
		if (!isLocalDomain(hostname)) {
			return "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS + "'" +
					" xmlns:stream='http://etherx.jabber.org/streams'" +
					" id='tigase-error-tigase'" + " from='" + getDefVHostItem() + "'" +
					" version='1.0' xml:lang='en'>" + "<stream:error>" +
					"<host-unknown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
					"</stream:error>" + "</stream:stream>";
		}    // end of if (!hostnames.contains(hostname))
		if ((fromJID != null) && (see_other_host_strategy != null) && see_other_host_strategy
				.isEnabled(SeeOtherHostIfc.Phase.OPEN)) {
			BareJID see_other_host = see_other_host_strategy.findHostForJID(fromJID,
					getDefHostName());

			if ((see_other_host != null) &&!see_other_host.equals(getDefHostName())) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Sending redirect for {0} to host {1}, connection {2}.",
							new Object[] { fromJID,
							see_other_host, serv });
				}

				return "<stream:stream" + " xmlns='" + XMLNS + "'" +
						" xmlns:stream='http://etherx.jabber.org/streams'" +
						" id='tigase-error-tigase'" + " from='" + getDefVHostItem() + "'" +
						" version='1.0' xml:lang='en'>" + see_other_host_strategy.getStreamError(
						"urn:ietf:params:xml:ns:xmpp-streams", see_other_host).toString() +
						"</stream:stream>";
			}
		}    // of if (from != null )

		String id = (String) serv.getSessionData().get(IOService.SESSION_ID_KEY);

		if (id == null) {
			id = UUID.randomUUID().toString();
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "No Session ID, generating a new one: {0}", id);
			}
			serv.getSessionData().put(IOService.SESSION_ID_KEY, id);
			serv.setXMLNS(XMLNS);
			serv.getSessionData().put(IOService.HOSTNAME_KEY, hostname);
			serv.setDataReceiver(JID.jidInstanceNS(routings.computeRouting(hostname)));

			String streamOpenData = "<?xml version='1.0'?><stream:stream" + " xmlns='" +
					XMLNS + "'" + " xmlns:stream='http://etherx.jabber.org/streams'" + " from='" +
					hostname + "'" + " id='" + id + "'" + " version='1.0' xml:lang='en'>";

			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Writing raw data to the socket: {0}", streamOpenData);
			}
			writeRawData(serv, streamOpenData);
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "DONE");
			}

			Packet streamOpen = Command.STREAM_OPENED.getPacket(serv.getConnectionId(), serv
					.getDataReceiver(), StanzaType.set, this.newPacketId("c2s-"), Command.DataType
					.submit);

			Command.addFieldValue(streamOpen, "session-id", id);
			Command.addFieldValue(streamOpen, "hostname", hostname);
			Command.addFieldValue(streamOpen, "xml:lang", lang);
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Sending a system command to SM: {0}", streamOpen);
			}
			addOutPacketWithTimeout(streamOpen, startedHandler, 45l, TimeUnit.SECONDS);
			log.log(Level.FINER, "DOEN 2");
		} else {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Session ID is: {0}", id);
			}
			writeRawData(serv, "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS +
					"'" + " xmlns:stream='http://etherx.jabber.org/streams'" + " from='" +
					hostname + "'" + " id='" + id + "'" + " version='1.0' xml:lang='en'>");
			addOutPacket(Command.GETFEATURES.getPacket(serv.getConnectionId(), serv
					.getDataReceiver(), StanzaType.get, UUID.randomUUID().toString(), null));
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods
	// ----------------------------------------------------------

	/**
	 * Method description
	 *
	 * @param params
	 *
	 *
	 * @return a value of Map<String,Object>
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		Boolean             r_mode = (Boolean) params.get(getName() + "/" +
				ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY);
		String see_other_host_class = (String) params.get(SeeOtherHostIfc
				.CM_SEE_OTHER_HOST_CLASS_PROPERTY);

		see_other_host_strategy = getSeeOtherHostInstance(see_other_host_class);
		props.put(SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_KEY, see_other_host_class);
		if (see_other_host_strategy != null) {
			see_other_host_strategy.getDefaults(props, params);
		}
		if (r_mode == null) {
			props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY, ROUTING_MODE_PROP_VAL);

			// If the server is configured as connection manager only node then
			// route packets to SM on remote host where is default routing
			// for external component.
			// Otherwise default routing is to SM on localhost
			if (params.get("config-type").equals(GEN_CONFIG_CS) && (params.get(GEN_EXT_COMP) !=
					null)) {
				String[] comp_params = ((String) params.get(GEN_EXT_COMP)).split(",");

				props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY, DEF_SM_NAME + "@" +
						comp_params[1]);
			} else {
				props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY, DEF_SM_NAME + "@" +
						DNSResolver.getDefaultHostname());
			}
		}
		props.put(SOCKET_CLOSE_WAIT_PROP_KEY, SOCKET_CLOSE_WAIT_PROP_DEF);
		props.put(TLS_WANT_CLIENT_AUTH_ENABLED_KEY, TLS_WANT_CLIENT_AUTH_ENABLED_DEF);

		return props;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @return a value of String
	 */
	@Override
	public String getDiscoCategoryType() {
		return "c2s";
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @return a value of String
	 */
	@Override
	public String getDiscoDescription() {
		return "Client connection manager";
	}

	/**
	 * Method description
	 *
	 *
	 * @param see_other_host_class
	 *
	 *
	 *
	 * @return a value of SeeOtherHostIfc
	 */
	public SeeOtherHostIfc getSeeOtherHostInstance(String see_other_host_class) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Configuring see_other_host strategy for: " + see_other_host_class);
		}
		if (see_other_host_class == null) {
			see_other_host_class = SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_DEF_VAL;
		}
		if (see_other_host_class.equals("none")) {
			return null;
		}
		try {
			see_other_host_strategy = (SeeOtherHostIfc) Class.forName(see_other_host_class)
					.newInstance();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not instantiate see_other_host strategy for class: " +
					see_other_host_class, e);
		}

		return see_other_host_strategy;
	}

	//~--- set methods ----------------------------------------------------------

	// ~--- set methods
	// ----------------------------------------------------------

	/**
	 * Method description
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		clientTrustManagerFactory.setProperties(props);
		if (props.get(SOCKET_CLOSE_WAIT_PROP_KEY) != null) {
			socket_close_wait_time = (Long) props.get(SOCKET_CLOSE_WAIT_PROP_KEY);
		}
		processors = XMPPIOProcessorsFactory.updateIOProcessors(this, processors, props);
		if (props.size() == 1) {

			// If props.size() == 1, it means this is a single property update
			// and this component does not support single property change for
			// the rest
			// of it's settings
			return;
		}

		String see_other_host_class = (String) props.get(SeeOtherHostIfc
				.CM_SEE_OTHER_HOST_CLASS_PROP_KEY);

		see_other_host_strategy = getSeeOtherHostInstance(see_other_host_class);
		if (see_other_host_strategy != null) {
			see_other_host_strategy.setProperties(props);
		}

		boolean routing_mode = (Boolean) props.get(ROUTINGS_PROP_KEY + "/" +
				ROUTING_MODE_PROP_KEY);

		routings = new RoutingsContainer(routing_mode);

		int idx = (ROUTINGS_PROP_KEY + "/").length();

		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/") &&!entry.getKey().equals(
					ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY)) {
				routings.addRouting(entry.getKey().substring(idx), (String) entry.getValue());
			}    // end of if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/"))
		}      // end of for ()
		if (props.containsKey(TLS_WANT_CLIENT_AUTH_ENABLED_KEY)) {
			tlsWantClientAuthEnabled = (Boolean) props.get(TLS_WANT_CLIENT_AUTH_ENABLED_KEY);
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param newAddress
	 * @param command_sessionId
	 * @param serv
	 *
	 *
	 *
	 * @return a value of JID
	 */
	protected JID changeDataReceiver(Packet packet, JID newAddress,
			String command_sessionId, XMPPIOService<Object> serv) {
		if (serv != null) {
			String serv_sessionId = (String) serv.getSessionData().get(IOService
					.SESSION_ID_KEY);

			if (serv_sessionId.equals(command_sessionId)) {
				JID old_receiver = serv.getDataReceiver();

				serv.setDataReceiver(newAddress);

				return old_receiver;
			} else {
				log.log(Level.WARNING,
						"Incorrect session ID, ignoring data redirect for: {0}, expected: {1}, received: {2}",
						new Object[] { newAddress,
						serv_sessionId, command_sessionId });
			}
		}

		return null;
	}

	// ~--- methods
	// --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of ReceiverTimeoutHandler
	 */
	protected ReceiverTimeoutHandler newStartedHandler() {
		return new StartedHandler();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of ReceiverTimeoutHandler
	 */
	protected ReceiverTimeoutHandler newStoppedHandler() {
		return new StoppedHandler();
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	protected void processCommand(Packet packet) {
		XMPPIOService<Object> serv = getXMPPIOService(packet);
		Iq                    iqc  = (Iq) packet;

		switch (iqc.getCommand()) {
		case GETFEATURES :
			if (iqc.getType() == StanzaType.result) {
				List<Element> features      = getFeatures(serv);
				Element       elem_features = new Element("stream:features");

				elem_features.addChildren(features);
				elem_features.addChildren(Command.getData(iqc));

				Packet result = Packet.packetInstance(elem_features, null, null);

				// Is it actually needed?? Yes, it is needed, IOService is
				// looked up based on this.
				result.setPacketTo(iqc.getTo());
				writePacketToSocket(result);
			}    // end of if (packet.getType() == StanzaType.get)

			break;

		case USER_LOGIN :
			String jid = Command.getFieldValue(iqc, "user-jid");

			if (jid != null) {
				if (serv != null) {
					BareJID fromJID = null;

					try {
						fromJID = BareJID.bareJIDInstance(jid);
					} catch (TigaseStringprepException ex) {
						log.log(Level.SEVERE, null, ex);
					}
					if ((fromJID != null) && ((see_other_host_strategy != null) &&
							see_other_host_strategy.isEnabled(SeeOtherHostIfc.Phase.LOGIN))) {
						BareJID see_other_host = see_other_host_strategy.findHostForJID(fromJID,
								getDefHostName());

						if ((see_other_host != null) &&!see_other_host.equals(getDefHostName())) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST,
										"Sending redirect for {0} to host {1}, connection {2}.",
										new Object[] { fromJID,
										see_other_host, serv });
							}

							String redirectMessage = "<stream:stream" + " xmlns='" + XMLNS + "'" +
									" xmlns:stream='http://etherx.jabber.org/streams'" +
									" id='tigase-error-tigase'" + " from='" + getDefVHostItem() + "'" +
									" version='1.0' xml:lang='en'>" + see_other_host_strategy
									.getStreamError("urn:ietf:params:xml:ns:xmpp-streams", see_other_host)
									.toString() + "</stream:stream>";

							try {
								SocketThread.removeSocketService(serv);
								serv.writeRawData(redirectMessage);
								serv.processWaitingPackets();
								Thread.sleep(socket_close_wait_time);
								serv.stop();
							} catch (Exception e) {}
						}
					} else {
						serv.setUserJid(jid);
					}
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Missing XMPPIOService for USER_LOGIN command: {0}", iqc);
					}
				}
			} else {
				log.log(Level.WARNING, "Missing user-jid for USER_LOGIN command: {0}", iqc);
			}

			break;

		case STARTZLIB :
			if (serv != null) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Starting zlib compression: {0}", serv);
				}
				try {
					Element compressed   = Command.getData(iqc, "compressed", null);
					Packet  p_compressed = Packet.packetInstance(compressed, null, null);

					// SocketThread readThread = SocketThread.getInstance();
					SocketThread.removeSocketService(serv);

					// writePacketToSocket(serv, p_proceed);
					serv.addPacketToSend(p_compressed);
					serv.processWaitingPackets();
					serv.startZLib(Deflater.BEST_COMPRESSION);

					// serv.call();
					SocketThread.addSocketService(serv);
				} catch (IOException ex) {
					log.log(Level.INFO, "Problem enabling zlib compression on the connection: ",
							ex);
				}
			} else {
				log.log(Level.WARNING, "Can't find sevice for STARTZLIB command: {0}", iqc);
			}

			break;

		case STARTTLS :
			if (serv != null) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Starting TLS for connection: {0}", serv);
				}
				try {

					// Note:
					// If you send <proceed> packet to client you must expect
					// instant response from the client with TLS handshaking
					// data before you will call startTLS() on server side.
					// So the initial handshaking data might be lost as they
					// will be processed in another thread reading data from the
					// socket.
					// That's why below code first removes service from reading
					// threads pool and then sends <proceed> packet and starts
					// TLS.
					Element proceed   = Command.getData(iqc, "proceed", null);
					Packet  p_proceed = Packet.packetInstance(proceed, null, null);

					// SocketThread readThread = SocketThread.getInstance();
					SocketThread.removeSocketService(serv);

					// writePacketToSocket(serv, p_proceed);
					serv.addPacketToSend(p_proceed);
					serv.processWaitingPackets();

					TrustManager[] x = clientTrustManagerFactory.getManager(serv);

					serv.setX509TrustManagers(x);
					serv.startTLS(false, isTlsWantClientAuthEnabled());
					SocketThread.addSocketService(serv);
				} catch (Exception e) {
					log.log(Level.WARNING, "Error starting TLS: {0}", e);
					serv.forceStop();
				}    // end of try-catch
			} else {
				log.log(Level.WARNING, "Can't find sevice for STARTTLS command: {0}", iqc);
			}      // end of else

			break;

		case REDIRECT :
			String command_sessionId = Command.getFieldValue(iqc, "session-id");
			JID    newAddress        = iqc.getFrom();
			JID    old_receiver = changeDataReceiver(iqc, newAddress, command_sessionId, serv);

			if (old_receiver != null) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Redirecting data for sessionId: {0}, to: {1}",
							new Object[] { command_sessionId,
							newAddress });
				}

				Packet response = null;

				response = iqc.commandResult(null);
				Command.addFieldValue(response, "session-id", command_sessionId);
				Command.addFieldValue(response, "action", "activate");
				response.getElement().setAttribute("to", newAddress.toString());
				addOutPacket(response);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Connection for REDIRECT command does not exist, ignoring " + "packet: " +
							"{0}", iqc.toStringSecure());
				}
			}

			break;

		case STREAM_CLOSED :
			break;

		case GETDISCO :
			break;

		case CLOSE :
			if (serv != null) {
				String        streamClose = "</stream:stream>";
				List<Element> err_el = packet.getElement().getChildrenStaticStr(Iq
						.IQ_COMMAND_PATH);
				boolean moreToSend = false;

				if ((err_el != null) && (err_el.size() > 0)) {
					streamClose = "<stream:error>" + err_el.get(0).toString() + "</stream:error>" +
							streamClose;
					moreToSend = true;
				}
				try {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Sending stream close to the client: {0}", streamClose);
					}
					serv.writeRawData(streamClose);
					if (moreToSend) {

						// This is kind of a workaround. serv.stop() is supposed
						// to wait
						// until all data are sent to the client, however, even
						// then there
						// is still a chance, that the connection is closed
						// before data
						// reached the client
						Thread.sleep(socket_close_wait_time);
					}
				} catch (Exception e) {}
				serv.stop();
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE,
							"Attempt to stop non-existen service for packet: {0}, Service already stopped?",
							iqc);
				}
			}    // end of if (serv != null) else

			break;

		case CHECK_USER_CONNECTION :
			if (serv != null) {

				// It's ok, the session has been found, respond with OK.
				addOutPacket(iqc.okResult((String) null, 0));
			} else {

				// Session is no longer active, respond with an error.
				try {
					addOutPacket(Authorization.ITEM_NOT_FOUND.getResponseMessage(iqc,
							"Connection gone.", false));
				} catch (PacketErrorTypeException e) {

					// Hm, error already, ignoring...
					log.log(Level.INFO, "Error packet is not really expected here: {0}", iqc
							.toStringSecure());
				}
			}

			break;

		case STREAM_MOVED :
			if (processors != null) {
				for (XMPPIOProcessor processor : processors) {

					// handled |= processor.processCommand(packet);
					processor.processCommand(serv, packet);
				}
			}

			break;

		default :
			writePacketToSocket(iqc);

			break;
		}    // end of switch (pc.getCommand())
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods
	// ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int[]
	 */
	@Override
	protected int[] getDefPlainPorts() {
		return new int[] { 5222 };
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int[]
	 */
	@Override
	protected int[] getDefSSLPorts() {
		return new int[] { 5223 };
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time for
	 * inactive connection. Let's assume user should send something at least
	 * once every 24 hours....
	 *
	 *  a <code>long</code> value
	 *
	 * @return a value of long
	 */
	@Override
	protected long getMaxInactiveTime() {
		return 24 * HOUR;
	}

	/**
	 * Method description
	 *
	 *
	 * @param def
	 *
	 *
	 *
	 * @return a value of Integer
	 */
	@Override
	protected Integer getMaxQueueSize(int def) {
		return def * 10;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of XMPPIOService<Object>
	 */
	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new XMPPIOService<Object>();
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of boolean
	 */
	@Override
	protected boolean isTlsWantClientAuthEnabled() {
		return tlsWantClientAuthEnabled;
	}

	// ~--- get methods
	// ----------------------------------------------------------
	private List<Element> getFeatures(XMPPIOService service) {
		List<Element> results = new LinkedList<Element>();

		for (XMPPIOProcessor proc : processors) {
			Element[] features = proc.supStreamFeatures(service);

			if (features != null) {
				results.addAll(Arrays.asList(features));
			}    // end of if (features != null)
		}      // end of for ()

		return results;
	}

	private JID getFromAddress(String id) {
		return JID.jidInstanceNS(getName(), getDefHostName().getDomain(), id);
	}

	private XMPPResourceConnection getXMPPSession(Packet p) {
		XMPPIOService<Object> serv = getXMPPIOService(p);

		return (serv == null)
				? null
				: (XMPPResourceConnection) serv.getSessionData().get("xmpp-session");
	}

	//~--- inner classes --------------------------------------------------------

	// ~--- inner classes
	// --------------------------------------------------------
	private class StartedHandler
					implements ReceiverTimeoutHandler {
		/**
		 * Method description
		 *
		 * @param packet
		 * @param response
		 */
		@Override
		public void responseReceived(Packet packet, Packet response) {

			// We are now ready to ask for features....
			addOutPacket(Command.GETFEATURES.getPacket(packet.getFrom(), packet.getTo(),
					StanzaType.get, UUID.randomUUID().toString(), null));
		}

		/**
		 * Method description
		 *
		 * @param packet
		 */
		@Override
		public void timeOutExpired(Packet packet) {

			// If we still haven't received confirmation from the SM then
			// the packet either has been lost or the server is overloaded
			// In either case we disconnect the connection.
			log.log(Level.INFO, "No response within time limit received for a packet: {0}",
					packet.toStringSecure());

			XMPPIOService<Object> serv = getXMPPIOService(packet.getFrom().toString());

			if (serv != null) {
				serv.stop();
			} else {
				log.log(Level.FINE,
						"Attempt to stop non-existen service for packet: {0}, Service already stopped?",
						packet);
			}    // end of if (serv != null) else
		}
	}


	private class StoppedHandler
					implements ReceiverTimeoutHandler {
		/**
		 * Method description
		 *
		 * @param packet
		 * @param response
		 */
		@Override
		public void responseReceived(Packet packet, Packet response) {

			// Great, nothing to worry about.
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Response for stop received...");
			}
		}

		/**
		 * Method description
		 *
		 * @param packet
		 */
		@Override
		public void timeOutExpired(Packet packet) {

			// Ups, doesn't look good, the server is either oveloaded or lost
			// a packet.
			log.log(Level.INFO, "No response within time limit received for a packet: {0}",
					packet.toStringSecure());
			addOutPacketWithTimeout(packet, stoppedHandler, 60l, TimeUnit.SECONDS);
		}
	}
}



// ~ Formatted in Tigase Code Convention on 13/02/20


//~ Formatted in Tigase Code Convention on 13/08/28
