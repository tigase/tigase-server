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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.Command;
import tigase.server.MessageRouter;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.StanzaType;
import tigase.util.ElementUtils;
import tigase.util.JIDUtils;

/**
 * Describe class ClusterController here.
 *
 *
 * Created: Mon Jun  9 20:03:28 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClusterController
	extends AbstractComponentRegistrator<ClusteredComponent>
	implements XMPPService {

  private static final Logger log =
		Logger.getLogger("tigase.cluster.ClusterController");

	private ServiceEntity serviceEntity = null;
	//private ServiceEntity stats_modules = null;
	private Level statsLevel = Level.INFO;

	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, "load", "Server clustering");
		serviceEntity.addIdentities(
			new ServiceIdentity("component", "load", "Server clustering"));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures(CMD_FEATURES);
	}

	public void componentAdded(ClusteredComponent component) {
		ServiceEntity item = serviceEntity.findNode(component.getName());
		if (item == null) {
			item = new ServiceEntity(getName(), component.getName(),
				"Component: " + component.getName());
			item.addFeatures(CMD_FEATURES);
			item.addIdentities(new ServiceIdentity("automation", "command-node",
						"Component: " + component.getName()));
			serviceEntity.addItems(item);
		}
	}

	public boolean isCorrectType(ServerComponent component) {
		return component instanceof ClusteredComponent;
	}

	public void componentRemoved(ClusteredComponent component) {}

	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && getName().equals(JIDUtils.getNodeNick(jid))) {
			return serviceEntity.getDiscoInfo(node);
		}
		return null;
	}

	public 	List<Element> getDiscoFeatures() { return null; }

	public List<Element> getDiscoItems(String node, String jid) {
		if (getName().equals(JIDUtils.getNodeNick(jid))) {
			return serviceEntity.getDiscoItems(node, jid);
		} else {
			return Arrays.asList(serviceEntity.getDiscoItem(null,
					JIDUtils.getNodeID(getName(), jid)));
		}
	}

	public void processPacket(final Packet packet, final Queue<Packet> results) {
		if (packet.getElement().getName() == ClusterElement.CLUSTER_EL_NAME) {
			ClusterElement clem = new ClusterElement(packet.getElement());
			if (clem.getMethodName() != null
				&& clem.getMethodName().equals(ClusterMethods.UPDATE_NODES.toString())) {
				String connected_nodes = clem.getMethodParam("connected");
				String disconnected_nodes = clem.getMethodParam("disconnected");
				for (ClusteredComponent comp: components.values()) {
					if (connected_nodes != null) {
						comp.nodesConnected(new
							LinkedHashSet<String>(Arrays.asList(connected_nodes.split(","))));
					}
					if (disconnected_nodes != null) {
						comp.nodesDisconnected(new
							LinkedHashSet<String>(Arrays.asList(disconnected_nodes.split(","))));
					}
				}
			}
		}
	}

}
