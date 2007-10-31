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

import java.util.Map;
import java.util.List;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;

/**
 * Describe interface DynamicRosterIfc here.
 *
 *
 * Created: Mon Oct 29 08:52:22 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface DynamicRosterIfc {

	void init(Map<String, Object> props);

	void init(String par);

	String[] getBuddies(XMPPResourceConnection session)
		throws NotAuthorizedException;

	Element getBuddyItem(XMPPResourceConnection session, String buddy)
		throws NotAuthorizedException;

	List<Element> getRosterItems(XMPPResourceConnection session)
		throws NotAuthorizedException;

}
