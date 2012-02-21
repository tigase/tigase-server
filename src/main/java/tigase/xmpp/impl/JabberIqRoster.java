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
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;
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
public class JabberIqRoster extends XMPPProcessor implements XMPPProcessorIfc, XMPPStopListenerIfc {

	/**
	 * Private logger for class instance.
	 */
	private static final Logger log = Logger.getLogger(JabberIqRoster.class.getName());
	private static final String[] ELEMENTS = { "query", "query" };
	private static final String[] XMLNSS = { RosterAbstract.XMLNS, RosterAbstract.XMLNS_DYNAMIC };
	private static final String ID = RosterAbstract.XMLNS;

	/** Field description */
	public static final String ANON = "anon";

	//~--- fields ---------------------------------------------------------------

	protected RosterAbstract roster_util = getRosterUtil();

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
			ArrayList<String> groups = new ArrayList<String>(1);

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

	protected static void dynamicGetRequest(Packet packet, XMPPResourceConnection session,
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

	protected static void dynamicSetRequest(Packet packet, XMPPResourceConnection session,
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

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors() * 2;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

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
	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		if (session == null) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is null, ignoring packet: {0}", packet);
			}

			return;
		}    // end of if (session == null)

		if ( !session.isAuthorized()) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is not authorized, ignoring packet: {0}", packet);
			}

			return;
		}

		// The roster request can be between the user and the server or between the
		// user and some other entity like transport
		JID connectionId = session.getConnectionId();

		if (connectionId.equals(packet.getPacketFrom())) {

			// Packet from the user, let's check where it should go
			if ((packet.getStanzaTo() != null)
					&&!session.isLocalDomain(packet.getStanzaTo().toString(), false)
						&&!session.isUserId(packet.getStanzaTo().getBareJID())) {
				results.offer(packet.copyElementOnly());

				return;
			}
		} else {

			// Packet probably to the user, let's check where it came from
			if (session.isUserId(packet.getStanzaTo().getBareJID())) {
				Packet result = packet.copyElementOnly();

				result.setPacketTo(session.getConnectionId(packet.getStanzaTo()));
				result.setPacketFrom(packet.getTo());
				results.offer(result);

				return;
			} else {

				// Hm, I do not know what to do here, should not happen
			}
		}

		try {
			if ((packet.getStanzaFrom() != null)
					&&!session.isUserId(packet.getStanzaFrom().getBareJID())) {

				// RFC says: ignore such request
				log.log(Level.WARNING,
						"Roster request ''from'' attribute doesn''t match " + "session: {0}, request: {1}",
							new Object[] { session,
						packet });

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
				if (xmlns == RosterAbstract.XMLNS_DYNAMIC) {
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
					log.log(Level.WARNING, "Unknown XMLNS for the roster plugin: {0}", packet);
				}
			}
		} catch (NotAuthorizedException e) {
			log.log(Level.WARNING, "Received roster request but user session is not authorized yet: {0}",
					packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Database problem, please contact admin:", e);
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
	@Override
	public void stopped(final XMPPResourceConnection session, final Queue<Packet> results,
			final Map<String, Object> settings) {

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

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return RosterAbstract.DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		return RosterAbstract.FEATURES;
	}

	//~--- get methods ----------------------------------------------------------

	protected RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}

	//~--- methods --------------------------------------------------------------

	protected void processGetRequest(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings)
			throws NotAuthorizedException, TigaseDBException {

		// Retrieve standard roster items.
		List<Element> ritems = roster_util.getRosterItems(session);

		// Recalculate the roster hash again with dynamic roster content
		StringBuilder roster_str = new StringBuilder(5000);

		// Retrieve all Dynamic roster elements from the roster repository
		List<Element> its = DynamicRoster.getRosterItems(session, settings);

		// There is always a chance that the same elements exist in a dynamic roster
		// and the standard user roster. Moreover, the items in the standard roster may
		// have a different presence subscription set.
		// Here we make sure they are both in sync, that is for each entry which exists
		// in both rosters we enforce 'both' subscription type for element in standard roster
		// and remove it from the dynamic roster list.
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
					log.log(Level.INFO, "JID from dynamic roster is incorrect, stringprep failed for: {0}",
							element.getAttribute("jid"));
					it.remove();
				}
			}

			// This may seem to be redundant as this call has already been made
			// but the roster could have been changed during above dynamic roster merge
			ritems = roster_util.getRosterItems(session);


			for (Element ritem : its) {
				roster_str.append(ritem.toString());
			}

		}

		for (Element ritem : ritems) {
			roster_str.append(ritem.toString());
		}
		
		roster_util.updateRosterHash(roster_str.toString(), session);

		// Check roster version hash.
		String incomingHash = packet.getElement().getAttribute("/iq/query", "ver");
		String storedHash = "";

		// If client provided hash and the server calculated hash are the same
		// return the success result and abort further roster processing.
		// No need to send the whole roster to the client.
		if (incomingHash != null) {
			storedHash = roster_util.getBuddiesHash(session);

			if (incomingHash.equals(storedHash)) {
				results.offer(packet.okResult((String) null, 0));

				return;
			}
		}

		// Send the user's standard roster first
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

		// Push the dynamic roster items now
		try {
			if ((its != null) && (its.size() > 0)) {
				ArrayDeque<Element> items = new ArrayDeque<Element>(its);

				while (items.size() > 0) {
					Element iq = new Element("iq", new String[] { "type", "id", "to" }, new String[] { "set",
							session.nextStanzaId(), session.getJID().toString() });
					iq.setXMLNS(CLIENT_XMLNS);
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
		} catch (NoConnectionIdException ex) {
			log.log(Level.WARNING,
					"Problem with roster request, no connection ID for session: {0}, request: {1}",
						new Object[] { session,
					packet });
		}
	}

	protected void processSetRequest(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, final Map<String, Object> settings)
			throws XMPPException, NotAuthorizedException, TigaseDBException {

		// Element request = packet.getElement();
		List<Element> items = packet.getElemChildren("/iq/query");

		if (items != null) {
			try {

				// RFC-3921 draft bis-03 forbids multiple items in one request
				// This however seems to make no much sense and actually was
				// requested by many users to allow for multiple items
				for (Element item : items) {
					JID buddy = JID.jidInstance(item.getAttribute("jid"));

					if (session.isUserId(buddy.getBareJID())) {
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
							// the server after the subscription is canceled
							Element pres = new Element("presence");
							pres.setXMLNS(CLIENT_XMLNS);

							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getJID().toString());
							pres.setAttribute("type", "unavailable");

							Packet pres_packet = Packet.packetInstance(pres, session.getJID(), buddy);

							// We have to set a higher priority for this particular unavailable packet
							// to make sure it is delivered before subscription cancellation
							pres_packet.setPriority(Priority.HIGH);
							results.offer(pres_packet);
							pres = new Element("presence");
							pres.setXMLNS(CLIENT_XMLNS);
							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getBareJID().toString());
							pres.setAttribute("type", "unsubscribe");
							results.offer(Packet.packetInstance(pres, session.getJID().copyWithoutResource(),
									buddy));
							pres = new Element("presence");
							pres.setXMLNS(CLIENT_XMLNS);
							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getBareJID().toString());
							pres.setAttribute("type", "unsubscribed");
							results.offer(Packet.packetInstance(pres, session.getJID().copyWithoutResource(),
									buddy));
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

						roster_util.addBuddy(session, buddy, name, gr, null);

						String type = item.getAttribute("type");

						if ((type != null) && type.equals(ANON)) {
							roster_util.setBuddySubscription(session, SubscriptionType.both, buddy);

							Element pres = (Element) session.getSessionData(XMPPResourceConnection.PRESENCE_KEY);

							if (pres == null) {
								pres = new Element("presence");
								pres.setXMLNS(CLIENT_XMLNS);
							} else {
								pres = pres.clone();
							}

							pres.setAttribute("to", buddy.toString());
							pres.setAttribute("from", session.getJID().toString());
							results.offer(Packet.packetInstance(pres, session.getJID(), buddy));
						}

						Element new_buddy = roster_util.getBuddyItem(session, buddy);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "1. New Buddy: {0}", new_buddy.toString());
						}

						if (roster_util.getBuddySubscription(session, buddy) == null) {
							roster_util.setBuddySubscription(session, SubscriptionType.none, buddy);
						}      // end of if (getBuddySubscription(session, buddy) == null)

						if (dynamicItem != null) {
							roster_util.setBuddySubscription(session, SubscriptionType.both, buddy);

							String[] itemGroups = getItemGroups(dynamicItem);

							if (itemGroups != null) {
								roster_util.addBuddyGroup(session, buddy, itemGroups);
							}
						}

						new_buddy = roster_util.getBuddyItem(session, buddy);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "2. New Buddy: {0}", new_buddy.toString());
						}

						roster_util.updateBuddyChange(session, results, new_buddy);
					}        // end of else

					// end of else
				}

				results.offer(packet.okResult((String) null, 0));
			} catch (TigaseStringprepException ex) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Buddy JID is incorrct, stringprep failed.", true));
			}
		} else {
			log.log(Level.WARNING, "No items found in roster set request: {0}", packet);
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"No items found in the roster set request", true));
		}
	}
}    // JabberIqRoster


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
