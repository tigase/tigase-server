/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.cluster.strategy;

import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.stats.StatisticsList;
import tigase.sys.OnlineJidsReporter;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Created: May 2, 2009 4:36:03 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ClusteringStrategyIfc extends OnlineJidsReporter {

	/**
	 * <strong>Note! This is not for a common use method.</strong> This is for
	 * debugging and diagnostic purposes only and maybe removed or changed at any
	 * time in the future. It returns a content of an internal cache from the
	 * strategy. Regardless of the cache data organization inside the strategy, it
	 * is returned here in a common format. It may be a copy or a direct reference
	 * to internal data. Therefore this is unmodifiable instance of the cache.
	 * Generating results of this structure may be a costly operation, therefore
	 * it must not be called frequently.
	 * 
	 * @return an Object with content of an internal cache data.
	 */
	@Deprecated
	Object getInternalCacheData();

	/**
	 * Returns a set with all ConnectionRecords found in the cache for a given
	 * user ID, that is BareJID. In other words all user's resources/connectionIDs
	 * found in the cache associated with user's account.
	 * 
	 * @param bareJID
	 *          is an instance of the user's BareJID, that is account ID.
	 * @return a Set instance with all ConnectionRecords found for a given
	 *         BareJID. Note, the result may be null or it maybe an empty Set or
	 *         non-empty set.
	 */
	Set<ConnectionRecord> getConnectionRecords(BareJID bareJID);

	/**
	 * Returns a ConnectionRecord object associated with this user's full JID if
	 * it exists in the cache or null if it does not. All parts of the user's JID
	 * are checked and ConnectionRecord is returned only if there is a match for
	 * all parts.
	 * 
	 * @param jid
	 *          is an instance of the user's full JID.
	 * @return ConnectionRecord instance associated with given user's JID or null
	 *         if there is no ConnectionRecord in the cache.
	 */
	ConnectionRecord getConnectionRecord(JID jid);

	/**
	 * The method allows to obtain SessionManagerHandler object by the strategy.
	 * The object is mainly used to access local VHosts configuration and check
	 * the ID of the local session manager.
	 * 
	 * @param sm
	 *          is an instance of the SessionManagerHandler class.
	 */
	void setSessionManagerHandler(SessionManagerHandler sm);

	/**
	 * The method returns all cluster nodes currently connected to the cluster.
	 * 
	 * @return List of all cluster nodes currently connected to the cluster.
	 */
	List<JID> getAllNodes();

	/**
	 * This method is used for configuration purpose. Following the convention
	 * used in the Tigase project this method is supposed to provide configuration
	 * defaults. All parameters which exist in configuration file overwrite
	 * corresponding default parameters. If some parameters are missing in
	 * configuration file defaults are used then.
	 * 
	 * A compiled set of parameters is then passed to <code>setProperties</code>
	 * method.
	 * 
	 * @param params
	 *          a <code>Map</code> with properties loaded from init.properties
	 *          file which should be used for generating defaults.
	 * @return a <code>Map</code> with all the class default configuration
	 *         parameters.
	 */
	Map<String, Object> getDefaults(Map<String, Object> params);

	/**
	 * Returns a <code>List</code> of all cluster nodes on which the given user
	 * session can exist. In the simplest scenario it can always return all
	 * cluster nodes as in theory the user can connect to any node if it is not on
	 * the local node. More specialized implementation can know kind of hashing
	 * algorithm which is used for binding a specific user to a specific node or
	 * the implementation can keep track of all connected users to all nodes and
	 * 'know' where is the user connected at any given time.
	 * 
	 * In theory it can also return <code>'null'</code> it it 'knows' the user is
	 * off-line.
	 * 
	 * @param jid
	 *          is a user full JID.
	 * @return List of cluster nodes to which the user can be connected.
	 */
	List<JID> getNodesForJid(JID jid);

	/**
	 * Add the strategy statistics to the List.
	 * 
	 * @param list
	 */
	void getStatistics(StatisticsList list);

	/**
	 * This method returns <code>'true'</code> if it needs online users
	 * synchronization upon the node connection to the cluster. Normally it should
	 * return <code>'false'</code>.
	 * 
	 * It it return <code>'true'</code> then the synchronization starts. All
	 * online users from all other nodes would sent in batches and the
	 * synchronization can take any amount of time.
	 * 
	 * @return a boolean value whether synchronization is needed.
	 */
	boolean needsSync();

	/**
	 * This is a handler method which is called when a new node connects to the
	 * cluster.
	 * 
	 * @param node
	 *          is a cluster node id.
	 */
	void nodeConnected(JID node);

	/**
	 * This is a handler method which is called when a node disconnects from the
	 * cluster.
	 * 
	 * @param node
	 *          is a cluster node id.
	 */
	void nodeDisconnected(JID node);

	/**
	 * Method used to pass configuration parameters to the class. Parameters are
	 * stored in <code>Map</code> which contains compiles set of defaults
	 * overwritten by parameters loaded from configuration file.
	 * 
	 * If he implementation took a good care of providing defaults for all
	 * parameters no parameter should be missing.
	 * 
	 * @param props
	 *          a <code>Map</code> with all configuration parameters for the
	 *          class.
	 */
	void setProperties(Map<String, Object> props);

	/**
	 * This is a handler method which is called when a user disconnects from some
	 * node in the cluster.
	 * 
	 * @param jid
	 *          us a user full JID just disconnected from the cluster.
	 * @param node
	 *          is a cluster node id from which the user disconnected.
	 * @param results
	 *          is a collection of packets which can be generated upon the user
	 *          disconnection by the implementation.
	 */
	void userDisconnected(Queue<Packet> results, ConnectionRecord rec);

	/**
	 * This is a handler method which is called when a user connects to some node
	 * in the cluster.
	 * 
	 * @param jids
	 *          is a list of full user JIDs which just connected to the cluster.
	 *          Normally there is only one JID provided, however, in some cases
	 *          (during synchronization) there might be more than one.
	 * @param node
	 *          is a cluster node id where the jid is connected to.
	 * @param results
	 *          is a collection of packets which can be generated upon the user
	 *          connection by the implementation.
	 */
	void usersConnected(Queue<Packet> results, ConnectionRecord... rec);

	/**
	 * The method allows the strategy implementation to control to which cluster
	 * nodes forward the given packet. It may offer a different algorithm for
	 * message broadcasting and different for presences, for example.
	 * 
	 * @param fromNode
	 *          a source address if the packet was forwarded from a different
	 *          node, this may be null if the packet was generated on this node.
	 * @param visitedNodes
	 *          a list of cluster nodes through which the packet already traveled,
	 *          this parameter can be null if the packet was generated on this
	 *          node
	 * @param packet
	 *          a packet which is supposed to be sent to other node.
	 * @return a list of cluster nodes JIDs to which the packet should be sent.
	 */
	List<JID> getNodesForPacketForward(JID fromNode, List<JID> visitedNodes, Packet packet);

	/**
	 * The method is called on user's presence update received from a remote
	 * cluster node. The clustering strategy may choose to cache the presence
	 * locally if necessary.
	 * 
	 * @param presence
	 *          Packet received from a remote cluster node.
	 * @param rec
	 *          is an instance of the user's ConnectionRecord.
	 */
	void presenceUpdate(Element presence, ConnectionRecord rec);

	/**
	 * The method allows the strategy implementation to control to which cluster
	 * nodes send the notification about user's new connection event.
	 * 
	 * @return a list of cluster nodes JIDs to which the notification should be
	 *         sent.
	 */
	List<JID> getNodesForUserConnect(JID jid);

	/**
	 * The method allows the strategy implementation to control to which cluster
	 * nodes send the notification about user's disconnection event.
	 * 
	 * @return a list of cluster nodes JIDs to which the notification should be
	 *         sent.
	 */
	List<JID> getNodesForUserDisconnect(JID jid);

}
