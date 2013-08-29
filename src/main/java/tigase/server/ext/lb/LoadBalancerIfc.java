/*
 * LoadBalancerIfc.java
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



package tigase.server.ext.lb;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentIOService;
import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

/**
 * @author Artur Hefczyc
 * Created Jul 9, 2011
 */
public interface LoadBalancerIfc {
	/**
	 * @param p
	 * @param conns
	 *
	 *
	 * @return a value of <code>ComponentIOService</code>
	 */
	ComponentIOService selectConnection(Packet p, List<ComponentConnection> conns);
}


//~ Formatted in Tigase Code Convention on 13/08/28
