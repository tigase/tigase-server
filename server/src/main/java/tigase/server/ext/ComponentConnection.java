/*
 * ComponentConnection.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param domain
	 * @param service
	 */
	public ComponentConnection(String domain, ComponentIOService service) {
		this.domain  = domain;
		this.service = service;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public int compareTo(ComponentConnection o) {
		if (o == null) {
			return -1;
		}

		return service.getRemoteAddress().compareTo(o.service.getRemoteAddress());

		// return service.getUniqueId().compareTo(o.service.getUniqueId());
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public ComponentIOService getService() {
		return service;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/16
