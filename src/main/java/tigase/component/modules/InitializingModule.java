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
package tigase.component.modules;

/**
 * Interface to be implemented by {@link Module modules} that need to be
 * informed about moments in module lifecycle.
 * 
 * @author bmalkow
 * 
 */
public interface InitializingModule {

	/**
	 * Called after registration.
	 */
	void afterRegistration();

	/**
	 * Called before registering.
	 */
	void beforeRegister();

	/**
	 * Called before module is unregistered.
	 */
	void unregisterModule();

}
