/*
 * Presence.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Packet;
import tigase.server.Priority;

import tigase.stats.StatisticsList;

import tigase.sys.TigaseRuntime;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.*;
import tigase.xmpp.impl.roster.*;

import static tigase.xmpp.impl.roster.RosterAbstract.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.server.Iq;
import tigase.server.PolicyViolationException;

import tigase.annotations.TigaseDeprecatedComponent;
import tigase.osgi.ModulesManagerImpl;

/**
 * Class responsible for handling Presence packets - deprecated
 * Use PresenceState and PresenceSubscription classes
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@TigaseDeprecatedComponent(note = "Please remove \'+presence\' from \'--sm-plugins=\' or switch to \'presence-state\' and \'presence-subscription\' plugins")
public class Presence
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPStopListenerIfc {
	/**
	 * key allowing enabling automatic authorisation.
	 */
	public static final String AUTO_AUTHORIZE_PROP_KEY = "auto-authorize";

	/**
	 * <code>DIRECT_PRESENCE</code> is a key in temporary session data for the
	 * collection of JIDs where direct presence was sent. To all these addresses
	 * unavailable presence must be sent when user disconnects.
	 */
	public static final String DIRECT_PRESENCE = "direct-presences";

	public static final String DISABLE_ROSTER_LAZY_LOADING_KEY = "disable-roster-lazy-loading";

	public static final String EXTENDED_PRESENCE_PROCESSORS_KEY = "extended-presence-processors";
	
	/** Field description */
	public static final String OFFLINE_BUD_SENT = "offline-bud-sent";

	/** Field description */
	public static final String OFFLINE_RES_SENT = "offline-res-sent";

	/** Field description */
	public static final String OFFLINE_ROSTER_LAST_SEEN_PROP_KEY =
			"offline-roster-last-seen";

	/** Field description */
	public static final String PRESENCE_ELEMENT_NAME = "presence";

	/**
	 * key allowing setting global forwarding JID address.
	 */
	public static final String PRESENCE_GLOBAL_FORWARD = "presence-global-forward";

	/** Field description */
	public static final String SKIP_OFFLINE_PROP_KEY = "skip-offline";

	/** Field description */
	public static final String SKIP_OFFLINE_SYS_PROP_KEY = "skip-offline-sys";

	/** Field description */
	public static final String USERS_STATUS_CHANGES = "Users status changes";

	/** Field description */
	protected static final String XMLNS                      = CLIENT_XMLNS;
	private static int            HIGH_PRIORITY_PRESENCES_NO = 10;

	/** Private logger for class instance. */
	private static final Logger     log = Logger.getLogger(Presence.class.getName());
	private static final long       MAX_DIRECT_PRESENCES_NO = 1000;
	private static final String[]   PRESENCE_PRIORITY_PATH  = { "presence", "priority" };
	private static final String[]   XMLNSS                  = { XMLNS, RosterAbstract.XMLNS_LOAD };
	private static boolean			rosterLazyLoading       = true;
	private static boolean          skipOfflineSys          = true;
	private static boolean          skipOffline             = false;
	private static final String[]   PRESENCE_C_PATH         = { PRESENCE_ELEMENT_NAME,
			"c" };
	private static final String     ID                      = PRESENCE_ELEMENT_NAME;
	private static final String[][] ELEMENTS                = {
		{ PRESENCE_ELEMENT_NAME }, { Iq.ELEM_NAME, Iq.QUERY_NAME }
	};

	/**
	 * variable holding setting regarding auto authorisation of items added to
	 * user roset
	 */
	private static boolean autoAuthorize = false;

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected RosterAbstract roster_util           = getRosterUtil();
	private String[]         offlineRosterLastSeen = null;
	private JID              presenceGLobalForward = null;
	private long             usersStatusChanges    = 0;
	private static final List<ExtendedPresenceProcessorIfc> extendedPresenceProcessors = new ArrayList<>();

	// ~--- methods --------------------------------------------------------------

	/**
	 * Add JID to collection of JIDs to which direct presence was sent. To all
	 * these addresses unavailable presence must be sent when user disconnects.
	 *
	 * @param jid     to which direct presence was sent.
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 */
	@SuppressWarnings({ "unchecked" })
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
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all
	 * buddies from roster and to all users to which direct presence was sent.
	 *
	 * @param session     user session which keeps all the user session data and
	 *                    also gives an access to the user's repository data.
	 * @param results     this a collection with packets which have been generated
	 *                    as input packet processing results.
	 * @param settings    this map keeps plugin specific settings loaded from the
	 *                    Tigase server configuration.
	 * @param roster_util instance of class implementing {@link RosterAbstract}.
	 *
	 * @exception NotAuthorizedException if an error occurs
	 * @throws TigaseDBException
	 */
	public static void broadcastOffline(XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, RosterAbstract roster_util)
					throws NotAuthorizedException, TigaseDBException {

		// Preventing sending offline notifications more than once
		if (session.getSessionData(OFFLINE_BUD_SENT) != null) {
			return;
		}
		session.putSessionData(OFFLINE_BUD_SENT, OFFLINE_BUD_SENT);

		Element pres = session.getPresence();

		if (pres != null) {
			sendPresenceBroadcast(StanzaType.unavailable, session, FROM_SUBSCRIBED, results,
					pres, settings, roster_util);
		} else {
			broadcastDirectPresences(StanzaType.unavailable, session, results, pres);
		}
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all
	 * buddies from roster and to all users to which direct presence was sent.
	 * Before sending presence method calls {@code  requiresPresenceSending()},
	 * configured to only check local environment status (if enabled) to verify
	 * whether presence needs to be sent.
	 *
	 *
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 * @exception NotAuthorizedException if an error occurs
	 * @throws TigaseDBException
	 */
	public void broadcastProbe(XMPPResourceConnection session, Queue<Packet> results,
			Map<String, Object> settings)
					throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Broadcasting probes for: {0}", session);
		}

		// Probe is always broadcasted with initial presence
		Element presInit  = session.getPresence();
		Element presProbe = prepareProbe( session );

		JID[] buddies = roster_util.getBuddies(session, SUB_BOTH);

		try {
			buddies = DynamicRoster.addBuddies(session, settings, buddies, TO_SUBSCRIBED);
		} catch (RosterRetrievingException | RepositoryAccessException ex) {

			// Ignore, handled in the JabberIqRoster code
		}
		if (buddies != null) {
			for (JID buddy : buddies) {
				if (requiresPresenceSending(roster_util, buddy, session, true)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() +
								" | Sending presence probe to: " + buddy);
					}
					sendPresence(null, null, buddy, results, presProbe);
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() +
								" | Sending intial presence to: " + buddy);
					}
					sendPresence(null, null, buddy, results, presInit);
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() +
								" | Skipping sending initial presence and probe to: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		JID[] buddies_to = roster_util.getBuddies(session, SUB_TO);

		if (buddies_to != null) {
			for (JID buddy : buddies_to) {
				if (requiresPresenceSending(roster_util, buddy, session, true)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Sending probe to: " + buddy);
					}
					sendPresence(null, null, buddy, results, presProbe);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() +
								" | Skipping sending presence probe to: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		// TODO: It might be a marginal number of cases here but just make it clear
		// we send a presence here regardless
		JID[] buddies_from = roster_util.getBuddies(session, SUB_FROM);

		if (buddies_from != null) {
			for (JID buddy : buddies_from) {
				if (requiresPresenceSending(roster_util, buddy, session, true)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() +
								" | Sending initial presence to: " + buddy);
					}
					sendPresence(null, null, buddy, results, presInit);
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() +
								" | Skipping sending initial presence and probe to: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)
	}

	private Element prepareProbe( XMPPResourceConnection session ) throws NotAuthorizedException {
		Element presProbe = new Element(PRESENCE_ELEMENT_NAME);
		presProbe.setXMLNS(XMLNS);
		presProbe.setAttribute("type", StanzaType.probe.toString());
		presProbe.setAttribute("from", session.getBareJID().toString());
		return presProbe;
	}

	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {

		// Init plugin configuration
		String tmp;

		tmp            = (String) settings.get(SKIP_OFFLINE_PROP_KEY);
		skipOffline    = (tmp != null)
				? Boolean.parseBoolean(tmp)
				: skipOffline;
		tmp            = (String) settings.get(SKIP_OFFLINE_SYS_PROP_KEY);
		skipOfflineSys = (tmp != null)
				? Boolean.parseBoolean(tmp)
				: skipOfflineSys;
		if (skipOffline || skipOfflineSys) {
			log.config(String.format(
					"Skipping sending presence to offline contacts enabled :: " +
					"skipOffline: %1$s, skipOfflineSys: %2$s", skipOffline, skipOfflineSys));
		}
		autoAuthorize = Boolean.parseBoolean((String) settings.get(AUTO_AUTHORIZE_PROP_KEY));
		if (autoAuthorize) {
			log.config(
					"Automatic presence autorization enabled, results in less strict XMPP specs compatibility ");
		}
		tmp = (String) settings.get( OFFLINE_ROSTER_LAST_SEEN_PROP_KEY );
		if ( tmp != null ){
			if ( tmp.contains( "off" ) ){
				offlineRosterLastSeen = null;
			} else {
				offlineRosterLastSeen = tmp.split( "," );
				log.log( Level.CONFIG, "Loaded roster offline last seen config: {0}", tmp );
			}
//		} else {
//			offlineRosterLastSeen = new String[] {"*"};
//			log.config("No configuration found for Loaded roster offline last seen. - enabling for All clients");
		}
		tmp = (String) settings.get(PRESENCE_GLOBAL_FORWARD);
		if (tmp != null) {
			try {
				presenceGLobalForward = JID.jidInstance(tmp);
			} catch (TigaseStringprepException ex) {
				presenceGLobalForward = null;
				log.log(Level.WARNING, "Presence global forward misconfiguration, cannot parse JID {0}", tmp);
			}
		}
		tmp = (String) settings.get(DISABLE_ROSTER_LAZY_LOADING_KEY);
		rosterLazyLoading = (tmp == null || !Boolean.parseBoolean(tmp));

		tmp = (String) settings.get(EXTENDED_PRESENCE_PROCESSORS_KEY);

		String[] extPresenceProcessorsClasses = tmp != null ? tmp.split( ",") : null ;

		if ( extPresenceProcessorsClasses != null ){
			for ( String clazz : extPresenceProcessorsClasses ) {
				try {
					ExtendedPresenceProcessorIfc processor = (ExtendedPresenceProcessorIfc) ModulesManagerImpl.getInstance().forName( clazz ).newInstance();

					extendedPresenceProcessors.add( processor );
					log.log(Level.CONFIG, "Loadeded ExtendedPresenceProcessor: {0}", processor.getClass());

				} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException ex ) {
					Logger.getLogger( Presence.class.getName() ).log( Level.SEVERE, null, ex );
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * Performs processing of <em>presence</em> packets and calls different
	 * methods for particular {@link PresenceType}
	 *
	 */
	@SuppressWarnings({ "unchecked", "fallthrough" })
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {
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
								processOutInitial(pres, session, results, settings, PresenceType.out_initial);
							} catch (NotAuthorizedException e) {
								log.log(Level.INFO,
										"Can not access user Roster, user session is not authorized yet: {0}",
										packet);
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
			if (finishProcessing)
				return;
		}
		
		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {
			try {
				PresenceType pres_type = roster_util.getPresenceType(session, packet);

				if (pres_type == null) {
					log.log(Level.INFO, "Invalid presence found: {0}", packet);

					return;
				}    // end of if (type == null)
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0} | {1} presence found: {2}",
									new Object[] { session.getBareJID().toString(), pres_type, packet });
				}

				// All 'in' subscription presences must have a valid from address
				switch (pres_type) {
				case in_unsubscribe :
				case in_subscribe :
				case in_unsubscribed :
				case in_subscribed :
					if (packet.getStanzaFrom() == null) {
						if (log.isLoggable(Level.FINE)) {
							log.fine("'in' subscription presence without valid 'from' address, " +
									"dropping packet: " + packet);
						}

						return;
					}
					if (session.isUserId(packet.getStanzaFrom().getBareJID())) {
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE,
									"''in'' subscription to myself, not allowed, returning " +
									"error for packet: " + "{0}", packet);
						}
						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
								"You can not subscribe to yourself.", false));

						return;
					}

					// as per http://xmpp.org/rfcs/rfc6121.html#sub
					// Implementation Note: When a server processes or generates an outbound
					// presence stanza of type "subscribe", "subscribed", "unsubscribe",
					// or "unsubscribed", the server MUST stamp the outgoing presence
					// stanza with the bare JID <localpart@domainpart> of the sending entity,
					// not the full JID <localpart@domainpart/resourcepart>.
					//
					// we enforce this rule also for incomming presence subscirption packets
					packet.initVars( packet.getStanzaFrom().copyWithoutResource(),
													 session.getJID().copyWithoutResource() );

					break;

				case out_subscribe :
				case out_unsubscribe :
				case out_subscribed :
				case out_unsubscribed :

					// Check wheher the destination address is correct to prevent
					// broken/corrupted roster entries:
					if ((packet.getStanzaTo() == null) || packet.getStanzaTo().toString()
							.isEmpty()) {
						results.offer(Authorization.JID_MALFORMED.getResponseMessage(packet,
								"The destination address is incorrect.", false));

						return;
					}

					// According to RFC 3921 draft bis-3, both source and destination
					// addresses must be BareJIDs, handled by initVars(...)
					packet.initVars(session.getJID().copyWithoutResource(), packet.getStanzaTo()
							.copyWithoutResource());

					break;

				default :
					break;
				}
				switch (pres_type) {
				case out_initial :
					processOutInitial(packet, session, results, settings, pres_type);

					break;

				case out_subscribe :
				case out_unsubscribe :
					processOutSubscribe(packet, session, results, settings, pres_type);

					break;

				case out_subscribed :
				case out_unsubscribed :
					processOutSubscribed(packet, session, results, settings, pres_type);

					break;

				case in_initial :
					processInInitial(packet, session, results, settings, pres_type);

					break;

				case in_subscribe :
					processInSubscribe(packet, session, results, settings, pres_type);

					break;

				case in_unsubscribe :
					processInUnsubscribe(packet, session, results, settings, pres_type);

					break;

				case in_subscribed :
					processInSubscribed(packet, session, results, settings, pres_type);

					break;

				case in_unsubscribed :
					processInUnsubscribed(packet, session, results, settings, pres_type);

					break;

				case in_probe :
					if (session.getPresence() == null) {

						// If the user has not yet sent initial presence then ignore the
						// probe.
						return;
					}
					processInProbe(packet, session, results, settings, pres_type);

					break;

				case out_probe :
					forwardPresence(results, packet, session.getJID());
					break;
					
				case error :
					processError(packet, session, results, settings, pres_type);

					break;

				default :
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"Request type is incorrect", false));

					break;
				}    // end of switch (type)
			} catch (NotAuthorizedException e) {
				log.log(Level.INFO,
						"Can not access user Roster, user session is not authorized yet: {0}",
						packet);
				log.log(Level.FINEST, "presence problem...", e);
			} catch ( PolicyViolationException e ) {
				log.log( Level.FINE, "Violation of roster items number policy: {0}", packet );
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Error accessing database for presence data: {0}", e);
			}    // end of try-catch
		}
	}

	/**
	 * Remove JID from collection of JIDs to which direct presence was sent.
	 *
	 * @param jid     to which direct presence was sent.
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 */
	@SuppressWarnings({ "unchecked" })
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
	 * Sends out all pending subscription request during user log-in.
	 *
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void resendPendingInRequests(XMPPResourceConnection session,
			Queue<Packet> results)
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
	 * Sends Presence stanza from provided parameters without returning created
	 * result {@link Packet} object. In case of missing {@code  pres} parameter a
	 * Presence stanza will be created with provided {@link StanzaType} type
	 * {@code t}, {@link JID} type {@code from} and {@link JID} type {@code to}.
	 * Otherwise Presence stanza {@code pres} will be cloned and {@code to}
	 * attribute will be set from parameter {@code to}.
	 *
	 *
	 * @param t       specifies type of the presence to be send.
	 * @param from    is a <code>JID</code> instance with stanza source address.
	 * @param to      is a <code>JID</code> instance with stanza destination
	 *                address.
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 * @param pres    an Object of type {@link Element} holding Presence stanza to
	 *                be sent.
	 */
	public static void sendPresence(StanzaType t, BareJID from, BareJID to,
			Queue<Packet> results, Element pres) {
		sendPresence(t, JID.jidInstance(from), JID.jidInstance(to), results, pres);
	}

	/**
	 * Sends Presence stanza from provided parameters as well as returns created
	 * result {@link Packet} object. In case of missing {@code  pres} parameter a
	 * Presence stanza will be created with provided {@link StanzaType} type
	 * {@code t}, {@link JID} type {@code from} and {@link JID} type {@code to}.
	 * Otherwise Presence stanza {@code pres} will be cloned and {@code to}
	 * attribute will be set from parameter {@code to}.
	 *
	 *
	 * @param t       specifies type of the presence to be send.
	 * @param from    is a <code>JID</code> instance with stanza source address.
	 * @param to      is a <code>JID</code> instance with stanza destination
	 *                address.
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 * @param pres    an Object of type {@link Element} holding Presence stanza to
	 *                be sent.
	 *
	 * @return an instance of {@link Packet} holding Presence stanza created from
	 *         provided parameters.
	 */
	public static Packet sendPresence(StanzaType t, JID from, JID to,
			Queue<Packet> results, Element pres) {
		Element presence = null;
		Packet  result   = null;

		if (pres == null) {
			presence = new Element(PRESENCE_ELEMENT_NAME);
			if (t != null) {
				presence.setAttribute("type", t.toString());
			}    // end of if (t != null)
					else {
				presence.setAttribute("type", StanzaType.unavailable.toString());
			}    // end of if (t != null) else
			if (null != from ) {
				presence.setAttribute("from", from.toString());
			}
			presence.setXMLNS(XMLNS);
		} else {
			presence = pres.clone();
		}      // end of if (pres == null) else
		presence.setAttribute("to", to.toString());
		try {

			// Connection IDs are not available so let's send it a normal way
			result = Packet.packetInstance(presence);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Sending presence info: {0}", result);
			}
			results.offer(result);
		} catch (TigaseStringprepException ex) {
			log.log(Level.FINE,
					"Packet stringprep addressing problem, skipping presence send: {0}", presence);
		}

		return result;
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all
	 * buddies from roster and to all users to which direct presence was sent.
	 * Before sending presence method calls {@code  requiresPresenceSending()}
	 * performing, if configured, both system and roster check to verify whether
	 * presence needs to be sent.
	 *
	 * @param t           specifies type of the presence to be send.
	 * @param session     user session which keeps all the user session data and
	 *                    also gives an access to the user's repository data.
	 * @param results     this a collection with packets which have been generated
	 *                    as input packet processing results.
	 * @param subscrs     an {@code EnumSet<SubscriptionType>} holding all
	 *                    {@link SubscriptionType} to which a Presence should be
	 *                    broadcast.
	 * @param pres        an Object of type {@link Element} holding Presence
	 *                    stanza to be sent.
	 * @param settings    this map keeps plugin specific settings loaded from the
	 *                    Tigase server configuration.
	 * @param roster_util instance of class implementing {@link RosterAbstract}.
	 *
	 * @exception NotAuthorizedException if an error occurs
	 * @throws TigaseDBException
	 */
	public static void sendPresenceBroadcast(StanzaType t, XMPPResourceConnection session,
			EnumSet<SubscriptionType> subscrs, Queue<Packet> results, Element pres, Map<String,
			Object> settings, RosterAbstract roster_util)
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
				log.log(Level.FINEST, session.getBareJID() + " | Buddies found: " + Arrays
						.toString(buddies));
			}

			Priority pack_priority = Priority.PRESENCE;
			int      pres_cnt      = 0;

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
						log.log(Level.FINEST, session.getBareJID() +
								" | Not sending presence to buddy: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No buddies found!!!!");
			}
		}
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String,
			Object> settings) {

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

					if (!StanzaType.unavailable.toString().equals(pres.getAttributeStaticStr(Packet
							.TYPE_ATT))) {
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
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	public static void rebroadcastPresence(XMPPResourceConnection session, Queue<Packet> results) throws NotAuthorizedException, TigaseDBException {
		if (session.getPresence() == null ) {
			// user has not sent initial presence yet, ignore
			return;
		}

		Element presence = session.getPresence().clone();

		for ( ExtendedPresenceProcessorIfc processor : extendedPresenceProcessors ) {
			Element extendContent = processor.extend( session, results );
			if ( extendContent != null ){
				// avoid duplicate
				Element child = presence.getChild( extendContent.getName(), extendContent.getXMLNS() );
				if ( child != null ){
					presence.removeChild( child );
				}
				presence.addChild( extendContent );
			}
		}
		
		sendPresenceBroadcast(StanzaType.available, session, FROM_SUBSCRIBED, results, presence, null, getRosterUtil());

		updateUserResources(presence, session, results, false);

//		sendPresenceBroadcast( StanzaType.get, session, SUB_TO, results, presence, null, null );
	}

	/**
	 * <code>updatePresenceChange</code> method is used to broadcast to all active
	 * resources presence stanza received from other users, like incoming
	 * availability presence, subscription presence and so on... Initial presences
	 * are however sent only to those resources which already have sent initial
	 * presence.
	 *
	 * @param presence an <code>Element</code> presence received from other users,
	 *                 we have to change 'to' attribute to full resource JID.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 *
	 * @exception NotAuthorizedException if an error occurs
	 */
	public static void updatePresenceChange(Packet presence,
			XMPPResourceConnection session, Queue<Packet> results)
					throws NotAuthorizedException {
		boolean initial_p = ((presence.getAttributeStaticStr(Packet.TYPE_ATT) == null) ||
				"available".equals(presence.getAttributeStaticStr(Packet.TYPE_ATT)) ||
				"unavailable".equals(presence.getAttributeStaticStr(Packet.TYPE_ATT)));

		for (XMPPResourceConnection conn : session.getActiveSessions()) {

			// Update presence change only for online resources that is
			// resources which already sent initial presence.
			if (((conn.getPresence() == null) && initial_p)) {

				// Ignore....
				if (log.isLoggable(Level.FINEST)) {
					log.finest(
							"Skipping update presence change for a resource which hasn't sent " +
							"initial presence yet, or is remote connection: " + conn);
				}
			} else {
				try {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Update presence change to: " + conn);
					}

					// Send to old resource presence about new resource
					Packet pres_update = presence.copyElementOnly();

					pres_update.initVars(presence.getStanzaFrom(), conn.getJID()
							.copyWithoutResource());
					pres_update.setPacketTo(conn.getConnectionId());
					results.offer(pres_update);
				} catch (NotAuthorizedException | NoConnectionIdException e) {

					// It might be quite possible that one of the user connections
					// is in state not allowed for sending presence, in such a case
					// none of user connections would receive presence.
					// This catch is to make sure all other resources receive
					// notification.
				}
			}
		}    // end of for (XMPPResourceConnection conn: sessions)
	}

	/**
	 * <code>updateUserResources</code> method is used to broadcast to all
	 * <strong>other</strong> resources presence stanza from one user resource. So
	 * if new resource connects this method updates presence information about new
	 * resource to old resources and about old resources to new resource.
	 *
	 * @param presence an <code>Element</code> presence received from other users,
	 *                 we have to change 'to' attribute to full resource JID.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param initial   specifies whether this is an initial presence or not (i.e.
	 *                 if there is a presence data from the presence stored within
	 *                 user session object or not)
	 * @exception NotAuthorizedException if an error occurs
	 */
	public static void updateUserResources(Element presence,
			XMPPResourceConnection session, Queue<Packet> results, boolean initial)
					throws NotAuthorizedException {
		for (XMPPResourceConnection conn : session.getActiveSessions()) {
			try {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Update presence change to: {0}", conn.getJID());
				}

				// We also do not send presence updates to any remote connections on
				// different cluster nodes. Each node takes care of delivering presence
				// locally
				if (conn.isResourceSet()) {

					// Send to old resource presence about new resource
					Element pres_update = presence.clone();
					Packet  pack_update = Packet.packetInstance(pres_update, session.getJID(), conn
							.getJID().copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);

					Element presence_el = conn.getPresence();

					// Send to new resource last presence sent by the old resource
					if ((presence_el != null) && initial && (conn != session)) {
						pres_update = presence_el.clone();
						pack_update = Packet.packetInstance(pres_update, conn.getJID(), session
								.getJID().copyWithoutResource());
						pack_update.setPacketTo(session.getConnectionId());
						results.offer(pack_update);
					}
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Skipping presence update to: " + conn.getJID());
					}
				}    // end of else
			} catch (NotAuthorizedException | NoConnectionIdException e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		}        // end of for (XMPPResourceConnection conn: sessions)
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(id(), USERS_STATUS_CHANGES, usersStatusChanges, Level.INFO);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * {@code broadcastDirectPresences} broadcast a direct Presence from provided
	 * {@code pres} {@link Element} object to the collection of JIDs stored in
	 * temporary session data under key {@code DIRECT_PRESENCE}.
	 *
	 *
	 * @param t       specifies type of the presence to be send.
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 * @param pres    an Object of type {@link Element} holding Presence stanza to
	 *                be sent.
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@SuppressWarnings({ "unchecked" })
	protected static void broadcastDirectPresences(StanzaType t,
			XMPPResourceConnection session, Queue<Packet> results, Element pres)
					throws NotAuthorizedException, TigaseDBException {
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
	 * Method sends back presence to contact while it becomes online (i.e. during
	 * processing of incoming initial presence of the contact/buddy)
	 *
	 *
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 * @param buddy   {@link JID} of a roster element for which an online state
	 *                will be set
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 * @param online  set whether given contact is online or offline
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void buddyOnline(XMPPResourceConnection session, JID buddy,
			Queue<Packet> results, boolean online)
					throws NotAuthorizedException, TigaseDBException {
		roster_util.setOnline(session, buddy, online);
		if (online && skipOffline &&!roster_util.presenceSent(session, buddy) && roster_util
				.isSubscribedFrom(session, buddy)) {
			Element pres = session.getPresence();

			if (pres != null) {
				sendPresence(null, null, buddy, results, pres);
				roster_util.setPresenceSent(session, buddy, true);
			}
		}
	}

	/**
	 * Simply forwards packet to the destination
	 *
	 *
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 * @param packet  to forward
	 * @param from    is a <code>JID</code> instance with stanza source address.
	 */
	protected static void forwardPresence(Queue<Packet> results, Packet packet, JID from) {
		Element result = packet.getElement().clone();

		// Not needed anymore. Packet filter does it for all stanzas.
		// According to spec we must set proper FROM attribute
		// Yes, but packet filter puts full JID and we need a subscription
		// presence without resource here.
		result.setAttribute("from", from.toString());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "\n\nFORWARD presence: {0}", result.toString());
		}
		results.offer(Packet.packetInstance(result, from, packet.getStanzaTo()));
	}

	/**
	 * Process presence stanza of type Error. Allows errors sent from server to
	 * user and ignore presence errors sent from the user.
	 *
	 *
	 * @param packet       packet is which being processed.
	 * @param session      user session which keeps all the user session data and
	 *                     also gives an access to the user's repository data.
	 * @param results      this a collection with packets which have been
	 *                     generated as input packet processing results.
	 * @param settings     this map keeps plugin specific settings loaded from the
	 *                     Tigase server configuration.
	 * @param presenceType specifies type of the presence
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processError(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType presenceType)
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
	 * Method is responsible for processing incoming initial presence (i.e. in the
	 * receivers session manager).
	 * <p>
	 * It validates the packet (whether from is present or if it's a direct
	 * presence to existing resource) and subsequently set received presence for
	 * the contact that sent it.
	 *
	 *
	 * @param packet       packet is which being processed.
	 * @param session      user session which keeps all the user session data and
	 *                     also gives an access to the user's repository data.
	 * @param results      this a collection with packets which have been
	 *                     generated as input packet processing results.
	 * @param settings     this map keeps plugin specific settings loaded from the
	 *                     Tigase server configuration.
	 * @param presenceType specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processInInitial(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType presenceType)
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

		if ((resource != null) &&!resource.isEmpty()) {
			XMPPResourceConnection direct = session.getParentSession().getResourceForResource(
					resource);

			if (direct != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received direct presence from: {0} to: {1}",
							new Object[] { packet.getStanzaFrom(),
							packet.getStanzaTo() });
				}

				// Send a direct presence to correct resource, otherwise ignore
				Packet result = packet.copyElementOnly();

				result.setPacketTo(direct.getConnectionId());
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Ignoring direct presence from: {0} to: {1}, resource gone.",
							new Object[] { packet.getStanzaFrom(),
							packet.getStanzaTo() });
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
		if ( roster_util.isSubscribedTo( session, presBuddy ) || ( dynItem != null ) ){
			RosterElement rel = roster_util.getRosterElement( session, presBuddy );

			if ( rel != null ){
				rel.setLastSeen( System.currentTimeMillis() );
			}
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST,
								 "Received initial presence, setting buddy: {0} online status to: {1}",
								 new Object[] { packet.getStanzaFrom(),
																online } );
			}
			updatePresenceChange(packet, session, results);
		}
	}

	/**
	 * Method is responsible for processing incoming presence probe (i.e. in the
	 * receivers session manager).
	 * <p>
	 * It validates whether the packet comes from a contact that has correct
	 * subscription and responds with presence of all user's resources presences.
	 *
	 *
	 * @param packet       packet is which being processed.
	 * @param session      user session which keeps all the user session data and
	 *                     also gives an access to the user's repository data.
	 * @param results      this a collection with packets which have been
	 *                     generated as input packet processing results.
	 * @param settings     this map keeps plugin specific settings loaded from the
	 *                     Tigase server configuration.
	 * @param presenceType specifies type of the presence.
	 *
	 * @throws NotAuthorizedException
	 * @throws PacketErrorTypeException
	 * @throws TigaseDBException
	 */
	protected void processInProbe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType presenceType)
					throws NotAuthorizedException, TigaseDBException, PacketErrorTypeException {
		SubscriptionType buddy_subscr = null;
		Element          dynItem;

		try {
			dynItem = DynamicRoster.getBuddyItem(session, settings, packet.getStanzaFrom());
		} catch (RosterRetrievingException | RepositoryAccessException ex) {
			dynItem = null;
		}
		if (dynItem != null) {
			buddy_subscr = SubscriptionType.both;
		} else {
			buddy_subscr = roster_util.getBuddySubscription(session, packet.getStanzaFrom());
		}
		if (buddy_subscr == null) {
			buddy_subscr = SubscriptionType.none;
		}    // end of if (buddy_subscr == null)
		if (roster_util.isSubscribedFrom(buddy_subscr)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Received probe, setting buddy: {0} as online.", packet
						.getStanzaFrom());
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
						sendPresence(null, null, packet.getStanzaFrom().copyWithoutResource(),
								results, pres);
						roster_util.setPresenceSent(session, packet.getStanzaFrom(), true);
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Received probe, sending presence response to: {0}",
									packet.getStanzaFrom());
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
				sendPresence(StanzaType.unsubscribed, session.getBareJID(), packet.getStanzaFrom()
						.getBareJID(), results, null);
			} else {
				// However, if a server receives a presence probe from a configured
				// domain of the server itself or another such trusted service, it MAY
				// provide presence information about the user to that entity.
				for (XMPPResourceConnection conn : session.getActiveSessions()) {
					Element pres = conn.getPresence();

					if (pres != null) {
						sendPresence(null, null, packet.getStanzaFrom(),
								results, pres);
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
	 * Method is responsible for processing incoming subscription request (i.e. in
	 * the receivers session manager).
	 * <p>
	 * If the contact is already subscribed the an auto-reply with
	 * type='subscribded' is sent, otherwise contact is added to the roster (if
	 * it's missing/there is no current subscription), sets the subscription type
	 * to {@code PresenceType.in_subscribe} and subsequently broadcast presence
	 * update to all connected resources.
	 *
	 *
	 * @param packet    packet is which being processed.
	 * @param session   user session which keeps all the user session data and
	 *                  also gives an access to the user's repository data.
	 * @param results   this a collection with packets which have been generated
	 *                  as input packet processing results.
	 * @param settings  this map keeps plugin specific settings loaded from the
	 *                  Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processInSubscribe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
					throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {

		// If the buddy is already subscribed then auto-reply with subscribed
		// presence stanza.
		if (roster_util.isSubscribedFrom(session, packet.getStanzaFrom())) {
			sendPresence(StanzaType.subscribed, session.getJID().copyWithoutResource(), packet
					.getStanzaFrom(), results, null);
		} else {
			SubscriptionType curr_sub = roster_util.getBuddySubscription(session, packet
					.getStanzaFrom());

			if (curr_sub == null) {
				roster_util.addBuddy(session, packet.getStanzaFrom(), null, null, null);
			}    // end of if (curr_sub == null)
			roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());
			if (!autoAuthorize) {
				updatePresenceChange(packet, session, results);
			} else {
				roster_util.setBuddySubscription(session, SubscriptionType.both, packet
						.getStanzaFrom().copyWithoutResource());
			}
		}    // end of else
		if (autoAuthorize) {
			roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(session,
					packet.getStanzaFrom().copyWithoutResource()));
			broadcastProbe(session, results, settings);
			sendPresence(StanzaType.subscribed, session.getJID(), packet.getStanzaFrom(),
					results, null);
		}
	}

	/**
	 * Method is responsible for processing incoming subscribed presence (i.e. in
	 * the receivers session manager).
	 * <p>
	 * Contact is added to the roster (if it's missing/there is no current
	 * subscription), sets the subscription type to
	 * {@code PresenceType.in_subscribed} and subsequently, if subscription has
	 * changed,forwards the presence to user resource connection as well as
	 * broadcast presence update to all connected resources.
	 *
	 *
	 * @param packet    packet is which being processed.
	 * @param session   user session which keeps all the user session data and
	 *                  also gives an access to the user's repository data.
	 * @param results   this a collection with packets which have been generated
	 *                  as input packet processing results.
	 * @param settings  this map keeps plugin specific settings loaded from the
	 *                  Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processInSubscribed(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
					throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {
		SubscriptionType curr_sub = roster_util.getBuddySubscription(session, packet
				.getStanzaFrom());

		if (!autoAuthorize && (curr_sub == null)) {
			roster_util.addBuddy(session, packet.getStanzaFrom(), null, null, null);
		}    // end of if (curr_sub == null)

		boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
				packet.getStanzaFrom());

		if (subscr_changed) {
			Packet forward_p = packet.copyElementOnly();

			forward_p.setPacketTo(session.getConnectionId());
			results.offer(forward_p);
			if (autoAuthorize) {
				roster_util.setBuddySubscription(session, SubscriptionType.both, packet
						.getStanzaFrom().copyWithoutResource());
			}
			roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(session,
					packet.getStanzaFrom()));

			Element delay = packet.getElement().getChild( "delay", "urn:xmpp:delay");
			if (delay != null ) {
				// offline packet, lets send probe
				Element presProbe = prepareProbe( session );
				sendPresence( null, null, packet.getStanzaFrom(), results, presProbe );
			}

		}
	}

	/**
	 * Method is responsible for processing incoming unsubscribe presence (i.e. in
	 * the receivers session manager).
	 * <p>
	 * First method performs update of subscription of the given contact and
	 * subsequently the request is forwarded to the client to make sure it says in
	 * synch with the server (in case there was actual change in subscription).
	 * Lastly a roster push is generated to all connected resources to update them
	 * with current state of the roster and items subscriptions.
	 *
	 *
	 * @param packet    packet is which being processed.
	 * @param session   user session which keeps all the user session data and
	 *                  also gives an access to the user's repository data.
	 * @param results   this a collection with packets which have been generated
	 *                  as input packet processing results.
	 * @param settings  this map keeps plugin specific settings loaded from the
	 *                  Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processInUnsubscribe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
					throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {
		boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
				packet.getStanzaFrom());

		if (subscr_changed) {

			// First forward the request to the client to make sure it stays in sync
			// with the server. This should be done only in the case of actual change of the state
			// and with auto-authorization disabled
			if (!autoAuthorize) {
				Packet forward_p = packet.copyElementOnly();

				forward_p.setPacketTo(session.getConnectionId());
				results.offer(forward_p);
			}

			Element item = roster_util.getBuddyItem(session, packet.getStanzaFrom());

			if (item != null) {
				roster_util.updateBuddyChange(session, results, item);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Received unsubscribe request from a user who is not in the roster: {0}",
							packet.getStanzaFrom());
				}
			}
			if (autoAuthorize) {
				broadcastProbe(session, results, settings);
			}
		}
	}

	/**
	 * Method is responsible for processing incoming unsubscribed presence (i.e.
	 * in the receivers session manager).
	 * <p>
	 * First method checks for the current subscription of the contact and if this
	 * verifies performs subsequent actions such as forwarding presence to the
	 * user connection to make sure it says in synch with the server, updates
	 * contact subscription with {@code PresenceType.in_unsubscribed} and in case
	 * that there was a change in user subscription send out a roster push to all
	 * connected resources to update them with current state of the roster and
	 * items subscriptions.
	 *
	 *
	 * @param packet    packet is which being processed.
	 * @param session   user session which keeps all the user session data and
	 *                  also gives an access to the user's repository data.
	 * @param results   this a collection with packets which have been generated
	 *                  as input packet processing results.
	 * @param settings  this map keeps plugin specific settings loaded from the
	 *                  Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processInUnsubscribed(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
					throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {
		SubscriptionType curr_sub = roster_util.getBuddySubscription(session, packet
				.getStanzaFrom());

		if (curr_sub != null) {

			// First forward the request to the client to make sure it stays in sync
			// with the server. This should be done only with auto-authorization disabled
			if (!autoAuthorize) {
				Packet forward_p = packet.copyElementOnly();

				forward_p.setPacketTo(session.getConnectionId());
				results.offer(forward_p);
			}

			boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
					packet.getStanzaFrom());

			if (subscr_changed) {
				Element item = roster_util.getBuddyItem(session, packet.getStanzaFrom());

				// The roster item could have been removed in the meantime....
				if (item != null) {
					roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(
							session, packet.getStanzaFrom()));
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Received unsubscribe request from a user who is not in the roster: {0}",
								packet.getStanzaFrom());
					}
				}
				if (autoAuthorize) {
					broadcastProbe(session, results, settings);
				}
			}
		}
	}

	/**
	 * Method is responsible for processing outgoing initial presence (i.e. in the
	 * sender session manager).
	 * <p>
	 * Process packet accordingly whether it's a direct presence (forward it, add
	 * to proper collection of JIDs to which a direct presence has been sent) or
	 * regular presence. THe latter causes properly address the packet, store
	 * presence within session data for subsequent use, and for the first
	 * availability presence (in case there is no prior presence stored in user
	 * session data) server sends probes to all contacts and pushes out all
	 * pending subscription request or (if there i already presence stored in
	 * session data) broadcast presence update to contacts.
	 * <p>
	 * If there is a JID forwarding set up, presence is also forwarded to
	 * configured JID.
	 *
	 *
	 * @param packet   packet is which being processed.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 * @param type     specifies type of the presence.
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processOutInitial(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType type)
					throws NotAuthorizedException, TigaseDBException {

		// Is it a direct presence to some entity on the network?
		if (packet.getStanzaTo() != null) {

			// Yes this is it, send direct presence
			results.offer(packet.copyElementOnly());

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

			for ( ExtendedPresenceProcessorIfc processor : extendedPresenceProcessors ) {
				Element extendContent = processor.extend( session, results );
				if ( extendContent != null ){
					presenceEl.addChild( extendContent );
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
				Element iq = new Element(Iq.ELEM_NAME, new String[] { "type" }, new String[] { "set" });
				Element query = new Element(Iq.QUERY_NAME, new String[] { Iq.XMLNS_ATT }, new String[] { RosterAbstract.XMLNS_LOAD });
				iq.addChild(query);
				Packet loadCmd = Packet.packetInstance(iq, packet.getStanzaFrom(), packet.getStanzaTo());
				loadCmd.setPacketFrom(packet.getPacketFrom());
				loadCmd.setPacketTo(packet.getPacketTo());
				results.add(loadCmd);
			}
		}
	}

	/**
	 * Method is responsible for processing outgoing subscribe and unsubscribe
	 * presence (i.e. in the sender session manager).
	 * <p>
	 * Presence packet is forwarded to the destination with the JID stripped from
	 * the resource.
	 * <p>In case of {@code PresenceType.out_subscribe} packet type contact is
	 * added to the roster (in case it was missing), a subscription state is being
	 * updated and, in case there was a change, a roster push is being sent to all
	 * user resources.
	 * <p>
	 * In case of {@code PresenceType.out_unsubscribe} method updates contact
	 * subscription (and generates roster push if there was a change) and if the
	 * resulting contact subscription is NONE then contact is removed from the
	 * roster.
	 *
	 *
	 * @param packet    packet is which being processed.
	 * @param session   user session which keeps all the user session data and
	 *                  also gives an access to the user's repository data.
	 * @param results   this a collection with packets which have been generated
	 *                  as input packet processing results.
	 * @param settings  this map keeps plugin specific settings loaded from the
	 *                  Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processOutSubscribe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
					throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {

		// According to RFC-3921 I must forward all these kind presence
		// requests, it allows to resynchronize
		// subscriptions in case of synchronization loss
		boolean subscr_changed = false;

		forwardPresence(results, packet, session.getJID().copyWithoutResource());

		SubscriptionType current_subscription = roster_util.getBuddySubscription(session,
				packet.getStanzaTo());

		if (pres_type == PresenceType.out_subscribe) {
			if (current_subscription == null) {
				roster_util.addBuddy(session, packet.getStanzaTo(), null, null, null);
			}    // end of if (current_subscription == null)
			subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet
					.getStanzaTo());
			if (autoAuthorize) {
				roster_util.setBuddySubscription(session, SubscriptionType.both, packet
						.getStanzaTo().copyWithoutResource());
			}
			if (subscr_changed) {
				roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(session,
						packet.getStanzaTo()));
			}    // end of if (subscr_changed)
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "out_subscribe: current_subscription = " +
						current_subscription);
			}
			if (current_subscription != null) {
				subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet
						.getStanzaTo());
				current_subscription = roster_util.getBuddySubscription(session, packet
						.getStanzaTo());
				if (subscr_changed) {
					roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(
							session, packet.getStanzaTo()));
				}    // end of if (subscr_changed)
				if (SUB_NONE.contains(current_subscription)) {
					roster_util.removeBuddy(session, packet.getStanzaTo());
				}    // end of if (current_subscription == null)
			}
		}
	}

	/**
	 * Method is responsible for processing outgoing subscribed and unsubscribed
	 * presence (i.e. in the sender session manager).
	 * <p>
	 * Presence packet is forwarded to the destination with the JID stripped from
	 * the resource, a subscription state is being updated and, in case there was
	 * a change, a roster push is being sent to all user resources. Also, in case
	 * of presence type out_subscribed server send current presence to the user
	 * from each of the contact's available resources. For the presence type
	 * out_unsubscribed an unavailable presence is sent.
	 *
	 *
	 * @param packet    packet is which being processed.
	 * @param session   user session which keeps all the user session data and
	 *                  also gives an access to the user's repository data.
	 * @param results   this a collection with packets which have been generated
	 *                  as input packet processing results.
	 * @param settings  this map keeps plugin specific settings loaded from the
	 *                  Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	protected void processOutSubscribed(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
					throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {

		// According to RFC-3921 I must forward all these kind presence
		// requests, it allows to re-synchronize
		// subscriptions in case of synchronization loss
		forwardPresence(results, packet, session.getJID().copyWithoutResource());

		Element initial_presence = session.getPresence();
		JID     buddy            = packet.getStanzaTo().copyWithoutResource();
		boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type,
				buddy);

		if (autoAuthorize && (pres_type == PresenceType.out_subscribed)) {
			roster_util.setBuddySubscription(session, SubscriptionType.both, buddy
					.copyWithoutResource());
		}
		if (subscr_changed) {
			roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(session,
					buddy));
			if (initial_presence != null) {
				if (pres_type == PresenceType.out_subscribed) {

					// The contact's server MUST then also send current presence to the user
					// from each of the contact's available resources.
					List<XMPPResourceConnection> activeSessions = session.getActiveSessions();

					for (XMPPResourceConnection userSessions : activeSessions) {
						Element presence = userSessions.getPresence();

						sendPresence(StanzaType.available, userSessions.getjid(), buddy, results,
								presence);
					}
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					sendPresence(StanzaType.unavailable, session.getJID(), buddy, results, null);
				}
			}    // end of if (subscr_changed)
		}
	}

	/**
	 * Method sends server generated presence unavailable for all buddies from the
	 * roster with a custom status message.
	 *
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 *
	 * @throws TigaseDBException
	 * @throws NotAuthorizedException
	 * @throws NoConnectionIdException
	 */
	@SuppressWarnings("empty-statement")
	protected void sendRosterOfflinePresence(XMPPResourceConnection session,
			Queue<Packet> results)
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
		int     i           = 0;

		if ( offlineRosterLastSeen.length > 0 && !offlineRosterLastSeen[0].equals( "*" ) ){
			while ( ( i < offlineRosterLastSeen.length ) && !( validClient |= node.contains(
					offlineRosterLastSeen[i++] ) ) );
			if ( !validClient ){
				log.finest( "Client does not match, skipping..." );

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
			int      pres_cnt      = 0;

			for ( JID buddy : buddies ) {
				List<Element> children = roster_util.getCustomChildren( session, buddy );

				if ( children != null && !children.isEmpty() ){
					Packet pack = sendPresence( StanzaType.unavailable, buddy, session.getJID(),
																			results, null );
					
					if ( pres_cnt == HIGH_PRIORITY_PRESENCES_NO ){
						++pres_cnt;
						pack_priority = Priority.LOWEST;
					}
					pack.setPriority( pack_priority );
					pack.setPacketTo( session.getConnectionId() );
					for ( Element child : children ) {
						pack.getElement().addChild( child );
					}
				}
			}    // end of for (String buddy: buddies)
		}
	}

	/**
	 * <code>updateOfflineChange</code> method broadcast off-line presence to all
	 * other user active resources.
	 *
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 * @param results this a collection with packets which have been generated as
	 *                input packet processing results.
	 *
	 * @exception NotAuthorizedException if an error occurs
	 */
	protected static void updateOfflineChange(XMPPResourceConnection session,
			Queue<Packet> results)
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
				if ((conn != session) && (conn.isResourceSet()) &&!conn.getResource().equals(
						session.getResource())) {

					// Send to old resource presence about new resource
					Element pres_update = new Element(PRESENCE_ELEMENT_NAME);

					pres_update.setAttribute("type", StanzaType.unavailable.toString());
					pres_update.setXMLNS(XMLNS);

					// accroding to RFC1621, 4.5.2.  Server Processing of Outbound Unavailable Presence
					// this presece packet should be addressed to fullJID
					Packet pack_update = Packet.packetInstance( pres_update, session.getJID(),
																											conn.getJID() );

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

	/**
	 * Method updates resources information upon receiving initial availability
	 * presence (type available or missing type)
	 *
	 *
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data.
	 * @param type    specifies type of the stanza.
	 * @param packet  packet is which being processed.
	 */
	protected static void updateResourcesAvailable(XMPPResourceConnection session,
			StanzaType type, Packet packet) {
		XMPPSession parentSession = session.getParentSession();

		if (parentSession != null) {
			Map<JID, Map> resources;
			boolean       online = (type == null) || (type == StanzaType.available);

			synchronized (parentSession) {
				resources = (Map<JID, Map>) parentSession.getCommonSessionData(
						XMPPResourceConnection.ALL_RESOURCES_KEY);
				if (resources == null) {
					if (!online) {
						return;
					}
					resources = new ConcurrentHashMap<>();
					session.putCommonSessionData(XMPPResourceConnection.ALL_RESOURCES_KEY,
							resources);
				}
			}
			if (online) {
				Map map = resources.get(packet.getStanzaFrom());

				if (map == null) {
					map = new ConcurrentHashMap();
					resources.put(packet.getStanzaFrom(), map);
				}

				String priorityStr = packet.getElemCDataStaticStr(PRESENCE_PRIORITY_PATH);

				if (priorityStr != null) {
					map.put(XMPPResourceConnection.ALL_RESOURCES_PRIORITY_KEY, Integer.parseInt(
							priorityStr));
				} else if (!map.containsKey(XMPPResourceConnection.ALL_RESOURCES_PRIORITY_KEY)) {
					map.put(XMPPResourceConnection.ALL_RESOURCES_PRIORITY_KEY, 0);
				}

				Element c = packet.getElement().getChild("c", "http://jabber.org/protocol/caps");

				if (c != null) {
					map.put(XMPPResourceConnection.ALL_RESOURCES_CAPS_KEY,
							PresenceCapabilitiesManager.processPresence(c));
				}
			} else {
				resources.remove(packet.getStanzaFrom());
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns shared instance of class implementing {@link RosterAbstract} -
	 * either default one ({@link RosterFlat}) or the one configured with
	 * <em>"roster-implementation"</em> property.
	 *
	 * @return shared instance of class implementing {@link RosterAbstract}
	 */
	protected static RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}

	//~--- methods --------------------------------------------------------------

	private boolean isAllowedForPresenceProbe(XMPPResourceConnection session, JID jid) {
		if (jid == null)
			return false;
		
		return session.getDomain().isTrustedJID(jid);
	}
	
	/**
	 * Method checks whether a given contact requires sending presence. In case of
	 * enabling option {@code skipOffline} and user being offline in the roster
	 * the presence is not sent. Alternatively enabling option
	 * {@code skipOfflineSys} would cause local environment check for user status
	 * and omit sending presence if the local use is offline.
	 *
	 *
	 * @param roster      instance of class implementing {@link RosterAbstract}.
	 * @param buddy       JID of a contact for which a check is to be performed.
	 * @param session     user session which keeps all the user session data and
	 *                    also gives an access to the user's repository data.
	 * @param systemCheck indicates whether the check should be only based on
	 *                    local environment state ({@code true}) or rooster state
	 *                    of given user should also be taken into consideration
	 *                    ({@code false}).
	 *
	 * @return {code true} if the contact requires sending presence (e.g. is not
	 *         online and options skipOffline or skipOfflineSys are enabled)
	 *
	 * @throws TigaseDBException
	 * @throws NotAuthorizedException
	 */
	private static boolean requiresPresenceSending(RosterAbstract roster, JID buddy,
			XMPPResourceConnection session, boolean systemCheck)
					throws NotAuthorizedException, TigaseDBException {
		boolean result = true;

		// if non-system check is enabled during broadcast of non-first initial
		// presence or offline presence
		if (!systemCheck) {
			boolean isOnline = roster.isOnline( session, buddy );
			if ( skipOffline && !isOnline ){
				if ( log.isLoggable( Level.FINEST ) ){
					log.log( Level.FINEST, "{0} | buddy: {1} is online: {2}",
									 new Object[] { session.getJID(), buddy, isOnline } );
				}
				result = result && false;
			}
		}
		if (skipOfflineSys) {
			TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();
			boolean isJidOnline = runtime.isJidOnline( buddy );

			if ( runtime.hasCompleteJidsInfo()
					 && session.isLocalDomain( buddy.getDomain(), false )
					 && !isJidOnline ){
				if ( log.isLoggable( Level.FINEST ) ){
					log.log( Level.FINEST, "{0} | buddy: {1} is online (sys): {2}",
																 new Object[] { session.getJID(), buddy, isJidOnline } );
				}
				result = result && false;
			}
		}

		return result;
	}

	public interface ExtendedPresenceProcessorIfc {
		Element extend( XMPPResourceConnection session, Queue<Packet> results );
	}

} 
