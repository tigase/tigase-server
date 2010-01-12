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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import tigase.cluster.ClusteringStrategyIfc;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xmpp.JID;

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

	//private Set<String> cluster_nodes = new ConcurrentSkipListSet<String>();
	private List<JID> cl_nodes_list = new CopyOnWriteArrayList<JID>();
	//private String smName = null;

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		return null;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
	}

	@Override
	public List<JID> getNodesForJid(JID jid) {
		Collections.rotate(cl_nodes_list, 1);
		return cl_nodes_list;
	}

	@Override
	public void nodeConnected(JID jid) {
		cl_nodes_list.add(jid);
		log.fine("Cluster nodes: " + cl_nodes_list.toString());
	}

	@Override
	public void nodeDisconnected(JID jid) {
		cl_nodes_list.remove(jid);
		log.fine("Cluster nodes: " + cl_nodes_list.toString());
	}

	@Override
	public void usersConnected(JID sm, Queue<Packet> results, JID ... jid) {
	}

	@Override
	public void userDisconnected(JID sm, Queue<Packet> results, JID jid) {
	}

	@Override
	public List<JID> getAllNodes() {
		return cl_nodes_list;
	}

	@Override
	public boolean needsSync() {
		return false;
	}

	@Override
	public void getStatistics(StatisticsList list) {	}

	@Override
	public boolean hasCompleteJidsInfo() {
		return false;
	}

	@Override
	public boolean containsJid(JID jid) {
		return false;
	}

	@Override
	public JID[] getConnectionIdsForJid(JID jid) {
		return null;
	}

//	@Override
//	public void init(String smName) {
//		this.smName = smName;
//	}

}
