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

import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.StanzaType;
import tigase.xmpp.NotAuthorizedException;
import tigase.server.Packet;
import tigase.db.UserNotFoundException;
import tigase.db.NonAuthUserRepository;

import static tigase.xmpp.impl.Roster.SubscriptionType;
import static tigase.xmpp.impl.Roster.PresenceType;
import static tigase.xmpp.impl.Roster.TO_SUBSCRIBED;
import static tigase.xmpp.impl.Roster.FROM_SUBSCRIBED;

/**
 * Describe class Presence here.
 *
 *
 * Created: Wed Feb 22 07:30:03 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Presence extends XMPPProcessor
	implements XMPPProcessorIfc, XMPPStopListenerIfc {

	/**
	 * Constant <code>PRESENCE_KEY</code> is a key in temporary session data
	 * where the last presence sent by the userto server is stored,
	 * either initial presence or off-line presence before disconnecting.
	 */
	private static final String PRESENCE_KEY = "user-presence";
	/**
	 * <code>DIRECT_PRESENCE</code> is a key in temporary session data for
	 * the collection of JIDs where direct presence was sent.
	 * To all these addresses unavailable presence must be sent when user
	 * disconnects.
	 */
	private static final String DIRECT_PRESENCE = "direct-presences";

	/**
   * Private logger for class instancess.
   */
  private static Logger log =	Logger.getLogger("tigase.xmpp.impl.Presence");

	private static final String ID = "presence";
  private static final String[] ELEMENTS = {"presence"};
  private static final String[] XMLNSS = {"jabber:client"};

	public String id() { return ID; }

	public String[] supElements()
	{ return Arrays.copyOf(ELEMENTS, ELEMENTS.length); }

  public String[] supNamespaces()
	{ return Arrays.copyOf(XMLNSS, XMLNSS.length); }

	/**
	 * <code>stopped</code> method is called when user disconnects or logs-out.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 */
	public void stopped(final XMPPResourceConnection session,
		final Queue<Packet> results) {
		Element pres = (Element)session.getSessionData(PRESENCE_KEY);
		if (pres == null || pres.getAttribute("type") == null
			|| !pres.getAttribute("type").equals("unavailable")) {
			try {
				sendPresenceBroadcast(StanzaType.unavailable, session,
					FROM_SUBSCRIBED, results, null);
				updateOfflineChange(session, results);
			} catch (NotAuthorizedException e) { } // end of try-catch
		}
	}

  /**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence
	 * to all budies from roster and to all users to which direct presence
	 * was sent.
	 *
	 * @param t a <code>StanzaType</code> value
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param pres an <code>Element</code> value
	 * @exception NotAuthorizedException if an error occurs
	 */
	@SuppressWarnings({"unchecked"})
	protected void sendPresenceBroadcast(final StanzaType t,
    final XMPPResourceConnection session,
		final EnumSet<SubscriptionType> subscrs,
		final Queue<Packet> results, final Element pres)
		throws NotAuthorizedException {
    String[] buddies = Roster.getBuddies(session, subscrs);
    if (buddies != null) {
			for (String buddy: buddies) {
				sendPresence(t, buddy, session.getJID(), results, pres);
			} // end of for (String buddy: buddies)
    } // end of if (buddies == null)
		Set<String> direct_presences =
			(Set<String>)session.getSessionData(DIRECT_PRESENCE);
		if (direct_presences != null) {
			for (String buddy: direct_presences) {
				log.finest("Updating direct presence for: " + buddy);
				sendPresence(t, buddy, session.getJID(), results, pres);
			} // end of for (String buddy: buddies)
		} // end of if (direct_presence != null)
  }

	/**
	 * <code>updateOfflineChange</code> method broadcast off-line presence
	 * to all other user active resources.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected void updateOfflineChange(final XMPPResourceConnection session,
		final Queue<Packet> results)
		throws NotAuthorizedException {
		for (XMPPResourceConnection conn: session.getActiveSessions()) {
			log.finer("Update presence change to: " + conn.getJID());
			if (conn != session && conn.getResource() != null
				&& !conn.getResource().equals(session.getResource())) {
				// Send to old resource presence about new resource
				Element pres_update = new Element("presence");
				pres_update.setAttribute("from", session.getJID());
				pres_update.setAttribute("to", conn.getJID());
				pres_update.setAttribute("type", StanzaType.unavailable.toString());
				Packet pack_update = new Packet(pres_update);
				pack_update.setTo(conn.getConnectionId());
				results.offer(pack_update);
			} else {
				log.finer("Skipping presence update to: " + conn.getJID());
			} // end of else
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	/**
	 * <code>updateUserResources</code> method is used to broadcast to all
	 * <strong>other</strong> resources presence stanza from one user resource.
	 * So if new resource connects this method updates presence information about
	 * new resource to old resources and about old resources to new resource.
	 *
	 * @param presence an <code>Element</code> presence received from other users,
	 * we have to change 'to' attribute to full resource JID.
	 * @param session a <code>XMPPResourceConnection</code> value keeping
	 * connection session object.
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected void updateUserResources(final Element presence,
    final XMPPResourceConnection session, final Queue<Packet> results)
		throws NotAuthorizedException {
		for (XMPPResourceConnection conn: session.getActiveSessions()) {
			log.finer("Update presence change to: " + conn.getJID());
			if (conn != session) {
				// Send to new resource presence about old resource
				Element pres_update = presence.clone();
				pres_update.setAttribute("from", session.getJID());
				pres_update.setAttribute("to", conn.getJID());
				Packet pack_update = new Packet(pres_update);
				pack_update.setTo(conn.getConnectionId());
				results.offer(pack_update);
				Element presence_el = (Element)conn.getSessionData(PRESENCE_KEY);
				if (presence_el != null) {
					pres_update = presence_el.clone();
					pres_update.setAttribute("to", session.getJID());
					pres_update.setAttribute("from", conn.getJID());
					pack_update = new Packet(pres_update);
					pack_update.setTo(session.getConnectionId());
					results.offer(pack_update);
				}
			} else {
				log.finer("Skipping presence update to: " + conn.getJID());
			} // end of else
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	/**
	 * <code>updatePresenceChange</code> method is used to broadcast
	 * to all active resources presence stanza received from other users, like
	 * incoming avaiability presence, subscription presence and so on...
	 *
	 * @param presence an <code>Element</code> presence received from other users,
	 * we have to change 'to' attribute to full resource JID.
	 * @param session a <code>XMPPResourceConnection</code> value keeping
	 * connection session object.
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected void updatePresenceChange(final Element presence,
    final XMPPResourceConnection session, final Queue<Packet> results)
		throws NotAuthorizedException {
		for (XMPPResourceConnection conn: session.getActiveSessions()) {
			log.finer("Update presence change to: " + conn.getJID());
			// Send to old resource presence about new resource
			Element pres_update = presence.clone();
			pres_update.setAttribute("to", conn.getJID());
			Packet pack_update = new Packet(pres_update);
			pack_update.setTo(conn.getConnectionId());
			results.offer(pack_update);
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	protected void forwardPresence(final Queue<Packet> results,
		final Packet packet, final String from) {
		Element result = packet.getElement().clone();
		// Not needed anymore. Packet filter does it for all stanzas.
// 		// According to spec we must set proper FROM attribute
// 		result.setAttribute("from", from);
		log.finest("\n\nFORWARD presence: " + result.toString());
		results.offer(new Packet(result));
	}

  protected final void sendPresence(final StanzaType t, final String to,
		final String from, final Queue<Packet> results, final Element pres) {

		Element presence = null;
		if (pres == null) {
			presence = new Element("presence");
			if (t != null) {
				presence.setAttribute("type", t.toString());
			} // end of if (t != null)
			else {
				presence.setAttribute("type", StanzaType.unavailable.toString());
			} // end of if (t != null) else
		} // end of if (pres == null)
		else {
			presence = pres.clone();
		} // end of if (pres == null) else
		presence.setAttribute("to", to);
		presence.setAttribute("from", from);
		Packet packet = new Packet(presence);
		log.finest("Sending presence info: " + packet.getStringData());
		results.offer(packet);
  }

	@SuppressWarnings({"unchecked"})
	protected void addDirectPresenceJID(String jid,
		XMPPResourceConnection session ) {
		Set<String> direct_presences =
			(Set<String>)session.getSessionData(DIRECT_PRESENCE);
		if (direct_presences == null) {
			direct_presences = new HashSet<String>();
			session.putSessionData(DIRECT_PRESENCE, direct_presences);
		} // end of if (direct_presences == null)
		direct_presences.add(jid);
		log.finest("Added direct presence jid: " + jid);
	}

	@SuppressWarnings("fallthrough")
  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return;
		} // end of if (session == null)

		try {
			final String jid = session.getJID();
			PresenceType pres_type = Roster.getPresenceType(session, packet);
			if (pres_type == null) {
				log.warning("Invalid presence found: " + packet.getStringData());
				return;
			} // end of if (type == null)

			StanzaType type = packet.getType();
			if (type == null) {
				type = StanzaType.available;
			} // end of if (type == null)

			// Not needed anymore. Packet filter does it for all stanzas.
// 			// For all messages coming from the owner of this account set
// 			// proper 'from' attribute
// 			if (packet.getFrom().equals(session.getConnectionId())) {
// 				packet.getElement().setAttribute("from", session.getJID());
// 			} // end of if (packet.getFrom().equals(session.getConnectionId()))

			log.finest(pres_type + " presence found: " + packet.toString());
			boolean subscr_changed = false;
			switch (pres_type) {
			case out_initial:
				// Is it direct presence to some entity on the network?
				if (packet.getElemTo() != null) {
					// Yes this is it, send direct presence
					Element result = packet.getElement().clone();
					results.offer(new Packet(result));
					addDirectPresenceJID(packet.getElemTo(), session);
				} else {
					boolean first = false;
					if (session.getSessionData(PRESENCE_KEY) == null) {
						first = true;
					}

					// Store user presence for later time...
					// To send response to presence probes for example.
					session.putSessionData(PRESENCE_KEY, packet.getElement());

					// Parse resource priority:
					String priority = packet.getElemCData("/presence/priority");
					if (priority != null) {
						int pr = 1;
						try {
							pr = Integer.decode(priority);
						} catch (NumberFormatException e) {
							log.finer("Incorrect priority value: " + priority
								+ ", setting 1 as default.");
							pr = 1;
						}
						session.setPriority(pr);
					}

					// Send presence probes to 'to' or 'both' contacts if this is
					// availability presence
					if (first && type == StanzaType.available) {
						sendPresenceBroadcast(StanzaType.probe, session, TO_SUBSCRIBED,
							results, null);
					} // end of if (type == StanzaType.available)

					// Broadcast initial presence to 'from' or 'both' contacts
					sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
						results, packet.getElement());

					// Broadcast initial presence to other available user resources
					//				Element presence = packet.getElement().clone();
					// Already done above, don't need to set it again here
					// presence.setAttribute("from", session.getJID());
					updateUserResources(packet.getElement(), session, results);
				}
				break;
			case out_subscribe:
			case out_unsubscribe:
				if (pres_type == PresenceType.out_subscribe) {
					SubscriptionType current_subscription =
						Roster.getBuddySubscription(session, packet.getElemTo());
					if (current_subscription == null) {
						Roster.addBuddy(session, packet.getElemTo());
					} // end of if (current_subscription == null)
				}
				subscr_changed = Roster.updateBuddySubscription(session, pres_type,
					packet.getElemTo());
				if (subscr_changed) {
					Roster.updateBuddyChange(session, results,
						Roster.getBuddyItem(session, packet.getElemTo()));
				} // end of if (subscr_changed)
				// According to RFC-3921 I must forward all these kind presence
				// requests, it allows to resynchronize
				// subscriptions in case of synchronization loss
				forwardPresence(results, packet, session.getUserId());
				break;
			case out_subscribed:
			case out_unsubscribed:
				subscr_changed = Roster.updateBuddySubscription(session, pres_type,
					packet.getElemTo());
				if (subscr_changed) {
					Roster.updateBuddyChange(session, results,
						Roster.getBuddyItem(session, packet.getElemTo()));
					forwardPresence(results, packet, session.getUserId());
					sendPresence(StanzaType.available, packet.getElemTo(),
						session.getJID(), results, null);
				} // end of if (subscr_changed)
				break;
			case in_initial:
				// If other users are in 'to' or 'both' contacts, broadcast
				// their preseces to all active resources
				if (Roster.isSubscribedTo(session, packet.getElemFrom())) {
					updatePresenceChange(packet.getElement(), session, results);
				} else {
					Element elem = packet.getElement().clone();
					Packet result = new Packet(elem);
					result.setTo(session.getConnectionId());
					result.setFrom(packet.getTo());
					results.offer(result);
				}
				break;
			case in_subscribe:
				// If the buddy is already subscribed then auto-reply with sybscribed
				// presence stanza.
				if (Roster.isSubscribedFrom(session, packet.getElemFrom())) {
					sendPresence(StanzaType.subscribed, packet.getElemFrom(),
						session.getJID(), results, null);
				} else {
					SubscriptionType curr_sub =
						Roster.getBuddySubscription(session, packet.getElemFrom());
					if (curr_sub == null) {
						curr_sub = SubscriptionType.none;
						Roster.addBuddy(session, packet.getElemFrom());
					} // end of if (curr_sub == null)
					subscr_changed = Roster.updateBuddySubscription(session, pres_type,
						packet.getElemFrom());
					if (subscr_changed) {
						updatePresenceChange(packet.getElement(), session, results);
					}
				} // end of else
				break;
			case in_unsubscribe:
				subscr_changed = Roster.updateBuddySubscription(session, pres_type,
					packet.getElemFrom());
				if (subscr_changed) {
					sendPresence(StanzaType.unsubscribed, packet.getElemFrom(),
						session.getJID(), results, null);
					updatePresenceChange(packet.getElement(), session, results);
					Roster.updateBuddyChange(session, results,
						Roster.getBuddyItem(session, packet.getElemFrom()));
				}
				break;
			case in_subscribed: {
				SubscriptionType curr_sub =
					Roster.getBuddySubscription(session, packet.getElemFrom());
				if (curr_sub == null) {
					curr_sub = SubscriptionType.none;
					Roster.addBuddy(session, packet.getElemFrom());
				} // end of if (curr_sub == null)
				subscr_changed = Roster.updateBuddySubscription(session, pres_type,
					packet.getElemFrom());
				if (subscr_changed) {
					updatePresenceChange(packet.getElement(), session, results);
					Roster.updateBuddyChange(session, results,
						Roster.getBuddyItem(session, packet.getElemFrom()));
				}
			}
				break;
			case in_unsubscribed: {
				SubscriptionType curr_sub =
					Roster.getBuddySubscription(session, packet.getElemFrom());
				if (curr_sub != null) {
					subscr_changed = Roster.updateBuddySubscription(session, pres_type,
						packet.getElemFrom());
					if (subscr_changed) {
						updatePresenceChange(packet.getElement(), session, results);
						Roster.updateBuddyChange(session, results,
							Roster.getBuddyItem(session, packet.getElemFrom()));
					}
				}
			}
				break;
			case in_probe:
				SubscriptionType buddy_subscr =
					Roster.getBuddySubscription(session, packet.getElemFrom());
				if (buddy_subscr == null) {
					buddy_subscr = SubscriptionType.none;
				} // end of if (buddy_subscr == null)
				switch (buddy_subscr) {
				case none:
				case none_pending_out:
				case to:
					results.offer(Authorization.FORBIDDEN.getResponseMessage(packet,
							"Presence information is forbidden.", false));
					break;
				case none_pending_in:
				case none_pending_out_in:
				case to_pending_in:
					results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
							"You are not authorized to get presence information.", false));
					break;
				default:
					break;
				} // end of switch (buddy_subscr)
				if (Roster.isSubscribedFrom(session, packet.getElemFrom())) {
					for (XMPPResourceConnection conn: session.getActiveSessions()) {
						Element pres = (Element)conn.getSessionData(PRESENCE_KEY);
						sendPresence(null, packet.getElemFrom(), conn.getJID(),
							results, pres);
					}
				} // end of if (Roster.isSubscribedFrom(session, packet.getElemFrom()))
				break;
			case error: {
				// This is message to 'this' client probably
				Element elem = packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId());
				result.setFrom(packet.getTo());
				results.offer(result);
			}
				break;
			default:
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Request type is incorrect", false));
				break;
			} // end of switch (type)
		} // end of try
		catch (NotAuthorizedException e) {
      log.warning(
				"Received roster request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}

} // Presence