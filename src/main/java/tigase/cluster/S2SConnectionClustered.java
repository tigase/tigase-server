/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusterElement;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.cluster.api.CommandListener;
import tigase.cluster.strategy.ConnectionRecord;
import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.S2SConnectionManager;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPSession;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 27, 2010 10:33:51 AM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SConnectionClustered extends S2SConnectionManager implements
		ClusteredComponentIfc {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(S2SConnectionClustered.class
			.getName());

	private static final String CHECK_DB_KEY_CMD = "check-db-key-s2s-cmd";
	private static final String CHECK_DB_KEY_RESULT_CMD = "check-db-key-s2s-cmd";

	private static final String CONN_CID = "connection-cid";
	private static final String KEY_CID = "key-cid";
	private static final String KEY_P = "key";
	private static final String FORKEY_SESSION_ID = "forkey_sessionId";
	private static final String ASKING_SESSION_ID = "asking_sessionId";
	private static final String VALID = "valid";

	private ClusterControllerIfc clusterController = null;
	private CommandListener checkDBKey = new CheckDBKey();
	private CommandListener checkDBKeyResult = new CheckDBKeyResult();

	// ~--- fields ---------------------------------------------------------------

	private List<JID> cl_nodes_array = new CopyOnWriteArrayList<JID>();

	// ~--- get methods ----------------------------------------------------------

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
	 * @return
	 */
	@Override
	public String getLocalDBKey(CID connectionCid, CID keyCid, String key,
			String key_sessionId, String asking_sessionId) {
		String local_key =
				super.getLocalDBKey(connectionCid, keyCid, key, key_sessionId, asking_sessionId);

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
			// If null is returned then the underlying API waits for the key to be delivered
			// at later time
			return null;
		} else {
			// If there is no cluster node available to ask for the db key then we 
			// just return something here to generate invalid key result.
			return "invalid-key";
		}

	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param node
	 */
	@Override
	public void nodeConnected(String node) {
		cl_nodes_array.add(JID.jidInstanceNS(getName(), node, null));
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param node
	 */
	@Override
	public void nodeDisconnected(String node) {
		cl_nodes_array.remove(JID.jidInstanceNS(getName(), node, null));
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
		clusterController = cl_controller;
		clusterController.removeCommandListener(CHECK_DB_KEY_CMD, checkDBKey);
		clusterController.removeCommandListener(CHECK_DB_KEY_RESULT_CMD, checkDBKeyResult);

		clusterController.setCommandListener(CHECK_DB_KEY_CMD, checkDBKey);
		clusterController.setCommandListener(CHECK_DB_KEY_RESULT_CMD, checkDBKeyResult);
	}

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

	private class CheckDBKey implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			CID connCid = new CID(data.get(CONN_CID));
			CID keyCid = new CID(data.get(KEY_CID));
			String key = data.get(KEY_P);
			String forkey_sessionId = data.get(FORKEY_SESSION_ID);
			String asking_sessionId = data.get(ASKING_SESSION_ID);
			if (fromNode.equals(getComponentId())) {
				// If the request came back to the first sending node then no one had a
				// valid
				// key for this connection, therefore we are sending invalid back
				sendVerifyResult(DB_VERIFY_EL_NAME, connCid, keyCid, false, forkey_sessionId,
						asking_sessionId, null, false);
				return;
			}

			String local_key =
					S2SConnectionClustered.super.getLocalDBKey(connCid, keyCid, key,
							forkey_sessionId, asking_sessionId);
			boolean valid = false;

			if (local_key == null) {
				// Forward the request to the next node
				JID nextNode = getNextNode(fromNode, visitedNodes);
				clusterController.sendToNodes(CHECK_DB_KEY_CMD, data, fromNode, visitedNodes,
						nextNode);
				return;
			}

			valid = local_key.equals(key);
			data.put(VALID, "" + valid);
			clusterController.sendToNodes(CHECK_DB_KEY_RESULT_CMD, data, getComponentId(),
					fromNode);
		}

		private JID getNextNode(JID fromNode, List<JID> visitedNodes) {
			JID result = fromNode;
			for (JID jid : cl_nodes_array) {
				if (!fromNode.equals(jid) && !visitedNodes.contains(jid)) {
					result = jid;
					break;
				}
			}
			return result;
		}

	}

	private class CheckDBKeyResult implements CommandListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
		 */
		@Override
		public void executeCommand(JID fromNode, List<JID> visitedNodes,
				Map<String, String> data, Queue<Element> packets) throws ClusterCommandException {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
						new Object[] { fromNode, visitedNodes, data, packets });
			}
			CID connCid = new CID(data.get(CONN_CID));
			CID keyCid = new CID(data.get(KEY_CID));
			String forkey_sessionId = data.get(FORKEY_SESSION_ID);
			String asking_sessionId = data.get(ASKING_SESSION_ID);
			boolean valid = "true".equals(data.get(VALID));
			// String key = data.get(KEY_P);
			// String from = connCid.getLocalHost();
			// String to = connCid.getRemoteHost();

			sendVerifyResult(DB_VERIFY_EL_NAME, connCid, keyCid, valid, forkey_sessionId,
					asking_sessionId, null, false);

		}

	}

}
