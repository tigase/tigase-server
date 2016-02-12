/*
 * ReflectEventRoutingSelector.java
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

import java.lang.reflect.Method;
import java.util.Collection;
import tigase.eventbus.component.stores.Subscription;

/**
 * This class is implementation of <code>EventRoutingSelector</code> used when
 * this selector is created based on annotated method of consumer class.
 * 
 * @author andrzej
 */
public class ReflectEventRoutingSelector implements EventRoutingSelector {

	private final Class eventClass;
	private final Object consumer;
	private final Method method;
	
	public ReflectEventRoutingSelector(Class eventClass, Object consumer, Method method) {
		this.eventClass = eventClass;
		this.consumer = consumer;
		this.method = method;
	}

	@Override
	public Class getEventClass() {
		return eventClass;
	}
	
	@Override
	public Collection<Subscription> getSubscriptions(Object event, Collection<Subscription> subscriptions) {
		try {
			return (Collection<Subscription>) method.invoke(consumer, event, subscriptions);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null || o.getClass() != getClass())
			return false;
		
		ReflectEventRoutingSelector s = (ReflectEventRoutingSelector) o;
		if (!consumer.equals(s.consumer))
			return false;
		return method.equals(s.method);
	}
	
	@Override
	public int hashCode() {
		int result = consumer.hashCode();
		result = 31 * result + method.hashCode();
		return result;
	}	
}
