/*
 * ClusterConnectionSelectorIfc.java
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
package tigase.cluster.api;

import java.util.Map;
import tigase.cluster.ClusterConnection;
import tigase.server.Packet;
import tigase.xmpp.XMPPIOService;

/**
 * Interface ClusterConnectionSelectorIfc is base interface for classes responsible
 * for selecting connection which should be used to send packet between cluster nodes
 * 
 * @author andrzej
 */
public interface ClusterConnectionSelectorIfc {
	
	/**
	 * Method returns XMPPIOService instances which should be used to
	 * send packet between cluster nodes
	 * 
	 * @param packet
	 * @param conn
	 * @return 
	 */
	XMPPIOService<Object> selectConnection(Packet packet, ClusterConnection conn);
	
	void setClusterConnectionHandler(ClusterConnectionHandler handler);

	void setProperties(Map<String,Object> props);
	
}
