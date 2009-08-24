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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;


import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.server.Packet;
import tigase.server.xmppserver.ServerConnectionManager;
import tigase.xml.Element;

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

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.cluster.ServerConnectionClustered");

	private static final String CID = "cid";
	private static final String KEY = "key";
	private static final String FORKEY_SESSION_ID = "forkey_sessionId";
	private static final String ASKING_SESSION_ID = "asking_sessionId";
	private static final String VALID = "valid";

	//private Set<String> cluster_nodes = new LinkedHashSet<String>();
	private List<String> cl_nodes_array = new CopyOnWriteArrayList<String>();

	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Received packet: " + packet.toString());
		}
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME
			&& packet.getElement().getXMLNS() == ClusterElement.XMLNS) {
			processClusterPacket(packet);
			return;
		}
		super.processPacket(packet);
	}

	protected void processClusterPacket(Packet packet) {
		ClusterElement clel = new ClusterElement(packet.getElement());
		clel.addVisitedNode(getComponentId());
		switch (packet.getType()) {
		case set:
			if (ClusterMethods.CHECK_DB_KEY.toString().equals(clel.getMethodName())) {
				String cid = clel.getMethodParam(CID);
				String key = clel.getMethodParam(KEY);
				String forkey_sessionId = clel.getMethodParam(FORKEY_SESSION_ID);
				String asking_sessionId = clel.getMethodParam(ASKING_SESSION_ID);
				String local_key =
          super.getLocalDBKey(cid, key, forkey_sessionId, asking_sessionId);
				ClusterElement result = null;
				boolean valid = false;
				if (local_key != null) {
					valid = local_key.equals(key);
				} else {
					result = ClusterElement.createForNextNode(clel, cl_nodes_array,
						getComponentId());
				}
				if (result == null) {
					Map<String, String> res_vals = new LinkedHashMap<String, String>();

					res_vals.put(VALID, "" + valid);
					result = clel.createMethodResponse(getComponentId(),
						StanzaType.result, res_vals);
				}
				addOutPacket(new Packet(result.getClusterElement()));
			}
			break;
		case get:

			break;
		case result:
			if (ClusterMethods.CHECK_DB_KEY.toString().equals(clel.getMethodName())) {
				String cid = clel.getMethodParam(CID);
				String key = clel.getMethodParam(KEY);
				String forkey_sessionId = clel.getMethodParam(FORKEY_SESSION_ID);
				String asking_sessionId = clel.getMethodParam(ASKING_SESSION_ID);
				boolean valid = "true".equals(clel.getMethodResultVal(VALID));
				String from = JIDUtils.getNodeNick(cid);
				String to = JIDUtils.getNodeHost(cid);
				sendVerifyResult(from, to, forkey_sessionId, valid,
					getServerConnections(cid), asking_sessionId);
			}
			break;
		case error:
			// There might be many different errors...
			// But they all mean the cluster node is unreachable.
			// Let's leave custom handling each error type for later...
			String from = packet.getElemFrom();
			clel.addVisitedNode(from);
			addOutPacket(new Packet(ClusterElement.createForNextNode(clel,
						cl_nodes_array, getComponentId()).getClusterElement()));
			break;
		default:
			break;
		}
	}

	protected String getFirstClusterNode() {
		String cluster_node = null;
		for (String node: cl_nodes_array) {
			if (!node.equals(getComponentId())) {
				cluster_node = node;
				break;
			}
		}
		return cluster_node;
	}

	@Override
	protected String getLocalDBKey(String cid, String key, String forkey_sessionId,
		String asking_sessionId) {
		String local_key = super.getLocalDBKey(cid, key, forkey_sessionId,
			asking_sessionId);
		if (local_key != null) {
			return local_key;
		} else {
			String cluster_node = getFirstClusterNode();
			if (cluster_node != null) {
				Map<String, String> params = new LinkedHashMap<String, String>();
				params.put(CID, cid);
				params.put(KEY, key);
				params.put(FORKEY_SESSION_ID, forkey_sessionId);
				params.put(ASKING_SESSION_ID, asking_sessionId);
				Element check_db_key =
          ClusterElement.createClusterMethodCall(getComponentId(), cluster_node,
						StanzaType.set, ClusterMethods.CHECK_DB_KEY.toString(),
						params).getClusterElement();
				addOutPacket(new Packet(check_db_key));
			}
			return null;
		}
	}

	@Override
	public void nodeConnected(String node) {
		cl_nodes_array.add(getName() + "@" + node);
	}

	@Override
	public void nodeDisconnected(String node) {
		cl_nodes_array.remove(getName() + "@" + node);
	}

	@Override
	public void setClusterController(ClusterController cl_controller) {
	}

}
