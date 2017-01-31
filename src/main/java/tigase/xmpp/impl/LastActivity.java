/*
 * LastActivity.java
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

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;

import tigase.server.Iq;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.server.Presence;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFactory;

import tigase.sys.TigaseRuntime;
import tigase.xml.Element;

import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.LastActivity.ID;
import static tigase.xmpp.impl.LastActivity.XMLNS;

/**
 * Implementation of <a
 * href='http://xmpp.org/extensions/xep-0012.html'>XEP-0012</a>: Last Activity.
 *
 * @author bmalkow
 *
 */

@Id(ID)
@DiscoFeatures({ XMLNS })
@Handles({ @Handle(path = { Iq.ELEM_NAME, Iq.QUERY_NAME }, xmlns = XMLNS), @Handle(path = { Presence.ELEM_NAME }, xmlns = Packet.CLIENT_XMLNS),
		@Handle(path = { Message.ELEM_NAME }, xmlns = Packet.CLIENT_XMLNS) })
public class LastActivity extends XMPPProcessorAbstract implements XMPPStopListenerIfc {

	enum ProtectionLevel {
		ALL, BUDDIES;
	}

	protected static final String XMLNS = "jabber:iq:last";
	protected final static String ID = XMLNS;
	private final static String LAST_ACTIVITY_KEY = "LAST_ACTIVITY_KEY";
	private final static String LAST_STATUS_KEY = "LAST_STATUS_KEY";
	private static final Logger log = Logger.getLogger(LastActivity.class.getName());
	private final static String PROTECTION_LEVEL_KEY = "protection-level";
	private final static String[] STATUS_PATH = new String[] { Presence.ELEM_NAME, "status" };
	private final static EnumSet<SubscriptionType> inTypes = EnumSet.of(SubscriptionType.both, SubscriptionType.from);
	private final static EnumSet<SubscriptionType> outTypes = EnumSet.of(SubscriptionType.both, SubscriptionType.to);

	private ProtectionLevel protectionLevel = ProtectionLevel.ALL;

	private static long getLastActivity(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		String result = repo.getPublicData(requestedJid, ID, LAST_ACTIVITY_KEY, "-1");
		if (result != null)
			return Long.parseLong(result);
		else
			throw new UserNotFoundException(requestedJid + " doesn't exist");
	}

	private static long getLastActivity(XMPPResourceConnection session, boolean global) {
		Long res = null;

		if (global)
			res = (Long) session.getCommonSessionData(LAST_ACTIVITY_KEY);
		else
			res = (Long) session.getSessionData(LAST_ACTIVITY_KEY);
		return res == null ? -1 : res;
	}

	private static long getLastActivity(XMPPResourceConnection session, Packet packet) {
		return getLastActivity(session, (packet.getStanzaTo().getResource() == null || packet.getStanzaTo().getResource().length() == 0));
	}

	protected static RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}

	private static String getStatus(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		return repo.getPublicData(requestedJid, ID, LAST_STATUS_KEY, null);
	}

	private static Packet preventFromINFLoop(Packet packet) {
		packet.setPacketTo(null);
		return packet;
	}

	private static void setLastActivity(XMPPResourceConnection session, Long last) {
		session.putCommonSessionData(LAST_ACTIVITY_KEY, last);
		session.putSessionData(LAST_ACTIVITY_KEY, last);
	}

	private String getStatus(XMPPResourceConnection session) {
		return session.getPresence() == null ? null : session.getPresence().getChildCDataStaticStr(STATUS_PATH);
	}

	private void handleLastActivityRequest(Packet packet, long last, String status, Queue<Packet> results) throws PacketErrorTypeException {
		if (last >= 0) {
			long result = (System.currentTimeMillis() - last) / 1000;
			Packet resp = packet.okResult((Element) null, 0);
			Element q;
			if (status == null) {
				q = new Element(Iq.QUERY_NAME, new String[] { Packet.XMLNS_ATT, "seconds" }, new String[] { XMLNS, "" + result });
			} else {
				q = new Element(Iq.QUERY_NAME, status, new String[] { Packet.XMLNS_ATT, "seconds" }, new String[] { XMLNS, "" + result });
			}
			resp.getElement().addChild(q);
			results.offer(resp);
		} else {
			results.offer(preventFromINFLoop(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", false)));
		}
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);
		if (settings.containsKey(PROTECTION_LEVEL_KEY))
			protectionLevel = ProtectionLevel.valueOf((String) settings.get(PROTECTION_LEVEL_KEY));
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, "Processing packet: {0}", packet);

		if (packet.getElemName() == Iq.ELEM_NAME) {
			super.process(packet, session, repo, results, settings);
		} else {
			if ((session != null && (packet.getStanzaFrom() != null) && session.isAuthorized() && session.getBareJID().equals(packet.getStanzaFrom().getBareJID()))) {
				final long time = System.currentTimeMillis();

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Updating last:activity of user " + session.getUserName() + " to " + time);
				}

				setLastActivity(session, time);
			}
		}
	}

	@Override
	public void processFromUserPacket(JID connectionId, Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Process From user packet: {0}", packet);
		}

		switch (packet.getType()) {
		case get:
			switch (protectionLevel) {
			case BUDDIES:
				if (getRosterUtil() == null) {
					log.warning("Roster factory returned null");
					results.offer(preventFromINFLoop(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
					break;
				}
				try {
					RosterElement element = getRosterUtil().getRosterElement(session, packet.getStanzaTo());
					if (element == null || !outTypes.contains(element.getSubscription())) {
						results.offer(preventFromINFLoop(Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
					} else {
						super.processFromUserPacket(connectionId, packet, session, repo, results, settings);
					}
				} catch (NotAuthorizedException | TigaseDBException e) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, e.getMessage(), e);
					}
					results.offer(preventFromINFLoop(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
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
			results.offer(preventFromINFLoop(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", false)));
			break;
		}
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
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
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing null session packet: {0}", packet);
		}

		packet.processedBy(ID);
		switch (packet.getType()) {
		case get:
			BareJID requestedJid = packet.getStanzaTo().getBareJID();
			try {
				final long last = getLastActivity(repo, requestedJid);
				final String status = getStatus(repo, requestedJid);
				handleLastActivityRequest(packet, last, status, results);
			} catch (UserNotFoundException e) {
				results.offer(preventFromINFLoop(Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
			}
		case error:
		case result:
			super.processNullSessionPacket(packet, repo, results, settings);
			break;
		default:
			results.offer(preventFromINFLoop(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", false)));
			break;
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing server session packet: {0}", packet);
		}
		packet.processedBy(ID);
		handleLastActivityRequest(packet, TigaseRuntime.getTigaseRuntime().getUptime(), null, results);
	}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing to user packet: {0}", packet);
		}

		packet.processedBy(ID);
		switch (packet.getType()) {
		case get:
			long last = getLastActivity(session, packet);
			String status = getStatus(session);

			switch (protectionLevel) {
			case BUDDIES:
				if (getRosterUtil() == null) {
					log.warning("Roster factory returned null");
					results.offer(preventFromINFLoop(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
					break;
				}
				try {
					RosterElement element = getRosterUtil().getRosterElement(session, packet.getStanzaFrom());
					if (element == null || !inTypes.contains(element.getSubscription())) {
						results.offer(preventFromINFLoop(Authorization.FORBIDDEN.getResponseMessage(packet, null, false)));
					} else {
						handleLastActivityRequest(packet, last, status, results);
					}
				} catch (NotAuthorizedException | TigaseDBException e) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, e.getMessage(), e);
					}
					results.offer(preventFromINFLoop(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, false)));
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
			results.offer(preventFromINFLoop(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", false)));
			break;
		}
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {
		if (session != null && session.isAuthorized()) {
			long last = getLastActivity(session, false);
			String status = getStatus(session);

			try {
				if (log.isLoggable(Level.FINEST))
					log.finest("Persiting last:activity of user " + session.getUserName() + " in storage (value=" + last + ", " + "status=" + status
							+ ").");
				session.setPublicData(ID, LAST_ACTIVITY_KEY, String.valueOf(last));
				session.setPublicData(ID, LAST_STATUS_KEY, status);
			} catch (NotAuthorizedException e) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("session isn't authorized" + session);
				}
			} catch (TigaseDBException e) {
				log.warning("Tigase Db Exception");
			}
		}
	}
}
