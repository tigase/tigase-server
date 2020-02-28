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
import tigase.mix.IMixComponent;
import tigase.mix.Mix;
import tigase.mix.model.IMixRepository;
import tigase.mix.model.MixAction;
import tigase.mix.model.MixLogic;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.Subscription;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.repository.ISubscriptions;
import tigase.pubsub.utils.PubSubLogic;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.util.Iterator;
import java.util.List;

@Bean(name="channelUpdateSubscriptionkModule", parent = IMixComponent.class, active = true)
public class ChannelUpdateSubscription extends AbstractPubSubModule {

	private static final Criteria CRIT_UPDATE_SUBSCRIPTION = ElementCriteria.nameType("iq", "set")
			.add(ElementCriteria.name("update-subscription", Mix.CORE1_XMLNS));

	@Inject
	private MixLogic mixLogic;
	@Inject
	private IMixRepository mixRepository;

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_UPDATE_SUBSCRIPTION;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getStanzaTo().getLocalpart() == null) {
			throw new PubSubException(Authorization.BAD_REQUEST);
		}

		BareJID channelJID = packet.getStanzaTo().getBareJID();
		BareJID senderJID = packet.getStanzaFrom().getBareJID();

		try {
			mixLogic.checkPermission(channelJID, senderJID, MixAction.publish);

			Element updateSubscriptionEl = packet.getElemChild("update-subscription", Mix.CORE1_XMLNS);
			List<String> nodes = updateSubscriptionEl.mapChildren(el -> el.getName() == "subscribe",
																  el -> el.getAttributeStaticStr("node"));
			if (nodes != null) {
				Iterator<String> it = nodes.iterator();
				while (it.hasNext()) {
					try {
						String node = it.next();
						mixLogic.checkPermission(channelJID, node, packet.getStanzaFrom(),
												 PubSubLogic.Action.subscribe);

						ISubscriptions subscriptions = getRepository().getNodeSubscriptions(channelJID, node);
						if (subscriptions == null) {
							it.remove();
							break;
						}
						if (subscriptions.getSubscription(senderJID) == null) {
							subscriptions.addSubscriberJid(senderJID, Subscription.subscribed);
							getRepository().update(channelJID, node, subscriptions);
						}
					} catch (Throwable ex) {
						it.remove();
					}
				}
			}
			Element responseContent = new Element("update-subscription", new String[]{"xmlns", "jid"},
												  new String[]{Mix.CORE1_XMLNS, senderJID.toString()});
			if (nodes != null) {
				nodes.stream().map(node -> new Element("subscribe", new String[]{"node "}, new String[]{node})).forEach(responseContent::addChild);
			}

			packetWriter.write(packet.okResult(responseContent, 0));
		} catch (RepositoryException ex) {
			throw new PubSubException(Authorization.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}
}
