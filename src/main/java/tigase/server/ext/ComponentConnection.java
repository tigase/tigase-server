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
package tigase.server.ext;

/**
 * Created: Sep 30, 2009 9:20:22 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentConnection
		implements Comparable<ComponentConnection> {

	private String domain = null;

	//private List<String> resources     = new ArrayList<String>();
	private ComponentIOService service = null;

	public ComponentConnection(String domain, ComponentIOService service) {
		this.domain = domain;
		this.service = service;
	}

	@Override
	public int compareTo(ComponentConnection o) {
		if (o == null) {
			return -1;
		}

		return service.getRemoteAddress().compareTo(o.service.getRemoteAddress());

		// return service.getUniqueId().compareTo(o.service.getUniqueId());
	}

	public String getDomain() {
		return domain;
	}

	public ComponentIOService getService() {
		return service;
	}
}

