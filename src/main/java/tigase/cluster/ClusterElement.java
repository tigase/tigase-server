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

import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;

import tigase.xml.Element;

/**
 * Class ClusterElement is a utility class for handling tigase cluster
 * specific packets. The cluster packet has the following form:
 * <pre>
 * <cluster xmlns="tigase:cluster" from="source" to="dest" type="set">
 *   <packets>
 *     <message xmlns="jabber:client" from="source-u" to="dest-x" type="chat">
 *       <body>Hello world!</body>
 *     </message>
 *   </packets>
 *   <data>
 *     <visited-nodes>
 *       <node-id>node1 JID address</node-id>
 *       <node-id>node2 JID address</node-id>
 *     </visited-nodes>
 *   </data>
 * </cluster>
 * </pre>
 *
 *
 * Created: Fri May  2 09:40:40 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterElement {

	public static final String XMLNS = "tigase:cluster";

	public static final String CLUSTER_EL_NAME = "cluster";
	public static final String CLUSTER_DATA_EL_NAME = "data";
	public static final String CLUSTER_PACKETS_EL_NAME = "packets";
	public static final String VISITED_NODES_EL_NAME = "visited-nodes";
	public static final String VISITED_NODES_EL_PATH =
    CLUSTER_EL_NAME + "/" + VISITED_NODES_EL_NAME;
	public static final String NODE_ID_EL_NAME = "node-id";

	/**
	 * Creates a new <code>ClusterElement</code> instance.
	 *
	 */
	public ClusterElement() {

	}

	public static Element createClusterElement(Set<String> visited_nodes) {
		Element cluster = new Element(CLUSTER_EL_NAME);
		cluster.setXMLNS(XMLNS);
		Element visited_nodes_el = new Element(VISITED_NODES_EL_NAME);
		cluster.addChild(visited_nodes_el);
		for (String node: visited_nodes) {
			visited_nodes_el.addChild(new Element(NODE_ID_EL_NAME, node));
		}
		return cluster;
	}

	public static void addVisitedNode(Element cluster, String node_id) {
		cluster.findChild(VISITED_NODES_EL_PATH)
      .addChild(new Element(NODE_ID_EL_NAME, node_id));
	}

	public static Set<String> getVisitedNodes(Element cluster) {
		if (cluster != null) {
			List<Element> nodes = cluster.getChildren(VISITED_NODES_EL_PATH);
			if (nodes != null) {
				Set<String> result = new LinkedHashSet<String>();
				for (Element node: nodes) {
					result.add(node.getCData());
				}
				return result;
			}
		}
		return null;
	}

}
