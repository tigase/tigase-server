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

package tigase.cluster;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import tigase.server.Packet;

/**
 * Created: May 2, 2009 4:36:03 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ClusteringStrategyIfc {

	//void init(String smName);

	/**
	 * This method is used for configuration purpose. Following the convention
	 * used in the Tigase project this method is supposed to provide configuration
	 * defaults. All parameters which exist in configuration file overwrite
	 * corresponding default parameters. If some parameters are missing in
	 * configuration file defaults are used then.
	 *
	 * A compiled set of parameters is then passed to <code>setProperties</code>
	 * method.
	 * @param params a <code>Map</code> with properties loaded from
	 * init.properties file which should be used for generating defaults.
	 * @return a <code>Map</code> with all the class default configuration
	 * parameters.
	 */
	Map<String, Object> getDefaults(Map<String, Object> params);

	/**
	 * Method used to pass configuration parameters to the class. Parameters are
	 * stored in <code>Map</code> which contains compiles set of defaults overwriten
	 * by parameters loaded from configuration file.
	 *
	 * If he implementation took a good care of providing defaults for all
	 * parameters no parameter should be missing.
	 * @param props a <code>Map</code> with all configuration parameters for the class.
	 */
	void setProperties(Map<String, Object> props);

	/**
	 * Returns a <code>List</code> of all cluster nodes on which the given user session
	 * can exist. In the simplest scenario it can always return all cluster nodes
	 * as in theory the user can connect to any node if it is not on the local node.
	 * More specialized implementation can know kind of hashing algorithm which is
	 * used for binding a specific user to a specific node or the implementation
	 * can keep track of all connected users to all nodes and 'know' where is
	 * the user connected at any given time.
	 *
	 * In theory it can also return <code>'null'</code> it it 'knows' the user is
	 * offline.
	 * @param jid is a user full JID.
	 * @return List of cluster nodes to which the user can be connected.
	 */
	List<String> getNodesForJid(String jid);

	/**
	 * The method retutns all cluster nodes currently connected to the cluster.
	 * @return List of all cluster nodes currently connected to the cluster.
	 */
	List<String> getAllNodes();

	/**
	 * This is a handler method which is called when a new node connects to
	 * the cluster.
	 * @param node is a cluster node id.
	 */
	void nodeConnected(String node);

	/**
	 * This is a handler method which is called when a node disconnects from
	 * the cluster.
	 * @param node is a cluster node id.
	 */
	void nodeDisconnected(String node);

	/**
	 * This is a handler method which is called when a user connects to some
	 * node in the cluster.
	 * @param jid is a user full JID just connected to the cluster.
	 * @param node is a cluster node id where the jid is connected to.
	 * @param results is a collection of packets which can be generated upon the
	 * user connection by the implementation.
	 */
	void userConnected(String jid, String node, Queue<Packet> results);

	/**
	 * This is a handler method which is called when a user disconnects from some
	 * node in the cluster.
	 * @param jid us a user full JID just disconnected from the cluster.
	 * @param node is a cluster node id from which the user disconnected.
	 * @param results is a collection of packets which can be generated upon the
	 * user disconnection by the implementation.
	 */
	void userDisconnected(String jid, String node, Queue<Packet> results);

}
