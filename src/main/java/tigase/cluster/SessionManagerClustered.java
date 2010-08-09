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

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

import tigase.server.Message;

//import tigase.cluster.methodcalls.SessionTransferMC;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;

import tigase.stats.StatisticsList;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.ConnectionStatus;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.impl.Presence;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Class SessionManagerClusteredOld
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManagerClustered extends SessionManager implements ClusteredComponent {
	private static final String AUTH_TIME = "auth-time";
	private static final String CL_BR_INITIAL_PRESENCE = "cl-br-init-pres";
	private static final String CL_BR_USER_CONNECTED = "cl-br-user_conn";
	private static final String CONNECTION_ID = "connectionId";
	private static final String CREATION_TIME = "creationTime";
	private static final String ERROR_CODE = "errorCode";

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";
	private static final String PRIORITY = "priority";
	private static final String RESOURCE = "resource";
	private static final String SM_ID = "smId";

	/** Field description */
	public static final String STRATEGY_CLASS_PROPERTY = "--sm-cluster-strategy-class";

	/** Field description */
	public static final String STRATEGY_CLASS_PROP_KEY = "cluster-strategy-class";

	/** Field description */
	public static final String STRATEGY_CLASS_PROP_VAL =
		"tigase.cluster.strategy.SMNonCachingAllNodes";

	/** Field description */
	public static final int SYNC_MAX_BATCH_SIZE = 1000;
	private static final String SYNC_ONLINE_JIDS = "sync-jids";
	private static final String TOKEN = "token";
	private static final String TRANSFER = "transfer";
	private static final String USER_ID = "userId";
	private static final String XMPP_SESSION_ID = "xmppSessionId";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(SessionManagerClustered.class.getName());

	//~--- fields ---------------------------------------------------------------

	private JID my_address = null;
	private JID my_hostname = null;
	private int nodesNo = 0;
	private ClusteringStrategyIfc strategy = null;

	//~--- methods --------------------------------------------------------------

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
		return super.containsJid(jid) || strategy.containsJid(jid);
	}

	//~--- get methods ----------------------------------------------------------

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
		JID[] ids = super.getConnectionIdsForJid(jid);

		if (ids == null) {
			ids = strategy.getConnectionIdsForJid(jid);
		}

		return ids;
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
		Map<String, Object> props = super.getDefaults(params);
		String strategy_class = (String) params.get(STRATEGY_CLASS_PROPERTY);

		if (strategy_class == null) {
			strategy_class = STRATEGY_CLASS_PROP_VAL;
		}

		props.put(STRATEGY_CLASS_PROP_KEY, strategy_class);

		try {
			ClusteringStrategyIfc strat_tmp =
				(ClusteringStrategyIfc) Class.forName(strategy_class).newInstance();
			Map<String, Object> strat_defs = strat_tmp.getDefaults(params);

			if (strat_defs != null) {
				props.putAll(strat_defs);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE,
					"Can not instantiate clustering strategy for class: " + strategy_class, e);
		}

		String[] local_domains = DNSResolver.getDefHostNames();

		if (params.get(GEN_VIRT_HOSTS) != null) {
			local_domains = ((String) params.get(GEN_VIRT_HOSTS)).split(",");
		}

//  defs.put(LOCAL_DOMAINS_PROP_KEY, LOCAL_DOMAINS_PROP_VAL);
		props.put(MY_DOMAIN_NAME_PROP_KEY, local_domains[0]);

		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String) params.get(CLUSTER_NODES)).split(",");

			nodesNo = cl_nodes.length;
		}

		return props;
	}

	/**
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		strategy.getStatistics(list);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param conn
	 */
	@Override
	public void handlePresenceSet(XMPPResourceConnection conn) {
		super.handlePresenceSet(conn);

		if (conn.getConnectionStatus() == ConnectionStatus.REMOTE) {
			return;
		}

		if (conn.getSessionData(CL_BR_INITIAL_PRESENCE) == null) {
			conn.putSessionData(CL_BR_INITIAL_PRESENCE, CL_BR_INITIAL_PRESENCE);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Handle presence set for" + " Connection: " + conn);
			}

			List<JID> cl_nodes = strategy.getAllNodes();

			broadcastUserPresence(conn, cl_nodes);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn
	 */
	@Override
	public void handleResourceBind(XMPPResourceConnection conn) {
		super.handleResourceBind(conn);

		if (conn.getConnectionStatus() == ConnectionStatus.REMOTE) {
			return;
		}

		if (conn.getSessionData(CL_BR_USER_CONNECTED) == null) {
			conn.putSessionData(CL_BR_USER_CONNECTED, CL_BR_USER_CONNECTED);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Handle resource bind for" + " Connection:: " + conn);
			}

			List<JID> cl_nodes = strategy.getAllNodes();

			try {
				Map<String, String> params = prepareBroadcastParams(conn, false);

				sendBroadcastPackets(null, params, ClusterMethods.USER_CONNECTED,
						cl_nodes.toArray(new JID[cl_nodes.size()]));
			} catch (Exception e) {
				log.log(Level.WARNING, "Problem with broadcast user connected for: " + conn, e);
			}
		} else {
			if (log.isLoggable(Level.WARNING)) {
				log.warning("User resourc-rebind - not implemented yet in the cluster."
						+ " Connection: " + conn);
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

///**
// *
// * @return
// */
//@Override
//public Set<String> getOnlineJids() {
//  return strategy.getOnlineJids();
//}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean hasCompleteJidsInfo() {
		return strategy.hasCompleteJidsInfo();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	@Override
	public void nodeConnected(String node) {
		log.fine("Nodes connected: " + node);

		JID jid = JID.jidInstanceNS(getName(), node, null);

		addTrusted(jid);
		strategy.nodeConnected(jid);
		sendAdminNotification("Cluster node '" + node + "' connected (" + (new Date()) + ")",
				"New cluster node connected: " + node, node);

		if (strategy.needsSync()) {
			requestSync(jid);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	@Override
	public void nodeDisconnected(String node) {
		log.log(Level.FINE, "Nodes disconnected: {0}", node);

		JID jid = JID.jidInstanceNS(getName(), node, null);

		strategy.nodeDisconnected(jid);

		// Not sure what to do here, there might be still packets
		// from the cluster node waiting....
		// delTrusted(jid);
		sendAdminNotification("Cluster node '" + node + "' disconnected (" + (new Date()) + ")",
				"Cluster node disconnected: " + node, node);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Received packet: {0}", packet);
		}

		if ((packet.getElemName() == ClusterElement.CLUSTER_EL_NAME)
				&& (packet.getElement().getXMLNS() == ClusterElement.XMLNS)) {
			if (isTrusted(packet.getStanzaFrom())) {
				try {
					processClusterPacket(packet);
				} catch (TigaseStringprepException ex) {
					log.log(Level.WARNING, "Packet processing stringprep problem: {0}", packet);
				}
			} else {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "Cluster packet from untrusted source: {0}", packet);
				}
			}

			return;
		}

		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");

			// No more processing is needed for command packet
			return;
		}    // end of if (pc.isCommand())

		XMPPResourceConnection conn = getXMPPResourceConnection(packet);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Ressource connection found: {0}", conn);
		}

		if ((conn == null)
				&& (isBrokenPacket(packet) || processAdminsOrDomains(packet)
					|| sendToNextNode(packet))) {
			return;
		}

		processPacket(packet, conn);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int processingThreads() {
		return Math.max(nodesNo, super.processingThreads());
	}

	//~--- set methods ----------------------------------------------------------

//@Override
//public void release() {
//  //delayedTasks.cancel();
//  super.release();
//}
//@Override
//public void start() {
//  super.start();
//  //delayedTasks = new Timer("SM Cluster Delayed Tasks", true);
//}

	/**
	 * Method description
	 *
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterController cl_controller) {}

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);

		String strategy_class = (String) props.get(STRATEGY_CLASS_PROP_KEY);

		try {
			ClusteringStrategyIfc strategy_tmp =
				(ClusteringStrategyIfc) Class.forName(strategy_class).newInstance();

			strategy_tmp.setProperties(props);

			// strategy_tmp.init(getName());
			strategy = strategy_tmp;
			strategy.nodeConnected(getComponentId());
			addTrusted(getComponentId());
		} catch (Exception e) {
			log.log(Level.SEVERE,
					"Can not clustering strategy instance for class: " + strategy_class, e);
		}

		try {
			my_hostname = JID.jidInstance((String) props.get(MY_DOMAIN_NAME_PROP_KEY));
			my_address = JID.jidInstance(getName(), (String) props.get(MY_DOMAIN_NAME_PROP_KEY),
					null);
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING,
					"Creating component source address failed stringprep processing: {0}@{1}",
						new Object[] { getName(),
					my_hostname });
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	protected void closeSession(XMPPResourceConnection conn, boolean closeOnly) {
		if ((conn.getConnectionStatus() != ConnectionStatus.REMOTE) && conn.isAuthorized()) {
			try {
				JID connectionId = conn.getConnectionId();
				BareJID userId = conn.getBareJID();
				String resource = conn.getResource();
				Map<String, String> params = new LinkedHashMap<String, String>();

				params.put(CONNECTION_ID, connectionId.toString());
				params.put(USER_ID, userId.toString());
				params.put(RESOURCE, resource);

				List<JID> cl_nodes = strategy.getAllNodes();

				for (JID node : cl_nodes) {
					if ( !node.equals(getComponentId())) {
						Element check_session_el =
							ClusterElement.createClusterMethodCall(getComponentId().toString(),
								node.toString(), StanzaType.set, ClusterMethods.USER_DISCONNECTED.toString(),
								params).getClusterElement();

						fastAddOutPacket(Packet.packetInstance(check_session_el, getComponentId(), node));
					}
				}
			} catch (Exception ex) {
				log.log(Level.WARNING, "Problem sending user disconnect broadcast for: " + conn, ex);
			}
		}

		XMPPSession parentSession = conn.getParentSession();

		super.closeSession(conn, closeOnly);

		if ((conn.getConnectionStatus() != ConnectionStatus.REMOTE) && (parentSession != null)
				&& (parentSession.getActiveResourcesSize()
					== parentSession.getResSizeForConnStatus(ConnectionStatus.REMOTE))) {
			List<XMPPResourceConnection> conns = parentSession.getActiveResources();

			for (XMPPResourceConnection xrc : conns) {
				try {
					super.closeConnection(xrc.getConnectionId(), true);

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Closed remote connection: " + xrc);
					}
				} catch (NoConnectionIdException ex) {
					log.log(Level.WARNING, "This should not happen, check it out!, ", ex);
				}
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	protected JID getFirstClusterNode(JID userJid) {
		JID cluster_node = null;
		List<JID> nodes = strategy.getNodesForJid(userJid);

		if (nodes != null) {
			for (JID node : nodes) {
				if ( !node.equals(getComponentId())) {
					cluster_node = node;

					break;
				}
			}
		}

		return cluster_node;
	}

	//~--- methods --------------------------------------------------------------

	@TODO(note = "Possible performance bottleneck if there are many users with multiple "
			+ "connections to different nodes.")
	protected void processClusterPacket(Packet packet) throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<Packet>();
		final ClusterElement clel = new ClusterElement(packet.getElement());
		ClusterMethods method = ClusterMethods.parseMethod(clel.getMethodName());

		switch (packet.getType()) {
			case set :
				if (clel.getMethodName() == null) {
					processPacket(clel);
				}

				switch (method) {
					case USER_INITIAL_PRESENCE : {
						BareJID userId = BareJID.bareJIDInstanceNS(clel.getMethodParam(USER_ID));
						String resource = clel.getMethodParam(RESOURCE);
						XMPPSession session = getSession(userId);

						if ((session != null) && (session.getResourceForResource(resource) == null)) {
							JID connectionId = JID.jidInstanceNS(clel.getMethodParam(CONNECTION_ID));
							String xmpp_sessionId = clel.getMethodParam(XMPP_SESSION_ID);
							String domain = userId.getDomain();
							XMPPResourceConnection res_con = loginUserSession(connectionId, domain, userId,
								resource, ConnectionStatus.REMOTE, xmpp_sessionId);

							if (res_con != null) {
								List<Element> packs = clel.getDataPackets();

								for (Element elem : packs) {
									if (elem.getName() == Presence.PRESENCE_ELEMENT_NAME) {
										res_con.setPresence(elem);
									}
								}

								res_con.putSessionData(SM_ID, packet.getStanzaFrom());

								// Send the presence from the new user connection to all local
								// (non-remote) user connections
								updateUserResources(res_con, results);

								for (XMPPResourceConnection xrc : session.getActiveResources()) {
									if ((xrc.getConnectionStatus() != ConnectionStatus.REMOTE)
											&& (xrc.getPresence() != null)) {
										broadcastUserPresence(xrc, packet.getStanzaFrom());
									}
								}

								if (log.isLoggable(Level.FINEST)) {
									log.finest("Added remote session for: " + userId + ", from: "
											+ packet.getStanzaFrom());
								}
							} else {
								if (log.isLoggable(Level.INFO)) {
									log.info("Couldn't create user session for: " + userId + ", resource: "
											+ resource + ", connectionId: " + connectionId);
								}
							}
						} else {

							// Ignoring this, nothing special to do.
							if (log.isLoggable(Level.FINEST)) {
								if (session == null) {
									log.finest("Ignoring USER_INITIAL_PRESENCE for: " + userId + ", from: "
											+ packet.getStanzaFrom()
												+ ", there is no other session for the user on this node.");
								} else {
									if (session.getResourceForResource(resource) != null) {
										log.finest("Ignoring USER_INITIAL_PRESENCE for: " + userId + ", from: "
												+ packet.getStanzaFrom()
													+ ", there is already a session on this node for this resource.");
									} else {
										log.finest("Ignoring USER_INITIAL_PRESENCE for: " + userId + ", from: "
												+ packet.getStanzaFrom() + ", reason unknown, please contact devs.");
									}
								}
							}
						}

						break;
					}

					case USER_CONNECTED : {
						BareJID userId = BareJID.bareJIDInstanceNS(clel.getMethodParam(USER_ID));
						String resource = clel.getMethodParam(RESOURCE);
						JID connectionId = JID.jidInstanceNS(clel.getMethodParam(CONNECTION_ID));

						strategy.usersConnected(packet.getStanzaFrom(), results,
								JID.jidInstanceNS(userId, resource + "#" + connectionId));

						break;
					}

					case USER_DISCONNECTED : {
						BareJID userId = BareJID.bareJIDInstanceNS(clel.getMethodParam(USER_ID));
						String resource = clel.getMethodParam(RESOURCE);

						strategy.userDisconnected(packet.getStanzaFrom(), results,
								JID.jidInstanceNS(userId, resource));

						XMPPSession session = getSession(userId);

						if (session != null) {
							JID connectionId = JID.jidInstanceNS(clel.getMethodParam(CONNECTION_ID));

							// Possible performance bottleneck if there are many users with
							// multiple connections to different nodes. If all disconnect at
							// the same time we might have a problem here.
							closeConnection(connectionId, true);

							if (log.isLoggable(Level.FINEST)) {
								log.finest("Removed remote session for: " + userId + ", from: "
										+ packet.getStanzaFrom());
							}
						}

						break;
					}
				}

				break;

			case get :
				switch (method) {
					case SYNC_ONLINE :

						// Send back all online users on this node
						Collection<XMPPResourceConnection> conns = connectionsByFrom.values();
						int counter = 0;
						StringBuilder sb = new StringBuilder(40000);

						for (XMPPResourceConnection conn : conns) {
							String jid = null;

							// Exception would be thrown for all not-authenticated yet connection
							// We don't have to worry about them, just ignore all of them
							// They should be synchronized later on using standard cluster
							// notifications.
							try {
								jid = conn.getJID() + "#" + conn.getConnectionId();
							} catch (Exception e) {
								jid = null;
							}

							if (jid != null) {
								if (sb.length() == 0) {
									sb.append(jid);
								} else {
									sb.append(',').append(jid);
								}

								if (++counter > SYNC_MAX_BATCH_SIZE) {

									// Send a new batch...
									ClusterElement resp = clel.createMethodResponse(getComponentId().toString(),
										StanzaType.result, null);

									resp.addMethodResult(SYNC_ONLINE_JIDS, sb.toString());
									fastAddOutPacket(Packet.packetInstance(resp.getClusterElement()));
									counter = 0;

									// Not sure which is better, create a new StringBuilder instance
									// or clearing existing up...., let's clear it up for now.
									sb.delete(0, sb.length());
								}
							}
						}

						if (sb.length() > 0) {

							// Send a new batch...
							ClusterElement resp = clel.createMethodResponse(getComponentId().toString(),
								StanzaType.result, null);

							resp.addMethodResult(SYNC_ONLINE_JIDS, sb.toString());
							fastAddOutPacket(Packet.packetInstance(resp.getClusterElement()));
						}

						break;

					default :

					// Do nothing...
				}

				break;

			case result :
				switch (method) {
					case SYNC_ONLINE :

						// Notify clustering strategy about SYNC_ONLINE response
						String jids = clel.getMethodResultVal(SYNC_ONLINE_JIDS);

						if (jids != null) {
							String[] jidsa = jids.split(",");
							JID[] jid_j = new JID[jidsa.length];
							int idx = 0;

							for (String jid : jidsa) {
								jid_j[idx++] = JID.jidInstanceNS(jid);
							}

							try {
								strategy.usersConnected(packet.getStanzaFrom(), results, jid_j);
							} catch (Exception e) {
								log.log(Level.WARNING,
										"Problem synchronizing cluster nodes for packet: " + packet, e);
							}
						} else {
							log.warning("Sync online packet with empty jids list! Please check this out: "
									+ packet.toString());
						}

						break;

					default :

					// Do nothing...
				}

				break;

			case error :
				JID from = packet.getStanzaFrom();

				clel.addVisitedNode(from.toString());
				processPacket(clel);

				break;

			default :
				break;
		}

		addOutPackets(results);
	}

	@SuppressWarnings("unchecked")
	protected void processPacket(ClusterElement packet) {
		List<Element> elems = packet.getDataPackets();

		// String packet_from = packet.getDataPacketFrom();
		if ((elems != null) && (elems.size() > 0)) {
			for (Element elem : elems) {
				try {
					Packet el_packet = Packet.packetInstance(elem);
					XMPPResourceConnection conn = getXMPPResourceConnection(el_packet);

					if ((conn != null) ||!sendToNextNode(packet, el_packet.getStanzaTo())) {
						processPacket(el_packet, conn);
					}
				} catch (TigaseStringprepException ex) {
					log.warning("Addressing problem, stringprep failed for packet: " + elem);
				}
			}
		} else {
			log.finest("Empty packets list in the cluster packet: " + packet.toString());
		}
	}

	protected boolean sendToNextNode(ClusterElement clel, JID userId)
			throws TigaseStringprepException {
		ClusterElement next_clel = ClusterElement.createForNextNode(clel,
			strategy.getNodesForJid(userId), getComponentId());

		if (next_clel != null) {
			fastAddOutPacket(Packet.packetInstance(next_clel.getClusterElement()));

			return true;
		} else {
			String first = clel.getFirstNode();

			if ((first != null) &&!first.equals(getComponentId().toString())) {
				List<Element> packets = clel.getDataPackets();
				Element elem = (((packets != null) && (packets.size() == 1)) ? packets.get(0) : null);
				Packet packet = ((elem != null) ? Packet.packetInstance(elem) : null);

				if ((packet == null)
						|| ((packet.getType() != StanzaType.result)
							&& (packet.getType() != StanzaType.available)
								&& (packet.getType() != StanzaType.unavailable)
									&& (packet.getType() != StanzaType.error)
										&&!((packet.getElemName() == "presence") && (packet.getType() == null)))) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Sending back to the first node: {0}", first);
					}

					ClusterElement result = clel.nextClusterNode(first);

					result.addVisitedNode(getComponentId().toString());
					fastAddOutPacket(Packet.packetInstance(result.getClusterElement()));
				}

				return true;
			} else {
				return false;
			}
		}
	}

	protected boolean sendToNextNode(Packet packet) {
		JID cluster_node = getFirstClusterNode(packet.getStanzaTo());

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Cluster node found: {0}", cluster_node);
		}

		if (cluster_node != null) {
			JID sess_man_id = getComponentId();
			ClusterElement clel = new ClusterElement(sess_man_id.toString(),
				cluster_node.toString(), StanzaType.set, packet);

			clel.addVisitedNode(sess_man_id.toString());
			fastAddOutPacket(Packet.packetInstance(clel.getClusterElement(), sess_man_id,
					cluster_node));

			return true;
		}

		return false;
	}

	protected void updateUserResources(XMPPResourceConnection res_con, Queue<Packet> results) {
		try {
			Element pres_update = res_con.getPresence();

			for (XMPPResourceConnection conn : res_con.getActiveSessions()) {
				try {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Update presence change to: " + conn.getjid());
					}

					if ((conn != res_con) && conn.isResourceSet()
							&& (conn.getConnectionStatus() != ConnectionStatus.REMOTE)) {
						if (pres_update != null) {
							pres_update = pres_update.clone();
						} else {
							pres_update = new Element(Presence.PRESENCE_ELEMENT_NAME);
						}

						// Below code not needed anymore, packetInstance(...) takes care of it
//          pres_update.setAttribute("from", res_con.getJID().toString());
//          pres_update.setAttribute("to", conn.getBareJID().toString());
						Packet pack_update = Packet.packetInstance(pres_update, res_con.getJID(),
							conn.getJID().copyWithoutResource());

						pack_update.setPacketTo(conn.getConnectionId());
						results.offer(pack_update);
					} else {
						if (log.isLoggable(Level.FINER)) {
							log.finer("Skipping presence update to: " + conn.getjid());
						}
					}    // end of else
				} catch (NoConnectionIdException ex) {

					// This actually should not happen... might be a bug:
					log.log(Level.WARNING, "This should not happen, check it out!, ", ex);
				} catch (NotAuthorizedException ex) {
					log.warning("This should not happen, unless the connection has been "
							+ "stopped in a concurrent thread or has not been authenticated yet: " + conn);
				}
			}    // end of for (XMPPResourceConnection conn: sessions)
		} catch (NotAuthorizedException ex) {
			log.warning("User session from another cluster node authentication problem: " + res_con);
		}
	}

	private void broadcastUserPresence(XMPPResourceConnection conn, JID... cl_nodes) {
		try {
			Map<String, String> params = prepareBroadcastParams(conn, true);
			Element presence = conn.getPresence();

			sendBroadcastPackets(presence, params, ClusterMethods.USER_INITIAL_PRESENCE, cl_nodes);
		} catch (Exception e) {
			log.log(Level.WARNING,
					"Problem with broadcast user initial presence message for: " + conn);
		}
	}

	private void broadcastUserPresence(XMPPResourceConnection conn, List<JID> cl_nodes) {
		try {
			Map<String, String> params = prepareBroadcastParams(conn, true);
			Element presence = conn.getPresence();

			if (presence == null) {
				log.log(Level.WARNING, "Something wrong. Initial presence NULL!!",
						Thread.currentThread().getStackTrace());
			}

			sendBroadcastPackets(presence, params, ClusterMethods.USER_INITIAL_PRESENCE,
					cl_nodes.toArray(new JID[cl_nodes.size()]));
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem with broadcast user initial presence for: " + conn, e);
		}
	}

	private Map<String, String> prepareBroadcastParams(XMPPResourceConnection conn,
			boolean full_details)
			throws NotAuthorizedException, NoConnectionIdException {
		BareJID userId = conn.getBareJID();
		String resource = conn.getResource();
		JID connectionId = conn.getConnectionId();
		Map<String, String> params = new LinkedHashMap<String, String>();

		params.put(USER_ID, userId.toString());
		params.put(RESOURCE, resource);
		params.put(CONNECTION_ID, connectionId.toString());

		if (full_details) {
			String xmpp_sessionId = conn.getSessionId();
			long authTime = conn.getAuthTime();

			params.put(XMPP_SESSION_ID, xmpp_sessionId);
			params.put(AUTH_TIME, "" + authTime);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending user: " + userId + " session, resource: " + resource
						+ ", xmpp_sessionId: " + xmpp_sessionId + ", connectionId: " + connectionId);
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Sending user: " + userId + " session, resource: " + resource);
			}
		}

		return params;
	}

	private void requestSync(JID node) {
		ClusterElement clel = ClusterElement.createClusterMethodCall(getComponentId().toString(),
			node.toString(), StanzaType.get, ClusterMethods.SYNC_ONLINE.name(), null);

		fastAddOutPacket(Packet.packetInstance(clel.getClusterElement(), getComponentId(), node));
	}

	private void sendAdminNotification(String msg, String subject, String node) {
		String message = msg;

		if (node != null) {
			message = msg + "\n";
		}

		int cnt = 0;

		message += node + " connected to " + getDefHostName();

		Packet p_msg = Message.getMessage(my_address, my_hostname, StanzaType.normal, message,
			subject, "xyz", newPacketId(null));

		sendToAdmins(p_msg);
	}

	private void sendBroadcastPackets(Element data, Map<String, String> params,
			ClusterMethods methodCall, JID... nodes) {
		ClusterElement clel = ClusterElement.createClusterMethodCall(getComponentId().toString(),
			nodes[0].toString(), StanzaType.set, methodCall.toString(), params);

		if (data != null) {
			clel.addDataPacket(data);
		}

		Element check_session_el = clel.getClusterElement();

		if ( !nodes[0].equals(getComponentId())) {
			fastAddOutPacket(Packet.packetInstance(check_session_el, getComponentId(), nodes[0]));
		}

		for (int i = 1; i < nodes.length; i++) {
			if ( !nodes[i].equals(getComponentId())) {
				Element elem = check_session_el.clone();

				elem.setAttribute("to", nodes[i].toString());
				fastAddOutPacket(Packet.packetInstance(elem, getComponentId(), nodes[i]));
			}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
