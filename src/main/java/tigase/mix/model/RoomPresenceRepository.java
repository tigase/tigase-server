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
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.mix.modules.RoomPresenceModule;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ClusterModeRequired(active = false)
@Bean(name = "roomPresenceRepository", parent = RoomPresenceModule.class, active = true)
public class RoomPresenceRepository {

	private final Map<BareJID, Map<String,JID>> tempParticipants = new ConcurrentHashMap<>();

	@Inject
	private MixLogic mixLogic;
	@Inject
	private RoomGhostbuster ghostbuster;

	public void addTempParticipant(BareJID channelJID, JID occupantJID, String nickname) {
		Map<String, JID> participants = tempParticipants.computeIfAbsent(channelJID, k -> new ConcurrentHashMap<>());
		participants.put(nickname, occupantJID);
		ghostbuster.register(channelJID, occupantJID);
	}

	public void removeTempParticipant(BareJID channelJID, JID occupantJID, String nickname) {
		Map<String, JID> participants = tempParticipants.get(channelJID);
		if (participants != null) {
			if (nickname != null) {
				JID removed = participants.remove(nickname);
				if (removed != null) {
					ghostbuster.unregister(channelJID, removed);
				}
			} else {
				Optional<String> nicknameOptional = participants.entrySet().stream().filter(e -> occupantJID.equals(e.getValue())).map(
						Map.Entry::getKey).findFirst();
				nicknameOptional.ifPresent( nick -> {
					participants.remove(nick);
				});
				ghostbuster.unregister(channelJID, occupantJID);
			}
		}
	}

	public boolean isNicknameInUse(BareJID channelJID, JID occupantJID, String nickname) {
		Map<String, JID> participants = tempParticipants.get(channelJID);
		if (participants == null) {
			return false;
		}
		JID jid =  participants.get(nickname);
		if (jid == null) {
			return false;
		}
		return !occupantJID.equals(jid);
	}

	public Collection<JID> getRoomParticipantJids(BareJID channelJID) {
		return Optional.ofNullable(tempParticipants.get(channelJID)).map(Map::values).orElse(Collections.emptyList());
	}

	public Set<String> getRoomParticipantsIds(BareJID channelJID) {
		Map<String, JID> participants = tempParticipants.get(channelJID);
		if (participants == null) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<>();
		for (JID jid : participants.values()) {
			try {
				result.add(mixLogic.generateTempParticipantId(channelJID, jid));
			} catch (RepositoryException ex) {
				// nothing to do..
			}
		}
		return result;
	}

	public boolean isRoomParticipant(BareJID channelJID, JID jid) {
		Map<String, JID> participants = tempParticipants.get(channelJID);
		if (participants == null) {
			return false;
		}
		return participants.values().contains(jid);
	}

	public Set<Map.Entry<BareJID, Map<String,JID>>> getTempParticipantsPresence() {
		return tempParticipants.entrySet();
	}

	public boolean isParticipant(BareJID channelJID, JID sender) {
		return Optional.ofNullable(tempParticipants.get(channelJID))
				.map(Map::values)
				.filter(set -> set.contains(sender))
				.isPresent();
	}
}
