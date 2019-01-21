/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

import tigase.eventbus.HandleEvent;
import tigase.eventbus.RegistrationException;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import static tigase.util.reflection.ReflectionHelper.Handler;
import static tigase.util.reflection.ReflectionHelper.collectAnnotatedMethods;

public class ReflectEventListenerHandlerFactory {

	private static final Handler<HandleEvent, AbstractHandler> HANDLER = (Object consumer, Method method, HandleEvent annotation) -> {
		if (method.getParameterCount() < 1) {
			throw new RegistrationException("Handler method must have parameter to receive event!");
		}

		final Class eventType = method.getParameters()[0].getType();
		final String packageName = eventType.getPackage().getName();
		final String eventName = eventType.getSimpleName();

		ReflectEventListenerHandler handler;
		switch (method.getParameterCount()) {
			case 1:
				handler = new ReflectEventListenerHandler(annotation.filter(), packageName, eventName, consumer,
														  method);
				break;
			case 2:
				final Class sourPar = method.getParameters()[1].getType();
				if (!sourPar.equals(Object.class)) {
					throw new RegistrationException("Second parameter (event source) must be Object type.");
				}
				handler = new ReflectEventSourceListenerHandler(annotation.filter(), packageName, eventName, consumer,
																method);
				break;
			default:
				throw new RegistrationException("Handler method must have exactly one parameter!");
		}

		method.setAccessible(true);

		return handler;
	};
	protected final Logger log = Logger.getLogger(this.getClass().getName());

	public Collection<AbstractHandler> create(final Object consumer) throws RegistrationException {
		return collectAnnotatedMethods(consumer, HandleEvent.class, HANDLER);
	}

}
