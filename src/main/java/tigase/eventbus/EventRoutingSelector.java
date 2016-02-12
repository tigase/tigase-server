/*
 * EventRoutingSelector.java
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
package tigase.eventbus;

import java.util.Collection;
import tigase.eventbus.component.stores.Subscription;

/**
 * This interface is required to be implemented by every class which wants to change
 * routing of delivering events to other machines.
 * 
 * @author andrzej
 */
public interface EventRoutingSelector {
	
	/**
	 * Returns class of event for which it modifies delivery
	 * 
	 * @return 
	 */
	Class getEventClass();
	
	/**
	 * Method responsible for actual modification of delivery by adding and removing
	 * items to Subscriptions collection
	 * 
	 * @param event				instance of event
	 * @param subscriptions		original list of subscriptions
	 * @return					modified list of subscriptions
	 */
	Collection<Subscription> getSubscriptions(Object event, Collection<Subscription> subscriptions);
	
}
