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
import tigase.mix.model.ChannelConfiguration;
import tigase.mix.model.MixAction;
import tigase.mix.model.MixLogic;
import tigase.pubsub.*;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.repository.IItems;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.datetime.TimestampHelper;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static tigase.pubsub.AbstractNodeConfig.PUBSUB;

@Bean(name="channelCreateModule", parent = IMixComponent.class, active = true)
public class ChannelCreateModule extends AbstractPubSubModule {

	private static final Criteria CRIT_CREATE = ElementCriteria.nameType("iq", "set")
			.add(ElementCriteria.name("create", Mix.CORE1_XMLNS));
	private static final String[] CREATE_PATH = new String[] { Iq.ELEM_NAME, "create" };

	private static final tigase.util.datetime.TimestampHelper timestampHelper = new TimestampHelper();

	@Inject
	private MixLogic mixLogic;

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_CREATE;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getStanzaTo().getLocalpart() != null) {
			throw new PubSubException(Authorization.BAD_REQUEST);
		}
		
		String channel = packet.getAttributeStaticStr(CREATE_PATH, "channel");
		
		boolean isAdHoc = channel == null;
		if (channel == null) {
			channel = UUID.randomUUID().toString();
		}
		
		BareJID channelJID = BareJID.bareJIDInstanceNS(channel, packet.getStanzaTo().getDomain());
		BareJID owner = packet.getStanzaFrom().getBareJID();

		try {
			mixLogic.checkPermission(channelJID, owner, MixAction.manage);

			if (getRepository().getNodeConfig(channelJID, Mix.Nodes.CONFIG) != null) {
				throw new PubSubException(Authorization.CONFLICT);
			}
			getRepository().createService(channelJID, !isAdHoc);

			ChannelConfiguration channelConfig = new ChannelConfiguration();
			channelConfig.setLastChangeMadeBy(owner);
			channelConfig.setOwners(Collections.singleton(owner));
			LeafNodeConfig config = new LeafNodeConfig(Mix.Nodes.CONFIG);
			config.setValue(PUBSUB + "max_items", "1");
			config.setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
			config.setValue(PUBSUB + "publish_model",PublisherModel.publishers.name());
			config.setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
			getRepository().createNode(channelJID, Mix.Nodes.CONFIG, owner,
									   config, NodeType.leaf, null);
			getRepository().addToRootCollection(channelJID, Mix.Nodes.CONFIG);

			config = new LeafNodeConfig(Mix.Nodes.PARTICIPANTS);
			config.setValue(PUBSUB + "max_items", null);
			config.setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
			config.setValue(PUBSUB + "publish_model",PublisherModel.publishers.name());
			config.setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
			config.setValue(PUBSUB + "notification_type", StanzaType.normal.name());
			getRepository().createNode(channelJID, Mix.Nodes.PARTICIPANTS, owner,
									   config, NodeType.leaf, null);
			getRepository().addToRootCollection(channelJID, Mix.Nodes.PARTICIPANTS);

			config = new LeafNodeConfig(Mix.Nodes.MESSAGES);
			config.setValue(PUBSUB + "max_items", null);
			config.setValue(PUBSUB + "pubsub#persist_items", false);
			config.setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
			config.setValue(PUBSUB + "publish_model",PublisherModel.publishers.name());
			config.setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
			config.setValue(PUBSUB + "notification_type", StanzaType.normal.name());
			getRepository().createNode(channelJID, Mix.Nodes.MESSAGES, owner,
									   config, NodeType.leaf, null);
			getRepository().addToRootCollection(channelJID, Mix.Nodes.MESSAGES);

			config = new LeafNodeConfig(Mix.Nodes.INFO);
			config.setValue(PUBSUB + "max_items", "1");
			config.setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
			config.setValue(PUBSUB + "publish_model",PublisherModel.publishers.name());
			config.setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
			config.setValue(PUBSUB + "notification_type", StanzaType.normal.name());
			getRepository().createNode(channelJID, Mix.Nodes.INFO, owner,
									   config, NodeType.leaf, null);
			getRepository().addToRootCollection(channelJID, Mix.Nodes.INFO);

			config = new LeafNodeConfig(Mix.Nodes.AVATAR_DATA);
			config.setValue(PUBSUB + "max_items", "1");
			config.setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
			config.setValue(PUBSUB + "publish_model",PublisherModel.publishers.name());
			config.setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
			config.setValue(PUBSUB + "notification_type", StanzaType.headline.name());
			getRepository().createNode(channelJID, Mix.Nodes.AVATAR_DATA, owner,
									   config, NodeType.leaf, null);
			getRepository().addToRootCollection(channelJID, Mix.Nodes.AVATAR_DATA);

			config = new LeafNodeConfig(Mix.Nodes.AVATAR_METADATA);
			config.setValue(PUBSUB + "max_items", "1");
			config.setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
			config.setValue(PUBSUB + "publish_model",PublisherModel.publishers.name());
			config.setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
			config.setValue(PUBSUB + "notification_type", StanzaType.normal.name());
			getRepository().createNode(channelJID, Mix.Nodes.AVATAR_METADATA, owner,
									   config, NodeType.leaf, null);
			getRepository().addToRootCollection(channelJID, Mix.Nodes.AVATAR_METADATA);

			channelConfig.setNodesPresent(new String[] {"participants", "information", "avatar"});
			IItems nodeItems = getRepository().getNodeItems(channelJID, Mix.Nodes.CONFIG);
			String configItemId = timestampHelper.format(new Date());
			nodeItems.writeItem(configItemId, owner.toString(), channelConfig.toElement(configItemId), null);

			Element item = new Element("item", new String[] {"id"}, new String[] {configItemId });
			new DataForm.Builder(item, Command.DataType.result).withFields(builder -> {
				builder.addField(DataForm.FieldType.Hidden, "FORM_TYPE").setValue(Mix.CORE1_XMLNS).build();
				builder.addField(DataForm.FieldType.TextSingle, "Name").setLabel("Channel Name").setValue("").build();
				builder.addField(DataForm.FieldType.TextSingle, "Description").setLabel("Channel Description").setValue("").build();
				builder.addField(DataForm.FieldType.JidMulti, "Contact").setLabel("Channel Administrative Contact").build();
			}).build();
			nodeItems = getRepository().getNodeItems(channelJID, Mix.Nodes.INFO);
			nodeItems.writeItem(configItemId, owner.toString(), item, null);

			Packet response = packet.okResult(new Element("create", new String[] {"xmlns", "channel"}, new String[] {Mix.CORE1_XMLNS, channel}), 0);
			response.getElemChild("create", Mix.CORE1_XMLNS).setAttribute("channel", channel);
			packetWriter.write(response);
		} catch (RepositoryException ex) {
			throw new PubSubException(Authorization.INTERNAL_SERVER_ERROR, null, ex);
		}
	}
}
