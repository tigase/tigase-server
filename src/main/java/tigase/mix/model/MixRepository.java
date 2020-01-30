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

import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.Affiliations;
import tigase.mix.Mix;
import tigase.mix.MixComponent;
import tigase.pubsub.AbstractNodeConfig;
import tigase.pubsub.CollectionItemsOrdering;
import tigase.pubsub.NodeType;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.modules.PublishItemModule;
import tigase.pubsub.modules.RetractItemModule;
import tigase.pubsub.modules.mam.Query;
import tigase.pubsub.repository.IItems;
import tigase.pubsub.repository.cached.CachedPubSubRepository;
import tigase.pubsub.repository.cached.IAffiliationsCached;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "repository", parent = MixComponent.class, active = true)
public class MixRepository extends CachedPubSubRepository implements IMixRepository {

	private static final Logger log = Logger.getLogger(MixRepository.class.getCanonicalName());

	@Inject
	private MixLogic mixLogic;

	@Inject(nullAllowed = true)
	private PublishItemModule publishItemModule;

	@Inject
	private RetractItemModule retractItemModule;

	private Map<BareJID, ChannelConfiguration> channelConfigs = Collections.synchronizedMap(
			new tigase.util.cache.SizedCache<BareJID, ChannelConfiguration>(1000));

	@Override
	protected boolean isServiceAutoCreated() {
		return false;
	}

	@Override
	public void createNode(BareJID serviceJid, String nodeName, BareJID ownerJid, AbstractNodeConfig nodeConfig,
						   NodeType nodeType, String collection) throws RepositoryException, PubSubException {
		mixLogic.checkNodeConfig(nodeConfig);
		super.createNode(serviceJid, nodeName, ownerJid, nodeConfig, nodeType, collection);
	}
	
	@Override
	public void update(BareJID serviceJid, String nodeName, AbstractNodeConfig nodeConfig) throws RepositoryException, PubSubException {
		mixLogic.checkNodeConfig(nodeConfig);
		super.update(serviceJid, nodeName, nodeConfig);
	}

	@Override
	public IParticipant getParticipant(BareJID channelJID, BareJID participantRealJID) throws RepositoryException {
		IItems items = getNodeItems(channelJID, Mix.Nodes.PARTICIPANTS);
		if (items == null) {
			return null;
		}

		String participantId = mixLogic.generateParticipantId(channelJID, participantRealJID);
		IItems.IItem item = items.getItem(participantId);
		if (item == null) {
			return null;
		}
		return new Participant(participantId, item.getItem().getChild("participant", Mix.CORE1_XMLNS));
	}

	@Override
	public void removeParticiapnt(BareJID channelJID, BareJID participantJID) throws RepositoryException {
		String id = mixLogic.generateParticipantId(channelJID, participantJID);
		retractItemModule.retractItems(channelJID, "urn:xmpp:mix:nodes:participants",
									   Collections.singletonList(id));
	}

	@Override
	public IParticipant updateParticipant(BareJID channelJID, BareJID participantJID, String nick)
			throws RepositoryException, PubSubException {
		IParticipant participant = new Participant(mixLogic.generateParticipantId(channelJID, participantJID), participantJID,
												   nick);
		Element itemEl = new Element("item");
		itemEl.setAttribute("id", participant.getParticipantId());
		itemEl.addChild(participant.toElement());

		publishItemModule.publishItems(channelJID, Mix.Nodes.PARTICIPANTS, JID.jidInstance(participantJID),
									   Collections.singletonList(itemEl), null);

		return participant;
	}

	@Override
	public ChannelConfiguration getChannelConfiguration(BareJID channelJID) throws RepositoryException {
		ChannelConfiguration configuration = channelConfigs.get(channelJID);
		if (configuration == null) {
			configuration = loadChannelConfiguration(channelJID);
			if (configuration != null) {
				ChannelConfiguration existing = channelConfigs.putIfAbsent(channelJID, configuration);
				if (existing != null) {
					configuration = existing;
				}
			}
		}
		return configuration;
	}

	protected ChannelConfiguration loadChannelConfiguration(BareJID channelJID) throws RepositoryException {
		IItems items = getNodeItems(channelJID, Mix.Nodes.CONFIG);
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
	public void queryItems(Query query, ItemHandler itemHandler) throws RepositoryException, ComponentException {
		super.queryItems(query, itemHandler);
	}

	@Override
	protected void userRemoved(BareJID userJid) {
		super.userRemoved(userJid);
		channelConfigs.remove(userJid);
	}

	@Override
	public void itemDeleted(BareJID serviceJID, String node, String id) {
		super.itemDeleted(serviceJID, node, id);
	}

	@Override
	public void itemWritten(BareJID serviceJID, String node, String id, String publisher, Element item, String uuid) {
		if (Mix.Nodes.CONFIG.equals(node)) {
			// node config has changed, we need to update it
			try {
				ChannelConfiguration configuration = new ChannelConfiguration(item);
				channelConfigs.put(serviceJID, configuration);
			} catch (PubSubException ex) {
				log.log(Level.WARNING, "Could not parse new configuration of channel " + serviceJID, ex);
			}
		}
		super.itemWritten(serviceJID, node, id, publisher, item, uuid);
	}

	@Override
	public void validateItem(BareJID serviceJID, String node, String id, String publisher, Element item)
			throws PubSubException {
		if (Mix.Nodes.CONFIG.equals(node)) {
			// this line is required as it validates it configuration is correct!
			ChannelConfiguration config = new ChannelConfiguration(item);
			ChannelConfiguration.updateLastChangeMadeBy(item, JID.jidInstanceNS(publisher));
		}
	}

	@Override
	protected IAffiliationsCached newNodeAffiliations(BareJID serviceJid, String nodeName, Object nodeId,
													  RepositorySupplier affiliationSupplier)
			throws RepositoryException {
		if (Mix.Nodes.ALL_NODES.contains(nodeName)) {
			return new Affiliations(serviceJid, nodeName, this);
		} else {
			return super.newNodeAffiliations(serviceJid, nodeName, nodeId, affiliationSupplier);
		}
	}
}
