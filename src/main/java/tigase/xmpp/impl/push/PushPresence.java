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
package tigase.xmpp.impl.push;

import tigase.cluster.strategy.ClusteringStrategyIfc;
import tigase.cluster.strategy.ConnectionRecordIfc;
import tigase.component.PacketWriter;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.map.ClusterMapFactory;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.xmppsession.SessionManager;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.util.cache.LRUConcurrentCache;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostItemImpl;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.impl.JabberIqPrivacy;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "push-presence", parent = SessionManager.class, active = false)
public class PushPresence
		extends AnnotatedXMPPProcessor
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(PushPresence.class.getCanonicalName());
	
	enum PresenceStatus {
		chat,
		available,
		away,
		xa,
		dnd,
		unavailable
	}

	@ConfigField(desc = "Presence show value for accounts with push devices")
	private PresenceStatus presenceStatus = PresenceStatus.xa;
	@ConfigField(desc = "Push presence resource name")
	private String resourceName = "push";
	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;
	@Inject
	private EventBus eventBus;
	@Inject
	private AbstractPushNotifications pushNotifications;
	@Inject(bean = "sess-man")
	private SessionManagerHandler sessionManager;
	@Inject
	private PacketWriter packetWriter;
	@Inject(nullAllowed = true)
	private ClusteringStrategyIfc<ConnectionRecordIfc> strategy;
	private Map<BareJID,Boolean> pushAvailability;
	private final JID offlineConnectionId = JID.jidInstanceNS("push-offline-conn", DNSResolverFactory.getInstance().getDefaultHost());
	private final LRUConcurrentCache<BareJID, Set<BareJID>> rosterSubscribedFromCache = new LRUConcurrentCache<>(10000);
	private final SessionManagerHandler offlineSessionManagerHandler = new SessionManagerHandler() {
		@Override
		public JID getComponentId() {
			return null;
		}

		@Override
		public void handleLogin(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handleDomainChange(String domain, XMPPResourceConnection conn) {

		}

		@Override
		public void handleLogout(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handlePresenceSet(XMPPResourceConnection conn) {

		}

		@Override
		public void handleResourceBind(XMPPResourceConnection conn) {

		}

		@Override
		public boolean isLocalDomain(String domain, boolean includeComponents) {
			return false;
		}
	};

	private RosterAbstract rosterUtil = RosterFactory.getRosterImplementation(true);

	public PushPresence() {
	}

	@Override
	public void initialize() {
		this.pushAvailability = ClusterMapFactory.get().createMap("push-availability", BareJID.class, Boolean.class);
		this.pushNotifications.setPushDevicesPresence(this);
		if (eventBus != null) {
			eventBus.registerAll(this);
		}
	}

	@Override
	public void beforeUnregister() {
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	protected void setRosterUtil(RosterAbstract rosterUtil) {
		this.rosterUtil = rosterUtil;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}
	
	protected boolean isPushAvailable(BareJID userJid) throws TigaseDBException {
		Boolean value = pushAvailability.get(userJid);
		if (value == null) {
			value = !pushNotifications.getPushServices(userJid).isEmpty();
			pushAvailability.put(userJid, value);
		}
		return value;
	}
	
	private boolean shouldNodeGeneratePresence(BareJID userJid) {
		if (strategy == null) {
			return true;
		}

		Set<ConnectionRecordIfc> records = strategy.getConnectionRecords(userJid);
		if (records != null) {
			List<String> nodes = new ArrayList<>();
			// if we are executing, either our node has user session or there is no user session on any nodes
			nodes.add(sessionManager.getComponentId().getDomain());

			// we know nodes that may have user session
			for (ConnectionRecordIfc record : records) {
				String node = record.getNode().getDomain();
				if (!nodes.contains(node)) {
					nodes.add(node);
				}
			}
			nodes.sort(String::compareTo);
			
			String selectedNode = nodes.get(userJid.hashCode() % nodes.size());
			return sessionManager.getComponentId().getDomain().equals(selectedNode);
		}
		// If records were null, we have no idea which nodes will be able to handle request as each node may
		// or may not have a user session. We are executing assuming that we should handle request supposing
		// that see-other-host will reconnect all connections to the same node
		return true;
	}

	public void processPresenceToOffline(JID recipient, JID sender, StanzaType stanzaType, Consumer<Packet> packetConsumer) {
		if (stanzaType != StanzaType.probe) {
			return;
		}
		processPresenceProbe(recipient, sender, packetConsumer);
	}

	public void processPresenceProbe(JID recipient, JID sender, Consumer<Packet> packetConsumer) {
		if (recipient == null || recipient.getResource() != null || sender == null) {
			return;
		}
		try {
			if (!isPushAvailable(recipient.getBareJID())) {
				return;
			}
			if (!shouldNodeGeneratePresence(recipient.getBareJID())) {
				return;
			}
			if (!getSubscribedWithFrom(recipient.getBareJID()).contains(sender.getBareJID())) {
				return;
			}
			sendPresenceFormPushDevices(recipient.getBareJID(), true, List.of(sender.getBareJID()), packetConsumer);
		} catch (TigaseDBException ex) {
			log.log(Level.FINEST, "failed to fetch push devices for jid " + recipient.getBareJID(), ex);
		}
	}

	private Packet createPresenceForPushDevices(boolean hasPushDevices) throws TigaseStringprepException {
		Element presenceEl = new Element("presence", new Element[]{new Element("priority", "-1"),}, null, null);
		presenceEl.setXMLNS(Presence.CLIENT_XMLNS);
		if (!hasPushDevices) {
			presenceEl.setAttribute("type", StanzaType.unavailable.toString());
		} else {
			switch (presenceStatus) {
				case unavailable -> presenceEl.setAttribute("type", StanzaType.unavailable.name());
				case available -> {}
				default -> presenceEl.withElement("show", null, presenceStatus.name());
			}
		}
		return Packet.packetInstance(presenceEl);
	}

	private void sendPresenceFormPushDevices(BareJID userJid, boolean hasPushDevices, Collection<BareJID> recipients, Consumer<Packet> packetConsumer){
		try {
			JID from = JID.jidInstance(userJid, resourceName);
			Packet presence = createPresenceForPushDevices(hasPushDevices);
			for (BareJID jid : recipients) {
				Packet clone = presence.copyElementOnly();
				clone.initVars(from, JID.jidInstance(jid));
				packetConsumer.accept(clone);
			}
		} catch (TigaseStringprepException ex) {
			log.log(Level.FINEST, "failed to prepare JID for push presence", ex);
		}
	}

	public void broadcastPresenceFromPushDevices(BareJID userJid, boolean hasPushDevices, Consumer<Packet> packetConsumer) {
		sendPresenceFormPushDevices(userJid, hasPushDevices, getSubscribedWithFrom(userJid), packetConsumer);
	}
	
	public void pushAvailabilityChanged(BareJID userJid, boolean newValue, Consumer<Packet> packetConsumer) {
		pushAvailability.put(userJid, newValue);
		broadcastPresenceFromPushDevices(userJid, newValue, packetConsumer);
	}

	private Set<BareJID> getSubscribedWithFrom(BareJID userJid) {
		Set<BareJID> jids = rosterSubscribedFromCache.get(userJid);
		if (jids == null) {
			jids = new CopyOnWriteArraySet<>();
			rosterSubscribedFromCache.put(userJid, jids);
			synchronized (jids) {
				XMPPResourceConnection session = createOfflineXMPPResourceConnection(userJid);
				try {
					JID[] buddies = rosterUtil.getBuddies(session, RosterAbstract.FROM_SUBSCRIBED);
					if (buddies != null) {
						for (JID buddy : buddies) {
							jids.add(buddy.getBareJID());
						}
					}
				} catch (NotAuthorizedException | TigaseDBException ex) {
					log.log(Level.FINEST, "failed to fetch buddies subscribed 'from' for jid " + userJid, ex);
				}
			}
		}
		return jids;
	}

	@HandleEvent
	public void handleRosterModified(RosterAbstract.RosterModifiedEvent event) {
		BareJID userJid = event.getUserJid().getBareJID();
		if (!shouldNodeGeneratePresence(userJid)) {
			return;
		}
		
		try {
			if (isPushAvailable(userJid)) {
				Set<BareJID> jids = rosterSubscribedFromCache.get(userJid);
				switch (event.getSubscription()) {
					case from:
					case from_pending_out:
					case both:
						if (jids != null) {
							jids.add(event.getJid().getBareJID());
						}
						sendPresenceFormPushDevices(userJid, true, List.of(event.getJid().getBareJID()),
													packetWriter::write);
						break;
					case to:
					case none:
						if (jids != null) {
							jids.remove(event.getJid().getBareJID());
						}
						sendPresenceFormPushDevices(userJid, false, List.of(event.getJid().getBareJID()),
													packetWriter::write);
				}
			}
		} catch (TigaseDBException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "failed to check for registered push devices for user " + userJid, ex);
			}
		}
	}
	
	private XMPPResourceConnection createOfflineXMPPResourceConnection(BareJID userJid) {
		try {
			XMPPResourceConnection session = new JabberIqPrivacy.OfflineResourceConnection(offlineConnectionId,
																						   userRepository,
																						   authRepository, offlineSessionManagerHandler);
			VHostItem vhost = new VHostItemImpl(userJid.getDomain());
			session.setDomain(vhost);
			session.authorizeJID(userJid, false);
			XMPPSession parentSession = new XMPPSession(userJid.getLocalpart());
			session.setParentSession(parentSession);
			return session;
		} catch (TigaseStringprepException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "creation of temporary session for offline user " + userJid + " failed", ex);
			}
			return null;
		}
	}
	
}
