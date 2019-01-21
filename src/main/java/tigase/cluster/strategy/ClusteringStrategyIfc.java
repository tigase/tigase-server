/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.cluster.strategy;

import tigase.annotations.TigaseDeprecated;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.server.Packet;
import tigase.stats.StatisticHolder;
import tigase.stats.StatisticsList;
import tigase.sys.OnlineJidsReporter;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created: May 2, 2009 4:36:03 PM
 *
 * @param <E>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ClusteringStrategyIfc<E extends ConnectionRecordIfc>
		extends OnlineJidsReporter, StatisticHolder {

	public void handleLocalPresenceSet(XMPPResourceConnection conn);

	public void handleLocalResourceBind(XMPPResourceConnection conn);

	public void handleLocalUserLogin(BareJID userId, XMPPResourceConnection conn);

	public void handleLocalUserLogout(BareJID userId, XMPPResourceConnection conn);

	public void handleLocalUserChangedConnId(BareJID userId, XMPPResourceConnection conn, JID oldConnId, JID newConnId);

	public String getInfo();

	void handleLocalPacket(Packet packet, XMPPResourceConnection conn);

	/**
	 * This is a handler method which is called when a new node connects to the cluster.
	 *
	 * @param node is a cluster node id.
	 */
	void nodeConnected(JID node);

	/**
	 * This is a handler method which is called when a node disconnects from the cluster.
	 *
	 * @param node is a cluster node id.
	 */
	void nodeDisconnected(JID node);

	boolean processPacket(Packet packet, XMPPResourceConnection conn);

	/**
	 * The method returns all cluster nodes currently connected to the cluster node.
	 *
	 * @return List of all cluster nodes currently connected to the cluster node.
	 */
	List<JID> getNodesConnected();

	/**
	 * Returns a ConnectionRecord object associated with this user's full JID if it exists in the cache or null if it
	 * does not. All parts of the user's JID are checked and ConnectionRecord is returned only if there is a match for
	 * all parts.
	 *
	 * @param jid is an instance of the user's full JID.
	 *
	 * @return ConnectionRecord instance associated with given user's JID or null if there is no ConnectionRecord in the
	 * cache.
	 */
	E getConnectionRecord(JID jid);

	E getConnectionRecordInstance();

	/**
	 * Returns a set with all ConnectionRecords found in the cache for a given user ID, that is BareJID. In other words
	 * all user's resources/connectionIDs found in the cache associated with user's account.
	 *
	 * @param bareJID is an instance of the user's BareJID, that is account ID.
	 *
	 * @return a Set instance with all ConnectionRecords found for a given BareJID. Note, the result may be null or it
	 * maybe an empty Set or non-empty set.
	 */
	Set<E> getConnectionRecords(BareJID bareJID);

	/**
	 * This method is used for configuration purpose. Following the convention used in the Tigase project this method is
	 * supposed to provide configuration defaults. All parameters which exist in configuration file overwrite
	 * corresponding default parameters. If some parameters are missing in configuration file defaults are used then.
	 * <br>
	 * A compiled set of parameters is then passed to <code>setProperties</code> method.
	 *
	 * @param params a <code>Map</code> with properties loaded from init.properties file which should be used for
	 * generating defaults.
	 *
	 * @return a <code>Map</code> with all the class default configuration parameters.
	 */
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	Map<String, Object> getDefaults(Map<String, Object> params);

	/**
	 * <strong>Note! This is not for a common use method.</strong> This is for debugging and diagnostic purposes only
	 * and maybe removed or changed at any time in the future. It returns a content of an internal cache from the
	 * strategy. Regardless of the cache data organization inside the strategy, it is returned here in a common format.
	 * It may be a copy or a direct reference to internal data. Therefore this is unmodifiable instance of the cache.
	 * Generating results of this structure may be a costly operation, therefore it must not be called frequently.
	 *
	 * @return an Object with content of an internal cache data.
	 */
	@Deprecated
	@TigaseDeprecated(since = "7.0.0", removeIn = "8.1.0")
	Object getInternalCacheData();

	/**
	 * Add the strategy statistics to the List.
	 */
	void getStatistics(StatisticsList list);

	void setClusterController(ClusterControllerIfc clComp);

	/**
	 * Method used to pass configuration parameters to the class. Parameters are stored in <code>Map</code> which
	 * contains compiles set of defaults overwritten by parameters loaded from configuration file.
	 * <br>
	 * If he implementation took a good care of providing defaults for all parameters no parameter should be missing.
	 *
	 * @param props a <code>Map</code> with all configuration parameters for the class.
	 */
	void setProperties(Map<String, Object> props);

}

