/*
 * AbstractAuthPreprocessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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

package tigase.xmpp.impl;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Abstract class which should be extended by any authorization processor as it
 * implements preprocessor feature which is responsible for stopping not allowed 
 * packets from not yet authorized client connections.
 * 
 * @author andrzej
 */
public abstract class AbstractAuthPreprocessor 
				extends XMPPProcessor 
				implements XMPPPreprocessorIfc {
	
	private static final Logger log = Logger.getLogger(AbstractAuthPreprocessor.class.getCanonicalName());
	
	private static final String[] AUTH_ONLY_ELEMS = { "message", "presence" };
	
	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		if ((session == null) || session.isServerSession()) {
			return false;
		}    // end of if (session == null)
		try {
			
			// For all messages coming from the owner of this account set
			// proper 'from' attribute. This is actually needed for the case
			// when the user sends a message to himself.
			if (session.getConnectionId().equals(packet.getPacketFrom())) {
				if (!session.isAuthorized()) {

					// We allow only certain packets here...
					// For now it is simpler to disallow all messages and presences
					// packets, the rest should be bounced back anyway
					for (String elem : AUTH_ONLY_ELEMS) {
						if (packet.getElemName() == elem) {
							results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
									"You must authenticate session first, before you" +
									" can send any message or presence packet.", true));
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE,
										"Packet received before the session has been authenticated." +
										"Session details: connectionId=" + "{0}, sessionId={1}, packet={2}",
										new Object[] { session.getConnectionId(),
										session.getSessionId(), packet.toStringSecure() });
							}

							return true;
						}
					}
					return false;
				}

			}
		} catch (PacketErrorTypeException e) {

			// Ignore this packet
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Ignoring packet with an error to non-existen user session: {0}", packet
						.toStringSecure());
			}
		} catch (Exception e) {
			log.log(Level.FINEST, "Packet preprocessing exception: ", e);

			return false;
		}    // end of try-catch
	
		return false;
	}
	
}
