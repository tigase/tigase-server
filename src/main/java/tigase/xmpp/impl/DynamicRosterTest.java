/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Created: Nov 28, 2008 10:27:55 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DynamicRosterTest implements DynamicRosterIfc {

  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.DynamicRosterTest");

	private Map<String, Element> memStorage = new LinkedHashMap<String, Element>();

	public void setItemExtraData(Element item) {
		String jid = item.getAttribute("jid");
		log.finest("Storing item: " + item + ", for jid=" + jid);
		memStorage.put(jid, item);
	}

	public Element getItemExtraData(Element item) {
		String jid = item.getAttribute("jid");
		Element result = memStorage.get(jid);
		log.finest("Retrieving item: " + result + ", for jid=" + jid);
		return result;
	}

	public void init(Map<String, Object> props) {	}

	public void init(String par) {}

	public String[] getBuddies(XMPPResourceConnection session)
					throws NotAuthorizedException { return null; }

	public Element getBuddyItem(XMPPResourceConnection session, String buddy)
					throws NotAuthorizedException { return null; }

	public List<Element> getRosterItems(XMPPResourceConnection session)
					throws NotAuthorizedException { return null; }

}
