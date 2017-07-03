/*
 * VHostItemDefaults.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package tigase.vhosts;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.vhosts.filter.DomainFilterPolicy;
import tigase.xmpp.JID;

import java.util.concurrent.ConcurrentSkipListSet;

import static tigase.vhosts.VHostItem.*;

/**
 * Created by andrzej on 01.07.2017.
 */
@Bean(name = "defaults", parent = VHostManager.class, active = true, exportable = true)
public class VHostItemDefaults {

	@ConfigField(desc = "Check DNS for domain", alias = "vhost-disable-dns-check")
	private boolean disableDnsCheck = false;

	@ConfigField(desc = "Global trusted jids", alias = "trusted")
	private ConcurrentSkipListSet<String> trusted = null;

	@ConfigField(desc = "Maximal number of users", alias = VHOST_MAX_USERS_PROP_KEY)
	private long maxUsersNumber = VHOST_MAX_USERS_PROP_DEF;
	@ConfigField(desc = "Message forward JID", alias = VHOST_MESSAGE_FORWARD_PROP_KEY)
	private JID messageForward = JID.jidInstanceNS(VHOST_MESSAGE_FORWARD_PROP_DEF);
	@ConfigField(desc = "Presence forward JID", alias = VHOST_PRESENCE_FORWARD_PROP_KEY)
	private JID presenceForward = JID.jidInstanceNS(VHOST_PRESENCE_FORWARD_PROP_DEF);
	@ConfigField(desc = "TLS required", alias = VHOST_TLS_REQUIRED_PROP_KEY)
	private boolean tlsRequired = VHOST_TLS_REQUIRED_PROP_DEF;
	@ConfigField(desc = "S2S secret", alias = S2S_SECRET_PROP_KEY)
	private String  s2sSecret = S2S_SECRET_PROP_DEF;
	@ConfigField(desc = "Registration allowed", alias = VHOST_REGISTER_ENABLED_PROP_KEY)
	private boolean registerEnabled = VHOST_REGISTER_ENABLED_PROP_DEF;
	@ConfigField(desc = "Domain filter policy", alias = DOMAIN_FILTER_POLICY_PROP_KEY)
	private DomainFilterPolicy domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
	@ConfigField(desc = "ANONYMOUS authentication enabled", alias = VHOST_ANONYMOUS_ENABLED_PROP_KEY)
	private boolean anonymousEnabled = VHOST_ANONYMOUS_ENABLED_PROP_DEF;
	@ConfigField(desc = "Hardened mode", alias = "hardened-mode")
	private boolean hardenedMode = false;

	public long getMaxUsersNumber() {
		return maxUsersNumber;
	}

	public JID getMessageForward() {
		return messageForward;
	}

	public JID getPresenceForward() {
		return presenceForward;
	}

	public boolean isTlsRequired() {
		return tlsRequired;
	}

	public String getS2sSecret() {
		return s2sSecret;
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public DomainFilterPolicy getDomainFilter() {
		return domainFilter;
	}

	public boolean isAnonymousEnabled() {
		return anonymousEnabled;
	}

	public ConcurrentSkipListSet<String> getTrusted() {
		return trusted;
	}

	public boolean isCheckDns() {
		return !disableDnsCheck;
	}

	public boolean isHardenedMode() {
		return hardenedMode;
	}
}
