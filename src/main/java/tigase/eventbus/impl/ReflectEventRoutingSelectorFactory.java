/*
 * ReflectEventRoutingSelectorFactory.java
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

import static tigase.util.ReflectionHelper.Handler;
import static tigase.util.ReflectionHelper.collectAnnotatedMethods;

import java.lang.reflect.Method;
import java.util.Collection;

import tigase.eventbus.EventRoutingSelector;
import tigase.eventbus.RegistrationException;
import tigase.eventbus.RouteEvent;

/**
 * Class responsible for generation of <code>EventRoutingSelectors</code> based
 * on methods of consumer class annotated with <code>@RouteEvent</code>
 *
 * @author andrzej
 */
public class ReflectEventRoutingSelectorFactory {

	private static final Handler<RouteEvent, EventRoutingSelector> HANDLER = (Object consumer, Method method,
			RouteEvent annotation) -> {
		if (method.getParameterCount() < 1) {
			throw new RegistrationException("Event routing selection method must have parameter to receive event!");
		}

		final Class eventType = method.getParameters()[0].getType();
		method.setAccessible(true);
		return new ReflectEventRoutingSelector(eventType, consumer, method);
	};

	/**
	 * Method looks for methods of consumer class and returns list of
	 * <code>EventRoutingSelectors</code> created based on methods annotated
	 * with <code>@RouteEvent</code>
	 *
	 * @param consumer
	 * @return
	 */
	public Collection<EventRoutingSelector> create(Object consumer) {
		return collectAnnotatedMethods(consumer, RouteEvent.class, HANDLER);
	}
}
