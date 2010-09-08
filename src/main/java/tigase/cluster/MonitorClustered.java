
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

//~--- non-JDK imports --------------------------------------------------------

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
public class MonitorClustered extends MonitorComponent implements ClusteredComponent {
	private static final Logger log = Logger.getLogger(MonitorClustered.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	@Override
	public void nodeConnected(String node) {}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	@Override
	public void nodeDisconnected(String node) {}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cl_controller
	 */
	@Override
	public void setClusterController(ClusterController cl_controller) {}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
