/*
 * CommandIfc.java
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



package tigase.server.script;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Iq;
import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

import javax.script.Bindings;

/**
 * Created: Jan 2, 2009 1:20:16 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface CommandIfc {
	/** Field description */
	public static final String ADD_SCRIPT_CMD = "add-script";

	/** Field description */
	public static final String ADMINS_SET = "adminsSet";

	/** Field description */
	public static final String ADMN_CMDS = "adminCommands";

	/** Field description */
	public static final String ADMN_DISC = "adminDisco";

	/** Field description */
	public static final String AUTH_REPO = "authRepository";

	/** Field description */
	public static final String COMMAND_ID = "Command Id";

	/** Field description */
	public static final String COMMANDS_ACL = "cmdsAcl";

	/** Field description */
	public static final String COMPONENT_NAME = "componentName";

	/** Field description */
	public static final String DEL_SCRIPT_CMD = "del-script";

	/** Field description */
	public static final String DESCRIPT = "Description";

	/** Field description */
	public static final String LANGUAGE = "Language";

	/** Field description */
	public static final String PACKET = "packet";

	/** Field description */
	public static final String SAVE_TO_DISK = "Save to disk";

	/** Field description */
	public static final String SCRI_MANA = "scriptManager";

	/** Field description */
	public static final String SCRIPT_BASE_DIR = "scriptBaseDir";

	/** Field description */
	public static final String SCRIPT_COMP_DIR = "scriptCompDir";

	/** Field description */
	public static final String SCRIPT_COMPONENT = "AS:Component:";

	/** Field description */
	public static final String SCRIPT_DESCRIPTION = "AS:Description:";

	/** Field description */
	public static final String SCRIPT_ID = "AS:CommandId:";

	/** Field description */
	public static final String SCRIPT_RESULT = "Script result";

	/** Field description */
	public static final String SCRIPT_TEXT = "Script text";

	/** Field description */
	public static final String SERVICES_MAP = "servicesMap";

	/** Field description */
	public static final String USER_CONN = "userConnections";

	/** Field description */
	public static final String USER_REPO = "userRepository";

	/** Field description */
	public static final String USER_SESS = "userSessions";

	/** Field description */
	public static final String VHOST_MANAGER = "vhostMan";

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>Bindings</code>
	 */
	public Bindings getBindings();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id is a <code>String</code>
	 * @param description is a <code>String</code>
	 */
	void init(String id, String description);

	/**
	 * Method description
	 *
	 *
	 * @param packet is a <code>Iq</code>
	 * @param binds is a <code>Bindings</code>
	 * @param results is a <code>Queue<Packet></code>
	 */
	void runCommand(Iq packet, Bindings binds, Queue<Packet> results);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	String getCommandId();

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	String getDescription();

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	boolean isAdminOnly();

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param adminOnly is a <code>boolean</code>
	 */
	void setAdminOnly(boolean adminOnly);
}


//~ Formatted in Tigase Code Convention on 13/08/28
