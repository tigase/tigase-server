/*
 * ReflectEventRoutedTransientFiller.java
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

package tigase.eventbus.impl;

import java.lang.reflect.Method;

import tigase.eventbus.EventRoutedTransientFiller;

/**
 * Class responsible for calling method on consumer instance which will fill
 * event transient fields.
 * 
 * @author andrzej
 */
public class ReflectEventRoutedTransientFiller implements EventRoutedTransientFiller {

	private final Class eventClass;
	private final Object consumer;
	private final Method method;
	
	public ReflectEventRoutedTransientFiller(Class eventClass, Object consumer, Method method) {
		this.eventClass = eventClass;
		this.consumer = consumer;
		this.method = method;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null || o.getClass() != getClass())
			return false;

		ReflectEventRoutedTransientFiller s = (ReflectEventRoutedTransientFiller) o;
		if (!consumer.equals(s.consumer))
			return false;
		return method.equals(s.method);
	}

	@Override
	public boolean fillEvent(Object event) {
		try {
			return (Boolean) method.invoke(consumer, event);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Class getEventClass() {
		return eventClass;
	}
	
	@Override
	public int hashCode() {
		int result = consumer.hashCode();
		result = 31 * result + method.hashCode();
		return result;
	}	
}
