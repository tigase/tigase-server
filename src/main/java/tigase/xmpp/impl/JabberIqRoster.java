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

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Packet;
import tigase.server.Priority;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;

import static tigase.xmpp.impl.roster.Roster.SubscriptionType;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.JabberIqRoster");
	protected static final String XMLNS_DYNAMIC = "jabber:iq:roster-dynamic";
	private static final String[] ELEMENTS = { "query", "query" };
	private static final String[] XMLNSS = { RosterAbstract.XMLNS, XMLNS_DYNAMIC };
	protected static final Element[] DISCO_FEATURES = {
		new Element("feature", new String[] { "var" }, new String[] { RosterAbstract.XMLNS }),
		new Element("feature", new String[] { "var" }, new String[] { XMLNS_DYNAMIC }) };
	protected static final Element[] FEATURES = {
		new Element("ver", new String[] { "xmlns" },
			new String[] { "urn:xmpp:features:rosterver" }) };

	/** Field description */
	public static final String ANON = "anon";
	private static RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param item
	 *
	 * @return
	 */
	public static String[] getItemGroups(Element item) {
		List<Element> elgr = item.getChildren();

		if ((elgr != null) && (elgr.size() > 0)) {
			ArrayList<String> groups = new ArrayList<String>();

			for (Element grp : elgr) {
				if (grp.getName() == RosterAbstract.GROUP) {
					groups.add(grp.getCData());
				}
			}

			if (groups.size() > 0) {
				return groups.toArray(new String[groups.size()]);
			}
		}

		return null;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws XMPPException
	 */
	public static void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		try {
			if ((packet.getStanzaFrom() != null)
					&&!session.getUserId().equals(packet.getStanzaFrom().getBareJID())) {

				// RFC says: ignore such request
				log.warning("Roster request 'from' attribute doesn't match session userid: "
						+ session.getUserId() + ", request: " + packet);

				return;
			}    // end of if (packet.getElemFrom() != null

			// && !session.getUserId().equals(JIDUtils.getNodeID(packet.getElemFrom())))
			StanzaType type = packet.getType();
			String xmlns = packet.getElement().getXMLNS("/iq/query");

			if (xmlns == RosterAbstract.XMLNS) {
				switch (type) {
					case get :
						processGetRequest(packet, session, results, settings);

						break;

					case set :
						processSetRequest(packet, session, results, settings);

						break;

					case result :

						// Ignore
						break;

					default :
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Request type is incorrect", false));

						break;
				}    // end of switch (type)
			} else {
				if (xmlns == XMLNS_DYNAMIC) {
					switch (type) {
						case get :
							dynamicGetRequest(packet, session, results, settings);

							break;

						case set :
							dynamicSetRequest(packet, session, results, settings);

							break;

						case result :

							// Ignore
							break;

						default :
							results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
									"Request type is incorrect", false));

							break;
					}    // end of switch (type)
				} else {

					// Hm, don't know what to do, unexpected name space, let's record it
					log.warning("Unknown XMLNS for the roster plugin: " + packet);
				}
			}
		} catch (NotAuthorizedException e) {
			log.warning("Received roster request but user session is not authorized yet: " + packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.warning("Database problem, please contact admin: " + e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
					"Database access problem, please contact administrator.", true));
		}    // end of try-catch
	}

	/**
	 * <code>stopped</code> method is called when user disconnects or logs-out.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 */
	public static void stopped(final XMPPResourceConnection session,
			final Queue<Packet> results, final Map<String, Object> settings) {

//  // Synchronization to avoid conflict with login/logout events
//  // processed in the SessionManager asynchronously
//  synchronized (session) {
//    try {
//      if (session.isAnonymous() && session.getAnonymousPeers() != null) {
//        log.finest("Anonymous session: " + session.getUserId());
//        String[] anon_peers = session.getAnonymousPeers();
//        for (String peer: anon_peers) {
//          Element iq = new Element("iq",
//            new String[] {"type", "id", "to", "from"},
//            new String[] {"set", session.getUserName(), peer, peer});
//          Element query = new Element("query");
//          query.setXMLNS(XMLNS);
//          iq.addChild(query);
//          Element item = new Element("item",
//            new String[] {"jid", "subscription", "type"},
//            new String[] {session.getUserId(), "remove", ANON});
//          query.addChild(item);
//          Packet rost_update = new Packet(iq);
//          results.offer(rost_update);
//          log.finest("Sending roster update: " + rost_update.toString());
//        }
//      }
//    } catch (NotAuthorizedException e) {
//      log.warning("Can not proceed with anonymous logout, session not authorized yet..."
//        + session.getConnectionId());
//    }
//  }
	}

	private static void dynamicGetRequest(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings)
			throws NotAuthorizedException {
		Element request = packet.getElement();
		Element item = request.findChild("/iq/query/item");

		if (item != null) {
			Element new_item = DynamicRoster.getItemExtraData(session, settings, item);

			if (new_item == null) {
				new_item = item;
			}

			results.offer(packet.okResult(new_item, 1));
		} else {
			try {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Missing 'item' element, request can not be processed.", true));
			} catch (PacketErrorTypeException ex) {
				log.log(Level.SEVERE, "Received error packet? not possible.", ex);
			}
		}
	}

	private static void dynamicSetRequest(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings) {
		Element request = packet.getElement();
		List<Element> items = request.getChildren("/iq/query");

		if ((items != null) && (items.size() > 0)) {
			for (Element item : items) {
				DynamicRoster.setItemExtraData(session, settings, item);
			}

			results.offer(packet.okResult((String) null, 0));
		} else {
			try {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Missing 'item' element, request can not be processed.", true));
			} catch (PacketErrorTypeException ex) {
				log.log(Level.SEVERE, "Received error packet? not possible.", ex);
			}
		}
	}

	private static void processGetRequest(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings)
			throws NotAuthorizedException, TigaseDBException {
		String incomingHash = packet.getElement().getAttribute("/iq/query", "ver");
		String storedHash = "";
		List<Element> its = DynamicRoster.getRosterItems(session, settings);

		if ((its != null) && (its.size() > 0)) {
			for (Iterator<Element> it = its.iterator(); it.hasNext(); ) {
				Element element = it.next();

				try {
					JID jid = JID.jidInstance(element.getAttribute("jid"));

					if (roster_util.containsBuddy(session, jid)) {
						roster_util.setBuddySubscription(session, SubscriptionType.both, jid);

						String[] itemGroups = getItemGroups(element);

						if (itemGroups != null) {
							roster_util.addBuddyGroup(session, jid, itemGroups);
						}

						it.remove();
					}
				} catch (TigaseStringprepException ex) {
					log.info("JID from dynamic roster is incorrect, stringprep failed for: "
							+ element.getAttribute("jid"));
					it.remove();
				}
			}
		}

		if (incomingHash != null) {
			storedHash = roster_util.getBuddiesHash(session);

			if (incomingHash.equals(storedHash)) {
				results.offer(packet.okResult((String) null, 0));

				return;
			}
		}

		List<Element> ritems = roster_util.getRosterItems(session);

		if ((ritems != null) && (ritems.size() > 0)) {
			Element query = new Element("query");

			query.setXMLNS(RosterAbstract.XMLNS);

			if (incomingHash != null) {
				query.setAttribute("ver", storedHash);
			}

			query.addChildren(ritems);
			results.offer(packet.okResult(query, 0));
		} else {
			results.offer(packet.okResult((String) null, 1));
		}

//  String[] buddies = roster_util.getBuddies(session, false);
//  if (buddies != null) {
//    Element query = new Element("query");
//    query.setXMLNS(XMLNS);
//    if (incomingHash != null)
//      query.setAttribute("ver", storedHash);
//    for (String buddy : buddies) {
//      try {
//        Element buddy_item = roster_util.getBuddyItem(session, buddy);
//        //String item_group = buddy_item.getCData("/item/group");
//        query.addChild(buddy_item);
//      } catch (TigaseDBException e) {
//        // It happens that some weird JIDs drive database crazy and
//        // it throws exceptions. Let's for now just ignore those
//        // contacts....
//        log.info("Can not retrieve data for contact: " + buddy
//          + ", an exception occurs: " + e);
//      }
//    }
//    if (query.getChildren() != null && query.getChildren().size() > 0) {
//      results.offer(packet.okResult(query, 0));
//    } else {
//      results.offer(packet.okResult((String)null, 1));
//    } // end of if (buddies != null) else
//  } else {
//    results.offer(packet.okResult((String)null, 1));
//  }
		if ((its != null) && (its.size() > 0)) {
			ArrayDeque<Element> items = new ArrayDeque<Element>(its);

			while (items.size() > 0) {
				Element iq = new Element("iq", new String[] { "type", "id", "to" },
					new String[] { "set",
						"dr-" + items.size(), session.getJID().toString() });
				Element query = new Element("query");

				query.setXMLNS(RosterAbstract.XMLNS);
				iq.addChild(query);
				query.addChild(items.poll());

				while ((query.getChildren().size() < 20) && (items.size() > 0)) {
					query.addChild(items.poll());
				}

				Packet rost_res = Packet.packetInstance(iq, null, session.getJID());

				rost_res.setPacketTo(session.getConnectionId());
				rost_res.setPacketFrom(packet.getTo());
				results.offer(rost_res);
			}
		}

//  if (session.isAnonymous()) {
//    log.finest("Anonymous session: " + session.getUserId());
//    String[] anon_peers = session.getAnonymousPeers();
//    if (anon_peers != null) {
//      for (String peer: anon_peers) {
//        Element iq = new Element("iq",
//          new String[] {"type", "id", "to", "from"},
//          new String[] {"set", session.getUserName(), peer, peer});
//        Element query = new Element("query");
//        query.setXMLNS(XMLNS);
//        iq.addChild(query);
//        Element item = new Element("item", new Element[] {
//            new Element("group", "Anonymous peers")},
//          new String[] {"jid", "type", "name"},
//          new String[] {session.getUserId(), ANON, session.getUserName()});
//        query.addChild(item);
//        Packet rost_update = new Packet(iq);
//        results.offer(rost_update);
//        log.finest("Sending roster update: " + rost_update.toString());
//      }
//    }
//  }
	}

	private static void processSetRequest(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, final Map<String, Object> settings)
			throws NotAuthorizedException, TigaseDBException, PacketErrorTypeException {

		// Element request = packet.getElement();
		List<Element> items = packet.getElemChildren("/iq/query");

		if (items != null) {
			try {

				// RFC-3921 draft bis-03 forbids multiple items in one request
				// This however seems to make no much sense and actually was
				// requested by many users to allow for multiple items
				for (Element item : items) {
					JID buddy = JID.jidInstance(item.getAttribute("jid"));

					if (buddy.getBareJID().equals(session.getUserId())) {
						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
								"User can't add himself to the roster, RFC says NO.", true));

						return;
					}

					String subscription = item.getAttribute("subscription");

					if ((subscription != null) && subscription.equals("remove")) {
						SubscriptionType sub = roster_util.getBuddySubscription(session, buddy);

						if (sub == null) {
							sub = SubscriptionType.none;
						}

						String type = item.getAttribute("type");

						if ((sub != SubscriptionType.none) && ((type == null) ||!type.equals(ANON))) {

							// Unavailable presence should be sent first, otherwise it will be blocked by
							// the server after the subscription is cancelled
							Element pres = new Element("presence");

							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getJID().toString());
							pres.setAttribute("type", "unavailable");

							Packet pres_packet = Packet.packetInstance(pres, session.getJID(), buddy);

							// We have to set a higher priority for this particular unavailable packet
							// to make sure it is delivered before subscription cancellation
							pres_packet.setPriority(Priority.HIGH);
							results.offer(pres_packet);
							pres = new Element("presence");
							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getUserId().toString());
							pres.setAttribute("type", "unsubscribe");
							results.offer(Packet.packetInstance(pres,
									session.getJID().copyWithoutResource(), buddy));
							pres = new Element("presence");
							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getUserId().toString());
							pres.setAttribute("type", "unsubscribed");
							results.offer(Packet.packetInstance(pres,
									session.getJID().copyWithoutResource(), buddy));
						}    // is in the roster while he isn't. In such a case just ensure the

						// client that the buddy has been removed for sure
						Element it = new Element("item");

						it.setAttribute("jid", buddy.toString());
						it.setAttribute("subscription", "remove");
						roster_util.updateBuddyChange(session, results, it);
						roster_util.removeBuddy(session, buddy);
					} else {
						Element dynamicItem = DynamicRoster.getBuddyItem(session, settings, buddy);
						String name = item.getAttribute("name");

						// if (name == null) {
						// name = buddy;
						// } // end of if (name == null)
						List<Element> groups = item.getChildren();
						String[] gr = null;

						if ((groups != null) && (groups.size() > 0)) {
							gr = new String[groups.size()];

							int cnt = 0;

							for (Element group : groups) {
								gr[cnt++] = ((group.getCData() == null) ? "" : group.getCData());
							}    // end of for (ElementData group : groups)

							// end of for (ElementData group : groups)
						}

						roster_util.addBuddy(session, buddy, name, gr);

						String type = item.getAttribute("type");

						if ((type != null) && type.equals(ANON)) {
							roster_util.setBuddySubscription(session, SubscriptionType.both, buddy);

							Element pres =
								(Element) session.getSessionData(XMPPResourceConnection.PRESENCE_KEY);

							if (pres == null) {
								pres = new Element("presence");
							} else {
								pres = pres.clone();
							}

							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getJID().toString());
							results.offer(Packet.packetInstance(pres, session.getJID(), buddy));
						}

						Element new_buddy = roster_util.getBuddyItem(session, buddy);

						if (log.isLoggable(Level.FINEST)) {
							log.finest("1. New Buddy: " + new_buddy.toString());
						}

						if (roster_util.getBuddySubscription(session, buddy) == null) {
							roster_util.setBuddySubscription(session, SubscriptionType.none, buddy);
						}    // end of if (getBuddySubscription(session, buddy) == null)

						if (dynamicItem != null) {
							roster_util.setBuddySubscription(session, SubscriptionType.both, buddy);

							String[] itemGroups = getItemGroups(dynamicItem);

							if (itemGroups != null) {
								roster_util.addBuddyGroup(session, buddy, itemGroups);
							}
						}

						new_buddy = roster_util.getBuddyItem(session, buddy);

						if (log.isLoggable(Level.FINEST)) {
							log.finest("2. New Buddy: " + new_buddy.toString());
						}

						roster_util.updateBuddyChange(session, results, new_buddy);
					}      // end of else

					// end of else
				}

				results.offer(packet.okResult((String) null, 0));
			} catch (TigaseStringprepException ex) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Buddy JID is incorrct, stringprep failed.", true));
			}
		} else {
			log.warning("No items found in roster set request: " + packet);
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"No items found in the roster set request", true));
		}
	}
}    // JabberIqRoster


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
