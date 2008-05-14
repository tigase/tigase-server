/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

//import tigase.cluster.ClusterElement;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.server.xmppsession.SessionManager;

import static tigase.server.xmppsession.SessionManagerConfig.*;

/**
 * Class SessionManagerClustered
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManagerClustered extends SessionManager {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppsession.SessionManagerClustered");

	private String[] cluster_nodes = {};

	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Received packet: " + packet.toString());
		}
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME
			&& packet.getElement().getXMLNS() == ClusterElement.XMLNS) {
			processClusterPacket(packet);
			return;
		}

		if (packet.isCommand()) {
			processCommand(packet);
			packet.processedBy("SessionManager");
			// No more processing is needed for command packet
			// 			return;
		} // end of if (pc.isCommand())
		XMPPResourceConnection conn = getXMPPResourceConnection(packet);
		if (conn == null
			&& (isBrokenPacket(packet) || processAdminsOrDomains(packet)
				|| sentToNextNode(packet))) {
			return;
		}
		processPacket(packet, conn);
	}

	protected void processClusterPacket(Packet packet) {
		ClusterElement clel = new ClusterElement(packet.getElement());
	}

	protected boolean sentToNextNode(Packet packet) {
		if (cluster_nodes.length > 0) {
			String sess_man_id = getComponentId();
			for (String cluster_node: cluster_nodes) {
				if (!cluster_node.equals(sess_man_id)) {
					ClusterElement clel = new ClusterElement(sess_man_id, cluster_node,
						StanzaType.set, packet.getElement());
					clel.addVisitedNode(sess_man_id);
					super.fastAddOutPacket(new Packet(clel.getClusterElement()));
					return true;
				}
			}
		}
		return false;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		cluster_nodes = (String[])props.get(CLUSTER_NODES_PROP_KEY);
	}

}
