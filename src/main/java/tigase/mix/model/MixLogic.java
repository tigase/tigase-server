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
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.utils.PubSubLogic;
import tigase.server.Packet;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.function.Consumer;

public interface MixLogic extends PubSubLogic {

	void generateAffiliationChangesNotifications(BareJID channelJid,
														ChannelConfiguration oldConfig,
														ChannelConfiguration newConfig, Consumer<Packet> packetConsumer);

	String generateParticipantId(BareJID channelJID, BareJID participantRealJID) throws RepositoryException;

	String generateTempParticipantId(BareJID channelJID, JID participantRealJID) throws RepositoryException;

	void checkPermission(BareJID channel, BareJID senderJid, MixAction action)
			throws PubSubException, RepositoryException;

	boolean isChannelCreationAllowed(BareJID channelJID, BareJID senderJID);
}
