/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.cluster.api;

import tigase.server.Packet;
import tigase.server.Priority;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class ClusterElement is a utility class for handling tigase cluster specific packets. The cluster packet has the
 * following form:
 * <br>
 * {@code <cluster xmlns="tigase:cluster" from="source" to="dest" type="set"> <data> <message xmlns="jabber:client"
 * from="source-u" to="dest-x" type="chat"> <body>Hello world!</body> </message> </data> <control> <first-node>node1 JID
 * address</first-node> <visited-nodes> <node-id>node1 JID address</node-id> <node-id>node2 JID address</node-id>
 * </visited-nodes> <method-call name="method name"> <par name="param1 name">value</par> <par name="param2
 * name">value</par> <results> <val name="val1 name">value</var> <val name="val2 name">value</var> </results>
 * </method-call> </control> </cluster> }
 * <br>
 * If none of nodes could process the packet it goes back to the first node as this node is the most likely to process
 * the packet correctly.
 * <br>
 * Created: Fri May 2 09:40:40 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterElement {

	public static final String CLUSTER_CONTROL_EL_NAME = "control";

	public static final String CLUSTER_DATA_EL_NAME = "data";

	public static final String CLUSTER_EL_NAME = "cluster";

	public static final String CLUSTER_METHOD_EL_NAME = "method-call";

	public static final String CLUSTER_METHOD_PAR_EL_NAME = "par";

	public static final String CLUSTER_METHOD_RESULTS_EL_NAME = "results";

	public static final String CLUSTER_METHOD_RESULTS_VAL_EL_NAME = "val";

	public static final String CLUSTER_NAME_ATTR = "name";

	public static final String FIRST_NODE_EL_NAME = "first-node";

	public static final String NODE_ID_EL_NAME = "node-id";

	public static final String VISITED_NODES_EL_NAME = "visited-nodes";

	public static final String XMLNS = "tigase:cluster";
	public static final String[] VISITED_NODES_PATH = {CLUSTER_EL_NAME, CLUSTER_CONTROL_EL_NAME, VISITED_NODES_EL_NAME};
	public static final String[] FIRST_NODE_PATH = {CLUSTER_EL_NAME, CLUSTER_CONTROL_EL_NAME, FIRST_NODE_EL_NAME};

	// public static final String PACKET_FROM_ATTR_NAME = "packet-from";
	public static final String[] CLUSTER_METHOD_RESULTS_PATH = {CLUSTER_EL_NAME, CLUSTER_CONTROL_EL_NAME,
																CLUSTER_METHOD_EL_NAME, CLUSTER_METHOD_RESULTS_EL_NAME};
	public static final String[] CLUSTER_METHOD_PATH = {CLUSTER_EL_NAME, CLUSTER_CONTROL_EL_NAME,
														CLUSTER_METHOD_EL_NAME};
	public static final String[] CLUSTER_DATA_PATH = {CLUSTER_EL_NAME, CLUSTER_DATA_EL_NAME};
	public static final String[] CLUSTER_CONTROL_PATH = {CLUSTER_EL_NAME, CLUSTER_CONTROL_EL_NAME};
	private static final Logger log = Logger.getLogger("tigase.cluster.ClusterElement");

	private Element elem = null;
	private JID first_node = null;
	private String method_name = null;
	private Map<String, String> method_params = null;
	private Map<String, String> method_results = null;
	private Queue<Element> packets = null;
	private Priority priority = null;
	private Set<JID> visited_nodes = null;

	public static Element clusterElement(JID from, JID to, StanzaType type) {
		Element cluster_el = new Element(CLUSTER_EL_NAME, new String[]{"from", "to", "type"},
										 new String[]{from.toString(), to.toString(), type.toString()});

		cluster_el.setXMLNS(XMLNS);
		cluster_el.addChild(
				new Element(CLUSTER_CONTROL_EL_NAME, new Element[]{new Element(VISITED_NODES_EL_NAME)}, null, null));

		return cluster_el;
	}

	public static Element createClusterElement(JID from, JID to, StanzaType type, String packet_from) {
		Element cluster_el = clusterElement(from, to, type);

		cluster_el.addChild(new Element(CLUSTER_DATA_EL_NAME));

		// new String[] {PACKET_FROM_ATTR_NAME}, new String[] {packet_from}));
		return cluster_el;
	}

	public static ClusterElement createClusterMethodCall(JID from, JID to, StanzaType type, String method_name,
														 Map<String, String> params) {
		Element cluster_el = clusterElement(from, to, type);
		Element method_call = new Element(CLUSTER_METHOD_EL_NAME, new String[]{CLUSTER_NAME_ATTR},
										  new String[]{method_name});

		if (params != null) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				method_call.addChild(
						new Element(CLUSTER_METHOD_PAR_EL_NAME, entry.getValue(), new String[]{CLUSTER_NAME_ATTR},
									new String[]{entry.getKey()}));
			}
		}
		cluster_el.findChildStaticStr(CLUSTER_CONTROL_PATH).addChild(method_call);

		ClusterElement result_cl = new ClusterElement(cluster_el);

		result_cl.addVisitedNode(from);

		return result_cl;
	}

	public static ClusterElement createForNextNode(ClusterElement clel, List<JID> cluster_nodes, JID comp_id) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Calculating a next node from nodes: " +
							   ((cluster_nodes != null) ? cluster_nodes.toString() : "null"));
		}
		if ((cluster_nodes != null) && (cluster_nodes.size() > 0)) {
			JID next_node = null;

			for (JID cluster_node : cluster_nodes) {
				if (!clel.isVisitedNode(cluster_node) && !cluster_node.equals(comp_id)) {
					next_node = cluster_node;
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Found next cluster node: " + next_node);
					}

					break;
				}
			}
			if (next_node != null) {
				ClusterElement result = clel.nextClusterNode(next_node);

				result.addVisitedNode(comp_id);

				return result;
			}
		}

		return null;
	}

	/**
	 * Creates a new <code>ClusterElement</code> instance.
	 *
	 * @param elem
	 */
	public ClusterElement(Element elem) {
		this.elem = elem;
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Parsing cluster element: " + elem.toString());
		}

		List<Element> children = elem.getChildrenStaticStr(CLUSTER_DATA_PATH);

		if ((children != null) && (children.size() > 0)) {
			packets = new ArrayDeque<Element>(children);
		}

		String fNode = elem.getCDataStaticStr(FIRST_NODE_PATH);

		if (fNode != null) {
			first_node = JID.jidInstanceNS(fNode);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("First node found: " + first_node);
		}
		visited_nodes = new LinkedHashSet<JID>();

		List<Element> nodes = elem.getChildrenStaticStr(VISITED_NODES_PATH);

		if (nodes != null) {
			int cnt = 0;

			for (Element node : nodes) {
				visited_nodes.add(JID.jidInstanceNS(node.getCData()));
				++cnt;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Found and added visited nodes: " + cnt);
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("No visited nodes found");
			}
		}

		String priorityStr = elem.getAttributeStaticStr(Packet.PRIORITY_ATT);
		if (priorityStr != null) {
			priority = Priority.valueOf(priorityStr);
		}

		Element method_call = elem.findChildStaticStr(CLUSTER_METHOD_PATH);

		if (method_call != null) {
			parseMethodCall(method_call);
		}
	}

	public ClusterElement(JID from, JID to, StanzaType type, Packet packet) {
		if (packet != null) {
			packets = new ArrayDeque<Element>();
			visited_nodes = new LinkedHashSet<JID>();
			elem = createClusterElement(from, to, type, packet.getFrom().toString());
			if (packet.getElement().getXMLNS() == null) {
				packet.getElement().setXMLNS("jabber:client");
			}
			addDataPacket(packet);
		}
	}

	public void addDataPacket(Packet packet) {
		addDataPacket(packet.getElement());
	}

	public void addDataPacket(Element packet) {
		if (packets == null) {
			packets = new ArrayDeque<Element>();
		}
		packets.offer(packet);
		if (elem.findChildStaticStr(CLUSTER_DATA_PATH) == null) {
			elem.addChild(new Element(CLUSTER_DATA_EL_NAME));
		}
		elem.findChildStaticStr(CLUSTER_DATA_PATH).addChild(packet);
	}

	public void addDataPackets(Queue<Element> packets) {
		if (packets != null) {
			for (Element elem : packets) {
				addDataPacket(elem);
			}
		}
	}
	
	public void addMethodResult(String key, String val) {
		Element res = elem.findChildStaticStr(CLUSTER_METHOD_RESULTS_PATH);

		if (res == null) {
			res = new Element(CLUSTER_METHOD_RESULTS_EL_NAME);
			elem.findChildStaticStr(CLUSTER_METHOD_PATH).addChild(res);
		}
		res.addChild(new Element(CLUSTER_METHOD_RESULTS_VAL_EL_NAME, val, new String[]{CLUSTER_NAME_ATTR},
								 new String[]{key}));
		if (method_results == null) {
			method_results = new LinkedHashMap<String, String>();
		}
		method_results.put(key, val);
	}

	public void addVisitedNode(JID node_id) {
		if (visited_nodes.size() == 0) {
			first_node = node_id;
			elem.findChildStaticStr(CLUSTER_CONTROL_PATH).addChild(new Element(FIRST_NODE_EL_NAME, node_id.toString()));
		}
		if (visited_nodes.add(node_id)) {
			elem.findChildStaticStr(VISITED_NODES_PATH).addChild(new Element(NODE_ID_EL_NAME, node_id.toString()));
		}
	}

	public void addVisitedNodes(Set<JID> nodes) {
		if (nodes != null) {
			for (JID node : nodes) {
				addVisitedNode(node);
			}
		}
	}

	public ClusterElement createMethodResponse(JID from, StanzaType type, Map<String, String> results) {
		return createMethodResponse(from, null, type, results);
	}

	public ClusterElement createMethodResponse(JID from, JID to, StanzaType type, Map<String, String> results) {
		Element result_el = elem.clone();

		result_el.setAttribute("from", from.toString());
		result_el.setAttribute("to", ((to != null) ? to.toString() : first_node.toString()));
		result_el.setAttribute("type", type.name());

		Element res = new Element(CLUSTER_METHOD_RESULTS_EL_NAME);

		result_el.findChildStaticStr(CLUSTER_METHOD_PATH).addChild(res);

		ClusterElement result_cl = new ClusterElement(result_el);

		if (results != null) {
			for (Map.Entry<String, String> entry : results.entrySet()) {
				result_cl.addMethodResult(entry.getKey(), entry.getValue());
			}
		}

		return result_cl;
	}

	public Map<String, String> getAllMethodParams() {
		return method_params;
	}

	public Map<String, String> getAllMethodResults() {
		return method_results;
	}

	public Element getClusterElement(String id) {
		elem.setAttribute("id", id);

		return elem;
	}

	public Queue<Element> getDataPackets() {
		return packets;
	}

	public JID getFirstNode() {
		return first_node;
	}

	public String getMethodName() {
		return method_name;
	}

	public String getMethodParam(String par_name) {
		return (method_params == null) ? null : method_params.get(par_name);
	}

	public long getMethodParam(String par_name, long def) {
		String val_str = getMethodParam(par_name);

		if (val_str == null) {
			return def;
		} else {
			try {
				return Long.parseLong(val_str);
			} catch (NumberFormatException e) {
				return def;
			}
		}
	}

	public String getMethodResultVal(String val_name) {
		return (method_results == null) ? null : method_results.get(val_name);
	}

	public long getMethodResultVal(String val_name, long def) {
		String val_str = getMethodResultVal(val_name);

		if (val_str == null) {
			return def;
		} else {
			try {
				return Long.parseLong(val_str);
			} catch (NumberFormatException e) {
				return def;
			}
		}
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
		this.elem.setAttribute(Packet.PRIORITY_ATT, priority.name());
	}

	public Set<JID> getVisitedNodes() {
		return visited_nodes;
	}

	public boolean isVisitedNode(JID node_id) {
		return visited_nodes.contains(node_id);
	}

	public ClusterElement nextClusterNode(JID node_id) {
		Element next_el = elem.clone();
		String from = elem.getAttributeStaticStr(Packet.TO_ATT);

		next_el.setAttribute("from", from);
		next_el.setAttribute("to", node_id.toString());

		// next_el.setAttribute("type", StanzaType.set.toString());
		ClusterElement next_cl = new ClusterElement(next_el);

		return next_cl;
	}

	protected void parseMethodCall(Element method_call) {
		method_name = method_call.getAttributeStaticStr(CLUSTER_NAME_ATTR);
		if (method_name != null) {
			method_name = method_name.intern();
		}
		method_params = new LinkedHashMap<String, String>();

		List<Element> children = method_call.getChildren();

		if (children != null) {
			for (Element child : children) {
				if (child.getName() == CLUSTER_METHOD_PAR_EL_NAME) {
					String par_name = child.getAttributeStaticStr(CLUSTER_NAME_ATTR);

					method_params.put(par_name, child.getCData());
				}
				if (child.getName() == CLUSTER_METHOD_RESULTS_EL_NAME) {
					if (method_results == null) {
						method_results = new LinkedHashMap<String, String>();
					}

					List<Element> res_children = child.getChildren();

					if (res_children != null) {
						for (Element res_child : res_children) {
							if (res_child.getName() == CLUSTER_METHOD_RESULTS_VAL_EL_NAME) {
								String val_name = res_child.getAttributeStaticStr(CLUSTER_NAME_ATTR);

								method_results.put(val_name, res_child.getCData());
							}
						}
					}
				}
			}
		}
	}
}

