/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.disco;

import java.util.List;
import tigase.xml.Element;
import tigase.server.ServerComponent;
import tigase.xmpp.JID;

/**
 * Interface XMPPService
 *
 * Objects which implement this interface can respond to "ServiceDiscovery"
 * requests. All such requests are managed by MessageRouter instance.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface XMPPService extends ServerComponent {

	/**
	 * A convenience constant with service discovery info xmlns string.
	 */
	public static final String INFO_XMLNS =
			"http://jabber.org/protocol/disco#info";
	/**
	 * A convenience constant with service discovery items xmlns string.
	 */
	public static final String ITEMS_XMLNS =
			"http://jabber.org/protocol/disco#items";
	/**
	 * A convenience constant with all stream features for service discovery.
	 */
	public static final String[] DEF_FEATURES = {INFO_XMLNS, ITEMS_XMLNS};
	/**
	 * A convenience constant with component features related to ad-hoc commands.
	 */
	public static final String[] CMD_FEATURES =
 {
		"http://jabber.org/protocol/commands", "jabber:x:data"};

	/**
	 * Returns service discovery info for the component. If the jid is null then this is
	 * info for the top level request. SM may return disco info on the top level. Other
	 * components should not.
	 *
	 * @param node is service discovery node for which the request is made. Is
	 * normally null for the component top level request.
	 * @param jid is the jid to which the request has been made.
	 * @param from is the request sender address. Some service discovery information
	 * is only meant for administrators.
	 * @return returns an XML Element with service discovery data.
	 */
	Element getDiscoInfo(String node, JID jid, JID from);

	/**
	 * Returns service discovery items for the component. If the JID is null then this is
	 * request for the top level request. SM may return disco items on the top level, other
	 * components should just return it's top level service discovery item for null node.
	 * @param node is a service discovery node for which the request has been made.
	 * @param jid is the jid to which the request has been made.
	 * @param from is the request sender address. Some service discovery information
	 * is only meant for administrators.
	 * @return a list of service discovery items for this component or the component
	 * itself disco item for the top level request.
	 */
	List<Element> getDiscoItems(String node, JID jid, JID from);

	/**
	 * Returns features for top level disco info
	 *
	 * @param from a request sender address. Some service disco elements are meant
	 * to be available only to system administrarors. The component is responsible to
	 * check whether the sender is the component administrator and return results
	 * appropriate.
	 * @return a list of elements with service discovery features.
	 */
	List<Element> getDiscoFeatures(JID from);

}
