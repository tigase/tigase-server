/*
 * LastActivity.java
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
package tigase.xmpp.impl;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.server.Iq;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;
import java.util.logging.Level;

/**
 * Implementation of <a
 * href='http://xmpp.org/extensions/xep-0012.html'>XEP-0012</a>: Last Activity.
 *
 * @author bmalkow
 *
 */
public class LastActivity extends XMPPProcessorAbstract implements XMPPStopListenerIfc {
	private static final String XMLNS = "jabber:iq:last";
	private final static String[] XMLNSS = new String[] { XMLNS, "jabber:client", "jabber:client" };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };
	private static final String[][] ELEMENTS = { Iq.IQ_QUERY_PATH, { Presence.ELEM_NAME }, { Message.ELEM_NAME } };
	private final static String ID = XMLNS;
	private final static String LAST_ACTIVITY_KEY = "LAST_ACTIVITY_KEY";
	private final static String LAST_STATUS_KEY = "LAST_STATUS_KEY";
	private static final Logger log = Logger.getLogger(LastActivity.class.getName());
	private final static String[] STATUS_PATH = new String[] { "presence", "status" };

	private static String getStatus(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		return repo.getPublicData(requestedJid, ID, LAST_STATUS_KEY, null);
	}

	private static long getTime(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		String data = repo.getPublicData(requestedJid, ID, LAST_ACTIVITY_KEY, null);

		if (data == null) {
			return -1;
		}
		try {
			return Long.parseLong(data);
		} catch (Exception e) {
			return -1;
		}
	}

	private static long getTime(XMPPResourceConnection session) {
		final Long last = (Long) session.getSessionData(LAST_ACTIVITY_KEY);

		return (last == null) ? -1 : last.longValue();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws XMPPException {
		if ((packet.getElemName() != "iq") && (session != null)
				&& session.getBareJID().equals(packet.getStanzaFrom().getBareJID())) {
			final long time = System.currentTimeMillis();

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Updating last:activity of user " + session.getUserName() + " to " + time);
			}
			session.putSessionData(LAST_ACTIVITY_KEY, time);
		} else {
			if (packet.getElemName() == "iq")
				super.process(packet, session, repo, results, settings);
		}
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {

		if (packet.getType() == StanzaType.get) {
			long last = getTime(session);

			if (last != -1) {
				Packet resp = packet.okResult((Element) null, 0);
				long result = (System.currentTimeMillis() - last) / 1000;
				Element q = new Element("query", new String[] { "xmlns", "seconds" }, new String[] { "jabber:iq:last",
						"" + result });

				resp.getElement().addChild(q);
				results.offer(resp);
			} else {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", true));
			}
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws PacketErrorTypeException {

		if (packet.getType() == StanzaType.get) {
			try {
				BareJID requestedJid = packet.getStanzaTo().getBareJID();
				final long last = getTime(repo, requestedJid);
				final String status = getStatus(repo, requestedJid);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Get last:activity of offline user " + requestedJid + ". value=" + last + ", status=" + status);
				}
				if (last != -1) {
					long result = (System.currentTimeMillis() - last) / 1000;
					Packet resp = packet.okResult((Element) null, 0);
					Element q = new Element("query", status, new String[] { "xmlns", "seconds" }, new String[] {
							"jabber:iq:last", "" + result });

					resp.getElement().addChild(q);
					results.offer(resp);
				} else {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", true));
				}
			} catch (UserNotFoundException e) {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "User not found", true));
			}
		} else if (packet.getType() == StanzaType.set) {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		} else {
			super.processNullSessionPacket(packet, repo, results, settings);
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
	}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {

		if (packet.getType() == StanzaType.get) {
			long last = getTime(session);

			if (last != -1) {
				long result = (System.currentTimeMillis() - last) / 1000;
				Packet resp = packet.okResult((Element) null, 0);
				Element q = new Element("query", new String[] { "xmlns", "seconds" }, new String[] { "jabber:iq:last",
						"" + result });

				resp.getElement().addChild(q);
				results.offer(resp);
			} else {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", true));
			}
		} else if (packet.getType() == StanzaType.set) {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		} else {
			super.processToUserPacket(packet, session, repo, results, settings);
		}
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {

		if (session != null) {
			long last = getTime(session);

			try {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Persiting last:activity of user " + session.getUserName() + " in storage (value=" + last + ").");
				}
				session.setPublicData(ID, LAST_ACTIVITY_KEY, String.valueOf(last));
				String status = session.getPresence().getChildCDataStaticStr(STATUS_PATH);
				session.setPublicData(ID, LAST_STATUS_KEY, status);
			} catch (NotAuthorizedException e) {
				log.finest("session isn't authorized" + session);
			} catch (TigaseDBException e) {
				log.warning("Tigase Db Exception");
			}
		}
	}

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}
