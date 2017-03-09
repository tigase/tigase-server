/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp.impl.roster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.PresenceAbstract;
import tigase.xmpp.impl.PresenceState;
//import tigase.xmpp.impl.Roster;
//import tigase.xmpp.impl.Presence;


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

	@Override
	public void init(Map<String, Object> props) {}

	@Override
	public void init(String par) {}

	@SuppressWarnings({"unchecked"})
	@Override
	public JID[] getBuddies(XMPPResourceConnection session)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			Set<JID> direct_presences =
			    (Set<JID>)session.getSessionData(PresenceState.DIRECT_PRESENCE);
			if (direct_presences != null) {
				JID[] result = new JID[direct_presences.size()];
				int i = 0;
				for (JID peer: direct_presences) {
					result[i++] = peer;
				}
				return result;
			}
		}
		return null;
	}

	@Override
	public Element getBuddyItem(XMPPResourceConnection session, JID buddy)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			JID[] anon_peers = getBuddies(session);
			if (anon_peers != null) {
				for (JID peer: anon_peers) {
					if (peer.getBareJID().equals(buddy.getBareJID())) {
						Element item = new Element("item", new Element[] {
								new Element("group", "Anonymous peers")},
							new String[] {"jid", "subscription", "name"},
							new String[] {peer.toString(), "both", peer.getLocalpart()});
						return item;
					}
				}
			}
		}
		return null;
	}

	@Override
	public List<Element> getRosterItems(XMPPResourceConnection session)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			JID[] anon_peers = getBuddies(session);
			if (anon_peers != null) {
				ArrayList<Element> al = new ArrayList<Element>();
				for (JID peer: anon_peers) {
					Element item = new Element("item", new Element[] {
							new Element("group", "Anonymous peers")},
						new String[] {"jid", "subscription", "name"},
						new String[] {peer.toString(), "both", peer.getLocalpart()});
					al.add(item);
				}
				return al;
			}
		}
		return null;
	}

	@Override
	public void setItemExtraData(Element item) { }

	@Override
	public Element getItemExtraData(Element item) { return null; }

}
