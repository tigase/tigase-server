/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp.impl;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.Roster;
import tigase.xmpp.impl.Presence;


/**
 * Describe class AnonymousRoster here.
 *
 *
 * Created: Tue Apr 22 21:41:46 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AnonymousRoster implements DynamicRosterIfc {

	public void init(Map<String, Object> props) {}

	public void init(String par) {}

	@SuppressWarnings({"unchecked"})
	public String[] getBuddies(XMPPResourceConnection session)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			Set<String> direct_presences =
			    (Set<String>)session.getSessionData(Presence.DIRECT_PRESENCE);
			if (direct_presences != null) {
				String[] result = new String[direct_presences.size()];
				int i = 0;
				for (String peer: direct_presences) {
					result[i++] = JIDUtils.getNodeID(peer);
				}
				return result;
			}
		}
		return null;
	}

	public Element getBuddyItem(XMPPResourceConnection session, String buddy)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			String[] anon_peers = getBuddies(session);
			if (anon_peers != null) {
				for (String peer: anon_peers) {
					if (peer.equals(JIDUtils.getNodeID(buddy))) {
						Element item = new Element("item", new Element[] {
								new Element("group", "Anonymous peers")},
							new String[] {"jid", "subscription", "name"},
							new String[] {peer, "both", JIDUtils.getNodeNick(peer)});
						return item;
					}
				}
			}
		}
		return null;
	}

	public List<Element> getRosterItems(XMPPResourceConnection session)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			String[] anon_peers = getBuddies(session);
			if (anon_peers != null) {
				ArrayList<Element> al = new ArrayList<Element>();
				for (String peer: anon_peers) {
					Element item = new Element("item", new Element[] {
							new Element("group", "Anonymous peers")},
						new String[] {"jid", "subscription", "name"},
						new String[] {peer, "both", JIDUtils.getNodeNick(peer)});
					al.add(item);
				}
				return al;
			}
		}
		return null;
	}

}
