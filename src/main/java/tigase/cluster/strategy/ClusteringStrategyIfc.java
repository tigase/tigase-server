/*
 * ClusteringStrategyIfc.java
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
import tigase.cluster.api.SessionManagerClusteredIfc;

import tigase.server.Packet;

import tigase.stats.StatisticHolder;
import tigase.stats.StatisticsList;

import tigase.sys.OnlineJidsReporter;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created: May 2, 2009 4:36:03 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 *
 * @param <E>
 */
public interface ClusteringStrategyIfc<E extends ConnectionRecordIfc>
				extends OnlineJidsReporter, StatisticHolder {
	
	/**
	 * Method description
	 * 
	 * 
	 * @param conn 
	 */
	public void handleLocalPresenceSet(XMPPResourceConnection conn);
	
	/**
	 * Method description
	 * 
	 * 
	 * @param conn 
	 */
	public void handleLocalResourceBind(XMPPResourceConnection conn);
	
	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param conn
	 */
	public void handleLocalUserLogin(BareJID userId, XMPPResourceConnection conn);

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param conn
	 */
	public void handleLocalUserLogout(BareJID userId, XMPPResourceConnection conn);

	public void handleLocalUserChangedConnId(BareJID userId, XMPPResourceConnection conn, JID oldConnId, JID newConnId);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getInfo();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param conn
	 */
	void handleLocalPacket(Packet packet, XMPPResourceConnection conn);

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
	boolean processPacket(Packet packet, XMPPResourceConnection conn);

	//~--- get methods ----------------------------------------------------------

	/**
	 * The method returns all cluster nodes currently connected to the cluster node.
	 *
	 * @return List of all cluster nodes currently connected to the cluster node.
	 */
	List<JID> getNodesConnected();

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
	E getConnectionRecord(JID jid);

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>E</code>
	 */
	E getConnectionRecordInstance();

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
	Set<E> getConnectionRecords(BareJID bareJID);

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
	 * Add the strategy statistics to the List.
	 *
	 * @param list
	 */
	void getStatistics(StatisticsList list);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 * @param clComp
	 */
	void setClusterController(ClusterControllerIfc clComp);

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
	 * The method allows to obtain SessionManagerHandler object by the strategy.
	 * The object is mainly used to access local VHosts configuration and check
	 * the ID of the local session manager.
	 *
	 * @param sm
	 *          is an instance of the SessionManagerHandler class.
	 */
	void setSessionManagerHandler(SessionManagerClusteredIfc sm);
}


//~ Formatted in Tigase Code Convention on 13/11/29
