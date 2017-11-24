/*
 * CompRepoItem.java
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



package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.RepositoryItemAbstract;
import tigase.net.ConnectionType;
import tigase.net.SocketType;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.ext.lb.LoadBalancerIfc;
import tigase.server.ext.lb.ReceiverBareJidLB;
import tigase.xml.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Oct 3, 2009 4:39:51 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompRepoItem
				extends RepositoryItemAbstract {
	public static final String CONN_TYPE_ATTR = "type";

	public static final String CONNECTION_TYPE_LABEL = "Connection type";

	public static final String DOMAIN_ATTR = "domain";

	public static final String DOMAIN_NAME_LABEL = "Domain name";

	public static final String DOMAIN_PASS_LABEL = "Domain password";

	public static final String LB_CLASS_LABEL = "Load balancer class";

	public static final String LB_NAME_ATTR = "lb-class";

	public static final String PASSWORD_ATTR = "pass";

	public static final String PORT_NO_ATTR = "port";

	public static final String PORT_NO_LABEL = "Port number";

	public static final String PROTO_XMLNS_ATTR = "proto-xmlns";

	public static final String PROTO_XMLNS_LABEL = "Protocol";

	public static final String REMOTE_HOST_ATTR = "remote";

	public static final String REMOTE_HOST_LABEL = "Remote host";

	public static final String REPO_ITEM_ELEM_NAME = "item";

	public static final String ROUTINGS_ATTR = "routings";

	public static final String ROUTINGS_LABEL = "(Optional) Routings";

	public static final String SOCKET_ATTR = "socket";

	public static final String SOCKET_LABEL = "(Optional) Socket type";

	private static final Logger log = Logger.getLogger(CompRepoItem.class.getName());

	public static final LoadBalancerIfc DEF_LB_CLASS = new ReceiverBareJidLB();

	private String auth_pass = null;

	// "accept:muc.domain.tld:5277:user:passwd"
	private String domain       = null;
	private int port            = -1;
	private String prop_xmlns   = null;
	private String remoteHost   = null;
	private String[] routings   = null;
	private ConnectionType type = ConnectionType.accept;
	private String xmlns        = null;
	private LoadBalancerIfc lb  = DEF_LB_CLASS;
	private SocketType socket  = SocketType.plain;

	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, DOMAIN_NAME_LABEL, ((domain != null)
						? domain
						: ""));
		Command.addFieldValue(packet, DOMAIN_PASS_LABEL, ((auth_pass != null)
						? auth_pass
						: ""));

		Command.addFieldValue(packet, CONNECTION_TYPE_LABEL, type.name(),
													CONNECTION_TYPE_LABEL, ConnectionType.names(), ConnectionType.names());
		Command.addFieldValue(packet, PORT_NO_LABEL, ((port > 0)
						? "" + port
						: ""));
		Command.addFieldValue(packet, REMOTE_HOST_LABEL, ((remoteHost != null)
						? remoteHost
						: ""));
		Command.addFieldValue(packet, PROTO_XMLNS_LABEL, ((prop_xmlns != null)
						? prop_xmlns
						: ""));
		Command.addFieldValue(packet, LB_CLASS_LABEL, ((lb != null)
						? lb.getClass().getName()
						: ""));
		Command.addFieldValue(packet, ROUTINGS_LABEL, "");
		Command.addFieldValue(packet, SOCKET_LABEL, socket.name(), SOCKET_LABEL, SocketType.names(),
		                      SocketType.names());
		super.addCommandFields(packet);
	}

	public String getAuthPasswd() {
		return auth_pass;
	}

	public ConnectionType getConnectionType() {
		return type;
	}

	public LoadBalancerIfc getLoadBalancer() {
		return lb;
	}

	public String getDomain() {
		return domain;
	}

	public SocketType getSocket() {
		return socket;
	}

	@Override
	public String getElemName() {
		return REPO_ITEM_ELEM_NAME;
	}

	@Override
	public String getKey() {
		return domain;
	}

	public int getPort() {
		return port;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public String[] getRoutings() {
		return routings;
	}

	public String getXMLNS() {
		return xmlns;
	}

	@Override
	public void initFromCommand(Packet packet) {
		super.initFromCommand(packet);
		domain    = Command.getFieldValue(packet, DOMAIN_NAME_LABEL);
		routings  = new String[] { domain, ".*@" + domain, ".*\\." + domain };
		auth_pass = Command.getFieldValue(packet, DOMAIN_PASS_LABEL);

		String tmp = Command.getFieldValue(packet, REMOTE_HOST_LABEL);

		if ((tmp != null) &&!tmp.isEmpty()) {
			remoteHost = tmp;
		}
		tmp = Command.getFieldValue(packet, CONNECTION_TYPE_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			type = parseConnectionType(tmp);
		}
		tmp = Command.getFieldValue(packet, PORT_NO_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			port = parsePortNo(tmp);
		}
		tmp = Command.getFieldValue(packet, PROTO_XMLNS_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			prop_xmlns = tmp;
			xmlns      = parseProtoXMLNS(prop_xmlns);
		}
		tmp = Command.getFieldValue(packet, LB_CLASS_LABEL);
		if ((tmp != null) &&!tmp.trim().isEmpty()) {
			lb = lbInstance(tmp);
		}
		tmp = Command.getFieldValue(packet, ROUTINGS_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			routings = tmp.split(",");
		}
		tmp = Command.getFieldValue(packet, SOCKET_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			socket = parseSocket(tmp);
		}
	}

	private LoadBalancerIfc lbInstance(String cls_name) {
		String class_name = cls_name;

//  if (!class_name.endsWith(".class")) {
//    class_name = class_name + ".class";
//  }
		log.log(Level.INFO, "Activating load-balancer for domain: {0}, class: {1}",
						new Object[] { domain,
													 class_name });

		LoadBalancerIfc result = null;

		try {
			result = (LoadBalancerIfc) Class.forName(class_name).newInstance();
		} catch (Exception ex1) {
			class_name = "tigase.server.ext.lb." + class_name;
			log.log(Level.INFO, "Cannot active load balancer for class: {0}, trying: {1}",
							new Object[] { cls_name,
														 class_name });
			try {
				result = (LoadBalancerIfc) Class.forName(class_name).newInstance();
			} catch (Exception ex2) {
				log.log(
						Level.WARNING,
						"Cannot active load balancer for class:" +
						" {0}, or: {1}, errors: {2} or {3}, using default LB: {4}", new Object[] {
							cls_name,
							class_name, ex1, ex2, DEF_LB_CLASS.getClass().getName() });
				result = DEF_LB_CLASS;
			}
		}
		log.log(Level.INFO, "Activated load-balancer for domain: {0}, class: {1}",
						new Object[] { domain,
													 result.getClass().getName() });

		return result;
	}

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != REPO_ITEM_ELEM_NAME) {
			throw new IllegalArgumentException("Incorrect element name, expected: " +
																				 REPO_ITEM_ELEM_NAME);
		}
		super.initFromElement(elem);
		setDomain(elem.getAttributeStaticStr(DOMAIN_ATTR));
		auth_pass  = elem.getAttributeStaticStr(PASSWORD_ATTR);
		remoteHost = elem.getAttributeStaticStr(REMOTE_HOST_ATTR);

		String tmp = elem.getAttributeStaticStr(CONN_TYPE_ATTR);

		if (tmp != null) {
			setConnectionType(tmp);
		}
		tmp = elem.getAttributeStaticStr(PORT_NO_ATTR);
		if (tmp != null) {
			port = parsePortNo(tmp);
		}
		tmp = elem.getAttributeStaticStr(PROTO_XMLNS_ATTR);
		if (tmp != null) {
			setProtocol(tmp);
		}
		tmp = elem.getAttributeStaticStr(LB_NAME_ATTR);
		if (tmp != null) {
			lb = lbInstance(tmp);
		}
		tmp = elem.getAttributeStaticStr(ROUTINGS_ATTR);
		if (tmp != null) {
			routings = tmp.split(",");
		}
		tmp = elem.getAttributeStaticStr(SOCKET_ATTR);
		if (tmp != null) {
			socket = parseSocket(tmp);
		}
	}

	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");

		if (props.length > 0) {
			setDomain(props[0]);
		}
		if (props.length > 1) {
			auth_pass = props[1];
		}
		if (props.length > 2 && !props[2].trim().isEmpty()) {
			setConnectionType(props[2]);
		}
		if (props.length > 3 && !props[3].trim().isEmpty()) {
			port = parsePortNo(props[3]);
		}
		if (props.length > 4 && !props[4].trim().isEmpty()) {
			remoteHost = props[4];
		}
		if (props.length > 5 && !props[5].trim().isEmpty()) {
			setProtocol(props[5]);
		}
		if (props.length > 6 && !props[6].trim().isEmpty()) {
			lb = lbInstance(props[6]);
		}
		if (props.length > 7 && !props[7].trim().isEmpty()) {
			socket = parseSocket(props[7]);
		}
	}

	private SocketType parseSocket(String socket) {
		SocketType result = SocketType.plain;
		try {
			return SocketType.valueOf(socket.trim().toLowerCase());
		} catch (Exception e) {
			log.log(Level.WARNING,
			        "Error parsing external connection type: " + socket + ", using default: " + SocketType.plain, e);
		}
		return result;
	}

	public void setDomain(String domain) {
		this.domain = domain;
		routings    = new String[] { domain, ".*@" + domain, ".*\\." + domain };
	}

	@Override
	public Element toElement() {
		Element elem = super.toElement();

		elem.addAttribute(DOMAIN_ATTR, domain);
		elem.addAttribute(PASSWORD_ATTR, auth_pass);
		if ((remoteHost != null) &&!remoteHost.isEmpty()) {
			elem.addAttribute(REMOTE_HOST_ATTR, remoteHost);
		}
		elem.addAttribute(CONN_TYPE_ATTR, type.name());
		if (port > 0) {
			elem.addAttribute(PORT_NO_ATTR, "" + port);
		}
		if (prop_xmlns != null)
			elem.addAttribute(PROTO_XMLNS_ATTR, prop_xmlns);
		elem.addAttribute(LB_NAME_ATTR, lb.getClass().getName());

		StringBuilder route = new StringBuilder();

		for (String r : routings) {
			if (route.length() > 0) {
				route.append(',');
			}
			route.append(r);
		}
		elem.addAttribute(ROUTINGS_ATTR, route.toString());
		elem.addAttribute(SOCKET_ATTR, socket.toString());
		return elem;
	}

	@Override
	public String toPropertyString() {
		return domain + ":" + auth_pass + ":" + type.name() + ":" + port + ":" + remoteHost +
					 ":" + prop_xmlns + ":" + lb.getClass().getName() + ":" + socket.toString();
	}

	@Override
	public String toString() {
		return toPropertyString();
	}

	void setConnectionType(String connection_type) {
		this.type = parseConnectionType(connection_type);
	}

	void setPassword(String password) {
		this.auth_pass = password;
	}

	void setPort(int port) {
		this.port = port;
	}

	void setProtocol(String protocol) {
		this.prop_xmlns = protocol;
		this.xmlns      = parseProtoXMLNS(protocol);
	}

	void setRemoteDomain(String remote_domain) {
		this.remoteHost = remote_domain;
	}

	void setSocket(String socket) {
		this.socket = parseSocket(socket);
	}

	String validate() {
		if (domain == null)
			return "Domain name is required";
		if (auth_pass == null)
			return "Password is required";
		if (prop_xmlns == null)
			return "Protocol is required";	
		return null;
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
}