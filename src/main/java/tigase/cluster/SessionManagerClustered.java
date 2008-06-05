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
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Arrays;

//import tigase.cluster.ClusterElement;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;

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

	private Set<String> cluster_nodes = new LinkedHashSet<String>();
	private Set<String> broken_nodes = new LinkedHashSet<String>();

	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Received packet: " + packet.toString());
		}
		if (packet.getElemName() == ClusterElement.CLUSTER_EL_NAME
			&& packet.getElement().getXMLNS() == ClusterElement.XMLNS) {
			processClusterPacket(packet);
			return;
		}

		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");
			// No more processing is needed for command packet
			return;
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
		switch (packet.getType()) {
		case set:
			List<Element> elems = clel.getDataPackets();
			if (elems != null && elems.size() > 0) {
				for (Element elem: elems) {
					XMPPResourceConnection conn = getXMPPResourceConnection(packet);
					if (conn != null || !sentToNextNode(clel)) {
						processPacket(new Packet(elem), conn);
					}
				}
			} else {
				log.finest("Empty packets list in the cluster packet: "
					+ packet.toString());
			}
			break;
		case get:

			break;
		case result:

			break;
		case error:
			// There might be many different errors...
			// But they all mean the cluster node is unreachable.
			// Let's leave custom handling each error type for later...
			String from = packet.getElemFrom();
			clel.addVisitedNode(from);
			if (cluster_nodes.remove(from)) {
				broken_nodes.add(from);
			}
			if (!sentToNextNode(clel)) {
				
			}
			break;
		default:
			break;
		}
	}

	protected boolean sentToNextNode(ClusterElement clel) {
		if (cluster_nodes.size() > 0) {
			String next_node = null;
			for (String cluster_node: cluster_nodes) {
				if (!clel.isVisitedNode(cluster_node)) {
					next_node = cluster_node;
					break;
				}
			}
			if (next_node == null) {
				String first_node = clel.getFirstNode();
				if (first_node == null) {
					log.warning("Something wrong - the first node should NOT be null here.");
				} else {
					if (!first_node.equals(getComponentId())) {
						next_node = first_node;
					}
				}
			}
			if (next_node != null) {
				ClusterElement next_clel = clel.nextClusterNode(next_node);
				super.fastAddOutPacket(new Packet(next_clel.getClusterElement()));
				return true;
			}
		}
		return false;
	}

	protected boolean sentToNextNode(Packet packet) {
		if (cluster_nodes.size() > 0) {
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
		String[] cl_nodes = (String[])props.get(CLUSTER_NODES_PROP_KEY);
		log.config("Cluster nodes loaded: " + Arrays.toString(cl_nodes));
		cluster_nodes = new LinkedHashSet<String>(Arrays.asList(cl_nodes));
		broken_nodes = new LinkedHashSet<String>();
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String)params.get(CLUSTER_NODES)).split(",");
			for (int i = 0; i < cl_nodes.length; i++) {
				if (cl_nodes[i].equals(JIDUtils.getNodeHost(cl_nodes[i]))) {
					cl_nodes[i] = DEF_SM_NAME + "@" + cl_nodes[i];
				}
			}
			props.put(CLUSTER_NODES_PROP_KEY, cl_nodes);
		} else {
			props.put(CLUSTER_NODES_PROP_KEY, new String[] {getComponentId()});
		}
		return props;
	}

}
