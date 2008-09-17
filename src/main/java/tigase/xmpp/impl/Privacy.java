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
package tigase.xmpp.impl;

import java.util.List;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.db.TigaseDBException;

/**
 * Class defining data structure for privacy lists.
 * Sample data storage:
 * <node name="privacy">
 *  <map>
 *   <entry value="private" type="String" key="default"/>
 *  </map>
 *  List name:
 *  <node name="private">
 *   <map/>
 *    Item order:
 *    <node name="1">
 *     <map>
 *      <entry value="jid" type="String" key="type"/>
 *      <entry value="user%40domain.com/res" type="String" key="value"/>
 *      <entry value="deny" type="String" key="action"/>
 *      <entry type="String[]" key="stanzas">
 *       <item value="message"/>
 *       <item value="iq"/>
 *      </entry>
 *     </map>
 *   </node>
 * </node>
 *
 *
 * Created: Mon Oct  9 20:50:09 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Privacy {

	/**
   * Private logger for class instancess.
   */
  private static Logger log =	Logger.getLogger("tigase.xmpp.impl.Roster");

	protected static final String PRIVACY = "privacy";
	protected static final String LIST = "list";
	protected static final String ITEM = "item";
	protected static final String NAME = "name";
	protected static final String ORDER = "order";
	protected static final String TYPE = "type";
	protected static final String VALUE = "value";
	protected static final String ACTION = "action";
	protected static final String STANZAS = "stanzas";
	protected static final String DEFAULT = "default-list";
	protected static final String ACTIVE = "active-list";


	public static String[] getLists(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
    return session.getDataGroups(PRIVACY);
	}

	public static String listNode(final String list) {
		return PRIVACY + "/" + list;
	}

	public static void setDefaultList(XMPPResourceConnection session,
		Element list) throws NotAuthorizedException, TigaseDBException {
		if (list.getAttribute(NAME) != null) {
			session.setData(PRIVACY, DEFAULT, list.getAttribute(NAME));
		} else {
			session.removeData(PRIVACY, DEFAULT);
		}
	}

	public static String getDefaultList(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		return session.getData(PRIVACY, DEFAULT, null);
	}

	public static void setActiveList(XMPPResourceConnection session, String lName)
    throws NotAuthorizedException, TigaseDBException {
		if (lName != null) {
			Element list = getList(session, lName);
			session.putSessionData(ACTIVE, list);
		} else {
			session.putSessionData(ACTIVE, null);
		}
	}

	public static Element getActiveList(XMPPResourceConnection session)
    throws NotAuthorizedException {
		return (Element)session.getSessionData(ACTIVE);
	}

	public static String getActiveListName(XMPPResourceConnection session)
    throws NotAuthorizedException {
		Element list = getActiveList(session);
		if (list != null) {
			return list.getAttribute(NAME);
		} else {
			return null;
		} // end of if (list != null) else
	}

	public static void addList(XMPPResourceConnection session,
		Element list) throws NotAuthorizedException, TigaseDBException {

		String lNode = listNode(list.getAttribute(NAME));

		// Always remove this list as it is either removed or replaced
		// by new one. To make sure there are no old data left, let's
		// remove it here.
		session.removeDataGroup(lNode);

		if (list.getChildren() != null && list.getChildren().size() > 0) {
			for (Element item: list.getChildren()) {
				String iNode = lNode + "/" + item.getAttribute(ORDER);
				if (item.getAttribute(TYPE) != null) {
					session.setData(iNode, TYPE, item.getAttribute(TYPE));
				} // end of if (item.getAttribute(TYPE) != null)
				if (item.getAttribute(VALUE) != null) {
					session.setData(iNode, VALUE, item.getAttribute(VALUE));
				} // end of if (item.getAttribute(VALUE) != null)
				session.setData(iNode, ACTION, item.getAttribute(ACTION));
				List<Element> stanzas_list = item.getChildren();
				if (stanzas_list != null && stanzas_list.size() > 0) {
					String[] stanzas = new String[stanzas_list.size()];
					int cnt = -1;
					for (Element stanza: stanzas_list) {
						stanzas[++cnt] = stanza.getName();
					} // end of for (Element stanza: stanzas_list)
					session.setDataList(iNode, STANZAS, stanzas);
				} // end of if (stanzas_list != null && stanzas_list.size() > 0)
			} // end of for (Element item: list.getChildren())
		}
	}

	public static Element getList(XMPPResourceConnection session,
		String list) throws NotAuthorizedException, TigaseDBException {
		String lNode = listNode(list);
		String[] items = session.getDataGroups(lNode);
		if (items != null) {
			Element eList = new Element(LIST,
				new String[] {NAME}, new String[] {list});
			for (String item: items) {
				String iNode = lNode + "/" + item;
				String type = session.getData(iNode, TYPE, null);
				String value = session.getData(iNode, VALUE, null);
				String action = session.getData(iNode, ACTION, null);
				String[] stanzas = session.getDataList(iNode, STANZAS);
				Element eItem = new Element(ITEM,
					new String[] {ORDER, ACTION},
					new String[] {item, action});
				if (type != null) {
					eItem.addAttribute(TYPE, type);
				} // end of if (type != null)
				if (value != null) {
					eItem.addAttribute(VALUE, value);
				} // end of if (value != null)
				if (stanzas != null) {
					for (String stanza: stanzas) {
					eItem.addChild(new Element(stanza));
					} // end of for (String stanza: stanzas)
				} // end of if (stanzas != null)
				eList.addChild(eItem);
			} // end of for (String item: items)
			return eList;
		} // end of if (items != null)
		return null;
	}

} // Privacy
