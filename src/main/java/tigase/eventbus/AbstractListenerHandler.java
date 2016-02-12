/*
 * AbstractListenerHandler.java
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

public abstract class AbstractListenerHandler<T> extends AbstractHandler {

	protected final T listener;

	protected AbstractListenerHandler(final String packageName, final String eventName, T listener) {
		super(packageName, eventName);
		this.listener = listener;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof AbstractListenerHandler))
			return false;

		AbstractListenerHandler that = (AbstractListenerHandler) o;

		return listener.equals(that.listener);

	}

	public T getListener() {
		return listener;
	}

	@Override
	public int hashCode() {
		return listener.hashCode();
	}

}
