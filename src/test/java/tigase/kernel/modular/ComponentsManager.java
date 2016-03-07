/*
 * ComponentsManager.java
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

package tigase.kernel.modular;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

import java.util.ArrayList;
import java.util.Collection;

@Bean(name = "componentsManager")
public class ComponentsManager {

	@Inject(nullAllowed = true)
	private Component[] components;

	public Component[] getComponents() {
		return components;
	}

	Collection<String> process(final String request) {
		ArrayList<String> response = new ArrayList<>();

		for (Component component : components) {
			response.add(component.execute(request));
		}

		return response;
	}

}
