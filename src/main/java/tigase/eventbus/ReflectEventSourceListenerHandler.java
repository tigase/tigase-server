/*
 * ReflectEventSourceListenerHandler.java
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

public class ReflectEventSourceListenerHandler extends ReflectEventListenerHandler {

	public ReflectEventSourceListenerHandler(HandleEvent.Type filter, String packageName, String eventName,
			Object consumerObject, Method handlerMethod) {
		super(filter, packageName, eventName, consumerObject, handlerMethod);
	}

	@Override
	public void dispatch(final Object event, final Object source, boolean remotelyGeneratedEvent) {
		if (remotelyGeneratedEvent && filter == HandleEvent.Type.local
				|| !remotelyGeneratedEvent && filter == HandleEvent.Type.remote)
			return;
		try {
			handlerMethod.invoke(consumerObject, event, source);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
