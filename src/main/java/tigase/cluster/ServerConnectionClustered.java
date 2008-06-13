/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.cluster;

import java.util.Set;
import java.util.LinkedHashSet;

import tigase.server.xmppserver.ServerConnectionManager;

/**
 * Describe class ServerConnectionClustered here.
 *
 *
 * Created: Fri Jun 13 14:57:41 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServerConnectionClustered extends ServerConnectionManager
	implements ClusteredComponent {

	private Set<String> cluster_nodes = new LinkedHashSet<String>();

	/**
	 * Creates a new <code>ServerConnectionClustered</code> instance.
	 *
	 */
	public ServerConnectionClustered() {

	}

	private void queryClusterNodesForKey(String local_hostname,
		String remote_hostname, String session_id, String db_key) {
		
	}

	public void nodesConnected(Set<String> node_hostnames) {
		for (String node: node_hostnames) {
			cluster_nodes.add(DEF_S2S_NAME + "@" + node);
		}
	}

	public void nodesDisconnected(Set<String> node_hostnames) {
		for (String node: node_hostnames) {
			cluster_nodes.remove(DEF_S2S_NAME + "@" + node);
		}
	}

}
