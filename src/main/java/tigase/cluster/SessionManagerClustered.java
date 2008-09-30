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
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import tigase.cluster.methodcalls.SessionTransferMC;
import tigase.server.Packet;
import tigase.server.Command;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.ConnectionStatus;
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
	private static final String SM_ID = "smId";
	private static final String CREATION_TIME = "creationTime";
	private static final String ERROR_CODE = "errorCode";

	private static final String XMPP_SESSION_ID = "xmppSessionId";
	private static final String RESOURCE = "resource";
	private static final String CONNECTION_ID = "connectionId";
	private static final String PRIORITY = "priority";
	private static final String TOKEN = "token";
	private static final String TRANSFER = "transfer";

	private SessionTransferMC sessionTransferMC = null;

	private Timer delayedTasks = null;
	private Set<String> cluster_nodes = new LinkedHashSet<String>();
	private Set<String> broken_nodes = new LinkedHashSet<String>();
	private ArrayList<MethodCall> methods = new ArrayList<MethodCall>();


	@SuppressWarnings("unchecked")
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
		if (conn != null) {
			switch (conn.getConnectionStatus()) {
			case ON_HOLD:
				LinkedList<Packet> packets =
          (LinkedList<Packet>)conn.getSessionData(SESSION_PACKETS);
				if (packets == null) {
					packets = new LinkedList<Packet>();
					conn.putSessionData(SESSION_PACKETS, packets);
				}
				packets.offer(packet);
				log.finest("Packet put on hold: " + packet.toString());
				return;
			case REDIRECT:
				packet.setTo((String)conn.getSessionData("redirect-to"));
				fastAddOutPacket(packet);
				log.finest("Packet redirected: " + packet.toString());
				return;
			}
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
		final ClusterElement clel = new ClusterElement(packet.getElement());
		clel.addVisitedNode(getComponentId());
		switch (packet.getType()) {
		case set:
			if (clel.getMethodName() == null) {
				processPacket(clel);
			}
			if (ClusterMethods.SESSION_TRANSFER.toString().equals(clel.getMethodName())) {
				String user_id = null;
				try {
					final String userId = clel.getMethodParam(USER_ID);
					// Only to make it available for the exception if thrown...
					user_id = userId;
					final String xmpp_sessionId = clel.getMethodParam(XMPP_SESSION_ID);
					//String defLand = conn.getLang();
					String resource = clel.getMethodParam(RESOURCE);
					final String connectionId = clel.getMethodParam(CONNECTION_ID);
					int priority = 5;
					try {
						priority = Integer.parseInt(clel.getMethodParam(PRIORITY));
					} catch (Exception e) {
						priority = 5;
					}
					final String token = clel.getMethodParam(TOKEN);
					String domain = JIDUtils.getNodeHost(userId);
					String nick = JIDUtils.getNodeNick(userId);
					final XMPPResourceConnection res_con =
            createUserSession(connectionId, domain,
							JIDUtils.getJID(nick, domain, resource));
					res_con.setSessionId(xmpp_sessionId);
					Authorization auth_res = res_con.loginToken(xmpp_sessionId, token);
					if (auth_res == Authorization.AUTHORIZED) {
						log.finest("SESSION_TRANSFER received SET request, userId: " + userId
							+ ", resource: " + resource
							+ ", xmpp_sessionId: " + xmpp_sessionId
							+ ", connectionId: " + connectionId + ", auth_res: " + auth_res);
						finishSessionTransfer(connectionId, xmpp_sessionId, clel);
					} else {
						log.finest("SESSION_TRANSFER authorization failed: " + auth_res
							+ ", userId: " + userId + ", MySQL cluster delay? Waiting 2 secs");
						delayedTasks.schedule(new TimerTask() {
								public void run() {
									try {
										Authorization auth =
                      res_con.loginToken(xmpp_sessionId, token);
										if (auth == Authorization.AUTHORIZED) {
											finishSessionTransfer(connectionId, xmpp_sessionId, clel);
										} else {
											log.finest("Authorization failed after delay: "
												+ auth + ", userId: " + userId
												+ ", closing the session and connection.");
											closeConnection(connectionId, true);
											Packet close = Command.CLOSE.getPacket(getComponentId(),
												connectionId, StanzaType.set, "1");
											fastAddOutPacket(close);
										}
									} catch (Exception e) {
										log.log(Level.WARNING,
											"Exception during user session transfer preparation", e);
									}
								}
							}, 2000);
					}
				} catch (Exception e) {
					log.log(Level.WARNING,
						"Exception during user session transfer: " + user_id, e);
				}
			}
			break;
		case get:
			if (ClusterMethods.CHECK_USER_SESSION.toString().equals(clel.getMethodName())) {
				ClusterElement result = null;
				String userId = clel.getMethodParam(USER_ID);
				XMPPSession session = getSession(userId);
				log.finest("CHECK_USER_SESSION received GET request for user: " + userId
					+ " from " + clel.getFirstNode());
				if (session == null) {
					log.finest("Session not local, forwarding CHECK_USER_SESSION "
						+ "to next node.");
					result = ClusterElement.createForNextNode(clel, cluster_nodes,
						getComponentId());
					if (result != null
						&& result.getClusterElement().getAttribute("to").equals(result.getFirstNode())) {
						log.finest("No more nodes for checking user sessiom, we don't want to send this packet back...");
						result = null;
					}
				} else {
					log.finest("Session found, sending back user session data...");
					Map<String, String> res_vals = new LinkedHashMap<String, String>();
					res_vals.put(SM_ID, getComponentId());
					res_vals.put(CREATION_TIME, ""+session.getLiveTime());
					result = clel.createMethodResponse(getComponentId(),
						StanzaType.result.toString(), res_vals);
				}
				if (result != null) {
					fastAddOutPacket(new Packet(result.getClusterElement()));
				}
			}
			if (ClusterMethods.SESSION_TRANSFER.toString().equals(clel.getMethodName())) {
				transferUserSession(clel.getMethodParam(USER_ID),
					clel.getMethodParam(SM_ID), clel);
			}
			break;
		case result:
			if (ClusterMethods.CHECK_USER_SESSION.toString().equals(clel.getMethodName())) {
				String userId = clel.getMethodParam(USER_ID);
				XMPPSession session = getSession(userId);
				if (session == null) {
					// Ups the session is gone by now, no need for further processing then
					return;
				}
				String remote_smId = clel.getMethodResultVal(SM_ID);
				long remote_creationTime = 0;
				try {
					remote_creationTime =
            Long.parseLong(clel.getMethodResultVal(CREATION_TIME));
				} catch (Exception e) {
					remote_creationTime = 0;
				}
				int remote_hashcode = (userId+remote_smId).hashCode();
				int local_hashcode = (userId+getComponentId()).hashCode();
				boolean transfer_out = false;
				if (remote_creationTime > session.getLiveTime()) {
					transfer_out = true;
				}
				if (remote_creationTime == session.getLiveTime()) {
					if (remote_hashcode > local_hashcode) {
						transfer_out = true;
					}
				}
				log.finest("CHECK_USER_SESSION received RESULT response for user: "
					+ userId + " from " + remote_smId
					+ ", remote_creationTime: " + remote_creationTime
					+ ", local_creationTime: " + session.getLiveTime()
					+ ", transfer_out: " + transfer_out);
				if (transfer_out) {
					transferUserSession(userId, remote_smId, clel);
				} else {
					log.finest("Requesting user: " + userId
						+ " session transfer from " + remote_smId);
					Map<String, String> params = new LinkedHashMap<String, String>();
					params.put(USER_ID, userId);
					params.put(SM_ID, getComponentId());
					Element sess_trans = ClusterElement.createClusterMethodCall(
						getComponentId(), remote_smId, "get",
						ClusterMethods.SESSION_TRANSFER.toString(), params).getClusterElement();
					fastAddOutPacket(new Packet(sess_trans));
				}
			}
			if (ClusterMethods.SESSION_TRANSFER.toString().equals(clel.getMethodName())) {
				String connectionId = clel.getMethodParam(CONNECTION_ID);
				XMPPResourceConnection conn = getXMPPResourceConnection(connectionId);
				if (conn != null) {
					sendAllOnHold(conn);
				}
				//closeConnection(connectionId, true);
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

	protected void finishSessionTransfer(String connectionId, String xmpp_sessionId,
		ClusterElement clel) {
		Packet redirect = Command.REDIRECT.getPacket(getComponentId(),
			connectionId, StanzaType.set, "1", "submit");
		Command.addFieldValue(redirect, "session-id", xmpp_sessionId);
		fastAddOutPacket(redirect);

		Map<String, String> res_vals = new LinkedHashMap<String, String>();
		res_vals.put(TRANSFER, "success");
		ClusterElement result = clel.createMethodResponse(getComponentId(),
			"result", res_vals);
		fastAddOutPacket(new Packet(result.getClusterElement()));
	}

	protected void transferUserSession(final String userId, final String remote_smId,
		ClusterElement clel) {
		XMPPSession session = getSession(userId);
		if (session != null) {
			log.finest("TRANSFERIN user: " + userId + " sessions to " + remote_smId);
			List<XMPPResourceConnection> conns = session.getActiveResources();
			for (XMPPResourceConnection conn_res: conns) {
				conn_res.setConnectionStatus(ConnectionStatus.ON_HOLD);
				conn_res.putSessionData("redirect-to", remote_smId);
				final XMPPResourceConnection conn = conn_res;
				// Delay is needed here as there might be packets which are being processing
				// while we are preparing session for transfer.
				// Putting it on hold makes all new incoming packets to be queued but
				// packets received just before session transfer are still in plugin
				// queues
				delayedTasks.schedule(new TimerTask() {
						public void run() {
							try {
								String xmpp_sessionId = conn.getSessionId();
								//String defLand = conn.getLang();
								String resource = conn.getResource();
								String connectionId = conn.getConnectionId();
								int priority = conn.getPriority();
								String token = conn.getAuthenticationToken(xmpp_sessionId);
								log.finest("Sending user: " + userId
									+ " session, resource: " + resource
									+ ", xmpp_sessionId: " + xmpp_sessionId
									+ ", connectionId: " + connectionId);
								Map<String, String> params = new LinkedHashMap<String, String>();
								params.put(USER_ID, userId);
								params.put(XMPP_SESSION_ID, xmpp_sessionId);
								params.put(RESOURCE, resource);
								params.put(CONNECTION_ID, connectionId);
								params.put(PRIORITY, "" + priority);
								params.put(TOKEN, token);
								Element sess_trans = ClusterElement.createClusterMethodCall(
									getComponentId(), remote_smId, "set",
									ClusterMethods.SESSION_TRANSFER.toString(), params).getClusterElement();
								fastAddOutPacket(new Packet(sess_trans));
							} catch (Exception e) {
								log.log(Level.WARNING,
									"Exception during user session transfer preparation", e);
							}
						}
					}, 500);
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
		init();
	}

	private void init() {
		this.sessionTransferMC = new SessionTransferMC();
	}

	private <T extends MethodCall> T registerMethodCall(final T methodCall) {
		log.config("Registering method call: "
			+ methodCall.getClass().getCanonicalName());
		this.methods.add(methodCall);
		return methodCall;
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
			cluster_nodes.add(getName() + "@" + node);
			broken_nodes.remove(getName() + "@" + node);
		}
		sendClusterNotification("Cluster nodes have been connected:",
			"New cluster nodes connected", node_hostnames);
	}

	public void nodesDisconnected(Set<String> node_hostnames) {
		for (String node: node_hostnames) {
			cluster_nodes.remove(getName() + "@" + node);
			broken_nodes.add(getName() + "@" + node);
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
				log.finest("CHECK_USER_SESSION on other cluster nodes for: "
					+ JIDUtils.getNodeID(userName, conn.getDomain()));
				Map<String, String> params = new LinkedHashMap<String, String>();
				params.put(USER_ID, JIDUtils.getNodeID(userName, conn.getDomain()));
				Element check_session_el = ClusterElement.createClusterMethodCall(
					getComponentId(), cluster_node, StanzaType.get.toString(),
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
		Packet p_msg = Packet.getMessage("",
			JIDUtils.getJID(getName(), getVHosts()[0], null), StanzaType.normal,
			message, subject, "xyz");
		sendToAdmins(p_msg);
	}

	public void release() {
		delayedTasks.cancel();
		super.release();
	}

	public void start() {
		super.start();
		delayedTasks = new Timer("SM Cluster Delayed Tasks", true);
	}

}
