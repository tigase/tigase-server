
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

import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.S2SConnectionManager;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class S2SConnectionClustered extends S2SConnectionManager implements ClusteredComponent {
	private static final Logger log = Logger.getLogger(S2SConnectionClustered.class.getName());
	private static final String CONN_CID = "connection-cid";
	private static final String KEY_CID = "key-cid";
	private static final String KEY_P = "key";
	private static final String FORKEY_SESSION_ID = "forkey_sessionId";
	private static final String ASKING_SESSION_ID = "asking_sessionId";
	private static final String VALID = "valid";

	//~--- fields ---------------------------------------------------------------

	private List<JID> cl_nodes_array = new CopyOnWriteArrayList<JID>();

	//~--- methods --------------------------------------------------------------

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
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Received packet: {0}", packet);
		}

		if ((packet.getElemName() == ClusterElement.CLUSTER_EL_NAME)
				&& (packet.getElement().getXMLNS() == ClusterElement.XMLNS)) {
			processClusterPacket(packet);

			return;
		}

		super.processPacket(packet);
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterController cl_controller) {}

	//~--- get methods ----------------------------------------------------------

	protected JID getFirstClusterNode() {
		JID cluster_node = null;

		for (JID node : cl_nodes_array) {
			if ( !node.equals(getComponentId())) {
				cluster_node = node;

				break;
			}
		}

		return cluster_node;
	}

	@Override
	protected String getLocalDBKey(CID connectionCid, CID keyCid, String key, String key_sessionId,
			String asking_sessionId) {
		String local_key = super.getLocalDBKey(connectionCid, keyCid, key, key_sessionId,
			asking_sessionId);

		if (local_key != null) {
			return local_key;
		}

		JID cluster_node = getFirstClusterNode();

		if (cluster_node != null) {
			Map<String, String> params = new LinkedHashMap<String, String>(6, 0.25f);

			params.put(CONN_CID, connectionCid.toString());
			params.put(KEY_CID, keyCid.toString());
			params.put(KEY_P, key);
			params.put(FORKEY_SESSION_ID, key_sessionId);
			params.put(ASKING_SESSION_ID, asking_sessionId);

			Element result = ClusterElement.createClusterMethodCall(getComponentId().toString(),
				cluster_node.toString(), StanzaType.set, ClusterMethods.CHECK_DB_KEY.toString(),
				params).getClusterElement();

			addOutPacket(Packet.packetInstance(result, getComponentId(), cluster_node));
		}

		return null;
	}

	//~--- methods --------------------------------------------------------------

	protected void processClusterPacket(Packet packet) {
		ClusterElement clel = new ClusterElement(packet.getElement());

		clel.addVisitedNode(getComponentId().toString());

		switch (packet.getType()) {
			case set :
				if (ClusterMethods.CHECK_DB_KEY.toString().equals(clel.getMethodName())) {
					String connCid = clel.getMethodParam(CONN_CID);
					String keyCid = clel.getMethodParam(KEY_CID);
					String key = clel.getMethodParam(KEY_P);
					String forkey_sessionId = clel.getMethodParam(FORKEY_SESSION_ID);
					String asking_sessionId = clel.getMethodParam(ASKING_SESSION_ID);
					String local_key = super.getLocalDBKey(new CID(connCid), new CID(keyCid), key,
						forkey_sessionId, asking_sessionId);
					ClusterElement result = null;
					boolean valid = false;

					if (local_key != null) {
						valid = local_key.equals(key);
					} else {
						result = ClusterElement.createForNextNode(clel, cl_nodes_array, getComponentId());
					}

					if (result == null) {
						Map<String, String> res_vals = new LinkedHashMap<String, String>(2, 0.25f);

						res_vals.put(VALID, "" + valid);
						result = clel.createMethodResponse(getComponentId().toString(), StanzaType.result,
								res_vals);
					}

					try {
						addOutPacket(Packet.packetInstance(result.getClusterElement()));
					} catch (TigaseStringprepException ex) {
						log.log(Level.WARNING, "Cluster packet addressing problem, stringprep failed for: {0}",
								result.getClusterElement());
					}
				}

				break;

			case get :
				break;

			case result :
				if (ClusterMethods.CHECK_DB_KEY.toString().equals(clel.getMethodName())) {
					CID connCid = new CID(clel.getMethodParam(CONN_CID));
					String key = clel.getMethodParam(KEY_P);
					String forkey_sessionId = clel.getMethodParam(FORKEY_SESSION_ID);
					String asking_sessionId = clel.getMethodParam(ASKING_SESSION_ID);
					boolean valid = "true".equals(clel.getMethodResultVal(VALID));
					String from = connCid.getLocalHost();
					String to = connCid.getRemoteHost();

					sendVerifyResult(DB_VERIFY_EL_NAME, connCid, valid, forkey_sessionId, asking_sessionId);
				}

				break;

			case error :

				// There might be many different errors...
				// But they all mean the cluster node is unreachable.
				// Let's leave custom handling each error type for later...
				JID from = packet.getStanzaFrom();

				clel.addVisitedNode(from.toString());

				Element result = ClusterElement.createForNextNode(clel, cl_nodes_array,
					getComponentId()).getClusterElement();

				try {
					addOutPacket(Packet.packetInstance(result));
				} catch (TigaseStringprepException ex) {
					log.log(Level.WARNING, "Cluster packet addressing problem, stringprep failed for: {0}",
							result);
				}

				break;

			default :
				break;
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
