/*
 * ReflectEventListenerHandler.java
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

import tigase.eventbus.HandleEvent;

public class ReflectEventListenerHandler extends AbstractHandler {

	protected final Object consumerObject;

	protected final Method handlerMethod;

	protected final HandleEvent.Type filter;

	public ReflectEventListenerHandler(HandleEvent.Type filter, final String packageName, final String eventName,
			Object consumerObject, Method handlerMethod) {
		super(packageName, eventName);
		this.filter = filter;
		this.consumerObject = consumerObject;
		this.handlerMethod = handlerMethod;
	}

	@Override
	public void dispatch(final Object event, final Object source, boolean remotelyGeneratedEvent) {
		if (remotelyGeneratedEvent && filter == HandleEvent.Type.local
				|| !remotelyGeneratedEvent && filter == HandleEvent.Type.remote)
			return;
		try {
			handlerMethod.invoke(consumerObject, event);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ReflectEventListenerHandler that = (ReflectEventListenerHandler) o;

		if (!consumerObject.equals(that.consumerObject))
			return false;
		return handlerMethod.equals(that.handlerMethod);

	}

	@Override
	public Type getRequiredEventType() {
		return Type.object;
	}

	@Override
	public int hashCode() {
		int result = consumerObject.hashCode();
		result = 31 * result + handlerMethod.hashCode();
		return result;
	}

}
