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

import tigase.kernel.beans.Bean;
import tigase.mix.IMixComponent;
import tigase.pubsub.AccessModel;
import tigase.pubsub.PublisherModel;
import tigase.pubsub.SendLastPublishedItem;
import tigase.xmpp.StanzaType;

@Bean(name = "defaultNodeConfig", parent = IMixComponent.class, active = true)
public class DefaultNodeConfig extends tigase.pubsub.DefaultNodeConfig {

	public DefaultNodeConfig() {
		super();
		setValue(PUBSUB + "access_model", AccessModel.whitelist.name());
		setValue(PUBSUB + "publish_model", PublisherModel.publishers.name());
		setValue(PUBSUB + "send_last_published_item", SendLastPublishedItem.never.name());
		setValue(PUBSUB + "notification_type", StanzaType.headline.name());
	}

}
