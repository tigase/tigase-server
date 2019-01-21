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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Nov 28, 2008 10:27:55 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DynamicRosterTest
		implements DynamicRosterIfc {

	private static Logger log = Logger.getLogger(DynamicRosterTest.class.getName());

	private Map<String, Element> memStorage = new LinkedHashMap<String, Element>();

	@Override
	public JID[] getBuddies(XMPPResourceConnection session) throws NotAuthorizedException {
		return new JID[]{JID.jidInstanceNS("dynrost@test-d")};
	}

	@Override
	public Element getBuddyItem(XMPPResourceConnection session, JID buddy) throws NotAuthorizedException {
		if ("dynrost@test-d".equals(buddy.getBareJID().toString())) {
			return getBuddy();
		} else {
			return null;
		}
	}

	@Override
	public Element getItemExtraData(Element item) {
		String jid = item.getAttributeStaticStr("jid");
		Element result = memStorage.get(jid);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Retrieving item: {0}, for jid={1}", new Object[]{result, jid});
		}

		return result;
	}

	@Override
	public List<Element> getRosterItems(XMPPResourceConnection session) throws NotAuthorizedException {
		return new ArrayList<Element>(Arrays.asList(getBuddy()));
	}

	@Override
	public void init(Map<String, Object> props) {
	}

	@Override
	public void init(String par) {
	}

	@Override
	public void setItemExtraData(Element item) {
		String jid = item.getAttributeStaticStr("jid");

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Storing item: {0}, for jid={1}", new Object[]{item, jid});
		}
		memStorage.put(jid, item);
	}

	private Element getBuddy() {
		return new Element("item", new Element[]{new Element("group", "test group")},
						   new String[]{"jid", "name", "subscription"},
						   new String[]{"dynrost@test-d", "dynrost", "both"});
	}
}

