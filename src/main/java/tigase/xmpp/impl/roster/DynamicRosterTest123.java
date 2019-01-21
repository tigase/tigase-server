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
package tigase.xmpp.impl.roster;

import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Artur Hefczyc Created Apr 1, 2011
 */
public class DynamicRosterTest123
		implements DynamicRosterIfc {

	private static final String[] buddy_names = {"test1", "test2", "test3"};
	private static final String[] managers = {"manager1"};

	@Override
	public void setItemExtraData(Element item) {
		// TODO Auto-generated method stub

	}

	@Override
	public Element getItemExtraData(Element item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(Map<String, Object> props) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(String par) {
		// TODO Auto-generated method stub

	}

	@Override
	public JID[] getBuddies(XMPPResourceConnection session) throws NotAuthorizedException {
		String domain = session.getDomain().getVhost().getDomain();
		ArrayList<JID> result = new ArrayList<JID>(buddy_names.length);
		for (String name : buddy_names) {
			if (!name.equals(session.getUserName())) {
				result.add(JID.jidInstanceNS(name, domain, null));
			}
		}
		for (String name : managers) {
			if (!name.equals(session.getUserName())) {
				result.add(JID.jidInstanceNS(name, domain, null));
			}
		}

		return result.toArray(new JID[result.size()]);
	}

	@Override
	public Element getBuddyItem(XMPPResourceConnection session, JID buddy) throws NotAuthorizedException {
		return new Element("item", new Element[]{new Element("group", "test group")},
						   new String[]{"jid", "name", "subscription"},
						   new String[]{buddy.getBareJID().toString(), buddy.getLocalpart(),
										(buddy.toString().contains("manager") ? "from" : "to")});

	}

	@Override
	public List<Element> getRosterItems(XMPPResourceConnection session) throws NotAuthorizedException {
		ArrayList<Element> result = new ArrayList<Element>(buddy_names.length);
		for (JID buddy : getBuddies(session)) {
			result.add(getBuddyItem(session, buddy));
		}
		return result;
	}

}
