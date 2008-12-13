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

	public static final String REDIRECT_TO_PROP_KEY = "redirect-to";
	public static final String DISCO_NAME_PROP_KEY = "disco-name";
	public static final String DISCO_NAME_PROP_VAL = "Multi User Chat";
	public static final String DISCO_NODE_PROP_KEY = "disco-node";
	public static final String DISCO_NODE_PROP_VAL = "";
	public static final String DISCO_TYPE_PROP_KEY = "disco-type";
	public static final String DISCO_TYPE_PROP_VAL = "text";
	public static final String DISCO_CATEGORY_PROP_KEY = "disco-category";
	public static final String DISCO_CATEGORY_PROP_VAL = "conference";
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

	public void setName(String name) {
		this.name = name;
		this.componentId = JIDUtils.getNodeID(name, DNSResolver.getDefaultHostname());
	}

	public String getName() {
		return name;
	}

	public String getComponentId() {
		return componentId;
	}

	public void release() {
	}

	public void processPacket(Packet packet, Queue<Packet> results) {
		if (redirectTo != null) {
			packet.setTo(redirectTo);
			results.add(packet);
		} else {
			log.info("No redirectTo address, dropping packet: " + packet.toString());
		}
	}

	public void initializationCompleted() {
	}

	public Element getDiscoInfo(String node, String jid) {
		return null;
	}

	public List<Element> getDiscoItems(String node, String jid) {
		Element result = serviceEntity.getDiscoItem(null, getName() + "." + jid);
		return Arrays.asList(result);
	}

	public List<Element> getDiscoFeatures() {
		return null;
	}

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
