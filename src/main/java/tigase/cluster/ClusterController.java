/*
 *   Tigase Jabber/XMPP Server
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.Configurable;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class ClusterController here.
 *
 *
 * Created: Mon Jun  9 20:03:28 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterController extends AbstractComponentRegistrator<ClusteredComponent>
				implements Configurable {
	private static final Logger log = Logger.getLogger("tigase.cluster.ClusterController");

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_VAL = "localhost";

	//~--- fields ---------------------------------------------------------------

	private JID my_address = null;
	private JID my_hostname = null;

	// private ServiceEntity serviceEntity = null;
	// private ServiceEntity stats_modules = null;
	private Level statsLevel = Level.INFO;
	private String this_node = DNSResolver.getDefaultHostname();;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentAdded(ClusteredComponent component) {
		component.setClusterController(this);
		updateServiceDiscoveryItem(getName(),
															 component.getName(),
															 "Component: " + component.getName(),
															 true);

//  ServiceEntity item = serviceEntity.findNode(component.getName());
//  if (item == null) {
//    item = new ServiceEntity(getName(), component.getName(),
//      "Component: " + component.getName());
//    item.addFeatures(CMD_FEATURES);
//    item.addIdentities(new ServiceIdentity("automation", "command-node",
//          "Component: " + component.getName()));
//    serviceEntity.addItems(item);
//  }
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentRemoved(ClusteredComponent component) {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		String[] local_domains = DNSResolver.getDefHostNames();

		if (params.get(GEN_VIRT_HOSTS) != null) {
			local_domains = ((String) params.get(GEN_VIRT_HOSTS)).split(",");
		}

//  defs.put(LOCAL_DOMAINS_PROP_KEY, LOCAL_DOMAINS_PROP_VAL);
		defs.put(MY_DOMAIN_NAME_PROP_KEY, local_domains[0]);

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoCategoryType() {
		return "load";
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return "Server clustering";
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 *
	 * @return
	 */
	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof ClusteredComponent;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	public void nodeConnected(String node) {
		for (ClusteredComponent comp : components.values()) {
			comp.nodeConnected(node);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 */
	public void nodeDisconnected(String node) {
		for (ClusteredComponent comp : components.values()) {
			comp.nodeDisconnected(node);
		}
	}

//@Override
//public Element getDiscoInfo(String node, String jid, String from) {
//  if (jid != null && getName().equals(JIDUtils.getNodeNick(jid))) {
//    return serviceEntity.getDiscoInfo(node);
//  }
//  return null;
//}
//
//@Override
//public  List<Element> getDiscoFeatures(String from) { return null; }
//
//@Override
//public List<Element> getDiscoItems(String node, String jid, String from) {
//  if (getName().equals(JIDUtils.getNodeNick(jid))) {
//    return serviceEntity.getDiscoItems(node, jid);
//  } else {
//    return Arrays.asList(serviceEntity.getDiscoItem(null,
//        JIDUtils.getNodeID(getName(), jid)));
//  }
//}
//

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param results
	 */
	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		super.setName(name);

//  serviceEntity = new ServiceEntity(name, "load", "Server clustering");
//  serviceEntity.addIdentities(
//    new ServiceIdentity("component", "load", "Server clustering"));
//  serviceEntity.addFeatures(DEF_FEATURES);
//  serviceEntity.addFeatures(CMD_FEATURES);
	}

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);
		my_hostname = JID.jidInstanceNS((String) properties.get(MY_DOMAIN_NAME_PROP_KEY));
		my_address = JID.jidInstanceNS(getName(),
																	 (String) properties.get(MY_DOMAIN_NAME_PROP_KEY), null);
	}

	//~--- methods --------------------------------------------------------------

	private Packet sendClusterNotification(String msg, String subject, String nodes) {
		String message = msg;

		if (nodes != null) {
			message = msg + "\n";
		}

		int cnt = 0;

		for (String node : nodes.split(",")) {
			message += "" + (++cnt) + ". " + node + " connected to " + this_node;
		}

		Packet p_msg = Message.getMessage(my_address,
																			my_hostname,
																			StanzaType.normal,
																			message,
																			subject,
																			"xyz",
																			newPacketId(null));

		return p_msg;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
