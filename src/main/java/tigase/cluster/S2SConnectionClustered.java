/*
 * S2SConnectionClustered.java
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



package tigase.cluster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.CommandListener;
import tigase.cluster.api.CommandListenerAbstract;

import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.S2SConnectionManager;

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Created: Jun 27, 2010 10:33:51 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SConnectionClustered
				extends S2SConnectionManager
				implements ClusteredComponentIfc {
	private static final String ASKING_SESSION_ID       = "asking_sessionId";
	private static final String CHECK_DB_KEY_CMD        = "check-db-key-s2s-cmd";
	private static final String CHECK_DB_KEY_RESULT_CMD = "check-db-key-result-s2s-cmd";
	private static final String CONN_CID                = "connection-cid";
	private static final String FORKEY_SESSION_ID       = "forkey_sessionId";
	private static final String KEY_CID                 = "key-cid";
	private static final String KEY_P                   = "key";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(S2SConnectionClustered.class
			.getName());

	private static final String VALID = "valid";

	//~--- fields ---------------------------------------------------------------

	private ClusterControllerIfc clusterController = null;
	private List<JID>            cl_nodes_array    = new CopyOnWriteArrayList<JID>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method is called on cluster node connection event. This is a
	 * notification to the component that a new cluster node has connected.
	 *
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	@Override
	public void nodeConnected(String node) {
		cl_nodes_array.add(JID.jidInstanceNS(getName(), node, null));
	}

	/**
	 * Method is called on cluster node disconnection event. This is a
	 * notification to the component that there was network connection lost to one
	 * of the cluster nodes.
	 *
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	@Override
	public void nodeDisconnected(String node) {
		cl_nodes_array.remove(JID.jidInstanceNS(getName(), node, null));
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoDescription() {
		return super.getDiscoDescription() + " clustered";
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Set's the configures the cluster controller object for cluster
	 * communication and API.
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
		clusterController = cl_controller;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	protected JID getFirstClusterNode() {
		JID cluster_node = null;

		for (JID node : cl_nodes_array) {
			if (!node.equals(getComponentId())) {
				cluster_node = node;

				break;
			}
		}

		return cluster_node;
	}

}


//~ Formatted in Tigase Code Convention on 13/10/15
