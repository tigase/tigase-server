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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.util.JIDUtils;

/**
 * Describe class Roster here.
 *
 *
 * Created: Tue Feb 21 18:05:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Roster {

	/**
   * Private logger for class instancess.
   */
  private static Logger log =	Logger.getLogger("tigase.xmpp.impl.Roster");

  protected static final String ROSTER_XMLNS = "jabber:iq:roster";
  protected static final String ROSTER = "roster";
  protected static final String GROUPS = "groups";
  protected static final String NAME = "name";
  protected static final String SUBSCRIPTION = "subscription";

	public enum PresenceType {
		out_initial,
			out_subscribe,
			out_unsubscribe,
			out_subscribed,
			out_unsubscribed,
			in_initial,
			in_subscribe,
			in_unsubscribe,
			in_subscribed,
			in_unsubscribed,
			in_probe,
			error;
	}

	public enum SubscriptionType {
		none("none", null),
			none_pending_out("none", "subscribe"),
			none_pending_in("none", null),
			none_pending_out_in("none", "subscribe"),
			to("to", null),
			to_pending_in("to", null),
			from("from", null),
			from_pending_out("from", "subscribe"),
			both("both", null),
			remove("remove", null);

		private Map<String, String> attrs = new LinkedHashMap<String, String>();

		private SubscriptionType(String subscr, String ask) {
			attrs.put("subscription", subscr);
			if (ask != null) {
				attrs.put("ask", ask);
			} // end of if (ask != null)
		}

		public Map<String, String> getSubscriptionAttr() {
			return attrs;
		}

	}

	protected static final EnumSet<SubscriptionType> TO_SUBSCRIBED =
		EnumSet.of(
			SubscriptionType.to,
			SubscriptionType.to_pending_in,
			SubscriptionType.both);
	protected static final EnumSet<SubscriptionType> FROM_SUBSCRIBED =
		EnumSet.of(
			SubscriptionType.from,
			SubscriptionType.from_pending_out,
			SubscriptionType.both);
	protected static final EnumSet<StanzaType> INITIAL_PRESENCES =
		EnumSet.of(StanzaType.available, StanzaType.unavailable);

	// Below StateTransition enum is implementation of all below tables
	// coming from RFC-3921

	//    Table 1: Recommended handling of outbound "subscribed" stanzas
	//    +----------------------------------------------------------------+
	//    |  EXISTING STATE          |  ROUTE?  |  NEW STATE               |
	//    +----------------------------------------------------------------+
	//    |  "None"                  |  no      |  no state change         |
	//    |  "None + Pending Out"    |  no      |  no state change         |
	//    |  "None + Pending In"     |  yes     |  "From"                  |
	//    |  "None + Pending Out/In" |  yes     |  "From + Pending Out"    |
	//    |  "To"                    |  no      |  no state change         |
	//    |  "To + Pending In"       |  yes     |  "Both"                  |
	//    |  "From"                  |  no      |  no state change         |
	//    |  "From + Pending Out"    |  no      |  no state change         |
	//    |  "Both"                  |  no      |  no state change         |
	//    +----------------------------------------------------------------+

	//    Table 2: Recommended handling of outbound "unsubscribed" stanzas
	//    +----------------------------------------------------------------+
	//    |  EXISTING STATE          |  ROUTE?  |  NEW STATE               |
	//    +----------------------------------------------------------------+
	//    |  "None"                  |  no      |  no state change         |
	//    |  "None + Pending Out"    |  no      |  no state change         |
	//    |  "None + Pending In"     |  yes     |  "None"                  |
	//    |  "None + Pending Out/In" |  yes     |  "None + Pending Out"    |
	//    |  "To"                    |  no      |  no state change         |
	//    |  "To + Pending In"       |  yes     |  "To"                    |
	//    |  "From"                  |  yes     |  "None"                  |
	//    |  "From + Pending Out"    |  yes     |  "None + Pending Out"    |
	//    |  "Both"                  |  yes     |  "To"                    |
	//    +----------------------------------------------------------------+

	//    Table 3: Recommended handling of inbound "subscribe" stanzas
	//    +------------------------------------------------------------------+
	//    |  EXISTING STATE          |  DELIVER?  |  NEW STATE               |
	//    +------------------------------------------------------------------+
	//    |  "None"                  |  yes       |  "None + Pending In"     |
	//    |  "None + Pending Out"    |  yes       |  "None + Pending Out/In" |
	//    |  "None + Pending In"     |  no        |  no state change         |
	//    |  "None + Pending Out/In" |  no        |  no state change         |
	//    |  "To"                    |  yes       |  "To + Pending In"       |
	//    |  "To + Pending In"       |  no        |  no state change         |
	//    |  "From"                  |  no *      |  no state change         |
	//    |  "From + Pending Out"    |  no *      |  no state change         |
	//    |  "Both"                  |  no *      |  no state change         |
	//    +------------------------------------------------------------------+

	//    Table 4: Recommended handling of inbound "unsubscribe" stanzas
	//    +------------------------------------------------------------------+
	//    |  EXISTING STATE          |  DELIVER?  |  NEW STATE               |
	//    +------------------------------------------------------------------+
	//    |  "None"                  |  no        |  no state change         |
	//    |  "None + Pending Out"    |  no        |  no state change         |
	//    |  "None + Pending In"     |  yes *     |  "None"                  |
	//    |  "None + Pending Out/In" |  yes *     |  "None + Pending Out"    |
	//    |  "To"                    |  no        |  no state change         |
	//    |  "To + Pending In"       |  yes *     |  "To"                    |
	//    |  "From"                  |  yes *     |  "None"                  |
	//    |  "From + Pending Out"    |  yes *     |  "None + Pending Out"    |
	//    |  "Both"                  |  yes *     |  "To"                    |
	//    +------------------------------------------------------------------+

	//    Table 5: Recommended handling of inbound "subscribed" stanzas
	//    +------------------------------------------------------------------+
	//    |  EXISTING STATE          |  DELIVER?  |  NEW STATE               |
	//    +------------------------------------------------------------------+
	//    |  "None"                  |  no        |  no state change         |
	//    |  "None + Pending Out"    |  yes       |  "To"                    |
	//    |  "None + Pending In"     |  no        |  no state change         |
	//    |  "None + Pending Out/In" |  yes       |  "To + Pending In"       |
	//    |  "To"                    |  no        |  no state change         |
	//    |  "To + Pending In"       |  no        |  no state change         |
	//    |  "From"                  |  no        |  no state change         |
	//    |  "From + Pending Out"    |  yes       |  "Both"                  |
	//    |  "Both"                  |  no        |  no state change         |
	//    +------------------------------------------------------------------+

	//    Table 6: Recommended handling of inbound "unsubscribed" stanzas
	//    +------------------------------------------------------------------+
	//    |  EXISTING STATE          |  DELIVER?  |  NEW STATE               |
	//    +------------------------------------------------------------------+
	//    |  "None"                  |  no        |  no state change         |
	//    |  "None + Pending Out"    |  yes       |  "None"                  |
	//    |  "None + Pending In"     |  no        |  no state change         |
	//    |  "None + Pending Out/In" |  yes       |  "None + Pending In"     |
	//    |  "To"                    |  yes       |  "None"                  |
	//    |  "To + Pending In"       |  yes       |  "None + Pending In"     |
	//    |  "From"                  |  no        |  no state change         |
	//    |  "From + Pending Out"    |  yes       |  "From"                  |
	//    |  "Both"                  |  yes       |  "From"                  |
	//    +------------------------------------------------------------------+

	// There are 2 tables missing I think in RFC-3921:

	//    Table 7: Recommended handling of outbound "subscribe" stanzas
	//    +------------------------------------------------------------------+
	//    |  EXISTING STATE          |  ROUTE?    |  NEW STATE               |
	//    +------------------------------------------------------------------+
	//    |  "None"                  |  yes       |  "None + Pending Out"    |
	//    |  "None + Pending Out"    |  no        |  no state change         |
	//    |  "None + Pending In"     |  yes       |  "None + Pending Out/In" |
	//    |  "None + Pending Out/In" |  no        |  no state change         |
	//    |  "To"                    |  no        |  no state change         |
	//    |  "To + Pending In"       |  no        |  no state change         |
	//    |  "From"                  |  yes       |  "From + Pending Out"    |
	//    |  "From + Pending Out"    |  no        |  no state change         |
	//    |  "Both"                  |  no        |  no state change         |
	//    +------------------------------------------------------------------+

	//    Table 8: Recommended handling of outbound "unsubscribe" stanzas
	//    +------------------------------------------------------------------+
	//    |  EXISTING STATE          |  ROUTE?    |  NEW STATE               |
	//    +------------------------------------------------------------------+
	//    |  "None"                  |  no        |  no state change         |
	//    |  "None + Pending Out"    |  yes       |  "None"                  |
	//    |  "None + Pending In"     |  no        |  no state change         |
	//    |  "None + Pending Out/In" |  yes       |  "None + Pending In"     |
	//    |  "To"                    |  yes       |  "None"                  |
	//    |  "To + Pending In"       |  yes       |  "None + Pending In"     |
	//    |  "From"                  |  no        |  no state change         |
	//    |  "From + Pending Out"    |  yes       |  "From"                  |
	//    |  "Both"                  |  yes       |  "From"                  |
	//    +------------------------------------------------------------------+


	public enum StateTransition {
		none(
			SubscriptionType.none,                   // Table 1.
			SubscriptionType.none,                   // Table 2.
			SubscriptionType.none_pending_in,        // Table 3.
			SubscriptionType.none,                   // Table 4.
			SubscriptionType.none,                   // Table 5.
			SubscriptionType.none,                   // Table 6.
			SubscriptionType.none_pending_out,       // Table 7.
			SubscriptionType.none                    // Table 8.
			),
			none_pending_out(
				SubscriptionType.none_pending_out,     // Table 1.
				SubscriptionType.none_pending_out,     // Table 2.
				SubscriptionType.none_pending_out_in,  // Table 3.
				SubscriptionType.none_pending_out,     // Table 4.
				SubscriptionType.to,                   // Table 5.
				SubscriptionType.none,                 // Table 6.
				SubscriptionType.none_pending_out,     // Table 7.
				SubscriptionType.none                  // Table 8.
				),
			none_pending_in(
				SubscriptionType.from,                 // Table 1.
				SubscriptionType.none,                 // Table 2.
				SubscriptionType.none_pending_in,      // Table 3.
				SubscriptionType.none,                 // Table 4.
				SubscriptionType.none_pending_in,      // Table 5.
				SubscriptionType.none_pending_in,      // Table 6.
				SubscriptionType.none_pending_out_in,  // Table 7.
				SubscriptionType.none_pending_in       // Table 8.
				),
			none_pending_out_in(
				SubscriptionType.from_pending_out,     // Table 1.
				SubscriptionType.none_pending_out,     // Table 2.
				SubscriptionType.none_pending_out_in,  // Table 3.
				SubscriptionType.none_pending_out,     // Table 4.
				SubscriptionType.to_pending_in,        // Table 5.
				SubscriptionType.none_pending_in,      // Table 6.
				SubscriptionType.none_pending_out_in,  // Table 7.
				SubscriptionType.none_pending_in       // Table 8.
				),
			to(
				SubscriptionType.to,                   // Table 1.
				SubscriptionType.to,                   // Table 2.
				SubscriptionType.to_pending_in,        // Table 3.
				SubscriptionType.to,                   // Table 4.
				SubscriptionType.to,                   // Table 5.
				SubscriptionType.none,                 // Table 6.
				SubscriptionType.to,                   // Table 7.
				SubscriptionType.none                  // Table 8.
				),
			to_pending_in(
				SubscriptionType.both,                 // Table 1.
				SubscriptionType.to,                   // Table 2.
				SubscriptionType.to_pending_in,        // Table 3.
				SubscriptionType.to,                   // Table 4.
				SubscriptionType.to_pending_in,        // Table 5.
				SubscriptionType.none_pending_in,      // Table 6.
				SubscriptionType.to_pending_in,        // Table 7.
				SubscriptionType.none_pending_in       // Table 8.
				),
			from(
				SubscriptionType.from,                 // Table 1.
				SubscriptionType.none,                 // Table 2.
				SubscriptionType.from,                 // Table 3.
				SubscriptionType.none,                 // Table 4.
				SubscriptionType.from,                 // Table 5.
				SubscriptionType.from,                 // Table 6.
				SubscriptionType.from_pending_out,     // Table 7.
				SubscriptionType.from                  // Table 8.
				),
			from_pending_out(
				SubscriptionType.from_pending_out,     // Table 1.
				SubscriptionType.none_pending_out,     // Table 2.
				SubscriptionType.from_pending_out,     // Table 3.
				SubscriptionType.none_pending_out,     // Table 4.
				SubscriptionType.both,                 // Table 5.
				SubscriptionType.from,                 // Table 6.
				SubscriptionType.from_pending_out,     // Table 7.
				SubscriptionType.from                  // Table 8.
				),
			both(
				SubscriptionType.both,                 // Table 1.
				SubscriptionType.to,                   // Table 2.
				SubscriptionType.both,                 // Table 3.
				SubscriptionType.to,                   // Table 4.
				SubscriptionType.both,                 // Table 5.
				SubscriptionType.from,                 // Table 6.
				SubscriptionType.both,                 // Table 7.
				SubscriptionType.from                  // Table 8.
				);

		private EnumMap<PresenceType, SubscriptionType> stateTransition =
			new EnumMap<PresenceType, SubscriptionType>(PresenceType.class);

		private StateTransition(
			SubscriptionType out_subscribed, SubscriptionType out_unsubscribed
			, SubscriptionType in_subscribe, SubscriptionType in_unsubscribe
			, SubscriptionType in_subscribed,	SubscriptionType in_unsubscribed
			, SubscriptionType out_subscribe,	SubscriptionType out_unsubscribe
			) {

			stateTransition.put(PresenceType.out_subscribed, out_subscribed);
			stateTransition.put(PresenceType.out_unsubscribed, out_unsubscribed);
			stateTransition.put(PresenceType.in_subscribe, in_subscribe);
			stateTransition.put(PresenceType.in_unsubscribe, in_unsubscribe);
			stateTransition.put(PresenceType.in_subscribed, in_subscribed);
			stateTransition.put(PresenceType.in_unsubscribed, in_unsubscribed);
			stateTransition.put(PresenceType.out_subscribe, out_subscribe);
			stateTransition.put(PresenceType.out_unsubscribe, out_unsubscribe);
		}

		public SubscriptionType getStateTransition(PresenceType pres_type) {
			SubscriptionType res = stateTransition.get(pres_type);
			log.finest("this="+this.toString()
				+", pres_type="+pres_type
				+", res="+res);
				return res;
		}

	}

	private static EnumMap<SubscriptionType, StateTransition>
		subsToStateMap =
		new EnumMap<SubscriptionType, StateTransition>(SubscriptionType.class);

	static {
		subsToStateMap.put(SubscriptionType.none, StateTransition.none);
		subsToStateMap.put(SubscriptionType.none_pending_out,
			StateTransition.none_pending_out);
		subsToStateMap.put(SubscriptionType.none_pending_in,
			StateTransition.none_pending_in);
		subsToStateMap.put(SubscriptionType.none_pending_out_in,
			StateTransition.none_pending_out_in);
		subsToStateMap.put(SubscriptionType.to, StateTransition.to);
		subsToStateMap.put(SubscriptionType.to_pending_in,
			StateTransition.to_pending_in);
		subsToStateMap.put(SubscriptionType.from, StateTransition.from);
		subsToStateMap.put(SubscriptionType.from_pending_out,
			StateTransition.from_pending_out);
		subsToStateMap.put(SubscriptionType.both, StateTransition.both);
	}

	public static SubscriptionType getStateTransition(
		final SubscriptionType subscription, final PresenceType presence) {
		return subsToStateMap.get(subscription).getStateTransition(presence);
	}

	private static long iq_id = 0;

	public static PresenceType getPresenceType(
		final XMPPResourceConnection session, final Packet packet)
		throws NotAuthorizedException {

		String to = packet.getElemTo();
		if (to != null) {
			to = JIDUtils.getNodeID(to);
		} // end of if (to != null)
		StanzaType type = packet.getType();
		if (type == null) {
			type = StanzaType.available;
		} else {
			if (type == StanzaType.error) {
				return PresenceType.error;
			}
		}

		if (to == null || !to.equals(session.getUserId())) {
			if (INITIAL_PRESENCES.contains(type)) {
				return PresenceType.out_initial;
			}
			if (type == StanzaType.subscribe) {
				return PresenceType.out_subscribe;
			} // end of if (type == StanzaType.subscribe)
			if (type == StanzaType.unsubscribe) {
				return PresenceType.out_unsubscribe;
			} // end of if (type == StanzaType.unsubscribe)
			if (type == StanzaType.subscribed) {
				return PresenceType.out_subscribed;
			} // end of if (type == StanzaType.subscribed)
			if (type == StanzaType.unsubscribed) {
				return PresenceType.out_unsubscribed;
			} // end of if (type == StanzaType.unsubscribed)
			// StanzaType.probe is invalid here....
		} // end of if (to == null || to.equals(session.getUserId()))

		if (to != null && to.equals(session.getUserId())) {
			if (INITIAL_PRESENCES.contains(type)) {
				return PresenceType.in_initial;
			}
			if (type == StanzaType.subscribe) {
				return PresenceType.in_subscribe;
			} // end of if (type == StanzaType.subscribe)
			if (type == StanzaType.unsubscribe) {
				return PresenceType.in_unsubscribe;
			} // end of if (type == StanzaType.unsubscribe)
			if (type == StanzaType.subscribed) {
				return PresenceType.in_subscribed;
			} // end of if (type == StanzaType.subscribed)
			if (type == StanzaType.unsubscribed) {
				return PresenceType.in_unsubscribed;
			} // end of if (type == StanzaType.unsubscribed)
			if (type == StanzaType.probe) {
				return PresenceType.in_probe;
			} // end of if (type == StanzaType.probe)
		} // end of if (to != null && !to.equals(session.getUserId()))

		return null;
	}

	public static boolean isSubscribedTo(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException {
		SubscriptionType subscr = getBuddySubscription(session, jid);
		return TO_SUBSCRIBED.contains(subscr);
	}

	public static boolean isSubscribedFrom(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException {
		SubscriptionType subscr = getBuddySubscription(session, jid);
		return FROM_SUBSCRIBED.contains(subscr);
	}

	public static String groupNode(final String buddy) {
    return ROSTER + "/" + JIDUtils.getNodeID(buddy);
  }

  public static String[] getBuddies(final XMPPResourceConnection session)
    throws NotAuthorizedException {
    return session.getDataGroups(ROSTER);
  }

  public static String[] getBuddies(final XMPPResourceConnection session,
		final EnumSet<SubscriptionType> subscrs)
    throws NotAuthorizedException {
    final String[] allBuddies = getBuddies(session);
    if (allBuddies == null) {
      return null;
    } // end of if (allBuddies == null)
    ArrayList<String> list = new ArrayList<String>();
    for (String buddy : allBuddies) {
      final SubscriptionType subs = getBuddySubscription(session, buddy);
			if (subscrs.contains(subs)) {
				list.add(buddy);
			} // end of if (subscrs.contains(subs))
    } // end of for ()
    return list.toArray(new String[list.size()]);
  }

  public static String getBuddyName(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException {
    return session.getData(groupNode(buddy), NAME, null);
  }

  public static void setBuddyName(final XMPPResourceConnection session,
		final String buddy, final String name) throws NotAuthorizedException {
    session.setData(groupNode(buddy), NAME, name);
  }

  public static void setBuddySubscription(final XMPPResourceConnection session,
    final SubscriptionType subscription, final String buddy)
		throws NotAuthorizedException {
    session.setData(groupNode(buddy), SUBSCRIPTION, subscription.toString());
  }

  public static SubscriptionType getBuddySubscription(
		final XMPPResourceConnection session,
    final String buddy) throws NotAuthorizedException {
		String subscr = session.getData(groupNode(buddy),	SUBSCRIPTION, null);
		if (subscr != null) {
			return SubscriptionType.valueOf(subscr);
		}
		return null;
  }

	public static boolean removeBuddy(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException {
		session.removeDataGroup(groupNode(jid));
		return true;
	}

	public static void addBuddy(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException {
		String nick = JIDUtils.getNodeNick(jid);
		if (nick == null) {
			nick = jid;
		}
		session.setData(groupNode(jid), NAME, nick);
    session.setData(groupNode(jid), SUBSCRIPTION,
			SubscriptionType.none.toString());
	}

	public static boolean updateBuddySubscription(
		final XMPPResourceConnection session,	final PresenceType presence,
		final String jid) throws NotAuthorizedException {
		SubscriptionType current_subscription =	getBuddySubscription(session, jid);
		log.finest("current_subscription="+current_subscription
			+" for jid="+jid);
// 		if (current_subscription == null) {
// 			current_subscription = SubscriptionType.none;
// 			addBuddy(session, jid);
// 		} // end of if (current_subscription == null)
		if (current_subscription != null) {
			final SubscriptionType new_subscription =
				getStateTransition(current_subscription, presence);
			log.finest("new_subscription="+new_subscription
				+" for presence="+presence);
			if (current_subscription != new_subscription) {
				setBuddySubscription(session, new_subscription, jid);
				return true;
			} // end of if (current_subscription != new_subscription)
		}
		return false;
	}

  public static String[] getBuddyGroups(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException {
    return session.getDataList(groupNode(buddy), GROUPS);
  }

  public static Element getBuddyItem(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException {
		SubscriptionType subscr = getBuddySubscription(session, buddy);
    if (subscr != null) {
			Element item = new Element("item");
			item.setAttribute("jid", buddy);
			item.addAttributes(subscr.getSubscriptionAttr());
			item.setAttribute("name", getBuddyName(session, buddy));
      String[] groups = getBuddyGroups(session, buddy);
      if (groups != null) {
        for (String gr : groups) {
					Element group = new Element("group");
					group.setCData(gr);
					item.addChild(group);
        } // end of for ()
      } // end of if-else
			return item;
    } // end of if
		return null;
  }

	public static void updateBuddyChange(final XMPPResourceConnection session,
		final Queue<Packet> results, final Element item)
		throws NotAuthorizedException {
		Element update = new Element("iq");
		update.setAttribute("type", StanzaType.set.toString());
		Element query = new Element("query");
		query.setXMLNS(ROSTER_XMLNS);
		query.addChild(item);
		update.addChild(query);
		for (XMPPResourceConnection conn: session.getActiveSessions()) {
			Element conn_update = update.clone();
			conn_update.setAttribute("to", conn.getJID());
			conn_update.setAttribute("id", ""+(++iq_id));
			Packet pack_update = new Packet(conn_update);
			pack_update.setTo(conn.getConnectionId());
			pack_update.setFrom(session.getJID());
			results.offer(pack_update);
		} // end of for (XMPPResourceConnection conn: sessions)
	}

} // Roster
