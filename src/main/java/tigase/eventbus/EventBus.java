/*
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
package tigase.eventbus;

import tigase.annotations.TigaseDeprecated;
import tigase.xml.Element;

public interface EventBus {

	/**
	 * Adds listener of event to EventBus.
	 *
	 * @param eventClass class of expected event.
	 * @param listener listener.
	 * @param <T> class of event.
	 */
	<T> void addListener(Class<T> eventClass, EventListener<T> listener);

	/**
	 * Adds listener of event to EventBus. If event matching to given packageName and eventName will be fired as Object
	 * (not Element), then event will be converted to XML.
	 *
	 * @param packageName package of event to listen.
	 * @param eventName name of event to listen. May be <code>null</code>, then listener is listening for all events
	 * with specific package name.
	 * @param listener listener.
	 */
	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	void addListener(String packageName, String eventName, EventListener<Element> listener);

	/**
	 * Fires event.
	 *
	 * @param event event to fire.
	 */
	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	void fire(Object event);

	/**
	 * Fires event.
	 *
	 * @param event event to fire.
	 */
	void fire(EventBusEvent event);

	/**
	 * Register all methods annotated with {@link HandleEvent @HandleEvent} as events handlers to EventBus.
	 *
	 * @param eventConsumer events consumer object.
	 *
	 * @throws RegistrationException if it is impossible to register all handlers method.
	 */
	void registerAll(Object eventConsumer);

	@Deprecated
	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Class based events should be used")
	void registerEvent(String event, String description, boolean privateEvent);

	public void registerEvent(Class<?> event, String description, boolean privateEvent);

	/**
	 * Removes listener from Eventbus.
	 *
	 * @param listener listener to remove.
	 */
	<T> void removeListener(EventListener<T> listener);

	/**
	 * Unregister all methods annotated with {@link HandleEvent @HandleEvent} as events handlers from EventBus.
	 *
	 * @param eventConsumer events consumer object.
	 */
	void unregisterAll(Object eventConsumer);

}
