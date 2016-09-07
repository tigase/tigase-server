/*
 * ExtServiceDiscoItem.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.server.extdisco;

import tigase.db.comp.RepositoryItem;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Created by andrzej on 06.09.2016.
 */
public class ExtServiceDiscoItem implements RepositoryItem {

	private static final String KEY_LABEL = "Service";
	private static final String HOST_LABEL = "Host";
	private static final String NAME_LABEL = "Service name";
	private static final String PORT_LABEL = "Port";
	private static final String TRANSPORT_LABEL = "Transport";
	private static final String TYPE_LABEL = "Type";
	private static final String RESTRICTED_LABEL = "Requires username and password";
	private static final String USERNAME_LABEL = "Username";
	private static final String PASSWORD_LABEL = "Password";

	private String key;

	private String host;
	private String name;
	private Integer port;
	private String transport;
	private String type;
	private boolean restricted;
	private String username;
	private String password;

	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, KEY_LABEL, key != null ? key : "");
		Command.addFieldValue(packet, NAME_LABEL, name != null ? name : "");
		Command.addFieldValue(packet, HOST_LABEL, host != null ? host : "");
		Command.addFieldValue(packet, PORT_LABEL, port != null ? String.valueOf(port) : "");
		Command.addFieldValue(packet, TYPE_LABEL, type != null ? type : "");
		Command.addFieldValue(packet, TRANSPORT_LABEL, transport != null ? transport : "");
		Command.addCheckBoxField(packet, RESTRICTED_LABEL, restricted);
		Command.addFieldValue(packet, USERNAME_LABEL, username != null ? username : "");
		Command.addFieldValue(packet, PASSWORD_LABEL, password != null ? password : "");
	}

	@Override
	public String[] getAdmins() {
		return null;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getOwner() {
		return null;
	}

	public String getType() {
		return type;
	}

	@Override
	public void initFromCommand(Packet packet) {
		String tmp = Command.getFieldValue(packet, KEY_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			key = tmp;
		} else {
			key = null;
		}
		tmp = Command.getFieldValue(packet, NAME_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			name = tmp;
		} else {
			name = null;
		}
		tmp = Command.getFieldValue(packet, HOST_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			host = tmp;
		} else {
			host = null;
		}
		tmp = Command.getFieldValue(packet, PORT_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			port = Integer.parseInt(tmp);
		} else {
			port = null;
		}
		tmp = Command.getFieldValue(packet, TYPE_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			type = tmp;
		} else {
			type = null;
		}
		tmp = Command.getFieldValue(packet, TRANSPORT_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			transport = tmp;
		} else {
			transport = null;
		}
		restricted = Command.getCheckBoxFieldValue(packet, RESTRICTED_LABEL);
		tmp = Command.getFieldValue(packet, USERNAME_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			username = tmp;
		} else {
			username = null;
		}
		tmp = Command.getFieldValue(packet, PASSWORD_LABEL);
		if (tmp != null && !(tmp = tmp.trim()).isEmpty()) {
			password = tmp;
		} else {
			password = null;
		}

	}

	@Override
	public void initFromElement(Element elem) {
		key = elem.getAttributeStaticStr("key");
		host = elem.getAttributeStaticStr("host");
		type = elem.getAttributeStaticStr("type");

		String tmp = elem.getAttributeStaticStr("port");
		if (tmp != null) {
			port = Integer.parseInt(tmp);
		} else {
			port = null;
		}

		name = elem.getAttributeStaticStr("name");
		transport = elem.getAttributeStaticStr("transport");

		restricted = "true".equals(elem.getAttributeStaticStr("restricted"));
		username = elem.getAttributeStaticStr("username");
		password = elem.getAttributeStaticStr("password");
	}

	@Override
	public void initFromPropertyString(String propString) {
		String[] tmp = propString.split(":");
		key = tmp[0];
		for (String part : tmp) {
			int idx = part.indexOf("=");
			String key = part;
			String val = null;
			if (idx > -1) {
				val = part.substring(idx+1);
				key = part.substring(0, idx);
			}

			switch (key) {
				case "host":
					host = val;
					break;
				case "type":
					type = val;
					break;
				case "port":
					port = (val != null && !val.isEmpty()) ? Integer.parseInt(val) : null;
					break;
				case "name":
					name = val;
					break;
				case "transport":
					transport = val;
					break;
				case "restricted":
					restricted = "true".equals(val);
					break;
				case "username":
					username = val;
					break;
				case "password":
					password = val;
					break;
				default:
					break;
			}
		}
	}

	@Override
	public boolean isAdmin(String id) {
		return false;
	}

	@Override
	public boolean isOwner(String id) {
		return false;
	}

	@Override
	public void setAdmins(String[] admins) {

	}

	@Override
	public void setOwner(String owner) {

	}

	@Override
	public Element toElement() {
		Element service = new Element("service");
		service.setAttribute("key", key);
		service.setAttribute("host", host);
		service.setAttribute("type", type);
		if (port != null) {
			service.setAttribute("port", String.valueOf(port));
		}
		if (name != null) {
			service.setAttribute("name", name);
		}
		if (transport != null) {
			service.setAttribute("transport", transport);
		}

		// Options for credentials to authenticate for external service
		if (restricted){
			service.setAttribute("restricted", "true");
		}
		if (username != null) {
			service.setAttribute("username", username);
		}
		if (password != null) {
			service.setAttribute("password", password);
		}

		return service;
	}

	@Override
	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		if (host != null) {
			sb.append(":host=").append(host);
		}
		if (type != null) {
			sb.append(":type=").append(type);
		}
		if (port != null) {
			sb.append(":port=").append(String.valueOf(port));
		}
		if (name != null) {
			sb.append(":name=").append(name);
		}
		if (transport != null) {
			sb.append(":transport=").append(transport);
		}
		if (restricted) {
			sb.append(":restricted=true");
		}
		if (username != null) {
			sb.append(":username=").append(username);
		}
		if (password != null) {
			sb.append(":password=").append(password);
		}
		return sb.toString();
	}

}
