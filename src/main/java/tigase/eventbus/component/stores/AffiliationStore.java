/*
 * AffiliationStore.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 */

package tigase.eventbus.component.stores;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.JID;

@Bean(name = "affiliations")
public class AffiliationStore {

	private final Map<JID, Affiliation> affiliations = new ConcurrentHashMap<JID, Affiliation>();

	@ConfigField(desc = "List of JIDs what can subscribe for events")
	private JID[] allowedSubscribers;

	public Affiliation getAffiliation(final JID jid) {
		Affiliation a = affiliations.get(jid);
		if (a == null && allowedSubscribers != null) {
			for (JID j : allowedSubscribers) {
				if (j.getResource() != null && j.equals(jid)) {
					return Affiliation.member;
				} else if (j.getResource() == null && j.getBareJID().equals(jid.getBareJID())) {
					return Affiliation.member;
				}
			}
		}
		return a == null ? Affiliation.none : a;
	}

	public JID[] getAllowedSubscribers() {
		return allowedSubscribers;
	}

	public void setAllowedSubscribers(JID[] allowedSubscribers) {
		this.allowedSubscribers = allowedSubscribers;
	}

	public void putAffiliation(JID jid, Affiliation affiliation) {
		this.affiliations.put(jid, affiliation);
	}

	public void removeAffiliation(JID jid) {
		this.affiliations.remove(jid);
	}
}
