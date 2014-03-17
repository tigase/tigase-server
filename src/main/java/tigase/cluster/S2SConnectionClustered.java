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
	@Deprecated
	private CommandListener      checkDBKeyResult = new CheckDBKeyResult(
			CHECK_DB_KEY_RESULT_CMD);
	@Deprecated
	private CommandListener checkDBKey = new CheckDBKey(CHECK_DB_KEY_CMD);

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

	/**
	 * Method description
	 *
	 *
	 * @param connectionCid
	 * @param keyCid
	 * @param key
	 * @param key_sessionId
	 * @param asking_sessionId
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	@Deprecated
	public String getLocalDBKey(CID connectionCid, CID keyCid, String key,
			String key_sessionId, String asking_sessionId) {
		String local_key = super.getLocalDBKey(connectionCid, keyCid, key, key_sessionId,
				asking_sessionId);

		if (local_key != null) {
			return local_key;
		}

		JID toNode = getFirstClusterNode();

		if (toNode != null) {
			Map<String, String> params = new LinkedHashMap<String, String>(6, 0.25f);

			params.put(CONN_CID, connectionCid.toString());
			params.put(KEY_CID, keyCid.toString());
			params.put(KEY_P, key);
			params.put(FORKEY_SESSION_ID, key_sessionId);
			params.put(ASKING_SESSION_ID, asking_sessionId);
			clusterController.sendToNodes(CHECK_DB_KEY_CMD, params, getComponentId(), toNode);

			// If null is returned then the underlying API waits for the key to be
			// delivered
			// at later time
			return null;
		} else {

			// If there is no cluster node available to ask for the db key then we
			// just return something here to generate invalid key result.
			return "invalid-key";
		}
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
		clusterController.removeCommandListener(checkDBKey);
		clusterController.removeCommandListener(checkDBKeyResult);
		clusterController.setCommandListener(checkDBKey);
		clusterController.setCommandListener(checkDBKeyResult);
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

	//~--- inner classes --------------------------------------------------------
	@Deprecated
	private class CheckDBKey
					extends CommandListenerAbstract {
		private CheckDBKey(String name) {
			super(name);
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param fromNode
		 * @param visitedNodes
		 * @param data
		 * @param packets
		 *
		 * @throws ClusterCommandException
		 */
		@Override
		public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
				String> data, Queue<Element> packets)
				throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode,
						visitedNodes, data, packets });
			}

			CID    connCid          = new CID(data.get(CONN_CID));
			CID    keyCid           = new CID(data.get(KEY_CID));
			String key              = data.get(KEY_P);
			String forkey_sessionId = data.get(FORKEY_SESSION_ID);
			String asking_sessionId = data.get(ASKING_SESSION_ID);

			if (fromNode.equals(getComponentId())) {

				// If the request came back to the first sending node then no one had a
				// valid key for this connection, therefore we are sending invalid back
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"the request came back to the first sending node then no one had a " +
							"valid key for this connection, therefore we are sending invalid back. " +
							"fromNode: {0}, compId: {1}, connCid: {2}, keyCid: {3}, " +
							"forkey_sessionId: {4}, asking_sessionId: {5}", new Object[] {
						fromNode, getComponentId(), connCid, keyCid, forkey_sessionId,
								asking_sessionId
					});
				}
				sendVerifyResult(DB_VERIFY_EL_NAME, connCid, keyCid, false, forkey_sessionId,
						asking_sessionId, null, false);

				return;
			}

			String local_key = S2SConnectionClustered.super.getLocalDBKey(connCid, keyCid, key,
					forkey_sessionId, asking_sessionId);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "LocalDBKey: {0}", local_key);
			}

			boolean valid = false;

			if (local_key == null) {

				// Forward the request to the next node
				JID nextNode = getNextNode(fromNode, visitedNodes);

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "No local db key, sending to next node: {0}", nextNode);
				}
				clusterController.sendToNodes(CHECK_DB_KEY_CMD, data, fromNode, visitedNodes,
						nextNode);

				return;
			}
			valid = local_key.equals(key);
			data.put(VALID, "" + valid);
			clusterController.sendToNodes(CHECK_DB_KEY_RESULT_CMD, data, getComponentId(),
					fromNode);
		}

		//~--- get methods --------------------------------------------------------

		private JID getNextNode(JID fromNode, Set<JID> visitedNodes) {
			JID result = fromNode;

			for (JID jid : cl_nodes_array) {
				if (!fromNode.equals(jid) &&!visitedNodes.contains(jid)) {
					result = jid;

					break;
				}
			}

			return result;
		}
	}

	@Deprecated
	private class CheckDBKeyResult
					extends CommandListenerAbstract {
		private CheckDBKeyResult(String name) {
			super(name);
		}

		//~--- methods ------------------------------------------------------------

		/**
		 *   Method description
		 *
		 *
		 *   @param fromNode
		 *   @param visitedNodes
		 *   @param data
		 *   @param packets
		 *
		 *   @throws ClusterCommandException
		 */
		@Override
		public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
				String> data, Queue<Element> packets)
				throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode,
						visitedNodes, data, packets });
			}

			CID     connCid          = new CID(data.get(CONN_CID));
			CID     keyCid           = new CID(data.get(KEY_CID));
			String  forkey_sessionId = data.get(FORKEY_SESSION_ID);
			String  asking_sessionId = data.get(ASKING_SESSION_ID);
			boolean valid            = "true".equals(data.get(VALID));

			// String key = data.get(KEY_P);
			// String from = connCid.getLocalHost();
			// String to = connCid.getRemoteHost();
			sendVerifyResult(DB_VERIFY_EL_NAME, connCid, keyCid, valid, forkey_sessionId,
					asking_sessionId, null, false);
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/10/15
