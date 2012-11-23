/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.cluster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.Configurable;

import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;

import tigase.server.DisableDisco;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;

import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostListener;
import tigase.vhosts.VHostManagerIfc;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * The purpose of this component implementation is to forward packets to a
 * target real component implementation in the cluster installation. Let's say
 * you have a cluster installation with full Tigase server installed on each
 * node and you also want to use a component which doesn't have clustered
 * implementation yet. In such case you deploy the component on one of the
 * cluster nodes and put the virtual component on all other nodes. With proper
 * configuration they pretend to be the component returning a correct service
 * disco information and forward all packets for this component to a cluster
 * node with real component running.
 *
 * This is a very lightweight implementation which doesn't use much resources
 * either memory or CPU.
 *
 * It can work well for any kind of a component: MUC, PubSub, transport either
 * native Tigase components or third-party components connected via XEP-0114 -
 * external protocol component.
 *
 * Basic configuration parameters are actually the same as for a real component.
 * You set a real component name as a name for the virtual component and a
 * vritual component class name to load. Let's say we want to deploy MUC
 * component this way. The MUC component is visible as
 * <code>muc.domain.our</code> in our installation. Thus the name of the
 * component is: <code>muc</code>:
 *
 * <pre>
 * --comp-name-1=muc
 * --comp-class-1=tigase.cluster.VirtualComponent
 * </pre>
 *
 * This is pretty much all you need to load a virtual component. A few other
 * options are needed to point to correct destination addresses for forwarded
 * packets and to set correct service discovery parameters:
 *
 * <pre>
 * muc/redirect-to=muc@cluster-node-with-real-muc.domain.our
 * muc/disco-name=Multi User Chat
 * muc/disco-node=
 * muc/disco-type=text
 * muc/disco-category=conference
 * muc/disco-features=http://jabber.org/protocol/muc
 * </pre>
 *
 * Above options set all possible parameters to setup virtual MUC component.
 * Created: Dec 13, 2008 7:44:35 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VirtualComponent implements ServerComponent, XMPPService, Configurable,
		DisableDisco, VHostListener {

	protected VHostManagerIfc vHostManager = null;

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.cluster.VirtualComponent");

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

	/** Field description */
	public static final String DISCO_NAME_PROP_VAL = "Multi User Chat";

	/**
	 * Parameter to set service discovery node name. In most cases you should
	 * leave it empty unless you really know what you are doing.
	 */
	public static final String DISCO_NODE_PROP_KEY = "disco-node";

	/** Field description */
	public static final String DISCO_NODE_PROP_VAL = "";

	/**
	 * Parameter to set service discovery item type for the virtual component. You
	 * should refer to a service discovery documentation for a correct type for
	 * your component. Or, alternatively you can have a look what returns your
	 * real component.
	 */
	public static final String DISCO_TYPE_PROP_KEY = "disco-type";

	/** Field description */
	public static final String DISCO_TYPE_PROP_VAL = "text";

	/**
	 * Parameter to set service discovery item category name for the virtual
	 * component. Please refer to service discovery documentation for a correct
	 * category or check what is returned by your real component instance.
	 */
	public static final String DISCO_CATEGORY_PROP_KEY = "disco-category";

	/** Field description */
	public static final String DISCO_CATEGORY_PROP_VAL = "conference";

	/**
	 * Comma separated list of features for the service discovery item reprezented
	 * by this virtual component. Please check with the real component to obtain a
	 * correct list of features.
	 */
	public static final String DISCO_FEATURES_PROP_KEY = "disco-features";

	/** Field description */
	public static final String DISCO_FEATURES_PROP_VAL = "http://jabber.org/protocol/muc";

	// ~--- fields ---------------------------------------------------------------

	private JID componentId = null;
	private String discoCategory = null;
	private String[] discoFeatures = null;
	private String discoName = null;
	private String discoNode = null;
	private String discoType = null;
	private String name = null;
	private JID redirectTo = null;
	private ServiceEntity serviceEntity = null;

	@Override
	public void setVHostManager(VHostManagerIfc manager) {
		this.vHostManager = manager;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean handlesLocalDomains() {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean handlesNameSubdomains() {
		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean handlesNonLocalDomains() {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public JID getComponentId() {
		return componentId;
	}

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
		Map<String, Object> defs = new LinkedHashMap<String, Object>();

		defs.put(REDIRECT_TO_PROP_KEY, "");

		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String) params.get(CLUSTER_NODES)).split(",");

			for (String node : cl_nodes) {
				if (!node.equals(DNSResolver.getDefaultHostname())) {
					defs.put(REDIRECT_TO_PROP_KEY, BareJID.toString(getName(), node));

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

	/**
	 * Method description
	 *
	 *
	 * @param from
	 *
	 * @return
	 */
	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param jid
	 * @param from
	 *
	 * @return
	 */
	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param jid
	 * @param from
	 *
	 * @return
	 */
	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		Element result = serviceEntity.getDiscoItem(null, getName() + "." + jid);

		return Arrays.asList(result);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return name;
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void initializationCompleted() {
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param results
	 */
	@Override
	public void processPacket(Packet packet, Queue<Packet> results) {
		if (redirectTo != null) {
			packet.setPacketTo(redirectTo);
			results.add(packet);
		} else {
			log.info("No redirectTo address, dropping packet: " + packet.toString());
		}
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void release() {
	}

	// ~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
		this.componentId = JID.jidInstanceNS(name, DNSResolver.getDefaultHostname(), null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {
		String redirect = (String) properties.get(REDIRECT_TO_PROP_KEY);

		if (redirect != null) {
			if (redirect.isEmpty()) {
				redirectTo = null;
			} else {
				try {
					redirectTo = JID.jidInstance(redirect);
				} catch (TigaseStringprepException ex) {
					redirectTo = null;
					log.warning("stringprep processing failed for given redirect address: "
							+ redirect);
				}
			}
		}

		if (properties.get(DISCO_NAME_PROP_KEY) != null) {
			discoName = (String) properties.get(DISCO_NAME_PROP_KEY);
		}
		if (properties.get(DISCO_NODE_PROP_KEY) != null) {
			discoNode = (String) properties.get(DISCO_NODE_PROP_KEY);
			if (discoNode.isEmpty()) {
				discoNode = null;
			}
		}

		if (properties.get(DISCO_CATEGORY_PROP_KEY) != null) {
			discoCategory = (String) properties.get(DISCO_CATEGORY_PROP_KEY);
		}
		if (properties.get(DISCO_TYPE_PROP_KEY) != null) {
			discoType = (String) properties.get(DISCO_TYPE_PROP_KEY);
		}
		if (properties.get(DISCO_TYPE_PROP_KEY) != null) {
			discoFeatures = ((String) properties.get(DISCO_TYPE_PROP_KEY)).split(",");
		}

		if (discoName != null && discoCategory != null && discoType != null
				&& discoFeatures != null) {
			serviceEntity = new ServiceEntity(getName(), null, discoName);
			serviceEntity
					.addIdentities(new ServiceIdentity(discoCategory, discoType, discoName));

			for (String feature : discoFeatures) {
				serviceEntity.addFeatures(feature);
			}
		}
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
