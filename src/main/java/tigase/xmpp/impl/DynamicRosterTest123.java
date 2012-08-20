/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.xmpp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

/**
 * @author Artur Hefczyc Created Apr 1, 2011
 */
public class DynamicRosterTest123 implements DynamicRosterIfc {

	private static final String[] buddy_names = { "test1", "test2", "test3" };

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.xmpp.impl.DynamicRosterIfc#setItemExtraData(tigase.xml.Element)
	 */
	@Override
	public void setItemExtraData(Element item) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.xmpp.impl.DynamicRosterIfc#getItemExtraData(tigase.xml.Element)
	 */
	@Override
	public Element getItemExtraData(Element item) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.xmpp.impl.DynamicRosterIfc#init(java.util.Map)
	 */
	@Override
	public void init(Map<String, Object> props) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.xmpp.impl.DynamicRosterIfc#init(java.lang.String)
	 */
	@Override
	public void init(String par) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * tigase.xmpp.impl.DynamicRosterIfc#getBuddies(tigase.xmpp.XMPPResourceConnection
	 * )
	 */
	@Override
	public JID[] getBuddies(XMPPResourceConnection session) throws NotAuthorizedException {
		String domain = session.getDomain().getVhost().getDomain();
		ArrayList<JID> result = new ArrayList<JID>(buddy_names.length);
		for (String name : buddy_names) {
			if (!name.equals(session.getUserName())) {
				result.add(JID.jidInstanceNS(name, domain, null));
			}
		}
		return result.toArray(new JID[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.xmpp.impl.DynamicRosterIfc#getBuddyItem(tigase.xmpp.
	 * XMPPResourceConnection, tigase.xmpp.JID)
	 */
	@Override
	public Element getBuddyItem(XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException {
		return new Element("item", new Element[] { new Element("group", "test group") },
				new String[] { "jid", "name", "subscription" }, new String[] {
						buddy.getBareJID().toString(), buddy.getLocalpart(), "both" });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.xmpp.impl.DynamicRosterIfc#getRosterItems(tigase.xmpp.
	 * XMPPResourceConnection)
	 */
	@Override
	public List<Element> getRosterItems(XMPPResourceConnection session)
			throws NotAuthorizedException {
		ArrayList<Element> result = new ArrayList<Element>(buddy_names.length);
		for (JID buddy : getBuddies(session)) {
				result.add(getBuddyItem(session, buddy));
		}
		return result;
	}

}
