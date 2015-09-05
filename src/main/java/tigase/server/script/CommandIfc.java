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

package tigase.server.script;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Iq;
import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

import javax.script.Bindings;
import tigase.stats.StatisticHolder;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: Jan 2, 2009 1:20:16 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface CommandIfc extends StatisticHolder {

	/** Field description */
	public static final String VHOST_MANAGER = "vhostMan";

	/** Field description */
	public static final String ADMINS_SET = "adminsSet";

	/** Field description */
	public static final String COMMANDS_ACL = "cmdsAcl";

	/** Field description */
	public static final String ADMN_CMDS = "adminCommands";

	/** Field description */
	public static final String USER_REPO = "userRepository";

	/** Field description */
	public static final String AUTH_REPO = "authRepository";

	/** Field description */
	public static final String USER_SESS = "userSessions";

	/** Field description */
	public static final String USER_CONN = "userConnections";

	/** Field description */
	public static final String ADMN_DISC = "adminDisco";

	/** Field description */
	public static final String SCRI_MANA = "scriptManager";

	/** Field description */
	public static final String SCRIPT_BASE_DIR = "scriptBaseDir";

	/** Field description */
	public static final String SCRIPT_COMP_DIR = "scriptCompDir";

	/** Field description */
	public static final String COMPONENT_NAME = "componentName";
	
	/** Field description */
	public static final String COMPONENT = "component";

	public static final String CONNECTED_NODES = "connectedNodes";
	public static final String CONNECTED_NODES_WITH_LOCAL = "connectedNodesWithLocal";
	
	/** Field description */
	public static final String SERVICES_MAP = "servicesMap";

	/** Field description */
	public static final String SCRIPT_DESCRIPTION = "AS:Description:";

	/** Field description */
	public static final String SCRIPT_ID = "AS:CommandId:";

	/** Field description */
	public static final String SCRIPT_COMPONENT = "AS:Component:";

	/** Field description */
	public static final String SCRIPT_CLASS = "AS:ComponentClass:";

	/** Field description */
	public static final String SCRIPT_GROUP = "AS:Group:";
	
	/** Field description */
	public static final String LANGUAGE = "Language";

	/** Field description */
	public static final String COMMAND_ID = "Command Id";

	/** Field description */
	public static final String SCRIPT_TEXT = "Script text";

	/** Field description */
	public static final String SCRIPT_RESULT = "Script result";

	/** Field description */
	public static final String DESCRIPT = "Description";

	/** Field description */
	public static final String GROUP = "Group";
	
	/** Field description */
	public static final String SAVE_TO_DISK = "Save to disk";

	/** Field description */
	public static final String REMOVE_FROM_DISK = "Remove from disk";

	/** Field description */
	public static final String PACKET = "packet";

	/** Field description */
	public static final String ADD_SCRIPT_CMD = "add-script";

	/** Field description */
	public static final String DEL_SCRIPT_CMD = "del-script";
	
	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Bindings getBindings();

	String getCommandId();

	String getDescription();
	
	String getGroup();

	//~--- methods --------------------------------------------------------------

	void init(String id, String description, String group);

	//~--- get methods ----------------------------------------------------------

	boolean isAdminOnly();

	//~--- methods --------------------------------------------------------------

	void runCommand(Iq packet, Bindings binds, Queue<Packet> results);

	//~--- set methods ----------------------------------------------------------

	void setAdminOnly(boolean adminOnly);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
