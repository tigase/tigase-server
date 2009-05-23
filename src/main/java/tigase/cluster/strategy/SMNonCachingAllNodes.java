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

package tigase.cluster.strategy;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;
import tigase.cluster.ClusteringStrategyIfc;
import tigase.server.Packet;

/**
 * Created: May 13, 2009 9:53:44 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SMNonCachingAllNodes implements ClusteringStrategyIfc {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger(SMNonCachingAllNodes.class.getName());

	private Set<String> cluster_nodes = new ConcurrentSkipListSet<String>();
	private String[] cl_nodes_array = new String[0];
	//private String smName = null;

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		return null;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
	}

	@Override
	public String[] getNodesForJid(String jid) {
		return cl_nodes_array;
	}

	@Override
	public void nodeConnected(String jid) {
		cluster_nodes.add(jid);
		cl_nodes_array = cluster_nodes.toArray(new String[cluster_nodes.size()]);
		log.fine("Cluster nodes: " + cluster_nodes.toString());
	}

	@Override
	public void nodeDisconnected(String jid) {
		cluster_nodes.remove(jid);
		cl_nodes_array = cluster_nodes.toArray(new String[cluster_nodes.size()]);
		log.fine("Cluster nodes: " + cluster_nodes.toString());
	}

	@Override
	public void userConnected(String jid, String sm, Queue<Packet> results) {
	}

	@Override
	public void userDisconnected(String jid, String sm, Queue<Packet> results) {
	}

	@Override
	public String[] getAllNodes() {
		return cl_nodes_array;
	}

//	@Override
//	public void init(String smName) {
//		this.smName = smName;
//	}

}
