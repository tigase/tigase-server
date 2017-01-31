/*
 * ClusterConnectionSelector.java
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
import tigase.server.Priority;
import tigase.xmpp.XMPPIOService;

/**
 * Advanced implementation of ClusterConnectionSelectorIfc which separates packets
 * with priority CLUSTER or higher from other packets in cluster connections
 * by using separate connections for them
 * 
 * @author andrzej
 */
public class ClusterConnectionSelector implements ClusterConnectionSelectorIfc {

	protected static final String CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY = "cluster-sys-connections-per-node";
	
	private int allConns = ClusterConnectionManager.CLUSTER_CONNECTIONS_PER_NODE_VAL;
	private int sysConns = 2;
	private ClusterConnectionHandler handler;
	
	@Override
	public XMPPIOService<Object> selectConnection(Packet p, ClusterConnection conn) {
		if (conn == null)
			return null;
		
		int code  = Math.abs(handler.hashCodeForPacket(p));
		List<XMPPIOService<Object>> conns = conn.getConnections();
		if (conns.size() > 0) {
			if (conns.size() > sysConns) {
				if (p.getPriority() != null && p.getPriority().ordinal() <= Priority.CLUSTER.ordinal()) {
					return conns.get(code % sysConns);
				} else {
					return conns.get(sysConns + (code % (conns.size() - sysConns)));
				}
			} else {
				return conns.get(code % conns.size());
			}
		}
		return null;		
	}
	
	@Override
	public void setClusterConnectionHandler(ClusterConnectionHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void setProperties(Map<String,Object> props) {
		if (props.containsKey(CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY)) {
			sysConns = (Integer) props.get(CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY);
		}
		if (props.containsKey(ClusterConnectionManager.CLUSTER_CONNECTIONS_PER_NODE_PROP_KEY)) {
			allConns = (Integer) props.get(ClusterConnectionManager.CLUSTER_CONNECTIONS_PER_NODE_PROP_KEY);
		}
	}	
}
