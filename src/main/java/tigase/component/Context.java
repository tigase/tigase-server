/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.component;

import tigase.component.modules.ModuleProvider;
import tigase.disteventbus.EventBus;
import tigase.xmpp.JID;

/**
 * Interface to provide configuration for an component.<br>
 * This interface should be extended if component requires access to database or
 * any other resources or informations.
 * 
 * @author bmalkow
 * 
 */
public interface Context {

	JID getComponentID();

	/**
	 * Returns version of component. Used for Service Discovery purposes.
	 * 
	 * @return version of component.
	 */
	String getComponentVersion();

	/**
	 * Returns category of component. Used for Service Discovery purposes.
	 * 
	 * @return category of component.
	 */
	String getDiscoCategory();

	/**
	 * Returns type of component. Used for Service Discovery purposes.
	 * 
	 * @return type of component.
	 */
	String getDiscoCategoryType();

	/**
	 * Returns human readable component name. Used for Service Discovery
	 * purposes.
	 * 
	 * @return name of component.
	 */
	String getDiscoDescription();

	/**
	 * Returns {@link EventBus}.
	 * 
	 * @return {@link EventBus} instance.
	 */
	EventBus getEventBus();

	/**
	 * Returns {@link ModuleProvider}.
	 * 
	 * @return {@link ModuleProvider} instance.
	 */
	ModuleProvider getModuleProvider();

	/**
	 * Returns {@link PacketWriter}.
	 * 
	 * @return {@link PacketWriter} instance.
	 */
	PacketWriter getWriter();

}
