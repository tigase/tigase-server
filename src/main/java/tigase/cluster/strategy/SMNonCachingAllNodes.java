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

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.ClusteringStrategyIfc;

import tigase.server.Packet;

import tigase.stats.StatisticsList;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	private static final Logger log = Logger.getLogger(SMNonCachingAllNodes.class.getName());

	//~--- fields ---------------------------------------------------------------

	// private Set<String> cluster_nodes = new ConcurrentSkipListSet<String>();
	private CopyOnWriteArrayList<JID> cl_nodes_list = new CopyOnWriteArrayList<JID>();

	//~--- methods --------------------------------------------------------------

	// private String smName = null;

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	@Override
	public boolean containsJid(JID jid) {
		return false;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public List<JID> getAllNodes() {
		return cl_nodes_list;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	@Override
	public JID[] getConnectionIdsForJid(JID jid) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	@Override
	public List<JID> getNodesForJid(JID jid) {

		// The code below, actually causes problems if there is a high traffic and
		// disconnects/reconnects occur between nodes. There is a race condition problem.
		// Adding a synchronization might be a solution but also a big performance problem.
		// Most of small systems have 2 cluster nodes anyway, so the code below is useless.
		// For bigger installation I recommend a different clustering strategy anyway.
		// Collections.rotate(cl_nodes_list, 1);
		return cl_nodes_list;
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean hasCompleteJidsInfo() {
		return false;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean needsSync() {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 */
	@Override
	public void nodeConnected(JID jid) {
		boolean result = cl_nodes_list.addIfAbsent(jid);

		log.log(Level.FINE, "Cluster nodes: {0}, added: {1}", new Object[] { cl_nodes_list, result });
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 */
	@Override
	public void nodeDisconnected(JID jid) {
		boolean result = cl_nodes_list.remove(jid);

		log.log(Level.FINE, "Cluster nodes: {0}, removed: {1}", new Object[] { cl_nodes_list, result });
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param sm
	 * @param results
	 * @param jid
	 */
	@Override
	public void userDisconnected(JID sm, Queue<Packet> results, JID jid) {}

	/**
	 * Method description
	 *
	 *
	 * @param sm
	 * @param results
	 * @param jid
	 */
	@Override
	public void usersConnected(JID sm, Queue<Packet> results, JID... jid) {}

//@Override
//public void init(String smName) {
//  this.smName = smName;
//}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
