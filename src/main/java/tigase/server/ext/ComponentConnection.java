/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.ext;

import java.util.ArrayList;
import java.util.List;
import tigase.xmpp.XMPPIOService;

/**
 * Created: Sep 30, 2009 9:20:22 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentConnection implements Comparable<ComponentConnection> {

	private List<String> resources = new ArrayList<String>();
	private XMPPIOService<ComponentConnection> service = null;
	private String domain = null;

	public ComponentConnection(String domain, 
			XMPPIOService<ComponentConnection> service) {
		this.domain = domain;
		this.service = service;
	}

	@Override
	public int compareTo(ComponentConnection o) {
		if (o == null) {
			return -1;
		}
		return service.getUniqueId().compareTo(o.service.getUniqueId());
	}

	public XMPPIOService<ComponentConnection> getService() {
		return service;
	}

}
