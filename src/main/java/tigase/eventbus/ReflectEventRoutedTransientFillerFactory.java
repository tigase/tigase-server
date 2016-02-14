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
 *
 */
package tigase.eventbus;

import java.lang.reflect.Method;
import java.util.Collection;
import tigase.util.ReflectionHelper.Handler;
import static tigase.util.ReflectionHelper.collectAnnotatedMethods;

/**
 * Class responsible for creation of <code>ReflectEventRoutedTransientFiller</code> instances
 * based on methods of consumer class annotated with <code>FillRoutedEvent</code> annotation.
 * 
 * @author andrzej
 */
public class ReflectEventRoutedTransientFillerFactory {
	
	public Collection<EventRoutedTransientFiller> create(Object consumer) {
		return collectAnnotatedMethods(consumer, FillRoutedEvent.class, HANDLER);
	}
	
	private static final Handler<FillRoutedEvent,EventRoutedTransientFiller> HANDLER = (Object consumer, Method method, FillRoutedEvent annotation) -> {
		if (method.getParameterCount() < 1) {
			throw new RegistrationException("Event routing selection method must have parameter to receive event!");
		}

		final Class eventType = method.getParameters()[0].getType();
		return new ReflectEventRoutedTransientFiller(eventType, consumer, method);
	};
	
}
