/*
 * ClusterConnection.java
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
import java.util.concurrent.CopyOnWriteArrayList;
import tigase.xmpp.XMPPIOService;

/**
 *
 * @author andrzej
 */
public class ClusterConnection {
	
	private final String addr;
	private final CopyOnWriteArrayList<XMPPIOService<Object>> conns = new CopyOnWriteArrayList<>();
	
	public ClusterConnection(String addr) {
		this.addr = addr;
	}
	
	public void addConn(XMPPIOService<Object> conn) {
		conns.add(conn);
	}
	
	public void removeConn(XMPPIOService<Object> conn) {
		conns.remove(conn);
	}
	
	public int size() {
		return conns.size();
	}
	
	public List<XMPPIOService<Object>> getConnections() {
		return conns;
	}

	@Override
	public String toString() {
		return addr + conns;
	}
	
}
