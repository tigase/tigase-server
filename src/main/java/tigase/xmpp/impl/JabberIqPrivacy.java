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

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import java.util.Comparator;
import java.util.Collections;
import tigase.server.Packet;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;
import tigase.xml.Element;
import tigase.db.NonAuthUserRepository;

import static tigase.xmpp.impl.Privacy.*;

/**
 * Describe class JabberIqPrivacy here.
 *
 *
 * Created: Mon Oct  9 18:18:11 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqPrivacy extends XMPPProcessor
	implements XMPPProcessorIfc, XMPPPreprocessorIfc {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqPrivacy");

  private static final String XMLNS = "jabber:iq:privacy";
	private static final String ID = XMLNS;
	private static final String[] ELEMENTS = {"query"};
  private static final String[] XMLNSS = {XMLNS};
  private static final Element[] DISCO_FEATURES =	{
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return Arrays.copyOf(DISCO_FEATURES, DISCO_FEATURES.length); }


	private enum ITEM_TYPE { jid, group, subscription, all };
	private enum ITEM_ACTION { allow, deny };
	private enum ITEM_SUBSCRIPTIONS { both, to, from, none };

	private static final Comparator<Element> compar =
		new Comparator<Element>() {
		public int compare(Element el1, Element el2) {
			String or1 = el1.getAttribute(ORDER);
			String or2 = el2.getAttribute(ORDER);
			return or1.compareTo(or2);
		}
		};

	public String id() { return ID; }

	public String[] supElements()
	{ return Arrays.copyOf(ELEMENTS, ELEMENTS.length); }

  public String[] supNamespaces()
	{ return Arrays.copyOf(XMLNSS, XMLNSS.length); }

	/**
	 * <code>preProcess</code> method checks only incoming stanzas
	 * so it doesn't check for presence-out at all.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param repo a <code>NonAuthUserRepository</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
		NonAuthUserRepository repo,	Queue<Packet> results) {

		if (session == null || !session.isAuthorized()) {
			return false;
		} // end of if (session == null)

		try {
			Element list = Privacy.getActiveList(session);
			if (list == null) {
				String lName = Privacy.getDefaultList(session);
				if (lName != null) {
					Privacy.setActiveList(session, lName);
					list = Privacy.getActiveList(session);
				} // end of if (lName != null)
			} // end of if (lName == null)
			if (list != null) {
				List<Element> items = list.getChildren();
				Collections.sort(items, compar);
				for (Element item: items) {
					boolean type_matched = false;
					boolean elem_matched = false;
					ITEM_TYPE type = ITEM_TYPE.all;
					if (item.getAttribute(TYPE) != null) {
						type = ITEM_TYPE.valueOf(item.getAttribute(TYPE));
					} // end of if (item.getAttribute(TYPE) != null)
					String value = item.getAttribute(VALUE);
					String from = packet.getElemFrom();
					if (from != null) {
						switch (type) {
						case jid:
							type_matched = from.contains(value);
							break;
						case group:
							String[] groups = Roster.getBuddyGroups(session, from);
							for (String group: groups) {
								if (type_matched = group.equals(value)) {
									break;
								} // end of if (group.equals(value))
							} // end of for (String group: groups)
							break;
						case subscription:
							ITEM_SUBSCRIPTIONS subscr = ITEM_SUBSCRIPTIONS.valueOf(value);
							switch (subscr) {
							case to:
								type_matched = Roster.isSubscribedTo(session, from);
								break;
							case from:
								type_matched = Roster.isSubscribedFrom(session, from);
								break;
							case none:
								type_matched = (!Roster.isSubscribedFrom(session, from)
									&& !Roster.isSubscribedTo(session, from));
								break;
							case both:
								type_matched = (Roster.isSubscribedFrom(session, from)
									&& Roster.isSubscribedTo(session, from));
								break;
							default:
								break;
							} // end of switch (subscr)
							break;
						case all:
						default:
							type_matched = true;
							break;
						} // end of switch (type)
					} else {
						if (type == ITEM_TYPE.all) {
							type_matched = true;
						}
					} // end of if (from != null) else
					if (!type_matched) {
						break;
					} // end of if (!type_matched)

					List<Element> elems = item.getChildren();
					if (elems == null || elems.size() == 0) {
						elem_matched = true;
					} else {
						for (Element elem: elems) {
							if (elem.getName().equals("presence-in")) {
								if (packet.getElemName().equals("presence")
									&& (packet.getType() == null
										|| packet.getType() == StanzaType.unavailable)) {
									elem_matched = true;
									break;
								}
							} else {
								if (elem.getName().equals(packet.getElemName())) {
									elem_matched = true;
									break;
								} // end of if (elem.getName().equals(packet.getElemName()))
							} // end of if (elem.getName().equals("presence-in")) else
						} // end of for (Element elem: elems)
					} // end of else
					if (!elem_matched) {
						break;
					} // end of if (!elem_matched)

					ITEM_ACTION action = ITEM_ACTION.valueOf(item.getAttribute(ACTION));
					switch (action) {
					case allow:
						return false;
					case deny:
						return true;
					default:
						break;
					} // end of switch (action)
				} // end of for (Element item: items)
			} // end of if (lName != null)
		} catch (NotAuthorizedException e) {
// 			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
// 					"You must authorize session first.", true));
		} // end of try-catch

		return false;
	}

  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results)
		throws XMPPException {

		if (session == null) {
			return;
		} // end of if (session == null)

		try {
			StanzaType type = packet.getType();
			switch (type) {
			case get:
				processGetRequest(packet, session, results);
				break;
			case set:
				processSetRequest(packet, session, results);
				break;
			case result:
				// Ignore
				break;
			default:
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Request type is incorrect", false));
				break;
			} // end of switch (type)

		} catch (NotAuthorizedException e) {
      log.warning(
				"Received privacy request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}

	private void processSetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException, XMPPException {
		List<Element> children = packet.getElemChildren("/iq/query");
		if (children != null && children.size() == 1) {
			Element child = children.get(0);
			if (child.getName().equals("list")) {
				// Broken privacy implementation sends list without name set
				// instead of sending BAD_REQUEST error I can just assign
				// 'default' name here.
				String name = child.getAttribute(NAME);
				if (name == null || name.length() == 0) {
					child.setAttribute(NAME, "default");
				} // end of if (name == null || name.length() == 0)
				Privacy.addList(session, child);
				results.offer(packet.okResult((String)null, 0));
			} // end of if (child.getName().equals("list))
			if (child.getName().equals("default")) {
				Privacy.setDefaultList(session, child);
				results.offer(packet.okResult((String)null, 0));
			} // end of if (child.getName().equals("list))
			if (child.getName().equals("active")) {
				Privacy.setActiveList(session, child.getAttribute(NAME));
				results.offer(packet.okResult((String)null, 0));
			} // end of if (child.getName().equals("list))
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"Only 1 element is allowed in privacy set request.", true));
		} // end of else
	}

	private void processGetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException, XMPPException {
		List<Element> children = packet.getElemChildren("/iq/query");
		if (children == null || children.size() == 0) {
			String[] lists = Privacy.getLists(session);
			if (lists != null) {
				StringBuilder sblists = new StringBuilder();
				for (String list : lists) {
					sblists.append("<list name=\"" + list + "\"/>");
				}
				String list = Privacy.getDefaultList(session);
				if (list != null) {
					sblists.append("<default name=\"" + list + "\"/>");
				} // end of if (defList != null)
				list = Privacy.getActiveListName(session);
				if (list != null) {
					sblists.append("<active name=\"" + list + "\"/>");
				} // end of if (defList != null)
				results.offer(packet.okResult(sblists.toString(), 1));
			} else {
				results.offer(packet.okResult((String)null, 1));
			} // end of if (buddies != null) else
		} else {
			if (children.size() > 1) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"You can retrieve only one list at a time.", true));
			} else {
				Element eList = Privacy.getList(session,
					children.get(0).getAttribute("name"));
				if (eList != null) {
					results.offer(packet.okResult(eList, 1));
				} else {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
							"Requested list not found.", true));
				} // end of if (eList != null) else
			} // end of else
		} // end of else
  }

} // JabberIqPrivacy
