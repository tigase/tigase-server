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
import tigase.mix.IMixComponent;
import tigase.mix.Mix;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.CollectionItemsOrdering;
import tigase.pubsub.repository.IItems;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.util.Optional;

@Bean(name = "roomVCardModule", parent = IMixComponent.class, active = true)
public class RoomVCardModule extends AbstractPubSubModule {

	private static final String[] FEATURES = new String[] { "vcard-temp" };

	private static final Criteria CRIT = ElementCriteria.nameType("iq", "get")
			.add(ElementCriteria.name("vCard", "vcard-temp"));

	@Override
	public boolean canHandle(Packet packet) {
		if (packet.getStanzaTo().getResource() == null && packet.getStanzaTo().getLocalpart() != null) {
			return super.canHandle(packet);
		}
		return false;
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		try {
			Element vcardTemp = new Element("vCard").withAttribute("xmlns", "vcard-temp");
			Optional<String> mimeType = getAvatarType(packet.getStanzaTo().getBareJID());
			if (mimeType.isPresent()) {
				Optional<String> data = getAvatarData(packet.getStanzaTo().getBareJID());
				if (data.isPresent()) {
					vcardTemp.addChild(new Element("PHOTO").withElement("TYPE", null, mimeType.get())
											   .withElement("BINVAL", null, data.get()));
				}
			}
			packetWriter.write(packet.okResult(vcardTemp, 0));
		} catch (RepositoryException ex) {
			throw new ComponentException(Authorization.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}

	protected Optional<String> getAvatarData(BareJID channelJID) throws RepositoryException, ComponentException {
		IItems items = getRepository().getNodeItems(channelJID, Mix.Nodes.AVATAR_DATA);
		if (items == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(items.getLastItem(CollectionItemsOrdering.byUpdateDate))
				.map(IItems.IItem::getItem)
				.map(item -> item.getChild("data", "urn:xmpp:avatar:data"))
				.map(el -> el.getCData());
	}

	protected Optional<String> getAvatarType(BareJID channelJID) throws RepositoryException, ComponentException {
		IItems items = getRepository().getNodeItems(channelJID, Mix.Nodes.AVATAR_METADATA);
		if (items == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(items.getLastItem(CollectionItemsOrdering.byUpdateDate))
				.map(IItems.IItem::getItem)
				.map(item -> item.getChild("metadata", "urn:xmpp:avatar:metadata"))
				.map(el -> el.getChild("info"))
				.map(el -> el.getAttributeStaticStr("type"));
	}
}
