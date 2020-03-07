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
package tigase.mix.modules;

import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.criteria.Criteria;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.IMixComponent;
import tigase.mix.model.IParticipant;
import tigase.mix.model.MixAction;
import tigase.mix.model.MixLogic;
import tigase.mix.model.MixRepository;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.exceptions.PubSubException;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

@Bean(name="channelRelayModule", parent = IMixComponent.class, active = true)
public class ChannelRelayModule extends AbstractPubSubModule {

	private static final Criteria CRIT_RELAY = new Criteria() {
		@Override
		public Criteria add(Criteria criteria) {
			return null;
		}

		@Override
		public boolean match(Element element) {
			if (element.getName() != "iq" && element.getName() != "presence") {
				return false;
			}
			String to = element.getAttributeStaticStr("to");
			if (to == null) {
				return false;
			}
			JID jid = JID.jidInstanceNS(to);
			if (jid.getLocalpart() == null) {
				return false;
			}
			return jid.getLocalpart().contains("#");
		}
	};

	@Inject
	private MixRepository mixRepository;
	@Inject
	private MixLogic mixLogic;

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_RELAY;
	}

	@Override
	public boolean canHandle(Packet packet) {
		if (packet.getElemName() != "iq" && packet.getElemName() != "message") {
			return false;
		}
		JID jid = packet.getStanzaTo();
		if (jid == null || jid.getLocalpart() == null) {
			return false;
		}
		return jid.getLocalpart().contains("#");
	}
	
	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		JID to = packet.getStanzaTo();
		if (to == null || to.getLocalpart() == null) {
			throw new PubSubException(Authorization.BAD_REQUEST);
		}

		int idx = to.getLocalpart().indexOf('#');
		String recipientId = to.getLocalpart().substring(0, idx);

		BareJID channelJID = BareJID.bareJIDInstanceNS(to.getLocalpart().substring(idx + 1), to.getDomain());
		BareJID senderJID = packet.getStanzaFrom().getBareJID();

		try {
			mixLogic.checkPermission(channelJID, senderJID, MixAction.relay);
			IParticipant recipient = mixRepository.getParticipant(channelJID, recipientId);
			IParticipant sender = mixRepository.getParticipant(channelJID, senderJID);
			if (recipient == null || sender == null) {
				throw new PubSubException(Authorization.ITEM_NOT_FOUND );
			}


			Packet forward = packet.copyElementOnly();
			forward.initVars(JID.jidInstanceNS(
					sender.getParticipantId() + "#" + channelJID.getLocalpart(), channelJID.getDomain(),
					packet.getStanzaFrom().getResource()), JID.jidInstanceNS(recipient.getRealJid(),
																			 packet.getStanzaTo().getResource()));
			packetWriter.write(forward);
		} catch (RepositoryException ex) {
			throw new PubSubException(Authorization.INTERNAL_SERVER_ERROR, null, ex);
		}
	}
}
