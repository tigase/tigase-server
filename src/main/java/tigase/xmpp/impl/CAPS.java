/*
 * CAPS.java
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Presence;

import tigase.xml.Element;

import tigase.xmpp.*;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.Configurable;

/**
 *
 * @author andrzej
 */
public class CAPS
				extends XMPPProcessor
				implements XMPPProcessorIfc {
	private static final String[][]     ELEMENTS = {
		{ Presence.ELEM_NAME }, Iq.IQ_QUERY_PATH
	};
	private static final String         ID       = "caps";
	private static final Logger         log = Logger.getLogger(CAPS.class
			.getCanonicalName());
	private static final String         XMLNS_DISCO =
			"http://jabber.org/protocol/disco#info";
	private static final String[]       XMLNSS   = { "jabber:client", XMLNS_DISCO };
	private static final RosterAbstract roster_impl = RosterFactory.getRosterImplementation(
			true);

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws XMPPException {
		if ((session != null) && session.isAuthorized()) {
			try {
				if (packet.getElemName() == Presence.ELEM_NAME) {
					JID                to = packet.getStanzaTo();
					Map<JID, String[]> resources = (Map<JID, String[]>) session
							.getCommonSessionData(ID);

					if (resources == null) {
						resources = new ConcurrentHashMap<JID, String[]>();
						session.putCommonSessionData(ID, resources);
					}
					if ((packet.getType() == null) || (packet.getType() == StanzaType.available)) {
						Element c = packet.getElement().getChild("c");

						if (c == null) {
							return;
						}

						String[] capsNodes    = PresenceCapabilitiesManager.processPresence(c);
						String[] capsNodesOld = resources.put(packet.getStanzaFrom(), capsNodes);

						if ((capsNodesOld == null) ||!Arrays.equals(capsNodes, capsNodesOld)) {

							// we shoud send pep notifications now
							if ((to != null) &&!roster_impl.isSubscribedFrom(session, packet
									.getStanzaFrom())) {

								// subscription is not sufficient
								return;
							}
							if (to == null) {
								to = session.getJID();
							}

							// checking for need of disco#info
							PresenceCapabilitiesManager.prepareCapsQueries(JID.jidInstanceNS(to
									.getDomain()), packet.getStanzaFrom(), capsNodes, results);
							if (!session.isUserId(to.getBareJID())) {
								return;
							}
							PresenceCapabilitiesManager.handlePresence(to, packet.getStanzaFrom(),
									capsNodes, results);
						}
					} else if ((packet.getType() == StanzaType.unavailable) || (packet.getType() ==
							StanzaType.error)) {
						resources.remove(packet.getStanzaFrom());
					}
				} else if ((packet.getElemName() == Iq.ELEM_NAME) && ((packet.getType() ==
						StanzaType.error) || (packet.getType() == StanzaType.result))) {
						String nick = packet.getStanzaTo().getLocalpart();

						if ((nick == null) || Configurable.DEF_SM_NAME.equals(nick)) {
							PresenceCapabilitiesManager.processCapsQueryResponse(packet);
						}
				}
			} catch (NotAuthorizedException ex) {
				Logger.getLogger(CAPS.class.getName()).log(Level.SEVERE, null, ex);
			} catch (TigaseDBException ex) {
				Logger.getLogger(CAPS.class.getName()).log(Level.SEVERE, null, ex);
			}
		} else if (((session == null) || session.isServerSession()) && (packet
				.getElemName() == Iq.ELEM_NAME) && ((packet.getType() == StanzaType.error) ||
				(packet.getType() == StanzaType.result))) {
			String nick = packet.getStanzaTo().getLocalpart();

			if ((nick == null) || Configurable.DEF_SM_NAME.equals(nick)) {
				PresenceCapabilitiesManager.processCapsQueryResponse(packet);
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param feature
	 *
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static Set<JID> getJidsWithFeature(XMPPResourceConnection session,
			String feature) {
		Set<JID>           jids      = new HashSet<JID>();
		Map<JID, String[]> resources = (Map<JID, String[]>) session.getCommonSessionData(ID);

		if (resources != null) {
			List<JID> available = new ArrayList<JID>(resources.keySet());

			for (JID jid : available) {
				String[] capsNodes = resources.get(jid);

				if (capsNodes == null) {
					continue;
				}
				for (String capsNode : capsNodes) {
					String[] features = PresenceCapabilitiesManager.getNodeFeatures(capsNode);

					if (features == null) {
						continue;
					}
					if (Arrays.binarySearch(features, feature) >= 0) {
						jids.add(jid);

						break;
					}
				}
			}
		}

		return jids;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/12
