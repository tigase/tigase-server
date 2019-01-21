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
package tigase.xmpp.impl;

import tigase.db.TigaseDBException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterFactory;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class defining data structure for privacy lists. Sample data storage: {@code <node name="privacy"> <map> <entry
 * value="private" type="String" key="default"/> </map> List name: <node name="private"> <map/> Item order: <node
 * name="1"> <map> <entry value="jid" type="String" key="type"/> <entry value="user%40domain.com/res" type="String"
 * key="value"/> <entry value="deny" type="String" key="action"/> <entry type="String[]" key="stanzas"> <item
 * value="message"/> <item value="iq"/> </entry> </map> </node> </node> }
 * <br>
 * Created: Mon Oct  9 20:50:09 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class Privacy {

	public static final String ACTION = "action";

	public static final String ACTIVE = "active-list";

	public static final String DEFAULT = "default-list";

	public static final String ITEM = "item";

	public static final String LIST = "list";

	public static final String NAME = "name";

	public static final String ORDER = "order";

	public static final String PRIVACY = "privacy";

	public static final String PRIVACY_LIST = "privacy-list";

	public static final String STANZAS = "stanzas";

	public static final String TYPE = "type";

	public static final String VALUE = "value";

	public static final String PRIVACY_LIST_LOADED = "privacy-lists-loaded";

	private static Logger log = Logger.getLogger(Privacy.class.getName());

	public static void addList(XMPPResourceConnection session, Element list)
			throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Saving privacy list: {0}", list);
		}

		String lNode = listNode(list.getAttributeStaticStr(NAME));

		session.setData(lNode, PRIVACY_LIST, list.toString());
	}

	public static boolean block(XMPPResourceConnection session, String jid)
			throws NotAuthorizedException, TigaseDBException {
		String name = getDefaultListName(session);
		if (name == null) {
			name = "default";
		}
		Element list = getList(session, name);
		if (list != null) {
			if (list.findChild(item -> jid.equals(item.getAttributeStaticStr(VALUE)) && isBlockItem(item)) != null) {
				return false;
			}
		}
		Element list_new = new Element(LIST, new String[]{NAME}, new String[]{name});
		list_new.addChild(
				new Element(ITEM, new String[]{TYPE, ACTION, VALUE, ORDER}, new String[]{"jid", "deny", jid, "0"}));
		if (list != null) {
			List<Element> items = list.getChildren();
			if (items != null) {
				Collections.sort(items, JabberIqPrivacy.compar);
				for (int i = 0; i < items.size(); i++) {
					items.get(i).setAttribute(ORDER, "" + (i + 1));
				}
				list_new.addChildren(items);
			}
		}
		updateList(session, name, list_new);
		return true;
	}

	public static PrivacyList getActiveList(XMPPResourceConnection session) throws NotAuthorizedException {
		return (PrivacyList) session.getSessionData(ACTIVE);
	}

	public static String getActiveListName(XMPPResourceConnection session) throws NotAuthorizedException {
		PrivacyList list = getActiveList(session);

		if (list != null) {
			return list.getName();
		} else {
			return null;
		}    // end of if (list != null) else
	}

	public static List<String> getBlocked(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		PrivacyList list = getDefaultList(session);
		List<String> ulist = null;
		if (list != null) {
			ulist = list.getBlockedJids().map(jid -> jid.toString()).collect(Collectors.toList());
		}
		return ulist;
	}

	public static PrivacyList getDefaultList(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		PrivacyList sessionDefaultList = (PrivacyList) session.getCommonSessionData(DEFAULT);
		if (session.getCommonSessionData(PRIVACY_LIST_LOADED) == null) {
			sessionDefaultList = PrivacyList.create(session, RosterFactory.getRosterImplementation(true),
													getDefaultListElement(session));
			if (null != sessionDefaultList) {
				session.putCommonSessionData(DEFAULT, sessionDefaultList);
			}
			session.putCommonSessionData(PRIVACY_LIST_LOADED, PRIVACY_LIST_LOADED);
		}
		return sessionDefaultList;
	}

	public static Element getDefaultListElement(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		Element sessionDefaultList = null;
		String defaultListName = getDefaultListName(session);
		if (defaultListName != null) {
			sessionDefaultList = Privacy.getList(session, defaultListName);
		}
		return sessionDefaultList;
	}

	public static String getDefaultListName(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		return session.getData(PRIVACY, DEFAULT, null);
	}

	public static Element getList(XMPPResourceConnection session, String list)
			throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Loading privacy list: {0}", list);
		}

		String lNode = listNode(list);
		String list_str = session.getData(lNode, PRIVACY_LIST, null);

		if ((list_str != null) && !list_str.isEmpty()) {
			SimpleParser parser = SingletonFactory.getParserInstance();
			DomBuilderHandler domHandler = new DomBuilderHandler();

			parser.parse(domHandler, list_str.toCharArray(), 0, list_str.length());

			Queue<Element> elems = domHandler.getParsedElements();
			Element result = elems.poll();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Loaded privacy list: {0}", result);
			}

			return result;
		} else {
			return getListOld(session, list);
		}
	}

	public static Element getListOld(XMPPResourceConnection session, String list)
			throws NotAuthorizedException, TigaseDBException {
		String lNode = listNode(list);
		String[] items = session.getDataGroups(lNode);

		if (items != null && list != null) {
			Element eList = new Element(LIST, new String[]{NAME}, new String[]{list});

			for (String item : items) {
				String iNode = lNode + "/" + item;
				String type = session.getData(iNode, TYPE, null);
				String value = session.getData(iNode, VALUE, null);
				String action = session.getData(iNode, ACTION, null);
				String[] stanzas = session.getDataList(iNode, STANZAS);

				if ((item == null) || (action == null)) {
					continue;
				}

				Element eItem = new Element(ITEM, new String[]{ORDER, ACTION}, new String[]{item, action});

				if (type != null) {
					eItem.addAttribute(TYPE, type);
				}      // end of if (type != null)
				if (value != null) {
					eItem.addAttribute(VALUE, value);
				}      // end of if (value != null)
				if (stanzas != null) {
					for (String stanza : stanzas) {
						eItem.addChild(new Element(stanza));
					}    // end of for (String stanza: stanzas)
				}      // end of if (stanzas != null)
				eList.addChild(eItem);
			}        // end of for (String item: items)

			return eList;
		}          // end of if (items != null)

		return null;
	}

	public static String[] getLists(XMPPResourceConnection session) throws NotAuthorizedException, TigaseDBException {
		return session.getDataGroups(PRIVACY);
	}

	private static boolean isBlockItem(Element item) {
		return "jid".equals(item.getAttributeStaticStr(TYPE)) && "deny".equals(item.getAttributeStaticStr(ACTION)) &&
				item.getChildren() == null;
	}

	public static String listNode(final String list) {
		return PRIVACY + "/" + list;
	}

	public static void removeList(XMPPResourceConnection session, Element list)
			throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Removing privacy list: {0}", list);
		}

		String lNode = listNode(list.getAttributeStaticStr(NAME));

		session.removeData(lNode, PRIVACY_LIST);
		session.removeDataGroup(lNode);
	}

	public static void setActiveList(XMPPResourceConnection session, String lName)
			throws NotAuthorizedException, TigaseDBException {
		if (lName == null) {

			// User declines to use current actiev list
			session.removeSessionData(ACTIVE);
		} else {

			// User selects a different active list
			PrivacyList list = PrivacyList.create(session, RosterFactory.getRosterImplementation(true),
												  getList(session, lName));

			if (list != null) {
				session.putSessionData(ACTIVE, list);
			} else {
				log.log(Level.INFO, "Setting active list to null, do something better than that, perhaps notify user.");
			}
		}
	}

	public static void setDefaultList(XMPPResourceConnection session, Element list)
			throws NotAuthorizedException, TigaseDBException {
		if ((list != null) && (list.getAttributeStaticStr(NAME) != null)) {
			session.setData(PRIVACY, DEFAULT, list.getAttributeStaticStr(NAME));
			session.putCommonSessionData(DEFAULT,
										 PrivacyList.create(session, RosterFactory.getRosterImplementation(true),
															list));
		} else {
			session.removeData(PRIVACY, DEFAULT);
			session.removeCommonSessionData(DEFAULT);
		}
	}

	public static boolean unblock(XMPPResourceConnection session, String jid)
			throws NotAuthorizedException, TigaseDBException {
		String name = getDefaultListName(session);
		Element list = getList(session, name);
		if (list == null) {
			return false;
		}

		Element list_new = new Element(LIST, new String[]{NAME}, new String[]{name});
		List<Element> items = list.findChildren(
				item -> !jid.equals(item.getAttributeStaticStr(VALUE)) || !isBlockItem(item));
		if (items != null) {
			Collections.sort(items, JabberIqPrivacy.compar);
			for (int i = 0; i < items.size(); i++) {
				items.get(i).setAttribute(ORDER, "" + (i + 1));
			}
			list_new.addChildren(items);
		}

		updateList(session, name, list_new);

		return false;
	}

	public static List<String> unblockAll(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		String name = getDefaultListName(session);
		Element list = getList(session, name);
		if (list == null) {
			return null;
		}

		List<String> ulist = list.mapChildren(item -> isBlockItem(item), item -> item.getAttributeStaticStr(VALUE));

		Element list_new = new Element(LIST, new String[]{NAME}, new String[]{name});
		List<Element> items = list.findChildren(item -> !isBlockItem(item));
		if (items != null) {
			Collections.sort(items, JabberIqPrivacy.compar);
			for (int i = 0; i < items.size(); i++) {
				items.get(i).setAttribute(ORDER, "" + (i + 1));
			}
			list_new.addChildren(items);
		}

		updateList(session, name, list_new);

		return ulist;
	}

	private static void updateList(XMPPResourceConnection session, String name, Element list_new)
			throws NotAuthorizedException, TigaseDBException {
		addList(session, list_new);
		Privacy.setDefaultList(session, list_new);
		if (getDefaultList(session) == null) {
			Privacy.setActiveList(session, name);
		} else if (name.equals(getActiveListName(session))) {
			session.putCommonSessionData(ACTIVE, list_new);
			session.putSessionData(ACTIVE, list_new);
		}
	}

}    // Privacy

