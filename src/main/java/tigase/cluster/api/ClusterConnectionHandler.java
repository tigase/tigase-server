/*
 * ClusterConnectionHandler.java
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

import tigase.server.Packet;

/**
 * ClusterConnectionHandler interface used by ClusterConnectionSelectorIfc
 * implementations to separate implementation from ClusterConnectionManager
 * 
 * @author andrzej
 */
public interface ClusterConnectionHandler {

	/**
	 * Generates hashCode for particular packet used to spread processing between
	 * thread or connections
	 * 
	 * @param packet
	 * @return 
	 */
	int hashCodeForPacket(Packet packet);
	
}
