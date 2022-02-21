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
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.PolicyViolationException;
import tigase.server.xmppsession.SessionManager;
import tigase.vhosts.*;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.PresenceType;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.roster.RosterAbstract.SUB_NONE;

/**
 * @author andrzej
 */
@Id(PresenceSubscription.ID)
@Handle(path = {PresenceAbstract.PRESENCE_ELEMENT_NAME}, xmlns = PresenceAbstract.CLIENT_XMLNS)
@Bean(name = PresenceSubscription.ID, parent = SessionManager.class, active = true)
public class PresenceSubscription
		extends PresenceAbstract {

	/**
	 * key allowing enabling automatic authorisation.
	 */
	public static final String AUTO_AUTHORIZE_PROP_KEY = "auto-authorize";
	protected static final String ID = "presence-subscription";
	private static final Logger log = Logger.getLogger(PresenceSubscription.class.getCanonicalName());
	private static final Set<StanzaType> TYPES = new HashSet<>(
			Arrays.asList(StanzaType.subscribe, StanzaType.subscribed, StanzaType.unsubscribe,
						  StanzaType.unsubscribed));
	/**
	 * variable holding setting regarding auto authorisation of items added to user roset
	 */
	@ConfigField(desc = "Automatically authorize subscription requests", alias = AUTO_AUTHORIZE_PROP_KEY)
	private static boolean autoAuthorize = false;
	
	@Inject(nullAllowed = true)
	protected VHostManagerIfc vHostManager = null;

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

		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
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

				// All 'in' subscription presences must have a valid from address
				switch (pres_type) {
					case in_unsubscribe:
					case in_subscribe:
					case in_unsubscribed:
					case in_subscribed:
						if (packet.getStanzaFrom() == null) {
							if (log.isLoggable(Level.FINE)) {
								log.fine("'in' subscription presence without valid 'from' address, " +
												 "dropping packet: " + packet);
							}

							return;
						}
						if (session.isUserId(packet.getStanzaFrom().getBareJID())) {
							if (log.isLoggable(Level.FINE)) {
								log.log(Level.FINE, "''in'' subscription to myself, not allowed, returning " +
										"error for packet: " + "{0}", packet);
							}
							results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
																					   "You can not subscribe to yourself.",
																					   false));

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
						packet.initVars(packet.getStanzaFrom().copyWithoutResource(),
										session.getJID().copyWithoutResource());

						break;

					case out_subscribe:
					case out_unsubscribe:
					case out_subscribed:
					case out_unsubscribed:

						// Check wheher the destination address is correct to prevent
						// broken/corrupted roster entries:
						if ((packet.getStanzaTo() == null) || packet.getStanzaTo().toString().isEmpty()) {
							results.offer(Authorization.JID_MALFORMED.getResponseMessage(packet,
																						 "The destination address is incorrect.",
																						 false));

							return;
						}

						// According to RFC 3921 draft bis-3, both source and destination
						// addresses must be BareJIDs, handled by initVars(...)
						packet.initVars(session.getJID().copyWithoutResource(),
										packet.getStanzaTo().copyWithoutResource());

						break;

					default:
						break;
				}
				switch (pres_type) {
					case out_subscribe:
					case out_unsubscribe:
						processOutSubscribe(packet, session, results, settings, pres_type);

						break;

					case out_subscribed:
					case out_unsubscribed:
						processOutSubscribed(packet, session, results, settings, pres_type);

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

					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Request type is incorrect",
																				   false));

						break;
				}    // end of switch (type)
			} catch (NotAuthorizedException e) {
				log.log(Level.INFO, "Can not access user Roster, user session is not authorized yet: {0}", packet);
				log.log(Level.FINEST, "presence problem...", e);
			} catch (PolicyViolationException e) {
				log.log(Level.FINE, "Violation of roster items number policy: {0}", packet);
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Error accessing database for presence data: {0}", e);
			}    // end of try-catch
		}

	}

	/**
	 * Method is responsible for processing incoming subscription request (i.e. in the receivers session manager).
	 * <br>
	 * If the contact is already subscribed the an auto-reply with type='subscribded' is sent, otherwise contact is
	 * added to the roster (if it's missing/there is no current subscription), sets the subscription type to {@code
	 * PresenceType.in_subscribe} and subsequently broadcast presence update to all connected resources.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 */
	protected void processInSubscribe(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
									  Map<String, Object> settings, RosterAbstract.PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {

		// If the buddy is already subscribed then auto-reply with subscribed
		// presence stanza.
		if (roster_util.isSubscribedFrom(session, packet.getStanzaFrom())) {
			sendPresence(StanzaType.subscribed, session.getJID().copyWithoutResource(), packet.getStanzaFrom(), results,
						 null);
		} else {
			RosterAbstract.SubscriptionType curr_sub = roster_util.getBuddySubscription(session,
																						packet.getStanzaFrom());

			if (curr_sub == null) {
				roster_util.addBuddy(session, packet.getStanzaFrom(), null, null,null, null);
			}    // end of if (curr_sub == null)
			final boolean preApproved = roster_util.isPreApproved(session, packet.getStanzaFrom());
			roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());
			if (!isAutoAuthorizeEnabled(session.getJID().getDomain())) {
				// broadcast the subscription request only if it wasn't pre-approved
				if (!preApproved) {
					updatePresenceChange(packet, session, results);
				} else {
					// was pre-approved, update status and send probe
					final Element buddyItem = roster_util.getBuddyItem(session,
																	   packet.getStanzaFrom().copyWithoutResource());
					roster_util.updateBuddyChange(session, results, buddyItem);
					broadcastProbe(session, results, settings);
					sendPresence(StanzaType.subscribed, session.getJID(), packet.getStanzaFrom(), results, null);
				}
			} else {
				roster_util.setBuddySubscription(session, RosterAbstract.SubscriptionType.both,
												 packet.getStanzaFrom().copyWithoutResource());
			}
		}    // end of else
		if (isAutoAuthorizeEnabled(session.getJID().getDomain())) {
			final Element buddyItem = roster_util.getBuddyItem(session, packet.getStanzaFrom().copyWithoutResource());
			roster_util.updateBuddyChange(session, results, buddyItem);
			broadcastProbe(session, results, settings);
			sendPresence(StanzaType.subscribed, session.getJID(), packet.getStanzaFrom(), results, null);
		}
	}

	/**
	 * Method is responsible for processing incoming subscribed presence (i.e. in the receivers session manager).
	 * <br>
	 * Contact is added to the roster (if it's missing/there is no current subscription), sets the subscription type to
	 * {@code PresenceType.in_subscribed} and subsequently, if subscription has changed,forwards the presence to user
	 * resource connection as well as broadcast presence update to all connected resources.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 */
	protected void processInSubscribed(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
									   Map<String, Object> settings, RosterAbstract.PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {
		RosterAbstract.SubscriptionType curr_sub = roster_util.getBuddySubscription(session, packet.getStanzaFrom());

		if (!isAutoAuthorizeEnabled(session.getJID().getDomain()) && (curr_sub == null)) {
			roster_util.addBuddy(session, packet.getStanzaFrom(), null, null, null, null);
		}    // end of if (curr_sub == null)

		boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());

		if (subscr_changed) {
			Packet forward_p = packet.copyElementOnly();

			forward_p.setPacketTo(session.getConnectionId());
			results.offer(forward_p);
			if (isAutoAuthorizeEnabled(session.getJID().getDomain())) {
				roster_util.setBuddySubscription(session, RosterAbstract.SubscriptionType.both,
												 packet.getStanzaFrom().copyWithoutResource());
			}
			roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(session, packet.getStanzaFrom()));

			Element delay = packet.getElement().getChild("delay", "urn:xmpp:delay");
			if (delay != null) {
				// offline packet, lets send probe
				Element presProbe = prepareProbe(session);
				sendPresence(null, session.getJID(), packet.getStanzaFrom(), results, presProbe);
			}

		}
	}

	/**
	 * Method is responsible for processing incoming unsubscribe presence (i.e. in the receivers session manager).
	 * <br>
	 * First method performs update of subscription of the given contact and subsequently the request is forwarded to
	 * the client to make sure it says in synch with the server (in case there was actual change in subscription).
	 * Lastly a roster push is generated to all connected resources to update them with current state of the roster and
	 * items subscriptions.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 */
	protected void processInUnsubscribe(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
										Map<String, Object> settings, RosterAbstract.PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {
		boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());

		if (subscr_changed) {

			// First forward the request to the client to make sure it stays in sync
			// with the server. This should be done only in the case of actual change of the state
			// and with auto-authorization disabled
			if (!isAutoAuthorizeEnabled(session.getJID().getDomain())) {
				Packet forward_p = packet.copyElementOnly();

				forward_p.setPacketTo(session.getConnectionId());
				results.offer(forward_p);
			}

			Element item = roster_util.getBuddyItem(session, packet.getStanzaFrom());

			if (item != null) {
				roster_util.updateBuddyChange(session, results, item);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received unsubscribe request from a user who is not in the roster: {0}",
							packet.getStanzaFrom());
				}
			}
			if (isAutoAuthorizeEnabled(session.getJID().getDomain())) {
				broadcastProbe(session, results, settings);
			}
		}
	}

	/**
	 * Method is responsible for processing incoming unsubscribed presence (i.e. in the receivers session manager).
	 * <br>
	 * First method checks for the current subscription of the contact and if this verifies performs subsequent actions
	 * such as forwarding presence to the user connection to make sure it says in synch with the server, updates contact
	 * subscription with {@code PresenceType.in_unsubscribed} and in case that there was a change in user subscription
	 * send out a roster push to all connected resources to update them with current state of the roster and items
	 * subscriptions.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 */
	protected void processInUnsubscribed(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
										 Map<String, Object> settings, RosterAbstract.PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {
		RosterAbstract.SubscriptionType curr_sub = roster_util.getBuddySubscription(session, packet.getStanzaFrom());

		if (curr_sub != null) {

			// First forward the request to the client to make sure it stays in sync
			// with the server. This should be done only with auto-authorization disabled
			if (!isAutoAuthorizeEnabled(session.getJID().getDomain())) {
				Packet forward_p = packet.copyElementOnly();

				forward_p.setPacketTo(session.getConnectionId());
				results.offer(forward_p);
			}

			boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaFrom());

			if (subscr_changed) {
				Element item = roster_util.getBuddyItem(session, packet.getStanzaFrom());

				// The roster item could have been removed in the meantime....
				if (item != null) {
					roster_util.updateBuddyChange(session, results,
												  roster_util.getBuddyItem(session, packet.getStanzaFrom()));
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Received unsubscribe request from a user who is not in the roster: {0}",
								packet.getStanzaFrom());
					}
				}
				if (isAutoAuthorizeEnabled(session.getJID().getDomain())) {
					broadcastProbe(session, results, settings);
				}
			}
		}
	}

	/**
	 * Method is responsible for processing outgoing subscribe and unsubscribe presence (i.e. in the sender session
	 * manager).
	 * <br>
	 * Presence packet is forwarded to the destination with the JID stripped from the resource. <p>In case of {@code
	 * PresenceType.out_subscribe} packet type contact is added to the roster (in case it was missing), a subscription
	 * state is being updated and, in case there was a change, a roster push is being sent to all user resources.
	 * <br>
	 * In case of {@code PresenceType.out_unsubscribe} method updates contact subscription (and generates roster push if
	 * there was a change) and if the resulting contact subscription is NONE then contact is removed from the roster.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 */
	protected void processOutSubscribe(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
									   Map<String, Object> settings, RosterAbstract.PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {

		// According to RFC-3921 I must forward all these kind presence
		// requests, it allows to resynchronize
		// subscriptions in case of synchronization loss
		boolean subscr_changed = false;

		forwardPresence(results, packet, session.getJID().copyWithoutResource());

		RosterAbstract.SubscriptionType current_subscription = roster_util.getBuddySubscription(session,
																								packet.getStanzaTo());

		if (pres_type == RosterAbstract.PresenceType.out_subscribe) {
			if (current_subscription == null) {
				roster_util.addBuddy(session, packet.getStanzaTo(), null, null, null, null);
			}    // end of if (current_subscription == null)
			subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaTo());
			if (isAutoAuthorizeEnabled(session.getJID().getDomain())) {
				roster_util.setBuddySubscription(session, RosterAbstract.SubscriptionType.both,
												 packet.getStanzaTo().copyWithoutResource());
			}
			if (subscr_changed) {
				roster_util.updateBuddyChange(session, results,
											  roster_util.getBuddyItem(session, packet.getStanzaTo()));
			}    // end of if (subscr_changed)
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "out_subscribe: current_subscription = " + current_subscription);
			}
			if (current_subscription != null) {
				subscr_changed = roster_util.updateBuddySubscription(session, pres_type, packet.getStanzaTo());
				current_subscription = roster_util.getBuddySubscription(session, packet.getStanzaTo());
				if (subscr_changed) {
					roster_util.updateBuddyChange(session, results,
												  roster_util.getBuddyItem(session, packet.getStanzaTo()));
				}    // end of if (subscr_changed)
				if (SUB_NONE.contains(current_subscription)) {
					roster_util.removeBuddy(session, packet.getStanzaTo());
				}    // end of if (current_subscription == null)
			}
		}
	}

	/**
	 * Method is responsible for processing outgoing subscribed and unsubscribed presence (i.e. in the sender session
	 * manager).
	 * <br>
	 * Presence packet is forwarded to the destination with the JID stripped from the resource, a subscription state is
	 * being updated and, in case there was a change, a roster push is being sent to all user resources. Also, in case
	 * of presence type out_subscribed server send current presence to the user from each of the contact's available
	 * resources. For the presence type out_unsubscribed an unavailable presence is sent.
	 *
	 * @param packet packet is which being processed.
	 * @param session user session which keeps all the user session data and also gives an access to the user's
	 * repository data.
	 * @param results this a collection with packets which have been generated as input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the Tigase server configuration.
	 * @param pres_type specifies type of the presence.
	 *
	 */
	protected void processOutSubscribed(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
										Map<String, Object> settings, RosterAbstract.PresenceType pres_type)
			throws NotAuthorizedException, TigaseDBException, NoConnectionIdException, PolicyViolationException {

		// According to RFC-3921 I must forward all these kind presence
		// requests, it allows to re-synchronize
		// subscriptions in case of synchronization loss

		Element initial_presence = session.getPresence();
		JID buddy = packet.getStanzaTo().copyWithoutResource();
		boolean subscr_changed = roster_util.updateBuddySubscription(session, pres_type, buddy);

		final boolean isPreApproved = roster_util.isPreApproved(session, packet.getStanzaTo());
		if (isAutoAuthorizeEnabled(session.getJID().getDomain()) &&
				(pres_type == RosterAbstract.PresenceType.out_subscribed)) {
			roster_util.setBuddySubscription(session, RosterAbstract.SubscriptionType.both,
											 buddy.copyWithoutResource());
		}

		// do not forward the subscribed if it's a pre-approval
		if (!isPreApproved) {
			forwardPresence(results, packet, session.getJID().copyWithoutResource());
		}

		if (subscr_changed) {
			roster_util.updateBuddyChange(session, results, roster_util.getBuddyItem(session, buddy));
			if (initial_presence != null && !isPreApproved) {
				if (pres_type == RosterAbstract.PresenceType.out_subscribed) {

					// The contact's server MUST then also send current presence to the user
					// from each of the contact's available resources.
					List<XMPPResourceConnection> activeSessions = session.getActiveSessions();

					for (XMPPResourceConnection userSessions : activeSessions) {
						Element presence = userSessions.getPresence();

						sendPresence(StanzaType.available, userSessions.getjid(), buddy, results, presence);
					}
					roster_util.setPresenceSent(session, buddy, true);
				} else {
					sendPresence(StanzaType.unavailable, session.getJID(), buddy, results, null);
				}
			}    // end of if (subscr_changed)
		}
	}

	private boolean isAutoAuthorizeEnabled(String domain) {

		AUTO_AUTHORIZE_MODE mode = AUTO_AUTHORIZE_MODE.global;
		if (vHostManager != null) {
			PresenceSubscriptionVHostItemExtension extension = vHostManager.getVHostItem(domain).getExtension(PresenceSubscriptionVHostItemExtension.class);
			if (extension != null) {
				mode = extension.getAutoAuthorizeMode();
			}
		}

		if (AUTO_AUTHORIZE_MODE.global.equals(mode)) {
			return autoAuthorize;
		} else {
			return mode.isEnabled();
		}
	}

	enum AUTO_AUTHORIZE_MODE {
		global,
		on(true),
		off(false);

		private static String[] names = null;
		private boolean enabled;

		AUTO_AUTHORIZE_MODE() {
			enabled = false;
		}

		AUTO_AUTHORIZE_MODE(boolean b) {
			enabled = b;
		}

		protected boolean isEnabled() {
			return enabled;
		}
	}

	@Bean(name = PresenceSubscriptionVHostItemExtension.ID, parent = VHostItemExtensionManager.class, active = true)
	public static class PresenceSubscriptionVHostItemExtensionProvider implements VHostItemExtensionProvider<PresenceSubscriptionVHostItemExtension> {

		@Override
		public String getId() {
			return PresenceSubscriptionVHostItemExtension.ID;
		}

		@Override
		public Class<PresenceSubscriptionVHostItemExtension> getExtensionClazz() {
			return PresenceSubscriptionVHostItemExtension.class;
		}
	}

	public static class PresenceSubscriptionVHostItemExtension extends AbstractVHostItemExtension<PresenceSubscriptionVHostItemExtension> implements VHostItemExtensionBackwardCompatible<PresenceSubscriptionVHostItemExtension> {

		public static final String ID = "presence-subscription";

		private AUTO_AUTHORIZE_MODE autoAuthorizeMode = AUTO_AUTHORIZE_MODE.global;

		public AUTO_AUTHORIZE_MODE getAutoAuthorizeMode() {
			return autoAuthorizeMode;
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public void initFromElement(Element item) {
			String tmp = item.getAttributeStaticStr(AUTO_AUTHORIZE_PROP_KEY);
			if (tmp != null) {
				autoAuthorizeMode = AUTO_AUTHORIZE_MODE.valueOf(tmp);
			} else {
				autoAuthorizeMode = AUTO_AUTHORIZE_MODE.global;
			}
		}

		@Override
		public void initFromCommand(String prefix, Packet packet) throws IllegalArgumentException {
			String tmp = Command.getFieldValue(packet, prefix + "-" + AUTO_AUTHORIZE_PROP_KEY);
			autoAuthorizeMode = AUTO_AUTHORIZE_MODE.global;
			if (tmp != null) {
				if (Boolean.parseBoolean(tmp)) {
					autoAuthorizeMode = AUTO_AUTHORIZE_MODE.on;
				} else {
					autoAuthorizeMode = AUTO_AUTHORIZE_MODE.off;
				}
			}
		}

		@Override
		public String toDebugString() {
			return "mode: " + autoAuthorizeMode;
		}

		@Override
		public Element toElement() {
			if (autoAuthorizeMode == AUTO_AUTHORIZE_MODE.global) {
				return null;
			}
			
			Element el = new Element(getId());
			el.setAttribute(AUTO_AUTHORIZE_PROP_KEY, autoAuthorizeMode.name());
			return el;
		}

		@Override
		public void addCommandFields(String prefix, Packet packet, boolean forDefault) {
			Element commandEl = packet.getElemChild(Command.COMMAND_EL, Command.XMLNS);
			Boolean value = null;
			switch (autoAuthorizeMode) {
				case global:
					break;
				case off:
					value = false;
					break;
				case on:
					value = true;
					break;
			}
			addBooleanFieldWithDefaultToCommand(commandEl, prefix + "-" + AUTO_AUTHORIZE_PROP_KEY,
												"Automatically authorize subscription requests", value, forDefault);
		}

		@Override
		public PresenceSubscriptionVHostItemExtension mergeWithDefaults(
				PresenceSubscriptionVHostItemExtension defaults) {
			return autoAuthorizeMode == AUTO_AUTHORIZE_MODE.global ? defaults : this;
		}

		@Override
		public void initFromData(Map<String, Object> data) {
			String tmp = (String) data.remove(AUTO_AUTHORIZE_PROP_KEY);
			if (tmp == null) {
				autoAuthorizeMode = AUTO_AUTHORIZE_MODE.global;
			} else {
				autoAuthorizeMode = AUTO_AUTHORIZE_MODE.valueOf(tmp);
			}
		}
	}
}
