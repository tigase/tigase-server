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

import tigase.component.PacketWriter;
import tigase.component.exceptions.RepositoryException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.Affiliations;
import tigase.mix.Mix;
import tigase.mix.MixComponent;
import tigase.pubsub.Affiliation;
import tigase.pubsub.CollectionItemsOrdering;
import tigase.pubsub.Subscription;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.modules.PublishItemModule;
import tigase.pubsub.modules.RetractItemModule;
import tigase.pubsub.repository.IAffiliations;
import tigase.pubsub.repository.IItems;
import tigase.pubsub.repository.IPubSubRepository;
import tigase.pubsub.repository.ISubscriptions;
import tigase.pubsub.repository.cached.CachedPubSubRepository;
import tigase.pubsub.repository.cached.IAffiliationsCached;
import tigase.pubsub.repository.stateless.UsersAffiliation;
import tigase.pubsub.repository.stateless.UsersSubscription;
import tigase.pubsub.utils.Cache;
import tigase.pubsub.utils.LRUCacheWithFuture;
import tigase.server.DataForm;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "mixRepository", parent = MixComponent.class, active = true)
public class MixRepository<T> implements IMixRepository, IPubSubRepository.IListener, CachedPubSubRepository.NodeAffiliationProvider<T> {

	private static final Logger log = Logger.getLogger(MixRepository.class.getCanonicalName());

	@Inject (nullAllowed = true)
	private MixLogic mixLogic;

	@Inject(nullAllowed = true)
	private PublishItemModule publishItemModule;

	@Inject
	private RetractItemModule retractItemModule;

	@Inject
	private IPubSubRepository pubSubRepository;

	@Inject
	private PacketWriter packetWriter;
	
	private final Cache<BareJID, ChannelConfiguration> channelConfigs = new LRUCacheWithFuture<>(1000);
	private final Cache<ParticipantKey, Participant> participants = new LRUCacheWithFuture<>(4000);

	@Override
	public Optional<List<BareJID>> getAllowed(BareJID channelJID) throws RepositoryException {
		IItems items = pubSubRepository.getNodeItems(channelJID, Mix.Nodes.ALLOWED);
		if (items == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(items.getItemsIds(CollectionItemsOrdering.byUpdateDate))
				.map(strings -> Arrays.stream(strings).map(BareJID::bareJIDInstanceNS).collect(Collectors.toList()));
	}

	@Override
	public Optional<List<BareJID>> getBanned(BareJID channelJID) throws RepositoryException {
		IItems items = pubSubRepository.getNodeItems(channelJID, Mix.Nodes.BANNED);
		if (items == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(items.getItemsIds(CollectionItemsOrdering.byUpdateDate))
				.map(strings -> Arrays.stream(strings).map(BareJID::bareJIDInstanceNS).collect(Collectors.toList()));
	}

	@Override
	public IParticipant getParticipant(BareJID channelJID, BareJID participantRealJID) throws RepositoryException {
		String participantId = mixLogic.generateParticipantId(channelJID, participantRealJID);
		return getParticipant(channelJID, participantId);
	}

	@Override
	public IParticipant getParticipant(BareJID channelJID, String participantId) throws RepositoryException {
		return getParticipant(new ParticipantKey(channelJID, participantId));
	}

	protected IParticipant getParticipant(ParticipantKey key) throws RepositoryException {
		try {
			return participants.computeIfAbsent(key, () -> {
				try {
					IItems items = pubSubRepository.getNodeItems(key.channelJID, Mix.Nodes.PARTICIPANTS);
					if (items == null) {
						return null;
					}
					IItems.IItem item = items.getItem(key.participantId);
					if (item == null) {
						return null;
					}
					return new Participant(key.participantId, item.getItem().getChild("participant", Mix.CORE1_XMLNS));
				} catch (RepositoryException ex) {
					throw new Cache.CacheException(ex);
				}
			});
		} catch (Cache.CacheException ex) {
			throw new RepositoryException(ex.getMessage(), ex);
		}
	}

	@Override
	public void removeParticiapnt(BareJID channelJID, BareJID participantJID) throws RepositoryException {
		String id = mixLogic.generateParticipantId(channelJID, participantJID);
		retractItemModule.retractItems(channelJID, "urn:xmpp:mix:nodes:participants",
									   Collections.singletonList(id));
		participants.remove(new ParticipantKey(channelJID, id));
	}

	@Override
	public IParticipant updateParticipant(BareJID channelJID, BareJID participantJID, String nick)
			throws RepositoryException, PubSubException {
		Participant participant = new Participant(mixLogic.generateParticipantId(channelJID, participantJID), participantJID,
												   nick);
		Element itemEl = new Element("item");
		itemEl.setAttribute("id", participant.getParticipantId());
		itemEl.addChild(participant.toElement());

		publishItemModule.publishItems(channelJID, Mix.Nodes.PARTICIPANTS, JID.jidInstance(participantJID),
									   Collections.singletonList(itemEl), null);

		participants.put(new ParticipantKey(channelJID, participant.getParticipantId()), participant);

		return participant;
	}

	@Override
	public IParticipant updateTempParticipant(BareJID channelJID, JID participantJID, String nick)
			throws RepositoryException, PubSubException {
		Participant participant = new Participant(mixLogic.generateTempParticipantId(channelJID, participantJID), participantJID.getBareJID(),
												  nick);
		Element itemEl = new Element("item");
		itemEl.setAttribute("id", participant.getParticipantId());
		Element participantEl = participant.toElement();
		participantEl.addChild(new Element("resource", participantJID.getResource()).withAttribute("xmlns", "tigase:mix:muc:0"));
		itemEl.addChild(participantEl);

		publishItemModule.publishItems(channelJID, Mix.Nodes.PARTICIPANTS, participantJID,
									   Collections.singletonList(itemEl), null);

		participants.put(new ParticipantKey(channelJID, participant.getParticipantId()), participant);

		return participant;
	}

	@Override
	public void removeTempParticipant(BareJID channelJID, JID participantJID) throws RepositoryException {
		String id = mixLogic.generateTempParticipantId(channelJID, participantJID);
		retractItemModule.retractItems(channelJID, "urn:xmpp:mix:nodes:participants",
									   Collections.singletonList(id));
		participants.remove(new ParticipantKey(channelJID, id));
	}

	public String getChannelName(BareJID channelJID) throws RepositoryException {
		IItems items = pubSubRepository.getNodeItems(channelJID, Mix.Nodes.INFO);
		if (items != null) {
			IItems.IItem item = items.getLastItem(CollectionItemsOrdering.byUpdateDate);
			if (item != null) {
				return DataForm.getFieldValue(item.getItem(), "Name");
			}
		}
		return null;
	}

	@Override
	public ChannelConfiguration getChannelConfiguration(BareJID channelJID) throws RepositoryException {
		try {
			ChannelConfiguration configuration = channelConfigs.computeIfAbsent(channelJID, () -> {
				try {
					return loadChannelConfiguration(channelJID);
				} catch (RepositoryException ex) {
					throw new Cache.CacheException(ex);
				}
			});
			return configuration;
		} catch (Cache.CacheException ex) {
			throw new RepositoryException(ex.getMessage(), ex);
		}
	}
	
	protected ChannelConfiguration loadChannelConfiguration(BareJID channelJID) throws RepositoryException {
		IItems items = pubSubRepository.getNodeItems(channelJID, Mix.Nodes.CONFIG);
		if (items != null) {
			String[] ids = items.getItemsIds(CollectionItemsOrdering.byUpdateDate);
			if (ids != null && ids.length > 0) {
				String lastID = ids[ids.length - 1];
				IItems.IItem item = items.getItem(lastID);
				if (item != null) {
					try {
						return new ChannelConfiguration(item.getItem());
					} catch (PubSubException ex) {
						throw new RepositoryException("Could not load channel " + channelJID + " configuration", ex);
					}
				}
			}
		}
		return null;
	}

	@Override
	public ISubscriptions getNodeSubscriptions(BareJID serviceJid, String nodeName) throws RepositoryException {
		return pubSubRepository.getNodeSubscriptions(serviceJid, nodeName);
	}

	@Override
	public void serviceRemoved(BareJID userJid) {
		channelConfigs.remove(userJid);
	}

	@Override
	public void itemDeleted(BareJID serviceJID, String node, String id) {
		switch (node) {
			case Mix.Nodes.ALLOWED:
				try {
					if (id != null) {
						bannedParticipantFromChannel(serviceJID, BareJID.bareJIDInstanceNS(id));
					}
				} catch (RepositoryException ex) {
					// if exception happended just ignore it..
				}
				break;
			default:
				// nothing to do..
				break;
		}
	}

	@Override
	public void itemWritten(BareJID serviceJID, String node, String id, String publisher, Element item, String uuid) {
		switch (node) {
			case Mix.Nodes.CONFIG:
				// node config has changed, we need to update it
				ChannelConfiguration oldConfig = null;
				ChannelConfiguration newConfig = null;

				try {
					oldConfig = getChannelConfiguration(serviceJID);
				} catch (RepositoryException ex) {
					// if exception happended just ignore it..
				}
				updateChannelConfiguration(serviceJID, item);

				try {
					newConfig = getChannelConfiguration(serviceJID);
					if (newConfig != null && oldConfig != null) {
						mixLogic.generateAffiliationChangesNotifications(serviceJID, oldConfig, newConfig, packetWriter::write);
					}
				} catch (RepositoryException ex) {
					// if exception happended just ignore it..
				}
				break;
			case Mix.Nodes.BANNED:
				try {
					if (id != null) {
						bannedParticipantFromChannel(serviceJID, BareJID.bareJIDInstanceNS(id));
					}
				} catch (RepositoryException ex) {
					// if exception happended just ignore it..
				}
				break;
			default:
				// nothing to do..
				break;
		}
	}

	@Override
	public boolean validateItem(BareJID serviceJID, String node, String id, String publisher, Element item)
			throws PubSubException {
		if (Mix.Nodes.CONFIG.equals(node)) {
			// this line is required as it validates it configuration is correct!
			ChannelConfiguration config = new ChannelConfiguration(item);
			ChannelConfiguration.updateLastChangeMadeBy(item, JID.jidInstanceNS(publisher));
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "validated channel " + serviceJID + " configuration as valid: " + config.toElement("UNSET"));
			}
			return config != null && config.isValid();
		}
		if (Mix.Nodes.INFO.equals(node)) {
			// we need to handle this properly..

		}
		return true;
	}

	@Override
	public Map<String, UsersAffiliation> getUserAffiliations(BareJID serviceJid, BareJID jid) throws RepositoryException {
		Map<String, UsersAffiliation> userAffiliations = new HashMap<>();
		String[] nodes = pubSubRepository.getRootCollection(serviceJid);
		if (nodes != null) {
			for (String node : nodes) {
				IAffiliations affiliations = pubSubRepository.getNodeAffiliations(serviceJid, node);
				if (affiliations != null) {
					UsersAffiliation affiliation = affiliations.getSubscriberAffiliation(jid);
					if (affiliation.getAffiliation() != Affiliation.none) {
						userAffiliations.put(node, affiliation);
					}
				}
			}
		}
		return userAffiliations;
	}

	@Override
	public IAffiliationsCached newNodeAffiliations(BareJID serviceJid, String nodeName, T nodeId,
												   IPubSubRepository.RepositorySupplier<Map<BareJID, UsersAffiliation>> affiliationSupplier)
			throws RepositoryException {
		if (Mix.Nodes.ALL_NODES.contains(nodeName)) {
			return new Affiliations(serviceJid, nodeName, this);
		} else {
			return null;
		}
	}

	protected void bannedParticipantFromChannel(BareJID channelJID, BareJID participantJID) throws RepositoryException {
		if (getParticipant(channelJID, participantJID) != null) {
			removeParticiapnt(channelJID, participantJID);
			Map<String, UsersSubscription> userSubscriptions = pubSubRepository.getUserSubscriptions(channelJID,
																									 participantJID);
			for (String node : userSubscriptions.keySet()) {
				ISubscriptions subscriptions = pubSubRepository.getNodeSubscriptions(channelJID, node);
				subscriptions.changeSubscription(participantJID, Subscription.none);
				pubSubRepository.update(channelJID, node, subscriptions);
			}
		}
	}

	protected void invalidateChannelParticipant(BareJID channelJID, BareJID participantId) throws RepositoryException {
		participants.remove(new ParticipantKey(channelJID, mixLogic.generateParticipantId(channelJID, participantId)));
	}

	protected void updateChannelConfiguration(BareJID serviceJID, Element item) {
		try {
			ChannelConfiguration configuration = new ChannelConfiguration(item);
			channelConfigs.put(serviceJID, configuration);
		} catch (PubSubException ex) {
			log.log(Level.WARNING, "Could not parse new configuration of channel " + serviceJID, ex);
		}
	}

	protected static class ParticipantKey {

		private final BareJID channelJID;
		private final String participantId;

		public ParticipantKey(BareJID channelJID, String participantId) {
			this.channelJID = channelJID;
			this.participantId = participantId;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ParticipantKey)) {
				return false;
			}
			ParticipantKey that = (ParticipantKey) o;
			return channelJID.equals(that.channelJID) && participantId.equals(that.participantId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(channelJID, participantId);
		}
	}
}
