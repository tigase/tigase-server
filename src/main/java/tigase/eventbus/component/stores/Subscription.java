/*
 * Subscription.java
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
 *
 */
package tigase.eventbus.component.stores;

import tigase.xmpp.JID;

public class Subscription {

	private final JID jid;

	private JID serviceJID;

	private boolean inClusterSubscription;

	public Subscription(JID jid) {
		super();
		this.jid = jid;
	}

	public Subscription(JID jid, JID serviceJID) {
		super();
		this.jid = jid;
		this.serviceJID = serviceJID;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (jid == null) {
			if (other.jid != null)
				return false;
		} else if (!jid.equals(other.jid))
			return false;
		return true;
	}

	public JID getJid() {
		return jid;
	}

	public JID getServiceJID() {
		return serviceJID;
	}

	public void setServiceJID(JID serviceJID) {
		this.serviceJID = serviceJID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jid == null) ? 0 : jid.hashCode());
		return result;
	}

	public boolean isInClusterSubscription() {
		return inClusterSubscription;
	}

	public void setInClusterSubscription(boolean inClusterSubscription) {
		this.inClusterSubscription = inClusterSubscription;
	}

	@Override
	public String toString() {
		return "Subscription{" + "jid=" + jid + ", serviceJID=" + serviceJID + '}';
	}
}
