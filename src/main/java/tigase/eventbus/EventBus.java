/*
 * EventBus.java
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

package tigase.eventbus;

import tigase.xml.Element;

public interface EventBus {

	/**
	 * Adds listener of event to EventBus.
	 * 
	 * @param eventClass
	 *            class of expected event.
	 * @param listener
	 *            listener.
	 * @param <T>
	 *            class of event.
	 */
	<T> void addListener(Class<T> eventClass, EventListener<T> listener);

	/**
	 * Adds listener of event to EventBus. If event matching to given
	 * packageName and eventName will be fired as Object (not Element), then
	 * event will be converted to XML.
	 * 
	 * @param packageName
	 *            package of event to listen.
	 * @param eventName
	 *            name of event to listen. May be <code>null</code>, then
	 *            listener is listening for all events with specific package
	 *            name.
	 * @param listener
	 *            listener.
	 */
	void addListener(String packageName, String eventName, EventListener<Element> listener);

	/**
	 * Fires event.
	 *
	 * @param event
	 *            event to fire.
	 */
	void fire(Object event);

	/**
	 * Register all methods annotated with {@link HandleEvent @HandleEvent} as
	 * events handlers to EventBus.
	 *
	 * @param eventConsumer
	 *            events consumer object.
	 * @throws RegistrationException
	 *             if it is impossible to register all handlers method.
	 */
	void registerAll(Object eventConsumer);

	void registerEvent(String event, String description, boolean privateEvent);

	/**
	 * Removes listener from Eventbus.
	 * 
	 * @param listener
	 *            listener to remove.
	 * @param <T>
	 */
	<T> void removeListener(EventListener<T> listener);

	/**
	 * Unregister all methods annotated with {@link HandleEvent @HandleEvent} as
	 * events handlers from EventBus.
	 *
	 * @param eventConsumer
	 *            events consumer object.
	 */
	void unregisterAll(Object eventConsumer);

}
