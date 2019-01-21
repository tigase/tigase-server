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
package tigase.kernel.modular;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

import java.util.ArrayList;
import java.util.Collection;

@Bean(name = "componentsManager", active = true)
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
