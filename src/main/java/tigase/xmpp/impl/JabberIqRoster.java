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
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;
import tigase.db.NonAuthUserRepository;

import static tigase.xmpp.impl.Roster.SubscriptionType;

/**
 * Class <code>JabberIqRoster</code> implements part of <em>RFC-3921</em> -
 * <em>XMPP Instant Messaging</em> specification describing roster management.
 * 7.  Roster Management
 *
 *
 * Created: Tue Feb 21 17:42:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class JabberIqRoster {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqRoster");

  protected static final String XMLNS = "jabber:iq:roster";
	private static final String[] ELEMENTS = {"query"};
  private static final String[] XMLNSS = {XMLNS};
  protected static final Element[] DISCO_FEATURES =	{
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};
	public static final String ANON = "anon";

	public static Element createRosterPacket(String iq_type, String iq_id,
		String to, String from, String item_jid, String item_name, String item_group,
		String subscription, String item_type) {
		Element iq = new Element("iq",
			new String[] {"type", "id"},
			new String[] {iq_type, iq_id});
		if (from != null) {
			iq.addAttribute("from", from);
		}
		if (to != null) {
			iq.addAttribute("to", to);
		}
		Element query = new Element("query");
		query.setXMLNS(XMLNS);
		iq.addChild(query);
		Element item = new Element("item",
			new String[] {"jid"},
			new String[] {item_jid});
		if (item_type != null) {
			item.addAttribute("type", item_type);
		}
		if (item_name != null) {
			item.addAttribute(Roster.NAME, item_name);
		}
		if (subscription != null) {
			item.addAttribute(Roster.SUBSCRIPTION, subscription);
		}
		if (item_group != null) {
			Element group = new Element(Roster.GROUP, item_group);
			item.addChild(group);
		}
		query.addChild(item);
		return iq;
	}

	private static void processSetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException {

		Element request = packet.getElement();

    String buddy =
			JIDUtils.getNodeID(request.getAttribute("/iq/query/item", "jid"));
    Element item =  request.findChild("/iq/query/item");
    String subscription = item.getAttribute("subscription");
    if (subscription != null && subscription.equals("remove")) {
			SubscriptionType sub = Roster.getBuddySubscription(session, buddy);
			if (sub == null) {
				sub = SubscriptionType.none;
			}
			String type = request.getAttribute("/iq/query/item", "type");
			if (sub != SubscriptionType.none && (type == null || !type.equals(ANON))) {
				Element pres = new Element("presence");
				pres.setAttribute("to", buddy);
				pres.setAttribute("from", session.getUserId());
				pres.setAttribute("type", "unsubscribe");
				results.offer(new Packet(pres));
				pres = new Element("presence");
				pres.setAttribute("to", buddy);
				pres.setAttribute("from", session.getUserId());
				pres.setAttribute("type", "unsubscribed");
				results.offer(new Packet(pres));
				pres = new Element("presence");
				pres.setAttribute("to", buddy);
				pres.setAttribute("from", session.getJID());
				pres.setAttribute("type", "unavailable");
				results.offer(new Packet(pres));
			}
			// It happens sometimes that the client still think the buddy
			// is in the roster while he isn't. In such a case just ensure the
			// client that the buddy has been removed for sure
			Element it = new Element("item");
			it.setAttribute("jid", buddy);
			it.setAttribute("subscription", "remove");
			Roster.updateBuddyChange(session, results, it);
			Roster.removeBuddy(session, buddy);
      results.offer(packet.okResult((String)null, 0));
    } else {
      String name = request.getAttribute("/iq/query/item", "name");
      if (name == null) {
        name = buddy;
      } // end of if (name == null)
      Roster.setBuddyName(session, buddy, name);
			String type = request.getAttribute("/iq/query/item", "type");
			if (type != null && type.equals(ANON)) {
        Roster.setBuddySubscription(session, SubscriptionType.both, buddy);
				Element pres = (Element)session.getSessionData(Presence.PRESENCE_KEY);
				if (pres == null) {
					pres = new Element("presence");
				} else {
					pres = pres.clone();
				}
				pres.setAttribute("to", buddy);
				pres.setAttribute("from", session.getJID());
				results.offer(new Packet(pres));
			}
      if (Roster.getBuddySubscription(session, buddy) == null) {
        Roster.setBuddySubscription(session, SubscriptionType.none, buddy);
      } // end of if (getBuddySubscription(session, buddy) == null)
      List<Element> groups = item.getChildren();
      if (groups != null && groups.size() > 0) {
        String[] gr = new String[groups.size()];
        int cnt = 0;
        for (Element group : groups) {
					gr[cnt++] = (group.getCData() == null ? "" : group.getCData());
        } // end of for (ElementData group : groups)
        session.setDataList(Roster.groupNode(buddy), Roster.GROUPS, gr);
      } else {
				session.removeData(Roster.groupNode(buddy), Roster.GROUPS);
			} // end of else
      results.offer(packet.okResult((String)null, 0));
      Roster.updateBuddyChange(session, results,
				Roster.getBuddyItem(session, buddy));
    } // end of else
  }

	private static void processGetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results,
		final Map<String, Object> settings)
    throws NotAuthorizedException {
    String[] buddies = Roster.getBuddies(session);
    if (buddies != null) {
			Element query = new Element("query");
			query.setXMLNS(XMLNS);
      for (String buddy : buddies) {
				Element buddy_item = Roster.getBuddyItem(session, buddy);
				String item_group = buddy_item.getCData("/item/group");
// 				if (item_group != null && !item_group.isEmpty()
// 					&& !item_group.equals("Upline Support")) {
					query.addChild(buddy_item);
// 				}
      }
			if (query.getChildren() != null && query.getChildren().size() > 0) {
				results.offer(packet.okResult(query, 0));
			} else {
				results.offer(packet.okResult((String)null, 1));
			} // end of if (buddies != null) else
		} else {
			results.offer(packet.okResult((String)null, 1));
		}
		List<Element> its = DynamicRoster.getRosterItems(session, settings);
		if (its != null) {
			LinkedList<Element> items = new LinkedList<Element>(its);
			while (items.size() > 0) {
				Element iq = new Element("iq",
					new String[] {"type", "id", "to"},
					new String[] {"set", "dr-"+items.size(), session.getJID()});
				Element query = new Element("query");
				query.setXMLNS(XMLNS);
				iq.addChild(query);
				query.addChild(items.poll());
				while (query.getChildren().size() < 20 && items.size() > 0) {
					query.addChild(items.poll());
				}
				Packet rost_res = new Packet(iq);
				rost_res.setTo(session.getConnectionId());
				rost_res.setFrom(packet.getTo());
				results.offer(rost_res);
			}
		}
// 		if (session.isAnonymous()) {
// 			log.finest("Anonymous session: " + session.getUserId());
// 			String[] anon_peers = session.getAnonymousPeers();
// 			if (anon_peers != null) {
// 				for (String peer: anon_peers) {
// 					Element iq = new Element("iq",
// 						new String[] {"type", "id", "to", "from"},
// 						new String[] {"set", session.getUserName(), peer, peer});
// 					Element query = new Element("query");
// 					query.setXMLNS(XMLNS);
// 					iq.addChild(query);
// 					Element item = new Element("item", new Element[] {
// 							new Element("group", "Anonymous peers")},
// 						new String[] {"jid", "type", "name"},
// 						new String[] {session.getUserId(), ANON, session.getUserName()});
// 					query.addChild(item);
// 					Packet rost_update = new Packet(iq);
// 					results.offer(rost_update);
// 					log.finest("Sending roster update: " + rost_update.toString());
// 				}
// 			}
// 		}
  }

	public static void process(final Packet packet,
		final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings) throws XMPPException {

		try {
			if (packet.getElemFrom() != null
				&& !session.getUserId().equals(JIDUtils.getNodeID(packet.getElemFrom()))) {
				// RFC says: ignore such request
				log.warning(
					"Roster request 'from' attribute doesn't match session userid: "
					+ session.getUserId()
					+ ", request: " + packet.getStringData());
				return;
			} // end of if (packet.getElemFrom() != null
				// && !session.getUserId().equals(JIDUtils.getNodeID(packet.getElemFrom())))

			StanzaType type = packet.getType();
			switch (type) {
			case get:
				processGetRequest(packet, session, results, settings);
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
				"Received roster request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}


	/**
	 * <code>stopped</code> method is called when user disconnects or logs-out.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 */
	public static void stopped(final XMPPResourceConnection session,
		final Queue<Packet> results, final Map<String, Object> settings) {
// 		// Synchronization to avoid conflict with login/logout events
// 		// processed in the SessionManager asynchronously
// 		synchronized (session) {
// 			try {
// 				if (session.isAnonymous() && session.getAnonymousPeers() != null) {
// 					log.finest("Anonymous session: " + session.getUserId());
// 					String[] anon_peers = session.getAnonymousPeers();
// 					for (String peer: anon_peers) {
// 						Element iq = new Element("iq",
// 							new String[] {"type", "id", "to", "from"},
// 							new String[] {"set", session.getUserName(), peer, peer});
// 						Element query = new Element("query");
// 						query.setXMLNS(XMLNS);
// 						iq.addChild(query);
// 						Element item = new Element("item",
// 							new String[] {"jid", "subscription", "type"},
// 							new String[] {session.getUserId(), "remove", ANON});
// 						query.addChild(item);
// 						Packet rost_update = new Packet(iq);
// 						results.offer(rost_update);
// 						log.finest("Sending roster update: " + rost_update.toString());
// 					}
// 				}
// 			} catch (NotAuthorizedException e) {
// 				log.warning("Can not proceed with anonymous logout, session not authorized yet..."
// 					+ session.getConnectionId());
// 			}
// 		}
	}

} // JabberIqRoster
