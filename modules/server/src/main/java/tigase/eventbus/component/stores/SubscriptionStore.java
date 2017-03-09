/*
 * SubscriptionStore.java
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

import tigase.eventbus.component.EventBusComponent;
import tigase.eventbus.impl.EventName;
import tigase.eventbus.impl.EventsNameMap;
import tigase.kernel.beans.Bean;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Bean(name = "subscriptionStore", parent = EventBusComponent.class, active = true)
public class SubscriptionStore {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final EventsNameMap<Subscription> subscribers = new EventsNameMap<Subscription>();

	public void addSubscription(String eventPackage, String eventName, Subscription subscription) {
		subscribers.put(eventPackage, eventName, subscription);
	}

	public Collection<Subscription> getAllData() {
		return subscribers.getAllData();
	}

	public Set<EventName> getSubscribedEvents() {
		return subscribers.getAllListenedEvents();
	}

	public Collection<Subscription> getSubscribersJIDs(String eventPackage, String eventName) {
		final HashSet<Subscription> handlers = new HashSet<Subscription>();
		handlers.addAll(subscribers.get(eventPackage, eventName));
		handlers.addAll(subscribers.get(eventPackage, null));
		return handlers;
	}

	public boolean hasSubscriber(String eventPackage, String eventName) {
		return subscribers.hasData(eventPackage, eventName);
	}

	public void remove(Subscription jid) {
		subscribers.delete(jid);
	}

	public void removeSubscription(String eventPackage, String eventName, Subscription jidInstanceNS) {
		subscribers.delete(eventPackage, eventName, jidInstanceNS);
	}

}
