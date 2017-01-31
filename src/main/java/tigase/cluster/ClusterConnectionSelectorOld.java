/*
 * ClusterConnectionSelectorOld.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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
package tigase.cluster;

import java.util.List;
import java.util.Map;
import tigase.cluster.api.ClusterConnectionHandler;
import tigase.cluster.api.ClusterConnectionSelectorIfc;
import tigase.server.Packet;
import tigase.xmpp.XMPPIOService;

/**
 * ClusterConnectionSelectorOld class implements old cluster connection selection
 * algoritm which before was part of ClusterConnectionManager class.
 * 
 * @author andrzej
 */
public class ClusterConnectionSelectorOld implements ClusterConnectionSelectorIfc {

	private ClusterConnectionHandler handler;
	
	@Override
	public XMPPIOService<Object> selectConnection(Packet p, ClusterConnection conn) {
		if (conn == null)
			return null;
		
		int code  = Math.abs(handler.hashCodeForPacket(p));
		List<XMPPIOService<Object>> conns = conn.getConnections();
		if (conns.size() > 0) {
			return conns.get(code % conns.size());
		}
		return null;
	}

	@Override
	public void setClusterConnectionHandler(ClusterConnectionHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void setProperties(Map<String,Object> props) {
		
	}
	
}
