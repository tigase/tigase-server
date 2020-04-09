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
import tigase.criteria.ElementCriteria;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.*;
import tigase.mix.model.*;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.Subscription;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.modules.PublishItemModule;
import tigase.pubsub.repository.ISubscriptions;
import tigase.pubsub.utils.PubSubLogic;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.util.Iterator;
import java.util.List;

@Bean(name="channelJoinModule", parent = IMixComponent.class, active = true)
public class ChannelJoinModule extends AbstractPubSubModule {

	private static final Criteria CRIT_JOIN = ElementCriteria.nameType("iq", "set")
			.add(ElementCriteria.name("join", Mix.CORE1_XMLNS));

	@Inject
	private MixLogic mixLogic;

	@Inject
	private IMixRepository mixRepository;

	@Inject
	private PublishItemModule publishItemModule;

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_JOIN;
	}

	@Inject(nullAllowed = true)
	private RoomPresenceModule roomPresenceModule;

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		try {
			if (packet.getStanzaTo().getLocalpart() == null) {
				throw new PubSubException(Authorization.BAD_REQUEST);
			}

			BareJID channelJID = packet.getStanzaTo().getBareJID();
			BareJID senderJID = packet.getStanzaFrom().getBareJID();

			Element joinEl = packet.getElemChild("join", Mix.CORE1_XMLNS);

			ChannelConfiguration config = mixRepository.getChannelConfiguration(channelJID);
			if (config == null) {
				throw new PubSubException(Authorization.ITEM_NOT_FOUND);
			}

			mixLogic.checkPermission(channelJID, senderJID, MixAction.join);

			String nick = joinEl.getCData(new String[]{"join", "nick"});
			if (config.isNickMandator() && (nick == null || nick.trim().isEmpty())) {
				throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Nick is required!");
			}
			
			IParticipant participant = mixRepository.updateParticipant(channelJID, senderJID, nick);

			List<String> nodes = joinEl.mapChildren(el -> el.getName() == "subscribe", el -> el.getAttributeStaticStr("node"));
			if (nodes != null) {
				Iterator<String> it = nodes.iterator();
				while (it.hasNext()) {
					try {
						String node = it.next();
						mixLogic.checkPermission(channelJID, node, packet.getStanzaFrom(), PubSubLogic.Action.subscribe);

						ISubscriptions subscriptions = getRepository().getNodeSubscriptions(channelJID, node);
						if (subscriptions == null) {
							it.remove();
							break;
						}
						subscriptions.addSubscriberJid(senderJID, Subscription.subscribed);
						getRepository().update(channelJID, node, subscriptions);
					} catch (Throwable ex) {
						it.remove();
					}
				}
			}

			Element responseContent = new Element("join", new String[]{"xmlns", "id"},
												  new String[]{Mix.CORE1_XMLNS, participant.getParticipantId()});
			if (nodes != null) {
				nodes.stream().map(node -> new Element("subscribe", new String[]{"node "}, new String[]{node})).forEach(responseContent::addChild);
			}
			if (nick != null) {
				responseContent.addChild(new Element("nick", nick));
			}

			packetWriter.write(packet.okResult(responseContent, 0));

			if (roomPresenceModule != null) {
				roomPresenceModule.participantJoined(channelJID, null, nick);
			}
		} catch (RepositoryException ex) {
			throw new PubSubException(Authorization.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}
}
