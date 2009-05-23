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
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.StanzaType;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.server.Packet;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.sys.TigaseRuntime;
import tigase.util.JIDUtils;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;

import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import static tigase.xmpp.impl.roster.RosterAbstract.PresenceType;
import static tigase.xmpp.impl.roster.RosterAbstract.TO_SUBSCRIBED;
import static tigase.xmpp.impl.roster.RosterAbstract.FROM_SUBSCRIBED;
import static tigase.xmpp.XMPPResourceConnection.PRESENCE_KEY;

/**
 * Describe class Presence here.
 *
 *
 * Created: Wed Feb 22 07:30:03 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class Presence {

	/**
	 * <code>DIRECT_PRESENCE</code> is a key in temporary session data for
	 * the collection of JIDs where direct presence was sent.
	 * To all these addresses unavailable presence must be sent when user
	 * disconnects.
	 */
	public static final String DIRECT_PRESENCE = "direct-presences";
	public static final String PRESENCE_ELEMENT_NAME = "presence";
	/**
	 * Private logger for class instancess.
	 */
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.Presence");
	protected static final String XMLNS = "jabber:client";
	private static final String[] ELEMENTS = {PRESENCE_ELEMENT_NAME};
	private static final String[] XMLNSS = {XMLNS};
	private static RosterAbstract roster_util =
					RosterFactory.getRosterImplementation(true);

	/**
	 * <code>stopped</code> method is called when user disconnects or logs-out.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 */
	@SuppressWarnings({"unchecked"})
	public static void stopped(final XMPPResourceConnection session,
					final Queue<Packet> results, final Map<String, Object> settings) {
		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {
			// According to the spec and logic actually offline status should
			// not be broadcasted if initial presence was not sent by the client.
			try {
				if (session.getSessionData(PRESENCE_KEY) != null) {
					broadcastOffline(session, results, settings);
					updateOfflineChange(session, results);
				} else {
					broadcastDirectPresences(StanzaType.unavailable, session, results,
									null);
				}
			} catch (NotAuthorizedException e) {
				// Do nothing, it may happen quite often when the user disconnects before
				// it authenticates
				} catch (TigaseDBException e) {
				log.warning("Error accessing database for offline message: " + e);
			} // end of try-catch
			if (session.isAnonymous()) {
				Set<String> direct_presences =
								(Set<String>) session.getSessionData(DIRECT_PRESENCE);
				if (direct_presences != null) {
					try {
						for (String buddy : direct_presences) {
							String peer = JIDUtils.getNodeID(buddy);
							Packet roster_update =
											new Packet(JabberIqRoster.createRosterPacket("set",
											session.nextStanzaId(), peer, peer, session.getUserId(),
											null,
											null, "remove", JabberIqRoster.ANON));
							results.offer(roster_update);
						} // end of for (String buddy: buddies)
					} catch (NotAuthorizedException e) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Anonymous user has logged out already: " + session.
											getConnectionId());
						}
					}
				} // end of if (direct_presence != null)
			}
		}
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence
	 * to all budies from roster and to all users to which direct presence
	 * was sent.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 * @exception NotAuthorizedException if an error occurs
	 * @throws TigaseDBException
	 */
	protected static void broadcastOffline(final XMPPResourceConnection session,
					final Queue<Packet> results, final Map<String, Object> settings)
					throws NotAuthorizedException, TigaseDBException {
		Element pres = (Element) session.getSessionData(PRESENCE_KEY);
		if (pres == null) {
			pres = new Element(PRESENCE_ELEMENT_NAME);
			pres.setXMLNS(XMLNS);
		}
		pres.setAttribute("type", StanzaType.unavailable.toString());
		String[] buddies = roster_util.getBuddies(session, FROM_SUBSCRIBED, true);
		buddies = DynamicRoster.addBuddies(session, settings, buddies);
		if (buddies != null) {
			Set<String> onlineJids = TigaseRuntime.getTigaseRuntime().getOnlineJids();
			for (String buddy : buddies) {
				// If buddy is a local buddy and he is offline, don't send him packet...
				String buddy_domain = JIDUtils.getNodeHost(buddy);
				if (!session.isLocalDomain(buddy_domain, false) ||
								onlineJids.contains(buddy)) {
					sendPresence(null, buddy, session.getJID(), results, pres);
				}
			} // end of for (String buddy: buddies)
		} // end of if (buddies == null)
		broadcastDirectPresences(null, session, results, pres);
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence
	 * to all budies from roster and to all users to which direct presence
	 * was sent.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 * @exception NotAuthorizedException if an error occurs
	 * @throws TigaseDBException
	 */
	protected static void broadcastProbe(final XMPPResourceConnection session,
					final Queue<Packet> results, final Map<String, Object> settings)
					throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Broadcasting probes for: " + session.getUserId());
		}
		Element presProbe = new Element(PRESENCE_ELEMENT_NAME);
		presProbe.setXMLNS(XMLNS);
		presProbe.setAttribute("type", StanzaType.probe.toString());
		String[] buddies = roster_util.getBuddies(session, TO_SUBSCRIBED, false);
		buddies = DynamicRoster.addBuddies(session, settings, buddies);
		if (buddies != null) {
			for (String buddy : buddies) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending probe to: " + buddy);
				}
				sendPresence(null, buddy, session.getUserId(), results, presProbe);
			} // end of for (String buddy: buddies)
		} // end of if (buddies == null)
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence
	 * to all budies from roster and to all users to which direct presence
	 * was sent.
	 *
	 * @param t a <code>StanzaType</code> value
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param subscrs
	 * @param results
	 * @param pres an <code>Element</code> value
	 * @param settings 
	 * @exception NotAuthorizedException if an error occurs
	 * @throws TigaseDBException
	 */
	protected static void sendPresenceBroadcast(final StanzaType t,
					final XMPPResourceConnection session,
					final EnumSet<SubscriptionType> subscrs,
					final Queue<Packet> results, final Element pres,
					final Map<String, Object> settings, boolean onlineOnly)
					throws NotAuthorizedException, TigaseDBException {
		String from = session.getJID();
		String[] buddies = roster_util.getBuddies(session, subscrs, onlineOnly);
		buddies = DynamicRoster.addBuddies(session, settings, buddies);
		if (buddies != null) {
			for (String buddy : buddies) {
				sendPresence(t, buddy, from, results, pres);
			} // end of for (String buddy: buddies)
		} // end of if (buddies == null)
		broadcastDirectPresences(t, session, results, pres);
	}

	@SuppressWarnings({"unchecked"})
	protected static void broadcastDirectPresences(StanzaType t,
					XMPPResourceConnection session, Queue<Packet> results, Element pres)
					throws NotAuthorizedException, TigaseDBException {
		Set<String> direct_presences =
						(Set<String>) session.getSessionData(DIRECT_PRESENCE);
		if (direct_presences != null && t != null && t == StanzaType.unavailable) {
			for (String buddy : direct_presences) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Updating direct presence for: " + buddy);
				}
				sendPresence(t, buddy, session.getJID(), results, pres);
			} // end of for (String buddy: buddies)
		} // end of if (direct_presence != null)
	}

	protected static void resendPendingInRequests(
					final XMPPResourceConnection session,
					final Queue<Packet> results)
					throws NotAuthorizedException, TigaseDBException {
		String[] buddies = roster_util.getBuddies(session, RosterAbstract.PENDING_IN,
						false);
		if (buddies != null) {
			for (String buddy : buddies) {
				Element presence = new Element(PRESENCE_ELEMENT_NAME);
				presence.setAttribute("type", StanzaType.subscribe.toString());
				presence.setAttribute("from", buddy);
				presence.setXMLNS(XMLNS);
				updatePresenceChange(presence, session, results);
			}
		}
	}

	/**
	 * <code>updateOfflineChange</code> method broadcast off-line presence
	 * to all other user active resources.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results 
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected static void updateOfflineChange(final XMPPResourceConnection session,
					final Queue<Packet> results)
					throws NotAuthorizedException {
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Update presence change to: " + conn.getJID());
			}
			if (conn != session && conn.getResource() != null && !conn.getResource().
							equals(session.getResource())) {
				// Send to old resource presence about new resource
				Element pres_update = new Element(PRESENCE_ELEMENT_NAME);
				pres_update.setAttribute("from", session.getJID());
				pres_update.setAttribute("to", conn.getUserId());
				pres_update.setAttribute("type", StanzaType.unavailable.toString());
				pres_update.setXMLNS(XMLNS);
				Packet pack_update = new Packet(pres_update);
				pack_update.setTo(conn.getConnectionId());
				results.offer(pack_update);
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Skipping presence update to: " + conn.getJID());
				}
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
	 * @param results 
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected static void updateUserResources(final Element presence,
					final XMPPResourceConnection session, final Queue<Packet> results)
					throws NotAuthorizedException {
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Update presence change to: " + conn.getJID());
			}
			if (conn != session && conn.isResourceSet()) {
				// Send to old resource presence about new resource
				Element pres_update = presence.clone();
				pres_update.setAttribute("from", session.getJID());
				pres_update.setAttribute("to", conn.getUserId());
				Packet pack_update = new Packet(pres_update);
				pack_update.setTo(conn.getConnectionId());
				results.offer(pack_update);
				Element presence_el = (Element) conn.getSessionData(PRESENCE_KEY);
				if (presence_el != null) {
					pres_update = presence_el.clone();
					pres_update.setAttribute("to", session.getUserId());
					pres_update.setAttribute("from", conn.getJID());
					pack_update = new Packet(pres_update);
					pack_update.setTo(session.getConnectionId());
					results.offer(pack_update);
				}
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Skipping presence update to: " + conn.getJID());
				}
			} // end of else
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	/**
	 * <code>updatePresenceChange</code> method is used to broadcast
	 * to all active resources presence stanza received from other users, like
	 * incoming avaiability presence, subscription presence and so on...
	 * Initial presences are however sent only to those resources which
	 * already have sent initial presence.
	 *
	 * @param presence an <code>Element</code> presence received from other users,
	 * we have to change 'to' attribute to full resource JID.
	 * @param session a <code>XMPPResourceConnection</code> value keeping
	 * connection session object.
	 * @param results
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected static void updatePresenceChange(final Element presence,
					final XMPPResourceConnection session, final Queue<Packet> results)
					throws NotAuthorizedException {
		boolean initial_p = (presence.getAttribute("type") == null ||
						"available".equals(presence.getAttribute("type")) ||
						"unavailable".equals(presence.getAttribute("type")));
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			if (conn.getSessionData(PRESENCE_KEY) != null || !initial_p) {
				// Update presence change only for online resources that is
				// resources which already sent initial presence.
				if (log.isLoggable(Level.FINER)) {
					log.finer("Update presence change to: " + conn.getUserId());
				}
				// Send to old resource presence about new resource
				Element pres_update = presence.clone();
				pres_update.setAttribute("to", conn.getUserId());
				Packet pack_update = new Packet(pres_update);
				pack_update.setTo(conn.getConnectionId());
				results.offer(pack_update);
			} else {
				// Ignore....
				if (log.isLoggable(Level.FINEST)) {
					log.finest(
									"Skipping update presence change for a resource which hasn't sent initial presence yet.");
				}
			}
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	protected static void forwardPresence(final Queue<Packet> results,
					final Packet packet, final String from) {
		Element result = packet.getElement().clone();
		// Not needed anymore. Packet filter does it for all stanzas.
		// According to spec we must set proper FROM attribute
		// Yes, but packet filter put full JID and we need a subscription
		// presence without resource here.
		result.setAttribute("from", from);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("\n\nFORWARD presence: " + result.toString());
		}
		results.offer(new Packet(result));
	}

	protected static void sendPresence(final StanzaType t, final String to,
					final String from, final Queue<Packet> results, final Element pres) {

		Element presence = null;
		if (pres == null) {
			presence = new Element(PRESENCE_ELEMENT_NAME);
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
		presence.setXMLNS(XMLNS);
		Packet packet = new Packet(presence);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Sending presence info: " + packet.getStringData());
		}
		results.offer(packet);
	}

	@SuppressWarnings({"unchecked"})
	protected static void addDirectPresenceJID(String jid,
					XMPPResourceConnection session) {
		Set<String> direct_presences =
						(Set<String>) session.getSessionData(DIRECT_PRESENCE);
		if (direct_presences == null) {
			direct_presences = new HashSet<String>();
			session.putSessionData(DIRECT_PRESENCE, direct_presences);
		} // end of if (direct_presences == null)
		direct_presences.add(jid);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Added direct presence jid: " + jid);
		}
	}

	@SuppressWarnings({"unchecked"})
	protected static void removeDirectPresenceJID(String jid,
					XMPPResourceConnection session) {
		Set<String> direct_presences =
						(Set<String>) session.getSessionData(DIRECT_PRESENCE);
		if (direct_presences != null) {
			direct_presences.remove(jid);
		} // end of if (direct_presences == null)
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Added direct presence jid: " + jid);
		}
	}

	protected static void outInitialAnonymous(Packet packet, XMPPResourceConnection session,
					Queue<Packet> results) throws NotAuthorizedException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Anonymous session: " + session.getUserId());
		}
		String peer = JIDUtils.getNodeID(packet.getElemTo());
		String nick = packet.getElemCData("/presence/nick");
		if (nick == null) {
			nick = session.getUserName();
		}
		Packet rost_update =
						new Packet(JabberIqRoster.createRosterPacket("set",
						session.nextStanzaId(), peer, peer, session.getUserId(),
						nick, new String[]{"Anonymous peers"}, null,
						JabberIqRoster.ANON));
		results.offer(rost_update);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Sending roster update: " + rost_update.toString());
		}
	}

	@SuppressWarnings({"unchecked", "fallthrough"})
	public static void process(final Packet packet,
					final XMPPResourceConnection session,
					final NonAuthUserRepository repo, final Queue<Packet> results,
					final Map<String, Object> settings)
					throws XMPPException {

		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {
			try {
				final String jid = session.getJID();
				PresenceType pres_type = roster_util.getPresenceType(session, packet);
				if (pres_type == null) {
					log.warning("Invalid presence found: " + packet.toString());
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

				if (log.isLoggable(Level.FINEST)) {
					log.finest(pres_type + " presence found: " + packet.toString());
				}

				// All 'in' subscription presences must have a valid from address
				switch (pres_type) {
					case in_unsubscribe:
					case in_subscribe:
					case in_unsubscribed:
					case in_subscribed:
						if (packet.getElemFrom() == null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("'in' subscription presence without valid 'from' address, dropping packet: " +
												packet.toString());
							}
							return;
						}
						if (JIDUtils.getNodeID(packet.getElemFrom()).equals(session.
										getUserId())) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("'in' subscription to myself, not allowed, returning error for packet: " +
												packet.toString());
							}
							results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
											"You can not subscribe to yourself.", false));
							return;
						}
						break;
					default:
						break;
				}


				boolean subscr_changed = false;
				switch (pres_type) {
					case out_initial:
						// Is it direct presence to some entity on the network?
						if (packet.getElemTo() != null) {
							// Yes this is it, send direct presence
							if (session.isAnonymous()) {
								outInitialAnonymous(packet, session, results);
							}
							Element result = packet.getElement().clone();
							results.offer(new Packet(result));
							// If this is unavailable presence, remove jid from Set
							// otherwise add it to the Set
							if (packet.getType() != null &&
											packet.getType() == StanzaType.unavailable) {
								removeDirectPresenceJID(packet.getElemTo(), session);
							} else {
								addDirectPresenceJID(packet.getElemTo(), session);
							}
						} else {
							boolean first = false;
							if (session.getSessionData(PRESENCE_KEY) == null) {
								first = true;
							}

							// Store user presence for later time...
							// To send response to presence probes for example.
							session.setPresence(packet.getElement());

							// Special actions on the first availability presence
							if (first && type == StanzaType.available) {
								// Send presence probes to 'to' or 'both' contacts
								broadcastProbe(session, results, settings);
								// Resend pending in subscription requests
								resendPendingInRequests(session, results);
								// Broadcast initial presence to 'from' or 'both' contacts
								sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
												results, packet.getElement(), settings, false);
							} else {
								// Broadcast initial presence to 'from' or 'both' contacts
								sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
												results, packet.getElement(), settings, true);
							}

							// Broadcast initial presence to other available user resources
							//				Element presence = packet.getElement().clone();
							// Already done above, don't need to set it again here
							// presence.setAttribute("from", session.getJID());
							updateUserResources(packet.getElement(), session, results);
						}
						break;
					case out_subscribe:
					case out_unsubscribe:
						// According to RFC-3921 I must forward all these kind presence
						// requests, it allows to resynchronize
						// subscriptions in case of synchronization loss
						forwardPresence(results, packet, session.getUserId());
						if (pres_type == PresenceType.out_subscribe) {
							SubscriptionType current_subscription =
											roster_util.getBuddySubscription(session,
											packet.getElemTo());
							if (current_subscription == null) {
								roster_util.addBuddy(session, packet.getElemTo(), null, null);
							} // end of if (current_subscription == null)
						}
						subscr_changed = roster_util.updateBuddySubscription(session,
										pres_type, packet.getElemTo());
						if (subscr_changed) {
							roster_util.updateBuddyChange(session, results,
											roster_util.getBuddyItem(session, packet.getElemTo()));
						} // end of if (subscr_changed)
						break;
					case out_subscribed:
					case out_unsubscribed:
						forwardPresence(results, packet, session.getUserId());
						String buddy = JIDUtils.getNodeID(packet.getElemTo());
						subscr_changed = roster_util.updateBuddySubscription(session,
										pres_type, buddy);
						if (subscr_changed) {
							roster_util.updateBuddyChange(session, results,
											roster_util.getBuddyItem(session, buddy));
							if (pres_type == PresenceType.out_subscribed) {
								Element presence =
												(Element) session.getSessionData(PRESENCE_KEY);
								sendPresence(StanzaType.available, buddy, session.getJID(),
												results, presence);
							} else {
								sendPresence(StanzaType.unavailable, buddy,
												session.getJID(), results, null);
							}
						} // end of if (subscr_changed)
						break;
					case in_initial:
						if (packet.getElemFrom() == null) {
							// That really happened already. It looks like a bug in tigase
							// let's try to catch it here....
							log.warning("Initial presence without from attribute set: " +
											packet.toString());
							return;
						}
						// If this is a direct presence to a resource which is already gone
						// Ignore it.
						String resource = JIDUtils.getNodeResource(packet.getElemTo());
						if (resource != null) {
							XMPPResourceConnection direct =
											session.getParentSession().getResourceForResource(resource);
							if (direct != null) {
								if (log.isLoggable(Level.FINEST)) {
									log.finest("Received direct presence from: " +
													packet.getElemFrom() + " to: " + packet.getElemTo());
								}
								// Send a direct presence to correct resource, otherwise ignore
								Element elem = packet.getElement().clone();
								Packet result = new Packet(elem);
								result.setTo(direct.getConnectionId());
								result.setFrom(packet.getTo());
								results.offer(result);
							} else {
								if (log.isLoggable(Level.FINEST)) {
									log.finest("Ignoring direct presence from: " +
													packet.getElemFrom() + " to: " + packet.getElemTo() +
													", resource gone.");
								}
							}
							break;
						}
						String presBuddy = JIDUtils.getNodeID(packet.getElemFrom());
						// If other users are in 'to' or 'both' contacts, broadcast
						// their preseces to all active resources
						if (roster_util.isSubscribedTo(session, presBuddy) ||
										(DynamicRoster.getBuddyItem(session, settings, presBuddy) != null) ||
										// This might be just unsubscribed buddy
										(roster_util.isBuddyOnline(session, presBuddy))) {
							boolean online = StanzaType.unavailable != packet.getType();
							if (log.isLoggable(Level.FINEST)) {
								log.finest("Received initial presence, setting buddy: " +
												packet.getElemFrom() + " online status to: " + online);
							}
//							if (!roster_util.isBuddyOnline(session, presBuddy)) {
//								// The buddy wasn't online before so it needs our presence too
//								for (XMPPResourceConnection conn : session.getActiveSessions()) {
//									Element pres = (Element) conn.getSessionData(PRESENCE_KEY);
//									if (pres != null) {
//										if (log.isLoggable(Level.FINEST)) {
//											log.finest("Received presence from a new buddy, sending presence to: " +
//															packet.getElemFrom());
//										}
//										sendPresence(null, JIDUtils.getNodeID(packet.getElemFrom()),
//														conn.getJID(), results, pres);
//									}
//								}
//							}
							updatePresenceChange(packet.getElement(), session, results);
							roster_util.setBuddyOnline(session, packet.getElemFrom(), online);
						} else {
							// The code below looks like a bug to me.
							// If the buddy is not subscribed I should ignore all presences
							// states from him. Commenting this out for now....
							// Well, it is not a bug and it is intentional.
							// All presences received from MUC come from not subscribed buddies
							// therefore it seems presences from unknown buddy should be passed out

							// Hm, commenting out again, direct presence is handled elsewhere
//							if (log.isLoggable(Level.FINEST)) {
//								log.finest("Received presence from unsubscribed: " + packet.getElemFrom());
//							}
//							Element elem = packet.getElement().clone();
//							Packet result = new Packet(elem);
//							result.setTo(session.getConnectionId());
//							result.setFrom(packet.getTo());
//							results.offer(result);
						}
						break;
					case in_subscribe:
						// If the buddy is already subscribed then auto-reply with sybscribed
						// presence stanza.
						if (roster_util.isSubscribedFrom(session, packet.getElemFrom())) {
							sendPresence(StanzaType.subscribed, 
											JIDUtils.getNodeID(packet.getElemFrom()),
											session.getUserId(), results, null);
						} else {
							SubscriptionType curr_sub =
											roster_util.getBuddySubscription(session, packet.
											getElemFrom());
							if (curr_sub == null) {
								curr_sub = SubscriptionType.none;
								roster_util.addBuddy(session, packet.getElemFrom(), null, null);
							} // end of if (curr_sub == null)
							roster_util.updateBuddySubscription(session, pres_type,
											packet.getElemFrom());
							updatePresenceChange(packet.getElement(), session, results);
						} // end of else
						// We can't know that actually, this might come from offline storage
						//roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
						break;
					case in_unsubscribe:
						subscr_changed = roster_util.updateBuddySubscription(session,
										pres_type,
										packet.getElemFrom());
						if (subscr_changed) {
							// No longer needed according to RFC-3921-bis5
							//sendPresence(StanzaType.unsubscribed, packet.getElemFrom(),
							//	session.getJID(), results, null);
							//updatePresenceChange(packet.getElement(), session, results);
							roster_util.updateBuddyChange(session, results,
											roster_util.getBuddyItem(session, packet.getElemFrom()));
						}
						break;
					case in_subscribed:
						 {
							 SubscriptionType curr_sub =
											 roster_util.getBuddySubscription(session, packet.
											 getElemFrom());
							 if (curr_sub == null) {
								 curr_sub = SubscriptionType.none;
								 roster_util.addBuddy(session, packet.getElemFrom(), null, null);
							 } // end of if (curr_sub == null)
							 subscr_changed = roster_util.updateBuddySubscription(session,
											 pres_type, packet.getElemFrom());
							 if (subscr_changed) {
								 //updatePresenceChange(packet.getElement(), session, results);
								 roster_util.updateBuddyChange(session, results,
												 roster_util.getBuddyItem(session, packet.getElemFrom()));
							 }
						// We can't know that actually, this might come from offline storage
							 //roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
						}
						break;
					case in_unsubscribed:
						 {
							SubscriptionType curr_sub =
											roster_util.getBuddySubscription(session, packet.
											getElemFrom());
							if (curr_sub != null) {
								subscr_changed = roster_util.updateBuddySubscription(session,
												pres_type,
												packet.getElemFrom());
								if (subscr_changed) {
									// No longer needed according to RFC-3921-bis5
									//updatePresenceChange(packet.getElement(), session, results);
									roster_util.updateBuddyChange(session, results,
													roster_util.getBuddyItem(session,
													packet.getElemFrom()));
								}
							}
						}
						break;
					case in_probe:
						SubscriptionType buddy_subscr = null;
						if (DynamicRoster.getBuddyItem(session, settings,
										packet.getElemFrom()) != null) {
							buddy_subscr = SubscriptionType.both;
						} else {
							buddy_subscr =
											roster_util.getBuddySubscription(session, packet.
											getElemFrom());
						}
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
								results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(
												packet,
												"You are not authorized to get presence information.",
												false));
								break;
							default:
								break;
						} // end of switch (buddy_subscr)
						if (roster_util.isSubscribedFrom(buddy_subscr)) {
							if (log.isLoggable(Level.FINEST)) {
								log.finest("Received probe, setting buddy: " +
												packet.getElemFrom() + " as online.");
							}
							roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
							for (XMPPResourceConnection conn : session.getActiveSessions()) {
								Element pres = (Element) conn.getSessionData(PRESENCE_KEY);
								if (pres != null) {
									sendPresence(null, JIDUtils.getNodeID(packet.getElemFrom()),
													conn.getJID(), results, pres);
									if (log.isLoggable(Level.FINEST)) {
										log.finest("Received probe, sending presence response to: " +
														packet.getElemFrom());
									}
								}
							}
						} // end of if (roster_util.isSubscribedFrom(session, packet.getElemFrom()))
						break;
					case error:
						 {
							// This is message to 'this' client probably
							// Only error responses to DIRECT presence should be sent back
							// to the client, all other should be ignored for now.
							// Later on the Tigase should remember who responded with
							// an error and don't send presence updates to this entity
							Set<String> direct_presences =
											(Set<String>) session.getSessionData(DIRECT_PRESENCE);
							if (direct_presences != null &&
											direct_presences.contains(packet.getElemFrom())) {
								Element elem = packet.getElement().clone();
								Packet result = new Packet(elem);
								result.setTo(session.getConnectionId());
								result.setFrom(packet.getTo());
								results.offer(result);
							} else {
								// Ignore for now....
							}
						}
						break;
					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
										"Request type is incorrect", false));
						break;
				} // end of switch (type)
			} // end of try
			catch (NotAuthorizedException e) {
				log.info(
								"Can not access user Roster, user session is not authorized yet: " +
								packet.getStringData());
				log.log(Level.FINEST, "presence problem...", e);
//				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
//						"You must authorize session first.", true));
			} catch (TigaseDBException e) {
				log.warning("Error accessing database for presence data: " + e);
			} // end of try-catch
		}
	}
} // Presence
