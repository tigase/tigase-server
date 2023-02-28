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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.PacketDefaultHandler;
import tigase.server.xmppsession.SessionManager;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "message-delivery-logic", parent = SessionManager.class, active = true, exportable = true)
public class MessageDeliveryLogic implements MessageDeliveryProviderIfc {

	private static final Logger log = Logger.getLogger(MessageDeliveryLogic.class.getCanonicalName());

	protected static final String ELEM_NAME = tigase.server.Message.ELEM_NAME;
	protected static final String XMLNS = "jabber:client";
	private static final String DELIVERY_RULES_KEY = "delivery-rules";
	private static final String SILENTLY_IGNORE_ERROR_KEY = "silently-ignore-message";
	public static Predicate<XMPPResourceConnection> VIABLE_FOR_MESSAGE_DELIVERY = (conn) -> conn.getPriority() >= 0;
	@ConfigField(desc = "Message delivery rules", alias = DELIVERY_RULES_KEY)
	private MessageDeliveryRules deliveryRules = MessageDeliveryRules.inteligent;
	@ConfigField(desc = "Silently ignore errors", alias = SILENTLY_IGNORE_ERROR_KEY)
	private boolean silentlyIgnoreError = false;
	@Inject(nullAllowed = true)
	private SessionManager.MessageArchive messageArchive;
	@Inject
	private VHostManagerIfc vHostManager;

	private PacketDefaultHandler packetDefaultHandler = new PacketDefaultHandler();

	public void handleDelivery(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

		// For performance reasons it is better to do the check
		// before calling logging method.
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}, for session: {1}", new Object[]{packet, session});
		}

		if (packet.getElemName() == Iq.ELEM_NAME) {
			packetDefaultHandler.process(packet, session, repo, results);
			return;
		}

		// You may want to skip processing completely if the user is offline.
		if (session == null) {
			processOfflineUser(packet, results);
			return;
		}    // end of if (session == null)
		try {

			// Remember to cut the resource part off before comparing JIDs
			BareJID id = (packet.getStanzaTo() != null) ? packet.getStanzaTo().getBareJID() : null;

			// Checking if this is a packet TO the owner of the session
			if (session.isUserId(id)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Message ''to'' this user, packet: {0}, for session: {1}",
							new Object[]{packet, session});
				}

				if (packet.getStanzaFrom() != null && session.isUserId(packet.getStanzaFrom().getBareJID())) {
					JID connectionId = session.getConnectionId();
					if (connectionId.equals(packet.getPacketFrom())) {
						Packet forward = packet.copyElementOnly();
						forward.setStableId(packet.getStableId());
						results.offer(forward);
						// this would cause message packet to be stored in offline storage and will not
						// send recipient-unavailable error but it will behave the same as a message to
						// unavailable resources from other sessions or servers
						return;
					}
				}

				// Yes this is message to 'this' client
				List<XMPPResourceConnection> conns = new ArrayList<XMPPResourceConnection>(5);

				// This is where and how we set the address of the component
				// which should rceive the result packet for the final delivery
				// to the end-user. In most cases this is a c2s or Bosh component
				// which keep the user connection.
				String resource = packet.getStanzaTo().getResource();

				if (resource == null) {

					// If the message is sent to BareJID then the message is delivered to
					// all resources
					conns.addAll(getConnectionsForMessageDelivery(session));
				} else {

					// Otherwise only to the given resource or sent back as error.
					XMPPResourceConnection con = session.getParentSession() == null
												 ? null
												 : session.getParentSession().getResourceForResource(resource);

					if (con != null) {
						conns.add(con);
					}
				}

				// MessageCarbons: message cloned to all resources? why? it should be copied only
				// to resources with non negative priority!!

				if (conns.size() > 0) {
					for (XMPPResourceConnection con : conns) {
						Packet result = packet.copyElementOnly();

						result.setStableId(packet.getStableId());
						result.setPacketTo(con.getConnectionId());


						// In most cases this might be skept, however if there is a
						// problem during packet delivery an error might be sent back
						result.setPacketFrom(packet.getTo());

						if (messageArchive != null) {
							messageArchive.addStableId(result, session);
						}

						// Don't forget to add the packet to the results queue or it
						// will be lost.
						results.offer(result);
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Delivering message, packet: {0}, to session: {1}",
									new Object[]{packet, con});
						}
					}
				} else {
					// if there are no user connections we should process packet
					// the same as with missing session (i.e. should be stored if
					// has type 'chat'
					processOfflineUser(packet, results);
				}

				return;
			}    // end of else

			// Remember to cut the resource part off before comparing JIDs
			id = (packet.getStanzaFrom() != null) ? packet.getStanzaFrom().getBareJID() : null;

			// Checking if this is maybe packet FROM the client
			if (session.isUserId(id)) {

				// This is a packet FROM this client, the simplest action is
				// to forward it to is't destination:
				// Simple clone the XML element and....
				// ... putting it to results queue is enough
				results.offer(packet.copyElementOnly());

				return;
			}

			// Can we really reach this place here?
			// Yes, some packets don't even have from or to address.
			// The best example is IQ packet which is usually a request to
			// the server for some data. Such packets may not have any addresses
			// And they usually require more complex processing
			// This is how you check whether this is a packet FROM the user
			// who is owner of the session:
			JID jid = packet.getFrom();

			// This test is in most cases equal to checking getElemFrom()
			if (session.getConnectionId().equals(jid)) {

				// Do some packet specific processing here, but we are dealing
				// with messages here which normally need just forwarding
				Element el_result = packet.getElement().clone();

				// If we are here it means FROM address was missing from the
				// packet, it is a place to set it here:
				el_result.setAttribute("from", session.getJID().toString());

				Packet result = Packet.packetInstance(el_result, session.getJID(), packet.getStanzaTo());

				// ... putting it to results queue is enough
				results.offer(result);
			}
		} catch (NotAuthorizedException e) {
			log.log(Level.FINE, "NotAuthorizedException for packet: " + packet + " for session: " + session, e);
			results.offer(
					Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.", true));
		}    // end of try-catch
	}
	
	/**
	 * Method returns list of XMPPResourceConnections to which message should be delivered for session passes as
	 * parameter if message was sent to bare JID
	 */
	public List<XMPPResourceConnection> getConnectionsForMessageDelivery(XMPPResourceConnection session)
			throws NotAuthorizedException {
		List<XMPPResourceConnection> conns = new ArrayList<XMPPResourceConnection>();
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			if (VIABLE_FOR_MESSAGE_DELIVERY.test(conn)) {
				conns.add(conn);
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Out of: {0} total connections, only: {1} have non-negative priority",
					new Object[]{session.getActiveSessions().size(), conns.size()});
		}

		return conns;
	}

	/**
	 * Method returns list of JIDs to which message should be delivered for session passes as parameter if message was
	 * sent to bare JID
	 */
	public Set<JID> getJIDsForMessageDelivery(XMPPResourceConnection session) throws NotAuthorizedException {
		Set<JID> jids = new HashSet<JID>();
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			if (VIABLE_FOR_MESSAGE_DELIVERY.test(conn)) {
				jids.add(conn.getJID());
			}
		}
		return jids;
	}

	/**
	 * Method returns true if there is at least one XMPPResourceConnection which is allowed to receive message for
	 * XMPPResourceConnection
	 */
	public boolean hasConnectionForMessageDelivery(XMPPResourceConnection session) {
		try {
			for (XMPPResourceConnection conn : session.getActiveSessions()) {
				if (VIABLE_FOR_MESSAGE_DELIVERY.test(conn)) {
					return true;
				}
			}
		} catch (NotAuthorizedException ex) {
			// should not happen, end even if it happend then we should return false
		}
		return false;
	}

	public boolean preProcessFilter(Packet packet, XMPPResourceConnection session) {
		final StanzaType type = packet.getType();
		// only handle messages within local domains; errors to external domains and components (i.e. mix, pubsub) will be delivered
		if (type == StanzaType.error && vHostManager.isLocalDomain(packet.getStanzaTo().getDomain())) {
			if (packet.getStanzaTo().getResource() == null) {
				// 1) https://xmpp.org/rfcs/rfc6121.html#rules-localpart-barejid
				//* 8.5.2.1.  Available or Connected Resources > 8.5.2.1.1.  Message
				//> For a message stanza of type "error", the server MUST silently ignore the message.
				//
				//* 8.5.2.2.  No Available or Connected Resources > 8.5.2.2.1.  Message
				//> For a message stanza of type "headline" or "error", the server MUST silently ignore the message.
				//
				return true;
			} else {
				//
				//* 8.5.3.2.  No Resource Matches > 8.5.3.2.1.  Message
				//> For a message stanza of type "error", the server MUST silently ignore the stanza.
				if (session != null && session.getjid() != null && session.getjid().equals(packet.getStanzaTo())) {
					// 2) https://xmpp.org/rfcs/rfc6121.html#rules-localpart-fulljid
					//* 8.5.3.1.  Resource Matches
					//> For a message stanza, the server MUST deliver the stanza to the resource.
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	private void processOfflineUser(Packet packet, Queue<Packet> results) throws PacketErrorTypeException {
		if (packet.getStanzaTo() != null && packet.getStanzaTo().getResource() != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing message to offline user, packet: {0}, deliveryRules: {1}",
						new Object[]{packet, deliveryRules});
			}

			if (deliveryRules != MessageDeliveryRules.strict) {
				StanzaType type = packet.getType();
				if (type == null) {
					type = StanzaType.normal;
				}
				switch (type) {
					case chat:
						// try to deliver this message to all available resources so we should
						// treat it as a stanza with bare "to" attribute
						Packet result = packet.copyElementOnly();
						result.setStableId(packet.getStableId());
						result.initVars(packet.getStanzaFrom(), packet.getStanzaTo().copyWithoutResource());
						results.offer(result);
						break;

					case error:
						// for error packet we should ignore stanza according to RFC 6121
						break;

					case headline:
					case groupchat:
					case normal:
					default:
						// for each of this types RFC 6121 recomends silent ignoring of stanza
						// or to return error recipient-unavailable - we will send error as
						// droping packet without response may not be a good idea
						if (!silentlyIgnoreError) {
							results.offer(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet,
																								 "The recipient is no longer available.",
																								 true));
						}
				}
			} else {
				if (!silentlyIgnoreError) {
					results.offer(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet,
																						 "The recipient is no longer available.",
																						 true));
				}
			}
		}
	}

	private static enum MessageDeliveryRules {
		strict,
		inteligent
	}

}
