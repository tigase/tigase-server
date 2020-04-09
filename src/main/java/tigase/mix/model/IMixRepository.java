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
import tigase.pubsub.repository.ISubscriptions;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.Optional;

public interface IMixRepository {

	String getChannelName(BareJID channelJID) throws RepositoryException;

	IParticipant getParticipant(BareJID channelJID, BareJID participantJID) throws RepositoryException;
	IParticipant getParticipant(BareJID channelJID, String participantId) throws RepositoryException;

	void removeParticiapnt(BareJID channelJID, BareJID participantJID) throws RepositoryException;

	IParticipant updateParticipant(BareJID channelJID, BareJID participantJID, String nick) throws RepositoryException,
																								   PubSubException;

	IParticipant updateTempParticipant(BareJID channelJID, JID participantJID, String nick) throws RepositoryException, PubSubException;

	void removeTempParticipant(BareJID channelJID, JID participantJID) throws RepositoryException;

	Optional<List<BareJID>> getAllowed(BareJID channelJID) throws RepositoryException;
	Optional<List<BareJID>> getBanned(BareJID channelJID) throws RepositoryException;

	ChannelConfiguration getChannelConfiguration(BareJID channelJID) throws RepositoryException;

	ISubscriptions getNodeSubscriptions(BareJID serviceJid, String nodeName) throws RepositoryException;
}
