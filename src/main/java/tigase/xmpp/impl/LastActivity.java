/**
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
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.sys.TigaseRuntime;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.*;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.LastActivity.ID;
import static tigase.xmpp.impl.LastActivityAbstract.XMLNS;

/**
 * Implementation of <a href='http://xmpp.org/extensions/xep-0012.html'>XEP-0012</a>: Last Activity.
 *
 * @author bmalkow
 */

@Id(ID)
@DiscoFeatures({XMLNS})
@HandleStanzaTypes(StanzaType.get)
@Handles(@Handle(path = {Iq.ELEM_NAME, Iq.QUERY_NAME}, xmlns = XMLNS))
@Bean(name = LastActivity.ID, parent = LastActivityMarker.class, active = false)
public class LastActivity
		extends XMPPProcessorAbstract {

	protected final static String ID = XMLNS;
	private static final Logger log = Logger.getLogger(LastActivity.class.getName());
	private final static String PROTECTION_LEVEL_KEY = "protection-level";
	private final static EnumSet<SubscriptionType> inTypes = EnumSet.of(SubscriptionType.both, SubscriptionType.from);
	private final static EnumSet<SubscriptionType> outTypes = EnumSet.of(SubscriptionType.both, SubscriptionType.to);
	@ConfigField(desc = "Protection level", alias = PROTECTION_LEVEL_KEY)
	private ProtectionLevel protectionLevel = ProtectionLevel.ALL;

	protected static RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}

	private static Packet preventFromINFLoop(Packet packet) {
		packet.setPacketTo(null);
		return packet;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}

		super.process(packet, session, repo, results, settings);
	}

	@Override
	public void processFromUserPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
									  NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Process From user packet: {0}", packet);
		}

		switch (packet.getType()) {
			case get:
				switch (protectionLevel) {
					case BUDDIES:
						if (getRosterUtil() == null) {
							log.warning("Roster factory returned null");
							results.offer(preventFromINFLoop(
									Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
							break;
						}
						try {
							RosterElement element = getRosterUtil().getRosterElement(session, packet.getStanzaTo());
							if (element == null || !outTypes.contains(element.getSubscription())) {
								results.offer(preventFromINFLoop(
										Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
							} else {
								super.processFromUserPacket(connectionId, packet, session, repo, results, settings);
							}
						} catch (NotAuthorizedException | TigaseDBException e) {
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, e.getMessage(), e);
							}
							results.offer(preventFromINFLoop(
									Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
						}
						break;
					case ALL:
						super.processFromUserPacket(connectionId, packet, session, repo, results, settings);
						break;
				}
				break;
			case error:
			case result:
				super.processFromUserPacket(connectionId, packet, session, repo, results, settings);
				break;
			default:
				results.offer(preventFromINFLoop(
						Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", false)));
				break;
		}
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing from user to server packet: {0}", packet);
		}

		packet.processedBy(ID);
		if (packet.getPermissions() == Permissions.ADMIN) {
			handleLastActivityRequest(packet, TigaseRuntime.getTigaseRuntime().getUptime(), null, results);
		} else {
			results.offer(preventFromINFLoop(Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing null session packet: {0}", packet);
		}

		packet.processedBy(ID);
		switch (packet.getType()) {
			case get:
				BareJID requestedJid = packet.getStanzaTo().getBareJID();
				try {
					final long last = LastActivityAbstract.getLastActivity(repo, requestedJid);
					final String status = LastActivityAbstract.getStatus(repo, requestedJid);
					handleLastActivityRequest(packet, last, status, results);
				} catch (UserNotFoundException e) {
					results.offer(preventFromINFLoop(Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
				}
			case error:
			case result:
				super.processNullSessionPacket(packet, repo, results, settings);
				break;
			default:
				results.offer(preventFromINFLoop(
						Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", false)));
				break;
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing server session packet: {0}", packet);
		}
		packet.processedBy(ID);
		handleLastActivityRequest(packet, TigaseRuntime.getTigaseRuntime().getUptime(), null, results);
	}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
									Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing to user packet: {0}", packet);
		}

		packet.processedBy(ID);
		switch (packet.getType()) {
			case get:
				long last = LastActivityAbstract.getLastActivity(session, packet);
				String status = LastActivityAbstract.getStatus(session);

				switch (protectionLevel) {
					case BUDDIES:
						if (getRosterUtil() == null) {
							log.warning("Roster factory returned null");
							results.offer(preventFromINFLoop(
									Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
							break;
						}
						try {
							RosterElement element = getRosterUtil().getRosterElement(session, packet.getStanzaFrom());
							if (element == null || !inTypes.contains(element.getSubscription())) {
								results.offer(preventFromINFLoop(
										Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
							} else {
								handleLastActivityRequest(packet, last, status, results);
							}
						} catch (NotAuthorizedException | TigaseDBException e) {
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, e.getMessage(), e);
							}
							results.offer(preventFromINFLoop(
									Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
						}
						break;
					case ALL:
						handleLastActivityRequest(packet, last, status, results);
						break;
				}
				break;
			case error:
			case result:
				super.processToUserPacket(packet, session, repo, results, settings);
				break;
			default:
				results.offer(preventFromINFLoop(
						Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", false)));
				break;
		}
	}

	private void handleLastActivityRequest(Packet packet, long last, String status, Queue<Packet> results)
			throws PacketErrorTypeException {
		if (last >= 0) {
			long result = (System.currentTimeMillis() - last) / 1000;
			Packet resp = packet.okResult((Element) null, 0);
			Element q;
			if (status == null) {
				q = new Element(Iq.QUERY_NAME, new String[]{Packet.XMLNS_ATT, "seconds"},
								new String[]{XMLNS, "" + result});
			} else {
				q = new Element(Iq.QUERY_NAME, status, new String[]{Packet.XMLNS_ATT, "seconds"},
								new String[]{XMLNS, "" + result});
			}
			resp.getElement().addChild(q);
			results.offer(resp);
		} else {
			results.offer(preventFromINFLoop(
					Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", false)));
		}
	}

	enum ProtectionLevel {
		ALL,
		BUDDIES;
	}
}
