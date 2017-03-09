/*
 * ClusterElement.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import tigase.server.Priority;

/**
 * Class ClusterElement is a utility class for handling tigase cluster specific
 * packets. The cluster packet has the following form:
 *
 * {@code
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
 * }
 *
 * If none of nodes could process the packet it goes back to the first node as
 * this node is the most likely to process the packet correctly.
 *
 *
 * Created: Fri May 2 09:40:40 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterElement {
	/** Field description */
	public static final String CLUSTER_CONTROL_EL_NAME = "control";

	/** Field description */
	public static final String CLUSTER_DATA_EL_NAME = "data";

	/** Field description */
	public static final String CLUSTER_EL_NAME = "cluster";

	/** Field description */
	public static final String CLUSTER_METHOD_EL_NAME = "method-call";

	/** Field description */
	public static final String CLUSTER_METHOD_PAR_EL_NAME = "par";

	/** Field description */
	public static final String CLUSTER_METHOD_RESULTS_EL_NAME = "results";

	/** Field description */
	public static final String CLUSTER_METHOD_RESULTS_VAL_EL_NAME = "val";

	/** Field description */
	public static final String CLUSTER_NAME_ATTR = "name";

	/** Field description */
	public static final String FIRST_NODE_EL_NAME = "first-node";

	/** Field description */
	public static final String NODE_ID_EL_NAME = "node-id";

	/** Field description */
	public static final String VISITED_NODES_EL_NAME = "visited-nodes";

	/** Field description */
	public static final String XMLNS = "tigase:cluster";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.cluster.ClusterElement");

	/** Field description */
	public static final String[] VISITED_NODES_PATH = { CLUSTER_EL_NAME,
					CLUSTER_CONTROL_EL_NAME, VISITED_NODES_EL_NAME };

	// public static final String PACKET_FROM_ATTR_NAME = "packet-from";

	/** Field description */
	public static final String[] FIRST_NODE_PATH = { CLUSTER_EL_NAME,
					CLUSTER_CONTROL_EL_NAME, FIRST_NODE_EL_NAME };

	/** Field description */
	public static final String[] CLUSTER_METHOD_RESULTS_PATH = { CLUSTER_EL_NAME,
					CLUSTER_CONTROL_EL_NAME, CLUSTER_METHOD_EL_NAME,
					CLUSTER_METHOD_RESULTS_EL_NAME };

	/** Field description */
	public static final String[] CLUSTER_METHOD_PATH = { CLUSTER_EL_NAME,
					CLUSTER_CONTROL_EL_NAME, CLUSTER_METHOD_EL_NAME };

	/** Field description */
	public static final String[] CLUSTER_DATA_PATH = { CLUSTER_EL_NAME,
					CLUSTER_DATA_EL_NAME };

	/** Field description */
	public static final String[] CLUSTER_CONTROL_PATH = { CLUSTER_EL_NAME,
					CLUSTER_CONTROL_EL_NAME };

	//~--- fields ---------------------------------------------------------------

	private Element elem                       = null;
	private JID first_node                     = null;
	private String method_name                 = null;
	private Map<String, String> method_params  = null;
	private Map<String, String> method_results = null;
	private Queue<Element> packets             = null;
	private Priority priority				   = null;
	private Set<JID> visited_nodes             = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>ClusterElement</code> instance.
	 *
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

	/**
	 * Constructs ...
	 *
	 *
	 * @param from
	 * @param to
	 * @param type
	 * @param packet
	 */
	public ClusterElement(JID from, JID to, StanzaType type, Packet packet) {
		if (packet != null) {
			packets       = new ArrayDeque<Element>();
			visited_nodes = new LinkedHashSet<JID>();
			elem          = createClusterElement(from, to, type, packet.getFrom().toString());
			if (packet.getElement().getXMLNS() == null) {
				packet.getElement().setXMLNS("jabber:client");
			}
			addDataPacket(packet);
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param from
	 * @param to
	 * @param type
	 *
	 * 
	 */
	public static Element clusterElement(JID from, JID to, StanzaType type) {
		Element cluster_el = new Element(CLUSTER_EL_NAME, new String[] { "from", "to",
						"type" }, new String[] { from.toString(), to.toString(), type.toString() });

		cluster_el.setXMLNS(XMLNS);
		cluster_el.addChild(new Element(CLUSTER_CONTROL_EL_NAME,
																		new Element[] { new Element(VISITED_NODES_EL_NAME) },
																		null, null));

		return cluster_el;
	}

	/**
	 * Method description
	 *
	 *
	 * @param from
	 * @param to
	 * @param type
	 * @param packet_from
	 *
	 * 
	 */
	public static Element createClusterElement(JID from, JID to, StanzaType type,
					String packet_from) {
		Element cluster_el = clusterElement(from, to, type);

		cluster_el.addChild(new Element(CLUSTER_DATA_EL_NAME));

		// new String[] {PACKET_FROM_ATTR_NAME}, new String[] {packet_from}));
		return cluster_el;
	}

	/**
	 * Method description
	 *
	 *
	 * @param from
	 * @param to
	 * @param type
	 * @param method_name
	 * @param params
	 *
	 * 
	 */
	public static ClusterElement createClusterMethodCall(JID from, JID to, StanzaType type,
					String method_name, Map<String, String> params) {
		Element cluster_el  = clusterElement(from, to, type);
		Element method_call = new Element(CLUSTER_METHOD_EL_NAME,
																			new String[] { CLUSTER_NAME_ATTR },
																			new String[] { method_name });

		if (params != null) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				method_call.addChild(new Element(CLUSTER_METHOD_PAR_EL_NAME, entry.getValue(),
																				 new String[] { CLUSTER_NAME_ATTR },
																				 new String[] { entry.getKey() }));
			}
		}
		cluster_el.findChildStaticStr(CLUSTER_CONTROL_PATH).addChild(method_call);

		ClusterElement result_cl = new ClusterElement(cluster_el);

		result_cl.addVisitedNode(from);

		return result_cl;
	}

	/**
	 * Method description
	 *
	 *
	 * @param clel
	 * @param cluster_nodes
	 * @param comp_id
	 *
	 * 
	 */
	public static ClusterElement createForNextNode(ClusterElement clel,
					List<JID> cluster_nodes, JID comp_id) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Calculating a next node from nodes: " + ((cluster_nodes != null)
							? cluster_nodes.toString()
							: "null"));
		}
		if ((cluster_nodes != null) && (cluster_nodes.size() > 0)) {
			JID next_node = null;

			for (JID cluster_node : cluster_nodes) {
				if (!clel.isVisitedNode(cluster_node) &&!cluster_node.equals(comp_id)) {
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
	 * Method description
	 *
	 *
	 * @param packet
	 */
	public void addDataPacket(Packet packet) {
		addDataPacket(packet.getElement());
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param packets
	 */
	public void addDataPackets(Queue<Element> packets) {
		if (packets != null) {
			for (Element elem : packets) {
				addDataPacket(elem);
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param val
	 */
	public void addMethodResult(String key, String val) {
		Element res = elem.findChildStaticStr(CLUSTER_METHOD_RESULTS_PATH);

		if (res == null) {
			res = new Element(CLUSTER_METHOD_RESULTS_EL_NAME);
			elem.findChildStaticStr(CLUSTER_METHOD_PATH).addChild(res);
		}
		res.addChild(new Element(CLUSTER_METHOD_RESULTS_VAL_EL_NAME, val,
														 new String[] { CLUSTER_NAME_ATTR }, new String[] { key }));
		if (method_results == null) {
			method_results = new LinkedHashMap<String, String>();
		}
		method_results.put(key, val);
	}

	/**
	 * Method description
	 *
	 *
	 * @param node_id
	 */
	public void addVisitedNode(JID node_id) {
		if (visited_nodes.size() == 0) {
			first_node = node_id;
			elem.findChildStaticStr(CLUSTER_CONTROL_PATH).addChild(
					new Element(FIRST_NODE_EL_NAME, node_id.toString()));
		}
		if (visited_nodes.add(node_id)) {
			elem.findChildStaticStr(VISITED_NODES_PATH).addChild(new Element(NODE_ID_EL_NAME,
							node_id.toString()));
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param nodes
	 */
	public void addVisitedNodes(Set<JID> nodes) {
		if (nodes != null) {
			for (JID node : nodes) {
				addVisitedNode(node);
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param from
	 * @param type
	 * @param results
	 *
	 * 
	 */
	public ClusterElement createMethodResponse(JID from, StanzaType type,
					Map<String, String> results) {
		return createMethodResponse(from, null, type, results);
	}

	/**
	 * Method description
	 *
	 *
	 * @param from
	 * @param to
	 * @param type
	 * @param results
	 *
	 * 
	 */
	public ClusterElement createMethodResponse(JID from, JID to, StanzaType type,
					Map<String, String> results) {
		Element result_el = elem.clone();

		result_el.setAttribute("from", from.toString());
		result_el.setAttribute("to", ((to != null)
																	? to.toString()
																	: first_node.toString()));
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

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Map<String, String> getAllMethodParams() {
		return method_params;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Map<String, String> getAllMethodResults() {
		return method_results;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param id
	 * 
	 */
	public Element getClusterElement(String id) {
		elem.setAttribute("id", id);

		return elem;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Queue<Element> getDataPackets() {
		return packets;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public JID getFirstNode() {
		return first_node;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getMethodName() {
		return method_name;
	}

	/**
	 * Method description
	 *
	 *
	 * @param par_name
	 *
	 * 
	 */
	public String getMethodParam(String par_name) {
		return (method_params == null)
					 ? null
					 : method_params.get(par_name);
	}

	/**
	 * Method description
	 *
	 *
	 * @param par_name
	 * @param def
	 *
	 * 
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param val_name
	 *
	 * 
	 */
	public String getMethodResultVal(String val_name) {
		return (method_results == null)
					 ? null
					 : method_results.get(val_name);
	}

	/**
	 * Method description
	 *
	 *
	 * @param val_name
	 * @param def
	 *
	 * 
	 */
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

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Set<JID> getVisitedNodes() {
		return visited_nodes;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node_id
	 *
	 * 
	 */
	public boolean isVisitedNode(JID node_id) {
		return visited_nodes.contains(node_id);
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
		this.elem.setAttribute(Packet.PRIORITY_ATT, priority.name());
	}
	
	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param node_id
	 *
	 * 
	 */
	public ClusterElement nextClusterNode(JID node_id) {
		Element next_el = elem.clone();
		String from     = elem.getAttributeStaticStr(Packet.TO_ATT);

		next_el.setAttribute("from", from);
		next_el.setAttribute("to", node_id.toString());

		// next_el.setAttribute("type", StanzaType.set.toString());
		ClusterElement next_cl = new ClusterElement(next_el);

		return next_cl;
	}

	/**
	 * Method description
	 *
	 *
	 * @param method_call
	 */
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


//~ Formatted in Tigase Code Convention on 13/02/20
