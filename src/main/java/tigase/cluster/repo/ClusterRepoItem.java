/*
 * ClusterRepoItem.java
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



package tigase.cluster.repo;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.RepositoryItemAbstract;

import tigase.server.Command;
import tigase.server.Packet;

import tigase.util.Algorithms;

import tigase.xml.Element;

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.text.DateFormat;
import java.text.ParseException;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/03/09
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class ClusterRepoItem
				extends RepositoryItemAbstract {
	/** Field description */
	public static final String CPU_USAGE_ATTR = "cpu";

	/** Field description */
	public static final String CPU_USAGE_LABEL = "CPU usage";

	/** Field description */
	public static final String HOSTNAME_ATTR = "host";

	/** Field description */
	public static final String HOSTNAME_LABEL = "Hostname";

	/** Field description */
	public static final String LAST_UPDATE_ATTR = "updated";

	/** Field description */
	public static final String LAST_UPDATE_LABEL = "Last update";

	/** Field description */
	public static final String MEM_USAGE_ATTR = "mem";

	/** Field description */
	public static final String MEM_USAGE_LABEL = "Memory usage";

	/** Field description */
	public static final String PASSWORD_ATTR = "passwd";

	/** Field description */
	public static final String PASSWORD_LABEL = "Password";

	/** Field description */
	public static final String PASSWORD_PROP_VAL = "someSecret";

	/** Field description */
	public static final String PORT_NO_ATTR = "port";

	/** Field description */
	public static final String PORT_NO_LABEL = "Port number";

	/** Field description */
	public static final int PORT_NO_PROP_VAL = 5277;

	/** Field description */
	public static final String REPO_ITEM_ELEM_NAME = "item";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(ClusterRepoItem.class.getName());

	//~--- fields ---------------------------------------------------------------

	private float  cpuUsage   = 0f;
	private String hostname   = null;
	private long   lastUpdate = 0l;
	private float  memUsage   = 0f;
	private String password   = Algorithms.sha256(UUID.randomUUID().toString());
	private int    portNo     = PORT_NO_PROP_VAL;

	//~--- methods --------------------------------------------------------------

	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, HOSTNAME_LABEL, ((hostname != null)
				? hostname
				: ""));
		Command.addFieldValue(packet, PASSWORD_LABEL, ((password != null)
				? password
				: ""));
		Command.addFieldValue(packet, PORT_NO_LABEL, ((portNo > 0)
				? "" + portNo
				: ""));
		Command.addFieldValue(packet, LAST_UPDATE_LABEL, "" + new Date(lastUpdate));
		Command.addFieldValue(packet, CPU_USAGE_LABEL, "" + cpuUsage);
		Command.addFieldValue(packet, MEM_USAGE_LABEL, "" + memUsage);
		super.addCommandFields(packet);
	}

	@Override
	public void initFromCommand(Packet packet) {
		super.initFromCommand(packet);
		hostname = Command.getFieldValue(packet, HOSTNAME_LABEL);
		password = Command.getFieldValue(packet, PASSWORD_LABEL);

		String tmp = Command.getFieldValue(packet, LAST_UPDATE_LABEL);

		if ((tmp != null) &&!tmp.isEmpty()) {
			try {
				lastUpdate = DateFormat.getInstance().parse(tmp).getTime();
			} catch (ParseException ex) {
				lastUpdate = System.currentTimeMillis();
			}
		}
		tmp = Command.getFieldValue(packet, PORT_NO_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			portNo = parsePortNo(tmp);
		}
		tmp = Command.getFieldValue(packet, CPU_USAGE_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			cpuUsage = Float.parseFloat(tmp);
		}
		tmp = Command.getFieldValue(packet, MEM_USAGE_LABEL);
		if ((tmp != null) &&!tmp.isEmpty()) {
			memUsage = Float.parseFloat(tmp);
		}
	}

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != REPO_ITEM_ELEM_NAME) {
			throw new IllegalArgumentException("Incorrect element name, expected: " +
					REPO_ITEM_ELEM_NAME);
		}
		super.initFromElement(elem);
		hostname   = elem.getAttributeStaticStr(HOSTNAME_ATTR);
		password   = elem.getAttributeStaticStr(PASSWORD_ATTR);
		portNo     = parsePortNo(elem.getAttributeStaticStr(PORT_NO_ATTR));
		lastUpdate = Long.parseLong(elem.getAttributeStaticStr(LAST_UPDATE_ATTR));
		cpuUsage   = Float.parseFloat(elem.getAttributeStaticStr(CPU_USAGE_ATTR));
		memUsage   = Float.parseFloat(elem.getAttributeStaticStr(MEM_USAGE_ATTR));
	}

	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");

		if (props.length > 0) {
			hostname = BareJID.parseJID(props[0])[1];
		}
		if ((props.length > 1) &&!props[1].trim().isEmpty()) {
			password = props[1];
		}
		if (props.length > 2) {
			portNo = parsePortNo(props[2]);
		}
		if (props.length > 3) {
			lastUpdate = Long.parseLong(props[3]);
		}
		if (props.length > 4) {
			cpuUsage = Float.parseFloat(props[4]);
		}
		if (props.length > 5) {
			memUsage = Float.parseFloat(props[5]);
		}
	}

	@Override
	public Element toElement() {
		Element elem = super.toElement();

		elem.addAttribute(HOSTNAME_ATTR, hostname);
		elem.addAttribute(PASSWORD_ATTR, password);
		elem.addAttribute(PORT_NO_ATTR, "" + portNo);
		elem.addAttribute(LAST_UPDATE_ATTR, "" + lastUpdate);
		elem.addAttribute(CPU_USAGE_ATTR, "" + cpuUsage);
		elem.addAttribute(MEM_USAGE_ATTR, "" + memUsage);

		return elem;
	}

	@Override
	public String toPropertyString() {
		return hostname + ":" + password + ":" + portNo + ":" + lastUpdate + ":" + cpuUsage +
				":" + memUsage;
	}

	@Override
	public String toString() {
		return toPropertyString();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public float getCpuUsage() {
		return cpuUsage;
	}

	@Override
	public String getElemName() {
		return REPO_ITEM_ELEM_NAME;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getHostname() {
		return hostname;
	}

	@Override
	public String getKey() {
		return hostname;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public float getMemUsage() {
		return memUsage;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public int getPortNo() {
		return portNo;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 * @param cpuUsage
	 */
	protected void setCpuUsage(float cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param hostname
	 */
	protected void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param update
	 */
	protected void setLastUpdate(long update) {
		this.lastUpdate = update;
	}

	/**
	 * Method description
	 *
	 *
	 * @param memUsage
	 */
	protected void setMemUsage(float memUsage) {
		this.memUsage = memUsage;
	}

	/**
	 * Method description
	 *
	 *
	 * @param password
	 */
	protected void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Method description
	 *
	 *
	 * @param port
	 */
	protected void setPort(int port) {
		this.portNo = port;
	}

	//~--- methods --------------------------------------------------------------

	private int parsePortNo(String input) {
		int result;

		try {
			result = Integer.parseInt(input);
		} catch (Exception e) {
			result = 5277;
			log.log(Level.WARNING, "Incorrect port number, can't parse: {0}", input);
		}

		return result;
	}
}


//~ Formatted in Tigase Code Convention on 13/07/06
