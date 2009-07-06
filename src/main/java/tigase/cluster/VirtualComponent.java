/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;
import tigase.conf.Configurable;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.DisableDisco;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.util.DNSResolver;
import tigase.util.JIDUtils;
import tigase.xml.Element;

/**
 * The purpose of this component implementation is to forward packets to a target
 * real component implementation in the cluster installation. Let's say you have
 * a cluster installation with full Tigase server installed on each node and you
 * also want to use a component which doesn't have clustered implementation yet.
 * In such case you deploy the component on one of the cluster nodes and put the
 * virtual component on all other nodes. With proper configuration they pretend
 * to be the component returning a correct service disco information and forward
 * all packets for this component to a cluster node with real component running.
 *
 * This is a very lightweight implementation which doesn't use much resources
 * either memory or CPU.
 *
 * It can work well for any kind of a component: MUC, PubSub, transport either
 * native Tigase components or third-party components connected via XEP-0114 -
 * external protocol component.
 *
 * Basic configuration parameters are actually the same as for a real component.
 * You set a real component name as a name for the virtual component and a vritual
 * component class name to load. Let's say we want to deploy MUC component this
 * way. The MUC component is visible as <code>muc.domain.our</code> in our
 * installation. Thus the name of the component is: <code>muc</code>:
 * <pre>
 * --comp-name-1=muc
 * --comp-class-1=tigase.cluster.VirtualComponent
 * </pre>
 * This is pretty much all you need to load a virtual component. A few other
 * options are needed to point to correct destination addresses for forwarded
 * packets and to set correct service discovery parameters:
 * <pre>
 * muc/redirect-to=muc@cluster-node-with-real-muc.domain.our
 * muc/disco-name=Multi User Chat
 * muc/disco-node=
 * muc/disco-type=text
 * muc/disco-category=conference
 * muc/disco-features=http://jabber.org/protocol/muc
 * </pre>
 * Above options set all possible parameters to setup virtual MUC component.
 * Created: Dec 13, 2008 7:44:35 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VirtualComponent 
				implements ServerComponent, XMPPService, Configurable, DisableDisco {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.cluster.VirtualComponent");

	/**
	 * Virtual component parameter setting packet redirect destination address.
	 */
	public static final String REDIRECT_TO_PROP_KEY = "redirect-to";
	/**
	 * Parameter to set service discovery item name for the virtual component
	 * instance. You should refer to service discovery documentation for a proper
	 * name for your component.
	 */
	public static final String DISCO_NAME_PROP_KEY = "disco-name";
	public static final String DISCO_NAME_PROP_VAL = "Multi User Chat";
	/**
	 * Parameter to set service discovery node name. In most cases you should leave
	 * it empty unless you really know what you are doing.
	 */
	public static final String DISCO_NODE_PROP_KEY = "disco-node";
	public static final String DISCO_NODE_PROP_VAL = "";
	/**
	 * Parameter to set service discovery item type for the virtual component.
	 * You should refer to a service discovery documentation for a correct type
	 * for your component. Or, alternatively you can have a look what returns
	 * your real component.
	 */
	public static final String DISCO_TYPE_PROP_KEY = "disco-type";
	public static final String DISCO_TYPE_PROP_VAL = "text";
	/**
	 * Parameter to set service discovery item category name for the virtual
	 * component. Please refer to service discovery documentation for a correct
	 * category or check what is returned by your real component instance.
	 */
	public static final String DISCO_CATEGORY_PROP_KEY = "disco-category";
	public static final String DISCO_CATEGORY_PROP_VAL = "conference";
	/**
	 * Comma separated list of features for the service discovery item
	 * reprezented by this virtual component. Please check with the real component
	 * to obtain a correct list of features.
	 */
	public static final String DISCO_FEATURES_PROP_KEY = "disco-features";
	public static final String DISCO_FEATURES_PROP_VAL = "http://jabber.org/protocol/muc";

	private String name = null;
	private String componentId = null;
	private ServiceEntity serviceEntity = null;
	private String redirectTo = null;
	private String discoName = null;
	private String discoNode = null;
	private String discoCategory = null;
	private String discoType = null;
	private String[] discoFeatures = null;

	@Override
	public void setName(String name) {
		this.name = name;
		this.componentId = JIDUtils.getNodeID(name, DNSResolver.getDefaultHostname());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getComponentId() {
		return componentId;
	}

	@Override
	public void release() {
	}

	@Override
	public void processPacket(Packet packet, Queue<Packet> results) {
		if (redirectTo != null) {
			packet.setTo(redirectTo);
			results.add(packet);
		} else {
			log.info("No redirectTo address, dropping packet: " + packet.toString());
		}
	}

	@Override
	public void initializationCompleted() {
	}

	@Override
	public Element getDiscoInfo(String node, String jid) {
		return null;
	}

	@Override
	public List<Element> getDiscoItems(String node, String jid) {
		Element result = serviceEntity.getDiscoItem(null, getName() + "." + jid);
		return Arrays.asList(result);
	}

	@Override
	public List<Element> getDiscoFeatures() {
		return null;
	}

	@Override
	public void setProperties(Map<String, Object> properties) {

		String redirect = (String) properties.get(REDIRECT_TO_PROP_KEY);
		if (redirect == null || redirect.isEmpty()) {
			redirectTo = null;
		} else {
			redirectTo = redirect;
		}
		discoName = (String) properties.get(DISCO_NAME_PROP_KEY);
		discoNode = (String) properties.get(DISCO_NODE_PROP_KEY);
		if (discoNode.isEmpty()) {
			discoNode = null;
		}
		discoCategory = (String) properties.get(DISCO_CATEGORY_PROP_KEY);
		discoType = (String) properties.get(DISCO_TYPE_PROP_KEY);
		discoFeatures = ((String) properties.get(DISCO_TYPE_PROP_KEY)).split(",");

		serviceEntity = new ServiceEntity(getName(), null, discoName);
		serviceEntity.addIdentities(new ServiceIdentity(discoCategory, discoType,
						discoName));
		for (String feature : discoFeatures) {
			serviceEntity.addFeatures(feature);
		}
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>();

		defs.put(REDIRECT_TO_PROP_KEY, "");
		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String)params.get(CLUSTER_NODES)).split(",");
			for (String node : cl_nodes) {
				if (!node.equals( DNSResolver.getDefaultHostname())) {
					defs.put(REDIRECT_TO_PROP_KEY, JIDUtils.getNodeID(getName(), node));
					break;
				}
			}
		}
		defs.put(DISCO_NAME_PROP_KEY, DISCO_NAME_PROP_VAL);
		defs.put(DISCO_NODE_PROP_KEY, DISCO_NODE_PROP_VAL);
		defs.put(DISCO_TYPE_PROP_KEY, DISCO_TYPE_PROP_VAL);
		defs.put(DISCO_CATEGORY_PROP_KEY, DISCO_CATEGORY_PROP_VAL);
		defs.put(DISCO_FEATURES_PROP_KEY, DISCO_FEATURES_PROP_VAL);
		return defs;
	}

}
