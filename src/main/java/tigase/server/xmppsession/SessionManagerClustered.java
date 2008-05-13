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

package tigase.server.xmppsession;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import tigase.server.Packet;
import tigase.util.JIDUtils;

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

	public Packet initalPacketProcessin(Packet packet) {
		return packet;
	}

	public boolean checkNonSessionPacket(Packet packet) {
		// It might be a packet to another cluster node...
		String sess_man_id = JIDUtils.getNodeID(getName(), getDefHostName());
		if (cluster_nodes.length > 0 && !packet.isVisitedClusterNode(sess_man_id)) {
			packet.addVisitedClusterNode(sess_man_id);
			for (String cluster_node: cluster_nodes) {
				if (!packet.isVisitedClusterNode(cluster_node)) {
					packet.setTo(cluster_node);
					super.fastAddOutPacket(packet);
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
