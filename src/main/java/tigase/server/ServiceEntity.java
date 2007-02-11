/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tigase.xml.Element;

/**
 * Describe class ServiceEntity here.
 *
 *
 * Created: Sat Feb 10 13:11:34 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServiceEntity {

	private String jid = null;
	private String node = null;
	private String name = null;
	private List<String> features = null;
	private List<ServiceIdentity> identities = null;
	private List<ServiceEntity> items = null;

	/**
	 * Creates a new <code>ServiceEntity</code> instance.
	 *
	 */
	public ServiceEntity(String jid, String node, String name) {
		this.jid = jid;
		this.node = node;
		this.name = name;
	}

	public void addFeatures(String[] features) {
		if (this.features == null) {
			this.features = new ArrayList<String>();
		}
		Collections.addAll(this.features, features);
	}

	public void addIdentities(ServiceIdentity[] identities) {
		if (this.identities == null) {
			this.identities = new ArrayList<ServiceIdentity>();
		}
		Collections.addAll(this.identities, identities);
	}

	public void addItems(ServiceEntity[] items) {
		if (this.items == null) {
			this.items = new ArrayList<ServiceEntity>();
		}
		Collections.addAll(this.items, items);
	}

	public String getJID() {
		return jid;
	}

	public String getNode() {
		return node;
	}

	public String getName() {
		return name;
	}

	public Element getDiscoInfo(String node) {
		Element query = null;
		if (node == null) {
			query = new Element("query",
				new String[] {"xmlns"},
				new String[] {"http://jabber.org/protocol/disco#info"});
			if (identities != null) {
				for (ServiceIdentity ident: identities) {
					query.addChild(ident.getElement());
				}
			}
			if (features != null) {
				for (String feature: features) {
					query.addChild(new Element("feature",
							new String[] {"var"},
							new String[] {feature}));
				}
			}
		} else {
			ServiceEntity entity = findNode(node);
			if (entity != null) {
				query = entity.getDiscoInfo(null);
				query.setAttribute("node", node);
			}
		}
		return query;
	}

	public Element getDiscoItem(String node, String jid) {
		Element item = new Element("item");
		if (jid != null) {
			item.setAttribute("jid", jid);
		} else {
			if (this.jid != null) {
				item.setAttribute("jid", this.jid);
			}
		}
		if (node != null) {
			item.setAttribute("node", node + (this.node != null ? "/" + this.node : ""));
		} else {
			if (this.node != null) {
				item.setAttribute("node", this.node);
			}
		}
		if (name != null) {
			item.setAttribute("name", name);
		}
		return item;
	}

	public List<Element> getItems(String node, String jid) {
		List<Element> result = null;
		if (items != null) {
			result = new ArrayList<Element>();
			for (ServiceEntity item: items) {
				result.add(item.getDiscoItem(node, jid));
			}
		}
		return result;
	}

	public List<Element> getDiscoItems(String node, String jid) {
		List<Element> result = null;
		if (node == null) {
			result = getItems(null, jid);
		} else {
			ServiceEntity entity = findNode(node);
			if (entity != null) {
				result = entity.getItems(node, jid);
			}
		}
		return result;
	}

	public ServiceEntity findNode(String node) {
		if (this.node != null && this.node.equals(node)) {
			return this;
		}
		if (items == null) {
			return null;
		}
		for (ServiceEntity item: items) {
			String n = item.getNode();
			if (n != null && node.equals(n)) {
				return item;
			}
		}
		int idx = node.indexOf('/');
		if (idx >= 0) {
			String top = node.substring(0, idx);
			ServiceEntity current = findNode(top);
			if (current != null) {
				String rest = node.substring(idx+1);
				return current.findNode(rest);
			}
		}
		return null;
	}

}
