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

import tigase.sys.TigaseRuntime;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;

import static tigase.xmpp.impl.roster.RosterAbstract.FROM_SUBSCRIBED;
import static tigase.xmpp.impl.roster.RosterAbstract.PresenceType;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_BOTH;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_FROM;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_TO;
import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

//~--- JDK imports ------------------------------------------------------------

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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

	/** Field description */
	public static final String PRESENCE_ELEMENT_NAME = "presence";
	protected static final String XMLNS = "jabber:client";

	/**
	 * Private logger for class instancess.
	 */
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.Presence");
	private static final String[] XMLNSS = { XMLNS };
	private static final String[] ELEMENTS = { PRESENCE_ELEMENT_NAME };
	private static long requiredNo = 0;
	private static long requiredYes = 0;
	private static RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);
	private static TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();

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
	@SuppressWarnings({ "unchecked", "fallthrough" })
	public static void process(final Packet packet, final XMPPResourceConnection session,
														 final NonAuthUserRepository repo,
														 final Queue<Packet> results,
														 final Map<String, Object> settings)
					throws XMPPException {

		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {
			try {
				JID jid = session.getJID();
				PresenceType pres_type = roster_util.getPresenceType(session, packet);

				if (pres_type == null) {
					log.warning("Invalid presence found: " + packet);

					return;
				}    // end of if (type == null)

				StanzaType type = packet.getType();

				if (type == null) {
					type = StanzaType.available;
				}    // end of if (type == null)

				// Not needed anymore. Packet filter does it for all stanzas.
				// // For all messages coming from the owner of this account set
				// // proper 'from' attribute
				// if (packet.getFrom().equals(session.getConnectionId())) {
				// packet.getElement().setAttribute("from", session.getJID());
				// } // end of if (packet.getFrom().equals(session.getConnectionId()))
				if (log.isLoggable(Level.FINEST)) {
					log.finest(pres_type + " presence found: " + packet);
				}

				// All 'in' subscription presences must have a valid from address
				switch (pres_type) {
					case in_unsubscribe :
					case in_subscribe :
					case in_unsubscribed :
					case in_subscribed :
						if (packet.getStanzaFrom() == null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("'in' subscription presence without valid 'from' address, dropping packet: "
												 + packet);
							}

							return;
						}

						if (packet.getStanzaFrom().getBareJID().equals(session.getUserId())) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("'in' subscription to myself, not allowed, returning error for packet: "
												 + packet);
							}

							results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
											"You can not subscribe to yourself.", false));

							return;
						}

						break;

					case out_subscribe :
					case out_unsubscribe :
					case out_subscribed :
					case out_unsubscribed :

						// According to RFC 3921 draft bis-3, both source and destination
						// addresses must be BareJIDs
						// No need for that, initVars(...) takes care of that
//          packet.getElement().setAttribute("from",
//              session.getJID().copyWithoutResource().toString());
//          packet.getElement().setAttribute("to",
//              packet.getStanzaTo().copyWithoutResource().toString());
						packet.initVars(session.getJID().copyWithoutResource(),
														packet.getStanzaTo().copyWithoutResource());

						break;

					default :
						break;
				}

				boolean subscr_changed = false;

				switch (pres_type) {
					case out_initial :

						// Is it direct presence to some entity on the network?
						if (packet.getStanzaTo() != null) {

							// Yes this is it, send direct presence
							if (session.isAnonymous()) {
								outInitialAnonymous(packet, session, results);
							}

							results.offer(packet.copyElementOnly());

							// If this is unavailable presence, remove jid from Set
							// otherwise add it to the Set
							if (packet.getType() == StanzaType.unavailable) {
								removeDirectPresenceJID(packet.getStanzaTo(), session);
							} else {
								addDirectPresenceJID(packet.getStanzaTo(), session);
							}
						} else {
							boolean first = false;

							if (session.getPresence() == null) {
								first = true;
							}

							// Set a correct from attribute
							// No need for that, initVars(...) takes care of that
							// packet.getElement().setAttribute("from", jid.toString());
							packet.initVars(jid, packet.getStanzaTo());

							// Store user presence for later time...
							// To send response to presence probes for example.
							session.setPresence(packet.getElement());

							// Special actions on the first availability presence
							if (type == StanzaType.available) {
								if (first) {

									// Send presence probes to 'to' or 'both' contacts
									broadcastProbe(session, results, settings);

									// Resend pending in subscription requests
									resendPendingInRequests(session, results);

									// Broadcast initial presence to 'from' or 'both' contacts
//                sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
//                        results, packet.getElement(), settings, false);
									// These below broadcast is handled by presenceProbe method
									// to improve buddy checks....
//                sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
//                        results, packet.getElement(), settings);
								} else {

									// Broadcast initial presence to 'from' or 'both' contacts
//                sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
//                        results, packet.getElement(), settings, false);
									sendPresenceBroadcast(type,
																				session,
																				FROM_SUBSCRIBED,
																				results,
																				packet.getElement(),
																				settings);
								}
							}

							// Broadcast initial presence to other available user resources
							// Element presence = packet.getElement().clone();
							// Already done above, don't need to set it again here
							// presence.setAttribute("from", session.getJID());
							updateUserResources(packet.getElement(), session, results);
						}

						break;

					case out_subscribe :
					case out_unsubscribe :

						// According to RFC-3921 I must forward all these kind presence
						// requests, it allows to resynchronize
						// subscriptions in case of synchronization loss
						forwardPresence(results, packet, session.getJID().copyWithoutResource());

						if (pres_type == PresenceType.out_subscribe) {
							SubscriptionType current_subscription =
								roster_util.getBuddySubscription(session,
																								 packet.getStanzaTo());

							if (current_subscription == null) {
								roster_util.addBuddy(session, packet.getStanzaTo(), null, null);
							}    // end of if (current_subscription == null)
						}

						subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
										packet.getStanzaTo());

						if (subscr_changed) {
							roster_util.updateBuddyChange(session,
																						results,
																						roster_util.getBuddyItem(session,
																						packet.getStanzaTo()));
						}    // end of if (subscr_changed)

						break;

					case out_subscribed :
					case out_unsubscribed :

						// According to RFC-3921 I must forward all these kind presence
						// requests, it allows to resynchronize
						// subscriptions in case of synchronization loss
						forwardPresence(results, packet, session.getJID().copyWithoutResource());

						JID buddy = packet.getStanzaTo().copyWithoutResource();

						subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
										buddy);

						if (subscr_changed) {
							roster_util.updateBuddyChange(session,
																						results,
																						roster_util.getBuddyItem(session, buddy));

							if (pres_type == PresenceType.out_subscribed) {
								Element presence = session.getPresence();

								sendPresence(StanzaType.available, null, buddy, results, presence);
							} else {
								sendPresence(StanzaType.unavailable,
														 session.getJID(),
														 buddy,
														 results,
														 null);
							}
						}    // end of if (subscr_changed)

						break;

					case in_initial :

//          if (session.getPresence() == null) {
//            // If the user has not yet sent initial presence then ignore
//            // the probe.
//            return;
//          }
						if (packet.getStanzaFrom() == null) {

							// That really happened already. It looks like a bug in tigase
							// let's try to catch it here....
							log.warning("Initial presence without from attribute set: " + packet);

							return;
						}

						// If this is a direct presence to a resource which is already gone
						// Ignore it.
						String resource = packet.getStanzaTo().getResource();

						if ((resource != null) &&!resource.isEmpty()) {
							XMPPResourceConnection direct =
								session.getParentSession().getResourceForResource(resource);

							if (direct != null) {
								if (log.isLoggable(Level.FINEST)) {
									log.finest("Received direct presence from: " + packet.getStanzaFrom()
														 + " to: " + packet.getStanzaTo());
								}

								// Send a direct presence to correct resource, otherwise ignore
								Packet result = packet.copyElementOnly();

								result.setPacketTo(direct.getConnectionId());
								result.setPacketFrom(packet.getTo());
								results.offer(result);
							} else {
								if (log.isLoggable(Level.FINEST)) {
									log.finest("Ignoring direct presence from: " + packet.getStanzaFrom()
														 + " to: " + packet.getStanzaTo() + ", resource gone.");
								}
							}

							break;
						}

						JID presBuddy = packet.getStanzaFrom().copyWithoutResource();

						// If other users are in 'to' or 'both' contacts, broadcast
						// their preseces to all active resources
						if (roster_util.isSubscribedTo(session, presBuddy)
								|| (DynamicRoster.getBuddyItem(session, settings, presBuddy) != null)

//          ||
//          // This might be just unsubscribed buddy
//          (roster_util.isBuddyOnline(session, presBuddy))
						) {
							boolean online = StanzaType.unavailable != packet.getType();

							if (log.isLoggable(Level.FINEST)) {
								log.finest("Received initial presence, setting buddy: "
													 + packet.getStanzaFrom() + " online status to: " + online);
							}

//            if (online && !roster_util.isBuddyOnline(session, presBuddy)) {
//              // The buddy wasn't online before so it needs our presence too
//              for (XMPPResourceConnection conn : session.getActiveSessions()) {
//                try {
//                Element pres = (Element) conn.getPresence();
//                if (pres != null) {
//                  if (log.isLoggable(Level.FINEST)) {
//                    log.finest("Received presence from a new buddy, sending presence to: " +
//                            packet.getElemFrom());
//                  }
//                  sendPresence(null, conn.getJID(),
//                    JIDUtils.getNodeID(packet.getElemFrom()), results, pres);
//                }
//              }
//            }
							updatePresenceChange(packet.getElement(), session, results);

							// roster_util.setBuddyOnline(session, packet.getElemFrom(), online);
						} else {

							// The code below looks like a bug to me.
							// If the buddy is not subscribed I should ignore all presences
							// states from him. Commenting this out for now....
							// Well, it is not a bug and it is intentional.
							// All presences received from MUC come from not subscribed buddies
							// therefore it seems presences from unknown buddy should be passed out
							// Hm, commenting out again, direct presence is handled elsewhere
//            if (log.isLoggable(Level.FINEST)) {
//              log.finest("Received presence from unsubscribed: " + packet.getElemFrom());
//            }
//            Element elem = packet.getElement().clone();
//            Packet result = new Packet(elem);
//            result.setTo(session.getConnectionId());
//            result.setFrom(packet.getTo());
//            results.offer(result);
						}

						break;

					case in_subscribe :

						// If the buddy is already subscribed then auto-reply with sybscribed
						// presence stanza.
						if (roster_util.isSubscribedFrom(session, packet.getStanzaFrom())) {
							sendPresence(StanzaType.subscribed,
													 session.getJID().copyWithoutResource(),
													 packet.getStanzaFrom(),
													 results,
													 null);
						} else {
							SubscriptionType curr_sub = roster_util.getBuddySubscription(session,
											packet.getStanzaFrom());

							if (curr_sub == null) {
								curr_sub = SubscriptionType.none;
								roster_util.addBuddy(session, packet.getStanzaFrom(), null, null);
							}    // end of if (curr_sub == null)

							roster_util.updateBuddySubscription(session,
											pres_type,
											packet.getStanzaFrom());
							updatePresenceChange(packet.getElement(), session, results);
						}    // end of else

						// We can't know that actually, this might come from offline storage
						// roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
						break;

					case in_unsubscribe :
						subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
										packet.getStanzaFrom());

						if (subscr_changed) {

							// No longer needed according to RFC-3921-bis5
							// sendPresence(StanzaType.unsubscribed, session.getJID(), packet.getElemFrom(),
							// results, null);
							// updatePresenceChange(packet.getElement(), session, results);
							roster_util.updateBuddyChange(session,
																						results,
																						roster_util.getBuddyItem(session,
																						packet.getStanzaFrom()));
						}

						break;

					case in_subscribed : {
						SubscriptionType curr_sub = roster_util.getBuddySubscription(session,
										packet.getStanzaFrom());

						if (curr_sub == null) {
							curr_sub = SubscriptionType.none;
							roster_util.addBuddy(session, packet.getStanzaFrom(), null, null);
						}    // end of if (curr_sub == null)

						subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
										packet.getStanzaFrom());

						if (subscr_changed) {

							// updatePresenceChange(packet.getElement(), session, results);
							roster_util.updateBuddyChange(session,
																						results,
																						roster_util.getBuddyItem(session,
																						packet.getStanzaFrom()));
						}

						// We can't know that actually, this might come from offline storage
						// roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
					}

					break;

					case in_unsubscribed : {
						SubscriptionType curr_sub = roster_util.getBuddySubscription(session,
										packet.getStanzaFrom());

						if (curr_sub != null) {
							subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
											packet.getStanzaFrom());

							if (subscr_changed) {

								// No longer needed according to RFC-3921-bis5
								// updatePresenceChange(packet.getElement(), session, results);
								roster_util.updateBuddyChange(session,
																							results,
																							roster_util.getBuddyItem(session,
																							packet.getStanzaFrom()));
							}
						}
					}

					break;

					case in_probe :
						if (session.getPresence() == null) {

							// If the user has not yet sent initial presence then ignore
							// the probe.
							return;
						}

						SubscriptionType buddy_subscr = null;

						if (DynamicRoster.getBuddyItem(session, settings, packet.getStanzaFrom())
								!= null) {
							buddy_subscr = SubscriptionType.both;
						} else {
							buddy_subscr = roster_util.getBuddySubscription(session,
											packet.getStanzaFrom());
						}

						if (buddy_subscr == null) {
							buddy_subscr = SubscriptionType.none;
						}    // end of if (buddy_subscr == null)

						switch (buddy_subscr) {
							case none :
							case none_pending_out :
							case to :
								results.offer(Authorization.FORBIDDEN.getResponseMessage(packet,
												"Presence information is forbidden.", false));

								break;

							case none_pending_in :
							case none_pending_out_in :
							case to_pending_in :
								results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
												"You are not authorized to get presence information.", false));

								break;

							default :
								break;
						}    // end of switch (buddy_subscr)

						if (roster_util.isSubscribedFrom(buddy_subscr)) {
							if (log.isLoggable(Level.FINEST)) {
								log.finest("Received probe, setting buddy: " + packet.getStanzaFrom()
													 + " as online.");
							}

//            roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
							for (XMPPResourceConnection conn : session.getActiveSessions()) {
								try {
									Element pres = conn.getPresence();

									if (pres != null) {
										sendPresence(null,
																 null,
																 packet.getStanzaFrom().copyWithoutResource(),
																 results,
																 pres);

										if (log.isLoggable(Level.FINEST)) {
											log.finest("Received probe, sending presence response to: "
																 + packet.getStanzaFrom());
										}
									}
								} catch (Exception e) {

									// It might be quite possible that one of the user connections
									// is in state not allowed for sending presence, in such a case
									// none of user connections would receive presence.
									// This catch is to make sure all other resources receive notification.
								}
							}
						}    // end of if (roster_util.isSubscribedFrom(session, packet.getElemFrom()))

						break;

					case error : {

						// This is message to 'this' client probably
						// Only error responses to DIRECT presence should be sent back
						// to the client, all other should be ignored for now.
						// Later on the Tigase should remember who responded with
						// an error and don't send presence updates to this entity
						Set<JID> direct_presences =
							(Set<JID>) session.getSessionData(DIRECT_PRESENCE);

						if ((direct_presences != null)
								&& direct_presences.contains(packet.getStanzaFrom())) {
							Packet result = packet.copyElementOnly();

							result.setPacketTo(session.getConnectionId());
							result.setPacketFrom(packet.getTo());
							results.offer(result);
						} else {

							// Ignore for now....
						}
					}

					break;

					default :
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
										"Request type is incorrect", false));

						break;
				}    // end of switch (type)
			}      // end of try
							catch (NotAuthorizedException e) {
				log.info("Can not access user Roster, user session is not authorized yet: "
								 + packet);
				log.log(Level.FINEST, "presence problem...", e);

//      results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
//          "You must authorize session first.", true));
			} catch (TigaseDBException e) {
				log.warning("Error accessing database for presence data: " + e);
			}    // end of try-catch
		}
	}

	/**
	 * <code>stopped</code> method is called when user disconnects or logs-out.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 */
	@SuppressWarnings({ "unchecked" })
	public static void stopped(XMPPResourceConnection session, Queue<Packet> results,
														 Map<String, Object> settings) {

		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {

			// According to the spec and logic actually offline status should
			// not be broadcasted if initial presence was not sent by the client.
			try {
				if (session.getPresence() != null) {
					broadcastOffline(session, results, settings);
					updateOfflineChange(session, results);
				} else {
					broadcastDirectPresences(StanzaType.unavailable, session, results, null);
				}
			} catch (NotAuthorizedException e) {

				// Do nothing, it may happen quite often when the user disconnects before
				// it authenticates
			} catch (TigaseDBException e) {
				log.warning("Error accessing database for offline message: " + e);
			}    // end of try-catch

			if (session.isAnonymous()) {
				Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

				if (direct_presences != null) {
					try {
						for (JID buddy : direct_presences) {
							JID peer = buddy.copyWithoutResource();
							Packet roster_update =
								Packet.packetInstance(JabberIqRoster.createRosterPacket("set",
									session.nextStanzaId(), peer, peer,
									session.getJID().copyWithoutResource(), null, null, "remove",
									JabberIqRoster.ANON),
																			peer,
																			peer);

							results.offer(roster_update);
						}    // end of for (String buddy: buddies)
					} catch (NotAuthorizedException e) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Anonymous user has logged out already: "
												 + session.getConnectionId());
						}
					}
				}    // end of if (direct_presence != null)
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected static void addDirectPresenceJID(JID jid, XMPPResourceConnection session) {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if (direct_presences == null) {
			direct_presences = new LinkedHashSet<JID>();
			session.putSessionData(DIRECT_PRESENCE, direct_presences);
		}    // end of if (direct_presences == null)

		direct_presences.add(jid);

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Added direct presence jid: " + jid);
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected static void broadcastDirectPresences(StanzaType t,
					XMPPResourceConnection session, Queue<Packet> results, Element pres)
					throws NotAuthorizedException, TigaseDBException {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if ((direct_presences != null) && (t != null) && (t == StanzaType.unavailable)) {
			for (JID buddy : direct_presences) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Updating direct presence for: " + buddy);
				}

				sendPresence(t, session.getJID(), buddy, results, pres);
			}    // end of for (String buddy: buddies)
		}      // end of if (direct_presence != null)
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
	protected static void broadcastOffline(XMPPResourceConnection session,
					Queue<Packet> results, Map<String, Object> settings)
					throws NotAuthorizedException, TigaseDBException {
		Element pres = session.getPresence();

		if (pres != null) {
			pres.setAttribute("type", StanzaType.unavailable.toString());
		}

		// String from = session.getJID();
//  String[] buddies = roster_util.getBuddies(session, FROM_SUBSCRIBED, false);
		JID[] buddies = roster_util.getBuddies(session, FROM_SUBSCRIBED);

		buddies = DynamicRoster.addBuddies(session, settings, buddies);

		// Only broadcast offline presence if there are any buddies expecting it
		// and only if the user has sent initial presence before.
		if ((buddies != null) && (pres != null)) {

//    Set<String> onlineJids = TigaseRuntime.getTigaseRuntime().getOnlineJids();
			// Below code is not needed, this should be done while the presence
			// is being processed and saved
			// pres.setAttribute("from", from);
			for (JID buddy : buddies) {

				// If buddy is a local buddy and he is offline, don't send him packet...
				if (requiresPresenceSending(buddy, session)) {
					sendPresence(StanzaType.unavailable, null, buddy, results, pres);
				}

//      sendPresence(StanzaType.unavailable, from, buddy, results, pres);
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		broadcastDirectPresences(StanzaType.unavailable, session, results, pres);
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
	protected static void broadcastProbe(XMPPResourceConnection session,
					Queue<Packet> results, Map<String, Object> settings)
					throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Broadcasting probes for: " + session.getUserId());
		}

		// Probe is always broadcasted with initial presence
		Element presInit = session.getPresence();
		Element presProbe = new Element(PRESENCE_ELEMENT_NAME);

		presProbe.setXMLNS(XMLNS);
		presProbe.setAttribute("type", StanzaType.probe.toString());
		presProbe.setAttribute("from", session.getUserId().toString());

//  String[] buddies = roster_util.getBuddies(session, TO_SUBSCRIBED, false);
		// We send presence probe to TO_SUBSCRIBED and initial presence to
		// FROM_SUBSCRIBED. Most of buddies however are BOTH. So as optimalization
		// Let's process BOTH first and then TO_ONLY and FROM_ONLY separately

		JID[] buddies = roster_util.getBuddies(session, SUB_BOTH);

		buddies = DynamicRoster.addBuddies(session, settings, buddies);

		if (buddies != null) {
			for (JID buddy : buddies) {
				if (requiresPresenceSending(buddy, session)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending probe and intial to: " + buddy);
					}

					sendPresence(null, null, buddy, results, presProbe);
					sendPresence(null, null, buddy, results, presInit);
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		JID[] buddies_to = roster_util.getBuddies(session, SUB_TO);

		if (buddies_to != null) {
			for (JID buddy : buddies_to) {
				if (requiresPresenceSending(buddy, session)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending probe to: " + buddy);
					}

					sendPresence(null, null, buddy, results, presProbe);
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		JID[] buddies_from = roster_util.getBuddies(session, SUB_FROM);

		if (buddies_from != null) {
			for (JID buddy : buddies_from) {
				if (requiresPresenceSending(buddy, session)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending initial to: " + buddy);
					}

					sendPresence(null, null, buddy, results, presInit);
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)
	}

	protected static void forwardPresence(Queue<Packet> results, Packet packet, JID from) {
		Element result = packet.getElement().clone();

		// Not needed anymore. Packet filter does it for all stanzas.
		// According to spec we must set proper FROM attribute
		// Yes, but packet filter puts full JID and we need a subscription
		// presence without resource here.
		result.setAttribute("from", from.toString());

		if (log.isLoggable(Level.FINEST)) {
			log.finest("\n\nFORWARD presence: " + result.toString());
		}

		results.offer(Packet.packetInstance(result, from, packet.getStanzaTo()));
	}

	protected static void outInitialAnonymous(Packet packet,
					XMPPResourceConnection session, Queue<Packet> results)
					throws NotAuthorizedException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Anonymous session: " + session.getUserId());
		}

		JID peer = packet.getStanzaTo().copyWithoutResource();
		String nick = packet.getElemCData("/presence/nick");

		if (nick == null) {
			nick = session.getUserName();
		}

		Packet rost_update = Packet.packetInstance(JabberIqRoster.createRosterPacket("set",
													 session.nextStanzaId(), peer, peer,
													 session.getJID().copyWithoutResource(), nick,
													 new String[] { "Anonymous peers" }, null, JabberIqRoster.ANON),
						peer,
						peer);

		results.offer(rost_update);

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Sending roster update: " + rost_update);
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected static void removeDirectPresenceJID(JID jid, XMPPResourceConnection session) {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if (direct_presences != null) {
			direct_presences.remove(jid);
		}    // end of if (direct_presences == null)

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Added direct presence jid: " + jid);
		}
	}

	protected static void resendPendingInRequests(XMPPResourceConnection session,
					Queue<Packet> results)
					throws NotAuthorizedException, TigaseDBException {

//  String[] buddies = roster_util.getBuddies(session, RosterAbstract.PENDING_IN,
//          false);
		JID[] buddies = roster_util.getBuddies(session, RosterAbstract.PENDING_IN);

		if (buddies != null) {
			for (JID buddy : buddies) {
				Element presence = new Element(PRESENCE_ELEMENT_NAME);

				presence.setAttribute("type", StanzaType.subscribe.toString());
				presence.setAttribute("from", buddy.toString());
				presence.setXMLNS(XMLNS);
				updatePresenceChange(presence, session, results);
			}
		}
	}

	protected static void sendPresence(StanzaType t, JID from, JID to,
																		 Queue<Packet> results, Element pres) {
		Element presence = null;

		if (pres == null) {
			presence = new Element(PRESENCE_ELEMENT_NAME);

			if (t != null) {
				presence.setAttribute("type", t.toString());
			}    // end of if (t != null)
							else {
				presence.setAttribute("type", StanzaType.unavailable.toString());
			}    // end of if (t != null) else

			presence.setAttribute("from", from.toString());
			presence.setXMLNS(XMLNS);
		} else {
			presence = pres.clone();
		}      // end of if (pres == null) else

		presence.setAttribute("to", to.toString());

		// Optimization, especially useful for cluster mode.
		// try getting all connection IDs connection manager addresses for the
		// destination packets if possible and send packets directly without
		// going through the session manager on other node.
		// Please note! may cause unneeded behaviour if privacy lists or other
		// blocking mechanism is used
		JID[] connIds = runtime.getConnectionIdsForJid(to);

		if ((connIds != null) && (connIds.length > 0)) {
			for (JID connId : connIds) {
				try {
					Packet packet = Packet.packetInstance(presence);

					packet.setPacketTo(connId);

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending presence info: " + packet);
					}

					results.offer(packet);
				} catch (TigaseStringprepException ex) {
					log.warning("Packet stringprep addressing problem, skipping presence send: "
											+ presence);
				}
			}
		} else {
			try {

				// Connection IDs are not available so let's send it a normal way
				Packet packet = Packet.packetInstance(presence);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending presence info: " + packet);
				}

				results.offer(packet);
			} catch (TigaseStringprepException ex) {
				log.warning("Packet stringprep addressing problem, skipping presence send: "
										+ presence);
			}
		}
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
	protected static void sendPresenceBroadcast(StanzaType t,
					XMPPResourceConnection session, EnumSet<SubscriptionType> subscrs,
					Queue<Packet> results, Element pres,

//final Map<String, Object> settings, boolean onlineOnly)
	Map<String, Object> settings) throws NotAuthorizedException, TigaseDBException {

		// String from = session.getJID();
//  String[] buddies = roster_util.getBuddies(session, subscrs, onlineOnly);
		JID[] buddies = roster_util.getBuddies(session, subscrs);

		buddies = DynamicRoster.addBuddies(session, settings, buddies);

		if (buddies != null) {
			for (JID buddy : buddies) {
				if (requiresPresenceSending(buddy, session)) {
					sendPresence(t, null, buddy, results, pres);
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		broadcastDirectPresences(t, session, results, pres);
	}

	/**
	 * <code>updateOfflineChange</code> method broadcast off-line presence
	 * to all other user active resources.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected static void updateOfflineChange(XMPPResourceConnection session,
					Queue<Packet> results)
					throws NotAuthorizedException {
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			try {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Update presence change to: " + conn.getJID());
				}

				if ((conn != session) && (conn.getResource() != null)
						&&!conn.getResource().equals(session.getResource())) {

					// Send to old resource presence about new resource
					Element pres_update = new Element(PRESENCE_ELEMENT_NAME);

					pres_update.setAttribute("from", session.getJID().toString());
					pres_update.setAttribute("to", conn.getUserId().toString());
					pres_update.setAttribute("type", StanzaType.unavailable.toString());
					pres_update.setXMLNS(XMLNS);

					Packet pack_update = Packet.packetInstance(pres_update,
									session.getJID(),
									conn.getJID().copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Skipping presence update to: " + conn.getJID());
					}
				}    // end of else
			} catch (Exception e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		}        // end of for (XMPPResourceConnection conn: sessions)
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
	protected static void updatePresenceChange(Element presence,
					XMPPResourceConnection session, Queue<Packet> results)
					throws NotAuthorizedException {
		boolean initial_p = ((presence.getAttribute("type") == null)
												 || "available".equals(presence.getAttribute("type"))
												 || "unavailable".equals(presence.getAttribute("type")));

		for (XMPPResourceConnection conn : session.getActiveSessions()) {

			// Update presence change only for online resources that is
			// resources which already sent initial presence.
			if ((conn.getPresence() != null) ||!initial_p) {
				try {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Update presence change to: " + conn.getUserId());
					}

					// Send to old resource presence about new resource
					Element pres_update = presence.clone();

					pres_update.setAttribute("to", conn.getUserId().toString());

					Packet pack_update = Packet.packetInstance(pres_update,
									session.getJID(),
									conn.getJID().copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);
				} catch (Exception e) {

					// It might be quite possible that one of the user connections
					// is in state not allowed for sending presence, in such a case
					// none of user connections would receive presence.
					// This catch is to make sure all other resources receive notification.
				}
			} else {

				// Ignore....
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Skipping update presence change for a resource which hasn't sent initial presence yet.");
				}
			}
		}    // end of for (XMPPResourceConnection conn: sessions)
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
	protected static void updateUserResources(Element presence,
					XMPPResourceConnection session, Queue<Packet> results)
					throws NotAuthorizedException {
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			try {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Update presence change to: " + conn.getJID());
				}

				if ((conn != session) && conn.isResourceSet()) {

					// Send to old resource presence about new resource
					Element pres_update = presence.clone();

					pres_update.setAttribute("from", session.getJID().toString());
					pres_update.setAttribute("to", conn.getUserId().toString());

					Packet pack_update = Packet.packetInstance(pres_update,
									session.getJID(),
									conn.getJID().copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);

					Element presence_el = conn.getPresence();

					// Send to new resource last presence sent by the old resource
					if (presence_el != null) {
						pres_update = presence_el.clone();
						pres_update.setAttribute("from", conn.getJID().toString());
						pres_update.setAttribute("to", session.getUserId().toString());
						pack_update = Packet.packetInstance(pres_update, conn.getJID(),
										session.getJID().copyWithoutResource());
						pack_update.setPacketTo(session.getConnectionId());
						results.offer(pack_update);
					}
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Skipping presence update to: " + conn.getJID());
					}
				}    // end of else
			} catch (Exception e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		}        // end of for (XMPPResourceConnection conn: sessions)
	}

	private static boolean requiresPresenceSending(JID buddy,
					XMPPResourceConnection session) {
		String buddy_domain = buddy.getDomain();
		boolean result = !runtime.hasCompleteJidsInfo() ||!session.isLocalDomain(buddy_domain,
						false) || runtime.isJidOnline(buddy);

		if (log.isLoggable(Level.FINEST)) {
			if (result) {
				++requiredYes;
			} else {
				++requiredNo;
			}

			log.finest("Yes/No: " + requiredYes + " / " + requiredNo + ", buddy: " + buddy
								 + ", result=" + result + ", !runtime.hasCompleteJidsInfo()="
								 + !runtime.hasCompleteJidsInfo()
								 + ", !session.isLocalDomain(buddy_domain, false)="
								 + !session.isLocalDomain(buddy_domain, false)
								 + ", runtime.isJidOnline(buddy)=" + runtime.isJidOnline(buddy));
		}

		return result;
	}
}    // Presence


//~ Formatted in Sun Code Convention on 2010.01.16 at 07:27:44 GMT


//~ Formatted by Jindent --- http://www.jindent.com
