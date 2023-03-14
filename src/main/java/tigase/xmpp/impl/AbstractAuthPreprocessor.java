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

import tigase.auth.BruteForceLockerBean;
import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.xmpp.*;
import tigase.xmpp.jid.BareJID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class which should be extended by any authorization processor as it implements preprocessor feature which is
 * responsible for stopping not allowed packets from not yet authorized client connections.
 *
 * @author andrzej
 */
public abstract class AbstractAuthPreprocessor
		extends XMPPProcessor
		implements XMPPPreprocessorIfc {

	private static final Logger log = Logger.getLogger(AbstractAuthPreprocessor.class.getCanonicalName());

	private static final String[] AUTH_ONLY_ELEMS = {"message", "presence"};

	@Inject(nullAllowed = true)
	protected BruteForceLockerBean bruteForceLocker;

	@ConfigField(desc = "Matchers selecting allowed packets for unauthorized session", alias = "allow-unauthorized")
	private ElementMatcher[] allowMatchers = new ElementMatcher[]{
			new ElementMatcher(new String[0], "urn:ietf:params:xml:ns:xmpp-tls", true),
			new ElementMatcher(new String[0], "http://jabber.org/protocol/compress", true),
			new ElementMatcher(new String[0], "urn:ietf:params:xml:ns:xmpp-sasl", true),
			new ElementMatcher(new String[0], "urn:xmpp:sasl:2", true),
			new ElementMatcher(Iq.IQ_QUERY_PATH, JabberIqRegister.ID, true),
			new ElementMatcher(Iq.IQ_QUERY_PATH, JabberIqAuth.ID, true)
	};

	protected boolean isBruteForceLockerEnabled(XMPPResourceConnection session) {
		return bruteForceLocker != null && bruteForceLocker.isEnabled(session);
	}

	protected boolean isLoginAllowedByBruteForceLocker(XMPPResourceConnection session, String clientIp, BareJID jid) {
		return isBruteForceLockerEnabled(session) && !bruteForceLocker.isLoginAllowed(session, clientIp, jid);
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
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
																								  " can send any message or presence packet.",
																						  true));
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, "Packet received before the session has been authenticated." +
												"Session details: connectionId=" + "{0}, sessionId={1}, packet={2}",
										new Object[]{session.getConnectionId(), session.getSessionId(),
													 packet.toStringSecure()});
							}

							return true;
						}
					}
					if (!isPacketAllowed(packet)) {
						results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
																					  "You must authenticate session first, before you" +
																							  " can send any message or presence packet.",
																					  true));
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "Packet received before the session has been authenticated." +
											"Session details: connectionId=" + "{0}, sessionId={1}, packet={2}",
									new Object[]{session.getConnectionId(), session.getSessionId(),
												 packet.toStringSecure()});
						}

						return true;
					}
					return false;
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
		}    // end of try-catch

		return false;
	}

	public String[] getAllowMatchers() {
		String[] result = new String[allowMatchers.length];
		for (int i = 0; i < allowMatchers.length; i++) {
			result[i] = allowMatchers[i].toString();
		}
		return result;
	}

	public void setAllowMatchers(String[] matcherStrs) {
		List<ElementMatcher> matchers = new ArrayList<>();
		for (String matcherStr : matcherStrs) {
			ElementMatcher matcher = ElementMatcher.create(matcherStr);
			if (matcher != null) {
				matchers.add(matcher);
			}
		}
		allowMatchers = matchers.toArray(new ElementMatcher[0]);
	}

	protected boolean isPacketAllowed(Packet packet) {
		for (ElementMatcher matcher : allowMatchers) {
			if (matcher.matches(packet)) {
				return matcher.getValue();
			}
		}
		return false;
	}

	public void setBruteForceLocker(BruteForceLockerBean bruteForceLocker) {
		log.log(Level.CONFIG, bruteForceLocker != null ? "BruteForceLocker enabled" : "BruteForceLocker disabled" );
		this.bruteForceLocker = bruteForceLocker;
	}

}
