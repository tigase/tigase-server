/*
 * DefaultClusteringStrategy.java
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



package tigase.cluster.strategy;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.CommandListener;
import tigase.cluster.api.SessionManagerClusteredIfc;
import tigase.cluster.strategy.cmd.PacketForwardCmd;
import tigase.cluster.strategy.cmd.UserConnectedCmd;
import tigase.cluster.strategy.cmd.UserPresenceCmd;

import tigase.server.Packet;

import tigase.stats.StatisticsList;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;

import static tigase.cluster.api.SessionManagerClusteredIfc.SESSION_FOUND_KEY;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;

/**
 * Created: May 13, 2009 9:53:44 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 *
 * @param <E>
 */
public class DefaultClusteringStrategy<E extends ConnectionRecordIfc>
				implements ClusteringStrategyIfc<E> {
	/** Field description */
	public static final String USER_CONNECTED_CMD = "user-connected-sm-cmd";

	/** Field description */
	public static final String USER_DISCONNECTED_CMD = "user-disconnected-sm-cmd";

	/** Field description */
	public static final String USER_PRESENCE_CMD = "user-presence-sm-cmd";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(DefaultClusteringStrategy.class
			.getName());
	private static final String PACKET_FORWARD_CMD = "packet-forward-sm-cmd";

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected ClusterControllerIfc cluster = null;

	// private ClusteringMetadataIfc<E> metadata = null;

	/** Field description */
	private SessionManagerClusteredIfc sm       = null;
	private Set<CommandListener>       commands =
			new CopyOnWriteArraySet<CommandListener>();

	/** Field description */
	protected CopyOnWriteArrayList<JID> cl_nodes_list = new CopyOnWriteArrayList<JID>();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public DefaultClusteringStrategy() {
		super();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cmd
	 */
	public final void addCommandListener(CommandListener cmd) {
		commands.add(cmd);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean containsJid(BareJID jid) {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param conn
	 */
	@Override
	public void handleLocalPacket(Packet packet, XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}: for connection: {1}", new Object[] { packet, conn });
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn is a <code>XMPPResourceConnection</code>
	 */
	@Override
	public void handleLocalPresenceSet(XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "for connection: {0}", new Object[] { conn });
		}
		try {
			Map<String, String> params   = prepareConnectionParams(conn);
			List<JID>           cl_nodes = getAllNodes();

			cluster.sendToNodes(USER_PRESENCE_CMD, params, conn.getPresence(), getSM()
					.getComponentId(), null, cl_nodes.toArray(new JID[cl_nodes.size()]));
		} catch (NotAuthorizedException | NoConnectionIdException e) {
			log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn is a <code>XMPPResourceConnection</code>
	 */
	@Override
	public void handleLocalResourceBind(XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "for connection: {0}", new Object[] { conn });
		}
		try {
			Map<String, String> params   = prepareConnectionParams(conn);
			List<JID>           cl_nodes = getAllNodes();

			cluster.sendToNodes(USER_CONNECTED_CMD, params, getSM().getComponentId(), cl_nodes
					.toArray(new JID[cl_nodes.size()]));
		} catch (NotAuthorizedException | NoConnectionIdException e) {
			log.log(Level.WARNING, "Problem with broadcast user presence for: " + conn, e);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param conn
	 */
	@Override
	public void handleLocalUserLogin(BareJID userId, XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}: for connection: {1}", new Object[] { userId, conn });
		}

		// Do nothing
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param conn
	 */
	@Override
	public void handleLocalUserLogout(BareJID userId, XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}: for connection: {1}", new Object[] { userId, conn });
		}

		// Do nothing
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

		log.log(Level.FINE, "Cluster nodes: {0}, added: {1}", new Object[] { cl_nodes_list,
				result });
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

		log.log(Level.FINE, "Cluster nodes: {0}, removed: {1}", new Object[] { cl_nodes_list,
				result });
	}

	/**
	 * A utility method used to prepare a Map of data with user session data
	 * before it can be sent over to another cluster node. This is supposed to
	 * contain all the user's session essential information which directly
	 * identify user's resource and network connection. This information allows to
	 * detect two different user's connection made for the same resource. This may
	 * happen if both connections are established to different nodes.
	 *
	 * @param conn
	 *          is user's XMPPResourceConnection for which Map structure is
	 *          prepare.
	 *
	 *  a Map structure with all user's connection essential data.
	 *
	 *
	 * @return a value of <code>Map<String,String></code>
	 * @throws NoConnectionIdException
	 * @throws NotAuthorizedException
	 */
	public Map<String, String> prepareConnectionParams(XMPPResourceConnection conn)
					throws NotAuthorizedException, NoConnectionIdException {
		Map<String, String> params = new LinkedHashMap<>(10);

		params.put(USER_ID, conn.getBareJID().toString());
		params.put(RESOURCE, conn.getResource());
		params.put(CONNECTION_ID, conn.getConnectionId().toString());
		params.put(XMPP_SESSION_ID, conn.getSessionId());
		params.put(AUTH_TIME, "" + conn.getAuthTime());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for conn: {0}, result: ", new Object[] { conn,
					params });
		}

		return params;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param conn
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean processPacket(Packet packet, XMPPResourceConnection conn) {
		List<JID> toNodes = getNodesForPacketForward(sm.getComponentId(), null, packet);
		boolean   result  = (toNodes != null) && (toNodes.size() > 0);

		if (result) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Forwarding packet {0} to nodes: {1}", new Object[] {
						packet,
						toNodes });
			}

			Map<String, String> data = null;

			if (conn != null) {
				data = new LinkedHashMap<>();
				data.put(SESSION_FOUND_KEY, sm.getComponentId().toString());
			}
			cluster.sendToNodes(PACKET_FORWARD_CMD, data, packet.getElement(), sm
					.getComponentId(), null, toNodes.toArray(new JID[toNodes.size()]));
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No cluster nodes found for packet forward: {0}",
						new Object[] { packet });
			}
		}

		return result;
	}

	/**
	 * Method attempts to send the packet to the next cluster node. Returns true
	 * on successful attempt and false on failure. The true result does not mean
	 * that the packet has been delivered though. Only that it was sent. The send
	 * attempt may fail if there is no more cluster nodes to send the packet or if
	 * the clustering strategy logic decided that the packet does not have to be
	 * sent.
	 *
	 *
	 * @param fromNode
	 * @param data
	 * @param packet
	 *          to be sent to a next cluster node
	 * @param visitedNodes
	 *          a list of nodes already visited by the packet.
	 * @return true if the packet was sent to next cluster node and false
	 *         otherwise.
	 */
	public boolean sendToNextNode(JID fromNode, Set<JID> visitedNodes, Map<String,
			String> data, Packet packet) {
		boolean   result    = false;
		List<JID> nextNodes = getNodesForPacketForward(fromNode, visitedNodes, packet);

		if ((nextNodes != null) && (nextNodes.size() > 0)) {
			cluster.sendToNodes(PACKET_FORWARD_CMD, data, packet.getElement(), fromNode,
					visitedNodes, nextNodes.toArray(new JID[nextNodes.size()]));
			result = true;
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Called for packet: {0}, visitedNodes: {1}, result: {2}",
					new Object[] { packet,
					visitedNodes, result });
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String toString() {
		return getInfo();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>List<JID></code>
	 */
	@Override
	public List<JID> getAllNodes() {
		return cl_nodes_list;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>ClusterControllerIfc</code>
	 */
	public ClusterControllerIfc getClusterController() {
		return cluster;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 *
	 *
	 * @return a value of <code>JID[]</code>
	 */
	@Override
	public JID[] getConnectionIdsForJid(BareJID jid) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 *
	 *
	 * @return a value of <code>E</code>
	 */
	@Override
	public E getConnectionRecord(JID jid) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>E</code>
	 */
	@Override
	public E getConnectionRecordInstance() {
		return (E) (new ConnectionRecord());
	}

	/**
	 * Method description
	 *
	 *
	 * @param bareJID
	 *
	 *
	 *
	 * @return a value of <code>Set<E></code>
	 */
	@Override
	public Set<E> getConnectionRecords(BareJID bareJID) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 *
	 *
	 * @return a value of <code>Map<String,Object></code>
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getInfo() {
		return "basic strategy";
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>Object</code>
	 */
	@Override
	@Deprecated
	public Object getInternalCacheData() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param fromNode
	 * @param visitedNodes
	 * @param packet
	 *
	 *
	 *
	 * @return a value of <code>List<JID></code>
	 */
	public List<JID> getNodesForPacketForward(JID fromNode, Set<JID> visitedNodes,
			Packet packet) {

		// If visited nodes is not null then we return null as this strategy never
		// sends packets in ring, the first node decides where to send a packet
		if (visitedNodes != null) {
			return null;
		}

		List<JID> nodes = null;

//  JID jidLookup = packet.getStanzaTo();
//
//  // Presence status change set by the user have a special treatment:
//  if (presenceStatusUpdate(packet)) {
//    jidLookup = packet.getStanzaFrom();
//  }
		if (isSuitableForForward(packet)) {

			// nodes = metadata.getNodesForJid(jidLookup);
			nodes = getAllNodes();
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Selected nodes: {0}, for packet: {1}", new Object[] {
						nodes,
						packet });
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet not suitable for forwarding: {0}", new Object[] {
						packet });
			}
		}

		return nodes;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>SessionManagerClusteredIfc</code>
	 */
	@Override
	public SessionManagerClusteredIfc getSM() {
		return sm;
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		list.add("cluster-strat", "Connected nodes", cl_nodes_list.size(), Level.INFO);
		for (CommandListener cmd : commands) {
			cmd.getStatistics(list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean hasCompleteJidsInfo() {
		return false;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 * @param clComp
	 */
	@Override
	public void setClusterController(ClusterControllerIfc clComp) {
		cluster = clComp;
		for (CommandListener cmd : commands) {
			cluster.removeCommandListener(cmd);
			cluster.setCommandListener(cmd);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		addCommandListener(new PacketForwardCmd(PACKET_FORWARD_CMD, this));
		addCommandListener(new UserConnectedCmd(USER_CONNECTED_CMD, this));
		addCommandListener(new UserPresenceCmd(USER_PRESENCE_CMD, this));
	}

	/**
	 * Method description
	 *
	 *
	 * @param sm
	 */
	@Override
	public void setSessionManagerHandler(SessionManagerClusteredIfc sm) {
		this.sm = sm;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	protected boolean isSuitableForForward(Packet packet) {

		// Do not forward any error packets for now.
		if (packet.getType() == StanzaType.error) {
			return false;
		}

		// Artur: Moved it to the front of the method for performance reasons.
		// TODO: make sure it does not affect logic.
		// If this is a packet addressed from SM, we do not forward.
		if ((packet.getPacketFrom() != null) &&!sm.getComponentId().equals(packet
				.getPacketFrom())) {
			return false;
		}

		// This is for packet forwarding logic.
		// Some packets are for certain not forwarded like packets without "to"
		// attribute set, or a packet addressed to a local domain or a packet
		// addressed to a SM component
		if ((packet.getStanzaTo() == null) || sm.isLocalDomain(packet.getStanzaTo()
				.toString(), false) || sm.getComponentId().getBareJID().equals(packet
				.getStanzaTo().getBareJID())) {
			return false;
		}

		// Also packets sent from the server to user are not being forwarded like
		// service discovery perhaps?
		if ((packet.getStanzaFrom() == null) || sm.isLocalDomain(packet.getStanzaFrom()
				.toString(), false) || sm.getComponentId().getBareJID().equals(packet
				.getStanzaFrom().getBareJID())) {
			return false;
		}

		// If the packet is to some external domain, it is not forwarded to other
		// nodes either. It is also not forwarded if it is addressed to some
		// component.
		if (!sm.isLocalDomain(packet.getStanzaTo().getDomain(), false)) {
			return false;
		}

		return true;
	}
}


//~ Formatted in Tigase Code Convention on 13/11/11
