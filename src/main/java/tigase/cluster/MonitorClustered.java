
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
package tigase.cluster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.server.monitor.MonitorComponent;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jul 4, 2010 7:18:46 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MonitorClustered extends MonitorComponent implements ClusteredComponentIfc {
	private static final Logger log = Logger.getLogger(MonitorClustered.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method is called on cluster node connection event. This is a
	 * notification to the component that a new cluster node has connected.
	 *
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	@Override
	public void nodeConnected(String node) {}

	/**
	 * Method is called on cluster node disconnection event. This is a
	 * notification to the component that there was network connection lost to one
	 * of the cluster nodes.
	 *
	 * @param node
	 *          is a hostname of a cluster node generating the event.
	 */
	@Override
	public void nodeDisconnected(String node) {}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Set's the configures the cluster controller object for cluster
	 * communication and API.
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
