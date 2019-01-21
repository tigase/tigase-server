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
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.jid.BareJID;

import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of <a href='http://xmpp.org/extensions/xep-0012.html'>XEP-0012</a>: Last Activity.
 *
 * @author bmalkow
 */

public abstract class LastActivityAbstract
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc {

	protected static final String XMLNS = "jabber:iq:last";
	public final static String LAST_ACTIVITY_KEY = "LAST_ACTIVITY_KEY";
	public final static String LAST_STATUS_KEY = "LAST_STATUS_KEY";
	public final static String LAST_SHOW_KEY = "LAST_SHOW_KEY";
	public final static String LAST_PRESENCE_KEY = "LAST_PRESENCE_KEY";
	private static final Logger log = Logger.getLogger(LastActivityAbstract.class.getName());
	public final static String[] STATUS_PATH = new String[]{Presence.ELEM_NAME, "status"};
	public final static String[] SHOW_PATH = new String[]{Presence.ELEM_NAME, "show"};

	private SimpleParser parser = SingletonFactory.getParserInstance();

	protected static long getLastActivity(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		String result = repo.getPublicData(requestedJid, XMLNS, LAST_ACTIVITY_KEY, "-1");
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"last-activity for: {0}, result: {1}",
					new String[]{String.valueOf(requestedJid), String.valueOf(result)});
		}

		if (result != null) {
			return Long.parseLong(result);
		} else {
			throw new UserNotFoundException(requestedJid + " doesn't exist");
		}
	}

	public static long getLastActivity(XMPPResourceConnection session, boolean global) {
		Long result = null;

		if (global) {
			result = (Long) session.getCommonSessionData(LAST_ACTIVITY_KEY);
		} else {
			result = (Long) session.getSessionData(LAST_ACTIVITY_KEY);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"last-activity for: {0}, result: {1}",
					new String[]{String.valueOf(session.getjid()), String.valueOf(result)});
		}

		return result == null ? -1 : result;
	}

	protected static long getLastActivity(XMPPResourceConnection session, Packet packet) {
		return getLastActivity(session, (packet.getStanzaTo().getResource() == null ||
				packet.getStanzaTo().getResource().length() == 0));
	}

	protected static String getStatus(XMPPResourceConnection session) {
		return session.getPresence() == null ? null : session.getPresence().getChildCDataStaticStr(STATUS_PATH);
	}

	protected static String getShow(XMPPResourceConnection session) {
		return session.getPresence() == null ? null : session.getPresence().getChildCDataStaticStr(SHOW_PATH);
	}

	protected static String getShow(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		return repo.getPublicData(requestedJid, XMLNS, LAST_SHOW_KEY, null);
	}

	protected static String getType(XMPPResourceConnection session) {
		return session.getPresence() == null ? null : session.getPresence().getAttributeStaticStr("type");
	}

	protected static String getType(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		return repo.getPublicData(requestedJid, XMLNS, LAST_SHOW_KEY, null);
	}


	protected Optional<Element> getPresence(NonAuthUserRepository repo, BareJID requestedJid)
			throws UserNotFoundException {
		final String publicData = repo.getPublicData(requestedJid, XMLNS, LAST_PRESENCE_KEY, null);

		if (publicData != null ) {
			DomBuilderHandler domHandler = new DomBuilderHandler();
			char[] data = publicData.toCharArray();
			parser.parse(domHandler, data, 0, data.length);

			final Queue<Element> parsedElements = domHandler.getParsedElements();
			return parsedElements.peek() != null ? Optional.of(parsedElements.poll()) : Optional.empty();
		} else {
			return Optional.empty();
		}
	}

	protected static String getStatus(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		return repo.getPublicData(requestedJid, XMLNS, LAST_STATUS_KEY, null);
	}

	protected static void persistLastActivity(XMPPResourceConnection session, Element presence) {
		long last = getLastActivity(session, false);
		String status = getStatus(session);

		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(
						"Persiting last:activity of user " + session.getUserName() + " in storage (value=" + last +
								", " + "presence=" + presence + ").");
			}
			session.setPublicData(XMLNS, LAST_ACTIVITY_KEY, String.valueOf(last));
			if (presence != null) {
				session.setPublicData(XMLNS, LAST_PRESENCE_KEY, String.valueOf(presence));
			}
			session.setPublicData(XMLNS, LAST_STATUS_KEY, status);
		} catch (NotAuthorizedException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("session isn't authorized" + session);
			}
		} catch (TigaseDBException e) {
			log.warning("Tigase Db Exception");
		}
	}


	enum ProtectionLevel {
		ALL,
		BUDDIES;
	}
}
