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

package tigase.server.ext;

import java.util.logging.Logger;
import tigase.db.comp.RepositoryItem;
import tigase.net.ConnectionType;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Created: Oct 3, 2009 4:39:51 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompRepoItem implements RepositoryItem {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger(CompRepoItem.class.getName());

	public static final String REPO_ITEM_ELEM_NAME = "item";
	public static final String DOMAIN_ATTR = "domain";
	public static final String REMOTE_HOST_ATTR = "remote";
	public static final String CONN_TYPE_ATTR = "type";
	public static final String PORT_NO_ATTR = "port";
	public static final String PASSWORD_ATTR = "pass";
	public static final String PROTO_XMLNS_ATTR = "proto-xmlns";
	public static final String ROUTINGS_ATTR = "routings";

	public static final String DOMAIN_NAME_LABEL = "Domain name";
	public static final String DOMAIN_PASS_LABEL = "Domain password";
	public static final String CONNECTION_TYPE_LABEL = "Connection type";
	public static final String PORT_NO_LABEL = "Port number";
	public static final String REMOTE_HOST_LABEL = "Remote host";
	public static final String PROTO_XMLNS_LABEL = "Protocol";
	public static final String ROUTINGS_LABEL = "(Optional) Routings";

	//"accept:muc.domain.tld:5277:user:passwd"
	private String domain = null;
	private String remoteHost = null;
	private ConnectionType type = ConnectionType.accept;
	private int port = -1;
	private String auth_pass = null;
	private String xmlns = null;
	private String prop_xmlns = null;
	private String[] routings = null;

	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");
		if (props.length > 0) {
			setDomain(props[0]);
		}
		if (props.length > 1) {
			auth_pass = props[1];
		}
		if (props.length > 2) {
			setConnectionType(props[2]);
		}
		if (props.length > 3) {
			port = parsePortNo(props[3]);
		}
		if (props.length > 4) {
			remoteHost = props[4];
		}
		if (props.length > 5) {
			setProtocol(props[5]);
		}
	}

	private String parseProtoXMLNS(String input) {
		String result = input;
		if (input.equals("accept")) {
			result = "jabber:component:accept";
		}
		if (input.equals("client")) {
			result = "jabber:client";
		}
		if (input.equals("connect")) {
			result = "jabber:component:connect";
		}
		return result;
	}

	private int parsePortNo(String input) {
		int result = -1;
		try {
			result = Integer.parseInt(input);
		} catch (Exception e) {
			result = 5277;
			log.warning("Incorrect port number, can't parse: " + input);
		}
		return result;
	}

	private ConnectionType parseConnectionType(String input) {
		ConnectionType result = ConnectionType.accept;
		if (input.equals("connect")) {
			result = ConnectionType.connect;
		}
		if (input.equals("accept") || input.equals("listen")) {
			result = ConnectionType.accept;
		}
		return result;
	}

	@Override
	public String toPropertyString() {
		return domain + ":" + auth_pass + ":" + type.name() + ":" + port + ":" +
				remoteHost + ":" + prop_xmlns;
	}

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != REPO_ITEM_ELEM_NAME) {
			throw new IllegalArgumentException("Incorrect element name, expected: " +
					REPO_ITEM_ELEM_NAME);
		}
		setDomain(elem.getAttribute(DOMAIN_ATTR));
		auth_pass = elem.getAttribute(PASSWORD_ATTR);
		remoteHost = elem.getAttribute(REMOTE_HOST_ATTR);
		String tmp = elem.getAttribute(CONN_TYPE_ATTR);
		if (tmp != null) {
			setConnectionType(tmp);
		}
		tmp = elem.getAttribute(PORT_NO_ATTR);
		if (tmp != null) {
			port = parsePortNo(tmp);
		}
		tmp = elem.getAttribute(PROTO_XMLNS_ATTR);
		if (tmp != null) {
			setProtocol(tmp);
		}
		tmp = elem.getAttribute(ROUTINGS_ATTR);
		if (tmp != null) {
			routings = tmp.split(",");
		}
	}

	@Override
	public Element toElement() {
		Element elem = new Element(REPO_ITEM_ELEM_NAME);
		elem.addAttribute(DOMAIN_ATTR, domain);
		elem.addAttribute(PASSWORD_ATTR, auth_pass);
		if (remoteHost != null && !remoteHost.isEmpty()) {
			elem.addAttribute(REMOTE_HOST_ATTR, remoteHost);
		}
		elem.addAttribute(CONN_TYPE_ATTR, type.name());
		if (port > 0) {
			elem.addAttribute(PORT_NO_ATTR, ""+port);
		}
		elem.addAttribute(PROTO_XMLNS_ATTR, prop_xmlns);
		StringBuilder route = new StringBuilder();
		for (String r : routings) {
			if (route.length() > 0) {
				route.append(',');
			}
			route.append(r);
		}
		elem.addAttribute(ROUTINGS_ATTR, route.toString());
		return elem;
	}

	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, DOMAIN_NAME_LABEL, 
				(domain != null ? domain : ""));
		Command.addFieldValue(packet, DOMAIN_PASS_LABEL,
				(auth_pass != null ? auth_pass : ""));
		String[] types = new String[ConnectionType.values().length];
		int i = 0;
		for (ConnectionType t : ConnectionType.values()) {
			types[i++] = t.name();
		}
		Command.addFieldValue(packet, CONNECTION_TYPE_LABEL, type.name(),
				CONNECTION_TYPE_LABEL, types, types);
		Command.addFieldValue(packet, PORT_NO_LABEL, (port > 0 ? "" + port : ""));
		Command.addFieldValue(packet, REMOTE_HOST_LABEL,
				(remoteHost != null ? remoteHost : ""));
		Command.addFieldValue(packet, PROTO_XMLNS_LABEL,
				(prop_xmlns != null ? prop_xmlns : ""));
		Command.addFieldValue(packet, ROUTINGS_LABEL, "");
	}

	@Override
	public void initFromCommand(Packet packet) {
		domain = Command.getFieldValue(packet, DOMAIN_NAME_LABEL);
		routings = new String[]{domain, ".*@" + domain, ".*\\." + domain};
		auth_pass = Command.getFieldValue(packet, DOMAIN_PASS_LABEL);
		String tmp = Command.getFieldValue(packet, REMOTE_HOST_LABEL);
		if (tmp != null && !tmp.isEmpty()) {
			remoteHost = tmp;
		}
		tmp = Command.getFieldValue(packet, CONNECTION_TYPE_LABEL);
		if (tmp != null && !tmp.isEmpty()) {
			type = parseConnectionType(tmp);
		}
		tmp = Command.getFieldValue(packet, PORT_NO_LABEL);
		if (tmp != null && !tmp.isEmpty()) {
			port = parsePortNo(tmp);
		}
		tmp = Command.getFieldValue(packet, PROTO_XMLNS_LABEL);
		if (tmp != null && !tmp.isEmpty()) {
			prop_xmlns = tmp;
			xmlns = parseProtoXMLNS(prop_xmlns);
		}
		tmp = Command.getFieldValue(packet, ROUTINGS_LABEL);
		if (tmp != null && !tmp.isEmpty()) {
			routings = tmp.split(",");
		}
	}

	@Override
	public String getKey() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
		routings = new String[]{domain, ".*@" + domain, ".*\\." + domain};
	}

	public ConnectionType getConnectionType() {
		return type;
	}

	public String getDomain() {
		return domain;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public int getPort() {
		return port;
	}

	public String getAuthPasswd() {
		return auth_pass;
	}

	public String[] getRoutings() {
		return routings;
	}

	public String getXMLNS() {
		return xmlns;
	}

	@Override
	public String toString() {
		return toPropertyString();
	}

	void setPassword(String password) {
		this.auth_pass = password;
	}

	void setPort(int port) {
		this.port = port;
	}

	void setRemoteDomain(String remote_domain) {
		this.remoteHost = remote_domain;
	}

	void setProtocol(String protocol) {
		this.prop_xmlns = protocol;
		this.xmlns = parseProtoXMLNS(protocol);
	}

	void setConnectionType(String connection_type) {
		this.type = parseConnectionType(connection_type);
	}

}
