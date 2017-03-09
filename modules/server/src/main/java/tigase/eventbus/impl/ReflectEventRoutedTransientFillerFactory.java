/*
 * ReflectEventRoutedTransientFillerFactory.java
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

import static tigase.util.ReflectionHelper.collectAnnotatedMethods;

import java.lang.reflect.Method;
import java.util.Collection;

import tigase.eventbus.EventRoutedTransientFiller;
import tigase.eventbus.FillRoutedEvent;
import tigase.eventbus.RegistrationException;
import tigase.util.ReflectionHelper.Handler;

/**
 * Class responsible for creation of <code>ReflectEventRoutedTransientFiller</code> instances
 * based on methods of consumer class annotated with <code>FillRoutedEvent</code> annotation.
 * 
 * @author andrzej
 */
public class ReflectEventRoutedTransientFillerFactory {
	
	private static final Handler<FillRoutedEvent,EventRoutedTransientFiller> HANDLER = (Object consumer, Method method, FillRoutedEvent annotation) -> {
		if (method.getParameterCount() < 1) {
			throw new RegistrationException("Event routing selection method must have parameter to receive event!");
		}

		final Class eventType = method.getParameters()[0].getType();
		method.setAccessible(true);
		return new ReflectEventRoutedTransientFiller(eventType, consumer, method);
	};

	public Collection<EventRoutedTransientFiller> create(Object consumer) {
		return collectAnnotatedMethods(consumer, FillRoutedEvent.class, HANDLER);
	}
	
}
