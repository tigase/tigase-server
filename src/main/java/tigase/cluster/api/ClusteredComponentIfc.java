/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.cluster.api;

import tigase.server.ServerComponent;

/**
 * Describe interface ClusteredComponent here.
 *
 *
 * Created: Mon Jun  9 20:00:46 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ClusteredComponentIfc extends ServerComponent {
	
	/**
	 * Set's the configures the cluster controller object for cluster
	 * communication and API.
	 *
	 * @param cl_controller cluster controller object used for cluster
	 *                      communication
	 */
	void setClusterController(ClusterControllerIfc cl_controller);

	/**
	 * Method is called on cluster node connection event. This is a
	 * notification to the component that a new cluster node has connected.
	 *
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	void nodeConnected(String node);

	/**
	 * Method is called on cluster node disconnection event. This is a
	 * notification to the component that there was network connection lost to one
	 * of the cluster nodes.
	 *
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	void nodeDisconnected(String node);

}
