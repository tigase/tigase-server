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
package tigase.xmpp.impl.push;

import tigase.component.exceptions.RepositoryException;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.impl.roster.RosterFlat;
import tigase.xmpp.jid.BareJID;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.push.IgnoreFromUnknownFilter.ID;

@Bean(name = ID, parent = PushNotifications.class, active = true)
public class IgnoreFromUnknownFilter
		implements PushNotificationsFilter {

	public static final String XMLNS = "tigase:push:ignore-unknown:0";
	
	private static final Logger log = Logger.getLogger(IgnoreFromUnknownFilter.class.getCanonicalName());

	public static final String ID = "ignore-from-unknown";

	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[]{"var"}, new String[]{XMLNS}) };

	protected final RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);

	@Inject
	private UserRepository userRepository;

	@Override
	public Element[] getDiscoFeatures() {
		return DISCO_FEATURES;
	}

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		if (enableEl.getChild("ignore-unknown", XMLNS) != null) {
			settingsEl.addAttribute(ID, "true");
		}
	}

	@Override
	public boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session,
												Element pushServiceSettings, Packet packet) {
		if (Boolean.valueOf(pushServiceSettings.getAttributeStaticStr(ID)) && packet.getType() != StanzaType.groupchat) {
			return isInRoster(packet, session);
		}
		return true;
	}

	protected boolean isInRoster(Packet packet, XMPPResourceConnection session) {
		try {
			if (session != null && session.isAuthorized()) {
				RosterElement rosterElement = roster_util.getRosterElement(session, packet.getStanzaFrom());
				if (rosterElement == null) {
					return false;
				}
				return rosterElement.getSubscription() != RosterAbstract.SubscriptionType.none_pending_in;
			}

			Map<BareJID, RosterElement> roster = getRoster(packet.getStanzaTo().getBareJID());

			if (roster != null) {
				RosterElement rosterElement = roster.get(packet.getStanzaFrom().getBareJID());
				if (rosterElement == null) {
					return false;
				}
				return rosterElement.getSubscription() != RosterAbstract.SubscriptionType.none_pending_in;
			}

			return false;
		} catch (RepositoryException | NotAuthorizedException ex) {
			log.log(Level.WARNING, "Could not retrieve roster for user " + packet.getStanzaTo(), ex);
			return false;
		}
	}

	protected Map<BareJID, RosterElement> getRoster(BareJID jid) throws TigaseDBException {
		String tmp = userRepository.getData(jid, "roster");
		Map<BareJID, RosterElement> roster = new HashMap<BareJID, RosterElement>();
		if (tmp != null) {
			RosterFlat.parseRosterUtil(tmp, roster, null);
		}
		return roster;
	}
}
