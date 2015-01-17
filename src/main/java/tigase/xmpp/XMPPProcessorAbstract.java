/*
 * XMPPProcessorAbstract.java
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

import tigase.db.NonAuthUserRepository;

import tigase.server.Message;
import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;

/**
 * Utility abstract class detecting what kind of packet is processed. Releases developers from
 * checking whether the packet is addressed to the user of the session or from the user
 * of the sesion or packet to the server itself.
 *
 * Created: Mar 1, 2010 10:21:29 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class XMPPProcessorAbstract
				extends AnnotatedXMPPProcessor
				implements XMPPProcessorIfc {
	private static final Logger log = Logger.getLogger(XMPPProcessorAbstract.class
			.getName());

	//~--- methods --------------------------------------------------------------

	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws XMPPException {
		try {
			if (session == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
							new Object[] { "processNullSessionPacket",
							packet, session });
				}
				processNullSessionPacket(packet, repo, results, settings);

				return;
			}
			if (session.isServerSession()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
							new Object[] { "processServerSessionPacket",
							packet, session });
				}
				processServerSessionPacket(packet, session, repo, results, settings);

				return;
			}
			try {
				JID connectionId = session.getConnectionId();

				if (connectionId.equals(packet.getPacketFrom())) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
								new Object[] { "processFromUserPacket",
								packet, session });
					}
					processFromUserPacket(connectionId, packet, session, repo, results, settings);

					return;
				}
			} catch (NoConnectionIdException ex) {
				log.log(Level.WARNING,
						"This should not happen, this is not a server session and " +
						"still connection id is not set: " + session + ", packet: " + packet, ex);
			}
			try {
				if (session.isUserId(packet.getStanzaTo().getBareJID())) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
								new Object[] { "processToUserPacket",
								packet, session });
					}
					processToUserPacket(packet, session, repo, results, settings);
				}
			} catch (NotAuthorizedException ex) {
				log.log(Level.WARNING,
						"Packet to a user session which is not yet authenticated: " + session +
						", packet: " + packet);
			}
		} catch (PacketErrorTypeException ex) {
			log.info("Attempt to send an error response to the error packet: " + packet +
					", session: " + session + ", plugin: " + id());
		}
	}

	/**
	 * The method is called when a packet is sent from the user, owner of the session
	 * somewhere else to other XMPP entity (other user on the server, other user on a
	 * different server, different server, component, transport, etc....).
	 * The default implementation just forwards the packet doing nothing else, which is good
	 * enough in most cases. You can overwrite the method to change the default behaviour.
	 *
	 *
	 * @param connectionId is a <code>JID</code> instance with the session connection ID.
	 * @param session is a <code>XMPPResourceConnection</code> instance with all the sending
	 * user session data.
	 * @param packet is a <code>Packet</code> sent by the user.
	 * @param repo is a <code>NonAuthUserRepository</code> instance giving access to a part
	 * of the user repository which is accessible regardless the session is authenticated or not.
	 * @param results is a packets <code>Queue</code> with all the processing results from
	 * the plugin.
	 * @param settings is a <code>Map</code> with all the configuration settings passed to the
	 * plugin.
	 * @throws PacketErrorTypeException on attempt to send an error response to the error
	 * packet.
	 */
	public void processFromUserOutPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException {
		results.offer(packet.copyElementOnly());
	}

	/**
	 * The method is called for all packets sent by the user, owner of this connection/session.
	 * Please note, it is likely that a user sends a packet addressed to his own server, like
	 * get server version information. In such a case only this method is called. Such a packet
	 * is not later passed to the <code>processServerSessionPacket(...)</code>.
	 * Note, the default implementation checks whether the packet is addressed to the server
	 * or is being sent to another XMPP entity. In the first case it calls
	 * <code>processFromUserToServerPacket(...)</code> method, otherwise it calls
	 * <code>processFromUserOutPacket</code>. You can overwrite the method to change
	 * the default behavior or implement the two called methods to handle each case
	 * separately.
	 *
	 *
	 * @param connectionId is a <code>JID</code> instance with the session connection ID.
	 * @param session is a <code>XMPPResourceConnection</code> instance with all the sending
	 * user session data.
	 * @param packet is a <code>Packet</code> sent by the user.
	 * @param repo is a <code>NonAuthUserRepository</code> instance giving access to a part
	 * of the user repository which is accessible regardless the session is authenticated or not.
	 * @param results is a packets <code>Queue</code> with all the processing results from
	 * the plugin.
	 * @param settings is a <code>Map</code> with all the configuration settings passed to the
	 * plugin.
	 * @throws PacketErrorTypeException on attempt to send an error response to the error
	 * packet.
	 */
	public void processFromUserPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException {
		try {

			// Check whether the packet is addressed to the server or some other, XMPP entity
			if ((packet.getStanzaTo() == null) || session.isLocalDomain(packet.getStanzaTo()
					.toString(), false) || session.isUserId(packet.getStanzaTo().getBareJID())) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
							new Object[] { "processFromUserToServerPacket",
							packet, session });
				}
				processFromUserToServerPacket(connectionId, packet, session, repo, results,
						settings);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Calling method: {0}, for packet={1}, for session={2}",
							new Object[] { "processFromUserOutPacket",
							packet, session });
				}
				processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
			}
		} catch (NotAuthorizedException ex) {
			log.info("Session not yet authorized to send ping requests: " + session +
					", packet: " + packet);
		}
	}

	/**
	 * The method is called when a packet is send from the user who is owner of the session
	 * to the local server (ping, roster management, privacy lists, etc...). There is no default
	 * implementation for the method.
	 *
	 *
	 * @param connectionId is a <code>JID</code> instance with the session connection ID.
	 * @param session is a <code>XMPPResourceConnection</code> instance with all the sending
	 * user session data.
	 * @param packet is a <code>Packet</code> sent by the user.
	 * @param repo is a <code>NonAuthUserRepository</code> instance giving access to a part
	 * of the user repository which is accessible regardless the session is authenticated or not.
	 * @param results is a packets <code>Queue</code> with all the processing results from
	 * the plugin.
	 * @param settings is a <code>Map</code> with all the configuration settings passed to the
	 * plugin.
	 * @throws PacketErrorTypeException on attempt to send an error response to the error
	 * packet.
	 */
	public abstract void processFromUserToServerPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException;

	/**
	 * The method is called for packets received by the server for which no user session
	 * is found - neither sender session or receiver session. The default implementation does
	 * nothing, just ignores such a packet. To change the default behaviour the method
	 * has to be overwritten.
	 *
	 *
	 * @param packet is a <code>Packet</code> received by the server.
	 * @param repo is a <code>NonAuthUserRepository</code> instance giving access to a part
	 * of the user repository which is accessible regardless the session is authenticated or not.
	 * @param results is a packets <code>Queue</code> with all the processing results from
	 * the plugin.
	 * @param settings is a <code>Map</code> with all the configuration settings passed to the
	 * plugin.
	 * @throws PacketErrorTypeException on attempt to send an error response to the error
	 * packet.
	 */
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {

		// Do nothing, which is a correct thing to do in most cases.
	}

	/**
	 * The method is called when a packet addressed to the server domain is received. Please
	 * note, if a local user sends a packet to the server, the packet is handled by the
	 * <code>processFromUserPacket(...)</code> method. This method is not called for such
	 * packets.
	 *
	 *
	 * @param session is a <code>XMPPResourceConnection</code> instance with all the server
	 * session data.
	 * @param packet is a <code>Packet</code> received by the server and addressed to the
	 * server - the server virtual domain name.
	 * @param repo is a <code>NonAuthUserRepository</code> instance giving access to a part
	 * of the user repository which is accessible regardless the session is authenticated or not.
	 * @param results is a packets <code>Queue</code> with all the processing results from
	 * the plugin.
	 * @param settings is a <code>Map</code> with all the configuration settings passed to the
	 * plugin.
	 * @throws PacketErrorTypeException on attempt to send an error response to the error
	 * packet.
	 */
	public abstract void processServerSessionPacket(Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings)
					throws PacketErrorTypeException;

	/**
	 * Method is called for all the packets sent TO the user - owner of the session. The default
	 * implementation just forwards the packet to the user connection. To change the default
	 * behavior the method has to be overwritten.
	 *
	 *
	 * @param session is a <code>XMPPResourceConnection</code> instance with all the receiving
	 * user session data.
	 * @param packet is a <code>Packet</code> received by the server and addressed to the
	 * server - the server virtual domain name.
	 * @param repo is a <code>NonAuthUserRepository</code> instance giving access to a part
	 * of the user repository which is accessible regardless the session is authenticated or not.
	 * @param results is a packets <code>Queue</code> with all the processing results from
	 * the plugin.
	 * @param settings is a <code>Map</code> with all the configuration settings passed to the
	 * plugin.
	 * @throws PacketErrorTypeException on attempt to send an error response to the error
	 * packet.
	 */
	public void processToUserPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {
		try {

			List<XMPPResourceConnection> conns    = new ArrayList<XMPPResourceConnection>(5);
			String                       resource = packet.getStanzaTo().getResource();

			if ((resource == null) && (packet.getElemName() == Message.ELEM_NAME)) {
				conns.addAll(session.getActiveSessions());
			} else {
				XMPPResourceConnection con = session.getParentSession().getResourceForResource(
						resource);

				if (con != null) {
					conns.add(con);
				}
			}
			if (conns.size() > 0) {
				for (XMPPResourceConnection con : conns) {
					Packet result = packet.copyElementOnly();

					result.setPacketTo(con.getConnectionId());
					result.setPacketFrom(packet.getTo());
					results.offer(result);
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Delivering packet: {0}, to session: {1}",
								new Object[] { packet,
								con });
					}
				}
			} else {
				Packet result = Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet,
						"The recipient is no longer available.", true);

				result.setPacketFrom(null);
				result.setPacketTo(null);
				results.offer(result);
			}
		} catch (NotAuthorizedException e) {
			log.warning("NotAuthorizedException for packet: " + packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (NoConnectionIdException ex) {
			log.log(Level.WARNING,
					"This should not happen, this is not a server session and " +
					"still connection id is not set: " + session + ", packet: " + packet, ex);
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/05/24
