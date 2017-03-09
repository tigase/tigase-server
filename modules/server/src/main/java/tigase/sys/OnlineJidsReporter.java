/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.sys;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

/**
 * Created: Apr 19, 2009 12:15:07 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public interface OnlineJidsReporter {

	/**
	 * Method checks whether the clustering strategy has a complete JIDs info.
	 * That is whether the strategy knows about all users connected to all nodes.
	 * Some strategies may choose not to share this information among nodes, hence
	 * the methods returns false. Other may synchronize this information and can
	 * provide it to further optimize cluster traffic.
	 *
	 * @return a true boolean value if the strategy has a complete information
	 *         about all users connected to all cluster nodes.
	 */
	boolean hasCompleteJidsInfo();

	/**
	 * The method checks whether the given JID is known to the installation,
	 * either user connected to local machine or any of the cluster nodes. False
	 * result does not mean the user is not connected. It means the method does
	 * not know anything about the JID. Some clustering strategies may not cache
	 * online users information.
	 *
	 * @param jid
	 *          a user's JID for whom we query information.
	 *
	 * @return true if the user is known as online to the installation, false if
	 *         the method does not know.
	 */
	boolean containsJid( BareJID jid );

	/**
	 * The method checks whether the given JID is known to local cluster node
	 * as connected user. False result means that given JID is not connected
	 * to local cluster node but it may be connected to other cluster node.
	 * Result of this method should be independent of used clustering strategy.
	 * 
	 * @param jid
	 *			a user's JID for whom we query information
	 * 
	 * @return true if user is known as connected to local cluster node, false if
	 *			it is not connected to local node
	 */
	boolean containsJidLocally( BareJID jid);

	/**
	 * The method checks whether the given JID is known to local cluster node
	 * as connected user. False result means that given JID is not connected
	 * to local cluster node but it may be connected to other cluster node.
	 * Result of this method should be independent of used clustering strategy.
	 * 
	 * @param jid
	 *			a user's JID for whom we query information
	 * 
	 * @return true if user is known as connected to local cluster node, false if
	 *			it is not connected to local node
	 */
	boolean containsJidLocally( JID jid);	
	
	/**
	 * Retrieve all connection IDs (CIDs) for the given user.
	 *
	 * @param jid id of the user for which we want to retrieve the list.
	 *
	 * @return an array of {@link JID} containing all Connection IDs (CIDs) for
	 *         the given user.
	 */
	JID[] getConnectionIdsForJid( BareJID jid );

}
