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
import tigase.mix.model.MixAction;
import tigase.mix.model.MixLogic;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.exceptions.PubSubException;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

@Bean(name="channelDestroyModule", parent = IMixComponent.class, active = true)
public class ChannelDestroyModule extends AbstractPubSubModule {

	private static final Criteria CRIT_DESTROY = ElementCriteria.nameType("iq", "set")
			.add(ElementCriteria.name("destroy", Mix.CORE1_XMLNS));
	private static final String[] DESTROY_PATH = new String[] {Iq.ELEM_NAME, "destroy" };

	@Inject
	private MixLogic mixLogic;

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_DESTROY;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getStanzaTo().getLocalpart() != null) {
			throw new PubSubException(Authorization.BAD_REQUEST);
		}

		String channel = packet.getAttributeStaticStr(DESTROY_PATH, "channel");
		if (channel == null) {
			throw new PubSubException(Authorization.BAD_REQUEST);
		}

		BareJID channelJID = BareJID.bareJIDInstanceNS(channel, packet.getStanzaTo().getDomain());

		try {
			mixLogic.checkPermission(channelJID, packet.getStanzaFrom().getBareJID(), MixAction.manage);

//			// do we really need that? removing service should be enough...
//			String[] nodes = getRepository().getRootCollection(channelJID);
//			if (nodes != null) {
//				for (String node : nodes) {
//					if (node.equals(Mix.Nodes.CONFIG)) {
//						continue;
//					}
//					getRepository().deleteNode(channelJID, node);
//					getRepository().removeFromRootCollection(channelJID, node);
//					// how to remove service JID?
//				}
//				getRepository().deleteNode(channelJID, Mix.Nodes.CONFIG);
//				getRepository().removeFromRootCollection(channelJID, Mix.Nodes.CONFIG);
//			}
			getRepository().deleteService(channelJID);

			packetWriter.write(packet.okResult((Element) null,  0));
		} catch (RepositoryException ex) {
			throw new PubSubException(Authorization.INTERNAL_SERVER_ERROR, null, ex);
		}
	}

}
