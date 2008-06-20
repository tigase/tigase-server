/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;

//import tigase.cluster.ClusterElement;
import tigase.server.Packet;
import tigase.server.Command;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;

import static tigase.server.xmppsession.SessionManagerConfig.*;

/**
 * Class SessionManagerClustered
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManagerClustered extends SessionManager
	implements ClusteredComponent {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppsession.SessionManagerClustered");

	private static final String USER_ID = "userId";
	private static final String SESSION_ID = "sessionId";
	private static final String CREATION_TIME = "creationTime";
	private static final String ERROR_CODE = "errorCode";

	private static final String XMPP_SESSION_ID = "xmppSessionId";
	private static final String RESOURCE = "resource";
	private static final String CONNECTION_ID = "connectionId";
	private static final String PRIORITY = "priority";
	private static final String TOKEN = "token";
	private static final String TRANSFER = "transfer";

	private Set<String> cluster_nodes = new LinkedHashSet<String>();
	private Set<String> broken_nodes = new LinkedHashSet<String>();

	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Received packet: " + packet.toString());
		}
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME
			&& packet.getElement().getXMLNS() == ClusterElement.XMLNS) {
			processClusterPacket(packet);
			return;
		}

		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");
			// No more processing is needed for command packet
			return;
		} // end of if (pc.isCommand())
		XMPPResourceConnection conn = getXMPPResourceConnection(packet);
		if (conn == null
			&& (isBrokenPacket(packet) || processAdminsOrDomains(packet)
				|| sentToNextNode(packet))) {
			return;
		}
		processPacket(packet, conn);
	}

	protected void processPacket(ClusterElement packet) {
		List<Element> elems = packet.getDataPackets();
		//String packet_from = packet.getDataPacketFrom();
		if (elems != null && elems.size() > 0) {
			for (Element elem: elems) {
					Packet el_packet = new Packet(elem);
					//el_packet.setFrom(packet_from);
					XMPPResourceConnection conn = getXMPPResourceConnection(el_packet);
					if (conn != null || !sentToNextNode(packet)) {
						processPacket(el_packet, conn);
					}
			}
		} else {
			log.finest("Empty packets list in the cluster packet: "
				+ packet.toString());
		}
	}

	protected void processClusterPacket(Packet packet) {
		ClusterElement clel = new ClusterElement(packet.getElement());
		clel.addVisitedNode(getComponentId());
		switch (packet.getType()) {
		case set:
			if (clel.getMethodName() == null) {
				processPacket(clel);
			}
			if (ClusterMethods.SESSION_TRANSFER.toString().equals(clel.getMethodName())) {
				String userId = null;
				try {
					userId = clel.getMethodParam(USER_ID);
					String xmpp_sessionId = clel.getMethodParam(XMPP_SESSION_ID);
					//String defLand = conn.getLang();
					String resource = clel.getMethodParam(RESOURCE);
					String connectionId = clel.getMethodParam(CONNECTION_ID);
					int priority = 5;
					try {
						priority = Integer.parseInt(clel.getMethodParam(PRIORITY));
					} catch (Exception e) {
						priority = 5;
					}
					String token = clel.getMethodParam(TOKEN);
					String domain = JIDUtils.getNodeHost(userId);
					String nick = JIDUtils.getNodeNick(userId);
					XMPPResourceConnection res_con = createUserSession(connectionId, domain,
						JIDUtils.getJID(nick, domain, resource));
					res_con.setSessionId(xmpp_sessionId);
					res_con.loginToken(xmpp_sessionId, token);

					Packet redirect = Command.REDIRECT.getPacket(getComponentId(),
						connectionId, StanzaType.set, "1", "submit");
					Command.addFieldValue(redirect, "session-id", xmpp_sessionId);
					fastAddOutPacket(redirect);

					Map<String, String> res_vals = new LinkedHashMap<String, String>();
					res_vals.put(TRANSFER, "success");
					ClusterElement result = clel.createMethodResponse(getComponentId(),
						"result", res_vals);
					fastAddOutPacket(new Packet(result.getClusterElement()));
				} catch (Exception e) {
					log.log(Level.WARNING,
						"Exception during user session transfer: " + userId, e);
				}
			}
			break;
		case get:
			if (ClusterMethods.CHECK_USER_SESSION.toString().equals(clel.getMethodName())) {
				ClusterElement result = null;
				String userId = clel.getMethodParam(USER_ID);
				XMPPSession session = getSession(userId);
				if (session == null) {
					result = ClusterElement.createForNextNode(clel, cluster_nodes,
						getComponentId());
				} else {
					Map<String, String> res_vals = new LinkedHashMap<String, String>();
					res_vals.put(SESSION_ID, getComponentId());
					res_vals.put(CREATION_TIME, ""+session.getCreationTime());
					result = clel.createMethodResponse(getComponentId(),
						"result", res_vals);
				}
				if (result != null) {
					fastAddOutPacket(new Packet(result.getClusterElement()));
				}
			}
			if (ClusterMethods.SESSION_TRANSFER.toString().equals(clel.getMethodName())) {
				transferUserSession(clel.getMethodParam(USER_ID),
					clel.getMethodParam(SESSION_ID), clel);
			}
			break;
		case result:
			if (ClusterMethods.CHECK_USER_SESSION.toString().equals(clel.getMethodName())) {
				String userId = clel.getMethodParam(USER_ID);
				String remote_sessionId = clel.getMethodResultVal(SESSION_ID);
				long remote_creationTime = 0;
				try {
					remote_creationTime =
            Long.parseLong(clel.getMethodResultVal(CREATION_TIME));
				} catch (Exception e) {
					remote_creationTime = 0;
				}
				int remote_hashcode = (userId+remote_sessionId).hashCode();
				int local_hashcode = (userId+getComponentId()).hashCode();
				XMPPSession session = getSession(userId);
				boolean transfer_out = false;
				if (remote_creationTime > session.getCreationTime()) {
					transfer_out = true;
				}
				if (remote_creationTime == session.getCreationTime()) {
					if (remote_hashcode > local_hashcode) {
						transfer_out = true;
					}
				}
				if (transfer_out) {
					transferUserSession(userId, remote_sessionId, clel);
				} else {
					Map<String, String> params = new LinkedHashMap<String, String>();
					params.put(USER_ID, userId);
					params.put(SESSION_ID, getComponentId());
					Element sess_trans = ClusterElement.createClusterMethodCall(
						getComponentId(), remote_sessionId, "get",
						ClusterMethods.SESSION_TRANSFER.toString(), params).getClusterElement();
					fastAddOutPacket(new Packet(sess_trans));
				}
			}
			if (ClusterMethods.SESSION_TRANSFER.toString().equals(clel.getMethodName())) {
				String connectionId = clel.getMethodParam(CONNECTION_ID);
				closeConnection(connectionId);
			}
			break;
		case error:
			// There might be many different errors...
			// But they all mean the cluster node is unreachable.
			// Let's leave custom handling each error type for later...
			String from = packet.getElemFrom();
			clel.addVisitedNode(from);
			if (cluster_nodes.remove(from)) {
				broken_nodes.add(from);
			}
			processPacket(clel);
			break;
		default:
			break;
		}
	}

	protected void transferUserSession(String userId, String remote_sessionId,
		ClusterElement clel) {
		XMPPSession session = getSession(userId);
		if (session != null) {
			List<XMPPResourceConnection> conns = session.getActiveResources();
			for (XMPPResourceConnection conn: conns) {
				try {
					String xmpp_sessionId = conn.getSessionId();
					//String defLand = conn.getLang();
					String resource = conn.getResource();
					String connectionId = conn.getConnectionId();
					int priority = conn.getPriority();
					String token = conn.getAuthenticationToken(xmpp_sessionId);
					Map<String, String> params = new LinkedHashMap<String, String>();
					params.put(USER_ID, userId);
					params.put(XMPP_SESSION_ID, xmpp_sessionId);
					params.put(RESOURCE, resource);
					params.put(CONNECTION_ID, connectionId);
					params.put(PRIORITY, "" + priority);
					params.put(TOKEN, token);
					Element sess_trans = ClusterElement.createClusterMethodCall(
						getComponentId(), remote_sessionId, "set",
						ClusterMethods.SESSION_TRANSFER.toString(), params).getClusterElement();
					fastAddOutPacket(new Packet(sess_trans));
				} catch (Exception e) {
					log.log(Level.WARNING,
						"Exception during user session transfer preparation", e);
				}
			}
		} else {
			Map<String, String> res_vals = new LinkedHashMap<String, String>();
			res_vals.put(ERROR_CODE, Authorization.ITEM_NOT_FOUND.toString());
			ClusterElement error_clel = clel.createMethodResponse(getComponentId(),
						"error", res_vals);
			fastAddOutPacket(new Packet(error_clel.getClusterElement()));
		}
	}

	protected boolean sentToNextNode(ClusterElement clel) {
		ClusterElement next_clel = ClusterElement.createForNextNode(clel,
			cluster_nodes, getComponentId());
		if (next_clel != null) {
			fastAddOutPacket(new Packet(next_clel.getClusterElement()));
			return true;
		} else {
			return false;
		}
	}

	protected boolean sentToNextNode(Packet packet) {
		if (cluster_nodes.size() > 0) {
			String sess_man_id = getComponentId();
			String cluster_node = getFirstClusterNode();
			if (cluster_node != null) {
				ClusterElement clel = new ClusterElement(sess_man_id, cluster_node,
					StanzaType.set, packet);
				clel.addVisitedNode(sess_man_id);
				fastAddOutPacket(new Packet(clel.getClusterElement()));
				return true;
			}
		}
		return false;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
// 		String[] cl_nodes = (String[])props.get(CLUSTER_NODES_PROP_KEY);
// 		log.config("Cluster nodes loaded: " + Arrays.toString(cl_nodes));
// 		cluster_nodes = new LinkedHashSet<String>(Arrays.asList(cl_nodes));
// 		broken_nodes = new LinkedHashSet<String>();
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
// 		if (params.get(CLUSTER_NODES) != null) {
// 			String[] cl_nodes = ((String)params.get(CLUSTER_NODES)).split(",");
// 			for (int i = 0; i < cl_nodes.length; i++) {
// 				if (cl_nodes[i].equals(JIDUtils.getNodeHost(cl_nodes[i]))) {
// 					cl_nodes[i] = DEF_SM_NAME + "@" + cl_nodes[i];
// 				}
// 			}
// 			props.put(CLUSTER_NODES_PROP_KEY, cl_nodes);
// 		} else {
// 			props.put(CLUSTER_NODES_PROP_KEY, new String[] {getComponentId()});
// 		}
		return props;
	}

	public void nodesConnected(Set<String> node_hostnames) {
		for (String node: node_hostnames) {
			cluster_nodes.add(DEF_SM_NAME + "@" + node);
			broken_nodes.remove(DEF_SM_NAME + "@" + node);
		}
		sendClusterNotification("Cluster nodes have been connected:",
			"New cluster nodes connected", node_hostnames);
	}

	public void nodesDisconnected(Set<String> node_hostnames) {
		for (String node: node_hostnames) {
			cluster_nodes.remove(DEF_SM_NAME + "@" + node);
			broken_nodes.add(DEF_SM_NAME + "@" + node);
		}
		sendClusterNotification("Cluster nodes have been disconnected:",
			"Disconnected cluster nodes", node_hostnames);
	}

	protected String getFirstClusterNode() {
		String cluster_node = null;
		for (String node: cluster_nodes) {
			if (!node.equals(getComponentId())) {
				cluster_node = node;
				break;
			}
		}
		return cluster_node;
	}

	public void handleLogin(String userName, XMPPResourceConnection conn) {
		super.handleLogin(userName, conn);
		if (!conn.isAnonymous()) {
			String cluster_node = getFirstClusterNode();
			if (cluster_node != null) {
				Map<String, String> params = new LinkedHashMap<String, String>();
				params.put(USER_ID, JIDUtils.getNodeID(userName, conn.getDomain()));
				Element check_session_el = ClusterElement.createClusterMethodCall(
					getComponentId(), cluster_node, "get",
					ClusterMethods.CHECK_USER_SESSION.toString(), params).getClusterElement();
				fastAddOutPacket(new Packet(check_session_el));
			}
		}
	}

	private void sendClusterNotification(String msg, String subject,
		Set<String> nodes) {
		String message = msg;
		if (nodes != null) {
			message = msg + "\n";
		}
		int cnt = 0;
		for (String node: nodes) {
			message += "" + (++cnt) + ". " + node;
		}
		Packet p_msg = Packet.getMessage("", getComponentId(), StanzaType.normal,
			message, subject, "xyz");
		sendToAdmins(p_msg);
	}

}
