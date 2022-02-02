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
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusEvent;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.sys.TigaseRuntime;
import tigase.util.cache.LRUConcurrentCache;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFlat;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.roster.RosterAbstract.ROSTER;

@Id(PresenceOffline.ID)
@Handles({@Handle(path = {PresenceAbstract.PRESENCE_ELEMENT_NAME}, xmlns = PresenceAbstract.CLIENT_XMLNS),
		  @Handle(path = {Iq.ELEM_NAME, Iq.QUERY_NAME}, xmlns = RosterAbstract.XMLNS)})
@Bean(name = PresenceOffline.ID, parent = SessionManager.class, active = false)
public class PresenceOffline
		extends PresenceAbstract
		implements XMPPStopListenerIfc, Initializable, UnregisterAware {

	public static final String CACHE_SIZE_PROP_KEY = "cache-size";
	protected static final String ID = "presence-offline";
	private static final Logger log = Logger.getLogger(PresenceOffline.class.getCanonicalName());
	private static final EnumSet<StanzaType> PRESENCE_SUB_CHANGE_TYPES = EnumSet.of(StanzaType.subscribed,
																					StanzaType.unsubscribe,
																					StanzaType.unsubscribed);
	private final static String LAST_OFFLINE_PRESENCE_KEY = "last-offline-presence";
	private final static String DELAY_STAMP_KEY = "delay-stamp";
	private final EventBus eventBus = EventBusFactory.getInstance();
	private final SimpleDateFormat formatter;
	private final SimpleParser parser = SingletonFactory.getParserInstance();
	private final String presenceSessionEventName = "start-stop";
	@ConfigField(desc = "Add delay stamp to offline presences", alias = DELAY_STAMP_KEY)
	boolean delayStamp = true;
	@ConfigField(desc = "Default cache size", alias = CACHE_SIZE_PROP_KEY)
	private int cacheSize = 1000;
	private LRUConcurrentCache<BareJID, Element> presenceCache = new LRUConcurrentCache<>(10000);
	private LRUConcurrentCache<BareJID, Map<BareJID, RosterElement>> rosterCache = new LRUConcurrentCache<>(10000);
	@Inject
	private UserRepository userRepository = null;

	{
		this.formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		this.formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	public void setCacheSize(int size) {
		this.cacheSize = size;

		presenceCache = new LRUConcurrentCache<>(cacheSize);
		rosterCache = new LRUConcurrentCache<>(cacheSize);
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	public void onEvent(StartStopEvent event) {

		BareJID[] jidElements = event.getJids();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Procesing userPresence event: {0} with following jids: {1}",
					new Object[]{event, jidElements});
		}

		if (jidElements != null) {
			for (BareJID jid : jidElements) {
				String actionStr = event.getAction();
				if (actionStr != null) {

					switch (actionStr) {
						case "presence":
							presenceCache.remove(jid);
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Clearing presence cache: {0}, remaining items: {1}",
										new Object[]{jid, presenceCache.size()});
							}
							break;

						case "roster":
							rosterCache.remove(jid);
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Clearing roster cache: {0}, remaining items: {1}",
										new Object[]{jid, rosterCache.size()});
							}
							break;
					}
				}
			}
		}
	}

	@SuppressWarnings({"unchecked", "fallthrough"})
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {

		// if presence probe & user is offline -> send last offline presence
		// if presence unavailable - from that user - store presence &
		// invalidate cache
		// if presence sub change or roster change -- invalidate roster cache
		if ("presence".equals(packet.getElemName())) {

			if (session == null && packet.getType() == StanzaType.probe && packet.getStanzaFrom() != null &&
					packet.getStanzaTo() != null && !packet.getStanzaFrom().equals(packet.getStanzaTo())) {

				BareJID stanzaTo = packet.getStanzaTo() != null ? packet.getStanzaTo().getBareJID() : null;
				BareJID stanzaFrom = packet.getStanzaFrom() != null ? packet.getStanzaFrom().getBareJID() : null;

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Processing presence probe {0} to offline user: {1}",
							new Object[]{packet, packet.getStanzaTo()});
				}

				if (stanzaTo != null && stanzaFrom != null) {

					if (isSubscriptionValid(stanzaTo, stanzaFrom)) {

						Element presence = presenceCache.get(stanzaTo);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Retrieved presence from cache: {0}", presence);
						}
						if (presence == null) {
							presence = loadPresenceFromRepo(stanzaTo);
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Retrieved presence from respository: {0}", presence);
							}
						}
						if (presence != null) {
							try {
								Packet p = Packet.packetInstance(presence.clone());
								p.initVars(p.getStanzaFrom(), packet.getStanzaFrom());

								results.offer(p);
							} catch (TigaseStringprepException ex) {
								log.log(Level.WARNING, "Error creating packet instance from presence: " + presence, ex);
							}
						}
					}
				}

			} else if (session != null && packet.getStanzaFrom() != null) {

				if (PRESENCE_SUB_CHANGE_TYPES.contains(packet.getType())) {

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Presence sub change - sending event to cleare cache {0}",
								packet.getElement());
					}

					sendEvent("roster", packet.getStanzaFrom() != null ? packet.getStanzaFrom().getBareJID() : null,
							  packet.getStanzaTo() != null ? packet.getStanzaTo().getBareJID() : null);

				} else if (session.isUserId(packet.getStanzaFrom().getBareJID()) &&
						((packet.getType() == null) || (packet.getType() == StanzaType.available)) // &&
					// !isNotOnlySession(
					// session
					// )
						) {

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"Presence session: {0} started - sending event and removing data from repository, packet: ",
								new Object[]{session, packet});
					}

					sendEvent("presence", session.getJID().getBareJID());

					try {
						userRepository.removeData(packet.getStanzaFrom().getBareJID(), LAST_OFFLINE_PRESENCE_KEY);
					} catch (TigaseDBException ex) {
						log.log(Level.WARNING,
								"Error removing data from repository while starting new presence session", ex);
					}
				}

			}
		} else if (packet.getType() == StanzaType.set &&
				packet.getElement().getXMLNSStaticStr(Iq.IQ_QUERY_PATH) == RosterAbstract.XMLNS) {

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Roster change - updated roster cache, packet: {0}", packet);
			}

			if (packet.getStanzaFrom() != null) {
				final BareJID user = packet.getStanzaFrom().getBareJID();
				sendEvent("roster", user);
			}
		}
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {

		// if ( isNotOnlySession( session ) ){
		// return;
		// }
		// Synchronization to avoid conflict with login/logout events
		// processed in the SessionManager asynchronously
		if (session == null || !session.isAuthorized()) {
			return;
		}

		synchronized (session) {

			sendEvent("presence", session.getjid().getBareJID());

			final Element lastPresence = session.getPresence() != null ? session.getPresence().clone() : null;
			if (lastPresence != null) {

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Session: {0} stopped, storing to repository last presence: {1}",
							new Object[]{session, lastPresence});
				}

				if (delayStamp) {
					String stamp = null;

					synchronized (formatter) {
						stamp = formatter.format(new Date());
					}

					if (stamp != null) {
						Element x = new Element("delay", new String[]{"stamp", "xmlns"},
												new String[]{stamp, "urn:xmpp:delay"});
						lastPresence.addChild(x);
					}

				}

				if (!StanzaType.unavailable.toString().equals(lastPresence.getAttributeStaticStr(Packet.TYPE_ATT))) {
					lastPresence.setAttribute(Packet.TYPE_ATT, StanzaType.unavailable.toString());
				}

				try {
					userRepository.setData(session.getjid().getBareJID(), LAST_OFFLINE_PRESENCE_KEY,
										   lastPresence.toString());
				} catch (TigaseDBException ex) {
					log.log(Level.WARNING, "Error storing last offline presence to repository: " + lastPresence, ex);
				}
			}
		}

	}

	protected boolean isNotOnlySession(XMPPResourceConnection session) {
		if (TigaseRuntime.getTigaseRuntime().hasCompleteJidsInfo() && session != null) {
			final JID userJID = session.getjid();
			if (TigaseRuntime.getTigaseRuntime().isJidOnline(userJID)) {
				// ok, this use is still connected - don't update the repository
				// just yet
				JID[] connectionIdsForJid = TigaseRuntime.getTigaseRuntime().getConnectionIdsForJid(userJID);
				if (connectionIdsForJid != null && connectionIdsForJid.length > 0) {
					if (connectionIdsForJid.length != 1 || !connectionIdsForJid[0].equals(userJID)) {

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "There are other user {0} sessions still active: {1}",
									new Object[]{session.getjid(), Arrays.asList(connectionIdsForJid)});
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean isSubscriptionValid(BareJID owner, BareJID contact) {
		RosterAbstract.SubscriptionType buddy_subscr = null;
		Map<BareJID, RosterElement> roster = null;

		boolean result = false;

		roster = rosterCache.get(owner);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Checking user {0} subscription of {1}, present in cache: {2}",
					new Object[]{owner, contact, roster != null});
		}

		if (roster == null) {
			String rosterString = null;
			try {
				rosterString = userRepository.getData(owner, ROSTER);
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Problem reading roster from DB: ", ex);
			}

			if (rosterString != null) {
				roster = new ConcurrentHashMap<BareJID, RosterElement>(100, 0.25f, 1);
				RosterFlat.parseRosterUtil(rosterString, roster, null);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Loaded roster from DB: {0}", roster);
				}
			}
		}

		if (roster != null) {
			RosterElement rosterElement = roster.get(contact);
			buddy_subscr = rosterElement.getSubscription();
			if (buddy_subscr == null) {
				buddy_subscr = RosterAbstract.SubscriptionType.none;
			}
			result = roster_util.isSubscribedFrom(buddy_subscr);
		}

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "isSubscriptionValid, owner: {0}, contact: {1}, result: {2}",
					new Object[]{owner, contact, result});
		}
		return result;
	}

	protected Element loadPresenceFromRepo(BareJID stanzaTo) {
		Element presence = null;
		try {
			String presString = userRepository.getData(stanzaTo, LAST_OFFLINE_PRESENCE_KEY);
			DomBuilderHandler domHandler = new DomBuilderHandler();

			Queue<Element> parsedElements = null;
			if (presString != null) {

				char[] data = presString.toCharArray();
				parser.parse(domHandler, data, 0, data.length);
				parsedElements = domHandler.getParsedElements();
			}

			if (parsedElements != null && parsedElements.size() > 0) {
				presence = parsedElements.poll();
				presenceCache.put(stanzaTo, presence);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Loaded presence: {0} and stored it in cache", presence);
				}
			}
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "Loading presence from repository failed!", ex);
		}
		return presence;
	}

	private void sendEvent(String action, BareJID... user) {
		if (user != null && user.length > 0) {

			StartStopEvent event = new StartStopEvent();
			event.setAction(action);
			event.setJids(user);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Sending event: " + event);
			}

			eventBus.fire(event);
		}

	}

	public class StartStopEvent
			implements Serializable, EventBusEvent {

		public StartStopEvent() {}

		private String action;
		private BareJID[] jids;

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public BareJID[] getJids() {
			return jids;
		}

		public void setJids(BareJID[] jids) {
			this.jids = jids;
		}
	}

}
