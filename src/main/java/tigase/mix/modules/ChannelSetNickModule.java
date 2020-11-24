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
import tigase.mix.model.IParticipant;
import tigase.mix.model.MixAction;
import tigase.mix.model.MixLogic;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.exceptions.PubSubException;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.util.Optional;

@Bean(name="channelSetNickModule", parent = IMixComponent.class, active = true)
public class ChannelSetNickModule extends AbstractPubSubModule {

	private static final Criteria CRIT_SETNICK = ElementCriteria.nameType("iq", "set")
			.add(ElementCriteria.name("setnick", Mix.CORE1_XMLNS));
	
	@Inject
	private MixLogic mixLogic;
	@Inject
	private IMixRepository mixRepository;

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_SETNICK;
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
			String nick = Optional.ofNullable(packet.getElement())
					.map(el -> el.findChild(c -> c.getName() == "setnick" && c.getXMLNS() == Mix.CORE1_XMLNS))
					.map(el -> el.findChild(c -> c.getName() == "nick"))
					.map(Element::getCData)
					.orElseThrow(() -> new PubSubException(Authorization.NOT_ALLOWED));

			IParticipant participant = mixRepository.updateParticipant(channelJID, senderJID, nick);

			Element setnickEl = new Element("setnick");
			setnickEl.setXMLNS(Mix.CORE1_XMLNS);
			Element nickEl = new Element("nick", nick);
			setnickEl.addChild(nickEl);
			packetWriter.write(packet.okResult(setnickEl, 0));
		} catch (RepositoryException ex) {
			throw new PubSubException(Authorization.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}
}
