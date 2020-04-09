/*
 * Tigase MIX - MIX component for Tigase
 * Copyright (C) 2020 Tigase, Inc. (office@tigase.com)
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
package tigase.mix.model;

import tigase.component.ScheduledTask;
import tigase.component.exceptions.RepositoryException;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.Mix;
import tigase.mix.modules.RoomPresenceModule;
import tigase.pubsub.CollectionItemsOrdering;
import tigase.pubsub.repository.IItems;
import tigase.pubsub.repository.IPubSubRepository;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.ReceiverTimeoutHandler;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "roomGhostbuster", parent = RoomPresenceModule.class, active = true)
public class RoomGhostbuster
		extends ScheduledTask {

	private static final Logger log = Logger.getLogger(RoomGhostbuster.class.getCanonicalName());

	private static final Set<String> REASONS = Collections.unmodifiableSet(new HashSet<>(
			Arrays.asList("gone", "item-not-found", "policy-violation", "recipient-unavailable", "redirect",
						  "remote-server-not-found", "remote-server-timeout", "service-unavailable")));

	private final ConcurrentHashMap<JID, MonitoredObject> monitoredObjects = new ConcurrentHashMap<>();

	private final ReceiverTimeoutHandler pingTimeoutHandler;

	@Inject(bean = "service")
	private AbstractMessageReceiver component;
	@Inject(nullAllowed = true)
	private GhostbusterFilter filter;
	@Inject
	private EventBus eventBus;
	@Inject
	private IPubSubRepository pubSubRepository;
	@Inject
	private VHostManagerIfc vHostManager;
	@Inject
	private RoomPresenceRepository roomPresenceRepository;
	
	private boolean firstRun = true;

	public RoomGhostbuster() {
		super(Duration.ofMinutes(10), Duration.ofMinutes(5));
		pingTimeoutHandler = new ReceiverTimeoutHandler() {
			@Override
			public void responseReceived(Packet data, Packet response) {
				try {
					onPingReceived(response);
				} catch (Exception e) {
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Problem on handling ping response", e);
					}
				}
			}

			@Override
			public void timeOutExpired(Packet data) {
				try {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Received ping timeout for ping " + data.getElement().getAttributeStaticStr("id"));
					}
					onPingTimeout(data.getStanzaTo());
				} catch (Exception e) {
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Problem on handling ping timeout", e);
					}
				}
			}
		};
	}

	public void register(BareJID channelJID, JID occupantJID) {
		MonitoredObject object = this.registerInternal(channelJID, occupantJID);
		if (object != null) {
			object.updateLastActivity();
		}
	}

	private MonitoredObject registerInternal(BareJID channelJID, JID occupantJID) {
		return monitoredObjects.compute(occupantJID, (k, prev) -> {
			MonitoredObject o = prev == null ? new MonitoredObject(occupantJID) : prev;
			o.addChannel(channelJID);
			return o;
		});
	}

	public void unregister(BareJID channelJID, JID occupantJID) {
		 monitoredObjects.computeIfPresent(occupantJID, (k, o) -> {
		 	o.updateLastActivity();
		 	o.removeChannel(channelJID);
		 	return o.isEmpty() ? null : o;
		});
	}
	
	@Override
	public void run() {
		try {
			if (firstRun) {
				firstRun = false;
				ForkJoinPool.commonPool().submit(() -> {
					try {
						for (JID vhost : vHostManager.getAllVHosts()) {
							List<BareJID> channels = pubSubRepository.getServices(
									BareJID.bareJIDInstanceNS(null, component.getName() + "." + vhost.getDomain()), null);
							if (channels != null) {
								for (BareJID channel : channels) {
									IItems items = pubSubRepository.getNodeItems(channel, Mix.Nodes.PARTICIPANTS);
									if (items != null) {
										String[] ids = items.getItemsIds(CollectionItemsOrdering.byUpdateDate);
										if (ids != null) {
											Set<String> participantIds = roomPresenceRepository.getRoomParticipantsIds(channel);
											for (String id : ids) {
												if (!id.startsWith("temp-")) {
													continue;
												}
												if (participantIds.contains(id)) {
													continue;
												}

												IItems.IItem item = items.getItem(id);
												if (item != null) {
													Element participantEl = item.getItem()
															.getChild("participant", Mix.CORE1_XMLNS);
													if (participantEl != null) {
														String jid = participantEl.getChildCData(el -> el.getName() == "jid");
														String resource = participantEl.getChildCData(
																el -> el.getName() == "resource" &&
																		el.getXMLNS() == "tigase:mix:muc:0");
														if (jid != null && resource != null) {
															eventBus.fire(new KickoutEvent(component.getName(), channel,
																						   JID.jidInstanceNS(
																								   BareJID.bareJIDInstanceNS(
																										   jid),
																								   resource)));
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} catch (RepositoryException ex) {
						log.log(Level.WARNING, "failed to load temporary occupants from database to ghostbuster!", ex);
					}
				});
			}

			final long border = System.currentTimeMillis() - 1000 * 60 * 60;
			monitoredObjects.values()
					.stream()
					.filter(obj -> !obj.wasActiveSince(border))
					.filter(obj -> filter == null || filter.shouldSendPing(obj.getOccupantJID()))
					.forEach(this::pingMonitoredObject);
		} catch (Throwable ex) {
			log.log(Level.FINEST, "exception during pinging room occupant", ex);
		}
	}

	protected void pingMonitoredObject(MonitoredObject monitoredObject) {
		try {
			BareJID sourceJID = monitoredObject.getPingSource();
			if (sourceJID == null) {
				return;
			}

			Packet packet = createPing(sourceJID, monitoredObject.getOccupantJID());
			component.addOutPacketWithTimeout(packet, pingTimeoutHandler, 1, TimeUnit.MINUTES);

			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Pinged " + monitoredObject.getOccupantJID());
			}
		} catch (Throwable ex) {
			log.log(Level.FINEST, "exception during pinging room occupant", ex);
		}
	}

	protected Packet createPing(BareJID sourceJID, JID destinationJID) throws TigaseStringprepException {
		final String id = "png-" + UUID.randomUUID().toString();

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Pinging " + destinationJID + ". id=" + id);
		}

		Element ping = new Element("iq", new String[]{"type", "id", "from", "to"},
								   new String[]{"get", id, sourceJID.toString(), destinationJID.toString()});

		ping.addChild(new Element("ping", new String[]{"xmlns"}, new String[]{"urn:xmpp:ping"}));

		Packet packet = Packet.packetInstance(ping);
		packet.setXMLNS(Packet.CLIENT_XMLNS);
		
		return packet;
	}

	protected void onPingReceived(Packet packet) throws TigaseStringprepException {
		if (packet.getStanzaFrom() == null) {
			return;
		}

		final MonitoredObject o = monitoredObjects.get(packet.getStanzaFrom());
		if (o == null) {
			return;
		}

		if (packet.getType() == StanzaType.error && Optional.ofNullable(packet.getElement())
				.map(el -> el.getChild("error"))
				.filter(elem -> Optional.ofNullable(elem.getChildren())
						.filter(el -> el.stream()
								.anyMatch(e -> (e.getXMLNS() == null ||
										e.getXMLNS() == "urn:ietf:params:xml:ns:xmpp-stanzas") && REASONS.contains(e.getName())))
						.isPresent()).isPresent()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Received presence error: " + packet.getElement().toString());
			}

			for (BareJID channel : o.getChannels()) {
				eventBus.fire(new KickoutEvent(component.getName(), channel, o.occupantJID));
			}
		} else {
			// update last activity
			if (log.isLoggable(Level.FINER)) {
				log.finer("Update activity of " + o.getOccupantJID());
			}

			o.updateLastActivity();
		}
	}

	protected void onPingTimeout(JID stanzaTo) throws TigaseStringprepException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Timeouted ping to: " + stanzaTo);
		}
	}

	protected class MonitoredObject {

		private final JID occupantJID;
		private long lastActivity = 0;
		private HashSet<BareJID> channels = new HashSet<BareJID>();

		public MonitoredObject(JID occupantJID) {
			this.occupantJID = occupantJID;
		}

		public JID getOccupantJID() {
			return occupantJID;
		}

		public synchronized BareJID getPingSource() {
			if (!channels.isEmpty()) {
				return channels.iterator().next();
			}
			return null;
		}

		public synchronized boolean wasActiveSince(long since) {
			return lastActivity > since;
		}

		protected synchronized void updateLastActivity() {
			this.lastActivity = System.currentTimeMillis();
		}

		protected synchronized void addChannel(BareJID channel) {
			this.channels.add(channel);
		}

		protected synchronized void removeChannel(BareJID channel) {
			this.channels.remove(channel);
		}

		protected synchronized BareJID[] getChannels() {
			return channels.toArray(new BareJID[channels.size()]);
		}

		protected synchronized boolean isEmpty() {
			return channels.isEmpty();
		}
	}

	@FunctionalInterface
	public interface GhostbusterFilter {
		boolean shouldSendPing(JID recipient);
	}

	public class KickoutEvent {

		private final String componentName;
		private final BareJID channelJID;
		private final JID occupantJID;

		public KickoutEvent(String componentName, BareJID channelJID, JID occupantJID) {
			this.componentName = componentName;
			this.channelJID = channelJID;
			this.occupantJID = occupantJID;
		}

		public String getComponentName() {
			return componentName;
		}

		public BareJID getChannelJID() {
			return channelJID;
		}

		public JID getOccupantJID() {
			return occupantJID;
		}
	}
}
