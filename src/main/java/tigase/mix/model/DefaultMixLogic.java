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
import tigase.pubsub.utils.DefaultPubSubLogic;
import tigase.util.Algorithms;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

@Bean(name = "logic", parent = IMixComponent.class, active = true)
public class DefaultMixLogic extends DefaultPubSubLogic
		implements MixLogic {

	private static final Set<String> MIX_NODES = Mix.Nodes.ALL_NODES;

	@Inject
	private IMixRepository mixRepository;

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
		}
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
