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

import tigase.component.exceptions.RepositoryException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.IMixComponent;
import tigase.mix.Mix;
import tigase.pubsub.*;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.modules.ManageAffiliationsModule;
import tigase.pubsub.repository.IAffiliations;
import tigase.pubsub.repository.ISubscriptions;
import tigase.pubsub.repository.stateless.UsersAffiliation;
import tigase.pubsub.repository.stateless.UsersSubscription;
import tigase.pubsub.utils.DefaultPubSubLogic;
import tigase.server.Packet;
import tigase.util.Algorithms;
import tigase.util.datetime.TimestampHelper;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "logic", parent = IMixComponent.class, active = true)
public class DefaultMixLogic extends DefaultPubSubLogic
		implements MixLogic {

	private static final Logger log = Logger.getLogger(DefaultMixLogic.class.getCanonicalName());

	private static final Set<String> MIX_NODES = Mix.Nodes.ALL_NODES;

	private static final tigase.util.datetime.TimestampHelper timestampHelper = new TimestampHelper();

	@Inject
	private IMixRepository mixRepository;

	@Inject(nullAllowed = true)
	private RoomPresenceRepository roomPresenceRepository;

	@Override
	public boolean isServiceAutoCreated() {
		return true;
	}

	public String generateParticipantId(BareJID channelJID, BareJID participantRealJID) throws RepositoryException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(channelJID.getDomain().getBytes(StandardCharsets.UTF_8));
			md.update(participantRealJID.toString().getBytes(StandardCharsets.UTF_8));
			md.update(channelJID.getLocalpart().getBytes(StandardCharsets.UTF_8));
			return Algorithms.bytesToHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	@Override
	public String generateTempParticipantId(BareJID channelJID, JID participantRealJID) throws RepositoryException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(channelJID.getDomain().getBytes(StandardCharsets.UTF_8));
			md.update(participantRealJID.toString().getBytes(StandardCharsets.UTF_8));
			md.update(channelJID.getLocalpart().getBytes(StandardCharsets.UTF_8));
			return "temp-" + Algorithms.bytesToHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	@Override
	public void checkNodeConfig(AbstractNodeConfig nodeConfig) throws PubSubException {
		if (nodeConfig.getNodeAccessModel() != AccessModel.whitelist) {
			throw new PubSubException(Authorization.NOT_ALLOWED, "Only 'whitelist' access mode is allowed!");
		}
		if (MIX_NODES.contains(nodeConfig.getNodeName())) {
			if (nodeConfig.getSendLastPublishedItem() != SendLastPublishedItem.never) {
				throw new PubSubException(Authorization.NOT_ALLOWED, "Only 'whitelist' access mode is allowed!");
			}
			if (nodeConfig.getNodeType() != NodeType.leaf) {
				throw new PubSubException(Authorization.NOT_ALLOWED, "MIX nodes can only be leafs!");
			}
			if (nodeConfig.isDeliverPresenceBased()) {
				throw new PubSubException(Authorization.NOT_ALLOWED, "Presence based delivery is not allowed for MIX nodes!");
			}
			if (nodeConfig.isPresenceExpired()) {
				throw new PubSubException(Authorization.NOT_ALLOWED, "Presence expiring is not allowed for MIX nodes!");
			}
			if (nodeConfig.getCollection() != null && !"".equals(nodeConfig.getCollection())) {
				throw new PubSubException(Authorization.NOT_ALLOWED, "MIX nodes cannot be part of the collection!");
			}
			if (nodeConfig.getPublisherModel() != PublisherModel.publishers) {
				throw new PubSubException(Authorization.NOT_ALLOWED, "Only publishers can post to MIX nodes!");
			}
		}
		super.checkNodeConfig(nodeConfig);
	}

	@Override
	public void checkPermission(BareJID channel, BareJID senderJid, MixAction action)
			throws PubSubException, RepositoryException {
		switch (action) {
			case manage:
				ChannelConfiguration configuration = mixRepository.getChannelConfiguration(channel);
				if (configuration != null) {
					if (!configuration.isOwner(senderJid)) {
						throw new PubSubException(Authorization.NOT_ALLOWED);
					}
				} else {
					// do we have any other requirements?? ie. for channel creation?
				}
				break;
			case publish:
				if (mixRepository.getParticipant(channel, senderJid) == null) {
					throw new PubSubException(Authorization.NOT_ALLOWED);
				}
				break;
			case join:
				ChannelConfiguration configuration2 = mixRepository.getChannelConfiguration(channel);
				if (configuration2 != null) {
					if (configuration2.isOwner(senderJid)) {
						return;
					}
				}
				Optional<List<BareJID>> allowed = mixRepository.getAllowed(channel);
				if (allowed.isPresent()) {
					if (!allowed.get().contains(senderJid)) {
						if (!allowed.get().contains(BareJID.bareJIDInstanceNS(senderJid.getDomain()))) {
							throw new PubSubException(Authorization.NOT_ALLOWED);
						}
					}
				}

				Optional<List<BareJID>> banned = mixRepository.getBanned(channel);
				if (banned.isPresent()) {
					if (banned.get().contains(senderJid)) {
						throw new PubSubException(Authorization.NOT_ALLOWED);
					}
					if (banned.get().contains(BareJID.bareJIDInstanceNS(senderJid.getDomain()))) {
						throw new PubSubException(Authorization.NOT_ALLOWED);
					}
				}
				break;
			case relay:
				ChannelConfiguration configuration1 = mixRepository.getChannelConfiguration(channel);
				if (configuration1 != null) {
					if (!configuration1.arePrivateMessagesAllowed()) {
						throw new PubSubException(Authorization.NOT_ALLOWED);
					}
					if (mixRepository.getParticipant(channel, senderJid) == null) {
						throw new PubSubException(Authorization.NOT_ALLOWED);
					}
				}
				break;
		}
	}

	@Override
	public void checkPermission(BareJID serviceJid, String nodeName, JID senderJid, Action action)
			throws PubSubException, RepositoryException {
		ChannelConfiguration configuration = mixRepository.getChannelConfiguration(serviceJid);
		if (configuration == null) {
			throw new PubSubException(Authorization.ITEM_NOT_FOUND );
		}
		if (action == Action.manageNode && (nodeName == null || nodeName.isEmpty())) {
			this.checkPermission(serviceJid, senderJid.getBareJID(), MixAction.manage);
		}
		if (action == Action.retrieveItems && Mix.Nodes.MESSAGES.equals(nodeName) && roomPresenceRepository != null) {
			if (roomPresenceRepository.isRoomParticipant(serviceJid, senderJid)) {
				return;
			}
		}
		super.checkPermission(serviceJid, nodeName, senderJid, action);
	}

	@Override
	public boolean isChannelCreationAllowed(BareJID channelJID, BareJID senderJID) {
		return true;
	}

	@Override
	public boolean isMAMEnabled(BareJID serviceJid, String node) throws RepositoryException {
		if (Mix.Nodes.MESSAGES.equals(node) || node == null) {
			return true;
		}
		return super.isMAMEnabled(serviceJid, node);
	}

	@Override
	public String validateItemId(BareJID toJid, String node, String id) {
		if (Mix.Nodes.INFO.equals(node) || Mix.Nodes.CONFIG.equals(node)) {
			return timestampHelper.format(new Date());
		} else {
			return super.validateItemId(toJid, node, id);
		}
	}

	@Override
	public void generateAffiliationChangesNotifications(BareJID channelJid,
														ChannelConfiguration oldConfig,
														ChannelConfiguration newConfig, Consumer<Packet> packetConsumer) {
		Set<BareJID> changed = new HashSet<>();
		xor(oldConfig.getOwners(), newConfig.getOwners(), changed::add);
		xor(oldConfig.getAdministrators(), newConfig.getAdministrators(), changed::add);
		if (!changed.isEmpty()) {
			try {
				String[] childNodes = getRepository().getRootCollection(channelJid);
				if (childNodes != null) {
					for (String node : childNodes) {
						generateAffiliationNotifications(channelJid, node, changed, packetConsumer);
					}
				}
			} catch (RepositoryException ex) {
				log.log(Level.FINEST, "Could not list nodes for channel " + channelJid, ex);
			}
		}
		generateAffiliationChangesNotificationsForNodeUpdateRights(channelJid, newConfig, oldConfig.getInformationNodeUpdateRights(),
																	 newConfig.getInformationNodeUpdateRights(),
																	 Mix.Nodes.INFO, packetConsumer);
		generateAffiliationChangesNotificationsForNodeUpdateRights(channelJid, newConfig, oldConfig.getAvatarNodesUpdateRights(),
																   newConfig.getAvatarNodesUpdateRights(),
																   Mix.Nodes.AVATAR_METADATA, packetConsumer);
		generateAffiliationChangesNotificationsForNodeUpdateRights(channelJid, newConfig, oldConfig.getAvatarNodesUpdateRights(),
																   newConfig.getAvatarNodesUpdateRights(),
																   Mix.Nodes.AVATAR_DATA, packetConsumer);
	}

	protected void generateAffiliationChangesNotificationsForNodeUpdateRights(BareJID channelJID, ChannelConfiguration configuration, ChannelNodePermission oldPermission, ChannelNodePermission newPermission, String node, Consumer<Packet> packetConsumer) {
		try {
			switch (oldPermission) {
				case owners:
					switch (newPermission) {
						case owners:
							break;
						case admins:
							generateAffiliationNotifications(channelJID, node, configuration.getAdministrators(),
															 packetConsumer);
							break;
						case participants:
							generateAffiliationNotifications(channelJID, node,
															 mixRepository.getNodeSubscriptions(channelJID, node)
																	 .getSubscriptions()
																	 .map(UsersSubscription::getJid)
																	 .filter(jid -> !configuration.isOwner(jid))
																	 .collect(Collectors.toSet()), packetConsumer);
							break;
						default:
							break;
					}
				case admins:
					switch (newPermission) {
						case owners:
							generateAffiliationNotifications(channelJID, node, configuration.getAdministrators(),
															 packetConsumer);
							break;
						case admins:
							break;
						case participants:
							generateAffiliationNotifications(channelJID, node,
															 mixRepository.getNodeSubscriptions(channelJID, node)
																	 .getSubscriptions()
																	 .map(UsersSubscription::getJid)
																	 .filter(jid -> !configuration.isAdministrator(jid))
																	 .filter(jid -> !configuration.isOwner(jid))
																	 .collect(Collectors.toSet()), packetConsumer);
							break;
						default:
							break;
					}
				case participants:
					switch (newPermission) {
						case owners:
							generateAffiliationNotifications(channelJID, node,
															 mixRepository.getNodeSubscriptions(channelJID, node)
																	 .getSubscriptions()
																	 .map(UsersSubscription::getJid)
																	 .filter(jid -> !configuration.isOwner(jid))
																	 .collect(Collectors.toSet()), packetConsumer);
						case admins:
							generateAffiliationNotifications(channelJID, node,
															 mixRepository.getNodeSubscriptions(channelJID, node)
																	 .getSubscriptions()
																	 .map(UsersSubscription::getJid)
																	 .filter(jid -> !configuration.isAdministrator(jid))
																	 .filter(jid -> !configuration.isOwner(jid))
																	 .collect(Collectors.toSet()), packetConsumer);
							break;
						case participants:
							break;
					}
				default:
					break;
			}
		} catch (RepositoryException ex) {
			log.log(Level.FINEST, "Could not load subscriptions for channel " + channelJID + " and node " + node, ex);
		}
	}

	protected void generateAffiliationNotifications(BareJID channelJID, String node, Set<BareJID> changed, Consumer<Packet> packetConsumer) {
		try {
			JID channel = JID.jidInstance(channelJID);
			IAffiliations affiliations = getRepository().getNodeAffiliations(channelJID, node);
			ISubscriptions subscriptions = getRepository().getNodeSubscriptions(channelJID, node);
			if (affiliations != null && subscriptions != null) {
				for (BareJID jid : changed) {
					if (subscriptions.getSubscription(jid) != Subscription.subscribed) {
						continue;
					}

					UsersAffiliation affiliation = affiliations.getSubscriberAffiliation(jid);
					Packet notification = ManageAffiliationsModule.createAffiliationNotification(channel,
																								 JID.jidInstance(jid),
																								 node,
																								 affiliation.getAffiliation());
					if (notification != null) {
						packetConsumer.accept(notification);
					}
				}
			}
		} catch (RepositoryException ex) {
			log.log(Level.FINEST, "Could not load affiliations for channel " + channelJID + " and node " + node, ex);
		}
	}

	private static <T> void xor(Collection<T> oldCollection, Collection<T> newCollection, Consumer<T> consumer) {
		oldCollection.stream().filter(it -> !newCollection.contains(it)).forEach(consumer);
		newCollection.stream().filter(it -> !oldCollection.contains(it)).forEach(consumer);
	}
	//	@Override
//	public void checkPermission(BareJID serviceJid, String nodeName, JID senderJid, Action action)
//			throws PubSubException, RepositoryException {
//		switch (action) {
//			case subscribe:
//				if (mixRepository.getParticipant(serviceJid, senderJid.getBareJID()) == null) {
//					throw new PubSubException(Authorization.NOT_ALLOWED);
//				}
//				break;
//			case retrieveItems:
//			case manageNode:
//				checkPermission(serviceJid, senderJid.getBareJID(), MixAction.manage);
//				break;
//			default:
//				break;
//		}
//		super.checkPermission(serviceJid, nodeName, senderJid, action);
//	}
}
