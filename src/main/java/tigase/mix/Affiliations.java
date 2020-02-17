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
package tigase.mix;

import tigase.component.exceptions.RepositoryException;
import tigase.mix.model.ChannelConfiguration;
import tigase.mix.model.ChannelNodePermission;
import tigase.mix.model.IMixRepository;
import tigase.pubsub.Affiliation;
import tigase.pubsub.Subscription;
import tigase.pubsub.repository.ISubscriptions;
import tigase.pubsub.repository.cached.IAffiliationsCached;
import tigase.pubsub.repository.stateless.UsersAffiliation;
import tigase.xmpp.jid.BareJID;

import java.util.Collections;
import java.util.Map;

public class Affiliations implements IAffiliationsCached {

	private final BareJID channelJID;
	private final String nodeName;
	private final IMixRepository mixRepository;

	public Affiliations(BareJID serviceJID, String nodeName, IMixRepository mixRepository) {
		this.channelJID = serviceJID;
		this.nodeName = nodeName;
		this.mixRepository = mixRepository;
	}

	@Override
	public boolean isChanged() {
		return false;
	}

	@Override
	public Map<BareJID, UsersAffiliation> getChanged() {
		return Collections.emptyMap();
	}

	@Override
	public void resetChangedFlag() {
		// nothing to do...
	}

	@Override
	public void merge() {
		// nothing to do...
	}

	@Override
	public void addAffiliation(BareJID jid, Affiliation affiliation) {
		//throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public void changeAffiliation(BareJID jid, Affiliation affiliation) {
		//throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public UsersAffiliation[] getAffiliations() {
		return new UsersAffiliation[0];
	}

	@Override
	public UsersAffiliation getSubscriberAffiliation(BareJID jid) {
		try {
			ChannelConfiguration channelConfiguration = mixRepository.getChannelConfiguration(channelJID);
			switch (nodeName) {
				case Mix.Nodes.CONFIG:
					switch (channelConfiguration.getConfigurationNodeAccess()) {
						case participants:
							if (channelConfiguration.isOwner(jid)) {
								return new UsersAffiliation(jid, Affiliation.owner);
							} else {
								return new UsersAffiliation(jid, !isParticipant(jid) ? Affiliation.none : Affiliation.member);
							}
						case admins:
							if (channelConfiguration.isOwner(jid)) {
								return new UsersAffiliation(jid, Affiliation.owner);
							} else if (channelConfiguration.isAdministrator(jid)) {
								return new UsersAffiliation(jid, !isParticipant(jid) ? Affiliation.none : Affiliation.member);
							} else {
								return new UsersAffiliation(jid, Affiliation.outcast);
							}
						case owners:
							if (channelConfiguration.isOwner(jid)) {
								return new UsersAffiliation(jid, Affiliation.owner);
							} else {
								return new UsersAffiliation(jid, Affiliation.none);
							}
						case allowed:
						case nobody:
						default:
							// TODO: add support when we add support for ALLOWED node!!!
							return new UsersAffiliation(jid, Affiliation.outcast);
					}
				case Mix.Nodes.INFO:
					if (channelConfiguration.isOwner(jid)) {
						return new UsersAffiliation(jid, Affiliation.owner);
					}
					ChannelNodePermission updatePermission = channelConfiguration.getInformationNodeUpdateRights();
					if (channelConfiguration.isAdministrator(jid) && updatePermission == ChannelNodePermission.admins) {
						return new UsersAffiliation(jid, Affiliation.publisher);
					}
					switch (channelConfiguration.getInformationNodeSubscription()) {
						case allowed:
						case participants:
							// TODO: add support when we add support for ALLOWED node!!!
							return new UsersAffiliation(jid, isParticipant(jid) ? (
									updatePermission == ChannelNodePermission.participants
									? Affiliation.publisher
									: Affiliation.member) : Affiliation.none);
						case anyone:
							return new UsersAffiliation(jid, Affiliation.member);
						default:
							return new UsersAffiliation(jid, Affiliation.none);
					}
				case Mix.Nodes.MESSAGES:
					switch (channelConfiguration.getMessagesNodeSubscription()) {
						case allowed:
							return new UsersAffiliation(jid, isParticipant(jid) ? Affiliation.member : Affiliation.none);
						case participants:
							// TODO: add support when we add support for ALLOWED node!!!
							return new UsersAffiliation(jid, isParticipant(jid) ? Affiliation.member : Affiliation.none);
						case anyone:
							return new UsersAffiliation(jid, Affiliation.member);
						default:
							return new UsersAffiliation(jid, Affiliation.none);
					}
				case Mix.Nodes.PARTICIPANTS:
					switch (channelConfiguration.getParticipantsNodeSubscription()) {
						case participants:
							return new UsersAffiliation(jid, isParticipant(jid) ? Affiliation.member : Affiliation.none);
						case anyone:
							return new UsersAffiliation(jid, Affiliation.member);
						case admins:
							return new UsersAffiliation(jid, channelConfiguration.isAdministrator(jid) ? Affiliation.member : Affiliation.none);
						case owners:
							return new UsersAffiliation(jid, channelConfiguration.isOwner(jid) ? Affiliation.member : Affiliation.none);
						case allowed:
						case nobody:
							// TODO: add support when we add support for ALLOWED node!!!
							return new UsersAffiliation(jid, Affiliation.none);
					}
				case Mix.Nodes.ALLOWED:
				case Mix.Nodes.BANNED:
					if (channelConfiguration.isOwner(jid)) {
						return new UsersAffiliation(jid, Affiliation.owner);
					} else {
						return new UsersAffiliation(jid, channelConfiguration.isAdministrator(jid) ? Affiliation.publisher : Affiliation.none);
					}
				default:
					return new UsersAffiliation(jid, Affiliation.none);
			}
		} catch (RepositoryException ex) {
			// TODO: Should we throw an exception in this case?
			return new UsersAffiliation(jid, Affiliation.none);
		}
	}

	protected boolean isParticipant(BareJID jid) throws RepositoryException {
		return mixRepository.getParticipant(channelJID, jid) != null;
	}

	protected boolean isSubscribed(BareJID jid) throws RepositoryException {
		ISubscriptions subscriptions = mixRepository.getNodeSubscriptions(channelJID, nodeName);
		if (subscriptions != null) {
			return subscriptions.getSubscription(jid) == Subscription.subscribed;
		}
		return false;
	}

	@Override
	public int size() {
		return 0;
	}
}
