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
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.StanzaSourceChecker;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "addressing-sanitizer", parent = SessionManager.class, active = true)
public class AddressingSanitizer
		extends XMPPProcessor
		implements XMPPPreprocessorIfc {

	private static final Logger log = Logger.getLogger(AddressingSanitizer.class.getName());
	
	private static final String[] COMPRESS_PATH = {"compress"};

	@Inject
	private StanzaSourceChecker stanzaSourceChecker;

	@Override
	public String id() {
		return "addressing-sanitizer";
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
		if (packet.getServerAuthorisedStanzaFrom().isPresent()) {
			// Correct user JID is already available in the packet so let's use it and short-circuit further processing.
			sanitizePacket(packet, packet.getServerAuthorisedStanzaFrom().get());
			return false;
		}
		if (session == null && stanzaSourceChecker.isPacketFromConnectionManager(packet)) {
			// rationale: packets coming from clients connections and arriving without existing session are most
			// likely send after the session has been already closed and if we don't have correct JID
			// to stamp (neither from packet itself nor from the session that doesn't exists)
			return true;
		}
		if ((session == null) || session.isServerSession() || !session.isAuthorized() ||
				C2SDeliveryErrorProcessor.isDeliveryError(packet)) {
			return false;
		}
		
		try {
			if (session.getConnectionId().equals(packet.getPacketFrom())) {
				// After authentication we require resource binding packet and
				// nothing else:
				// actually according to XEP-0170:
				// http://xmpp.org/extensions/xep-0170.html
				// stream compression might occur between authentication and resource
				// binding
				if (session.isResourceSet() ||
						packet.isXMLNSStaticStr(Iq.IQ_BIND_PATH, "urn:ietf:params:xml:ns:xmpp-bind") ||
						packet.isXMLNSStaticStr(COMPRESS_PATH, "http://jabber.org/protocol/compress")) {
					JID from_jid = session.getJID();

					if (from_jid != null) {

						// http://xmpp.org/rfcs/rfc6120.html#stanzas-attributes-from
						if (packet.getElemName() == tigase.server.Presence.ELEM_NAME &&
								StanzaType.getSubsTypes().contains(packet.getType()) &&
								(packet.getStanzaFrom() == null ||
										!from_jid.getBareJID().equals(packet.getStanzaFrom().getBareJID()) ||
										packet.getStanzaFrom().getResource() != null)) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Setting correct from attribute: {0}", from_jid);
							}
							sanitizePacket(packet, from_jid.copyWithoutResource());
						} else if ((packet.getStanzaFrom() == null) ||
								((packet.getElemName() == tigase.server.Presence.ELEM_NAME &&
										!StanzaType.getSubsTypes().contains(packet.getType()) ||
										packet.getElemName() != tigase.server.Presence.ELEM_NAME) &&
										!from_jid.equals(packet.getStanzaFrom()))) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Setting correct from attribute: {0}", from_jid);
							}
							sanitizePacket(packet, from_jid);
						} else {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST,
										"Skipping setting correct from attribute: {0}, is already correct.",
										packet.getStanzaFrom());
							}
						 	sanitizePacket(packet, packet.getStanzaFrom());
						}
					} else {
						log.log(Level.WARNING, "Session is authenticated but session.getJid() is empty: {0}",
								packet.toStringSecure());
					}
				} else {

					// We do not accept anything without resource binding....
					results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
																				  "You must bind the resource first: " +
																						  "http://www.xmpp.org/rfcs/rfc3920.html#bind",
																				  true));
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Session details: JID={0}, connectionId={1}, sessionId={2}",
								new Object[]{session.getjid(), session.getConnectionId(), session.getSessionId()});
					}

					return true;
				}
			}
		} catch (PacketErrorTypeException e) {

			// Ignore this packet
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Ignoring packet with an error to non-existen user session: {0}",
						packet.toStringSecure());
			}
		} catch (Exception e) {
			log.log(Level.FINEST, "Packet preprocessing exception: ", e);

			return false;
		}
		
		return false;
	}

	protected void sanitizePacket(Packet packet, JID stanzaFrom) {
		if (Message.ELEM_NAME == packet.getElemName() && packet.getStanzaTo() == null && stanzaFrom != null) {
			packet.initVars(stanzaFrom, stanzaFrom.copyWithoutResource());
		} else if (!stanzaFrom.equals(packet.getStanzaFrom())) {
			packet.initVars(stanzaFrom, packet.getStanzaTo());
		}
	}
	
}
