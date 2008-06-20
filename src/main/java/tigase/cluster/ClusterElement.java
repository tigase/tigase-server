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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import tigase.xmpp.StanzaType;
import tigase.xml.Element;
import tigase.server.Packet;

/**
 * Class ClusterElement is a utility class for handling tigase cluster
 * specific packets. The cluster packet has the following form:
 * <pre>
 * <cluster xmlns="tigase:cluster" from="source" to="dest" type="set">
 *   <data>
 *     <message xmlns="jabber:client" from="source-u" to="dest-x" type="chat">
 *       <body>Hello world!</body>
 *     </message>
 *   </data>
 *   <control>
 *     <first-node>node1 JID address</first-node>
 *     <visited-nodes>
 *       <node-id>node1 JID address</node-id>
 *       <node-id>node2 JID address</node-id>
 *     </visited-nodes>
 *     <method-call name="method name">
 *       <par name="param1 name">value</par>
 *       <par name="param2 name">value</par>
 *       <results>
 *         <val name="val1 name">value</var>
 *         <val name="val2 name">value</var>
 *       </results>
 *     </method-call>
 *   </control>
 * </cluster>
 * </pre>
 * If none of nodes could process the packet it goes back to the first node
 * as this node is the most likely to process the packet correctly.
 *
 *
 * Created: Fri May  2 09:40:40 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterElement {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.cluster.ClusterElement");

	public static final String XMLNS = "tigase:cluster";

	public static final String CLUSTER_EL_NAME = "cluster";
	public static final String CLUSTER_CONTROL_EL_NAME = "control";
	public static final String CLUSTER_CONTROL_PATH =
    "/" + CLUSTER_EL_NAME + "/" + CLUSTER_CONTROL_EL_NAME;
	public static final String CLUSTER_DATA_EL_NAME = "data";
	public static final String CLUSTER_DATA_PATH =
    "/" + CLUSTER_EL_NAME + "/" + CLUSTER_DATA_EL_NAME;
	public static final String CLUSTER_METHOD_EL_NAME = "method-call";
	public static final String CLUSTER_METHOD_PATH =
    "/" + CLUSTER_CONTROL_PATH + "/" + CLUSTER_METHOD_EL_NAME;
	public static final String CLUSTER_NAME_ATTR = "name";
	public static final String CLUSTER_METHOD_PAR_EL_NAME = "par";
	public static final String CLUSTER_METHOD_RESULTS_EL_NAME = "results";
	public static final String CLUSTER_METHOD_RESULTS_VAL_EL_NAME = "val";

	public static final String VISITED_NODES_EL_NAME = "visited-nodes";
	public static final String FIRST_NODE_EL_NAME = "first-node";
	//	public static final String PACKET_FROM_ATTR_NAME = "packet-from";
	public static final String FIRST_NODE_PATH =
    CLUSTER_CONTROL_PATH + "/" + FIRST_NODE_EL_NAME;
	public static final String VISITED_NODES_PATH =
    CLUSTER_CONTROL_PATH + "/" + VISITED_NODES_EL_NAME;
	public static final String NODE_ID_EL_NAME = "node-id";

	private Element elem = null;
	private List<Element> packets = null;
	private Set<String> visited_nodes = null;
	private String first_node = null;
	private String method_name = null;
	private Map<String, String> method_results = null;
	private Map<String, String> method_params = null;

	/**
	 * Creates a new <code>ClusterElement</code> instance.
	 *
	 */
	public ClusterElement(Element elem) {
		this.elem = elem;
		packets = elem.getChildren(CLUSTER_DATA_PATH);
		first_node = elem.getCData(FIRST_NODE_PATH);
		visited_nodes = new LinkedHashSet<String>();
		List<Element> nodes = elem.getChildren(VISITED_NODES_PATH);
		if (nodes != null) {
			for (Element node: nodes) {
				visited_nodes.add(node.getCData());
			}
		}
		Element method_call = elem.findChild(CLUSTER_METHOD_PATH);
		if (method_call != null) {
			parseMethodCall(method_call);
		}
	}

	public ClusterElement(String from, String to, StanzaType type, Packet packet) {
		packets = new ArrayList<Element>();
		visited_nodes = new LinkedHashSet<String>();
		elem = createClusterElement(from, to, type.toString(), packet.getFrom());
		if (packet != null) {
			if (packet.getElement().getXMLNS() == null) {
				packet.getElement().setXMLNS("jabber:client");
			}
			addDataPacket(packet);
		}
	}

	public static Element clusterElement(String from, String to, String type) {
		Element cluster_el = new Element(CLUSTER_EL_NAME,
			new String[] {"from", "to", "type"},
			new String[] {from, to, type});
		cluster_el.setXMLNS(XMLNS);
		cluster_el.addChild(new Element(CLUSTER_CONTROL_EL_NAME,
				new Element[] {new Element(VISITED_NODES_EL_NAME)}, null, null));
		return cluster_el;
	}

	public static Element createClusterElement(String from, String to,
		String type, String packet_from) {
		Element cluster_el = clusterElement(from, to, type);
		cluster_el.addChild(new Element(CLUSTER_DATA_EL_NAME));
// 				new String[] {PACKET_FROM_ATTR_NAME}, new String[] {packet_from}));
		return cluster_el;
	}

	public static ClusterElement createClusterMethodCall(String from, String to,
		String type, String method_name, Map<String, String> params) {
		Element cluster_el = clusterElement(from, to, type);
		Element method_call = new Element(CLUSTER_METHOD_EL_NAME,
			new String [] {CLUSTER_NAME_ATTR}, new String[] {method_name});
		if (params != null) {
			for (Map.Entry<String, String> entry: params.entrySet()) {
				method_call.addChild(new Element(CLUSTER_METHOD_PAR_EL_NAME,
						entry.getValue(),
						new String[] {CLUSTER_NAME_ATTR}, new String[] {entry.getKey()}));
			}
		}
		cluster_el.findChild(CLUSTER_CONTROL_PATH).addChild(method_call);
		ClusterElement result_cl = new ClusterElement(cluster_el);
		result_cl.addVisitedNode(from);
		return result_cl;
	}

	public ClusterElement createMethodResponse(String from, String type,
		Map<String, String> results) {
		Element result_el = elem.clone();
		result_el.setAttribute("from", from);
		result_el.setAttribute("to", first_node);
		result_el.setAttribute("type", type);
		if (results != null) {
			Element results_el = new Element(CLUSTER_METHOD_RESULTS_EL_NAME);
			for (Map.Entry<String, String> entry: results.entrySet()) {
				results_el.addChild(new Element(CLUSTER_METHOD_RESULTS_VAL_EL_NAME,
						entry.getValue(),
						new String[] {CLUSTER_NAME_ATTR}, new String[] {entry.getKey()}));
			}
			result_el.findChild(CLUSTER_METHOD_PATH).addChild(results_el);
		}
		ClusterElement result_cl = new ClusterElement(result_el);
		return result_cl;
	}

	public static ClusterElement createForNextNode(ClusterElement clel,
		Set<String> cluster_nodes, String comp_id) {
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
					if (!first_node.equals(comp_id)) {
						next_node = first_node;
					}
				}
			}
			if (next_node != null) {
				return clel.nextClusterNode(next_node);
			}
		}
		return null;
	}

	protected void parseMethodCall(Element method_call) {
		method_name = method_call.getAttribute(CLUSTER_NAME_ATTR);
		method_params = new LinkedHashMap<String, String>();
		List<Element> children = method_call.getChildren();
		for (Element child: children) {
			if (child.getName() == CLUSTER_METHOD_PAR_EL_NAME) {
				String par_name = child.getAttribute(CLUSTER_NAME_ATTR);
				method_params.put(par_name, child.getCData());
			}
			if (child.getName() == CLUSTER_METHOD_RESULTS_EL_NAME) {
				if (method_results == null) {
					method_results = new LinkedHashMap<String, String>();
				}
				List<Element> res_children = child.getChildren();
				for (Element res_child: res_children) {
					if (res_child.getName() == CLUSTER_METHOD_RESULTS_VAL_EL_NAME) {
						String val_name = res_child.getAttribute(CLUSTER_NAME_ATTR);
						method_results.put(val_name, res_child.getCData());
					}
				}
			}
		}
	}

	public String getMethodName() {
		return method_name;
	}

	public String getMethodParam(String par_name) {
		return method_params == null ? null : method_params.get(par_name);
	}

	public Map<String, String> getAllMethodParams() {
		return method_params;
	}

	public String getMethodResultVal(String val_name) {
		return method_results == null ? null : method_results.get(val_name);
	}

	public Map<String, String> getAllMethodResults() {
		return method_results;
	}

	public String getFirstNode() {
		return first_node;
	}

	public ClusterElement nextClusterNode(String node_id) {
		Element next_el = elem.clone();
		String from = elem.getAttribute("to");
		next_el.setAttribute("from", from);
		next_el.setAttribute("to", node_id);
		next_el.setAttribute("type", StanzaType.set.toString());
		ClusterElement next_cl = new ClusterElement(next_el);
		return next_cl;
	}

	public void addDataPacket(Packet packet) {
		packets.add(packet.getElement());
		elem.findChild(CLUSTER_DATA_PATH).addChild(packet.getElement());
	}

	public List<Element> getDataPackets() {
		return packets;
	}

// 	public String getDataPacketFrom() {
// 		return elem.getAttribute(CLUSTER_DATA_PATH, PACKET_FROM_ATTR_NAME);
// 	}

	public Element getClusterElement() {
		return elem;
	}

	public void addVisitedNode(String node_id) {
		if (visited_nodes.size() == 0) {
			first_node = node_id;
			elem.findChild(CLUSTER_CONTROL_PATH)
        .addChild(new Element(FIRST_NODE_EL_NAME, node_id));
		}
		visited_nodes.add(node_id);
		elem.findChild(VISITED_NODES_PATH)
      .addChild(new Element(NODE_ID_EL_NAME, node_id));
	}

	public boolean isVisitedNode(String node_id) {
		return visited_nodes.contains(node_id);
	}

	public Set<String> getVisitedNodes() {
		return visited_nodes;
	}

}
