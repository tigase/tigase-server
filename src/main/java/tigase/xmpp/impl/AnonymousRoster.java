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

import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

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

	public String[] getBuddies(XMPPResourceConnection session)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			return session.getAnonymousPeers();
		}
		return null;
	}

	public Element getBuddyItem(XMPPResourceConnection session, String buddy)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			String[] anon_peers = session.getAnonymousPeers();
			if (anon_peers != null) {
				String peer = JIDUtils.getNodeID(buddy);
				if (Arrays.binarySearch(anon_peers, peer) >= 0) {
					Element item = new Element("item", new Element[] {
							new Element("group", "Anonymous peers")},
						new String[] {"jid", "subscription", "name"},
						new String[] {peer, "both", JIDUtils.getNodeNick(peer)});
					return item;
				}
			}
		}
		return null;
	}

	public List<Element> getRosterItems(XMPPResourceConnection session)
		throws NotAuthorizedException {
		if (session.isAnonymous()) {
			String[] anon_peers = session.getAnonymousPeers();
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
