/*
 * SMNonCachingAllNodes.java
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

import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;

import tigase.stats.StatisticsList;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import tigase.cluster.api.SessionManagerClusteredIfc;

/**
 * Created: May 13, 2009 9:53:44 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class SMNonCachingAllNodes
				implements ClusteringStrategyIfc<ConnectionRecord> {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(SMNonCachingAllNodes.class
			.getName());

	//~--- fields ---------------------------------------------------------------

	private SessionManagerClusteredIfc     sm            = null;

	// Simple random generator, we do not need a strong randomization here.
	// Just enough to ensure better traffic distribution
	private Random rand = new Random();

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean containsJid(BareJID jid) {
		return false;
	}

	@Override
	public void nodeConnected(JID jid) {
	}

	@Override
	public void nodeDisconnected(JID jid) {
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public List<JID> getNodesConnected() {
		return sm.getNodesConnected();
	}

	@Override
	public JID[] getConnectionIdsForJid(BareJID jid) {
		return null;
	}

	@Override
	public Set<ConnectionRecord> getConnectionRecords(BareJID bareJID) {
		return null;
	}

	@Override
	@Deprecated
	public Object getInternalCacheData() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * 
	 */
	public List<JID> getNodesForJid(JID jid) {
		return getNodesConnected();
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
	 */
	public List<JID> getNodesForPacketForward(JID fromNode, Set<JID> visitedNodes,
			Packet packet) {

		// If the packet visited other nodes already it means it went through other
		// checking
		// like isSuitableForForward, etc... so there is no need for doing it again
		if (visitedNodes != null) {
			List<JID> result = selectNodes(fromNode, visitedNodes);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Visited nodes not null: {0}, selecting new node: {1}, for packet: {2}",
						new Object[] { visitedNodes,
						result, packet });
			}

			return result;
		}

		// Presence status change set by the user have a special treatment:
		if ((packet.getElemName() == "presence") && (packet.getType() != StanzaType.error) &&
				(packet.getStanzaFrom() != null) && (packet.getStanzaTo() == null)) {
			List<JID> result = getNodesConnected();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Presence packet found: {0}, selecting all nodes: {1}",
						new Object[] { packet,
						result });
			}

			return result;
		}
		if (isSuitableForForward(packet)) {
			List<JID> result = selectNodes(fromNode, visitedNodes);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
						"Visited nodes null, selecting new node: {0}, for packet: {1}",
						new Object[] { result,
						packet });
			}

			return result;
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet not suitable for forwarding: {0}", new Object[] {
						packet });
			}

			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.strategy.ClusteringStrategyIfc#getNodesForUserConnect()
	 */

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * 
	 */
	public List<JID> getNodesForUserConnect(JID jid) {
		return getNodesConnected();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * tigase.cluster.strategy.ClusteringStrategyIfc#getNodesForUserDisconnect()
	 */

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * 
	 */
	public List<JID> getNodesForUserDisconnect(JID jid) {
		return getNodesConnected();
	}

	@Override
	public void getStatistics(StatisticsList list) {
	}

	@Override
	public boolean hasCompleteJidsInfo() {
		return false;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props) {}

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
	 */
	protected boolean isSuitableForForward(Packet packet) {

		// Do not forward any error packets for now.
		if (packet.getType() == StanzaType.error) {
			return false;
		}

		// Artur: Moved it to the front of the method for performance reasons.
		// TODO: make sure it does not affect logic.
		if ((packet.getPacketFrom() != null) &&!sm.getComponentId().equals(packet
				.getPacketFrom())) {
			return false;
		}

		// This is for packet forwarding logic.
		// Some packets are for certain not forwarded like packets without to
		// attribute set.
		if ((packet.getStanzaTo() == null) || sm.isLocalDomain(packet.getStanzaTo()
				.toString(), false) || sm.getComponentId().equals((packet.getStanzaTo()
				.getBareJID()))) {
			return false;
		}

		// Also packets sent from the server to user are not being forwarded like
		// service discovery perhaps?
		if ((packet.getStanzaFrom() == null) || sm.isLocalDomain(packet.getStanzaFrom()
				.toString(), false) || sm.getComponentId().equals((packet.getStanzaFrom()
				.getBareJID()))) {
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

	//~--- methods --------------------------------------------------------------

	/**
	 * @param fromNode
	 * @param visitedNodes
	 * 
	 */
	private List<JID> selectNodes(JID fromNode, Set<JID> visitedNodes) {
		List<JID> result = null;
		List<JID> cl_nodes_list = getNodesConnected();
		int       size   = cl_nodes_list.size();

		if (size == 0) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No connected cluster nodes found, returning null");
			}

			return null;
		}

		int idx = rand.nextInt(size);

		if ((visitedNodes == null) || (visitedNodes.size() == 0)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "No visited nodes yet, trying random idx: " + idx);
			}
			try {
				result = Collections.singletonList(cl_nodes_list.get(idx));
			} catch (IndexOutOfBoundsException ioobe) {

				// This may happen if the node disconnected in the meantime....
				try {
					result = Collections.singletonList(cl_nodes_list.get(0));
				} catch (IndexOutOfBoundsException ioobe2) {

					// Yes, this may happen too if there were only 2 nodes before
					// disconnect....
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								"IndexOutOfBoundsException twice! Should not happen very often, returning null");
					}
				}
			}
		} else {
			for (JID jid : cl_nodes_list) {
				if (!visitedNodes.contains(jid)) {
					result = Collections.singletonList(jid);

					break;
				}
			}

			// If all nodes visited already. We have to either send it back to the
			// first node
			// or if this is the first node return null
			if ((result == null) &&!sm.getComponentId().equals(fromNode)) {
				result = Collections.singletonList(fromNode);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"All nodes visited, sending it back to the first node: " + result);
				}
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "List of result nodes: " + result);
		}

		return result;
	}
}

