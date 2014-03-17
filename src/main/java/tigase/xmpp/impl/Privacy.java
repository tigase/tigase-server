/*
 * Privacy.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.TigaseDBException;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Queue;

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
	/** Field description */
	protected static final String ACTION = "action";

	/** Field description */
	protected static final String ACTIVE = "active-list";

	/** Field description */
	protected static final String DEFAULT = "default-list";

	/** Field description */
	protected static final String ITEM = "item";

	/** Field description */
	protected static final String LIST = "list";

	/** Field description */
	protected static final String NAME = "name";

	/** Field description */
	protected static final String ORDER = "order";

	/** Field description */
	protected static final String PRIVACY = "privacy";

	/** Field description */
	protected static final String PRIVACY_LIST = "privacy-list";

	/** Field description */
	protected static final String STANZAS = "stanzas";

	/** Field description */
	protected static final String TYPE = "type";

	/** Field description */
	protected static final String VALUE = "value";

	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger(Privacy.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static void addList(XMPPResourceConnection session, Element list)
					throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Saving privacy list: {0}", list);
		}

		String lNode = listNode(list.getAttributeStaticStr(NAME));

		session.setData(lNode, PRIVACY_LIST, list.toString());
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static void removeList(XMPPResourceConnection session, Element list)
					throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Removing privacy list: {0}", list);
		}

		String lNode = listNode(list.getAttributeStaticStr(NAME));

		session.removeData(lNode, PRIVACY_LIST);
		session.removeDataGroup(lNode);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 */
	public static Element getActiveList(XMPPResourceConnection session)
					throws NotAuthorizedException {
		return (Element) session.getSessionData(ACTIVE);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 */
	public static String getActiveListName(XMPPResourceConnection session)
					throws NotAuthorizedException {
		Element list = getActiveList(session);

		if (list != null) {
			return list.getAttributeStaticStr(NAME);
		} else {
			return null;
		}    // end of if (list != null) else
	}

	/**
	 *
	 * @param session
	 * @return
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static Element getDefaultList(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		Element sessionDefaultList = (Element) session.getCommonSessionData( DEFAULT );
		if ( sessionDefaultList == null ){
			String defaultListName = getDefaultListName( session );
			if ( defaultListName != null ){
				sessionDefaultList = Privacy.getList( session, defaultListName);
				session.putCommonSessionData( DEFAULT, sessionDefaultList );
			}
		}
		return sessionDefaultList;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static String getDefaultListName(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		return session.getData(PRIVACY, DEFAULT, null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param list
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static Element getList(XMPPResourceConnection session, String list)
					throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Loading privacy list: {0}", list);
		}

		String lNode    = listNode(list);
		String list_str = session.getData(lNode, PRIVACY_LIST, null);

		if ((list_str != null) &&!list_str.isEmpty()) {
			SimpleParser parser          = SingletonFactory.getParserInstance();
			DomBuilderHandler domHandler = new DomBuilderHandler();

			parser.parse(domHandler, list_str.toCharArray(), 0, list_str.length());

			Queue<Element> elems = domHandler.getParsedElements();
			Element result       = elems.poll();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Loaded privacy list: {0}", result);
			}

			return result;
		} else {
			return getListOld(session, list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param list
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static Element getListOld(XMPPResourceConnection session, String list)
					throws NotAuthorizedException, TigaseDBException {
		String lNode   = listNode(list);
		String[] items = session.getDataGroups(lNode);

		if (items != null) {
			Element eList = new Element(LIST, new String[] { NAME }, new String[] { list });

			for (String item : items) {
				String iNode     = lNode + "/" + item;
				String type      = session.getData(iNode, TYPE, null);
				String value     = session.getData(iNode, VALUE, null);
				String action    = session.getData(iNode, ACTION, null);
				String[] stanzas = session.getDataList(iNode, STANZAS);

				if ((item == null) || (action == null)) {
					continue;
				}

				Element eItem = new Element(ITEM, new String[] { ORDER, ACTION },
																		new String[] { item,
								action });

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

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static String[] getLists(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		return session.getDataGroups(PRIVACY);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param list
	 *
	 * 
	 */
	public static String listNode(final String list) {
		return PRIVACY + "/" + list;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param lName
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static void setActiveList(XMPPResourceConnection session, String lName)
					throws NotAuthorizedException, TigaseDBException {
		if (lName == null) {

			// User declines to use current actiev list
			session.removeSessionData( ACTIVE );
		} else {

			// User selects a different active list
			Element list = getList(session, lName);

			if (list != null) {
				session.putSessionData(ACTIVE, list);
			} else {
				log.log(
						Level.INFO,
						"Setting active list to null, do something better than that, perhaps notify user.");
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static void setDefaultList(XMPPResourceConnection session, Element list)
					throws NotAuthorizedException, TigaseDBException {
		if ((list != null) && (list.getAttributeStaticStr(NAME) != null)) {
			session.setData(PRIVACY, DEFAULT, list.getAttributeStaticStr(NAME));
		} else {
			session.removeData(PRIVACY, DEFAULT);
			session.removeCommonSessionData( DEFAULT );
		}
	}

}    // Privacy


//~ Formatted in Tigase Code Convention on 13/02/20
