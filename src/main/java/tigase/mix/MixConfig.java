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
package tigase.mix;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.pubsub.IPubSubConfig;
import tigase.pubsub.PubSubConfig;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

@Bean(name = "config", parent = IMixComponent.class, active = true)
public class MixConfig implements IPubSubConfig {

	@ConfigField(desc = "Max Cache size", alias = PubSubConfig.MAX_CACHE_SIZE)
	private Integer maxCacheSize = 2000;
	
	private BareJID serviceBareJID;

	@Inject(bean = "service")
	private IMixComponent component;

	public void setComponent(IMixComponent component) {
		this.component = component;
		serviceBareJID = BareJID.bareJIDInstanceNS(component.getName(), "mix");
	}

	@Override
	public String[] getAdmins() {
		return new String[0];
	}

	@Override
	public JID getComponentJID() {
		return component.getComponentId();
	}

	@Override
	public long getDelayOnLowMemory() {
		return 0;
	}

	@Override
	public Integer getMaxCacheSize() {
		return maxCacheSize;
	}

	@Override
	public BareJID getServiceBareJID() {
		return serviceBareJID;
	}

	@Override
	public boolean isAutoSubscribeNodeCreator() {
		return false;
	}

	@Override
	public boolean isAdmin(BareJID jid) {
		return false;
	}

	@Override
	public boolean isAdmin(JID jid) {
		return false;
	}

	@Override
	public boolean isMAMEnabled() {
		return false;
	}

	@Override
	public boolean isPepPeristent() {
		return false;
	}

	@Override
	public boolean isPepRemoveEmptyGeoloc() {
		return false;
	}

	@Override
	public boolean isSendLastPublishedItemOnPresence() {
		return false;
	}

	@Override
	public boolean isSubscribeByPresenceFilteredNotifications() {
		return false;
	}

	@Override
	public boolean isHighMemoryUsage() {
		return false;
	}
}
