/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.disco;

import java.util.List;
import tigase.xml.Element;
import tigase.server.ServerComponent;

/**
 * Interface XMPPService
 *
 * Objects of which inherit this interface can respond to "ServiceDiscovery"
 * requests. All such requests are managed by XMPPServiceCollector object.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface XMPPService extends ServerComponent {

	public static final String INFO_XMLNS =
		"http://jabber.org/protocol/disco#info";
	public static final String ITEMS_XMLNS =
		"http://jabber.org/protocol/disco#items";

	public static final String[] DEF_FEATURES =	{ INFO_XMLNS, ITEMS_XMLNS };

	public static final String[] CMD_FEATURES =
	{"http://jabber.org/protocol/commands", "jabber:x:data"};

	Element getDiscoInfo(String node, String jid);

	List<Element> getDiscoItems(String node, String jid);

	/**
	 * Returns features for top level disco info
	 *
	 * @return
	 */
	List<Element> getDiscoFeatures();

}
