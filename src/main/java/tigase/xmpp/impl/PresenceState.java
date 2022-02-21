/*
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

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.impl.roster.*;
import tigase.xmpp.impl.roster.RosterAbstract.PresenceType;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.roster.RosterAbstract.FROM_SUBSCRIBED;
import static tigase.xmpp.impl.roster.RosterAbstract.TO_SUBSCRIBED;

/**
 * @author andrzej
 */
@Id(PresenceState.ID)
@Handles({@Handle(path = {PresenceAbstract.PRESENCE_ELEMENT_NAME}, xmlns = PresenceAbstract.CLIENT_XMLNS),
		  @Handle(path = {Iq.ELEM_NAME, Iq.QUERY_NAME}, xmlns = RosterAbstract.XMLNS_LOAD)})
@Bean(name = PresenceState.ID, parent = SessionManager.class, active = true)
public class PresenceState
		extends PresenceAbstract
		implements XMPPStopListenerIfc {

	/**
	 * <code>DIRECT_PRESENCE</code> is a key in temporary session data for the collection of JIDs where direct presence
	 * was sent. To all these addresses unavailable presence must be sent when user disconnects.
	 */
	public static final String DIRECT_PRESENCE = "direct-presences";

	public static final String ENABLE_ROSTER_LAZY_LOADING_KEY = "enable-roster-lazy-loading";

	public static final String EXTENDED_PRESENCE_PROCESSORS_KEY = "extended-presence-processors";

	public static final String OFFLINE_BUD_SENT = "offline-bud-sent";

	public static final String OFFLINE_RES_SENT = "offline-res-sent";

	public static final String OFFLINE_ROSTER_LAST_SEEN_PROP_KEY = "offline-roster-last-seen";

	/**
	 * key allowing setting global forwarding JID address.
	 */
	public static final String PRESENCE_GLOBAL_FORWARD = "presence-global-forward";
	protected static final String ID = "presence-state";
	private static final Logger log = Logger.getLogger(PresenceState.class.getCanonicalName());
	private static final long MAX_DIRECT_PRESENCES_NO = 1000;
	private static final String[] PRESENCE_C_PATH = {PRESENCE_ELEMENT_NAME, "c"};
	private static final Set<StanzaType> TYPES = new HashSet<>(
			Arrays.asList(StanzaType.available, StanzaType.unavailable, StanzaType.probe, StanzaType.error,
						  StanzaType.result, null));
	protected static int HIGH_PRIORITY_PRESENCES_NO = 10;
	@Inject(nullAllowed = true)
	private List<ExtendedPresenceProcessorIfc> extendedPresenceProcessors = new ArrayList<>();
	@ConfigField(desc = "Send last seen infomations for matching clients", alias = OFFLINE_ROSTER_LAST_SEEN_PROP_KEY)
	private String[] offlineRosterLastSeen = null;
	@ConfigField(desc = "Forward all presences to following JID", alias = PRESENCE_GLOBAL_FORWARD)
	private JID presenceGLobalForward = null;
	@ConfigField(desc = "Enable roster lazy loading", alias = ENABLE_ROSTER_LAZY_LOADING_KEY)
	private boolean rosterLazyLoading = true;
	private long usersStatusChanges = 0;

	/**
	 * Add JID to collection of JIDs to which direct presence was sent. To all these addresses unavailable presence must
	 * be sent when user disconnects.
	 *
	 * @param jid to which direct presence was sent.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 */
	@SuppressWarnings({"unchecked"})
	public static void addDirectPresenceJID(JID jid, XMPPResourceConnection session) {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if (direct_presences == null) {
			direct_presences = new LinkedHashSet<JID>(10);
			session.putSessionData(DIRECT_PRESENCE, direct_presences);
		}    // end of if (direct_presences == null)
		if (direct_presences.size() < MAX_DIRECT_PRESENCES_NO) {
			direct_presences.add(jid);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Added direct presence jid: {0}", jid);
		}
	}

	/**
	 * {@code broadcastDirectPresences} broadcast a direct Presence from provided {@code pres} {@link Element} object to
	 * the collection of JIDs stored in temporary session data under key {@code DIRECT_PRESENCE}.
	 *
	 * @param t specifies type of the presence to be send.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param pres an Object of type {@link Element} holding Presence stanza to be sent.
	 *
	 */
	@SuppressWarnings({"unchecked"})
	protected static void broadcastDirectPresences(StanzaType t, XMPPResourceConnection session, Queue<Packet> results,
												   Element pres) throws NotAuthorizedException, TigaseDBException {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if ((direct_presences != null) && (t != null) && (t == StanzaType.unavailable)) {
			for (JID buddy : direct_presences) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Updating direct presence for: {0}", buddy);
				}

				Packet pack = sendPresence(t, session.getJID(), buddy, results, pres);

				pack.setPriority(Priority.LOW);
			}    // end of for (String buddy: buddies)
		}      // end of if (direct_presence != null)
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all buddies from roster and to all users
	 * to which direct presence was sent.
	 *
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param roster_util instance of class implementing {@link RosterAbstract}.
	 *
	 * @throws NotAuthorizedException if an error occurs
	 */
	public static void broadcastOffline(XMPPResourceConnection session, Queue<Packet> results,
										Map<String, Object> settings, RosterAbstract roster_util)
			throws NotAuthorizedException, TigaseDBException {

		// Preventing sending offline notifications more than once
		if (session.getSessionData(OFFLINE_BUD_SENT) != null) {
			return;
		}
		session.putSessionData(OFFLINE_BUD_SENT, OFFLINE_BUD_SENT);

		Element pres = session.getPresence();

		if (pres != null) {
			sendPresenceBroadcast(StanzaType.unavailable, session, FROM_SUBSCRIBED, results, pres, settings,
								  roster_util);
		} else {
			broadcastDirectPresences(StanzaType.unavailable, session, results, pres);
		}
	}

	/**
	 * Remove JID from collection of JIDs to which direct presence was sent.
	 *
	 * @param jid to which direct presence was sent.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 */
	@SuppressWarnings({"unchecked"})
	public static void removeDirectPresenceJID(JID jid, XMPPResourceConnection session) {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if (direct_presences != null) {
			direct_presences.remove(jid);
		}    // end of if (direct_presences == null)
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Added direct presence jid: {0}", jid);
		}
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all buddies from roster and to all users
	 * to which direct presence was sent. Before sending presence method calls {@code  requiresPresenceSending()}
	 * performing, if configured, both system and roster check to verify whether presence needs to be sent.
	 *
	 * @param t specifies type of the presence to be send.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param subscrs an {@code EnumSet<SubscriptionType>} holding all {@link SubscriptionType} to which a Presence
	 * should be broadcast.
	 * @param pres an Object of type {@link Element} holding Presence stanza to be sent.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param roster_util instance of class implementing {@link RosterAbstract}.
	 *
	 * @throws NotAuthorizedException if an error occurs
	 */
	public static void sendPresenceBroadcast(StanzaType t, XMPPResourceConnection session,
											 EnumSet<RosterAbstract.SubscriptionType> subscrs, Queue<Packet> results,
											 Element pres, Map<String, Object> settings, RosterAbstract roster_util)
			throws NotAuthorizedException, TigaseDBException {

		// Direct presence if any should be sent first
		broadcastDirectPresences(t, session, results, pres);

		RosterAbstract roster = roster_util;

		if (roster == null) {
			roster = RosterFactory.getRosterImplementation(true);
		}

		JID[] buddies = roster.getBuddies(session, subscrs);

		try {
			buddies = DynamicRoster.addBuddies(session, settings, buddies, subscrs);
		} catch (RosterRetrievingException | RepositoryAccessException ex) {

			// Ignore, handled in the JabberIqRoster code
		}
		if (buddies != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, session.getBareJID() + " | Buddies found: " + Arrays.toString(buddies));
			}

			Priority pack_priority = Priority.PRESENCE;
			int pres_cnt = 0;

			for (JID buddy : buddies) {
				if (requiresPresenceSending(roster, buddy, session, false)) {
					Packet pack = sendPresence(t, session.getJID(), buddy, results, pres);

					if (pres_cnt == HIGH_PRIORITY_PRESENCES_NO) {
						++pres_cnt;
						pack_priority = Priority.LOWEST;
					}
					if (pack != null) {
						pack.setPriority(pack_priority);
						roster.setPresenceSent(session, buddy, true);
					}
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Not sending presence to buddy: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No buddies found!!!!");
			}
		}
	}

	/**
	 * <code>updateOfflineChange</code> method broadcast off-line presence to all other user active resources.
	 *
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 *
	 * @throws NotAuthorizedException if an error occurs
	 */
	protected static void updateOfflineChange(XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException {

		// Preventing sending offline notifications more than once
		if (session.getSessionData(OFFLINE_RES_SENT) != null) {
			return;
		}
		session.putSessionData(OFFLINE_RES_SENT, OFFLINE_RES_SENT);
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			try {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Update presence change to: {0}", conn.getJID());
				}
				if ((conn != session) && (conn.isResourceSet()) && !conn.getResource().equals(session.getResource())) {

					// Send to old resource presence about new resource
					Element pres_update = new Element(PRESENCE_ELEMENT_NAME);

					pres_update.setAttribute("type", StanzaType.unavailable.toString());
					pres_update.setXMLNS(XMLNS);

					// accroding to RFC1621, 4.5.2.  Server Processing of Outbound Unavailable Presence
					// this presece packet should be addressed to fullJID
					Packet pack_update = Packet.packetInstance(pres_update, session.getJID(), conn.getJID());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Skipping presence update to: {0}", conn.getJID());
					}
				}    // end of else
			} catch (NoConnectionIdException | NotAuthorizedException e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		}        // end of for (XMPPResourceConnection conn: sessions)
	}

	@Override
	public Set<StanzaType> supTypes() {
		return TYPES;
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * Performs processing of <em>presence</em> packets and calls different methods for particular {@link PresenceType}
	 */
	@SuppressWarnings({"unchecked", "fallthrough"})
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is null, ignoring packet: {0}", packet);
			}

			return;
		}    // end of if (session == null)
		if (!session.isAuthorized()) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is not authorized, ignoring packet: {0}", packet);
			}

			return;
		}

		if (packet.getElemName() == Iq.ELEM_NAME) {
			// here we process results of roster loading process requests
			boolean finishProcessing = true;
			switch (packet.getType()) {
				case result:
					synchronized (session) {
						Element presEl = session.getPresence();
						if (presEl != null) {
							session.removeSessionData(XMPPResourceConnection.PRESENCE_KEY);
							presEl.removeAttribute("from");
							presEl.removeAttribute("to");
							Packet pres = Packet.packetInstance(presEl, packet.getStanzaFrom(), packet.getStanzaTo());
							pres.setPacketFrom(packet.getPacketFrom());
							pres.setPacketTo(packet.getPacketTo());
							try {
								processOutInitial(pres, session, results, settings,
												  RosterAbstract.PresenceType.out_initial);
							} catch (NotAuthorizedException e) {
								log.log(Level.INFO,
										"Can not access user Roster, user session is not authorized yet: {0}", packet);
								log.log(Level.FINEST, "presence problem...", e);
							} catch (TigaseDBException e) {
								log.log(Level.WARNING, "Error accessing database for presence data: {0}", e);
							}    // end of try-catch
						}
					}
					break;
				default:
					// ignore this
					break;
			}
			if (finishProcessing) {
				return;
			}
		}

		synchronized (session) {
			try {
				RosterAbstract.PresenceType pres_type = roster_util.getPresenceType(session, packet);

				if (pres_type == null) {
					log.log(Level.INFO, "Invalid presence found: {0}", packet);

					return;
				}    // end of if (type == null)
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0} | {1} presence found: {2}",
							new Object[]{session.getBareJID().toString(), pres_type, packet});
				}

				switch (pres_type) {
					case out_initial:
						processOutInitial(packet, session, results, settings, pres_type);

						break;
					case in_initial:
						processInInitial(packet, session, results, settings, pres_type);

						break;
					case in_probe:
						if (session.getPresence() == null) {

							// If the user has not yet sent initial presence then ignore the
							// probe.
							return;
						}
						processInProbe(packet, session, results, settings, pres_type);

						break;

					case out_probe:
						forwardPresence(results, packet, session.getJID());
						break;

					case error:
						processError(packet, session, results, settings, pres_type);

						break;

					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Request type is incorrect",
																				   false));

						break;
				}    // end of switch (type)
			} catch (NotAuthorizedException e) {
				log.log(Level.INFO, "Can not access user Roster, user session is not authorized yet: {0}", packet);
				log.log(Level.FINEST, "presence problem...", e);
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Error accessing database for presence data: {0}", e);
			}    // end of try-catch
		}
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {

		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {

			// According to the spec and logic actually offline status should
			// not be broadcasted if initial presence was not sent by the client.
			try {
				if (session.getPresence() != null) {

					// If this was called without sending unavailable presence
					// we have to generate it on our own.
					Element pres = session.getPresence();

					if (!StanzaType.unavailable.toString().equals(pres.getAttributeStaticStr(Packet.TYPE_ATT))) {
						pres.setAttribute(Packet.TYPE_ATT, StanzaType.unavailable.toString());
						session.setPresence(pres);
					}
					broadcastOffline(session, results, settings, roster_util);
					updateOfflineChange(session, results);
				} else {
					broadcastDirectPresences(StanzaType.unavailable, session, results, null);
				}
				roster_util.logout(session);
			} catch (NotAuthorizedException e) {

				// Do nothing, it may happen quite often when the user disconnects
				// before it authenticates.
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Error accessing database for offline message: ", e);
			}    // end of try-catch
		}
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(id(), USERS_STATUS_CHANGES, usersStatusChanges, Level.INFO);
	}

	public void rebroadcastPresence(XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException {
		if (session.getPresence() == null) {
			// user has not sent initial presence yet, ignore
			return;
		}

		Element presence = session.getPresence().clone();

		for (ExtendedPresenceProcessorIfc processor : extendedPresenceProcessors) {
			Element extendContent = processor.extend(presence, session, results);
			if (extendContent != null) {
				// avoid duplicate
				Element child = presence.getChild(extendContent.getName(), extendContent.getXMLNS());
				if (child != null) {
					presence.removeChild(child);
				}
				presence.addChild(extendContent);
			}
		}

		sendPresenceBroadcast(StanzaType.available, session, FROM_SUBSCRIBED, results, presence, null, getRosterUtil());

		updateUserResources(presence, session, results, false);

//		sendPresenceBroadcast( StanzaType.get, session, SUB_TO, results, presence, null, null );
	}

	/**
	 * Sends out all pending subscription request during user log-in.
	 *
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 *
	 */
	public void resendPendingInRequests(XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException {
		JID[] buddies = roster_util.getBuddies(session, RosterAbstract.PENDING_IN);

		if (buddies != null) {
			for (JID buddy : buddies) {
				Element presence = new Element(PRESENCE_ELEMENT_NAME);

				presence.setAttribute("type", StanzaType.subscribe.toString());
				presence.setXMLNS(XMLNS);

				Packet pres = Packet.packetInstance(presence, buddy, null);

				updatePresenceChange(pres, session, results);
			}
		}
	}

	/**
	 * Process presence stanza of type Error. Allows errors sent from server to user and ignore presence errors sent
	 * from the user.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param presenceType specifies type of the presence
	 *
	 */
	protected void processError(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
								Map<String, Object> settings, RosterAbstract.PresenceType presenceType)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {

		// Strategy change.
		// Now we allow all error presences sent to the user, but we just ignore
		// presence errors sent from the user
		if (session.isUserId(packet.getStanzaTo().getBareJID())) {
			Packet result = packet.copyElementOnly();

			result.setPacketTo(session.getConnectionId());
			result.setPacketFrom(packet.getTo());
			results.offer(result);
		} else {

			// Ignore....
		}
	}

	/**
	 * Method is responsible for processing incoming initial presence (i.e. in the receivers session manager).
	 * <br>
	 * It validates the packet (whether from is present or if it's a direct presence to existing resource) and
	 * subsequently set received presence for the contact that sent it.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param presenceType specifies type of the presence.
	 *
	 */
	protected void processInInitial(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
									Map<String, Object> settings, RosterAbstract.PresenceType presenceType)
			throws NoConnectionIdException, NotAuthorizedException, TigaseDBException {
		if (packet.getStanzaFrom() == null) {

			// That really happened already. It looks like a bug in tigase
			// let's try to catch it here....
			log.log(Level.WARNING, "Initial presence without from attribute set: {0}", packet);

			return;
		}

		// If this is a direct presence to a resource which is already gone
		// Ignore it.
		String resource = packet.getStanzaTo().getResource();

		if ((resource != null) && !resource.isEmpty()) {
			XMPPResourceConnection direct = session.getParentSession().getResourceForResource(resource);

			if (direct != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received direct presence from: {0} to: {1}",
							new Object[]{packet.getStanzaFrom(), packet.getStanzaTo()});
				}

				// Send a direct presence to correct resource, otherwise ignore
				Packet result = packet.copyElementOnly();

				result.setPacketTo(direct.getConnectionId());
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Ignoring direct presence from: {0} to: {1}, resource gone.",
							new Object[]{packet.getStanzaFrom(), packet.getStanzaTo()});
				}
			}

			return;
		}

		boolean online = StanzaType.unavailable != packet.getType();

		buddyOnline(session, packet.getStanzaFrom(), results, online);
		if (session.getPresence() == null) {

			// Just ignore, this user does not want to receive presence updates
			return;
		}

		JID presBuddy = packet.getStanzaFrom().copyWithoutResource();

		// If other users are in 'to' or 'both' contacts, broadcast
		// their presences to all active resources
		Element dynItem;

		try {
			dynItem = DynamicRoster.getBuddyItem(session, settings, presBuddy);
		} catch (RosterRetrievingException | RepositoryAccessException ex) {
			dynItem = null;
		}
		if (roster_util.isSubscribedTo(session, presBuddy) || (dynItem != null)) {
			RosterElement rel = roster_util.getRosterElement(session, presBuddy);

			if (rel != null) {
				rel.setLastSeen(System.currentTimeMillis());
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Received initial presence, setting buddy: {0} online status to: {1}",
						new Object[]{packet.getStanzaFrom(), online});
			}
		}
		updatePresenceChange(packet, session, results);
	}

	/**
	 * Method is responsible for processing incoming presence probe (i.e. in the receivers session manager).
	 * <br>
	 * It validates whether the packet comes from a contact that has correct subscription and responds with presence of
	 * all user's resources presences.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param presenceType specifies type of the presence.
	 *
	 */
	protected void processInProbe(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
								  Map<String, Object> settings, RosterAbstract.PresenceType presenceType)
			throws NotAuthorizedException, TigaseDBException, PacketErrorTypeException {
		RosterAbstract.SubscriptionType buddy_subscr = null;
		Element dynItem;

		try {
			dynItem = DynamicRoster.getBuddyItem(session, settings, packet.getStanzaFrom());
		} catch (RosterRetrievingException | RepositoryAccessException ex) {
			dynItem = null;
		}
		if (dynItem != null) {
			buddy_subscr = RosterAbstract.SubscriptionType.both;
		} else {
			buddy_subscr = roster_util.getBuddySubscription(session, packet.getStanzaFrom());
		}
		if (buddy_subscr == null) {
			buddy_subscr = RosterAbstract.SubscriptionType.none;
		}    // end of if (buddy_subscr == null)
		if (roster_util.isSubscribedFrom(buddy_subscr)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Received probe, setting buddy: {0} as online.", packet.getStanzaFrom());
			}

			// Probe is usually without resource anyway, so probably more correct approach would
			// be to remove below.
			if (packet.getStanzaFrom().getResource() != null) {
				roster_util.setOnline(session, packet.getStanzaFrom(), true);
			}
			for (XMPPResourceConnection conn : session.getActiveSessions()) {
				try {
					Element pres = conn.getPresence();

					if (pres != null) {
						JID to = probeFullJID ? packet.getStanzaFrom(): packet.getStanzaFrom().copyWithoutResource();
						sendPresence(null, conn.getJID(), to, results, pres);
						roster_util.setPresenceSent(session, packet.getStanzaFrom(), true);
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Received probe, sending presence response to: {0}", to);
						}
					}
				} catch (NotAuthorizedException | TigaseDBException e) {

					// It might be quite possible that one of the user connections
					// is in state not allowed for sending presence, in such a case
					// none of user connections would receive presence.
					// This catch is to make sure all other resources receive
					// notification.
				}
			}
		} else {
			// acording to spec 4.3.2. Server Processing of Inbound Presence Probe
			// http://xmpp.org/rfcs/rfc6121.html#presence-probe-inbound
			// If the user's bare JID is in the contact's roster with a subscription
			// state other
			// than "From", "From + Pending Out", or "Both", then the contact's server
			// SHOULD return a presence stanza of type "unsubscribed"
			if (!isAllowedForPresenceProbe(session, packet.getStanzaFrom())) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Received probe, users bare JID: {0} is not in the roster. Responding with unsubscribed",
							packet.getStanzaFrom().getBareJID());
				}
				sendPresence(StanzaType.unsubscribed, session.getBareJID(), packet.getStanzaFrom().getBareJID(),
							 results, null);
			} else {
				// However, if a server receives a presence probe from a configured
				// domain of the server itself or another such trusted service, it MAY
				// provide presence information about the user to that entity.
				for (XMPPResourceConnection conn : session.getActiveSessions()) {
					Element pres = conn.getPresence();

					if (pres != null) {
						sendPresence(null, conn.getJID(), packet.getStanzaFrom(), results, pres);
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Received probe, sending presence response to: {0}",
									packet.getStanzaFrom());
						}
					}
				}
			}
		}    // end of if (roster_util.isSubscribedFrom(session, packet.getElemFrom()))
	}

	/**
	 * Method is responsible for processing outgoing initial presence (i.e. in the sender session manager).
	 * <br>
	 * Process packet accordingly whether it's a direct presence (forward it, add to proper collection of JIDs to which
	 * a direct presence has been sent) or regular presence. THe latter causes properly address the packet, store
	 * presence within session data for subsequent use, and for the first availability presence (in case there is no
	 * prior presence stored in user session data) server sends probes to all contacts and pushes out all pending
	 * subscription request or (if there i already presence stored in session data) broadcast presence update to
	 * contacts.
	 * <br>
	 * If there is a JID forwarding set up, presence is also forwarded to configured JID.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param type specifies type of the presence.
	 *
	 */
	protected void processOutInitial(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
									 Map<String, Object> settings, RosterAbstract.PresenceType type)
			throws NotAuthorizedException, TigaseDBException {

		// Is it a direct presence to some entity on the network?
		if (packet.getStanzaTo() != null) {

			Packet presence = packet.copyElementOnly();
			Element presenceEl = presence.getElement();
			for (ExtendedPresenceProcessorIfc processor : extendedPresenceProcessors) {
				Element extendContent = processor.extend(presenceEl, session, results);
				if (extendContent != null) {
					presenceEl.addChild(extendContent);
				}
			}
			// Yes this is it, send direct presence
			results.offer(presence);

			// If this is unavailable presence, remove jid from Set
			// otherwise add it to the Set
			if (packet.getType() == StanzaType.unavailable) {
				removeDirectPresenceJID(packet.getStanzaTo(), session);
			} else {
				addDirectPresenceJID(packet.getStanzaTo(), session);
			}
		} else {
			++usersStatusChanges;

			boolean first = false;

			if (session.getPresence() == null) {
				first = true;
			}
			Packet resultPacket = packet.copyElementOnly();
			resultPacket.initVars(session.getJID(), packet.getStanzaTo());
			final Element presenceEl = resultPacket.getElement();

			for (ExtendedPresenceProcessorIfc processor : extendedPresenceProcessors) {
				Element extendContent = processor.extend(presenceEl, session, results);
				if (extendContent != null) {
					presenceEl.addChild(extendContent);
				}
			}

			// Store user presence for later time...
			// To send response to presence probes for example.
			session.setPresence(presenceEl);

			// here we need to check if roster is loaded
			if (!rosterLazyLoading || roster_util.isRosterLoaded(session)) {
				// if it is already loaded then continue processing
				// Special actions on the first availability presence
				if ((packet.getType() == null) || (packet.getType() == StanzaType.available)) {
					session.removeSessionData(OFFLINE_BUD_SENT);
					session.removeSessionData(OFFLINE_RES_SENT);
					if (first) {
						try {
							sendRosterOfflinePresence(session, results);
						} catch (NotAuthorizedException | TigaseDBException | NoConnectionIdException ex) {
							log.log(Level.INFO, "Experimental code throws exception: ", ex);
						}

						// Send presence probes to 'to' or 'both' contacts
						broadcastProbe(session, results, settings);

						// Resend pending in subscription requests
						resendPendingInRequests(session, results);
					} else {
						// Broadcast initial presence to 'from' or 'both' contacts
						sendPresenceBroadcast(StanzaType.available, session, FROM_SUBSCRIBED, results, presenceEl,
											  settings, roster_util);
					}

					// Broadcast initial presence to other available user resources
					updateUserResources(presenceEl, session, results, first);
				} else {
					stopped(session, results, settings);
				}

				// Presence forwarding
				JID forwardTo = session.getDomain().getPresenceForward();

				if (forwardTo == null) {
					forwardTo = presenceGLobalForward;
				}
				if (forwardTo != null) {
					sendPresence(null, session.getJID(), forwardTo, results, presenceEl);
				}
			} else {
				// if roster is not yet loaded we need to trigger roster load by Roster plugin
				Element iq = new Element(Iq.ELEM_NAME, new String[]{"type"}, new String[]{"set"});
				Element query = new Element(Iq.QUERY_NAME, new String[]{Iq.XMLNS_ATT},
											new String[]{RosterAbstract.XMLNS_LOAD});
				iq.addChild(query);
				Packet loadCmd = Packet.packetInstance(iq, packet.getStanzaFrom(), packet.getStanzaTo());
				loadCmd.setPacketFrom(packet.getPacketFrom());
				loadCmd.setPacketTo(packet.getPacketTo());
				results.add(loadCmd);
			}
		}
	}

	/**
	 * Method sends back presence to contact while it becomes online (i.e. during processing of incoming initial
	 * presence of the contact/buddy)
	 *
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param buddy {@link JID} of a roster element for which an online state will be set
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param online set whether given contact is online or offline
	 *
	 */
	protected void buddyOnline(XMPPResourceConnection session, JID buddy, Queue<Packet> results, boolean online)
			throws NotAuthorizedException, TigaseDBException {
		roster_util.setOnline(session, buddy, online);
		if (online && skipOffline && !roster_util.presenceSent(session, buddy) &&
				roster_util.isSubscribedFrom(session, buddy)) {
			Element pres = session.getPresence();

			if (pres != null) {
				sendPresence(null, session.getJID(), buddy, results, pres);
				roster_util.setPresenceSent(session, buddy, true);
			}
		}
	}

	/**
	 * Method sends server generated presence unavailable for all buddies from the roster with a custom status message.
	 *
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 *
	 */
	@SuppressWarnings("empty-statement")
	protected void sendRosterOfflinePresence(XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {
		if (offlineRosterLastSeen == null) {
			log.finest("No clients specified in config, skipping...");

			return;
		}

		Element pres = session.getPresence();

		if (pres == null) {
			log.finest("Presence not set yet, skipping...");

			return;
		}

		String node = pres.getAttributeStaticStr(PRESENCE_C_PATH, "node");

		if (node == null) {
			log.finest("Presence node not set, skipping...");

			return;
		}

		boolean validClient = false;
		int i = 0;

		if (offlineRosterLastSeen.length > 0 && !offlineRosterLastSeen[0].equals("*")) {
			while ((i < offlineRosterLastSeen.length) && !(validClient |= node.contains(offlineRosterLastSeen[i++]))) {
				;
			}
			if (!validClient) {
				log.finest("Client does not match, skipping...");

				return;
			}
		} else {
			// enabled for all clients
		}

		JID[] buddies = roster_util.getBuddies(session, TO_SUBSCRIBED);

		if (buddies != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Buddies found: {0}", Arrays.toString(buddies));
			}

			Priority pack_priority = Priority.PRESENCE;
			int pres_cnt = 0;

			for (JID buddy : buddies) {
				List<Element> children = roster_util.getCustomChildren(session, buddy);

				if (children != null && !children.isEmpty()) {
					Packet pack = sendPresence(StanzaType.unavailable, buddy, session.getJID(), results, null);

					if (pres_cnt == HIGH_PRIORITY_PRESENCES_NO) {
						++pres_cnt;
						pack_priority = Priority.LOWEST;
					}
					pack.setPriority(pack_priority);
					pack.setPacketTo(session.getConnectionId());
					for (Element child : children) {
						pack.getElement().addChild(child);
					}
				}
			}    // end of for (String buddy: buddies)
		}
	}

	private boolean isAllowedForPresenceProbe(XMPPResourceConnection session, JID jid) {
		if (jid == null) {
			return false;
		}

		return session.getDomain().isTrustedJID(jid);
	}

	public interface ExtendedPresenceProcessorIfc {

		default Element extend(Element presence, XMPPResourceConnection session, Queue<Packet> results) {
			return extend(session, results);
		}

		Element extend(XMPPResourceConnection session, Queue<Packet> results);
	}

}
