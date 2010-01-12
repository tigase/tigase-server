/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.logging.Logger;
import java.util.logging.Level;

import tigase.server.ServiceChecker;
import tigase.xmpp.XMPPIOService;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.xmpp.JID;

/**
 * Describe class ClientConnectionClustered here.
 *
 *
 * Created: Sat Jun 21 22:23:18 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClientConnectionClustered extends ClientConnectionManager
	implements ClusteredComponent {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.cluster.ClientConnectionClustered");

	@Override
	public void nodeConnected(String node) {}

	/**
	 * 
	 * @param node
	 */
	@Override
	public void nodeDisconnected(String node) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Disconnected nodes: " + node);
		}
		final String hostname = node;
		doForAllServices(new ServiceChecker() {
			@Override
			public void check(final XMPPIOService service) {
				JID dataReceiver = service.getDataReceiver();
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Checking service for dataReceiver: " + dataReceiver);
				}
				if (dataReceiver != null && dataReceiver.getDomain().equals(hostname)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest(
								"Stopping service because corresponding cluster node stopped.");
					}
					service.stop();
				}
			}
		});
	}

	@Override
	public void setClusterController(ClusterController cl_controller) {
	}

}
