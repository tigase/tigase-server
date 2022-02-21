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

import tigase.db.TigaseDBException;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.sys.TigaseRuntime;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.roster.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.roster.RosterAbstract.*;

/**
 * Class responsible for handling Presence packets
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public abstract class PresenceAbstract
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc {

	public static final String PRESENCE_ELEMENT_NAME = "presence";

	public static final String SKIP_OFFLINE_PROP_KEY = "skip-offline";

	public static final String PRESENCE_PROBE_FULL_JID_KEY = "probe-full-jid";

	public static final String SKIP_OFFLINE_SYS_PROP_KEY = "skip-offline-sys";

	public static final String USERS_STATUS_CHANGES = "Users status changes";

	protected static final String XMLNS = CLIENT_XMLNS;

	private static final Logger log = Logger.getLogger(PresenceAbstract.class.getName());
	//private static final String[]   PRESENCE_PRIORITY_PATH  = { "presence", "priority" };
	//private static final String[]   XMLNSS                  = { XMLNS, RosterAbstract.XMLNS_LOAD };
	@ConfigField(desc = "Probe full JID", alias = PRESENCE_PROBE_FULL_JID_KEY)
	protected static boolean probeFullJID = false;
	@ConfigField(desc = "Skip offline", alias = SKIP_OFFLINE_PROP_KEY)
	protected static boolean skipOffline = false;
	@ConfigField(desc = "Skip offline sys", alias = SKIP_OFFLINE_SYS_PROP_KEY)
	private static boolean skipOfflineSys = true;

	protected RosterAbstract roster_util = getRosterUtil();

	// This is required to make sure that dynamic roster will get initialized
	@Inject(nullAllowed = true)
	private DynamicRoster dynamicRoster;

	/**
	 * Simply forwards packet to the destination
	 *
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param packet to forward
	 * @param from is a <code>JID</code> instance with stanza source address.
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
	 * Returns shared instance of class implementing {@link RosterAbstract} - either default one ({@link RosterFlat}) or
	 * the one configured with <em>"roster-implementation"</em> property.
	 *
	 * @return shared instance of class implementing {@link RosterAbstract}
	 */
	protected static RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}

	protected static Element prepareProbe(XMPPResourceConnection session) throws NotAuthorizedException {
		Element presProbe = new Element(PRESENCE_ELEMENT_NAME);
		presProbe.setXMLNS(XMLNS);
		presProbe.setAttribute("type", StanzaType.probe.toString());
		if (probeFullJID) {
			presProbe.setAttribute("from", session.getJID().toString());
		} else {
			presProbe.setAttribute("from", session.getBareJID().toString());
		}
		return presProbe;
	}

	/**
	 * Method checks whether a given contact requires sending presence. In case of enabling option {@code skipOffline}
	 * and user being offline in the roster the presence is not sent. Alternatively enabling option {@code
	 * skipOfflineSys} would cause local environment check for user status and omit sending presence if the local use is
	 * offline.
	 *
	 * @param roster instance of class implementing {@link RosterAbstract}.
	 * @param buddy JID of a contact for which a check is to be performed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param systemCheck indicates whether the check should be only based on local environment state ({@code true}) or
	 * rooster state of given user should also be taken into consideration ({@code false}).
	 *
	 * @return {code true} if the contact requires sending presence (e.g. is not online and options skipOffline or
	 * skipOfflineSys are enabled)
	 *
	 */
	protected static boolean requiresPresenceSending(RosterAbstract roster, JID buddy, XMPPResourceConnection session,
													 boolean systemCheck)
			throws NotAuthorizedException, TigaseDBException {
		boolean result = true;

		// if non-system check is enabled during broadcast of non-first initial
		// presence or offline presence
		if (!systemCheck) {
			boolean isOnline = roster.isOnline(session, buddy);
			if (skipOffline && !isOnline) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0} | buddy: {1} is online: {2}",
							new Object[]{session.getJID(), buddy, isOnline});
				}
				result = result && false;
			}
		}
		if (skipOfflineSys) {
			TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();
			boolean isJidOnline = runtime.isJidOnline(buddy);

			if (runtime.hasCompleteJidsInfo() && session.isLocalDomain(buddy.getDomain(), false) && !isJidOnline) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0} | buddy: {1} is online (sys): {2}",
							new Object[]{session.getJID(), buddy, isJidOnline});
				}
				result = result && false;
			}
		}

		return result;
	}

	/**
	 * Sends Presence stanza from provided parameters as well as returns created result {@link Packet} object. In case
	 * of missing {@code  pres} parameter a Presence stanza will be created with provided {@link StanzaType} type {@code
	 * t}, {@link JID} type {@code from} and {@link JID} type {@code to}. Otherwise Presence stanza {@code pres} will be
	 * cloned and {@code to} attribute will be set from parameter {@code to}.
	 *
	 * @param t specifies type of the presence to be send.
	 * @param from is a <code>JID</code> instance with stanza source address.
	 * @param to is a <code>JID</code> instance with stanza destination address.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param pres an Object of type {@link Element} holding Presence stanza to be sent.
	 *
	 * @return an instance of {@link Packet} holding Presence stanza created from provided parameters.
	 */
	public static Packet sendPresence(StanzaType t, JID from, JID to, Queue<Packet> results, Element pres) {
		Objects.requireNonNull(from);
		Objects.requireNonNull(to);
		Element presence = null;
		Packet result = null;

		if (pres == null) {
			presence = new Element(PRESENCE_ELEMENT_NAME);
			if (t != null) {
				presence.setAttribute("type", t.toString());
			}    // end of if (t != null)
			else {
				presence.setAttribute("type", StanzaType.unavailable.toString());
			}    // end of if (t != null) else
			if (null != from) {
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
			log.log(Level.FINE, "Packet stringprep addressing problem, skipping presence send: {0}", presence);
		}

		return result;
	}

	/**
	 * Sends Presence stanza from provided parameters without returning created result {@link Packet} object. In case of
	 * missing {@code  pres} parameter a Presence stanza will be created with provided {@link StanzaType} type {@code
	 * t}, {@link JID} type {@code from} and {@link JID} type {@code to}. Otherwise Presence stanza {@code pres} will be
	 * cloned and {@code to} attribute will be set from parameter {@code to}.
	 *
	 * @param t specifies type of the presence to be send.
	 * @param from is a <code>JID</code> instance with stanza source address.
	 * @param to is a <code>JID</code> instance with stanza destination address.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param pres an Object of type {@link Element} holding Presence stanza to be sent.
	 */
	public static void sendPresence(StanzaType t, BareJID from, BareJID to, Queue<Packet> results, Element pres) {
		sendPresence(t, JID.jidInstance(from), JID.jidInstance(to), results, pres);
	}

	/**
	 * <code>updatePresenceChange</code> method is used to broadcast to all active resources presence stanza received
	 * from other users, like incoming availability presence, subscription presence and so on... Initial presences are
	 * however sent only to those resources which already have sent initial presence.
	 *
	 * @param presence an <code>Element</code> presence received from other users, we have to change 'to' attribute to
	 * full resource JID.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 *
	 * @throws NotAuthorizedException if an error occurs
	 */
	public static void updatePresenceChange(Packet presence, XMPPResourceConnection session, Queue<Packet> results)
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
					log.finest("Skipping update presence change for a resource which hasn't sent " +
									   "initial presence yet, or is remote connection: " + conn);
				}
			} else {
				try {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Update presence change to: " + conn);
					}

					// Send to old resource presence about new resource
					Packet pres_update = presence.copyElementOnly();

					pres_update.initVars(presence.getStanzaFrom(), conn.getJID().copyWithoutResource());
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
	 * <code>updateUserResources</code> method is used to broadcast to all <strong>other</strong> resources presence
	 * stanza from one user resource. So if new resource connects this method updates presence information about new
	 * resource to old resources and about old resources to new resource.
	 *
	 * @param presence an <code>Element</code> presence received from other users, we have to change 'to' attribute to
	 * full resource JID.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param initial specifies whether this is an initial presence or not (i.e. if there is a presence data from the
	 * presence stored within user session object or not)
	 *
	 * @throws NotAuthorizedException if an error occurs
	 */
	public static void updateUserResources(Element presence, XMPPResourceConnection session, Queue<Packet> results,
										   boolean initial) throws NotAuthorizedException {
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
					Packet pack_update = Packet.packetInstance(pres_update, session.getJID(),
															   conn.getJID().copyWithoutResource());

					pack_update.setPacketTo(conn.getConnectionId());
					results.offer(pack_update);

					Element presence_el = conn.getPresence();

					// Send to new resource last presence sent by the old resource
					if ((presence_el != null) && initial && (conn != session)) {
						pres_update = presence_el.clone();
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
			} catch (NotAuthorizedException | NoConnectionIdException e) {

				// It might be quite possible that one of the user connections
				// is in state not allowed for sending presence, in such a case
				// none of user connections would receive presence.
				// This catch is to make sure all other resources receive notification.
			}
		}        // end of for (XMPPResourceConnection conn: sessions)
	}

	public boolean isSkipOfflineSys() {
		return skipOfflineSys;
	}

	public void setSkipOfflineSys(boolean skipOfflineSys) {
		PresenceAbstract.skipOfflineSys = skipOfflineSys;
	}

	public boolean isSkipOffline() {
		return skipOffline;
	}

	public void setSkipOffline(boolean skipOffline) {
		PresenceAbstract.skipOffline = skipOffline;
	}

	public boolean getProbeFullJID() {
		return probeFullJID;
	}

	public void setProbeFullJID(boolean probeFullJID) {
		PresenceAbstract.probeFullJID = probeFullJID;
	}

//	/**
//	 * Method updates resources information upon receiving initial availability
//	 * presence (type available or missing type)
//	 *
//	 *
//	 * @param session user session which keeps all the user session data and also
//	 *                gives an access to the user's repository data.
//	 * @param type    specifies type of the stanza.
//	 * @param packet  packet is which being processed.
//	 */
//	protected static void updateResourcesAvailable(XMPPResourceConnection session,
//			StanzaType type, Packet packet) {
//		XMPPSession parentSession = session.getParentSession();
//
//		if (parentSession != null) {
//			Map<JID, Map> resources;
//			boolean       online = (type == null) || (type == StanzaType.available);
//
//			synchronized (parentSession) {
//				resources = (Map<JID, Map>) parentSession.getCommonSessionData(
//						XMPPResourceConnection.ALL_RESOURCES_KEY);
//				if (resources == null) {
//					if (!online) {
//						return;
//					}
//					resources = new ConcurrentHashMap<>();
//					session.putCommonSessionData(XMPPResourceConnection.ALL_RESOURCES_KEY,
//							resources);
//				}
//			}
//			if (online) {
//				Map map = resources.get(packet.getStanzaFrom());
//
//				if (map == null) {
//					map = new ConcurrentHashMap();
//					resources.put(packet.getStanzaFrom(), map);
//				}
//
//				String priorityStr = packet.getElemCDataStaticStr(PRESENCE_PRIORITY_PATH);
//
//				if (priorityStr != null) {
//					map.put(XMPPResourceConnection.ALL_RESOURCES_PRIORITY_KEY, Integer.parseInt(
//							priorityStr));
//				} else if (!map.containsKey(XMPPResourceConnection.ALL_RESOURCES_PRIORITY_KEY)) {
//					map.put(XMPPResourceConnection.ALL_RESOURCES_PRIORITY_KEY, 0);
//				}
//
//				Element c = packet.getElement().getChild("c", "http://jabber.org/protocol/caps");
//
//				if (c != null) {
//					map.put(XMPPResourceConnection.ALL_RESOURCES_CAPS_KEY,
//							PresenceCapabilitiesManager.processPresence(c));
//				}
//			} else {
//				resources.remove(packet.getStanzaFrom());
//			}
//		}
//	}

	/**
	 * <code>sendPresenceBroadcast</code> method broadcasts given presence to all buddies from roster and to all users
	 * to which direct presence was sent. Before sending presence method calls {@code  requiresPresenceSending()},
	 * configured to only check local environment status (if enabled) to verify whether presence needs to be sent.
	 *
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 *
	 * @throws NotAuthorizedException if an error occurs
	 */
	public void broadcastProbe(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings)
			throws NotAuthorizedException, TigaseDBException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Broadcasting probes for: {0}", session);
		}

		// Probe is always broadcasted with initial presence
		Element presInit = session.getPresence();
		Element presProbe = prepareProbe(session);

		JID[] buddies = roster_util.getBuddies(session, SUB_BOTH);

		try {
			buddies = DynamicRoster.addBuddies(session, settings, buddies, SUB_BOTH);
		} catch (RosterRetrievingException | RepositoryAccessException ex) {

			// Ignore, handled in the JabberIqRoster code
		}
		if (buddies != null) {
			for (JID buddy : buddies) {
				if (requiresPresenceSending(roster_util, buddy, session, true)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Sending presence probe to: " + buddy);
					}
					sendPresence(null, session.getBareJID(), buddy.getBareJID(), results, presProbe);
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Sending initial presence to: " + buddy);
					}
					sendPresence(null, session.getBareJID(), buddy.getBareJID(), results, presInit);
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								session.getBareJID() + " | Skipping sending initial presence and probe to: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		JID[] buddies_to = roster_util.getBuddies(session, SUB_TO);

		try {
			buddies_to = DynamicRoster.addBuddies(session, settings, buddies_to, SUB_TO);
		} catch (RosterRetrievingException | RepositoryAccessException ex) {
			// Ignore, handled in the JabberIqRoster code
		}

		if (buddies_to != null) {
			for (JID buddy : buddies_to) {
				if (requiresPresenceSending(roster_util, buddy, session, true)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Sending probe to: " + buddy);
					}
					sendPresence(null, session.getBareJID(), buddy.getBareJID(), results, presProbe);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Skipping sending presence probe to: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)

		// TODO: It might be a marginal number of cases here but just make it clear
		// we send a presence here regardless
		JID[] buddies_from = roster_util.getBuddies(session, SUB_FROM);

		try {
			buddies_from = DynamicRoster.addBuddies(session, settings, buddies_from, SUB_FROM);
		} catch (RosterRetrievingException | RepositoryAccessException ex) {
			// Ignore, handled in the JabberIqRoster code
		}

		if (buddies_from != null) {
			for (JID buddy : buddies_from) {
				if (requiresPresenceSending(roster_util, buddy, session, true)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, session.getBareJID() + " | Sending initial presence to: " + buddy);
					}
					sendPresence(null, session.getBareJID(), buddy.getBareJID(), results, presInit);
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								session.getBareJID() + " | Skipping sending initial presence and probe to: " + buddy);
					}
				}
			}    // end of for (String buddy: buddies)
		}      // end of if (buddies == null)
	}

	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}

} 
