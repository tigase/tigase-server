/*
 * VirtualComponent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.cluster;

import tigase.server.ComponentInfo;
import tigase.server.DisableDisco;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import tigase.conf.Configurable;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.util.DNSResolverFactory;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostListener;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * muc/fixed-domain=example.com
 * </pre>
 *
 * Above options set all possible parameters to setup virtual MUC component.
 * Created: Dec 13, 2008 7:44:35 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class VirtualComponent
				implements ServerComponent, XMPPService, Configurable, DisableDisco,
						VHostListener {
	/**
	 * Parameter to set service discovery item category name for the virtual
	 * component. Please refer to service discovery documentation for a correct
	 * category or check what is returned by your real component instance.
	 */
	public static final String DISCO_CATEGORY_PROP_KEY = "disco-category";

	/** Field description */
	public static final String DISCO_CATEGORY_PROP_VAL = "conference";

	/**
	 * Comma separated list of features for the service discovery item represented
	 * by this virtual component. Please check with the real component to obtain a
	 * correct list of features.
	 */
	public static final String DISCO_FEATURES_PROP_KEY = "disco-features";

	/** Field description */
	public static final String DISCO_FEATURES_PROP_VAL = "http://jabber.org/protocol/muc";

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

	/** A default value for service discovery item type, which is 'text' */
	public static final String DISCO_TYPE_PROP_VAL = "text";

	/**
	 * If set, then it is used as the component domain name part. This domains is
	 * displayed on the service discovery information, instead of virtual host based on
	 * the user's query.
	 */
	public static final String FIXED_DOMAIN_PROP_KEY = "fixed-domain";

	/**
	 * Virtual component parameter setting packet redirect destination address.
	 */
	public static final String REDIRECT_TO_PROP_KEY = "redirect-to";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.cluster.VirtualComponent");

	//~--- fields ---------------------------------------------------------------

	protected VHostManagerIfc vHostManager  = null;
	private JID               componentId   = null;
	private String            discoCategory = null;
	private String[]          discoFeatures = null;
	private String            discoName     = null;
	private String            discoNode     = null;
	private String            discoType     = null;
	private String            fixedDomain   = null;
	private String            name          = null;
	private JID               redirectTo    = null;
	private ServiceEntity     serviceEntity = null;
	private ComponentInfo     cmpInfo				= null;

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean handlesLocalDomains() {
		return false;
	}

	@Override
	public boolean handlesNameSubdomains() {
		return true;
	}

	@Override
	public boolean handlesNonLocalDomains() {
		return false;
	}

	@Override
	public void initializationCompleted() {}

	@Override
	public void processPacket(Packet packet, Queue<Packet> results) {
		if (redirectTo != null) {
			packet.setPacketTo(redirectTo);
			results.add(packet);
		} else {
			log.log(Level.INFO, "No redirectTo address, dropping packet: {0}", packet);
		}
	}

	@Override
	public void release() {}

	@Override
	public JID getComponentId() {
		return componentId;
	}

	@Override
	public ComponentInfo getComponentInfo() {
		if ( cmpInfo == null ){
			cmpInfo = new ComponentInfo( getName(), this.getClass() );
		}
		return cmpInfo;
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>();

		defs.put(REDIRECT_TO_PROP_KEY, "");
		if (params.get(CLUSTER_NODES) != null) {
			String[] cl_nodes = ((String) params.get(CLUSTER_NODES)).split(",");

			for (String node : cl_nodes) {
				if (!node.equals(DNSResolverFactory.getInstance().getDefaultHost())) {
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
		defs.put(FIXED_DOMAIN_PROP_KEY, null);

		return defs;
	}

	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return null;
	}

	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		return null;
	}

	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		String domain = jid.toString();

		if (fixedDomain != null) {
			domain = fixedDomain;
		}

		Element result = serviceEntity.getDiscoItem(null, getName() + "." + domain);

		return Arrays.asList(result);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isInitializationComplete() {
		return false;
	}

	@Override
	public void setName(String name) {
		this.name        = name;
		this.componentId = JID.jidInstanceNS(name, DNSResolverFactory.getInstance().getDefaultHost(), null);
	}

	@Override
	public void setProperties(Map<String, Object> properties) {
		fixedDomain = (String) properties.get(FIXED_DOMAIN_PROP_KEY);
		if (fixedDomain != null) {
			this.componentId = JID.jidInstanceNS(null, name + "." + fixedDomain, null);
		}

		String redirect = (String) properties.get(REDIRECT_TO_PROP_KEY);

		if (redirect != null) {
			if (redirect.isEmpty()) {
				redirectTo = null;
			} else {
				try {
					redirectTo = JID.jidInstance(redirect);
				} catch (TigaseStringprepException ex) {
					redirectTo = null;
					log.log(Level.WARNING,
							"stringprep processing failed for given redirect address: {0}", redirect);
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
		if ((discoName != null) && (discoCategory != null) && (discoType != null) &&
				(discoFeatures != null)) {
			serviceEntity = new ServiceEntity(getName(), null, discoName);
			serviceEntity.addIdentities(new ServiceIdentity(discoCategory, discoType,
					discoName));
			for (String feature : discoFeatures) {
				serviceEntity.addFeatures(feature);
			}
		}
		cmpInfo = new ComponentInfo( getName(), this.getClass() );
	}

	@Override
	public void setVHostManager(VHostManagerIfc manager) {
		this.vHostManager = manager;
	}
}
