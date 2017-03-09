/*
 * ReceiverBareJidLB.java
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
 * @author Artur Hefczyc Created Jul 9, 2011
 */
public class ReceiverBareJidLB
				implements LoadBalancerIfc {
	@Override
	public ComponentIOService selectConnection(Packet p, List<ComponentConnection> conns) {
		ComponentIOService  result = null;
		int                 idx = Math.abs(p.getStanzaTo().getBareJID().hashCode() % conns
				.size());
		ComponentConnection conn   = conns.get(idx);

		if ((conn.getService() != null) && conn.getService().isConnected()) {
			result = conn.getService();
		}

		return result;
	}
}
