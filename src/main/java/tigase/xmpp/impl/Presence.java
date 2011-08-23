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

import static tigase.xmpp.impl.roster.RosterAbstract.FROM_SUBSCRIBED;
import static tigase.xmpp.impl.roster.RosterAbstract.PresenceType;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_BOTH;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_FROM;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_TO;
import static tigase.xmpp.impl.roster.RosterAbstract.SUB_NONE;
import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.xmpp.BareJID;

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
public class Presence extends XMPPProcessor implements XMPPProcessorIfc,
		XMPPStopListenerIfc {

	/**
	 * <code>DIRECT_PRESENCE</code> is a key in temporary session data for the
	 * collection of JIDs where direct presence was sent. To all these addresses
	 * unavailable presence must be sent when user disconnects.
	 */
	public static final String DIRECT_PRESENCE = "direct-presences";

	/** Field description */
	public static final String PRESENCE_ELEMENT_NAME = "presence";

	/** Field description */
	public static final String SKIP_OFFLINE_PROP_KEY = "skip-offline";
	protected static final String XMLNS = CLIENT_XMLNS;

	/**
	 * Private logger for class instance.
	 */
	private static final Logger log = Logger.getLogger(Presence.class.getName());
	private static final String[] XMLNSS = { XMLNS };
	private static final String[] ELEMENTS = { PRESENCE_ELEMENT_NAME };
	private static final String ID = PRESENCE_ELEMENT_NAME;
	private static long requiredNo = 0;
	private static long requiredYes = 0;
	private static TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();

	/** Field description */
	public static final String OFFLINE_RES_SENT = "offline-res-sent";

	/** Field description */
	public static final String OFFLINE_BUD_SENT = "offline-bud-sent";
	private static boolean skipOffline = false;

	// ~--- fields ---------------------------------------------------------------

	protected RosterAbstract roster_util = getRosterUtil();

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param session
	 */
	@SuppressWarnings({ "unchecked" })
	public static void addDirectPresenceJID(JID jid, XMPPResourceConnection session) {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if (direct_presences == null) {
			direct_presences = new LinkedHashSet<JID>(10);
			session.putSessionData(DIRECT_PRESENCE, direct_presences);
		} // end of if (direct_presences == null)

		direct_presences.add(jid);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Added direct presence jid: {0}", jid);
		}
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all
	 * buddies from roster and to all users to which direct presence was sent.
	 *
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 * @param roster_util
	 * @exception NotAuthorizedException
	 *              if an error occurs
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
			// This code should not be necessary anymore. It is done in the stopped
			// method.
			// pres.setAttribute("type", StanzaType.unavailable.toString());
			sendPresenceBroadcast(StanzaType.unavailable, session, FROM_SUBSCRIBED, results,
					pres, settings, roster_util);
		} else {
			broadcastDirectPresences(StanzaType.unavailable, session, results, pres);
		}

		// // String from = session.getJID();
		// //String[] buddies = roster_util.getBuddies(session, FROM_SUBSCRIBED,
		// false);
		// JID[] buddies = roster_util.getBuddies(session, FROM_SUBSCRIBED);
		//
		// buddies = DynamicRoster.addBuddies(session, settings, buddies);
		//
		// // Only broadcast offline presence if there are any buddies expecting it
		// // and only if the user has sent initial presence before.
		// if ((buddies != null) && (pres != null)) {
		//
		// // Set<String> onlineJids =
		// TigaseRuntime.getTigaseRuntime().getOnlineJids();
		// // Below code is not needed, this should be done while the presence
		// // is being processed and saved
		// // pres.setAttribute("from", from);
		// for (JID buddy : buddies) {
		//
		// // If buddy is a local buddy and he is offline, don't send him packet...
		// if (requiresPresenceSending(buddy, session)) {
		// sendPresence(StanzaType.unavailable, null, buddy, results, pres);
		// }
		//
		// // sendPresence(StanzaType.unavailable, from, buddy, results, pres);
		// } // end of for (String buddy: buddies)
		// } // end of if (buddies == null)
		//
		// broadcastDirectPresences(StanzaType.unavailable, session, results, pres);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param session
	 */
	@SuppressWarnings({ "unchecked" })
	public static void removeDirectPresenceJID(JID jid, XMPPResourceConnection session) {
		Set<JID> direct_presences = (Set<JID>) session.getSessionData(DIRECT_PRESENCE);

		if (direct_presences != null) {
			direct_presences.remove(jid);
		} // end of if (direct_presences == null)

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Added direct presence jid: {0}", jid);
		}
	}

	public static void sendPresence(StanzaType t, BareJID from, BareJID to, Queue<Packet> results, Element pres) {
		sendPresence( t, JID.jidInstance( from ), JID.jidInstance( to ), results, pres);
	}

	/**
	 * Method description
	 *
	 *
	 * @param t
	 * @param from
	 * @param to
	 * @param results
	 * @param pres
	 */
	public static void sendPresence(StanzaType t, JID from, JID to, Queue<Packet> results,
			Element pres) {
		Element presence = null;

		if (pres == null) {
			presence = new Element(PRESENCE_ELEMENT_NAME);

			if (t != null) {
				presence.setAttribute("type", t.toString());
			} // end of if (t != null)
			else {
				presence.setAttribute("type", StanzaType.unavailable.toString());
			} // end of if (t != null) else

			presence.setAttribute("from", from.toString());
			presence.setXMLNS(XMLNS);
		} else {
			presence = pres.clone();
		} // end of if (pres == null) else

		presence.setAttribute("to", to.toString());

		// Optimization, especially useful for cluster mode.
		// try getting all connection IDs connection manager addresses for the
		// destination packets if possible and send packets directly without
		// going through the session manager on other node.
		// Please note! may cause unneeded behavior if privacy lists or other
		// blocking mechanism is used
		// This is actually not such a good idea, it is always better to send the
		// packets through the SM. The only packets which could be optimized that
		// way
		// are Message packets.
		// JID[] connIds = runtime.getConnectionIdsForJid(to);
		//
		// if ((connIds != null) && (connIds.length > 0)) {
		// for (JID connId : connIds) {
		// try {
		// Packet packet = Packet.packetInstance(presence);
		//
		// packet.setPacketTo(connId);
		//
		// if (log.isLoggable(Level.FINEST)) {
		// log.finest("Sending presence info: " + packet);
		// }
		//
		// results.offer(packet);
		// } catch (TigaseStringprepException ex) {
		// log.warning("Packet stringprep addressing problem, skipping presence send: "
		// + presence);
		// }
		// }
		// } else {
		try {

			// Connection IDs are not available so let's send it a normal way
			Packet packet = Packet.packetInstance(presence);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Sending presence info: {0}", packet);
			}

			results.offer(packet);
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING,
					"Packet stringprep addressing problem, skipping presence send: {0}", presence);
		}

		// }
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all
	 * buddies from roster and to all users to which direct presence was sent.
	 *
	 * @param t
	 *          a <code>StanzaType</code> value
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @param subscrs
	 * @param results
	 * @param pres
	 *          an <code>Element</code> value
	 * @param settings
	 * @param roster_util
	 * @exception NotAuthorizedException
	 *              if an error occurs
	 * @throws TigaseDBException
	 */
	public static void sendPresenceBroadcast(StanzaType t, XMPPResourceConnection session,
			EnumSet<SubscriptionType> subscrs, Queue<Packet> results, Element pres,

			// final Map<String, Object> settings, boolean onlineOnly)
			Map<String, Object> settings, RosterAbstract roster_util)
			throws NotAuthorizedException, TigaseDBException {

		// String from = session.getJID();
		// String[] buddies = roster_util.getBuddies(session, subscrs, onlineOnly);
		RosterAbstract roster = roster_util;

		if (roster == null) {
			roster = RosterFactory.getRosterImplementation(true);
		}

		JID[] buddies = roster.getBuddies(session, subscrs);

		buddies = DynamicRoster.addBuddies(session, settings, buddies);

		if (buddies != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Buddies found: " + Arrays.toString(buddies));
			}
			for (JID buddy : buddies) {
				if (requiresPresenceSending(roster, buddy, session)) {
					sendPresence(t, session.getJID(), buddy, results, pres);
					roster.setPresenceSent(session, buddy, true);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Not sending presence to buddy: " + buddy);
					}
				}
			} // end of for (String buddy: buddies)
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No buddies found!!!!");
			}
		}

		broadcastDirectPresences(t, session, results, pres);
	}

	/**
	 * <code>updatePresenceChange</code> method is used to broadcast to all active
	 * resources presence stanza received from other users, like incoming
	 * availability presence, subscription presence and so on... Initial presences
	 * are however sent only to those resources which already have sent initial
	 * presence.
	 *
	 * @param presence
	 *          an <code>Element</code> presence received from other users, we
	 *          have to change 'to' attribute to full resource JID.
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value keeping connection
	 *          session object.
	 * @param results
	 * @exception NotAuthorizedException
	 *              if an error occurs
	 */
	public static void updatePresenceChange(Packet presence,
			XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException {
		boolean initial_p =
				((presence.getAttribute("type") == null)
						|| "available".equals(presence.getAttribute("type")) || "unavailable"
						.equals(presence.getAttribute("type")));

		for (XMPPResourceConnection conn : session.getActiveSessions()) {

			// Update presence change only for online resources that is
			// resources which already sent initial presence.
			if (((conn.getPresence() == null) && initial_p)) {

				// Ignore....
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Skipping update presence change for a resource which hasn't sent "
							+ "initial presence yet, or is remote connection: " + conn);
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
				} catch (Exception e) {

					// It might be quite possible that one of the user connections
					// is in state not allowed for sending presence, in such a case
					// none of user connections would receive presence.
					// This catch is to make sure all other resources receive
					// notification.
				}
			}
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	/**
	 * <code>updateUserResources</code> method is used to broadcast to all
	 * <strong>other</strong> resources presence stanza from one user resource. So
	 * if new resource connects this method updates presence information about new
	 * resource to old resources and about old resources to new resource.
	 *
	 * @param presence
	 *          an <code>Element</code> presence received from other users, we
	 *          have to change 'to' attribute to full resource JID.
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value keeping connection
	 *          session object.
	 * @param results
	 * @exception NotAuthorizedException
	 *              if an error occurs
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

					// Below settings are applied by packetInstance(...) method a few
					// lines later
					// pres_update.setAttribute("from", session.getJID().toString());
					// pres_update.setAttribute("to", conn.getBareJID().toString());
					Packet pack_update =
							Packet.packetInstance(pres_update, session.getJID(), conn.getJID()
									.copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);

					Element presence_el = conn.getPresence();

					// Send to new resource last presence sent by the old resource
					if (presence_el != null && initial && conn != session) {
						pres_update = presence_el.clone();

						// Below is not necessary, initVars(...) which is called from
						// packetInstance() sets from/to attributes for stanza
						// pres_update.setAttribute("from", conn.getJID().toString());
						// pres_update.setAttribute("to", session.getUserId().toString());
						pack_update =
								Packet.packetInstance(pres_update, conn.getJID(), session.getJID()
										.copyWithoutResource());
						pack_update.setPacketTo(session.getConnectionId());
						results.offer(pack_update);
					}
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Skipping presence update to: " + conn.getJID());
					}
				} // end of else
			} catch (Exception e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		} // end of for (XMPPResourceConnection conn: sessions)
	}

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

				sendPresence(t, session.getJID(), buddy, results, pres);
			} // end of for (String buddy: buddies)
		} // end of if (direct_presence != null)
	}

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
	 * <code>updateOfflineChange</code> method broadcast off-line presence to all
	 * other user active resources.
	 *
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @exception NotAuthorizedException
	 *              if an error occurs
	 */
	protected static void updateOfflineChange(XMPPResourceConnection session,
			Queue<Packet> results) throws NotAuthorizedException {

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

				if ((conn != session) && (conn.isResourceSet())
						&& !conn.getResource().equals(session.getResource())) {

					// Send to old resource presence about new resource
					Element pres_update = new Element(PRESENCE_ELEMENT_NAME);

					// Below is set also by packetInstance method.
					// pres_update.setAttribute("from", session.getJID().toString());
					// pres_update.setAttribute("to", conn.getBareJID().toString());
					pres_update.setAttribute("type", StanzaType.unavailable.toString());
					pres_update.setXMLNS(XMLNS);

					Packet pack_update =
							Packet.packetInstance(pres_update, session.getJID(), conn.getJID()
									.copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Skipping presence update to: {0}", conn.getJID());
					}
				} // end of else
			} catch (Exception e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		} // end of for (XMPPResourceConnection conn: sessions)
	}

	private static boolean requiresPresenceSending(RosterAbstract roster, JID buddy,
			XMPPResourceConnection session) throws NotAuthorizedException, TigaseDBException {
		boolean result = true;

		if (skipOffline && !roster.isOnline(session, buddy)) {
			result = false;
		}

		return result;
	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all
	 * budies from roster and to all users to which direct presence was sent.
	 *
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 * @exception NotAuthorizedException
	 *              if an error occurs
	 * @throws TigaseDBException
	 */
	public void broadcastProbe(XMPPResourceConnection session, Queue<Packet> results,
			Map<String, Object> settings) throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Broadcasting probes for: {0}", session);
		}

		// Probe is always broadcasted with initial presence
		Element presInit = session.getPresence();
		Element presProbe = new Element(PRESENCE_ELEMENT_NAME);

		presProbe.setXMLNS(XMLNS);
		presProbe.setAttribute("type", StanzaType.probe.toString());
		presProbe.setAttribute("from", session.getBareJID().toString());

		// String[] buddies = roster_util.getBuddies(session, TO_SUBSCRIBED, false);
		// We send presence probe to TO_SUBSCRIBED and initial presence to
		// FROM_SUBSCRIBED. Most of buddies however are BOTH. So as optimalization
		// Let's process BOTH first and then TO_ONLY and FROM_ONLY separately
		JID[] buddies = roster_util.getBuddies(session, SUB_BOTH);

		buddies = DynamicRoster.addBuddies(session, settings, buddies);

		if (buddies != null) {
			for (JID buddy : buddies) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Sending probe to: {0}", buddy);
				}

				sendPresence(null, null, buddy, results, presProbe);

				if (requiresPresenceSending(roster_util, buddy, session)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Sending intial to: {0}", buddy);
					}

					sendPresence(null, null, buddy, results, presInit);
					roster_util.setPresenceSent(session, buddy, true);
				}
			} // end of for (String buddy: buddies)
		} // end of if (buddies == null)

		JID[] buddies_to = roster_util.getBuddies(session, SUB_TO);

		if (buddies_to != null) {
			for (JID buddy : buddies_to) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Sending probe to: {0}", buddy);
				}

				sendPresence(null, null, buddy, results, presProbe);
			} // end of for (String buddy: buddies)
		} // end of if (buddies == null)

		// TODO: It might be a marginal number of cases here but just make it clear
		// we send a presence here regardles
		JID[] buddies_from = roster_util.getBuddies(session, SUB_FROM);

		if (buddies_from != null) {
			for (JID buddy : buddies_from) {
				if (requiresPresenceSending(roster_util, buddy, session)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Sending initial to: {0}", buddy);
					}

					sendPresence(null, null, buddy, results, presInit);
					roster_util.setPresenceSent(session, buddy, true);
				}
			} // end of for (String buddy: buddies)
		} // end of if (buddies == null)
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
	 * @param settings
	 *
	 * @throws TigaseDBException
	 */
	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {

		// Init plugin configuration
		skipOffline = Boolean.parseBoolean((String) settings.get(SKIP_OFFLINE_PROP_KEY));
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
	@SuppressWarnings({ "unchecked", "fallthrough" })
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results,
			final Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is null, ignoring packet: {0}", packet);
			}

			return;
		} // end of if (session == null)

		if (!session.isAuthorized()) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is not authorized, ignoring packet: {0}", packet);
			}

			return;
		}

		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		synchronized (session) {
			try {
				PresenceType pres_type = roster_util.getPresenceType(session, packet);

				if (pres_type == null) {
					log.log(Level.WARNING, "Invalid presence found: {0}", packet);

					return;
				} // end of if (type == null)

				StanzaType type = packet.getType();

				if (type == null) {
					type = StanzaType.available;
				} // end of if (type == null)

				// Not needed anymore. Packet filter does it for all stanzas.
				// // For all messages coming from the owner of this account set
				// // proper 'from' attribute
				// if (packet.getFrom().equals(session.getConnectionId())) {
				// packet.getElement().setAttribute("from", session.getJID());
				// } // end of if (packet.getFrom().equals(session.getConnectionId()))
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0} presence found: {1}", new Object[] { pres_type,
							packet });
				}

				// All 'in' subscription presences must have a valid from address
				switch (pres_type) {
					case in_unsubscribe:
					case in_subscribe:
					case in_unsubscribed:
					case in_subscribed:
						if (packet.getStanzaFrom() == null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("'in' subscription presence without valid 'from' address, "
										+ "dropping packet: " + packet);
							}

							return;
						}

						if (session.isUserId(packet.getStanzaFrom().getBareJID())) {
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE,
										"''in'' subscription to myself, not allowed, returning "
												+ "error for packet: " + "{0}", packet);
							}

							results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
									"You can not subscribe to yourself.", false));

							return;
						}

						break;

					case out_subscribe:
					case out_unsubscribe:
					case out_subscribed:
					case out_unsubscribed:

						// Check wheher the destination address is correct to prevent
						// broken/corrupted roster entries:
						if ((packet.getStanzaTo() == null)
								|| packet.getStanzaTo().toString().isEmpty()) {
							results.offer(Authorization.JID_MALFORMED.getResponseMessage(packet,
									"The destination address is incorrect.", false));

							return;
						}

						// According to RFC 3921 draft bis-3, both source and destination
						// addresses must be BareJIDs
						// No need for that, initVars(...) takes care of that
						// packet.getElement().setAttribute("from",
						// session.getJID().copyWithoutResource().toString());
						// packet.getElement().setAttribute("to",
						// packet.getStanzaTo().copyWithoutResource().toString());
						packet.initVars(session.getJID().copyWithoutResource(), packet.getStanzaTo()
								.copyWithoutResource());

						break;

					default:
						break;
				}

				switch (pres_type) {
					case out_initial:
						processOutInitial(packet, session, results, settings, pres_type);

						break;

					case out_subscribe:
					case out_unsubscribe:
						processOutSubscribe(packet, session, results, settings, pres_type);

						break;

					case out_subscribed:
					case out_unsubscribed:
						processOutSubscribed(packet, session, results, settings, pres_type);

						break;

					case in_initial:
						if (session.getPresence() == null) {
							// If the user has not yet sent initial presence then ignore the
							// probe.
							return;
						}
						if (type == StanzaType.unavailable) {
							roster_util.setOnline(session, packet.getStanzaFrom(), false);
						} else {
							buddyOnline(session, packet.getStanzaFrom(), results);
						}

						processInInitial(packet, session, results, settings, pres_type);

						break;

					case in_subscribe:
						processInSubscribe(packet, session, results, settings, pres_type);

						break;

					case in_unsubscribe:
						processInUnsubscribe(packet, session, results, settings, pres_type);

						break;

					case in_subscribed:
						processInSubscribed(packet, session, results, settings, pres_type);

						break;

					case in_unsubscribed:
						processInUnsubscribed(packet, session, results, settings, pres_type);

						break;

					case in_probe:
						if (session.getPresence() == null) {
							// If the user has not yet sent initial presence then ignore the
							// probe.
							return;
						}

						processInProbe(packet, session, results, settings, pres_type);
						// this is actually already handled in processInProbe
						// buddyOnline(session, packet.getStanzaFrom(), results);

						break;

					case error:
						processError(packet, session, results, settings, pres_type);

						break;

					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Request type is incorrect", false));

						break;
				} // end of switch (type)
			} catch (NotAuthorizedException e) {
				log.log(Level.INFO,
						"Can not access user Roster, user session is not authorized yet: {0}", packet);
				log.log(Level.FINEST, "presence problem...", e);

				// results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
				// "You must authorize session first.", true));
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Error accessing database for presence data: {0}", e);
			} // end of try-catch
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param results
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void resendPendingInRequests(XMPPResourceConnection session,
			Queue<Packet> results) throws NotAuthorizedException, TigaseDBException {

		JID[] buddies = roster_util.getBuddies(session, RosterAbstract.PENDING_IN);

		if (buddies != null) {
			for (JID buddy : buddies) {
				Element presence = new Element(PRESENCE_ELEMENT_NAME);

				presence.setAttribute("type", StanzaType.subscribe.toString());

				// presence.setAttribute("from", buddy.toString());
				presence.setXMLNS(XMLNS);

				Packet pres = Packet.packetInstance(presence, buddy, null);

				updatePresenceChange(pres, session, results);
			}
		}
	}

	/**
	 * <code>stopped</code> method is called when user disconnects or logs-out.
	 *
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @param results
	 * @param settings
	 */
	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results,
			Map<String, Object> settings) {

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
					if (!StanzaType.unavailable.toString().equals(pres.getAttribute("type"))) {
						pres.setAttribute("type", StanzaType.unavailable.toString());
						session.setPresence(pres);
					}
					broadcastOffline(session, results, settings, roster_util);
					updateOfflineChange(session, results);
				} else {
					broadcastDirectPresences(StanzaType.unavailable, session, results, null);
				}
			} catch (NotAuthorizedException e) {

				// Do nothing, it may happen quite often when the user disconnects
				// before
				// it authenticates
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Error accessing database for offline message: ", e);
			} // end of try-catch

		}
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

	protected RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}

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

		if ((resource != null) && !resource.isEmpty()) {
			XMPPResourceConnection direct =
					session.getParentSession().getResourceForResource(resource);

			if (direct != null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received direct presence from: {0} to: {1}",
							new Object[] { packet.getStanzaFrom(), packet.getStanzaTo() });
				}

				// Send a direct presence to correct resource, otherwise ignore
				Packet result = packet.copyElementOnly();

				result.setPacketTo(direct.getConnectionId());
				result.setPacketFrom(packet.getTo());
				results.offer(result);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Ignoring direct presence from: {0} to: {1}, resource gone.", new Object[] {
									packet.getStanzaFrom(), packet.getStanzaTo() });
				}
			}

			return;
		}

		if (session.getPresence() == null) {
			// Just ignore, this user does not want to receive presence updates
			return;
		}

		JID presBuddy = packet.getStanzaFrom().copyWithoutResource();

		// If other users are in 'to' or 'both' contacts, broadcast
		// their presences to all active resources
		if (roster_util.isSubscribedTo(session, presBuddy)
				|| (DynamicRoster.getBuddyItem(session, settings, presBuddy) != null)

		// ||
		// // This might be just unsubscribed buddy
		// (roster_util.isBuddyOnline(session, presBuddy))
		) {
			boolean online = StanzaType.unavailable != packet.getType();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Received initial presence, setting buddy: {0} online status to: {1}",
						new Object[] { packet.getStanzaFrom(), online });
			}
			roster_util.setOnline(session, packet.getStanzaFrom(), online);

			updatePresenceChange(packet, session, results);

			if (skipOffline && !roster_util.presenceSent(session, packet.getStanzaFrom())) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Presence not yet sent to this buddy, sending: {0}",
							new Object[] { packet.getStanzaFrom() });
				}
				sendPresence(null, null, packet.getStanzaFrom().copyWithoutResource(), results,
						session.getPresence());
				roster_util.setPresenceSent(session, packet.getStanzaFrom(), true);
			}
		} else {

		}
	}

	protected void processInProbe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType presenceType)
			throws NotAuthorizedException, TigaseDBException, PacketErrorTypeException {
		SubscriptionType buddy_subscr = null;

		if (DynamicRoster.getBuddyItem(session, settings, packet.getStanzaFrom()) != null) {
			buddy_subscr = SubscriptionType.both;
		} else {
			buddy_subscr = roster_util.getBuddySubscription(session, packet.getStanzaFrom());
		}

		if (buddy_subscr == null) {
			buddy_subscr = SubscriptionType.none;
		} // end of if (buddy_subscr == null)

		if (roster_util.isSubscribedFrom(buddy_subscr)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Received probe, setting buddy: {0} as online.",
						packet.getStanzaFrom());
			}

			roster_util.setOnline(session, packet.getStanzaFrom(), true);
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
				} catch (Exception e) {

					// It might be quite possible that one of the user connections
					// is in state not allowed for sending presence, in such a case
					// none of user connections would receive presence.
					// This catch is to make sure all other resources receive
					// notification.
				}
			}
		}
		// acording to spec 4.3.2.  Server Processing of Inbound Presence Probe
		// http://xmpp.org/rfcs/rfc6121.html#presence-probe-inbound
		// If the user's bare JID is in the contact's roster with a subscription state other
		// than "From", "From + Pending Out", or "Both", then the contact's server
		// SHOULD return a presence stanza of type "unsubscribed"
		else
		{
			if ( log.isLoggable( Level.FINEST ) ) {
				log.log( Level.FINEST, "Received probe, users bare JID: {0} is not in the roster. Responding with unsubscribed",
						 packet.getStanzaFrom().getBareJID() );
			}
			sendPresence( StanzaType.unsubscribed, session.getBareJID(), packet.getStanzaFrom().getBareJID(), results, null );
		} // end of if (roster_util.isSubscribedFrom(session, packet.getElemFrom()))
	}

	protected void processInSubscribe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {

		// If the buddy is already subscribed then auto-reply with subscribed
		// presence stanza.
		if (roster_util.isSubscribedFrom(session, packet.getStanzaFrom())) {
			sendPresence(StanzaType.subscribed, session.getJID().copyWithoutResource(),
					packet.getStanzaFrom(), results, null);
		} else {
			SubscriptionType curr_sub =
					roster_util.getBuddySubscription(session, packet.getStanzaFrom());

			if (curr_sub == null) {
				curr_sub = SubscriptionType.none;
				roster_util.addBuddy(session, packet.getStanzaFrom(), null, null, null);
			} // end of if (curr_sub == null)

			roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());
			updatePresenceChange(packet, session, results);
		} // end of else

		// We can't know that actually, this might come from offline storage
		// roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
	}

	protected void processInSubscribed(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {
		SubscriptionType curr_sub =
				roster_util.getBuddySubscription(session, packet.getStanzaFrom());

		if (curr_sub == null) {
			curr_sub = SubscriptionType.none;
			roster_util.addBuddy(session, packet.getStanzaFrom(), null, null, null);
		} // end of if (curr_sub == null)

		boolean subscr_changed =
				roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());

		if (subscr_changed) {

			Packet forward_p = packet.copyElementOnly();
			forward_p.setPacketTo(session.getConnectionId());
			results.offer(forward_p);
			// updatePresenceChange(packet.getElement(), session, results);
			roster_util.updateBuddyChange(session, results,
					roster_util.getBuddyItem(session, packet.getStanzaFrom()));
		}

		// We can't know that actually, this might come from offline storage
		// roster_util.setBuddyOnline(session, packet.getElemFrom(), true);
	}

	protected void processInUnsubscribe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {

		boolean subscr_changed =
				roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());

		if (subscr_changed) {
			// First forward the request to the client to make sure it stays in sync
			// with the server.
			// this should be done only in the case of actual change of the state
			Packet forward_p = packet.copyElementOnly();
			forward_p.setPacketTo(session.getConnectionId());
			results.offer(forward_p);

			// No longer needed according to RFC-3921-bis5
			// sendPresence(StanzaType.unsubscribed, session.getJID(),
			// packet.getElemFrom(),
			// results, null);
			// updatePresenceChange(packet.getElement(), session, results);
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
		}
	}

	protected void processInUnsubscribed(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {
		SubscriptionType curr_sub =
				roster_util.getBuddySubscription(session, packet.getStanzaFrom());

		if (curr_sub != null) {
			// First forward the request to the client to make sure it stays in sync
			// with
			// the server.
			Packet forward_p = packet.copyElementOnly();
			forward_p.setPacketTo(session.getConnectionId());
			results.offer(forward_p);
			boolean subscr_changed =
					roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());

			if (subscr_changed) {

				// No longer needed according to RFC-3921-bis5
				// updatePresenceChange(packet.getElement(), session, results);
				Element item = roster_util.getBuddyItem(session, packet.getStanzaFrom());

				// The roster item could have been removed in the meantime....
				if (item != null) {
					roster_util.updateBuddyChange(session, results,
							roster_util.getBuddyItem(session, packet.getStanzaFrom()));
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Received unsubscribe request from a user who is not in the roster: {0}",
								packet.getStanzaFrom());
					}
				}
			}
		}
	}

	protected void processOutInitial(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType type)
			throws NotAuthorizedException, TigaseDBException {

		// if (session != null) {
		// throw new NullPointerException("THIS IS A TEST!!");
		// }
		// Is it a direct presence to some entity on the network?
		if (packet.getStanzaTo() != null) {

			// Yes this is it, send direct presence
			// if (session.isAnonymous()) {
			// outInitialAnonymous(packet, session, results);
			// }
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
			packet.initVars(session.getJID(), packet.getStanzaTo());

			// Store user presence for later time...
			// To send response to presence probes for example.
			session.setPresence(packet.getElement());

			// Special actions on the first availability presence
			if ((packet.getType() == null) || (packet.getType() == StanzaType.available)) {
				session.removeSessionData(OFFLINE_BUD_SENT);
				session.removeSessionData(OFFLINE_RES_SENT);

				if (first) {

					// Send presence probes to 'to' or 'both' contacts
					broadcastProbe(session, results, settings);

					// Resend pending in subscription requests
					resendPendingInRequests(session, results);

					// Broadcast initial presence to 'from' or 'both' contacts
					// sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
					// results, packet.getElement(), settings, false);
					// These below broadcast is handled by presenceProbe method
					// to improve buddy checks....
					// sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
					// results, packet.getElement(), settings);
				} else {

					// Broadcast initial presence to 'from' or 'both' contacts
					// sendPresenceBroadcast(type, session, FROM_SUBSCRIBED,
					// results, packet.getElement(), settings, false);
					sendPresenceBroadcast(StanzaType.available, session, FROM_SUBSCRIBED, results,
							packet.getElement(), settings, roster_util);
				}

				// Broadcast initial presence to other available user resources
				// Element presence = packet.getElement().clone();
				// Already done above, don't need to set it again here
				// presence.setAttribute("from", session.getJID());
				updateUserResources(packet.getElement(), session, results, first);
			} else {
				stopped(session, results, settings);
			}
		}
	}

	protected void processOutSubscribe(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {

		// According to RFC-3921 I must forward all these kind presence
		// requests, it allows to resynchronize
		// subscriptions in case of synchronization loss
		forwardPresence(results, packet, session.getJID().copyWithoutResource());

		SubscriptionType current_subscription =
				roster_util.getBuddySubscription(session, packet.getStanzaTo());
		if (pres_type == PresenceType.out_subscribe) {

			if (current_subscription == null) {
				roster_util.addBuddy(session, packet.getStanzaTo(), null, null, null);
			} // end of if (current_subscription == null)
			boolean subscr_changed =
					roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaTo());

			if (subscr_changed) {
				roster_util.updateBuddyChange(session, results,
						roster_util.getBuddyItem(session, packet.getStanzaTo()));
			} // end of if (subscr_changed)
		} else {
			if (SUB_NONE.contains(current_subscription)) {
				roster_util.removeBuddy(session, packet.getStanzaTo());
			} // end of if (current_subscription == null)
		}

	}

	protected void processOutSubscribed(Packet packet, XMPPResourceConnection session,
			Queue<Packet> results, Map<String, Object> settings, PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException {

		// According to RFC-3921 I must forward all these kind presence
		// requests, it allows to re-synchronize
		// subscriptions in case of synchronization loss
		forwardPresence(results, packet, session.getJID().copyWithoutResource());

		Element initial_presence = session.getPresence();
		JID buddy = packet.getStanzaTo().copyWithoutResource();
		boolean subscr_changed =
				roster_util.updateBuddySubscription(session, pres_type, buddy);

		if (subscr_changed) {
			roster_util.updateBuddyChange(session, results,
					roster_util.getBuddyItem(session, buddy));

			if (initial_presence != null) {
				if (pres_type == PresenceType.out_subscribed) {
					sendPresence(StanzaType.available, null, buddy, results, initial_presence);
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					sendPresence(StanzaType.unavailable, session.getJID(), buddy, results, null);
				}
			} // end of if (subscr_changed)
		}
	}

	protected void buddyOnline(XMPPResourceConnection session, JID buddy,
			Queue<Packet> results) throws NotAuthorizedException, TigaseDBException {
		roster_util.setOnline(session, buddy, true);

		if (skipOffline && !roster_util.presenceSent(session, buddy)
				&& roster_util.isSubscribedFrom(session, buddy)) {
			Element pres = session.getPresence();

			if (pres != null) {
				sendPresence(null, null, buddy, results, pres);
				roster_util.setPresenceSent(session, buddy, true);
			}
		}
	}
} // Presence
