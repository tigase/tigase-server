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

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ModuleProvider {

	/**
	 * Returns features offered by registered modules.
	 *
	 * @return collection of available features.
	 */
	Set<String> getAvailableFeatures();

	/**
	 * Return module implementation by module identifier.
	 *
	 * @param id identifier
	 *
	 * @return module implementation.
	 */
	<T extends Module> T getModule(String id);

	/**
	 * Returns list of all registered {@link Module modules} in current component.
	 *
	 * @return list of modules.
	 */
	List<Module> getModules();

	/**
	 * Returns collection of identifiers of registered {@link Module modules}.
	 *
	 * @return list of modules ID.
	 */
	Collection<String> getModulesId();

}
